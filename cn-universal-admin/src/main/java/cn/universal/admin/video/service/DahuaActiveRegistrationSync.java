package cn.universal.admin.video.service;

import cn.universal.admin.video.sync.DahuaSdkDeviceSyncHandler;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformDeviceExt;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformDeviceExtMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

/** 仅认证成功的注册目录进入实例租户，目录读取失败不会改为离线。 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DahuaActiveRegistrationSync {
  private final VideoPlatformInstanceMapper instances;
  private final VideoPlatformAdapterRegistry adapters;
  private final DahuaSdkDeviceSyncHandler handler;
  @Resource private VideoPlatformDeviceMapper devices;
  @Resource private VideoPlatformDeviceExtMapper extensions;
  private final ObjectMapper json = new ObjectMapper();
  private final AtomicBoolean running = new AtomicBoolean();
  private final Map<String, VideoPlatformInstance> applied = new ConcurrentHashMap<>();
  private final Set<String> stopped = ConcurrentHashMap.newKeySet();
  private final Map<String, Map<String, Object>> state = new ConcurrentHashMap<>();
  private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
    Thread thread = new Thread(task, "dahua-registration-sync"); thread.setDaemon(true); return thread;
  });

  @Scheduled(initialDelayString = "${video.dahua.registration-sync-initial-delay-ms:2000}",
      fixedDelayString = "${video.dahua.registration-sync-interval-ms:5000}")
  public void requestSync() {
    if (!running.compareAndSet(false, true)) return;
    try { worker.execute(() -> {
      try { reconcileNow(); }
      catch (Exception e) { log.warn("[大华注册同步] 巡检失败: {}", e.getMessage()); }
      finally { running.set(false); }
    }); } catch (RuntimeException e) { running.set(false); throw e; }
  }

  public synchronized void reconcileNow() {
    List<VideoPlatformInstance> saved = instances.select(VideoPlatformInstance.builder().platformType("dahua_sdk").build());
    Set<String> current = new java.util.HashSet<>();
    for (VideoPlatformInstance snapshot : saved) {
      String key = snapshot.getInstanceKey(); current.add(key);
      try {
        VideoPlatformInstance instance = instances.selectByPrimaryKey(snapshot.getId());
        if (instance == null) continue;
        if (!DahuaActiveRegistrationConfiguration.enabled(instance) || !Integer.valueOf(1).equals(instance.getEnabled())) {
          VideoPlatformInstance previous = applied.get(key);
          Exception offlineError = null;
          if (previous != null || DahuaActiveRegistrationConfiguration.configured(instance)
              || Integer.valueOf(0).equals(instance.getEnabled())) {
            try { markRegisteredDevicesOffline(instance); }
            catch (Exception e) { offlineError = e; }
          }
          if ((previous != null || DahuaActiveRegistrationConfiguration.configured(instance)) && !stopped.contains(key)) {
            Map<String, Object> stopState = adapters.getAdapter("dahua_sdk")
                .executeOperation(previous == null ? instance : previous, "stopActiveRegistration", Map.of());
            if (!"stopped".equals(stopState.get("status")) || Boolean.TRUE.equals(stopState.get("enabled"))) {
              if (offlineError != null) throw offlineError;
              state.put(key, Map.of("applicationStatus", "stopping", "lastSyncAttemptAt", System.currentTimeMillis()));
              continue;
            }
            applied.remove(key);
            stopped.add(key);
          }
          if (offlineError != null) throw offlineError;
          if (DahuaActiveRegistrationConfiguration.configured(instance) || previous != null)
            state.put(key, Map.of("applicationStatus", "stopped", "lastSyncAttemptAt", System.currentTimeMillis()));
          continue;
        }
        if (instance.getTenantId() == null || instance.getTenantId().isBlank()
            || instance.getCreatorId() == null || instance.getCreatorId().isBlank())
          throw new IllegalStateException("大华实例缺少可信租户或创建者");
        var adapter = adapters.getAdapter("dahua_sdk");
        stopped.remove(key);
        VideoPlatformInstance previous = applied.get(key);
        boolean gatewayChanged = previous != null
            && !java.util.Objects.equals(gatewayEndpoint(previous), gatewayEndpoint(instance));
        if (previous != null && (gatewayChanged
            || !java.util.Objects.equals(listenerBinding(previous), listenerBinding(instance)))) {
          // 重新监听会清空旧认证目录，先落已有设备离线再迁移监听。
          markRegisteredDevicesOffline(instance);
        }
        if (gatewayChanged) {
          Map<String, Object> previousState = adapter.executeOperation(previous, "stopActiveRegistration", Map.of());
          if (!"stopped".equals(previousState.get("status")) || Boolean.TRUE.equals(previousState.get("enabled"))) {
            state.put(key, Map.of("applicationStatus", "stopping", "lastSyncAttemptAt", System.currentTimeMillis()));
            continue;
          }
        }
        Map<String, Object> configuration = adapter.executeOperation(instance, "configureActiveRegistration", Map.of());
        applied.put(key, instance);
        if (!"listening".equals(configuration.get("status"))) {
          state.put(key, Map.of("applicationStatus", "stopping", "lastSyncAttemptAt", System.currentTimeMillis()));
          continue;
        }
        List<Map<String, Object>> directory = adapter.listDevices(instance, Map.of("accessMode", "active_registration", "pageSize", 10000));
        int synced = 0, failed = 0;
        for (Map<String, Object> device : directory) {
          if (!authenticated(device)) continue;
          try { handler.syncRegisteredDevice(instance, device); synced++; }
          catch (Exception e) { failed++; log.warn("[大华注册同步] 设备落库失败 instanceKey={}, deviceId={}", key, device.get("deviceId")); }
        }
        state.put(key, Map.of("applicationStatus", "applied", "lastSyncAt", System.currentTimeMillis(),
            "syncedDevices", synced, "failedDevices", failed));
      } catch (Exception e) {
        Map<String, Object> error = new LinkedHashMap<>(state.getOrDefault(key, Map.of()));
        error.put("applicationStatus", "error"); error.put("lastSyncAttemptAt", System.currentTimeMillis());
        error.put("syncError", "主动注册同步失败，请检查网关连接和实例配置");
        state.put(key, Map.copyOf(error));
        log.warn("[大华注册同步] 实例待重试 instanceKey={}, reason={}", key, e.getMessage());
      }
    }
    for (String key : Set.copyOf(applied.keySet())) if (!current.contains(key)) {
      try { adapters.getAdapter("dahua_sdk").executeOperation(applied.get(key), "stopActiveRegistration", Map.of()); applied.remove(key); }
      catch (RuntimeException e) { log.warn("[大华注册同步] 已删除实例监听释放待重试 instanceKey={}", key); }
    }
    state.keySet().retainAll(current);
    stopped.retainAll(current);
  }

  private String gatewayEndpoint(VideoPlatformInstance instance) throws Exception {
    return DahuaActiveRegistrationConfiguration.gatewayEndpoint(instance, readMap(instance.getOptions()));
  }

  private List<Object> listenerBinding(VideoPlatformInstance instance) throws Exception {
    Object value = readMap(instance.getOptions()).get("activeRegistration");
    Map<?, ?> registration = value instanceof Map<?, ?> map ? map : Map.of();
    String ip = java.util.Objects.toString(registration.get("listenIp"), "0.0.0.0").trim();
    if (ip.isBlank()) ip = "0.0.0.0";
    int port = Integer.parseInt(java.util.Objects.toString(registration.get("listenPort"), "9500"));
    return List.of(ip, port);
  }

  /** 网关停止后目录会被清空，必须从现存记录落离线；该路径不创建新设备。 */
  private void markRegisteredDevicesOffline(VideoPlatformInstance instance) throws Exception {
    Example query = new Example(VideoPlatformDevice.class);
    query.createCriteria().andEqualTo("instanceKey", instance.getInstanceKey());
    List<VideoPlatformDevice> savedDevices = devices.selectByExample(query);
    if (savedDevices.isEmpty()) return;
    Map<Long, VideoPlatformDevice> byId = new LinkedHashMap<>();
    for (VideoPlatformDevice device : savedDevices) byId.put(device.getId(), device);
    Example extensionQuery = new Example(VideoPlatformDeviceExt.class);
    extensionQuery.createCriteria().andEqualTo("instanceKey", instance.getInstanceKey())
        .andIn("deviceId", byId.keySet());
    int failures = 0;
    for (VideoPlatformDeviceExt extension : extensions.selectByExample(extensionQuery)) {
      try {
        VideoPlatformDevice device = byId.get(extension.getDeviceId());
        if (device == null) continue;
        Map<String, Object> data = readMap(extension.getExtField3());
        if (!"active_registration".equals(data.get("loginMode"))
            && !"active_registration".equals(data.get("accessMode"))) continue;
        handler.syncRegisteredDevice(instance, offlineSnapshot(device, extension, data));
      } catch (Exception e) {
        failures++;
        log.warn("[大华注册同步] 停用设备落离线待重试 instanceKey={}, deviceId={}",
            instance.getInstanceKey(), extension.getPlatformDeviceId());
      }
    }
    if (failures > 0) throw new IllegalStateException("主动注册设备离线状态同步失败: " + failures);
  }

  private Map<String, Object> offlineSnapshot(VideoPlatformDevice device,
      VideoPlatformDeviceExt extension, Map<String, Object> data) throws Exception {
    data.put("deviceId", device.getDeviceId());
    data.put("deviceName", device.getDeviceName());
    data.put("deviceIp", device.getDeviceIp());
    data.put("devicePort", device.getDevicePort());
    data.put("deviceModel", device.getDeviceModel());
    data.put("model", device.getDeviceModel());
    data.put("manufacturer", device.getManufacturer());
    data.put("orgId", device.getOrgId());
    data.put("orgName", device.getOrgName());
    data.put("gpsX", device.getGpsX());
    data.put("gpsY", device.getGpsY());
    data.put("gpsZ", device.getGpsZ());
    if (extension.getDeviceSn() != null) data.put("deviceSn", extension.getDeviceSn());
    if (extension.getDeviceCapabilitySet() != null) data.put("deviceCapabilitySet", extension.getDeviceCapabilitySet());
    if (extension.getMediaServerId() != null) data.put("mediaServerId", extension.getMediaServerId());
    if (extension.getOwnerCode() != null) data.put("ownerCode", extension.getOwnerCode());
    if (extension.getSubSystem() != null) data.put("subSystem", extension.getSubSystem());
    if (extension.getUnitsInfo() != null) data.put("unitsInfo", json.readValue(extension.getUnitsInfo(), Object.class));
    data.put("accessMode", "active_registration");
    data.put("loginMode", "active_registration");
    data.put("deviceStatus", "OFF");
    data.put("status", "OFF");
    data.put("online", false);
    data.put("isOnline", "0");
    Map<String, Object> configuration = readMap(device.getConfiguration());
    if (configuration.get("channelList") instanceof List<?> savedChannels) {
      List<Map<String, Object>> offlineChannels = new ArrayList<>();
      for (Object raw : savedChannels) if (raw instanceof Map<?, ?> savedChannel) {
        Map<String, Object> channel = new LinkedHashMap<>();
        savedChannel.forEach((name, value) -> channel.put(String.valueOf(name), value));
        channel.put("channelStatus", "offline");
        channel.put("status", "offline");
        channel.put("online", false);
        channel.put("isOnline", "0");
        offlineChannels.add(channel);
      }
      data.put("channelList", offlineChannels);
    }
    return data;
  }

  private Map<String, Object> readMap(String value) throws Exception {
    if (value == null || value.isBlank()) return new LinkedHashMap<>();
    return json.readValue(value, new TypeReference<LinkedHashMap<String, Object>>() {});
  }

  static boolean authenticated(Map<String, Object> device) {
    if (Boolean.FALSE.equals(device.get("authenticated"))) return false;
    if (Boolean.TRUE.equals(device.get("authenticated"))) return true;
    String status = java.util.Objects.toString(device.getOrDefault("deviceStatus", device.get("status")), "").toLowerCase(java.util.Locale.ROOT);
    return Set.of("on", "online", "off", "offline", "authenticated", "connected").contains(status);
  }
  public Map<String, Object> metadata(String key) { return state.getOrDefault(key, Map.of()); }
  @PreDestroy public void shutdown() { worker.shutdownNow(); }
}
