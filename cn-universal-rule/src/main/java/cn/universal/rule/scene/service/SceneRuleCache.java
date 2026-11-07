package cn.universal.rule.scene.service;

import cn.hutool.json.JSONUtil;
import cn.universal.persistence.entity.SceneLinkage;
import cn.universal.persistence.entity.bo.TriggerBO;
import cn.universal.persistence.mapper.SceneLinkageMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/** 按租户加载、按产品和设备查找。缓存空结果，避免无规则设备反复访问数据库。 */
@Component
@Slf4j
public class SceneRuleCache implements MessageListener, MeterBinder {
  private static final String TOPIC = "scene:rules:invalidated";
  private static final String REVISION_KEY = "scene:rules:revision";
  private final SceneLinkageMapper mapper;
  private final Cache<Scope, Index> cache;
  private final Semaphore loadPermits;
  private final AtomicBoolean versionReadFailed = new AtomicBoolean();
  private volatile String observedRevision;
  private final AtomicLong generation = new AtomicLong();
  @Resource private StringRedisTemplate redis;
  @Resource private RedisMessageListenerContainer listenerContainer;

  @Autowired
  public SceneRuleCache(
      SceneLinkageMapper mapper,
      @Value("${nexiot.scene.cache-ttl-seconds:30}") long ttlSeconds,
      @Value("${nexiot.scene.cache-max-weight:100000}") long maxWeight,
      @Value("${nexiot.scene.cache-max-concurrent-loads:2}") int maxLoads) {
    this(mapper, ttlSeconds, maxWeight, maxLoads, Ticker.systemTicker());
  }

  SceneRuleCache(SceneLinkageMapper mapper, long ttlSeconds, long maxWeight) {
    this(mapper, ttlSeconds, maxWeight, 2, Ticker.systemTicker());
  }

  SceneRuleCache(SceneLinkageMapper mapper, long ttlSeconds, long maxWeight, Ticker ticker) {
    this(mapper, ttlSeconds, maxWeight, 2, ticker);
  }

  private SceneRuleCache(
      SceneLinkageMapper mapper, long ttlSeconds, long maxWeight, int maxLoads, Ticker ticker) {
    this.mapper = mapper;
    loadPermits = new Semaphore(Math.max(1, maxLoads));
    cache =
        Caffeine.newBuilder()
            .ticker(ticker)
            .recordStats()
            .expireAfterWrite(Duration.ofSeconds(Math.max(1, ttlSeconds)))
            .maximumWeight(Math.max(1, maxWeight))
            .weigher((Scope key, Index index) -> index.weight())
            .build();
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    CaffeineCacheMetrics.monitor(registry, cache, "sceneRules");
    registry.gauge("scene.cache.loads.waiting", loadPermits, Semaphore::getQueueLength);
  }

  @PostConstruct
  public void subscribe() {
    listenerContainer.addMessageListener(this, new ChannelTopic(TOPIC));
  }

  @PreDestroy
  public void unsubscribe() {
    listenerContainer.removeMessageListener(this);
  }

  public List<Rule> find(String productKey, String deviceId, String tenantId, String creatorId) {
    if (!StringUtils.hasText(productKey)
        || !StringUtils.hasText(deviceId)
        || (!StringUtils.hasText(tenantId) && !StringUtils.hasText(creatorId))) {
      return List.of();
    }
    DeviceKey device = new DeviceKey(productKey, deviceId);
    // 失效期间正在加载的旧快照不能被后续查询重新使用。
    while (true) {
      long version = generation.get();
      List<Rule> tenantRules =
          StringUtils.hasText(tenantId)
              ? cache
                  .get(new Scope(version, tenantId, null), this::load)
                  .devices()
                  .getOrDefault(device, List.of())
              : List.of();
      List<Rule> legacyRules =
          StringUtils.hasText(creatorId)
              ? cache
                  .get(new Scope(version, null, creatorId), this::load)
                  .devices()
                  .getOrDefault(device, List.of())
              : List.of();
      if (version == generation.get()) {
        if (tenantRules.isEmpty()) return legacyRules;
        if (legacyRules.isEmpty()) return tenantRules;
        List<Rule> result = new ArrayList<>(tenantRules.size() + legacyRules.size());
        result.addAll(tenantRules);
        result.addAll(legacyRules);
        return result;
      }
    }
  }

  private Index load(Scope scope) {
    try {
      loadPermits.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("场景规则加载被中断", e);
    }
    try {
      return loadIndex(scope);
    } finally {
      loadPermits.release();
    }
  }

  private Index loadIndex(Scope scope) {
    Map<DeviceKey, List<Rule>> devices = new HashMap<>();
    int weight = 1;
    for (SceneLinkage scene :
        mapper.selectEnabledSceneLinkageScope(scope.tenantId(), scope.creatorId())) {
      if (!Integer.valueOf(0).equals(scene.getStatus())) continue;
      try {
        List<TriggerBO> triggers =
            List.copyOf(JSONUtil.toList(scene.getTriggerCondition(), TriggerBO.class));
        Rule rule = new Rule(scene, triggers);
        // 相同设备的多个触发器仅执行一次场景。
        triggers.stream()
            .filter(t -> "device".equals(t.getTrigger()))
            .filter(
                t -> StringUtils.hasText(t.getDeviceId()) && StringUtils.hasText(t.getProductKey()))
            .map(t -> new DeviceKey(t.getProductKey(), t.getDeviceId()))
            .distinct()
            .forEach(key -> devices.computeIfAbsent(key, ignored -> new ArrayList<>()).add(rule));
        long bytes =
            (long) Objects.toString(scene.getTriggerCondition(), "").length()
                + Objects.toString(scene.getExecAction(), "").length();
        weight =
            (int) Math.min(Integer.MAX_VALUE, (long) weight + 1 + triggers.size() + bytes / 512);
      } catch (RuntimeException e) {
        log.warn("场景触发条件无效，跳过规则,sceneId={}", scene.getId(), e);
      }
    }
    devices.replaceAll((key, value) -> List.copyOf(value));
    return new Index(Map.copyOf(devices), weight);
  }

  public void invalidateLocal() {
    generation.incrementAndGet();
    // 不在 afterCommit 中等待正在加载的 cache 节点锁：事务连接此时尚未归还，
    // 同步清空可能与等待数据库连接的加载形成互相等待。旧代条目由 TTL/容量淘汰回收。
  }

  /** 保存、删除和产品重命名必须在事务提交后通知，回滚不影响当前规则。 */
  public void invalidateAfterCommit() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      if (TransactionSynchronizationManager.getSynchronizations().stream()
          .noneMatch(s -> s instanceof InvalidationSynchronization)) {
        TransactionSynchronizationManager.registerSynchronization(
            new InvalidationSynchronization());
      }
    } else {
      publishInvalidation();
    }
  }

  private void publishInvalidation() {
    invalidateLocal();
    try {
      String revision = UUID.randomUUID().toString();
      // 版本号不设 TTL：节点漏收 Pub/Sub 后仍能发现规则变化。
      redis.opsForValue().set(REVISION_KEY, revision);
      observedRevision = revision;
      redis.convertAndSend(TOPIC, revision);
    } catch (RuntimeException e) {
      // 写入已经提交，不能向调用方报告保存失败；其他节点由 TTL 兜底。
      log.warn("场景缓存失效通知失败，其他节点将在缓存过期后刷新", e);
    }
  }

  @Scheduled(
      fixedDelayString = "${nexiot.scene.cache-version-poll-ms:1000}",
      initialDelayString = "${nexiot.scene.cache-version-poll-ms:1000}")
  public void checkRevision() {
    if (cache.estimatedSize() == 0) return;
    try {
      observe(redis.opsForValue().get(REVISION_KEY));
      if (versionReadFailed.getAndSet(false)) log.info("场景缓存版本检查恢复");
    } catch (RuntimeException e) {
      if (versionReadFailed.compareAndSet(false, true)) log.warn("场景缓存版本检查失败，暂由缓存过期刷新兜底", e);
    }
  }

  private void observe(String revision) {
    if (!Objects.equals(observedRevision, revision)) {
      observedRevision = revision;
      invalidateLocal();
    }
  }

  @Override
  public void onMessage(Message message, byte[] pattern) {
    if (TOPIC.equals(new String(message.getChannel(), StandardCharsets.UTF_8))) {
      observe(new String(message.getBody(), StandardCharsets.UTF_8));
    }
  }

  private class InvalidationSynchronization implements TransactionSynchronization {
    @Override
    public void afterCommit() {
      publishInvalidation();
    }
  }

  public record Rule(SceneLinkage scene, List<TriggerBO> triggers) {}

  private record DeviceKey(String productKey, String deviceId) {}

  private record Scope(long generation, String tenantId, String creatorId) {}

  private record Index(Map<DeviceKey, List<Rule>> devices, int weight) {}
}
