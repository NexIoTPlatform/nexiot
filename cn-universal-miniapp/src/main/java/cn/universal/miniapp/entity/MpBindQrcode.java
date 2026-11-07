/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * 小程序绑定二维码实体
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Table(name = "mp_bind_qrcode")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MpBindQrcode implements Serializable {

  private static final long serialVersionUID = 1L;
  @KeySql(genId = SQenGenId.class)
  @Id private Long id;

  /** 二维码唯一码 */
  private String code;

  /** 小程序APPID */
  @Column(name = "app_id")
  private String appId;

  /** 目标绑定用户ID */
  @Column(name = "target_user_id")
  private Long targetUserId;

  /** 过期时间 */
  @Column(name = "expire_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date expireTime;

  /** 状态：0-待使用 1-已使用 2-已过期 */
  private Integer status;

  /** 使用者OpenID */
  @Column(name = "used_open_id")
  private String usedOpenId;

  /** 使用时间 */
  @Column(name = "used_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date usedTime;

  /** 创建时间 */
  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date createTime;

  /** 二维码状态常量 */
  public static class Status {
    public static final int PENDING = 0; // 待使用
    public static final int USED = 1; // 已使用
    public static final int EXPIRED = 2; // 已过期
  }
}
