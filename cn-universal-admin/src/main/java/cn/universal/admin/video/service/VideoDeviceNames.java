package cn.universal.admin.video.service;

import cn.universal.common.exception.BaseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 设备原名与平台别名共用的快照规则；通道标识始终保持不变。 */
public final class VideoDeviceNames {
  public static final String DEVICE_OVERRIDE = "deviceNameOverride";
  public static final String CHANNEL_OVERRIDES = "channelNameOverrides";
  public static final String SOURCE_DEVICE = "sourceDeviceName";
  public static final String SOURCE_CHANNELS = "sourceChannelNames";
  public static final List<String> METADATA =
      List.of(DEVICE_OVERRIDE, CHANNEL_OVERRIDES, SOURCE_DEVICE, SOURCE_CHANNELS);
  private static final ObjectMapper JSON = new ObjectMapper();

  private VideoDeviceNames() {}

  public static Map<String, Object> configuration(String value) {
    if (value == null || value.isBlank()) return new LinkedHashMap<>();
    try {
      Map<String, Object> result =
          JSON.readValue(value, new TypeReference<LinkedHashMap<String, Object>>() {});
      if (result == null) throw new IllegalArgumentException("null configuration");
      return result;
    } catch (Exception e) {
      throw new BaseException("视频设备配置无效，已停止修改以保留原配置");
    }
  }

  public static Map<String, Object> object(Object value) {
    Map<String, Object> result = new LinkedHashMap<>();
    if (value instanceof Map<?, ?> map)
      map.forEach((key, item) -> result.put(String.valueOf(key), item));
    return result;
  }

  public static String text(Object value) {
    return value == null ? "" : String.valueOf(value).strip();
  }

  public static String physicalId(String channelId) {
    return channelId.endsWith("_sub") ? channelId.substring(0, channelId.length() - 4) : channelId;
  }

  public static String displayName(String channelId, String baseName) {
    return channelId.endsWith("_sub") ? baseName + " Sub" : baseName;
  }

  /** 在写入主表、通道表、IoT 前统一应用别名，并保存本次探测到的原名。 */
  public static Map<String, Object> apply(
      String storedConfiguration, Map<String, Object> incoming) {
    Map<String, Object> stored = configuration(storedConfiguration);
    Map<String, Object> result = new LinkedHashMap<>(incoming);
    METADATA.forEach(result::remove);
    String sourceDevice = first(incoming.get("deviceName"), incoming.get("name"));
    if (sourceDevice.isEmpty()) sourceDevice = text(stored.get(SOURCE_DEVICE));
    if (!sourceDevice.isEmpty()) result.put(SOURCE_DEVICE, sourceDevice);
    String deviceOverride = text(stored.get(DEVICE_OVERRIDE));
    if (!deviceOverride.isEmpty()) {
      result.put(DEVICE_OVERRIDE, deviceOverride);
      result.put("deviceName", deviceOverride);
      result.put("name", deviceOverride);
    }
    Map<String, Object> overrides = object(stored.get(CHANNEL_OVERRIDES));
    Map<String, Object> sources = object(stored.get(SOURCE_CHANNELS));
    if (!overrides.isEmpty()) result.put(CHANNEL_OVERRIDES, overrides);
    if (incoming.get("channelList") instanceof List<?> list) {
      List<Map<String, Object>> channels = new ArrayList<>();
      for (Object item : list) {
        Map<String, Object> channel = object(item);
        String id = first(channel.get("channelId"), channel.get("id"));
        String source = first(channel.get("channelName"), channel.get("name"));
        boolean fallback =
            "fallback".equals(channel.get("channelNameSource"))
                || "fallback".equals(object(channel.get("attributes")).get("channelNameSource"));
        if (fallback && !text(sources.get(id)).isEmpty()) {
          source = text(sources.get(id));
          channel.put("channelName", source);
          channel.put("name", source);
        } else if (!id.isEmpty() && !source.isEmpty() && !fallback) sources.put(id, source);
        String alias = text(overrides.get(physicalId(id)));
        if (!alias.isEmpty()) {
          channel.put("channelName", displayName(id, alias));
          channel.put("name", displayName(id, alias));
        }
        channels.add(channel);
      }
      result.put("channelList", channels);
    }
    if (!sources.isEmpty()) result.put(SOURCE_CHANNELS, sources);
    return result;
  }

  private static String first(Object first, Object second) {
    String value = text(first);
    return value.isEmpty() ? text(second) : value;
  }

  public static String encode(Map<String, Object> value) {
    try {
      return JSON.writeValueAsString(value);
    } catch (Exception e) {
      throw new BaseException("保存视频设备名称配置失败");
    }
  }

  public static String validateName(Object value) {
    if (!(value instanceof String)) throw new BaseException("请输入名称");
    String name = text(value);
    if (name.isEmpty()
        || name.length() > 200
        || name.codePoints().anyMatch(Character::isISOControl))
      throw new BaseException("名称须为1至200个字符，不能包含控制字符");
    return name;
  }
}
