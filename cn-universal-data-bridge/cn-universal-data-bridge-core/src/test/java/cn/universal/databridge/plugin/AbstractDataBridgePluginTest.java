package cn.universal.databridge.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.PluginInfo;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AbstractDataBridgePlugin 测试")
class AbstractDataBridgePluginTest {

  private final TestDataBridgePlugin plugin = new TestDataBridgePlugin();

  @Test
  @DisplayName("数据桥模板变量应保留工业采样data/rawData/payload上下文")
  void shouldExposeIndustrialContextToTemplateVariables() {
    BaseUPRequest request =
        BaseUPRequest.builder()
            .ioTDeviceDTO(
                IoTDeviceDTO.builder()
                    .iotId("iot-1")
                    .deviceId("plc-1")
                    .deviceName("PLC-1")
                    .productKey("pk-industrial")
                    .applicationId("app-1")
                    .tenantId("tenant-1")
                    .deviceNode(DeviceNode.DEVICE)
                    .build())
            .ioTProduct(
                IoTProduct.builder()
                    .productKey("pk-industrial")
                    .name("Industrial Product")
                    .thirdPlatform("industrial")
                    .transportProtocol("modbus")
                    .tenantId("tenant-1")
                    .build())
            .iotId("iot-1")
            .deviceId("plc-1")
            .deviceName("PLC-1")
            .productKey("pk-industrial")
            .applicationId("app-1")
            .tenantId("tenant-1")
            .deviceNode(DeviceNode.DEVICE)
            .messageType(MessageType.PROPERTIES)
            .properties(Map.of("temperature", 12.3D))
            .data(Map.of("temperature", 12.3D))
            .rawData(
                Map.of(
                    "source",
                    "industrial",
                    "qualities",
                    Map.of("temperature", "GOOD"),
                    "samples",
                    List.of(
                        Map.of(
                            "pointId",
                            1001L,
                            "tagAddress",
                            "holding-register:1:INT",
                            "value",
                            12.3D))))
            .payload("{\"source\":\"industrial\"}")
            .time(1710000000123L)
            .build();

    JSONObject configJson = JSONUtil.parseObj("{\"field_mapping\":{\"line\":\"A1\"}}");
    Map<String, Object> variables = plugin.variables(request, configJson);

    assertEquals("plc-1", variables.get("deviceId"));
    assertEquals("plc-1", variables.get("deviceKey"));
    assertEquals("pk-industrial", variables.get("productKey"));
    assertEquals("app-1", variables.get("applicationId"));
    assertEquals("tenant-1", variables.get("tenantId"));
    assertEquals("PROPERTIES", variables.get("messageType"));
    assertEquals("DEVICE", variables.get("deviceNode"));
    assertEquals("plc-1", ((Map<?, ?>) variables.get("device")).get("deviceId"));
    assertEquals("Industrial Product", ((Map<?, ?>) variables.get("product")).get("productName"));
    assertEquals("modbus", ((Map<?, ?>) variables.get("product")).get("transportProtocol"));
    assertEquals("app-1", ((Map<?, ?>) variables.get("application")).get("applicationId"));
    assertEquals(Map.of("temperature", 12.3D), variables.get("properties"));
    assertEquals(Map.of("temperature", 12.3D), variables.get("data"));
    assertEquals("{\"source\":\"industrial\"}", variables.get("payload"));
    assertEquals("A1", variables.get("line"));
    assertEquals(configJson, variables.get("configJson"));

    String rendered =
        plugin.template(
            "#{device.deviceId}|#{product.transportProtocol}|#{application.applicationId}|#{data.temperature}|#{rawData.qualities.temperature}|#{rawData.samples.0.tagAddress}|#{payload}|#{line}",
            variables);
    assertEquals(
        "plc-1|modbus|app-1|12.3|GOOD|holding-register:1:INT|{\"source\":\"industrial\"}|A1",
        rendered);
  }

  @Test
  @DisplayName("数据桥模板变量缺少deviceNode和messageType时不应异常")
  void shouldBuildTemplateVariablesWhenOptionalEnumsMissing() {
    BaseUPRequest request = BaseUPRequest.builder().deviceId("plc-1").build();

    Map<String, Object> variables = plugin.variables(request, new JSONObject());

    assertEquals("plc-1", variables.get("deviceId"));
    assertNull(variables.get("deviceNode"));
    assertNull(variables.get("messageType"));
  }

  private static final class TestDataBridgePlugin extends AbstractDataBridgePlugin {

    Map<String, Object> variables(BaseUPRequest request, JSONObject configJson) {
      Map<String, Object> variables = buildTemplateVariables(request, configJson);
      variables.putIfAbsent("deviceKey", variables.get("deviceId"));
      return variables;
    }

    String template(String template, Map<String, Object> variables) {
      return processTemplate(template, variables);
    }

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
