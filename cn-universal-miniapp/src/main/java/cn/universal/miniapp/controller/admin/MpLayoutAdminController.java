/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.controller.admin;

import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.enums.OperatorType;
import cn.universal.miniapp.dto.MpLayoutConfigDTO;
import cn.universal.miniapp.entity.MpDetailLayout;
import cn.universal.miniapp.service.MpDetailLayoutService;
import cn.universal.persistence.query.AjaxResult;
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
 * 后台管理-装修配置管理接口
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@RestController
@RequestMapping("/admin/mp/layout")
@Tag(name = "装修配置管理", description = "后台管理-装修配置管理接口")
public class MpLayoutAdminController {

  @Autowired private MpDetailLayoutService detailLayoutService;

  /**
   * 查询布局配置列表
   *
   * @param appId 应用ID
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @return 布局配置列表
   */
  @GetMapping("/list")
  @Operation(summary = "查询布局配置列表", description = "根据应用ID查询布局配置列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功查询布局配置列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "appId", description = "应用ID", required = true)
  @Parameter(name = "pageNum", description = "页码", required = false)
  @Parameter(name = "pageSize", description = "每页大小", required = false)
  public AjaxResult<PageInfo<MpDetailLayout>> listLayouts(
      @RequestParam String appId,
      @RequestParam(defaultValue = "1") int pageNum,
      @RequestParam(defaultValue = "10") int pageSize) {
    PageInfo<MpDetailLayout> pageInfo =
        detailLayoutService.queryAdminLayoutList(appId, pageNum, pageSize);
    return AjaxResult.success(pageInfo);
  }

  /**
   * 获取布局配置详情
   *
   * @param appId 应用ID
   * @param productKey 产品Key
   * @return 布局配置详情
   */
  @GetMapping("/{appId}/{productKey}")
  @Operation(summary = "获取布局配置详情", description = "根据应用ID和产品Key获取布局配置详情")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取布局配置详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "appId", description = "应用ID", required = true)
  @Parameter(name = "productKey", description = "产品Key", required = true)
  public AjaxResult<MpDetailLayout> getLayout(
      @PathVariable String appId, @PathVariable String productKey) {
    MpDetailLayout layout = detailLayoutService.getAdminLayoutConfig(appId, productKey);
    return AjaxResult.success(layout);
  }

  /**
   * 保存布局配置
   *
   * @param dto 布局配置信息
   * @return 保存的布局配置
   */
  @PostMapping("/save")
  @Operation(summary = "保存布局配置", description = "保存新的布局配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功保存布局配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Log(
      title = "保存布局配置",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<MpDetailLayout> saveLayout(@RequestBody MpLayoutConfigDTO dto) {
    MpDetailLayout layout = detailLayoutService.saveAdminLayoutConfig(dto);
    return AjaxResult.success(layout);
  }

  /**
   * 更新布局配置
   *
   * @param id 布局配置ID
   * @param dto 布局配置信息
   * @return 更新结果
   */
  @PutMapping("/{id}")
  @Operation(summary = "更新布局配置", description = "更新指定的布局配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功更新布局配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "布局配置ID", required = true)
  @Log(
      title = "更新布局配置",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> updateLayout(@PathVariable Long id, @RequestBody MpLayoutConfigDTO dto) {
    detailLayoutService.updateAdminLayoutConfig(id, dto);
    return AjaxResult.success();
  }

  /**
   * 删除布局配置
   *
   * @param id 布局配置ID
   * @return 删除结果
   */
  @DeleteMapping("/{id}")
  @Operation(summary = "删除布局配置", description = "删除指定的布局配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除布局配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "布局配置ID", required = true)
  @Log(
      title = "删除布局配置",
      businessType = BusinessType.DELETE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> deleteLayout(@PathVariable Long id) {
    detailLayoutService.deleteAdminLayout(id);
    return AjaxResult.success();
  }

  /**
   * 启用/禁用布局
   *
   * @param id 布局配置ID
   * @param status 布局状态(0-禁用, 1-启用)
   * @return 操作结果
   */
  @PutMapping("/{id}/status")
  @Operation(summary = "启用/禁用布局", description = "启用或禁用指定的布局配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功启用/禁用布局",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "布局配置ID", required = true)
  @Parameter(name = "status", description = "布局状态(0-禁用, 1-启用)", required = true)
  @Log(
      title = "启用/禁用布局",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
    detailLayoutService.toggleAdminLayoutStatus(id, status);
    return AjaxResult.success();
  }
}
