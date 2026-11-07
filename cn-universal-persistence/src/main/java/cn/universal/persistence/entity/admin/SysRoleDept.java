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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import tk.mybatis.mapper.annotation.KeySql;

/** 角色和部门关联 sys_role_dept */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Table(name = "sys_role_dept")
@Schema(description = "角色部门关联实体")
public class SysRoleDept implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @KeySql(genId = SQenGenId.class)
  @Schema(description = "UUID")
  private Long uuid;

  /** 角色ID */
  @Schema(description = "角色ID")
  private Long roleId;

  /** 部门ID */
  @Schema(description = "部门ID")
  private Long deptId;
}
