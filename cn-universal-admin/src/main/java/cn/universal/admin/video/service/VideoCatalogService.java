package cn.universal.admin.video.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.common.service.BaseServiceImpl;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.vo.VideoCatalogChannelCount;
import cn.universal.persistence.entity.vo.VideoCatalogConfigurationChannel;
import cn.universal.persistence.entity.vo.VideoCatalogDeviceRow;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Tenant/dept-scoped directory. Device pages never query or serialize every native channel. */
@Service
public class VideoCatalogService extends BaseServiceImpl {
  private static final int DEFAULT_PAGE_SIZE = 50;
  private static final int MAX_PAGE_SIZE = 100;
  private static final Set<String> SDK_PLATFORMS = Set.of(
      "imoulife", "dahua_cloud", "hik_sdk", "dahua_sdk", "uniview_sdk", "tiandy_sdk");

  private final IoTDeviceMapper devices;

  public VideoCatalogService(IoTDeviceMapper devices) {
    this.devices = devices;
  }

  public VideoCatalogPage list(int requestedPage, int requestedSize, String keyword, String groupId) {
    int pageNum = Math.max(1, requestedPage);
    int pageSize = pageSize(requestedSize);
    IoTUser user = requireCurrentUser();
    IoTDevice query = scopedQuery(user);
    List<VideoCatalogDeviceRow> records;
    long total;
    // PageHelper adds dialect pagination to this projection and uses its matching _COUNT query.
    try (Page<VideoCatalogDeviceRow> page = PageHelper.startPage(pageNum, pageSize, true)) {
      page.setReasonable(false);
      records = devices.selectVideoCatalogDevices(
          query, StrUtil.trimToNull(keyword), StrUtil.trimToNull(groupId), null);
      total = page.getTotal();
    }
    return new VideoCatalogPage(normalizeDevicePage(records, resolveTenantId(user)), total,
        pageNum, pageSize, (long) pageNum * pageSize < total);
  }

  public VideoCatalogChannelPage channels(String iotId, int requestedPage, int requestedSize) {
    int pageNum = Math.max(1, requestedPage);
    int pageSize = pageSize(requestedSize);
    IoTUser user = requireCurrentUser();
    VideoCatalogDeviceRow device = visibleDevice(iotId, user);
    JSONObject config = parseConfig(device.getConfiguration());
    device.setInstanceKey(instanceKey(device, config));
    String tenantId = device.getTenantId();
    if (nativeCounts(List.of(device), tenantId).getOrDefault(pairKey(device), 0L) > 0) {
      try (Page<VideoPlatformChannel> page = PageHelper.startPage(pageNum, pageSize, true)) {
        page.setReasonable(false);
        List<VideoPlatformChannel> records = devices.selectVideoCatalogNativeChannels(
            device.getInstanceKey(), device.getDeviceId(), tenantId, null);
        return new VideoCatalogChannelPage(toNativeChannels(records).stream()
            .map(channel -> inheritCapabilities(channel, config)).toList(), page.getTotal(),
            pageNum, pageSize, (long) pageNum * pageSize < page.getTotal());
      }
    }

    // Legacy cloud devices store channels as one JSON blob, not normalized table rows.
    // Whitelist/normalize that one blob, then expose it as a DB rowset. PageHelper still owns
    // count + LIMIT/OFFSET; no in-memory subList paging or unbounded channel response is used.
    List<VideoCatalogChannel> configured = configurationChannels(config);
    try (Page<VideoCatalogConfigurationChannel> page = PageHelper.startPage(pageNum, pageSize, true)) {
      page.setReasonable(false);
      List<VideoCatalogConfigurationChannel> records = devices.selectVideoCatalogConfigurationChannels(
          JSONUtil.toJsonStr(configured));
      List<VideoCatalogChannel> rows = records.stream().map(this::fromConfigurationProjection)
          .map(channel -> inheritCapabilities(channel, config)).toList();
      return new VideoCatalogChannelPage(rows, page.getTotal(), pageNum, pageSize,
          (long) pageNum * pageSize < page.getTotal());
    }
  }

  /** SDK authorization callers must resolve through this same tenant/dept scope. */
  public VideoCatalogDeviceRow requireVisibleDevice(String iotId) {
    return visibleDevice(iotId, requireCurrentUser());
  }

  public VideoCatalogChannel requireVisibleChannel(String iotId, String channelId) {
    return requireVisibleChannel(requireVisibleDevice(iotId), channelId);
  }

  /** The device must have just been returned by requireVisibleDevice in the current request. */
  public VideoCatalogChannel requireVisibleChannel(VideoCatalogDeviceRow device, String channelId) {
    if (StrUtil.isBlank(channelId)) throw new BaseException("视频通道不存在或无权访问");
    IoTUser user = requireCurrentUser();
    JSONObject config = parseConfig(device.getConfiguration());
    device.setInstanceKey(instanceKey(device, config));
    String tenantId = device.getTenantId();
    if (nativeCounts(List.of(device), tenantId).getOrDefault(pairKey(device), 0L) > 0) {
      List<VideoPlatformChannel> records = devices.selectVideoCatalogNativeChannels(
          device.getInstanceKey(), device.getDeviceId(), tenantId, channelId);
      if (records != null && !records.isEmpty()) return inheritCapabilities(toNativeChannel(records.get(0)), config);
    } else {
      for (VideoCatalogChannel channel : configurationChannels(config)) {
        if (channelId.equals(channel.getChannelId())) return inheritCapabilities(channel, config);
      }
    }
    throw new BaseException("视频通道不存在或无权访问");
  }

  private VideoCatalogDeviceRow visibleDevice(String iotId, IoTUser user) {
    if (StrUtil.isBlank(iotId)) throw new BaseException("视频设备不存在或无权访问");
    List<VideoCatalogDeviceRow> rows = devices.selectVideoCatalogDevices(scopedQuery(user), null, null, iotId);
    if (rows == null || rows.size() != 1) throw new BaseException("视频设备不存在或无权访问");
    VideoCatalogDeviceRow row = rows.get(0);
    row.setInstanceKey(instanceKey(row, parseConfig(row.getConfiguration())));
    return row;
  }

  private List<VideoCatalogDevice> normalizeDevicePage(List<VideoCatalogDeviceRow> rows, String tenantId) {
    if (rows == null || rows.isEmpty()) return List.of();
    Map<String, JSONObject> configs = new HashMap<>();
    for (VideoCatalogDeviceRow row : rows) {
      JSONObject config = parseConfig(row.getConfiguration());
      configs.put(row.getIotId(), config);
      row.setInstanceKey(instanceKey(row, config));
    }
    Map<String, Long> counts = nativeCounts(rows, tenantId);
    List<VideoCatalogDevice> result = new ArrayList<>(rows.size());
    for (VideoCatalogDeviceRow row : rows) {
      JSONObject config = configs.get(row.getIotId());
      String platform = normalizePlatform(row.getPlatformType(), config.getStr("platformType"));
      VideoCatalogDevice device = new VideoCatalogDevice();
      device.setId(row.getId() == null ? row.getIotId() : row.getId().toString());
      device.setIotId(row.getIotId());
      device.setDeviceId(row.getDeviceId());
      device.setDeviceName(StrUtil.blankToDefault(row.getDeviceName(), row.getDeviceId()));
      device.setProductKey(row.getProductKey());
      device.setInstanceKey(row.getInstanceKey());
      device.setPlatformType(platform);
      device.setState(row.getState());
      device.setSdkVendor(sdkVendor(platform));
      device.setSdkRequired(SDK_PLATFORMS.contains(platform));
      Map<String, Object> capabilities = new LinkedHashMap<>(capabilityMap(config, null, null));
      capabilities.put("sdkVendor", device.getSdkVendor());
      capabilities.put("sdkRequired", device.isSdkRequired());
      capabilities.put("sdkPreview", Set.of("imoulife", "dahua_cloud").contains(platform));
      device.setCapabilities(capabilities);
      long count = counts.getOrDefault(pairKey(row), 0L);
      if (count == 0) count = configuredChannelIds(config).size();
      device.setChannelCount((int) Math.min(count, Integer.MAX_VALUE));
      // Zero inline channel rows keeps a 100-device page bounded even for large NVRs.
      device.setChannelsTruncated(count > 0);
      result.add(device);
    }
    return result;
  }

  private Map<String, Long> nativeCounts(List<VideoCatalogDeviceRow> rows, String tenantId) {
    if (rows.isEmpty()) return Map.of();
    List<VideoCatalogChannelCount> counts = devices.selectVideoCatalogChannelCounts(rows, tenantId);
    Map<String, Long> result = new HashMap<>();
    if (counts != null) for (VideoCatalogChannelCount count : counts) {
      result.put(pairKey(count.getInstanceKey(), count.getDeviceId(), count.getTenantId()), count.getChannelCount());
    }
    return result;
  }

  private List<VideoCatalogChannel> toNativeChannels(List<VideoPlatformChannel> rows) {
    if (rows == null) return List.of();
    return rows.stream().map(this::toNativeChannel).toList();
  }

  private VideoCatalogChannel toNativeChannel(VideoPlatformChannel source) {
    String id = firstNonBlank(source.getChannelId(), source.getChannelCode(), source.getChannelNo(),
        source.getChannelSeq() == null ? null : String.valueOf(source.getChannelSeq()));
    VideoCatalogChannel channel = new VideoCatalogChannel();
    channel.setChannelId(id);
    channel.setChannelName(StrUtil.blankToDefault(source.getChannelName(), "通道" + id));
    channel.setChannelStatus(normalizeStatus(source.getChannelStatus()));
    channel.setChannelNo(parseChannelNo(source.getChannelNo(), source.getChannelSeq(), id));
    channel.setStreamType(id.toLowerCase(Locale.ROOT).endsWith("_sub") ? "sub" : "main");
    channel.setCapabilities(capabilityMap(parseConfig(source.getCapabilities()), source.getPtz(), source.getHasAudio()));
    return channel;
  }

  private List<VideoCatalogChannel> configurationChannels(JSONObject config) {
    Map<String, VideoCatalogChannel> unique = new LinkedHashMap<>();
    for (JSONObject source : configurationSources(config)) {
      String id = configuredId(source);
      if (id == null || !videoChannel(source) || Boolean.FALSE.equals(bool(source.get("enabled")))) continue;
      unique.putIfAbsent(id, channelFromConfig(source, id));
    }
    List<VideoCatalogChannel> result = new ArrayList<>(unique.values());
    result.sort(Comparator.comparing(VideoCatalogChannel::getChannelNo, Comparator.nullsLast(Integer::compareTo))
        .thenComparing(VideoCatalogChannel::getChannelId));
    return result;
  }

  private Set<String> configuredChannelIds(JSONObject config) {
    Set<String> ids = new LinkedHashSet<>();
    for (JSONObject source : configurationSources(config)) {
      String id = configuredId(source);
      if (id != null && videoChannel(source) && !Boolean.FALSE.equals(bool(source.get("enabled")))) ids.add(id);
    }
    return ids;
  }

  private List<JSONObject> configurationSources(JSONObject config) {
    JSONObject snapshot = object(config.get("cloudDeviceSnapshot"));
    Collection<?> selected = null;
    for (JSONObject source : List.of(config, snapshot)) {
      for (String key : List.of("channelList", "channels")) {
        Object candidate = source.get(key);
        if (candidate instanceof Collection<?> collection && !collection.isEmpty()) {
          selected = collection;
          break;
        }
      }
      if (selected != null) break;
    }
    List<JSONObject> result = new ArrayList<>();
    if (selected != null) {
      for (Object item : selected) if (item instanceof Map<?, ?>) result.add(object(item));
    } else {
      String id = firstNonBlank(config.getStr("channelId"), config.getStr("channelNo"), snapshot.getStr("channelId"));
      // Never invent channel 0 for an unknown/empty catalog. Literal configured "0" is valid.
      if (id != null) result.add(new JSONObject().set("channelId", id)
          .set("channelName", firstNonBlank(config.getStr("channelName"), "通道" + id)));
    }
    return result;
  }

  private String configuredId(JSONObject source) {
    return firstNonBlank(source.getStr("channelId"), source.getStr("id"), source.getStr("channelNo"));
  }

  private VideoCatalogChannel channelFromConfig(JSONObject source, String id) {
    VideoCatalogChannel channel = new VideoCatalogChannel();
    channel.setChannelId(id);
    channel.setChannelName(firstNonBlank(source.getStr("channelName"), source.getStr("name"), "通道" + id));
    channel.setChannelStatus(normalizeStatus(firstNonBlank(source.getStr("channelStatus"), source.getStr("cameraStatus"), source.getStr("status"))));
    channel.setChannelNo(parseChannelNo(source.getStr("channelNo"),
        parseChannelNo(source.getStr("channelSeq"), null, null), id));
    channel.setStreamType(Set.of("sub", "1").contains(source.getStr("streamType", ""))
        || id.toLowerCase(Locale.ROOT).endsWith("_sub") ? "sub" : "main");
    channel.setCapabilities(capabilityMap(source, null, null));
    return channel;
  }

  private VideoCatalogChannel fromConfigurationProjection(VideoCatalogConfigurationChannel row) {
    VideoCatalogChannel result = new VideoCatalogChannel();
    result.setChannelId(row.getChannelId());
    result.setChannelName(row.getChannelName());
    result.setChannelStatus(row.getChannelStatus());
    result.setChannelNo(row.getChannelNo());
    result.setStreamType(row.getStreamType());
    Map<String, String> capabilities = new LinkedHashMap<>();
    capabilities.put("preview", row.getPreview());
    capabilities.put("playback", row.getPlayback());
    capabilities.put("ptz", row.getPtz());
    capabilities.put("audio", row.getAudio());
    result.setCapabilities(capabilities);
    return result;
  }

  private Map<String, String> capabilityMap(JSONObject raw, Integer ptz, Integer audio) {
    Map<String, String> result = new LinkedHashMap<>();
    result.put("preview", capability(raw, List.of("preview", "canPreview", "previewSupported", "live", "liveStream"), null));
    result.put("playback", capability(raw, List.of("playback", "canPlayback", "playbackSupported", "recordPlayback", "localStorage"), null));
    result.put("ptz", capability(raw, List.of("ptz", "canPtz", "ptzSupported", "movable"), ptz));
    result.put("audio", capability(raw, List.of("audio", "hasAudio", "audioSupported", "talk"), audio));
    return result;
  }

  private VideoCatalogChannel inheritCapabilities(VideoCatalogChannel channel, JSONObject config) {
    Map<String, String> inherited = capabilityMap(config, null, null);
    Map<String, String> merged = new LinkedHashMap<>(channel.getCapabilities());
    inherited.forEach((key, value) -> {
      if ("unknown".equals(merged.getOrDefault(key, "unknown"))) merged.put(key, value);
    });
    channel.setCapabilities(merged);
    return channel;
  }

  private boolean videoChannel(JSONObject channel) {
    // Cloud device directories may also contain alarm/relay units alongside video channels.
    return !channel.containsKey("unitType") || "1".equals(channel.getStr("unitType"));
  }

  private String capability(JSONObject raw, List<String> keys, Integer numeric) {
    if (numeric != null) return numeric == 0 ? "unsupported" : numeric == 1 ? "supported" : "unknown";
    for (JSONObject source : List.of(raw, object(raw.get("capabilities")), object(raw.get("deviceAbility")),
        object(raw.get("cloudDeviceSnapshot")))) {
      for (String key : keys) {
        Boolean parsed = bool(source.get(key));
        if (parsed != null) return parsed ? "supported" : "unsupported";
      }
      for (String field : List.of("deviceAbility", "channelAbility", "shareFunctions", "abilityRefs")) {
        Object value = source.get(field);
        Collection<?> tokens = value instanceof Collection<?> list ? list
            : value instanceof String text ? List.of(text.split("[,;\\s]+")) : List.of();
        for (Object token : tokens) for (String key : keys) {
          if (key.equalsIgnoreCase(String.valueOf(token).trim())) return "supported";
        }
      }
    }
    return "unknown";
  }

  private JSONObject parseConfig(String json) {
    if (StrUtil.isBlank(json)) return new JSONObject();
    try { return JSONUtil.parseObj(json); } catch (RuntimeException ignored) { return new JSONObject(); }
  }

  private JSONObject object(Object value) {
    if (value instanceof Map<?, ?> map) return JSONUtil.parseObj(map);
    if (value instanceof String text && text.trim().startsWith("{")) return parseConfig(text);
    return new JSONObject();
  }

  private String instanceKey(VideoCatalogDeviceRow row, JSONObject config) {
    return firstNonBlank(row.getGwProductKey(), row.getExtDeviceId(), config.getStr("instanceKey"), row.getProductKey());
  }

  private String normalizePlatform(String primary, String fallback) {
    String value = StrUtil.blankToDefault(primary, fallback);
    return value == null ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
  }

  private String sdkVendor(String platform) {
    return switch (platform) {
      case "imoulife" -> "imou";
      case "dahua_cloud" -> "dahua_cloud";
      case "hik_sdk", "hik_isup" -> "hikvision";
      case "dahua_sdk" -> "dahua";
      case "uniview_sdk" -> "uniview";
      case "tiandy_sdk" -> "tiandy";
      default -> "unknown";
    };
  }

  private String normalizeStatus(String status) {
    if (status == null) return "UNKNOWN";
    String normalized = status.trim().toLowerCase(Locale.ROOT);
    if (Set.of("1", "online", "on", "true", "active", "connected", "正常").contains(normalized)) return "ON";
    if (Set.of("0", "offline", "off", "false", "inactive", "disconnected", "离线").contains(normalized)) return "OFF";
    return "UNKNOWN";
  }

  private Integer parseChannelNo(String channelNo, Integer channelSeq, String id) {
    if (StrUtil.isNotBlank(channelNo)) {
      try { return Integer.valueOf(channelNo.trim()); } catch (NumberFormatException ignored) { }
    }
    if (channelSeq != null) return channelSeq;
    try { return Integer.valueOf(id); } catch (RuntimeException ignored) { return null; }
  }

  private IoTDevice scopedQuery(IoTUser user) {
    IoTDevice query = new IoTDevice();
    applyDataPermissionScope(query, user);
    if (StrUtil.isBlank(query.getTenantId())) query.setTenantId(resolveTenantId(user));
    if (StrUtil.isBlank(query.getTenantId()) && (query.getParams() == null ||
        (!Boolean.TRUE.equals(query.getParams().get("tenantAdminScope"))
            && !Boolean.TRUE.equals(query.getParams().get("scopeDenied"))
            && query.getParams().get("scopeUnionIds") == null
            && query.getParams().get("scopeDeptIds") == null))) {
      throw new BaseException("无法确认当前用户的数据权限范围");
    }
    return query;
  }

  private IoTUser requireCurrentUser() {
    IoTUser user = currentTenantUser();
    if (user == null) throw new BaseException("请先登录");
    return user;
  }

  private int pageSize(int requestedSize) {
    return requestedSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(requestedSize, MAX_PAGE_SIZE);
  }

  private String pairKey(VideoCatalogDeviceRow row) {
    return pairKey(row.getInstanceKey(), row.getDeviceId(), row.getTenantId());
  }

  private String pairKey(String instance, String serial, String tenant) {
    return Objects.toString(instance, "") + "\u0000" + Objects.toString(serial, "")
        + "\u0000" + Objects.toString(tenant, "");
  }

  private static Boolean bool(Object value) {
    if (value instanceof Boolean b) return b;
    if (value instanceof Number n) return n.intValue() == 0 ? Boolean.FALSE : n.intValue() == 1 ? Boolean.TRUE : null;
    if (value instanceof String s) {
      return switch (s.trim().toLowerCase(Locale.ROOT)) {
        case "1", "true", "yes", "supported", "on" -> Boolean.TRUE;
        case "0", "false", "no", "unsupported", "off" -> Boolean.FALSE;
        default -> null;
      };
    }
    return null;
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) if (StrUtil.isNotBlank(value)) return value;
    return null;
  }
}
