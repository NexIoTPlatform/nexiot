package cn.universal.admin.video.service;

import cn.universal.dm.video.VideoPlatformAdapter;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.dm.video.VideoSdkEventCatalog;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

/** 根据数据库中的订阅配置恢复网关监听，不依赖浏览器或后端重启。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoSdkSubscriptionRecovery {
  private final VideoPlatformInstanceMapper instanceMapper;
  private final VideoPlatformAdapterRegistry adapterRegistry;
  private final ObjectMapper objectMapper;
  private final VideoSdkEventCatalog eventCatalog;
  private final Map<String, Object> locks = new ConcurrentHashMap<>();
  private final Map<String, RetryState> retries = new ConcurrentHashMap<>();
  private final AtomicBoolean running = new AtomicBoolean();
  private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
    Thread thread = new Thread(task, "video-sdk-subscription-recovery");
    thread.setDaemon(true);
    return thread;
  });

  @Value("${video.sdk.subscription.recovery-enabled:true}")
  private boolean enabled = true;
  @Value("${video.sdk.subscription.recovery-interval-ms:30000}")
  private long intervalMs = 30000;
  @Value("${video.sdk.subscription.recovery-max-delay-ms:300000}")
  private long maxDelayMs = 300000;

  private record RetryState(int failures, long nextAttemptAt, String message) {}

  public <T> T withLock(String instanceKey, Supplier<T> operation) {
    synchronized (locks.computeIfAbsent(instanceKey, key -> new Object())) {
      return operation.get();
    }
  }

  public void resetRetry(String instanceKey) { retries.remove(instanceKey); }

  public Map<String, Object> metadata(String instanceKey) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("autoRecoveryEnabled", enabled);
    result.put("recoveryIntervalMs", intervalMs);
    RetryState state = retries.get(instanceKey);
    if (state != null) {
      result.put("nextRecoveryAt", state.nextAttemptAt());
      result.put("recoveryError", state.message());
    }
    return result;
  }

  @Scheduled(initialDelayString = "${video.sdk.subscription.recovery-interval-ms:30000}",
      fixedDelayString = "${video.sdk.subscription.recovery-interval-ms:30000}")
  public void scheduleRecovery() {
    // 原子门闩限制最多一个巡检任务；网关变慢时不在执行器中持续堆积任务。
    if (!enabled || !running.compareAndSet(false, true)) return;
    try {
      worker.execute(() -> {
        try { recoverNow(); }
        catch (Exception e) { log.warn("[视频SDK告警] 订阅巡检失败: {}", e.getMessage()); }
        finally { running.set(false); }
      });
    } catch (RuntimeException e) {
      running.set(false);
      throw e;
    }
  }

  public Map<String, Object> recoverNow() {
    if (!enabled) return Map.of("total", 0, "success", 0, "failed", 0);
    Example query = new Example(VideoPlatformInstance.class);
    query.createCriteria().andIn("platformType", List.of("hik_sdk", "dahua_sdk", "tiandy_sdk", "uniview_sdk", "hik_isup"));
    List<VideoPlatformInstance> instances = instanceMapper.selectByExample(query);
    Set<String> keys = new java.util.HashSet<>();
    int success = 0;
    int failed = 0;
    for (VideoPlatformInstance instance : instances) {
      String key = instance.getInstanceKey();
      keys.add(key);
      RetryState retry = retries.get(key);
      if (retry != null && retry.nextAttemptAt() > System.currentTimeMillis()) continue;
      try {
        boolean restored = withLock(key, () -> recoverOne(instance.getId()));
        retries.remove(key);
        if (restored) success++;
      } catch (Exception e) {
        int failures = retry == null ? 1 : retry.failures() + 1;
        // 指数退避有上限，故障设备不会每轮反复登录 SDK；成功后清除失败次数。
        long delay = Math.min(Math.max(intervalMs, maxDelayMs),
            Math.max(1000, intervalMs) * (1L << Math.min(failures - 1, 10)));
        retries.put(key, new RetryState(failures, System.currentTimeMillis() + delay, e.getMessage()));
        failed++;
        log.warn("[视频SDK告警] 自动恢复待重试 instanceKey={}, delayMs={}, reason={}", key, delay, e.getMessage());
      }
    }
    retries.keySet().retainAll(keys);
    return Map.of("total", instances.size(), "success", success, "failed", failed);
  }

  private boolean recoverOne(Long id) {
    // 加锁后重新读取，避免用户刚停用订阅又被旧的巡检快照重新启用。
    VideoPlatformInstance instance = instanceMapper.selectByPrimaryKey(id);
    if (instance == null) return false;
    Map<String, Object> options = readOptions(instance.getOptions());
    Map<String, Object> config = objectMap(options.get("subscription"));
    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    List<String> topics = config.get("topics") instanceof List<?> values
        ? values.stream().map(String::valueOf).toList() : List.of();
    boolean subscribed = Integer.valueOf(1).equals(instance.getEnabled())
        && Boolean.parseBoolean(String.valueOf(config.get("enabled"))) && !topics.isEmpty();
    List<Map<String, Object>> desired = subscribed ? adapter.resolveSubscriptionDevices(instance) : List.of();
    // 查询整个实例，才能发现停用、删除或移出订阅范围后遗留的监听。
    Map<String, Object> gateway = adapter.executeOperation(instance, "subscriptionStatus", Map.of());
    List<Map<String, Object>> active = runtimeSubscriptions(gateway);
    String callback = String.valueOf(config.getOrDefault("callback", ""));
    List<Map<String, Object>> catalog = subscribed ? eventCatalog.subscriptionCatalog(instance.getPlatformType()) : List.of();
    if (matchesConfiguration(desired, active, topics, callback, catalog)) return false;
    if (desired.isEmpty()) {
      adapter.unsubscribe(instance, List.of());
      if (!runtimeSubscriptions(adapter.executeOperation(instance, "subscriptionStatus", Map.of())).isEmpty()) {
        throw new IllegalStateException("已停用设备的监听尚未清理，请稍后重试");
      }
      return true;
    }
    if (callback.isBlank()) throw new IllegalStateException("请配置 NexIoT 告警回调地址");
    adapter.subscribe(instance, topics, callback);
    // 原生 SDK 可部分失败；只有再次查询确认后，才认为已恢复。
    List<Map<String, Object>> restored = runtimeSubscriptions(
        adapter.executeOperation(instance, "subscriptionStatus", Map.of()));
    if (!matchesConfiguration(desired, restored, topics, callback, catalog)) {
      throw new IllegalStateException("部分设备尚未建立监听，请检查设备在线状态和凭据");
    }
    log.info("[视频SDK告警] 自动恢复成功 instanceKey={}, devices={}", instance.getInstanceKey(), desired.size());
    return true;
  }

  private static boolean matchesScope(List<Map<String, Object>> desired, List<Map<String, Object>> active) {
    return desired.size() == active.size()
        && desired.stream().allMatch(device -> active.stream().anyMatch(row -> matchesDevice(device, row)));
  }

  private static boolean matchesConfiguration(List<Map<String, Object>> desired, List<Map<String, Object>> active,
      List<String> topics, String callback, List<Map<String, Object>> catalog) {
    if (!matchesScope(desired, active)) return false;
    for (Map<String, Object> row : active) {
      // 老网关缺少这些字段时兼容既有设备健康检查；新网关按完整配置修复漂移。
      if (row.containsKey("topics") && !businessTopics(topics).equals(businessTopics(row.get("topics")))) return false;
      if (row.containsKey("callback") && !callback.equals(String.valueOf(row.get("callback")))) return false;
      if (Boolean.FALSE.equals(row.get("eventCatalogReady"))) return false;
      if ((row.containsKey("forwardingActive") || row.containsKey("eventCatalogReady")) && !row.containsKey("eventCatalog")) return false;
      if (row.containsKey("eventCatalog") && !catalog.equals(row.get("eventCatalog"))) return false;
    }
    return true;
  }

  private static Set<String> businessTopics(Object value) {
    if (!(value instanceof List<?> values)) return Set.of();
    Set<String> result = new java.util.HashSet<>();
    values.stream().map(String::valueOf).filter(topic -> !topic.startsWith("__sdk_")).forEach(result::add);
    return result;
  }

  public static boolean matchesDevice(Map<String, Object> device, Map<String, Object> runtime) {
    if (Boolean.FALSE.equals(runtime.get("success")) || Boolean.FALSE.equals(runtime.get("forwardingActive"))) return false;
    List<String> fields = "isup".equals(device.get("accessMode")) ? List.of("deviceId") : List.of("deviceId", "host", "port");
    for (String field : fields) {
      if (!String.valueOf(device.get(field)).equals(String.valueOf(runtime.get(field)))) return false;
    }
    return true;
  }

  public static List<Map<String, Object>> runtimeSubscriptions(Map<String, Object> gateway) {
    if (gateway == null || Boolean.FALSE.equals(gateway.get("accepted"))) {
      throw new IllegalStateException("网关订阅状态查询失败");
    }
    Map<String, Object> attributes = objectMap(gateway.get("attributes"));
    if (!(attributes.get("subscriptions") instanceof List<?> rows)) {
      throw new IllegalStateException("网关未返回有效的订阅状态");
    }
    List<Map<String, Object>> result = new ArrayList<>();
    for (Object row : rows) result.add(objectMap(row));
    return result;
  }

  private Map<String, Object> readOptions(String json) {
    if (json == null || json.isBlank()) return Map.of();
    try { return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {}); }
    catch (Exception e) { throw new IllegalStateException("订阅配置格式无效", e); }
  }

  private static Map<String, Object> objectMap(Object value) {
    Map<String, Object> result = new LinkedHashMap<>();
    if (value instanceof Map<?, ?> map) map.forEach((key, item) -> result.put(String.valueOf(key), item));
    return result;
  }

  @PreDestroy public void close() { worker.shutdownNow(); }
}
