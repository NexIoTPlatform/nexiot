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

package cn.universal.admin.network.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.persistence.entity.Network;
import lombok.extern.slf4j.Slf4j;

/**
 * 网络组件配置工具类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j
public class NetworkConfigUtil {

  /**
   * 验证网络组件配置
   *
   * @param network 网络组件
   * @return 验证结果
   */
  public static boolean validateConfig(Network network) {
    if (network == null) {
      return false;
    }

    // 验证必填字段
    if (StrUtil.isBlank(network.getType())
        || StrUtil.isBlank(network.getUnionId())
        || StrUtil.isBlank(network.getName())) {
      return false;
    }

    // 验证配置JSON格式
    if (StrUtil.isNotBlank(network.getConfiguration())) {
      try {
        JSONUtil.parseObj(network.getConfiguration());
      } catch (Exception e) {
        log.warn("网络组件配置JSON格式错误: {}", e.getMessage());
        return false;
      }
    }

    return true;
  }

  /**
   * 验证TCP配置
   *
   * @param config 配置JSON字符串
   * @return 验证结果
   */
  public static boolean validateTcpConfig(String config) {
    if (StrUtil.isBlank(config)) {
      return false;
    }

    try {
      JSONObject jsonConfig = JSONUtil.parseObj(config);

      // 验证必填字段
      if (!jsonConfig.containsKey("host") || !jsonConfig.containsKey("port")) {
        return false;
      }

      // 验证端口范围
      int port = jsonConfig.getInt("port", 0);
      if (port <= 0 || port > 65535) {
        return false;
      }

      return true;
    } catch (Exception e) {
      log.warn("TCP配置验证失败: {}", e.getMessage());
      return false;
    }
  }

  /**
   * 验证MQTT配置
   *
   * @param config 配置JSON字符串
   * @return 验证结果
   */
  public static boolean validateMqttConfig(String config) {
    if (StrUtil.isBlank(config)) {
      return false;
    }

    try {
      JSONObject jsonConfig = JSONUtil.parseObj(config);

      // 验证必填字段
      if (!jsonConfig.containsKey("host")) {
        return false;
      }

      // 验证QoS范围
      if (jsonConfig.containsKey("defaultQos")) {
        int qos = jsonConfig.getInt("defaultQos", -1);
        if (qos < 0 || qos > 2) {
          return false;
        }
      }

      // 验证心跳间隔
      if (jsonConfig.containsKey("keepAliveInterval")) {
        int keepAlive = jsonConfig.getInt("keepAliveInterval", 0);
        if (keepAlive < 0) {
          return false;
        }
      }

      return true;
    } catch (Exception e) {
      log.warn("MQTT配置验证失败: {}", e.getMessage());
      return false;
    }
  }

  /**
   * 根据网络类型验证配置
   *
   * @param network 网络组件
   * @return 验证结果
   */
  public static boolean validateConfigByType(Network network) {
    if (network == null
        || StrUtil.isBlank(network.getType())
        || StrUtil.isBlank(network.getConfiguration())) {
      return false;
    }

    switch (network.getType()) {
      case "TCP_CLIENT":
      case "TCP_SERVER":
        return validateTcpConfig(network.getConfiguration());
      case "MQTT_CLIENT":
      case "MQTT_SERVER":
        return validateMqttConfig(network.getConfiguration());
      case "WEB_SOCKET_SERVER":
      case "WEB_SOCKET_CLIENT":
        return validateWebSocketConfig(network.getConfiguration());
      case "UDP":
        return validateUdpConfig(network.getConfiguration());
      default:
        return false;
    }
  }

  /**
   * 获取默认TCP配置
   *
   * @return 默认TCP配置
   */
  public static String getDefaultTcpConfig() {
    JSONObject config = new JSONObject();
    config.set("host", "0.0.0.0");
    config.set("port", 8080);
    config.set("ssl", false);
    config.set("allIdleTime", 0);
    config.set("readerIdleTime", 360);
    config.set("writerIdleTime", 0);
    config.set("decoderType", "STRING");
    config.set("parserType", "DELIMITER");
    config.set(
        "parserConfiguration",
        new JSONObject()
            .set("byteOrderLittle", true)
            .set("delimiter", "]")
            .set("delimitedMaxlength", 1024)
            .set("failFast", true));
    return config.toString();
  }

  /**
   * 获取默认MQTT配置
   *
   * @return 默认MQTT配置
   */
  public static String getDefaultMqttConfig() {
    JSONObject config = new JSONObject();
    config.set("host", "tcp://localhost:1883");
    config.set("username", "");
    config.set("password", "");
    config.set("clientIdPrefix", "univ_cli_");
    config.set("defaultQos", 1);
    config.set("keepAliveInterval", 60);
    config.set("connectTimeout", 30);
    config.set("autoReconnect", true);
    config.set("cleanSession", true);
    config.set("ssl", false);
    config.set("enabled", true);
    return config.toString();
  }

  /**
   * 根据网络类型获取默认配置
   *
   * @param type 网络类型
   * @return 默认配置
   */
  public static String getDefaultConfigByType(String type) {
    switch (type) {
      case "TCP_CLIENT":
      case "TCP_SERVER":
        return getDefaultTcpConfig();
      case "MQTT_CLIENT":
      case "MQTT_SERVER":
        return getDefaultMqttConfig();
      default:
        return "{}";
    }
  }

  /**
   * 格式化配置显示
   *
   * @param config 配置JSON字符串
   * @return 格式化后的配置
   */
  public static String formatConfig(String config) {
    if (StrUtil.isBlank(config)) {
      return "{}";
    }

    try {
      return JSONUtil.parseObj(config).toStringPretty();
    } catch (Exception e) {
      log.warn("格式化配置失败: {}", e.getMessage());
      return config;
    }
  }

  /**
   * 验证WebSocket配置
   *
   * @param configuration 配置JSON字符串
   * @return 是否有效
   */
  private static boolean validateWebSocketConfig(String configuration) {
    try {
      JSONObject config = JSONUtil.parseObj(configuration);
      
      // host 字段 - 客户端必填，服务端不需要（通过 type 无法判断，暂不严格校验）
      // 如果存在则验证不为空
      String host = config.getStr("host");
      if (host != null && StrUtil.isBlank(host)) {
        log.warn("WebSocket配置验证失败: host不能为空字符串");
        return false;
      }
      
      // 端口号必填且有效
      Integer port = config.getInt("port");
      if (port == null || port <= 0 || port > 65535) {
        log.warn("WebSocket配置验证失败: 端口号无效 - {}", port);
        return false;
      }
      
      // 路径必填且必须以/开头
      String path = config.getStr("path");
      if (StrUtil.isBlank(path)) {
        log.warn("WebSocket配置验证失败: 路径不能为空");
        return false;
      }
      if (!path.startsWith("/")) {
        log.warn("WebSocket配置验证失败: 路径必须以/开头 - {}", path);
        return false;
      }
      
      // clientId 可选，如果存在则验证不为空字符串
      String clientId = config.getStr("clientId");
      if (clientId != null && StrUtil.isBlank(clientId)) {
        log.warn("WebSocket配置验证失败: clientId不能为空字符串");
        return false;
      }
      
      // username 和 password 可选，不做验证
      
      return true;
    } catch (Exception e) {
      log.warn("WebSocket配置验证失败: 解析异常 - {}", e.getMessage());
      return false;
    }
  }

  /**
   * 验证UDP配置
   *
   * @param configuration 配置JSON字符串
   * @return 是否有效
   */
  private static boolean validateUdpConfig(String configuration) {
    try {
      JSONObject config = JSONUtil.parseObj(configuration);
      Integer port = config.getInt("port");
      
      // 端口号必填且有效
      if (port == null || port <= 0 || port > 65535) {
        log.warn("UDP配置验证失败: 端口号无效 - {}", port);
        return false;
      }
      
      return true;
    } catch (Exception e) {
      log.warn("UDP配置验证失败: 解析异常 - {}", e.getMessage());
      return false;
    }
  }

  /**
   * 获取WebSocket默认配置
   *
   * @return 默认WebSocket配置
   */
  public static String getDefaultWebSocketConfig() {
    JSONObject config = new JSONObject();
    config.set("host", ""); // 客户端必填，服务端不需要
    config.set("port", 9001);
    config.set("path", "/ws");
    config.set("clientId", ""); // 可选，不填则自动生成UUID
    config.set("username", ""); // 可选
    config.set("password", ""); // 可选
    config.set("maxConnections", 1000);
    config.set("allowOrigins", true);
    return config.toString();
  }

  /**
   * 获取UDP默认配置
   *
   * @return 默认UDP配置
   */
  public static String getDefaultUdpConfig() {
    JSONObject config = new JSONObject();
    config.set("host", "0.0.0.0");
    config.set("port", 8889);
    config.set("bufferSize", 2048);
    return config.toString();
  }
}
