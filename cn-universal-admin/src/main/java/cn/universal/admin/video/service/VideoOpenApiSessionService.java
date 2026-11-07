package cn.universal.admin.video.service;

import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Opaque OpenAPI handles: callers never choose a native playback ID or a ZLM stream to control. */
@Service
@RequiredArgsConstructor
public class VideoOpenApiSessionService {
  private static final String PREFIX = "video:openapi:session:";
  private static final Duration LIFETIME = Duration.ofHours(2);
  private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};
  private final StringRedisTemplate redis;
  private final ObjectMapper mapper;
  private final VideoPlatformInstanceService instances;
  private final VideoIsupPreviewService isup;
  private final VideoDahuaPreviewService dahua;
  private final VideoUniviewPreviewService uniview;
  private final VideoSdkPlaybackService playback;
  private final VideoPlatformDeviceMapper devices;
  private final VideoPlatformChannelMapper channels;

  public String create(String ownerScope, VideoPlatformInstance instance, String deviceId,
      String channelId, String kind, Map<String, Object> stream) {
    String id = UUID.randomUUID().toString().replace("-", "");
    Map<String, Object> record = new LinkedHashMap<>();
    record.put("ownerScope", ownerScope);
    record.put("instanceKey", instance.getInstanceKey());
    record.put("tenantId", instance.getTenantId());
    record.put("deviceId", deviceId);
    record.put("channelId", channelId);
    record.put("kind", kind);
    record.put("stream", new LinkedHashMap<>(stream));
    record.put("expiresAt", System.currentTimeMillis() + LIFETIME.toMillis());
    write(id, record);
    return id;
  }

  public Map<String, Object> require(String id, String ownerScope) {
    Map<String, Object> record = read(id);
    if (!Objects.equals(record.get("ownerScope"), ownerScope))
      throw new org.springframework.security.access.AccessDeniedException("视频会话不属于当前应用或通道");
    return record;
  }

  public void update(String id, String ownerScope, Map<String, Object> stream) {
    Map<String, Object> record = require(id, ownerScope);
    record.put("stream", new LinkedHashMap<>(stream));
    write(id, record);
  }

  public void remove(String id, String ownerScope) {
    require(id, ownerScope);
    redis.delete(PREFIX + id);
  }

  /** Only called after resolving an unexpired player ticket to its server-created handle. */
  public void heartbeat(String id) {
    Map<String, Object> record = read(id);
    VideoPlatformInstance instance = instances.getByInstanceKey(text(record, "instanceKey"));
    if (instance == null || !Objects.equals(instance.getTenantId(), record.get("tenantId"))
        || !Integer.valueOf(1).equals(instance.getEnabled()))
      throw new BaseException("播放实例已失效");
    Map<String, Object> stream = stream(record);
    String device = text(record, "deviceId"), channel = text(record, "channelId");
    VideoPlatformDevice currentDevice = devices.selectOne(VideoPlatformDevice.builder()
        .instanceKey(instance.getInstanceKey()).deviceId(device).build());
    if (currentDevice == null || !Objects.equals(currentDevice.getTenantId(), record.get("tenantId"))
        || Integer.valueOf(0).equals(currentDevice.getEnabled())) throw new BaseException("播放设备已失效");
    VideoPlatformChannel currentChannel = channels.selectOne(VideoPlatformChannel.builder()
        .instanceKey(instance.getInstanceKey()).deviceId(currentDevice.getId()).channelId(channel).build());
    if (currentChannel == null || !Objects.equals(currentChannel.getTenantId(), record.get("tenantId"))
        || Integer.valueOf(0).equals(currentChannel.getEnabled())) throw new BaseException("播放通道已失效");
    if ("playback".equals(record.get("kind")) && stream.get("playbackId") != null) {
      Map<String, Object> input = new LinkedHashMap<>();
      input.put("playbackId", stream.get("playbackId"));
      input.put("deviceId", device);
      input.put("channelId", channel);
      if (stream.get("ownerNode") != null) input.put("ownerNode", stream.get("ownerNode"));
      Map<String, Object> state = playback.control(instance, "playbackStreamStatus", input);
      if (java.util.Set.of("ended", "failed", "stopped", "expired").contains(Objects.toString(state.get("status"), "")))
        throw new BaseException("录像播放已结束");
    } else if ("preview".equals(record.get("kind"))) {
      String name = Objects.toString(stream.getOrDefault("zlmStream", stream.get("stream")), "");
      if ("hik_isup".equals(instance.getPlatformType())) {
        isup.assertOwner(stream.get("ownerNode"));
        isup.heartbeat(instance, device, channel, name);
      } else if (stream.get("previewId") != null && "dahua_sdk".equals(instance.getPlatformType())) {
        dahua.assertOwner(stream.get("ownerNode"));
        requireActivePreview(dahua.heartbeat(instance, device, channel, name, stream.get("previewId")));
      } else if (stream.get("previewId") != null && "uniview_sdk".equals(instance.getPlatformType())) {
        uniview.assertOwner(stream.get("ownerNode"));
        requireActivePreview(uniview.heartbeat(instance, device, channel, name, stream.get("previewId")));
      }
    }
  }

  private static void requireActivePreview(Map<String, Object> state) {
    if (state == null || Boolean.FALSE.equals(state.get("active"))
        || java.util.Set.of("ended", "failed", "stopped", "expired")
            .contains(Objects.toString(state.get("status"), "").toLowerCase(java.util.Locale.ROOT)))
      throw new BaseException("预览会话已结束，请重新创建播放链接");
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> stream(Map<String, Object> record) {
    return new LinkedHashMap<>((Map<String, Object>) record.get("stream"));
  }

  private Map<String, Object> read(String id) {
    if (id == null || !id.matches("[a-f0-9]{32}")) throw new BaseException("视频会话无效");
    String json = redis.opsForValue().get(PREFIX + id);
    if (json == null) throw new BaseException("视频会话已结束或过期");
    try {
      Map<String, Object> record = mapper.readValue(json, MAP);
      if (((Number) record.get("expiresAt")).longValue() <= System.currentTimeMillis())
        throw new BaseException("视频会话已过期");
      return record;
    } catch (BaseException e) {
      throw e;
    } catch (Exception e) {
      throw new BaseException("视频会话不可用");
    }
  }

  private void write(String id, Map<String, Object> record) {
    try {
      long remaining = ((Number) record.get("expiresAt")).longValue() - System.currentTimeMillis();
      if (remaining <= 0) throw new BaseException("视频会话已过期");
      redis.opsForValue().set(PREFIX + id, mapper.writeValueAsString(record), Duration.ofMillis(remaining));
    } catch (BaseException e) {
      throw e;
    } catch (Exception e) {
      throw new BaseException("无法保存视频会话");
    }
  }

  private static String text(Map<String, Object> map, String key) {
    return Objects.toString(map.get(key), "");
  }
}
