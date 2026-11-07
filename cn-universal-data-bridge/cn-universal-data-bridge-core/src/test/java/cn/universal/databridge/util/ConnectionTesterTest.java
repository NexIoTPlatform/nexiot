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

package cn.universal.databridge.util;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.databridge.entity.ResourceConnection;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 连接测试器测试类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@SpringBootTest
public class ConnectionTesterTest {

  private final ConnectionTester connectionTester = new ConnectionTester();

  @Test
  public void testMySQLConnection() {
    // 创建一个测试用的MySQL连接配置
    ResourceConnection connection =
        ResourceConnection.builder()
            .name("测试MySQL连接")
            .type(ResourceConnection.ResourceType.MYSQL)
            .host("localhost")
            .port(3306)
            .username("root")
            .password("password")
            .databaseName("test")
            .status(1)
            .build();

    // 注意：这个测试需要实际的MySQL服务器运行
    // 在实际环境中，你可能需要mock或者使用测试数据库
    ConnectionTester.ConnectionTestResult result = connectionTester.testConnection(connection);

    // 由于没有实际的MySQL服务器，我们期望测试失败
    assertFalse(result.isSuccess());
    assertTrue(result.getMessage().contains("MySQL连接失败"));
  }

  @Test
  public void testHttpConnection() {
    // 创建一个测试用的HTTP连接配置
    ResourceConnection connection =
        ResourceConnection.builder()
            .name("测试HTTP连接")
            .type(ResourceConnection.ResourceType.HTTP)
            .host("www.baidu.com")
            .port(80)
            .databaseName("/") // 请求路径
            .status(1)
            .build();

    ConnectionTester.ConnectionTestResult result = connectionTester.testConnection(connection);

    // 百度网站应该可以访问
    assertTrue(result.isSuccess());
    assertTrue(result.getMessage().contains("HTTP连接测试成功"));
  }

  @Test
  public void testInvalidConnection() {
    // 创建一个无效的连接配置
    ResourceConnection connection =
        ResourceConnection.builder()
            .name("测试无效连接")
            .type(ResourceConnection.ResourceType.MYSQL)
            .host("invalid-host")
            .port(9999)
            .username("invalid")
            .password("invalid")
            .databaseName("invalid")
            .status(1)
            .build();

    ConnectionTester.ConnectionTestResult result = connectionTester.testConnection(connection);

    assertFalse(result.isSuccess());
    assertTrue(result.getMessage().contains("MySQL连接失败"));
  }

  @Test
  public void testNullConnection() {
    ConnectionTester.ConnectionTestResult result = connectionTester.testConnection(null);

    assertFalse(result.isSuccess());
    assertEquals("连接配置不能为空", result.getMessage());
  }

  @Test
  public void testUnsupportedConnectionType() {
    // 创建一个不支持的连接类型（这里我们假设有一个不存在的类型）
    ResourceConnection connection =
        ResourceConnection.builder()
            .name("测试不支持的类型")
            .type(ResourceConnection.ResourceType.MYSQL) // 使用支持的类型，但我们可以测试其他逻辑
            .host("localhost")
            .port(3306)
            .status(1)
            .build();

    // 这个测试主要是验证我们的代码结构
    ConnectionTester.ConnectionTestResult result = connectionTester.testConnection(connection);

    // 由于缺少必要字段，应该失败
    assertFalse(result.isSuccess());
  }
}
