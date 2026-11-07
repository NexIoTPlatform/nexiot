package cn.universal.dm.device.service.push.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.dm.device.service.push.transform.executor.ScriptPushTransformExecutor;
import cn.universal.dm.device.service.push.transform.executor.TemplatePushTransformExecutor;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTPushTransformRule;
import cn.universal.persistence.entity.bo.PushTransformPreviewBO;
import cn.universal.persistence.entity.bo.UPPushBO;
import cn.universal.persistence.entity.vo.PushTransformPreviewVO;
import cn.universal.persistence.mapper.IoTPushTransformRuleMapper;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PushTransformService 测试")
class PushTransformServiceTest {

  private PushTransformService service;

  @BeforeEach
  void setUp() throws Exception {
    service = new PushTransformService();
    setField(service, "templatePushTransformExecutor", new TemplatePushTransformExecutor());
    setField(service, "scriptPushTransformExecutor", new ScriptPushTransformExecutor());
    setField(service, "contextBuilder", new PushTransformContextBuilder());
  }

  @Test
  @DisplayName("应用推送转换应保留工业采样rawData/payload/data上下文")
  @SuppressWarnings("unchecked")
  void shouldExposeIndustrialRawContextInActualAppPushTransform() throws Exception {
    setField(
        service,
        "ioTPushTransformRuleMapper",
        mapperReturningPublishedRules(
            List.of(
                IoTPushTransformRule.builder()
                    .id(100L)
                    .version(1)
                    .appUniqueId("app-1")
                    .scopeType("PRODUCT")
                    .productKey("pk-industrial")
                    .ruleType("SCRIPT")
                    .content(
                        """
                        return {
                          deviceId: ctx.deviceId,
                          productKey: ctx.productKey,
                          deviceKey: ctx.device.deviceId,
                          productName: ctx.product.productName,
                          transportProtocol: ctx.product.transportProtocol,
                          applicationKey: ctx.application.applicationId,
                          source: ctx.rawData.source,
                          quality: ctx.rawData.qualities.temperature,
                          tag: ctx.rawData.samples[0].tagAddress,
                          valueFromData: ctx.data.temperature,
                          payloadText: ctx.payload
                        }
                        """)
                    .status(1)
                    .build())));

    BaseUPRequest request =
        BaseUPRequest.builder()
            .applicationId("app-1")
            .iotId("iot-1")
            .deviceId("plc-1")
            .productKey("pk-industrial")
            .deviceName("PLC-1")
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
                    List.of(Map.of("tagAddress", "holding-register:1:INT", "value", 12.3D))))
            .payload("{\"source\":\"industrial\",\"driver\":\"MODBUS\"}")
            .ioTDeviceDTO(
                IoTDeviceDTO.builder()
                    .applicationId("app-1")
                    .iotId("iot-1")
                    .deviceId("plc-1")
                    .productKey("pk-industrial")
                    .deviceName("PLC-1")
                    .deviceNode(DeviceNode.DEVICE)
                    .tenantId("tenant-1")
                    .build())
            .ioTProduct(
                IoTProduct.builder()
                    .productKey("pk-industrial")
                    .name("Industrial Product")
                    .thirdPlatform("industrial")
                    .transportProtocol("modbus")
                    .tenantId("tenant-1")
                    .build())
            .build();
    UPPushBO config =
        UPPushBO.builder()
            .transform(
                UPPushBO.TransformConfig.builder()
                    .support(true)
                    .enable(true)
                    .fallbackMode("ORIGINAL")
                    .build())
            .build();

    String transformed = service.transformPayload(request, config, "{}");

    Map<String, Object> output = JSONUtil.toBean(transformed, Map.class);
    assertEquals("plc-1", output.get("deviceId"));
    assertEquals("pk-industrial", output.get("productKey"));
    assertEquals("plc-1", output.get("deviceKey"));
    assertEquals("Industrial Product", output.get("productName"));
    assertEquals("modbus", output.get("transportProtocol"));
    assertEquals("app-1", output.get("applicationKey"));
    assertEquals("industrial", output.get("source"));
    assertEquals("GOOD", output.get("quality"));
    assertEquals("holding-register:1:INT", output.get("tag"));
    assertEquals(12.3D, ((Number) output.get("valueFromData")).doubleValue());
    assertEquals("{\"source\":\"industrial\",\"driver\":\"MODBUS\"}", output.get("payloadText"));
  }

  @Test
  @DisplayName("推送转换预览应保留工业采样rawData上下文")
  @SuppressWarnings("unchecked")
  void shouldExposeIndustrialRawDataInPreviewTransform() {
    PushTransformPreviewBO previewBO =
        PushTransformPreviewBO.builder()
            .appUniqueId("app-1")
            .ruleType("SCRIPT")
            .sampleInput(
                """
                {
                  "productKey": "pk-industrial",
                  "deviceId": "plc-1",
                  "deviceName": "PLC-1",
                  "iotId": "iot-1",
                  "deviceNode": "DEVICE",
                  "messageType": "PROPERTIES",
                  "time": 1710000000123,
                  "properties": {
                    "temperature": 12.3
                  },
                  "rawData": {
                    "source": "industrial",
                    "qualities": {
                      "temperature": "GOOD"
                    },
                    "samples": [
                      {
                        "pointId": 1001,
                        "tagAddress": "%DB1.DBD4:REAL",
                        "value": 12.3,
                        "quality": "GOOD"
                      }
                    ]
                  }
                }
                """)
            .content(
                """
                return {
                  deviceId: ctx.deviceId,
                  source: ctx.rawData.source,
                  quality: ctx.rawData.qualities.temperature,
                  pointId: ctx.rawData.samples[0].pointId,
                  tag: ctx.rawData.samples[0].tagAddress,
                  value: ctx.properties.temperature
                }
                """)
            .build();

    PushTransformPreviewVO result = service.preview(previewBO);

    assertEquals(Boolean.TRUE, result.getSuccess());
    Map<String, Object> output = JSONUtil.toBean(result.getOutputJson(), Map.class);
    assertEquals("plc-1", output.get("deviceId"));
    assertEquals("industrial", output.get("source"));
    assertEquals("GOOD", output.get("quality"));
    assertEquals(1001, ((Number) output.get("pointId")).intValue());
    assertEquals("%DB1.DBD4:REAL", output.get("tag"));
    assertEquals(12.3D, ((Number) output.get("value")).doubleValue());

    Map<String, Object> input = JSONUtil.toBean(result.getInputJson(), Map.class);
    Map<String, Object> rawData = (Map<String, Object>) input.get("rawData");
    assertEquals("industrial", rawData.get("source"));
  }

  @Test
  @DisplayName("同一应用始终使用唯一规则，不再按产品匹配")
  @SuppressWarnings("unchecked")
  void shouldUseSingleApplicationRuleForEveryProduct() throws Exception {
    setField(
        service,
        "ioTPushTransformRuleMapper",
        mapperReturningPublishedRules(
            List.of(
                IoTPushTransformRule.builder()
                    .id(1L)
                    .ruleType("SCRIPT")
                    .content("return { selected: 'application' };")
                    .status(1)
                    .build(),
                IoTPushTransformRule.builder()
                    .id(2L)
                    .scopeType("PRODUCT")
                    .productKey("pk-2")
                    .ruleType("SCRIPT")
                    .content("return { selected: 'legacy-product' };")
                    .status(1)
                    .build())));
    BaseUPRequest request =
        BaseUPRequest.builder().applicationId("app-1").productKey("pk-2").build();
    UPPushBO config =
        UPPushBO.builder()
            .transform(
                UPPushBO.TransformConfig.builder().enable(true).support(true).build())
            .build();

    Map<String, Object> output =
        JSONUtil.toBean(service.transformPayload(request, config, "{}"), Map.class);

    assertEquals("application", output.get("selected"));
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = PushTransformService.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static IoTPushTransformRuleMapper mapperReturningPublishedRules(
      List<IoTPushTransformRule> rules) {
    return (IoTPushTransformRuleMapper)
        Proxy.newProxyInstance(
            IoTPushTransformRuleMapper.class.getClassLoader(),
            new Class<?>[] {IoTPushTransformRuleMapper.class},
            (proxy, method, args) -> {
              if ("selectPublishedByAppUniqueId".equals(method.getName())) {
                return rules;
              }
              if ("toString".equals(method.getName())) {
                return "TestIoTPushTransformRuleMapper";
              }
              Class<?> returnType = method.getReturnType();
              if (!returnType.isPrimitive()) {
                return null;
              }
              if (boolean.class.equals(returnType)) {
                return false;
              }
              return 0;
            });
  }
}
