package cn.universal.core.alarm;

import cn.hutool.json.JSONUtil;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 视频来源的公共证据字段；业务事件标识由各协议物模型决定，不推测未知厂商事件码。 */
public final class VideoAlarmPayload {
  /** Imou callback transport fields have no business meaning after canonical projection. */
  public static final Set<String> IMOULIFE_TRANSPORT_FIELDS = Set.of(
      "rawData", "raw", "sourcePayload", "content", "data", "params", "properties",
      "alarmId", "messageId", "id", "did", "dname", "deviceName", "pid", "appId", "msgType", "eventType",
      "sessionId", "event", "alarmType", "standardEventType", "cameraEventName",
      "channel", "channelNo", "cid", "utcTime", "localTime", "time", "timestamp", "messageTime", "occurTime", "alarmTime",
      "action", "status", "crypt", "pictureList", "picUrls", "picUrlArr", "picUrlArray", "picUrl",
      "pictureUrl", "imageUrl", "snapshotUrl", "thumbUrl", "clipUrl", "videoUrl");

  private VideoAlarmPayload() {}

  /** 仅统一已知同义事件；保留未知厂商编码，避免把所有事件误归为入侵。 */
  public static String canonicalEvent(String event) {
    return switch (Objects.toString(event, "").toLowerCase(Locale.ROOT)) {
      case "motion", "motiondetect", "motiondetection" -> "motionDetection";
      case "line_crossing", "linecrossing", "linedetection" -> "lineCrossing";
      case "region_intrusion", "regionintrusion", "fielddetection" -> "regionIntrusion";
      case "human", "humandetection", "persondetection" -> "human";
      case "face", "facedetection" -> "face";
      case "sound", "sounddetection", "audioexception" -> "soundDetection";
      default -> Objects.toString(event, "");
    };
  }

  /** 萤石官方告警类型表：https://open.ys7.com/help/981 。 */
  public static String ezvizEvent(String type) {
    return switch (Objects.toString(type, "").toLowerCase(Locale.ROOT)) {
      case "pir", "infrared", "human", "person", "10000", "10010" -> "human";
      case "motiondetect", "motion", "10002" -> "motionDetection";
      case "linedetection", "10013" -> "line_crossing";
      case "fielddetection", "10014" -> "region_intrusion";
      case "facedetection", "10015" -> "face";
      case "audioexception", "10022" -> "soundDetection";
      case "" -> "alarm";
      default -> "vendor_" + type.replaceAll("[^a-zA-Z0-9_]", "_");
    };
  }

  public static Map<String, Object> normalize(String provider, String eventType, Map<String, ?> source) {
    // Later field copies and string conversions must never read the original callback objects.
    source = safeData(source);
    Map<String, Object> result = new LinkedHashMap<>(source);
    boolean imoulife = "IMOULIFE".equalsIgnoreCase(provider);
    if (!imoulife) result.put("rawData", safeData(source));
    if (!imoulife) properties(source).forEach(result::putIfAbsent);
    result.put("platformType", provider);
    result.put("cameraEventType", eventType);
    result.put("standardEventType", canonicalEvent(eventType));
    result.put("eventId", first(source, "eventId", "messageId", "alarmId", "id"));
    result.put("channelId", first(source, "channelId", "channelNo", "channel", "cid"));
    result.put("instanceKey", first(source, "instanceKey"));
    String action = first(source, "alarmState", "action", "status").toLowerCase(Locale.ROOT);
    if (action.isBlank() && source.get("content") instanceof Map<?, ?> content
        && content.get("monitor") instanceof Map<?, ?> monitor) {
      action = Objects.toString(monitor.get("action"), "");
    }
    String state = switch (action) {
      case "stop", "end", "recovered", "recover", "inactive", "0" -> "RECOVERED";
      case "start", "active", "trigger", "1" -> "ACTIVE";
      case "pulse", "", "instant" -> "NOT_APPLICABLE";
      default -> "UNKNOWN";
    };
    result.put("sourceState", state);
    result.put("alarmState", action);
    long timestamp = timestamp(source.get("utcTime"), ZoneOffset.UTC);
    if (timestamp == 0) for (String key : List.of("alarmTime", "occurTime", "time", "localTime", "timestamp", "messageTime")) {
      timestamp = timestamp(source.get(key), ZoneId.of("Asia/Shanghai"));
      if (timestamp > 0) break;
    }
    result.put("occurredAt", timestamp);
    for (String key : List.of("pictureList", "picUrls", "picUrlArr", "picUrlArray", "picUrl", "pictureUrl", "imageUrl", "snapshotUrl", "thumbUrl", "clipUrl", "videoUrl"))
      if (source.get(key) != null) result.put(key, source.get(key));
    if (source.get("crypt") != null) result.put("imageEncryption", source.get("crypt"));
    Object content = source.get("content");
    if (content instanceof Map<?, ?> map) {
      if (map.get("event") != null) result.put("vendorEventCode", map.get("event").toString());
      if (map.get("outputData") instanceof Map<?, ?> eventOutput) result.put("outputData", eventOutput);
    }
    if (!result.containsKey("vendorEventCode")) result.put("vendorEventCode", first(source, "alarmType", "msgType", "eventType"));
    return imoulife ? compactImoulife(result) : result;
  }

  /** Keep each official output value and picture source once, before logging or publishing. */
  private static Map<String, Object> compactImoulife(Map<String, Object> source) {
    Map<String, Object> result = new LinkedHashMap<>();
    mergeImoulifeFields(source, result, 0);
    if (!(result.get("occurredAt") instanceof Number time) || time.longValue() == 0) {
      for (String field : List.of("utcTime", "alarmTime", "occurTime", "time", "localTime", "timestamp", "messageTime")) {
        long timestamp = timestamp(result.get(field), "utcTime".equals(field) ? ZoneOffset.UTC : ZoneId.of("Asia/Shanghai"));
        if (timestamp > 0) { result.put("occurredAt", timestamp); break; }
      }
    }
    for (String field : IMOULIFE_TRANSPORT_FIELDS) result.remove(field);
    if (result.get("monitor") instanceof Map<?, ?> monitor) {
      Map<String, Object> business = new LinkedHashMap<>(safeData(monitor));
      for (String field : List.of("vendor", "vendorV3", "sessionId", "action")) business.remove(field);
      if (Objects.toString(business.get("channel"), "").equals(Objects.toString(result.get("channelId"), "")))
        business.remove("channel");
      if (business.isEmpty()) result.remove("monitor"); else result.put("monitor", business);
    }
    if (result.get("outputData") instanceof Map<?, ?> output && output.isEmpty()) result.remove("outputData");
    // The callback aliases may contain the same long signed URL many times.
    List<Map<String, Object>> images = new ArrayList<>();
    for (var ref : evidence(source)) {
      Map<String, Object> image = new LinkedHashMap<>();
      image.put("url", ref.url);
      if (!ref.id.isBlank()) image.put("id", ref.id);
      if (!"ORIGINAL".equals(ref.kind)) image.put("kind", ref.kind);
      if (!ref.mimeType.isBlank()) image.put("contentType", ref.mimeType);
      images.add(image);
    }
    if (!images.isEmpty()) result.put("images", images); else result.remove("images");
    return result;
  }

  private static void mergeImoulifeFields(Map<?, ?> source, Map<String, Object> result, int depth) {
    if (depth > 5) return;
    source.forEach((key, value) -> {
      String name = Objects.toString(key, "");
      if (!Set.of("rawData", "raw", "sourcePayload", "content", "data", "params", "properties").contains(name))
        if (!result.containsKey(name)) result.put(name, value);
    });
    for (String field : List.of("properties", "data", "params", "content")) {
      if (source.get(field) instanceof Map<?, ?> nested) mergeImoulifeFields(nested, result, depth + 1);
    }
  }

  /** Archived bytes support retries without retaining every expiring source URL. */
  public static void compactImoulifeEvidence(Map<String, Object> data) {
    if (data == null) return;
    data.remove("snapshotUrl");
    if (!(data.get("evidence") instanceof Collection<?> evidence) || evidence.isEmpty()) return;
    boolean archived = evidence.stream().allMatch(value -> value instanceof Map<?, ?> entry
        && (entry.get("ossId") != null || entry.get("original") instanceof Map<?, ?> original && original.get("ossId") != null));
    // Preserve the original ordering for mixed failed/successful batch retries.
    if (archived) data.remove("images");
  }

  /** Preserve vendor fields for rule conditions, excluding callback credentials at every level. */
  public static Map<String, Object> safeData(Map<?, ?> source) {
    Map<String, Object> safe = new LinkedHashMap<>();
    if (source == null) return safe;
    source.forEach((key, value) -> {
      String name = Objects.toString(key, "");
      String lower = name.toLowerCase(Locale.ROOT);
      if (Set.of("token", "accesstoken", "accesskey", "secretaccesskey", "secret", "sign",
          "signature", "checksum", "verifier", "verifierv2", "password", "sourcepayload",
          "picturesecret", "secretkey", "devicepassword", "devcode", "authorization", "authtoken", "apikey").contains(lower)) return;
      safe.put(name, safeValue(value));
    });
    return safe;
  }

  private static Object safeValue(Object value) {
    if (value instanceof Map<?, ?> map) return safeData(map);
    if (value instanceof Collection<?> collection) return collection.stream().map(VideoAlarmPayload::safeValue).toList();
    if (value instanceof Object[] array) return Arrays.stream(array).map(VideoAlarmPayload::safeValue).toList();
    return value;
  }

  /** Official IoT callbacks carry actual properties under content.properties/outputData or data. */
  public static Map<String, Object> properties(Map<?, ?> source) {
    Map<String, Object> values = new LinkedHashMap<>();
    if (source == null) return values;
    for (String key : List.of("properties", "outputData", "data")) {
      if (source.get(key) instanceof Map<?, ?> map) values.putAll(safeData(map));
    }
    if (source.get("content") instanceof Map<?, ?> content) values.putAll(properties(content));
    return values;
  }

  private static String first(Map<String, ?> source, String... keys) {
    for (String key : keys) {
      Object value = source.get(key);
      if (value != null && !value.toString().isBlank()) return value.toString();
    }
    return "";
  }

  public static long timestamp(Object value, ZoneId zone) {
    if (value == null) return 0;
    String text = value.toString();
    try {
      if (text.matches("[0-9]{10,13}")) {
        long number = Long.parseLong(text);
        return text.length() <= 10 ? number * 1000 : number;
      }
      try { return Instant.parse(text).toEpochMilli(); } catch (RuntimeException ignored) { }
      var format = text.matches("[0-9]{8}T[0-9]{6}") ? DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
          : text.contains("T") ? DateTimeFormatter.ISO_LOCAL_DATE_TIME : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      return LocalDateTime.parse(text, format).atZone(zone).toInstant().toEpochMilli();
    } catch (RuntimeException ignored) { return 0; }
  }

  public static List<AlarmSignal.EvidenceRef> evidence(Map<?, ?> data) {
    Map<String, AlarmSignal.EvidenceRef> refs = new LinkedHashMap<>();
    if (data != null && data.get("evidence") instanceof Collection<?> archived) {
      for (Object value : archived) {
        if (!(value instanceof Map<?, ?> entry) || !"ready".equals(entry.get("status"))) continue;
        var ref = new AlarmSignal.EvidenceRef();
        // id is intentionally empty: these refs are not yet AlarmEvidence rows.
        ref.name = Objects.toString(entry.get("name"), "现场抓拍.jpg");
        ref.url = Objects.toString(entry.get("url"), "");
        ref.objectKey = Objects.toString(entry.get("objectKey"), "");
        ref.provider = Objects.toString(entry.get("provider"), "");
        ref.mimeType = Objects.toString(entry.get("mimeType"), "");
        ref.kind = "ORIGINAL";
        refs.put(ref.objectKey.isBlank() ? ref.url : ref.objectKey, ref);
      }
      return new ArrayList<>(refs.values());
    }
    collectEvidence(data, refs, 0);
    if (data != null && data.get("images") instanceof Collection<?> images) {
      for (Object value : images) {
        if (refs.size() >= 50) break;
        if (!(value instanceof Map<?, ?> image)) continue;
        String status = Objects.toString(image.get("status"), "");
        if (!Set.of("", "stored", "ready", "encrypted").contains(status)) continue;
        String url = Objects.toString(image.get("url"), "");
        if (!(url.startsWith("https://") || url.startsWith("http://")) || refs.containsKey(url)) continue;
        var ref = new AlarmSignal.EvidenceRef();
        ref.url = url;
        ref.name = Objects.toString(image.get("name"), "现场抓拍" + (refs.size() + 1) + ".jpg");
        ref.mimeType = Objects.toString(image.get("contentType"), "");
        String kind = Objects.toString(image.get("kind"), "ORIGINAL");
        ref.kind = Set.of("VIDEO", "THUMBNAIL", "ENCRYPTED_IMAGE").contains(kind) ? kind : "ORIGINAL";
        if (!"VIDEO".equals(ref.kind) && ("encrypted".equals(status) || url.matches(".*[?&]isEncrypted=1(?:&.*)?")
            || Set.of("1", "2").contains(Objects.toString(data.get("imageEncryption"), "0"))
            )) ref.kind = "ENCRYPTED_IMAGE";
        // Gateway storage status is not proof of tenant OSS ownership. Archive these source URLs
        // through the normal evidence worker; only the explicit evidence[] path above is trusted.
        refs.put(url, ref);
      }
    }
    return new ArrayList<>(refs.values());
  }

  private static void collectEvidence(Map<?, ?> data, Map<String, AlarmSignal.EvidenceRef> refs, int depth) {
    if (data == null || depth > 4 || refs.size() >= 50) return;
    for (String key : List.of("pictureList", "picUrls", "picUrlArr", "picUrlArray", "pictureUrl", "imageUrl", "snapshotUrl", "picUrl", "thumbUrl", "clipUrl", "videoUrl")) {
      Object value = data.get(key);
      if (value instanceof String json && json.stripLeading().startsWith("[")) {
        try { value = JSONUtil.parseArray(json); } catch (RuntimeException ignored) { }
      }
      Collection<?> urls = value instanceof Collection<?> collection ? collection : value == null ? List.of() : List.of(value);
      for (Object entry : urls) {
        Object url = entry instanceof Map<?, ?> picture ? picture.get("url") : entry;
        if (!(url instanceof String text) || !(text.startsWith("https://") || text.startsWith("http://")) || refs.containsKey(text)) continue;
        if (refs.size() >= 50) break;
        boolean video = key.equals("clipUrl") || key.equals("videoUrl");
        var ref = new AlarmSignal.EvidenceRef();
        ref.url = text;
        ref.name = (video ? "现场录像" : key.equals("thumbUrl") ? "缩略图" : "现场抓拍") + (refs.size() + 1) + (video ? ".mp4" : ".jpg");
        ref.kind = video ? "VIDEO" : key.equals("thumbUrl") ? "THUMBNAIL" : "ORIGINAL";
        if (!video && (text.matches(".*[?&]isEncrypted=1(?:&.*)?")
            || Set.of("1", "2").contains(Objects.toString(data.get("imageEncryption"), "0")))) ref.kind = "ENCRYPTED_IMAGE";
        ref.mimeType = video ? "video/mp4" : "";
        if (entry instanceof Map<?, ?> picture) ref.id = Objects.toString(picture.get("id"), "");
        refs.put(text, ref);
      }
    }
    for (String key : List.of("content", "outputData", "data", "params", "rawData")) {
      if (data.get(key) instanceof Map<?, ?> nested) collectEvidence(nested, refs, depth + 1);
    }
  }
}
