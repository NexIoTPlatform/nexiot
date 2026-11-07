package cn.universal.admin.video.alarm;

import static org.junit.jupiter.api.Assertions.*;

import cn.hutool.json.JSONUtil;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VideoAlarmEventDataTest {
  @Test
  void tiandyKeepsCoreBusinessAndOneImageReferenceWithoutRawOrDebugCopies() {
    String url = "http://gateway/sdk/images/20261016/snapshot.jpg";
    Map<String, Object> image = new LinkedHashMap<>(Map.of(
        "url", url, "localUrl", url, "snapshotUrl", url, "imageUrl", url,
        "downloadUrl", url + "?download=true", "localKey", "20261016/snapshot.jpg",
        "localStatus", "stored", "gatewayStored", true, "provider", "s3"));
    image.putAll(Map.of("width", 1280, "height", 720, "size", 162102,
        "sha256", "image-integrity", "imageSource", "alarm_snapshot", "contentType", "image/jpeg",
        "customCaption", "北门"));
    Map<String, Object> data = new LinkedHashMap<>(Map.of(
        "instanceKey", "tiandy", "platformType", "tiandy_sdk", "deviceId", "camera",
        "eventId", "trace", "channelId", "1", "cameraEventType", "motion",
        "alarmState", "start", "alarmTime", 1792131225132L, "images", List.of(image)));
    data.putAll(Map.of("alarmType", "motion", "alarmName", "移动侦测", "cameraEventName", "移动侦测",
        "vendorEventCode", "0", "vendorEventCodeHex", "0x0", "command", 0,
        "nativeAdditionalParameters", List.of(0, 0, 0, 0), "snapshotUrl", url,
        "sdkEventFamily", "alarm", "channelNo", 1));
    data.put("rawData", new LinkedHashMap<>(data));
    data.put("analysis", Map.of("objectCount", 2, "confidence", 0.92));
    var compact = VideoAlarmEventData.compact(data, "移动侦测");
    assertEquals("motion", compact.get("cameraEventType"));
    assertEquals("0", compact.get("vendorEventCode"));
    assertEquals("alarm", compact.get("sdkEventFamily"));
    assertEquals(1792131225132L, compact.get("alarmTime"));
    assertEquals(Map.of("objectCount", 2, "confidence", 0.92), compact.get("analysis"));
    assertFalse(compact.containsKey("rawData"));
    assertFalse(compact.containsKey("channelNo"));
    for (String field : VideoAlarmEventData.REDUNDANT_FIELDS) assertFalse(compact.containsKey(field), field);
    var stored = (Map<?, ?>) ((List<?>) compact.get("images")).getFirst();
    assertEquals(url, stored.get("url"));
    assertEquals("stored", stored.get("status"));
    assertEquals(1280, stored.get("width"));
    assertEquals("image-integrity", stored.get("sha256"));
    assertEquals("北门", stored.get("customCaption"));
    assertFalse(stored.containsKey("snapshotUrl"));
    assertFalse(stored.containsKey("localUrl"));
    assertFalse(compact.containsKey("snapshotUrl"));
    assertTrue(bytes(compact) < bytes(data) / 2, "日志、推送和Flow真正使用精简后的数据");
    assertTrue(data.containsKey("rawData"), "不修改原始回调对象");
    assertEquals(compact, VideoAlarmEventData.compact(compact, "移动侦测"));
  }

  @Test
  void wvpKeepsNativeCodesAndRealLocationWithoutRepeatedLabels() {
    var data = new LinkedHashMap<String, Object>(Map.of(
        "cameraEventType", "intrusion", "alarmType", "intrusion", "alarmName", "区域入侵",
        "alarmMethod", "5", "gbAlarmType", "6", "vendorEventCode", "5:6",
        "alarmLevel", "1", "alarmPriority", "1", "longitude", 0d, "latitude", 0d));
    data.put("description", "进入警戒区域");
    data.put("alarmDescription", "进入警戒区域");
    var compact = VideoAlarmEventData.compact(data, "区域入侵");
    assertEquals("5:6", compact.get("vendorEventCode"));
    assertEquals("6", compact.get("gbAlarmType"));
    assertEquals("1", compact.get("alarmLevel"));
    assertEquals(0d, compact.get("longitude"));
    assertEquals("进入警戒区域", compact.get("description"));
    assertFalse(compact.containsKey("alarmDescription"));
  }

  @Test
  void standaloneSnapshotIsConvertedAndDistinctRecordingEndpointsStayAvailable() {
    var data = new LinkedHashMap<String, Object>(Map.of(
        "snapshotUrl", "https://media/image.jpg", "gatewayStored", true,
        "evidence", List.of(Map.of("kind", "video", "url", "https://media/clip.mp4",
            "mp4Url", "https://media/clip.mp4", "hlsUrl", "https://media/clip.m3u8",
            "downloadUrl", "https://media/download/clip", "status", "ready"))));
    var compact = VideoAlarmEventData.compact(data, "告警");
    assertEquals(List.of(Map.of("url", "https://media/image.jpg", "status", "stored")), compact.get("images"));
    var recording = (Map<?, ?>) ((List<?>) compact.get("evidence")).getFirst();
    assertFalse(recording.containsKey("mp4Url"));
    assertEquals("https://media/clip.m3u8", recording.get("hlsUrl"));
    assertEquals("https://media/download/clip", recording.get("downloadUrl"));
  }

  @Test
  void readableCloudImageSurvivesFailedLocalReplicaAndDuplicateReferencesAreMerged() {
    var image = Map.<String, Object>of("url", "https://media/image.jpg", "cloudUrl", "https://media/image.jpg",
        "localStatus", "failed", "gatewayStored", true, "s3Status", "stored", "cloudReadStatus", "ready");
    var compact = VideoAlarmEventData.compact(Map.of("images", List.of(image,
        Map.of("url", "https://media/image.jpg", "width", 1280, "sizeBytes", 100))), "告警");
    assertEquals(List.of(Map.of("url", "https://media/image.jpg", "status", "stored", "width", 1280, "size", 100)),
        compact.get("images"));
    assertEquals(1, cn.universal.core.alarm.VideoAlarmPayload.evidence(compact).size());
  }

  private static int bytes(Object value) {
    return JSONUtil.toJsonStr(value).getBytes(StandardCharsets.UTF_8).length;
  }
}
