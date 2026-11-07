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
import cn.universal.dm.device.entity.IoTPushResult;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.entity.bo.UPPushBO;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * MQTT推送策略实现
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@Component
public class MqttPushStrategy implements PushStrategy<UPPushBO.MqttPushConfig> {

  private static final long FAILURE_LOG_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(30);

  private final AtomicLong nextFailureWarningNanos = new AtomicLong();

  @Autowired
  @Qualifier("sysMQTTManager")
  private MQTTPushService mqttPushService;

  @Value("${mqtt.cfg.defined.thing:$thing}")
  private String thingPrefix;

  @Override
  public IoTPushResult execute(
      BaseUPRequest request, String messageJson, UPPushBO.MqttPushConfig config) {
    if (config == null) {
      log.warn("[MQTT推送] 配置为空，跳过推送");
      return IoTPushResult.failed(
          request.getIoTDeviceDTO().getThirdPlatform(),
          request.getProductKey(),
          request.getIotId(),
          "MQTT",
          messageJson,
          "配置为空",
          "CONFIG_NULL");
    }
    try {
      long startTime = System.currentTimeMillis();
      String defaultTopic =
          thingPrefix
              + "/"
              + request.getIoTDeviceDTO().getAppId()
              + "/"
              + request.getProductKey()
              + "/"
              + request.getDeviceId();
      String fullTopic = StrUtil.blankToDefault(config.getTopic(), defaultTopic);

      log.debug("[MQTT推送] 推送到Topic: {}", fullTopic);

      boolean accepted =
          mqttPushService.publishMessage(
              fullTopic, messageJson.getBytes(StandardCharsets.UTF_8), 0, false);
      if (!accepted) {
        return IoTPushResult.failed(
            request.getIoTDeviceDTO().getThirdPlatform(),
            request.getProductKey(),
            request.getIotId(),
            "MQTT",
            messageJson,
            "系统MQTT未接受发布请求",
            "PUBLISH_REJECTED");
      }

      return IoTPushResult.success(
          request.getIoTDeviceDTO().getThirdPlatform(),
          request.getProductKey(),
          request.getIotId(),
          "MQTT",
          messageJson,
          System.currentTimeMillis() - startTime);

    } catch (Exception e) {
      logFailure(request.getIotId(), e);
      return IoTPushResult.failed(
          request.getIoTDeviceDTO().getThirdPlatform(),
          request.getProductKey(),
          request.getIotId(),
          "MQTT",
          messageJson,
          "推送异常: " + e.getMessage(),
          "PUSH_EXCEPTION");
    }
  }

  @Override
  public boolean isSupported(UPPushBO.MqttPushConfig config) {
    return config != null && config.isSupport() && config.isEnable();
  }

  private void logFailure(String iotId, Exception exception) {
    long now = System.nanoTime();
    long next = nextFailureWarningNanos.get();
    if (now >= next
        && nextFailureWarningNanos.compareAndSet(next, now + FAILURE_LOG_INTERVAL_NANOS)) {
      log.warn("[MQTT推送] QoS0 单次投递失败: iotId={}", iotId, exception);
      return;
    }
    log.debug("[MQTT推送] QoS0 单次投递失败: iotId={}, error={}", iotId, exception.getMessage());
  }
}
