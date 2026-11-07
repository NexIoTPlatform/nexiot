/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.controller.admin;

import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.enums.OperatorType;
import cn.universal.miniapp.entity.MpComponentLibrary;
import cn.universal.miniapp.service.MpComponentLibraryService;
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
 * 后台管理-组件库管理接口
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@RestController
@RequestMapping("/admin/mp/component")
@Tag(name = "组件库管理", description = "后台管理-组件库管理接口")
public class MpComponentAdminController {

  @Autowired private MpComponentLibraryService componentLibraryService;

  /**
   * 查询组件列表
   *
   * @param type 组件类型
   * @param status 组件状态
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @return 组件列表
   */
  @GetMapping("/list")
  @Operation(summary = "查询组件列表", description = "根据条件查询组件列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功查询组件列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "type", description = "组件类型", required = false)
  @Parameter(name = "status", description = "组件状态", required = false)
  @Parameter(name = "pageNum", description = "页码", required = false)
  @Parameter(name = "pageSize", description = "每页大小", required = false)
  public AjaxResult<PageInfo<MpComponentLibrary>> listComponents(
      @RequestParam(required = false) String type,
      @RequestParam(required = false) Integer status,
      @RequestParam(defaultValue = "1") int pageNum,
      @RequestParam(defaultValue = "10") int pageSize) {
    PageInfo<MpComponentLibrary> pageInfo =
        componentLibraryService.queryComponentList(type, status, pageNum, pageSize);
    return AjaxResult.success(pageInfo);
  }

  /**
   * 获取组件详情
   *
   * @param id 组件ID
   * @return 组件详情
   */
  @GetMapping("/{id}")
  @Operation(summary = "获取组件详情", description = "根据组件ID获取组件详情")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取组件详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "组件ID", required = true)
  public AjaxResult<MpComponentLibrary> getComponent(@PathVariable Long id) {
    MpComponentLibrary component = componentLibraryService.getComponentById(id);
    return AjaxResult.success(component);
  }

  /**
   * 创建组件
   *
   * @param component 组件信息
   * @return 创建的组件
   */
  @PostMapping("/create")
  @Operation(summary = "创建组件", description = "创建新的组件")
  @ApiResponse(
      responseCode = "200",
      description = "成功创建组件",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Log(
      title = "创建组件",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<MpComponentLibrary> createComponent(@RequestBody MpComponentLibrary component) {
    MpComponentLibrary created = componentLibraryService.createComponent(component);
    return AjaxResult.success(created);
  }

  /**
   * 更新组件
   *
   * @param component 组件信息
   * @return 更新结果
   */
  @PutMapping("/update")
  @Operation(summary = "更新组件", description = "更新组件信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功更新组件",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Log(
      title = "更新组件",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> updateComponent(@RequestBody MpComponentLibrary component) {
    componentLibraryService.updateComponent(component);
    return AjaxResult.success();
  }

  /**
   * 删除组件
   *
   * @param id 组件ID
   * @return 删除结果
   */
  @DeleteMapping("/{id}")
  @Operation(summary = "删除组件", description = "删除指定的组件")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除组件",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "组件ID", required = true)
  @Log(
      title = "删除组件",
      businessType = BusinessType.DELETE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> deleteComponent(@PathVariable Long id) {
    componentLibraryService.deleteComponent(id);
    return AjaxResult.success();
  }

  /**
   * 启用/禁用组件
   *
   * @param id 组件ID
   * @param status 组件状态(0-禁用, 1-启用)
   * @return 操作结果
   */
  @PutMapping("/{id}/status")
  @Operation(summary = "启用/禁用组件", description = "启用或禁用指定的组件")
  @ApiResponse(
      responseCode = "200",
      description = "成功启用/禁用组件",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "组件ID", required = true)
  @Parameter(name = "status", description = "组件状态(0-禁用, 1-启用)", required = true)
  @Log(
      title = "启用/禁用组件",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
    componentLibraryService.toggleComponentStatus(id, status);
    return AjaxResult.success();
  }
}
