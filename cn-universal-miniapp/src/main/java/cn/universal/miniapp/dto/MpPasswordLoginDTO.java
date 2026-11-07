/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.dto;

import lombok.Data;

/**
 * 账号密码登录DTO
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Data
public class MpPasswordLoginDTO {

  /** 小程序APPID */
  private String appId;

  /** 用户名 */
  private String username;

  /** 密码 */
  private String password;
}
