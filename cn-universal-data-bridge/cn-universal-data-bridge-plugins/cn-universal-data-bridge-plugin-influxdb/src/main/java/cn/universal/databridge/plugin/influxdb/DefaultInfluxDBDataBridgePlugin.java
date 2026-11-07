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

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.PluginInfo;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.databridge.plugin.AbstractDataOutputPlugin;
import cn.universal.databridge.plugin.SourceScope;
import cn.universal.persistence.base.BaseUPRequest;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 默认InfluxDB数据桥接插件 - 输出方向 专门处理InfluxDB时序数据库的数据写入
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/15
 */
@Component("defaultInfluxDBDataBridgePlugin")
@ConditionalOnMissingBean(name = "influxdbDataBridgePlugin")
@Slf4j
public class DefaultInfluxDBDataBridgePlugin extends AbstractDataOutputPlugin {

  @Override
  public PluginInfo getPluginInfo() {
    return PluginInfo.builder()
        .name("默认InfluxDB数据桥接插件")
        .version("2.0.0")
        .description("默认的InfluxDB时序数据库数据桥接实现，支持高效的时间序列数据写入，支持Magic脚本自定义处理逻辑")
        .author("gitee.com/NexIoT")
        .pluginType("INFLUXDB")
        .supportedResourceTypes(List.of("INFLUXDB"))
        .dataDirection(PluginInfo.DataDirection.OUTPUT)
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

    // 验证配置
    JSONObject configJson = parseConfig(config);
    if (!configJson.containsKey("measurement") || !configJson.containsKey("bucket")) {
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
      InfluxDBClient client = createInfluxDBClient(connection);
      WriteApiBlocking writeApi = client.getWriteApiBlocking();

      try {
        // 根据Magic脚本返回的数据，生成InfluxDB数据点
        List<Point> points = generateInfluxDBPoints(processedData, config, request);

        // 批量写入数据点
        if (!points.isEmpty()) {
          writeApi.writePoints(points);
          log.debug("InfluxDB数据写入成功，数据点数量: {}", points.size());
        }

      } finally {
        client.close();
      }

    } catch (Exception e) {
      log.error("处理InfluxDB数据失败: {}", e.getMessage(), e);
      throw new RuntimeException("处理InfluxDB数据失败: " + e.getMessage(), e);
    }
  }

  @Override
  protected void processTemplateResult(
      String templateResult,
      BaseUPRequest request,
      DataBridgeConfig config,
      ResourceConnection connection) {
    try {
      InfluxDBClient client = createInfluxDBClient(connection);
      WriteApiBlocking writeApi = client.getWriteApiBlocking();

      try {
        // 解析模板结果为JSON配置
        JSONObject configJson = JSONUtil.parseObj(templateResult);

        // 生成数据点
        Point point = createPointFromConfig(configJson, request);

        // 写入数据点
        writeApi.writePoint(point);
        log.debug("InfluxDB模板执行成功: {}", templateResult);

      } finally {
        client.close();
      }

    } catch (Exception e) {
      log.error("执行InfluxDB模板失败: {}", e.getMessage(), e);
      throw new RuntimeException("执行InfluxDB模板失败: " + e.getMessage(), e);
    }
  }

  /** 生成InfluxDB数据点 */
  private List<Point> generateInfluxDBPoints(
      Object processedData, DataBridgeConfig config, BaseUPRequest request) {
    List<Point> points = new java.util.ArrayList<>();

    try {
      JSONObject configJson = parseConfig(config);

      // 如果Magic脚本直接返回Point对象列表
      if (processedData instanceof List) {
        @SuppressWarnings("unchecked")
        List<Object> dataList = (List<Object>) processedData;
        for (Object item : dataList) {
          if (item instanceof Point) {
            points.add((Point) item);
          } else if (item instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) item;
            Point point = createPointFromDataMap(dataMap, configJson, request);
            points.add(point);
          }
        }
      }
      // 如果Magic脚本返回的是Map，创建单个数据点
      else if (processedData instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> dataMap = (Map<String, Object>) processedData;
        Point point = createPointFromDataMap(dataMap, configJson, request);
        points.add(point);
      }
      // 如果Magic脚本返回的是Point对象
      else if (processedData instanceof Point) {
        points.add((Point) processedData);
      } else {
        log.warn(
            "Magic脚本返回的数据类型不支持: {}",
            processedData != null ? processedData.getClass().getSimpleName() : "null");
      }

    } catch (Exception e) {
      log.error("生成InfluxDB数据点失败: {}", e.getMessage(), e);
      throw new RuntimeException("生成InfluxDB数据点失败: " + e.getMessage(), e);
    }

    return points;
  }

  /** 从数据Map创建数据点 */
  private Point createPointFromDataMap(
      Map<String, Object> dataMap, JSONObject configJson, BaseUPRequest request) {
    String measurement = configJson.getStr("measurement", "device_data");
    Point point = Point.measurement(measurement);

    // 添加标签
    if (configJson.containsKey("tags")) {
      JSONObject tags = configJson.getJSONObject("tags");
      tags.forEach(
          (key, value) -> {
            String tagValue =
                processTemplate(value.toString(), buildTemplateVariables(request, configJson));
            point.addTag(key, tagValue);
          });
    }

    // 添加字段
    if (configJson.containsKey("fields")) {
      JSONObject fields = configJson.getJSONObject("fields");
      fields.forEach(
          (key, value) -> {
            String fieldValue =
                processTemplate(value.toString(), buildTemplateVariables(request, configJson));
            point.addField(key, fieldValue);
          });
    }

    // 添加时间戳
    if (configJson.containsKey("timestamp")) {
      String timestampStr = configJson.getStr("timestamp");
      if (StrUtil.isNotBlank(timestampStr)) {
        try {
          long timestamp = Long.parseLong(timestampStr);
          point.time(timestamp, WritePrecision.MS);
        } catch (NumberFormatException e) {
          log.warn("时间戳格式错误，使用当前时间: {}", timestampStr);
          point.time(System.currentTimeMillis(), WritePrecision.MS);
        }
      }
    } else {
      point.time(System.currentTimeMillis(), WritePrecision.MS);
    }

    return point;
  }

  /** 从配置创建数据点 */
  private Point createPointFromConfig(JSONObject configJson, BaseUPRequest request) {
    String measurement = configJson.getStr("measurement", "device_data");
    Point point = Point.measurement(measurement);

    // 添加标签
    if (configJson.containsKey("tags")) {
      JSONObject tags = configJson.getJSONObject("tags");
      tags.forEach(
          (key, value) -> {
            String tagValue =
                processTemplate(value.toString(), buildTemplateVariables(request, configJson));
            point.addTag(key, tagValue);
          });
    }

    // 添加字段
    if (configJson.containsKey("fields")) {
      JSONObject fields = configJson.getJSONObject("fields");
      fields.forEach(
          (key, value) -> {
            String fieldValue =
                processTemplate(value.toString(), buildTemplateVariables(request, configJson));
            point.addField(key, fieldValue);
          });
    }

    // 添加时间戳
    if (configJson.containsKey("timestamp")) {
      String timestampStr = configJson.getStr("timestamp");
      if (StrUtil.isNotBlank(timestampStr)) {
        try {
          long timestamp = Long.parseLong(timestampStr);
          point.time(timestamp, WritePrecision.MS);
        } catch (NumberFormatException e) {
          log.warn("时间戳格式错误，使用当前时间: {}", timestampStr);
          point.time(System.currentTimeMillis(), WritePrecision.MS);
        }
      }
    } else {
      point.time(System.currentTimeMillis(), WritePrecision.MS);
    }

    return point;
  }

  /** 创建InfluxDB客户端 */
  private InfluxDBClient createInfluxDBClient(ResourceConnection connection) {
    String url = "http://" + connection.getHost() + ":" + connection.getPort();
    String token = connection.getPassword(); // InfluxDB使用token作为密码

    return InfluxDBClientFactory.create(url, token.toCharArray());
  }
}
