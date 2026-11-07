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
 * 小程序用户权限实体
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Table(name = "mp_user_permission")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MpUserPermission implements Serializable {

  private static final long serialVersionUID = 1L;
  @KeySql(genId = SQenGenId.class)
  @Id private Long id;

  /** 用户ID */
  @Column(name = "user_id")
  private Long userId;

  /** 小程序APPID */
  @Column(name = "app_id")
  private String appId;

  /** 权限码 */
  @Column(name = "permission_code")
  private String permissionCode;

  /** 创建时间 */
  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date createTime;

  /** 权限码常量 */
  public static class PermissionCode {
    /** 查看设备（仅设备列表与详情） */
    public static final String DEVICE_VIEW = "DEVICE_VIEW";

    /** 查看所有设备（管理员，全量可见） */
    public static final String DEVICE_VIEW_ALL = "DEVICE_VIEW_ALL";

    /** 设备添加权限 */
    public static final String DEVICE_ADD = "DEVICE_ADD";

    /** 设备删除权限 */
    public static final String DEVICE_DELETE = "DEVICE_DELETE";

    /** 功能调用权限 */
    public static final String FUNCTION_CALL = "FUNCTION_CALL";

    /** 设备解绑权限 */
    public static final String DEVICE_UNBIND = "DEVICE_UNBIND";
  }
}
