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

package cn.universal.admin.network.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 网络类型枚举
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Getter
@AllArgsConstructor
public enum NetworkTypeEnum {

  /** TCP客户端 */
  TCP_CLIENT("TCP_CLIENT", "TCP客户端", "tcp_parser_type"),

  /** TCP服务端 */
  TCP_SERVER("TCP_SERVER", "TCP服务端", "tcp_parser_type"),

  /** MQTT客户端 */
  MQTT_CLIENT("MQTT_CLIENT", "MQTT客户端", "mqtt_parser_type"),

  /** MQTT服务端 */
  MQTT_SERVER("MQTT_SERVER", "MQTT服务端", "mqtt_parser_type"),

  /** HTTP客户端 */
  HTTP_CLIENT("HTTP_CLIENT", "HTTP客户端", "http_operation_type"),

  /** HTTP服务端 */
  HTTP_SERVER("HTTP_SERVER", "HTTP服务端", "http_operation_type");

  /** 类型代码 */
  private final String code;

  /** 类型名称 */
  private final String name;

  /** 字典类型 */
  private final String dictType;

  /**
   * 根据代码获取枚举
   *
   * @param code 类型代码
   * @return 枚举
   */
  public static NetworkTypeEnum getByCode(String code) {
    for (NetworkTypeEnum type : values()) {
      if (type.getCode().equals(code)) {
        return type;
      }
    }
    return null;
  }

  /**
   * 根据代码获取名称
   *
   * @param code 类型代码
   * @return 类型名称
   */
  public static String getNameByCode(String code) {
    NetworkTypeEnum type = getByCode(code);
    return type != null ? type.getName() : code;
  }

  /**
   * 根据代码获取字典类型
   *
   * @param code 类型代码
   * @return 字典类型
   */
  public static String getDictTypeByCode(String code) {
    NetworkTypeEnum type = getByCode(code);
    return type != null ? type.getDictType() : null;
  }
}
