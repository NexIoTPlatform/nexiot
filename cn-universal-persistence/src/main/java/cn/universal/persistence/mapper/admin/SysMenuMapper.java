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
import cn.universal.persistence.entity.admin.SysMenu;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 菜单表 数据层 @Author ruoyi */
// @Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

  /**
   * 根据用户所有权限
   *
   * @return 权限列表
   */
  public List<String> selectMenuPerms();

  /** 按条件查询系统菜单列表 */
  public List<SysMenu> selectMenuList(SysMenu menu);

  /**
   * 根据用户查询系统菜单列表
   *
   * @param menu 菜单信息
   * @return 菜单列表
   */
  public List<SysMenu> selectMenuListByUserId(SysMenu menu);

  /**
   * 根据用户ID查询权限
   *
   * @param unionId 用户unionId
   * @return 权限列表
   */
  public List<String> selectMenuPermsByUserId(String unionId);

  /**
   * 根据用户ID查询菜单
   *
   * @return 菜单列表
   */
  public List<SysMenu> selectMenuTreeAll();

  /**
   * 根据用户ID查询菜单
   *
   * @param unionId 用户ID
   * @return 菜单列表
   */
  public List<SysMenu> selectMenuTreeByUnionId(@Param("unionId") String unionId);

  /**
   * 根据角色ID查询菜单树信息
   *
   * @param roleId 角色ID
   * @param menuCheckStrictly 菜单树选择项是否关联显示
   * @return 选中菜单列表
   */
  public List<Long> selectMenuListByRoleId(
      @Param("roleId") Long roleId, @Param("menuCheckStrictly") boolean menuCheckStrictly);

}
