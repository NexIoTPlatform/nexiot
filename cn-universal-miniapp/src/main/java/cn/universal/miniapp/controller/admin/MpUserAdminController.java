/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.controller.admin;

import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.enums.OperatorType;
import cn.universal.miniapp.dto.MpDeviceQueryDTO;
import cn.universal.miniapp.dto.MpUserCreateDTO;
import cn.universal.miniapp.dto.MpUserQueryDTO;
import cn.universal.miniapp.entity.MpUser;
import cn.universal.miniapp.service.MpAppService;
import cn.universal.miniapp.service.MpDeviceService;
import cn.universal.miniapp.service.MpUserService;
import cn.universal.miniapp.vo.MpDeviceVO;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.utils.SecurityUtils;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 后台管理-小程序用户管理接口
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@RestController
@RequestMapping("/admin/mp/user")
@Tag(name = "小程序用户管理", description = "后台管理-小程序用户管理接口")
public class MpUserAdminController {

  @Autowired private MpUserService mpUserService;

  @Autowired private MpDeviceService mpDeviceService;

  @Autowired private MpAppService mpAppService;

  /**
   * 创建小程序用户
   *
   * @param dto 用户创建信息
   * @return 创建的用户信息
   */
  @PostMapping("/create")
  @Operation(summary = "创建小程序用户", description = "创建新的小程序用户")
  @ApiResponse(
      responseCode = "200",
      description = "成功创建小程序用户",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Log(
      title = "创建小程序用户",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<MpUser> createUser(@RequestBody MpUserCreateDTO dto) {
    String creatorId = SecurityUtils.getUnionId();
    MpUser user = mpUserService.createUser(dto, creatorId);
    return AjaxResult.success(user);
  }

  /**
   * 查询用户列表
   *
   * @param query 用户查询条件
   * @return 用户列表
   */
  @GetMapping("/list")
  @Operation(summary = "查询用户列表", description = "根据条件查询小程序用户列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功查询用户列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  public AjaxResult<PageInfo<MpUser>> listUsers(MpUserQueryDTO query) {
    PageInfo<MpUser> pageInfo = mpUserService.queryUserList(query);
    return AjaxResult.success(pageInfo);
  }

  /**
   * 获取用户详情
   *
   * @param userId 用户ID
   * @return 用户详情
   */
  @GetMapping("/{userId}")
  @Operation(summary = "获取用户详情", description = "根据用户ID获取小程序用户详情")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取用户详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "userId", description = "用户ID", required = true)
  public AjaxResult<MpUser> getUser(@PathVariable Long userId) {
    MpUser user = mpUserService.getAccessibleAdminUserById(userId);
    return AjaxResult.success(user);
  }

  /**
   * 获取用户权限列表
   *
   * @param userId 用户ID
   * @param appId 应用ID
   * @return 用户权限列表
   */
  @GetMapping("/{userId}/permissions")
  @Operation(summary = "获取用户权限列表", description = "根据用户ID和应用ID获取用户权限列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取用户权限列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "userId", description = "用户ID", required = true)
  @Parameter(name = "appId", description = "应用ID", required = true)
  public AjaxResult<List<String>> getUserPermissions(
      @PathVariable Long userId, @RequestParam String appId) {
    List<String> permissions = mpUserService.getAccessibleAdminUserPermissions(userId, appId);
    return AjaxResult.success(permissions);
  }

  /**
   * 更新用户权限
   *
   * @param userId 用户ID
   * @param appId 应用ID
   * @param permissions 权限列表
   * @return 更新结果
   */
  @PutMapping("/{userId}/permissions")
  @Operation(summary = "更新用户权限", description = "更新指定用户的权限列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功更新用户权限",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "userId", description = "用户ID", required = true)
  @Parameter(name = "appId", description = "应用ID", required = true)
  @Log(
      title = "更新用户权限",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> updatePermissions(
      @PathVariable Long userId,
      @RequestParam String appId,
      @RequestBody List<String> permissions) {
    mpUserService.updateUserPermissions(userId, appId, permissions);
    return AjaxResult.success();
  }

  /**
   * 获取用户设备绑定列表
   *
   * @param userId 用户ID
   * @param appId 应用ID
   * @return 设备iotId列表
   */
  @GetMapping("/{userId}/devices")
  @Operation(summary = "获取用户设备绑定列表", description = "根据用户ID和应用ID获取用户已授权的设备iotId列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取用户设备绑定列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "userId", description = "用户ID", required = true)
  @Parameter(name = "appId", description = "应用ID", required = true)
  public AjaxResult<List<String>> getUserDevices(
      @PathVariable Long userId, @RequestParam String appId) {
    List<String> iotIds = mpUserService.getUserDeviceIotIds(userId, appId);
    return AjaxResult.success(iotIds);
  }

  /**
   * 更新用户设备绑定列表
   *
   * @param userId 用户ID
   * @param appId 应用ID
   * @param iotIds 设备iotId列表
   * @return 更新结果
   */
  @PutMapping("/{userId}/devices")
  @Operation(summary = "更新用户设备绑定列表", description = "更新指定用户在某个小程序应用下授权可见的设备集合")
  @ApiResponse(
      responseCode = "200",
      description = "成功更新用户设备绑定列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "userId", description = "用户ID", required = true)
  @Parameter(name = "appId", description = "应用ID", required = true)
  @Log(
      title = "更新用户设备绑定",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> updateUserDevices(
      @PathVariable Long userId,
      @RequestParam String appId,
      @RequestBody List<String> iotIds) {
    mpUserService.updateUserDevices(userId, appId, iotIds);
    return AjaxResult.success();
  }

  /**
   * 查询可授权设备列表
   *
   * @param appId 小程序应用ID
   * @param query 查询条件
   * @return 设备列表
   */
  @GetMapping("/devices")
  @Operation(summary = "查询可授权设备列表", description = "根据小程序应用ID查询其下可授权的设备列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功查询可授权设备列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "appId", description = "应用ID", required = true)
  public AjaxResult<PageInfo<MpDeviceVO>> listAppDevices(
      @RequestParam String appId, MpDeviceQueryDTO query) {
    mpAppService.getAccessibleAdminAppByAppId(appId);
    PageInfo<MpDeviceVO> pageInfo = mpDeviceService.getDevicePageWithQuery(appId, query);
    return AjaxResult.success(pageInfo);
  }

  /**
   * 禁用用户
   *
   * @param userId 用户ID
   * @return 禁用结果
   */
  @PutMapping("/{userId}/disable")
  @Operation(summary = "禁用用户", description = "禁用指定的小程序用户")
  @ApiResponse(
      responseCode = "200",
      description = "成功禁用用户",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "userId", description = "用户ID", required = true)
  @Log(
      title = "禁用小程序用户",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> disableUser(@PathVariable Long userId) {
    mpUserService.disableUser(userId);
    return AjaxResult.success();
  }

  /**
   * 启用用户
   *
   * @param userId 用户ID
   * @return 启用结果
   */
  @PutMapping("/{userId}/enable")
  @Operation(summary = "启用用户", description = "启用指定的小程序用户")
  @ApiResponse(
      responseCode = "200",
      description = "成功启用用户",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "userId", description = "用户ID", required = true)
  @Log(
      title = "启用小程序用户",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> enableUser(@PathVariable Long userId) {
    mpUserService.enableUser(userId);
    return AjaxResult.success();
  }

  /**
   * 删除用户
   *
   * @param userId 用户ID
   * @return 删除结果
   */
  @DeleteMapping("/{userId}")
  @Operation(summary = "删除用户", description = "删除指定的小程序用户")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除用户",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "userId", description = "用户ID", required = true)
  @Log(
      title = "删除小程序用户",
      businessType = BusinessType.DELETE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> deleteUser(@PathVariable Long userId) {
    mpUserService.deleteUser(userId);
    return AjaxResult.success();
  }
}
