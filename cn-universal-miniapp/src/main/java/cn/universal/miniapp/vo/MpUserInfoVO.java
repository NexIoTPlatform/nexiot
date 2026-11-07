/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小程序用户信息视图对象（含权限）
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "小程序用户信息")
public class MpUserInfoVO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "用户ID")
  private Long userId;

  @Schema(description = "小程序APPID")
  private String appId;

  @Schema(description = "昵称")
  private String nickname;

  @Schema(description = "头像URL")
  private String avatar;

  @Schema(description = "手机号")
  private String phone;

  @Schema(description = "状态：0-待绑定 1-已绑定 2-禁用")
  private Integer status;

  @Schema(description = "用户权限列表")
  private List<String> permissions;

  @Schema(description = "Token")
  private String token;

  @Schema(description = "Token过期时间")
  private Long tokenExpireTime;
}
