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

package cn.universal.persistence.entity.vo;

import jakarta.persistence.Column;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信用户信息对象 wx_user_info @Author gitee.com/NexIoT
 *
 * @since 2025-10-19
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WxMiniUserInfoVO implements Serializable {

  private static final long serialVersionUID = 1L;

  /** $column.columnComment */
  private Long uuid;

  // 用户是否订阅该公众号标识，值为0时，代表此用户没有关注该公众号。
  private Integer subscribe;

  /** 用户微信openid */
  @Column(name = "open_id")
  private String openId;

  /** 开放平台id */
  @Column(name = "union_id")
  private String unionId;

  /** 微信名 */
  @Column(name = "nick_name")
  private String nickName;

  /** 公众号id */
  @Column(name = "app_id")
  private String appId;

  /** 微信头像 */
  @Column(name = "head_img_url")
  private String headImgUrl;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "update_time")
  private Date updateTime;

  /** 手机 */
  @Column(name = "phone")
  private String phone;
}
