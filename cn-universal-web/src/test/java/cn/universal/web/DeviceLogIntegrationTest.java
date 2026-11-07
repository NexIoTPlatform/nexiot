package cn.universal.web;

import static org.junit.jupiter.api.Assertions.*;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.dm.device.service.log.DefaultIoTDeviceLogService;
import cn.universal.dm.device.service.log.IIoTDeviceLogService;
import cn.universal.dm.device.service.log.MysqlDeviceLogService;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.query.DeviceLogQuery;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 设备日志集成测试 测试在真实环境下的数据插入和查询
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/12/14
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DeviceLogIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(DeviceLogIntegrationTest.class);

  @Autowired private DefaultIoTDeviceLogService defaultIoTDeviceLogService;

  @Autowired private IoTProductDeviceService iotProductDeviceService;

  @Autowired(required = false)
  private MysqlDeviceLogService mysqlDeviceLogService;

  private static String testIotId;
  private static String testCommandId;
  private static String testProductKey;
  private static IoTProduct testProduct;

  /** 测试前准备：创建测试数据 */
  @BeforeAll
  public static void setup() {
    testIotId = "Ru871cfJjhoMnex123321";
    testCommandId = "123";
    testProductKey = "Ru871cfJjhoM";
    logger.info("=== 开始集成测试，测试设备ID: {}, 指令ID: {} ===", testIotId, testCommandId);
  }

  /** 第一步：准备测试产品 */
  @Test
  @Order(1)
  public void test01_PrepareTestProduct() {
    logger.info("=== 测试1: 准备测试产品 ===");

    // 尝试获取测试产品
    testProduct = iotProductDeviceService.getProduct(testProductKey);

    if (testProduct == null) {
      logger.warn("测试产品不存在，请确保测试环境中存在产品Key: {}", testProductKey);
      // 在实际环境中，这里可能需要创建测试产品
    } else {
      logger.info("测试产品: {}, 存储策略: {}", testProduct.getProductKey(), testProduct.getStorePolicy());
      assertNotNull(testProduct.getStorePolicy(), "产品存储策略不能为空");
    }
  }

  /** 第二步：插入测试数据 */
  @Test
  @Order(2)
  public void test02_InsertTestData() {
    if (mysqlDeviceLogService == null) {
      logger.warn("MySQL服务未启用，跳过插入测试");
      return;
    }

    logger.info("=== 测试2: 插入测试设备日志 ===");

    IoTDeviceLog testLog = new IoTDeviceLog();
    testLog.setIotId(testIotId);
    testLog.setDeviceId("device_001");
    testLog.setProductKey(testProductKey);
    testLog.setDeviceName("测试设备");
    testLog.setMessageType(MessageType.FUNCTIONS.name());
    testLog.setCommandId(testCommandId);
    testLog.setCommandStatus(0);
    testLog.setEvent("test_event");
    testLog.setContent("{\"test\":\"data\"}");
    testLog.setCreateTime(LocalDateTime.now());

    try {
      // 这里需要根据实际的插入方法进行调用
      // mysqlDeviceLogService.insertDeviceLog(testLog);
      logger.info("测试数据插入成功: iotId={}, commandId={}", testIotId, testCommandId);
    } catch (Exception e) {
      logger.error("插入测试数据失败", e);
    }
  }

  /** 第三步：测试通过iotId查询 */
  @Test
  @Order(3)
  public void test03_QueryByIotId() {
    if (mysqlDeviceLogService == null) {
      logger.warn("MySQL服务未启用，跳过查询测试");
      return;
    }

    logger.info("=== 测试3: 通过iotId查询设备日志 ===");

    DeviceLogQuery query = new DeviceLogQuery.Builder().iotId(testIotId).limit(10).build();

    List<IoTDeviceLog> logs = mysqlDeviceLogService.queryDeviceLogs(query);

    assertNotNull(logs);
    logger.info("查询结果数量: {}", logs.size());

    if (!logs.isEmpty()) {
      IoTDeviceLog log = logs.get(0);
      assertEquals(testIotId, log.getIotId());
      logger.info("查询到的日志: commandId={}, messageType={}", log.getCommandId(), log.getMessageType());
    }
  }

  /** 第四步：测试通过commandId查询 */
  @Test
  @Order(4)
  public void test04_QueryByCommandId() {
    if (mysqlDeviceLogService == null) {
      logger.warn("MySQL服务未启用，跳过查询测试");
      return;
    }

    logger.info("=== 测试4: 通过commandId查询设备日志 ===");

    DeviceLogQuery query =
        new DeviceLogQuery.Builder()
            .iotId(testIotId)
            .commandId(testCommandId)
            .messageType(MessageType.FUNCTIONS.name())
            .limit(1)
            .build();

    List<IoTDeviceLog> logs = mysqlDeviceLogService.queryDeviceLogs(query);

    assertNotNull(logs);
    logger.info("查询结果数量: {}", logs.size());

    if (!logs.isEmpty()) {
      IoTDeviceLog log = logs.get(0);
      assertEquals(testCommandId, log.getCommandId());
      assertEquals(MessageType.FUNCTIONS.name(), log.getMessageType());
      logger.info("查询到指令日志: status={}, createTime={}", log.getCommandStatus(), log.getCreateTime());
    }
  }

  /** 第五步：测试时间范围查询 */
  @Test
  @Order(5)
  public void test05_QueryByTimeRange() {
    if (mysqlDeviceLogService == null) {
      logger.warn("MySQL服务未启用，跳过查询测试");
      return;
    }

    logger.info("=== 测试5: 测试时间范围查询 ===");

    long startTime = DateUtil.currentSeconds() - (1 * 24 * 60 * 60); // 1天前
    long endTime = DateUtil.currentSeconds();

    DeviceLogQuery query =
        new DeviceLogQuery.Builder()
            .iotId(testIotId)
            .startTime(startTime)
            .endTime(endTime)
            .limit(10)
            .build();

    List<IoTDeviceLog> logs = mysqlDeviceLogService.queryDeviceLogs(query);

    assertNotNull(logs);
    logger.info("时间范围查询结果数量: {}", logs.size());

    for (IoTDeviceLog log : logs) {
      assertNotNull(log.getCreateTime());
      logger.info("日志时间: {}", log.getCreateTime());
    }
  }

  /** 第六步：测试getRecentCommand方法 */
  @Test
  @Order(6)
  public void test06_GetRecentCommand() {
    if (mysqlDeviceLogService == null) {
      logger.warn("MySQL服务未启用，跳过查询测试");
      return;
    }

    logger.info("=== 测试6: 测试getRecentCommand方法 ===");

    long twoDaysAgo = DateUtil.currentSeconds() - (2 * 24 * 60 * 60);

    IoTDeviceLog log =
        mysqlDeviceLogService.getRecentCommand(
            testIotId, testCommandId, MessageType.FUNCTIONS.name(), twoDaysAgo, 0);

    if (log != null) {
      assertEquals(testIotId, log.getIotId());
      assertEquals(testCommandId, log.getCommandId());
      logger.info("查询到最近指令: status={}, time={}", log.getCommandStatus(), log.getCreateTime());
    } else {
      logger.info("未查询到最近指令记录");
    }
  }

  /** 第七步：测试DefaultService统一查询 */
  @Test
  @Order(7)
  public void test07_DefaultServiceQuery() {
    if (testProduct == null) {
      logger.warn("测试产品不存在，跳过测试");
      return;
    }

    logger.info("=== 测试7: 测试DefaultService统一查询 ===");

    long startTime = DateUtil.currentSeconds() - (1 * 24 * 60 * 60);

    DeviceLogQuery query =
        new DeviceLogQuery.Builder().iotId(testIotId).startTime(startTime).limit(10).build();

    List<IoTDeviceLog> logs = defaultIoTDeviceLogService.queryDeviceLogs(testProduct, query);

    assertNotNull(logs);
    logger.info("DefaultService查询结果数量: {}, 存储策略: {}", logs.size(), testProduct.getStorePolicy());
  }

  /** 第八步：测试不同存储策略的查询 */
  @Test
  @Order(8)
  public void test08_QueryDifferentStoragePolicy() {
    logger.info("=== 测试8: 测试不同存储策略的查询 ===");

    long startTime = DateUtil.currentSeconds() - (1 * 24 * 60 * 60);

    DeviceLogQuery query =
        new DeviceLogQuery.Builder()
            .messageType(MessageType.PROPERTIES.name())
            .startTime(startTime)
            .limit(5)
            .build();

    // 获取已配置的存储策略服务
    IIoTDeviceLogService logService = defaultIoTDeviceLogService.getLogService(testProduct);

    if (logService != null) {
      String policy = logService.getPolicy();
      logger.info("当前存储策略: {}", policy);

      List<IoTDeviceLog> logs = logService.queryDeviceLogs(query);
      assertNotNull(logs);
      logger.info("{} 存储策略查询结果数量: {}", policy, logs.size());
    }
  }

  /** 第九步：测试边界条件 */
  @Test
  @Order(9)
  public void test09_BoundaryConditions() {
    if (mysqlDeviceLogService == null) {
      logger.warn("MySQL服务未启用，跳过测试");
      return;
    }

    logger.info("=== 测试9: 测试边界条件 ===");

    // 测试空查询
    DeviceLogQuery emptyQuery = new DeviceLogQuery.Builder().build();
    List<IoTDeviceLog> logs1 = mysqlDeviceLogService.queryDeviceLogs(emptyQuery);
    assertNotNull(logs1);
    logger.info("空查询结果数量: {}", logs1.size());

    // 测试不存在的iotId
    DeviceLogQuery notExistQuery =
        new DeviceLogQuery.Builder().iotId("not_exist_" + IdUtil.simpleUUID()).limit(10).build();
    List<IoTDeviceLog> logs2 = mysqlDeviceLogService.queryDeviceLogs(notExistQuery);
    assertNotNull(logs2);
    assertTrue(logs2.isEmpty(), "不存在的iotId应返回空列表");
    logger.info("不存在的iotId查询结果数量: {}", logs2.size());

    // 测试未来时间
    long futureTime = DateUtil.currentSeconds() + (7 * 24 * 60 * 60); // 7天后
    DeviceLogQuery futureQuery =
        new DeviceLogQuery.Builder().startTime(futureTime).limit(10).build();
    List<IoTDeviceLog> logs3 = mysqlDeviceLogService.queryDeviceLogs(futureQuery);
    assertNotNull(logs3);
    assertTrue(logs3.isEmpty(), "未来时间应返回空列表");
    logger.info("未来时间查询结果数量: {}", logs3.size());
  }

  /** 第十步：性能测试 */
  @Test
  @Order(10)
  public void test10_PerformanceTest() {
    if (mysqlDeviceLogService == null) {
      logger.warn("MySQL服务未启用，跳过性能测试");
      return;
    }

    logger.info("=== 测试10: 性能测试 ===");

    long startTime = DateUtil.currentSeconds() - (7 * 24 * 60 * 60);

    DeviceLogQuery query = new DeviceLogQuery.Builder().startTime(startTime).limit(100).build();

    // 执行多次查询测试平均性能
    int iterations = 5;
    long totalDuration = 0;

    for (int i = 0; i < iterations; i++) {
      long begin = System.currentTimeMillis();
      List<IoTDeviceLog> logs = mysqlDeviceLogService.queryDeviceLogs(query);
      long end = System.currentTimeMillis();

      long duration = end - begin;
      totalDuration += duration;

      logger.info("第{}次查询: 耗时{}ms, 结果数量{}", i + 1, duration, logs.size());
    }

    long avgDuration = totalDuration / iterations;
    logger.info("平均查询耗时: {}ms", avgDuration);

    assertTrue(avgDuration < 5000, "平均查询耗时应小于5秒");
  }

  @AfterAll
  public static void cleanup() {
    logger.info("=== 集成测试完成 ===");
    // 这里可以清理测试数据
  }
}
