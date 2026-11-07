package cn.universal.dm.video;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.dm.cache.IoTCacheInvalidationEvent;
import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.entity.admin.SysDictType;
import cn.universal.persistence.mapper.admin.SysDictDataMapper;
import cn.universal.persistence.mapper.admin.SysDictTypeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VideoSdkEventCatalogTest {
  @Test void univiewStartAndRecoveryCodesShareBusinessIdentityAndUnknownCodesStayRaw() {
    enabled();
    when(data.selectDictDataByType("video_sdk_event_uniview_sdk")).thenReturn(List.of(
        code(1, "0x00000001", "motion", "motionDetection"),
        code(2, "0x00000002", "motion", "motionDetection"),
        code(3, "0x00000003", "video_lost", "videoLoss"),
        code(4, "0x00000004", "video_lost", "videoLoss")));
    for (int value : List.of(1, 2)) {
      assertEquals("motionDetection", catalog.resolve("uniview_sdk", "unknown",
          Map.of("sdkEventFamily", "ALARM", "vendorEventCode", value)).id());
    }
    assertEquals("videoLoss", catalog.resolve("uniview_sdk", "motion",
        Map.of("vendorEventCode", 4)).id());
    assertEquals("univiewSdkEvent", catalog.resolve("uniview_sdk", "motion",
        Map.of("vendorEventCode", 999)).id());
    assertEquals(2, catalog.events("uniview_sdk").size());
    assertEquals(List.of("0x00000001", "0x00000002"), catalog.subscriptionCatalog("uniview_sdk")
        .stream().filter(event -> "motion".equals(event.get("type"))).findFirst().orElseThrow().get("sdkCodes"));
    assertEquals("sdkDeviceHealth", catalog.resolve("uniview_sdk", "device_health",
        Map.of("sdkEventFamily", "gateway")).id());
  }

  @Test void univiewOfficialSmartAlarmCodesExposeNamedTopicsAndTakePrecedenceOverRawNames() {
    enabled();
    when(data.selectDictDataByType("video_sdk_event_uniview_sdk")).thenReturn(List.of(
        code(1, "0x000003EB", "line_crossing", "lineCrossing").setDictLabel("越界告警"),
        code(2, "0x000003EC", "intrusion", "regionIntrusion").setDictLabel("区域入侵")
            .setRemark("{\"type\":\"intrusion\",\"eventId\":\"regionIntrusion\",\"sdkCode\":\"NETDEV_ALARM_OBJECTS_INSIDE (1004)\",\"aliases\":[\"uniview_alarm_1004\"]}"),
        code(3, "0x000003F7", "region_enter", "regionEntrance"),
        code(4, "0x000003F8", "region_exit", "regionExit")));
    for (var entry : Map.of(1003, "lineCrossing", 1004, "regionIntrusion",
        1015, "regionEntrance", 1016, "regionExit").entrySet()) {
      assertEquals(entry.getValue(), catalog.resolve("uniview_sdk", "motion",
          Map.of("vendorEventCode", entry.getKey())).id());
    }
    assertEquals("regionIntrusion", catalog.resolve("uniview_sdk", "uniview_alarm_1004").id());
    var intrusion = catalog.options("uniview_sdk").stream()
        .filter(option -> "intrusion".equals(option.get("value"))).findFirst().orElseThrow();
    assertEquals("区域入侵", intrusion.get("label"));
    assertEquals("NETDEV_ALARM_OBJECTS_INSIDE (1004)", intrusion.get("sdkCode"));
    assertEquals(List.of("0x000003EC"), intrusion.get("sdkCodes"));
    var subscription = catalog.subscriptionCatalog("uniview_sdk").stream()
        .filter(option -> "intrusion".equals(option.get("type"))).findFirst().orElseThrow();
    assertEquals(List.of("0x000003EC"), subscription.get("sdkCodes"));
    assertEquals(List.of("uniview_alarm_1004"), subscription.get("aliases"));
    assertEquals("univiewSdkEvent", catalog.resolve("uniview_sdk", "intrusion",
        Map.of("vendorEventCode", 1005)).id());
    assertEquals("hikSdkEvent", catalog.resolve("hik_sdk", "intrusion",
        Map.of("vendorEventCode", 1004)).id());
  }

  @Test void disabledUniviewSmartAlarmMappingDoesNotFallBackToItsSemanticName() {
    enabled();
    when(data.selectDictDataByType("video_sdk_event_uniview_sdk")).thenReturn(List.of(
        code(1, "0x000003EC", "intrusion", "regionIntrusion").setStatus("1")));
    var event = catalog.resolve("uniview_sdk", "intrusion", Map.of("vendorEventCode", 1004));
    assertEquals("univiewSdkEvent", event.id());
    assertEquals("vendor_raw", event.type());
    assertFalse(catalog.options("uniview_sdk").stream()
        .anyMatch(option -> "intrusion".equals(option.get("value"))));
  }

  @Test void gbAlarmSubtypesAreScopedByMethodAndShareSdkEventIds() {
    enabled();
    when(data.selectDictDataByType("video_sdk_event_wvp")).thenReturn(List.of(
        code(1, "2", "device_alarm", "deviceAlarm"),
        code(2, "2:2", "device_tamper", "deviceTamper"),
        code(3, "5:2", "motion", "motionDetection"),
        code(4, "5:6", "intrusion", "regionIntrusion"),
        code(5, "5:13", "shelter", "videoTamper"),
        code(6, "6:2", "fan_error", "fanError")));
    assertEquals("deviceAlarm", catalog.resolve("wvp", "cameraAlarm", Map.of("alarmMethod", "2")).id());
    for (var entry : Map.of("2", "deviceTamper", "5", "motionDetection", "6", "fanError").entrySet()) {
      assertEquals(entry.getValue(), catalog.resolve("wvp", "cameraAlarm",
          Map.of("alarmMethod", entry.getKey(), "gbAlarmType", 2)).id());
    }
    assertEquals("regionIntrusion", catalog.resolve("wvp", "cameraAlarm",
        Map.of("alarmMethod", 5, "gbAlarmType", "6")).id());
    assertEquals("videoTamper", catalog.resolve("wvp", "cameraAlarm",
        Map.of("alarmMethod", "5", "gbAlarmType", "13")).id());
    assertEquals("wvpGb28181Event", catalog.resolve("wvp", "cameraAlarm",
        Map.of("alarmMethod", "2/5", "gbAlarmType", "2")).id());
    assertEquals(List.of("5:6"), catalog.options("wvp").stream()
        .filter(e -> "intrusion".equals(e.get("value"))).findFirst().orElseThrow().get("sdkCodes"));
    assertFalse(catalog.modelEvents("wvp").stream().anyMatch(e -> e.id().startsWith("sdk")));
  }

  @Test void unknownDisabledAndAmbiguousGbAlarmsRetainUnresolvedGbEvent() {
    enabled();
    when(data.selectDictDataByType("video_sdk_event_wvp")).thenReturn(List.of(
        code(1, "5:2", "motion", "motionDetection"),
        code(2, "5:6", "intrusion", "regionIntrusion").setStatus("1"),
        code(3, "2", "device_alarm", "deviceAlarm"),
        code(4, "2:2", "device_tamper", "deviceTamper"),
        code(5, "6:1", "disk_error", "diskError"),
        code(6, "1:2", "invalid", "invalidEvent")));
    for (var attrs : List.of(Map.<String, Object>of(), Map.<String, Object>of("alarmMethod", "5"),
        Map.<String, Object>of("alarmMethod", "5", "gbAlarmType", "999"),
        Map.<String, Object>of("alarmMethod", "5", "gbAlarmType", "6"),
        Map.<String, Object>of("alarmMethod", "25", "gbAlarmType", "2"),
        Map.<String, Object>of("alarmMethod", "5", "gbAlarmType", "0"),
        Map.<String, Object>of("alarmMethod", "0", "gbAlarmType", "2"),
        Map.<String, Object>of("alarmMethod", "2", "gbAlarmType", "99"))) {
      var event = catalog.resolve("wvp", "motion", attrs);
      assertEquals("wvpGb28181Event", event.id());
      assertEquals("vendor_raw", event.type());
    }
    // 设备故障编号 6:1 必须是硬盘故障，不能沿用 WVP 旧描述中的人工视频报警。
    assertEquals("diskError", catalog.resolve("wvp", "cameraAlarm",
        Map.of("alarmMethod", "6", "gbAlarmType", "1")).id());
    assertFalse(catalog.events("wvp").stream().anyMatch(e -> e.id().equals("invalidEvent")));
  }

  @Test void emptyTopicsNeverBecomeImplicitAllAndSubscriptionCatalogUsesMachineCodes() {
    enabled();
    var motion = code(1, "0x4000:3", "motion", "motionDetection");
    motion.setRemark("{\"type\":\"motion\",\"eventId\":\"motionDetection\",\"aliases\":[\"motion_alias\"]}");
    when(data.selectDictDataByType("video_sdk_event_hik_sdk")).thenReturn(List.of(motion));
    for (String platform : List.of("hik_sdk", "hik_isup", "dahua_sdk", "tiandy_sdk")) {
      assertEquals(List.of(),catalog.subscriptionTopics(platform,List.of()));
      assertEquals(List.of(),catalog.subscriptionTopics(platform,null));
    }
    var direct = catalog.subscriptionCatalog("hik_sdk").stream().filter(row -> "motion".equals(row.get("type"))).findFirst().orElseThrow();
    assertEquals(List.of("0x00004000:0x03"),direct.get("sdkCodes"));
    assertEquals(List.of("motion_alias"),direct.get("aliases"));
    var isup = catalog.subscriptionCatalog("hik_isup").stream().filter(row -> "motion".equals(row.get("type"))).findFirst().orElseThrow();
    assertEquals(List.of("isup:0x00000001:0x00000008","isup:0x00000006:0x00000008","isapi:VMD"),isup.get("sdkCodes"));
  }

  @Test void isupUsesStandardBusinessEventRatherThanHcNetSdkNumericCode() {
    enabled();
    when(data.selectDictDataByType("video_sdk_event_hik_sdk"))
        .thenReturn(List.of(code(1, "0x00004000:0x03", "motion", "motionDetection"),
            code(2, "0x00004000:0x02", "shelter", "videoTamper")));
    assertEquals("motionDetection", catalog.resolve("hik_isup", "motion",
        Map.of("sdkEventFamily", "ISUP", "vendorEventCode", 13)).id());
    assertEquals("videoTamper", catalog.resolve("hik_isup", "shelter",
        Map.of("vendorEventCode", 1, "vendorAlarmType", 7)).id());
    assertEquals(2, catalog.events("hik_isup").size());
  }

  @Test void isupUsesItsOwnSubtypeAndIsapiSemanticWithoutDirectSdkCodeFallback() {
    enabled();
    when(data.selectDictDataByType("video_sdk_event_hik_sdk"))
        .thenReturn(List.of(code(1, "0x4000:3", "motion", "motionDetection"),
            code(2, "0x4000:6", "shelter", "videoTamper")));
    assertEquals("motionDetection", catalog.resolve("hik_isup", "unknown",
        Map.of("vendorEventCode", 13, "cameraEventType", "VMD")).id());
    assertEquals("motionDetection", catalog.resolve("hik_isup", "unknown",
        Map.of("vendorEventCode", 1, "vendorAlarmType", 8)).id());
    assertEquals("videoTamper", catalog.resolve("hik_isup", "motion",
        Map.of("vendorEventCode", 6, "vendorAlarmType", 7)).id());
    assertEquals("hikSdkEvent", catalog.resolve("hik_isup", "unknown",
        Map.of("vendorEventCode", 13)).id());
    assertEquals("hikSdkEvent", catalog.resolve("hik_isup", "motion",
        Map.of("vendorEventCode", "0x4000", "vendorAlarmType", 3)).id());
    assertEquals("hikSdkEvent", catalog.resolve("hik_isup", "motion",
        Map.of("vendorEventCode", 1, "vendorAlarmType", 3)).id());
    assertEquals("hikSdkEvent", catalog.resolve("hik_isup", "unknown",
        Map.of("vendorEventCode", 13, "cameraEventType", "unimplementedType")).id());
    // HCNetSDK 子类型3继续权威匹配移动侦测，不受 ISUP 子类型8影响。
    assertEquals("motionDetection", catalog.resolve("hik_sdk", "wrong",
        Map.of("vendorEventCode", "0x4000", "vendorAlarmType", 3)).id());
  }

  @Test void isupOptionsDescribeImplementedIsupCodesAndExcludeUnsupportedDirectEvents() {
    enabled();
    when(data.selectDictDataByType("video_sdk_event_hik_sdk"))
        .thenReturn(List.of(code(1, "0x4000:3", "motion", "motionDetection")
                .setRemark("{\"type\":\"motion\",\"eventId\":\"motionDetection\",\"sdkCode\":\"COMM_ALARM_V30\"}"),
            code(2, "0x4000:4", "disk_unformatted", "diskUnformatted")));
    var options = catalog.options("hik_isup");
    var motion = options.stream().filter(option -> "motion".equals(option.get("value")))
        .findFirst().orElseThrow();
    assertEquals(List.of("EHOME_ALARM(1): dwAlarmType=8",
        "EHOME_ALARM_NOTICE_PICURL(6): dwAlarmType=8", "EHOME_ISAPI_ALARM(13): VMD"),
        motion.get("sdkCodes"));
    assertFalse(options.toString().contains("COMM_"));
    assertFalse(options.toString().contains("0x00004000"));
    assertFalse(options.stream().anyMatch(option -> "disk_unformatted".equals(option.get("value"))));
  }
  private final SysDictDataMapper data = mock(SysDictDataMapper.class);
  private final SysDictTypeMapper types = mock(SysDictTypeMapper.class);
  private final VideoSdkEventCatalog catalog =
      new VideoSdkEventCatalog(data, types, new ObjectMapper());

  private void enabled() {
    var type = new SysDictType();
    type.setStatus("0");
    when(types.selectOne(any())).thenReturn(type);
  }

  private SysDictData entry(long code, String value, String config) {
    return new SysDictData()
        .setDictCode(code)
        .setDictSort(1)
        .setDictLabel(value)
        .setDictValue(value)
        .setStatus("0")
        .setRemark(config);
  }

  @Test
  void tiandyNumericZeroMapsMotionAndUnknownCodesKeepVendorIdentity() {
    enabled();
    when(data.selectDictDataByType("video_sdk_event_tiandy_sdk"))
        .thenReturn(List.of(
            code(1, "0x00000000", "motion", "motionDetection"),
            code(2, "0x00000002", "video_lost", "videoLoss"),
            code(3, "0x00000003", "alarm_input", "alarmInput"),
            code(4, "0x00000005", "shelter", "videoTamper").setStatus("1"),
            code(5, "0x00000063", "reserved", "tiandySdkEvent")));
    assertEquals("motionDetection", catalog.resolve("tiandy_sdk", "unexpected",
        Map.of("vendorEventCode", 0, "sdkEventFamily", "alarm")).id());
    assertEquals("videoLoss", catalog.resolve("tiandy_sdk", "motion",
        Map.of("vendorEventCode", 2)).id());
    assertEquals("tiandySdkEvent", catalog.resolve("tiandy_sdk", "motion",
        Map.of("vendorEventCode", 6)).id());
    assertEquals("tiandySdkEvent", catalog.resolve("tiandy_sdk", "shelter",
        Map.of("vendorEventCode", 5)).id());
    assertEquals("tiandySdkEvent", catalog.resolve("tiandy_sdk", "reserved").id());
    assertEquals("sdkDeviceHealth", catalog.resolve("tiandy_sdk", "device_health",
        Map.of("sdkEventFamily", "gateway")).id());
    assertEquals(3, catalog.events("tiandy_sdk").size());
    assertFalse(catalog.subscriptionTopics("tiandy_sdk", List.of("cameraAlarm"))
        .contains("__sdk_ivs_optional"));
  }

  @Test
  void dynamicAliasesCacheInvalidationAndVendorIsolation() {
    enabled();
    String dict = "video_sdk_event_dahua_sdk";
    when(data.selectDictDataByType(dict))
        .thenReturn(
            List.of(
                entry(
                    1,
                    "custom_alarm",
                    "{\"eventId\":\"customAlarm\",\"aliases\":[\"dahua_command_999\"]}")));
    when(data.selectDictDataByType("video_sdk_event_hik_sdk")).thenReturn(List.of());
    assertEquals("customAlarm", catalog.resolve("dahua_sdk", "dahua_command_999").id());
    assertEquals("customAlarm", catalog.resolve("dahua_sdk", "custom_alarm").id());
    assertEquals("hikSdkEvent", catalog.resolve("hik_sdk", "custom_alarm").id());
    assertEquals("custom_alarm", catalog.options("dahua_sdk").getFirst().get("value"));
    verify(data, times(1)).selectDictDataByType(dict);
    when(data.selectDictDataByType(dict)).thenReturn(List.of());
    catalog.onCacheInvalidation(
        new IoTCacheInvalidationEvent(
            "dict-update",
            null,
            null,
            Set.of(),
            Map.of("selectDictDataByType", Set.of(dict)),
            Set.of()));
    assertEquals("dahuaSdkEvent", catalog.resolve("dahua_sdk", "custom_alarm").id());
    verify(data, times(2)).selectDictDataByType(dict);
  }

  @Test
  void malformedDisabledAndDuplicateRowsDoNotBreakOtherEvents() {
    enabled();
    when(data.selectDictDataByType(anyString()))
        .thenReturn(
            List.of(
                entry(1, "bad_json", "{"),
                entry(2, "motion", "{\"eventId\":\"motionDetection\"}"),
                entry(3, "duplicate", "{\"eventId\":\"motionDetection\"}"),
                entry(4, "disabled", "{\"eventId\":\"disabled\"}").setStatus("1"),
                entry(5, "vendor_raw", "{\"eventId\":\"raw\"}")));
    assertEquals(1, catalog.events("hik_sdk").size());
    assertEquals("motionDetection", catalog.resolve("hik_sdk", "motion").id());
    assertEquals("hikSdkEvent", catalog.resolve("hik_sdk", "disabled").id());
    assertEquals("hikSdkEvent", catalog.resolve("hik_sdk", null).id());
  }

  @Test
  void disabledTypeDoesNotQueryRowsAndDatabaseFailureIsRetryable() {
    var type = new SysDictType();
    type.setStatus("1");
    when(types.selectOne(any())).thenReturn(type);
    assertTrue(catalog.events("hik_sdk").isEmpty());
    verifyNoInteractions(data);
    enabled();
    when(data.selectDictDataByType(anyString()))
        .thenThrow(new IllegalStateException("db unavailable"))
        .thenReturn(List.of(entry(1, "custom", "{\"eventId\":\"customEvent\"}")));
    assertThrows(IllegalStateException.class, () -> catalog.events("dahua_sdk"));
    assertEquals("customEvent", catalog.resolve("dahua_sdk", "custom").id());
  }

  private SysDictData code(long row, String value, String type, String eventId) {
    return entry(row, value, "{\"type\":\"" + type + "\",\"eventId\":\"" + eventId + "\"}")
        .setDictLabel(type);
  }

  @Test
  void rawHexAndDecimalCodesAreAuthoritativeAndCustomCodesNeedNoJavaMapping() {
    enabled();
    when(data.selectDictDataByType(anyString()))
        .thenReturn(
            List.of(
                code(1, "0x2135", "temperature", "temperatureAlarm"),
                code(2, "0x4000:0x03", "motion", "motionDetection"),
                code(3, "0x4007:3", "motion", "motionDetection"),
                code(4, "0x4000:0x02", "lost", "videoLoss"),
                code(5, "ivs:0x00000002", "line", "lineCrossing"),
                code(6, "0x80000001", "custom", "customEvent")));
    assertEquals(
        "temperatureAlarm",
        catalog
            .resolve("dahua_sdk", "dahua_command_8501", Map.of("vendorEventCodeHex", "0X00002135"))
            .id());
    assertEquals("temperatureAlarm", catalog.resolve("dahua_sdk", "0x2135").id());
    assertEquals(
        "motionDetection",
        catalog
            .resolve(
                "hik_sdk", "wrong_name", Map.of("vendorEventCode", 16384, "vendorAlarmType", "3"))
            .id());
    assertEquals(
        "videoLoss",
        catalog
            .resolve(
                "hik_sdk", "motion", Map.of("vendorEventCode", "0x4000", "vendorAlarmType", "0x02"))
            .id());
    assertEquals(
        "hikSdkEvent",
        catalog.resolve("hik_sdk", "motion", Map.of("vendorEventCode", "0x4000")).id());
    assertEquals(
        "hikSdkEvent",
        catalog.resolve("hik_sdk", "motion", Map.of("vendorEventCode", "0x9999")).id());
    assertEquals(
        "dahuaSdkEvent",
        catalog
            .resolve("dahua_sdk", "line", Map.of("vendorEventCode", 2, "sdkEventFamily", "ALARM"))
            .id());
    assertEquals(
        "lineCrossing",
        catalog
            .resolve("dahua_sdk", "unknown", Map.of("vendorEventCode", 2, "sdkEventFamily", "IVS"))
            .id());
    assertEquals(
        "customEvent",
        catalog.resolve("dahua_sdk", "unknown", Map.of("vendorEventCode", -2147483647)).id());
    assertEquals(5, catalog.events("dahua_sdk").size());
    assertTrue(catalog.subscriptionTopics("dahua_sdk", List.of("line")).contains("__sdk_ivs"));
    assertFalse(
        catalog.subscriptionTopics("dahua_sdk", List.of("temperature")).contains("__sdk_ivs"));
    assertTrue(
        catalog
            .subscriptionTopics("dahua_sdk", List.of("cameraAlarm"))
            .contains("__sdk_ivs_optional"));
    assertEquals(
        List.of("0x00004000:0x03", "0x00004007:0x03"),
        catalog.options("hik_sdk").stream()
            .filter(option -> "motion".equals(option.get("value")))
            .findFirst()
            .orElseThrow()
            .get("sdkCodes"));
  }

  @Test
  void normalizedDuplicateCodesAndOutOfRangeValuesAreRejected() {
    enabled();
    when(data.selectDictDataByType(anyString()))
        .thenReturn(
            List.of(
                code(1, "0x2102", "motion", "motionDetection"),
                code(2, "8450", "wrong", "wrongEvent"),
                code(3, "0x100000000", "overflow", "overflowEvent")));
    assertEquals(1, catalog.events("dahua_sdk").size());
    assertEquals(
        "motionDetection", catalog.resolve("dahua_sdk", null, Map.of("command", 8450)).id());
    assertEquals(
        "dahuaSdkEvent", catalog.resolve("dahua_sdk", "motion", Map.of("command", "1.5")).id());
  }
  @Test void gatewayEvidenceAndHealthHaveSeparateModelEvents() {
    for (String platform : List.of("hik_sdk", "dahua_sdk")) {
      assertEquals("sdkEvidenceReady", catalog.resolve(platform, "evidence_ready", Map.of("sdkEventFamily", "gateway")).id());
      assertEquals("sdkDeviceHealth", catalog.resolve(platform, "device_health", Map.of("sdkEventFamily", "gateway")).id());
      assertTrue(catalog.modelEvents(platform).stream().anyMatch(e -> e.id().equals("sdkEvidenceReady")));
    }
  }

}
