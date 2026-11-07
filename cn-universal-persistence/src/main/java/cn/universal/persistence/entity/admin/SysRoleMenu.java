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

/** 角色和菜单关联 sys_role_menu @Author ruoyi */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@AllArgsConstructor
@Builder
@Table(name = "sys_role_menu")
@Schema(description = "角色菜单关联实体")
public class SysRoleMenu implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @KeySql(genId = SQenGenId.class)
  @Schema(description = "UUID")
  private Long uuid;

  /** 角色ID */
  @Schema(description = "角色ID")
  private Long roleId;

  /** 菜单ID */
  @Schema(description = "菜单ID")
  private Long menuId;
}
