package cn.universal.admin.platform.service.debug;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.admin.platform.service.impl.MagicFunctionCatalogService;
import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.annotation.Function;
import cn.universal.core.engine.extend.HexFunctions;
import cn.universal.core.engine.extend.ModbusFunctions;
import cn.universal.core.engine.extend.TypeCheckFunctions;
import cn.universal.core.engine.extend.convert.ConvertFunctions;
import cn.universal.core.engine.reflection.JavaReflection;
import cn.universal.core.engine.runtime.RuntimeContext;
import java.util.List;
import org.junit.jupiter.api.Test;

class MagicFunctionCatalogServiceTest {
  public static class PluginFunctions {
    @Function
    @Comment("插件扩展函数")
    public String custom_helper(
        RuntimeContext context, @Comment(name = "payload", value = "原始报文") String value) {
      return value;
    }

    public String internalMethod() {
      return "not a script function";
    }
  }

  @Test
  void catalogsEveryRegisteredFunctionIncludingBuiltinsAndExternalPlugins() {
    var targets =
        List.of(
            new HexFunctions(),
            new ModbusFunctions(),
            new ConvertFunctions(),
            new TypeCheckFunctions(),
            new PluginFunctions());
    try {
      targets.forEach(JavaReflection::registerFunction);
      var catalog = new MagicFunctionCatalogService().listFunctions();
      var names = catalog.stream().map(item -> item.getStr("name")).toList();
      assertTrue(
          names.containsAll(
              List.of(
                  "println",
                  "hex_645AddrDecode",
                  "hex_reverse",
                  "modbus_crc16",
                  "isJson",
                  "custom_helper")));
      assertFalse(names.contains("internalMethod"));
      var custom =
          catalog.stream()
              .filter(item -> "custom_helper".equals(item.getStr("name")))
              .findFirst()
              .orElseThrow();
      assertEquals("custom_helper(String payload)", custom.getStr("signature"));
      assertEquals(1, custom.getInt("paramCount"));
      assertEquals("原始报文", custom.getJSONArray("params").getJSONObject(0).getStr("description"));
      var hex =
          catalog.stream()
              .filter(item -> "hex_645AddrDecode".equals(item.getStr("name")))
              .findFirst()
              .orElseThrow();
      assertTrue(hex.getStr("description").contains("645"));
    } finally {
      JavaReflection.getFunctions()
          .removeIf(invoker -> targets.contains(invoker.getDefaultTarget()));
    }
  }
}
