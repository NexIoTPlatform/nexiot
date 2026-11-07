package cn.universal.dm.downlink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.core.downlink.DownlinkContext;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.dm.device.shadow.DeviceShadowWrapper;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class DownlinkFunctionFillParamsInterceptorTest {

  private static final String PRODUCT_KEY = "test-product";
  private static final String DEVICE_ID = "test-device";
  private static final String FUNCTION_ID = "playAudio";
  private static final String COMMAND_ID = "test-command";

  private DownlinkFunctionFillParamsInterceptor interceptor;
  private IoTProductDeviceService productService;
  private IoTDeviceShadowService shadowService;
  private IoTDeviceService deviceService;
  private ValueOperations<String, String> values;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws Exception {
    interceptor = new DownlinkFunctionFillParamsInterceptor();
    productService = mock(IoTProductDeviceService.class);
    shadowService = mock(IoTDeviceShadowService.class);
    deviceService = mock(IoTDeviceService.class);
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    setField("ioTProductDeviceService", productService);
    setField("ioTDeviceShadowService", shadowService);
    setField("ioTDeviceService", deviceService);
    setField("stringRedisTemplate", redis);
  }

  @Test
  void shouldReadDeviceConfigurationInsteadOfProductConfiguration() throws Exception {
    productWithInputs(input("audioUrl"));
    when(deviceService.selectDevInstanceBO(PRODUCT_KEY, DEVICE_ID))
        .thenReturn(
            IoTDeviceDTO.builder()
                .configuration("{\"audioUrl\":\"https://product.example/announcement.wav\"}")
                .devConfiguration(
                    "{\"audioUrl\":\"https://device.example/announcement.wav\",\"unrelated\":\"value\"}")
                .build());
    DownlinkContext<?> context = context(FUNCTION_ID, Map.of());

    assertTrue(interceptor.preHandle(context));

    assertEquals("https://device.example/announcement.wav", data(context).get("audioUrl"));
    assertFalse(data(context).containsKey("unrelated"));
    assertEquals(1, context.<Integer>getAttribute("filledCount"));
    assertEquals(COMMAND_ID, context.<String>getAttribute("commandId"));
    verify(values)
        .set(
            "iot:cmd:" + PRODUCT_KEY + ":" + DEVICE_ID + ":" + FUNCTION_ID,
            COMMAND_ID,
            300L,
            TimeUnit.SECONDS);
  }

  @Test
  void shouldFillNullAndBlankInputsButKeepExplicitValues() throws Exception {
    productWithInputs(
        input("audioUrl"), input("callerId"), input("volume"), input("loop"), input("message"));
    shadow(
        Map.of(
            "audioUrl", "https://cache.example/announcement.wav",
            "callerId", "1001",
            "volume", 50,
            "loop", true,
            "message", "cached"),
        Map.of());
    Map<String, Object> requested = new HashMap<>();
    requested.put("audioUrl", null);
    requested.put("callerId", " \t ");
    requested.put("volume", 0);
    requested.put("loop", false);
    requested.put("message", "user supplied");
    DownlinkContext<?> context = context(FUNCTION_ID, requested);

    interceptor.preHandle(context);

    assertEquals("https://cache.example/announcement.wav", data(context).get("audioUrl"));
    assertEquals("1001", data(context).get("callerId"));
    assertEquals(0, data(context).get("volume"));
    assertEquals(false, data(context).get("loop"));
    assertEquals("user supplied", data(context).get("message"));
    assertEquals(2, context.<Integer>getAttribute("filledCount"));
    verifyNoInteractions(deviceService);
  }

  @Test
  void shouldPreferReportedThenDesiredThenDeviceConfigurationThenMetadataDefault()
      throws Exception {
    productWithInputs(
        input("reported").set("defaultValue", "default"),
        input("desired").set("defaultValue", "default"),
        input("device").set("defaultValue", "default"),
        input("fallback").set("valueType", Map.of("type", "string", "default", "default")));
    shadow(Map.of("reported", "reported"), Map.of("reported", "desired", "desired", "desired"));
    when(deviceService.selectDevInstanceBO(PRODUCT_KEY, DEVICE_ID))
        .thenReturn(
            IoTDeviceDTO.builder()
                .devConfiguration("{\"reported\":\"device\",\"desired\":\"device\",\"device\":\"device\"}")
                .build());
    DownlinkContext<?> context = context(FUNCTION_ID, Map.of());

    interceptor.preHandle(context);

    assertEquals("reported", data(context).get("reported"));
    assertEquals("desired", data(context).get("desired"));
    assertEquals("device", data(context).get("device"));
    assertEquals("default", data(context).get("fallback"));
    assertEquals(4, context.<Integer>getAttribute("filledCount"));
  }

  @Test
  void shouldReadSupportedDefaultLocationsFromRawFunctionMetadata() throws Exception {
    productWithInputs(
        input("topDefaultValue").set("defaultValue", "https://oss.example/alarm.wav"),
        input("topDefault").set("default", "managed"),
        input("expandedDefault").set("expands", Map.of("defaultValue", "expanded")),
        input("typedDefaultValue").set("valueType", Map.of("type", "int", "defaultValue", 0)),
        input("typedDefault").set("valueType", Map.of("type", "boolean", "default", false)));
    DownlinkContext<?> context = context(FUNCTION_ID, Map.of());

    interceptor.preHandle(context);

    assertEquals("https://oss.example/alarm.wav", data(context).get("topDefaultValue"));
    assertEquals("managed", data(context).get("topDefault"));
    assertEquals("expanded", data(context).get("expandedDefault"));
    assertEquals(0, data(context).get("typedDefaultValue"));
    assertEquals(false, data(context).get("typedDefault"));
    assertEquals(5, context.<Integer>getAttribute("filledCount"));
  }

  @Test
  void shouldSkipBlankCachedAndConfiguredValuesBeforeUsingDefault() throws Exception {
    productWithInputs(input("audioUrl").set("defaultValue", "https://oss.example/alarm.wav"));
    shadow(Map.of("audioUrl", " "), Map.of());
    when(deviceService.selectDevInstanceBO(PRODUCT_KEY, DEVICE_ID))
        .thenReturn(IoTDeviceDTO.builder().devConfiguration("{\"audioUrl\":\"\"}").build());
    DownlinkContext<?> context = context(FUNCTION_ID, Map.of("audioUrl", ""));

    interceptor.preHandle(context);

    assertEquals("https://oss.example/alarm.wav", data(context).get("audioUrl"));
    assertEquals(1, context.<Integer>getAttribute("filledCount"));
  }

  @Test
  void shouldFillJsonNullFromDesiredWhenReportedIsBlank() throws Exception {
    productWithInputs(input("audioUrl"));
    shadow(Map.of("audioUrl", " "), Map.of("audioUrl", "https://oss.example/alarm.wav"));
    DownlinkContext<?> context = new DownlinkContext<>();
    context.setCommand(
        UnifiedDownlinkCommand.fromString(
            """
            {
              "productKey": "test-product",
              "deviceId": "test-device",
              "cmd": "DEV_FUNCTION",
              "function": {
                "function": "playAudio",
                "data": {"audioUrl": null, "commandId": "test-command"}
              }
            }
            """));

    interceptor.preHandle(context);

    assertEquals("https://oss.example/alarm.wav", data(context).get("audioUrl"));
    verifyNoInteractions(deviceService);
  }

  @Test
  void shouldUseOnlyAllowlistedStandardFieldsAfterDeviceConfiguration() throws Exception {
    productWithInputs(
        input("deviceId"),
        input("iotId"),
        input("productKey"),
        input("deviceName").set("defaultValue", "default name"),
        input("extDeviceId"),
        input("deviceSecret"),
        input("imei"));
    when(deviceService.selectDevInstanceBO(PRODUCT_KEY, DEVICE_ID))
        .thenReturn(
            IoTDeviceDTO.builder()
                .devConfiguration("{\"deviceId\":\"configured-device\"}")
                .deviceId(DEVICE_ID)
                .iotId("test-iot-id")
                .productKey(PRODUCT_KEY)
                .deviceName("Test device")
                .extDeviceId("test-extension")
                .deviceSecret("not-a-function-parameter")
                .build());
    DownlinkContext<?> context = context(FUNCTION_ID, Map.of());

    interceptor.preHandle(context);

    assertEquals("configured-device", data(context).get("deviceId"));
    assertEquals("test-iot-id", data(context).get("iotId"));
    assertEquals(PRODUCT_KEY, data(context).get("productKey"));
    assertEquals("Test device", data(context).get("deviceName"));
    assertEquals("test-extension", data(context).get("extDeviceId"));
    assertFalse(data(context).containsKey("deviceSecret"));
    assertFalse(data(context).containsKey("imei"));
    assertEquals(5, context.<Integer>getAttribute("filledCount"));
  }

  @Test
  void shouldPreferCustomFieldDefaultBeforeFunctionInputDefault() throws Exception {
    IoTProduct product =
        productWithInputs(input("audioUrl").set("defaultValue", "https://function.example/alarm.wav"));
    product.setThirdConfiguration(
        "{\"customField\":[{\"id\":\"audioUrl\",\"defaultValue\":\"https://custom.example/alarm.wav\"}]}");
    DownlinkContext<?> context = context(FUNCTION_ID, Map.of());

    interceptor.preHandle(context);

    assertEquals("https://custom.example/alarm.wav", data(context).get("audioUrl"));
    assertEquals(1, context.<Integer>getAttribute("filledCount"));
  }

  @Test
  void shouldNotFillDisabledParameterButKeepItsExplicitValue() throws Exception {
    productWithInputs(
        input("callId")
            .set("expands", Map.of("autoFill", false))
            .set("defaultValue", "default-call"));
    shadow(Map.of("callId", "ended-call"), Map.of());
    when(deviceService.selectDevInstanceBO(PRODUCT_KEY, DEVICE_ID))
        .thenReturn(IoTDeviceDTO.builder().devConfiguration("{\"callId\":\"configured-call\"}").build());
    DownlinkContext<?> omitted = context(FUNCTION_ID, Map.of());
    DownlinkContext<?> explicit = context(FUNCTION_ID, Map.of("callId", "active-call"));

    interceptor.preHandle(omitted);
    interceptor.preHandle(explicit);

    assertFalse(data(omitted).containsKey("callId"));
    assertEquals("active-call", data(explicit).get("callId"));
  }

  @Test
  void shouldKeepDesiredPropertiesPatchWithoutFillingMissingFields() throws Exception {
    DownlinkContext<?> context =
        context(IoTConstant.SET_DEVICE_DESIRED_PROPERTIES, Map.of("volume", 20));

    interceptor.preHandle(context);

    assertEquals(Map.of("volume", 20, "commandId", COMMAND_ID), data(context));
    assertEquals(true, context.<Boolean>getAttribute("paramsFilled"));
    assertEquals(COMMAND_ID, context.<String>getAttribute("commandId"));
    verifyNoInteractions(productService, shadowService, deviceService);
  }

  private JSONObject input(String id) {
    return new JSONObject().set("id", id).set("valueType", Map.of("type", "string"));
  }

  private IoTProduct productWithInputs(JSONObject... inputs) {
    JSONObject function = new JSONObject().set("id", FUNCTION_ID).set("inputs", List.of(inputs));
    IoTProduct product =
        IoTProduct.builder()
            .productKey(PRODUCT_KEY)
            .metadata(new JSONObject().set("functions", List.of(function)).toString())
            .build();
    when(productService.getProduct(PRODUCT_KEY)).thenReturn(product);
    return product;
  }

  private void shadow(Map<String, Object> reported, Map<String, Object> desired) {
    DeviceShadowWrapper wrapper =
        new DeviceShadowWrapper(
            JSONUtil.parseObj(Map.of("state", Map.of("reported", reported, "desired", desired))));
    when(shadowService.getDeviceShadowWrapper(PRODUCT_KEY, DEVICE_ID)).thenReturn(wrapper);
  }

  private DownlinkContext<?> context(String functionId, Map<String, Object> requested) {
    Map<String, Object> data = new HashMap<>(requested);
    data.put("commandId", COMMAND_ID);
    Map<String, Object> function = new HashMap<>();
    function.put("function", functionId);
    function.put("data", data);
    DownlinkContext<?> context = new DownlinkContext<>();
    context.setCommand(
        UnifiedDownlinkCommand.builder()
            .productKey(PRODUCT_KEY)
            .deviceId(DEVICE_ID)
            .cmd(DownCmd.DEV_FUNCTION)
            .function(function)
            .build());
    return context;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> data(DownlinkContext<?> context) {
    return (Map<String, Object>) context.getCommand().getFunction().get("data");
  }

  private void setField(String name, Object value) throws Exception {
    Field field = DownlinkFunctionFillParamsInterceptor.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(interceptor, value);
  }
}
