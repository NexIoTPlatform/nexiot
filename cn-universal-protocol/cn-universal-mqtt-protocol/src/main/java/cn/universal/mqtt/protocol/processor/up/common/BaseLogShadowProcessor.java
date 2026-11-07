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

package cn.universal.mqtt.protocol.processor.up.common;

import cn.hutool.core.collection.CollUtil;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.core.metadata.AbstractEventMetadata;
import cn.universal.core.metadata.DeviceMetadata;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.processor.MqttMessageProcessor;
import cn.universal.persistence.base.BaseUPRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * 公共日志和影子处理器基类
 *
 * <p>步骤SIX：日志清晰和入库
 *
 * <p>三种主题类型的公共处理逻辑： - 保存设备日志数据 - 更新设备影子状态 - 处理设备数据持久化
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
public abstract class BaseLogShadowProcessor extends AbstratIoTService
    implements MqttMessageProcessor {

  @Override
  public String getName() {
    return "日志影子处理器-" + getTopicType();
  }

  @Override
  public String getDescription() {
    return "处理" + getTopicType() + "主题的日志记录和影子更新";
  }

  @Override
  public int getOrder() {
    return 9999; // 日志影子处理是第六步
  }

  @Override
  public ProcessorResult process(MQTTUPRequest request) {
    try {
      log.debug("[{}] 开始处理日志和影子，设备: {}", getName(), request.getDeviceId());

      // 1. 获取处理后的请求列表
      List<BaseUPRequest> requestList = request.getUpRequestList();
      if (CollUtil.isEmpty(requestList)) {
        log.debug("[{}] 请求列表为空，跳过日志影子处理", getName());
        return ProcessorResult.CONTINUE;
      }

      // 2. 过滤非调试消息进行处理
      int processedCount = 0;
      int shadowUpdatedCount = 0;

      for (BaseUPRequest upRequest : requestList) {
        if (upRequest != null && !upRequest.isDebug()) {
          // 保存设备日志
          if (saveDeviceLog(upRequest, request)) {
            processedCount++;
          }

          // 更新设备影子
          if (updateDeviceShadow(upRequest, request)) {
            shadowUpdatedCount++;
          }
          // 设备事件
          if (updateEventName(upRequest)) {}
        }
      }

      // 3. 主题类型特定的日志影子处理
      if (!processTopicSpecificLogShadow(request)) {
        log.error("[{}] 主题特定日志影子处理失败", getName());
        return ProcessorResult.ERROR;
      }

      // 4. 更新处理统计
      request.setContextValue("logProcessedCount", processedCount);
      request.setContextValue("shadowUpdatedCount", shadowUpdatedCount);
      request.setContextValue("logShadowProcessed", true);

      log.debug("[{}] 日志影子处理完成，日志: {}, 影子: {}", getName(), processedCount, shadowUpdatedCount);
      return ProcessorResult.CONTINUE;

    } catch (Exception e) {
      log.error("[{}] 日志影子处理异常，设备: {}, 异常: ", getName(), request.getDeviceId(), e);
      return ProcessorResult.ERROR;
    }
  }

  @Override
  public boolean supports(MQTTUPRequest request) {
    // 支持有设备信息和请求列表的消息
    return request.getIoTDeviceDTO() != null
        && request.getIoTProduct() != null
        && CollUtil.isNotEmpty(request.getUpRequestList());
  }

  protected boolean updateEventName(BaseUPRequest upRequest) {
    // 如果是事件，则完善事件名称
    if (MessageType.EVENT.equals(upRequest.getMessageType())) {
      DeviceMetadata deviceMetadata =
          iotProductDeviceService.getDeviceMetadata(upRequest.getProductKey());
      AbstractEventMetadata metadata = deviceMetadata.getEventOrNull(upRequest.getEvent());
      if (metadata != null) {
        upRequest.setEventName(metadata.getName());
      }
    }
    return true;
  }

  /** 保存设备日志 */
  protected boolean saveDeviceLog(BaseUPRequest upRequest, MQTTUPRequest mqttRequest) {
    try {
      if (upRequest == null) {
        return false;
      }

      // 调用IoT服务保存设备日志
      iIoTDeviceDataService.saveDeviceLog(
          upRequest, mqttRequest.getIoTDeviceDTO(), mqttRequest.getIoTProduct());

      log.debug(
          "[{}] 设备日志保存成功 - 产品: {}, 设备: {}, 消息类型: {}",
          getName(),
          upRequest.getProductKey(),
          upRequest.getIotId(),
          upRequest.getMessageType());
      return true;

    } catch (Exception e) {
      log.error(
          "[{}] 设备日志保存异常 - 产品: {}, 设备: {}, 异常: ",
          getName(),
          upRequest.getProductKey(),
          upRequest.getIotId(),
          e);
      return false;
    }
  }

  /** 更新设备影子 */
  protected boolean updateDeviceShadow(BaseUPRequest upRequest, MQTTUPRequest mqttRequest) {
    try {
      if (upRequest == null) {
        return false;
      }

      // 调用影子服务更新设备影子
      iotDeviceShadowService.doShadow(upRequest, mqttRequest.getIoTDeviceDTO());

      log.debug(
          "[{}] 设备影子更新成功 - 产品: {}, 设备: {}",
          getName(),
          upRequest.getProductKey(),
          upRequest.getIotId());
      return true;

    } catch (Exception e) {
      log.error(
          "[{}] 设备影子更新异常 - 产品: {}, 设备: {}, 异常: ",
          getName(),
          upRequest.getProductKey(),
          upRequest.getIotId(),
          e);
      return false;
    }
  }

  /** 收集日志统计信息 */
  protected void collectLogStatistics(MQTTUPRequest request) {
    try {
      List<BaseUPRequest> requestList = request.getUpRequestList();
      if (CollUtil.isEmpty(requestList)) {
        return;
      }

      int totalRequests = requestList.size();
      long debugRequests = requestList.stream().filter(req -> req != null && req.isDebug()).count();
      int normalRequests = totalRequests - (int) debugRequests;

      request.setContextValue("totalLogRequests", totalRequests);
      request.setContextValue("debugLogRequests", (int) debugRequests);
      request.setContextValue("normalLogRequests", normalRequests);

      log.debug(
          "[{}] 日志统计 - 总数: {}, 调试: {}, 普通: {}",
          getName(),
          totalRequests,
          debugRequests,
          normalRequests);

    } catch (Exception e) {
      log.warn("[{}] 日志统计异常: ", getName(), e);
    }
  }

  /** 检查数据敏感性 */
  protected boolean isSensitiveData(BaseUPRequest upRequest) {
    if (upRequest == null) {
      return false;
    }

    // 检查是否包含敏感信息（密码、密钥等）
    String content = upRequest.toString().toLowerCase();
    return content.contains("password")
        || content.contains("secret")
        || content.contains("key")
        || content.contains("token");
  }

  /** 清理敏感数据 */
  protected void sanitizeSensitiveData(BaseUPRequest upRequest) {
    if (isSensitiveData(upRequest)) {
      log.debug("[{}] 检测到敏感数据，进行脱敏处理", getName());
      // 这里可以实现具体的脱敏逻辑
      // 例如：替换敏感字段、加密存储等
    }
  }

  // ==================== 抽象方法，由子类实现 ====================

  /** 获取主题类型名称 */
  protected abstract String getTopicType();

  /** 处理主题类型特定的日志影子逻辑 */
  protected abstract boolean processTopicSpecificLogShadow(MQTTUPRequest request);

  // ==================== 生命周期方法 ====================

  @Override
  public boolean preCheck(MQTTUPRequest request) {
    // 检查必要的数据
    return request.getIoTDeviceDTO() != null && request.getIoTProduct() != null;
  }

  @Override
  public void postProcess(MQTTUPRequest request, ProcessorResult result) {
    if (result == ProcessorResult.CONTINUE) {
      // 收集日志统计信息
      collectLogStatistics(request);

      Integer logCount = (Integer) request.getContextValue("logProcessedCount");
      Integer shadowCount = (Integer) request.getContextValue("shadowUpdatedCount");

      log.debug(
          "[{}] 日志影子处理成功 - 设备: {}, 日志: {}, 影子: {}",
          getName(),
          request.getDeviceId(),
          logCount != null ? logCount : 0,
          shadowCount != null ? shadowCount : 0);
    } else {
      log.warn("[{}] 日志影子处理失败 - 设备: {}, 结果: {}", getName(), request.getDeviceId(), result);
    }
  }

  @Override
  public void onError(MQTTUPRequest request, Exception e) {
    log.error("[{}] 日志影子处理异常，设备: {}, 异常: ", getName(), request.getDeviceId(), e);
    request.setError("日志影子处理失败: " + e.getMessage());
  }
}
