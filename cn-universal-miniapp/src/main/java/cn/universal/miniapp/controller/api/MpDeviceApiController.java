/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.controller.api;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.domain.R;
import cn.universal.miniapp.dto.MpDeviceQueryDTO;
import cn.universal.miniapp.security.MpAppIdContextHolder;
import cn.universal.miniapp.security.MpPermission;
import cn.universal.miniapp.service.MpDeviceService;
import cn.universal.miniapp.vo.MpDashboardVO;
import cn.universal.miniapp.vo.MpDeviceVO;
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
 * 小程序API-设备接口 设备通过 linked_app_id 关联查询，所有绑定用户可看到相同的设备列表
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@RestController
@RequestMapping("/mp/api/device")
@Tag(name = "小程序-设备接口", description = "小程序API-设备接口，设备通过 linked_app_id 关联查询，所有绑定用户可看到相同的设备列表")
public class MpDeviceApiController {

  @Autowired private MpDeviceService mpDeviceService;

  /**
   * 获取设备列表 通过 mp_app.linked_app_id 关联查询 iot_device.application
   *
   * @return 设备列表
   */
  @GetMapping("/list")
  @Operation(
      summary = "获取设备列表",
      description = "通过 mp_app.linked_app_id 关联查询 iot_device.application 获取设备列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  public AjaxResult<List<MpDeviceVO>> getDeviceList() {
    String appId = MpAppIdContextHolder.getAppId();
    List<MpDeviceVO> list = mpDeviceService.getDeviceList(appId);
    return AjaxResult.success(list);
  }

  /**
   * 获取设备列表（V3，按当前登录用户与权限策略过滤）
   *
   * <p>规则：
   * <ul>
   *   <li>若当前用户拥有 DEVICE_VIEW_ALL 权限，则返回应用下所有设备（与V2一致）。</li>
   *   <li>否则，仅返回在 mp_user_device 中为该用户配置的设备列表。</li>
   * </ul>
   *
   * @param queryDTO 查询条件
   * @return 设备列表
   */
  @GetMapping("/v3/list")
  @Operation(
      summary = "获取设备列表（按用户设备授权）",
      description =
          "通过 mp_app.linked_app_id 关联查询 iot_device.application，在此基础上按当前登录用户的权限与设备授权范围进行过滤")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  public AjaxResult<List<MpDeviceVO>> getDeviceListV3(MpDeviceQueryDTO queryDTO) {
    String appId = MpAppIdContextHolder.getAppId();
    Long userId = MpAppIdContextHolder.getUserId();
    List<MpDeviceVO> list = mpDeviceService.getDeviceListForUser(appId, userId, queryDTO);
    return AjaxResult.success(list);
  }

  /**
   * 获取设备列表（支持多条件查询） 通过 mp_app.linked_app_id 关联查询 iot_device.application
   *
   * @param queryDTO 查询条件
   * @return 设备列表
   */
  @GetMapping("/v2/list")
  @Operation(
      summary = "获取设备列表（支持多条件查询）",
      description =
          "通过 mp_app.linked_app_id 关联查询 iot_device.application 获取设备列表，支持设备名称、设备ID模糊搜索，以及设备类型、标签、分组、分类筛选")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  public AjaxResult<List<MpDeviceVO>> getDeviceListV2(MpDeviceQueryDTO queryDTO) {
    String appId = MpAppIdContextHolder.getAppId();
    List<MpDeviceVO> list = mpDeviceService.getDeviceListWithQuery(appId, queryDTO);
    return AjaxResult.success(list);
  }

  /**
   * 获取仪表盘数据
   *
   * @return 仪表盘数据
   */
  @GetMapping("/dashboard")
  @Operation(summary = "获取仪表盘数据", description = "获取设备相关的仪表盘统计数据")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取仪表盘数据",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  public AjaxResult<MpDashboardVO> getDashboard() {
    String appId = MpAppIdContextHolder.getAppId();
    MpDashboardVO dashboard = mpDeviceService.getDashboard(appId);
    return AjaxResult.success(dashboard);
  }

  /**
   * 获取设备详情
   *
   * @param iotId 设备ID
   * @return 设备详情
   */
  @GetMapping("/detail/{iotId}")
  @Operation(summary = "获取设备详情", description = "根据iotId获取设备详细信息，包括设备属性、网关信息和子设备列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "iotId", description = "设备ID", required = true)
  public AjaxResult<MpDeviceVO> getDeviceDetail(@PathVariable String iotId) {
    MpDeviceVO device = mpDeviceService.getDeviceDetail(iotId);
    if (device == null) {
      return AjaxResult.error("设备不存在", null);
    }
    return AjaxResult.success(device);
  }

  /**
   * 功能调用
   *
   * @param iotId 设备ID
   * @param functionId 功能ID
   * @param paramsStr 参数字符串
   * @return 调用结果
   */
  @PostMapping("/function/{iotId}")
  @Operation(summary = "功能调用", description = "调用设备的指定功能")
  @ApiResponse(
      responseCode = "200",
      description = "成功调用功能",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "iotId", description = "设备ID", required = true)
  @Parameter(name = "functionId", description = "功能ID", required = true)
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "参数字符串",
      required = false,
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = String.class)))
  @MpPermission(value = "FUNCTION_CALL", message = "无功能调用权限")
  public AjaxResult<Object> callFunction(
      @PathVariable String iotId,
      @RequestParam String functionId,
      @RequestBody(required = false) String paramsStr) {
    String appId = MpAppIdContextHolder.getAppId();

    try {
      // 解析参数
      JSONObject params = null;
      if (paramsStr != null && !paramsStr.trim().isEmpty()) {
        try {
          params = JSONUtil.parseObj(paramsStr);
        } catch (Exception e) {
          log.warn("解析参数失败, 使用空对象: paramsStr={}", paramsStr);
          params = new JSONObject();
        }
      }

      // 调用Service
      R result = mpDeviceService.callFunction(appId, iotId, functionId, params);

      if (result.getCode() == 0) {
        return AjaxResult.success(result.getData());
      } else {
        return AjaxResult.error(result.getMsg(), null);
      }
    } catch (Exception e) {
      log.error("功能调用异常", e);
      return AjaxResult.error("功能调用失败: " + e.getMessage(), null);
    }
  }
}
