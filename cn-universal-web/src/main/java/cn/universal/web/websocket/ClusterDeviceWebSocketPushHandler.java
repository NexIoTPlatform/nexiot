package cn.universal.web.websocket;

import cn.hutool.json.JSONUtil;
import cn.universal.admin.system.service.UserWebSocketPushSettingService;
import cn.universal.common.event.EventMessage;
import cn.universal.common.event.processer.DeviceWebSocketPushProcessor;
import cn.universal.persistence.entity.bo.DeviceWebSocketPushEvent;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class ClusterDeviceWebSocketPushHandler implements DeviceWebSocketPushProcessor {

  @Resource private WebSocketPushSessionManager webSocketPushSessionManager;

  @Resource private UserWebSocketPushSettingService userWebSocketPushSettingService;

  @Override
  public void handleDeviceWebSocketPush(EventMessage message) {
    if (!userWebSocketPushSettingService.isGlobalEnabled()) {
      return;
    }
    DeviceWebSocketPushEvent event =
        JSONUtil.toBean(JSONUtil.toJsonStr(message.getData()), DeviceWebSocketPushEvent.class);
    webSocketPushSessionManager.broadcast(event);
  }
}
