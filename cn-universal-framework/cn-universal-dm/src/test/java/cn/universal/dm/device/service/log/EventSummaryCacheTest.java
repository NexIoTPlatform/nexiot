package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.query.PageBean;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class EventSummaryCacheTest {
  private final DefaultIoTDeviceLogService service = new DefaultIoTDeviceLogService();
  private final IIoTDeviceLogService storage = mock(IIoTDeviceLogService.class);
  private final IoTProduct product =
      IoTProduct.builder().productKey("product").tenantId("tenant-a").storePolicy("mysql").build();

  private void configure() throws Exception {
    IoTProductDeviceService products = mock(IoTProductDeviceService.class);
    when(products.getProduct("product")).thenReturn(product);
    when(storage.getPolicy()).thenReturn("mysql");
    when(storage.queryEventTotal("product", "device")).thenReturn(summary());
    set("iotProductDeviceService", products);
    set("policies", Map.of("mysql", storage));
    set("historyQueryCoordinator", new HistoryQueryCoordinator(4, 2, 5, 128, 128000, 8));
  }

  @Test
  void cachesSuccessfulResultsWithoutSharingMutableEventCounts() throws Exception {
    configure();
    var first = service.queryEventTotal("product", "device");
    first.getList().getFirst().setQty("broken");
    var second = service.queryEventTotal("product", "device");
    assertEquals("99+", second.getList().getFirst().getQty());
    verify(storage).queryEventTotal("product", "device");
  }

  @Test
  void expiresAfterThirtySecondsAndSeparatesTenantsAndDevices() throws Exception {
    configure();
    AtomicLong clock = new AtomicLong();
    set(
        "eventTotalCache",
        Caffeine.newBuilder()
            .maximumSize(1024)
            .expireAfterWrite(Duration.ofSeconds(30))
            .ticker(clock::get)
            .build());
    service.queryEventTotal("product", "device");
    clock.set(TimeUnit.SECONDS.toNanos(29));
    service.queryEventTotal("product", "device");
    verify(storage).queryEventTotal("product", "device");
    clock.set(TimeUnit.SECONDS.toNanos(30));
    service.queryEventTotal("product", "device");
    verify(storage, times(2)).queryEventTotal("product", "device");
    product.setTenantId("tenant-b");
    service.queryEventTotal("product", "device");
    verify(storage, times(3)).queryEventTotal("product", "device");
    when(storage.queryEventTotal("product", "other-device")).thenReturn(summary());
    service.queryEventTotal("product", "other-device");
    verify(storage).queryEventTotal("product", "other-device");
  }

  @Test
  void queryFailuresAreNotCachedAsZero() throws Exception {
    configure();
    when(storage.queryEventTotal("product", "device"))
        .thenThrow(new IllegalStateException("database timeout"))
        .thenReturn(summary());
    assertThrows(IllegalStateException.class, () -> service.queryEventTotal("product", "device"));
    assertEquals("99+", service.queryEventTotal("product", "device").getList().getFirst().getQty());
    verify(storage, times(2)).queryEventTotal("product", "device");
  }

  @Test
  void simultaneousRequestsShareOneDatabaseLoad() throws Exception {
    configure();
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    when(storage.queryEventTotal("product", "device"))
        .thenAnswer(
            call -> {
              started.countDown();
              if (!release.await(5, TimeUnit.SECONDS))
                throw new IllegalStateException("test timeout");
              return summary();
            });
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var first = executor.submit(() -> service.queryEventTotal("product", "device"));
      org.junit.jupiter.api.Assertions.assertTrue(started.await(5, TimeUnit.SECONDS));
      var second = executor.submit(() -> service.queryEventTotal("product", "device"));
      release.countDown();
      assertEquals("99+", first.get(5, TimeUnit.SECONDS).getList().getFirst().getQty());
      assertEquals("99+", second.get(5, TimeUnit.SECONDS).getList().getFirst().getQty());
      verify(storage).queryEventTotal("product", "device");
    } finally {
      release.countDown();
    }
  }

  private PageBean<IoTDeviceEvents> summary() {
    return new PageBean<>(
        List.of(
            IoTDeviceEvents.builder().id("alarm").qty("99+").time("2026-10-16T12:00:00").build()),
        1L,
        100,
        1);
  }

  private void set(String name, Object value) throws Exception {
    Field field = DefaultIoTDeviceLogService.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(service, value);
  }
}
