package cn.universal.core.alarm;

import static org.junit.Assert.*;
import cn.hutool.json.JSONUtil;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class VideoCloudPayloadTest {
  @Test public void preservesRealIoTFieldsAndMonitorStateWithoutCredentials() {
    var payload = VideoAlarmPayload.normalize("IMOULIFE", "vendor_336500", Map.of(
        "alarmId", "1161936682740483200", "channelId", "0", "token", "callback-secret",
        "utcTime", "20261009T160435", "content", Map.of("event", "336500",
            "outputData", Map.of("confidence", 92, "object", Map.of("type", "human")),
            "monitor", Map.of("action", 1, "verifierV2", "private-verifier"))));
    assertEquals("1161936682740483200", payload.get("eventId"));
    assertEquals("336500", payload.get("vendorEventCode"));
    assertEquals("ACTIVE", payload.get("sourceState"));
    assertEquals(92, ((Map<?, ?>) payload.get("outputData")).get("confidence"));
    assertFalse(payload.containsKey("confidence"));
    assertEquals(java.time.Instant.parse("2026-10-09T16:04:35Z").toEpochMilli(), payload.get("occurredAt"));
    assertFalse(payload.containsKey("token"));
    assertFalse(payload.toString().contains("private-verifier"));
    assertFalse(payload.toString().contains("callback-secret"));
  }

  @Test public void extractsIoTPropertyValuesAndNestedJsonStringPictures() {
    assertEquals(Map.of("111100", 1, "disk", Map.of("free", 30)), VideoAlarmPayload.properties(
        Map.of("content", Map.of("properties", Map.of("111100", 1, "disk", Map.of("free", 30))))));
    var refs = VideoAlarmPayload.evidence(Map.of("content", Map.of("outputData", Map.of(
        "picUrlArr", "[\"https://camera.example/a.jpg\",\"https://camera.example/b.jpg\"]"))));
    assertEquals(2, refs.size());
  }

  @Test public void archivedEvidenceReplacesExpiringSourceForAlarmRules() {
    var refs = VideoAlarmPayload.evidence(Map.of("picUrlArr", List.of("https://vendor.example/expired"),
        "evidence", List.of(Map.of("status", "ready", "url", "https://ours.example/image",
            "objectKey", "snapshot/cloud.jpg", "ossId", "123", "id", "vendor-picture-id", "provider", "minio"))));
    assertEquals(1, refs.size());
    assertEquals("snapshot/cloud.jpg", refs.getFirst().objectKey);
    assertEquals("", refs.getFirst().id);
    assertEquals("https://ours.example/image", refs.getFirst().url);
  }

  @Test public void compactSdkImagesRemainSourceEvidenceWithoutDuplicatedSnapshotUrl() {
    var refs = VideoAlarmPayload.evidence(Map.of("cameraEventType", "motion", "images", List.of(
        Map.of("url", "https://gateway.example/one.jpg", "status", "stored", "contentType", "image/jpeg"),
        Map.of("url", "https://vendor.example/two.jpg", "status", "ready",
            "objectKey", "untrusted/vendor.jpg", "provider", "s3", "id", "vendor-picture-id"),
        Map.of("url", "https://vendor.example/three.jpg"),
        Map.of("url", "https://gateway.example/waiting.jpg", "status", "waiting"),
        Map.of("url", "file:///tmp/picture.jpg", "status", "stored"))));
    assertEquals(3, refs.size());
    assertEquals("image/jpeg", refs.getFirst().mimeType);
    for (var ref : refs) {
      assertEquals("", ref.objectKey);
      assertEquals("", ref.provider);
      assertEquals("", ref.id);
      assertEquals("ORIGINAL", ref.kind);
    }
  }

  @Test public void imagesDeduplicateLegacyUrlsAndRespectArchivedEvidencePriority() {
    var images = List.of(Map.of("url", "https://gateway.example/one.jpg", "status", "stored"));
    assertEquals(1, VideoAlarmPayload.evidence(Map.of("images", images,
        "snapshotUrl", "https://gateway.example/one.jpg")).size());
    var refs = VideoAlarmPayload.evidence(Map.of("images", images, "evidence", List.of(
        Map.of("url", "https://ours.example/archived.jpg", "objectKey", "alarm/archived.jpg",
            "provider", "minio", "status", "ready"))));
    assertEquals(1, refs.size());
    assertEquals("alarm/archived.jpg", refs.getFirst().objectKey);
    assertEquals("https://ours.example/archived.jpg", refs.getFirst().url);
  }

  @Test public void normalizingVendorPicturesAndOutputDoesNotRestoreNestedCredentials() {
    var output = new LinkedHashMap<String, Object>(Map.of("confidence", 92,
        "object", Map.of("type", "human", "password", "output-password"),
        "detections", List.of(Map.of("count", 1, "token", "detection-token"))));
    var picture = new LinkedHashMap<String, Object>(Map.of("url", "https://camera.example/alarm.jpg?sign=required",
        "sign", "picture-sign", "meta", Map.of("width", 640, "accessToken", "picture-token")));
    var source = Map.<String, Object>of("content", Map.of("event", "336500", "outputData", output),
        "pictureList", List.of(picture), "imageUrl", Map.of("url", "https://camera.example/second.jpg",
            "password", "image-password"), "crypt", Map.of("mode", 2, "secret", "crypt-secret"));

    var payload = VideoAlarmPayload.normalize("IMOULIFE", "vendor_336500", source);

    assertNoCredentials(payload, "output-password", "detection-token", "picture-sign", "picture-token",
        "image-password", "crypt-secret");
    assertEquals(Map.of("confidence", 92, "object", Map.of("type", "human"),
        "detections", List.of(Map.of("count", 1))), payload.get("outputData"));
    assertEquals(List.of(Map.of("url", "https://camera.example/alarm.jpg?sign=required"),
        Map.of("url", "https://camera.example/second.jpg")), payload.get("images"));
    assertFalse(payload.containsKey("pictureList"));
    assertFalse(payload.containsKey("imageUrl"));
    assertEquals(Map.of("mode", 2), payload.get("imageEncryption"));
    assertEquals("https://camera.example/alarm.jpg?sign=required", VideoAlarmPayload.evidence(payload).getFirst().url);
    assertTrue(picture.containsKey("sign"));
    assertTrue(((Map<?, ?>) output.get("object")).containsKey("password"));
    output.put("password", "mutated-output-password");
    picture.put("token", "mutated-picture-token");
    assertNoCredentials(payload, "mutated-output-password", "mutated-picture-token");
  }

  @Test public void structuredIdentityAndMonitorFieldsAreSanitizedBeforeStringConversion() {
    var payload = VideoAlarmPayload.normalize("IMOULIFE", "vendor_336500", Map.of(
        "alarmId", Map.of("value", "alarm-1", "password", "id-password"),
        "channelId", Map.of("value", "0", "sign", "channel-sign"),
        "instanceKey", List.of(Map.of("value", "instance-1", "token", "instance-token")),
        "content", Map.of("event", Map.of("value", "336500", "secret", "event-secret"),
            "monitor", Map.of("action", Map.of("value", "start", "authorization", "monitor-auth")))));
    assertNoCredentials(payload, "id-password", "channel-sign", "instance-token", "event-secret", "monitor-auth");
    assertTrue(payload.get("eventId").toString().contains("alarm-1"));
    assertTrue(payload.get("channelId").toString().contains("0"));
    assertTrue(payload.get("vendorEventCode").toString().contains("336500"));
    assertEquals("UNKNOWN", payload.get("sourceState"));

    var fallback = VideoAlarmPayload.normalize("EZVIZ", "alarm", Map.of(
        "alarmState", Map.of("value", "start", "password", "state-password"),
        "alarmType", Map.of("value", "motion", "signature", "type-signature")));
    assertNoCredentials(fallback, "state-password", "type-signature");
    assertTrue(fallback.get("vendorEventCode").toString().contains("motion"));
  }

  @Test public void objectArrayPicturesAndVendorDataAreRecursivelySanitized() {
    var pictures = new Object[]{Map.of("url", "https://camera.example/array.jpg", "password", "array-password")};
    var values = new Object[]{Map.of("value", 7, "sign", "array-sign"),
        List.of(Map.of("count", 1, "token", "array-token"))};
    var payload = VideoAlarmPayload.normalize("DAHUA_CLOUD", "human", Map.of(
        "pictureList", pictures, "data", Map.of("values", values)));
    assertNoCredentials(payload, "array-password", "array-sign", "array-token");
    assertEquals(List.of(Map.of("url", "https://camera.example/array.jpg")), payload.get("pictureList"));
    assertEquals(List.of(Map.of("value", 7), List.of(Map.of("count", 1))), payload.get("values"));
    assertEquals("https://camera.example/array.jpg", VideoAlarmPayload.evidence(payload).getFirst().url);
    assertTrue(((Map<?, ?>) pictures[0]).containsKey("password"));
  }

  @Test public void imoulifeCallbackKeepsBusinessFieldsAndOnlyOneSignedPictureSource() {
    String url = "https://example.imoulife.com/Alarm/example.jpg?Signature=example";
    var source = JSONUtil.parseObj("""
        {"alarmId":"example-alarm","channelId":"1","content":{"outputData":{},
         "localTime":"20261016T170156","monitor":{"eventId":"27006","picRatio":{"width":640,"height":352},
         "vendor":["COS","OSS","BOS","US3","OBS","TOS"],"channel":1,"cname":"固定镜头",
         "linkchan":[0],"action":1,"vendorV3":["OSS","OBS","COS","US3","TOS"]},
         "sessionId":"session-example","event":"336700"},"did":"example-device","dname":"双目相机",
         "msgType":"iotEvent","pid":"example-pid","time":"20261016T170156","utcTime":"20261016T090153","appId":"example-app"}
        """);
    source.set("picUrlArr", List.of(url)).set("thumbUrl", url);
    var data = VideoAlarmPayload.normalize("IMOULIFE", "vendor_336700", source);
    assertEquals("336700", data.get("vendorEventCode"));
    assertEquals("example-alarm", data.get("eventId"));
    assertEquals("ACTIVE", data.get("sourceState"));
    assertEquals("1", data.get("alarmState"));
    assertEquals(Map.of("eventId", "27006", "picRatio", Map.of("width", 640, "height", 352),
        "cname", "固定镜头", "linkchan", List.of(0)), data.get("monitor"));
    assertEquals(List.of(Map.of("url", url)), data.get("images"));
    for (String key : List.of("content", "rawData", "data", "outputData", "sessionId", "picUrlArr", "thumbUrl", "did", "dname"))
      assertFalse(key, data.containsKey(key));
    assertTrue(source.containsKey("content"));
    assertEquals(java.time.Instant.parse("2026-10-16T09:01:53Z").toEpochMilli(), data.get("occurredAt"));
    // Match the previous projection so this representative byte comparison remains reviewable.
    var legacy = new LinkedHashMap<String, Object>(source);
    legacy.put("rawData", VideoAlarmPayload.safeData(source));
    for (String key : List.of("platformType", "cameraEventType", "eventId", "channelId", "sourceState", "alarmState", "occurredAt", "vendorEventCode"))
      legacy.put(key, data.get(key));
    legacy.put("instanceKey", "");
    legacy.put("standardEventType", "vendor_336700");
    legacy.put("outputData", Map.of());
    int before = JSONUtil.toJsonStr(legacy).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    int after = JSONUtil.toJsonStr(data).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    System.out.printf("IMOULIFE representative payload: %d -> %d bytes%n", before, after);
    assertTrue(after < before / 2);
  }

  @Test public void archivedImoulifeSourceRemovalKeepsFailedBatchRetrySources() {
    var data = new LinkedHashMap<String, Object>();
    data.put("snapshotUrl", "https://ours.example/image");
    data.put("images", List.of(Map.of("url", "https://vendor.example/image")));
    data.put("evidence", List.of(Map.of("status", "encrypted", "ossId", "1")));
    VideoAlarmPayload.compactImoulifeEvidence(data);
    assertFalse(data.containsKey("snapshotUrl"));
    assertFalse(data.containsKey("images"));
    data.put("images", List.of(Map.of("url", "https://vendor.example/failed")));
    data.put("evidence", List.of(Map.of("status", "failed")));
    VideoAlarmPayload.compactImoulifeEvidence(data);
    assertTrue(data.containsKey("images"));
    assertEquals("ENCRYPTED_IMAGE", VideoAlarmPayload.evidence(Map.of("images", List.of(
        Map.of("url", "https://vendor.example/encrypted", "kind", "ENCRYPTED_IMAGE")))).getFirst().kind);
  }

  private static void assertNoCredentials(Map<String, Object> payload, String... credentials) {
    var serialized = JSONUtil.toJsonStr(payload);
    for (String credential : credentials) {
      assertFalse("Credential leaked into serialized alarm payload", serialized.contains(credential));
      assertFalse("Credential leaked into alarm field text", payload.toString().contains(credential));
    }
  }
}
