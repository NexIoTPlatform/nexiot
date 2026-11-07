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
import cn.universal.admin.common.service.BaseServiceImpl;
import cn.universal.admin.system.service.ISysDeptService;
import cn.universal.common.constant.IoTUserConstants;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.admin.SysDept;
import cn.universal.persistence.entity.admin.SysRoleDept;
import cn.universal.persistence.entity.admin.TreeSelect;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.admin.SysDeptMapper;
import cn.universal.persistence.mapper.admin.SysRoleDeptMapper;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.support.DeptDataPermissionSupport.DeptDataPermissionScope;
import cn.universal.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

/** 部门业务层实现 */
@Service
public class SysDeptServiceImpl extends BaseServiceImpl implements ISysDeptService {

  private static final String ROOT_ANCESTORS = "0";
  private static final String ROOT_DEPT_NAME = "租户根部门";
  private static final String LEGACY_ROOT_DEPT_NAME = "默认部门";

  @Resource private SysDeptMapper deptMapper;
  @Resource private SysRoleDeptMapper roleDeptMapper;
  @Resource private IoTUserService ioTUserService;
  @Resource private IoTTenantMapper tenantMapper;

  @Override
  public List<SysDept> selectDeptList(SysDept dept) {
    SysDept queryDept = dept == null ? new SysDept() : dept;
    String tenantId = resolveQueryTenantId(queryDept);
    if (StrUtil.isNotBlank(tenantId)) {
      ensureTenantRootDept(tenantId, resolveTenantOwnerUnionId(tenantId), null);
      queryDept.setTenantId(tenantId);
    }
    Example example = new Example(SysDept.class);
    Example.Criteria criteria = example.createCriteria();
    if (StrUtil.isNotBlank(queryDept.getTenantId())) {
      criteria.andEqualTo("tenantId", queryDept.getTenantId());
    }
    if (StrUtil.isNotBlank(queryDept.getDeptName())) {
      criteria.andLike("deptName", "%" + queryDept.getDeptName() + "%");
    }
    if (StrUtil.isNotBlank(queryDept.getStatus())) {
      criteria.andEqualTo("status", queryDept.getStatus());
    }
    if (queryDept.getParentId() != null) {
      criteria.andEqualTo("parentId", queryDept.getParentId());
    }
    example.setOrderByClause("tenant_id asc, parent_id asc, order_num asc");
    return filterAssignableDeptList(deptMapper.selectByExample(example));
  }

  @Override
  public List<SysDept> selectDeptListExcludeChild(Long deptId) {
    SysDept currentDept = selectDeptById(deptId);
    if (currentDept == null) {
      return new ArrayList<>();
    }
    List<SysDept> deptList = selectDeptList(SysDept.builder().tenantId(currentDept.getTenantId()).build());
    return deptList.stream()
        .filter(dept -> !deptId.equals(dept.getDeptId()))
        .filter(dept -> !isChildDept(dept, deptId))
        .collect(Collectors.toList());
  }

  @Override
  public List<SysDept> buildDeptTree(List<SysDept> depts) {
    List<SysDept> returnList = new ArrayList<>();
    List<Long> tempList = depts.stream().map(SysDept::getDeptId).collect(Collectors.toList());
    for (SysDept dept : depts) {
      if (!tempList.contains(dept.getParentId())) {
        recursionFn(depts, dept);
        returnList.add(dept);
      }
    }
    return returnList.isEmpty() ? depts : returnList;
  }

  @Override
  public List<TreeSelect> buildDeptTreeSelect(List<SysDept> depts) {
    return buildDeptTree(depts).stream().map(TreeSelect::new).collect(Collectors.toList());
  }

  @Override
  public SysDept selectDeptById(Long deptId) {
    return deptMapper.selectByPrimaryKey(deptId);
  }

  @Override
  public List<Long> selectDeptListByRoleId(Long roleId) {
    return roleDeptMapper.select(SysRoleDept.builder().roleId(roleId).build()).stream()
        .map(SysRoleDept::getDeptId)
        .collect(Collectors.toList());
  }

  @Override
  public String checkDeptNameUnique(SysDept dept) {
    if (dept == null
        || StrUtil.isBlank(dept.getTenantId())
        || StrUtil.isBlank(dept.getDeptName())
        || dept.getParentId() == null) {
      return IoTUserConstants.UNIQUE;
    }
    Long deptId = dept.getDeptId() == null ? -1L : dept.getDeptId();
    Example example = new Example(SysDept.class);
    example
        .createCriteria()
        .andEqualTo("tenantId", dept.getTenantId())
        .andEqualTo("parentId", dept.getParentId())
        .andEqualTo("deptName", dept.getDeptName());
    List<SysDept> deptList = deptMapper.selectByExample(example);
    if (!CollectionUtils.isEmpty(deptList)
        && !deptId.equals(deptList.get(0).getDeptId())) {
      return IoTUserConstants.NOT_UNIQUE;
    }
    return IoTUserConstants.UNIQUE;
  }

  @Override
  public boolean hasChildByDeptId(Long deptId) {
    Example example = new Example(SysDept.class);
    example.createCriteria().andEqualTo("parentId", deptId);
    return deptMapper.selectCountByExample(example) > 0;
  }

  @Override
  public boolean checkDeptExistUser(Long deptId) {
    return !CollectionUtils.isEmpty(ioTUserService.selectUserList(IoTUser.builder().deptId(deptId).build()));
  }

  @Override
  @Transactional
  public int insertDept(SysDept dept) {
    normalizeDeptForInsert(dept);
    if (Long.valueOf(0L).equals(dept.getParentId()) && hasTenantRootDept(dept.getTenantId())) {
      throw new IoTException("当前租户已存在根部门，请在根部门下新增子部门");
    }
    if (IoTUserConstants.NOT_UNIQUE.equals(checkDeptNameUnique(dept))) {
      throw new IoTException("新增部门'" + dept.getDeptName() + "'失败，部门名称已存在");
    }
    dept.setCreateBy(SecurityUtils.getUnionId());
    dept.setCreateTime(new Date());
    return deptMapper.insert(dept);
  }

  @Override
  @Transactional
  public int updateDept(SysDept dept) {
    SysDept oldDept = selectDeptById(dept.getDeptId());
    if (oldDept == null) {
      throw new IoTException("部门不存在");
    }
    assertDeptAccess(oldDept);
    if (dept.getDeptId().equals(dept.getParentId())) {
      throw new IoTException("上级部门不能选择自己");
    }

    Long parentId = dept.getParentId() == null ? oldDept.getParentId() : dept.getParentId();
    boolean oldRootDept = Long.valueOf(0L).equals(oldDept.getParentId());
    if (oldRootDept && !Long.valueOf(0L).equals(parentId)) {
      throw new IoTException("租户根部门不允许调整上级");
    }
    if (!oldRootDept && Long.valueOf(0L).equals(parentId)) {
      throw new IoTException("不允许调整为租户根部门");
    }

    String tenantId = oldDept.getTenantId();
    dept.setTenantId(tenantId);
    dept.setParentId(parentId);
    dept.setAncestors(resolveAncestors(parentId, tenantId));
    if (IoTUserConstants.NOT_UNIQUE.equals(checkDeptNameUnique(dept))) {
      throw new IoTException("修改部门'" + dept.getDeptName() + "'失败，部门名称已存在");
    }

    dept.setUpdateBy(SecurityUtils.getUnionId());
    dept.setUpdateTime(new Date());
    int rows = deptMapper.updateByPrimaryKeySelective(dept);
    if (!StrUtil.equals(oldDept.getAncestors(), dept.getAncestors())) {
      updateDeptChildren(dept.getDeptId(), oldDept.getAncestors(), dept.getAncestors(), tenantId);
    }
    return rows;
  }

  @Override
  @Transactional
  public int deleteDeptById(Long deptId) {
    SysDept dept = selectDeptById(deptId);
    if (dept == null) {
      return 0;
    }
    assertDeptAccess(dept);
    if (Long.valueOf(0L).equals(dept.getParentId())) {
      throw new IoTException("租户根部门不允许删除");
    }
    if (hasChildByDeptId(deptId)) {
      throw new IoTException("存在子部门,不允许删除");
    }
    if (checkDeptExistUser(deptId)) {
      throw new IoTException("部门下存在用户,不允许删除");
    }
    Example roleDeptExample = new Example(SysRoleDept.class);
    roleDeptExample.createCriteria().andEqualTo("deptId", deptId);
    roleDeptMapper.deleteByExample(roleDeptExample);
    return deptMapper.deleteByPrimaryKey(deptId);
  }

  @Override
  @Transactional
  public SysDept ensureTenantRootDept(String tenantId, String ownerUnionId, String deptName) {
    if (StrUtil.isBlank(tenantId)) {
      throw new IoTException("租户标识不能为空");
    }
    String resolvedDeptName = resolveRootDeptName(tenantId, deptName);
    Example example = new Example(SysDept.class);
    example
        .createCriteria()
        .andEqualTo("tenantId", tenantId)
        .andEqualTo("parentId", 0L);
    example.setOrderByClause("create_time asc");
    List<SysDept> deptList = deptMapper.selectByExample(example);
    if (!CollectionUtils.isEmpty(deptList)) {
      SysDept rootDept = deptList.get(0);
      upgradeRootDeptNameIfNeeded(rootDept, resolvedDeptName, null);
      return selectDeptById(rootDept.getDeptId());
    }

    SysDept rootDept =
        SysDept.builder()
            .parentId(0L)
            .ancestors(ROOT_ANCESTORS)
            .deptName(resolvedDeptName)
            .orderNum(0)
            .status("0")
            .tenantId(tenantId)
            .createBy(ownerUnionId)
            .createTime(new Date())
            .remark("系统自动初始化的租户根部门")
            .build();
    deptMapper.insert(rootDept);
    return rootDept;
  }

  @Override
  public boolean checkDeptBelongsToTenant(Long deptId, String tenantId) {
    if (deptId == null || StrUtil.isBlank(tenantId)) {
      return false;
    }
    SysDept dept = selectDeptById(deptId);
    return dept != null && StrUtil.equals(dept.getTenantId(), tenantId);
  }

  private boolean hasTenantRootDept(String tenantId) {
    if (StrUtil.isBlank(tenantId)) {
      return false;
    }
    Example example = new Example(SysDept.class);
    example.createCriteria().andEqualTo("tenantId", tenantId).andEqualTo("parentId", 0L);
    return deptMapper.selectCountByExample(example) > 0;
  }

  private void recursionFn(List<SysDept> list, SysDept dept) {
    List<SysDept> childList = getChildList(list, dept);
    dept.setChildren(childList);
    for (SysDept child : childList) {
      if (hasChild(list, child)) {
        recursionFn(list, child);
      }
    }
  }

  private List<SysDept> getChildList(List<SysDept> list, SysDept dept) {
    List<SysDept> childList = new ArrayList<>();
    for (SysDept item : list) {
      if (item.getParentId() != null && item.getParentId().longValue() == dept.getDeptId().longValue()) {
        childList.add(item);
      }
    }
    return childList;
  }

  private boolean hasChild(List<SysDept> list, SysDept dept) {
    return !CollectionUtils.isEmpty(getChildList(list, dept));
  }

  private boolean isChildDept(SysDept dept, Long deptId) {
    if (StrUtil.isBlank(dept.getAncestors())) {
      return false;
    }
    Set<String> ancestorSet = new HashSet<>(StrUtil.split(dept.getAncestors(), ','));
    return ancestorSet.contains(String.valueOf(deptId));
  }

  private void normalizeDeptForInsert(SysDept dept) {
    if (dept == null) {
      throw new IoTException("部门参数不能为空");
    }
    if (dept.getParentId() == null) {
      dept.setParentId(0L);
    }
    String tenantId = resolveWriteTenantId(dept);
    dept.setTenantId(tenantId);
    dept.setAncestors(resolveAncestors(dept.getParentId(), tenantId));
    if (StrUtil.isBlank(dept.getStatus())) {
      dept.setStatus("0");
    }
  }

  private String resolveWriteTenantId(SysDept dept) {
    IoTUser currentUser = currentTenantUser();
    if (currentUser != null && !currentUser.isAdmin()) {
      return resolveTenantId(currentUser);
    }
    if (Long.valueOf(0L).equals(dept.getParentId())) {
      if (StrUtil.isBlank(dept.getTenantId())) {
        throw new IoTException("租户标识不能为空");
      }
      return dept.getTenantId();
    }
    SysDept parentDept = selectDeptById(dept.getParentId());
    if (parentDept == null) {
      throw new IoTException("上级部门不存在");
    }
    assertDeptAccess(parentDept);
    return parentDept.getTenantId();
  }

  private String resolveRootDeptName(String tenantId, String deptName) {
    if (StrUtil.isNotBlank(deptName)) {
      return StrUtil.trim(deptName);
    }
    if (StrUtil.isNotBlank(tenantId)) {
      IoTTenant tenant = tenantMapper.selectByPrimaryKey(tenantId);
      if (tenant != null && StrUtil.isNotBlank(tenant.getTenantName())) {
        return StrUtil.trim(tenant.getTenantName());
      }
    }
    return ROOT_DEPT_NAME;
  }

  private void upgradeRootDeptNameIfNeeded(
      SysDept rootDept, String expectedDeptName, String previousDeptName) {
    if (rootDept == null || !canSyncRootDeptName(rootDept.getDeptName(), expectedDeptName, previousDeptName)) {
      return;
    }
    rootDept.setDeptName(expectedDeptName);
    rootDept.setUpdateBy(SecurityUtils.getUnionIdQuietly());
    rootDept.setUpdateTime(new Date());
    deptMapper.updateByPrimaryKeySelective(rootDept);
  }

  private boolean canSyncRootDeptName(
      String currentDeptName, String expectedDeptName, String previousDeptName) {
    String currentName = StrUtil.trim(currentDeptName);
    String expectedName = StrUtil.trim(expectedDeptName);
    if (StrUtil.isBlank(expectedName) || StrUtil.equals(currentName, expectedName)) {
      return false;
    }
    return StrUtil.isBlank(currentName)
        || StrUtil.equalsAny(currentName, ROOT_DEPT_NAME, LEGACY_ROOT_DEPT_NAME)
        || (StrUtil.isNotBlank(previousDeptName)
            && StrUtil.equals(currentName, StrUtil.trim(previousDeptName)));
  }

  private String resolveAncestors(Long parentId, String tenantId) {
    if (Long.valueOf(0L).equals(parentId)) {
      return ROOT_ANCESTORS;
    }
    SysDept parentDept = selectDeptById(parentId);
    if (parentDept == null) {
      throw new IoTException("上级部门不存在");
    }
    if (!StrUtil.equals(parentDept.getTenantId(), tenantId)) {
      throw new IoTException("不允许跨租户选择上级部门");
    }
    return parentDept.getAncestors() + "," + parentDept.getDeptId();
  }

  private void updateDeptChildren(Long deptId, String oldAncestors, String newAncestors, String tenantId) {
    Example example = new Example(SysDept.class);
    example
        .createCriteria()
        .andEqualTo("tenantId", tenantId)
        .andLike("ancestors", oldAncestors + "," + deptId + "%");
    List<SysDept> children = deptMapper.selectByExample(example);
    String oldPrefix = oldAncestors + "," + deptId;
    String newPrefix = newAncestors + "," + deptId;
    for (SysDept child : children) {
      child.setAncestors(StrUtil.replace(child.getAncestors(), oldPrefix, newPrefix));
      child.setUpdateBy(SecurityUtils.getUnionIdQuietly());
      child.setUpdateTime(new Date());
      deptMapper.updateByPrimaryKeySelective(child);
    }
  }

  private void assertDeptAccess(SysDept dept) {
    if (dept == null) {
      throw new IoTException("部门不存在");
    }
    IoTUser currentUser = currentTenantUser();
    if (currentUser == null || currentUser.isAdmin()) {
      return;
    }
    if (!StrUtil.equals(resolveTenantId(currentUser), dept.getTenantId())) {
      throw new IoTException("你无权操作当前部门");
    }
  }

  private List<SysDept> filterAssignableDeptList(List<SysDept> deptList) {
    if (CollectionUtils.isEmpty(deptList)) {
      return deptList;
    }
    IoTUser currentUser = currentTenantUser();
    if (currentUser == null || currentUser.isAdmin() || isTenantAdmin(currentUser)) {
      return deptList;
    }
    DeptDataPermissionScope scope = resolveDataPermissionScope(currentUser);
    if (scope != null && scope.isAll()) {
      return deptList;
    }
    Set<Long> assignableDeptIds = new LinkedHashSet<>();
    if (scope != null) {
      assignableDeptIds.addAll(scope.getScopeDeptIds());
      if (currentUser.getDeptId() != null
          && scope.getScopeUnionIds().contains(currentUser.getUnionId())) {
        assignableDeptIds.add(currentUser.getDeptId());
      }
    } else if (currentUser.getDeptId() != null) {
      assignableDeptIds.add(currentUser.getDeptId());
    }
    if (CollectionUtils.isEmpty(assignableDeptIds)) {
      return new ArrayList<>();
    }
    return deptList.stream()
        .filter(dept -> dept.getDeptId() != null && assignableDeptIds.contains(dept.getDeptId()))
        .collect(Collectors.toList());
  }

  private boolean isTenantAdmin(IoTUser user) {
    return user != null
        && !user.isAdmin()
        && StrUtil.equalsIgnoreCase(user.getTenantRole(), "ADMIN");
  }

  private String resolveQueryTenantId(SysDept dept) {
    IoTUser currentUser = currentTenantUser();
    if (currentUser != null && !currentUser.isAdmin()) {
      return resolveTenantId(currentUser);
    }
    return dept == null ? null : dept.getTenantId();
  }

  protected String resolveTenantOwnerUnionId(String tenantId) {
    IoTUser currentUser = currentTenantUser();
    if (currentUser != null && StrUtil.equals(resolveTenantId(currentUser), tenantId)) {
      return currentUser.getUnionId();
    }
    List<IoTUser> adminUsers =
        ioTUserService.selectUserList(
            IoTUser.builder().tenantId(tenantId).tenantRole("ADMIN").build());
    if (!CollectionUtils.isEmpty(adminUsers)) {
      return adminUsers.get(0).getUnionId();
    }
    return StrUtil.blankToDefault(SecurityUtils.getUnionIdQuietly(), tenantId);
  }
}
