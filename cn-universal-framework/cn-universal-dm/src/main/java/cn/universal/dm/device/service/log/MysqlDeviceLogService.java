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

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.common.exception.IoTException;
import cn.universal.core.message.UPRequest;
import cn.universal.core.metadata.AbstractPropertyMetadata;
import cn.universal.core.metadata.DeviceMetadata;
import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import cn.universal.dm.device.service.log.writer.DeviceLogEntityWrite;
import cn.universal.dm.device.service.log.writer.DeviceLogWriteRequest;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.common.inteceptor.SQenGenId;
import cn.universal.persistence.consistent.DeviceLogHashSharding;
import cn.universal.persistence.consistent.DeviceLogMetaHashSharding;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.dto.LogStorePolicyDTO;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTDeviceLogMetadata;
import cn.universal.persistence.entity.IoTDeviceLogMetadata.IoTDeviceLogMetadataBuilder;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.vo.EventCountAndTimeVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.mapper.IoTDeviceLogMapper;
import cn.universal.persistence.mapper.IoTDeviceLogMetadataShardMapper;
import cn.universal.persistence.mapper.IoTDeviceLogShardMapper;
import cn.universal.persistence.query.DeviceLogQuery;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import cn.universal.persistence.query.PropertyTrendQuery;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 设备日志
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/9/22 16:10
 */
@Component
@Slf4j
public class MysqlDeviceLogService extends AbstractIoTDeviceLogService {
  private static final int MAX_RELATIONAL_BATCH_ROWS = 1000;

  private String storePolicy = "mysql";
  @Resource private IoTDeviceLogMapper ioTDeviceLogMapper;
  @Resource private IoTDeviceLogShardMapper ioTDeviceLogShardMapper;
  @Resource private IoTDeviceLogMetadataShardMapper ioTDeviceLogMetadataShardMapper;
  @Resource private DeviceLogHashSharding deviceLogHashSharding;
  @Resource private DeviceLogMetaHashSharding deviceLogMetaHashSharding;
  @Resource private RelationalHistorySchemaSupport historySchema;

  @Value("${nexiot.device-log.writer.relational-shard-concurrency:4}")
  private int relationalShardConcurrency = 4;

  private ExecutorService relationalShardExecutor;

  @PostConstruct
  void startRelationalShardWriter() {
    relationalShardConcurrency = Math.max(1, Math.min(16, relationalShardConcurrency));
    if (relationalShardConcurrency == 1) {
      return;
    }
    relationalShardExecutor =
        Executors.newFixedThreadPool(
            relationalShardConcurrency,
            Thread.ofPlatform()
                .daemon(true)
                .name("device-log-shard-" + getPolicy() + "-", 0)
                .factory());
    log.info(
        "Relational device-log shard writer started policy={}, concurrency={}",
        getPolicy(),
        relationalShardConcurrency);
  }

  @PreDestroy
  void stopRelationalShardWriter() {
    ExecutorService executor = relationalShardExecutor;
    relationalShardExecutor = null;
    if (executor == null) {
      return;
    }
    executor.shutdown();
    try {
      if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      executor.shutdownNow();
    }
  }

  @Override
  public String getPolicy() {
    return storePolicy;
  }

  @Override
  @Async
  public void saveDeviceLog(
      BaseUPRequest upRequest, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {
    saveDeviceLogDirect(upRequest, ioTDeviceDTO, ioTProduct);
  }

  @Override
  public void saveDeviceLogDirect(
      BaseUPRequest upRequest, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {
    /** 产品数据存储策略，不为空则保存日志 */
    if (StrUtil.isNotBlank(ioTProduct.getStorePolicy())) {
      try {
        IoTDeviceLog log = build(upRequest, ioTDeviceDTO);
        ioTDeviceLogShardMapper.insertSelective(log);
      } catch (Exception e) {
        log.error("保存设备日志报错={}", e);
      }
      String storePolicyConfiguration = ioTProduct.getStorePolicyConfiguration();
      try {
        if (StrUtil.isNotBlank(storePolicyConfiguration)
            || MessageType.EVENT.equals(upRequest.getMessageType())) {
          LogStorePolicyDTO productLogStorePolicy = resolveLogStorePolicy(ioTProduct);
          saveLogStorePolicy(productLogStorePolicy, upRequest, ioTProduct);
        }
      } catch (Exception e) {
        log.error("保存设备属性扩展日志报错={}", e);
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
      try {
        IoTDeviceDTO device =
            request.ioTDeviceDTO() == null
                ? request.upRequest().getIoTDeviceDTO()
                : request.ioTDeviceDTO();
        if (device == null) {
          log.warn(
              "MySQL/Timescale批量日志跳过，设备为空 productKey={}, deviceId={}",
              request.upRequest().getProductKey(),
              request.upRequest().getDeviceId());
          continue;
        }
        logs.add(build(request.upRequest(), device));
      } catch (Exception e) {
        log.error("构建设备日志失败={}", e);
      }
      metadataRequests.add(request);
    }
    insertDeviceLogRecords(logs);
    List<IoTDeviceLogMetadata> metadataRecords = new ArrayList<>();
    for (DeviceLogWriteRequest request : metadataRequests) {
      BaseUPRequest upRequest = request.upRequest();
      IoTProduct product = request.ioTProduct();
      String storePolicyConfiguration = product.getStorePolicyConfiguration();
      try {
        if (StrUtil.isNotBlank(storePolicyConfiguration)
            || MessageType.EVENT.equals(upRequest.getMessageType())) {
          LogStorePolicyDTO productLogStorePolicy = resolveLogStorePolicy(product);
          metadataRecords.addAll(
              buildLogStorePolicyRecords(productLogStorePolicy, upRequest, product));
        }
      } catch (Exception e) {
        log.error("保存设备属性扩展日志报错={}", e);
      }
    }
    insertMetadataRecords(metadataRecords);
  }

  @Override
  public void saveDeviceLog(
      IoTDeviceLog ioTDeviceLog, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {
    /** 产品数据存储策略，不为空则保存日志 */
    if (StrUtil.isNotBlank(ioTProduct.getStorePolicy())) {
      try {
        ioTDeviceLogShardMapper.insertSelective(ensureLogIdentity(ioTDeviceLog));
      } catch (Exception e) {
        log.error("保存设备日志报错={}", e);
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
    insertDeviceLogRecords(logs);
  }

  private void insertDeviceLogRecords(List<IoTDeviceLog> logs) {
    if (logs == null || logs.isEmpty()) {
      return;
    }
    Map<String, RoutedBatch<IoTDeviceLog>> grouped = new LinkedHashMap<>();
    for (IoTDeviceLog log : logs) {
      IoTDeviceLog target = ensureLogIdentity(log);
      if (target == null || StrUtil.isBlank(target.getIotId())) {
        continue;
      }
      String routingIotId = target.getIotId();
      String shardKey = deviceLogShardKey(routingIotId);
      grouped
          .computeIfAbsent(shardKey, ignored -> new RoutedBatch<>(routingIotId))
          .records()
          .add(target);
    }
    executeRoutedBatches(grouped, this::insertDeviceLogBatch);
  }

  private void insertDeviceLogBatch(
      String shardKey, String routingIotId, List<IoTDeviceLog> batch) {
    try {
      ioTDeviceLogShardMapper.batchInsertByIotId(routingIotId, batch);
    } catch (Exception e) {
      log.warn(
          "批量写入 MySQL/Timescale 日志失败，回退逐条写入: shard={}, rows={}",
          shardKey,
          batch.size(),
          e);
      for (IoTDeviceLog deviceLog : batch) {
        ioTDeviceLogShardMapper.insertSelective(deviceLog);
      }
    }
  }

  private String deviceLogShardKey(String iotId) {
    return deviceLogHashSharding == null
        ? iotId
        : deviceLogHashSharding.generateTableName("iot_device_log", iotId);
  }

  private void saveLogStorePolicy(
      LogStorePolicyDTO logStorePolicyDTO, UPRequest up, IoTProduct ioTProduct) {
    insertMetadataRecords(buildLogStorePolicyRecords(logStorePolicyDTO, up, ioTProduct));
  }

  private List<IoTDeviceLogMetadata> buildLogStorePolicyRecords(
      LogStorePolicyDTO logStorePolicyDTO, UPRequest up, IoTProduct ioTProduct) {
    LogStorePolicyDTO effectivePolicy =
        logStorePolicyDTO == null ? LogStorePolicyDTO.builder().build() : logStorePolicyDTO;
    List<IoTDeviceLogMetadata> metadataRecords = new ArrayList<>();
    DeviceMetadata deviceMetadata = getDeviceMetadata(ioTProduct.getMetadata());
    if (MessageType.PROPERTIES.equals(up.getMessageType())
        && up.getProperties() != null
        && CollectionUtil.isNotEmpty(effectivePolicy.getProperties())) {
      up.getProperties()
          .forEach(
              (key, value) -> {
                if (effectivePolicy.getProperties().containsKey(key)) {
                  AbstractPropertyMetadata propertyOrNull = deviceMetadata.getPropertyOrNull(key);
                  IoTDevicePropertiesBO ioTDevicePropertiesBO = new IoTDevicePropertiesBO();
                  ioTDevicePropertiesBO.withValue(propertyOrNull, value);
                  // TODO event
                  IoTDeviceLogMetadataBuilder IoTDeviceLogMetadataBuilder = builder(up);
                  IoTDeviceLogMetadataBuilder.property(key);
                  IoTDeviceLogMetadataBuilder.content(metadataValueText(value));
                  IoTDeviceLogMetadataBuilder.ext1(ioTDevicePropertiesBO.getPropertyName());
                  IoTDeviceLogMetadataBuilder.ext2(ioTDevicePropertiesBO.getFormatValue());
                  IoTDeviceLogMetadataBuilder.ext3(ioTDevicePropertiesBO.getSymbol());
                  metadataRecords.add(IoTDeviceLogMetadataBuilder.build());
                }
              });
    }
    if (MessageType.EVENT.equals(up.getMessageType())) {
      IoTDeviceLogMetadataBuilder IoTDeviceLogMetadataBuilder = builder(up);
      IoTDeviceLogMetadataBuilder.event(up.getEvent());
      IoTDeviceLogMetadataBuilder.content(up.getEventName());
      metadataRecords.add(IoTDeviceLogMetadataBuilder.build());
    }

    if (metadataRecords.isEmpty()) {
      return metadataRecords;
    }

    ensureMetadataIds(metadataRecords);
    return metadataRecords;
  }

  private void insertMetadataRecords(List<IoTDeviceLogMetadata> metadataRecords) {
    if (metadataRecords == null || metadataRecords.isEmpty()) {
      return;
    }
    Map<String, RoutedBatch<IoTDeviceLogMetadata>> grouped = new LinkedHashMap<>();
    for (IoTDeviceLogMetadata metadata : metadataRecords) {
      if (metadata == null || StrUtil.isBlank(metadata.getIotId())) {
        continue;
      }
      String routingIotId = metadata.getIotId();
      String shardKey = deviceLogMetadataShardKey(routingIotId);
      grouped
          .computeIfAbsent(shardKey, ignored -> new RoutedBatch<>(routingIotId))
          .records()
          .add(metadata);
    }
    executeRoutedBatches(grouped, this::insertMetadataBatch);
  }

  private void insertMetadataBatch(
      String shardKey, String routingIotId, List<IoTDeviceLogMetadata> batch) {
    try {
      ioTDeviceLogMetadataShardMapper.batchInsertByIotId(routingIotId, batch);
    } catch (Exception e) {
      log.warn(
          "批量写入 MySQL/Timescale 元数据失败，回退逐条写入: shard={}, rows={}",
          shardKey,
          batch.size(),
          e);
      for (IoTDeviceLogMetadata metadata : batch) {
        insertMetadataRecordSafely(metadata);
      }
    }
  }

  private void insertMetadataRecordSafely(IoTDeviceLogMetadata metadata) {
    try {
      insertMetadataRecord(metadata);
    } catch (Exception ex) {
      log.error(
          "逐条写入 MySQL/Timescale 元数据失败: iotId={}, property={}, event={}",
          metadata.getIotId(),
          metadata.getProperty(),
          metadata.getEvent(),
          ex);
    }
  }

  private String deviceLogMetadataShardKey(String iotId) {
    return deviceLogMetaHashSharding == null
        ? iotId
        : deviceLogMetaHashSharding.generateTableName("iot_device_log_metadata", iotId);
  }

  private void insertMetadataRecord(IoTDeviceLogMetadata metadata) {
    ioTDeviceLogMetadataShardMapper.batchInsertByIotId(metadata.getIotId(), List.of(metadata));
  }

  private void ensureMetadataIds(List<IoTDeviceLogMetadata> metadataRecords) {
    for (IoTDeviceLogMetadata metadata : metadataRecords) {
      if (metadata.getId() == null) {
        metadata.setId(SQenGenId.nextId());
      }
      normalizeMetadataForRelationalStore(metadata);
    }
  }

  private static <T> List<List<T>> partition(List<T> records) {
    if (records.size() <= MAX_RELATIONAL_BATCH_ROWS) {
      return List.of(records);
    }
    List<List<T>> batches = new ArrayList<>();
    for (int from = 0; from < records.size(); from += MAX_RELATIONAL_BATCH_ROWS) {
      int to = Math.min(records.size(), from + MAX_RELATIONAL_BATCH_ROWS);
      batches.add(records.subList(from, to));
    }
    return batches;
  }

  private <T> void executeRoutedBatches(
      Map<String, RoutedBatch<T>> grouped, RoutedBatchWriter<T> writer) {
    if (grouped == null || grouped.isEmpty()) {
      return;
    }
    ExecutorService executor = relationalShardExecutor;
    if (executor == null || grouped.size() == 1) {
      grouped.forEach((shardKey, routedBatch) -> writeRoutedBatch(shardKey, routedBatch, writer));
      return;
    }

    List<Future<?>> futures = new ArrayList<>(grouped.size());
    grouped.forEach(
        (shardKey, routedBatch) ->
            futures.add(executor.submit(() -> writeRoutedBatch(shardKey, routedBatch, writer))));
    awaitShardWrites(futures);
  }

  private <T> void writeRoutedBatch(
      String shardKey, RoutedBatch<T> routedBatch, RoutedBatchWriter<T> writer) {
    for (List<T> batch : partition(routedBatch.records())) {
      writer.write(shardKey, routedBatch.routingIotId(), batch);
    }
  }

  private void awaitShardWrites(List<Future<?>> futures) {
    RuntimeException failure = null;
    for (Future<?> future : futures) {
      try {
        future.get();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        futures.forEach(item -> item.cancel(true));
        throw new IllegalStateException("Interrupted while writing device-log shards", ex);
      } catch (ExecutionException ex) {
        if (failure == null) {
          Throwable cause = ex.getCause();
          failure =
              cause instanceof RuntimeException runtimeException
                  ? runtimeException
                  : new IllegalStateException("Device-log shard write failed", cause);
        }
      }
    }
    if (failure != null) {
      throw failure;
    }
  }

  @FunctionalInterface
  private interface RoutedBatchWriter<T> {
    void write(String shardKey, String routingIotId, List<T> batch);
  }

  private static final class RoutedBatch<T> {
    private final String routingIotId;
    private final List<T> records = new ArrayList<>();

    private RoutedBatch(String routingIotId) {
      this.routingIotId = routingIotId;
    }

    private String routingIotId() {
      return routingIotId;
    }

    private List<T> records() {
      return records;
    }
  }

  @Override
  public PageBean<IoTDeviceLogVO> pageList(LogQuery bo) {
    requireHistoryTable(deviceLogHashSharding.generateTableName("iot_device_log", bo.getIotId()));
    if (bo.isCursorMode()) {
      return HistoryPageSupport.page(
          ioTDeviceLogShardMapper.queryLogCursorList(bo),
          bo,
          IoTDeviceLogVO::getCreateTime,
          IoTDeviceLogVO::getId);
    }
    PageHelper.startPage(bo.getPageNum(), bo.getPageSize());
    if (ObjectUtil.isNull(bo.getId())) {
      List<IoTDeviceLogVO> ioTDeviceLogVOS = ioTDeviceLogShardMapper.queryLogPageV2List(bo);
      return new PageBean(
          ioTDeviceLogVOS,
          new PageInfo(ioTDeviceLogVOS).getTotal(),
          bo.getPageSize(),
          bo.getPageNum());
    } else {
      List<IoTDeviceLogVO> ioTDeviceLogVOS = ioTDeviceLogShardMapper.queryLogPageV2ByIdList(bo);
      return new PageBean(
          ioTDeviceLogVOS,
          new PageInfo(ioTDeviceLogVOS).getTotal(),
          bo.getPageSize(),
          bo.getPageNum());
    }
  }

  @Override
  public IoTDeviceLogVO queryById(LogQuery logQuery) {
    IoTDeviceLogVO ioTDeviceLogVO = ioTDeviceLogMapper.queryLogById(logQuery);
    return ioTDeviceLogVO;
  }

  @Override
  public PageBean<IoTDeviceEvents> queryEventTotal(String productKey, String iotId) {
    List<IoTDeviceEvents> list = selectDevEvents(productKey);
    if (list.isEmpty()) return new PageBean<>(list, 0L, 100, 1);
    requireHistoryTable(deviceLogHashSharding.generateTableName("iot_device_log", iotId));
    // A single round trip matters for SDK products with hundreds of event definitions.
    Map<String, EventCountAndTimeVO> summaries = new java.util.HashMap<>();
    for (EventCountAndTimeVO summary :
        ioTDeviceLogShardMapper.queryHistoryEventSummaries(
            iotId, productKey, list.stream().map(IoTDeviceEvents::getId).distinct().toList())) {
      summaries.put(summary.getEvent(), summary);
    }
    for (IoTDeviceEvents devEvent : list) {
      EventCountAndTimeVO eventCountAndTimeVO = summaries.get(devEvent.getId());
      devEvent.setQty("0");
      devEvent.setTime(null);
      if (eventCountAndTimeVO != null) {
        if (eventCountAndTimeVO.getLatestCreateTime() != null) {
          devEvent.setTime(DateUtil.formatDateTime(eventCountAndTimeVO.getLatestCreateTime()));
        }
        long qty =
            eventCountAndTimeVO.getTotalCount() == null ? 0 : eventCountAndTimeVO.getTotalCount();
        devEvent.setQty(qty >= 100 ? "99+" : String.valueOf(qty));
      }
    }
    return new PageBean<>(list, (long) list.size(), 100, 1);
  }

  @Override
  public PageBean<IoTDeviceLogMetadataVO> queryLogMeta(LogQuery logQuery) {
    requireHistoryTable(
        deviceLogMetaHashSharding.generateTableName(
            "iot_device_log_metadata", logQuery.getIotId()));
    if (logQuery.isCursorMode()) {
      return HistoryPageSupport.page(
          ioTDeviceLogMetadataShardMapper.selectLogMetaCursor(logQuery),
          logQuery,
          IoTDeviceLogMetadataVO::getCreateTime,
          IoTDeviceLogMetadataVO::getId);
    }
    PageHelper.startPage(logQuery.getPageNum(), logQuery.getPageSize());
    List<IoTDeviceLogMetadataVO> list = ioTDeviceLogMetadataShardMapper.selectLogMetaList(logQuery);
    return new PageBean(
        list, new PageInfo(list).getTotal(), logQuery.getPageSize(), logQuery.getPageNum());
  }

  @Override
  public PropertyTrendVO queryPropertyTrend(PropertyTrendQuery query) {
    query.validateRange();
    requireHistoryTable(
        deviceLogMetaHashSharding.generateTableName("iot_device_log_metadata", query.getIotId()));
    try {
      if (query.isSampledQuery()) {
        return TrendSamplingSupport.aggregate(
            query, ioTDeviceLogMetadataShardMapper.selectPropertyTrendSamples(query));
      }
      return PropertyTrendVO.of(query, ioTDeviceLogMetadataShardMapper.selectPropertyTrend(query));
    } catch (DataAccessException failure) {
      if (isHistoryTimeout(failure)) {
        throw new IoTException("历史趋势查询超时，请降低读取上限或缩小时间范围后重试", 503, failure);
      }
      throw failure;
    }
  }

  private boolean isHistoryTimeout(DataAccessException failure) {
    if (failure instanceof QueryTimeoutException) return true;
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      if (cause instanceof SQLTimeoutException
          || (cause instanceof SQLException sql && "57014".equals(sql.getSQLState()))) return true;
    }
    return false;
  }

  @Override
  public JSONObject configMetadata() {
    JSONObject metadata = new JSONObject();
    if (historySchema != null) {
      metadata.set(
          "historySchema",
          Map.of(
              "metrics", historySchema.metrics(),
              "readiness", historySchema.readinessSnapshot(),
              "warnings", historySchema.maintenanceWarnings()));
    }
    return metadata;
  }

  /**
   * 查询最近的指令记录
   *
   * @param productKey 产品
   * @param deviceId 产品
   * @param commandId 指令ID
   * @param messageType 消息类型
   * @param createTime 创建时间
   * @param commandStatus 指令状态
   * @return 指令记录
   */
  @Override
  public IoTDeviceLog getRecentCommand(
      String productKey,
      String deviceId,
      String commandId,
      String messageType,
      Long createTime,
      Integer commandStatus) {
    String iotId =
        productKey == null || productKey.isEmpty() || deviceId == null || deviceId.isEmpty()
            ? null
            : productKey + deviceId;
    DeviceLogQuery query =
        new DeviceLogQuery.Builder()
            .iotId(iotId)
            .productKey(productKey)
            .deviceId(deviceId)
            .commandId(commandId)
            .messageType(messageType)
            .startTime(createTime)
            .commandStatus(commandStatus)
            .limit(1)
            .build();
    List<IoTDeviceLog> logs = queryDeviceLogs(query);
    return logs.isEmpty() ? null : logs.get(0);
  }

  /** 通用设备日志查询接口 */
  @Override
  public List<IoTDeviceLog> queryDeviceLogs(DeviceLogQuery query) {
    return ioTDeviceLogShardMapper.queryDeviceLogs(query);
  }

  private void requireHistoryTable(String table) {
    if (historySchema != null) {
      historySchema.requireReady(table);
    }
  }
}
