package cn.universal.databridge.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.PluginInfo;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.persistence.base.BaseUPRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AbstractDataOutputPlugin 测试")
class AbstractDataOutputPluginTest {

  private final TestDataOutputPlugin plugin = new TestDataOutputPlugin();

  @Test
  @DisplayName("Magic脚本结构化返回值应按JSON输出")
  void shouldRenderStructuredMagicResultAsJson() {
    Map<String, Object> sample = new LinkedHashMap<>();
    sample.put("tag", "holding-register:1:INT");
    sample.put("value", 12);

    Map<String, Object> processedData = new LinkedHashMap<>();
    processedData.put("deviceId", "plc-1");
    processedData.put("samples", List.of(sample));

    String payload = plugin.payload(processedData);

    assertEquals(
        "{\"deviceId\":\"plc-1\",\"samples\":[{\"tag\":\"holding-register:1:INT\",\"value\":12}]}",
        payload);
  }

  @Test
  @DisplayName("Magic脚本字符串返回值应原样输出")
  void shouldKeepStringMagicResultAsIs() {
    assertEquals("{\"ok\":true}", plugin.payload("{\"ok\":true}"));
  }

  private static final class TestDataOutputPlugin extends AbstractDataOutputPlugin {

    String payload(Object processedData) {
      return renderProcessedPayload(processedData);
    }

    @Override
    protected void processProcessedData(
        Object processedData,
        BaseUPRequest request,
        DataBridgeConfig config,
        ResourceConnection connection) {}

    @Override
    protected void processTemplateResult(
        String templateResult,
        BaseUPRequest request,
        DataBridgeConfig config,
        ResourceConnection connection) {}

    @Override
    public PluginInfo getPluginInfo() {
      return PluginInfo.builder().name("test").build();
    }

    @Override
    public Boolean testConnection(ResourceConnection connection) {
      return true;
    }

    @Override
    public Boolean validateConfig(DataBridgeConfig config) {
      return true;
    }

    @Override
    public List<SourceScope> getSupportedSourceScopes() {
      return List.of(SourceScope.ALL_PRODUCTS);
    }
  }
}
