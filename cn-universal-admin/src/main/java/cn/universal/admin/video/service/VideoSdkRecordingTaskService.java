package cn.universal.admin.video.service;

import cn.universal.common.exception.BaseException;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.persistence.entity.*;
import cn.universal.persistence.mapper.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Database leases let multiple backend nodes track the same gateway without double submission. */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoSdkRecordingTaskService {
  private static final Set<String> TERMINAL = Set.of("ready", "partial", "failed", "cancelled", "expired", "interrupted");
  private final VideoSdkRecordTaskMapper tasks;
  private final VideoPlatformInstanceMapper instances;
  private final VideoPlatformDeviceMapper devices;
  private final VideoPlatformChannelMapper channels;
  private final VideoPlatformAdapterRegistry adapters;
  private final ObjectMapper json;
  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private org.springframework.beans.factory.ObjectProvider<cn.universal.dm.video.zlm.VideoZlmStreamProxyService> media;
  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private org.springframework.beans.factory.ObjectProvider<cn.universal.admin.video.alarm.SdkVideoAlarmUpService> alarmPublisher;
  @org.springframework.beans.factory.annotation.Autowired(required=false)
  private VideoPlatformDeviceExtMapper deviceExtensions;
  private final String owner = UUID.randomUUID().toString();
  private final ExecutorService workers = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS,
      new SynchronousQueue<>(), r -> { Thread t = new Thread(r, "sdk-record-task"); t.setDaemon(true); return t; });

  public synchronized Map<String, Object> create(VideoPlatformInstance instance, Map<String, Object> input, String creator) {
    requireSdk(instance);
    if (!Integer.valueOf(1).equals(instance.getEnabled())) throw new BaseException("实例已停用");
    String id = Objects.toString(input.get("requestId"), UUID.randomUUID().toString());
    if (!id.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")) throw new BaseException("任务请求标识无效");
    String deviceId = Objects.toString(input.get("deviceId"), ""), channelId = Objects.toString(input.get("channelId"), "");
    String mode = Objects.toString(input.get("mode"), "export");
    if (!Set.of("instant", "export", "platform").contains(mode)) throw new BaseException("录像任务类型无效");
    if ("platform".equals(mode) && !"hik_isup".equals(instance.getPlatformType())) throw new BaseException("此接入方式尚未支持平台直播录像");
    long from, to;
    if ("platform".equals(mode)) {
      Object value = input.get("durationSeconds");
      if (!(value instanceof Number number) || number.doubleValue() != number.longValue()
          || number.longValue() < 1 || number.longValue() > 7200) throw new BaseException("平台录像时长需为1到7200秒的整数");
      VideoSdkRecordTask previous = tasks.selectByPrimaryKey(id);
      from = previous == null ? System.currentTimeMillis() : previous.getStartTime();
      to = from + number.longValue() * 1000;
    } else { from = timestamp(input.get("startTime")); to = timestamp(input.get("endTime")); }
    if (to - from < 1000 || to - from > 7200000) throw new BaseException("单次录像任务需在 1 秒到 2 小时之间");
    VideoSdkRecordTask existing = tasks.selectByPrimaryKey(id);
    if (existing != null) {
      assertScope(instance, existing);
      assertParameters(existing, deviceId, channelId, mode, from, to);
      return view(existing);
    }
    var deviceProbe = new VideoPlatformDevice(); deviceProbe.setInstanceKey(instance.getInstanceKey()); deviceProbe.setDeviceId(deviceId);
    var device = devices.selectOne(deviceProbe);
    if (device == null || Integer.valueOf(0).equals(device.getEnabled())) throw new BaseException("设备不存在或已停用");
    var channelProbe = new VideoPlatformChannel(); channelProbe.setInstanceKey(instance.getInstanceKey());
    channelProbe.setPlatformDeviceId(deviceId); channelProbe.setChannelId(channelId);
    if (channels.selectCount(channelProbe) == 0) throw new BaseException("设备通道不存在");
    if (tasks.active(tenant(instance), instance.getInstanceKey()) >= 100) throw new BaseException("此实例已有 100 个未完成任务，请稍后再提交");
    VideoSdkRecordTask task = new VideoSdkRecordTask();
    task.setId(id); task.setTenantId(tenant(instance)); task.setInstanceKey(instance.getInstanceKey());
    task.setDeviceId(deviceId); task.setChannelId(channelId); task.setMode(mode); task.setStartTime(from); task.setEndTime(to);
    String name = Objects.toString(input.get("taskName"), "").trim();
    if (name.isBlank()) name = Objects.toString(device.getDeviceName(), deviceId) + " / " + channelId;
    task.setTaskName(name.substring(0, Math.min(180, name.length())));
    task.setStatus("pending"); task.setProgress(0); task.setMessage("等待网关接收，关闭页面不影响任务");
    task.setCreatedBy(creator); task.setCreatedAt(System.currentTimeMillis()); task.setUpdatedAt(task.getCreatedAt());
    task.setNextPollAt(0L); task.setLeaseUntil(0L); task.setLeaseOwner(""); task.setCancelRequested(0);
    if (input.get("sourceEventId") instanceof String source && !source.isBlank()) {
      try { task.setResultJson(json.writeValueAsString(Map.of("sourceEventId", source,
          "sourceEventType", Objects.toString(input.get("sourceEventType"), "motion"), "alarmTime", input.getOrDefault("alarmTime", from)))); }
      catch (Exception ex) { throw new BaseException("告警片段任务关联数据无效"); }
      task.setNextPollAt(Math.max(0, to + 2000));
    }
    try { tasks.insert(task); }
    catch (org.springframework.dao.DuplicateKeyException ex) {
      existing = tasks.selectByPrimaryKey(id); assertScope(instance, existing);
      assertParameters(existing, deviceId, channelId, mode, from, to); return view(existing);
    }
    return view(task);
  }

  public Map<String, Object> list(VideoPlatformInstance instance, String status, int page, int size) {
    requireSdk(instance); size = Math.max(1, Math.min(100, size)); page = Math.max(1, Math.min(100000, page));
    return Map.of("records", tasks.page(tenant(instance), instance.getInstanceKey(), status, size, (page - 1) * size).stream().map(this::view).toList(),
        "total", tasks.count(tenant(instance), instance.getInstanceKey(), status), "activeCount", tasks.active(tenant(instance), instance.getInstanceKey()));
  }
  public Map<String, Object> get(VideoPlatformInstance instance, String id) { return view(requireTask(instance, id)); }
  public Map<String, Object> cancel(VideoPlatformInstance instance, String id) {
    requireTask(instance, id);
    tasks.requestCancel(id, tenant(instance), instance.getInstanceKey());
    return get(instance, id);
  }
  public Map<String, Object> retry(VideoPlatformInstance instance, String id, String requestId, String creator) {
    var task = requireTask(instance, id);
    if (!TERMINAL.contains(task.getStatus())) throw new BaseException("任务尚未结束，请等待或取消后再重试");
    Map<String, Object> input = new LinkedHashMap<>(Map.of("requestId", requestId, "deviceId", task.getDeviceId(), "channelId", task.getChannelId(),
        "mode", task.getMode(), "startTime", task.getStartTime(), "endTime", task.getEndTime(), "taskName", task.getTaskName()));
    if ("platform".equals(task.getMode())) input.put("durationSeconds", (task.getEndTime() - task.getStartTime()) / 1000);
    return create(instance, input, creator);
  }
  private VideoSdkRecordTask requireTask(VideoPlatformInstance instance, String id) {
    requireSdk(instance); var task = tasks.selectByPrimaryKey(id); assertScope(instance, task); return task;
  }
  private static void assertParameters(VideoSdkRecordTask existing, String device, String channel, String mode, long from, long to) {
    if (!Objects.equals(existing.getDeviceId(), device) || !Objects.equals(existing.getChannelId(), channel)
        || !Objects.equals(existing.getMode(), mode) || existing.getStartTime() != from || existing.getEndTime() != to)
      throw new BaseException("任务请求标识已被其他录像参数使用");
  }
  private static void assertScope(VideoPlatformInstance instance, VideoSdkRecordTask task) {
    if (instance == null || task == null || !Objects.equals(task.getInstanceKey(), instance.getInstanceKey()) || !Objects.equals(task.getTenantId(), tenant(instance)))
      throw new BaseException("录像任务不存在或无权访问");
  }
  private static String tenant(VideoPlatformInstance instance) {
    return instance.getTenantId() == null || instance.getTenantId().isBlank() ? instance.getCreatorId() : instance.getTenantId();
  }
  private static void requireSdk(VideoPlatformInstance instance) {
    if (instance == null || !cn.universal.dm.video.VideoSdkPlatforms.managed(instance.getPlatformType())) throw new BaseException("此实例不支持 SDK 后台录像任务");
  }
  static long timestamp(Object value) {
    if (value == null) throw new BaseException("请指定录像起止时间");
    try {
      String s = value.toString();
      if (s.matches("[0-9]{10,13}")) { long n = Long.parseLong(s); return n < 100000000000L ? n * 1000 : n; }
      return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
    } catch (Exception ex) { throw new BaseException("录像时间格式无效"); }
  }

  @Scheduled(initialDelay = 5000, fixedDelay = 2000)
  public void tick() {
    try {
      for (var task : tasks.dueForPlatforms(System.currentTimeMillis(), adapters.getSupportedPlatformTypes())) {
        try { workers.execute(() -> poll(task.getId())); }
        catch (RejectedExecutionException ignored) { break; }
      }
    } catch (Exception ex) { log.warn("录像任务调度暂不可用：{}", ex.getClass().getSimpleName()); }
  }
  /** Do not let a node without the required protocol module acquire a shared task lease. */
  private boolean canPoll(String id) {
    VideoSdkRecordTask candidate = tasks.selectByPrimaryKey(id);
    if (candidate == null) return false;
    var probe = new VideoPlatformInstance(); probe.setInstanceKey(candidate.getInstanceKey());
    var instance = instances.selectOne(probe);
    return instance == null || adapters.isSupported(instance.getPlatformType());
  }
  void poll(String id) {
    if (!canPoll(id)) return;
    long now = System.currentTimeMillis();
    if (tasks.claim(id, owner, now, now + 120000) != 1) return;
    VideoSdkRecordTask task = tasks.selectByPrimaryKey(id);
    if (task == null) return;
    try {
      if (TERMINAL.contains(task.getStatus()) && !("instant".equals(task.getMode()) && "ready".equals(task.getStatus()))) return;
      var probe = new VideoPlatformInstance(); probe.setInstanceKey(task.getInstanceKey());
      var instance = instances.selectOne(probe);
      if (instance == null || !Objects.equals(tenant(instance), task.getTenantId())) { fail(task, "所属实例已删除或变更"); return; }
      if (!Integer.valueOf(1).equals(instance.getEnabled())) { fail(task, "实例已停用"); return; }
      var adapter = adapters.getAdapter(instance.getPlatformType());
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("deviceId", task.getDeviceId()); payload.put("channelId", task.getChannelId());
      payload.put("startTime", task.getStartTime()); payload.put("endTime", task.getEndTime());
      payload.put("playbackMode", task.getMode());
      payload.put("requestId", task.getId()); payload.put("jobId", task.getId());
      if ("platform".equals(task.getMode())) { payload.put("source", "live"); payload.put("durationSeconds", (task.getEndTime() - task.getStartTime()) / 1000); }
      boolean cancelling = Integer.valueOf(1).equals(task.getCancelRequested());
      if (cancelling && task.getGatewayJobId() == null) { task.setStatus("cancelled"); task.setMessage("已取消"); return; }
      Map<String, Object> result;
      if (cancelling) result = adapter.executeOperation(instance, "recordingCancel", payload);
      else {
        if (task.getGatewayJobId() == null) {
          tasks.markDispatch(id, owner); task.setGatewayJobId(id);
          result = adapter.executeOperation(trustedRecordingInstance(instance,task), startOperation(task), payload);
        } else {
          try { result = adapter.executeOperation(instance, "recordingStatus", payload); }
          catch (RuntimeException ex) {
            if (ex.getMessage() == null || !ex.getMessage().contains("录像任务不存在")) throw ex;
            // Start is idempotent by requestId, including a lost submit response.
            result = adapter.executeOperation(trustedRecordingInstance(instance,task), startOperation(task), payload);
          }
        }
      }
      task.setStatus(Objects.toString(result.get("status"), "pending"));
      task.setProgress(result.get("progress") instanceof Number n ? n.intValue() : 0);
      task.setMessage(Objects.toString(result.get("message"), ""));
      Map<String, Object> combined = task.getResultJson() == null ? new LinkedHashMap<>() : json.readValue(task.getResultJson(), new TypeReference<LinkedHashMap<String,Object>>() {});
      combined.putAll(result);
      if (Set.of("ready", "partial").contains(task.getStatus()) && combined.get("sourceEventId") != null && !Boolean.TRUE.equals(combined.get("evidencePublished"))) {
        // A ready file still needs a durable evidence receipt. Keep it schedulable if publication fails.
        String mediaStatus = task.getStatus();
        task.setStatus("awaiting_evidence");
        task.setResultJson(json.writeValueAsString(combined));
        var publisher = alarmPublisher == null ? null : alarmPublisher.getIfAvailable();
        if (publisher == null) throw new BaseException("告警片段取证上行服务不可用");
        publisher.publishRecordingEvidence(instance, task, combined, result);
        combined.put("evidencePublished", true);
        task.setStatus(mediaStatus);
      }
      task.setResultJson(json.writeValueAsString(combined));
      task.setNextPollAt(now + (Set.of("ready", "partial").contains(task.getStatus()) ? 30000 : 2000));
    } catch (Exception ex) {
      if (now - task.getCreatedAt() > 3600000) fail(task, "任务超过 1 小时未完成，请检查网关后重试");
      else {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        task.setMessage("等待重试：" + message.substring(0, Math.min(950, message.length())));
        task.setNextPollAt(now + 10000);
      }
    } finally {
      task.setUpdatedAt(System.currentTimeMillis());
      tasks.finishPoll(task, owner);
    }
  }
  private static void fail(VideoSdkRecordTask task, String message) { task.setStatus("failed"); task.setMessage(message); }
  private static String startOperation(VideoSdkRecordTask task) {
    return "platform".equals(task.getMode()) ? "platformRecordingStart" : "instant".equals(task.getMode()) ? "playbackStart" : "recordingStart";
  }
  private VideoPlatformInstance trustedRecordingInstance(VideoPlatformInstance instance,VideoSdkRecordTask task) throws Exception {
    if (!"hik_isup".equals(instance.getPlatformType())) return instance;
    var service = media == null ? null : media.getIfAvailable();
    if (service == null) throw new BaseException("尚未接入平台录像媒体服务");
    Map<String, Object> options = instance.getOptions() == null ? new LinkedHashMap<>()
        : json.readValue(instance.getOptions(), new TypeReference<LinkedHashMap<String,Object>>() {});
    String node = Objects.toString(options.getOrDefault("mediaServerId", options.getOrDefault("zlmMediaServerId", options.get("previewMediaServerId"))), "");
    if(deviceExtensions!=null) {
      VideoPlatformDevice device=devices.selectOne(VideoPlatformDevice.builder().instanceKey(instance.getInstanceKey()).deviceId(task.getDeviceId()).build());
      if(device!=null){VideoPlatformDeviceExt probe=new VideoPlatformDeviceExt();probe.setDeviceId(device.getId());probe.setInstanceKey(instance.getInstanceKey());
        VideoPlatformDeviceExt extension=deviceExtensions.selectOne(probe);
        if(extension!=null&&extension.getMediaServerId()!=null&&!extension.getMediaServerId().isBlank())node=extension.getMediaServerId();}
    }
    Map<String, Object> target = service.recordingTarget(instance.getTenantId(), node);
    VideoPlatformInstance trusted = json.convertValue(instance, VideoPlatformInstance.class);
    options.putAll(target); trusted.setOptions(json.writeValueAsString(options));
    return trusted;
  }
  private Map<String, Object> view(VideoSdkRecordTask task) {
    Map<String, Object> result = new LinkedHashMap<>();
    if (task.getResultJson() != null) try { result.putAll(json.readValue(task.getResultJson(), new TypeReference<Map<String, Object>>() {})); } catch (Exception ignored) { }
    result.put("id", task.getId()); result.put("taskName", task.getTaskName()); result.put("instanceKey", task.getInstanceKey());
    result.put("deviceId", task.getDeviceId()); result.put("channelId", task.getChannelId()); result.put("mode", task.getMode());
    result.put("startTime", task.getStartTime()); result.put("endTime", task.getEndTime()); result.put("status", task.getStatus());
    result.put("progress", task.getProgress()); result.put("message", task.getMessage());
    result.put("createdAt", task.getCreatedAt()); result.put("updatedAt", task.getUpdatedAt());
    result.put("cancelRequested", Integer.valueOf(1).equals(task.getCancelRequested()));
    if (result.get("expiresAt") instanceof Number n && n.longValue() < System.currentTimeMillis() && "ready".equals(task.getStatus())) {
      result.put("status", "expired"); result.put("message", "即时回放链接已过期，可重新执行或导出 MP4"); result.remove("hlsUrl");
    }
    return result;
  }
  @PreDestroy public void stop() { workers.shutdownNow(); }
}
