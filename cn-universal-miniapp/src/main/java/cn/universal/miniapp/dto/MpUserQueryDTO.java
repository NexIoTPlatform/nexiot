/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户查询DTO
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MpUserQueryDTO {

  /** 小程序APPID */
  private String appId;

  /** 手机号 */
  private String phone;

  /** 昵称 */
  private String nickname;

  /** 状态 */
  private Integer status;

  /** 页码 */
  @Builder.Default private int pageNum = 1;

  /** 每页数量 */
  @Builder.Default private int pageSize = 10;
}
