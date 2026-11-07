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

package cn.universal.admin.system.permission;

import cn.universal.admin.system.service.ISysMenuService;
import cn.universal.admin.system.service.ISysRoleService;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.security.support.MenuPermissionCache;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 用户权限处理服务 */
@Component
public class SysPermissionService {

  @Autowired private cn.universal.security.service.IoTUserService userService;

  @Autowired private ISysRoleService roleService;

  @Autowired private ISysMenuService menuService;

  @Autowired private MenuPermissionCache menuPermissionCache;

  /** 获取角色数据权限 */
  public Set<String> getRolePermission(IoTUser user) {
    Set<String> roles = new HashSet<String>();
    // 管理员拥有所有权限
    if (user.isAdmin()) {
      roles.add("admin");
    } else {
      roles.addAll(roleService.selectRolePermissionByUnionId(user.getUnionId()));
    }
    return roles;
  }

  /**
   * 获取菜单数据权限
   *
   * @param user 用户信息
   * @return 菜单权限信息
   */
  public Set<String> getMenuPermission(IoTUser user) {
    if (user == null || user.getUnionId() == null) return Set.of();
    return menuPermissionCache.get(user.getUnionId(), () -> {
      // 缓存失效后从数据库确认账号身份，避免其他节点的用户 L1 缓存保留旧管理员身份。
      IoTUser query = new IoTUser();
      query.setUnionId(user.getUnionId());
      query.setDeleted(0);
      IoTUser current = userService.queryOne(query);
      if (current == null || !Integer.valueOf(0).equals(current.getStatus())) return Set.of();
      return current.isAdmin() ? Set.of("*:*:*")
          : new HashSet<>(menuService.selectMenuPermsByUserId(current.getUnionId()));
    });
  }
  /** Check the authenticated account against the same menu grants used by the frontend. */
  public boolean hasPermission(String permission) {
    Set<String> permissions = getCurrentPermissions();
    return permissions.contains("*:*:*") || permissions.contains(permission);
  }

  /** 一次加载当前人员的菜单及租户套餐权限，供批量能力查询复用，复用共享权限缓存。 */
  public Set<String> getCurrentPermissions() {
    String unionId = cn.universal.security.utils.SecurityUtils.getUnionIdQuietly();
    if (unionId == null) return Set.of();
    IoTUser user = userService.selectUserByUnionId(unionId);
    return user == null ? Set.of() : getMenuPermission(user);
  }

}
