package cn.universal.dm.device.service.push.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.universal.common.event.RedisEventPublisher;
import cn.universal.dm.device.service.UPPushConfigService;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class AppPushConfigChangeNotifierTest {

  @AfterEach
  void cleanup() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
    TransactionSynchronizationManager.setActualTransactionActive(false);
  }

  @Test
  void shouldInvalidateOnlyAfterCommit() throws Exception {
    TrackingPushConfigService configService = new TrackingPushConfigService();
    TrackingPushTransformService transformService = new TrackingPushTransformService();
    TrackingRedisEventPublisher eventPublisher = new TrackingRedisEventPublisher();
    AppPushConfigChangeNotifier notifier = notifier(configService, transformService, eventPublisher);
    TransactionSynchronizationManager.setActualTransactionActive(true);
    TransactionSynchronizationManager.initSynchronization();

    notifier.notifyAppConfigChanged("app-1", "UPDATED");

    assertEquals(0, configService.refreshCount);
    for (TransactionSynchronization synchronization :
        TransactionSynchronizationManager.getSynchronizations()) {
      synchronization.afterCommit();
    }
    assertEquals(1, configService.refreshCount);
    assertEquals(1, transformService.refreshCount);
    assertEquals(1, eventPublisher.publishCount);
  }

  @Test
  void rollbackShouldNotInvalidateCache() throws Exception {
    TrackingPushConfigService configService = new TrackingPushConfigService();
    AppPushConfigChangeNotifier notifier =
        notifier(
            configService, new TrackingPushTransformService(), new TrackingRedisEventPublisher());
    TransactionSynchronizationManager.setActualTransactionActive(true);
    TransactionSynchronizationManager.initSynchronization();

    notifier.notifyAppConfigChanged("app-1", "UPDATED");
    for (TransactionSynchronization synchronization :
        TransactionSynchronizationManager.getSynchronizations()) {
      synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
    }

    assertEquals(0, configService.refreshCount);
  }

  private AppPushConfigChangeNotifier notifier(
      UPPushConfigService configService,
      PushTransformService transformService,
      RedisEventPublisher eventPublisher)
      throws Exception {
    AppPushConfigChangeNotifier notifier = new AppPushConfigChangeNotifier();
    set(notifier, "upPushConfigService", configService);
    set(notifier, "pushTransformService", transformService);
    set(notifier, "redisEventPublisher", eventPublisher);
    return notifier;
  }

  private void set(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static final class TrackingPushConfigService extends UPPushConfigService {
    private int refreshCount;

    @Override
    public void refreshCache(String applicationId) {
      refreshCount++;
    }
  }

  private static final class TrackingPushTransformService extends PushTransformService {
    private int refreshCount;

    @Override
    public void refreshAppCache(String appUniqueId) {
      refreshCount++;
    }
  }

  private static final class TrackingRedisEventPublisher extends RedisEventPublisher {
    private int publishCount;

    @Override
    public void publishEvent(String topic, Object event) {
      publishCount++;
    }
  }
}
