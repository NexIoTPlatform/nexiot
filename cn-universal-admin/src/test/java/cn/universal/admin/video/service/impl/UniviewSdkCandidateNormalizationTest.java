package cn.universal.admin.video.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import cn.universal.persistence.entity.VideoPlatformInstance;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class UniviewSdkCandidateNormalizationTest {
  private final VideoPlatformInstanceServiceImpl service =
      mock(VideoPlatformInstanceServiceImpl.class, CALLS_REAL_METHODS);

  @BeforeEach void setup() {
    ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
  }

  @ParameterizedTest
  @CsvSource({"uniview_sdk,uniview,Uniview,80,sdk_only", "hik_sdk,hik,Hikvision,8000,rtsp_first",
      "hik_isup,hik,Hikvision,8000,rtsp_first", "dahua_sdk,dahua,Dahua,37777,rtsp_first",
      "tiandy_sdk,tiandy,Tiandy,3000,rtsp_first"})
  void probeAndImportUseTheSelectedVendorsDefaults(String platform, String vendor,
      String manufacturer, int sdkPort, String streamMode) {
    var instance = instance(platform);
    Map<String, Object> source = Map.of("deviceId", "camera", "ip", "192.0.2.10");
    Map<String, Object> display = ReflectionTestUtils.invokeMethod(
        service, "normalizeSdkCandidateForDisplay", instance, source);
    Map<String, Object> imported = normalizeForSync(instance, source);
    for (Map<String, Object> device : List.of(display, imported)) {
      assertEquals(vendor, device.get("vendor"));
      assertEquals(manufacturer, device.get("manufacturer"));
      assertEquals(sdkPort, device.get("devicePort"));
      assertEquals(manufacturer, channels(device).getFirst().get("manufacturer"));
    }
    assertEquals(streamMode, imported.get("streamMode"));
  }

  @Test void importingAnOlderUniviewProbeCorrectsTheFormerHikvisionDefault() {
    Map<String, Object> source = Map.of("deviceId", "nvr", "vendor", "uniview",
        "manufacturer", "Hikvision", "channels", List.of(
            Map.of("channelId", "42", "manufacturer", "Hikvision")));
    Map<String, Object> result = normalizeForSync(instance("uniview_sdk"), source);
    assertEquals("Uniview", result.get("manufacturer"));
    assertEquals("Uniview", channels(result).getFirst().get("manufacturer"));
    assertEquals("sdk_only", result.get("streamMode"));
  }

  @Test void anExplicitManufacturerNameIsPreserved() {
    Map<String, Object> result = normalizeForSync(instance("uniview_sdk"),
        Map.of("deviceId", "camera", "manufacturer", "UNV OEM"));
    assertEquals("UNV OEM", result.get("manufacturer"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"rtsp_first", "sdk_fallback", "sdk_only"})
  void importingUniviewRespectsAnExplicitPlatformStrategy(String strategy) {
    var instance = instance("uniview_sdk");
    instance.setAuth("{\"streamMode\":\"" + strategy + "\"}");
    assertEquals(strategy, normalizeForSync(instance, Map.of("deviceId", "camera")).get("streamMode"));
  }

  private Map<String, Object> normalizeForSync(VideoPlatformInstance instance, Map<String, Object> source) {
    return ReflectionTestUtils.invokeMethod(service, "normalizeSdkCandidateForSync", instance, source, Map.of());
  }

  private VideoPlatformInstance instance(String platformType) {
    return VideoPlatformInstance.builder().instanceKey("sdk-defaults").platformType(platformType).build();
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> channels(Map<String, Object> device) {
    return (List<Map<String, Object>>) device.get("channelList");
  }
}
