/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoTXin 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoTXin
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */
package cn.universal.common.enums;

public enum NetworkType {
  TCP_CLIENT("TCP_CLIENT", "TCP客户端"),
  TCP_SERVER("TCP_SERVER", "TCP服务"),
  // MQTT
  MQTT_CLIENT("MQTT_CLIENT", "MQTT客户端"),
  MQTT_SERVER("MQTT_SERVER", "MQTT服务"),

  HTTP_CLIENT("HTTP_CLIENT", "HTTP客户端"),
  HTTP_SERVER("HTTP_SERVER", "HTTP服务"),

  WEB_SOCKET_CLIENT("WEB_SOCKET_CLIENT", "WebSocket客户端"),
  WEB_SOCKET_SERVER("WEB_SOCKET_SERVER", "WebSocket服务"),

  UDP("UDP", "UDP服务"),

  COAP_CLIENT("COAP_CLIENT", "CoAP客户端"),
  COAP_SERVER("COAP_SERVER", "CoAP服务");

  private final String id;
  private final String description;

  NetworkType(String id, String description) {
    this.id = id;
    this.description = description;
  }

  public String getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }
}
