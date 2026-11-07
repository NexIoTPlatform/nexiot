/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA上报抽取器
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.dm.device.service.ota.support;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.persistence.base.BaseUPRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 将标准上行消息中的 OTA 语义抽取成统一字段 */
@Component
public class OtaReportExtractor {

  /**
   * 判断当前上行是否应由 OTA 链路接管。
   *
   * <p>这里优先依赖明确语义：
   *
   * <ul>
   *   <li>版本确认：{@code messageType = PROPERTIES}，且通常带 {@code function = otaUpgrade}
   *   <li>升级进度：{@code messageType = EVENT, event = otaProgress}
   *   <li>升级结果：{@code messageType = EVENT, event = otaResult}
   * </ul>
   *
   * <p>同时保留对旧格式的轻量兼容，避免升级期间历史脚本立刻失效。
   */
  public boolean supports(BaseUPRequest upRequest) {
    if (upRequest == null) {
      return false;
    }
    Map<String, Object> merged = buildMergedPayload(upRequest);
    return isOtaLike(merged, upRequest);
  }

  public Map<String, Object> extract(
      BaseUPRequest upRequest, String productKey, String deviceId, String protocol) {
    if (upRequest == null) {
      return null;
    }
    Map<String, Object> merged = buildMergedPayload(upRequest);

    if (!isOtaLike(merged, upRequest)) {
      return null;
    }

    Map<String, Object> normalized = new LinkedHashMap<>(merged);
    normalized.put("productKey", productKey);
    normalized.put("deviceId", deviceId);
    normalized.put("protocol", protocol);

    normalized.put("reportType", normalizeReportType(normalized, upRequest));
    normalized.put("currentVersion", firstNonBlank(normalized, "currentVersion", "version", "firmwareVersion"));
    normalized.put("hardwareVersion", firstNonBlank(normalized, "hardwareVersion", "hwVersion"));
    normalized.put("targetVersion", firstNonBlank(normalized, "targetVersion", "toVersion"));
    normalized.put("taskId", firstNonBlank(normalized, "taskId", "jobId", "upgradeTaskId"));
    normalized.put("sessionId", firstNonBlank(normalized, "sessionId"));
    normalized.put("transferAction", firstNonBlank(normalized, "transferAction"));
    normalized.put(
        "packageType",
        firstNonBlank(normalized, "packageType", "firmwareType", "pkgType", "otaType", "type"));
    normalized.put("customType", firstNonBlank(normalized, "customType", "tag"));
    normalized.put("module", firstNonBlank(normalized, "module", "component", "part"));
    normalized.put(
        "upgradeStatus",
        firstNonBlank(normalized, "upgradeStatus", "status", "result", "step", "stage"));
    normalized.put("errorCode", firstNonBlank(normalized, "errorCode", "code"));
    normalized.put("errorMessage", firstNonBlank(normalized, "errorMessage", "message", "reason"));
    if (!normalized.containsKey("progress")) {
      Object progress = firstNonNull(normalized, "progress", "percent", "percentage");
      if (progress != null) {
        normalized.put("progress", Convert.toInt(progress));
      }
    } else {
      normalized.put("progress", Convert.toInt(normalized.get("progress")));
    }

    String packageType = Convert.toStr(normalized.get("packageType"));
    if (StrUtil.isBlank(packageType)) {
      normalized.put("packageType", OtaConstants.DEFAULT_PACKAGE_TYPE);
    }
    String module = Convert.toStr(normalized.get("module"));
    if (StrUtil.isBlank(module)) {
      normalized.put("module", OtaConstants.DEFAULT_MODULE);
    }
    return normalized;
  }

  private Map<String, Object> buildMergedPayload(BaseUPRequest upRequest) {
    Map<String, Object> merged = new LinkedHashMap<>();
    mergeJsonPayload(merged, upRequest.getPayload());
    mergeMap(merged, upRequest.getData());
    mergeMap(merged, upRequest.getProperties());

    // 向后兼容旧模板中 data.ota / data.upgrade / data.firmware 的套层写法。
    // 新设计推荐直接把 OTA 字段平铺在 data 节点中，不再多包一层 ota。
    flattenNested(merged, "ota");
    flattenNested(merged, "upgrade");
    flattenNested(merged, "firmware");

    if (StrUtil.isNotBlank(upRequest.getEvent())) {
      merged.putIfAbsent("event", upRequest.getEvent());
    }
    if (StrUtil.isNotBlank(upRequest.getFunction())) {
      merged.putIfAbsent("function", upRequest.getFunction());
    }
    if (upRequest.getMessageType() != null) {
      merged.putIfAbsent("messageType", upRequest.getMessageType().name());
    }
    return merged;
  }

  private void mergeJsonPayload(Map<String, Object> merged, String payload) {
    if (StrUtil.isBlank(payload) || !JSONUtil.isTypeJSONObject(payload)) {
      return;
    }
    JSONObject jsonObject = JSONUtil.parseObj(payload);
    merged.putAll(jsonObject);
  }

  private void mergeMap(Map<String, Object> merged, Map<String, Object> source) {
    if (source == null || source.isEmpty()) {
      return;
    }
    merged.putAll(source);
  }

  private void flattenNested(Map<String, Object> merged, String key) {
    Object nested = merged.get(key);
    if (nested instanceof Map<?, ?> nestedMap) {
      for (Map.Entry<?, ?> entry : nestedMap.entrySet()) {
        if (entry.getKey() != null && entry.getValue() != null) {
          merged.putIfAbsent(entry.getKey().toString(), entry.getValue());
        }
      }
    } else if (nested instanceof JSONObject nestedJson) {
      for (String itemKey : nestedJson.keySet()) {
        merged.putIfAbsent(itemKey, nestedJson.get(itemKey));
      }
    }
  }

  private boolean isOtaLike(Map<String, Object> merged, BaseUPRequest upRequest) {
    if (StrUtil.equalsAnyIgnoreCase(firstNonBlank(merged, "reportType"),
        OtaConstants.ReportType.TRANSFER_ACK, OtaConstants.ReportType.TRANSFER_REQUEST)) {
      return StrUtil.isNotBlank(firstNonBlank(merged, "sessionId"));
    }
    MessageType messageType = resolveMessageType(upRequest, merged);
    String event = firstNonBlank(merged, "event");
    String function = firstNonBlank(merged, "function");
    boolean otaFunction = StrUtil.equalsIgnoreCase(function, OtaConstants.DEFAULT_FUNCTION_ID);
    boolean hasVersionFields =
        firstNonNull(merged, "currentVersion", "version", "firmwareVersion", "hardwareVersion") != null;
    boolean hasTraceFields =
        firstNonNull(merged, "taskId", "taskDeviceId", "jobId", "upgradeTaskId", "targetVersion") != null;
    boolean hasProgressFields =
        firstNonNull(merged, "progress", "percent", "percentage") != null
            || StrUtil.isNotBlank(firstNonBlank(merged, "upgradeStatus", "status", "result", "step", "stage"))
            || StrUtil.isNotBlank(firstNonBlank(merged, "errorCode", "errorMessage", "reason"));

    if (MessageType.PROPERTIES.equals(messageType)) {
      if (StrUtil.equalsIgnoreCase(event, OtaConstants.Event.OTA_INFO)) {
        return hasVersionFields;
      }
      return otaFunction && (hasVersionFields || hasTraceFields);
    }

    if (MessageType.EVENT.equals(messageType)) {
      if (StrUtil.equalsAnyIgnoreCase(
          event,
          OtaConstants.Event.OTA_PROGRESS,
          OtaConstants.Event.OTA_RESULT,
          OtaConstants.Event.OTA_INFO)) {
        return true;
      }
      // 兼容旧协议: event 没有标准化，但 function 已明确标识为 otaUpgrade。
      return otaFunction && (hasProgressFields || hasTraceFields || hasVersionFields);
    }
    return false;
  }

  private String normalizeReportType(Map<String, Object> merged, BaseUPRequest upRequest) {
    String explicit = firstNonBlank(merged, "reportType");
    if (StrUtil.isNotBlank(explicit)) {
      String value = explicit.toUpperCase();
      if (OtaConstants.ReportType.TRANSFER_ACK.equals(value)
          || OtaConstants.ReportType.TRANSFER_REQUEST.equals(value)) {
        return value;
      }
      if (value.contains("RESULT")) {
        return OtaConstants.ReportType.UPGRADE_RESULT;
      }
      if (value.contains("PROGRESS")) {
        return OtaConstants.ReportType.UPGRADE_PROGRESS;
      }
      return OtaConstants.ReportType.FIRMWARE_INFO;
    }

    String event = firstNonBlank(merged, "event");
    if (StrUtil.equalsIgnoreCase(event, OtaConstants.Event.OTA_PROGRESS)) {
      return OtaConstants.ReportType.UPGRADE_PROGRESS;
    }
    if (StrUtil.equalsIgnoreCase(event, OtaConstants.Event.OTA_RESULT)) {
      return OtaConstants.ReportType.UPGRADE_RESULT;
    }
    if (StrUtil.equalsIgnoreCase(event, OtaConstants.Event.OTA_INFO)) {
      return OtaConstants.ReportType.FIRMWARE_INFO;
    }

    MessageType messageType = resolveMessageType(upRequest, merged);
    if (MessageType.PROPERTIES.equals(messageType)) {
      return OtaConstants.ReportType.FIRMWARE_INFO;
    }

    String status = firstNonBlank(merged, "upgradeStatus", "status", "result");
    if (StrUtil.isNotBlank(status)) {
      String statusValue = status.toLowerCase();
      if (statusValue.contains("success")
          || statusValue.contains("fail")
          || statusValue.contains("error")
          || statusValue.contains("cancel")) {
        return OtaConstants.ReportType.UPGRADE_RESULT;
      }
    }
    if (firstNonNull(merged, "progress", "percent", "percentage") != null) {
      return OtaConstants.ReportType.UPGRADE_PROGRESS;
    }
    return OtaConstants.ReportType.FIRMWARE_INFO;
  }

  private MessageType resolveMessageType(BaseUPRequest upRequest, Map<String, Object> merged) {
    if (upRequest != null && upRequest.getMessageType() != null) {
      return upRequest.getMessageType();
    }
    String messageType = firstNonBlank(merged, "messageType");
    if (StrUtil.isBlank(messageType)) {
      return null;
    }
    return MessageType.find(messageType);
  }

  private String firstNonBlank(Map<String, Object> merged, String... keys) {
    for (String key : keys) {
      Object value = merged.get(key);
      if (ObjectUtil.isNotEmpty(value) && StrUtil.isNotBlank(Convert.toStr(value))) {
        return Convert.toStr(value);
      }
    }
    return null;
  }

  private Object firstNonNull(Map<String, Object> merged, String... keys) {
    for (String key : keys) {
      if (merged.containsKey(key) && merged.get(key) != null) {
        return merged.get(key);
      }
    }
    return null;
  }
}
