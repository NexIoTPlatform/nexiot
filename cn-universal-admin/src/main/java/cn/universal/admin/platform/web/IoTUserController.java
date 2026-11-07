/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 *
 * @Author: gitee.com/NexIoT
 *
 * @Email: wo8335224@gmail.com
 *
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.admin.platform.web;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.common.utils.ExcelUtil;
import cn.universal.admin.system.service.IIoTTenantService;
import cn.universal.admin.system.service.IIoTUserApplicationService;
import cn.universal.admin.system.service.ISysDeptService;
import cn.universal.admin.system.service.ISysRoleService;
import cn.universal.common.annotation.Log;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTUserConstants;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.exception.IoTException;
import cn.universal.common.utils.StringUtils;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.admin.SysDept;
import cn.universal.persistence.entity.admin.SysRole;
import cn.universal.persistence.entity.bo.IoTUserBO;
import cn.universal.persistence.entity.bo.IoTUserImportBO;
import cn.universal.persistence.entity.vo.IoTUserVO;
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
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * IoT用户管理控制器
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/8/12 19:11
 */
@RestController
@Tag(name = "IoT用户管理", description = "IoT用户管理接口，包括用户的增删改查、角色分配等")
@RequestMapping("/admin/system/user")
@Slf4j
public class IoTUserController extends BaseController {

  @Resource private IoTUserService ioTUserService;
  @Resource private ISysRoleService iSysRoleService;
  @Resource private ISysDeptService sysDeptService;
  @Resource private IIoTTenantService tenantService;

  @Resource private IIoTUserApplicationService iotUserApplicationService;

  /** 分页查询用户列表 */
  @Operation(summary = "分页查询用户列表", description = "分页查询用户列表信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取用户列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo<IoTUser> pageList(IoTUser user) {
    log.info("当前用户={}", SecurityUtils.getUnionId());
    IoTUser currentUser = currentUser();
    if (currentUser.isAdmin()) {
      user.setCreateBy(null);
      user.setTenantId(null);
    } else {
      user.setTenantId(resolveTenantId(currentUser));
      user.setUnionId(null);
      user.setCreateBy(null);
    }
    startPage();
    List<IoTUser> userList = ioTUserService.selectUserList(user);
    return getDataTable(userList);
  }

  @Operation(summary = "导出用户应用信息列表", description = "导出用户应用信息列表数据")
  @ApiResponse(
      responseCode = "200",
      description = "成功导出用户应用信息列表",
      content =
          @Content(
              mediaType = "application/octet-stream",
              schema = @Schema(implementation = Void.class)))
  @PostMapping("/export")
  @Log(title = "导出用户应用信息列表", businessType = BusinessType.EXPORT)
  public void export(HttpServletResponse response, IoTUser iotUser) {
    log.info("导出用户,操作人={},参数={}", SecurityUtils.getUnionId(), JSONUtil.toJsonStr(iotUser));
    IoTUser currentUser = currentUser();
    if (currentUser.isAdmin()) {
      iotUser.setCreateBy(null);
      iotUser.setTenantId(null);
    } else {
      iotUser.setTenantId(resolveTenantId(currentUser));
      iotUser.setUnionId(null);
      iotUser.setCreateBy(null);
    }
    List<IoTUser> list = ioTUserService.selectUserList(iotUser);
    ExcelUtil<IoTUser> util = new ExcelUtil<IoTUser>(IoTUser.class);
    util.exportExcel(response, list, "用户信息数据");
  }

  /** 下载用户导入模板。 */
  @PostMapping("/importTemplate")
  public void importTemplate(HttpServletResponse response) {
    ExcelUtil<IoTUserImportBO> util = new ExcelUtil<>(IoTUserImportBO.class);
    util.importTemplateExcel(response, "用户数据");
  }

  /** 批量导入用户，复用单用户新增/编辑的租户隔离和校验规则。 */
  @PostMapping("/importData")
  @Log(title = "用户导入", businessType = BusinessType.IMPORT)
  @Transactional
  public AjaxResult<Void> importData(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "updateSupport", defaultValue = "0") int updateSupport) {
    IoTUser operator = currentUser();
    if (!canManageUsers(operator)) {
      return error("你无权操作");
    }
    if (file == null || file.isEmpty()) {
      return error("请选择需要导入的文件");
    }

    try {
      ExcelUtil<IoTUserImportBO> util = new ExcelUtil<>(IoTUserImportBO.class);
      List<IoTUserImportBO> rows = util.importExcel(file.getInputStream());
      if (rows.isEmpty()) {
        return error("导入文件中没有用户数据");
      }
      if (rows.size() > 500) {
        return error("单次最多导入500个用户");
      }

      int created = 0;
      int updated = 0;
      for (int index = 0; index < rows.size(); index++) {
        IoTUserImportBO row = rows.get(index);
        validateImportRow(row, index + 2);
        IoTUser existing = ioTUserService.selectUserByUserName(row.getUsername().trim());
        IoTUserBO user = BeanUtil.toBean(row, IoTUserBO.class);
        user.setUsername(row.getUsername().trim());
        user.setAlias(StrUtil.blankToDefault(row.getAlias(), row.getUsername()).trim());
        user.setMobile(row.getMobile().trim());
        user.setStatus(row.getStatus() == null ? 0 : row.getStatus());

        AjaxResult<Void> result;
        if (existing == null) {
          if (StrUtil.isBlank(row.getPassword())) {
            throw new IoTException("第" + (index + 2) + "行新增用户密码不能为空");
          }
          result = add(user);
          created++;
        } else {
          if (updateSupport == 0) {
            throw new IoTException("第" + (index + 2) + "行用户名已存在：" + row.getUsername());
          }
          user.setId(existing.getId());
          user.setUnionId(existing.getUnionId());
          result = edit(user);
          updated++;
        }
        if (!AjaxResult.SUCCESS.equals(result.getCode())) {
          throw new IoTException("第" + (index + 2) + "行导入失败：" + result.getMsg());
        }
      }
      return success("导入完成，新增" + created + "条，更新" + updated + "条");
    } catch (IoTException e) {
      throw e;
    } catch (Exception e) {
      log.error("用户导入失败", e);
      throw new IoTException("用户导入失败：" + e.getMessage());
    }
  }

  private void validateImportRow(IoTUserImportBO row, int rowNumber) {
    if (row == null || StrUtil.isBlank(row.getUsername())) {
      throw new IoTException("第" + rowNumber + "行用户名不能为空");
    }
    if (StrUtil.isBlank(row.getMobile())) {
      throw new IoTException("第" + rowNumber + "行手机号不能为空");
    }
    if (row.getStatus() != null && row.getStatus() != 0 && row.getStatus() != 1) {
      throw new IoTException("第" + rowNumber + "行状态只能为0或1");
    }
  }

  /** 根据用户编号获取详细信息 */
  @GetMapping(value = {"/", "/{id}"})
  public AjaxResult<JSONObject> getInfo(@PathVariable(value = "id", required = false) Long id) {

    JSONObject jsonObject = new JSONObject();
    IoTUser currentUser = currentUser();
    jsonObject.set("roles", listManageableRoles(currentUser));
    jsonObject.set("createMode", currentUser.isAdmin() ? "TENANT_ADMIN" : "TENANT_MEMBER");
    jsonObject.set("canManageUsers", canManageUsers(currentUser));
    jsonObject.set("currentTenantId", resolveTenantId(currentUser));
    if (StringUtils.isNotNull(id)) {
      IoTUser iotUser = ioTUserService.selectUserById(id);
      if (iotUser == null || !canAccessUser(currentUser, iotUser)) {
        throw new IoTException("你无权查询用户");
      }
      IoTUserVO iotUserVO = BeanUtil.toBean(iotUser, IoTUserVO.class);
      iotUserVO.setRoleIds(iSysRoleService.selectRoleListByUnionId(iotUser.getUnionId()));
      jsonObject.set("data", iotUserVO);
    }

    return AjaxResult.success(jsonObject);
  }

  /** 新增用户 */
  @Operation(summary = "新增用户", description = "新增用户信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增用户",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping
  @Log(title = "新增用户", businessType = BusinessType.INSERT)
  @Transactional
  public AjaxResult<Void> add(@Validated @RequestBody IoTUserBO userbo) {
    log.info("新增用户,操作人={},参数={}", SecurityUtils.getUnionId(), JSONUtil.toJsonStr(userbo));
    IoTUser currentUser = currentUser();
    if (!canManageUsers(currentUser)) {
      throw new IoTException("你无权操作");
    }
    String unionId = currentUser.getUnionId();
    IoTUser user = BeanUtil.toBean(userbo, IoTUser.class);
    if (Objects.isNull(user.getMobile())) {
      return AjaxResult.error("新增用户'" + user.getUsername() + "'失败，手机号码不得为空");
    }
    if (IoTUserConstants.NOT_UNIQUE.equals(ioTUserService.checkUserNameUnique(user))) {
      return AjaxResult.error("新增用户'" + user.getUsername() + "'失败，用户名已存在");
    } else if (Validator.isNotEmpty(user.getMobile())
        && IoTUserConstants.NOT_UNIQUE.equals(ioTUserService.checkPhoneUnique(user))) {
      return AjaxResult.error("新增用户'" + user.getUsername() + "'失败，手机号码已存在");
    }
    if (StrUtil.isNotEmpty(user.getPassword())) {
      validatePassword(user.getPassword());
    }

    userbo.setCreateBy(unionId);
    if (currentUser.isAdmin()) {
      userbo.setParentUnionId("");
      userbo.setIdentity(1);
      userbo.setTenantId(null);
      userbo.setTenantRole("ADMIN");
      userbo.setRoleIds(null);
    } else {
      validateAssignableRoles(currentUser, userbo.getRoleIds());
      userbo.setParentUnionId("");
      userbo.setIdentity(1);
      userbo.setTenantId(resolveTenantId(currentUser));
      userbo.setTenantRole("MEMBER");
    }
    userbo.setCreateDate(new Date());
    userbo.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
    // userbo.setPassword(user.getPassword());
    userbo.setUnionId(userbo.getUsername());
    if (currentUser.isAdmin() && StrUtil.isBlank(userbo.getTenantId())) {
      IoTTenant bootstrapTenant =
          tenantService.createBootstrapTenant(
              userbo.getUsername(),
              StrUtil.blankToDefault(userbo.getAlias(), userbo.getUsername()),
              userbo.getAlias(),
              userbo.getMobile(),
              userbo.getUnionId(),
              currentUser.getUnionId());
      userbo.setTenantId(bootstrapTenant.getTenantId());
    }
    fillNewUserTenantDefaults(userbo);
    tenantService.assertTenantUserQuota(userbo.getTenantId(), 1);
    fillNewUserDeptDefaults(currentUser, userbo);
    userbo.setDeleted(0);
    int rows = ioTUserService.insertUser(userbo);
    if (rows > 0 && currentUser.isAdmin()) {
      tenantService.ensureTenantAdminRoleBinding(userbo.getTenantId(), userbo.getUnionId());
    }
    return toAjax(rows);
  }

  /** 修改用户 */
  @Operation(summary = "修改用户", description = "修改用户信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改用户",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping
  @Log(title = "修改用户", businessType = BusinessType.UPDATE)
  @Transactional
  public AjaxResult<Void> edit(@Validated @RequestBody IoTUserBO userbo) {
    log.info("修改用户,操作人={},参数={}", SecurityUtils.getUnionId(), JSONUtil.toJsonStr(userbo));
    IoTUser user = BeanUtil.toBean(userbo, IoTUser.class);
    ioTUserService.checkUserAllowed(user);
    IoTUser updateUser = ioTUserService.selectUserById(userbo.getId());
    IoTUser currentUser = currentUser();
    if (updateUser == null || !canUpdateUser(currentUser, updateUser)) {
      return error("你无权操作");
    }
    if (Validator.isEmpty(user.getMobile())) {
      return error("修改用户'" + user.getUsername() + "'失败，手机号码不能为空");
    }
    if (IoTUserConstants.NOT_UNIQUE.equals(ioTUserService.checkPhoneUnique(user))) {
      return error("修改用户'" + user.getUsername() + "'失败，手机号码已存在");
    }

    if ("".equals(userbo.getPassword())) {
      userbo.setPassword(null);
    } else if (userbo.getPassword() != null) {
      validatePassword(user.getPassword());
      userbo.setPassword(SecurityUtils.encryptPassword(userbo.getPassword()));
    }
    userbo.setTenantId(updateUser.getTenantId());
    userbo.setTenantRole(updateUser.getTenantRole());
    userbo.setParentUnionId("");
    userbo.setIdentity(updateUser.isAdmin() ? 0 : 1);
    userbo.setUnionId(updateUser.getUnionId());
    if (userbo.getStatus() == null) {
      userbo.setStatus(updateUser.getStatus());
    }
    if (isManagedTenantAdmin(updateUser)) {
      userbo.setRoleIds(null);
    } else {
      validateAssignableRoles(currentUser, userbo.getRoleIds());
    }
    fillUpdateUserDeptDefaults(updateUser, userbo);
    userbo.setUpdateBy(SecurityUtils.getUnionId());
    userbo.setUpdateDate(new Date());
    int count = ioTUserService.updateUser(userbo);
    if (count > 0) {
      iotUserApplicationService.enDisableIoTUser(
          updateUser.getUnionId(), IoTConstant.NORMAL.equals(userbo.getStatus()));
    }
    return toAjax(count);
  }

  /** 重置用户密码 */
  @Operation(summary = "重置用户密码", description = "仅重置指定用户的登录密码")
  @ApiResponse(
      responseCode = "200",
      description = "成功重置用户密码",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping("/resetPwd")
  @Log(title = "重置用户密码", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> resetPwd(@RequestBody IoTUserBO userbo) {
    if (userbo == null || userbo.getId() == null) {
      return error("请选择需要重置密码的用户");
    }
    if (StrUtil.isBlank(userbo.getPassword())) {
      return error("新密码不能为空");
    }
    IoTUser currentUser = currentUser();
    if (!canManageUsers(currentUser)) {
      return error("你无权操作");
    }
    IoTUser targetUser = ioTUserService.selectUserById(userbo.getId());
    if (targetUser == null || !canUpdateUser(currentUser, targetUser)) {
      return error("你无权操作");
    }
    validatePassword(userbo.getPassword());
    IoTUser updateUser = new IoTUser();
    updateUser.setId(targetUser.getId());
    updateUser.setUnionId(targetUser.getUnionId());
    updateUser.setPassword(SecurityUtils.encryptPassword(userbo.getPassword()));
    updateUser.setUpdateBy(SecurityUtils.getUnionId());
    updateUser.setUpdateDate(new Date());
    ioTUserService.updateUserById(updateUser);
    log.info("重置用户密码,操作人={},目标用户={}", SecurityUtils.getUnionId(), targetUser.getUnionId());
    return success();
  }

  /** 删除用户 */
  @Operation(summary = "删除用户", description = "根据用户ID数组删除用户")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除用户",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "userIds", description = "用户ID数组", required = true)
  @DeleteMapping("/{userIds}")
  @Log(title = "删除用户", businessType = BusinessType.DELETE)
  public AjaxResult<Void> remove(@PathVariable Long[] userIds) {
    if (userIds != null && userIds.length > 1) {
      return error("不允许批量删除");
    }
    IoTUser currentUser = currentUser();
    IoTUser deleteUser = ioTUserService.selectUserById(userIds[0]);
    if (deleteUser == null) {
      return error("用户不存在");
    }
    if (StrUtil.equals(currentUser.getUnionId(), deleteUser.getUnionId())) {
      return error("不允许删除当前登录账号");
    }
    if (isManagedTenantAdmin(deleteUser)) {
      return error("租户管理员账号请在租户管理中停用或删除租户，不支持在用户页直接删除");
    }
    if (!canDeleteUser(currentUser, deleteUser)) {
      return error("你无权删除用户");
    }
    return toAjax(ioTUserService.deleteUserByIds(userIds));
  }

  /** 根据用户编号获取授权角色 */
  @Operation(summary = "根据用户编号获取授权角色", description = "根据用户ID获取用户授权的角色信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取用户授权角色",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "用户ID", required = true)
  @GetMapping("/authRole/{id}")
  public AjaxResult authRole(@PathVariable("id") Long id) {
    log.info("授权用户,操作人={},参数={}", SecurityUtils.getUnionId(), id);

    JSONObject object = new JSONObject();
    IoTUser currentUser = currentUser();
    IoTUser user = ioTUserService.selectUserById(id);
    if (isManagedTenantAdmin(user)) {
      return error("租户管理员角色由系统按租户套餐自动维护，不支持手工授权");
    }
    if (user == null || !canGrantRole(currentUser, user)) {
      return error("你无权操作用户");
    }
    List<SysRole> roles = listManageableRoles(currentUser);
    Set<Long> assignedRoleIds = new HashSet<>(iSysRoleService.selectRoleListByUnionId(user.getUnionId()));
    roles.forEach(role -> role.setFlag(assignedRoleIds.contains(role.getRoleId())));
    object.set("user", user);
    object.set(
        "roles",
        user.isAdmin()
            ? roles
            : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
    return AjaxResult.success(object);
  }

  /** 用户授权角色 */
  @Operation(summary = "用户授权角色", description = "为用户分配角色权限")
  @ApiResponse(
      responseCode = "200",
      description = "成功为用户授权角色",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "unionId", description = "用户UnionId", required = true)
  @Parameter(name = "roleIds", description = "角色ID数组", required = true)
  @PutMapping("/authRole")
  public AjaxResult<Void> insertAuthRole(String unionId, Long[] roleIds) {
    IoTUser currentUser = currentUser();
    IoTUser user = ioTUserService.selectUserByUnionId(unionId);
    if (isManagedTenantAdmin(user)) {
      return error("租户管理员角色由系统按租户套餐自动维护，不支持手工授权");
    }
    if (user == null || !canGrantRole(currentUser, user)) {
      return error("你无权操作用户");
    }
    validateAssignableRoles(currentUser, roleIds);
    ioTUserService.insertUserAuth(unionId, roleIds);
    return success();
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

  private boolean canManageUsers(IoTUser user) {
    return user != null && (user.isAdmin() || isTenantAdmin(user));
  }

  private boolean canAccessUser(IoTUser operator, IoTUser target) {
    if (operator == null || target == null) {
      return false;
    }
    if (operator.isAdmin()) {
      return true;
    }
    if (StrUtil.equals(operator.getUnionId(), target.getUnionId())) {
      return true;
    }
    return isTenantAdmin(operator)
        && StrUtil.equals(resolveTenantId(operator), resolveTenantId(target));
  }

  private boolean canUpdateUser(IoTUser operator, IoTUser target) {
    if (!canAccessUser(operator, target)) {
      return false;
    }
    if (operator == null || target == null || operator.isAdmin()) {
      return true;
    }
    return !StrUtil.equalsIgnoreCase(target.getTenantRole(), "ADMIN")
        || StrUtil.equals(operator.getUnionId(), target.getUnionId());
  }

  private boolean canDeleteUser(IoTUser operator, IoTUser target) {
    if (operator == null || target == null) {
      return false;
    }
    if (operator.isAdmin()) {
      return !target.isAdmin();
    }
    return isTenantAdmin(operator)
        && StrUtil.equals(resolveTenantId(operator), resolveTenantId(target))
        && !StrUtil.equalsIgnoreCase(target.getTenantRole(), "ADMIN");
  }

  private boolean canGrantRole(IoTUser operator, IoTUser target) {
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

  private boolean isManagedTenantAdmin(IoTUser user) {
    return user != null && !user.isAdmin() && StrUtil.equalsIgnoreCase(user.getTenantRole(), "ADMIN");
  }

  private List<SysRole> listManageableRoles(IoTUser operator) {
    List<SysRole> roles = iSysRoleService.selectRoleAll();
    if (operator == null) {
      return List.of();
    }
    if (operator.isAdmin()) {
      return roles;
    }
    if (!isTenantAdmin(operator)) {
      return List.of();
    }
    return roles.stream()
        .filter(role -> !role.isAdmin())
        .filter(role -> StrUtil.equals(role.getCreateBy(), operator.getUnionId()))
        .collect(Collectors.toList());
  }

  private void validateAssignableRoles(IoTUser operator, Long[] roleIds) {
    if (roleIds == null || roleIds.length == 0) {
      return;
    }
    if (operator == null) {
      throw new IoTException("当前操作用户不存在");
    }
    Set<Long> manageableRoleIds =
        operator.isAdmin()
            ? null
            : listManageableRoles(operator).stream()
                .map(SysRole::getRoleId)
                .collect(Collectors.toSet());
    for (Long roleId : roleIds) {
      if (roleId == null) {
        continue;
      }
      SysRole role = iSysRoleService.selectRoleById(roleId);
      if (role == null) {
        throw new IoTException("角色不存在");
      }
      if (operator.isAdmin()) {
        continue;
      }
      if (role.isAdmin()
          || manageableRoleIds == null
          || !manageableRoleIds.contains(roleId)
          || !StrUtil.equals(role.getCreateBy(), operator.getUnionId())) {
        throw new IoTException("所选角色不存在或不属于当前租户");
      }
    }
  }

  private void validatePassword(String password) {
    Matcher matcher = IoTConstant.pattern.matcher(password);
    if (!matcher.matches()) {
      throw new IoTException("密码中必须包含字母、数字、特殊字符，至少8个字符，最多30个字符");
    }
  }

  private void fillNewUserTenantDefaults(IoTUserBO userbo) {
    if (userbo == null) {
      return;
    }
    if (StrUtil.isBlank(userbo.getTenantId())) {
      if (StrUtil.equalsIgnoreCase(userbo.getTenantRole(), "ADMIN")) {
        userbo.setTenantId(userbo.getUnionId());
      }
    }
    if (StrUtil.isBlank(userbo.getTenantRole())) {
      userbo.setTenantRole(StrUtil.equals(userbo.getTenantId(), userbo.getUnionId()) ? "ADMIN" : "MEMBER");
    }
  }

  private void fillNewUserDeptDefaults(IoTUser currentUser, IoTUserBO userbo) {
    if (currentUser == null || userbo == null) {
      return;
    }
    if (currentUser.isAdmin()) {
      SysDept rootDept =
          sysDeptService.ensureTenantRootDept(userbo.getTenantId(), userbo.getUnionId(), null);
      userbo.setDeptId(rootDept.getDeptId());
      return;
    }
    ensureMemberDept(userbo.getDeptId(), userbo.getTenantId());
  }

  private void fillUpdateUserDeptDefaults(IoTUser updateUser, IoTUserBO userbo) {
    if (updateUser == null || userbo == null) {
      return;
    }
    String tenantId = resolveTenantId(updateUser);
    if (StrUtil.equalsIgnoreCase(updateUser.getTenantRole(), "ADMIN")) {
      SysDept rootDept =
          sysDeptService.ensureTenantRootDept(tenantId, updateUser.getUnionId(), null);
      userbo.setDeptId(rootDept.getDeptId());
      return;
    }
    if (userbo.getDeptId() == null) {
      if (updateUser.getDeptId() != null) {
        userbo.setDeptId(updateUser.getDeptId());
        return;
      }
      SysDept rootDept = sysDeptService.ensureTenantRootDept(tenantId, updateUser.getUnionId(), null);
      userbo.setDeptId(rootDept.getDeptId());
      return;
    }
    ensureMemberDept(userbo.getDeptId(), tenantId);
  }

  private void ensureMemberDept(Long deptId, String tenantId) {
    if (deptId == null) {
      throw new IoTException("请选择所属部门");
    }
    SysDept dept = sysDeptService.selectDeptById(deptId);
    if (dept == null || !StrUtil.equals(dept.getTenantId(), tenantId)) {
      throw new IoTException("所选部门不存在或不属于当前租户");
    }
    if (!StrUtil.equals(dept.getStatus(), "0")) {
      throw new IoTException("所选部门已停用");
    }
  }
}
