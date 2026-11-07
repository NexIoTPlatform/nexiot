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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table(name = "iot_user")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT用户VO")
public class IoTUserVO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "用户ID")
  @Id
  private Long id;

  /** 归属组织机构 */
  @Column(name = "org_id")
  @Schema(description = "组织ID")
  private Long orgId;

  /** 租户ID */
  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  /** 租户角色 */
  @Column(name = "tenant_role")
  @Schema(description = "租户角色")
  private String tenantRole;

  /** 部门ID */
  @Column(name = "dept_id")
  @Schema(description = "部门ID")
  private Long deptId;

  /** 用户名 */
  @Schema(description = "用户名")
  private String username;

  /** 密码 */
  @JsonIgnore
  @Schema(description = "密码")
  private String password;

  /** 别名 */
  @Schema(description = "用户别名")
  private String alias;

  /** 用户唯一标识 */
  @Column(name = "union_id")
  @Schema(description = "用户唯一标识")
  private String unionId;

  /** 邮箱 */
  @Schema(description = "邮箱地址")
  private String email;

  /** 密码加盐 */
  @Schema(description = "密码盐值")
  private String salt;

  /** 手机号 */
  @Schema(description = "手机号码")
  private String mobile;

  /** 账号状态（0正常，1停用） */
  @Schema(description = "账号状态")
  private Integer status;

  /** 头像 */
  @Schema(description = "头像URL")
  private String avatar;

  /** 历史上级账号唯一id（兼容字段） */
  @Schema(description = "历史上级账号标识（兼容字段）")
  private String parentUnionId;

  /** 账号身份 0.平台管理员 1.租户账号 2.历史兼容账号 */
  @Schema(description = "用户身份")
  private Integer identity;

  /** 登录IP */
  @Column(name = "login_ip")
  @Schema(description = "登录IP地址")
  private String loginIp;

  /** 登录时间 */
  @Column(name = "login_date")
  @Schema(description = "登录时间")
  private Date loginDate;

  /** 创建者 */
  @Column(name = "create_by")
  @Schema(description = "创建者")
  private String createBy;

  /** 创建时间 */
  @Column(name = "create_date")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createDate;

  @Column(name = "update_by")
  @Schema(description = "更新者")
  private String updateBy;

  @Column(name = "update_date")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "更新时间")
  private Date updateDate;

  /** 备注 */
  @Schema(description = "备注信息")
  private String remark;

  /** 0-正常，1-删除 */
  @Schema(description = "删除状态")
  private Integer deleted;

  @Schema(description = "部门名称")
  private String deptName;

  @Schema(description = "角色ID列表")
  private List<Long> roleIds;

  /** 请求参数 */
  @Builder.Default
  @Schema(description = "请求参数")
  private Map<String, Object> params = new HashMap<>();

  public IoTUserVO(Long userId) {
    this.id = userId;
  }

  public boolean isAdmin() {
    return identity != null && identity == 0;
  }
}
