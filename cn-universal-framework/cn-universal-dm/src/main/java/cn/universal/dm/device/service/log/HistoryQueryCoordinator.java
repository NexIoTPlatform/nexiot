package cn.universal.dm.device.service.log;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.query.PropertyTrendQuery;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 进程级历史读取预算，独立于采集写入线程池。相同趋势合并请求，成功结果短暂缓存。 领导请求在调用线程执行，数据库适配器必须自行配置原生查询超时；等待者超时不会提前释放仍在查询的名额。 */
@Component
public class HistoryQueryCoordinator {
  private final int globalLimit;
  private final int policyLimit;
  private final int maxEntries;
  private final int maxBuckets;
  private final long ttlNanos;
  private final long timeoutNanos;
  private final LongSupplier nanoClock;
  private final Semaphore globalPermits;
  private final Map<String, Semaphore> policyPermits = new ConcurrentHashMap<>();
  private final Map<TrendKey, Flight> flights = new ConcurrentHashMap<>();
  private final LinkedHashMap<TrendKey, CachedTrend> cache = new LinkedHashMap<>(16, 0.75f, true);
  private int cachedBuckets;
  private final LongAdder cacheHits = new LongAdder();
  private final LongAdder joinedRequests = new LongAdder();
  private final LongAdder executed = new LongAdder();
  private final LongAdder rejected = new LongAdder();
  private final LongAdder failed = new LongAdder();
  private final LongAdder timedOut = new LongAdder();

  @Autowired
  public HistoryQueryCoordinator(
      @Value("${nexiot.log.history.max-concurrent:4}") int globalLimit,
      @Value("${nexiot.log.history.max-concurrent-per-policy:2}") int policyLimit,
      @Value("${nexiot.log.history.cache-ttl-seconds:5}") long ttlSeconds,
      @Value("${nexiot.log.history.cache-max-entries:128}") int maxEntries,
      @Value("${nexiot.log.history.cache-max-buckets:128000}") int maxBuckets,
      @Value("${nexiot.log.history.query-timeout-seconds:8}") long timeoutSeconds) {
    this(
        globalLimit,
        policyLimit,
        TimeUnit.SECONDS.toNanos(ttlSeconds),
        maxEntries,
        maxBuckets,
        TimeUnit.SECONDS.toNanos(timeoutSeconds),
        System::nanoTime);
  }

  HistoryQueryCoordinator(
      int globalLimit,
      int policyLimit,
      long ttlNanos,
      int maxEntries,
      int maxBuckets,
      long timeoutNanos,
      LongSupplier nanoClock) {
    if (globalLimit < 1
        || policyLimit < 1
        || ttlNanos < 0
        || maxEntries < 1
        || maxBuckets < 1
        || timeoutNanos < 1) {
      throw new IllegalArgumentException("历史查询并发、容量、超时必须为正数，缓存TTL不能为负数");
    }
    this.globalLimit = globalLimit;
    this.policyLimit = Math.min(policyLimit, globalLimit);
    this.ttlNanos = ttlNanos;
    this.maxEntries = maxEntries;
    this.maxBuckets = maxBuckets;
    this.timeoutNanos = timeoutNanos;
    this.nanoClock = Objects.requireNonNull(nanoClock);
    globalPermits = new Semaphore(globalLimit);
  }

  public PropertyTrendVO executeTrend(
      String tenantId,
      String policy,
      PropertyTrendQuery query,
      Supplier<PropertyTrendVO> operation) {
    normalizeTrendQuery(query);
    String policyKey = normalizePolicy(policy);
    TrendKey key =
        new TrendKey(
            tenantId,
            policyKey,
            query.getProductKey(),
            query.getIotId(),
            query.getDeviceId(),
            query.getProperty(),
            query.getBeginCreateTime(),
            query.getEndCreateTime(),
            query.getMaxPoints(),
            query.getMaxScanRows(),
            query.getSamplingMode());
    PropertyTrendVO cached = findCached(key);
    if (cached != null) return cached;
    long deadline = nanoClock.getAsLong() + timeoutNanos;
    Flight proposed = new Flight(new CompletableFuture<>(), deadline);
    Flight existing = flights.putIfAbsent(key, proposed);
    if (existing != null) {
      joinedRequests.increment();
      return await(existing, deadline);
    }
    try {
      // 前一次查询可能刚好在cache读取之后结束。
      cached = findCached(key);
      if (cached != null) {
        proposed.result().complete(cached);
        return copy(cached);
      }
      try (Permit permit = acquire(policyKey)) {
        executed.increment();
        PropertyTrendVO result = operation.get();
        checkDeadline(deadline);
        if (result == null
            || result.getBuckets() == null
            || result.getBuckets().size() > query.getMaxPoints()) {
          throw new IoTException("历史趋势返回无效或超量结果，请稍后重试", 503);
        }
        PropertyTrendVO snapshot = copy(result);
        cache(key, snapshot);
        proposed.result().complete(snapshot);
        return copy(snapshot);
      }
    } catch (Throwable failure) {
      failed.increment();
      proposed.result().completeExceptionally(failure);
      throw propagate(failure);
    } finally {
      flights.remove(key, proposed);
    }
  }

  public <T> T executeSlice(String policy, Supplier<T> operation) {
    long deadline = nanoClock.getAsLong() + timeoutNanos;
    try (Permit permit = acquire(normalizePolicy(policy))) {
      executed.increment();
      T result = operation.get();
      checkDeadline(deadline);
      return result;
    } catch (RuntimeException | Error failure) {
      failed.increment();
      throw failure;
    }
  }

  /** 趋势固定为单设备单属性；分页、事件、文本筛选和嵌套旧参数不参与任何引擎查询。 */
  private void normalizeTrendQuery(PropertyTrendQuery query) {
    query.validateRange();
    query.setParams(null);
    query.setEvent(null);
    query.setId(null);
    query.setDeviceName(null);
    query.setExtDeviceId(null);
    query.setCursor(null);
    query.setCursorMode(false);
    query.setBeforeId(null);
    query.setBeforeTime(null);
    query.setPageNum(1);
    query.setPageSize(50);
  }

  private Permit acquire(String policy) {
    if (!globalPermits.tryAcquire()) {
      rejected.increment();
      throw busy();
    }
    Semaphore local = policyPermits.computeIfAbsent(policy, ignored -> new Semaphore(policyLimit));
    if (!local.tryAcquire()) {
      globalPermits.release();
      rejected.increment();
      throw busy();
    }
    return new Permit(local);
  }

  private PropertyTrendVO await(Flight flight, long requestDeadline) {
    long remaining = Math.min(flight.deadline(), requestDeadline) - nanoClock.getAsLong();
    if (remaining <= 0) throw timeout();
    try {
      PropertyTrendVO result = flight.result().get(remaining, TimeUnit.NANOSECONDS);
      checkDeadline(Math.min(flight.deadline(), requestDeadline));
      return copy(result);
    } catch (TimeoutException e) {
      throw timeout();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IoTException("历史查询已取消，请稍后重试", 503);
    } catch (ExecutionException e) {
      throw propagate(e.getCause());
    }
  }

  private void checkDeadline(long deadline) {
    if (nanoClock.getAsLong() - deadline >= 0) throw timeout();
  }

  private IoTException timeout() {
    timedOut.increment();
    return new IoTException("历史查询超时，请缩小时间范围后重试", 503);
  }

  private IoTException busy() {
    return new IoTException("历史查询繁忙，请稍后重试", 429);
  }

  private RuntimeException propagate(Throwable failure) {
    if (failure instanceof Error error) throw error;
    if (failure instanceof RuntimeException runtime) return runtime;
    return new IoTException("历史查询失败，请稍后重试", 503, failure);
  }

  private String normalizePolicy(String policy) {
    return policy == null || policy.isBlank() ? "mysql" : policy.toLowerCase(Locale.ROOT).strip();
  }

  private synchronized PropertyTrendVO findCached(TrendKey key) {
    expireCache();
    CachedTrend entry = cache.get(key);
    if (entry == null) return null;
    cacheHits.increment();
    return copy(entry.value());
  }

  private synchronized void cache(TrendKey key, PropertyTrendVO result) {
    int bucketCount = result.getBuckets().size();
    if (ttlNanos == 0 || bucketCount > maxBuckets) return;
    expireCache();
    CachedTrend replaced =
        cache.put(key, new CachedTrend(result, nanoClock.getAsLong() + ttlNanos));
    if (replaced != null) cachedBuckets -= replaced.value().getBuckets().size();
    cachedBuckets += bucketCount;
    Iterator<CachedTrend> entries = cache.values().iterator();
    while (cache.size() > maxEntries || cachedBuckets > maxBuckets) {
      cachedBuckets -= entries.next().value().getBuckets().size();
      entries.remove();
    }
  }

  private void expireCache() {
    long now = nanoClock.getAsLong();
    Iterator<CachedTrend> entries = cache.values().iterator();
    while (entries.hasNext()) {
      CachedTrend entry = entries.next();
      if (now - entry.expiresAt() >= 0) {
        cachedBuckets -= entry.value().getBuckets().size();
        entries.remove();
      }
    }
  }

  public synchronized Map<String, Object> metrics() {
    expireCache();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("maxConcurrent", globalLimit);
    result.put("maxConcurrentPerPolicy", policyLimit);
    result.put("active", globalLimit - globalPermits.availablePermits());
    result.put("inFlightTrends", flights.size());
    result.put("cacheEntries", cache.size());
    result.put("cachedBuckets", cachedBuckets);
    result.put("cacheHits", cacheHits.sum());
    result.put("joinedRequests", joinedRequests.sum());
    result.put("executed", executed.sum());
    result.put("rejected", rejected.sum());
    result.put("failed", failed.sum());
    result.put("timedOut", timedOut.sum());
    return result;
  }

  private PropertyTrendVO copy(PropertyTrendVO source) {
    PropertyTrendVO result = new PropertyTrendVO();
    result.setBeginCreateTime(source.getBeginCreateTime());
    result.setEndCreateTime(source.getEndCreateTime());
    result.setIntervalSeconds(source.getIntervalSeconds());
    result.setTotalCount(source.getTotalCount());
    result.setNumericCount(source.getNumericCount());
    result.setSampled(source.isSampled());
    result.setCountExact(source.isCountExact());
    result.setScannedCount(source.getScannedCount());
    result.setScanLimit(source.getScanLimit());
    result.setSamplingMode(source.getSamplingMode());
    ArrayList<PropertyTrendVO.Bucket> buckets = new ArrayList<>(source.getBuckets().size());
    for (PropertyTrendVO.Bucket value : source.getBuckets()) {
      PropertyTrendVO.Bucket bucket = new PropertyTrendVO.Bucket();
      bucket.setTime(value.getTime());
      bucket.setCount(value.getCount());
      bucket.setNumericCount(value.getNumericCount());
      bucket.setMin(value.getMin());
      bucket.setMax(value.getMax());
      bucket.setAvg(value.getAvg());
      buckets.add(bucket);
    }
    result.setBuckets(buckets);
    return result;
  }

  private record TrendKey(
      String tenantId,
      String policy,
      String product,
      String iotId,
      String deviceId,
      String property,
      long begin,
      long end,
      int maxPoints,
      int maxScanRows,
      String samplingMode) {}

  private record Flight(CompletableFuture<PropertyTrendVO> result, long deadline) {}

  private record CachedTrend(PropertyTrendVO value, long expiresAt) {}

  private final class Permit implements AutoCloseable {
    private final Semaphore local;

    private Permit(Semaphore local) {
      this.local = local;
    }

    @Override
    public void close() {
      local.release();
      globalPermits.release();
    }
  }
}
