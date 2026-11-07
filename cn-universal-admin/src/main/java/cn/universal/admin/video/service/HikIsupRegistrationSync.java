package cn.universal.admin.video.service;

import cn.universal.admin.video.sync.HikIsupDeviceSyncHandler;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 已认证的注册目录自动落到实例租户，不依赖登录态或人工勾选。 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HikIsupRegistrationSync {
  private final VideoPlatformInstanceMapper instances;
  private final VideoPlatformAdapterRegistry adapters;
  private final HikIsupDeviceSyncHandler handler;
  private final AtomicBoolean running = new AtomicBoolean();
  private final Set<String> stopped = ConcurrentHashMap.newKeySet();
  private final Map<String, Map<String, Object>> state = new ConcurrentHashMap<>();
  private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
    Thread thread = new Thread(task, "hik-isup-registration-sync");
    thread.setDaemon(true);
    return thread;
  });

  @Scheduled(initialDelayString = "${video.isup.sync-initial-delay-ms:2000}",
      fixedDelayString = "${video.isup.sync-interval-ms:5000}")
  public void requestSync() {
    if (!running.compareAndSet(false, true)) return;
    try {
      worker.execute(() -> {
        try { reconcileNow(); }
        catch (Exception e) { log.warn("[ISUP注册同步] 巡检失败: {}", e.getMessage()); }
        finally { running.set(false); }
      });
    } catch (RuntimeException e) { running.set(false); throw e; }
  }

  public synchronized void reconcileNow() {
    List<VideoPlatformInstance> saved = instances.select(VideoPlatformInstance.builder().platformType("hik_isup").build());
    Set<String> current = new java.util.HashSet<>();
    for (VideoPlatformInstance snapshot : saved) {
      String key = snapshot.getInstanceKey();
      current.add(key);
      try {
        // 排队期间实例可能已停用，启动前重新读取可信配置。
        VideoPlatformInstance instance = instances.selectByPrimaryKey(snapshot.getId());
        if (instance == null) continue;
        var adapter = adapters.getAdapter("hik_isup");
        if (!Integer.valueOf(1).equals(instance.getEnabled())) {
          if (!stopped.contains(key)) {
            adapter.executeOperation(instance, "stopServices", Map.of());
            stopped.add(key);
          }
          state.put(key, Map.of("applicationStatus", "stopped", "savedRevision", HikIsupPlatformConfiguration.savedRevision(instance),
              "lastSyncAttemptAt", System.currentTimeMillis()));
          continue;
        }
        stopped.remove(key);
        if (instance.getTenantId() == null || instance.getTenantId().isBlank()
            || instance.getCreatorId() == null || instance.getCreatorId().isBlank())
          throw new IllegalStateException("ISUP 实例缺少可信租户或创建者");
        adapter.executeOperation(instance, "configure", Map.of());
        long appliedRevision = HikIsupPlatformConfiguration.savedRevision(instance);
        state.put(key, Map.of("applicationStatus", "applied", "appliedRevision", appliedRevision,
            "savedRevision", appliedRevision, "appliedAt", System.currentTimeMillis()));
        List<Map<String, Object>> devices = adapter.listDevices(instance, Map.of("pageSize", 10000));
        int synced = 0;
        int failed = 0;
        for (Map<String, Object> device : devices) {
          try { handler.syncRegisteredDevice(instance, device); synced++; }
          catch (Exception e) {
            failed++;
            log.warn("[ISUP注册同步] 设备落库失败 instanceKey={}, deviceId={}, reason={}", key, device.get("deviceId"), e.getMessage());
          }
        }
        Map<String, Object> applied = new java.util.LinkedHashMap<>(state.get(key));
        applied.put("lastSyncAt", System.currentTimeMillis()); applied.put("syncedDevices", synced); applied.put("failedDevices", failed);
        state.put(key, Map.copyOf(applied));
      } catch (Exception e) {
        // 目录读取失败不能解释为设备下线，也不能清空通道和本地配置。
        Map<String, Object> error = new java.util.LinkedHashMap<>(state.getOrDefault(key, Map.of()));
        error.put("lastSyncAttemptAt", System.currentTimeMillis());
        error.put("applicationStatus", "error");
        error.put("savedRevision", HikIsupPlatformConfiguration.savedRevision(snapshot));
        error.put("syncError", "注册同步失败，请检查 SDK 网关及平台配置");
        state.put(key, Map.copyOf(error));
        log.warn("[ISUP注册同步] 实例待重试 instanceKey={}, reason={}", key, e.getMessage());
      }
    }
    stopped.retainAll(current);
    state.keySet().retainAll(current);
  }

  public Map<String, Object> metadata(String instanceKey) { return state.getOrDefault(instanceKey, Map.of()); }

  public Map<String, Object> applicationState(VideoPlatformInstance instance) {
    Map<String, Object> result = new java.util.LinkedHashMap<>(metadata(instance.getInstanceKey()));
    long saved = HikIsupPlatformConfiguration.savedRevision(instance);
    result.put("savedRevision", saved);
    if (result.get("appliedRevision") == null || !java.util.Objects.equals(((Number) result.get("appliedRevision")).longValue(), saved))
      result.put("applicationStatus", "error".equals(result.get("applicationStatus")) ? "error" : "saved");
    if (!Integer.valueOf(1).equals(instance.getEnabled())) result.put("applicationStatus", "stopped");
    return result;
  }

  @PreDestroy public void shutdown() { worker.shutdownNow(); }
}
