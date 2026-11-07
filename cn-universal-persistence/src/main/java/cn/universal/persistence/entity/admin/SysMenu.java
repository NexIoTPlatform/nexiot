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

/** 菜单权限表 sys_menu @Author ruoyi */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Table(name = "sys_menu")
@Schema(description = "系统菜单实体")
public class SysMenu implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 菜单ID */
  @Id
  @Schema(description = "菜单ID")
  @KeySql(genId = SQenGenId.class)
  private Long menuId;

  /** 菜单名称 */
  @Column(name = "menu_name")
  @Schema(description = "菜单名称")
  private String menuName;

  /** 父菜单名称 */
  //  @Column(name = "parent_name")
  //  private String parentName;

  /** 父菜单ID */
  @Column(name = "parent_id")
  @Schema(description = "父菜单ID")
  private Long parentId;

  /** 显示顺序 */
  @Column(name = "order_num")
  @Schema(description = "显示顺序")
  private Integer orderNum;

  /** 路由地址 */
  @Schema(description = "路由地址")
  private String path;

  /** 组件路径 */
  @Schema(description = "组件路径")
  private String component;

  /** 是否为外链（0是 1否） */
  @Column(name = "is_frame")
  @Schema(description = "是否为外链（0是 1否）")
  private Integer isFrame;

  /** 是否缓存（0缓存 1不缓存） */
  @Column(name = "is_cache")
  @Schema(description = "是否缓存（0缓存 1不缓存）")
  private Integer isCache;

  /** 类型（M目录 C菜单 F按钮） */
  @Column(name = "menu_type")
  @Schema(description = "菜单类型（M目录 C菜单 F按钮）")
  private String menuType;

  /** 显示状态（0显示 1隐藏） */
  @Schema(description = "显示状态（0显示 1隐藏）")
  private String visible;

  /** 菜单状态（0显示 1隐藏） */
  @Schema(description = "菜单状态（0显示 1隐藏）")
  private String status;

  /** 权限字符串 */
  @Schema(description = "权限字符串")
  private String perms;

  /** 菜单图标 */
  @Schema(description = "菜单图标")
  private String icon;

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
  @Builder.Default
  @Schema(description = "请求参数")
  private Map<String, Object> params = new HashMap<>();

  /** 子菜单 */
  @Builder.Default
  @Schema(description = "子菜单列表")
  private List<SysMenu> children = new ArrayList<SysMenu>();
}
