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
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.core.message.UPRequest;
import cn.universal.core.metadata.AbstractPropertyMetadata;
import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.dto.LogStorePolicyDTO;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTDeviceLogMetadata.IoTDeviceLogMetadataBuilder;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

  @Resource private IoTProductDeviceService iotProductDeviceService;

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

  @Value(value = "${clickhouse.batch.size:1000}")
  private int batchSize;

  @Resource private IoTDeviceService iotDeviceService;

  private HikariDataSource dataSource;
  private JdbcTemplate jdbcTemplate;

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
      config.setLeakDetectionThreshold(60000); // 60秒连接泄漏检测

      // 连接池名称
      config.setPoolName("ClickHouse-Pool");

      // ClickHouse 特定配置
      config.setConnectionTestQuery("SELECT 1");
      config.setAutoCommit(true);
      config.setRegisterMbeans(false); // 关闭JMX监控

      // 创建数据源
      dataSource = new HikariDataSource(config);
      jdbcTemplate = new JdbcTemplate(dataSource);

      // 测试连接
      jdbcTemplate.queryForObject("SELECT 1", Integer.class);

      log.info(
          "初始化 ClickHouse 数据源成功，连接池配置: maxSize={}, minIdle={}, batchSize={}",
          maxPoolSize,
          minIdle,
          batchSize);

      // 自动创建表
      createTablesIfNotExist();
      log.info("ClickHouse 表结构检查/创建完成");

    } catch (Exception e) {
      log.error("初始化 ClickHouse 数据源失败", e);
      throw new RuntimeException("初始化 ClickHouse 数据源失败", e);
    }
  }

  @PreDestroy
  public void destroy() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
      log.info("ClickHouse 数据源已关闭");
    }
  }

  /** 自动创建表（如果不存在） */
  private void createTablesIfNotExist() {
    try {
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
          SETTINGS index_granularity = 8192
          """
              .formatted(devLogTableName);

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
          SETTINGS index_granularity = 8192
          """
              .formatted(devLogMetaTableName);

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
    /** 产品数据存储策略，不为空则保存日志 */
    if (StrUtil.isNotBlank(ioTProduct.getStorePolicy())) {
      IoTDeviceDTO ioTDeviceDTO =
          iotDeviceService.selectDevInstanceBO(upRequest.getProductKey(), upRequest.getDeviceId());
      try {
        IoTDeviceLog ioTDeviceLog = build(upRequest, ioTDeviceDTO);
        insertDeviceLog(ioTDeviceLog);
        log.debug("ClickHouse 插入设备日志成功");
      } catch (Exception e) {
        log.error("保存设备日志报错", e);
      }
      String storePolicyConfiguration = ioTProduct.getStorePolicyConfiguration();
      try {
        if (StrUtil.isNotBlank(storePolicyConfiguration)) {
          LogStorePolicyDTO productLogStorePolicy =
              iotProductDeviceService.getProductLogStorePolicy(ioTProduct.getProductKey());
          saveLogStorePolicy(productLogStorePolicy, upRequest, ioTProduct);
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
        insertDeviceLog(ioTDeviceLog);
        log.debug("ClickHouse 插入设备日志成功");
      } catch (Exception e) {
        log.error("保存设备日志报错", e);
      }
    }
  }

  /** 插入设备日志 */
  private void insertDeviceLog(IoTDeviceLog log) {
    String sql =
        "INSERT INTO "
            + devLogTableName
            + " (id, iot_id, device_id, product_key, device_name, message_type, "
            + "command_id, command_status, event, content, point, create_time) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    jdbcTemplate.update(
        sql,
        log.getId(),
        log.getIotId(),
        log.getDeviceId(),
        log.getProductKey(),
        log.getDeviceName(),
        log.getMessageType(),
        log.getCommandId(),
        log.getCommandStatus(),
        log.getEvent(),
        log.getContent(),
        log.getPoint(),
        log.getCreateTime() != null
            ? Timestamp.valueOf(log.getCreateTime())
            : Timestamp.valueOf(LocalDateTime.now()));
  }

  private void saveLogStorePolicy(
      LogStorePolicyDTO logStorePolicyDTO, UPRequest up, IoTProduct ioTProduct) {
    List<Object[]> batchArgs = new ArrayList<>();

    if (MessageType.PROPERTIES.equals(up.getMessageType()) && up.getProperties() != null) {
      up.getProperties()
          .forEach(
              (key, value) -> {
                if (logStorePolicyDTO.getProperties().containsKey(key)) {
                  AbstractPropertyMetadata propertyOrNull =
                      getDeviceMetadata(ioTProduct.getMetadata()).getPropertyOrNull(key);
                  IoTDevicePropertiesBO ioTDevicePropertiesBO = new IoTDevicePropertiesBO();
                  ioTDevicePropertiesBO.withValue(propertyOrNull, value);

                  IoTDeviceLogMetadataBuilder builder = builder(up);
                  builder.id(IdUtil.getSnowflake().nextId());
                  builder.property(key);
                  builder.content(StrUtil.str(value, CharsetUtil.charset("UTF-8")));
                  builder.ext1(ioTDevicePropertiesBO.getPropertyName());
                  builder.ext2(ioTDevicePropertiesBO.getFormatValue());
                  builder.ext3(ioTDevicePropertiesBO.getSymbol());

                  var metadata = builder.build();
                  batchArgs.add(
                      new Object[] {
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
                      });
                }
              });
    }
    if (MessageType.EVENT.equals(up.getMessageType())
        && logStorePolicyDTO.getEvent().containsKey(up.getEvent())) {
      IoTDeviceLogMetadataBuilder builder = builder(up);
      builder.id(IdUtil.getSnowflake().nextId());
      builder.event(up.getEvent());
      builder.content(up.getEventName());

      var metadata = builder.build();
      batchArgs.add(
          new Object[] {
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
          });
    }

    // 批量插入
    if (!batchArgs.isEmpty()) {
      String sql =
          "INSERT INTO "
              + devLogMetaTableName
              + " (id, iot_id, product_key, device_name, device_id, message_type, "
              + "event, property, content, ext1, ext2, ext3, create_time) "
              + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

      try {
        int[] counts = jdbcTemplate.batchUpdate(sql, batchArgs);
        log.debug("ClickHouse 批量插入元数据成功，数量={}", counts.length);
      } catch (Exception e) {
        log.error("批量插入 ClickHouse 元数据异常", e);
      }
    }
  }

  @Override
  public PageBean<IoTDeviceLogVO> pageList(LogQuery logQuery) {
    StringBuilder sql = new StringBuilder("SELECT * FROM " + devLogTableName + " WHERE 1=1");
    List<Object> params = new ArrayList<>();

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
      sql.append(" AND JSONHas(content,'properties',?)=1");
      params.add(logQuery.getParams().get("properties"));
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

    sql.append(" ORDER BY create_time DESC");

    try {
      // 查询总数
      String countSql = "SELECT COUNT(*) FROM (" + sql + ")";
      Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

      // 分页查询
      String pageSql = sql + " LIMIT ? OFFSET ?";
      List<Object> pageParams = new ArrayList<>(params);
      int offset = (logQuery.getPageNum() - 1) * logQuery.getPageSize();
      pageParams.add(logQuery.getPageSize());
      pageParams.add(offset);

      List<IoTDeviceLogVO> list =
          jdbcTemplate.query(pageSql, new DeviceLogRowMapper(), pageParams.toArray());

      return new PageBean<>(
          list, total != null ? total : 0L, logQuery.getPageSize(), logQuery.getPageNum());

    } catch (Exception e) {
      log.warn("ClickHouse 查询日志错误", e);
      return null;
    }
  }

  @Override
  public IoTDeviceLogVO queryById(LogQuery logQuery) {
    String sql = "SELECT * FROM " + devLogTableName + " WHERE id = ?";
    try {
      return jdbcTemplate.queryForObject(sql, new DeviceLogRowMapper(), logQuery.getId());
    } catch (Exception e) {
      log.warn("ClickHouse 根据ID查询日志错误", e);
      return null;
    }
  }

  @Override
  public PageBean<IoTDeviceEvents> queryEventTotal(String productKey, String iotId) {
    List<IoTDeviceEvents> list = selectDevEvents(productKey);
    for (IoTDeviceEvents devEvent : list) {
      String sql =
          "SELECT COUNT(1) as qty, max(create_time) as create_time "
              + "FROM "
              + devLogTableName
              + " WHERE event = ? AND message_type = 'EVENT' AND iot_id = ? "
              + "ORDER BY create_time DESC";
      try {
        Map<String, Object> result = jdbcTemplate.queryForMap(sql, devEvent.getId(), iotId);
        Integer qty = result.get("qty") != null ? ((Number) result.get("qty")).intValue() : 0;
        devEvent.setQty(qty >= 100 ? "99+" : String.valueOf(qty));
        devEvent.setTime(
            result.get("create_time") != null ? result.get("create_time").toString() : null);
      } catch (Exception e) {
        log.warn("ClickHouse 查询事件统计错误", e);
      }
    }
    return new PageBean<>(list, 100L, 1, 100);
  }

  @Override
  public PageBean<IoTDeviceLogMetadataVO> queryLogMeta(LogQuery logQuery) {
    StringBuilder sql = new StringBuilder("SELECT * FROM " + devLogMetaTableName + " WHERE 1=1");
    List<Object> params = new ArrayList<>();

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
    // 添加时间查询条件 - ClickHouse DateTime64 类型直接使用 Timestamp
    if (logQuery.getBeginCreateTime() != null) {
      sql.append(" AND create_time >= ?");
      params.add(new Timestamp(logQuery.getBeginCreateTime() * 1000));
    }
    if (logQuery.getEndCreateTime() != null) {
      sql.append(" AND create_time <= ?");
      params.add(new Timestamp(logQuery.getEndCreateTime() * 1000));
    }
    sql.append(" ORDER BY create_time DESC");

    try {
      // 查询总数
      String countSql = "SELECT COUNT(*) FROM (" + sql + ")";
      Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

      // 分页查询
      String pageSql = sql + " LIMIT ? OFFSET ?";
      List<Object> pageParams = new ArrayList<>(params);
      int offset = (logQuery.getPageNum() - 1) * logQuery.getPageSize();
      pageParams.add(logQuery.getPageSize());
      pageParams.add(offset);

      List<IoTDeviceLogMetadataVO> list =
          jdbcTemplate.query(pageSql, new DeviceLogMetadataRowMapper(), pageParams.toArray());

      return new PageBean<>(
          list, total != null ? total : 0L, logQuery.getPageSize(), logQuery.getPageNum());

    } catch (Exception e) {
      log.warn("ClickHouse 查询元数据日志错误", e);
      return null;
    }
  }

  @Override
  public String getPolicy() {
    return storePolicy;
  }

  @Override
  public JSONObject configMetadata() {
    return null;
  }

  /** 设备日志行映射器 */
  private class DeviceLogRowMapper implements RowMapper<IoTDeviceLogVO> {
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
  private class DeviceLogMetadataRowMapper implements RowMapper<IoTDeviceLogMetadataVO> {
    @Override
    public IoTDeviceLogMetadataVO mapRow(ResultSet rs, int rowNum) throws SQLException {
      IoTDeviceLogMetadataVO vo = new IoTDeviceLogMetadataVO();
      // id 字段在 VO 中被注释掉了，不需要设置
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
}
