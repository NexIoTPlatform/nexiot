package cn.universal.dm.device.service.push;

import cn.hutool.core.util.StrUtil;
import cn.universal.dm.device.entity.IoTPushResult;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.entity.bo.UPPushBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExternalMqttPushStrategy
    implements PushStrategy<UPPushBO.ExternalMqttPushConfig> {

  private final ExternalMqttClientManager clientManager;

  public ExternalMqttPushStrategy(ExternalMqttClientManager clientManager) {
    this.clientManager = clientManager;
  }

  @Override
  public IoTPushResult execute(
      BaseUPRequest request, String messageJson, UPPushBO.ExternalMqttPushConfig config) {
    String appUniqueId = resolveAppUniqueId(request);
    String topic = resolveTopic(config.getTopic(), request, appUniqueId);
    long start = System.currentTimeMillis();
    try {
      clientManager.publish(appUniqueId, config, topic, messageJson);
      log.debug(
          "[外部MQTT推送] 推送成功: appUniqueId={}, deviceId={}, topic={}, qos={}, retained={}, elapsedMs={},payload={}",
          appUniqueId,
          request.getIotId(),
          topic,
          config.getQos(),
          config.isRetained(),
          System.currentTimeMillis() - start,messageJson);
      return IoTPushResult.success(
          platform(request),
          request.getProductKey(),
          request.getIotId(),
          "ExternalMQTT",
          messageJson,
          System.currentTimeMillis() - start);
    } catch (Exception e) {
      log.debug(
          "[外部MQTT推送] 失败: appUniqueId={}, deviceId={}, error={}",
          appUniqueId,
          request.getIotId(),
          e.getMessage(),
          e);
      return IoTPushResult.failed(
          platform(request),
          request.getProductKey(),
          request.getIotId(),
          "ExternalMQTT",
          messageJson,
          "外部MQTT推送失败: " + e.getMessage(),
          "EXTERNAL_MQTT_ERROR");
    }
  }

  @Override
  public boolean isSupported(UPPushBO.ExternalMqttPushConfig config) {
    return config != null
        && config.isSupport()
        && config.isEnable()
        && StrUtil.isNotBlank(config.getBrokerUrl())
        && StrUtil.isNotBlank(config.getTopic());
  }

  private String resolveTopic(String template, BaseUPRequest request, String appUniqueId) {
    return template
        .replace("${applicationId}", StrUtil.nullToEmpty(appUniqueId))
        .replace("${productKey}", StrUtil.nullToEmpty(request.getProductKey()))
        .replace("${deviceId}", StrUtil.nullToEmpty(request.getDeviceId()))
        .replace("${iotId}", StrUtil.nullToEmpty(request.getIotId()))
        .replace(
            "${messageType}", request.getMessageType() == null ? "" : request.getMessageType().name());
  }

  private String resolveAppUniqueId(BaseUPRequest request) {
    if (StrUtil.isNotBlank(request.getApplicationId())) {
      return request.getApplicationId();
    }
    if (request.getIoTDeviceDTO() != null
        && StrUtil.isNotBlank(request.getIoTDeviceDTO().getApplicationId())) {
      return request.getIoTDeviceDTO().getApplicationId();
    }
    throw new IllegalArgumentException("应用标识不能为空");
  }

  private String platform(BaseUPRequest request) {
    return request.getIoTDeviceDTO() == null
        ? null
        : request.getIoTDeviceDTO().getThirdPlatform();
  }
}
