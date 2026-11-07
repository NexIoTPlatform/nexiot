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

package cn.universal.persistence.entity.bo;

import cn.universal.common.annotation.Excel;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table(name = "iot_user")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IoTUserBO implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id private Long id;

  /** 归属组织机构 */
  @Column(name = "org_id")
  private Long orgId;

  /** 租户ID */
  @Column(name = "tenant_id")
  private String tenantId;

  /** 租户角色 */
  @Column(name = "tenant_role")
  private String tenantRole;

  /** 部门ID */
  @Column(name = "dept_id")
  private Long deptId;

  /** 用户名 */
  private String username;

  /** 密码 */
  private String password;

  /** 别名 */
  private String alias;

  /** 用户唯一标识 */
  @Column(name = "union_id")
  private String unionId;

  /** 邮箱 */
  private String email;

  /** 密码加盐 */
  private String salt;

  /** 手机号 */
  private String mobile;

  /** 账号状态（0正常，1停用） */
  private Integer status;

  /** 历史上级账号唯一id（兼容字段） */
  @Excel(name = "历史上级账号唯一id（兼容字段）")
  private String parentUnionId;

  /** 账号身份 0.平台管理员 1.租户账号 2.历史兼容账号 */
  @Excel(name = "账号身份 0.平台管理员 1.租户账号 2.历史兼容账号")
  private Integer identity;

  /** 头像 */
  private String avatar;

  /** 登录IP */
  @Column(name = "login_ip")
  private String loginIp;

  /** 登录时间 */
  @Column(name = "login_date")
  private Date loginDate;

  /** 创建者 */
  @Column(name = "create_by")
  private String createBy;

  /** 创建时间 */
  @Column(name = "create_date")
  private Date createDate;

  @Column(name = "update_by")
  private String updateBy;

  @Column(name = "update_date")
  private Date updateDate;

  /** 备注 */
  private String remark;

  /** 0-正常，1-删除 */
  private Integer deleted;

  private Set<String> roles;

  /** 角色组 */
  private Long[] roleIds;

  /** 角色ID */
  private Long roleId;

  private String cfg;

  @Builder.Default
  private List<Long> scopeDeptIds = new ArrayList<>();

  @Builder.Default
  private List<String> scopeUnionIds = new ArrayList<>();

  private boolean scopeDenied;
}
