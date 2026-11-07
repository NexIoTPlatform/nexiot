/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * 小程序用户实体
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Table(name = "mp_user")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MpUser implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 所属小程序APPID */
  @Column(name = "app_id")
  private String appId;

  /** 用户名（账号密码登录） */
  private String username;

  /** 密码（BCrypt加密） */
  @JsonIgnore private String password;

  /** 微信OpenID */
  @Column(name = "open_id")
  private String openId;

  /** 微信UnionID */
  @Column(name = "union_id")
  private String unionId;

  /** 手机号 */
  private String phone;

  /** 昵称 */
  private String nickname;

  /** 头像URL */
  private String avatar;

  /** 真实姓名 */
  @Column(name = "real_name")
  private String realName;

  /** 备注 */
  private String remark;

  /** 状态：0-待绑定 1-已绑定 2-禁用 */
  private Integer status;

  /** 绑定时间 */
  @Column(name = "bind_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date bindTime;

  /** 最后登录时间 */
  @Column(name = "last_login_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date lastLoginTime;

  /** 最后登录IP */
  @Column(name = "last_login_ip")
  private String lastLoginIp;

  /** 创建者ID */
  @Column(name = "creator_id")
  private String creatorId;

  /** 创建时间 */
  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date createTime;

  /** 更新时间 */
  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date updateTime;

  /** 删除标记：0-正常 1-已删除 */
  private Integer deleted;

  /** 后台可访问的小程序范围（非数据库字段） */
  @Builder.Default
  @Transient
  private List<String> scopeAppIds = new ArrayList<>();

  /** 是否无权限访问（非数据库字段） */
  @Builder.Default
  @Transient
  private boolean scopeDenied = false;

  /** 用户状态枚举 */
  public static class Status {
    public static final int PENDING = 0; // 待绑定
    public static final int BINDOUND = 1; // 已绑定
    public static final int DISABLED = 2; // 禁用
  }
}
