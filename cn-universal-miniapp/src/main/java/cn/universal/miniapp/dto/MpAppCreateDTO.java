/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建应用DTO
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MpAppCreateDTO {

  /** 小程序APPID */
  private String appId;

  /** 小程序密钥 */
  private String appSecret;

  /** 应用名称 */
  private String name;

  /** 应用Logo */
  private String logo;

  /** 应用描述 */
  private String description;

  /** 数据源类型：1-关联已有应用 2-AK/SK自定义 */
  private Integer dataSourceType;

  /** 小程序访问二维码 */
  private String qrcodeImage;

  /** 小程序访问URL（用于二维码链接） */
  private String accessUrl;

  /** 关联的IoT应用ID */
  private String linkedAppId;

  /** Access Key */
  private String ak;

  /** Secret Key */
  private String sk;

  /** 主题色 */
  private String primaryColor;

  /** 欢迎语 */
  private String welcomeText;

  /** 客服电话 */
  private String contactPhone;

  /** 客服邮箱 */
  private String contactEmail;
}
