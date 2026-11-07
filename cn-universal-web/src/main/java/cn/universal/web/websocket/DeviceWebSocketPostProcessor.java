package cn.universal.web.websocket;

import cn.universal.admin.system.service.UserWebSocketPushSettingService;
import cn.universal.common.event.EventTopics;
import cn.universal.common.event.RedisEventPublisher;
import cn.universal.core.message.DownRequest;
import cn.universal.persistence.base.IoTDevicePostProcessor;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.bo.DeviceWebSocketPushEvent;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class DeviceWebSocketPostProcessor implements IoTDevicePostProcessor {

  @Resource private DeviceWebSocketEventBuilder deviceWebSocketEventBuilder;

  @Resource private WebSocketPushSessionManager webSocketPushSessionManager;

  @Resource private ClusterWebSocketSubscriptionRegistry clusterWebSocketSubscriptionRegistry;

  @Resource private UserWebSocketPushSettingService userWebSocketPushSettingService;

  @Resource private RedisEventPublisher redisEventPublisher;

  @Override
  public String getName() {
    return "deviceWebSocketPostProcessor";
  }

  @Override
  public int getOrder() {
    return 1000;
  }

  @Override
  public boolean supports(Operation operation) {
    return operation == Operation.CREATE
        || operation == Operation.ONLINE
        || operation == Operation.OFFLINE
        || operation == Operation.COMMAND_RESP;
  }

  @Override
  public void process(Operation operation, IoTDeviceDTO deviceDTO, DownRequest downRequest) {
    if (!userWebSocketPushSettingService.isGlobalEnabled()) {
      return;
    }
    DeviceWebSocketPushEvent event =
        deviceWebSocketEventBuilder.build(operation, deviceDTO, downRequest);
    if (event == null) {
      return;
    }

    boolean hasLocalSubscribers = webSocketPushSessionManager.hasLocalSubscribers(event.getCategory());
    boolean hasRemoteSubscribers =
        clusterWebSocketSubscriptionRegistry.hasRemoteSubscribers(event.getCategory());
    if (!hasLocalSubscribers && !hasRemoteSubscribers) {
      return;
    }

    if (hasLocalSubscribers) {
      webSocketPushSessionManager.broadcast(event);
    }

    if (hasRemoteSubscribers) {
      redisEventPublisher.publishEvent(EventTopics.DEVICE_WEBSOCKET_PUSH, event);
    }
  }
}
