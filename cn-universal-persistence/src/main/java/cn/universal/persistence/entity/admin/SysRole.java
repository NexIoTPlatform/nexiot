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

package cn.universal.persistence.entity.admin;

import cn.universal.common.annotation.Excel;
import cn.universal.persistence.common.typehandler.PostgreSqlNumericBooleanTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.ibatis.type.JdbcType;
import tk.mybatis.mapper.annotation.ColumnType;

/** 角色表 sys_role @Author ruoyi */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@AllArgsConstructor
@Builder
@Table(name = "sys_role")
@Schema(description = "系统角色实体")
public class SysRole implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 角色ID */
  @Id
  @Schema(description = "角色ID")
  private Long roleId;

  /** 角色名称 */
  @Excel(name = "角色名称")
  @Column(name = "role_name")
  @Schema(description = "角色名称")
  private String roleName;

  /** 角色权限 */
  @Column(name = "role_key")
  @Schema(description = "角色权限键")
  private String roleKey;

  /** 角色排序 */
  @Column(name = "role_sort")
  @Schema(description = "角色排序")
  private Integer roleSort;

  /** 数据范围（1：所有数据权限；2：自定义数据权限；3：本部门数据权限；4：本部门及以下数据权限；5：仅本人数据权限） */
  @Column(name = "data_scope")
  @Schema(description = "数据范围")
  private String dataScope;

  /** 租户ID。平台角色为空，租户角色必须归属到一个租户。 */
  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  /** 角色类型：PLATFORM_ADMIN / TENANT_ADMIN / TENANT_CUSTOM */
  @Column(name = "role_type")
  @Schema(description = "角色类型")
  private String roleType;

  /** 菜单树选择项是否关联显示（ 0：父子不互相关联显示 1：父子互相关联显示） */
  @Column(name = "menu_check_strictly")
  // PG/达梦保留 numeric smallint 标记位，避免直接把 Boolean 绑定成数据库 boolean。
  @ColumnType(
      jdbcType = JdbcType.SMALLINT,
      typeHandler = PostgreSqlNumericBooleanTypeHandler.class)
  @Schema(description = "菜单检查严格模式")
  private Boolean menuCheckStrictly;

  /** 部门树选择项是否关联显示（0：父子不互相关联显示 1：父子互相关联显示 ） */
  @Column(name = "dept_check_strictly")
  // PG/达梦保留 numeric smallint 标记位，避免直接把 Boolean 绑定成数据库 boolean。
  @ColumnType(
      jdbcType = JdbcType.SMALLINT,
      typeHandler = PostgreSqlNumericBooleanTypeHandler.class)
  @Schema(description = "部门检查严格模式")
  private Boolean deptCheckStrictly;

  /** 角色状态（0正常 1停用） */
  @Schema(description = "角色状态（0正常 1停用）")
  private String status;

  /** 创建者 */
  @Column(name = "create_by")
  @Schema(description = "创建者")
  private String createBy;

  /** 创建时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Column(name = "create_time")
  @Schema(description = "创建时间")
  private Date createTime;

  /** 更新者 */
  @Column(name = "update_by")
  @Schema(description = "更新者")
  private String updateBy;

  /** 更新时间 */
  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "更新时间")
  private Date updateTime;

  /** 备注 */
  @Schema(description = "备注")
  private String remark;

  /** 请求参数 */
  @Builder.Default
  @Schema(description = "请求参数")
  private Map<String, Object> params = new HashMap<>();

  /** 用户是否存在此角色标识 默认不存在 */
  @Schema(description = "角色标识")
  private boolean flag = false;

  /** 菜单组 */
  @Schema(description = "菜单ID数组")
  private Long[] menuIds;

  /** 部门组（数据权限） */
  @Schema(description = "部门ID数组")
  private Long[] deptIds;
  public SysRole(Long roleId) {
    this.roleId = roleId;
  }

  public boolean isAdmin() {
    return isAdmin(this.roleId);
  }

  public static boolean isAdmin(Long roleId) {
    return roleId != null && 1L == roleId;
  }
}
