/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 *
 * @Author: gitee.com/NexIoT
 *
 * @Email: wo8335224@gmail.com
 *
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.dm.device.service.log;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.dm.device.service.log.writer.DeviceLogEntityWrite;
import cn.universal.dm.device.service.log.writer.DeviceLogWriteBuffer;
import cn.universal.dm.device.service.log.writer.DeviceLogWriteRecord;
import cn.universal.dm.device.service.log.writer.DeviceLogWriteRequest;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.query.DeviceLogQuery;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import cn.universal.persistence.query.PropertyTrendQuery;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/9/23
 */
@Component
@Slf4j
public class DefaultIoTDeviceLogService implements IIoTDeviceDataService, ApplicationContextAware {

  @Resource private IoTDeviceMapper ioTDeviceMapper;

  @Resource private IoTProductDeviceService iotProductDeviceService;

  @Resource private StringRedisTemplate stringRedisTemplate;

  @Resource private DeviceLogWriteBuffer deviceLogWriteBuffer;

  @Resource private HistoryQueryCoordinator historyQueryCoordinator;

  private Map<String, IIoTDeviceLogService> policies = new ConcurrentHashMap<>();
  private final Map<String, PolicyRuntimeMetrics> policyRuntimeMetrics = new ConcurrentHashMap<>();

  // Cache successful summaries and coalesce identical loads before consuming history permits.
  private final Cache<EventTotalKey, PageBean<IoTDeviceEvents>> eventTotalCache =
      Caffeine.newBuilder().maximumSize(1024).expireAfterWrite(Duration.ofSeconds(30)).build();

  private record EventTotalKey(String tenantId, String policy, String productKey, String iotId) {}

  @Autowired
  @Qualifier("virtualScheduledExecutor")
  private ScheduledExecutorService scheduledExecutorService;

  private AtomicBoolean scheduled = new AtomicBoolean();

  @Value("${iot.device.online-flush-batch-size:200}")
  private Integer onlineFlushBatchSize;

  // 1. online_time基础刷新间隔，默认12秒
  @Value("${iot.device.online-flush-interval-ms:12000}")
  private Long onlineFlushIntervalMs;

  // 2. 单设备online_time刷新限频窗口，默认60秒；小于等于0表示关闭限频
  @Value("${iot.device.online-refresh-interval-seconds:60}")
  private Long onlineRefreshIntervalSeconds;

  // 3. 单设备online_time刷新限频Redis key前缀
  @Value("${iot.device.online-refresh-key-prefix:iot:device:online-refresh:}")
  private String onlineRefreshKeyPrefix;

  // 4. 节点级偏移范围（0~8秒，打散集群节点的基准时间）
  private static final int NODE_OFFSET_RANGE = 8000;
  // 5. 每次执行的随机抖动范围（±200ms，避免长期运行后趋同）
  private static final int RANDOM_JITTER_RANGE = 200;
  private static final long LIMITER_WARN_INTERVAL_MS = 60 * 1000L;
  private static final int LOCAL_ONLINE_REFRESH_CACHE_MAX_SIZE = 100_000;
  private static final long LOCAL_ONLINE_REFRESH_CLEANUP_INTERVAL_MS = 60 * 1000L;
  // 节点专属的固定偏移量（每个节点启动时生成一次）
  private long nodeFixedOffset;
  private final AtomicLong lastLimiterWarnTime = new AtomicLong();
  private final Map<String, AtomicLong> localOnlineRefreshDeadlineMs = new ConcurrentHashMap<>();
  private final AtomicLong nextOnlineRefreshCleanupAt = new AtomicLong();
  private final AtomicLong onlineRefreshLocalSkipCount = new AtomicLong();
  private final AtomicLong onlineRefreshRedisAttemptCount = new AtomicLong();
  private final AtomicLong onlineRefreshRedisAllowCount = new AtomicLong();
  private final AtomicLong onlineRefreshRedisRejectCount = new AtomicLong();
  private final AtomicLong onlineRefreshRedisErrorCount = new AtomicLong();

  // 初始化节点偏移量（构造方法中执行，保证每个节点唯一）
  public DefaultIoTDeviceLogService() {
    // 生成0~8000ms的固定偏移量，不同节点启动时生成的值不同
    this.nodeFixedOffset = RandomUtil.randomInt(0, NODE_OFFSET_RANGE);
    log.info("当前节点定时任务基础偏移量：{}ms", nodeFixedOffset);
  }

  private Set<String> devBoSet = new ConcurrentHashSet<>();

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    Map<String, IIoTDeviceLogService> beansOfType =
        applicationContext.getBeansOfType(IIoTDeviceLogService.class);
    beansOfType.forEach(
        (k, v) -> {
          policies.put(v.getPolicy().toLowerCase(Locale.ROOT), v);
        });
    if (scheduled.compareAndSet(false, true)) {
      // 4. 初始延迟 = 节点固定偏移 + 小范围随机抖动（首次执行就打散）
      long initialDelay =
          nodeFixedOffset + RandomUtil.randomInt(-RANDOM_JITTER_RANGE, RANDOM_JITTER_RANGE);
      // 确保初始延迟不为负数
      initialDelay = Math.max(0, initialDelay);

      // 5. 循环间隔 = 基础间隔 + 每次随机抖动（避免固定间隔趋同）
      long flushInterval = normalizeFlushIntervalMs();
      long delay = Math.max(1000L, flushInterval + randomJitter());
      scheduledExecutorService.scheduleWithFixedDelay(
          this::doOnline,
          initialDelay, // 节点专属初始延迟，保证集群启动时不同时执行
          delay,
          TimeUnit.MILLISECONDS);
      // 为集群的节点不在统一时间给数据库造成压力
      log.info(
          "当前节点定时任务已启动，初始延迟：{}ms，循环间隔：{}ms",
          initialDelay,
          delay);
    }
  }

  @Override
  public Set<String> getEnabledStorePolicies() {
    return policies.keySet().stream()
        .map(this::resolvePolicyKey)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public Map<String, Object> metrics() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("enabledPolicies", getEnabledStorePolicies());
    result.put("onlineRefresh", onlineRefreshMetrics());
    result.put("historyQueries", historyQueryCoordinator.metrics());
    result.put("writer", deviceLogWriteBuffer.metrics());
    Map<String, Object> policyMetrics = new LinkedHashMap<>();
    policies.forEach(
        (policy, service) -> {
          if (service != null) {
            String policyKey = resolvePolicyKey(policy);
            Map<String, Object> metadata = new LinkedHashMap<>();
            if (service.configMetadata() != null) {
              metadata.putAll(service.configMetadata());
            }
            metadata.put("writer", deviceLogWriteBuffer.policyMetrics(policyKey));
            metadata.put("runtime", metricsFor(policyKey).snapshot());
            policyMetrics.put(policyKey, metadata);
          }
        });
    result.put("policies", policyMetrics);
    return result;
  }

  @Override
  public void saveDeviceLog(
      BaseUPRequest upRequest, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {
    saveDeviceLogInternal(upRequest, ioTDeviceDTO, ioTProduct, false);
  }

  @Override
  public void saveDeviceLogDirect(
      BaseUPRequest upRequest, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {
    saveDeviceLogInternal(upRequest, ioTDeviceDTO, ioTProduct, true);
  }

  private void saveDeviceLogInternal(
      BaseUPRequest upRequest,
      IoTDeviceDTO ioTDeviceDTO,
      IoTProduct ioTProduct,
      boolean direct) {
    String policyKey = resolvePolicyKey(ioTProduct.getStorePolicy());
    IIoTDeviceLogService logService = resolveLogService(policyKey);
    if (direct) {
      invokeSave(policyKey, logService, true, upRequest, ioTDeviceDTO, ioTProduct);
    } else {
      submitSave(policyKey, logService, upRequest, ioTDeviceDTO, ioTProduct);
    }
    // SIP 日志来自语音服务，包括离线通知；在线状态只由 SIP 注册生命周期维护。
    if (!"sip".equalsIgnoreCase(ioTProduct.getThirdPlatform())
        && !IoTConstant.notDeviceDataReportEvent.contains(upRequest.getEvent())
        && MessageType.devReallyReport(upRequest.getMessageType())) {
      updateOnline(ioTDeviceDTO.getIotId());
    }
    // 开启MYSQL双写
    if (!IoTConstant.MYSQL.equals(resolvePolicyKey(ioTProduct.getStorePolicy()))
        && enableMysqlWrite(upRequest, ioTProduct, direct)) {
      IIoTDeviceLogService mysqlLogService = policies.get(IoTConstant.MYSQL);
      if (mysqlLogService != null) {
        if (direct) {
          invokeSave(IoTConstant.MYSQL, mysqlLogService, true, upRequest, ioTDeviceDTO, ioTProduct);
        } else {
          submitSave(IoTConstant.MYSQL, mysqlLogService, upRequest, ioTDeviceDTO, ioTProduct);
        }
      }
    }
  }

  @Override
  public void saveDeviceLog(
      IoTDeviceLog ioTDeviceLog, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {
    String policyKey = resolvePolicyKey(ioTProduct.getStorePolicy());
    IIoTDeviceLogService logService = resolveLogService(policyKey);
    long start = System.currentTimeMillis();
    boolean submitted =
        deviceLogWriteBuffer.submitEntity(
            policyKey, logService, ioTDeviceLog, ioTDeviceDTO, ioTProduct);
    // Queue saturation, direct fallback and drops are owned by DeviceLogWriteBuffer.
    metricsFor(policyKey).record(System.currentTimeMillis() - start, submitted);
    // SIP 服务端事件不能作为终端在线的依据。
    if (!"sip".equalsIgnoreCase(ioTProduct.getThirdPlatform())
        && !IoTConstant.notDeviceDataReportEvent.contains(ioTDeviceLog.getEvent())
        && MessageType.devReallyReport(MessageType.find(ioTDeviceLog.getMessageType()))) {
      updateOnline(ioTDeviceDTO.getIotId());
    }
  }

  private void submitSave(
      String policyKey,
      IIoTDeviceLogService logService,
      BaseUPRequest upRequest,
      IoTDeviceDTO ioTDeviceDTO,
      IoTProduct ioTProduct) {
    long start = System.currentTimeMillis();
    boolean success = false;
    try {
      success =
          deviceLogWriteBuffer.submitRequest(
              policyKey, logService, upRequest, ioTDeviceDTO, ioTProduct);
    } finally {
      metricsFor(policyKey).record(System.currentTimeMillis() - start, success);
    }
  }

  private void invokeSave(
      String policyKey,
      IIoTDeviceLogService logService,
      boolean direct,
      BaseUPRequest upRequest,
      IoTDeviceDTO ioTDeviceDTO,
      IoTProduct ioTProduct) {
    long start = System.currentTimeMillis();
    boolean success = false;
    try {
      if (direct) {
        logService.saveDeviceLogDirect(upRequest, ioTDeviceDTO, ioTProduct);
      } else {
        logService.saveDeviceLog(upRequest, ioTDeviceDTO, ioTProduct);
      }
      success = true;
    } finally {
      metricsFor(policyKey).record(System.currentTimeMillis() - start, success);
    }
  }

  /** Persist an immutable MQ event without entering the producer buffer again. */
  public void persistMqRecord(DeviceLogWriteRecord record) {
    persistMqRecords(List.of(record));
  }

  /** Persist an MQ poll batch grouped by storage policy and concrete service. */
  public void persistMqRecords(List<DeviceLogWriteRecord> records) {
    if (records == null || records.isEmpty()) {
      return;
    }
    Map<MqServiceKey, MqWriteBatch> grouped = new LinkedHashMap<>();
    for (DeviceLogWriteRecord record : records) {
      if (record == null || record.schemaVersion() != DeviceLogWriteRecord.CURRENT_SCHEMA_VERSION) {
        throw new IllegalArgumentException("Unsupported device-log event schema");
      }
      IoTDeviceDTO device = record.toDevice();
      IoTProduct product = record.toProduct();
      if (product == null) {
        throw new IllegalArgumentException("Device-log event has no product snapshot");
      }
      String policyKey =
          resolvePolicyKey(StrUtil.blankToDefault(record.policy(), product.getStorePolicy()));
      IIoTDeviceLogService logService = resolveLogService(policyKey);
      if (record.requestWrite()) {
        BaseUPRequest request = record.toRequest();
        requestBatch(grouped, policyKey, logService)
            .requests()
            .add(new DeviceLogWriteRequest(request, device, product));
      } else if (record.entityWrite()) {
        requestBatch(grouped, policyKey, logService)
            .entities()
            .add(new DeviceLogEntityWrite(record.toDeviceLog(), device, product));
      } else {
        throw new IllegalArgumentException("Device-log event has no payload");
      }
    }
    grouped.values().forEach(this::flushMqBatch);
  }

  private MqWriteBatch requestBatch(
      Map<MqServiceKey, MqWriteBatch> grouped,
      String policyKey,
      IIoTDeviceLogService logService) {
    return grouped.computeIfAbsent(
        new MqServiceKey(policyKey, logService),
        ignored -> new MqWriteBatch(policyKey, logService, new ArrayList<>(), new ArrayList<>()));
  }

  private void flushMqBatch(MqWriteBatch batch) {
    long start = System.currentTimeMillis();
    boolean success = false;
    try {
      if (!batch.requests().isEmpty()) {
        batch.logService().flushDeviceLogRequests(batch.requests());
      }
      if (!batch.entities().isEmpty()) {
        batch.logService().flushDeviceLogEntities(batch.entities());
      }
      success = true;
    } finally {
      metricsFor(batch.policyKey()).record(System.currentTimeMillis() - start, success);
    }
  }

  private record MqServiceKey(String policyKey, IIoTDeviceLogService logService) {}

  private record MqWriteBatch(
      String policyKey,
      IIoTDeviceLogService logService,
      List<DeviceLogWriteRequest> requests,
      List<DeviceLogEntityWrite> entities) {}

  private boolean enableMysqlWrite(BaseUPRequest upRequest, IoTProduct ioTProduct, boolean direct) {
    Boolean configured = enableMysqlWriteFromLoadedProduct(ioTProduct);
    if (configured != null) {
      return configured;
    }
    if (direct && ioTProduct != null) {
      return false;
    }
    return Boolean.TRUE.equals(
        iotProductDeviceService.enableMysqlWrite(
            upRequest == null ? null : upRequest.getProductKey()));
  }

  private Boolean enableMysqlWriteFromLoadedProduct(IoTProduct ioTProduct) {
    if (ioTProduct == null) {
      return null;
    }
    String configuration = ioTProduct.getConfiguration();
    if (StrUtil.isBlank(configuration)) {
      return Boolean.FALSE;
    }
    try {
      if (!JSONUtil.isTypeJSON(configuration)) {
        return Boolean.FALSE;
      }
      JSONObject config = JSONUtil.parseObj(configuration);
      return config.getBool(IoTConstant.ENABLE_MYSQL_WRITE, Boolean.FALSE);
    } catch (Exception e) {
      log.warn(
          "解析产品日志双写配置失败，productKey={}, err={}",
          ioTProduct.getProductKey(),
          e.getMessage());
      return Boolean.FALSE;
    }
  }

  @Override
  public PageBean<IoTDeviceLogVO> pageList(LogQuery logQuery) {
    HistoryPageSupport.prepare(logQuery, false);
    IoTProduct product = iotProductDeviceService.getProduct(logQuery.getProductKey());
    IIoTDeviceLogService logService = resolveLogService(product.getStorePolicy());
    // 优化点1：使用封装的工具方法处理空值
    return getSafePageBean(
        historyQueryCoordinator.executeSlice(
            logService.getPolicy(), () -> logService.pageList(logQuery)));
  }

  @Override
  public IoTDeviceLogVO queryById(LogQuery logQuery) {
    IoTProduct product = iotProductDeviceService.getProduct(logQuery.getProductKey());
    IIoTDeviceLogService logService = resolveLogService(product.getStorePolicy());
    return logService.queryById(logQuery);
  }

  @Override
  public PageBean<IoTDeviceEvents> queryEventTotal(String productKey, String iotId) {
    IoTProduct product = iotProductDeviceService.getProduct(productKey);
    IIoTDeviceLogService logService = resolveLogService(product.getStorePolicy());
    EventTotalKey key =
        new EventTotalKey(product.getTenantId(), logService.getPolicy(), productKey, iotId);
    PageBean<IoTDeviceEvents> summary =
        eventTotalCache.get(
            key,
            ignored ->
                copyEventTotals(
                    getSafePageBean(
                        historyQueryCoordinator.executeSlice(
                            logService.getPolicy(),
                            () -> logService.queryEventTotal(productKey, iotId)))));
    return copyEventTotals(summary);
  }

  private PageBean<IoTDeviceEvents> copyEventTotals(PageBean<IoTDeviceEvents> source) {
    List<IoTDeviceEvents> events =
        source.getList() == null
            ? List.of()
            : source.getList().stream()
                .map(
                    event ->
                        IoTDeviceEvents.builder()
                            .id(event.getId())
                            .name(event.getName())
                            .level(event.getLevel())
                            .description(event.getDescription())
                            .valueType(event.getValueType())
                            .expands(event.getExpands())
                            .storagePolicy(event.isStoragePolicy())
                            .qty(event.getQty())
                            .time(event.getTime())
                            .build())
                .collect(Collectors.toList());
    return new PageBean<>(events, source.getTotalCount(), 100, 1);
  }

  @Override
  public PageBean<IoTDeviceLogMetadataVO> queryLogMeta(LogQuery logQuery) {
    HistoryPageSupport.prepare(logQuery, true);
    IoTProduct product = iotProductDeviceService.getProduct(logQuery.getProductKey());
    IIoTDeviceLogService logService = resolveLogService(product.getStorePolicy());
    // 优化点3：复用工具方法
    return getSafePageBean(
        historyQueryCoordinator.executeSlice(
            logService.getPolicy(), () -> logService.queryLogMeta(logQuery)));
  }

  @Override
  public PropertyTrendVO queryPropertyTrend(PropertyTrendQuery query) {
    query.validateRange();
    IoTProduct product = iotProductDeviceService.getProduct(query.getProductKey());
    IIoTDeviceLogService logService = resolveLogService(product.getStorePolicy());
    return historyQueryCoordinator.executeTrend(
        product.getTenantId(),
        logService.getPolicy(),
        query,
        () -> logService.queryPropertyTrend(query));
  }

  /**
   * 通用的分页结果空值处理工具方法
   *
   * @param pageBean 原始分页结果
   * @return 非空的分页结果（如果原始为null则返回空分页对象）
   */
  private <T> PageBean<T> getSafePageBean(PageBean<T> pageBean) {
    // 使用Optional简化空值判断，同时初始化默认值
    return Optional.ofNullable(pageBean)
        .map(
            p -> {
              // 确保totalCount不为null
              if (p.getTotalCount() == null) {
                p.setTotalCount(0L);
              }
              return p;
            })
        .orElseGet(
            () -> {
              PageBean<T> emptyPage = new PageBean<>();
              emptyPage.setTotalCount(0L);
              return emptyPage;
            });
  }

  private void updateOnline(String iotId) {
    if (StrUtil.isBlank(iotId) || !allowOnlineTimeRefresh(iotId)) {
      return;
    }
    int batchSize = normalizeFlushBatchSize();
    if (devBoSet.size() < batchSize) {
      devBoSet.add(iotId);
    } else {
      log.warn("设备最后更新时间达到批量阈值，threshold={}", batchSize);
      // just write current devBoSet to file.
      doOnline();
      // after force writing, add accessLogData to current devBoSet
      devBoSet.add(iotId);
    }
  }

  private boolean allowOnlineTimeRefresh(String iotId) {
    long refreshIntervalSeconds = normalizeRefreshIntervalSeconds();
    if (refreshIntervalSeconds <= 0) {
      return true;
    }
    long now = System.currentTimeMillis();
    long refreshIntervalMs = TimeUnit.SECONDS.toMillis(Math.max(1L, refreshIntervalSeconds));
    if (!tryAcquireLocalOnlineRefreshSlot(iotId, now, refreshIntervalMs)) {
      onlineRefreshLocalSkipCount.incrementAndGet();
      return false;
    }
    onlineRefreshRedisAttemptCount.incrementAndGet();
    try {
      Boolean acquired =
          stringRedisTemplate
              .opsForValue()
              .setIfAbsent(
                  onlineRefreshKeyPrefix + iotId,
                  "1",
                  refreshIntervalSeconds,
                  TimeUnit.SECONDS);
      if (Boolean.TRUE.equals(acquired)) {
        onlineRefreshRedisAllowCount.incrementAndGet();
        return true;
      }
      onlineRefreshRedisRejectCount.incrementAndGet();
      return false;
    } catch (Exception e) {
      onlineRefreshRedisErrorCount.incrementAndGet();
      logLimiterException(iotId, e);
      return true;
    }
  }

  private boolean tryAcquireLocalOnlineRefreshSlot(
      String iotId, long now, long refreshIntervalMs) {
    cleanupLocalOnlineRefreshCache(now);
    AtomicLong deadline =
        localOnlineRefreshDeadlineMs.computeIfAbsent(iotId, ignored -> new AtomicLong());
    while (true) {
      long current = deadline.get();
      if (now < current) {
        return false;
      }
      long nextDeadline = now + refreshIntervalMs;
      if (deadline.compareAndSet(current, nextDeadline)) {
        return true;
      }
    }
  }

  private void cleanupLocalOnlineRefreshCache(long now) {
    long nextCleanup = nextOnlineRefreshCleanupAt.get();
    if (localOnlineRefreshDeadlineMs.size() <= LOCAL_ONLINE_REFRESH_CACHE_MAX_SIZE
        && now < nextCleanup) {
      return;
    }
    if (!nextOnlineRefreshCleanupAt.compareAndSet(
        nextCleanup, now + LOCAL_ONLINE_REFRESH_CLEANUP_INTERVAL_MS)) {
      return;
    }
    localOnlineRefreshDeadlineMs
        .entrySet()
        .removeIf(entry -> entry.getValue() == null || entry.getValue().get() <= now);
  }

  private Map<String, Object> onlineRefreshMetrics() {
    Map<String, Object> metrics = new LinkedHashMap<>();
    metrics.put("localCacheSize", localOnlineRefreshDeadlineMs.size());
    metrics.put("pendingOnlineFlushCount", devBoSet.size());
    metrics.put("refreshIntervalSeconds", normalizeRefreshIntervalSeconds());
    metrics.put("localSkipCount", onlineRefreshLocalSkipCount.get());
    metrics.put("redisAttemptCount", onlineRefreshRedisAttemptCount.get());
    metrics.put("redisAllowCount", onlineRefreshRedisAllowCount.get());
    metrics.put("redisRejectCount", onlineRefreshRedisRejectCount.get());
    metrics.put("redisErrorCount", onlineRefreshRedisErrorCount.get());
    return metrics;
  }

  private void logLimiterException(String iotId, Exception e) {
    long now = System.currentTimeMillis();
    long last = lastLimiterWarnTime.get();
    if (now - last > LIMITER_WARN_INTERVAL_MS && lastLimiterWarnTime.compareAndSet(last, now)) {
      log.warn(
          "设备在线时间刷新限频失败，降级为本地批量刷新，iotId={}, error={}",
          iotId,
          e.getMessage());
    }
  }

  private int normalizeFlushBatchSize() {
    return onlineFlushBatchSize == null || onlineFlushBatchSize <= 0 ? 200 : onlineFlushBatchSize;
  }

  private long normalizeFlushIntervalMs() {
    return onlineFlushIntervalMs == null || onlineFlushIntervalMs <= 0
        ? 12 * 1000L
        : onlineFlushIntervalMs;
  }

  private long normalizeRefreshIntervalSeconds() {
    return onlineRefreshIntervalSeconds == null ? 60L : onlineRefreshIntervalSeconds;
  }

  private int randomJitter() {
    return RandomUtil.randomInt(-RANDOM_JITTER_RANGE, RANDOM_JITTER_RANGE);
  }

  public void flushLogBatch(Set<String> iotIds) {
    if (iotIds == null || iotIds.isEmpty()) {
      return;
    }
    try {
      ioTDeviceMapper.batchFlushLog(iotIds);
      log.debug("批量更新最后通信时间完成，count={}", iotIds.size());
    } catch (Exception e) {
      log.error("批量更新设备最后一次通信时间异常={}", e);
    }
  }

  private void doOnline() {
    // log.debug("开始进行设备最后在线时间处理");
    try {
      Set<String> snapshot;
      synchronized (devBoSet) {
        if (devBoSet.isEmpty()) {
          return;
        }
        snapshot = new HashSet<>(devBoSet);
        devBoSet.clear();
      }
      flushLogBatch(snapshot);
    } catch (Exception e) {
      log.error("更新设备最后一次通信时间进程异常={}", e);
    }
  }

  /** 根据产品配置获取对应的日志服务 */
  public IIoTDeviceLogService getLogService(IoTProduct product) {
    return resolveLogService(product == null ? null : product.getStorePolicy());
  }

  private IIoTDeviceLogService resolveLogService(String storePolicy) {
    return policies.getOrDefault(resolvePolicyKey(storePolicy), policies.get("none"));
  }

  private String resolvePolicyKey(String storePolicy) {
    return StrUtil.blankToDefault(storePolicy, "none").trim().toLowerCase(Locale.ROOT);
  }

  private PolicyRuntimeMetrics metricsFor(String policyKey) {
    return policyRuntimeMetrics.computeIfAbsent(resolvePolicyKey(policyKey), ignored -> new PolicyRuntimeMetrics());
  }

  private static class PolicyRuntimeMetrics {
    private final AtomicLong callCount = new AtomicLong();
    private final AtomicLong successCount = new AtomicLong();
    private final AtomicLong failureCount = new AtomicLong();
    private final AtomicLong totalDurationMs = new AtomicLong();
    private final AtomicLong maxDurationMs = new AtomicLong();
    private final AtomicLong lastDurationMs = new AtomicLong();
    private final AtomicLong lastCompletedAt = new AtomicLong();

    void record(long durationMs, boolean success) {
      long safeDuration = Math.max(0, durationMs);
      callCount.incrementAndGet();
      if (success) {
        successCount.incrementAndGet();
      } else {
        failureCount.incrementAndGet();
      }
      totalDurationMs.addAndGet(safeDuration);
      lastDurationMs.set(safeDuration);
      lastCompletedAt.set(System.currentTimeMillis());
      maxDurationMs.accumulateAndGet(safeDuration, Math::max);
    }

    Map<String, Object> snapshot() {
      long calls = callCount.get();
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("callCount", calls);
      result.put("successCount", successCount.get());
      result.put("failureCount", failureCount.get());
      result.put("lastDurationMs", lastDurationMs.get());
      result.put("maxDurationMs", maxDurationMs.get());
      result.put("avgDurationMs", calls == 0 ? 0 : totalDurationMs.get() / calls);
      result.put("lastCompletedAt", lastCompletedAt.get());
      return result;
    }
  }

  /** 通用设备日志查询接口 */
  public List<IoTDeviceLog> queryDeviceLogs(IoTProduct product, DeviceLogQuery query) {
    IIoTDeviceLogService logService = getLogService(product);
    return logService.queryDeviceLogs(query);
  }

  /** 查询最近的指令记录 */
  @Override
  public IoTDeviceLog getRecentCommand(
      IoTProduct product,
      String productKey,
      String deviceId,
      String commandId,
      String messageType,
      Long createTime,
      Integer commandStatus) {
    IIoTDeviceLogService logService = getLogService(product);
    return logService.getRecentCommand(
        productKey, deviceId, commandId, messageType, createTime, commandStatus);
  }
}
