package cn.universal.mqtt.protocol.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.core.downlink.DownlinkContext;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.mqtt.protocol.entity.MQTTDownRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MQTT 子设备长期下发消息体测试")
class MQTTDownRequestConverterEnvelopeTest {

  @Test
  @DisplayName("网关代理子设备下发应构造长期 envelope")
  void shouldBuildGatewaySubDeviceEnvelope() throws Exception {
    MQTTDownRequestConverter converter = new MQTTDownRequestConverter();

    Map<String, Object> function = new HashMap<>();
    function.put("messageType", "FUNCTIONS");
    function.put("function", "setDeviceDesiredProperties");

    UnifiedDownlinkCommand command =
        UnifiedDownlinkCommand.builder().cmd(DownCmd.DEV_FUNCTION).msgId("msg-1").function(function).build();

    JSONObject mergedData = JSONUtil.parseObj("{\"TempUp\":13,\"commandId\":\"setDeviceDesiredProperties:21\"}");

    IoTProduct gatewayProduct = IoTProduct.builder().productKey("gatewayPk").build();
    IoTDeviceDTO gatewayDevice = IoTDeviceDTO.builder().deviceId("gatewayDk").build();
    IoTProduct subProduct = IoTProduct.builder().productKey("subPk").transportProtocol("opcua").build();
    IoTDeviceDTO subDevice =
        IoTDeviceDTO.builder()
            .deviceId("subDk")
            .configuration("{\"nodeId\":\"ns=2;s=TempUp\"}")
            .build();

    Method method =
        MQTTDownRequestConverter.class.getDeclaredMethod(
            "buildGatewaySubDeviceEnvelope",
            UnifiedDownlinkCommand.class,
            DownlinkContext.class,
            IoTProduct.class,
            IoTDeviceDTO.class,
            IoTProduct.class,
            IoTDeviceDTO.class,
            JSONObject.class);
    method.setAccessible(true);

    String payload =
        (String)
            method.invoke(
                converter,
                command,
                new DownlinkContext<>(),
                gatewayProduct,
                gatewayDevice,
                subProduct,
                subDevice,
                mergedData);

    JSONObject result = JSONUtil.parseObj(payload);
    assertEquals("nexiot.downlink.subdevice.v2", result.getStr("schema"));
    assertEquals("FUNCTIONS", result.getStr("messageType"));
    assertEquals("setDeviceDesiredProperties", result.getStr("function"));
    assertEquals("setDeviceDesiredProperties:21", result.getStr("commandId"));
    assertEquals("subDevice", result.getByPath("target.type", String.class));
    assertEquals("gatewayPk", result.getByPath("target.gateway.productKey", String.class));
    assertEquals("gatewayDk", result.getByPath("target.gateway.deviceId", String.class));
    assertEquals("subPk", result.getByPath("target.subDevice.productKey", String.class));
    assertEquals("subDk", result.getByPath("target.subDevice.deviceId", String.class));
    assertEquals("opcua", result.getByPath("target.route.protocol", String.class));
    assertEquals("ns=2;s=TempUp", result.getByPath("target.route.locator.nodeId", String.class));
    assertEquals(13, result.getByPath("data.TempUp", Integer.class));
  }

  @Test
  @DisplayName("编解码成功时不应封装长期 envelope")
  void shouldSkipEnvelopeWhenPayloadSourceIsCodec() throws Exception {
    MQTTDownRequestConverter converter = new MQTTDownRequestConverter();
    MQTTDownRequest request = new MQTTDownRequest();
    request.setIsGatewayProxy(true);
    request.setPayloadSource(IoTConstant.PAYLOAD_SOURCE_CODEC);

    Method method =
        MQTTDownRequestConverter.class.getDeclaredMethod(
            "shouldWrapGatewaySubDeviceEnvelope", MQTTDownRequest.class);
    method.setAccessible(true);

    boolean shouldWrap = (boolean) method.invoke(converter, request);

    assertFalse(shouldWrap);
  }

  @Test
  @DisplayName("子设备产品配置应回填专用下发主题")
  void shouldApplySubDeviceDownTopicFromSubProductConfig() throws Exception {
    MQTTDownRequestConverter converter = new MQTTDownRequestConverter();
    MQTTDownRequest request = new MQTTDownRequest();
    IoTProduct subProduct =
        IoTProduct.builder()
            .productKey("subPk")
            .configuration(
                "{\"subDeviceDownTopic\":\"$thing/down/${gatewayPk}/${gatewayDk}/sub/${subPk}/${subDk}\"}")
            .build();

    Method method =
        MQTTDownRequestConverter.class.getDeclaredMethod(
            "applySubDeviceTopicConfig", MQTTDownRequest.class, IoTProduct.class);
    method.setAccessible(true);
    method.invoke(converter, request, subProduct);

    assertEquals(
        "$thing/down/${gatewayPk}/${gatewayDk}/sub/${subPk}/${subDk}",
        request.getSubDeviceDownTopic());
  }

  @Test
  @DisplayName("raw-fallback 时应封装长期 envelope")
  void shouldWrapEnvelopeWhenPayloadSourceIsRawFallback() throws Exception {
    MQTTDownRequestConverter converter = new MQTTDownRequestConverter();
    MQTTDownRequest request = new MQTTDownRequest();
    request.setIsGatewayProxy(true);
    request.setPayloadSource(IoTConstant.PAYLOAD_SOURCE_RAW_FALLBACK);

    Method method =
        MQTTDownRequestConverter.class.getDeclaredMethod(
            "shouldWrapGatewaySubDeviceEnvelope", MQTTDownRequest.class);
    method.setAccessible(true);

    boolean shouldWrap = (boolean) method.invoke(converter, request);

    assertTrue(shouldWrap);
  }
}
