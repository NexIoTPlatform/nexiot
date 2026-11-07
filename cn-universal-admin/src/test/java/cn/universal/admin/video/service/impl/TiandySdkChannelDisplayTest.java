package cn.universal.admin.video.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import cn.universal.persistence.entity.VideoPlatformInstance;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TiandySdkChannelDisplayTest {

  private final VideoPlatformInstanceServiceImpl service =
      mock(VideoPlatformInstanceServiceImpl.class, CALLS_REAL_METHODS);

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
  }

  @Test
  void successfulEmptyTiandyCatalogRemainsEmptyInDiscoveryAndSyncCandidates() {
    Map<String, Object> source =
        Map.of(
            "deviceId", "nvr",
            "online", true,
            "channels", List.of(),
            "attributes", Map.of("channelCatalogAuthoritative", true));

    Map<String, Object> display = normalize("tiandy_sdk", source);
    Map<String, Object> sync = ReflectionTestUtils.invokeMethod(
        service, "normalizeSdkCandidateForSync", instance("tiandy_sdk"), source, Map.of());

    for (Map<String, Object> result : List.of(display, sync)) {
      assertTrue(channels(result).isEmpty());
      assertEquals(List.of(), result.get("channels"));
      assertEquals(0, result.get("channelCount"));
      assertEquals(true, result.get("channelCatalogAuthoritative"));
    }
  }

  @Test
  void emptyLegacyTiandyCatalogKeepsItsDefaultChannel() {
    Map<String, Object> result =
        normalize("tiandy_sdk", Map.of("deviceId", "legacy", "channels", List.of()));

    assertEquals(1, channels(result).size());
  }

  @Test
  void authoritativeMarkerDoesNotChangeAnotherVendorsDefaultChannelBehavior() {
    Map<String, Object> result = normalize(
        "onvif",
        Map.of("deviceId", "camera", "channels", List.of(), "channelCatalogAuthoritative", true));

    assertEquals(1, channels(result).size());
  }

  @Test
  void explicitTiandyStatesDoNotInheritOnlineFromTheDeviceOrAnOlderChannelStatus() {
    Map<String, Object> source = Map.of(
            "deviceId", "nvr",
            "online", true,
            "channels",
            List.of(channel("1", "online"), channel("2", "offline"), channel("3", "unknown")));
    Map<String, Object> sync = ReflectionTestUtils.invokeMethod(service,
        "normalizeSdkCandidateForSync", instance("tiandy_sdk"), source, Map.of());

    for (Map<String, Object> result : List.of(normalize("tiandy_sdk", source), sync)) {
      assertEquals(
          List.of("online", "offline", "unknown"),
          channels(result).stream().map(ch -> ch.get("channelStatus")).toList());
      assertEquals(
          List.of("online", "offline", "unknown"),
          channels(result).stream().map(ch -> ch.get("status")).toList());
    }
  }

  @Test
  void channelsWithoutSdkStatesAndOtherVendorsKeepTheirExistingStatusRules() {
    Map<String, Object> legacy = normalize(
        "tiandy_sdk",
        Map.of("deviceId", "nvr", "channels", List.of(
            Map.of("channelId", "1"), Map.of("channelId", "2", "online", false))));
    assertEquals(
        List.of("online", "offline"),
        channels(legacy).stream().map(ch -> ch.get("channelStatus")).toList());

    Map<String, Object> otherVendor = normalize(
        "onvif", Map.of("deviceId", "nvr", "channels", List.of(channel("1", "unknown"))));
    assertEquals("online", channels(otherVendor).getFirst().get("channelStatus"));
  }

  @Test
  void sparseTiandyChannelNumbersStayBoundToTheirOriginalSdkSlots() {
    Map<String, Object> result = normalize(
        "tiandy_sdk",
        Map.of("deviceId", "nvr", "channels", List.of(
            Map.of("channelId", "2", "channelNo", 2),
            Map.of("channelId", "5_sub", "channelNo", 5))));

    assertEquals(List.of("2", "5_sub"), channels(result).stream().map(ch -> ch.get("channelId")).toList());
    assertEquals(List.of(2, 5), channels(result).stream().map(ch -> ch.get("channelSeq")).toList());
    assertEquals(List.of("2", "5"), channels(result).stream().map(ch -> ch.get("rtspChannel")).toList());
  }

  @Test
  void authoritativeCatalogCountsPhysicalChannelsRatherThanMainAndSubStreams() {
    Map<String, Object> result = normalize("tiandy_sdk", Map.of("deviceId", "nvr",
        "channelCatalogAuthoritative", true, "channels", List.of(
            Map.of("channelId", "2", "channelNo", 2),
            Map.of("channelId", "2_sub", "channelNo", 2),
            Map.of("channelId", "5", "channelNo", 5),
            Map.of("channelId", "5_sub", "channelNo", 5))));

    assertEquals(2, result.get("channelCount"));
    assertEquals(4, channels(result).size());
  }

  @Test
  void zeroBasedNativeSdkChannelsAndOneBasedRtspRoutesSurviveDiscoveryAndSync() {
    Map<String, Object> source = Map.of("deviceId", "nvr", "online", true,
        "channels", List.of(
            nativeChannel("1", 0, 1, "main"), nativeChannel("1_sub", 0, 1, "sub"),
            nativeChannel("5", 4, 5, "main"), nativeChannel("5_sub", 4, 5, "sub")),
        "attributes", Map.of("channelCatalogAuthoritative", true, "loginChannelCount", 8));
    Map<String, Object> sync = ReflectionTestUtils.invokeMethod(service,
        "normalizeSdkCandidateForSync", instance("tiandy_sdk"), source, Map.of());

    for (Map<String, Object> result : List.of(normalize("tiandy_sdk", source), sync)) {
      List<Map<String, Object>> catalog = channels(result);
      assertEquals(List.of("1", "1_sub", "5", "5_sub"),
          catalog.stream().map(channel -> channel.get("channelId")).toList());
      assertEquals(List.of(0, 0, 4, 4),
          catalog.stream().map(channel -> channel.get("sdkChannel")).toList());
      assertEquals(List.of(1, 1, 5, 5),
          catalog.stream().map(channel -> channel.get("channelNo")).toList());
      assertEquals(List.of(0, 0, 4, 4),
          catalog.stream().map(channel -> channel.get("nativeChannelNo")).toList());
      assertEquals(List.of("1", "1", "5", "5"),
          catalog.stream().map(channel -> channel.get("rtspChannel")).toList());
      assertEquals(List.of(1, 1, 5, 5),
          catalog.stream().map(channel -> channel.get("channelSeq")).toList());
      assertEquals(List.of("main", "sub", "main", "sub"),
          catalog.stream().map(channel -> channel.get("streamType")).toList());
      assertTrue(catalog.stream().allMatch(channel -> Boolean.TRUE.equals(channel.get("configured"))));
      assertTrue(catalog.stream().allMatch(channel -> "unknown".equals(channel.get("channelStatus"))));
      assertEquals(8, result.get("loginChannelCount"));
      assertEquals(2, result.get("channelCount"));
    }
  }

  private Map<String, Object> nativeChannel(String id, int sdkChannel, int rtspChannel, String streamType) {
    return Map.of("channelId", id, "channelNo", rtspChannel,
        "attributes", Map.of("sdkChannel", sdkChannel, "rtspChannel", rtspChannel,
            "channelSeq", rtspChannel, "streamType", streamType, "onlineState", "unknown",
            "configured", true, "nativeChannelNo", sdkChannel));
  }

  private Map<String, Object> normalize(String platformType, Map<String, Object> source) {
    return ReflectionTestUtils.invokeMethod(
        service, "normalizeSdkCandidateForDisplay", instance(platformType), source);
  }

  private VideoPlatformInstance instance(String platformType) {
    return VideoPlatformInstance.builder().instanceKey("sdk-display").platformType(platformType).build();
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> channels(Map<String, Object> device) {
    return (List<Map<String, Object>>) device.get("channelList");
  }

  private Map<String, Object> channel(String id, String onlineState) {
    Map<String, Object> channel = new LinkedHashMap<>();
    channel.put("channelId", id);
    channel.put("online", null);
    channel.put("channelStatus", "online");
    channel.put("attributes", Map.of("onlineState", onlineState));
    return channel;
  }
}
