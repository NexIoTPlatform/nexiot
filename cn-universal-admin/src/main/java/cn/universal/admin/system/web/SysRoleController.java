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
import cn.universal.admin.common.utils.ExcelUtil;
import cn.universal.admin.system.service.ISysRoleService;
import cn.universal.common.annotation.Log;
import cn.universal.common.constant.IoTUserConstants;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.admin.SysRole;
import cn.universal.persistence.entity.admin.SysUserRole;
import cn.universal.persistence.entity.bo.IoTUserBO;
import cn.universal.persistence.page.TableDataInfo;
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
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 角色信息 @Author ruoyi */
@RestController
@RequestMapping("/admin/system/role")
@Tag(name = "系统角色管理", description = "系统角色信息管理接口")
public class SysRoleController extends BaseController {

  private static final String ROLE_TYPE_TENANT_CUSTOM = "TENANT_CUSTOM";

  @Autowired private ISysRoleService roleService;

  @Autowired private IoTUserService ioTUserService;

  @Operation(summary = "查询角色列表", description = "分页查询角色列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取角色列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo list(SysRole role) {
    IoTUser currentUser = currentUser();
    if (!currentUser.isAdmin()) {
      role.setTenantId(resolveTenantId(currentUser));
    }
    startPage();
    return getDataTable(roleService.selectPageRoleList(role));
  }

  @Operation(summary = "导出角色列表", description = "导出角色列表数据到Excel文件")
  @ApiResponse(responseCode = "200", description = "成功导出角色列表")
  @Parameter(name = "response", description = "HTTP响应对象", required = true)
  @PostMapping("/export")
  public void export(HttpServletResponse response, SysRole role) {
    IoTUser currentUser = currentUser();
    if (!currentUser.isAdmin()) {
      role.setTenantId(resolveTenantId(currentUser));
    }
    List<SysRole> list = roleService.selectRoleList(role);
    ExcelUtil<SysRole> util = new ExcelUtil<SysRole>(SysRole.class);
    util.exportExcel(response, list, "角色数据");
  }

  /** 根据角色编号获取详细信息 */
  @Operation(summary = "根据角色编号获取详细信息", description = "根据角色ID获取角色详细信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取角色详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "roleId", description = "角色ID", required = true)
  @GetMapping(value = "/{roleId}")
  public AjaxResult getInfo(@PathVariable Long roleId) {
    SysRole sysRole = roleService.selectRoleById(roleId);
    ensureRolePermission(currentUser(), sysRole);
    return AjaxResult.success(sysRole);
  }

  /** 新增角色 */
  @Operation(summary = "新增角色", description = "新增系统角色信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增角色",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping
  @Log(title = "新增角色", businessType = BusinessType.INSERT)
  public AjaxResult add(@Validated @RequestBody SysRole role) {
    IoTUser currentUser = currentUser();
    if (!canManageRoles(currentUser)) {
      throw new IoTException("你无权操作");
    }
    role.setCreateBy(currentUser.getUnionId());
    if (!currentUser.isAdmin()) {
      role.setTenantId(resolveTenantId(currentUser));
      role.setRoleType(ROLE_TYPE_TENANT_CUSTOM);
    }
    if (IoTUserConstants.NOT_UNIQUE.equals(roleService.checkRoleNameUnique(role))) {
      return error("新增角色'" + role.getRoleName() + "'失败，角色名称已存在");
    } else if (IoTUserConstants.NOT_UNIQUE.equals(roleService.checkRoleKeyUnique(role))) {
      return error("新增角色'" + role.getRoleName() + "'失败，角色权限已存在");
    }
    role.setCreateTime(new Date());
    return toAjax(roleService.insertRole(role));
  }

  /** 修改保存角色 */
  @Operation(summary = "修改保存角色", description = "修改保存系统角色信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改角色",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping
  @Log(title = "修改保存角色", businessType = BusinessType.UPDATE)
  public AjaxResult edit(@Validated @RequestBody SysRole role) {
    SysRole sysRole = roleService.selectRoleById(role.getRoleId());
    ensureRolePermission(currentUser(), sysRole);
    roleService.checkRoleAllowed(role);
    role.setCreateBy(sysRole.getCreateBy());
    role.setTenantId(sysRole.getTenantId());
    role.setRoleType(sysRole.getRoleType());
    if (IoTUserConstants.NOT_UNIQUE.equals(roleService.checkRoleNameUnique(role))) {
      return error("修改角色'" + role.getRoleName() + "'失败，角色名称已存在");
    } else if (IoTUserConstants.NOT_UNIQUE.equals(roleService.checkRoleKeyUnique(role))) {
      return error("修改角色'" + role.getRoleName() + "'失败，角色权限已存在");
    }
    role.setUpdateBy(SecurityUtils.getUnionId());

    if (roleService.updateRole(role) > 0) {
      // 更新缓存用户权限
      //      LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
      //      if (Validator.isNotNull(loginUser.getUser()) && !loginUser.getUser().isAdmin()) {
      //        loginUser.setPermissions(permissionService.getMenuPermission(loginUser.getUser()));
      //
      // loginUser.setUser(userService.selectUserByUserName(loginUser.getUser().getUserName()));
      //        tokenService.setLoginUser(loginUser);
      //      }
      return success();
    }
    return error("修改角色'" + role.getRoleName() + "'失败，请联系管理员");
  }

  /** 修改保存数据权限 */
  @PutMapping("/dataScope")
  @Log(title = "角色数据权限", businessType = BusinessType.UPDATE)
  public AjaxResult dataScope(@RequestBody SysRole role) {
    SysRole sysRole = roleService.selectRoleById(role.getRoleId());
    ensureRolePermission(currentUser(), sysRole);
    roleService.checkRoleAllowed(role);
    role.setUpdateBy(SecurityUtils.getUnionId());
    return toAjax(roleService.authDataScope(role));
  }

  /** 状态修改 */
  @Operation(summary = "状态修改", description = "修改角色状态")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改角色状态",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping("/changeStatus")
  public AjaxResult changeStatus(@RequestBody SysRole role) {
    SysRole sysRole = roleService.selectRoleById(role.getRoleId());
    ensureRolePermission(currentUser(), sysRole);
    roleService.checkRoleAllowed(role);
    role.setUpdateBy(SecurityUtils.getUnionId());
    return toAjax(roleService.updateRoleStatus(role));
  }

  /** 删除角色 */
  @Operation(summary = "删除角色", description = "根据角色ID数组删除角色")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除角色",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "roleIds", description = "角色ID数组", required = true)
  @DeleteMapping("/{roleIds}")
  @Log(title = "删除角色", businessType = BusinessType.DELETE)
  public AjaxResult remove(@PathVariable Long[] roleIds) {
    IoTUser currentUser = currentUser();
    Arrays.stream(roleIds)
        .forEach(
            id -> {
              SysRole sysRole = roleService.selectRoleById(id);
              ensureRolePermission(currentUser, sysRole);
            });

    return toAjax(roleService.deleteRoleByIds(roleIds));
  }

  /** 获取角色选择框列表 */

  //  @GetMapping("/optionselect")
  //  public AjaxResult optionselect() {
  //    return AjaxResult.success(roleService.selectRoleAll());
  //  }

  /** 查询已分配用户角色列表 */
  @Operation(summary = "查询已分配用户角色列表", description = "根据角色ID查询已分配的用户列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取已分配用户列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/authUser/allocatedList")
  public TableDataInfo allocatedList(IoTUserBO iotUserBo) {
    SysRole sysRole = roleService.selectRoleById(iotUserBo.getRoleId());
    IoTUser currentUser = currentUser();
    ensureRolePermission(currentUser, sysRole);
    if (!currentUser.isAdmin()) {
      iotUserBo.setTenantId(resolveTenantId(currentUser));
    }
    startPage();
    return getDataTable(ioTUserService.selectAllocatedList(iotUserBo));
  }

  /** 查询未分配用户角色列表 */
  @Operation(summary = "查询未分配用户角色列表", description = "根据角色ID查询未分配的用户列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取未分配用户列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/authUser/unallocatedList")
  public TableDataInfo unallocatedList(IoTUserBO user) {
    SysRole sysRole = roleService.selectRoleById(user.getRoleId());
    IoTUser currentUser = currentUser();
    ensureRolePermission(currentUser, sysRole);
    startPage();
    if (!currentUser.isAdmin()) {
      user.setTenantId(resolveTenantId(currentUser));
      user.setCreateBy(null);
    }
    return getDataTable(ioTUserService.selectUnallocatedList(user));
  }

  /** 取消授权用户 */
  @Operation(summary = "取消授权用户", description = "取消用户的角色授权")
  @ApiResponse(
      responseCode = "200",
      description = "成功取消用户授权",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping("/authUser/cancel")
  public AjaxResult cancelAuthUser(@RequestBody SysUserRole userRole) {
    SysRole sysRole = roleService.selectRoleById(userRole.getRoleId());
    IoTUser currentUser = currentUser();
    ensureRolePermission(currentUser, sysRole);
    ensureGrantableUser(currentUser, userRole.getUnionId());
    return toAjax(roleService.deleteAuthUser(userRole));
  }

  /** 批量取消授权用户 */
  @Operation(summary = "批量取消授权用户", description = "批量取消用户的角色授权")
  @ApiResponse(
      responseCode = "200",
      description = "成功批量取消用户授权",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "roleId", description = "角色ID", required = true)
  @Parameter(name = "unionIds", description = "用户ID数组", required = true)
  @PutMapping("/authUser/cancelAll")
  public AjaxResult cancelAuthUserAll(Long roleId, String[] unionIds) {
    SysRole sysRole = roleService.selectRoleById(roleId);
    IoTUser currentUser = currentUser();
    ensureRolePermission(currentUser, sysRole);
    ensureGrantableUsers(currentUser, unionIds);
    return toAjax(roleService.deleteAuthUsers(roleId, unionIds));
  }

  /** 批量选择用户授权 */
  @Operation(summary = "批量选择用户授权", description = "批量为用户授予角色权限")
  @ApiResponse(
      responseCode = "200",
      description = "成功批量授权用户",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "roleId", description = "角色ID", required = true)
  @Parameter(name = "unionIds", description = "用户ID数组", required = true)
  @PutMapping("/authUser/selectAll")
  public AjaxResult selectAuthUserAll(Long roleId, String[] unionIds) {
    SysRole sysRole = roleService.selectRoleById(roleId);
    IoTUser currentUser = currentUser();
    ensureRolePermission(currentUser, sysRole);
    ensureGrantableUsers(currentUser, unionIds);
    return toAjax(roleService.insertAuthUsers(roleId, unionIds));
  }

  private IoTUser currentUser() {
    return ioTUserService.selectUserByUnionId(SecurityUtils.getUnionId());
  }

  private boolean isTenantAdmin(IoTUser user) {
    return user != null && !user.isAdmin() && StrUtil.equalsIgnoreCase(user.getTenantRole(), "ADMIN");
  }

  private boolean canManageRoles(IoTUser user) {
    return user != null && (user.isAdmin() || isTenantAdmin(user));
  }

  protected String resolveTenantId(IoTUser user) {
    if (user == null) {
      return null;
    }
    return StrUtil.blankToDefault(user.getTenantId(), user.getUnionId());
  }

  private boolean canGrantRoleToUser(IoTUser operator, IoTUser target) {
    if (operator == null || target == null) {
      return false;
    }
    if (operator.isAdmin()) {
      return true;
    }
    return isTenantAdmin(operator)
        && StrUtil.equals(resolveTenantId(operator), resolveTenantId(target))
        && !StrUtil.equalsIgnoreCase(target.getTenantRole(), "ADMIN");
  }

  private void ensureGrantableUser(IoTUser operator, String unionId) {
    IoTUser target = ioTUserService.selectUserByUnionId(unionId);
    if (!canGrantRoleToUser(operator, target)) {
      throw new IoTException("你无权操作目标用户");
    }
  }

  private void ensureGrantableUsers(IoTUser operator, String[] unionIds) {
    if (unionIds == null || unionIds.length == 0) {
      return;
    }
    for (String unionId : unionIds) {
      ensureGrantableUser(operator, unionId);
    }
  }

  private void ensureRolePermission(IoTUser operator, SysRole role) {
    if (ObjectUtil.isEmpty(role)) {
      return;
    }
    if (operator != null && operator.isAdmin()) {
      return;
    }
    String operatorTenantId = resolveTenantId(operator);
    boolean tenantRoleMatched =
        StrUtil.isNotBlank(role.getTenantId())
            ? StrUtil.equals(role.getTenantId(), operatorTenantId)
            : StrUtil.equals(role.getCreateBy(), operator.getUnionId());
    if (!canManageRoles(operator) || !tenantRoleMatched) {
      throw new IoTException("你无权操作");
    }
  }
}
