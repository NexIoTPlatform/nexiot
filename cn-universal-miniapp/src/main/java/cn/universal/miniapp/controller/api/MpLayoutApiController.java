/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.controller.api;

import cn.universal.miniapp.entity.MpComponentLibrary;
import cn.universal.miniapp.entity.MpDetailLayout;
import cn.universal.miniapp.security.MpAppIdContextHolder;
import cn.universal.miniapp.service.MpComponentLibraryService;
import cn.universal.miniapp.service.MpDetailLayoutService;
import cn.universal.persistence.query.AjaxResult;
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
 * 小程序端-装修配置API
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@RestController
@RequestMapping("/api/mp/layout")
@Tag(name = "小程序装修配置API", description = "小程序端-装修配置API")
public class MpLayoutApiController {

  @Autowired private MpComponentLibraryService componentLibraryService;

  @Autowired private MpDetailLayoutService detailLayoutService;

  /**
   * 获取组件库列表
   *
   * @param type 组件类型
   * @return 组件库列表
   */
  @GetMapping("/components")
  @Operation(summary = "获取组件库列表", description = "根据组件类型获取组件库列表，如果不指定类型则返回所有启用的组件")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取组件库列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "type", description = "组件类型", required = false)
  public AjaxResult<List<MpComponentLibrary>> getComponents(
      @RequestParam(required = false) String type) {
    List<MpComponentLibrary> components;
    if (type != null && !type.isEmpty()) {
      components = componentLibraryService.getComponentsByType(type);
    } else {
      components = componentLibraryService.getAllEnabledComponents();
    }
    return AjaxResult.success(components);
  }

  /**
   * 获取设备详情布局配置
   *
   * @param productKey 产品Key
   * @return 设备详情布局配置
   */
  @GetMapping("/detail/{productKey}")
  @Operation(summary = "获取设备详情布局配置", description = "根据产品Key获取设备详情布局配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备详情布局配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  public AjaxResult<MpDetailLayout> getDeviceLayout(@PathVariable String productKey) {
    String appId = MpAppIdContextHolder.getAppId();
    MpDetailLayout layout = detailLayoutService.getLayoutConfig(appId, productKey);
    return AjaxResult.success(layout);
  }

  /**
   * 获取当前应用所有布局配置
   *
   * @return 当前应用所有布局配置
   */
  @GetMapping("/all")
  @Operation(summary = "获取当前应用所有布局配置", description = "获取当前应用所有布局配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取当前应用所有布局配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  public AjaxResult<List<MpDetailLayout>> getAllLayouts() {
    String appId = MpAppIdContextHolder.getAppId();
    List<MpDetailLayout> layouts = detailLayoutService.getLayoutsByAppId(appId);
    return AjaxResult.success(layouts);
  }
}
