package cn.universal.admin.video.alarm;

import cn.universal.common.exception.BaseException;
import cn.universal.core.alarm.VideoAlarmPayload;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.entity.VideoSdkAlarmReceipt;
import cn.universal.persistence.mapper.VideoSdkAlarmReceiptMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Acknowledges only persisted payloads; database leases survive backend restarts. */
@Service
@Slf4j
@RequiredArgsConstructor
public class VideoSdkAlarmReceiptService {
  private final VideoSdkAlarmReceiptMapper receipts;
  private final ObjectMapper json;
  private final ObjectProvider<SdkVideoAlarmUpService> publisher;
  private final String owner = UUID.randomUUID().toString();
  private final ExecutorService workers = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS,
      new SynchronousQueue<>(), r -> { Thread t = new Thread(r, "video-alarm-receipt"); t.setDaemon(true); return t; });

  public Map<String, Object> accept(VideoPlatformInstance instance, SdkVideoAlarmCallbackRequest request) {
    if (request.getEventId() == null || request.getEventId().isBlank() || request.getEventId().length() > 200)
      throw new BaseException("ISUP 告警必须携带稳定 eventId");
    String fingerprint = "";
    try {
      if ("evidence_ready".equals(request.getAlarmType()))
        fingerprint = digest(json.writeValueAsString(Arrays.asList(request.getImages(), request.getEvidence(), request.getImageStatus())));
      else if (request.getActivePostCount() != null || request.getAlarmState() != null)
        fingerprint = digest(json.writeValueAsString(Arrays.asList(request.getAlarmState(),request.getActivePostCount())));
      String id = digest(String.join("\u001f", Objects.toString(instance.getTenantId(), ""), instance.getInstanceKey(),
          Objects.toString(request.getDeviceId(), ""), Objects.toString(request.getChannelId(), ""), request.getEventId(),
          Objects.toString(request.getAlarmType(), ""), fingerprint));
      VideoSdkAlarmReceipt existing = receipts.selectByPrimaryKey(id);
      if (existing != null) return acknowledgement(existing, true);
      VideoSdkAlarmReceipt receipt = new VideoSdkAlarmReceipt();
      receipt.setId(id); receipt.setTenantId(instance.getTenantId()); receipt.setInstanceKey(instance.getInstanceKey());
      receipt.setDeviceId(Objects.toString(request.getDeviceId(), "")); receipt.setChannelId(Objects.toString(request.getChannelId(), ""));
      receipt.setEventId(request.getEventId()); receipt.setEventKind(Objects.toString(request.getAlarmType(), ""));
      receipt.setImageFingerprint(fingerprint); receipt.setPayloadJson(json.writeValueAsString(durablePayload(request)));
      if (receipt.getPayloadJson().length() > 1_048_576) throw new BaseException("告警回调数据过大");
      receipt.setStatus("pending"); receipt.setAttempts(0); receipt.setLogPersisted(0); receipt.setFlowDispatched(0);
      receipt.setCreatedAt(System.currentTimeMillis()); receipt.setUpdatedAt(receipt.getCreatedAt());
      receipt.setNextAttemptAt(0L); receipt.setLeaseUntil(0L); receipt.setLeaseOwner("");
      try { receipts.insert(receipt); }
      catch (org.springframework.dao.DuplicateKeyException race) { receipt = receipts.selectByPrimaryKey(id); }
      return acknowledgement(receipt, false);
    } catch (BaseException ex) { throw ex; }
    catch (Exception ex) { throw new BaseException("告警尚未持久接收，请网关重试"); }
  }
  private SdkVideoAlarmCallbackRequest durablePayload(SdkVideoAlarmCallbackRequest request) {
    // Keep the complete replay DTO without persisting a second copy of its decoded vendor data.
    var payload = json.convertValue(request, SdkVideoAlarmCallbackRequest.class);
    Map<String, Object> data = VideoAlarmPayload.safeData(payload.getData());
    VideoAlarmPayload.safeData(payload.getRaw()).forEach((key, value) -> {
      if (!data.containsKey(key)) data.put(key, value);
    });
    payload.setData(data);
    payload.setRaw(null);
    return payload;
  }
  private static Map<String, Object> acknowledgement(VideoSdkAlarmReceipt receipt, boolean duplicate) {
    if (receipt == null) throw new BaseException("告警收据不可用，请重试");
    return Map.of("accepted", true, "receiptId", receipt.getId(), "eventId", receipt.getEventId(),
        "status", receipt.getStatus(), "duplicate", duplicate, "ruleDispatched", receipt.getFlowDispatched() == 1);
  }
  @Scheduled(initialDelay = 2000, fixedDelay = 1000)
  public void tick() {
    try { for (var receipt : receipts.due(System.currentTimeMillis())) {
      try { workers.execute(() -> process(receipt.getId())); }
      catch (RejectedExecutionException full) { break; }
    }} catch (Exception ex) { log.warn("视频告警收据调度暂不可用：{}", ex.getClass().getSimpleName()); }
  }
  void process(String id) {
    long now = System.currentTimeMillis();
    if (receipts.claim(id, owner, now, now + 120_000) != 1) return;
    VideoSdkAlarmReceipt receipt = receipts.selectByPrimaryKey(id);
    try {
      var request = json.readValue(receipt.getPayloadJson(), SdkVideoAlarmCallbackRequest.class);
      Map<String, Object> result = publisher.getObject().publishReceipt(request, receipt);
      if (receipts.complete(id, owner, Boolean.TRUE.equals(result.get("ruleDispatched")) ? 1 : 0,
          json.writeValueAsString(result), System.currentTimeMillis()) != 1) throw new IllegalStateException("Receipt lease lost");
    } catch (Exception ex) {
      receipts.retry(id, owner, now + Math.min(300_000, 1000L << Math.min(8, receipt.getAttempts())), System.currentTimeMillis());
      log.warn("视频告警收据待重试 receiptId={}, reason={}", id, ex.getClass().getSimpleName());
    }
  }
  public void logged(VideoSdkAlarmReceipt receipt) {
    if (receipts.logged(receipt.getId(), owner, System.currentTimeMillis()) != 1) throw new IllegalStateException("Receipt lease lost");
    receipt.setLogPersisted(1);
  }
  public boolean cooledDown(VideoSdkAlarmReceipt receipt, int seconds) {
    if (seconds <= 0) return false;
    String guard = digest(String.join("\u001f", "cooldown", receipt.getTenantId(), receipt.getInstanceKey(),
        receipt.getDeviceId(), receipt.getChannelId(), receipt.getEventKind()));
    return reserveCooldown(receipt, seconds, guard, "__policy_cooldown__");
  }
  /** Independent evidence budget; callback identities are always 64 hex chars, never f:-prefixed. */
  public boolean frameCooledDown(VideoSdkAlarmReceipt receipt, int seconds) {
    if (seconds < 1 || seconds > 3600) throw new IllegalArgumentException("Frame cooldown must be 1..3600 seconds");
    String guard = "f:" + digest(String.join("\u001f", "frame-cooldown-v1", receipt.getTenantId(), receipt.getInstanceKey(),
        receipt.getDeviceId(), receipt.getChannelId(), receipt.getEventKind())).substring(0, 62);
    return reserveCooldown(receipt, seconds, guard, "__frame_cooldown__");
  }
  private boolean reserveCooldown(VideoSdkAlarmReceipt receipt, int seconds, String guard, String marker) {
    if (receipts.selectByPrimaryKey(guard) == null) {
      VideoSdkAlarmReceipt policy = new VideoSdkAlarmReceipt();
      policy.setId(guard); policy.setTenantId(receipt.getTenantId()); policy.setInstanceKey(receipt.getInstanceKey());
      policy.setDeviceId(receipt.getDeviceId()); policy.setChannelId(receipt.getChannelId()); policy.setEventKind(receipt.getEventKind());
      policy.setEventId(marker); policy.setImageFingerprint("policy"); policy.setStatus("done");
      policy.setPayloadJson("{}"); policy.setAttempts(0); policy.setLogPersisted(0); policy.setFlowDispatched(0);
      policy.setCreatedAt(0L); policy.setUpdatedAt(0L); policy.setNextAttemptAt(0L); policy.setLeaseUntil(0L); policy.setLeaseOwner("");
      try { receipts.insert(policy); } catch (org.springframework.dao.DuplicateKeyException race) { }
    }
    long now = System.currentTimeMillis();
    return receipts.reserveDispatch(guard, receipt.getId(), now, now - seconds * 1000L) != 1;
  }
  static String digest(String value) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
    catch (Exception impossible) { throw new IllegalStateException(impossible); }
  }
  @PreDestroy public void stop() { workers.shutdownNow(); }
}
