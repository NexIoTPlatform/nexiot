package cn.universal.databridge.plugin.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.databridge.engine.ParamSql;
import cn.universal.databridge.engine.ParamTemplateEngine;
import cn.universal.databridge.engine.dialect.MySqlDialectAdapter;
import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.PluginInfo;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.databridge.plugin.AbstractDataBridgePlugin;
import cn.universal.databridge.plugin.SourceScope;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 快速模板测试 - 验证通用模板处理功能 */
public class QuickTemplateTest {

  @Test
  public void testGenericTemplateProcessing() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("csq", "25");
    properties.put("code", "200");
    properties.put("temperature", "23.5");

    Map<String, Object> variables = new HashMap<>();
    variables.put("deviceId", "device001");
    variables.put("properties", properties);

    String template = "Device: #{deviceId}, CSQ: #{properties.csq}, Code: #{properties.code}";

    System.out.println("=== 通用模板处理测试 ===");
    System.out.println("原始模板: " + template);

    try {
      AbstractDataBridgePlugin genericPlugin =
          new AbstractDataBridgePlugin() {
            @Override
            public PluginInfo getPluginInfo() {
              return null;
            }

            @Override
            public Boolean testConnection(ResourceConnection connection) {
              return null;
            }

            @Override
            public Boolean validateConfig(DataBridgeConfig config) {
              return null;
            }

            @Override
            public List<SourceScope> getSupportedSourceScopes() {
              return List.of();
            }
          };
      java.lang.reflect.Method genericMethod =
          AbstractDataBridgePlugin.class.getDeclaredMethod(
              "processTemplate", String.class, Map.class);
      genericMethod.setAccessible(true);
      String genericResult = (String) genericMethod.invoke(genericPlugin, template, variables);

      System.out.println("通用处理结果: " + genericResult);

      assertTrue(genericResult.contains("device001"));
      assertTrue(genericResult.contains("25"));
      assertTrue(genericResult.contains("200"));
      assertFalse(genericResult.contains("'device001'")); // 不应该有单引号

      System.out.println("✅ 通用模板处理测试通过！");

    } catch (Exception e) {
      fail("测试失败: " + e.getMessage());
    }
  }

  @Test
  public void testJdbcParamTemplateDoesNotInlineSqlValues() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("remark", "x'); drop table device_data; --");

    Map<String, Object> variables = new HashMap<>();
    variables.put("deviceId", "device001");
    variables.put("properties", properties);

    String template =
        "insert into device_data(device_id, remark) values(#{deviceId}, #{properties.remark})";
    ParamSql result = new ParamTemplateEngine().process(template, variables, new MySqlDialectAdapter());

    assertEquals("insert into device_data(device_id, remark) values(?, ?)", result.getSql());
    assertEquals(List.of("device001", "x'); drop table device_data; --"), result.getParams());
    assertFalse(result.getSql().contains("drop table"));
  }

  @Test
  public void testNestedPropertyAccess() {
    Map<String, Object> level2 = new HashMap<>();
    level2.put("value", "nested_value");

    Map<String, Object> level1 = new HashMap<>();
    level1.put("level2", level2);

    Map<String, Object> variables = new HashMap<>();
    variables.put("root", level1);

    String template = "Nested: #{root.level2.value}";

    AbstractDataBridgePlugin plugin =
        new AbstractDataBridgePlugin() {
          @Override
          public PluginInfo getPluginInfo() {
            return null;
          }

          @Override
          public Boolean testConnection(ResourceConnection connection) {
            return null;
          }

          @Override
          public Boolean validateConfig(DataBridgeConfig config) {
            return null;
          }

          @Override
          public List<SourceScope> getSupportedSourceScopes() {
            return List.of();
          }
        };

    try {
      java.lang.reflect.Method method =
          AbstractDataBridgePlugin.class.getDeclaredMethod(
              "processTemplate", String.class, Map.class);
      method.setAccessible(true);
      String result = (String) method.invoke(plugin, template, variables);

      System.out.println("=== 嵌套属性访问测试 ===");
      System.out.println("模板: " + template);
      System.out.println("结果: " + result);

      assertEquals("Nested: nested_value", result);
      System.out.println("✅ 嵌套属性访问测试通过！");

    } catch (Exception e) {
      fail("测试失败: " + e.getMessage());
    }
  }
}
