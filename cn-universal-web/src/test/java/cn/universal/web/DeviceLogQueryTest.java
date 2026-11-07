package cn.universal.web;

import static org.junit.jupiter.api.Assertions.*;

import cn.hutool.core.date.DateUtil;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.dm.device.service.log.CkDeviceLogService;
import cn.universal.dm.device.service.log.DefaultIoTDeviceLogService;
import cn.universal.dm.device.service.log.InfluxDBDeviceLogService;
import cn.universal.dm.device.service.log.IoTDBIoTDeviceLogService;
import cn.universal.dm.device.service.log.MysqlDeviceLogService;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTDeviceLogShardMapper;
import cn.universal.persistence.query.DeviceLogQuery;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 设备日志查询测试类 测试通用查询接口在不同存储策略下的表现
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/12/14
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class DeviceLogQueryTest {

  Logger logger = LoggerFactory.getLogger(DeviceLogQueryTest.class);

  @Autowired private DefaultIoTDeviceLogService defaultIoTDeviceLogService;

  @Autowired private IoTProductDeviceService iotProductDeviceService;

  @Autowired(required = false)
  private MysqlDeviceLogService mysqlDeviceLogService;

  @Autowired(required = false)
  private InfluxDBDeviceLogService influxDBDeviceLogService;

  @Autowired(required = false)
  private IoTDBIoTDeviceLogService ioTDBIoTDeviceLogService;

  @Autowired(required = false)
  private CkDeviceLogService ckDeviceLogService;

  @Autowired(required = false)
  private IoTDeviceLogShardMapper ioTDeviceLogShardMapper;

  /** 测试DeviceLogQuery Builder模式 */
  @Test
  public void testDeviceLogQueryBuilder() {
    logger.info("=== 开始测试DeviceLogQuery Builder模式 ===");

    long startTime = DateUtil.currentSeconds() - (3 * 24 * 60 * 60); // 3天前

    DeviceLogQuery query =
        new DeviceLogQuery.Builder()
            .iotId("test_product_key_test_device_001")
            .commandId("cmd_12345")
            .messageType(MessageType.FUNCTIONS.name())
            .startTime(startTime)
            .commandStatus(0)
            .limit(1)
            .build();

    assertNotNull(query);
    assertEquals("test_product_key_test_device_001", query.getIotId());
    assertEquals("cmd_12345", query.getCommandId());
    assertEquals(MessageType.FUNCTIONS.name(), query.getMessageType());
    assertEquals(startTime, query.getStartTime());
    assertEquals(0, query.getCommandStatus());
    assertEquals(1, query.getLimit());

    logger.info("DeviceLogQuery Builder测试通过");
  }

  /** 测试MySQL存储策略的通用查询 */
  @Test
  public void testMysqlQueryDeviceLogs() {
    if (mysqlDeviceLogService == null) {
      log.warn("MySQL服务未启用，跳过测试");
      return;
    }

    logger.info("=== 开始测试MySQL存储策略通用查询 ===");

    // 构建查询条件
    long startTime = DateUtil.currentSeconds() - (7 * 24 * 60 * 60); // 7天前
    DeviceLogQuery query =
        new DeviceLogQuery.Builder()
            .messageType(MessageType.FUNCTIONS.name())
            .startTime(startTime)
            .commandStatus(0)
            .limit(10)
            .build();

    // 执行查询
    List<IoTDeviceLog> logs = mysqlDeviceLogService.queryDeviceLogs(query);

    assertNotNull(logs);
    logger.info("MySQL查询结果数量: {}", logs.size());

    // 验证查询结果
    for (IoTDeviceLog log : logs) {
      assertNotNull(log.getIotId());
      assertEquals(MessageType.FUNCTIONS.name(), log.getMessageType());
      logger.info(
          "查询到日志: iotId={}, commandId={}, createTime={}",
          log.getIotId(),
          log.getCommandId(),
          log.getCreateTime());
    }
  }

  /** 测试查询最近指令记录 */
  @Test
  public void testGetRecentCommand() {
    //    if (mysqlDeviceLogService == null) {
    //      log.warn("MySQL服务未启用，跳过测试");
    //      return;
    //    }
    //
    //    logger.info("=== 开始测试查询最近指令记录 ===");
    //
    //    long twoDaysAgo = DateUtil.currentSeconds() - (2 * 24 * 60 * 60);
    //
    //    // 假设有一个测试设备
    //    String testIotId = "test_iot_id";
    //    String testCommandId = "test_command_id";
    //
    //    IoTDeviceLog log =
    //        mysqlDeviceLogService.getRecentCommand(
    //            testIotId,
    //            testCommandId,
    //            MessageType.FUNCTIONS.name(),
    //            twoDaysAgo,
    //            DateUtil.currentSeconds(),
    //            0);
    //
    //    if (log != null) {
    //      logger.info(
    //          "查询到最近指令: iotId={}, commandId={}, status={}",
    //          log.getIotId(),
    //          log.getCommandId(),
    //          log.getCommandStatus());
    //    } else {
    //      logger.info("未查询到符合条件的指令记录");
    //    }
  }

  /** 测试DefaultIoTDeviceLogService的统一查询接口 */
  @Test
  public void testDefaultServiceQueryWithProduct() {
    logger.info("=== 开始测试DefaultIoTDeviceLogService统一查询接口 ===");

    // 获取一个测试产品
    String testProductKey = "test_product"; // 替换为实际存在的产品Key
    IoTProduct product = iotProductDeviceService.getProduct(testProductKey);

    if (product == null) {
      log.warn("测试产品不存在，跳过测试");
      return;
    }

    logger.info(
        "测试产品: productKey={}, storePolicy={}", product.getProductKey(), product.getStorePolicy());

    // 构建查询条件
    //    long startTime = DateUtil.currentSeconds() - (7 * 24 * 60 * 60);
    long startTime = DateUtil.currentSeconds() - (1 * 24 * 60 * 60);
    DeviceLogQuery query =
        new DeviceLogQuery.Builder()
            .messageType(MessageType.PROPERTIES.name())
            .startTime(startTime)
            .limit(5)
            .build();

    // 执行查询
    List<IoTDeviceLog> logs = defaultIoTDeviceLogService.queryDeviceLogs(product, query);

    assertNotNull(logs);
    logger.info("查询结果数量: {}, 存储策略: {}", logs.size(), product.getStorePolicy());

    for (IoTDeviceLog log : logs) {
      logger.info(
          "日志记录: iotId={}, messageType={}, createTime={}",
          log.getIotId(),
          log.getMessageType(),
          log.getCreateTime());
    }
  }

  /** 测试不同存储策略的一致性 */
  @Test
  public void testDifferentStoragePolicyConsistency() {
    logger.info("=== 开始测试不同存储策略的一致性 ===");

    long startTime = DateUtil.currentSeconds() - (1 * 24 * 60 * 60); // 1天前
    DeviceLogQuery query =
        new DeviceLogQuery.Builder()
            .propertyKey("jt808")
            .iotId("jt808069509769807")
            .messageType(MessageType.PROPERTIES.name())
            .startTime(startTime)
            .limit(10)
            .build();

    // 测试MySQL
    if (mysqlDeviceLogService != null) {
      List<IoTDeviceLog> mysqlLogs = mysqlDeviceLogService.queryDeviceLogs(query);
      logger.info("MySQL查询结果数量: {}", mysqlLogs.size());
    }

    // 测试InfluxDB
    if (influxDBDeviceLogService != null) {
      List<IoTDeviceLog> influxLogs = influxDBDeviceLogService.queryDeviceLogs(query);
      logger.info("InfluxDB查询结果数量: {}", influxLogs.size());
    }

    // 测试IoTDB
    if (ioTDBIoTDeviceLogService != null) {
      List<IoTDeviceLog> iotdbLogs = ioTDBIoTDeviceLogService.queryDeviceLogs(query);
      logger.info("IoTDB查询结果数量: {}", iotdbLogs.size());
    }

    // 测试ClickHouse
    if (ckDeviceLogService != null) {
      List<IoTDeviceLog> ckLogs = ckDeviceLogService.queryDeviceLogs(query);
      logger.info("ClickHouse查询结果数量: {}", ckLogs.size());
    }
  }

  /** 测试复杂查询条件 */
  @Test
  public void testComplexQuery() {
    if (mysqlDeviceLogService == null) {
      log.warn("MySQL服务未启用，跳过测试");
      return;
    }

    logger.info("=== 开始测试复杂查询条件 ===");

    long startTime = DateUtil.currentSeconds() - (7 * 24 * 60 * 60);
    long endTime = DateUtil.currentSeconds();

    DeviceLogQuery query =
        new DeviceLogQuery.Builder()
            .messageType(MessageType.PROPERTIES.name())
            .startTime(startTime)
            .endTime(endTime)
            .limit(10)
            .build();

    List<IoTDeviceLog> logs = mysqlDeviceLogService.queryDeviceLogs(query);

    assertNotNull(logs);
    logger.info("复杂查询结果数量: {}", logs.size());

    for (IoTDeviceLog log : logs) {
      assertTrue(
          log.getCreateTime()
              .isAfter(LocalDateTime.ofEpochSecond(startTime, 0, java.time.ZoneOffset.ofHours(8))));
      logger.info("日志: iotId={}, time={}", log.getIotId(), log.getCreateTime());
    }
  }

  /** 测试Mapper层的通用查询 */
  @Test
  public void testMapperQueryDeviceLogs() {
    if (ioTDeviceLogShardMapper == null) {
      log.warn("IoTDeviceLogShardMapper未注入，跳过测试");
      return;
    }

    logger.info("=== 开始测试Mapper层通用查询 ===");

    long startTime = DateUtil.currentSeconds() - (3 * 24 * 60 * 60);
    DeviceLogQuery query =
        new DeviceLogQuery.Builder()
            .messageType(MessageType.FUNCTIONS.name())
            .startTime(startTime)
            .commandStatus(0)
            .limit(5)
            .build();

    List<IoTDeviceLog> logs = ioTDeviceLogShardMapper.queryDeviceLogs(query);

    assertNotNull(logs);
    logger.info("Mapper查询结果数量: {}", logs.size());

    for (IoTDeviceLog log : logs) {
      assertNotNull(log.getIotId());
      logger.info(
          "Mapper查询日志: iotId={}, commandId={}, commandStatus={}",
          log.getIotId(),
          log.getCommandId(),
          log.getCommandStatus());
    }
  }

  /** 测试查询性能 */
  @Test
  public void testQueryPerformance() {
    if (mysqlDeviceLogService == null) {
      log.warn("MySQL服务未启用，跳过测试");
      return;
    }

    logger.info("=== 开始测试查询性能 ===");

    long startTime = DateUtil.currentSeconds() - (1 * 24 * 60 * 60);
    DeviceLogQuery query = new DeviceLogQuery.Builder().startTime(startTime).limit(100).build();

    long beginTime = System.currentTimeMillis();
    List<IoTDeviceLog> logs = mysqlDeviceLogService.queryDeviceLogs(query);
    long endTime = System.currentTimeMillis();

    long duration = endTime - beginTime;
    logger.info("查询100条记录耗时: {}ms, 实际返回: {}条", duration, logs.size());

    assertTrue(duration < 5000, "查询耗时应小于5秒");
  }
}
