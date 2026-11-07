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

package cn.universal.admin.system.web;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.admin.system.service.ISysDeptService;
import cn.universal.admin.system.service.ISysRoleService;
import cn.universal.common.annotation.Log;
import cn.universal.common.constant.IoTUserConstants;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.admin.SysDept;
import cn.universal.persistence.entity.admin.SysRole;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 部门管理控制器 */
@RestController
@RequestMapping("/admin/system/dept")
@Tag(name = "系统部门管理", description = "系统部门信息管理接口")
public class SysDeptController extends BaseController {

  @Resource private ISysDeptService deptService;
  @Resource private ISysRoleService roleService;
  @Resource private IoTUserService ioTUserService;

  @Operation(summary = "查询部门列表", description = "根据条件查询部门列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取部门列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/list")
  public AjaxResult<List<SysDept>> list(SysDept dept) {
    return AjaxResult.success(deptService.selectDeptList(dept));
  }

  @Operation(summary = "查询部门列表（排除节点）", description = "排除指定部门及其子部门")
  @ApiResponse(responseCode = "200", description = "成功获取过滤后的部门列表")
  @Parameter(name = "deptId", description = "部门ID", required = true)
  @GetMapping("/list/exclude/{deptId}")
  public AjaxResult<List<SysDept>> excludeChild(@PathVariable("deptId") Long deptId) {
    return AjaxResult.success(deptService.selectDeptListExcludeChild(deptId));
  }

  @Operation(summary = "根据部门编号获取详细信息", description = "根据部门ID获取详情")
  @ApiResponse(responseCode = "200", description = "成功获取部门详情")
  @Parameter(name = "deptId", description = "部门ID", required = true)
  @GetMapping(value = "/{deptId}")
  public AjaxResult<SysDept> getInfo(@PathVariable Long deptId) {
    SysDept dept = deptService.selectDeptById(deptId);
    ensureDeptReadable(currentUser(), dept);
    return AjaxResult.success(dept);
  }

  @Operation(summary = "获取部门下拉树", description = "获取部门下拉树结构")
  @ApiResponse(responseCode = "200", description = "成功获取部门树")
  @GetMapping("/treeselect")
  public AjaxResult treeselect(SysDept dept) {
    List<SysDept> depts = deptService.selectDeptList(dept);
    return AjaxResult.success(deptService.buildDeptTreeSelect(depts));
  }

  @Operation(summary = "根据角色ID获取部门树", description = "获取角色对应部门树结构")
  @ApiResponse(responseCode = "200", description = "成功获取角色部门树")
  @Parameter(name = "roleId", description = "角色ID", required = true)
  @GetMapping("/roleDeptTreeselect/{roleId}")
  public AjaxResult roleDeptTreeselect(@PathVariable("roleId") Long roleId) {
    SysRole role = roleService.selectRoleById(roleId);
    ensureRolePermission(currentUser(), role);
    SysDept query = new SysDept();
    String tenantId = resolveRoleTenantId(role);
    if (StrUtil.isNotBlank(tenantId)) {
      query.setTenantId(tenantId);
    }
    Map<String, Object> ajax = new HashMap<>();
    ajax.put("checkedKeys", deptService.selectDeptListByRoleId(roleId));
    ajax.put("depts", deptService.buildDeptTreeSelect(deptService.selectDeptList(query)));
    return AjaxResult.success(ajax);
  }

  @Operation(summary = "新增部门", description = "新增部门信息")
  @ApiResponse(responseCode = "200", description = "成功新增部门")
  @PostMapping
  @Log(title = "新增部门", businessType = BusinessType.INSERT)
  public AjaxResult add(@Validated @RequestBody SysDept dept) {
    ensureDeptWritable(currentUser());
    if (IoTUserConstants.NOT_UNIQUE.equals(deptService.checkDeptNameUnique(prepareDeptForUniqueCheck(dept)))) {
      return error("新增部门'" + dept.getDeptName() + "'失败，部门名称已存在");
    }
    return toAjax(deptService.insertDept(dept));
  }

  @Operation(summary = "修改部门", description = "修改部门信息")
  @ApiResponse(responseCode = "200", description = "成功修改部门")
  @PutMapping
  @Log(title = "修改部门", businessType = BusinessType.UPDATE)
  public AjaxResult edit(@Validated @RequestBody SysDept dept) {
    ensureDeptWritable(currentUser());
    if (ObjectUtil.equal(dept.getDeptId(), dept.getParentId())) {
      return error("修改部门'" + dept.getDeptName() + "'失败，上级部门不能是自己");
    }
    return toAjax(deptService.updateDept(dept));
  }

  @Operation(summary = "删除部门", description = "根据部门ID删除部门")
  @ApiResponse(responseCode = "200", description = "成功删除部门")
  @Parameter(name = "deptId", description = "部门ID", required = true)
  @DeleteMapping("/{deptId}")
  @Log(title = "删除部门", businessType = BusinessType.DELETE)
  public AjaxResult remove(@PathVariable("deptId") Long deptId) {
    ensureDeptWritable(currentUser());
    return toAjax(deptService.deleteDeptById(deptId));
  }

  private SysDept prepareDeptForUniqueCheck(SysDept dept) {
    if (dept == null) {
      return null;
    }
    SysDept target = new SysDept();
    target.setDeptId(dept.getDeptId());
    target.setParentId(dept.getParentId() == null ? 0L : dept.getParentId());
    target.setDeptName(dept.getDeptName());
    if (StrUtil.isNotBlank(dept.getTenantId())) {
      target.setTenantId(dept.getTenantId());
      return target;
    }
    IoTUser user = currentUser();
    if (user != null && !user.isAdmin()) {
      target.setTenantId(resolveTenantId(user));
      return target;
    }
    if (!ObjectUtil.equal(target.getParentId(), 0L)) {
      SysDept parentDept = deptService.selectDeptById(target.getParentId());
      if (parentDept != null) {
        target.setTenantId(parentDept.getTenantId());
      }
    }
    return target;
  }

  private IoTUser currentUser() {
    return ioTUserService.selectUserByUnionId(SecurityUtils.getUnionId());
  }

  protected String resolveTenantId(IoTUser user) {
    if (user == null) {
      return null;
    }
    return StrUtil.blankToDefault(user.getTenantId(), user.getUnionId());
  }

  private boolean isTenantAdmin(IoTUser user) {
    return user != null && !user.isAdmin() && StrUtil.equalsIgnoreCase(user.getTenantRole(), "ADMIN");
  }

  private void ensureDeptWritable(IoTUser user) {
    if (user == null || (!user.isAdmin() && !isTenantAdmin(user))) {
      throw new IoTException("你无权操作");
    }
  }

  private void ensureDeptReadable(IoTUser user, SysDept dept) {
    if (dept == null) {
      return;
    }
    if (user != null && user.isAdmin()) {
      return;
    }
    if (user == null || !StrUtil.equals(resolveTenantId(user), dept.getTenantId())) {
      throw new IoTException("你无权操作");
    }
  }

  private void ensureRolePermission(IoTUser operator, SysRole role) {
    if (ObjectUtil.isEmpty(role)) {
      return;
    }
    if (operator != null && operator.isAdmin()) {
      return;
    }
    if (!isTenantAdmin(operator) || !StrUtil.equals(role.getCreateBy(), operator.getUnionId())) {
      throw new IoTException("你无权操作");
    }
  }

  private String resolveRoleTenantId(SysRole role) {
    if (role == null || StrUtil.isBlank(role.getCreateBy())) {
      return null;
    }
    IoTUser roleOwner = ioTUserService.selectUserByUnionId(role.getCreateBy());
    return resolveTenantId(roleOwner);
  }
}
