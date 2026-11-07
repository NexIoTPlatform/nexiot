package cn.universal.admin.platform.scheduler;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.universal.admin.platform.service.IGatewayPollingService;
import cn.universal.persistence.entity.GatewayPollingConfig;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.event.ContextClosedEvent;

class GatewayPollingSchedulerTest {

  private ExecutorService executor;

  @AfterEach
  void tearDown() {
    if (executor != null) {
      executor.shutdownNow();
    }
  }

  @Test
  void shouldSkipScheduledPollingAfterContextClose() throws Exception {
    GatewayPollingScheduler scheduler = new GatewayPollingScheduler();
    IGatewayPollingService service = mock(IGatewayPollingService.class);
    RedissonClient redisson = mock(RedissonClient.class);
    setField(scheduler, "gatewayPollingService", service);
    setField(scheduler, "redissonClient", redisson);
    setField(scheduler, "shutdownTimeoutMs", 1000L);

    scheduler.onContextClosed(mock(ContextClosedEvent.class));
    scheduler.polling30s();

    verify(redisson, never()).getLock(anyString());
    verify(service, never()).getDuePollingDevices(anyInt(), anyString());
  }

  @Test
  void shouldCancelAndWaitForInFlightPollingDuringShutdown() throws Exception {
    GatewayPollingScheduler scheduler = new GatewayPollingScheduler();
    IGatewayPollingService service = mock(IGatewayPollingService.class);
    RedissonClient redisson = mock(RedissonClient.class);
    RLock lock = mock(RLock.class);
    GatewayPollingConfig config = new GatewayPollingConfig();
    config.setDeviceId("device-1");
    CountDownLatch taskStarted = new CountDownLatch(1);
    CountDownLatch taskInterrupted = new CountDownLatch(1);
    executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());

    when(redisson.getLock(anyString())).thenReturn(lock);
    when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(true);
    when(lock.isHeldByCurrentThread()).thenReturn(true);
    when(service.getDuePollingDevices(30, "CLOUD_POLLING")).thenReturn(List.of(config));
    when(service.getDuePollingDevices(30, "MODBUS_COLLECT")).thenReturn(List.of());
    org.mockito.Mockito.doAnswer(
            ignored -> {
              taskStarted.countDown();
              try {
                new CountDownLatch(1).await();
              } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                taskInterrupted.countDown();
              }
              return null;
            })
        .when(service)
        .pollGatewayDevice(config);

    setField(scheduler, "gatewayPollingService", service);
    setField(scheduler, "redissonClient", redisson);
    setField(scheduler, "pollingExecutor", executor);
    setField(scheduler, "maxConcurrency", 1);
    setField(scheduler, "shutdownTimeoutMs", 2000L);

    Thread pollingThread = Thread.ofVirtual().start(scheduler::polling30s);
    assertTrue(taskStarted.await(2, TimeUnit.SECONDS));

    scheduler.onContextClosed(mock(ContextClosedEvent.class));

    assertTrue(taskInterrupted.await(2, TimeUnit.SECONDS));
    pollingThread.join(2000L);
    assertTrue(!pollingThread.isAlive());
    verify(lock).unlock();
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = GatewayPollingScheduler.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
