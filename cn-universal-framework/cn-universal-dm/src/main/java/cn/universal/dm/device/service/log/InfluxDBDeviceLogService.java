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
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.core.metadata.AbstractPropertyMetadata;
import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.dto.LogStorePolicyDTO;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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

  @Resource private IoTDeviceService iotDeviceService;

  @Resource private IoTProductDeviceService iotProductDeviceService;

  private InfluxDBClient influxDBClient;
  // 时间格式化器：适配InfluxDB的RFC3339格式
  private static final DateTimeFormatter RFC3339_FORMATTER =
      DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault());

  @PostConstruct
  public void initClient() {
    try {
      influxDBClient = InfluxDBClientFactory.create(url, token.toCharArray(), organization, bucket);

      // 测试连接
      if (influxDBClient.ping()) {
        log.info("初始化InfluxDB连接成功，url={}, org={}, bucket={}", url, organization, bucket);
      } else {
        log.warn("InfluxDB连接可能异常，请检查配置");
      }
    } catch (Exception e) {
      log.error("初始化InfluxDB连接失败", e);
    }
  }

  @PreDestroy
  public void closeClient() {
    if (influxDBClient != null) {
      try {
        influxDBClient.close();
        log.info("InfluxDB连接已关闭");
      } catch (Exception e) {
        log.error("关闭InfluxDB连接失败", e);
      }
    }
  }

  @Override
  @Async("taskExecutor")
  public void saveDeviceLog(BaseUPRequest upRequest, IoTDeviceDTO noUse, IoTProduct ioTProduct) {
    /** 产品数据存储策略，不为空则保存日志 */
    if (StrUtil.isNotBlank(ioTProduct.getStorePolicy())) {
      IoTDeviceDTO ioTDeviceDTO =
          iotDeviceService.selectDevInstanceBO(upRequest.getProductKey(), upRequest.getDeviceId());
      try {
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
          LogStorePolicyDTO productLogStorePolicy =
              iotProductDeviceService.getProductLogStorePolicy(ioTProduct.getProductKey());
          saveLogStorePolicy(productLogStorePolicy, upRequest, ioTProduct);
        }
      } catch (Exception e) {
        log.error("InfluxDB保存设备属性扩展日志报错={}", e);
      }
    }
  }

  @Override
  public void saveDeviceLog(
      IoTDeviceLog ioTDeviceLog, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {
    /** 产品数据存储策略，不为空则保存日志 */
    if (StrUtil.isNotBlank(ioTProduct.getStorePolicy())) {
      try {
        saveDeviceLogToInfluxDB(ioTDeviceLog);
        log.debug("InfluxDB插入设备日志成功，deviceId={}", ioTDeviceDTO.getDeviceId());
      } catch (Exception e) {
        log.error("保存设备日志到InfluxDB报错={}", e);
      }
    }
  }

  /** 根据存储策略保存元数据日志 支持属性和事件的详细信息存储 */
  private void saveLogStorePolicy(
      LogStorePolicyDTO logStorePolicyDTO, BaseUPRequest up, IoTProduct ioTProduct) {
    // 处理属性消息
    if (MessageType.PROPERTIES.equals(up.getMessageType())
        && up.getProperties() != null
        && CollectionUtil.isNotEmpty(logStorePolicyDTO.getProperties())) {
      up.getProperties()
          .forEach(
              (key, value) -> {
                if (logStorePolicyDTO.getProperties().containsKey(key)) {
                  try {
                    AbstractPropertyMetadata propertyOrNull =
                        getDeviceMetadata(ioTProduct.getMetadata()).getPropertyOrNull(key);
                    IoTDevicePropertiesBO ioTDevicePropertiesBO = new IoTDevicePropertiesBO();
                    ioTDevicePropertiesBO.withValue(propertyOrNull, value);

                    // 构建元数据并保存
                    String property = key;
                    String propertyName = ioTDevicePropertiesBO.getPropertyName();
                    String formatValue = ioTDevicePropertiesBO.getFormatValue();
                    String symbol = ioTDevicePropertiesBO.getSymbol();
                    String content = StrUtil.str(value, CharsetUtil.charset("UTF-8"));

                    savePropertyMetadataToInfluxDB(
                        up, property, content, propertyName, formatValue, symbol);

                    // 注意：InfluxDB 通过保留策略自动清理旧数据，不需要手动删除超出maxStorage的记录
                    // 建议在 InfluxDB 中配置 retention policy，例如：30天自动删除
                  } catch (Exception e) {
                    log.error("InfluxDB保存属性元数据失败: iotId={}, property={}", up.getIotId(), key, e);
                  }
                }
              });
    }

    // 处理事件消息
    if (MessageType.EVENT.equals(up.getMessageType())) {
      try {
        int maxStorage = 10;
        if (CollectionUtil.isNotEmpty(logStorePolicyDTO.getEvent())
            && logStorePolicyDTO.getEvent().containsKey(up.getEvent())) {
          maxStorage = logStorePolicyDTO.getEvent().get(up.getEvent()).getMaxStorage();
        }

        saveEventMetadataToInfluxDB(up, maxStorage);
        // 注意：InfluxDB 通过保留策略自动清理旧数据，不需要手动删除超出maxStorage的记录
      } catch (Exception e) {
        log.error("InfluxDB保存事件元数据失败: iotId={}, event={}", up.getIotId(), up.getEvent(), e);
      }
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
        Point.measurement("property_metadata")
            .addTag("productKey", up.getProductKey())
            .addTag("deviceId", up.getDeviceId())
            .addTag("iotId", up.getIotId())
            .addTag("messageType", MessageType.PROPERTIES.name())
            .addTag("property", property)
            .addField("content", content != null ? content : "")
            .addField("deviceName", up.getDeviceName() != null ? up.getDeviceName() : "")
            .addField("ext1", propertyName != null ? propertyName : "")
            .addField("ext2", formatValue != null ? formatValue : "")
            .addField("ext3", symbol != null ? symbol : "")
            .time(Instant.now(), WritePrecision.MS);

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

    Point point =
        Point.measurement("event_metadata")
            .addTag("productKey", up.getProductKey())
            .addTag("deviceId", up.getDeviceId())
            .addTag("iotId", up.getIotId())
            .addTag("messageType", MessageType.EVENT.name())
            .addTag("event", up.getEvent())
            .addField("content", up.getEventName() != null ? up.getEventName() : "")
            .addField("deviceName", up.getDeviceName() != null ? up.getDeviceName() : "")
            .addField("ext1", JSONUtil.toJsonStr(up.getData()))
            .addField("maxStorage", maxStorage)
            .time(Instant.now(), WritePrecision.MS);

    try {
      writeApi.writePoint(point);
      log.debug("InfluxDB插入事件元数据成功: event={}, iotId={}", up.getEvent(), up.getIotId());
    } catch (Exception e) {
      log.error("InfluxDB插入事件元数据失败: event={}, iotId={}", up.getEvent(), up.getIotId(), e);
      throw e;
    }
  }

  /**
   * 保存设备日志到InfluxDB measurement: device_log tags: productKey, deviceId, iotId, messageType, event
   * fields: content, deviceName, commandId, commandStatus, point
   *
   * <p>注意:deviceName为必填字段,用于count统计;content可能为空
   */
  private void saveDeviceLogToInfluxDB(IoTDeviceLog ioTDeviceLog) throws Exception {
    if (influxDBClient == null) {
      throw new RuntimeException("InfluxDB client未初始化");
    }

    WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

    // 时间戳（毫秒）
    Instant timestamp = ioTDeviceLog.getCreateTime().atZone(ZoneId.systemDefault()).toInstant();

    Point point =
        Point.measurement("device_log")
            .addTag("productKey", ioTDeviceLog.getProductKey())
            .addTag("deviceId", ioTDeviceLog.getDeviceId())
            .addTag("iotId", ioTDeviceLog.getIotId())
            .addTag("messageType", ioTDeviceLog.getMessageType())
            .addTag("event", ioTDeviceLog.getEvent() != null ? ioTDeviceLog.getEvent() : "")
            .addField("content", ioTDeviceLog.getContent() != null ? ioTDeviceLog.getContent() : "")
            .addField(
                "deviceName",
                ioTDeviceLog.getDeviceName() != null ? ioTDeviceLog.getDeviceName() : "")
            .addField(
                "commandId", ioTDeviceLog.getCommandId() != null ? ioTDeviceLog.getCommandId() : "")
            .addField(
                "commandStatus",
                ioTDeviceLog.getCommandStatus() != null ? ioTDeviceLog.getCommandStatus() : 0)
            .addField("point", ioTDeviceLog.getPoint() != null ? ioTDeviceLog.getPoint() : "")
            .time(timestamp, WritePrecision.MS);

    try {
      writeApi.writePoint(point);
      log.debug(
          "InfluxDB插入设备日志成功: productKey={}, deviceId={}, timestamp={}",
          ioTDeviceLog.getProductKey(),
          ioTDeviceLog.getDeviceId(),
          timestamp);
    } catch (Exception e) {
      log.error(
          "InfluxDB插入设备日志失败: productKey={}, deviceId={}, error={}",
          ioTDeviceLog.getProductKey(),
          ioTDeviceLog.getDeviceId(),
          e.getMessage());
      throw e;
    }
  }

  @Override
  public PageBean<IoTDeviceLogVO> pageList(LogQuery logQuery) {
    try {
      if (influxDBClient == null) {
        log.warn("InfluxDB client未初始化");
        return new PageBean<>(new ArrayList<>(), 0L, logQuery.getPageSize(), logQuery.getPageNum());
      }

      // 构建 Flux 查询语句
      StringBuilder flux = new StringBuilder();
      flux.append("from(bucket: \"").append(bucket).append("\")\n");
      // 动态时间范围：适配Unix时间戳
      flux.append(buildRangeClause(logQuery)).append("\n");
      flux.append("  |> filter(fn: (r) => r._measurement == \"device_log\")\n");

      // 添加过滤条件
      if (StrUtil.isNotBlank(logQuery.getProductKey())) {
        flux.append("  |> filter(fn: (r) => r.productKey == \"")
            .append(logQuery.getProductKey())
            .append("\")\n");
      }
      if (StrUtil.isNotBlank(logQuery.getDeviceId())) {
        flux.append("  |> filter(fn: (r) => r.deviceId == \"")
            .append(logQuery.getDeviceId())
            .append("\")\n");
      }
      if (StrUtil.isNotBlank(logQuery.getIotId())) {
        flux.append("  |> filter(fn: (r) => r.iotId == \"")
            .append(logQuery.getIotId())
            .append("\")\n");
      }
      if (StrUtil.isNotBlank(logQuery.getMessageType())) {
        flux.append("  |> filter(fn: (r) => r.messageType == \"")
            .append(logQuery.getMessageType())
            .append("\")\n");
      }
      if (StrUtil.isNotBlank(logQuery.getEvent())) {
        flux.append("  |> filter(fn: (r) => r.event == \"")
            .append(logQuery.getEvent())
            .append("\")\n");
      }

      // 关键修复:使用pivot将多个field合并为一行记录
      flux.append(
          "  |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n");
      // 取消所有分组,确保数据合并为一个table
      flux.append("  |> group()\n");
      // 删除系统列,避免干扰
      flux.append("  |> drop(columns: [\"_start\", \"_stop\", \"_measurement\"])\n");
      flux.append("  |> sort(columns: [\"_time\"], desc: true)\n");

      // InfluxDB 分页实现：limit + offset
      int offset = (logQuery.getPageNum() - 1) * logQuery.getPageSize();
      flux.append("  |> limit(n: ")
          .append(logQuery.getPageSize())
          .append(", offset: ")
          .append(offset)
          .append(")\n");

      log.info("InfluxDB分页查询Flux: {}", flux);

      QueryApi queryApi = influxDBClient.getQueryApi();
      List<FluxTable> tables = queryApi.query(flux.toString());

      log.info(
          "InfluxDB返回tables数量: {}, 每个table的records数: {}",
          tables.size(),
          tables.isEmpty() ? 0 : tables.stream().mapToInt(t -> t.getRecords().size()).sum());

      // 解析结果
      List<IoTDeviceLogVO> resultList = parseDeviceLogResults(tables, logQuery);

      // 查询总数（使用count聚合）
      long total = queryTotalCount(logQuery);

      log.info("InfluxDB查询成功，返回 {} 条记录，总数 {}", resultList.size(), total);

      return new PageBean<>(resultList, total, logQuery.getPageSize(), logQuery.getPageNum());

    } catch (Exception e) {
      log.error("InfluxDB查询设备日志失败", e);
      return new PageBean<>(new ArrayList<>(), 0L, logQuery.getPageSize(), logQuery.getPageNum());
    }
  }

  /** 查询总记录数(适配Unix时间戳) */
  private long queryTotalCount(LogQuery logQuery) {
    try {
      StringBuilder flux = new StringBuilder();
      flux.append("from(bucket: \"").append(bucket).append("\")\n");
      // 动态时间范围:与分页查询保持一致
      flux.append(buildRangeClause(logQuery)).append("\n");
      flux.append("  |> filter(fn: (r) => r._measurement == \"device_log\")\n");

      // 添加过滤条件
      if (StrUtil.isNotBlank(logQuery.getProductKey())) {
        flux.append("  |> filter(fn: (r) => r.productKey == \"")
            .append(logQuery.getProductKey())
            .append("\")\n");
      }
      if (StrUtil.isNotBlank(logQuery.getDeviceId())) {
        flux.append("  |> filter(fn: (r) => r.deviceId == \"")
            .append(logQuery.getDeviceId())
            .append("\")\n");
      }
      if (StrUtil.isNotBlank(logQuery.getIotId())) {
        flux.append("  |> filter(fn: (r) => r.iotId == \"")
            .append(logQuery.getIotId())
            .append("\")\n");
      }
      if (StrUtil.isNotBlank(logQuery.getMessageType())) {
        flux.append("  |> filter(fn: (r) => r.messageType == \"")
            .append(logQuery.getMessageType())
            .append("\")\n");
      }
      if (StrUtil.isNotBlank(logQuery.getEvent())) {
        flux.append("  |> filter(fn: (r) => r.event == \"")
            .append(logQuery.getEvent())
            .append("\")\n");
      }

      // 关键修复:统计deviceName字段(必填字段)避免重复计数
      // 注意:不能用content因为可能为空,应该用productKey/deviceId等必填字段对应的field
      flux.append("  |> filter(fn: (r) => r._field == \"deviceName\")\n");
      flux.append("  |> count()\n");

      QueryApi queryApi = influxDBClient.getQueryApi();
      List<FluxTable> tables = queryApi.query(flux.toString());

      // 累加所有table的count结果
      long totalCount = 0L;
      for (FluxTable table : tables) {
        for (FluxRecord record : table.getRecords()) {
          Object value = record.getValue();
          if (value != null) {
            totalCount += ((Number) value).longValue();
          }
        }
      }

      return totalCount;
    } catch (Exception e) {
      log.warn("InfluxDB查询总数失败: {}", e.getMessage());
      return 0L;
    }
  }

  /** 解析设备日志查询结果 使用pivot后,每条记录的所有field已经合并在一行,直接解析即可 */
  private List<IoTDeviceLogVO> parseDeviceLogResults(List<FluxTable> tables, LogQuery logQuery) {
    List<IoTDeviceLogVO> resultList = new ArrayList<>();

    for (FluxTable table : tables) {
      for (FluxRecord record : table.getRecords()) {
        Instant time = record.getTime();
        if (time == null) continue;

        IoTDeviceLogVO vo = new IoTDeviceLogVO();
        vo.setId(time.toEpochMilli());
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
    resultList.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));

    return resultList;
  }

  @Override
  public IoTDeviceLogVO queryById(LogQuery logQuery) {
    try {
      if (influxDBClient == null || logQuery.getId() == null) {
        log.warn("InfluxDB client未初始化或ID为空");
        return null;
      }

      // InfluxDB 使用时间戳作为主键查询
      Instant timestamp = Instant.ofEpochMilli(Long.parseLong(logQuery.getId()));

      StringBuilder flux = new StringBuilder();
      flux.append("from(bucket: \"").append(bucket).append("\")\n");
      flux.append("  |> range(start: ")
          .append(timestamp.toString())
          .append(", stop: ")
          .append(timestamp.plusMillis(1).toString())
          .append(")\n");
      flux.append("  |> filter(fn: (r) => r._measurement == \"device_log\")\n");

      if (StrUtil.isNotBlank(logQuery.getIotId())) {
        flux.append("  |> filter(fn: (r) => r.iotId == \"")
            .append(logQuery.getIotId())
            .append("\")\n");
      }

      // 添加pivot合并field
      flux.append(
          "  |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n");
      // 取消所有分组
      flux.append("  |> group()\n");

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

    for (IoTDeviceEvents devEvent : list) {
      try {
        if (influxDBClient == null) {
          log.warn("InfluxDB client未初始化");
          continue;
        }

        // 查询事件元数据统计
        StringBuilder flux = new StringBuilder();
        flux.append("from(bucket: \"").append(bucket).append("\")\n");
        // 这里也可以替换为 buildRangeClause，但该方法无LogQuery入参，暂保留默认30天
        flux.append("  |> range(start: -30d)\n");
        flux.append("  |> filter(fn: (r) => r._measurement == \"event_metadata\")\n");
        flux.append("  |> filter(fn: (r) => r.iotId == \"").append(iotId).append("\")\n");
        flux.append("  |> filter(fn: (r) => r.event == \"")
            .append(devEvent.getId())
            .append("\")\n");
        flux.append("  |> filter(fn: (r) => r._field == \"content\")\n"); // 只统计一个字段避免重复计数
        flux.append("  |> count()\n");

        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux.toString());

        long count = 0;
        if (!tables.isEmpty() && !tables.get(0).getRecords().isEmpty()) {
          Object value = tables.get(0).getRecords().get(0).getValue();
          count = value != null ? ((Number) value).longValue() : 0L;
        }

        devEvent.setQty(count >= 100 ? "99+" : String.valueOf(count));

        // 查询最后时间
        flux = new StringBuilder();
        flux.append("from(bucket: \"").append(bucket).append("\")\n");
        flux.append("  |> range(start: -30d)\n");
        flux.append("  |> filter(fn: (r) => r._measurement == \"event_metadata\")\n");
        flux.append("  |> filter(fn: (r) => r.iotId == \"").append(iotId).append("\")\n");
        flux.append("  |> filter(fn: (r) => r.event == \"")
            .append(devEvent.getId())
            .append("\")\n");
        flux.append("  |> filter(fn: (r) => r._field == \"content\")\n");
        flux.append("  |> sort(columns: [\"_time\"], desc: true)\n");
        flux.append("  |> limit(n: 1)\n");

        tables = queryApi.query(flux.toString());
        if (!tables.isEmpty() && !tables.get(0).getRecords().isEmpty()) {
          Instant time = tables.get(0).getRecords().get(0).getTime();
          if (time != null) {
            devEvent.setTime(time.toString());
          }
        }

      } catch (Exception e) {
        log.error("InfluxDB查询事件统计失败, productKey={}, iotId={}", productKey, iotId, e);
        devEvent.setQty("0");
        devEvent.setTime("");
      }
    }

    return new PageBean<>(list, 100L, 1, 100);
  }

  @Override
  public PageBean<IoTDeviceLogMetadataVO> queryLogMeta(LogQuery logQuery) {
    try {
      if (influxDBClient == null) {
        log.warn("InfluxDB client未初始化");
        return new PageBean<>(new ArrayList<>(), 0L, logQuery.getPageSize(), logQuery.getPageNum());
      }

      // 根据查询条件选择 measurement
      String measurement =
          StrUtil.isNotBlank(logQuery.getProperty()) ? "property_metadata" : "event_metadata";

      // 构建基础过滤条件（不含pivot和分页）
      String baseFilterFlux = buildBaseFilterFlux(logQuery, measurement);

      QueryApi queryApi = influxDBClient.getQueryApi();

      // 1. 先查询总数（pivot后添加计数列，再进行count）
      // 关键：pivot后添加record_count=1列，然后对该列count
      String countFlux =
          baseFilterFlux
              + "  |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n"
              + "  |> group()\n" // 合并所有分组
              + "  |> map(fn: (r) => ({r with record_count: 1}))\n" // 添加计数列
              + "  |> count(column: \"record_count\")\n"; // 统计记录数
      log.debug("InfluxDB查询元数据总数Flux: {}", countFlux);
      List<FluxTable> countTables = queryApi.query(countFlux);
      long total = parseCountResult(countTables);

      // 2. 再查询分页数据（包含pivot转换）
      int offset = (logQuery.getPageNum() - 1) * logQuery.getPageSize();
      String dataFlux =
          baseFilterFlux
              + "  |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n"
              + "  |> group()\n"
              + "  |> sort(columns: [\"_time\"], desc: true)\n"
              + "  |> limit(n: "
              + logQuery.getPageSize()
              + ", offset: "
              + offset
              + ")\n";
      log.debug("InfluxDB查询元数据分页Flux: {}", dataFlux);
      List<FluxTable> dataTables = queryApi.query(dataFlux);

      // 解析结果
      List<IoTDeviceLogMetadataVO> resultList = parseMetadataResults(dataTables);

      log.info("InfluxDB查询元数据成功，总数: {}, 当前页: {} 条记录", total, resultList.size());

      return new PageBean<>(resultList, total, logQuery.getPageSize(), logQuery.getPageNum());

    } catch (Exception e) {
      log.error("InfluxDB查询设备元数据失败", e);
      return new PageBean<>(new ArrayList<>(), 0L, logQuery.getPageSize(), logQuery.getPageNum());
    }
  }

  /** 构建基础过滤条件（不含pivot、排序和分页） */
  private String buildBaseFilterFlux(LogQuery logQuery, String measurement) {
    StringBuilder flux = new StringBuilder();
    flux.append("from(bucket: \"").append(bucket).append("\")\n");
    // 核心改造：适配Unix时间戳的动态时间范围
    flux.append(buildRangeClause(logQuery)).append("\n");
    flux.append("  |> filter(fn: (r) => r._measurement == \"").append(measurement).append("\")\n");

    // 添加过滤条件
    if (StrUtil.isNotBlank(logQuery.getIotId())) {
      flux.append("  |> filter(fn: (r) => r.iotId == \"")
          .append(logQuery.getIotId())
          .append("\")\n");
    }
    if (StrUtil.isNotBlank(logQuery.getDeviceId())) {
      flux.append("  |> filter(fn: (r) => r.deviceId == \"")
          .append(logQuery.getDeviceId())
          .append("\")\n");
    }
    if (StrUtil.isNotBlank(logQuery.getProperty())) {
      flux.append("  |> filter(fn: (r) => r.property == \"")
          .append(logQuery.getProperty())
          .append("\")\n");
    }
    if (StrUtil.isNotBlank(logQuery.getEvent())) {
      flux.append("  |> filter(fn: (r) => r.event == \"")
          .append(logQuery.getEvent())
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

    for (FluxTable table : tables) {
      List<FluxRecord> records = table.getRecords();
      if (records != null && !records.isEmpty()) {
        FluxRecord record = records.get(0);

        // count(column: "record_count") 后，结果在 record_count 字段中
        Object countValue = record.getValueByKey("record_count");
        if (countValue instanceof Number) {
          long total = ((Number) countValue).longValue();
          log.debug("解析到总记录数: {}", total);
          return total;
        } else {
          log.warn("Count结果类型异常: {}", countValue);
        }
      }
    }

    log.warn("无法从Count查询结果中解析总数");
    return 0L;
  }

  /** 解析元数据查询结果 使用pivot后,每条记录的所有field已经合并在一行,直接解析即可 */
  private List<IoTDeviceLogMetadataVO> parseMetadataResults(List<FluxTable> tables) {
    List<IoTDeviceLogMetadataVO> resultList = new ArrayList<>();

    for (FluxTable table : tables) {
      for (FluxRecord record : table.getRecords()) {
        Instant time = record.getTime();
        if (time == null) continue;

        IoTDeviceLogMetadataVO vo = new IoTDeviceLogMetadataVO();
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
    resultList.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));

    return resultList;
  }

  /**
   * 构建动态时间范围子句（适配Unix时间戳）
   *
   * @param logQuery 查询参数（包含秒级Unix时间戳 beginCreateTime/endCreateTime）
   * @return Flux的range子句
   */
  private String buildRangeClause(LogQuery logQuery) {
    // 1. 定义默认时间范围（最近30天）
    String defaultStart = "-30d";
    String defaultStop = "now()";

    // 2. 解析Unix时间戳（秒级）并转换为RFC3339格式
    String start = defaultStart;
    String stop = defaultStop;

    try {
      // 处理开始时间：beginCreateTime（秒级Unix戳）
      if (logQuery.getBeginCreateTime() != null && logQuery.getBeginCreateTime() > 0) {
        // 秒级转毫秒级，再转为Instant
        Instant startInstant = Instant.ofEpochSecond(logQuery.getBeginCreateTime());
        start = RFC3339_FORMATTER.format(startInstant);
      }

      // 处理结束时间：endCreateTime（秒级Unix戳）
      if (logQuery.getEndCreateTime() != null && logQuery.getEndCreateTime() > 0) {
        Instant endInstant = Instant.ofEpochSecond(logQuery.getEndCreateTime());
        stop = RFC3339_FORMATTER.format(endInstant);
      }

      log.debug("InfluxDB时间范围：start={}, stop={}", start, stop);
    } catch (Exception e) {
      log.warn("Unix时间戳解析失败，使用默认30天范围: {}", e.getMessage());
      start = defaultStart;
      stop = defaultStop;
    }

    return String.format("  |> range(start: %s, stop: %s)", start, stop);
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
