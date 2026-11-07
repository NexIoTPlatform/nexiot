package cn.universal.databridge.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DataBridgeMagicScriptEngine 测试")
class DataBridgeMagicScriptEngineTest {

  private final DataBridgeMagicScriptEngine engine = new DataBridgeMagicScriptEngine();

  @Test
  @DisplayName("数据桥Magic脚本应能读取工业采样rawData/payload/data上下文")
  @SuppressWarnings("unchecked")
  void shouldExposeIndustrialRawContextToMagicScript() {
    BaseUPRequest request =
        BaseUPRequest.builder()
            .ioTDeviceDTO(
                IoTDeviceDTO.builder()
                    .iotId("iot-1")
                    .deviceId("plc-1")
                    .productKey("pk-industrial")
                    .deviceName("PLC-1")
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
            .productKey("pk-industrial")
            .deviceName("PLC-1")
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
                            "%DB1.DBD4:REAL",
                            "value",
                            12.3D))))
            .payload("{\"source\":\"industrial\"}")
            .time(1710000000123L)
            .build();
    DataBridgeConfig config = new DataBridgeConfig();
    config.setConfig("{\"target\":\"qa\"}");

    Object result =
        engine.executeIotToYourScript(
            """
            var iotToYour = (request, config, connection) => {
              return {
                deviceId: deviceId,
                productKey: productKey,
                iotId: iotId,
                deviceName: deviceName,
                applicationId: applicationId,
                tenantId: tenantId,
                deviceNode: deviceNode,
                deviceKey: device.deviceId,
                productName: product.productName,
                transportProtocol: product.transportProtocol,
                applicationKey: application.applicationId,
                source: rawData.source,
                quality: rawData.qualities.temperature,
                pointId: rawData.samples[0].pointId,
                tag: rawData.samples[0].tagAddress,
                value: data.temperature,
                time: time,
                payloadText: payload,
                target: configJson.target
              }
            }
            """,
            request,
            config,
            null);

    Map<String, Object> output = (Map<String, Object>) result;
    assertEquals("plc-1", output.get("deviceId"));
    assertEquals("pk-industrial", output.get("productKey"));
    assertEquals("iot-1", output.get("iotId"));
    assertEquals("PLC-1", output.get("deviceName"));
    assertEquals("app-1", output.get("applicationId"));
    assertEquals("tenant-1", output.get("tenantId"));
    assertEquals("DEVICE", output.get("deviceNode"));
    assertEquals("plc-1", output.get("deviceKey"));
    assertEquals("Industrial Product", output.get("productName"));
    assertEquals("modbus", output.get("transportProtocol"));
    assertEquals("app-1", output.get("applicationKey"));
    assertEquals("industrial", output.get("source"));
    assertEquals("GOOD", output.get("quality"));
    assertEquals(1001, ((Number) output.get("pointId")).intValue());
    assertEquals("%DB1.DBD4:REAL", output.get("tag"));
    assertEquals(12.3D, ((Number) output.get("value")).doubleValue());
    assertEquals(1710000000123L, ((Number) output.get("time")).longValue());
    assertEquals("{\"source\":\"industrial\"}", output.get("payloadText"));
    assertEquals("qa", output.get("target"));
  }
}
