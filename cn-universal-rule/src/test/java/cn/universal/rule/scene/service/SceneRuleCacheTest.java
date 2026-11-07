package cn.universal.rule.scene.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.persistence.entity.SceneLinkage;
import cn.universal.persistence.mapper.SceneLinkageMapper;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class SceneRuleCacheTest {
  static SceneLinkage scene(long id, String json) {
    return SceneLinkage.builder().id(id).status(0).triggerCondition(json).build();
  }

  static final String CONDITIONS =
      """
      [{"trigger":"device","deviceId":"d","productKey":"p","type":"properties"},
       {"trigger":"device","deviceId":"d","productKey":"p","type":"event"}]
      """;

  @Test
  void indexesDevicePairsDeduplicatesAndCachesEmptyScopes() {
    var mapper = mock(SceneLinkageMapper.class);
    when(mapper.selectEnabledSceneLinkageScope("t1", null))
        .thenReturn(List.of(scene(1, CONDITIONS)));
    when(mapper.selectEnabledSceneLinkageScope("t2", null)).thenReturn(List.of());
    when(mapper.selectEnabledSceneLinkageScope(null, "u"))
        .thenReturn(List.of(scene(2, CONDITIONS)));
    var cache = new SceneRuleCache(mapper, 30, 1000);
    assertEquals(
        List.of(1L, 2L),
        cache.find("p", "d", "t1", "u").stream().map(r -> r.scene().getId()).toList());
    for (int i = 0; i < 1000; i++) {
      assertTrue(cache.find("other", "d", "t1", "u").isEmpty());
      assertTrue(cache.find("p", "d", "t2", null).isEmpty());
    }
    verify(mapper, times(1)).selectEnabledSceneLinkageScope("t1", null);
    verify(mapper, times(1)).selectEnabledSceneLinkageScope("t2", null);
    verify(mapper, times(1)).selectEnabledSceneLinkageScope(null, "u");
    assertTrue(cache.find("p", "d", null, null).isEmpty());
  }

  @Test
  void expiredCacheRecoversWhenNotificationWasLost() {
    var mapper = mock(SceneLinkageMapper.class);
    when(mapper.selectEnabledSceneLinkageScope("t", null))
        .thenReturn(List.of(scene(1, CONDITIONS)), List.of());
    AtomicLong clock = new AtomicLong();
    var cache = new SceneRuleCache(mapper, 30, 1000, clock::get);
    assertEquals(1, cache.find("p", "d", "t", null).size());
    clock.set(TimeUnit.SECONDS.toNanos(31));
    assertTrue(cache.find("p", "d", "t", null).isEmpty());
  }

  @Test
  void invalidationDuringLoadCannotResurrectOldSnapshot() throws Exception {
    var mapper = mock(SceneLinkageMapper.class);
    CountDownLatch loading = new CountDownLatch(1), release = new CountDownLatch(1);
    when(mapper.selectEnabledSceneLinkageScope("t", null))
        .thenAnswer(
            invocation -> {
              loading.countDown();
              assertTrue(release.await(5, TimeUnit.SECONDS));
              return List.of(scene(1, CONDITIONS));
            })
        .thenReturn(List.of());
    var cache = new SceneRuleCache(mapper, 30, 1000);
    try (var threads = Executors.newVirtualThreadPerTaskExecutor()) {
      var first = threads.submit(() -> cache.find("p", "d", "t", null));
      assertTrue(loading.await(5, TimeUnit.SECONDS));
      // 提交回调必须能够在旧加载尚未完成时立即失效，不能等缓存加载锁。
      assertTimeoutPreemptively(java.time.Duration.ofSeconds(1), cache::invalidateLocal);
      release.countDown();
      assertTrue(first.get(5, TimeUnit.SECONDS).isEmpty());
      assertTrue(cache.find("p", "d", "t", null).isEmpty());
    }
  }

  @Test
  void concurrentColdRequestsShareOneDatabaseLoad() throws Exception {
    var mapper = mock(SceneLinkageMapper.class);
    when(mapper.selectEnabledSceneLinkageScope("t", null))
        .thenReturn(List.of(scene(1, CONDITIONS)));
    var cache = new SceneRuleCache(mapper, 30, 1000);
    try (var threads = Executors.newVirtualThreadPerTaskExecutor()) {
      var tasks =
          java.util.stream.IntStream.range(0, 100)
              .mapToObj(i -> threads.submit(() -> cache.find("p", "d", "t", null)))
              .toList();
      for (var task : tasks) assertEquals(1, task.get(5, TimeUnit.SECONDS).size());
    }
    verify(mapper, times(1)).selectEnabledSceneLinkageScope("t", null);
  }

  @Test
  void versionPollRecoversAMissedPubsubMessage() {
    var mapper = mock(SceneLinkageMapper.class);
    when(mapper.selectEnabledSceneLinkageScope("t", null))
        .thenReturn(List.of(scene(1, CONDITIONS)), List.of());
    var redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(values.get("scene:rules:revision")).thenReturn("new-version");
    var cache = new SceneRuleCache(mapper, 30, 1000);
    ReflectionTestUtils.setField(cache, "redis", redis);
    assertEquals(1, cache.find("p", "d", "t", null).size());
    cache.checkRevision();
    assertTrue(cache.find("p", "d", "t", null).isEmpty());
  }

  @Test
  void commitPublishesButRollbackDoesNotAndOtherNodeInvalidates() {
    var mapper = mock(SceneLinkageMapper.class);
    var redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    var cache = new SceneRuleCache(mapper, 30, 1000);
    ReflectionTestUtils.setField(cache, "redis", redis);
    TransactionSynchronizationManager.initSynchronization();
    try {
      cache.invalidateAfterCommit();
      verifyNoInteractions(redis);
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
      verifyNoInteractions(redis);
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
    TransactionSynchronizationManager.initSynchronization();
    try {
      cache.invalidateAfterCommit();
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);
      verify(redis).convertAndSend(eq("scene:rules:invalidated"), anyString());
      verify(values).set(eq("scene:rules:revision"), anyString());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
    when(mapper.selectEnabledSceneLinkageScope("t", null))
        .thenReturn(List.of(scene(1, CONDITIONS)), List.of());
    assertEquals(1, cache.find("p", "d", "t", null).size());
    var notification = mock(Message.class);
    when(notification.getChannel()).thenReturn("scene:rules:invalidated".getBytes());
    when(notification.getBody()).thenReturn("other-node-revision".getBytes());
    cache.onMessage(notification, null);
    assertTrue(cache.find("p", "d", "t", null).isEmpty());
  }
}
