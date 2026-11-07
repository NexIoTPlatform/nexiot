/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.dm.device.service.log;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.common.exception.IoTException;
import cn.universal.core.message.UPRequest;
import cn.universal.core.metadata.AbstractPropertyMetadata;
import cn.universal.core.metadata.DeviceMetadata;
import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.log.writer.DeviceLogEntityWrite;
import cn.universal.dm.device.service.log.writer.DeviceLogWriteRequest;
import cn.universal.dm.device.util.DistributedLockUtil;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.dto.LogStorePolicyDTO;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTDeviceLogMetadata;
import cn.universal.persistence.entity.IoTDeviceLogMetadata.IoTDeviceLogMetadataBuilder;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.entity.vo.PropertyTrendSampleVO;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.query.DeviceLogQuery;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import cn.universal.persistence.query.PropertyTrendQuery;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * ClickHouse 存储日志服务
 *
 * <p>使用 HikariCP + Spring JdbcTemplate 实现，提供高性能、稳定的数据库访问
 *
 * <p>通过配置文件动态加载
 *
 * @since 2025/9/30 16:10
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "clickhouse", name = "enable", havingValue = "true")
public class CkDeviceLogService extends AbstractIoTDeviceLogService {
  private String storePolicy = "clickhouse";

  @Value("${clickhouse.table.iot_device_log:iot_device_log}")
  private String devLogTableName;

  @Value("${clickhouse.table.iot_device_log_metadata:iot_device_log_metadata}")
  private String devLogMetaTableName;

  @Value(value = "${clickhouse.address:jdbc:clickhouse://127.0.0.1:8123/default}")
  private String address;

  @Value(value = "${clickhouse.username:default}")
  private String username;

  @Value(value = "${clickhouse.password:}")
  private String password;

  @Value(value = "${clickhouse.pool.max-size:10}")
  private int maxPoolSize;

  @Value(value = "${clickhouse.pool.min-idle:2}")
  private int minIdle;

  @Value(value = "${clickhouse.pool.connection-timeout:30000}")
  private long connectionTimeout;

  @Value(value = "${clickhouse.socketTimeout:6000}")
  private int socketTimeoutMs;

  @Value(value = "${clickhouse.query-timeout-seconds:30}")
  private int queryTimeoutSeconds;

  @Value(value = "${clickhouse.batch.size:1000}")
  private int batchSize;

  @Value(value = "${clickhouse.batch.enabled:true}")
  private boolean batchEnabled;

  @Value(value = "${clickhouse.batch.flush-interval-ms:200}")
  private long batchFlushIntervalMs;

  @Value(value = "${clickhouse.batch.queue-capacity:50000}")
  private int batchQueueCapacity;

  @Value(value = "${clickhouse.batch.shutdown-timeout-ms:10000}")
  private long batchShutdownTimeoutMs;

  @Value(value = "${clickhouse.batch.flush-all-timeout-ms:5000}")
  private long batchFlushAllTimeoutMs;

  @Value(value = "${clickhouse.pool.leak-detection-threshold:120000}")
  private long leakDetectionThresholdMs;

  @Value("${clickhouse.ttl.days:0}")
  private int ttlDays;

  @Resource private IoTDeviceService iotDeviceService;
  @Resource private DistributedLockUtil distributedLockUtil;

  private HikariDataSource dataSource;
  private JdbcTemplate jdbcTemplate;
  private JdbcTemplate historyJdbcTemplate;
  private ClickHouseHistoryOptimizer historyOptimizer;

  @Value("${clickhouse.history.auto-optimize:true}")
  private boolean historyAutoOptimize = true;

  @Value("${clickhouse.history.materialize:true}")
  private boolean historyMaterialize = true;

  @Value("${clickhouse.history.max-partition-bytes:536870912}")
  private long historyMaxPartitionBytes = 536870912L;

  @Value("${nexiot.log.history.query-timeout-seconds:8}")
  private int historyQueryTimeoutSeconds = 8;

  private BlockingQueue<IoTDeviceLog> deviceLogQueue;
  private BlockingQueue<IoTDeviceLogMetadata> deviceLogMetadataQueue;
  private ExecutorService deviceLogBatchExecutor;
  private ExecutorService deviceLogMetadataBatchExecutor;
  private final AtomicBoolean deviceLogBatcherRunning = new AtomicBoolean();
  private final AtomicBoolean deviceLogMetadataBatcherRunning = new AtomicBoolean();
  private final ReentrantLock deviceLogFlushLock = new ReentrantLock();
  private final ReentrantLock deviceLogMetadataFlushLock = new ReentrantLock();
  private final AtomicLong enqueuedDeviceLogCount = new AtomicLong();
  private final AtomicLong insertedDeviceLogCount = new AtomicLong();
  private final AtomicLong failedDeviceLogCount = new AtomicLong();
  private final AtomicLong queueFullFallbackCount = new AtomicLong();
  private final AtomicLong enqueuedDeviceLogMetadataCount = new AtomicLong();
  private final AtomicLong insertedDeviceLogMetadataCount = new AtomicLong();
  private final AtomicLong failedDeviceLogMetadataCount = new AtomicLong();
  private final AtomicLong metadataQueueFullFallbackCount = new AtomicLong();
  private final AtomicLong lastDeviceLogFlushRows = new AtomicLong();
  private final AtomicLong lastDeviceLogFlushDurationMs = new AtomicLong();
  private final AtomicLong maxDeviceLogFlushDurationMs = new AtomicLong();
  private final AtomicLong lastDeviceLogQueueDepthBeforeFlush = new AtomicLong();
  private final AtomicLong lastDeviceLogQueueLagMs = new AtomicLong();
  private final AtomicLong maxDeviceLogQueueLagMs = new AtomicLong();
  private final AtomicLong lastDeviceLogFlushAt = new AtomicLong();
  private final AtomicLong lastMetadataFlushRows = new AtomicLong();
  private final AtomicLong lastMetadataFlushDurationMs = new AtomicLong();
  private final AtomicLong maxMetadataFlushDurationMs = new AtomicLong();
  private final AtomicLong lastMetadataQueueDepthBeforeFlush = new AtomicLong();
  private final AtomicLong lastMetadataQueueLagMs = new AtomicLong();
  private final AtomicLong maxMetadataQueueLagMs = new AtomicLong();
  private final AtomicLong lastMetadataFlushAt = new AtomicLong();

  @PostConstruct
  public void initDb() {
    try {
      // 创建 HikariCP 数据源配置
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(address);
      config.setUsername(username);
      config.setPassword(password);
      config.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");

      // 连接池配置
      config.setMaximumPoolSize(maxPoolSize);
      config.setMinimumIdle(minIdle);
      config.setConnectionTimeout(connectionTimeout);
      config.setIdleTimeout(600000); // 10分钟空闲超时
      config.setMaxLifetime(1800000); // 30分钟最大生命周期
      config.setValidationTimeout(3000); // 3秒验证超时
      if (leakDetectionThresholdMs > 0) {
        config.setLeakDetectionThreshold(Math.max(2000, leakDetectionThresholdMs));
      }

      // 连接池名称
      config.setPoolName("ClickHouse-Pool");

      // ClickHouse 特定配置
      config.setConnectionTestQuery("SELECT 1");
      config.setAutoCommit(true);
      config.setRegisterMbeans(false); // 关闭JMX监控
      config.addDataSourceProperty("connection_timeout", safeConnectionTimeoutMs());
      config.addDataSourceProperty("socket_timeout", safeSocketTimeoutMs());
      config.addDataSourceProperty("max_execution_time", safeQueryTimeoutSeconds());
      config.addDataSourceProperty("socket_keepalive", true);

      // 创建数据源
      dataSource = new HikariDataSource(config);
      jdbcTemplate = new JdbcTemplate(dataSource);
      jdbcTemplate.setQueryTimeout(safeQueryTimeoutSeconds());
      historyJdbcTemplate = new JdbcTemplate(dataSource);
      historyJdbcTemplate.setQueryTimeout(Math.max(1, historyQueryTimeoutSeconds));

      // 测试连接
      jdbcTemplate.queryForObject("SELECT 1", Integer.class);

      log.info(
          "初始化 ClickHouse 数据源成功，连接池配置: maxSize={}, minIdle={}, batchSize={}, socketTimeoutMs={},"
              + " queryTimeoutSeconds={}",
          maxPoolSize,
          minIdle,
          batchSize,
          safeSocketTimeoutMs(),
          safeQueryTimeoutSeconds());

      // 自动创建表
      createTablesIfNotExist();
      log.info("ClickHouse 表结构检查/创建完成");
      startDeviceLogBatcher();
      if (historyAutoOptimize) {
        historyOptimizer =
            new ClickHouseHistoryOptimizer(
                historyJdbcTemplate,
                devLogMetaTableName,
                historyMaterialize,
                historyMaxPartitionBytes);
        historyOptimizer.start(
            () ->
                distributedLockUtil.tryLockAndExecute(
                    "history:ck:upgrade:"
                        + DigestUtil.sha256Hex(address + ":" + devLogMetaTableName),
                    0,
                    -1,
                    TimeUnit.SECONDS,
                    (Runnable) historyOptimizer::upgrade));
      }

    } catch (Exception e) {
      log.error("初始化 ClickHouse 数据源失败", e);
      throw new RuntimeException("初始化 ClickHouse 数据源失败", e);
    }
  }

  @PreDestroy
  public void destroy() {
    if (historyOptimizer != null) historyOptimizer.close();
    stopDeviceLogBatcher();
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
      log.info("ClickHouse 数据源已关闭");
    }
  }

  /** 自动创建表（如果不存在） */
  private void createTablesIfNotExist() {
    try {
      String ttlClause = buildTtlClause();
      // 创建设备日志主表
      String createDeviceLogTable =
          """
          CREATE TABLE IF NOT EXISTS %s (
            `id` Int64,
            `iot_id` String,
            `device_id` String,
            `product_key` String,
            `device_name` String,
            `message_type` String,
            `command_id` Nullable(String),
            `command_status` Nullable(Int32),
            `event` Nullable(String),
            `content` String,
            `point` Nullable(String),
            `create_time` DateTime64(3)
          ) ENGINE = MergeTree()
          PARTITION BY toYYYYMM(create_time)
          ORDER BY (iot_id, create_time)
          %s
          SETTINGS index_granularity = 8192
          """
              .formatted(devLogTableName, ttlClause);

      jdbcTemplate.execute(createDeviceLogTable);
      log.info("ClickHouse 表 {} 检查/创建成功", devLogTableName);

      // 创建设备元数据日志表
      String createMetadataTable =
          """
          CREATE TABLE IF NOT EXISTS %s (
            `id` Int64,
            `iot_id` String,
            `product_key` String,
            `device_name` String,
            `device_id` String,
            `message_type` String,
            `event` Nullable(String),
            `property` Nullable(String),
            `content` String,
            `ext1` Nullable(String),
            `ext2` Nullable(String),
            `ext3` Nullable(String),
            `create_time` DateTime64(3)
          ) ENGINE = MergeTree()
          PARTITION BY toYYYYMM(create_time)
          ORDER BY (iot_id, create_time)
          %s
          SETTINGS index_granularity = 8192
          """
              .formatted(devLogMetaTableName, ttlClause);

      jdbcTemplate.execute(createMetadataTable);
      log.info("ClickHouse 表 {} 检查/创建成功", devLogMetaTableName);

    } catch (Exception e) {
      log.error("ClickHouse 创建表失败", e);
      throw new RuntimeException("ClickHouse 创建表失败", e);
    }
  }

  @Override
  @Async("taskExecutor")
  public void saveDeviceLog(BaseUPRequest upRequest, IoTDeviceDTO noUse, IoTProduct ioTProduct) {
    saveDeviceLogDirect(upRequest, noUse, ioTProduct);
  }

  @Override
  public void saveDeviceLogDirect(
      BaseUPRequest upRequest, IoTDeviceDTO noUse, IoTProduct ioTProduct) {
    /** 产品数据存储策略，不为空则保存日志 */
    if (StrUtil.isNotBlank(ioTProduct.getStorePolicy())) {
      IoTDeviceDTO ioTDeviceDTO = resolveDevice(upRequest, noUse);
      try {
        if (ioTDeviceDTO == null) {
          log.warn(
              "ClickHouse保存设备日志跳过，设备不存在 productKey={}, deviceId={}",
              upRequest.getProductKey(),
              upRequest.getDeviceId());
          return;
        }
        IoTDeviceLog ioTDeviceLog = build(upRequest, ioTDeviceDTO);
        enqueueDeviceLog(ioTDeviceLog);
        log.debug("ClickHouse 设备日志已提交写入队列");
      } catch (Exception e) {
        log.error("保存设备日志报错", e);
      }
      String storePolicyConfiguration = ioTProduct.getStorePolicyConfiguration();
      try {
        if (StrUtil.isNotBlank(storePolicyConfiguration)) {
          LogStorePolicyDTO productLogStorePolicy = resolveLogStorePolicy(ioTProduct);
          saveLogStorePolicy(productLogStorePolicy, upRequest, ioTProduct);
        }
      } catch (Exception e) {
        log.error("保存设备属性扩展日志报错", e);
      }
    }
  }

  @Override
  public void flushDeviceLogRequests(List<DeviceLogWriteRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      return;
    }
    List<IoTDeviceLog> logs = new ArrayList<>(requests.size());
    List<DeviceLogWriteRequest> metadataRequests = new ArrayList<>(requests.size());
    for (DeviceLogWriteRequest request : requests) {
      if (request == null || request.upRequest() == null || request.ioTProduct() == null) {
        continue;
      }
      IoTProduct product = request.ioTProduct();
      if (StrUtil.isBlank(product.getStorePolicy())) {
        continue;
      }
      IoTDeviceDTO device = resolveDevice(request.upRequest(), request.ioTDeviceDTO());
      if (device == null) {
        log.warn(
            "ClickHouse批量保存设备日志跳过，设备不存在 productKey={}, deviceId={}",
            request.upRequest().getProductKey(),
            request.upRequest().getDeviceId());
        continue;
      }
      logs.add(build(request.upRequest(), device));
      metadataRequests.add(request);
    }
    insertDeviceLogs(logs);
    for (DeviceLogWriteRequest request : metadataRequests) {
      String storePolicyConfiguration = request.ioTProduct().getStorePolicyConfiguration();
      try {
        if (StrUtil.isNotBlank(storePolicyConfiguration)) {
          LogStorePolicyDTO productLogStorePolicy = resolveLogStorePolicy(request.ioTProduct());
          saveLogStorePolicy(productLogStorePolicy, request.upRequest(), request.ioTProduct());
        }
      } catch (Exception e) {
        log.error("保存设备属性扩展日志报错", e);
      }
    }
  }

  @Override
  public void saveDeviceLog(
      IoTDeviceLog ioTDeviceLog, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {
    /** 产品数据存储策略，不为空则保存日志 */
    if (StrUtil.isNotBlank(ioTProduct.getStorePolicy())) {
      try {
        enqueueDeviceLog(ensureLogIdentity(ioTDeviceLog));
        log.debug("ClickHouse 设备日志已提交写入队列");
      } catch (Exception e) {
        log.error("保存设备日志报错", e);
      }
    }
  }

  @Override
  public void flushDeviceLogEntities(List<DeviceLogEntityWrite> entities) {
    if (entities == null || entities.isEmpty()) {
      return;
    }
    List<IoTDeviceLog> logs = new ArrayList<>(entities.size());
    for (DeviceLogEntityWrite entity : entities) {
      if (entity == null || entity.ioTDeviceLog() == null || entity.ioTProduct() == null) {
        continue;
      }
      if (StrUtil.isBlank(entity.ioTProduct().getStorePolicy())) {
        continue;
      }
      logs.add(ensureLogIdentity(entity.ioTDeviceLog()));
    }
    insertDeviceLogs(logs);
  }

  /** 插入设备日志 */
  private void insertDeviceLog(IoTDeviceLog deviceLog) {
    insertDeviceLogs(List.of(deviceLog));
  }

  private void enqueueDeviceLog(IoTDeviceLog deviceLog) {
    IoTDeviceLog target = ensureLogIdentity(deviceLog);
    if (target == null) {
      return;
    }
    if (!batchEnabled || deviceLogQueue == null) {
      insertDeviceLog(target);
      return;
    }
    if (deviceLogQueue.offer(target)) {
      enqueuedDeviceLogCount.incrementAndGet();
      return;
    }
    queueFullFallbackCount.incrementAndGet();
    flushPendingDeviceLogs(batchSize);
    if (deviceLogQueue.offer(target)) {
      enqueuedDeviceLogCount.incrementAndGet();
      return;
    }
    try {
      if (deviceLogQueue.offer(
          target, Math.max(50L, batchFlushIntervalMs), TimeUnit.MILLISECONDS)) {
        enqueuedDeviceLogCount.incrementAndGet();
        return;
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
    log.warn(
        "ClickHouse 设备日志队列已满，退化为当前线程批量写入 queueCapacity={}, fallbackCount={}",
        batchQueueCapacity,
        queueFullFallbackCount.get());
    insertDeviceLog(target);
  }

  private void insertDeviceLogs(Collection<IoTDeviceLog> logs) {
    if (logs == null || logs.isEmpty()) {
      return;
    }
    String sql =
        "INSERT INTO "
            + devLogTableName
            + " (id, iot_id, device_id, product_key, device_name, message_type, "
            + "command_id, command_status, event, content, point, create_time) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    List<Object[]> batchArgs = new ArrayList<>(logs.size());
    for (IoTDeviceLog log : logs) {
      IoTDeviceLog target = ensureLogIdentity(log);
      if (target == null) {
        continue;
      }
      batchArgs.add(deviceLogArgs(target));
    }
    if (batchArgs.isEmpty()) {
      return;
    }
    try {
      debugPreparedBatchInsertSql(storePolicy, sql, batchArgs);
      int[] counts = jdbcTemplate.batchUpdate(sql, batchArgs);
      insertedDeviceLogCount.addAndGet(counts.length);
    } catch (Exception ex) {
      failedDeviceLogCount.addAndGet(batchArgs.size());
      throw ex;
    }
  }

  private void enqueueDeviceLogMetadata(IoTDeviceLogMetadata metadata) {
    IoTDeviceLogMetadata target = ensureMetadataIdentity(metadata);
    if (target == null) {
      return;
    }
    if (!batchEnabled || deviceLogMetadataQueue == null) {
      insertDeviceLogMetadata(List.of(target));
      return;
    }
    if (deviceLogMetadataQueue.offer(target)) {
      enqueuedDeviceLogMetadataCount.incrementAndGet();
      return;
    }
    metadataQueueFullFallbackCount.incrementAndGet();
    flushPendingDeviceLogMetadata(batchSize);
    if (deviceLogMetadataQueue.offer(target)) {
      enqueuedDeviceLogMetadataCount.incrementAndGet();
      return;
    }
    try {
      if (deviceLogMetadataQueue.offer(
          target, Math.max(50L, batchFlushIntervalMs), TimeUnit.MILLISECONDS)) {
        enqueuedDeviceLogMetadataCount.incrementAndGet();
        return;
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
    log.warn(
        "ClickHouse 设备元数据日志队列已满，退化为当前线程批量写入 queueCapacity={}, fallbackCount={}",
        batchQueueCapacity,
        metadataQueueFullFallbackCount.get());
    insertDeviceLogMetadata(List.of(target));
  }

  private void insertDeviceLogMetadata(Collection<IoTDeviceLogMetadata> metadataLogs) {
    if (metadataLogs == null || metadataLogs.isEmpty()) {
      return;
    }
    String sql =
        "INSERT INTO "
            + devLogMetaTableName
            + " (id, iot_id, product_key, device_name, device_id, message_type, "
            + "event, property, content, ext1, ext2, ext3, create_time) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    List<Object[]> batchArgs = new ArrayList<>(metadataLogs.size());
    for (IoTDeviceLogMetadata metadata : metadataLogs) {
      IoTDeviceLogMetadata target = ensureMetadataIdentity(metadata);
      if (target == null) {
        continue;
      }
      batchArgs.add(deviceLogMetadataArgs(target));
    }
    if (batchArgs.isEmpty()) {
      return;
    }
    try {
      debugPreparedBatchInsertSql(storePolicy, sql, batchArgs);
      int[] counts = jdbcTemplate.batchUpdate(sql, batchArgs);
      insertedDeviceLogMetadataCount.addAndGet(counts.length);
      log.debug("ClickHouse 批量插入元数据成功，数量={}", counts.length);
    } catch (Exception ex) {
      failedDeviceLogMetadataCount.addAndGet(batchArgs.size());
      throw ex;
    }
  }

  private Object[] deviceLogMetadataArgs(IoTDeviceLogMetadata metadata) {
    return new Object[] {
      metadata.getId(),
      metadata.getIotId(),
      metadata.getProductKey(),
      metadata.getDeviceName(),
      metadata.getDeviceId(),
      metadata.getMessageType(),
      metadata.getEvent(),
      metadata.getProperty(),
      metadata.getContent(),
      metadata.getExt1(),
      metadata.getExt2(),
      metadata.getExt3(),
      metadata.getCreateTime() != null
          ? Timestamp.valueOf(metadata.getCreateTime())
          : Timestamp.valueOf(LocalDateTime.now())
    };
  }

  private Object[] deviceLogArgs(IoTDeviceLog target) {
    return new Object[] {
      target.getId(),
      target.getIotId(),
      target.getDeviceId(),
      target.getProductKey(),
      target.getDeviceName(),
      target.getMessageType(),
      target.getCommandId(),
      target.getCommandStatus(),
      target.getEvent(),
      target.getContent(),
      target.getPoint(),
      target.getCreateTime() != null
          ? Timestamp.valueOf(target.getCreateTime())
          : Timestamp.valueOf(LocalDateTime.now())
    };
  }

  private IoTDeviceDTO resolveDevice(BaseUPRequest upRequest, IoTDeviceDTO providedDevice) {
    return resolveDevice(upRequest, providedDevice, iotDeviceService);
  }

  private IoTDeviceLogMetadata ensureMetadataIdentity(IoTDeviceLogMetadata metadata) {
    if (metadata == null) {
      return null;
    }
    if (metadata.getCreateTime() == null) {
      metadata.setCreateTime(LocalDateTime.now());
    }
    if (metadata.getId() == null || metadata.getId() <= 0) {
      metadata.setId(IdUtil.getSnowflake().nextId());
    }
    return metadata;
  }

  private void startDeviceLogBatcher() {
    if (!batchEnabled) {
      log.info("ClickHouse 设备日志批量写入未启用，使用同步写入");
      return;
    }
    int safeQueueCapacity = Math.max(1000, batchQueueCapacity);
    deviceLogQueue = new ArrayBlockingQueue<>(safeQueueCapacity);
    deviceLogMetadataQueue = new ArrayBlockingQueue<>(safeQueueCapacity);
    if (deviceLogBatcherRunning.compareAndSet(false, true)) {
      deviceLogBatchExecutor =
          Executors.newSingleThreadExecutor(
              task -> {
                Thread thread = new Thread(task, "clickhouse-device-log-batcher");
                thread.setDaemon(true);
                return thread;
              });
      deviceLogBatchExecutor.execute(this::runDeviceLogBatcher);
    }
    if (deviceLogMetadataBatcherRunning.compareAndSet(false, true)) {
      deviceLogMetadataBatchExecutor =
          Executors.newSingleThreadExecutor(
              task -> {
                Thread thread = new Thread(task, "clickhouse-device-log-metadata-batcher");
                thread.setDaemon(true);
                return thread;
              });
      deviceLogMetadataBatchExecutor.execute(this::runDeviceLogMetadataBatcher);
    }
    log.info(
        "ClickHouse 设备日志批量写入已启动，batchSize={}, flushIntervalMs={}, queueCapacity={},"
            + " batchers=device-log+metadata",
        batchSize,
        safeBatchFlushIntervalMs(),
        safeQueueCapacity);
  }

  private void stopDeviceLogBatcher() {
    deviceLogBatcherRunning.set(false);
    deviceLogMetadataBatcherRunning.set(false);
    shutdownBatchExecutor(deviceLogBatchExecutor);
    shutdownBatchExecutor(deviceLogMetadataBatchExecutor);
    flushAllPendingDeviceLogs(batchShutdownTimeoutMs);
    flushAllPendingDeviceLogMetadata(batchShutdownTimeoutMs);
  }

  private void runDeviceLogBatcher() {
    while (deviceLogBatcherRunning.get() || hasPendingDeviceLogs()) {
      try {
        flushPendingDeviceLogs(batchSize, true);
      } catch (Exception ex) {
        log.error("ClickHouse 设备日志批量写入失败", ex);
      }
    }
  }

  private void runDeviceLogMetadataBatcher() {
    while (deviceLogMetadataBatcherRunning.get() || hasPendingDeviceLogMetadata()) {
      try {
        flushPendingDeviceLogMetadata(batchSize, true);
      } catch (Exception ex) {
        log.error("ClickHouse 设备元数据日志批量写入失败", ex);
      }
    }
  }

  private void shutdownBatchExecutor(ExecutorService executor) {
    if (executor == null) {
      return;
    }
    executor.shutdown();
    try {
      if (!executor.awaitTermination(
          Math.max(1000L, batchShutdownTimeoutMs), TimeUnit.MILLISECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      executor.shutdownNow();
    }
  }

  private boolean hasPendingDeviceLogs() {
    return deviceLogQueue != null && !deviceLogQueue.isEmpty();
  }

  private boolean hasPendingDeviceLogMetadata() {
    return deviceLogMetadataQueue != null && !deviceLogMetadataQueue.isEmpty();
  }

  private void flushPendingDeviceLogs(int maxRows) {
    try {
      flushPendingDeviceLogs(maxRows, false);
    } catch (Exception ex) {
      log.error("ClickHouse 设备日志队列刷新失败", ex);
    }
  }

  private void flushPendingDeviceLogMetadata(int maxRows) {
    try {
      flushPendingDeviceLogMetadata(maxRows, false);
    } catch (Exception ex) {
      log.error("ClickHouse 设备元数据日志队列刷新失败", ex);
    }
  }

  private void flushAllPendingDeviceLogs(long timeoutMs) {
    long deadline = System.currentTimeMillis() + Math.max(100L, timeoutMs);
    while (hasPendingDeviceLogs() && System.currentTimeMillis() < deadline) {
      int before = deviceLogQueue == null ? 0 : deviceLogQueue.size();
      flushPendingDeviceLogs(safeBatchSize(), false);
      int after = deviceLogQueue == null ? 0 : deviceLogQueue.size();
      if (after >= before) {
        break;
      }
    }
    if (hasPendingDeviceLogs()) {
      log.warn(
          "ClickHouse 设备日志队列仍有待写入数据，queueDepth={}, timeoutMs={}",
          deviceLogQueue == null ? 0 : deviceLogQueue.size(),
          timeoutMs);
    }
  }

  private void flushAllPendingDeviceLogMetadata(long timeoutMs) {
    long deadline = System.currentTimeMillis() + Math.max(100L, timeoutMs);
    while (hasPendingDeviceLogMetadata() && System.currentTimeMillis() < deadline) {
      int before = deviceLogMetadataQueue == null ? 0 : deviceLogMetadataQueue.size();
      flushPendingDeviceLogMetadata(safeBatchSize(), false);
      int after = deviceLogMetadataQueue == null ? 0 : deviceLogMetadataQueue.size();
      if (after >= before) {
        break;
      }
    }
    if (hasPendingDeviceLogMetadata()) {
      log.warn(
          "ClickHouse 设备元数据日志队列仍有待写入数据，queueDepth={}, timeoutMs={}",
          deviceLogMetadataQueue == null ? 0 : deviceLogMetadataQueue.size(),
          timeoutMs);
    }
  }

  private void flushPendingDeviceLogs(int maxRows, boolean waitForFirst) {
    BlockingQueue<IoTDeviceLog> queue = deviceLogQueue;
    if (queue == null) {
      return;
    }
    deviceLogFlushLock.lock();
    try {
      long startedAt = System.currentTimeMillis();
      int queueDepthBeforeFlush = queue.size();
      long queueLagMs = oldestDeviceLogQueueLagMs(queue);
      List<IoTDeviceLog> batch = drainDeviceLogBatch(queue, maxRows, waitForFirst);
      if (!batch.isEmpty()) {
        insertDeviceLogs(batch);
        recordDeviceLogFlush(
            batch.size(),
            System.currentTimeMillis() - startedAt,
            queueDepthBeforeFlush,
            queueLagMs);
      }
    } finally {
      deviceLogFlushLock.unlock();
    }
  }

  private void flushPendingDeviceLogMetadata(int maxRows, boolean waitForFirst) {
    BlockingQueue<IoTDeviceLogMetadata> queue = deviceLogMetadataQueue;
    if (queue == null) {
      return;
    }
    deviceLogMetadataFlushLock.lock();
    try {
      long startedAt = System.currentTimeMillis();
      int queueDepthBeforeFlush = queue.size();
      long queueLagMs = oldestDeviceLogMetadataQueueLagMs(queue);
      List<IoTDeviceLogMetadata> batch = drainDeviceLogMetadataBatch(queue, maxRows, waitForFirst);
      if (!batch.isEmpty()) {
        insertDeviceLogMetadata(batch);
        recordMetadataFlush(
            batch.size(),
            System.currentTimeMillis() - startedAt,
            queueDepthBeforeFlush,
            queueLagMs);
      }
    } finally {
      deviceLogMetadataFlushLock.unlock();
    }
  }

  private List<IoTDeviceLog> drainDeviceLogBatch(
      BlockingQueue<IoTDeviceLog> queue, int maxRows, boolean waitForFirst) {
    int limit = maxRows <= 0 ? safeBatchSize() : Math.min(maxRows, safeBatchSize());
    List<IoTDeviceLog> batch = new ArrayList<>(Math.min(limit, safeBatchSize()));
    IoTDeviceLog first = pollFirstDeviceLog(queue, waitForFirst);
    if (first == null) {
      return batch;
    }
    batch.add(first);
    drainDeviceLogQueue(queue, batch, limit);
    if (waitForFirst && batch.size() < limit) {
      waitAndFillDeviceLogBatch(queue, batch, limit);
    }
    return batch;
  }

  private List<IoTDeviceLogMetadata> drainDeviceLogMetadataBatch(
      BlockingQueue<IoTDeviceLogMetadata> queue, int maxRows, boolean waitForFirst) {
    int limit = maxRows <= 0 ? safeBatchSize() : Math.min(maxRows, safeBatchSize());
    List<IoTDeviceLogMetadata> batch = new ArrayList<>(Math.min(limit, safeBatchSize()));
    IoTDeviceLogMetadata first = pollFirstDeviceLogMetadata(queue, waitForFirst);
    if (first == null) {
      return batch;
    }
    batch.add(first);
    drainDeviceLogMetadataQueue(queue, batch, limit);
    if (waitForFirst && batch.size() < limit) {
      waitAndFillDeviceLogMetadataBatch(queue, batch, limit);
    }
    return batch;
  }

  private IoTDeviceLog pollFirstDeviceLog(BlockingQueue<IoTDeviceLog> queue, boolean waitForFirst) {
    if (!waitForFirst) {
      return queue.poll();
    }
    try {
      return queue.poll(safeBatchFlushIntervalMs(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  private IoTDeviceLogMetadata pollFirstDeviceLogMetadata(
      BlockingQueue<IoTDeviceLogMetadata> queue, boolean waitForFirst) {
    if (!waitForFirst) {
      return queue.poll();
    }
    try {
      return queue.poll(safeBatchFlushIntervalMs(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  private void waitAndFillDeviceLogBatch(
      BlockingQueue<IoTDeviceLog> queue, List<IoTDeviceLog> batch, int limit) {
    long deadline = System.currentTimeMillis() + safeBatchFlushIntervalMs();
    while (batch.size() < limit) {
      drainDeviceLogQueue(queue, batch, limit);
      if (batch.size() >= limit) {
        return;
      }
      long remainingMs = deadline - System.currentTimeMillis();
      if (remainingMs <= 0) {
        return;
      }
      try {
        IoTDeviceLog next = queue.poll(remainingMs, TimeUnit.MILLISECONDS);
        if (next == null) {
          return;
        }
        batch.add(next);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  private void waitAndFillDeviceLogMetadataBatch(
      BlockingQueue<IoTDeviceLogMetadata> queue, List<IoTDeviceLogMetadata> batch, int limit) {
    long deadline = System.currentTimeMillis() + safeBatchFlushIntervalMs();
    while (batch.size() < limit) {
      drainDeviceLogMetadataQueue(queue, batch, limit);
      if (batch.size() >= limit) {
        return;
      }
      long remainingMs = deadline - System.currentTimeMillis();
      if (remainingMs <= 0) {
        return;
      }
      try {
        IoTDeviceLogMetadata next = queue.poll(remainingMs, TimeUnit.MILLISECONDS);
        if (next == null) {
          return;
        }
        batch.add(next);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  private void drainDeviceLogQueue(
      BlockingQueue<IoTDeviceLog> queue, List<IoTDeviceLog> batch, int limit) {
    if (batch.size() >= limit) {
      return;
    }
    int remaining = limit - batch.size();
    queue.drainTo(batch, remaining);
  }

  private void drainDeviceLogMetadataQueue(
      BlockingQueue<IoTDeviceLogMetadata> queue, List<IoTDeviceLogMetadata> batch, int limit) {
    if (batch.size() >= limit) {
      return;
    }
    int remaining = limit - batch.size();
    queue.drainTo(batch, remaining);
  }

  private int safeBatchSize() {
    return Math.max(1, batchSize);
  }

  private long safeBatchFlushIntervalMs() {
    return Math.max(10L, batchFlushIntervalMs);
  }

  private long safeConnectionTimeoutMs() {
    return Math.max(1000L, connectionTimeout);
  }

  private void recordDeviceLogFlush(
      int rows, long durationMs, int queueDepthBeforeFlush, long queueLagMs) {
    lastDeviceLogFlushRows.set(Math.max(0, rows));
    lastDeviceLogFlushDurationMs.set(Math.max(0L, durationMs));
    maxDeviceLogFlushDurationMs.updateAndGet(
        current -> Math.max(current, Math.max(0L, durationMs)));
    lastDeviceLogQueueDepthBeforeFlush.set(Math.max(0, queueDepthBeforeFlush));
    lastDeviceLogQueueLagMs.set(Math.max(0L, queueLagMs));
    maxDeviceLogQueueLagMs.updateAndGet(current -> Math.max(current, Math.max(0L, queueLagMs)));
    lastDeviceLogFlushAt.set(System.currentTimeMillis());
  }

  private void recordMetadataFlush(
      int rows, long durationMs, int queueDepthBeforeFlush, long queueLagMs) {
    lastMetadataFlushRows.set(Math.max(0, rows));
    lastMetadataFlushDurationMs.set(Math.max(0L, durationMs));
    maxMetadataFlushDurationMs.updateAndGet(current -> Math.max(current, Math.max(0L, durationMs)));
    lastMetadataQueueDepthBeforeFlush.set(Math.max(0, queueDepthBeforeFlush));
    lastMetadataQueueLagMs.set(Math.max(0L, queueLagMs));
    maxMetadataQueueLagMs.updateAndGet(current -> Math.max(current, Math.max(0L, queueLagMs)));
    lastMetadataFlushAt.set(System.currentTimeMillis());
  }

  private long oldestDeviceLogQueueLagMs(BlockingQueue<IoTDeviceLog> queue) {
    IoTDeviceLog head = queue == null ? null : queue.peek();
    return head == null ? 0L : lagMs(head.getCreateTime());
  }

  private long oldestDeviceLogMetadataQueueLagMs(BlockingQueue<IoTDeviceLogMetadata> queue) {
    IoTDeviceLogMetadata head = queue == null ? null : queue.peek();
    return head == null ? 0L : lagMs(head.getCreateTime());
  }

  private long lagMs(LocalDateTime createTime) {
    if (createTime == null) {
      return 0L;
    }
    return Math.max(0L, Duration.between(createTime, LocalDateTime.now()).toMillis());
  }

  private int safeSocketTimeoutMs() {
    return Math.max(1000, socketTimeoutMs);
  }

  private int safeQueryTimeoutSeconds() {
    return Math.max(1, queryTimeoutSeconds);
  }

  private void saveLogStorePolicy(
      LogStorePolicyDTO logStorePolicyDTO, UPRequest up, IoTProduct ioTProduct) {
    LogStorePolicyDTO effectivePolicy =
        logStorePolicyDTO == null ? LogStorePolicyDTO.builder().build() : logStorePolicyDTO;
    List<IoTDeviceLogMetadata> metadataLogs = new ArrayList<>();

    if (MessageType.PROPERTIES.equals(up.getMessageType())
        && up.getProperties() != null
        && !effectivePolicy.getProperties().isEmpty()) {
      DeviceMetadata deviceMetadata = getDeviceMetadata(ioTProduct.getMetadata());
      String dataJson = JSONUtil.toJsonStr(up.getData());
      up.getProperties()
          .forEach(
              (key, value) -> {
                if (effectivePolicy.getProperties().containsKey(key)) {
                  AbstractPropertyMetadata propertyOrNull = deviceMetadata.getPropertyOrNull(key);
                  IoTDevicePropertiesBO ioTDevicePropertiesBO = new IoTDevicePropertiesBO();
                  ioTDevicePropertiesBO.withValue(propertyOrNull, value);

                  IoTDeviceLogMetadataBuilder builder = builder(up, dataJson);
                  builder.id(IdUtil.getSnowflake().nextId());
                  builder.property(key);
                  builder.content(metadataValueText(value));
                  builder.ext1(ioTDevicePropertiesBO.getPropertyName());
                  builder.ext2(ioTDevicePropertiesBO.getFormatValue());
                  builder.ext3(ioTDevicePropertiesBO.getSymbol());

                  metadataLogs.add(ensureMetadataIdentity(builder.build()));
                }
              });
    }
    if (MessageType.EVENT.equals(up.getMessageType())) {
      IoTDeviceLogMetadataBuilder builder = builder(up);
      builder.id(IdUtil.getSnowflake().nextId());
      builder.event(up.getEvent());
      builder.content(up.getEventName());

      metadataLogs.add(ensureMetadataIdentity(builder.build()));
    }

    for (IoTDeviceLogMetadata metadata : metadataLogs) {
      enqueueDeviceLogMetadata(metadata);
    }
  }

  @Override
  public PageBean<IoTDeviceLogVO> pageList(LogQuery logQuery) {
    StringBuilder sql = new StringBuilder("SELECT * FROM " + devLogTableName + " WHERE 1=1");
    List<Object> params = new ArrayList<>();

    if (StrUtil.isNotBlank(logQuery.getProductKey())) {
      sql.append(" AND product_key = ?");
      params.add(logQuery.getProductKey());
    }
    if (StrUtil.isNotBlank(logQuery.getIotId())) {
      sql.append(" AND iot_id = ?");
      params.add(logQuery.getIotId());
    }
    if (StrUtil.isNotBlank(logQuery.getDeviceId())) {
      sql.append(" AND device_id = ?");
      params.add(logQuery.getDeviceId());
    }
    if (StrUtil.isNotBlank(logQuery.getMessageType())) {
      sql.append(" AND message_type = ?");
      params.add(logQuery.getMessageType());
    }
    if (StrUtil.isNotBlank(logQuery.getDeviceName())) {
      sql.append(" AND device_name = ?");
      params.add(logQuery.getDeviceName());
    }
    if (StrUtil.isNotBlank(logQuery.getEvent())) {
      sql.append(" AND event = ?");
      params.add(logQuery.getEvent());
    }
    if (MapUtil.isNotEmpty(logQuery.getParams())
        && ObjectUtil.isNotNull(logQuery.getParams().get("properties"))) {
      sql.append(" AND JSONHas(content, 'properties', '")
          .append(escapeSqlLiteral(String.valueOf(logQuery.getParams().get("properties"))))
          .append("') = 1");
    }
    if (MapUtil.isNotEmpty(logQuery.getParams())
        && ObjectUtil.isNotNull(logQuery.getParams().get("event"))) {
      sql.append(" AND event = ?");
      params.add(logQuery.getParams().get("event"));
    }
    // 添加时间查询条件 - 使用 logQuery 的字段，不从 params 获取
    if (logQuery.getBeginCreateTime() != null) {
      sql.append(" AND create_time >= ?");
      params.add(new Timestamp(logQuery.getBeginCreateTime() * 1000));
    }
    if (logQuery.getEndCreateTime() != null) {
      sql.append(" AND create_time <= ?");
      params.add(new Timestamp(logQuery.getEndCreateTime() * 1000));
    }

    if (StrUtil.isNotBlank(logQuery.getId())) {
      sql.append(" AND id = ?");
      params.add(Long.parseLong(logQuery.getId()));
    }
    if (logQuery.isCursorMode()) {
      appendHistoryCursor(sql, params, logQuery);
      sql.append(" ORDER BY create_time DESC, id DESC LIMIT ?");
      params.add(logQuery.getFetchSize());
      return HistoryPageSupport.page(
          historyJdbc().query(sql.toString(), new DeviceLogRowMapper(), params.toArray()),
          logQuery,
          IoTDeviceLogVO::getCreateTime,
          IoTDeviceLogVO::getId);
    }
    sql.append(" ORDER BY create_time DESC, id DESC");

    try {
      // 查询总数
      String countSql = "SELECT COUNT(*) FROM (" + sql + ") AS total_logs";
      Long total = historyJdbc().queryForObject(countSql, Long.class, params.toArray());

      // 分页查询
      String pageSql = sql + " LIMIT ? OFFSET ?";
      List<Object> pageParams = new ArrayList<>(params);
      int offset = (logQuery.getPageNum() - 1) * logQuery.getPageSize();
      pageParams.add(logQuery.getPageSize());
      pageParams.add(offset);

      List<IoTDeviceLogVO> list =
          historyJdbc().query(pageSql, new DeviceLogRowMapper(), pageParams.toArray());

      return new PageBean<>(
          list, total != null ? total : 0L, logQuery.getPageSize(), logQuery.getPageNum());

    } catch (Exception e) {
      log.warn("ClickHouse 查询日志错误", e);
      throw new IoTException("设备日志查询失败，请缩小时间范围或稍后重试", e);
    }
  }

  @Override
  public IoTDeviceLogVO queryById(LogQuery logQuery) {
    StringBuilder sql = new StringBuilder("SELECT * FROM " + devLogTableName + " WHERE id = ?");
    List<Object> params = new ArrayList<>();
    params.add(logQuery.getId());
    if (StrUtil.isNotBlank(logQuery.getProductKey())) {
      sql.append(" AND product_key = ?");
      params.add(logQuery.getProductKey());
    }
    if (StrUtil.isNotBlank(logQuery.getDeviceId())) {
      sql.append(" AND device_id = ?");
      params.add(logQuery.getDeviceId());
    }
    if (StrUtil.isNotBlank(logQuery.getIotId())) {
      sql.append(" AND iot_id = ?");
      params.add(logQuery.getIotId());
    }
    sql.append(" ORDER BY create_time DESC LIMIT 1");
    try {
      return historyJdbc()
          .queryForObject(sql.toString(), new DeviceLogRowMapper(), params.toArray());
    } catch (Exception e) {
      log.warn("ClickHouse 根据ID查询日志错误", e);
      return null;
    }
  }

  @Override
  public PageBean<IoTDeviceEvents> queryEventTotal(String productKey, String iotId) {
    List<IoTDeviceEvents> list = selectDevEvents(productKey);
    long deadline =
        System.nanoTime() + TimeUnit.SECONDS.toNanos(Math.max(1, historyQueryTimeoutSeconds));
    for (IoTDeviceEvents devEvent : list) {
      if (System.nanoTime() >= deadline) throw new IoTException("事件统计查询超时，请稍后重试", 503);
      String sql =
          "SELECT create_time "
              + "FROM "
              + devLogTableName
              + " WHERE event = ? AND message_type = 'EVENT' AND iot_id = ? AND product_key = ? "
              + "ORDER BY create_time DESC LIMIT 100";
      try {
        List<Map<String, Object>> rows =
            historyJdbc().queryForList(sql, devEvent.getId(), iotId, productKey);
        int qty = rows.size();
        devEvent.setQty(qty >= 100 ? "99+" : String.valueOf(qty));
        Object latest = rows.isEmpty() ? null : rows.getFirst().get("create_time");
        devEvent.setTime(latest == null ? null : latest.toString());
      } catch (Exception e) {
        log.warn("ClickHouse 查询事件统计错误", e);
        throw new IoTException("事件统计查询失败，请稍后重试", 503, e);
      }
    }
    return new PageBean<>(list, (long) list.size(), 1, 100);
  }

  @Override
  public PageBean<IoTDeviceLogMetadataVO> queryLogMeta(LogQuery logQuery) {
    StringBuilder sql = new StringBuilder("SELECT * FROM " + devLogMetaTableName + " WHERE 1=1");
    List<Object> params = appendMetadataFilters(sql, logQuery);
    if (logQuery.isCursorMode()) {
      appendHistoryCursor(sql, params, logQuery);
      sql.append(" ORDER BY create_time DESC, id DESC LIMIT ?");
      params.add(logQuery.getFetchSize());
      return HistoryPageSupport.page(
          historyJdbc().query(sql.toString(), new DeviceLogMetadataRowMapper(), params.toArray()),
          logQuery,
          IoTDeviceLogMetadataVO::getCreateTime,
          IoTDeviceLogMetadataVO::getId);
    }
    try {
      // 查询总数
      String countSql = "SELECT COUNT(*) FROM (" + sql + ") AS total_meta";
      Long total = historyJdbc().queryForObject(countSql, Long.class, params.toArray());

      // 分页查询
      String pageSql = sql + " ORDER BY create_time DESC, id DESC LIMIT ? OFFSET ?";
      List<Object> pageParams = new ArrayList<>(params);
      int offset = (logQuery.getPageNum() - 1) * logQuery.getPageSize();
      pageParams.add(logQuery.getPageSize());
      pageParams.add(offset);

      List<IoTDeviceLogMetadataVO> list =
          historyJdbc().query(pageSql, new DeviceLogMetadataRowMapper(), pageParams.toArray());

      return new PageBean<>(
          list, total != null ? total : 0L, logQuery.getPageSize(), logQuery.getPageNum());

    } catch (Exception e) {
      log.warn("ClickHouse 查询元数据日志错误", e);
      throw new IoTException("历史数据查询失败，请稍后重试或缩小时间范围", e);
    }
  }

  private void appendHistoryCursor(StringBuilder sql, List<Object> params, LogQuery query) {
    if (query.getBeforeTime() == null) return;
    sql.append(" AND (create_time, id) < (?, ?)");
    params.add(Timestamp.valueOf(query.getBeforeTime()));
    params.add(query.getBeforeId());
  }

  private JdbcTemplate historyJdbc() {
    if (historyJdbcTemplate != null) return historyJdbcTemplate;
    return jdbcTemplate;
  }

  private List<Object> appendMetadataFilters(StringBuilder sql, LogQuery logQuery) {
    List<Object> params = new ArrayList<>();

    if (StrUtil.isNotBlank(logQuery.getProductKey())) {
      sql.append(" AND product_key = ?");
      params.add(logQuery.getProductKey());
    }
    if (StrUtil.isNotBlank(logQuery.getIotId())) {
      sql.append(" AND iot_id = ?");
      params.add(logQuery.getIotId());
    }
    if (StrUtil.isNotBlank(logQuery.getDeviceId())) {
      sql.append(" AND device_id = ?");
      params.add(logQuery.getDeviceId());
    }
    if (StrUtil.isNotBlank(logQuery.getProperty())) {
      sql.append(" AND property = ?");
      params.add(logQuery.getProperty());
    }
    if (StrUtil.isNotBlank(logQuery.getEvent())) {
      sql.append(" AND event = ?");
      params.add(logQuery.getEvent());
    }
    if (StrUtil.isNotBlank(logQuery.getMessageType())) {
      sql.append(" AND message_type = ?");
      params.add(logQuery.getMessageType());
    }
    // 添加时间查询条件 - ClickHouse DateTime64 类型直接使用 Timestamp
    if (logQuery.getBeginCreateTime() != null) {
      sql.append(" AND create_time >= ?");
      params.add(new Timestamp(logQuery.getBeginCreateTime() * 1000));
    }
    if (logQuery.getEndCreateTime() != null) {
      sql.append(" AND create_time <= ?");
      params.add(new Timestamp(logQuery.getEndCreateTime() * 1000));
    }
    return params;
  }

  @Override
  public PropertyTrendVO queryPropertyTrend(PropertyTrendQuery query) {
    query.validateRange();
    if (query.isSampledQuery()) {
      return querySampledTrend(query);
    }
    if (historyOptimizer != null
        && historyOptimizer.projectionReady()
        && query.getBeginCreateTime() % 60 == 0
        && query.getEndCreateTime() % 60 == 0
        && query.getIntervalSeconds() >= 60
        && StrUtil.isBlank(query.getEvent())) {
      return queryMinuteTrend(query);
    }
    String numeric =
        historyOptimizer != null && historyOptimizer.numericReady()
            ? "numeric_value"
            : ClickHouseHistoryOptimizer.NUMERIC_EXPRESSION;
    StringBuilder source =
        new StringBuilder(
            "SELECT create_time, "
                + numeric
                + " AS numeric_value FROM "
                + devLogMetaTableName
                + " WHERE 1=1");
    List<Object> filters = appendMetadataFilters(source, query);
    String sql =
        "SELECT (? + bucket * ?) * 1000 AS time, count() AS count, count(numeric_value) AS"
            + " numeric_count, minOrNull(numeric_value) AS min, maxOrNull(numeric_value) AS max,"
            + " avgOrNull(numeric_value) AS avg FROM (SELECT floor(dateDiff('millisecond',"
            + " toDateTime64(?, 3), create_time) / (1000 * ?)) AS bucket, numeric_value FROM ("
            + source
            + ")) GROUP BY bucket ORDER BY bucket";
    List<Object> params = new ArrayList<>();
    params.add(query.getBeginCreateTime());
    params.add(query.getIntervalSeconds());
    // 与列表查询使用同一个 JDBC 时间基准，避免数据库时区导致趋势偏移。
    params.add(new Timestamp(query.getBeginCreateTime() * 1000));
    params.add(query.getIntervalSeconds());
    params.addAll(filters);
    List<PropertyTrendVO.Bucket> buckets =
        historyJdbc()
            .query(
                sql,
                (rs, rowNum) -> {
                  PropertyTrendVO.Bucket bucket = new PropertyTrendVO.Bucket();
                  bucket.setTime(rs.getLong("time"));
                  bucket.setCount(rs.getLong("count"));
                  bucket.setNumericCount(rs.getLong("numeric_count"));
                  bucket.setMin(rs.getBigDecimal("min"));
                  bucket.setMax(rs.getBigDecimal("max"));
                  bucket.setAvg(rs.getBigDecimal("avg"));
                  return bucket;
                },
                params.toArray());
    return PropertyTrendVO.of(query, buckets);
  }

  private PropertyTrendVO querySampledTrend(PropertyTrendQuery query) {
    List<PropertyTrendQuery.SampleWindow> ranges = query.getSampleWindows();
    List<PropertyTrendSampleVO> samples = new ArrayList<>();
    long deadline =
        System.nanoTime() + TimeUnit.SECONDS.toNanos(Math.max(1, historyQueryTimeoutSeconds));
    try {
      // Hundreds of UNION branches retain substantial query-plan memory even for empty windows.
      // Bound each plan while all batches share one request deadline and the same sample budget.
      for (int begin = 0; begin < ranges.size(); begin += 100) {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) throw new IoTException("ClickHouse趋势采样超时", 503);
        List<String> windows = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (PropertyTrendQuery.SampleWindow window :
            ranges.subList(begin, Math.min(begin + 100, ranges.size()))) {
          StringBuilder source =
              new StringBuilder("SELECT content FROM " + devLogMetaTableName + " WHERE 1=1");
          params.addAll(appendMetadataFilters(source, query));
          source.append(" AND create_time >= ? AND create_time < ?");
          params.add(new Timestamp(window.getBeginMillis()));
          params.add(new Timestamp(window.getEndExclusiveMillis()));
          // LIMIT belongs to the indexed time seek, before any numeric parsing or aggregate.
          source.append(" ORDER BY create_time DESC LIMIT ").append(query.getSampleFetchSize());
          windows.add(
              "SELECT "
                  + window.getIndex()
                  + " AS window_index, if(length(content)<=1024, content, NULL) AS content FROM ("
                  + source
                  + ")");
        }
        String sql =
            String.join(" UNION ALL ", windows)
                + " SETTINGS optimize_read_in_order=1, max_threads=2, max_execution_time="
                + ((remaining + TimeUnit.SECONDS.toNanos(1) - 1) / TimeUnit.SECONDS.toNanos(1));
        samples.addAll(
            historyJdbc()
                .query(
                    sql,
                    (rs, rowNum) ->
                        new PropertyTrendSampleVO(
                            rs.getInt("window_index"), rs.getString("content")),
                    params.toArray()));
        if (System.nanoTime() >= deadline) throw new IoTException("ClickHouse趋势采样超时", 503);
      }
      return TrendSamplingSupport.aggregate(query, samples);
    } catch (RuntimeException failure) {
      log.warn(
          "ClickHouse趋势采样失败 iotId={}, property={}", query.getIotId(), query.getProperty(), failure);
      throw new IoTException("ClickHouse趋势查询失败或超时，请缩小时间范围后重试", 503, failure);
    }
  }

  private PropertyTrendVO queryMinuteTrend(PropertyTrendQuery query) {
    String filters = " product_key=? AND iot_id=? AND property=? AND message_type='PROPERTIES'";
    List<Object> key =
        new ArrayList<>(List.of(query.getProductKey(), query.getIotId(), query.getProperty()));
    if (StrUtil.isNotBlank(query.getDeviceId())) {
      filters += " AND device_id=?";
      key.add(query.getDeviceId());
    }
    String aggregate =
        "SELECT toStartOfMinute(create_time) AS minute, count() AS samples, count(numeric_value) AS"
            + " numbers, min(numeric_value) AS low, max(numeric_value) AS high, sum(numeric_value)"
            + " AS total FROM "
            + devLogMetaTableName
            + " WHERE "
            + filters;
    // Full minutes use the aggregate projection. The inclusive end instant is read from raw data.
    String source =
        aggregate
            + " AND toStartOfMinute(create_time)>=toDateTime64(?,3)"
            + " AND toStartOfMinute(create_time)<toDateTime64(?,3) GROUP BY minute UNION ALL "
            + aggregate
            + " AND create_time=toDateTime64(?,3) GROUP BY minute";
    String sql =
        "SELECT (? + bucket * ?) * 1000 AS time, sum(samples) AS count, sum(numbers) AS"
            + " numeric_count, min(low) AS min, max(high) AS max, sum(total)/nullIf(sum(numbers),0)"
            + " AS avg FROM (SELECT floor(dateDiff('second',toDateTime64(?,3),minute)/?) AS bucket,"
            + " * FROM ("
            + source
            + ")) GROUP BY bucket ORDER BY bucket";
    List<Object> params =
        new ArrayList<>(
            List.of(
                query.getBeginCreateTime(),
                query.getIntervalSeconds(),
                new Timestamp(query.getBeginCreateTime() * 1000),
                query.getIntervalSeconds()));
    params.addAll(key);
    params.add(new Timestamp(query.getBeginCreateTime() * 1000));
    params.add(new Timestamp(query.getEndCreateTime() * 1000));
    params.addAll(key);
    params.add(new Timestamp(query.getEndCreateTime() * 1000));
    return PropertyTrendVO.of(
        query,
        historyJdbc()
            .query(
                sql,
                (rs, row) -> {
                  PropertyTrendVO.Bucket bucket = new PropertyTrendVO.Bucket();
                  bucket.setTime(rs.getLong("time"));
                  bucket.setCount(rs.getLong("count"));
                  bucket.setNumericCount(rs.getLong("numeric_count"));
                  bucket.setMin(rs.getBigDecimal("min"));
                  bucket.setMax(rs.getBigDecimal("max"));
                  bucket.setAvg(rs.getBigDecimal("avg"));
                  return bucket;
                },
                params.toArray()));
  }

  @Override
  public String getPolicy() {
    return storePolicy;
  }

  @Override
  public JSONObject configMetadata() {
    JSONObject config = new JSONObject();
    config.put(
        "historyOptimization",
        historyOptimizer == null ? Map.of("status", "disabled") : historyOptimizer.metrics());
    config.put("address", address);
    config.put("database", extractDatabaseName());
    config.put("ttlDays", ttlDays);
    config.put("poolMaxSize", maxPoolSize);
    config.put("poolMinIdle", minIdle);
    config.put("poolActiveConnections", activeClickHouseConnections());
    config.put("poolIdleConnections", idleClickHouseConnections());
    config.put("poolAwaitingThreads", awaitingClickHouseConnectionThreads());
    config.put("socketTimeoutMs", safeSocketTimeoutMs());
    config.put("queryTimeoutSeconds", safeQueryTimeoutSeconds());
    config.put("batchEnabled", batchEnabled);
    config.put("batchSize", safeBatchSize());
    config.put("batchFlushIntervalMs", safeBatchFlushIntervalMs());
    config.put("batchFlushAllTimeoutMs", batchFlushAllTimeoutMs);
    config.put("batchQueueCapacity", batchQueueCapacity);
    config.put("deviceLogBatcherRunning", deviceLogBatcherRunning.get());
    config.put("metadataBatcherRunning", deviceLogMetadataBatcherRunning.get());
    config.put("batchQueueDepth", deviceLogQueue == null ? 0 : deviceLogQueue.size());
    config.put(
        "metadataBatchQueueDepth",
        deviceLogMetadataQueue == null ? 0 : deviceLogMetadataQueue.size());
    config.put("oldestDeviceLogQueueLagMs", oldestDeviceLogQueueLagMs(deviceLogQueue));
    config.put(
        "oldestMetadataQueueLagMs", oldestDeviceLogMetadataQueueLagMs(deviceLogMetadataQueue));
    config.put("enqueuedDeviceLogCount", enqueuedDeviceLogCount.get());
    config.put("insertedDeviceLogCount", insertedDeviceLogCount.get());
    config.put("failedDeviceLogCount", failedDeviceLogCount.get());
    config.put("queueFullFallbackCount", queueFullFallbackCount.get());
    config.put("enqueuedDeviceLogMetadataCount", enqueuedDeviceLogMetadataCount.get());
    config.put("insertedDeviceLogMetadataCount", insertedDeviceLogMetadataCount.get());
    config.put("failedDeviceLogMetadataCount", failedDeviceLogMetadataCount.get());
    config.put("metadataQueueFullFallbackCount", metadataQueueFullFallbackCount.get());
    config.put("lastDeviceLogFlushRows", lastDeviceLogFlushRows.get());
    config.put("lastDeviceLogFlushDurationMs", lastDeviceLogFlushDurationMs.get());
    config.put("maxDeviceLogFlushDurationMs", maxDeviceLogFlushDurationMs.get());
    config.put("lastDeviceLogQueueDepthBeforeFlush", lastDeviceLogQueueDepthBeforeFlush.get());
    config.put("lastDeviceLogQueueLagMs", lastDeviceLogQueueLagMs.get());
    config.put("maxDeviceLogQueueLagMs", maxDeviceLogQueueLagMs.get());
    config.put("lastDeviceLogFlushAt", lastDeviceLogFlushAt.get());
    config.put("lastMetadataFlushRows", lastMetadataFlushRows.get());
    config.put("lastMetadataFlushDurationMs", lastMetadataFlushDurationMs.get());
    config.put("maxMetadataFlushDurationMs", maxMetadataFlushDurationMs.get());
    config.put("lastMetadataQueueDepthBeforeFlush", lastMetadataQueueDepthBeforeFlush.get());
    config.put("lastMetadataQueueLagMs", lastMetadataQueueLagMs.get());
    config.put("maxMetadataQueueLagMs", maxMetadataQueueLagMs.get());
    config.put("lastMetadataFlushAt", lastMetadataFlushAt.get());
    return config;
  }

  private int activeClickHouseConnections() {
    return dataSource != null && dataSource.getHikariPoolMXBean() != null
        ? dataSource.getHikariPoolMXBean().getActiveConnections()
        : 0;
  }

  private int idleClickHouseConnections() {
    return dataSource != null && dataSource.getHikariPoolMXBean() != null
        ? dataSource.getHikariPoolMXBean().getIdleConnections()
        : 0;
  }

  private int awaitingClickHouseConnectionThreads() {
    return dataSource != null && dataSource.getHikariPoolMXBean() != null
        ? dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        : 0;
  }

  /** 查询最近的指令记录 */
  @Override
  public IoTDeviceLog getRecentCommand(
      String productKey,
      String deviceId,
      String commandId,
      String messageType,
      Long createTime,
      Integer commandStatus) {
    flushAllPendingDeviceLogs(batchFlushAllTimeoutMs);
    try {
      StringBuilder sql =
          new StringBuilder(
              "SELECT * FROM "
                  + devLogTableName
                  + " WHERE product_key=? AND device_id=? AND message_type=? AND command_id=? AND"
                  + " command_status=?");
      List<Object> params = new ArrayList<>();
      params.add(productKey);
      params.add(deviceId);
      params.add(messageType);
      params.add(commandId);
      params.add(commandStatus != null ? commandStatus : 0);
      if (createTime != null) {
        sql.append(" AND create_time >= ?");
        params.add(new Timestamp(resolveEpochMillis(createTime)));
      }
      sql.append(" ORDER BY create_time DESC, id DESC LIMIT 1");

      List<IoTDeviceLog> logs =
          jdbcTemplate.query(sql.toString(), params.toArray(), new IoTDeviceLogRowMapper());
      return logs.isEmpty() ? null : logs.get(0);
    } catch (Exception e) {
      log.error("ClickHouse查询指令记录异常: {}", e.getMessage(), e);
      return null;
    }
  }

  /** 通用设备日志查询接口 */
  @Override
  public List<IoTDeviceLog> queryDeviceLogs(DeviceLogQuery query) {
    try {
      StringBuilder sqlBuilder = new StringBuilder();
      sqlBuilder.append("SELECT * FROM ").append(devLogTableName);

      // 构造WHERE条件
      List<String> conditions = new ArrayList<>();
      List<Object> params = new ArrayList<>();

      if (query.getIotId() != null) {
        conditions.add("iot_id=?");
        params.add(query.getIotId());
      }

      if (query.getProductKey() != null) {
        conditions.add("product_key=?");
        params.add(query.getProductKey());
      }

      if (query.getDeviceId() != null) {
        conditions.add("device_id=?");
        params.add(query.getDeviceId());
      }

      if (query.getMessageType() != null) {
        conditions.add("message_type=?");
        params.add(query.getMessageType());
      }

      if (query.getLogId() != null) {
        conditions.add("id=?");
        params.add(query.getLogId());
      }

      if (query.getCommandId() != null) {
        conditions.add("command_id=?");
        params.add(query.getCommandId());
      }

      if (query.getCommandStatus() != null) {
        conditions.add("command_status=?");
        params.add(query.getCommandStatus());
      }

      if (query.getStartTime() != null) {
        conditions.add("create_time >= ?");
        params.add(new Timestamp(resolveEpochMillis(query.getStartTime())));
      }

      if (query.getEndTime() != null) {
        conditions.add("create_time <= ?");
        params.add(new Timestamp(resolveEpochMillis(query.getEndTime())));
      }

      if (query.getEventType() != null) {
        conditions.add("event=?");
        params.add(query.getEventType());
      }

      // 添加WHERE子句
      if (!conditions.isEmpty()) {
        sqlBuilder.append(" WHERE ").append(String.join(" AND ", conditions));
      }

      sqlBuilder.append(" ORDER BY create_time DESC, id DESC");

      // 添加LIMIT子句
      if (query.getLimit() > 0) {
        sqlBuilder.append(" LIMIT ").append(query.getLimit());
      }

      return jdbcTemplate.query(
          sqlBuilder.toString(), params.toArray(), new IoTDeviceLogRowMapper());
    } catch (Exception e) {
      log.error("ClickHouse通用查询异常: {}", e.getMessage(), e);
      return Collections.emptyList();
    }
  }

  /** IoTDeviceLog结果集映射器 */
  private static class IoTDeviceLogRowMapper implements RowMapper<IoTDeviceLog> {
    @Override
    public IoTDeviceLog mapRow(ResultSet rs, int rowNum) throws SQLException {
      IoTDeviceLog log = new IoTDeviceLog();
      log.setId(rs.getLong("id"));
      log.setIotId(rs.getString("iot_id"));
      log.setDeviceId(rs.getString("device_id"));
      log.setProductKey(rs.getString("product_key"));
      log.setDeviceName(rs.getString("device_name"));
      log.setMessageType(rs.getString("message_type"));
      log.setCommandId(rs.getString("command_id"));
      log.setCommandStatus(
          rs.getObject("command_status") != null ? rs.getInt("command_status") : null);
      log.setEvent(rs.getString("event"));
      log.setContent(rs.getString("content"));
      log.setPoint(rs.getString("point"));

      // 处理时间戳
      Timestamp createTime = rs.getTimestamp("create_time");
      if (createTime != null) {
        log.setCreateTime(createTime.toLocalDateTime());
      }

      return log;
    }
  }

  /** 设备日志行映射器 */
  private static class DeviceLogRowMapper implements RowMapper<IoTDeviceLogVO> {
    @Override
    public IoTDeviceLogVO mapRow(ResultSet rs, int rowNum) throws SQLException {
      IoTDeviceLogVO vo = new IoTDeviceLogVO();
      vo.setId(rs.getLong("id"));
      vo.setIotId(rs.getString("iot_id"));
      vo.setDeviceId(rs.getString("device_id"));
      vo.setProductKey(rs.getString("product_key"));
      vo.setDeviceName(rs.getString("device_name"));
      vo.setMessageType(rs.getString("message_type"));
      vo.setCommandId(rs.getString("command_id"));
      vo.setCommandStatus(
          rs.getObject("command_status") != null ? rs.getInt("command_status") : null);
      vo.setEvent(rs.getString("event"));
      vo.setContent(rs.getString("content"));
      vo.setPoint(rs.getString("point"));
      Timestamp createTime = rs.getTimestamp("create_time");
      if (createTime != null) {
        vo.setCreateTime(createTime.toLocalDateTime());
      }
      return vo;
    }
  }

  /** 设备日志元数据行映射器 */
  private static class DeviceLogMetadataRowMapper implements RowMapper<IoTDeviceLogMetadataVO> {
    @Override
    public IoTDeviceLogMetadataVO mapRow(ResultSet rs, int rowNum) throws SQLException {
      IoTDeviceLogMetadataVO vo = new IoTDeviceLogMetadataVO();
      vo.setId(rs.getLong("id"));
      vo.setIotId(rs.getString("iot_id"));
      vo.setProductKey(rs.getString("product_key"));
      vo.setDeviceName(rs.getString("device_name"));
      vo.setDeviceId(rs.getString("device_id"));
      vo.setMessageType(rs.getString("message_type"));
      vo.setEvent(rs.getString("event"));
      vo.setProperty(rs.getString("property"));
      vo.setContent(rs.getString("content"));
      vo.setExt1(rs.getString("ext1"));
      vo.setExt2(rs.getString("ext2"));
      vo.setExt3(rs.getString("ext3"));
      Timestamp createTime = rs.getTimestamp("create_time");
      if (createTime != null) {
        vo.setCreateTime(createTime.toLocalDateTime());
      }
      return vo;
    }
  }

  private String buildTtlClause() {
    if (ttlDays <= 0) {
      return "";
    }
    return "TTL create_time + toIntervalDay(" + ttlDays + ")";
  }

  private String escapeSqlLiteral(String value) {
    return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
  }

  private String extractDatabaseName() {
    int slash = address.lastIndexOf('/');
    if (slash < 0 || slash == address.length() - 1) {
      return "";
    }
    return address.substring(slash + 1);
  }
}
