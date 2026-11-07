/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.controller.admin;

import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.enums.OperatorType;
import cn.universal.miniapp.dto.MpAppCreateDTO;
import cn.universal.miniapp.entity.MpApp;
import cn.universal.miniapp.service.MpAppService;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.utils.SecurityUtils;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 后台管理-小程序应用管理接口
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@RestController
@RequestMapping("/admin/mp/app")
@Tag(name = "小程序应用管理", description = "后台管理-小程序应用管理接口")
public class MpAppAdminController {

  @Autowired private MpAppService mpAppService;

  /**
   * 创建小程序应用
   *
   * @param dto 应用创建信息
   * @return 创建的应用信息
   */
  @PostMapping("/create")
  @Operation(summary = "创建小程序应用", description = "创建新的小程序应用")
  @ApiResponse(
      responseCode = "200",
      description = "成功创建小程序应用",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "应用创建信息",
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = MpAppCreateDTO.class)))
  @Log(
      title = "创建小程序应用",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<MpApp> createApp(@RequestBody MpAppCreateDTO dto) {
    String creatorId = SecurityUtils.getUnionId();
    MpApp app = mpAppService.createApp(dto, creatorId);
    return AjaxResult.success(app);
  }

  /**
   * 查询应用列表
   *
   * @param name 应用名称
   * @param status 应用状态
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @return 应用列表
   */
  @GetMapping("/list")
  @Operation(summary = "查询应用列表", description = "根据条件查询小程序应用列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功查询应用列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "name", description = "应用名称", required = false)
  @Parameter(name = "status", description = "应用状态", required = false)
  @Parameter(name = "pageNum", description = "页码", required = false)
  @Parameter(name = "pageSize", description = "每页大小", required = false)
  public AjaxResult<PageInfo<MpApp>> listApps(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Integer status,
      @RequestParam(defaultValue = "1") int pageNum,
      @RequestParam(defaultValue = "10") int pageSize) {
    PageInfo<MpApp> pageInfo = mpAppService.queryAppList(name, status, pageNum, pageSize);
    return AjaxResult.success(pageInfo);
  }

  /**
   * 获取应用详情
   *
   * @param appId 应用ID
   * @return 应用详情
   */
  @GetMapping("/{appId}")
  @Operation(summary = "获取应用详情", description = "根据应用ID获取小程序应用详情")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取应用详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "appId", description = "应用ID", required = true)
  public AjaxResult<MpApp> getApp(@PathVariable String appId) {
    MpApp app = mpAppService.getAccessibleAdminAppByAppId(appId);
    return AjaxResult.success(app);
  }

  /**
   * 更新应用
   *
   * @param app 应用信息
   * @return 更新结果
   */
  @PutMapping("/update")
  @Operation(summary = "更新应用", description = "更新小程序应用信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功更新应用",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "应用信息",
      required = true,
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = MpApp.class)))
  @Log(
      title = "更新小程序应用",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> updateApp(@RequestBody MpApp app) {
    mpAppService.updateApp(app);
    return AjaxResult.success();
  }

  /**
   * 禁用应用
   *
   * @param appId 应用ID
   * @return 禁用结果
   */
  @PutMapping("/{appId}/disable")
  @Operation(summary = "禁用应用", description = "禁用指定的小程序应用")
  @ApiResponse(
      responseCode = "200",
      description = "成功禁用应用",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "appId", description = "应用ID", required = true)
  @Log(
      title = "禁用小程序应用",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> disableApp(@PathVariable String appId) {
    mpAppService.disableApp(appId);
    return AjaxResult.success();
  }

  /**
   * 启用应用
   *
   * @param appId 应用ID
   * @return 启用结果
   */
  @PutMapping("/{appId}/enable")
  @Operation(summary = "启用应用", description = "启用指定的小程序应用")
  @ApiResponse(
      responseCode = "200",
      description = "成功启用应用",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "appId", description = "应用ID", required = true)
  @Log(
      title = "启用小程序应用",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> enableApp(@PathVariable String appId) {
    mpAppService.enableApp(appId);
    return AjaxResult.success();
  }

  /**
   * 刷新应用统计
   *
   * @param appId 应用ID
   * @return 刷新结果
   */
  @PostMapping("/{appId}/refresh")
  @Operation(summary = "刷新应用统计", description = "刷新指定小程序应用的统计数据")
  @ApiResponse(
      responseCode = "200",
      description = "成功刷新应用统计",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "appId", description = "应用ID", required = true)
  public AjaxResult<Void> refreshStatistics(@PathVariable String appId) {
    mpAppService.refreshAppStatistics(appId);
    return AjaxResult.success();
  }

  /**
   * 删除应用
   *
   * @param appId 应用ID
   * @return 删除结果
   */
  @DeleteMapping("/{appId}")
  @Operation(summary = "删除应用", description = "删除指定的小程序应用")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除应用",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "appId", description = "应用ID", required = true)
  @Log(
      title = "删除小程序应用",
      businessType = BusinessType.DELETE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> deleteApp(@PathVariable String appId) {
    mpAppService.deleteApp(appId);
    return AjaxResult.success();
  }

  /**
   * 同步关联应用的密钥
   *
   * @param linkedAppId 关联应用ID
   * @return 同步结果
   */
  @PostMapping("/sync-secret/{linkedAppId}")
  @Operation(summary = "同步密钥", description = "同步关联应用的密钥")
  @ApiResponse(
      responseCode = "200",
      description = "成功同步密钥",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "linkedAppId", description = "关联应用ID", required = true)
  @Log(
      title = "同步应用密钥",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> syncAppSecret(@PathVariable String linkedAppId) {
    mpAppService.syncAccessibleAppSecretFromLinkedApp(linkedAppId);
    return AjaxResult.success();
  }

  /**
   * 同步所有应用的密钥
   *
   * @return 同步结果
   */
  @PostMapping("/sync-all-secret")
  @Operation(summary = "同步所有密钥", description = "同步所有小程序应用的密钥")
  @ApiResponse(
      responseCode = "200",
      description = "成功同步所有密钥",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Log(
      title = "同步所有应用密钥",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> syncAllAppSecrets() {
    mpAppService.syncAllAdminAppSecrets();
    return AjaxResult.success();
  }
}
