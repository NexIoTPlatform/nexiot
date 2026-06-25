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

package cn.universal.admin.system.service;

import cn.universal.persistence.entity.admin.SysMenu;
import cn.universal.persistence.entity.admin.TreeSelect;
import cn.universal.persistence.entity.admin.vo.RouterVo;
import java.util.List;
import java.util.Set;

/** 菜单业务层接口 */
public interface ISysMenuService {

  /**
   * 根据用户查询系统菜单列表
   *
   * @param unionId 用户ID
   * @return 菜单列表
   */
  public List<SysMenu> selectMenuList(String unionId);

  /**
   * 根据用户查询系统菜单列表
   *
   * @param menu 菜单信息
   * @param unionId 用户unionId
   * @return 菜单列表
   */
  public List<SysMenu> selectMenuList(SysMenu menu, String unionId);

  /**
   * 根据用户ID查询权限
   *
   * @param unionId 用户unionId
   * @return 权限列表
   */
  public Set<String> selectMenuPermsByUserId(String unionId);

  /**
   * 根据用户ID查询菜单树信息
   *
   * @param unionId 用户unionId
   * @return 菜单列表
   */
  public List<SysMenu> selectMenuTreeByUnionId(String unionId);

  /**
   * 根据角色ID查询菜单树信息
   *
   * @param roleId 角色ID
   * @return 选中菜单列表
   */
  public List<Long> selectMenuListByRoleId(Long roleId);

  /**
   * 构建前端路由所需要的菜单
   *
   * @param menus 菜单列表
   * @return 路由列表
   */
  public List<RouterVo> buildMenus(List<SysMenu> menus);

  /**
   * 构建前端所需要树结构
   *
   * @param menus 菜单列表
   * @return 树结构列表
   */
  public List<SysMenu> buildMenuTree(List<SysMenu> menus);

  /**
   * 构建前端所需要下拉树结构
   *
   * @param menus 菜单列表
   * @return 下拉树结构列表
   */
  public List<TreeSelect> buildMenuTreeSelect(List<SysMenu> menus);

  /**
   * 根据菜单ID查询信息
   *
   * @param menuId 菜单ID
   * @return 菜单信息
   */
  public SysMenu selectMenuById(Long menuId);

  /**
   * 是否存在菜单子节点
   *
   * @param menuId 菜单ID
   * @return 结果 true 存在 false 不存在
   */
  public boolean hasChildByMenuId(Long menuId);

  /**
   * 查询菜单是否存在角色
   *
   * @param menuId 菜单ID
   * @return 结果 true 存在 false 不存在
   */
  public boolean checkMenuExistRole(Long menuId);

  /**
   * 新增保存菜单信息
   *
   * @param menu 菜单信息
   * @return 结果
   */
  public int insertMenu(SysMenu menu);

  /**
   * 修改保存菜单信息
   *
   * @param menu 菜单信息
   * @return 结果
   */
  public int updateMenu(SysMenu menu);

  /**
   * 删除菜单管理信息
   *
   * @param menuId 菜单ID
   * @return 结果
   */
  public int deleteMenuById(Long menuId);

  /**
   * 校验菜单名称是否唯一
   *
   * @param menu 菜单信息
   * @return 结果
   */
  public String checkMenuNameUnique(SysMenu menu);

}
