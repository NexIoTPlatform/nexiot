/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信登录DTO
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MpWxLoginDTO {

  /** 微信登录code */
  private String wxCode;

  /** 绑定二维码code */
  private String bindCode;

  /** 微信OpenID（测试用） */
  private String openId;

  /** 微信UnionID */
  private String unionId;
}
