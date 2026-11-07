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

import cn.universal.databridge.entity.ResourceConnection;
import lombok.extern.slf4j.Slf4j;

/**
 * 资源连接工具类 提供资源连接相关的工具方法
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/15
 */
@Slf4j
public class ResourceConnectionUtils {

  /** 资源类型到插件类型的默认映射 */
  private static final java.util.Map<ResourceConnection.ResourceType, String>
      DEFAULT_RESOURCE_PLUGIN_MAPPING =
          java.util.Map.of(
              ResourceConnection.ResourceType.MYSQL, "JDBC",
              ResourceConnection.ResourceType.KAFKA, "KAFKA",
              ResourceConnection.ResourceType.MQTT, "MQTT",
              ResourceConnection.ResourceType.HTTP, "HTTP",
              ResourceConnection.ResourceType.IOTDB, "IOTDB",
              ResourceConnection.ResourceType.INFLUXDB, "INFLUXDB",
              ResourceConnection.ResourceType.ELASTICSEARCH, "ELASTICSEARCH",
              ResourceConnection.ResourceType.REDIS, "REDIS");

  /** 为资源连接设置默认的插件类型（如果未设置） */
  public static void setDefaultPluginTypeIfMissing(ResourceConnection connection) {
    if (connection.getPluginType() == null || connection.getPluginType().trim().isEmpty()) {
      String defaultPluginType = DEFAULT_RESOURCE_PLUGIN_MAPPING.get(connection.getType());
      if (defaultPluginType != null) {
        connection.setPluginType(defaultPluginType);
        log.info("为资源连接 {} 设置默认插件类型: {}", connection.getName(), defaultPluginType);
      } else {
        log.warn("资源类型 {} 没有默认的插件类型", connection.getType());
      }
    }
  }

  /** 根据资源类型获取默认的插件类型 */
  public static String getDefaultPluginType(ResourceConnection.ResourceType resourceType) {
    return DEFAULT_RESOURCE_PLUGIN_MAPPING.get(resourceType);
  }
}
