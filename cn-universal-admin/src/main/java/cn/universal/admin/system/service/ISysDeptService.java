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

import cn.universal.persistence.entity.admin.SysDept;
import cn.universal.persistence.entity.admin.TreeSelect;
import java.util.List;

/** 部门业务层 */
public interface ISysDeptService {

  List<SysDept> selectDeptList(SysDept dept);

  List<SysDept> selectDeptListExcludeChild(Long deptId);

  List<SysDept> buildDeptTree(List<SysDept> depts);

  List<TreeSelect> buildDeptTreeSelect(List<SysDept> depts);

  SysDept selectDeptById(Long deptId);

  List<Long> selectDeptListByRoleId(Long roleId);

  String checkDeptNameUnique(SysDept dept);

  boolean hasChildByDeptId(Long deptId);

  boolean checkDeptExistUser(Long deptId);

  int insertDept(SysDept dept);

  int updateDept(SysDept dept);

  int deleteDeptById(Long deptId);

  SysDept ensureTenantRootDept(String tenantId, String ownerUnionId, String deptName);

  boolean checkDeptBelongsToTenant(Long deptId, String tenantId);
}
