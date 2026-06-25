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

package cn.universal.admin.system.service.impl;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.universal.admin.system.service.ISysMenuService;
import cn.universal.common.constant.Constants;
import cn.universal.common.constant.IoTUserConstants;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.admin.SysMenu;
import cn.universal.persistence.entity.admin.SysRole;
import cn.universal.persistence.entity.admin.SysRoleMenu;
import cn.universal.persistence.entity.admin.TreeSelect;
import cn.universal.persistence.entity.admin.vo.MetaVo;
import cn.universal.persistence.entity.admin.vo.RouterVo;
import cn.universal.persistence.mapper.admin.SysMenuMapper;
import cn.universal.persistence.mapper.admin.SysRoleMapper;
import cn.universal.persistence.mapper.admin.SysRoleMenuMapper;
import cn.universal.security.service.IoTUserService;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/** 菜单 业务层处理 @Author ruoyi */
@Service
public class SysMenuServiceImpl implements ISysMenuService {

  public static final String PREMISSION_STRING = "perms[\"{0}\"]";

  @Resource private SysRoleMapper roleMapper;

  @Resource private SysRoleMenuMapper roleMenuMapper;
  @Resource private SysMenuMapper menuMapper;
  @Resource private IoTUserService ioTUserService;

  /**
   * 根据用户查询系统菜单列表
   *
   * @param unionId 用户unionId
   * @return 菜单列表
   */
  @Override
  public List<SysMenu> selectMenuList(String unionId) {
    return selectMenuList(new SysMenu(), unionId);
  }

  /**
   * 查询系统菜单列表
   *
   * @param menu 菜单信息
   * @return 菜单列表
   */
  @Override
  @Transactional
  public List<SysMenu> selectMenuList(SysMenu menu, String unionId) {
    List<SysMenu> menuList = null;
    // 管理员显示所有菜单信息
    IoTUser iotUser = ioTUserService.selectUserByUnionId(unionId);
    if (iotUser.isAdmin()) {
      menuList = menuMapper.selectMenuList(menu);
    } else {
      menu.getParams().put("unionId", unionId);
      menuList = menuMapper.selectMenuListByUserId(menu);
    }
    return menuList;
  }

  /**
   * 根据用户ID查询权限
   *
   * @param unionId 用户unionId
   * @return 权限列表
   */
  @Override
  public Set<String> selectMenuPermsByUserId(String unionId) {
    List<String> perms = menuMapper.selectMenuPermsByUserId(unionId);
    Set<String> permsSet = new HashSet<>();
    for (String perm : perms) {
      if (Validator.isNotEmpty(perm)) {
        permsSet.addAll(Arrays.asList(perm.trim().split(",")));
      }
    }
    return permsSet;
  }

  @Override
  public List<SysMenu> selectMenuTreeByUnionId(String unionId) {
    List<SysMenu> menus = null;
    IoTUser iotUser = ioTUserService.selectUserByUnionId(unionId);
    if (iotUser.isAdmin()) {
      menus = menuMapper.selectMenuTreeAll();
    } else {
      menus = menuMapper.selectMenuTreeByUnionId(unionId);
    }
    return getChildPerms(menus, 0);
  }

  /**
   * 根据角色ID查询菜单树信息
   *
   * @param roleId 角色ID
   * @return 选中菜单列表
   */
  @Override
  public List<Long> selectMenuListByRoleId(Long roleId) {
    SysRole role = roleMapper.selectByPrimaryKey(roleId);
    return menuMapper.selectMenuListByRoleId(roleId, role.getMenuCheckStrictly());
  }

  /**
   * 构建前端路由所需要的菜单
   *
   * @param menus 菜单列表
   * @return 路由列表
   */
  @Override
  public List<RouterVo> buildMenus(List<SysMenu> menus) {
    List<RouterVo> routers = new LinkedList<RouterVo>();
    for (SysMenu menu : menus) {
      RouterVo router = new RouterVo();
      router.setHidden("1".equals(menu.getVisible()));
      router.setName(getRouteName(menu));
      router.setPath(getRouterPath(menu));
      router.setComponent(getComponent(menu));
      router.setMeta(
          new MetaVo(
              menu.getMenuName(),
              menu.getIcon(),
              StrUtil.equals("1", menu.getIsCache()),
              menu.getPath()));
      List<SysMenu> cMenus = menu.getChildren();
      if (!cMenus.isEmpty() && IoTUserConstants.TYPE_DIR.equals(menu.getMenuType())) {
        router.setAlwaysShow(true);
        router.setRedirect("noRedirect");
        router.setChildren(buildMenus(cMenus));
      } else if (isMenuFrame(menu)) {
        router.setMeta(null);
        List<RouterVo> childrenList = new ArrayList<RouterVo>();
        RouterVo children = new RouterVo();
        children.setPath(menu.getPath());
        children.setComponent(menu.getComponent());
        children.setName(StrUtil.upperFirst(menu.getPath()));
        children.setMeta(
            new MetaVo(
                menu.getMenuName(),
                menu.getIcon(),
                StrUtil.equals("1", menu.getIsCache()),
                menu.getPath()));
        childrenList.add(children);
        router.setChildren(childrenList);
      } else if (menu.getParentId().intValue() == 0 && isInnerLink(menu)) {
        router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon()));
        router.setPath("/inner");
        List<RouterVo> childrenList = new ArrayList<RouterVo>();
        RouterVo children = new RouterVo();
        String routerPath = StringUtils.replace(menu.getPath(), Constants.HTTP, "");
        routerPath = StringUtils.replace(routerPath, Constants.HTTPS, "");
        children.setPath(routerPath);
        children.setComponent(IoTUserConstants.INNER_LINK);
        children.setName(StrUtil.upperFirst(routerPath));
        children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), menu.getPath()));
        childrenList.add(children);
        router.setChildren(childrenList);
      }
      routers.add(router);
    }
    return routers;
  }

  /**
   * 构建前端所需要树结构
   *
   * @param menus 菜单列表
   * @return 树结构列表
   */
  @Override
  public List<SysMenu> buildMenuTree(List<SysMenu> menus) {
    List<SysMenu> returnList = new ArrayList<SysMenu>();
    List<Long> tempList = new ArrayList<Long>();
    for (SysMenu dept : menus) {
      tempList.add(dept.getMenuId());
    }
    for (SysMenu menu : menus) {
      // 如果是顶级节点, 遍历该父节点的所有子节点
      if (!tempList.contains(menu.getParentId())) {
        recursionFn(menus, menu);
        returnList.add(menu);
      }
    }
    if (returnList.isEmpty()) {
      returnList = menus;
    }
    return returnList;
  }

  /**
   * 构建前端所需要下拉树结构
   *
   * @param menus 菜单列表
   * @return 下拉树结构列表
   */
  @Override
  public List<TreeSelect> buildMenuTreeSelect(List<SysMenu> menus) {
    List<SysMenu> menuTrees = buildMenuTree(menus);
    return menuTrees.stream().map(TreeSelect::new).collect(Collectors.toList());
  }

  /**
   * 根据菜单ID查询信息
   *
   * @param menuId 菜单ID
   * @return 菜单信息
   */
  @Override
  public SysMenu selectMenuById(Long menuId) {
    return menuMapper.selectByPrimaryKey(menuId);
  }

  /**
   * 是否存在菜单子节点
   *
   * @param menuId 菜单ID
   * @return 结果
   */
  @Override
  public boolean hasChildByMenuId(Long menuId) {
    int result = menuMapper.selectCount(SysMenu.builder().parentId(menuId).build());
    return result > 0;
  }

  /**
   * 查询菜单使用数量
   *
   * @param menuId 菜单ID
   * @return 结果
   */
  @Override
  public boolean checkMenuExistRole(Long menuId) {
    int result = roleMenuMapper.selectCount(SysRoleMenu.builder().menuId(menuId).build());
    return result > 0;
  }

  /**
   * 新增保存菜单信息
   *
   * @param menu 菜单信息
   * @return 结果
   */
  @Override
  public int insertMenu(SysMenu menu) {
    return menuMapper.insert(menu);
  }

  /**
   * 修改保存菜单信息
   *
   * @param menu 菜单信息
   * @return 结果
   */
  @Override
  public int updateMenu(SysMenu menu) {
    return menuMapper.updateByPrimaryKey(menu);
  }

  /**
   * 删除菜单管理信息
   *
   * @param menuId 菜单ID
   * @return 结果
   */
  @Override
  public int deleteMenuById(Long menuId) {
    return menuMapper.deleteByPrimaryKey(menuId);
  }

  /**
   * 校验菜单名称是否唯一
   *
   * @param menu 菜单信息
   * @return 结果
   */
  @Override
  public String checkMenuNameUnique(SysMenu menu) {
    Long menuId = Validator.isNull(menu.getMenuId()) ? -1L : menu.getMenuId();
    List<SysMenu> info =
        menuMapper.selectMenuList(
            SysMenu.builder().menuName(menu.getMenuName()).parentId(menu.getParentId()).build());
    if (!CollectionUtils.isEmpty(info)
        && info.get(0).getMenuId().longValue() != menuId.longValue()) {
      return IoTUserConstants.NOT_UNIQUE;
    }
    return IoTUserConstants.UNIQUE;
  }

  /**
   * 获取路由名称
   *
   * @param menu 菜单信息
   * @return 路由名称
   */
  public String getRouteName(SysMenu menu) {
    String routerName = StrUtil.upperFirst(menu.getPath());
    // 非外链并且是一级目录（类型为目录）
    if (isMenuFrame(menu)) {
      routerName = StrUtil.EMPTY;
    }
    return routerName;
  }

  /**
   * 获取路由地址
   *
   * @param menu 菜单信息
   * @return 路由地址
   */
  public String getRouterPath(SysMenu menu) {
    String routerPath = menu.getPath();
    // 内链打开外网方式
    if (menu.getParentId().intValue() != 0 && isInnerLink(menu)) {
      routerPath = StringUtils.replace(routerPath, Constants.HTTP, "");
      routerPath = StringUtils.replace(routerPath, Constants.HTTPS, "");
    }
    // 非外链并且是一级目录（类型为目录）
    if (0 == menu.getParentId().intValue()
        && IoTUserConstants.TYPE_DIR.equals(menu.getMenuType())
        && IoTUserConstants.NO_FRAME.equals(menu.getIsFrame())) {
      routerPath = "/" + menu.getPath();
    }
    // 非外链并且是一级目录（类型为菜单）
    else if (isMenuFrame(menu)) {
      routerPath = "/";
    }
    return routerPath;
  }

  /**
   * 获取组件信息
   *
   * @param menu 菜单信息
   * @return 组件信息
   */
  public String getComponent(SysMenu menu) {
    String component = IoTUserConstants.LAYOUT;
    if (StrUtil.isNotEmpty(menu.getComponent()) && !isMenuFrame(menu)) {
      component = menu.getComponent();
    } else if (StrUtil.isEmpty(menu.getComponent())
        && menu.getParentId().intValue() != 0
        && isInnerLink(menu)) {
      component = IoTUserConstants.INNER_LINK;
    } else if (StrUtil.isEmpty(menu.getComponent()) && isParentView(menu)) {
      component = IoTUserConstants.PARENT_VIEW;
    }
    return component;
  }

  /**
   * 是否为菜单内部跳转
   *
   * @param menu 菜单信息
   * @return 结果
   */
  public boolean isMenuFrame(SysMenu menu) {
    return menu.getParentId().intValue() == 0
        && IoTUserConstants.TYPE_MENU.equals(menu.getMenuType())
        && menu.getIsFrame().equals(IoTUserConstants.NO_FRAME);
  }

  /**
   * 是否为内链组件
   *
   * @param menu 菜单信息
   * @return 结果
   */
  public boolean isInnerLink(SysMenu menu) {
    return menu.getIsFrame().equals(IoTUserConstants.NO_FRAME) && Validator.isUrl(menu.getPath());
  }

  /**
   * 是否为parent_view组件
   *
   * @param menu 菜单信息
   * @return 结果
   */
  public boolean isParentView(SysMenu menu) {
    return menu.getParentId().intValue() != 0
        && IoTUserConstants.TYPE_DIR.equals(menu.getMenuType());
  }

  /**
   * 根据父节点的ID获取所有子节点
   *
   * @param list 分类表
   * @param parentId 传入的父节点ID
   * @return String
   */
  public List<SysMenu> getChildPerms(List<SysMenu> list, int parentId) {
    List<SysMenu> returnList = new ArrayList<SysMenu>();
    for (SysMenu t : list) {
      // 一、根据传入的某个父节点ID,遍历该父节点的所有子节点
      if (t.getParentId() == parentId) {
        recursionFn(list, t);
        returnList.add(t);
      }
    }
    return returnList;
  }

  /** 递归列表 */
  private void recursionFn(List<SysMenu> list, SysMenu t) {
    // 得到子节点列表
    List<SysMenu> childList = getChildList(list, t);
    t.setChildren(childList);
    for (SysMenu tChild : childList) {
      if (hasChild(list, tChild)) {
        recursionFn(list, tChild);
      }
    }
  }

  /** 得到子节点列表 */
  private List<SysMenu> getChildList(List<SysMenu> list, SysMenu t) {
    List<SysMenu> tlist = new ArrayList<SysMenu>();
    for (SysMenu n : list) {
      if (n.getParentId().longValue() == t.getMenuId().longValue()) {
        tlist.add(n);
      }
    }
    return tlist;
  }

  /** 判断是否有子节点 */
  private boolean hasChild(List<SysMenu> list, SysMenu t) {
    return getChildList(list, t).size() > 0;
  }

}
