package cn.universal.dm.device.service.wrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.hutool.json.JSONObject;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.domain.R;
import cn.universal.core.message.DownRequest;
import cn.universal.core.metadata.AbstractPropertyMetadata;
import cn.universal.core.metadata.DeviceMetadata;
import cn.universal.core.metadata.PropertyMetadata;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IoTDeviceDownFunctionInterceptTest {

  @Test
  void shouldSkipDeviceSourceWhenUpdatingDeviceTag() throws Exception {
    IoTDeviceDownFunctionIntercept intercept = new IoTDeviceDownFunctionIntercept();
    setField(intercept, "ioTProductDeviceService", stubMetadataService(buildMetadata()));

    JSONObject input = new JSONObject();
    input.set("totalEnergy", 12.5D);
    input.set("status", "online");
    input.set("voltage", 220);
    input.set("unknownField", "keep");

    JSONObject result = invokeFilter(intercept, "pk-test", input);

    assertEquals(12.5D, result.getDouble("totalEnergy"));
    assertFalse(result.containsKey("status"));
    assertEquals(220, result.getInt("voltage"));
    assertEquals("keep", result.getStr("unknownField"));
    assertTrue(result.size() == 3);
  }

  @Test
  void shouldReuseDesiredShadowServiceWithOnlyWritablePropertiesAndDeviceIdentity()
      throws Exception {
    IoTDeviceShadowService shadowService = mock(IoTDeviceShadowService.class);
    IoTDeviceDownFunctionIntercept intercept = desiredInterceptor(shadowService);
    JSONObject patch =
        new JSONObject().set("audioUrl", "https://oss.example/alarm.wav").set("commandId", "cmd-1");
    IoTDevice device =
        IoTDevice.builder()
            .productKey("pk-test")
            .deviceId("1001")
            .extDeviceId("sip-1001")
            .deviceName("值班电话")
            .tenantId("tenant")
            .build();

    R<?> result =
        intercept.beforeFunctionOrConfigDown(
            IoTProduct.builder().productKey("pk-test").build(),
            device,
            desiredRequest(patch, IoTConstant.PAYLOAD_SOURCE_SHADOW_ONLY));

    assertTrue(result.isSuccess());
    ArgumentCaptor<IoTDeviceDTO> captured = ArgumentCaptor.forClass(IoTDeviceDTO.class);
    verify(shadowService)
        .doDesiredShadow(captured.capture(), eq(Map.of("audioUrl", "https://oss.example/alarm.wav")), anyLong());
    assertEquals("pk-test1001", captured.getValue().getIotId());
    assertEquals("pk-test", captured.getValue().getProductKey());
    assertEquals("1001", captured.getValue().getDeviceId());
    assertEquals("sip-1001", captured.getValue().getExtDeviceId());
    assertEquals("值班电话", captured.getValue().getDeviceName());
    assertEquals("tenant", captured.getValue().getTenantId());
  }

  @Test
  void shouldReturnErrorForShadowOnlyWhenDesiredWriteFails() throws Exception {
    IoTDeviceShadowService shadowService = mock(IoTDeviceShadowService.class);
    IoTDeviceMapper deviceMapper = mock(IoTDeviceMapper.class);
    doThrow(new IllegalStateException("cache unavailable"))
        .when(shadowService)
        .doDesiredShadow(any(), anyMap(), anyLong());
    IoTDeviceDownFunctionIntercept intercept = desiredInterceptor(shadowService, deviceMapper);

    R<?> result =
        intercept.beforeFunctionOrConfigDown(
            IoTProduct.builder().productKey("pk-test").build(),
            IoTDevice.builder().iotId("iot-1").productKey("pk-test").deviceId("1001").build(),
            desiredRequest(
                new JSONObject().set("audioUrl", "https://oss.example/alarm.wav"),
                IoTConstant.PAYLOAD_SOURCE_SHADOW_ONLY));

    assertTrue(result.isError());
    assertEquals("期望值保存失败，请稍后重试", result.getMsg());
    verifyNoInteractions(deviceMapper);
  }

  @Test
  void shouldPreserveLegacyContinuationWhenNonShadowOnlyDesiredWriteFails() throws Exception {
    IoTDeviceShadowService shadowService = mock(IoTDeviceShadowService.class);
    IoTDeviceMapper deviceMapper = mock(IoTDeviceMapper.class);
    doThrow(new IllegalStateException("cache unavailable"))
        .when(shadowService)
        .doDesiredShadow(any(), anyMap(), anyLong());
    IoTDeviceDownFunctionIntercept intercept = desiredInterceptor(shadowService, deviceMapper);

    R<?> result =
        intercept.beforeFunctionOrConfigDown(
            IoTProduct.builder().productKey("pk-test").build(),
            IoTDevice.builder().iotId("iot-1").productKey("pk-test").deviceId("1001").build(),
            desiredRequest(new JSONObject().set("audioUrl", "https://oss.example/alarm.wav"), "codec"));

    assertNull(result);
    verifyNoInteractions(deviceMapper);
  }

  private IoTDeviceDownFunctionIntercept desiredInterceptor(IoTDeviceShadowService shadowService)
      throws Exception {
    return desiredInterceptor(shadowService, mock(IoTDeviceMapper.class));
  }

  private IoTDeviceDownFunctionIntercept desiredInterceptor(
      IoTDeviceShadowService shadowService, IoTDeviceMapper deviceMapper) throws Exception {
    IoTDeviceDownFunctionIntercept intercept = new IoTDeviceDownFunctionIntercept();
    PropertyMetadata audioUrl = new PropertyMetadata();
    audioUrl.setId("audioUrl");
    audioUrl.setSource("extended");
    audioUrl.setMode("rw");
    DeviceMetadata metadata = new DeviceMetadata();
    metadata.setPropertiesMap(Map.of("audioUrl", audioUrl));
    setField(intercept, "ioTProductDeviceService", stubMetadataService(metadata));
    setField(intercept, "ioTDeviceShadowService", shadowService);
    setField(intercept, "ioTDeviceMapper", deviceMapper);
    return intercept;
  }

  private DownRequest desiredRequest(JSONObject patch, String payloadSource) {
    DownRequest request = new DownRequest();
    request.setFunction(Map.of("function", IoTConstant.SET_DEVICE_DESIRED_PROPERTIES, "data", patch));
    request.setPayloadSource(payloadSource);
    return request;
  }

  private DeviceMetadata buildMetadata() {
    PropertyMetadata extendedProperty = new PropertyMetadata();
    extendedProperty.setId("totalEnergy");
    extendedProperty.setSource("extended");

    PropertyMetadata deviceProperty = new PropertyMetadata();
    deviceProperty.setId("status");
    deviceProperty.setSource("device");

    PropertyMetadata defaultProperty = new PropertyMetadata();
    defaultProperty.setId("voltage");

    Map<String, AbstractPropertyMetadata> properties = new LinkedHashMap<>();
    properties.put(extendedProperty.getId(), extendedProperty);
    properties.put(deviceProperty.getId(), deviceProperty);
    properties.put(defaultProperty.getId(), defaultProperty);

    DeviceMetadata metadata = new DeviceMetadata();
    metadata.setPropertiesMap(properties);
    return metadata;
  }

  private IoTProductDeviceService stubMetadataService(DeviceMetadata metadata) {
    return new IoTProductDeviceService() {
      @Override
      public DeviceMetadata getDeviceMetadata(String productKey) {
        return metadata;
      }
    };
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private JSONObject invokeFilter(
      IoTDeviceDownFunctionIntercept intercept, String productKey, JSONObject input)
      throws Exception {
    Method method =
        IoTDeviceDownFunctionIntercept.class.getDeclaredMethod(
            "filterExtendedPropertiesForDeviceTag", String.class, JSONObject.class);
    method.setAccessible(true);
    return (JSONObject) method.invoke(intercept, productKey, input);
  }
}
