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

package cn.universal.persistence.mapper.admin;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.admin.SysRole;
import com.github.pagehelper.Page;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.List;

/** 角色表 数据层 @Author ruoyi */
// @Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

  Page<SysRole> selectPageRoleList(@Param("page") Page<SysRole> page, @Param("role") SysRole role);

  /**
   * 根据条件分页查询角色数据
   *
   * @param role 角色信息
   * @return 角色数据集合信息
   */
  public List<SysRole> selectRoleList(SysRole role);

  /**
   * 根据用户ID查询角色
   *
   * @param unionId 用户unionId
   * @return 角色列表
   */
  public List<SysRole> selectRolePermissionByUnionId(String unionId);

  /**
   * 根据用户ID获取角色选择框列表
   *
   * @param unionId 用户unionId
   * @return 选中角色ID列表
   */
  public List<Long> selectRoleListByUnionId(String unionId);
}
