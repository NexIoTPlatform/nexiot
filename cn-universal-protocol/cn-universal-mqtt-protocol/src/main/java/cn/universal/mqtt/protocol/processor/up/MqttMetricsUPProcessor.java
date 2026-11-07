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

package cn.universal.mqtt.protocol.processor.up;

import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.metrics.MqttMetricsMananer;
import cn.universal.mqtt.protocol.processor.MqttMessageProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MQTT指标统计处理器
 *
 * <p>收集MQTT消息处理的各项指标 支持多维度统计：连接、消息、产品、主题、设备等
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
@Component
public class MqttMetricsUPProcessor implements MqttMessageProcessor {

  @Autowired private MqttMetricsMananer metricsCollector;

  @Override
  public String getName() {
    return "MQTT指标统计处理器";
  }

  @Override
  public String getDescription() {
    return "收集MQTT消息处理的各项指标";
  }

  @Override
  public int getOrder() {
    return 600;
  }

  @Override
  public boolean supports(MQTTUPRequest request) {
    // 支持所有消息，进行统计
    return true;
  }

  @Override
  public ProcessorResult process(MQTTUPRequest request) {
    try {
      log.debug(
          "[{}] 开始收集指标，设备: {}, 主题: {}", getName(), request.getDeviceId(), request.getUpTopic());

      // 1. 基础消息指标
      collectBasicMetrics(request);

      // 2. 设备指标
      collectDeviceMetrics(request);

      // 3. 主题指标
      collectTopicMetrics(request);

      // 4. 产品指标
      collectProductMetrics(request);

      // 5. QoS指标
      collectQosMetrics(request);

      // 6. 处理时间指标
      collectProcessingMetrics(request);

      log.debug("[{}] 指标收集完成，设备: {}", getName(), request.getDeviceId());
      return ProcessorResult.CONTINUE;

    } catch (Exception e) {
      log.error("[{}] 指标收集异常，设备: {}, 异常: ", getName(), request.getDeviceId(), e);
      // 指标收集失败不应该影响消息处理
      return ProcessorResult.CONTINUE;
    }
  }

  /** 收集基础消息指标 */
  private void collectBasicMetrics(MQTTUPRequest request) {
    try {
      // 消息总数
      metricsCollector.incrementMessageCount();

      // 订阅消息数
      metricsCollector.incrementSubscribeMessageCount();

      // 消息大小分布
      int payloadSize = request.getPayload().length();
      metricsCollector.recordMessageSize(payloadSize);

      // QoS分布
      metricsCollector.recordQosDistribution(request.getQos());

      // 保留消息统计
      if (request.isRetained()) {
        metricsCollector.incrementRetainedMessageCount();
      }

      // 重复消息统计
      if (request.isDuplicate()) {
        metricsCollector.incrementDuplicateMessageCount();
      }

      log.debug("[{}] 基础消息指标收集完成 - 大小: {}, QoS: {}", getName(), payloadSize, request.getQos());

    } catch (Exception e) {
      log.warn("[{}] 基础指标收集失败: ", getName(), e);
    }
  }

  /** 收集设备指标 */
  private void collectDeviceMetrics(MQTTUPRequest request) {
    try {
      String deviceId = request.getDeviceId();
      String productKey = request.getProductKey();

      if (deviceId != null && productKey != null) {
        // 设备消息数
        metricsCollector.incrementDeviceMessageCount(productKey, deviceId);

        // 设备活跃度
        metricsCollector.recordDeviceActivity(productKey, deviceId);

        // 设备在线状态
        metricsCollector.updateDeviceOnlineStatus(productKey, deviceId, true);

        log.debug("[{}] 设备指标收集完成 - 产品: {}, 设备: {}", getName(), productKey, deviceId);
      }

    } catch (Exception e) {
      log.warn("[{}] 设备指标收集失败: ", getName(), e);
    }
  }

  /** 收集主题指标 */
  private void collectTopicMetrics(MQTTUPRequest request) {
    try {
      String topic = request.getUpTopic();
      if (topic != null) {
        // 主题消息数
        metricsCollector.incrementTopicMessageCount(topic);

        // 主题类型统计
        String topicType = (String) request.getContextValue("topicType");
        if (topicType != null) {
          metricsCollector.incrementTopicTypeCount(topicType);
        }

        // 主题层级分布
        Integer topicLevel = (Integer) request.getContextValue("topicLevel");
        if (topicLevel != null) {
          metricsCollector.recordTopicLevelDistribution(topicLevel);
        }

        log.debug("[{}] 主题指标收集完成 - 主题: {}, 类型: {}", getName(), topic, topicType);
      }

    } catch (Exception e) {
      log.warn("[{}] 主题指标收集失败: ", getName(), e);
    }
  }

  /** 收集产品指标 */
  private void collectProductMetrics(MQTTUPRequest request) {
    try {
      String productKey = request.getProductKey();
      if (productKey != null) {
        // 产品消息数
        metricsCollector.incrementProductMessageCount(productKey);

        // 产品活跃设备数
        String deviceId = request.getDeviceId();
        if (deviceId != null) {
          metricsCollector.recordActiveDevice(productKey, deviceId);
        }

        log.debug("[{}] 产品指标收集完成 - 产品: {}", getName(), productKey);
      }

    } catch (Exception e) {
      log.warn("[{}] 产品指标收集失败: ", getName(), e);
    }
  }

  /** 收集QoS指标 */
  private void collectQosMetrics(MQTTUPRequest request) {
    try {
      int qos = request.getQos();

      // QoS级别统计
      switch (qos) {
        case 0:
          metricsCollector.incrementQos0MessageCount();
          break;
        case 1:
          metricsCollector.incrementQos1MessageCount();
          break;
        case 2:
          metricsCollector.incrementQos2MessageCount();
          break;
        default:
          log.warn("[{}] 未知QoS级别: {}", getName(), qos);
      }

      log.debug("[{}] QoS指标收集完成 - QoS: {}", getName(), qos);

    } catch (Exception e) {
      log.warn("[{}] QoS指标收集失败: ", getName(), e);
    }
  }

  /** 收集处理时间指标 */
  private void collectProcessingMetrics(MQTTUPRequest request) {
    try {
      Long startTime = null;
      if (startTime != null && startTime > 0) {
        long processingTime = System.currentTimeMillis() - startTime;

        // 记录处理时间
        metricsCollector.recordProcessingTime(processingTime);

        // 处理时间分布
        if (processingTime < 10) {
          metricsCollector.incrementFastProcessingCount();
        } else if (processingTime < 100) {
          metricsCollector.incrementNormalProcessingCount();
        } else {
          metricsCollector.incrementSlowProcessingCount();
        }

        log.debug("[{}] 处理时间指标收集完成 - 耗时: {}ms", getName(), processingTime);
      }

    } catch (Exception e) {
      log.warn("[{}] 处理时间指标收集失败: ", getName(), e);
    }
  }

  @Override
  public boolean preCheck(MQTTUPRequest request) {
    // 指标收集不需要特别的前置条件
    return true;
  }

  @Override
  public void postProcess(MQTTUPRequest request, ProcessorResult result) {
    try {
      // 根据处理结果收集成功/失败指标
      if (result == ProcessorResult.CONTINUE || result == ProcessorResult.STOP) {
        metricsCollector.incrementSuccessfulProcessingCount();
      } else if (result == ProcessorResult.ERROR) {
        metricsCollector.incrementFailedProcessingCount();
      } else if (result == ProcessorResult.SKIP) {
        metricsCollector.incrementSkippedProcessingCount();
      }

      log.debug("[{}] 后置指标收集完成 - 结果: {}", getName(), result);

    } catch (Exception e) {
      log.warn("[{}] 后置指标收集失败: ", getName(), e);
    }
  }

  @Override
  public void onError(MQTTUPRequest request, Exception e) {
    try {
      // 收集错误指标
      metricsCollector.incrementErrorCount();
      metricsCollector.recordError(e.getClass().getSimpleName(), e.getMessage());

      log.error("[{}] 指标收集异常，设备: {}, 异常: ", getName(), request.getDeviceId(), e);

    } catch (Exception ex) {
      log.error("[{}] 错误指标收集失败: ", getName(), ex);
    }
  }

  @Override
  public boolean isRequired() {
    // 指标收集通常是必需的
    return true;
  }

  @Override
  public int getPriority() {
    // 指标收集优先级较高
    return 10;
  }
}
