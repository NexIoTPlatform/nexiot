package cn.universal.dm.device.service.ota.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.domain.R;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.dm.device.service.ota.api.OtaService.OtaServiceResult;
import cn.universal.dm.device.service.ota.support.OtaConstants;
import cn.universal.dm.device.service.ota.support.OtaFirmwareStore;
import cn.universal.dm.device.service.ota.support.OtaTransferProfile;
import cn.universal.persistence.entity.IoTOtaPackage;
import cn.universal.persistence.entity.IoTOtaTask;
import cn.universal.persistence.entity.IoTOtaTaskDevice;
import cn.universal.persistence.mapper.IoTOtaPackageMapper;
import cn.universal.persistence.mapper.IoTOtaTaskDeviceMapper;
import cn.universal.persistence.mapper.IoTOtaTaskMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 持久化的停等式 OTA 分包传输：一次只允许一片在途，ACK 后推进。
 * 产品脚本只负责报文翻译，切片、会话隔离、重传和重启恢复由平台负责。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtaTransferService {

  private final IoTOtaTaskDeviceMapper deviceMapper;
  private final IoTOtaPackageMapper packageMapper;
  private final OtaFirmwareStore firmwareStore;
  private final IoTOtaTaskMapper taskMapper;

  /** 调用者已锁定任务设备；将会话与 start 指令一起持久化。 */
  @SuppressWarnings("unchecked")
  public void initialize(
      IoTOtaTask task, IoTOtaTaskDevice device, IoTOtaPackage firmware, Map<String, Object> function)
      throws IOException {
    JSONObject strategy = StrUtil.isBlank(task.getStrategyJson()) ? new JSONObject() : JSONUtil.parseObj(task.getStrategyJson());
    OtaTransferProfile profile = OtaTransferProfile.from(strategy.get("transferProfile"));
    Map<String, Object> data = (Map<String, Object>) function.get("data");
    data.put("transfer", profile.toMap());
    if (!profile.isChunked()) {
      device.setTransferState("");
      return;
    }
    long size = Files.size(firmwareStore.prepare(firmware));
    if (size <= 0 || firmware.getFileSize() == null || size != firmware.getFileSize()) {
      throw new IOException("固件文件长度与登记信息不一致");
    }
    long total = (size + profile.getChunkSize() - 1) / profile.getChunkSize();
    if (total > Integer.MAX_VALUE) throw new IOException("固件分包数量过多");
    JSONObject state = new JSONObject();
    state.set("sessionId", IdUtil.fastSimpleUUID());
    state.set("phase", "START");
    state.set("totalChunks", (int) total);
    state.set("ackedChunks", 0);
    state.set("nextChunkIndex", 0);
    state.set("chunkIndex", -1);
    state.set("profile", profile.toMap());
    state.set("sentAt", System.currentTimeMillis());
    state.set("retries", 0);
    device.setTransferState(state.toString());
    data.put("sessionId", state.getStr("sessionId"));
    ((Map<String, Object>) data.get("transfer")).put("totalChunks", (int) total);
  }

  /** 精确匹配设备、任务和本次会话；旧会话、重复确认和跳片均不会推进。 */
  @Transactional(rollbackFor = Exception.class)
  public OtaServiceResult handleReport(Map<String, Object> report) {
    Long id = resolveReportTarget(report);
    if (id == null) return OtaServiceResult.PROTOCOL_ERROR;
    IoTOtaTaskDevice device = lockDevice(id);
    if (!matchesDevice(device, report) || OtaConstants.isFinalDeviceStatus(device.getStatus())) {
      return OtaServiceResult.PROTOCOL_ERROR;
    }
    JSONObject state = state(device);
    if (state == null || !Objects.equals(state.getStr("sessionId"), string(report, "sessionId"))) {
      return OtaServiceResult.PROTOCOL_ERROR;
    }
    String action = string(report, "transferAction");
    String reportType = string(report, "reportType");
    String phase = state.getStr("phase");
    OtaTransferProfile profile = OtaTransferProfile.from(state.get("profile"));
    boolean expectedAck = OtaConstants.ReportType.TRANSFER_ACK.equals(reportType)
        && (("ready".equals(action) && "START".equals(phase))
        || ("chunk".equals(action) && "CHUNK".equals(phase) && matchesIndex(report, state.getInt("chunkIndex"), profile))
        || ("finish".equals(action) && "FINISH".equals(phase)));
    if (OtaConstants.ReportType.TRANSFER_ACK.equals(reportType) && !expectedAck) return OtaServiceResult.PROTOCOL_ERROR;
    try {
      if (OtaConstants.ReportType.TRANSFER_REQUEST.equals(reportType)) {
        // PULL 使用明确的逐片 ACK；只有已确认上一片，才能索取下一片。
        if (!"PULL".equals(profile.getMode()) || !"WAIT_REQUEST".equals(phase)
            || !"chunk".equals(action) || !matchesIndex(report, state.getInt("nextChunkIndex"), profile)) {
          return OtaServiceResult.PROTOCOL_ERROR;
        }
        sendChunk(device, state, state.getInt("nextChunkIndex"));
      } else if (OtaConstants.ReportType.TRANSFER_ACK.equals(reportType)) {
        if (report.containsKey("success") && !Boolean.TRUE.equals(report.get("success"))) {
          fail(device, state, "设备拒绝分包传输: " + StrUtil.blankToDefault(string(report, "errorMessage"), action));
        } else if ("ready".equals(action) && "START".equals(phase)) {
          if ("PUSH".equals(profile.getMode())) sendChunk(device, state, 0);
          else waitForRequest(device, state);
        } else if ("chunk".equals(action) && "CHUNK".equals(phase)
            && matchesIndex(report, state.getInt("chunkIndex"), profile)) {
          int next = state.getInt("chunkIndex") + 1;
          state.set("ackedChunks", next);
          state.set("nextChunkIndex", next);
          device.setProgress(Math.min(90, (int) (90L * next / state.getInt("totalChunks"))));
          if (next == state.getInt("totalChunks")) sendFinish(device, state);
          else if ("PUSH".equals(profile.getMode())) sendChunk(device, state, next);
          else waitForRequest(device, state);
        } else if ("finish".equals(action) && "FINISH".equals(phase)) {
          state.set("phase", "COMPLETE");
          device.setStatus(OtaConstants.DeviceStatus.VERIFYING);
          device.setStatusDetail("全部分包已确认，等待设备校验、安装和版本确认");
        } else {
          return OtaServiceResult.PROTOCOL_ERROR;
        }
      } else {
        return OtaServiceResult.PROTOCOL_ERROR;
      }
      device.setLastReportTime(new Date());
      device.setRawReport(JSONUtil.toJsonStr(report));
      persist(device, state);
      return OtaServiceResult.SUCCESS;
    } catch (OtaSendException e) {
      device.setStatusDetail("分包发送失败，等待自动重试: " + e.getMessage());
      device.setLastReportTime(new Date());
      persist(device, state);
      return OtaServiceResult.SUCCESS;
    } catch (Exception e) {
      fail(device, state, "分包传输失败: " + e.getMessage());
      persist(device, state);
      return OtaServiceResult.FAILED;
    }
  }

  /** 调度器逐行调用；状态来自数据库，因此服务重启后可继续等待或重发当前片。 */
  @Transactional(rollbackFor = Exception.class)
  public boolean resume(Long id) {
    IoTOtaTaskDevice device = lockDevice(id);
    if (device == null || OtaConstants.isFinalDeviceStatus(device.getStatus())) return false;
    JSONObject state = state(device);
    if (state == null || "COMPLETE".equals(state.getStr("phase")) || "FAILED".equals(state.getStr("phase"))) return false;
    OtaTransferProfile profile = OtaTransferProfile.from(state.get("profile"));
    long now = System.currentTimeMillis();
    long timeout = profile.getAckTimeoutSeconds() * 1000L;
    if (now - state.getLong("sentAt", now) < timeout) return false;
    if ("WAIT_REQUEST".equals(state.getStr("phase"))) {
      if (now - state.getLong("sentAt") < timeout * (profile.getChunkMaxRetries() + 1L)) return false;
      fail(device, state, "等待设备请求下一分包超时");
    } else if (state.getInt("retries", 0) >= profile.getChunkMaxRetries()) {
      fail(device, state, "分包确认超时，已达到重试上限");
    } else {
      int retries = state.getInt("retries", 0) + 1;
      try {
        switch (state.getStr("phase")) {
          case "START" -> send(device, JSONUtil.parseObj(device.getCommandPayload()));
          case "CHUNK" -> transmitChunk(device, state, state.getInt("chunkIndex"));
          case "FINISH" -> transmitFinish(device, state);
          default -> throw new IllegalStateException("未知分包状态");
        }
        state.set("retries", retries);
        state.set("sentAt", now);
        device.setStatusDetail("等待分包确认，重试 " + retries + "/" + profile.getChunkMaxRetries());
      } catch (Exception e) {
        // 发送失败也消耗重试预算，防止断网时无限发送。
        state.set("retries", retries);
        state.set("sentAt", now);
        device.setStatusDetail("分包重试发送失败: " + e.getMessage());
        if (retries >= profile.getChunkMaxRetries()) fail(device, state, device.getStatusDetail());
      }
    }
    persist(device, state);
    return true;
  }

  public boolean hasIncompleteTransfer(IoTOtaTaskDevice device) {
    JSONObject state = state(device);
    return state != null && !"COMPLETE".equals(state.getStr("phase"));
  }

  public boolean canConfirmInstallation(IoTOtaTaskDevice device) {
    JSONObject state = state(device);
    return state == null || "FINISH".equals(state.getStr("phase")) || "COMPLETE".equals(state.getStr("phase"));
  }

  public boolean matchesSession(IoTOtaTaskDevice device, Map<String, Object> report) {
    JSONObject state = state(device);
    return state == null || Objects.equals(state.getStr("sessionId"), string(report, "sessionId"));
  }

  private void waitForRequest(IoTOtaTaskDevice device, JSONObject state) {
    state.set("phase", "WAIT_REQUEST");
    state.set("sentAt", System.currentTimeMillis());
    state.set("retries", 0);
    device.setStatus(OtaConstants.DeviceStatus.DOWNLOADING);
    device.setStatusDetail("等待设备请求第 " + (state.getInt("nextChunkIndex") + 1) + " 个分包");
  }

  private void sendChunk(IoTOtaTaskDevice device, JSONObject state, int index) throws IOException {
    state.set("phase", "CHUNK");
    state.set("chunkIndex", index);
    state.set("sentAt", System.currentTimeMillis());
    state.set("retries", 0);
    device.setStatus(OtaConstants.DeviceStatus.DOWNLOADING);
    device.setStatusDetail("已发送分包 " + (index + 1) + "/" + state.getInt("totalChunks") + "，等待确认");
    transmitChunk(device, state, index);
  }

  private void transmitChunk(IoTOtaTaskDevice device, JSONObject state, int index) throws IOException {
    OtaTransferProfile profile = OtaTransferProfile.from(state.get("profile"));
    IoTOtaPackage firmware = packageMapper.selectPackageById(device.getPackageId());
    if (firmware == null) throw new IOException("升级包不存在");
    byte[] bytes = firmwareStore.readChunk(firmware, profile.getChunkSize(), index);
    long expectedSize = Math.min(profile.getChunkSize(), firmware.getFileSize() - (long) index * profile.getChunkSize());
    if (expectedSize <= 0 || bytes.length != expectedSize) throw new IOException("分包数据长度不一致");
    JSONObject function = function(device, state, "chunk");
    JSONObject data = function.getJSONObject("data");
    data.set("chunkIndex", index + profile.getIndexBase());
    data.set("chunkOffset", (long) index * profile.getChunkSize());
    data.set("chunkLength", bytes.length);
    data.set("totalChunks", state.getInt("totalChunks"));
    data.set("dataEncoding", profile.getDataEncoding());
    data.set("chunkData", "HEX".equals(profile.getDataEncoding()) ? HexFormat.of().formatHex(bytes) : Base64.getEncoder().encodeToString(bytes));
    try {
      data.set("chunkChecksum", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
    data.set("chunkChecksumAlgorithm", "SHA256");
    send(device, function);
  }

  private void sendFinish(IoTOtaTaskDevice device, JSONObject state) throws IOException {
    state.set("phase", "FINISH");
    state.set("sentAt", System.currentTimeMillis());
    state.set("retries", 0);
    device.setStatus(OtaConstants.DeviceStatus.VERIFYING);
    device.setStatusDetail("分包全部确认，已通知设备校验并安装");
    transmitFinish(device, state);
  }

  private void transmitFinish(IoTOtaTaskDevice device, JSONObject state) throws IOException {
    send(device, function(device, state, "finish"));
  }

  private JSONObject function(IoTOtaTaskDevice device, JSONObject state, String action) throws IOException {
    if (StrUtil.isBlank(device.getCommandPayload())) throw new IOException("OTA 开始指令快照缺失");
    JSONObject function = JSONUtil.parseObj(device.getCommandPayload());
    JSONObject data = function.getJSONObject("data");
    data.set("action", action);
    data.set("sessionId", state.getStr("sessionId"));
    return function;
  }

  /** 保持与普通功能下发相同的 encode → 协议插件链路。 */
  protected void send(IoTOtaTaskDevice device, Map<String, Object> function) throws IOException {
    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.builder()
        .productKey(device.getProductKey()).deviceId(device.getDeviceId()).iotId(device.getIotId())
        .cmd(DownCmd.DEV_FUNCTION).function(function).applicationId("system-ota").appUnionId("system-ota")
        .msgId(IdUtil.fastSimpleUUID()).timestamp(System.currentTimeMillis()).build();
    command.withSource("system-ota");
    try {
      R<?> result = IoTDownlFactory.getIDownOrThrow(device.getProtocol()).doAction(command);
      if (result == null || !result.isSuccess()) throw new OtaSendException(result == null ? "协议发送失败" : result.getMsg());
    } catch (OtaSendException e) {
      throw e;
    } catch (Exception e) {
      throw new OtaSendException(e.getMessage());
    }
  }

  private void fail(IoTOtaTaskDevice device, JSONObject state, String reason) {
    state.set("phase", "FAILED");
    device.setStatus(OtaConstants.DeviceStatus.FAILED);
    device.setStatusDetail(reason);
    device.setErrorMessage(reason);
    device.setErrorCode("OTA_TRANSFER_FAILED");
  }

  private void persist(IoTOtaTaskDevice device, JSONObject state) {
    device.setTransferState(state.toString());
    deviceMapper.updateTaskDeviceIfActive(device);
  }

  private JSONObject state(IoTOtaTaskDevice device) {
    if (device == null || StrUtil.isBlank(device.getTransferState())) return null;
    return JSONUtil.parseObj(device.getTransferState());
  }

  private IoTOtaTaskDevice lockDevice(Long id) {
    IoTOtaTaskDevice candidate = deviceMapper.selectTaskDeviceById(id);
    if (candidate == null) return null;
    // All OTA writers acquire task before device; this also serializes cancellation and batch dispatch.
    taskMapper.selectTaskForUpdate(candidate.getTaskId());
    return deviceMapper.selectTaskDeviceForUpdate(id);
  }

  private Long resolveReportTarget(Map<String, Object> report) {
    String explicitId = string(report, "taskDeviceId");
    if (explicitId != null) {
      try { return Long.valueOf(explicitId); } catch (NumberFormatException e) { return null; }
    }
    String productKey = string(report, "productKey");
    String deviceId = string(report, "deviceId");
    String sessionId = string(report, "sessionId");
    if (StrUtil.isBlank(productKey) || StrUtil.isBlank(deviceId) || StrUtil.isBlank(sessionId)) return null;
    List<IoTOtaTaskDevice> candidates = deviceMapper.selectActiveCandidatesByDevice(productKey, deviceId);
    if (candidates == null) return null;
    for (IoTOtaTaskDevice candidate : candidates) {
      JSONObject state = state(candidate);
      if (state != null && sessionId.equals(state.getStr("sessionId"))) return candidate.getId();
    }
    return null;
  }

  private boolean matchesDevice(IoTOtaTaskDevice device, Map<String, Object> report) {
    return device != null && Objects.equals(device.getProductKey(), string(report, "productKey"))
        && Objects.equals(device.getDeviceId(), string(report, "deviceId"))
        && (report.get("taskId") == null || Objects.equals(String.valueOf(device.getTaskId()), string(report, "taskId")));
  }

  private boolean matchesIndex(Map<String, Object> report, int expected, OtaTransferProfile profile) {
    try {
      return Integer.parseInt(string(report, "chunkIndex")) == expected + profile.getIndexBase();
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private String string(Map<String, Object> report, String key) {
    Object value = report.get(key);
    return value == null ? null : String.valueOf(value);
  }

  private static class OtaSendException extends IOException {
    OtaSendException(String message) { super(message); }
  }
}
