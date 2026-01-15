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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 网络类型工具类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j
public class NetworkTypeUtil {

  /** 支持的网络类型列表 */
  public static final List<String> SUPPORTED_TYPES =
      Arrays.asList("TCP_CLIENT", "TCP_SERVER", "MQTT_CLIENT", "MQTT_SERVER", "WEB_SOCKET_CLIENT", "WEB_SOCKET_SERVER", "UDP");

  /**
   * 解析网络类型字符串，支持逗号分隔的多个类型
   *
   * @param typeStr 类型字符串，如 "MQTT_CLIENT,MQTT_SERVER"
   * @return 类型列表
   */
  public static List<String> parseTypes(String typeStr) {
    if (StrUtil.isBlank(typeStr)) {
      return List.of();
    }

    // 分割并清理空白字符
    return Arrays.stream(typeStr.split(","))
        .map(String::trim)
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toList());
  }

  /**
   * 验证网络类型是否有效
   *
   * @param type 网络类型
   * @return 是否有效
   */
  public static boolean isValidType(String type) {
    return SUPPORTED_TYPES.contains(type);
  }

  /**
   * 验证多个网络类型是否都有效
   *
   * @param types 网络类型列表
   * @return 是否都有效
   */
  public static boolean isValidTypes(List<String> types) {
    return types.stream().allMatch(NetworkTypeUtil::isValidType);
  }

  /**
   * 获取无效的类型列表
   *
   * @param types 网络类型列表
   * @return 无效的类型列表
   */
  public static List<String> getInvalidTypes(List<String> types) {
    return types.stream().filter(type -> !isValidType(type)).collect(Collectors.toList());
  }

  /**
   * 格式化类型字符串（去重、排序）
   *
   * @param typeStr 原始类型字符串
   * @return 格式化后的类型字符串
   */
  public static String formatTypes(String typeStr) {
    List<String> types = parseTypes(typeStr);
    return types.stream().distinct().sorted().collect(Collectors.joining(","));
  }

  /**
   * 检查是否包含指定类型
   *
   * @param typeStr 类型字符串
   * @param targetType 目标类型
   * @return 是否包含
   */
  public static boolean containsType(String typeStr, String targetType) {
    List<String> types = parseTypes(typeStr);
    return types.contains(targetType);
  }

  /**
   * 获取类型的中文名称
   *
   * @param type 类型代码
   * @return 中文名称
   */
  public static String getTypeName(String type) {
    return switch (type) {
      case "TCP_CLIENT" -> "TCP客户端";
      case "TCP_SERVER" -> "TCP服务端";
      case "MQTT_CLIENT" -> "MQTT客户端";
      case "MQTT_SERVER" -> "MQTT服务端";
      case "WEB_SOCKET_CLIENT" -> "WebSocket客户端";
      case "WEB_SOCKET_SERVER" -> "WebSocket服务端";
      case "UDP" -> "UDP服务";
      default -> type;
    };
  }

  /**
   * 获取多个类型的中文名称
   *
   * @param types 类型列表
   * @return 中文名称列表
   */
  public static List<String> getTypeNames(List<String> types) {
    return types.stream().map(NetworkTypeUtil::getTypeName).collect(Collectors.toList());
  }
}
