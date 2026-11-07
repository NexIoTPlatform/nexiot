package cn.universal.admin.video.alarm;

import cn.universal.core.video.CameraThingModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 视频标准上行的存储投影；原生调试副本不随每条日志、订阅和 Flow 重复保存。 */
final class VideoAlarmEventData {
  static final Set<String> REDUNDANT_FIELDS = CameraThingModel.COMPACT_VIDEO_FIELDS;
  private static final List<String> URL_FIELDS = List.of(
      "url", "snapshotUrl", "imageUrl", "localUrl", "ossUrl");

  private VideoAlarmEventData() {}

  static Map<String, Object> compact(Map<String, Object> source, String eventName) {
    Map<String, Object> data = new LinkedHashMap<>(source);
    data.entrySet().removeIf(entry -> entry.getValue() == null
        || entry.getValue() instanceof String text && text.isBlank());
    if (Objects.equals(data.get("description"), eventName)) data.remove("description");
    if (Objects.equals(data.get("alarmDescription"), data.get("description"))) data.remove("alarmDescription");
    if (Objects.equals(data.get("channelName"), data.get("channelId"))) data.remove("channelName");
    if (data.get("channelNo") != null && Objects.toString(data.get("channelNo")).equals(data.get("channelId"))) data.remove("channelNo");
    if (Objects.equals(data.get("sourceEventId"), data.get("eventId"))) data.remove("sourceEventId");

    List<Map<String, Object>> images = new ArrayList<>();
    if (data.get("images") instanceof List<?> list) {
      for (Object value : list) if (value instanceof Map<?, ?> image) {
        Map<String, Object> compact = image(image, data.get("imageStatus"));
        if (!compact.isEmpty()) {
          Map<String, Object> existing = images.stream()
              .filter(item -> compact.get("url") != null && compact.get("url").equals(item.get("url")))
              .findFirst().orElse(null);
          if (existing == null) images.add(compact); else compact.forEach(existing::putIfAbsent);
        }
      }
    }
    String snapshot = first(data, "snapshotUrl", "imageUrl", "localUrl");
    if (!snapshot.isBlank() && images.stream().noneMatch(image -> snapshot.equals(image.get("url")))) {
      Map<String, Object> image = new LinkedHashMap<>();
      image.put("url", snapshot);
      if (data.get("imageStatus") != null) image.put("status", data.get("imageStatus"));
      else if (Boolean.TRUE.equals(data.get("gatewayStored"))) image.put("status", "stored");
      images.add(image);
    }
    REDUNDANT_FIELDS.forEach(data::remove);
    if (images.isEmpty()) data.remove("images"); else data.put("images", images);
    if (data.get("evidence") instanceof List<?> list) {
      List<Object> references = new ArrayList<>();
      for (Object value : list) {
        if (!(value instanceof Map<?, ?> reference)) { references.add(value); continue; }
        Map<String, Object> compact = new LinkedHashMap<>();
        reference.forEach((key, item) -> compact.put(String.valueOf(key), item));
        String url = first(reference, "url", "snapshotUrl", "imageUrl", "localUrl", "mp4Url");
        if (!url.isBlank()) {
          compact.put("url", url);
          for (String alias : List.of("snapshotUrl", "imageUrl", "localUrl", "mp4Url", "downloadUrl"))
            if (url.equals(compact.get(alias))) compact.remove(alias);
        }
        references.add(compact);
      }
      data.put("evidence", references);
    }
    return data;
  }

  private static Map<String, Object> image(Map<?, ?> source, Object status) {
    Map<String, Object> image = new LinkedHashMap<>();
    String url = first(source, URL_FIELDS.toArray(String[]::new));
    source.forEach((key, value) -> {
      String field = String.valueOf(key);
      if (!CameraThingModel.COMPACT_VIDEO_IMAGE_REDUNDANT_FIELDS.contains(field)
          && value != null && (!(value instanceof String text) || !text.isBlank())) image.put(field, value);
    });
    if (!url.isBlank()) image.put("url", url);
    String download = Objects.toString(image.get("downloadUrl"), "");
    if (download.equals(url) || download.equals(url + (url.contains("?") ? "&" : "?") + "download=true"))
      image.remove("downloadUrl");
    if (!image.containsKey("size") && source.get("sizeBytes") != null) image.put("size", source.get("sizeBytes"));
    String stored = first(source, "status");
    // A failed local replica does not invalidate the readable cloud URL selected by the gateway.
    if (stored.isBlank() && Boolean.TRUE.equals(source.get("gatewayStored"))) stored = "stored";
    if (stored.isBlank()) stored = first(source, "localStatus");
    if (stored.isBlank()) stored = Objects.toString(status, "");
    if (!stored.isBlank()) image.put("status", stored);
    return image;
  }

  private static String first(Map<?, ?> source, String... keys) {
    for (String key : keys) {
      String value = Objects.toString(source.get(key), "");
      if (!value.isBlank()) return value;
    }
    return "";
  }
}
