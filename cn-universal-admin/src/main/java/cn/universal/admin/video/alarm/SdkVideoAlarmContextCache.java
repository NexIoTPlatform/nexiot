package cn.universal.admin.video.alarm;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.admin.video.service.VideoPlatformProductService;
import cn.universal.common.exception.BaseException;
import cn.universal.dm.cache.IoTCacheInvalidationEvent;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.dm.video.VideoSdkEventCatalog;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tk.mybatis.mapper.entity.Example;

/** 告警高频路径仅缓存路由资料；鉴权 token 和订阅开关仍逐次读取实例。 */
@Component
@RequiredArgsConstructor
public class SdkVideoAlarmContextCache {
  public static final String CACHE_NAME = "video_sdk_alarm_context";

  private record DeviceKey(String instanceKey, String deviceId, String ip) {}

  private record ProductKey(
      String instanceKey, String tenantId, List<VideoSdkEventCatalog.Event> events) {}

  private final VideoPlatformDeviceMapper platformDeviceMapper;
  private final IoTDeviceMapper deviceMapper;
  private final VideoPlatformProductService productService;
  private final VideoSdkEventCatalog eventCatalog;
  private final IoTCacheInvalidationPublisher invalidationPublisher;
  private final Object pendingChangesKey = new Object();

  // record 组合键避免字符串拼接碰撞；实例维度隔离同 IP/设备号，不跨平台命中。
  private final Cache<DeviceKey, VideoPlatformDevice> platformDevices =
      Caffeine.newBuilder().maximumSize(10000).expireAfterWrite(Duration.ofSeconds(30)).build();
  private final Cache<DeviceKey, IoTDevice> devices =
      Caffeine.newBuilder().maximumSize(10000).expireAfterWrite(Duration.ofSeconds(30)).build();
  private final Cache<ProductKey, IoTProduct> products =
      Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(Duration.ofMinutes(5)).build();

  public VideoPlatformDevice platformDevice(String instanceKey, String deviceId, String ip) {
    return platformDevices.get(
        new DeviceKey(instanceKey, deviceId, ip),
        key -> {
          VideoPlatformDevice found = findPlatformDevice(instanceKey, "deviceId", deviceId);
          // 部分 SDK 只回 IP；仅在 ID 未命中时兼容回退，IP 不唯一时拒绝猜测归属。
          return found != null ? found : findPlatformDevice(instanceKey, "deviceIp", ip);
        });
  }

  private VideoPlatformDevice findPlatformDevice(String instanceKey, String field, String value) {
    if (StrUtil.isBlank(value)) return null;
    Example example = new Example(VideoPlatformDevice.class);
    example.createCriteria().andEqualTo("instanceKey", instanceKey).andEqualTo(field, value);
    List<VideoPlatformDevice> found = platformDeviceMapper.selectByExample(example);
    if (found.size() > 1) throw new BaseException("摄像头标识不唯一，请使用明确的 deviceId");
    return found.isEmpty() ? null : found.get(0);
  }

  public IoTDevice device(String instanceKey, String deviceId, String ip) {
    // Caffeine 不缓存 null/异常，刚同步或刚导入的设备可以立即重试。
    return devices.get(
        new DeviceKey(instanceKey, deviceId, ip),
        key -> {
          IoTDevice found = deviceMapper.selectIoTDevice(instanceKey, deviceId);
          if (found == null && StrUtil.isNotBlank(ip) && !ip.equals(deviceId)) {
            found = deviceMapper.selectIoTDevice(instanceKey, ip);
          }
          return found;
        });
  }

  public IoTProduct product(VideoPlatformInstance instance) {
    // 字典内容参与 key：新事件下一次上报即可补齐物模型，不必等待产品缓存过期。
    ProductKey key =
        new ProductKey(
            instance.getInstanceKey(),
            instance.getTenantId(),
            eventCatalog.modelEvents(instance.getPlatformType()));
    IoTProduct product = products.get(key, ignored -> productService.prepareAlarmProduct(instance));
    // 上行管道可以修改实体；返回副本，避免污染随后告警共享的缓存资料。
    return product == null ? null : BeanUtil.copyProperties(product, IoTProduct.class);
  }

  public void publishPlatformChange(String instanceKey) {
    if (TransactionSynchronizationManager.isActualTransactionActive()
        && TransactionSynchronizationManager.isSynchronizationActive()) {
      @SuppressWarnings("unchecked")
      Set<String> pending = (Set<String>) TransactionSynchronizationManager.getResource(pendingChangesKey);
      if (pending == null) {
        pending = new HashSet<>();
        Set<String> transactionChanges = pending;
        TransactionSynchronizationManager.bindResource(pendingChangesKey, pending);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
          @Override public void suspend() {
            TransactionSynchronizationManager.unbindResource(pendingChangesKey);
          }
          @Override public void resume() {
            TransactionSynchronizationManager.bindResource(pendingChangesKey, transactionChanges);
          }
          @Override public void afterCompletion(int status) {
            TransactionSynchronizationManager.unbindResourceIfPossible(pendingChangesKey);
          }
        });
      }
      // 视频目录、IoT 资料在同一设备事务内更新，提交后只需刷新一次实例。
      if (!pending.add(instanceKey)) return;
    }
    invalidationPublisher.publishCustomCacheChange(
        "video-sdk-device-change", Set.of(), Map.of(CACHE_NAME, Set.of(instanceKey)));
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onCacheInvalidation(IoTCacheInvalidationEvent event) {
    if (VideoSdkEventCatalog.affectsCatalog(event)) products.invalidateAll();
    if (event.getCacheNames().contains(CACHE_NAME)
        || event.getCacheNames().contains("selectDevInstanceBOIoTId")) {
      platformDevices.invalidateAll();
      devices.invalidateAll();
      products.invalidateAll();
      return;
    }
    for (Object instanceKey : event.getCacheKeys().getOrDefault(CACHE_NAME, Set.of())) {
      evictInstance(String.valueOf(instanceKey));
    }
    String productKey = event.getProductKey();
    if (StrUtil.isBlank(productKey)) return;
    if (StrUtil.isBlank(event.getDeviceId())) {
      evictInstance(productKey);
    } else {
      // IP 回退可能使缓存 key 与 IoT deviceId 不同，按缓存实体的 iotId/设备号一并驱逐。
      devices
          .asMap()
          .entrySet()
          .removeIf(
              entry ->
                  entry.getKey().instanceKey().equals(productKey)
                      && (StrUtil.equals(entry.getValue().getDeviceId(), event.getDeviceId())
                          || StrUtil.equals(entry.getValue().getIotId(), event.getIotId())));
    }
  }

  private void evictInstance(String instanceKey) {
    platformDevices.asMap().keySet().removeIf(key -> key.instanceKey().equals(instanceKey));
    devices.asMap().keySet().removeIf(key -> key.instanceKey().equals(instanceKey));
    products.asMap().keySet().removeIf(key -> key.instanceKey().equals(instanceKey));
  }
}
