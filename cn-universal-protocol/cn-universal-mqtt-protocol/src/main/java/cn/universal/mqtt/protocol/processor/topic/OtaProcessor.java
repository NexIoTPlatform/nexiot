/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.mqtt.protocol.processor.topic;

import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.dm.device.service.ota.api.IoTOtaRuntimeService;
import cn.universal.dm.device.service.ota.api.OtaService.OtaServiceResult;
import cn.universal.dm.device.service.ota.support.OtaConstants;
import cn.universal.mqtt.protocol.config.MqttConstant;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.processor.MqttMessageProcessor;
import cn.universal.mqtt.protocol.topic.MQTTTopicManager;
import cn.universal.persistence.base.BaseUPRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * OTA处理器
 *
 * <p>专门处理系统级OTA相关的MQTT消息： - 固件上报 ($ota/report/${productKey}/${deviceId}) - 固件更新
 * ($ota/update/${productKey}/${deviceId})
 *
 * <p>通过通用OTA接口处理不同协议的OTA逻辑
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
@Component
public class OtaProcessor implements MqttMessageProcessor {

  @Autowired private MQTTTopicManager topicManager;

  @Autowired private IoTOtaRuntimeService otaRuntimeService;

  @Autowired private ObjectMapper objectMapper;

  @Override
  public String getName() {
    return "OTA处理器";
  }

  @Override
  public String getDescription() {
    return "处理设备固件上报和固件更新消息";
  }

  @Override
  public int getOrder() {
    return 200; // OTA处理优先级中等
  }

  @Override
  public boolean supports(MQTTUPRequest request) {
    if (request.getUpTopic() == null || request.getPayload() == null) {
      return false;
    }
    return MqttConstant.TopicCategory.SYSTEM_LEVEL.equals(
        MQTTTopicManager.matchCategory(request.getUpTopic()));
  }

  @Override
  public ProcessorResult process(MQTTUPRequest request) {
    try {
      MQTTTopicManager.TopicInfo topicInfo = topicManager.extractTopicInfo(request.getUpTopic());

      log.debug(
          "[{}] 开始处理OTA消息 - 类型: {}, 设备: {}",
          getName(),
          topicInfo.getTopicType(),
          topicInfo.getDeviceUniqueId());

      // 根据OTA消息类型分发处理
      switch (topicInfo.getTopicType()) {
        case OTA_REPORT:
          return processFirmwareReport(request, topicInfo);

        case OTA_UPDATE:
          return processFirmwareUpdate(request, topicInfo);

        default:
          log.warn("[{}] 不支持的OTA类型: {}", getName(), topicInfo.getTopicType());
          return ProcessorResult.CONTINUE;
      }

    } catch (Exception e) {
      log.error("[{}] OTA消息处理异常: ", getName(), e);
      return ProcessorResult.ERROR;
    }
  }

  /** 处理固件上报 */
  private ProcessorResult processFirmwareReport(
      MQTTUPRequest request, MQTTTopicManager.TopicInfo topicInfo) {
    try {
      String payload = request.getPayload();
      JsonNode reportData = objectMapper.readTree(payload);

      // 系统 OTA Topic 不会进入透传/物模型编解码链。新会话报文在这里保留完整字段，
      // 交给同一抽取器处理 ACK、请求、安装进度及结果；身份仍取已经匹配的 Topic。
      if (isSessionReport(reportData)) {
        Map<String, Object> data = objectMapper.convertValue(reportData, Map.class);
        if (reportData.path("data").isObject()) {
          data = objectMapper.convertValue(reportData.path("data"), Map.class);
        }
        BaseUPRequest decoded = BaseUPRequest.builder()
            .productKey(topicInfo.getProductKey()).deviceId(topicInfo.getDeviceId())
            .messageType(MessageType.EVENT).function(OtaConstants.DEFAULT_FUNCTION_ID)
            .payload(payload).data(data).build();
        boolean handled = otaRuntimeService.handleDecodedReport(
            decoded, topicInfo.getProductKey(), topicInfo.getDeviceId(), "mqtt");
        request.setContextValue("otaHandled", handled);
        if (!handled) {
          request.setContextValue("validationError", "OTA会话上报缺少必要字段");
        }
        return ProcessorResult.CONTINUE;
      }

      log.info(
          "[{}] 处理固件上报 - 设备: {}, 固件版本: {}",
          getName(),
          topicInfo.getDeviceUniqueId(),
          reportData.path("version").asText("unknown"));

      // 设置上下文信息

      // 构建OTA报告数据
      Map<String, Object> otaReportData = buildOtaReportData(reportData, topicInfo);

      // 验证上报数据
      if (validateReportData(otaReportData)) {
        OtaServiceResult result = handleReportByType(otaReportData);

        // 根据OTA处理结果设置上下文
        handleOtaServiceResult(request, result, "固件上报");

        // 记录处理统计
        incrementReportCounter(topicInfo.getProductKey(), otaReportData);

        log.debug(
            "[{}] 固件上报处理完成 - 设备: {}, 结果: {}", getName(), topicInfo.getDeviceUniqueId(), result);
        return ProcessorResult.CONTINUE;
      } else {
        log.warn("[{}] 固件上报数据验证失败 - 设备: {}", getName(), topicInfo.getDeviceUniqueId());
        request.setContextValue("validationError", "固件上报数据格式不正确");
        return ProcessorResult.CONTINUE;
      }

    } catch (Exception e) {
      log.error("[{}] 固件上报处理异常 - 设备: {}, 异常: ", getName(), topicInfo.getDeviceUniqueId(), e);
      return ProcessorResult.ERROR;
    }
  }

  private boolean isSessionReport(JsonNode report) {
    if (report == null || !report.isObject()) return false;
    String reportType = report.path("reportType").asText("");
    return report.hasNonNull("sessionId")
        || OtaConstants.ReportType.TRANSFER_ACK.equalsIgnoreCase(reportType)
        || OtaConstants.ReportType.TRANSFER_REQUEST.equalsIgnoreCase(reportType)
        || isSessionReport(report.path("data"));
  }

  /** 处理固件更新 */
  private ProcessorResult processFirmwareUpdate(
      MQTTUPRequest request, MQTTTopicManager.TopicInfo topicInfo) {
    try {
      String payload = request.getPayload();
      JsonNode updateData = objectMapper.readTree(payload);

      log.info(
          "[{}] 处理固件更新 - 设备: {}, 目标版本: {}",
          getName(),
          topicInfo.getDeviceUniqueId(),
          updateData.path("targetVersion").asText("unknown"));

      // 设置上下文信息
      request.setContextValue("messageType", "FIRMWARE_UPDATE");
      request.setContextValue("topicType", "OTA_UPDATE");

      // 构建OTA更新数据
      Map<String, Object> otaUpdateData = buildOtaUpdateData(updateData, topicInfo);

      // 验证更新数据
      if (validateUpdateData(otaUpdateData)) {
        // 调用通用OTA服务处理
        OtaServiceResult result = otaRuntimeService.dispatchUpgradeCommand(otaUpdateData);

        // 根据OTA处理结果设置上下文
        handleOtaServiceResult(request, result, "固件更新");

        // 记录处理统计
        incrementUpdateCounter(topicInfo.getProductKey(), otaUpdateData);

        // 固件更新可能需要回复确认
        if (result == OtaServiceResult.SUCCESS) {
          request.setContextValue("needReply", true);
          request.setContextValue("replyTopic", buildUpdateReplyTopic(topicInfo));
        }

        log.debug(
            "[{}] 固件更新处理完成 - 设备: {}, 结果: {}", getName(), topicInfo.getDeviceUniqueId(), result);
        return ProcessorResult.CONTINUE;
      } else {
        log.warn("[{}] 固件更新数据验证失败 - 设备: {}", getName(), topicInfo.getDeviceUniqueId());
        request.setContextValue("validationError", "固件更新数据格式不正确");
        return ProcessorResult.CONTINUE;
      }

    } catch (Exception e) {
      log.error("[{}] 固件更新处理异常 - 设备: {}, 异常: ", getName(), topicInfo.getDeviceUniqueId(), e);
      return ProcessorResult.ERROR;
    }
  }

  /** 构建OTA报告数据 */
  private Map<String, Object> buildOtaReportData(
      JsonNode reportData, MQTTTopicManager.TopicInfo topicInfo) {
    Map<String, Object> otaData = new HashMap<>();

    // 基本设备信息
    otaData.put("productKey", topicInfo.getProductKey());
    otaData.put("deviceId", topicInfo.getDeviceId());
    otaData.put("protocol", "MQTT");
    otaData.put("timestamp", System.currentTimeMillis());

    // 固件信息
    otaData.put("reportType", reportData.path("reportType").asText(null));
    otaData.put("taskId", reportData.path("taskId").asText(null));
    otaData.put("taskDeviceId", reportData.path("taskDeviceId").asText(null));
    otaData.put("currentVersion", textOrNull(reportData, "currentVersion", "version"));
    otaData.put("targetVersion", textOrNull(reportData, "targetVersion", "toVersion"));
    otaData.put("hardwareVersion", textOrNull(reportData, "hardwareVersion", "hwVersion"));
    otaData.put("packageType", textOrNull(reportData, "packageType", "firmwareType", "type"));
    otaData.put("customType", textOrNull(reportData, "customType", "tag"));
    otaData.put("module", textOrNull(reportData, "module", "component"));
    otaData.put("upgradeStatus", textOrNull(reportData, "upgradeStatus", "status", "result"));
    otaData.put("errorCode", textOrNull(reportData, "errorCode", "code"));
    otaData.put("errorMessage", textOrNull(reportData, "errorMessage", "message", "reason"));
    otaData.put("checksum", reportData.path("checksum").asText());
    otaData.put("fileSize", reportData.path("size").asLong(0));
    if (reportData.has("progress")) {
      otaData.put("progress", reportData.path("progress").asInt());
    } else if (reportData.has("percent")) {
      otaData.put("progress", reportData.path("percent").asInt());
    }

    // 设备状态信息
    otaData.put("batteryLevel", reportData.path("battery").asInt(-1));
    otaData.put("signalStrength", reportData.path("signal").asInt(-1));
    otaData.put("freeSpace", reportData.path("freeSpace").asLong(-1));

    // 扩展属性
    if (reportData.has("properties")) {
      Map<String, Object> properties =
          objectMapper.convertValue(reportData.get("properties"), Map.class);
      otaData.put("properties", properties);
    }

    return otaData;
  }

  /** 构建OTA更新数据 */
  private Map<String, Object> buildOtaUpdateData(
      JsonNode updateData, MQTTTopicManager.TopicInfo topicInfo) {
    Map<String, Object> otaData = new HashMap<>();

    // 基本设备信息
    otaData.put("productKey", topicInfo.getProductKey());
    otaData.put("deviceId", topicInfo.getDeviceId());
    otaData.put("protocol", "MQTT");
    otaData.put("timestamp", System.currentTimeMillis());

    // 升级信息
    otaData.put("taskId", updateData.path("taskId").asText());
    otaData.put("targetVersion", updateData.path("targetVersion").asText());
    otaData.put("packageUrl", updateData.path("url").asText());
    otaData.put("packageSize", updateData.path("size").asLong(0));
    otaData.put("packageChecksum", updateData.path("checksum").asText());
    otaData.put("upgradeType", updateData.path("type").asText("FULL"));

    // 升级策略
    otaData.put("forceUpgrade", updateData.path("force").asBoolean(false));
    otaData.put("retryCount", updateData.path("retryCount").asInt(3));
    otaData.put("timeout", updateData.path("timeout").asInt(3600)); // 默认1小时

    return otaData;
  }

  /** 验证上报数据 */
  private boolean validateReportData(Map<String, Object> reportData) {
    return reportData.containsKey("productKey") && reportData.containsKey("deviceId");
  }

  /** 验证更新数据 */
  private boolean validateUpdateData(Map<String, Object> updateData) {
    // 基本字段验证
    if (!updateData.containsKey("productKey") || !updateData.containsKey("deviceId")) {
      return false;
    }

    String targetVersion = (String) updateData.get("targetVersion");
    String packageUrl = (String) updateData.get("packageUrl");

    return targetVersion != null
        && !targetVersion.trim().isEmpty()
        && packageUrl != null
        && !packageUrl.trim().isEmpty();
  }

  /** 处理OTA服务结果 */
  private void handleOtaServiceResult(
      MQTTUPRequest request, OtaServiceResult result, String operation) {
    request.setContextValue("otaResult", result);
    request.setContextValue("otaOperation", operation);

    if (result == OtaServiceResult.SUCCESS) {
      request.setContextValue("otaSuccess", true);
    } else {
      request.setContextValue("otaSuccess", false);
      request.setContextValue("otaErrorMessage", result.getDescription());

      // 特殊错误处理
      if (result == OtaServiceResult.DEVICE_OFFLINE) {
        request.setContextValue("deviceStatus", "OFFLINE");
      } else if (result == OtaServiceResult.PACKAGE_NOT_FOUND) {
        request.setContextValue("needPackageCheck", true);
      }
    }
  }

  /** 构建更新回复主题 */
  private String buildUpdateReplyTopic(MQTTTopicManager.TopicInfo topicInfo) {
    // 固件更新回复主题：$ota/reply/${productKey}/${deviceId}
    return "$ota/reply/" + topicInfo.getProductKey() + "/" + topicInfo.getDeviceId();
  }

  /** 增加上报计数器 */
  private void incrementReportCounter(String productKey, Map<String, Object> reportData) {
    // TODO: 实现固件上报统计逻辑
    String version = (String) reportData.get("currentVersion");
    log.debug("[{}] 固件上报计数器增加 - 产品: {}, 版本: {}", getName(), productKey, version);
  }

  /** 增加更新计数器 */
  private void incrementUpdateCounter(String productKey, Map<String, Object> updateData) {
    // TODO: 实现固件更新统计逻辑
    String targetVersion = (String) updateData.get("targetVersion");
    log.debug("[{}] 固件更新计数器增加 - 产品: {}, 目标版本: {}", getName(), productKey, targetVersion);
  }

  private OtaServiceResult handleReportByType(Map<String, Object> otaReportData) {
    String reportType = (String) otaReportData.get("reportType");
    if (reportType != null) {
      String normalizedType = reportType.toUpperCase();
      if (normalizedType.contains("RESULT")) {
        return otaRuntimeService.handleUpgradeResult(otaReportData);
      }
      if (normalizedType.contains("PROGRESS")) {
        return otaRuntimeService.handleUpgradeProgress(otaReportData);
      }
    }

    String upgradeStatus = (String) otaReportData.get("upgradeStatus");
    if (upgradeStatus != null) {
      String normalizedStatus = upgradeStatus.toLowerCase();
      if (normalizedStatus.contains("success")
          || normalizedStatus.contains("fail")
          || normalizedStatus.contains("error")
          || normalizedStatus.contains("cancel")) {
        otaReportData.put("reportType", "UPGRADE_RESULT");
        return otaRuntimeService.handleUpgradeResult(otaReportData);
      }
      otaReportData.put("reportType", "UPGRADE_PROGRESS");
      return otaRuntimeService.handleUpgradeProgress(otaReportData);
    }

    if (otaReportData.containsKey("progress")) {
      Integer progress = (Integer) otaReportData.get("progress");
      if (progress != null && progress >= 100) {
        otaReportData.put("upgradeStatus", OtaConstants.DeviceStatus.SUCCESS);
      }
      otaReportData.put("reportType", "UPGRADE_PROGRESS");
      return otaRuntimeService.handleUpgradeProgress(otaReportData);
    }

    otaReportData.put("reportType", "FIRMWARE_INFO");
    return otaRuntimeService.handleFirmwareReport(otaReportData);
  }

  private String textOrNull(JsonNode node, String... fieldNames) {
    for (String fieldName : fieldNames) {
      if (node.has(fieldName) && !node.path(fieldName).isNull()) {
        String value = node.path(fieldName).asText(null);
        if (value != null && !value.trim().isEmpty()) {
          return value;
        }
      }
    }
    return null;
  }

  @Override
  public boolean preCheck(MQTTUPRequest request) {
    return supports(request);
  }

  @Override
  public void postProcess(MQTTUPRequest request, ProcessorResult result) {
    if (result == ProcessorResult.CONTINUE) {
      log.debug("[{}] OTA处理完成 - 设备: {}", getName(), request.getDeviceUniqueId());
    } else {
      log.warn("[{}] OTA处理异常 - 设备: {}, 结果: {}", getName(), request.getDeviceUniqueId(), result);
    }
  }

  @Override
  public void onError(MQTTUPRequest request, Exception e) {
    log.error("[{}] OTA处理器异常 - 设备: {}, 异常: ", getName(), request.getDeviceUniqueId(), e);
    request.setError("OTA处理失败: " + e.getMessage());
  }

  @Override
  public int getPriority() {
    // OTA处理优先级中等
    return 8;
  }
}
