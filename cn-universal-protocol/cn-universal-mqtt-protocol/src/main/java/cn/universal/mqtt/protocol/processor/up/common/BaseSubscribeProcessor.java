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
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.processor.MqttMessageProcessor;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.entity.IoTDeviceSubscribe;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * 公共订阅处理器基类
 *
 * <p>步骤FIVE：以及上层应用数据订阅的回填
 *
 * <p>三种主题类型的公共处理逻辑： - 查询设备订阅配置 - 回填订阅URL信息 - 准备数据推送配置
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
public abstract class BaseSubscribeProcessor extends AbstratIoTService
    implements MqttMessageProcessor {

  @Override
  public String getName() {
    return "订阅处理器-" + getTopicType();
  }

  @Override
  public String getDescription() {
    return "处理" + getTopicType() + "主题的数据订阅回填";
  }

  @Override
  public int getOrder() {
    return 500; // 订阅处理是第五步
  }

  @Override
  public ProcessorResult process(MQTTUPRequest request) {
    try {
      log.debug("[{}] 开始处理订阅回填，设备: {}", getName(), request.getDeviceId());

      // 1. 获取处理后的请求列表
      List<BaseUPRequest> requestList = request.getUpRequestList();
      if (CollUtil.isEmpty(requestList)) {
        log.debug("[{}] 请求列表为空，跳过订阅处理", getName());
        return ProcessorResult.CONTINUE;
      }

      // 2. 为每个请求回填订阅信息
      int processedCount = 0;
      for (BaseUPRequest upRequest : requestList) {
        if (processSubscribeForRequest(upRequest)) {
          processedCount++;
        }
      }

      // 3. 主题类型特定的订阅处理
      if (!processTopicSpecificSubscribe(request)) {
        log.error("[{}] 主题特定订阅处理失败", getName());
        return ProcessorResult.ERROR;
      }

      // 4. 更新处理统计
      request.setContextValue("subscribeProcessedCount", processedCount);
      request.setContextValue("subscribeProcessed", true);

      log.debug("[{}] 订阅处理完成，处理数量: {}", getName(), processedCount);
      return ProcessorResult.CONTINUE;

    } catch (Exception e) {
      log.error("[{}] 订阅处理异常，设备: {}, 异常: ", getName(), request.getDeviceId(), e);
      return ProcessorResult.ERROR;
    }
  }

  @Override
  public boolean supports(MQTTUPRequest request) {
    // 支持有设备信息和请求列表的消息
    return request.getIoTDeviceDTO() != null && CollUtil.isNotEmpty(request.getUpRequestList());
  }

  /** 为单个请求处理订阅信息 */
  protected boolean processSubscribeForRequest(BaseUPRequest upRequest) {
    try {
      if (upRequest == null) {
        return false;
      }

      String productKey = upRequest.getProductKey();
      String iotId = upRequest.getIotId();
      MessageType messageType = upRequest.getMessageType();

      // 查询订阅配置
      List<IoTDeviceSubscribe> subscribeList = querySubscribeUrl(productKey, iotId, messageType);

      if (CollUtil.isNotEmpty(subscribeList)) {
        upRequest.setDevSubscribe(subscribeList);

        log.debug(
            "[{}] 订阅信息回填成功 - 产品: {}, 设备: {}, 消息类型: {}, 订阅数: {}",
            getName(),
            productKey,
            iotId,
            messageType,
            subscribeList.size());
        return true;
      } else {
        log.debug(
            "[{}] 无订阅配置 - 产品: {}, 设备: {}, 消息类型: {}", getName(), productKey, iotId, messageType);
        return false;
      }

    } catch (Exception e) {
      log.error("[{}] 单个请求订阅处理异常: ", getName(), e);
      return false;
    }
  }

  /** 统计订阅信息 */
  protected void collectSubscribeStatistics(MQTTUPRequest request) {
    try {
      List<BaseUPRequest> requestList = request.getUpRequestList();
      if (CollUtil.isEmpty(requestList)) {
        return;
      }

      int totalSubscribes = 0;
      int activeSubscribes = 0;

      for (BaseUPRequest upRequest : requestList) {
        Object subscribeObj = upRequest.getDevSubscribe();
        if (subscribeObj instanceof List) {
          @SuppressWarnings("unchecked")
          List<IoTDeviceSubscribe> subscribeList = (List<IoTDeviceSubscribe>) subscribeObj;
          if (CollUtil.isNotEmpty(subscribeList)) {
            totalSubscribes += subscribeList.size();

            // 统计活跃订阅
            long active =
                subscribeList.stream()
                    .filter(IoTDeviceSubscribe::isEnabledFlag)
                    .count();
            activeSubscribes += active;
          }
        }

        request.setContextValue("totalSubscribes", totalSubscribes);
        request.setContextValue("activeSubscribes", activeSubscribes);

        log.debug("[{}] 订阅统计 - 总数: {}, 活跃: {}", getName(), totalSubscribes, activeSubscribes);
      }
    } catch (Exception e) {
      log.warn("[{}] 订阅统计异常: ", getName(), e);
    }
  }

  // ==================== 抽象方法，由子类实现 ====================

  /** 获取主题类型名称 */
  protected abstract String getTopicType();

  /** 处理主题类型特定的订阅逻辑 */
  protected abstract boolean processTopicSpecificSubscribe(MQTTUPRequest request);

  // ==================== 生命周期方法 ====================

  @Override
  public boolean preCheck(MQTTUPRequest request) {
    // 检查必要的数据
    return request.getIoTDeviceDTO() != null;
  }

  @Override
  public void postProcess(MQTTUPRequest request, ProcessorResult result) {
    if (result == ProcessorResult.CONTINUE) {
      // 收集订阅统计信息
      collectSubscribeStatistics(request);

      Integer processedCount = (Integer) request.getContextValue("subscribeProcessedCount");
      log.debug(
          "[{}] 订阅处理成功 - 设备: {}, 处理数量: {}",
          getName(),
          request.getDeviceId(),
          processedCount != null ? processedCount : 0);
    } else {
      log.warn("[{}] 订阅处理失败 - 设备: {}, 结果: {}", getName(), request.getDeviceId(), result);
    }
  }

  @Override
  public void onError(MQTTUPRequest request, Exception e) {
    log.error("[{}] 订阅处理异常，设备: {}, 异常: ", getName(), request.getDeviceId(), e);
    request.setError("订阅处理失败: " + e.getMessage());
  }
}
