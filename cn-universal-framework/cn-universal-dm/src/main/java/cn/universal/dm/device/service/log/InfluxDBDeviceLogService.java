/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权,未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.dm.device.service.log;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.common.exception.IoTException;
import cn.universal.core.metadata.AbstractPropertyMetadata;
import cn.universal.core.metadata.DeviceMetadata;
import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.log.writer.DeviceLogEntityWrite;
import cn.universal.dm.device.service.log.writer.DeviceLogWriteRequest;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.common.inteceptor.SQenGenId;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.dto.LogStorePolicyDTO;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.entity.vo.PropertyTrendSampleVO;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.query.DeviceLogQuery;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import cn.universal.persistence.query.PropertyTrendQuery;
import com.influxdb.client.BucketsApi;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.client.OrganizationsApi;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.BucketRetentionRules;
import com.influxdb.client.domain.Organization;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * InfluxDB 存储日志
 *
 * <p>通过配置文件动态加载
 *
 * <p>InfluxDB 2.x 时序数据库适配实现
 *
 * <p>数据模型：
 *
 * <ul>
 *   <li>measurement: device_log / property_metadata / event_metadata
 *   <li>tags: productKey, deviceId, messageType, event, property (索引字段)
 *   <li>fields: content, deviceName, commandId, commandStatus, point 等 (数值字段)
 *   <li>timestamp: 使用数据产生时间作为时间戳
 * </ul>
 *
 * @author gitee.com/NexIoT
 * @version 1.2 // 版本升级：适配Unix时间戳查询
 * @since 2025/10/30
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "influxdb", name = "enable", havingValue = "true")
public class InfluxDBDeviceLogService extends AbstractIoTDeviceLogService {

  private String storePolicy = "influxdb";

  @Value("${influxdb.url:http://127.0.0.1:8086}")
  private String url;

  @Value("${influxdb.token:}")
  private String token;

  @Value("${influxdb.org:nexiot}")
  private String organization;

  @Value("${influxdb.bucket:device_logs}")
  private String bucket;

  @Value("${influxdb.auto-init:true}")
  private boolean autoInit;

  @Value("${influxdb.auto-create-org:false}")
  private boolean autoCreateOrganization;

  @Value("${influxdb.bucket-retention-seconds:0}")
  private int bucketRetentionSeconds;

  @Value("${nexiot.log.history.query-timeout-seconds:8}")
  private int historyQueryTimeoutSeconds = 8;

  @Resource private IoTDeviceService iotDeviceService;

  private InfluxDBClient influxDBClient;
  private InfluxDBClient historyQueryClient;
  private static final Pattern DECIMAL_VALUE =
      Pattern.compile("[+-]?(?:[0-9]+(?:[.][0-9]*)?|[.][0-9]+)(?:[eE][+-]?[0-9]+)?");
  // 时间格式化器：适配InfluxDB的RFC3339格式
  private static final DateTimeFormatter RFC3339_FORMATTER =
      DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault());

  @PostConstruct
  public void initClient() {
    try {
      influxDBClient = InfluxDBClientFactory.create(url, token.toCharArray(), organization, bucket);

      // 历史查询独立限时，避免大范围读取占住连接，也不改变采集写入的超时。
      historyQueryClient =
          InfluxDBClientFactory.create(
              InfluxDBClientOptions.builder()
                  .url(url)
                  .authenticateToken(token.toCharArray())
                  .org(organization)
                  .bucket(bucket)
                  .okHttpClient(
                      new OkHttpClient.Builder()
                          .connectTimeout(3, TimeUnit.SECONDS)
                          .readTimeout(historyQueryTimeoutSeconds, TimeUnit.SECONDS)
                          .callTimeout(historyQueryTimeoutSeconds, TimeUnit.SECONDS))
                  .build());

      // 测试连接
      if (influxDBClient.ping()) {
        log.info("初始化InfluxDB连接成功，url={}, org={}, bucket={}", url, organization, bucket);
        if (autoInit) {
          ensureInfluxResources();
        }
      } else {
        log.warn("InfluxDB连接可能异常，请检查配置");
      }
    } catch (Exception e) {
      log.error("初始化InfluxDB连接失败", e);
    }
  }

  @PreDestroy
  public void closeClient() {
    if (historyQueryClient != null) {
      historyQueryClient.close();
    }
    if (influxDBClient != null) {
      try {
        influxDBClient.close();
        log.info("InfluxDB连接已关闭");
      } catch (Exception e) {
        log.error("关闭InfluxDB连接失败", e);
      }
    }
  }

  private void ensureInfluxResources() {
    try {
      Organization organizationEntity = resolveOrganization();
      if (organizationEntity == null) {
        log.warn("InfluxDB未找到组织，跳过 bucket 自动初始化: org={}", organization);
        return;
      }

      BucketsApi bucketsApi = influxDBClient.getBucketsApi();
      Bucket existingBucket = bucketsApi.findBucketByName(bucket);
      if (existingBucket != null) {
        log.info("InfluxDB bucket 已存在，跳过创建: org={}, bucket={}", organization, bucket);
        return;
      }

      BucketRetentionRules retentionRules = buildBucketRetentionRules();
      if (retentionRules == null) {
        bucketsApi.createBucket(bucket, organizationEntity);
      } else {
        bucketsApi.createBucket(bucket, retentionRules, organizationEntity);
      }
      log.info("InfluxDB bucket 自动创建成功: org={}, bucket={}", organization, bucket);
    } catch (Exception e) {
      log.error("InfluxDB 自动初始化 bucket 失败: org={}, bucket={}", organization, bucket, e);
    }
  }

  private Organization resolveOrganization() {
    OrganizationsApi organizationsApi = influxDBClient.getOrganizationsApi();
    for (Organization organizationEntity : organizationsApi.findOrganizations()) {
      if (StrUtil.equals(organization, organizationEntity.getName())) {
        return organizationEntity;
      }
    }

    if (!autoCreateOrganization) {
      return null;
    }

    try {
      Organization created = organizationsApi.createOrganization(organization);
      log.info("InfluxDB organization 自动创建成功: org={}", organization);
      return created;
    } catch (Exception e) {
      log.error("InfluxDB organization 自动创建失败: org={}", organization, e);
      return null;
    }
  }

  private BucketRetentionRules buildBucketRetentionRules() {
    if (bucketRetentionSeconds <= 0) {
      return null;
    }
    return new BucketRetentionRules().everySeconds(bucketRetentionSeconds);
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
      IoTDeviceDTO ioTDeviceDTO = resolveDevice(upRequest, noUse, iotDeviceService);
      try {
        if (ioTDeviceDTO == null) {
          log.warn(
              "InfluxDB保存设备日志跳过，设备不存在 productKey={}, deviceId={}",
              upRequest.getProductKey(),
              upRequest.getDeviceId());
          return;
        }
        IoTDeviceLog ioTDeviceLog = build(upRequest, ioTDeviceDTO);
        saveDeviceLogToInfluxDB(ioTDeviceLog);
        log.debug("InfluxDB插入设备日志成功，deviceId={}", upRequest.getDeviceId());
      } catch (Exception e) {
        log.error("保存设备日志到InfluxDB报错={}", e);
      }

      // 处理存储策略配置
      String storePolicyConfiguration = ioTProduct.getStorePolicyConfiguration();
      try {
        if (StrUtil.isNotBlank(storePolicyConfiguration)
            || MessageType.EVENT.equals(upRequest.getMessageType())) {
          LogStorePolicyDTO productLogStorePolicy = resolveLogStorePolicy(ioTProduct);
          saveLogStorePolicy(productLogStorePolicy, upRequest, ioTProduct);
        }
      } catch (Exception e) {
        log.error("InfluxDB保存设备属性扩展日志报错={}", e);
      }
    }
  }

  @Override
  public void flushDeviceLogRequests(List<DeviceLogWriteRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      return;
    }
    for (DeviceLogWriteRequest request : requests) {
      if (request != null) {
        saveDeviceLogDirect(request.upRequest(), request.ioTDeviceDTO(), request.ioTProduct());
      }
    }
  }

  @Override
  public void saveDeviceLog(
      IoTDeviceLog ioTDeviceLog, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {
    /** 产品数据存储策略，不为空则保存日志 */
    if (StrUtil.isNotBlank(ioTProduct.getStorePolicy())) {
      try {
        saveDeviceLogToInfluxDB(ensureLogIdentity(ioTDeviceLog));
        log.debug("InfluxDB插入设备日志成功，deviceId={}", ioTDeviceDTO.getDeviceId());
      } catch (Exception e) {
        log.error("保存设备日志到InfluxDB报错={}", e);
      }
    }
  }

  @Override
  public void flushDeviceLogEntities(List<DeviceLogEntityWrite> entities) {
    if (entities == null || entities.isEmpty()) {
      return;
    }
    List<Point> points = new ArrayList<>(entities.size());
    for (DeviceLogEntityWrite entity : entities) {
      if (entity == null || entity.ioTDeviceLog() == null || entity.ioTProduct() == null) {
        continue;
      }
      if (StrUtil.isBlank(entity.ioTProduct().getStorePolicy())) {
        continue;
      }
      points.add(buildDeviceLogPoint(ensureLogIdentity(entity.ioTDeviceLog())));
    }
    writeDeviceLogPoints(points);
  }

  /** 根据存储策略保存元数据日志 支持属性和事件的详细信息存储 */
  private void saveLogStorePolicy(
      LogStorePolicyDTO logStorePolicyDTO, BaseUPRequest up, IoTProduct ioTProduct) {
    LogStorePolicyDTO effectivePolicy =
        logStorePolicyDTO == null ? LogStorePolicyDTO.builder().build() : logStorePolicyDTO;
    List<MetadataPoint> metadataPoints = new ArrayList<>();
    DeviceMetadata deviceMetadata = getDeviceMetadata(ioTProduct.getMetadata());

    // 处理属性消息
    if (MessageType.PROPERTIES.equals(up.getMessageType())
        && up.getProperties() != null
        && CollectionUtil.isNotEmpty(effectivePolicy.getProperties())) {
      up.getProperties()
          .forEach(
              (key, value) -> {
                if (effectivePolicy.getProperties().containsKey(key)) {
                  try {
                    AbstractPropertyMetadata propertyOrNull = deviceMetadata.getPropertyOrNull(key);
                    IoTDevicePropertiesBO ioTDevicePropertiesBO = new IoTDevicePropertiesBO();
                    ioTDevicePropertiesBO.withValue(propertyOrNull, value);

                    // 构建元数据并保存
                    String property = key;
                    String propertyName = ioTDevicePropertiesBO.getPropertyName();
                    String formatValue = ioTDevicePropertiesBO.getFormatValue();
                    String symbol = ioTDevicePropertiesBO.getSymbol();
                    String content = metadataValueText(value);

                    metadataPoints.add(
                        MetadataPoint.property(
                            property,
                            buildPropertyMetadataPoint(
                                up, property, content, propertyName, formatValue, symbol)));
                  } catch (Exception e) {
                    log.error("InfluxDB保存属性元数据失败: iotId={}, property={}", up.getIotId(), key, e);
                  }
                }
              });
    }

    // 处理事件消息
    if (MessageType.EVENT.equals(up.getMessageType())) {
      try {
        int maxStorage =
            CollectionUtil.isNotEmpty(effectivePolicy.getEvent())
                    && effectivePolicy.getEvent().containsKey(up.getEvent())
                ? resolveMaxStorage(effectivePolicy.getEvent().get(up.getEvent()))
                : IoTConstant.maxStorage;

        metadataPoints.add(
            MetadataPoint.event(up.getEvent(), buildEventMetadataPoint(up, maxStorage)));
      } catch (Exception e) {
        log.error("InfluxDB保存事件元数据失败: iotId={}, event={}", up.getIotId(), up.getEvent(), e);
      }
    }

    if (!metadataPoints.isEmpty()) {
      writeMetadataPointsWithFallback(up, metadataPoints);
    }
  }

  /**
   * 保存属性元数据到InfluxDB measurement: property_metadata tags: productKey, deviceId, iotId, messageType,
   * property fields: content, deviceName, ext1(propertyName), ext2(formatValue), ext3(symbol)
   */
  private void savePropertyMetadataToInfluxDB(
      BaseUPRequest up,
      String property,
      String content,
      String propertyName,
      String formatValue,
      String symbol)
      throws Exception {
    if (influxDBClient == null) {
      throw new RuntimeException("InfluxDB client未初始化");
    }

    WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
    Point point =
        buildPropertyMetadataPoint(up, property, content, propertyName, formatValue, symbol);

    try {
      writeApi.writePoint(point);
      log.debug("InfluxDB插入属性元数据成功: property={}, iotId={}", property, up.getIotId());
    } catch (Exception e) {
      log.error("InfluxDB插入属性元数据失败: property={}, iotId={}", property, up.getIotId(), e);
      throw e;
    }
  }

  /**
   * 保存事件元数据到InfluxDB measurement: event_metadata tags: productKey, deviceId, iotId, messageType,
   * event fields: content(eventName), deviceName, ext1(JSONData), maxStorage
   */
  private void saveEventMetadataToInfluxDB(BaseUPRequest up, int maxStorage) throws Exception {
    if (influxDBClient == null) {
      throw new RuntimeException("InfluxDB client未初始化");
    }

    WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
    Point point = buildEventMetadataPoint(up, maxStorage);

    try {
      writeApi.writePoint(point);
      log.debug("InfluxDB插入事件元数据成功: event={}, iotId={}", up.getEvent(), up.getIotId());
    } catch (Exception e) {
      log.error("InfluxDB插入事件元数据失败: event={}, iotId={}", up.getEvent(), up.getIotId(), e);
      throw e;
    }
  }

  private Point buildPropertyMetadataPoint(
      BaseUPRequest up,
      String property,
      String content,
      String propertyName,
      String formatValue,
      String symbol) {
    Point point =
        Point.measurement("property_metadata")
            .addTag("productKey", up.getProductKey())
            .addTag("deviceId", up.getDeviceId())
            .addTag("iotId", up.getIotId())
            .addTag("messageType", MessageType.PROPERTIES.name())
            .addTag("property", property)
            .addField("id", ensureMetadataId(up, property, null))
            .addField("content", content != null ? content : "")
            .addField("deviceName", up.getDeviceName() != null ? up.getDeviceName() : "")
            .addField("ext1", propertyName != null ? propertyName : "")
            .addField("ext2", formatValue != null ? formatValue : "")
            .addField("ext3", symbol != null ? symbol : "")
            .time(resolvePointTime(up), WritePrecision.MS);
    // 新写入保留原文，并提供可原生聚合的数值列；历史字符串兼容查询不依赖回填。
    Double numericValue = finiteNumericValue(content);
    point.addField("sampleCount", 1L).addField("numericCount", numericValue == null ? 0L : 1L);
    if (numericValue != null) {
      point.addField("numericValue", numericValue);
    }
    return point;
  }

  private Double finiteNumericValue(String content) {
    if (content == null || !DECIMAL_VALUE.matcher(content.strip()).matches()) {
      return null;
    }
    try {
      double value = Double.parseDouble(content.strip());
      return Double.isFinite(value) ? value : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Point buildEventMetadataPoint(BaseUPRequest up, int maxStorage) {
    return Point.measurement("event_metadata")
        .addTag("productKey", up.getProductKey())
        .addTag("deviceId", up.getDeviceId())
        .addTag("iotId", up.getIotId())
        .addTag("messageType", MessageType.EVENT.name())
        .addTag("event", up.getEvent())
        .addField("id", ensureMetadataId(up, null, up.getEvent()))
        .addField("content", up.getEventName() != null ? up.getEventName() : "")
        .addField("deviceName", up.getDeviceName() != null ? up.getDeviceName() : "")
        .addField("ext1", JSONUtil.toJsonStr(up.getData()))
        .addField("maxStorage", maxStorage)
        .time(resolvePointTime(up), WritePrecision.MS);
  }

  private void writeMetadataPointsWithFallback(
      BaseUPRequest up, List<MetadataPoint> metadataPoints) {
    if (influxDBClient == null) {
      throw new RuntimeException("InfluxDB client未初始化");
    }

    WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
    List<Point> points = new ArrayList<>(metadataPoints.size());
    for (MetadataPoint metadataPoint : metadataPoints) {
      points.add(metadataPoint.point());
    }

    try {
      if (points.size() == 1) {
        writeApi.writePoint(points.get(0));
      } else {
        writeApi.writePoints(points);
      }
      log.debug("InfluxDB批量插入元数据成功: iotId={}, 数量={}", up.getIotId(), points.size());
    } catch (Exception e) {
      log.warn("InfluxDB批量插入元数据失败，回退单点写入: iotId={}", up.getIotId(), e);
      for (MetadataPoint metadataPoint : metadataPoints) {
        try {
          writeApi.writePoint(metadataPoint.point());
        } catch (Exception ex) {
          log.error(
              "InfluxDB单点写入元数据失败: iotId={}, property={}, event={}",
              up.getIotId(),
              metadataPoint.property(),
              metadataPoint.event(),
              ex);
        }
      }
    }
  }

  private record MetadataPoint(String property, String event, Point point) {
    private static MetadataPoint property(String property, Point point) {
      return new MetadataPoint(property, null, point);
    }

    private static MetadataPoint event(String event, Point point) {
      return new MetadataPoint(null, event, point);
    }
  }

  /**
   * 保存设备日志到InfluxDB measurement: device_log tags: productKey, deviceId, iotId, messageType, event
   * fields: content, deviceName, commandId, commandStatus, point
   *
   * <p>注意:deviceName为必填字段,用于count统计;content可能为空
   */
  private void saveDeviceLogToInfluxDB(IoTDeviceLog ioTDeviceLog) throws Exception {
    Point point = buildDeviceLogPoint(ioTDeviceLog);
    try {
      writeDeviceLogPoints(List.of(point));
      log.debug(
          "InfluxDB插入设备日志成功: productKey={}, deviceId={}, timestamp={}",
          ioTDeviceLog.getProductKey(),
          ioTDeviceLog.getDeviceId(),
          ioTDeviceLog.getCreateTime());
    } catch (Exception e) {
      log.error(
          "InfluxDB插入设备日志失败: productKey={}, deviceId={}, error={}",
          ioTDeviceLog.getProductKey(),
          ioTDeviceLog.getDeviceId(),
          e.getMessage());
      throw e;
    }
  }

  private Point buildDeviceLogPoint(IoTDeviceLog ioTDeviceLog) {
    IoTDeviceLog target = ensureLogIdentity(ioTDeviceLog);
    Instant timestamp = target.getCreateTime().atZone(ZoneId.systemDefault()).toInstant();
    return Point.measurement("device_log")
        .addTag("productKey", target.getProductKey())
        .addTag("deviceId", target.getDeviceId())
        .addTag("iotId", target.getIotId())
        .addTag("messageType", target.getMessageType())
        .addTag("event", target.getEvent() != null ? target.getEvent() : "")
        .addField("id", target.getId())
        .addField("content", target.getContent() != null ? target.getContent() : "")
        .addField("deviceName", target.getDeviceName() != null ? target.getDeviceName() : "")
        .addField("commandId", target.getCommandId() != null ? target.getCommandId() : "")
        .addField(
            "commandStatus", target.getCommandStatus() != null ? target.getCommandStatus() : 0)
        .addField("point", target.getPoint() != null ? target.getPoint() : "")
        .time(timestamp, WritePrecision.MS);
  }

  private void writeDeviceLogPoints(List<Point> points) {
    if (points == null || points.isEmpty()) {
      return;
    }
    if (influxDBClient == null) {
      throw new RuntimeException("InfluxDB client未初始化");
    }
    WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
    if (points.size() == 1) {
      writeApi.writePoint(points.get(0));
    } else {
      writeApi.writePoints(points);
    }
  }

  @Override
  public PageBean<IoTDeviceLogVO> pageList(LogQuery query) {
    try {
      StringBuilder source = new StringBuilder();
      source.append("from(bucket: \"").append(quoteFluxString(bucket)).append("\")\n");
      source.append(buildRangeClause(query)).append("\n");
      source.append("  |> filter(fn: (r) => r._measurement == \"device_log\")\n");
      appendLogQueryFilters(source, query);
      String dataFlux = buildHistoryPageFlux(source.toString(), query, false);
      List<IoTDeviceLogVO> rows = parseDeviceLogResults(historyQueryApi().query(dataFlux), query);
      if (query.isCursorMode()) {
        return HistoryPageSupport.page(
            rows, query, IoTDeviceLogVO::getCreateTime, IoTDeviceLogVO::getId);
      }
      return new PageBean<>(rows, queryTotalCount(query), query.getPageSize(), query.getPageNum());
    } catch (Exception e) {
      log.error("InfluxDB查询设备日志失败", e);
      throw new IoTException("历史记录查询失败或超时，请缩小时间范围后重试", 503);
    }
  }

  private long queryTotalCount(LogQuery query) {
    StringBuilder flux = new StringBuilder();
    flux.append("from(bucket: \"").append(quoteFluxString(bucket)).append("\")\n");
    flux.append(buildRangeClause(query)).append("\n");
    flux.append("  |> filter(fn: (r) => r._measurement == \"device_log\")\n");
    appendLogQueryFilters(flux, query);
    flux.append("  |> filter(fn: (r) => r._field == \"content\")\n  |> count()\n");
    return parseCountResult(historyQueryApi().query(flux.toString()));
  }

  /**
   * 先按时间/tag范围裁剪，并对每个series/field取有界候选，再pivot和合并排序。
   * cursor续页不执行COUNT/OFFSET；id为同时间的稳定次序，不能仅用时间游标跳过同时间记录。
   */
  String buildHistoryPageFlux(String source, LogQuery query, boolean metadata) {
    long offset = query.isCursorMode() ? 0 : (long) (query.getPageNum() - 1) * query.getPageSize();
    int fetchSize = query.isCursorMode() ? query.getFetchSize() : query.getPageSize();
    long candidates = offset + fetchSize + (query.getBeforeTime() == null ? 0 : 1);
    StringBuilder flux = new StringBuilder(source);
    if (metadata) {
      // typed数值辅助列是稀疏列，不参与原始记录pivot，避免制造只有辅助列的残缺记录。
      flux.append(
          "  |> filter(fn: (r) => r._field == \"id\" or r._field == \"content\" or r._field =="
              + " \"deviceName\" or r._field == \"ext1\" or r._field == \"ext2\" or r._field =="
              + " \"ext3\")\n");
    }
    // from/range returns each storage series/field in timestamp order. Retain its newest
    // candidates before pivot; sorting a whole historical field here defeats the bound.
    // The final bounded sort still defines the cross-series timestamp/id cursor order.
    flux.append("  |> tail(n: ")
        .append(candidates)
        .append(")\n")
        .append(
            "  |> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n");
    if (query.isCursorMode() && query.getBeforeTime() != null) {
      String time =
          RFC3339_FORMATTER.format(
              query.getBeforeTime().atZone(ZoneId.systemDefault()).toInstant());
      flux.append("  |> filter(fn: (r) => r._time < ")
          .append(time)
          .append(" or (r._time == ")
          .append(time)
          .append(" and exists r.id and r.id < ")
          .append(query.getBeforeId())
          .append("))\n");
    }
    flux.append("  |> group()\n")
        .append("  |> sort(columns: [\"_time\", \"id\"], desc: true)\n")
        .append("  |> limit(n: ")
        .append(fetchSize);
    if (!query.isCursorMode()) {
      flux.append(", offset: ").append(offset);
    }
    return flux.append(")\n").toString();
  }

  /** 解析设备日志查询结果 使用pivot后,每条记录的所有field已经合并在一行,直接解析即可 */
  private List<IoTDeviceLogVO> parseDeviceLogResults(List<FluxTable> tables, LogQuery logQuery) {
    List<IoTDeviceLogVO> resultList = new ArrayList<>();

    for (FluxTable table : tables) {
      for (FluxRecord record : table.getRecords()) {
        Instant time = record.getTime();
        if (time == null) continue;

        IoTDeviceLogVO vo = new IoTDeviceLogVO();
        Long id = getLongValue(record.getValueByKey("id"));
        if (logQuery != null && logQuery.isCursorMode() && id == null) {
          throw new IoTException("历史记录缺少稳定标识，请先完成存储兼容升级", 503);
        }
        vo.setId(id != null ? id : time.toEpochMilli());
        vo.setCreateTime(LocalDateTime.ofInstant(time, ZoneId.systemDefault()));

        // 从tags中提取字段
        vo.setProductKey(getStringValue(record.getValueByKey("productKey")));
        vo.setDeviceId(getStringValue(record.getValueByKey("deviceId")));
        vo.setIotId(getStringValue(record.getValueByKey("iotId")));
        vo.setMessageType(getStringValue(record.getValueByKey("messageType")));
        vo.setEvent(getStringValue(record.getValueByKey("event")));

        // pivot后,field值直接在record中可以通过getValueByKey获取
        vo.setContent(getStringValue(record.getValueByKey("content")));
        vo.setDeviceName(getStringValue(record.getValueByKey("deviceName")));
        vo.setCommandId(getStringValue(record.getValueByKey("commandId")));
        vo.setCommandStatus(getIntValue(record.getValueByKey("commandStatus")));
        vo.setPoint(getStringValue(record.getValueByKey("point")));

        resultList.add(vo);
      }
    }

    // 按时间降序排序(虽然Flux已经排序,但保险起见)
    // 保留Flux的时间、id双重倒序，不能在这里重新破坏同时间记录的顺序。

    return resultList;
  }

  @Override
  public IoTDeviceLogVO queryById(LogQuery logQuery) {
    try {
      if (influxDBClient == null || logQuery.getId() == null) {
        log.warn("InfluxDB client未初始化或ID为空");
        return null;
      }

      Long targetId = Long.parseLong(logQuery.getId());
      Instant timestamp = findDeviceLogTimestamp(targetId, logQuery);
      if (timestamp == null) {
        return null;
      }

      StringBuilder flux = new StringBuilder();
      flux.append("from(bucket: \"").append(quoteFluxString(bucket)).append("\")\n");
      flux.append("  |> range(start: ")
          .append(timestamp.toString())
          .append(", stop: ")
          .append(timestamp.plusMillis(1).toString())
          .append(")\n");
      flux.append("  |> filter(fn: (r) => r._measurement == \"device_log\")\n");
      appendLogQueryFilters(flux, logQuery);

      // 添加pivot合并field
      flux.append(
          "  |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n");
      // 取消所有分组
      flux.append("  |> group()\n");
      flux.append("  |> filter(fn: (r) => r.id == ").append(targetId).append(")\n");

      log.debug("InfluxDB查询日志详情Flux: {}", flux);

      QueryApi queryApi = influxDBClient.getQueryApi();
      List<FluxTable> tables = queryApi.query(flux.toString());

      List<IoTDeviceLogVO> resultList = parseDeviceLogResults(tables, logQuery);

      return resultList.isEmpty() ? null : resultList.get(0);

    } catch (Exception e) {
      log.error("InfluxDB查询设备日志详情失败", e);
      return null;
    }
  }

  @Override
  public PageBean<IoTDeviceEvents> queryEventTotal(String productKey, String iotId) {
    List<IoTDeviceEvents> list = selectDevEvents(productKey);
    if (list.isEmpty()) return new PageBean<>(list, 0L, 1, 100);
    try {
      // 一个限时请求完成所有事件，保留原有全历史范围与99+显示语义。
      List<FluxTable> tables =
          historyQueryApi().query(buildEventTotalFlux(productKey, iotId, list));
      for (IoTDeviceEvents event : list) {
        long count = 0;
        Instant latest = null;
        for (FluxTable table : tables) {
          for (FluxRecord record : table.getRecords()) {
            if (!event.getId().equals(record.getValueByKey("event"))) continue;
            if ("event_counts".equals(record.getValueByKey("result"))) {
              Object value = record.getValue();
              if (value instanceof Number number)
                count = Math.min(100, count + Math.min(100, number.longValue()));
            } else if ("event_latest".equals(record.getValueByKey("result"))) {
              Instant time = record.getTime();
              if (time != null && (latest == null || time.isAfter(latest))) latest = time;
            }
          }
        }
        event.setQty(count >= 100 ? "99+" : String.valueOf(count));
        event.setTime(latest == null ? "" : latest.toString());
      }
      return new PageBean<>(list, (long) list.size(), 1, 100);
    } catch (Exception e) {
      log.error("InfluxDB查询事件统计失败, productKey={}, iotId={}", productKey, iotId, e);
      throw new IoTException("历史事件统计失败或超时，请稍后重试", 503);
    }
  }

  String buildEventTotalFlux(String productKey, String iotId, List<IoTDeviceEvents> events) {
    String selectors =
        events.stream()
            .map(event -> "r.event == \"" + quoteFluxString(event.getId()) + "\"")
            .collect(java.util.stream.Collectors.joining(" or "));
    return "source = from(bucket: \""
        + quoteFluxString(bucket)
        + "\")\n"
        + "  |> range(start: 1970-01-01T00:00:00Z)\n"
        + "  |> filter(fn: (r) => r._measurement == \"event_metadata\" and r._field =="
        + " \"content\")\n"
        + "  |> filter(fn: (r) => r.productKey == \""
        + quoteFluxString(productKey)
        + "\" and r.iotId == \""
        + quoteFluxString(iotId)
        + "\")\n"
        + "  |> filter(fn: (r) => "
        + selectors
        + ")\n"
        // 每个series最多读取100条即可确定99+，避免为展示徽标遍历全历史。
        + "source |> limit(n: 100) |> count() |> group(columns: [\"event\"]) |> sum() |>"
        + " yield(name: \"event_counts\")\n"
        + "source |> last() |> group(columns: [\"event\"]) |> sort(columns: [\"_time\"], desc:"
        + " true) |> limit(n: 1) |> yield(name: \"event_latest\")\n";
  }

  @Override
  public PropertyTrendVO queryPropertyTrend(PropertyTrendQuery query) {
    query.validateRange();
    try {
      if (query.isSampledQuery()) {
        List<PropertyTrendSampleVO> samples = new ArrayList<>();
        for (FluxTable table : historyQueryApi().query(buildSampledPropertyTrendFlux(query))) {
          for (FluxRecord record : table.getRecords()) {
            Object index = record.getValueByKey("window_index");
            if (!(index instanceof Number)) {
              throw new IllegalStateException("InfluxDB sample is missing its time window");
            }
            samples.add(
                new PropertyTrendSampleVO(
                    ((Number) index).intValue(), getStringValue(record.getValue())));
          }
        }
        return TrendSamplingSupport.aggregate(query, samples);
      }
      List<FluxTable> tables = historyQueryApi().query(buildPropertyTrendFlux(query));
      List<PropertyTrendVO.Bucket> result = new ArrayList<>();
      for (FluxTable table : tables) {
        for (FluxRecord record : table.getRecords()) {
          PropertyTrendVO.Bucket value = new PropertyTrendVO.Bucket();
          value.setTime(record.getTime().toEpochMilli());
          value.setCount(((Number) record.getValueByKey("count")).longValue());
          value.setNumericCount(((Number) record.getValueByKey("numericCount")).longValue());
          if (value.getNumericCount() > 0) {
            value.setMin(new BigDecimal(record.getValueByKey("min").toString()));
            value.setMax(new BigDecimal(record.getValueByKey("max").toString()));
            value.setAvg(new BigDecimal(record.getValueByKey("avg").toString()));
          }
          result.add(value);
        }
      }
      if (result.size() > query.getMaxPoints()) {
        throw new IllegalStateException("InfluxDB trend exceeded its requested window count");
      }
      result.sort(java.util.Comparator.comparingLong(PropertyTrendVO.Bucket::getTime));
      return PropertyTrendVO.of(query, result);
    } catch (Exception e) {
      log.error(
          "InfluxDB属性趋势查询失败 productKey={}, iotId={}, property={}",
          query.getProductKey(),
          query.getIotId(),
          query.getProperty(),
          e);
      throw new IoTException("历史趋势查询失败或超时，请缩小时间范围后重试", 503);
    }
  }

  String buildSampledPropertyTrendFlux(PropertyTrendQuery query) {
    StringBuilder flux = new StringBuilder("import \"strings\"\n");
    List<String> sources = new ArrayList<>();
    String filters = buildBaseFilterFlux(query);
    filters = filters.substring(filters.indexOf("  |> filter"));
    for (PropertyTrendQuery.SampleWindow window : query.getSampleWindows()) {
      String source = "sample" + window.getIndex();
      sources.add(source);
      Instant stop =
          window.getEndExclusiveMillis() == query.getEndCreateTime() * 1000L + 1L
              ? Instant.ofEpochSecond(query.getEndCreateTime()).plusNanos(1)
              : Instant.ofEpochMilli(window.getEndExclusiveMillis());
      flux.append(source)
          .append(" = from(bucket: \"")
          .append(quoteFluxString(bucket))
          .append("\")\n  |> range(start: ")
          .append(Instant.ofEpochMilli(window.getBeginMillis()))
          .append(", stop: ")
          .append(stop)
          .append(")\n")
          .append(filters)
          .append("  |> filter(fn: (r) => r._field == \"content\")\n")
          // A per-series storage limit precedes group(). Numeric conversion happens only in Java.
          // The second limit bounds the combined series without sorting their complete histories.
          .append("  |> limit(n: ")
          .append(query.getSampleFetchSize())
          .append(")\n  |> group()\n  |> limit(n: ")
          .append(query.getSampleFetchSize())
          .append(")\n  |> map(fn: (r) => ({window_index: ")
          .append(window.getIndex())
          .append(", _value: if strings.strlen(v:r._value)<=1024 then r._value else \"\"}))\n");
    }
    // Flux union requires two streams; a zero-width inclusive range has one window.
    if (sources.size() == 1) return flux.append(sources.getFirst()).append("\n").toString();
    return flux.append("union(tables: [")
        .append(String.join(",", sources))
        .append("])\n")
        .toString();
  }

  /** 字符串兼容路径：在数据库内按窗口流式归约，只返回 maxPoints 个桶。 不在 Java 扫描原始数据；这仍是原始序列扫描，不能替代超高频设备的预聚合保留策略。 */
  String buildPropertyTrendFlux(PropertyTrendQuery query) {
    long interval = query.getIntervalSeconds();
    long offset = Math.floorMod(query.getBeginCreateTime(), interval);
    return FINITE_NUMBER_FLUX
        + buildBaseFilterFlux(query)
        + "  |> filter(fn: (r) => r._field == \"content\")\n"
        + "  |> map(fn: (r) => { n = finiteNumber(text: r._value) return {r with numeric: n.valid,"
        + " value: n.value} })\n"
        + "  |> window(every: "
        + interval
        + "s, offset: "
        + offset
        + "s, createEmpty: false)\n"
        + "  |> reduce(identity: {count: 0, numericCount: 0, min: 0.0, max: 0.0, avg: 0.0}, fn: (r,"
        + " accumulator) => ({\n"
        + "    count: accumulator.count + 1, numericCount: accumulator.numericCount + (if r.numeric"
        + " then 1 else 0),\n"
        + "    min: if not r.numeric then accumulator.min else if accumulator.numericCount == 0 or"
        + " r.value < accumulator.min then r.value else accumulator.min,\n"
        + "    max: if not r.numeric then accumulator.max else if accumulator.numericCount == 0 or"
        + " r.value > accumulator.max then r.value else accumulator.max,\n"
        + "    avg: if not r.numeric then accumulator.avg else weightedMean(a: accumulator.avg, b:"
        + " r.value, left: accumulator.numericCount, right: 1)\n"
        + "  }))\n"
        // 不同 tag series 可能属于同一设备属性；在数据库内按窗口合并，不能平均各series的均值。
        + "  |> group(columns: [\"_start\"])\n"
        + "  |> reduce(identity: {count: 0, numericCount: 0, min: 0.0, max: 0.0, avg: 0.0}, fn: (r,"
        + " accumulator) => ({\n"
        + "    count: accumulator.count + r.count, numericCount: accumulator.numericCount +"
        + " r.numericCount,\n"
        + "    min: if r.numericCount == 0 then accumulator.min else if accumulator.numericCount =="
        + " 0 or r.min < accumulator.min then r.min else accumulator.min,\n"
        + "    max: if r.numericCount == 0 then accumulator.max else if accumulator.numericCount =="
        + " 0 or r.max > accumulator.max then r.max else accumulator.max,\n"
        + "    avg: if r.numericCount == 0 then accumulator.avg else weightedMean(a:"
        + " accumulator.avg, b: r.avg, left: accumulator.numericCount, right: r.numericCount)\n"
        + "  }))\n"
        + "  |> map(fn: (r) => ({r with _time: r._start}))\n"
        + "  |> group()\n"
        + "  |> sort(columns: [\"_time\"])\n"
        + "  |> limit(n: "
        + (query.getMaxPoints() + 1)
        + ")\n";
  }

  // Flux float() 会因无效文本或溢出直接终止整个查询；先验证十进制格式和双精度上界。
  // 2^1024 - 2^970 是舍入到 Infinity 的精确十进制阈值，比较有效数字避免转换自身溢出。
  private static final String FINITE_NUMBER_FLUX =
      """
import "strings"
weightedMean = (a, b, left, right) => {
  total = float(v: left + right)
  average = a * (float(v: left) / total) + b * (float(v: right) / total)
  low = if a < b then a else b
  high = if a > b then a else b
  return if left == 0 then b else if average < low then low else if average > high then high else average
}
finiteNumber = (text) => {
  value = strings.trimSpace(v: text)
  valid = value =~ /^[+-]?([0-9]+([.][0-9]*)?|[.][0-9]+)([eE][+-]?[0-9]+)?$/
  parts = strings.split(v: strings.toLower(v: value), t: "e")
  coefficient = strings.trimLeft(v: parts[0], cutset: "+-")
  exponentText = if length(arr: parts) > 1 then parts[1] else "0"
  exponentDigits = strings.trimLeft(v: strings.trimLeft(v: exponentText, cutset: "+-"), cutset: "0")
  exponentSign = if strings.hasPrefix(v: exponentText, prefix: "-") then -1 else 1
  exponent = if not valid or exponentDigits == "" then 0 else if strings.strlen(v: exponentDigits) > 6 then exponentSign * 10000000 else exponentSign * int(v: exponentDigits)
  digits = strings.trimLeft(v: strings.replaceAll(v: coefficient, t: ".", u: ""), cutset: "0")
  dot = strings.index(v: coefficient, substr: ".")
  decimals = if dot < 0 then 0 else strings.strlen(v: coefficient) - dot - 1
  order = exponent + strings.strlen(v: digits) - decimals - 1
  finite = valid and (digits == "" or order < 308 or (order == 308 and digits < "179769313486231580793728971405303415079934132710037826936173778980444968292764750946649017977587207096330286416692887910946555547851940402630657488671505820681908902000708383676273854845817711531764475730270069855571366959622842914819860834936475292719074168444365510704342711559699508093042880177904174497792"))
  return {valid: finite, value: if not finite or digits == "" or order < -324 then 0.0 else float(v: value)}
}
""";

  private QueryApi historyQueryApi() {
    if (historyQueryClient == null) {
      throw new IoTException("历史数据存储未连接，请稍后重试", 503);
    }
    return historyQueryClient.getQueryApi();
  }

  @Override
  public PageBean<IoTDeviceLogMetadataVO> queryLogMeta(LogQuery query) {
    try {
      String source = buildBaseFilterFlux(query);
      QueryApi queryApi = historyQueryApi();
      List<IoTDeviceLogMetadataVO> rows =
          parseMetadataResults(queryApi.query(buildHistoryPageFlux(source, query, true)), query);
      if (query.isCursorMode()) {
        return HistoryPageSupport.page(
            rows, query, IoTDeviceLogMetadataVO::getCreateTime, IoTDeviceLogMetadataVO::getId);
      }
      long total =
          parseCountResult(
              queryApi.query(
                  source + "  |> filter(fn: (r) => r._field == \"content\")\n  |> count()\n"));
      return new PageBean<>(rows, total, query.getPageSize(), query.getPageNum());
    } catch (Exception e) {
      log.error("InfluxDB查询设备元数据失败", e);
      throw new IoTException("历史记录查询失败或超时，请缩小时间范围后重试", 503);
    }
  }

  /** 构建基础过滤条件（不含pivot、排序和分页） */
  private String buildBaseFilterFlux(LogQuery logQuery) {
    StringBuilder flux = new StringBuilder();
    flux.append("from(bucket: \"").append(quoteFluxString(bucket)).append("\")\n");
    // 核心改造：适配Unix时间戳的动态时间范围
    flux.append(buildRangeClause(logQuery)).append("\n");
    flux.append("  |> filter(fn: (r) => ")
        .append(buildMetadataMeasurementFilter(logQuery))
        .append(")\n");

    // 添加过滤条件
    if (StrUtil.isNotBlank(logQuery.getProductKey())) {
      flux.append("  |> filter(fn: (r) => r.productKey == \"")
          .append(quoteFluxString(logQuery.getProductKey()))
          .append("\")\n");
    }
    if (StrUtil.isNotBlank(logQuery.getIotId())) {
      flux.append("  |> filter(fn: (r) => r.iotId == \"")
          .append(quoteFluxString(logQuery.getIotId()))
          .append("\")\n");
    }
    if (StrUtil.isNotBlank(logQuery.getDeviceId())) {
      flux.append("  |> filter(fn: (r) => r.deviceId == \"")
          .append(quoteFluxString(logQuery.getDeviceId()))
          .append("\")\n");
    }
    if (StrUtil.isNotBlank(logQuery.getMessageType())) {
      flux.append("  |> filter(fn: (r) => r.messageType == \"")
          .append(quoteFluxString(logQuery.getMessageType()))
          .append("\")\n");
    }
    if (StrUtil.isNotBlank(logQuery.getProperty())) {
      flux.append("  |> filter(fn: (r) => r.property == \"")
          .append(quoteFluxString(logQuery.getProperty()))
          .append("\")\n");
    }
    if (StrUtil.isNotBlank(logQuery.getEvent())) {
      flux.append("  |> filter(fn: (r) => r.event == \"")
          .append(quoteFluxString(logQuery.getEvent()))
          .append("\")\n");
    }

    return flux.toString();
  }

  /** 解析count查询结果，获取总记录数 */
  private long parseCountResult(List<FluxTable> tables) {
    if (tables == null || tables.isEmpty()) {
      log.warn("Count查询返回空结果");
      return 0L;
    }

    long total = 0L;
    for (FluxTable table : tables) {
      List<FluxRecord> records = table.getRecords();
      if (records != null) {
        for (FluxRecord record : records) {
          Object countValue = record.getValue();
          if (countValue instanceof Number) {
            total += ((Number) countValue).longValue();
          }
        }
      }
    }
    return total;
  }

  /** 解析元数据查询结果 使用pivot后,每条记录的所有field已经合并在一行,直接解析即可 */
  private List<IoTDeviceLogMetadataVO> parseMetadataResults(
      List<FluxTable> tables, LogQuery query) {
    List<IoTDeviceLogMetadataVO> resultList = new ArrayList<>();

    for (FluxTable table : tables) {
      for (FluxRecord record : table.getRecords()) {
        Instant time = record.getTime();
        if (time == null) continue;

        IoTDeviceLogMetadataVO vo = new IoTDeviceLogMetadataVO();
        Long id = getLongValue(record.getValueByKey("id"));
        if (query.isCursorMode() && id == null) {
          throw new IoTException("历史记录缺少稳定标识，请先完成存储兼容升级", 503);
        }
        vo.setId(id != null ? id : time.toEpochMilli());
        vo.setCreateTime(LocalDateTime.ofInstant(time, ZoneId.systemDefault()));

        // 从tags中提取字段
        vo.setProductKey(getStringValue(record.getValueByKey("productKey")));
        vo.setDeviceId(getStringValue(record.getValueByKey("deviceId")));
        vo.setIotId(getStringValue(record.getValueByKey("iotId")));
        vo.setMessageType(getStringValue(record.getValueByKey("messageType")));
        vo.setProperty(getStringValue(record.getValueByKey("property")));
        vo.setEvent(getStringValue(record.getValueByKey("event")));

        // pivot后,field值直接在record中可以通过getValueByKey获取
        vo.setContent(getStringValue(record.getValueByKey("content")));
        vo.setDeviceName(getStringValue(record.getValueByKey("deviceName")));
        vo.setExt1(getStringValue(record.getValueByKey("ext1")));
        vo.setExt2(getStringValue(record.getValueByKey("ext2")));
        vo.setExt3(getStringValue(record.getValueByKey("ext3")));

        resultList.add(vo);
      }
    }

    // 按时间降序排序(虽然Flux已经排序,但保险起见)
    // 保留Flux的时间、id双重倒序，不能在这里重新破坏同时间记录的顺序。

    return resultList;
  }

  @Override
  public IoTDeviceLog getRecentCommand(
      String productKey,
      String deviceId,
      String commandId,
      String messageType,
      Long createTime,
      Integer commandStatus) {
    DeviceLogQuery query =
        new DeviceLogQuery.Builder()
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

  @Override
  public List<IoTDeviceLog> queryDeviceLogs(DeviceLogQuery query) {
    if (influxDBClient == null) {
      log.warn("InfluxDB client未初始化");
      return Collections.emptyList();
    }
    try {
      StringBuilder flux = new StringBuilder();
      flux.append("from(bucket: \"").append(quoteFluxString(bucket)).append("\")\n");
      flux.append(buildRangeClause(query)).append("\n");
      flux.append("  |> filter(fn: (r) => r._measurement == \"device_log\")\n");
      appendDeviceLogQueryFilters(flux, query);
      flux.append(
          "  |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n");
      flux.append("  |> group()\n");
      appendDeviceLogFieldFilters(flux, query);
      flux.append("  |> sort(columns: [\"_time\"], desc: true)\n");
      if (query.getLimit() > 0) {
        flux.append("  |> limit(n: ").append(query.getLimit()).append(")\n");
      }

      List<FluxTable> tables = influxDBClient.getQueryApi().query(flux.toString());
      return parseDeviceLogEntities(tables);
    } catch (Exception e) {
      log.error("InfluxDB通用查询异常", e);
      return Collections.emptyList();
    }
  }

  /**
   * 构建动态时间范围子句（适配Unix时间戳）
   *
   * @param logQuery 查询参数（包含秒级Unix时间戳 beginCreateTime/endCreateTime）
   * @return Flux的range子句
   */
  private String buildRangeClause(LogQuery query) {
    String start = "-30d";
    String stop = "now()";
    if (query.getBeginCreateTime() != null) {
      start = RFC3339_FORMATTER.format(Instant.ofEpochSecond(query.getBeginCreateTime()));
    }
    Instant end =
        query.getEndCreateTime() == null ? null : Instant.ofEpochSecond(query.getEndCreateTime());
    if (query.isCursorMode() && query.getBeforeTime() != null) {
      Instant before = query.getBeforeTime().atZone(ZoneId.systemDefault()).toInstant();
      if (end == null || before.isBefore(end)) {
        end = before;
      }
    }
    // Flux stop是排他边界，接口与其他引擎约定结束时间包含在内。
    if (end != null) {
      stop = RFC3339_FORMATTER.format(end.plusNanos(1));
    }
    return String.format("  |> range(start: %s, stop: %s)", start, stop);
  }

  private String buildRangeClause(DeviceLogQuery query) {
    String start = "1970-01-01T00:00:00Z";
    String stop = "now()";
    if (query.getStartTime() != null && query.getStartTime() > 0) {
      start =
          RFC3339_FORMATTER.format(Instant.ofEpochMilli(resolveEpochMillis(query.getStartTime())));
    }
    if (query.getEndTime() != null && query.getEndTime() > 0) {
      stop = RFC3339_FORMATTER.format(Instant.ofEpochMilli(resolveEpochMillis(query.getEndTime())));
    }
    return String.format("  |> range(start: %s, stop: %s)", start, stop);
  }

  private void appendLogQueryFilters(StringBuilder flux, LogQuery logQuery) {
    appendTagFilter(flux, "productKey", logQuery.getProductKey());
    appendTagFilter(flux, "deviceId", logQuery.getDeviceId());
    appendTagFilter(flux, "iotId", logQuery.getIotId());
    appendTagFilter(flux, "messageType", logQuery.getMessageType());
    appendTagFilter(flux, "event", logQuery.getEvent());
  }

  private void appendDeviceLogQueryFilters(StringBuilder flux, DeviceLogQuery query) {
    appendTagFilter(flux, "productKey", query.getProductKey());
    appendTagFilter(flux, "deviceId", query.getDeviceId());
    appendTagFilter(flux, "iotId", query.getIotId());
    appendTagFilter(flux, "messageType", query.getMessageType());
    appendTagFilter(flux, "event", query.getEventType());
  }

  private void appendDeviceLogFieldFilters(StringBuilder flux, DeviceLogQuery query) {
    if (StrUtil.isNotBlank(query.getCommandId())) {
      flux.append("  |> filter(fn: (r) => r.commandId == \"")
          .append(quoteFluxString(query.getCommandId()))
          .append("\")\n");
    }
    if (query.getCommandStatus() != null) {
      flux.append("  |> filter(fn: (r) => r.commandStatus == ")
          .append(query.getCommandStatus())
          .append(")\n");
    }
    if (query.getLogId() != null) {
      flux.append("  |> filter(fn: (r) => r.id == ").append(query.getLogId()).append(")\n");
    }
  }

  private void appendTagFilter(StringBuilder flux, String field, String value) {
    if (StrUtil.isBlank(value)) {
      return;
    }
    flux.append("  |> filter(fn: (r) => r.")
        .append(field)
        .append(" == \"")
        .append(quoteFluxString(value))
        .append("\")\n");
  }

  private String buildMetadataMeasurementFilter(LogQuery logQuery) {
    boolean wantProperty =
        MessageType.PROPERTIES.name().equalsIgnoreCase(logQuery.getMessageType())
            || StrUtil.isNotBlank(logQuery.getProperty());
    boolean wantEvent =
        MessageType.EVENT.name().equalsIgnoreCase(logQuery.getMessageType())
            || StrUtil.isNotBlank(logQuery.getEvent());

    if (wantProperty && !wantEvent) {
      return "r._measurement == \"property_metadata\"";
    }
    if (wantEvent && !wantProperty) {
      return "r._measurement == \"event_metadata\"";
    }
    return "r._measurement == \"property_metadata\" or r._measurement == \"event_metadata\"";
  }

  private List<IoTDeviceLog> parseDeviceLogEntities(List<FluxTable> tables) {
    List<IoTDeviceLog> result = new ArrayList<>();
    for (IoTDeviceLogVO vo : parseDeviceLogResults(tables, null)) {
      result.add(
          IoTDeviceLog.builder()
              .id(vo.getId())
              .iotId(vo.getIotId())
              .deviceId(vo.getDeviceId())
              .productKey(vo.getProductKey())
              .deviceName(vo.getDeviceName())
              .messageType(vo.getMessageType())
              .commandId(vo.getCommandId())
              .commandStatus(vo.getCommandStatus())
              .event(vo.getEvent())
              .point(vo.getPoint())
              .content(vo.getContent())
              .createTime(vo.getCreateTime())
              .build());
    }
    return result;
  }

  private Instant findDeviceLogTimestamp(Long id, LogQuery logQuery) {
    StringBuilder flux = new StringBuilder();
    flux.append("from(bucket: \"").append(quoteFluxString(bucket)).append("\")\n");
    flux.append("  |> range(start: 1970-01-01T00:00:00Z)\n");
    flux.append("  |> filter(fn: (r) => r._measurement == \"device_log\")\n");
    appendLogQueryFilters(flux, logQuery);
    flux.append("  |> filter(fn: (r) => r._field == \"id\")\n");
    flux.append("  |> filter(fn: (r) => r._value == ").append(id).append(")\n");
    flux.append("  |> limit(n: 1)\n");

    List<FluxTable> tables = influxDBClient.getQueryApi().query(flux.toString());
    for (FluxTable table : tables) {
      for (FluxRecord record : table.getRecords()) {
        if (record.getTime() != null) {
          return record.getTime();
        }
      }
    }
    return null;
  }

  private long ensureMetadataId(BaseUPRequest up, String property, String event) {
    return SQenGenId.nextId();
  }

  private Instant resolvePointTime(BaseUPRequest up) {
    return Instant.ofEpochMilli(resolveEpochMillis(up.getTime()));
  }

  private int resolveMaxStorage(LogStorePolicyDTO.StorePolicy storePolicy) {
    if (storePolicy == null || storePolicy.getMaxStorage() <= 0) {
      return IoTConstant.maxStorage;
    }
    return storePolicy.getMaxStorage();
  }

  private String quoteFluxString(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  private String escapeDeletePredicate(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  /** 安全获取字符串值 */
  private String getStringValue(Object value) {
    if (value == null) {
      return null;
    }
    return value.toString();
  }

  /** 安全获取整型值 */
  private Integer getIntValue(Object value) {
    if (value == null) {
      return null;
    }

    try {
      if (value instanceof Number) {
        return ((Number) value).intValue();
      }
      return Integer.parseInt(value.toString());
    } catch (NumberFormatException e) {
      log.warn("解析整型失败: {}", value);
      return null;
    }
  }

  private Long getLongValue(Object value) {
    if (value == null) {
      return null;
    }

    try {
      if (value instanceof Number) {
        return ((Number) value).longValue();
      }
      return Long.parseLong(value.toString());
    } catch (NumberFormatException e) {
      log.warn("解析长整型失败: {}", value);
      return null;
    }
  }

  @Override
  public String getPolicy() {
    return storePolicy;
  }

  @Override
  public JSONObject configMetadata() {
    JSONObject config = new JSONObject();
    config.put("url", url);
    config.put("org", organization);
    config.put("bucket", bucket);
    return config;
  }
}
