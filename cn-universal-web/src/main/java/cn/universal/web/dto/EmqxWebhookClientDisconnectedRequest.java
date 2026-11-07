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
package cn.universal.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * EMQX Webhook 客户端断开事件请求 DTO。
 *
 * <p>兼容 EMQX client.disconnected 事件常见字段。
 */
@Data
public class EmqxWebhookClientDisconnectedRequest {

  /** 事件名，通常为 client.disconnected */
  private String event;

  /** 客户端ID */
  private String clientid;

  /** 用户名 */
  private String username;

  /** 断开时间 */
  @JsonProperty("disconnected_at")
  private Long disconnectedAt;

  /** 连接建立时间 */
  @JsonProperty("connected_at")
  private Long connectedAt;

  /** 断开原因 */
  private String reason;

  /** 协议名称 */
  private String protocol;

  /** 协议版本 */
  @JsonProperty("proto_ver")
  private String protoVer;

  /** 连接来源 IP */
  @JsonProperty("ip_address")
  private String ipAddress;

  /** 连接来源端口 */
  private Integer port;

  /** 挂载点 */
  private String mountpoint;
}
