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

import lombok.Data;

/**
 * EMQX 认证请求 DTO 对应 EMQX 发送的认证请求参数
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Data
public class EmqxAuthRequest {

  /** 用户名 */
  private String username;

  /** 密码 */
  private String password;

  /** 客户端ID */
  private String clientid;

  /** 协议名称 */
  private String protocol;

  /** 协议版本 */
  private String proto_ver;

  /** 连接来源IP */
  private String ip_address;

  /** 连接来源端口 */
  private String port;

  /** 连接来源 */
  private String mountpoint;
}
