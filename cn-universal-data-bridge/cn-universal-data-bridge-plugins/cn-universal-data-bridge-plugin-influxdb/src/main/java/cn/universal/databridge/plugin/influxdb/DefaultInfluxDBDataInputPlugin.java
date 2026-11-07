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

package cn.universal.databridge.plugin.influxdb;

import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.PluginInfo;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.databridge.plugin.AbstractDataInputPlugin;
import cn.universal.databridge.plugin.SourceScope;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.query.FluxTable;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 默认InfluxDB数据输入插件 - 输入方向 专门处理从InfluxDB时序数据库读取数据并发送到IoT平台
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/15
 */
@Component("defaultInfluxDBDataInputPlugin")
@ConditionalOnMissingBean(name = "influxdbDataInputPlugin")
@Slf4j
public class DefaultInfluxDBDataInputPlugin extends AbstractDataInputPlugin {

  @Override
  public PluginInfo getPluginInfo() {
    return PluginInfo.builder()
        .name("默认InfluxDB数据输入插件")
        .version("2.0.0")
        .description("默认的InfluxDB时序数据库数据输入实现，支持从InfluxDB读取时间序列数据并发送到IoT平台")
        .author("gitee.com/NexIoT")
        .pluginType("INFLUXDB")
        .supportedResourceTypes(List.of("INFLUXDB"))
        .dataDirection(PluginInfo.DataDirection.INPUT)
        .category("时序数据库")
        .icon("database")
        .build();
  }

  @Override
  public Boolean testConnection(ResourceConnection connection) {
    try {
      InfluxDBClient client = createInfluxDBClient(connection);
      client.ping();
      client.close();
      return true;
    } catch (Exception e) {
      log.error("InfluxDB连接测试失败: {}", e.getMessage(), e);
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
      log.debug("发送InfluxDB数据到IoT平台: {}", processedData);

      // 示例实现：这里可以调用IoT平台的服务
      // iotPlatformService.sendData(processedData, config.getConfig());

    } catch (Exception e) {
      log.error("发送InfluxDB数据到IoT平台失败: {}", e.getMessage(), e);
      throw new RuntimeException("发送InfluxDB数据到IoT平台失败: " + e.getMessage(), e);
    }
  }

  @Override
  protected void processExternalDataWithDefaultLogic(
      Object externalData, DataBridgeConfig config, ResourceConnection connection) {
    try {
      // 默认的InfluxDB数据处理逻辑
      log.debug("使用默认逻辑处理InfluxDB数据: {}", externalData);

      // 这里可以实现默认的数据转换逻辑
      // 例如：将InfluxDB查询结果转换为IoT设备数据格式

      if (externalData instanceof List) {
        @SuppressWarnings("unchecked")
        List<FluxTable> fluxTables = (List<FluxTable>) externalData;
        // 处理查询结果
        processInfluxDBQueryResult(fluxTables, config);
      }

    } catch (Exception e) {
      log.error("默认InfluxDB数据处理失败: {}", e.getMessage(), e);
      throw new RuntimeException("默认InfluxDB数据处理失败: " + e.getMessage(), e);
    }
  }

  /** 处理InfluxDB查询结果 */
  private void processInfluxDBQueryResult(List<FluxTable> fluxTables, DataBridgeConfig config) {
    try {
      // 遍历查询结果
      for (FluxTable fluxTable : fluxTables) {
        // 处理每个表的数据
        fluxTable
            .getRecords()
            .forEach(
                record -> {
                  log.debug("处理InfluxDB查询结果记录: {}", record);

                  // 转换为IoT平台数据格式
                  // 发送到IoT平台
                  sendToIoTPlatform(record, config);
                });
      }
    } catch (Exception e) {
      log.error("处理InfluxDB查询结果失败: {}", e.getMessage(), e);
      throw new RuntimeException("处理InfluxDB查询结果失败: " + e.getMessage(), e);
    }
  }

  /** 创建InfluxDB客户端 */
  private InfluxDBClient createInfluxDBClient(ResourceConnection connection) {
    String url = "http://" + connection.getHost() + ":" + connection.getPort();
    String token = connection.getPassword(); // InfluxDB使用token作为密码

    return InfluxDBClientFactory.create(url, token.toCharArray());
  }
}
