package cn.universal.admin.video.service;

import cn.hutool.json.JSONUtil;
import cn.universal.common.exception.BaseException;
import cn.universal.core.alarm.VideoAlarmPayload;
import cn.universal.dm.device.service.log.IIoTDeviceDataService;
import cn.universal.dm.device.service.log.HistoryPageSupport;
import cn.universal.ossm.oss.context.OssTenantContext;
import cn.universal.ossm.oss.factory.OssFactory;
import cn.universal.ossm.service.ISysOssService;
import cn.universal.ossm.util.VideoCloudEvidenceIndex;
import cn.universal.ossm.util.VideoCloudEvidenceService;
import cn.universal.ossm.util.VideoCloudImageDecoder;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.query.LogQuery;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Cloud alarm pages backed by the same cursor-based device event history used by logs. */
@Service
@RequiredArgsConstructor
public class VideoCloudEvidenceTimelineService {
  private final IIoTDeviceDataService logs;
  private final ISysOssService oss;
  private final VideoCloudEvidenceIndex indexes;
  private final VideoCloudEvidenceService evidence;
  private final List<VideoCloudImageDecoder> decoders;
  private final com.github.benmanes.caffeine.cache.Cache<String, java.util.concurrent.locks.ReentrantLock> retries =
      com.github.benmanes.caffeine.cache.Caffeine.newBuilder().maximumSize(1000)
          .expireAfterAccess(java.time.Duration.ofHours(1)).build();

  public Map<String, Object> timeline(IoTDevice device, Map<String, Object> body) {
    return timeline(device, null, body);
  }

  public Map<String, Object> timeline(IoTDevice device, IoTProduct product, Map<String, Object> body) {
    long end = number(body.get("endTime"), System.currentTimeMillis());
    long begin = number(body.get("startTime"), end - 86400000);
    if (begin < 0 || end < begin || end - begin > 31L * 86400000)
      throw new BaseException("请选择不超过31天的有效时间范围");
    int limit = (int) Math.min(200, Math.max(1, number(body.get("pageSize"), number(body.get("limit"), 50))));
    String channel = Objects.toString(body.get("channelId"), "");
    String eventType = Objects.toString(body.get("eventType"), "");
    boolean onlyEvidence = Boolean.parseBoolean(Objects.toString(body.get("onlyEvidence"), "false"));
    LogQuery query = LogQuery.builder().productKey(device.getProductKey()).iotId(device.getIotId())
        .deviceId(device.getDeviceId()).messageType("EVENT").beginCreateTime(begin / 1000)
        .endCreateTime(end / 1000).cursorMode(true).pageSize(limit)
        .cursor(Objects.toString(body.get("cursor"), "")).build();
    List<Map<String, Object>> events = new ArrayList<>();
    boolean more = false;
    String nextCursor = "";
    for (int page = 0; page < 10; page++) {
      var slice = logs.pageList(query);
      if (slice == null || slice.getList() == null) break;
      var archived = indexes.read(device.getTenantId(), device.getProductKey(), device.getDeviceId(),
          slice.getList().stream().map(row -> Objects.toString(row.getId(), "")).toList());
      for (int index = 0; index < slice.getList().size(); index++) {
        IoTDeviceLogVO row = slice.getList().get(index);
        Map<String, Object> payload;
        try { payload = eventData(row); }
        catch (RuntimeException e) { continue; }
        Map<String, Object> data = payload;
        mergeIndex(data, archived.get(Objects.toString(row.getId(), "")));
        if (!channel.isBlank() && !channel.equals(Objects.toString(data.get("channelId"), ""))) continue;
        if (!eventType.isBlank() && !eventType.equals(Objects.toString(data.get("cameraEventType"), row.getEvent()))) continue;
        var entry = event(row, data, device.getTenantId(), product);
        if (onlyEvidence && ((List<?>) entry.get("images")).isEmpty()) continue;
        events.add(entry);
        if (events.size() == limit) {
          more = index + 1 < slice.getList().size() || slice.isHasMore();
          nextCursor = more ? HistoryPageSupport.cursor(query, row.getCreateTime(), row.getId()) : "";
          return Map.of("events", events, "hasMore", more, "nextCursor", nextCursor,
              "total", events.size(), "truncated", more);
        }
      }
      more = slice.isHasMore();
      nextCursor = more ? Objects.toString(slice.getNextCursor(), "") : "";
      if (more && (nextCursor.isBlank() || nextCursor.equals(query.getCursor())))
        throw new BaseException("告警翻页位置无效，请重新查询");
      if (!more) break;
      query.setCursor(nextCursor);
    }
    // 限制单次扫描量，但仍返回接续游标，稀疏通道或图片筛选也能继续读取更早记录。
    return Map.of("events", events, "hasMore", more, "nextCursor", nextCursor,
        "total", events.size(), "truncated", more);
  }

  public Map<String, Object> retry(IoTDevice device, IoTProduct product, Map<String, Object> body) {
    if (!(body.get("evidenceIds") instanceof List<?> values) || values.isEmpty() || values.size() > 20)
      throw new BaseException("请指定1至20条待重试的取证记录");
    List<String> ids = values.stream().map(value -> Objects.toString(value, "")).distinct().toList();
    if (ids.stream().anyMatch(id -> !id.matches("[0-9]{1,20}"))) throw new BaseException("取证记录编号无效");
    String key = device.getTenantId() + ":" + device.getProductKey() + ":" + device.getDeviceId();
    var lock = retries.get(key, ignored -> new java.util.concurrent.locks.ReentrantLock());
    if (!lock.tryLock()) throw new BaseException("该设备已有取证重试任务，请稍后刷新");
    try {
      var archived = indexes.read(device.getTenantId(), device.getProductKey(), device.getDeviceId(), ids);
      var decoder = decoders.stream().filter(value -> value.supports(product.getThirdPlatform())).findFirst().orElse(null);
      List<Map<String, Object>> events = new ArrayList<>();
      int processed = 0, ready = 0;
      for (String id : ids) {
        var row = logs.queryById(LogQuery.builder().id(id).productKey(device.getProductKey())
            .iotId(device.getIotId()).deviceId(device.getDeviceId()).messageType("EVENT").build());
        if (row == null || !Objects.equals(device.getProductKey(), row.getProductKey())
            || !Objects.equals(device.getDeviceId(), row.getDeviceId()) || !"EVENT".equals(row.getMessageType()))
          throw new BaseException("取证记录不存在或不属于该设备");
        Map<String, Object> data = eventData(row);
        mergeIndex(data, archived.get(id));
        var before = evidenceState(data);
        int attempted = evidence.retry(device.getTenantId(), device.getProductKey(), device.getDeviceId(), data,
            decoder == null ? null : (url, bytes) -> decoder.decodeArchived(url, bytes, device, product), 20 - processed);
        if (attempted > 0 && !before.equals(evidenceState(data)))
          indexes.save(device.getTenantId(), device.getProductKey(), device.getDeviceId(), id, data);
        processed += attempted;
        var result = event(row, data, device.getTenantId(), product);
        if ("ready".equals(result.get("imageStatus"))) ready++;
        events.add(result);
        if (processed >= 20) break;
      }
      return Map.of("processed", events.size(), "resourceAttempts", processed, "ready", ready, "failed", events.size() - ready, "events", events);
    } finally { lock.unlock(); }
  }

  private Map<String, Object> eventData(IoTDeviceLogVO row) {
    Map<String, Object> payload = JSONUtil.parseObj(row.getContent());
    Object nested = payload.get("data");
    // New device logs preserve the uplink envelope. Old cloud logs store the event data directly;
    // those may themselves contain a vendor data object, so do not unwrap them a second time.
    if ("EVENT".equals(payload.get("messageType")) && payload.containsKey("event")
        && nested instanceof Map<?, ?> map) return VideoAlarmPayload.safeData(map);
    boolean eventData = payload.containsKey("cameraEventType") || payload.containsKey("evidence") || payload.containsKey("instanceKey");
    return !eventData && nested instanceof Map<?, ?> map ? VideoAlarmPayload.safeData(map) : VideoAlarmPayload.safeData(payload);
  }

  private void mergeIndex(Map<String, Object> data, Map<String, Object> index) {
    if (index != null) {
      data.put("evidence", index.get("evidence"));
      data.put("imageStatus", index.get("imageStatus"));
    }
  }

  private Map<String, Object> evidenceState(Map<String, Object> data) {
    Map<String, Object> state = new LinkedHashMap<>();
    for (String field : List.of("evidence", "imageStatus")) state.put(field, comparisonValue(data.get(field)));
    return state;
  }

  // Retry may mutate nested entries in place. Snapshot them without treating JSON map order or
  // equivalent numeric representations from the stored index as a new result.
  private Object comparisonValue(Object value) {
    if (value instanceof Map<?, ?> map) {
      Map<Object, Object> copy = new LinkedHashMap<>();
      map.forEach((key, entry) -> copy.put(key, comparisonValue(entry)));
      return copy;
    }
    if (value instanceof Collection<?> values) return values.stream().map(this::comparisonValue).toList();
    if (value instanceof Number number) {
      try { return new BigDecimal(number.toString()).stripTrailingZeros(); }
      catch (NumberFormatException ignored) { /* Preserve non-finite values if supplied by a decoder. */ }
    }
    return value;
  }

  private Map<String, Object> event(IoTDeviceLogVO row, Map<String, Object> data, String tenant, IoTProduct product) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("evidenceId", Objects.toString(row.getId(), ""));
    result.put("kind", "alarm");
    long time = number(data.get("occurredAt"), 0);
    if (time == 0 && row.getCreateTime() != null) time = row.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    result.put("alarmTime", time);
    String eventId = Objects.toString(data.get("cameraEventType"), row.getEvent());
    String eventName = Objects.toString(data.get("cameraEventName"), row.getEvent());
    result.put("cameraEventType", eventId);
    result.put("cameraEventName", product == null ? eventName
        : cn.universal.core.video.CameraThingModel.eventName(product.getMetadata(), eventId, eventName));
    result.put("alarmState", Objects.toString(data.get("alarmState"), "pulse"));
    result.put("channelId", Objects.toString(data.get("channelId"), ""));
    result.put("recordingStatus", "none");
    List<Map<String, Object>> images = new ArrayList<>();
    String status = Objects.toString(data.get("imageStatus"), "none");
    String message = "";
    if (data.get("evidence") instanceof List<?> entries) {
      for (Object value : entries) {
        if (!(value instanceof Map<?, ?> entry)) continue;
        if (message.isBlank()) message = Objects.toString(entry.get("message"), "");
        if (!"ready".equals(entry.get("status"))) continue;
        try {
          var file = oss.getById(Long.valueOf(Objects.toString(entry.get("ossId"), "")));
          if (file == null || !Objects.equals(tenant, file.getTenantId())) continue;
          String url = OssTenantContext.run(tenant,
              () -> OssFactory.instance(file.getService()).signedReadUrl(VideoCloudEvidenceService.objectKey(file), 600));
          images.add(Map.of("snapshotUrl", url, "imageSource", "event"));
        } catch (RuntimeException e) { status = "failed"; message = "已归档图片暂时无法读取"; }
      }
    } else if (!VideoAlarmPayload.evidence(data).isEmpty()) {
      status = "pending"; message = "历史图片尚未归档，可重试补采仍有效的厂家地址";
    }
    result.put("images", images);
    result.put("imageStatus", images.isEmpty() ? status : "ready");
    result.put("imageMessage", message);
    result.put("retryable", images.isEmpty() && (!"none".equals(status)));
    result.put("data", publicData(data));
    return result;
  }

  private Map<String, Object> publicData(Map<?, ?> source) {
    Map<String, Object> result = new LinkedHashMap<>();
    source.forEach((key, value) -> {
      String name = Objects.toString(key, "");
      if (name.equals("evidence") || name.toLowerCase(java.util.Locale.ROOT).contains("url")) return;
      result.put(name, publicValue(value));
    });
    return result;
  }

  private Object publicValue(Object value) {
    if (value instanceof Map<?, ?> map) return publicData(map);
    if (value instanceof List<?> list) return list.stream().map(this::publicValue).toList();
    if (value instanceof String text && (text.startsWith("https://") || text.startsWith("http://"))) return "[图片来源地址已隐藏]";
    return value;
  }

  private static long number(Object value, long fallback) {
    try { return value == null ? fallback : Long.parseLong(value.toString()); }
    catch (NumberFormatException e) { return fallback; }
  }
}
