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
import cn.universal.databridge.plugin.AbstractDataOutputPlugin;
import cn.universal.databridge.plugin.SourceScope;
import cn.universal.persistence.base.BaseUPRequest;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.iotdb.session.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 默认IoTDB数据桥接插件 - 输出方向 专门处理IoTDB时序数据库的数据写入
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/15
 */
@Component("defaultIoTDBDataBridgePlugin")
@ConditionalOnMissingBean(name = "iotdbDataBridgePlugin")
@Slf4j
public class DefaultIoTDBDataBridgePlugin extends AbstractDataOutputPlugin {

  @Override
  public PluginInfo getPluginInfo() {
    return PluginInfo.builder()
        .name("默认IoTDB数据桥接插件")
        .version("2.0.0")
        .description("默认的IoTDB时序数据库数据桥接实现，支持高效的时间序列数据写入，支持Magic脚本自定义处理逻辑")
        .author("gitee.com/NexIoT")
        .pluginType("IOTDB")
        .supportedResourceTypes(List.of("IOTDB"))
        .dataDirection(PluginInfo.DataDirection.OUTPUT)
        .category("时序数据库")
        .icon("database")
        .build();
  }

  @Override
  public Boolean testConnection(ResourceConnection connection) {
    try {
      // 使用Session API测试IoTDB连接
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

    // 验证模板
    if (config.getTemplate() == null || config.getTemplate().trim().isEmpty()) {
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
  protected void processProcessedData(
      Object processedData,
      BaseUPRequest request,
      DataBridgeConfig config,
      ResourceConnection connection) {
    try {
      Session session = createSession(connection);
      session.open();

      try {
        // 根据Magic脚本返回的数据，生成IoTDB插入语句
        List<String> insertStatements =
            generateIoTDBInsertStatements(processedData, config, request);

        // 批量执行插入语句
        for (String statement : insertStatements) {
          session.executeNonQueryStatement(statement);
        }

        log.debug("IoTDB数据插入成功，语句数量: {}", insertStatements.size());

      } finally {
        session.close();
      }

    } catch (Exception e) {
      log.error("处理IoTDB数据失败: {}", e.getMessage(), e);
      throw new RuntimeException("处理IoTDB数据失败: " + e.getMessage(), e);
    }
  }

  @Override
  protected void processTemplateResult(
      String templateResult,
      BaseUPRequest request,
      DataBridgeConfig config,
      ResourceConnection connection) {
    try {
      Session session = createSession(connection);
      session.open();

      try {
        // 直接执行模板结果
        session.executeNonQueryStatement(templateResult);
        log.debug("IoTDB模板执行成功: {}", templateResult);

      } finally {
        session.close();
      }

    } catch (Exception e) {
      log.error("执行IoTDB模板失败: {}", e.getMessage(), e);
      throw new RuntimeException("执行IoTDB模板失败: " + e.getMessage(), e);
    }
  }

  /** 生成IoTDB插入语句 */
  private List<String> generateIoTDBInsertStatements(
      Object processedData, DataBridgeConfig config, BaseUPRequest request) {
    List<String> statements = new java.util.ArrayList<>();

    try {
      String deviceKey =
          request.getIoTDeviceDTO() != null ? request.getIoTDeviceDTO().getDeviceId() : "unknown";
      long timestamp = System.currentTimeMillis(); // 使用当前时间戳

      // 如果Magic脚本直接返回SQL字符串
      if (processedData instanceof String) {
        statements.add((String) processedData);
      }
      // 如果Magic脚本返回SQL字符串列表
      else if (processedData instanceof List) {
        @SuppressWarnings("unchecked")
        List<Object> dataList = (List<Object>) processedData;
        for (Object item : dataList) {
          if (item instanceof String) {
            statements.add((String) item);
          }
        }
      }
      // 如果Magic脚本返回的是Map，使用模板生成IoTDB语句
      else if (processedData instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> dataMap = (Map<String, Object>) processedData;
        String statement = generateIoTDBStatement(deviceKey, timestamp, dataMap, config);
        statements.add(statement);
      }
      // 如果Magic脚本返回的是List<Map>，为每个Map生成IoTDB语句
      else if (processedData instanceof List) {
        @SuppressWarnings("unchecked")
        List<Object> dataList = (List<Object>) processedData;
        for (Object item : dataList) {
          if (item instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) item;
            String statement = generateIoTDBStatement(deviceKey, timestamp, dataMap, config);
            statements.add(statement);
          }
        }
      } else {
        log.warn(
            "Magic脚本返回的数据类型不支持: {}",
            processedData != null ? processedData.getClass().getSimpleName() : "null");
      }

    } catch (Exception e) {
      log.error("生成IoTDB插入语句失败: {}", e.getMessage(), e);
      throw new RuntimeException("生成IoTDB插入语句失败: " + e.getMessage(), e);
    }

    return statements;
  }

  /** 生成IoTDB插入语句 */
  private String generateIoTDBStatement(
      String deviceKey, long timestamp, Map<String, Object> dataMap, DataBridgeConfig config) {
    StringBuilder statement = new StringBuilder();
    statement.append("INSERT INTO root.device.").append(deviceKey).append(" (timestamp");

    // 添加测量值
    for (String key : dataMap.keySet()) {
      statement.append(", ").append(key);
    }

    statement.append(") VALUES (").append(timestamp);

    // 添加值
    for (Object value : dataMap.values()) {
      statement.append(", ").append(formatValue(value));
    }

    statement.append(")");

    return statement.toString();
  }

  /** 格式化值 */
  private String formatValue(Object value) {
    if (value == null) {
      return "null";
    }

    if (value instanceof String) {
      return "'" + value + "'";
    }

    if (value instanceof Number) {
      return value.toString();
    }

    if (value instanceof Boolean) {
      return value.toString();
    }

    // 其他类型转换为字符串
    return "'" + value.toString() + "'";
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
