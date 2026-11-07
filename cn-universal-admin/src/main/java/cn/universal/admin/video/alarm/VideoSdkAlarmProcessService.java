package cn.universal.admin.video.alarm;

import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.*;
import cn.universal.persistence.mapper.*;
import java.time.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Durable motion projection. Callback UUIDs identify members, never motion processes. */
@Service
public class VideoSdkAlarmProcessService {
  public static final int GAP_SECONDS = 30;
  private static final long GAP = GAP_SECONDS * 1000L;
  private final VideoSdkAlarmProcessMapper processes;
  private final VideoSdkAlarmProcessMemberMapper members;
  private final TransactionTemplate transactions;

  public VideoSdkAlarmProcessService(
      VideoSdkAlarmProcessMapper processes,
      VideoSdkAlarmProcessMemberMapper members,
      PlatformTransactionManager manager) {
    this.processes = processes;
    this.members = members;
    this.transactions = new TransactionTemplate(manager);
    transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transactions.setTimeout(15);
  }

  public String record(
      VideoPlatformInstance instance,
      VideoSdkAlarmReceipt receipt,
      SdkVideoAlarmCallbackRequest request) {
    requireScope(instance, receipt, request);
    if ("evidence_ready".equals(request.getAlarmType())) {
      if (request.getSourceEventId() == null || request.getSourceEventId().isBlank()) return null;
      var originals =
          members.original(
              receipt.getTenantId(),
              receipt.getInstanceKey(),
              receipt.getDeviceId(),
              receipt.getChannelId(),
              request.getSourceEventId());
      var ids =
          originals.stream().map(VideoSdkAlarmProcessMember::getProcessId).distinct().toList();
      return ids.size() == 1 ? ids.getFirst() : null;
    }
    if (!"motion".equals(request.getAlarmType())
        || receipt.getChannelId().isBlank()
        || "gateway".equals(request.getSdkEventFamily())
        || request.getData() != null
            && (request.getData().containsKey("acceptanceFixture")
                || Boolean.TRUE.equals(request.getData().get("synthetic")))) return null;
    String guardId =
        guardId(
            receipt.getTenantId(),
            receipt.getInstanceKey(),
            receipt.getDeviceId(),
            receipt.getChannelId());
    // A racing insert must roll back its own transaction before the main transaction:
    // catching DuplicateKeyException inside PostgreSQL's transaction leaves it aborted.
    if (processes.selectByPrimaryKey(guardId) == null)
      try {
        transactions.executeWithoutResult(
            status -> {
              if (processes.selectByPrimaryKey(guardId) == null)
                processes.insert(guard(receipt, guardId));
            });
      } catch (DuplicateKeyException race) {
        /* The committed guard will be locked below. */
      }
    return transactions.execute(
        status -> {
          VideoSdkAlarmProcess guard = processes.lock(guardId);
          if (guard == null) throw new BaseException("告警过程范围锁不可用");
          var existing = members.selectByPrimaryKey(receipt.getId());
          if (existing != null) {
            requireMemberScope(existing, receipt);
            return existing.getProcessId();
          }
          long received = receipt.getCreatedAt(),
              alarm =
                  request.getAlarmTime() != null && request.getAlarmTime() > 0
                      ? request.getAlarmTime()
                      : received;
          String state = normalizeState(request.getAlarmState());
          VideoSdkAlarmProcess current =
              guard.getCurrentProcessId() == null
                  ? null
                  : processes.selectByPrimaryKey(guard.getCurrentProcessId());
          if (current != null
              && (!"process".equals(current.getRowKind())
                  || !Objects.equals(current.getTenantId(), receipt.getTenantId())
                  || !Objects.equals(current.getInstanceKey(), receipt.getInstanceKey())
                  || !Objects.equals(current.getDeviceId(), receipt.getDeviceId())
                  || !Objects.equals(current.getChannelId(), receipt.getChannelId())))
            throw new BaseException("告警过程范围指针不匹配");
          if (current != null
              && Set.of("active", "unknown").contains(current.getState())
              && received - current.getLastReceivedAt() > GAP) {
            closeGap(current);
            processes.updateByPrimaryKey(current);
            current = null;
          }
          if (current != null
              && "unknown_gap".equals(current.getState())
              && received - current.getLastReceivedAt() > GAP) current = null;
          if (current != null
              && !Set.of("active", "unknown", "unknown_gap").contains(current.getState()))
            current = null;
          boolean historical =
              current != null
                  && (received < current.getFirstReceivedAt() - GAP
                      || "stop".equals(state) && received < current.getLastReceivedAt());
          if (historical) current = null;
          boolean create = current == null;
          if (create) current = process(receipt, alarm, received, state);
          merge(current, request.getEventId(), alarm, received, state);
          if (historical && !"stop".equals(state)) {
            current.setState("unknown_gap");
            current.setClosedAt(received + GAP);
            current.setCloseReason("late_out_of_window");
          }
          if (create) processes.insert(current);
          else processes.updateByPrimaryKey(current);
          var member = new VideoSdkAlarmProcessMember();
          member.setReceiptId(receipt.getId());
          member.setProcessId(current.getId());
          member.setTenantId(receipt.getTenantId());
          member.setInstanceKey(receipt.getInstanceKey());
          member.setDeviceId(receipt.getDeviceId());
          member.setChannelId(receipt.getChannelId());
          member.setEventId(request.getEventId());
          member.setSourceEventId(bounded(request.getSourceEventId(), 256));
          member.setEvidenceId(evidenceId(request));
          member.setAlarmState(state);
          member.setAlarmTime(alarm);
          member.setReceivedAt(received);
          member.setActivePostCount(
              request.getActivePostCount() != null && request.getActivePostCount() >= 0
                  ? request.getActivePostCount().longValue()
                  : null);
          members.insert(member);
          if (!historical) {
            guard.setCurrentProcessId(current.getId());
            guard.setUpdatedAt(received);
            processes.updateByPrimaryKey(guard);
          }
          return current.getId();
        });
  }

  @Scheduled(initialDelay = 5000, fixedDelay = 5000)
  public void expire() {
    for (var candidate : processes.expired(System.currentTimeMillis() - GAP))
      transactions.executeWithoutResult(
          status -> {
            var guard =
                processes.lock(
                    guardId(
                        candidate.getTenantId(),
                        candidate.getInstanceKey(),
                        candidate.getDeviceId(),
                        candidate.getChannelId()));
            if (guard == null || !candidate.getId().equals(guard.getCurrentProcessId())) return;
            var current = processes.selectByPrimaryKey(candidate.getId());
            if (current != null
                && Set.of("active", "unknown").contains(current.getState())
                && current.getLastReceivedAt() < System.currentTimeMillis() - GAP) {
              closeGap(current);
              processes.updateByPrimaryKey(current);
              guard.setUpdatedAt(System.currentTimeMillis());
              processes.updateByPrimaryKey(guard);
            }
          });
  }

  public Map<String, Object> list(VideoPlatformInstance instance, Map<String, String> parameters) {
    String device = bounded(parameters.get("deviceId"), 100);
    if (device == null || device.isBlank()) throw new BaseException("查询告警过程必须指定设备");
    long start = time(parameters.get("startTime"), System.currentTimeMillis() - 86400000),
        end = time(parameters.get("endTime"), System.currentTimeMillis());
    if (end <= start || end - start > 7 * 86400000L) throw new BaseException("告警过程查询范围需在7天以内");
    String state = parameters.getOrDefault("state", "all");
    if (!Set.of("all", "active", "unknown", "ended", "unknown_gap").contains(state))
      throw new BaseException("告警过程状态无效");
    int page = positive(parameters.get("page"), 1, 1000000),
        size = positive(parameters.get("pageSize"), 50, 200);
    Map<String, Object> scope = new HashMap<>();
    scope.put("tenant", instance.getTenantId());
    scope.put("instance", instance.getInstanceKey());
    scope.put("device", device);
    scope.put("channel", bounded(parameters.get("channelId"), 100));
    scope.put("start", start);
    scope.put("end", end);
    scope.put("state", state);
    return page(
        processes.page(scope, size, (page - 1L) * size).stream()
            .map(VideoSdkAlarmProcessService::view)
            .toList(),
        processes.count(scope),
        page,
        size);
  }

  public Map<String, Object> events(
      VideoPlatformInstance instance, String processId, Map<String, String> parameters) {
    var process = processes.selectByPrimaryKey(processId);
    if (process == null
        || !"process".equals(process.getRowKind())
        || !Objects.equals(instance.getTenantId(), process.getTenantId())
        || !instance.getInstanceKey().equals(process.getInstanceKey()))
      throw new BaseException("告警过程不存在或无权访问");
    int page = positive(parameters.get("page"), 1, 1000000),
        size = positive(parameters.get("pageSize"), 50, 200);
    return page(
        members
            .page(
                instance.getTenantId(),
                instance.getInstanceKey(),
                processId,
                size,
                (page - 1L) * size)
            .stream()
            .map(VideoSdkAlarmProcessService::view)
            .toList(),
        members.count(instance.getTenantId(), instance.getInstanceKey(), processId),
        page,
        size);
  }

  private static Map<String, Object> page(List<?> rows, long total, int page, int size) {
    return Map.of(
        "rows", rows, "total", total, "page", page, "pageSize", size, "gapSeconds", GAP_SECONDS);
  }

  private static Map<String, Object> view(VideoSdkAlarmProcess p) {
    var v = new LinkedHashMap<String, Object>();
    v.put("processId", p.getId());
    v.put("deviceId", p.getDeviceId());
    v.put("channelId", p.getChannelId());
    v.put("firstTime", p.getFirstTime());
    v.put("lastTime", p.getLastTime());
    v.put("count", p.getEventCount());
    v.put("firstEventId", p.getFirstEventId());
    v.put("lastEventId", p.getLastEventId());
    v.put("state", p.getState());
    v.put("startObserved", p.getStartObserved() == 1);
    v.put("stopObserved", p.getStopObserved() == 1);
    if (p.getClosedAt() > 0) v.put("closedAt", p.getClosedAt());
    v.put("closeReason", p.getCloseReason());
    return v;
  }

  private static Map<String, Object> view(VideoSdkAlarmProcessMember m) {
    var v = new LinkedHashMap<String, Object>();
    v.put("receiptId", m.getReceiptId());
    v.put("eventId", m.getEventId());
    if (m.getSourceEventId() != null) v.put("sourceEventId", m.getSourceEventId());
    if (m.getEvidenceId() != null) v.put("evidenceId", m.getEvidenceId());
    v.put("alarmTime", m.getAlarmTime());
    v.put("receivedAt", m.getReceivedAt());
    v.put("state", m.getAlarmState());
    if (m.getActivePostCount() != null) v.put("activePostCount", m.getActivePostCount());
    return v;
  }

  private static VideoSdkAlarmProcess guard(VideoSdkAlarmReceipt r, String id) {
    var p = process(r, 0, 0, "unknown");
    p.setId(id);
    p.setRowKind("guard");
    p.setState("guard");
    p.setFirstEventId("");
    p.setLastEventId("");
    return p;
  }

  private static VideoSdkAlarmProcess process(
      VideoSdkAlarmReceipt r, long alarm, long received, String state) {
    var p = new VideoSdkAlarmProcess();
    p.setId(VideoSdkAlarmReceiptService.digest("process\u001f" + r.getId()));
    p.setTenantId(r.getTenantId());
    p.setInstanceKey(r.getInstanceKey());
    p.setDeviceId(r.getDeviceId());
    p.setChannelId(r.getChannelId());
    p.setEventKind("motion");
    p.setRowKind("process");
    p.setState("unknown");
    p.setFirstTime(alarm);
    p.setLastTime(alarm);
    p.setFirstReceivedAt(received);
    p.setLastReceivedAt(received);
    p.setEventCount(0L);
    p.setFirstEventId(r.getEventId());
    p.setLastEventId(r.getEventId());
    p.setStartObserved(0);
    p.setStopObserved(0);
    p.setClosedAt(0L);
    p.setCloseReason("");
    p.setCreatedAt(r.getCreatedAt());
    p.setUpdatedAt(r.getCreatedAt());
    return p;
  }

  private static void merge(
      VideoSdkAlarmProcess p, String event, long alarm, long received, String state) {
    if (alarm < p.getFirstTime()) {
      p.setFirstTime(alarm);
      p.setFirstEventId(event);
    }
    if (alarm >= p.getLastTime()) {
      p.setLastTime(alarm);
      p.setLastEventId(event);
    }
    p.setFirstReceivedAt(Math.min(p.getFirstReceivedAt(), received));
    p.setLastReceivedAt(Math.max(p.getLastReceivedAt(), received));
    p.setEventCount(p.getEventCount() + 1);
    p.setUpdatedAt(Math.max(p.getUpdatedAt(), received));
    if ("start".equals(state)) {
      p.setStartObserved(1);
      p.setState("active");
      p.setClosedAt(0L);
      p.setCloseReason("");
    }
    if ("stop".equals(state)) {
      p.setStopObserved(1);
      p.setState("ended");
      p.setClosedAt(alarm);
      p.setCloseReason("device_inactive");
    }
  }

  private static void closeGap(VideoSdkAlarmProcess p) {
    p.setState("unknown_gap");
    p.setClosedAt(p.getLastReceivedAt() + GAP);
    p.setCloseReason("no_inactive_observed");
    p.setUpdatedAt(System.currentTimeMillis());
  }

  private static String guardId(String tenant, String instance, String device, String channel) {
    return VideoSdkAlarmReceiptService.digest(
        String.join("\u001f", "motion-scope", tenant, instance, device, channel));
  }

  private static String normalizeState(String state) {
    return switch (Objects.toString(state, "").toLowerCase(Locale.ROOT)) {
      case "start", "active" -> "start";
      case "stop", "inactive" -> "stop";
      default -> "unknown";
    };
  }

  private static String evidenceId(SdkVideoAlarmCallbackRequest r) {
    Object id = r.getData() == null ? null : r.getData().get("evidenceId");
    String text = Objects.toString(id, "");
    if (text.matches("[a-f0-9-]{36}")) return text;
    return r.getEventId() != null && r.getEventId().matches("[a-f0-9-]{36}")
        ? r.getEventId()
        : null;
  }

  private static String bounded(String text, int limit) {
    if (text != null && (text.length() > limit || text.chars().anyMatch(c -> c < 32 || c == 127)))
      throw new BaseException("告警过程标识无效");
    return text;
  }

  private static int positive(String value, int fallback, int max) {
    try {
      int number = value == null ? fallback : Integer.parseInt(value);
      if (number < 1 || number > max) throw new IllegalArgumentException();
      return number;
    } catch (RuntimeException invalid) {
      throw new BaseException("告警过程分页参数无效");
    }
  }

  private static long time(String value, long fallback) {
    if (value == null || value.isBlank()) return fallback;
    try {
      if (value.matches("[0-9]+")) {
        long number = Long.parseLong(value);
        return number < 100000000000L ? number * 1000 : number;
      }
      return LocalDateTime.parse(value.replace(' ', 'T'))
          .atZone(ZoneId.of("Asia/Shanghai"))
          .toInstant()
          .toEpochMilli();
    } catch (RuntimeException invalid) {
      throw new BaseException("告警过程查询时间无效");
    }
  }

  private static void requireScope(
      VideoPlatformInstance instance,
      VideoSdkAlarmReceipt r,
      SdkVideoAlarmCallbackRequest request) {
    if (instance == null
        || r == null
        || request == null
        || r.getCreatedAt() == null
        || r.getCreatedAt() < 0
        || r.getTenantId() == null
        || !Objects.equals(instance.getTenantId(), r.getTenantId())
        || !Objects.equals(instance.getInstanceKey(), r.getInstanceKey())
        || !Objects.equals(r.getInstanceKey(), request.getInstanceKey())
        || !Objects.equals(r.getDeviceId(), request.getDeviceId())
        || !Objects.equals(r.getChannelId(), Objects.toString(request.getChannelId(), ""))
        || !Objects.equals(r.getEventId(), request.getEventId()))
      throw new BaseException("告警过程收据范围不匹配");
  }

  private static void requireMemberScope(
      VideoSdkAlarmProcessMember member, VideoSdkAlarmReceipt receipt) {
    if (!Objects.equals(member.getTenantId(), receipt.getTenantId())
        || !Objects.equals(member.getInstanceKey(), receipt.getInstanceKey())
        || !Objects.equals(member.getDeviceId(), receipt.getDeviceId())
        || !Objects.equals(member.getChannelId(), receipt.getChannelId()))
      throw new BaseException("告警过程成员范围不匹配");
  }
}
