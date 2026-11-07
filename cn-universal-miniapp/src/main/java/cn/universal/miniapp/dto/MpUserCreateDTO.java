/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建用户DTO
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MpUserCreateDTO {

  /** 小程序APPID */
  private String appId;

  /** 用户名（账号密码登录） */
  private String username;

  /** 密码 */
  private String password;

  /** 手机号 */
  private String phone;

  /** 昵称 */
  private String nickname;

  /** 真实姓名 */
  private String realName;

  /** 备注 */
  private String remark;

  /** 权限列表 */
  private List<String> permissions;
}
