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

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.IdUtil;
import cn.universal.admin.system.service.ISysDeptService;
import cn.universal.admin.system.service.ISysRoleService;
import cn.universal.common.annotation.DataScope;
import cn.universal.common.constant.IoTUserConstants;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.common.inteceptor.SQenGenId;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTTenantPackageMenu;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.admin.SysRole;
import cn.universal.persistence.entity.admin.SysRoleDept;
import cn.universal.persistence.entity.admin.SysRoleMenu;
import cn.universal.persistence.entity.admin.SysUserRole;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.IoTTenantPackageMenuMapper;
import cn.universal.persistence.mapper.admin.SysRoleDeptMapper;
import cn.universal.persistence.mapper.admin.SysRoleMapper;
import cn.universal.persistence.mapper.admin.SysRoleMenuMapper;
import cn.universal.persistence.mapper.admin.SysUserRoleMapper;
import cn.universal.security.support.EvictMenuPermissions;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

/** 角色 业务层处理 @Author ruoyi */
@Service
public class SysRoleServiceImpl implements ISysRoleService {

  private static final String ROLE_TYPE_PLATFORM_ADMIN = "PLATFORM_ADMIN";
  private static final String ROLE_TYPE_TENANT_ADMIN = "TENANT_ADMIN";
  private static final String ROLE_TYPE_TENANT_CUSTOM = "TENANT_CUSTOM";
  private static final String TENANT_ADMIN_ROLE_KEY = "tenant:admin";

  @Resource private SysRoleDeptMapper roleDeptMapper;
  @Resource private SysRoleMenuMapper roleMenuMapper;

  @Resource private SysUserRoleMapper userRoleMapper;
  @Resource private SysRoleMapper roleMapper;
  @Resource private ISysDeptService deptService;
  @Resource private IoTUserService ioTUserService;
  @Resource private IoTTenantMapper tenantMapper;
  @Resource private IoTTenantPackageMenuMapper tenantPackageMenuMapper;

  @Override
  //  @DataScope(alias = "ur", created = "r.create_by")
  public List<SysRole> selectPageRoleList(SysRole role) {
    return roleMapper.selectRoleList(role);
  }

  /**
   * 根据条件分页查询角色数据
   *
   * @param role 角色信息
   * @return 角色数据集合信息
   */
  @Override
  @DataScope(alias = "ur", created = "r.create_by")
  public List<SysRole> selectRoleList(SysRole role) {
    return roleMapper.selectRoleList(role);
  }

  /**
   * 根据用户ID查询角色
   *
   * @param unionId 用户unionId
   * @return 角色列表
   */
  @Override
  public List<SysRole> selectRolesByUnionId(String unionId) {
    List<SysRole> userRoles = roleMapper.selectRolePermissionByUnionId(unionId);
    List<SysRole> roles = roleMapper.selectRolePermissionByUnionId(SecurityUtils.getUnionId());
    if (CollectionUtils.isEmpty(roles) || roles.get(0).getRoleId() == 1) {
      roles = roleMapper.selectAll();
    }
    for (SysRole role : roles) {
      for (SysRole userRole : userRoles) {
        if (role.getRoleId().longValue() == userRole.getRoleId().longValue()) {
          role.setFlag(true);
        }
      }
    }
    return roles;
  }

  /**
   * 根据用户ID查询权限
   *
   * @param unionId 用户unionId
   * @return 权限列表
   */
  @Override
  public Set<String> selectRolePermissionByUnionId(String unionId) {
    List<SysRole> perms = roleMapper.selectRolePermissionByUnionId(unionId);
    Set<String> permsSet = new HashSet<>();
    for (SysRole perm : perms) {
      if (Validator.isNotNull(perm)) {
        permsSet.addAll(Arrays.asList(perm.getRoleKey().trim().split(",")));
      }
    }
    return permsSet;
  }

  /**
   * 查询所有角色
   *
   * @return 角色列表
   */
  @Override
  public List<SysRole> selectRoleAll() {
    return roleMapper.selectRoleList(new SysRole());
  }

  /**
   * 根据用户ID获取角色选择框列表
   *
   * @param unionId 用户unionId
   * @return 选中角色ID列表
   */
  @Override
  public List<Long> selectRoleListByUnionId(String unionId) {
    return roleMapper.selectRoleListByUnionId(unionId);
  }

  /**
   * 通过角色ID查询角色
   *
   * @param roleId 角色ID
   * @return 角色对象信息
   */
  @Override
  public SysRole selectRoleById(Long roleId) {
    return roleMapper.selectByPrimaryKey(roleId);
  }

  /**
   * 校验角色名称是否唯一
   *
   * @param role 角色信息
   * @return 结果
   */
  @Override
  public String checkRoleNameUnique(SysRole role) {
    Long roleId = Validator.isNull(role.getRoleId()) ? -1L : role.getRoleId();
    String createBy = StrUtil.blankToDefault(role.getCreateBy(), SecurityUtils.getUnionIdQuietly());
    String tenantId = resolveRoleTenantId(role, createBy);
    Example ex = new Example(SysRole.class);
    Example.Criteria criteria = ex.createCriteria().andEqualTo("roleName", role.getRoleName());
    if (StrUtil.isNotBlank(tenantId)) {
      criteria.andEqualTo("tenantId", tenantId);
    } else if (StrUtil.isNotBlank(createBy)) {
      criteria.andEqualTo("createBy", createBy);
    }
    List<SysRole> roles = roleMapper.selectByExample(ex);
    //    List<SysRole> roles = roleMapper
    //        .selectRoleList(SysRole.builder().roleName(role.getRoleName()).build());
    if (!CollectionUtils.isEmpty(roles)
        && roles.get(0).getRoleId().longValue() != roleId.longValue()) {
      return IoTUserConstants.NOT_UNIQUE;
    }
    return IoTUserConstants.UNIQUE;
  }

  /**
   * 校验角色权限是否唯一
   *
   * @param role 角色信息
   * @return 结果
   */
  @Override
  public String checkRoleKeyUnique(SysRole role) {
    Long roleId = Validator.isNull(role.getRoleId()) ? -1L : role.getRoleId();
    String createBy = StrUtil.blankToDefault(role.getCreateBy(), SecurityUtils.getUnionIdQuietly());
    String tenantId = resolveRoleTenantId(role, createBy);
    Example ex = new Example(SysRole.class);
    Example.Criteria criteria = ex.createCriteria().andEqualTo("roleKey", role.getRoleKey());
    if (StrUtil.isNotBlank(tenantId)) {
      criteria.andEqualTo("tenantId", tenantId);
    } else if (StrUtil.isNotBlank(createBy)) {
      criteria.andEqualTo("createBy", createBy);
    }
    List<SysRole> roles = roleMapper.selectByExample(ex);
    if (!CollectionUtils.isEmpty(roles)
        && roles.get(0).getRoleId().longValue() != roleId.longValue()) {
      return IoTUserConstants.NOT_UNIQUE;
    }
    return IoTUserConstants.UNIQUE;
  }

  /**
   * 校验角色是否允许操作
   *
   * @param role 角色信息
   */
  @Override
  public void checkRoleAllowed(SysRole role) {
    if (Validator.isNotNull(role.getRoleId()) && role.isAdmin()) {
      throw new IoTException("不允许操作超级管理员角色");
    }
  }

  /**
   * 通过角色ID查询角色使用数量
   *
   * @param roleId 角色ID
   * @return 结果
   */
  @Override
  public int countUserRoleByRoleId(Long roleId) {
    // 当前用户！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
    //    Long userId = SecurityUtils.getLoginUser().getUser().getUserId();
    Example ex = new Example(SysUserRole.class);
    ex.createCriteria().andEqualTo("roleId", roleId);
    return userRoleMapper.selectCountByExample(ex);
    //            .andNotEqualTo("userId", userId));
  }

  /**
   * 新增保存角色信息
   *
   * @param role 角色信息
   * @return 结果
   */
  @Override
  @Transactional
  @EvictMenuPermissions
  public int insertRole(SysRole role) {
    validateRoleMenuScope(role);
    // 新增角色信息
    role.setDataScope("4");
    normalizeRoleTenantScope(role, null);
    role.setCreateTime(new Date());
    roleMapper.insertUseGeneratedKeys(role);
    return insertRoleMenu(role);
  }

  /**
   * 修改保存角色信息
   *
   * @param role 角色信息
   * @return 结果
   */
  @Override
  @Transactional
  @EvictMenuPermissions
  public int updateRole(SysRole role) {
    validateRoleMenuScope(role);
    // 修改角色信息
    SysRole persistedRole = role.getRoleId() == null ? null : roleMapper.selectByPrimaryKey(role.getRoleId());
    normalizeRoleTenantScope(role, persistedRole);
    role.setUpdateTime(new Date());
    roleMapper.updateByPrimaryKeySelective(role);
    // 删除角色与菜单关联
    Example ex = new Example(SysRoleMenu.class);
    ex.createCriteria().andEqualTo("roleId", role.getRoleId());
    roleMenuMapper.deleteByExample(ex);
    return insertRoleMenu(role);
  }

  /**
   * 修改角色状态
   *
   * @param role 角色信息
   * @return 结果
   */
  @Override
  @EvictMenuPermissions
  public int updateRoleStatus(SysRole role) {
    return roleMapper.updateByPrimaryKeySelective(role);
  }

  /**
   * 修改数据权限信息
   *
   * @param role 角色信息
   * @return 结果
   */
  @Override
  @Transactional
  @EvictMenuPermissions
  public int authDataScope(SysRole role) {
    validateRoleDeptScope(role);
    role.setUpdateTime(new Date());
    roleMapper.updateByPrimaryKeySelective(role);
    Example ex = new Example(SysRoleDept.class);
    ex.createCriteria().andEqualTo("roleId", role.getRoleId());
    roleDeptMapper.deleteByExample(ex);
    return insertRoleDept(role);
  }

  /**
   * 新增角色菜单信息
   *
   * @param role 角色对象
   */
  @EvictMenuPermissions
  public int insertRoleMenu(SysRole role) {
    List<SysRoleMenu> roleMenus = buildRoleMenus(role == null ? null : role.getRoleId(), role == null ? null : role.getMenuIds());
    if (CollectionUtils.isEmpty(roleMenus)) {
      return 1;
    }
    int rows = roleMenuMapper.batchInsert(roleMenus);
    return rows == 0 ? 1 : rows;
  }

  /**
   * 新增角色部门信息(数据权限)
   *
   * @param role 角色对象
   */
  public int insertRoleDept(SysRole role) {
    List<SysRoleDept> roleDepts =
        buildRoleDepts(role == null ? null : role.getRoleId(), role == null ? null : role.getDeptIds());
    if (CollectionUtils.isEmpty(roleDepts)) {
      return 1;
    }
    int rows = roleDeptMapper.batchInsert(roleDepts);
    return rows == 0 ? 1 : rows;
  }

  private void validateRoleDeptScope(SysRole role) {
    if (role == null || !StrUtil.equals(role.getDataScope(), "2")) {
      return;
    }
    if (role.getDeptIds() == null || role.getDeptIds().length == 0) {
      return;
    }
    SysRole persistedRole = role.getRoleId() == null ? null : roleMapper.selectByPrimaryKey(role.getRoleId());
    String ownerUnionId =
        StrUtil.blankToDefault(role.getCreateBy(), persistedRole == null ? null : persistedRole.getCreateBy());
    String tenantId =
        StrUtil.blankToDefault(
            role.getTenantId(),
            persistedRole == null ? resolveTenantId(ownerUnionId) : persistedRole.getTenantId());
    if (StrUtil.isBlank(tenantId)) {
      return;
    }
    for (Long deptId : role.getDeptIds()) {
      if (deptId == null) {
        continue;
      }
      if (!deptService.checkDeptBelongsToTenant(deptId, tenantId)) {
        throw new IoTException("所选数据权限部门不属于当前租户");
      }
    }
  }

  private void validateRoleMenuScope(SysRole role) {
    if (role == null || role.getMenuIds() == null || role.getMenuIds().length == 0) {
      return;
    }
    IoTUser currentUser = ioTUserService.selectUserByUnionId(SecurityUtils.getUnionIdQuietly());
    if (currentUser == null || currentUser.isAdmin()) {
      return;
    }
    Set<Long> packageMenuIds = resolveTenantPackageMenuIds(currentUser);
    if (packageMenuIds == null) {
      return;
    }
    for (Long menuId : role.getMenuIds()) {
      if (menuId != null && !packageMenuIds.contains(menuId)) {
        throw new IoTException("所选菜单不在当前租户套餐授权范围内");
      }
    }
  }

  private String resolveTenantId(String unionId) {
    if (StrUtil.isBlank(unionId)) {
      return null;
    }
    IoTUser user = ioTUserService.selectUserByUnionId(unionId);
    if (user == null) {
      return null;
    }
    return StrUtil.blankToDefault(user.getTenantId(), user.getUnionId());
  }

  private String resolveRoleTenantId(SysRole role, String ownerUnionId) {
    if (role != null && StrUtil.isNotBlank(role.getTenantId())) {
      return role.getTenantId();
    }
    return resolveTenantId(ownerUnionId);
  }

  private void normalizeRoleTenantScope(SysRole role, SysRole persistedRole) {
    if (role == null) {
      return;
    }
    if (persistedRole != null && StrUtil.isNotBlank(persistedRole.getTenantId())) {
      role.setTenantId(persistedRole.getTenantId());
    } else if (StrUtil.isBlank(role.getTenantId())) {
      role.setTenantId(resolveTenantId(role.getCreateBy()));
    }
    if (StrUtil.isBlank(role.getRoleType())) {
      if (StrUtil.equals(role.getRoleKey(), TENANT_ADMIN_ROLE_KEY)) {
        role.setRoleType(ROLE_TYPE_TENANT_ADMIN);
      } else if (StrUtil.isBlank(role.getTenantId())) {
        role.setRoleType(ROLE_TYPE_PLATFORM_ADMIN);
      } else {
        role.setRoleType(ROLE_TYPE_TENANT_CUSTOM);
      }
    }
  }

  private Set<Long> resolveTenantPackageMenuIds(IoTUser user) {
    if (user == null) {
      return null;
    }
    String tenantId = StrUtil.blankToDefault(user.getTenantId(), user.getUnionId());
    if (StrUtil.isBlank(tenantId)) {
      return null;
    }
    IoTTenant tenant = tenantMapper.selectByPrimaryKey(tenantId);
    if (tenant == null || tenant.getPackageId() == null) {
      return null;
    }
    Example example = new Example(IoTTenantPackageMenu.class);
    example.createCriteria().andEqualTo("packageId", tenant.getPackageId());
    List<IoTTenantPackageMenu> relations = tenantPackageMenuMapper.selectByExample(example);
    return relations.stream().map(IoTTenantPackageMenu::getMenuId).collect(Collectors.toSet());
  }

  /**
   * 通过角色ID删除角色
   *
   * @param roleId 角色ID
   * @return 结果
   */
  @Override
  @Transactional
  @EvictMenuPermissions
  public int deleteRoleById(Long roleId) {
    Example roleMenuExample = new Example(SysRoleMenu.class);
    roleMenuExample.createCriteria().andEqualTo("roleId", roleId);
    roleMenuMapper.deleteByExample(roleMenuExample);
    Example roleDeptExample = new Example(SysRoleDept.class);
    roleDeptExample.createCriteria().andEqualTo("roleId", roleId);
    roleDeptMapper.deleteByExample(roleDeptExample);
    return roleMapper.deleteByPrimaryKey(roleId);
  }

  /**
   * 批量删除角色信息
   *
   * @param roleIds 需要删除的角色ID
   * @return 结果
   */
  @Override
  @Transactional
  @EvictMenuPermissions
  public int deleteRoleByIds(Long[] roleIds) {
    for (Long roleId : roleIds) {
      checkRoleAllowed(new SysRole(roleId));
      SysRole role = selectRoleById(roleId);
      if (countUserRoleByRoleId(roleId) > 0) {
        throw new IoTException(String.format("%1$s已分配,不能删除", role.getRoleName()));
      }
    }
    List<Long> ids = Arrays.asList(roleIds);
    String ids1 = ids.stream().map(Object::toString).collect(Collectors.joining(","));
    // 删除角色与菜单关联
    Example ex = new Example(SysRoleMenu.class);
    ex.createCriteria().andIn("roleId", ids);
    roleMenuMapper.deleteByExample(ex);
    // 删除角色与部门关联
    Example roleDeptExample = new Example(SysRoleDept.class);
    roleDeptExample.createCriteria().andIn("roleId", ids);
    roleDeptMapper.deleteByExample(roleDeptExample);
    return roleMapper.deleteByIds(ids1);
  }

  /**
   * 取消授权用户角色
   *
   * @param userRole 用户和角色关联信息
   * @return 结果
   */
  @Override
  @EvictMenuPermissions
  public int deleteAuthUser(SysUserRole userRole) {
    return userRoleMapper.delete(
        SysUserRole.builder().unionId(userRole.getUnionId()).roleId(userRole.getRoleId()).build());
  }

  /**
   * 批量取消授权用户角色
   *
   * @param roleId 角色ID
   * @param unionIds 需要取消授权的用户数据ID
   * @return 结果
   */
  @Override
  @EvictMenuPermissions
  public int deleteAuthUsers(Long roleId, String[] unionIds) {
    Example ex = new Example(SysUserRole.class);
    ex.createCriteria().andEqualTo("roleId", roleId).andIn("unionId", Arrays.asList(unionIds));
    return userRoleMapper.deleteByExample(ex);
  }

  /**
   * 批量选择授权用户角色
   *
   * @param roleId 角色ID
   * @param unionIds 需要删除的用户数据ID
   * @return 结果
   */
  @Override
  @EvictMenuPermissions
  public int insertAuthUsers(Long roleId, String[] unionIds) {
    List<SysUserRole> userRoles = buildUserRoles(roleId, unionIds);
    if (CollectionUtils.isEmpty(userRoles)) {
      return 1;
    }
    int rows = userRoleMapper.batchInsert(userRoles);
    return rows == 0 ? 1 : rows;
  }

  private List<SysRoleMenu> buildRoleMenus(Long roleId, Long[] menuIds) {
    if (roleId == null || menuIds == null || menuIds.length == 0) {
      return new ArrayList<>();
    }
    Set<Long> uniqueMenuIds = new LinkedHashSet<>();
    for (Long menuId : menuIds) {
      if (menuId != null) {
        uniqueMenuIds.add(menuId);
      }
    }
    if (uniqueMenuIds.isEmpty()) {
      return new ArrayList<>();
    }
    List<SysRoleMenu> roleMenus = new ArrayList<>(uniqueMenuIds.size());
    for (Long menuId : uniqueMenuIds) {
      roleMenus.add(
          SysRoleMenu.builder()
              .uuid(SQenGenId.nextId())
              .roleId(roleId)
              .menuId(menuId)
              .build());
    }
    return roleMenus;
  }

  private List<SysRoleDept> buildRoleDepts(Long roleId, Long[] deptIds) {
    if (roleId == null || deptIds == null || deptIds.length == 0) {
      return new ArrayList<>();
    }
    Set<Long> uniqueDeptIds = new LinkedHashSet<>();
    for (Long deptId : deptIds) {
      if (deptId != null) {
        uniqueDeptIds.add(deptId);
      }
    }
    if (uniqueDeptIds.isEmpty()) {
      return new ArrayList<>();
    }
    List<SysRoleDept> roleDepts = new ArrayList<>(uniqueDeptIds.size());
    for (Long deptId : uniqueDeptIds) {
      roleDepts.add(
          SysRoleDept.builder()
              .uuid(SQenGenId.nextId())
              .roleId(roleId)
              .deptId(deptId)
              .build());
    }
    return roleDepts;
  }

  private List<SysUserRole> buildUserRoles(Long roleId, String[] unionIds) {
    if (roleId == null || unionIds == null || unionIds.length == 0) {
      return new ArrayList<>();
    }
    Set<String> uniqueUnionIds = new LinkedHashSet<>();
    for (String unionId : unionIds) {
      if (StrUtil.isNotBlank(unionId)) {
        uniqueUnionIds.add(unionId);
      }
    }
    if (uniqueUnionIds.isEmpty()) {
      return new ArrayList<>();
    }
    List<SysUserRole> userRoles = new ArrayList<>(uniqueUnionIds.size());
    for (String unionId : uniqueUnionIds) {
      userRoles.add(
          SysUserRole.builder()
              .uuid(SQenGenId.nextId())
              .unionId(unionId)
              .roleId(roleId)
              .build());
    }
    return userRoles;
  }

  @Override
  public String selectUserRoleGroup(String unionId) {
    List<SysRole> list = roleMapper.selectRolePermissionByUnionId(unionId);
    if (CollectionUtils.isEmpty(list)) {
      return "";
    }
    return list.stream().map(SysRole::getRoleName).collect(Collectors.joining(","));
  }
}
