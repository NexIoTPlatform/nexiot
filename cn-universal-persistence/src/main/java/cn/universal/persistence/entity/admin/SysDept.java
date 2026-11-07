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

import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import tk.mybatis.mapper.annotation.KeySql;

/** 部门表 sys_dept */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Table(name = "sys_dept")
@Schema(description = "系统部门实体")
public class SysDept implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 部门ID */
  @Id
  @KeySql(genId = SQenGenId.class)
  @Schema(description = "部门ID")
  private Long deptId;

  /** 父部门ID */
  @Column(name = "parent_id")
  @Schema(description = "父部门ID")
  private Long parentId;

  /** 祖级列表 */
  @Schema(description = "祖级列表")
  private String ancestors;

  /** 部门名称 */
  @Column(name = "dept_name")
  @Schema(description = "部门名称")
  private String deptName;

  /** 显示顺序 */
  @Column(name = "order_num")
  @Schema(description = "显示顺序")
  private Integer orderNum;

  /** 负责人 */
  @Schema(description = "负责人")
  private String leader;

  /** 联系电话 */
  @Schema(description = "联系电话")
  private String phone;

  /** 邮箱 */
  @Schema(description = "邮箱")
  private String email;

  /** 部门状态（0正常 1停用） */
  @Schema(description = "部门状态")
  private String status;

  /** 租户ID */
  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  /** 创建者 */
  @Column(name = "create_by")
  @Schema(description = "创建者")
  private String createBy;

  /** 创建时间 */
  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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
  @Transient
  @Builder.Default
  @Schema(description = "请求参数")
  private Map<String, Object> params = new HashMap<>();

  /** 子部门 */
  @Transient
  @Builder.Default
  @Schema(description = "子部门")
  private List<SysDept> children = new ArrayList<>();
}
