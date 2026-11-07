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

package cn.universal.databridge.plugin.iotdb;

import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.PluginInfo;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.databridge.plugin.AbstractDataInputPlugin;
import cn.universal.databridge.plugin.SourceScope;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.session.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 默认IoTDB数据输入插件 - 输入方向 专门处理从IoTDB时序数据库读取数据并发送到IoT平台
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/15
 */
@Component("defaultIoTDBDataInputPlugin")
@ConditionalOnMissingBean(name = "iotdbDataInputPlugin")
@Slf4j
public class DefaultIoTDBDataInputPlugin extends AbstractDataInputPlugin {

  @Override
  public PluginInfo getPluginInfo() {
    return PluginInfo.builder()
        .name("默认IoTDB数据输入插件")
        .version("2.0.0")
        .description("默认的IoTDB时序数据库数据输入实现，支持从IoTDB读取时间序列数据并发送到IoT平台")
        .author("gitee.com/NexIoT")
        .pluginType("IOTDB")
        .supportedResourceTypes(List.of("IOTDB"))
        .dataDirection(PluginInfo.DataDirection.INPUT)
        .category("时序数据库")
        .icon("database")
        .build();
  }

  @Override
  public Boolean testConnection(ResourceConnection connection) {
    try {
      Session session = createSession(connection);
      session.open();
      session.close();
      return true;
    } catch (Exception e) {
      log.error("IoTDB连接测试失败: {}", e.getMessage(), e);
      return false;
    }
  }

  @Override
  public Boolean validateConfig(DataBridgeConfig config) {
    if (config == null) {
      return false;
    }

    // 验证输入配置
    if (config.getConfig() == null || config.getConfig().trim().isEmpty()) {
      return false;
    }

    return true;
  }

  @Override
  public List<SourceScope> getSupportedSourceScopes() {
    return List.of(
        SourceScope.ALL_PRODUCTS, SourceScope.SPECIFIC_PRODUCTS, SourceScope.APPLICATION);
  }

  @Override
  protected void sendToIoTPlatform(Object processedData, DataBridgeConfig config) {
    try {
      // 这里需要实现将处理后的数据发送到IoT平台的逻辑
      // 例如：调用IoT平台的API、发送到消息队列等
      log.debug("发送IoTDB数据到IoT平台: {}", processedData);

      // 示例实现：这里可以调用IoT平台的服务
      // iotPlatformService.sendData(processedData, config.getConfig());

    } catch (Exception e) {
      log.error("发送IoTDB数据到IoT平台失败: {}", e.getMessage(), e);
      throw new RuntimeException("发送IoTDB数据到IoT平台失败: " + e.getMessage(), e);
    }
  }

  @Override
  protected void processExternalDataWithDefaultLogic(
      Object externalData, DataBridgeConfig config, ResourceConnection connection) {
    try {
      // 默认的IoTDB数据处理逻辑
      log.debug("使用默认逻辑处理IoTDB数据: {}", externalData);

      // 这里可以实现默认的数据转换逻辑
      // 例如：将IoTDB查询结果转换为IoT设备数据格式

      if (externalData instanceof SessionDataSet) {
        SessionDataSet dataSet = (SessionDataSet) externalData;
        // 处理查询结果
        processIoTDBQueryResult(dataSet, config);
      }

    } catch (Exception e) {
      log.error("默认IoTDB数据处理失败: {}", e.getMessage(), e);
      throw new RuntimeException("默认IoTDB数据处理失败: " + e.getMessage(), e);
    }
  }

  /** 处理IoTDB查询结果 */
  private void processIoTDBQueryResult(SessionDataSet dataSet, DataBridgeConfig config) {
    try {
      // 遍历查询结果
      while (dataSet.hasNext()) {
        // 获取下一行数据
        // 这里需要根据实际的IoTDB查询结果结构来处理
        log.debug("处理IoTDB查询结果行");

        // 转换为IoT平台数据格式
        // 发送到IoT平台
        sendToIoTPlatform(dataSet, config);
      }
    } catch (Exception e) {
      log.error("处理IoTDB查询结果失败: {}", e.getMessage(), e);
      throw new RuntimeException("处理IoTDB查询结果失败: " + e.getMessage(), e);
    }
  }

  /** 创建IoTDB会话 */
  private Session createSession(ResourceConnection connection) {
    Session session =
        new Session.Builder()
            .host(connection.getHost())
            .port(connection.getPort())
            .username(connection.getUsername())
            .password(connection.getPassword())
            .build();

    return session;
  }
}
