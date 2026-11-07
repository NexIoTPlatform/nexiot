package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.query.PropertyTrendQuery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;

class HistoryQueryCoordinatorTest {
  private HistoryQueryCoordinator coordinator() {
    return new HistoryQueryCoordinator(4, 2, 5, 128, 128000, 8);
  }

  private PropertyTrendQuery query(String property) {
    PropertyTrendQuery query = new PropertyTrendQuery();
    query.setProductKey("product");
    query.setIotId("iot-device");
    query.setDeviceId("device");
    query.setProperty(property);
    query.setBeginCreateTime(1700000000L);
    query.setEndCreateTime(1700000060L);
    query.setMaxPoints(20);
    return query;
  }

  private PropertyTrendVO result(int size) {
    ArrayList<PropertyTrendVO.Bucket> buckets = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      PropertyTrendVO.Bucket bucket = new PropertyTrendVO.Bucket();
      bucket.setTime(1700000000000L + i * 4000L);
      bucket.setCount(100L);
      bucket.setNumericCount(90L);
      bucket.setAvg(BigDecimal.TEN);
      buckets.add(bucket);
    }
    return PropertyTrendVO.of(query("speed"), buckets);
  }

  @Test
  void concurrentIdenticalRequestsExecuteOnlyOnceAndReturnIndependentResults() throws Exception {
    HistoryQueryCoordinator coordinator = coordinator();
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger calls = new AtomicInteger();
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Future<PropertyTrendVO> first =
          executor.submit(
              () ->
                  coordinator.executeTrend(
                      "tenant",
                      "influxdb",
                      query("speed"),
                      () -> {
                        calls.incrementAndGet();
                        started.countDown();
                        await(release);
                        return result(2);
                      }));
      assertTrue(started.await(2, TimeUnit.SECONDS));
      Future<PropertyTrendVO> second =
          executor.submit(
              () ->
                  coordinator.executeTrend(
                      "tenant",
                      "influxdb",
                      query("speed"),
                      () -> {
                        fail("重复查询应合并");
                        return null;
                      }));
      until(() -> ((Number) coordinator.metrics().get("joinedRequests")).longValue() == 1);
      assertEquals(1, coordinator.metrics().get("active"));
      release.countDown();
      PropertyTrendVO a = first.get(2, TimeUnit.SECONDS);
      PropertyTrendVO b = second.get(2, TimeUnit.SECONDS);
      a.getBuckets().get(0).setCount(0);
      assertEquals(100, b.getBuckets().get(0).getCount());
      PropertyTrendVO cached =
          coordinator.executeTrend(
              "tenant",
              "influxdb",
              query("speed"),
              () -> {
                fail();
                return null;
              });
      assertEquals(100, cached.getBuckets().get(0).getCount());
      assertEquals(1, calls.get());
      assertEquals(0, coordinator.metrics().get("active"));
      assertEquals(0, coordinator.metrics().get("inFlightTrends"));
    } finally {
      release.countDown();
    }
  }

  @Test
  void globalAndPerDatabaseLimitsCoverSlicesAndTrendsAndFailFast() throws Exception {
    HistoryQueryCoordinator coordinator = new HistoryQueryCoordinator(2, 1, 5, 128, 128000, 8);
    CountDownLatch started = new CountDownLatch(2);
    CountDownLatch release = new CountDownLatch(1);
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Future<Integer> a =
          executor.submit(
              () ->
                  coordinator.executeSlice(
                      "influxdb",
                      () -> {
                        started.countDown();
                        await(release);
                        return 1;
                      }));
      Future<Integer> b =
          executor.submit(
              () ->
                  coordinator.executeSlice(
                      "clickhouse",
                      () -> {
                        started.countDown();
                        await(release);
                        return 2;
                      }));
      assertTrue(started.await(2, TimeUnit.SECONDS));
      long start = System.nanoTime();
      IoTException busy =
          assertThrows(
              IoTException.class,
              () -> coordinator.executeTrend("tenant", "mysql", query("speed"), () -> result(1)));
      assertEquals(429, busy.getCode());
      assertTrue(System.nanoTime() - start < TimeUnit.SECONDS.toNanos(1));
      release.countDown();
      assertEquals(1, a.get(2, TimeUnit.SECONDS));
      assertEquals(2, b.get(2, TimeUnit.SECONDS));
      coordinator.executeSlice(
          "influxdb",
          () -> {
            IoTException local =
                assertThrows(
                    IoTException.class,
                    () ->
                        coordinator.executeTrend(
                            "tenant", "influxdb", query("speed"), () -> result(1)));
            assertEquals(429, local.getCode());
            assertEquals(3, coordinator.executeSlice("clickhouse", () -> 3));
            return 1;
          });
      assertEquals(0, coordinator.metrics().get("active"));
      assertEquals(
          100,
          coordinator
              .executeTrend("tenant", "influxdb", query("speed"), () -> result(1))
              .getTotalCount());
    } finally {
      release.countDown();
    }
  }

  @Test
  void failedSingleflightIsSharedThenRemovedAndNextRequestRetries() throws Exception {
    HistoryQueryCoordinator coordinator = coordinator();
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Future<?> leader =
          executor.submit(
              () ->
                  coordinator.executeTrend(
                      "tenant",
                      "influxdb",
                      query("speed"),
                      () -> {
                        started.countDown();
                        await(release);
                        throw new IoTException("database failed", 503);
                      }));
      assertTrue(started.await(2, TimeUnit.SECONDS));
      Future<?> follower =
          executor.submit(
              () ->
                  coordinator.executeTrend("tenant", "influxdb", query("speed"), () -> result(1)));
      until(() -> ((Number) coordinator.metrics().get("joinedRequests")).longValue() == 1);
      release.countDown();
      assertEquals(
          "database failed",
          assertThrows(ExecutionException.class, () -> leader.get(2, TimeUnit.SECONDS))
              .getCause()
              .getMessage());
      assertEquals(
          "database failed",
          assertThrows(ExecutionException.class, () -> follower.get(2, TimeUnit.SECONDS))
              .getCause()
              .getMessage());
      assertEquals(0, coordinator.metrics().get("cacheEntries"));
      assertEquals(0, coordinator.metrics().get("inFlightTrends"));
      assertEquals(0, coordinator.metrics().get("active"));
      assertEquals(
          100,
          coordinator
              .executeTrend("tenant", "influxdb", query("speed"), () -> result(1))
              .getTotalCount());
    } finally {
      release.countDown();
    }
  }

  @Test
  void followerDeadlineDoesNotFreeTheStillRunningDatabaseSlot() throws Exception {
    HistoryQueryCoordinator coordinator =
        new HistoryQueryCoordinator(
            1,
            1,
            TimeUnit.SECONDS.toNanos(5),
            8,
            100,
            TimeUnit.MILLISECONDS.toNanos(100),
            System::nanoTime);
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
      Future<?> leader =
          executor.submit(
              () ->
                  coordinator.executeTrend(
                      "tenant",
                      "influxdb",
                      query("speed"),
                      () -> {
                        started.countDown();
                        await(release);
                        return result(1);
                      }));
      assertTrue(started.await(2, TimeUnit.SECONDS));
      IoTException timeout =
          assertThrows(
              IoTException.class,
              () ->
                  coordinator.executeTrend("tenant", "influxdb", query("speed"), () -> result(1)));
      assertEquals(503, timeout.getCode());
      assertEquals(1, coordinator.metrics().get("active"));
      assertEquals(
          429,
          assertThrows(IoTException.class, () -> coordinator.executeSlice("clickhouse", () -> 1))
              .getCode());
      release.countDown();
      assertInstanceOf(
          IoTException.class,
          assertThrows(ExecutionException.class, () -> leader.get(2, TimeUnit.SECONDS)).getCause());
      assertEquals(0, coordinator.metrics().get("cacheEntries"));
      assertEquals(0, coordinator.metrics().get("active"));
      assertEquals(0, coordinator.metrics().get("inFlightTrends"));
    } finally {
      release.countDown();
    }
  }

  @Test
  void successfulCacheHasTtlLruAndTotalBucketBounds() {
    AtomicLong clock = new AtomicLong();
    HistoryQueryCoordinator coordinator =
        new HistoryQueryCoordinator(4, 2, 5, 2, 3, 100, clock::get);
    AtomicInteger calls = new AtomicInteger();
    coordinator.executeTrend(
        "tenant",
        "influxdb",
        query("a"),
        () -> {
          calls.incrementAndGet();
          return result(1);
        });
    coordinator.executeTrend(
        "tenant",
        "influxdb",
        query("b"),
        () -> {
          calls.incrementAndGet();
          return result(1);
        });
    coordinator.executeTrend(
        "tenant",
        "influxdb",
        query("a"),
        () -> {
          fail();
          return null;
        });
    coordinator.executeTrend(
        "tenant",
        "influxdb",
        query("c"),
        () -> {
          calls.incrementAndGet();
          return result(2);
        });
    assertEquals(2, coordinator.metrics().get("cacheEntries"));
    assertEquals(3, coordinator.metrics().get("cachedBuckets"));
    coordinator.executeTrend(
        "tenant",
        "influxdb",
        query("a"),
        () -> {
          fail();
          return null;
        });
    coordinator.executeTrend(
        "tenant",
        "influxdb",
        query("b"),
        () -> {
          calls.incrementAndGet();
          return result(1);
        });
    assertEquals(4, calls.get());
    clock.set(5);
    assertEquals(0, coordinator.metrics().get("cacheEntries"));
    assertEquals(0, coordinator.metrics().get("cachedBuckets"));
    coordinator.executeTrend(
        "tenant",
        "influxdb",
        query("a"),
        () -> {
          calls.incrementAndGet();
          return result(1);
        });
    assertEquals(5, calls.get());
  }

  @Test
  void tenantPolicyAndEveryEffectiveFilterSeparateCacheKeys() {
    HistoryQueryCoordinator coordinator = coordinator();
    AtomicInteger calls = new AtomicInteger();
    coordinator.executeTrend(
        "one",
        "influxdb",
        query("speed"),
        () -> {
          calls.incrementAndGet();
          return result(1);
        });
    coordinator.executeTrend(
        "two",
        "influxdb",
        query("speed"),
        () -> {
          calls.incrementAndGet();
          return result(1);
        });
    coordinator.executeTrend(
        "one",
        "clickhouse",
        query("speed"),
        () -> {
          calls.incrementAndGet();
          return result(1);
        });
    for (int i = 0; i < 10; i++) {
      PropertyTrendQuery changed = query("speed");
      switch (i) {
        case 0 -> changed.setProductKey("different");
        case 1 -> changed.setIotId("different");
        case 2 -> changed.setDeviceId("different");
        case 3 -> changed.setProperty("different");
        case 4 -> changed.setBeginCreateTime(1700000001L);
        case 5 -> changed.setEndCreateTime(1700000061L);
        case 6 -> changed.setMaxPoints(21);
        case 7 -> changed.setDeviceId(null);
        case 8 -> changed.setMaxScanRows(50000);
        case 9 -> changed.setSamplingMode("exact");
      }
      coordinator.executeTrend(
          "one",
          "influxdb",
          changed,
          () -> {
            calls.incrementAndGet();
            return result(1);
          });
    }
    assertEquals(13, calls.get());
    PropertyTrendQuery noisy = query("speed");
    noisy.setParams(Map.of("event", "injected"));
    noisy.setEvent("event");
    noisy.setId("id");
    noisy.setDeviceName("name");
    noisy.setBeforeId(1L);
    coordinator.executeTrend(
        "one",
        " INFLUXDB ",
        noisy,
        () -> {
          fail();
          return null;
        });
    assertNull(noisy.getParams());
    assertNull(noisy.getEvent());
    assertNull(noisy.getId());
    assertNull(noisy.getDeviceName());
    assertNull(noisy.getBeforeId());
    assertEquals("PROPERTIES", noisy.getMessageType());
  }

  @Test
  void cachedSamplesKeepTheirTruncationAndBudgetMetadata() {
    HistoryQueryCoordinator coordinator = coordinator();
    PropertyTrendVO sampled = result(1);
    sampled.setSampled(true);
    sampled.setCountExact(false);
    sampled.setScannedCount(101);
    sampled.setScanLimit(1000);
    sampled.setSamplingMode("time-window-sample");
    coordinator.executeTrend("tenant", "mysql", query("speed"), () -> sampled);
    PropertyTrendVO cached = coordinator.executeTrend("tenant", "mysql", query("speed"), () -> {
      fail("Second request must hit cache");
      return null;
    });
    assertTrue(cached.isSampled());
    assertFalse(cached.isCountExact());
    assertEquals(101, cached.getScannedCount());
    assertEquals(1000, cached.getScanLimit());
    assertEquals("time-window-sample", cached.getSamplingMode());
  }

  @Test
  void invalidRequestsAndLateOrMalformedResultsNeverCacheAndReleasePermits() {
    AtomicLong clock = new AtomicLong();
    HistoryQueryCoordinator coordinator =
        new HistoryQueryCoordinator(1, 1, 5, 8, 100, 10, clock::get);
    PropertyTrendQuery invalid = query("speed");
    invalid.setIotId(null);
    assertEquals(
        400,
        assertThrows(
                IoTException.class,
                () -> coordinator.executeTrend("tenant", "influxdb", invalid, () -> result(1)))
            .getCode());
    assertEquals(
        503,
        assertThrows(
                IoTException.class,
                () ->
                    coordinator.executeTrend(
                        "tenant",
                        "influxdb",
                        query("speed"),
                        () -> {
                          clock.addAndGet(10);
                          return result(1);
                        }))
            .getCode());
    assertThrows(
        IoTException.class,
        () -> coordinator.executeTrend("tenant", "influxdb", query("speed"), () -> null));
    assertThrows(
        IoTException.class,
        () -> coordinator.executeTrend("tenant", "influxdb", query("speed"), () -> result(21)));
    assertThrows(
        AssertionError.class,
        () ->
            coordinator.executeTrend(
                "tenant",
                "influxdb",
                query("speed"),
                () -> {
                  throw new AssertionError("failure");
                }));
    assertEquals(0, coordinator.metrics().get("active"));
    assertEquals(0, coordinator.metrics().get("cacheEntries"));
    assertEquals(0, coordinator.metrics().get("inFlightTrends"));
    assertEquals(
        100,
        coordinator
            .executeTrend("tenant", "influxdb", query("speed"), () -> result(1))
            .getTotalCount());
  }

  private void await(CountDownLatch latch) {
    try {
      assertTrue(latch.await(2, TimeUnit.SECONDS), "测试同步超时");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError(e);
    }
  }

  private void until(java.util.function.BooleanSupplier condition) {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
    while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
      LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
    }
    assertTrue(condition.getAsBoolean(), "测试同步超时");
  }
}
