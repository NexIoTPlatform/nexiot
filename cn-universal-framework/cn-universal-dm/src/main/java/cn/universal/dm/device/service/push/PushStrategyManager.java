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

package cn.universal.dm.device.service.push;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.universal.core.message.UPRequest;
import cn.universal.dm.device.entity.IoTPushResult;
import cn.universal.dm.device.service.push.transform.PushTransformService;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.entity.IoTDeviceSubscribe;
import cn.universal.persistence.entity.bo.UPPushBO;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 推送策略管理器
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@Component
public class PushStrategyManager {

  @Autowired private HttpPushStrategy httpPushStrategy;

  @Autowired private MqttPushStrategy mqttPushStrategy;

  @Autowired private ExternalMqttPushStrategy externalMqttPushStrategy;

  @Autowired private TcpPushStrategy tcpPushStrategy;

  @Autowired private KafkaPushStrategy kafkaPushStrategy;

  @Autowired private RocketMQPushStrategy rocketMQPushStrategy;

  @Autowired private SubscribePushStrategy subscribePushStrategy;

  @Autowired private PushTransformService pushTransformService;

  /**
   * 执行推送
   *
   * @param request 上行请求
   * @param config 推送配置
   * @return 推送结果列表
   */
  public List<IoTPushResult> executePush(BaseUPRequest request, UPPushBO config) {
    if (request == null || config == null) {
      log.warn("[推送策略管理器] 请求或配置为空，跳过推送");
      return List.of();
    }

    if (!hasEnabledChannel(request, config)) {
      return List.of();
    }

    String originalMessageJson = JSONUtil.toJsonStr((UPRequest) request);
    String messageJson = pushTransformService.transformPayload(request, config, originalMessageJson);
    if (messageJson == null) {
      log.warn("[推送策略管理器] 数据转换返回空消息，已跳过推送: {}", request.getIotId());
      return List.of();
    }
    log.debug("[推送策略管理器] 开始推送消息: {}", request.getIotId());

    List<IoTPushResult> results = new ArrayList<>();

    // HTTP 推送
    if (config.getHttp() != null && config.getHttp().isEnable()) {
      if (httpPushStrategy.isSupported(config.getHttp())) {
        IoTPushResult result =
            executeSinglePush(httpPushStrategy, request, messageJson, config.getHttp(), "HTTP");
        if (result != null) {
          results.add(result);
        }
      }
    }

    // MQTT 推送
    if (config.getMqtt() != null && config.getMqtt().isEnable()) {
      if (mqttPushStrategy.isSupported(config.getMqtt())) {
        IoTPushResult result =
            executeSinglePush(mqttPushStrategy, request, messageJson, config.getMqtt(), "MQTT");
        if (result != null) {
          results.add(result);
        }
      }
    }

    // Kafka 推送
    if (config.getExternalMqtt() != null && config.getExternalMqtt().isEnable()) {
      if (externalMqttPushStrategy.isSupported(config.getExternalMqtt())) {
        IoTPushResult result =
            executeSinglePush(
                externalMqttPushStrategy,
                request,
                messageJson,
                config.getExternalMqtt(),
                "ExternalMQTT");
        if (result != null) {
          results.add(result);
        }
      }
    }

    if (config.getTcp() != null && config.getTcp().isEnable()) {
      if (tcpPushStrategy.isSupported(config.getTcp())) {
        IoTPushResult result =
            executeSinglePush(tcpPushStrategy, request, messageJson, config.getTcp(), "TCP");
        if (result != null) {
          results.add(result);
        }
      }
    }

    // Kafka 推送
    if (config.getKafka() != null && config.getKafka().isEnable()) {
      if (kafkaPushStrategy.isSupported(config.getKafka())) {
        IoTPushResult result =
            executeSinglePush(kafkaPushStrategy, request, messageJson, config.getKafka(), "Kafka");
        if (result != null) {
          results.add(result);
        }
      }
    }

    // RocketMQ 推送
    if (config.getRocketMQ() != null && config.getRocketMQ().isEnable()) {
      if (rocketMQPushStrategy.isSupported(config.getRocketMQ())) {
        IoTPushResult result =
            executeSinglePush(
                rocketMQPushStrategy, request, messageJson, config.getRocketMQ(), "RocketMQ");
        if (result != null) {
          results.add(result);
        }
      }
    }

    if (hasEnabledSubscription(request)) {
      IoTPushResult result =
          executeSinglePush(subscribePushStrategy, request, messageJson, null, "Subscribe");
      if (result != null) {
        results.add(result);
      }
    }

    logPushResults(results);

    log.debug("[推送策略管理器] 推送完成: {}", request.getIotId());
    return results;
  }

  public boolean hasEnabledChannel(BaseUPRequest request, UPPushBO config) {
    if (request == null || config == null) {
      return false;
    }
    return (config.getHttp() != null && httpPushStrategy.isSupported(config.getHttp()))
        || (config.getMqtt() != null && mqttPushStrategy.isSupported(config.getMqtt()))
        || (config.getExternalMqtt() != null
            && externalMqttPushStrategy.isSupported(config.getExternalMqtt()))
        || (config.getTcp() != null && tcpPushStrategy.isSupported(config.getTcp()))
        || (config.getKafka() != null && kafkaPushStrategy.isSupported(config.getKafka()))
        || (config.getRocketMQ() != null
            && rocketMQPushStrategy.isSupported(config.getRocketMQ()))
        || hasEnabledSubscription(request);
  }

  private boolean hasEnabledSubscription(BaseUPRequest request) {
    if (!(request.getDevSubscribe() instanceof List<?> subscriptions)) {
      return false;
    }
    return subscriptions.stream()
        .filter(IoTDeviceSubscribe.class::isInstance)
        .map(IoTDeviceSubscribe.class::cast)
        .anyMatch(subscribe -> subscribe.isEnabledFlag() && StrUtil.isNotBlank(subscribe.getUrl()));
  }

  /** 执行单个推送策略 */
  private <C> IoTPushResult executeSinglePush(
      PushStrategy<C> strategy,
      BaseUPRequest request,
      String messageJson,
      C channelConfig,
      String channel) {
    long startTime = System.currentTimeMillis();

    try {
      IoTPushResult result = strategy.execute(request, messageJson, channelConfig);

      // 补充推送结果信息
      if (result != null) {
        result.setDeviceId(request.getIotId());
        result.setProductKey(request.getProductKey());
        result.setChannel(channel);
        result.setResponseTime(System.currentTimeMillis() - startTime);
        result.setRequestId(request.getIotId() + "_" + System.currentTimeMillis());
      }

      return result;
    } catch (Exception e) {
      log.error("[推送策略管理器] {} 推送失败: {}", channel, request.getIotId(), e);

      // 创建失败结果 - 从设备配置中获取平台信息
      String platform = null;
      if (request.getIoTDeviceDTO() != null) {
        platform = request.getIoTDeviceDTO().getThirdPlatform();
      }

      return IoTPushResult.failed(
          platform, // 从设备配置中获取平台信息
          request.getProductKey(),
          request.getIotId(),
          channel,
          messageJson,
          e.getMessage(),
          "PUSH_ERROR");
    }
  }

  /** 处理推送结果 */
  private void logPushResults(List<IoTPushResult> results) {
    if (results == null || results.isEmpty()) {
      return;
    }

    for (IoTPushResult result : results) {
      try {
        if (result != null) {
          if (!result.isOk()) {
            log.debug(
                "[推送策略管理器] 推送失败: deviceId={}, channel={}, error={}",
                result.getDeviceId(),
                result.getChannel(),
                result.getErrorMessage());
          } else {
            log.debug(
                "[推送策略管理器] 推送成功: deviceId={}, channel={}, responseTime={}ms",
                result.getDeviceId(),
                result.getChannel(),
                result.getResponseTime());
          }
        }
      } catch (Exception e) {
        log.error(
            "[推送策略管理器] 处理推送结果失败: deviceId={}, channel={}",
            result.getDeviceId(),
            result.getChannel(),
            e);
      }
    }
  }

  /**
   * 批量推送
   *
   * @param requests 请求列表
   * @param config 推送配置
   * @return 所有推送结果
   */
  public List<IoTPushResult> executeBatchPush(List<BaseUPRequest> requests, UPPushBO config) {
    if (requests == null || requests.isEmpty()) {
      log.warn("[推送策略管理器] 请求列表为空，跳过批量推送");
      return null;
    }

    log.debug("[推送策略管理器] 开始批量推送，消息数量: {}", requests.size());

    List<IoTPushResult> allResults = new ArrayList<>();

    for (BaseUPRequest request : requests) {
      try {
        List<IoTPushResult> results = executePush(request, config);
        if (results != null) {
          allResults.addAll(results);
        }
      } catch (Exception e) {
        log.error("[推送策略管理器] 单条消息推送失败: {}", request.getIotId(), e);

        // 创建失败结果
        IoTPushResult failedResult =
            IoTPushResult.failed(
                request.getIoTDeviceDTO().getThirdPlatform(), // 平台信息暂时设为null，后续可以从配置中获取
                request.getProductKey(),
                request.getIotId(),
                "BATCH_PUSH",
                JSONUtil.toJsonStr(request),
                e.getMessage(),
                "BATCH_PUSH_ERROR");
        allResults.add(failedResult);
      }
    }

    log.debug("[推送策略管理器] 批量推送完成，消息数量: {}, 结果数量: {}", requests.size(), allResults.size());
    return allResults;
  }
}
