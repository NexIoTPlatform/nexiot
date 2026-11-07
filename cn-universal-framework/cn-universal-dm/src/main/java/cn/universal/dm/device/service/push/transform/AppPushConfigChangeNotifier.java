package cn.universal.dm.device.service.push.transform;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.event.EventTopics;
import cn.universal.common.event.RedisEventPublisher;
import cn.universal.dm.device.service.UPPushConfigService;
import cn.universal.dm.device.service.push.ExternalMqttClientManager;
import cn.universal.dm.device.service.push.TcpPushClientManager;
import cn.universal.persistence.entity.bo.AppPushConfigEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
public class AppPushConfigChangeNotifier {

  @Resource private UPPushConfigService upPushConfigService;

  @Resource private PushTransformService pushTransformService;

  @Resource private ExternalMqttClientManager externalMqttClientManager;

  @Resource private TcpPushClientManager tcpPushClientManager;

  @Resource private RedisEventPublisher redisEventPublisher;

  public void notifyAppConfigChanged(String appUniqueId, String reason) {
    notifyAppConfigChanged(appUniqueId, reason, null);
  }

  public void notifyAppConfigChanged(String appUniqueId, String reason, Long ruleId) {
    if (StrUtil.isBlank(appUniqueId)) {
      return;
    }
    Runnable notification = () -> publishChange(appUniqueId, reason, ruleId);
    if (TransactionSynchronizationManager.isActualTransactionActive()
        && TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              notification.run();
            }
          });
      return;
    }
    notification.run();
  }

  private void publishChange(String appUniqueId, String reason, Long ruleId) {
    upPushConfigService.refreshCache(appUniqueId);
    pushTransformService.refreshAppCache(appUniqueId);
    if (externalMqttClientManager != null) {
      externalMqttClientManager.invalidate(appUniqueId);
    }
    if (tcpPushClientManager != null) {
      tcpPushClientManager.invalidate(appUniqueId);
    }
    redisEventPublisher.publishEvent(
        EventTopics.APP_PUSH_CONFIG_UPDATED,
        AppPushConfigEvent.builder()
            .appUniqueId(appUniqueId)
            .reason(reason)
            .ruleId(ruleId)
            .build());
    log.info(
        "[数据转换] 已发布应用推送配置变更事件: appUniqueId={}, reason={}, ruleId={}",
        appUniqueId,
        reason,
        ruleId);
  }
}
