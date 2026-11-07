package cn.universal.dm.device.service.push.transform;

import cn.hutool.json.JSONUtil;
import cn.universal.common.event.EventMessage;
import cn.universal.common.event.processer.AppPushConfigProcessor;
import cn.universal.dm.device.service.UPPushConfigService;
import cn.universal.dm.device.service.push.ExternalMqttClientManager;
import cn.universal.dm.device.service.push.TcpPushClientManager;
import cn.universal.persistence.entity.bo.AppPushConfigEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ClusterAppPushConfigEventHandler implements AppPushConfigProcessor {

  @Resource private UPPushConfigService upPushConfigService;

  @Resource private PushTransformService pushTransformService;

  @Resource private ExternalMqttClientManager externalMqttClientManager;

  @Resource private TcpPushClientManager tcpPushClientManager;

  @Override
  public void handleAppPushConfigUpdated(EventMessage message) {
    AppPushConfigEvent event =
        JSONUtil.toBean(JSONUtil.toJsonStr(message.getData()), AppPushConfigEvent.class);
    if (event == null || event.getAppUniqueId() == null) {
      return;
    }
    upPushConfigService.refreshCache(event.getAppUniqueId());
    pushTransformService.refreshAppCache(event.getAppUniqueId());
    if (externalMqttClientManager != null) {
      externalMqttClientManager.invalidate(event.getAppUniqueId());
    }
    if (tcpPushClientManager != null) {
      tcpPushClientManager.invalidate(event.getAppUniqueId());
    }
    log.info(
        "[数据转换] 收到集群缓存刷新事件: appUniqueId={}, reason={}, ruleId={}",
        event.getAppUniqueId(),
        event.getReason(),
        event.getRuleId());
  }
}
