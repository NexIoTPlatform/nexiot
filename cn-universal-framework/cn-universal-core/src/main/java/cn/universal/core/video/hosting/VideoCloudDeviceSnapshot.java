package cn.universal.core.video.hosting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Diagnostic whitelist: vendor credentials and temporary playback URLs never enter snapshots. */
public final class VideoCloudDeviceSnapshot {
  private static final Set<String> DEVICE_FIELDS = Set.of(
      "deviceModel", "deviceVersion", "deviceStatus", "deviceAbility", "abilityRefs",
      "encryptMode", "isTcmDevice", "networkAccessType", "sdcardStatus", "source",
      "channelNum", "deviceSpec", "productId", "deviceType", "brand", "authStatus", "accessType",
      "lastOffLineTime", "port", "rtspPort", "httpPort", "tlsEnable");
  private static final Set<String> CHANNEL_FIELDS = Set.of(
      "channelId", "channelName", "channelStatus", "cameraStatus", "channelAbility",
      "abilityRefs", "shareFunctions", "movable", "unitType", "resolutions", "productId",
      "csStatus", "lastOffLineTime");
  private static final Set<String> RESOLUTION_FIELDS = Set.of(
      "name", "imageSize", "streamType", "width", "height");

  private VideoCloudDeviceSnapshot() {}

  public static Map<String, Object> device(VideoCloudHostingProvider.HostedDevice hosted) {
    Map<String, Object> snapshot = select(hosted.raw(), DEVICE_FIELDS);
    if (hosted.model() != null && !hosted.model().isBlank()) snapshot.put("deviceModel", hosted.model());
    if (!snapshot.containsKey("isTcmDevice") && snapshot.containsKey("deviceAbility")) {
      Object ability = snapshot.get("deviceAbility");
      List<String> tokens = new ArrayList<>();
      if (ability instanceof String text) tokens.addAll(List.of(text.split(",")));
      else if (ability instanceof Iterable<?> values) values.forEach(value -> tokens.add(String.valueOf(value)));
      snapshot.put("isTcmDevice", tokens.stream().anyMatch(token -> "TCM".equalsIgnoreCase(token.trim())));
    }
    return snapshot;
  }

  public static List<Map<String, Object>> channels(Map<String, Object> raw) {
    List<Map<String, Object>> result = new ArrayList<>();
    if (raw == null) return result;
    Object channels = raw.get("channelList");
    if (channels == null) channels = raw.get("channels");
    if (channels instanceof Iterable<?> items) {
      for (Object item : items) {
        if (!(item instanceof Map<?, ?> channel)) continue;
        Map<String, Object> safe = channel(channel);
        Object id = safe.get("channelId");
        if (id == null) id = channel.get("id");
        if (id == null || id.toString().isBlank()) continue;
        safe.put("channelId", id.toString());
        safe.putIfAbsent("channelName", "通道" + id);
        result.add(safe);
      }
    }
    return result;
  }

  public static Map<String, Object> channel(Map<?, ?> raw) {
    Map<String, Object> safe = select(raw, CHANNEL_FIELDS);
    safe.remove("resolutions");
    if (raw != null && raw.get("resolutions") instanceof Iterable<?> resolutions) {
      List<Map<String, Object>> values = new ArrayList<>();
      for (Object value : resolutions)
        if (value instanceof Map<?, ?> resolution) values.add(select(resolution, RESOLUTION_FIELDS));
      safe.put("resolutions", values);
    }
    return safe;
  }

  private static Map<String, Object> select(Map<?, ?> raw, Set<String> fields) {
    Map<String, Object> safe = new LinkedHashMap<>();
    if (raw == null) return safe;
    for (String key : fields) {
      Object value = raw.get(key);
      if (value instanceof String || value instanceof Number || value instanceof Boolean)
        safe.put(key, value);
      else if (value instanceof Iterable<?> items && !"resolutions".equals(key)) {
        List<Object> scalars = new ArrayList<>();
        for (Object item : items)
          if (item instanceof String || item instanceof Number || item instanceof Boolean) scalars.add(item);
        safe.put(key, scalars);
      }
    }
    return safe;
  }
}
