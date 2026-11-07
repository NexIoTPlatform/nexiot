package cn.universal.admin.video.alarm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.video.service.VideoPlatformProductService;
import cn.universal.dm.cache.IoTCacheInvalidationEvent;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.dm.video.VideoSdkEventCatalog;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

class SdkVideoAlarmContextCacheTest {
  private final VideoPlatformDeviceMapper platformMapper = mock(VideoPlatformDeviceMapper.class);
  private final IoTDeviceMapper devices = mock(IoTDeviceMapper.class);
  private final VideoPlatformProductService products = mock(VideoPlatformProductService.class);
  private final VideoSdkEventCatalog catalog = mock(VideoSdkEventCatalog.class);
  private final IoTCacheInvalidationPublisher publisher = mock(IoTCacheInvalidationPublisher.class);
  private final SdkVideoAlarmContextCache cache =
      new SdkVideoAlarmContextCache(
          platformMapper, devices, products, catalog, publisher);

  @Test
  void platformChangesCoalescePerTransactionAndSurviveSuspension() {
    org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
    org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
    try {
      cache.publishPlatformChange("one");
      cache.publishPlatformChange("one");
      cache.publishPlatformChange("two");
      var outer = org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations();
      outer.forEach(org.springframework.transaction.support.TransactionSynchronization::suspend);
      org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
      org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
      cache.publishPlatformChange("one");
      var inner = org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations();
      inner.forEach(s -> s.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));
      org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
      org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
      outer.forEach(org.springframework.transaction.support.TransactionSynchronization::resume);
      cache.publishPlatformChange("one");
      outer.forEach(s -> s.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED));
      verify(publisher, times(2)).publishCustomCacheChange("video-sdk-device-change", Set.of(),
          Map.of(SdkVideoAlarmContextCache.CACHE_NAME, Set.of("one")));
      verify(publisher).publishCustomCacheChange("video-sdk-device-change", Set.of(),
          Map.of(SdkVideoAlarmContextCache.CACHE_NAME, Set.of("two")));
    } finally {
      org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
      org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(false);
    }
    cache.publishPlatformChange("one");
    cache.publishPlatformChange("one");
    verify(publisher, times(4)).publishCustomCacheChange("video-sdk-device-change", Set.of(),
        Map.of(SdkVideoAlarmContextCache.CACHE_NAME, Set.of("one")));
  }

  @BeforeAll
  static void initMapper() {
    EntityHelper.initEntityNameMap(VideoPlatformDevice.class, new Config());
  }

  @Test
  void concurrentCallbacksOnlyPrepareProductOnceAndDoNotShareMutableProduct() throws Exception {
    var instance = new VideoPlatformInstance();
    instance.setInstanceKey("camera-product");
    var product = new IoTProduct();
    product.setProductKey("camera-product");
    product.setMetadata("{}");
    when(products.prepareAlarmProduct(instance)).thenReturn(product);
    try (var executor = Executors.newFixedThreadPool(8)) {
      List<Future<IoTProduct>> pending = new ArrayList<>();
      for (int i = 0; i < 32; i++) pending.add(executor.submit(() -> cache.product(instance)));
      for (var future : pending) assertEquals("camera-product", future.get().getProductKey());
    }
    cache.product(instance).setMetadata("modified");
    assertEquals("{}", cache.product(instance).getMetadata());
    verify(products, times(1)).prepareAlarmProduct(instance);
    when(catalog.modelEvents(null))
        .thenReturn(List.of(new VideoSdkEventCatalog.Event("new", "newEvent", "新增", "both", "")));
    cache.product(instance);
    verify(products, times(2)).prepareAlarmProduct(instance);
  }

  @Test
  void deviceCacheIsInstanceScopedAndRefreshesOnChanges() {
    var first = new IoTDevice();
    first.setDeviceId("ip");
    first.setIotId("iot-1");
    var second = new IoTDevice();
    second.setDeviceId("ip");
    second.setIotId("iot-2");
    when(devices.selectIoTDevice("one", "ip")).thenReturn(first);
    when(devices.selectIoTDevice("two", "ip")).thenReturn(second);
    assertSame(first, cache.device("one", "id", "ip"));
    assertSame(first, cache.device("one", "id", "ip"));
    assertSame(second, cache.device("two", "id", "ip"));
    verify(devices, times(1)).selectIoTDevice("one", "id");
    cache.onCacheInvalidation(
        new IoTCacheInvalidationEvent(
            "device-update", "one", "ip", "iot-1", Set.of(), Map.of(), Set.of()));
    assertSame(first, cache.device("one", "id", "ip"));
    verify(devices, times(2)).selectIoTDevice("one", "id");
    verify(devices, times(1)).selectIoTDevice("two", "id");
  }

  @Test
  void missingDevicesAreNotCachedAndAmbiguousIpIsRejected() {
    var device = new IoTDevice();
    when(devices.selectIoTDevice("one", "id")).thenReturn(null, device);
    assertNull(cache.device("one", "id", null));
    assertSame(device, cache.device("one", "id", null));
    when(platformMapper.selectByExample(any()))
        .thenReturn(List.of(new VideoPlatformDevice(), new VideoPlatformDevice()));
    assertThrows(RuntimeException.class, () -> cache.platformDevice("one", null, "ip"));
  }

  @Test
  void platformMutationEvictsMappingAndProduct() {
    when(platformMapper.selectByExample(any())).thenReturn(List.of(new VideoPlatformDevice()));
    cache.platformDevice("one", "id", "ip");
    cache.platformDevice("one", "id", "ip");
    cache.onCacheInvalidation(
        new IoTCacheInvalidationEvent(
            "video-update",
            null,
            null,
            Set.of(),
            Map.of(SdkVideoAlarmContextCache.CACHE_NAME, Set.of("one")),
            Set.of()));
    cache.platformDevice("one", "id", "ip");
    verify(platformMapper, times(2)).selectByExample(any());
  }
}
