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

package cn.universal.admin.platform.web;

import cn.universal.admin.platform.service.IIoTDeviceGroupService;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.enums.OperatorType;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.bo.IoTDeviceGroupBO;
import cn.universal.persistence.entity.vo.IoTDeviceGroupVO;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备分组Controller
 *
 * @since 2025-12-30
 */
@RestController
@Tag(name = "设备分组管理", description = "设备分组管理")
@RequestMapping("/admin/v1/instance/group")
public class IoTDeviceGroupController extends BaseController {

  @Autowired private IIoTDeviceGroupService devGroupService;

  /**
   * 查询设备分组列表
   *
   * @return 设备分组列表
   */
  @Operation(summary = "查询设备分组树", description = "获取所有设备分组信息，以树形结构展示")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备分组列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/list")
  public AjaxResult<List<IoTDeviceGroupVO>> list() {
    List<IoTDeviceGroupVO> ioTDeviceGroupVO = devGroupService.selectDevGroupList();
    return AjaxResult.success(ioTDeviceGroupVO);
  }

  /**
   * 获取设备分组详细信息
   *
   * @param id 分组ID
   * @return 设备分组详细信息
   */
  @Operation(summary = "获取设备分组详细信息", description = "根据分组ID获取设备分组的详细信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备分组详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping(value = "/{id}")
  public AjaxResult<IoTDeviceGroupVO> getInfo(
      @Parameter(description = "分组ID", example = "1") @PathVariable("id") Long id) {
    return AjaxResult.success(devGroupService.selectDevGroupById(id));
  }

  /**
   * 新增设备分组
   *
   * @param devGroup 设备分组信息
   * @return 操作结果
   */
  @Operation(summary = "新增设备分组", description = "创建一个新的设备分组")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增设备分组",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping
  @Log(
      title = "新增设备分组",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> add(
      @Parameter(description = "设备分组信息") @RequestBody IoTDeviceGroupBO devGroup) {
    rejectSuperAdminCreate("设备分组");
    return devGroupService.insertDevGroup(devGroup);
  }

  /**
   * 修改设备分组
   *
   * @param devGroup 设备分组信息
   * @return 操作结果
   */
  @Operation(summary = "修改设备分组", description = "更新设备分组信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改设备分组",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping
  @Log(
      title = "修改设备分组",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> edit(
      @Parameter(description = "设备分组信息") @RequestBody IoTDeviceGroupBO devGroup) {
    devGroupService.updateDevGroup(devGroup);
    return AjaxResult.success();
  }

  /**
   * 删除设备分组
   *
   * @param id 分组ID
   * @return 操作结果
   */
  @Operation(summary = "删除设备分组", description = "根据分组ID删除设备分组")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除设备分组",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @DeleteMapping("/{id}")
  @Log(
      title = "删除设备分组",
      businessType = BusinessType.DELETE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> remove(
      @Parameter(description = "分组ID", example = "1") @PathVariable("id") Long id) {
    return devGroupService.deleteDevGroupById(id);
  }

  /**
   * 批量绑定设备到分组
   *
   * @param devGroup 设备分组信息（包含要绑定的设备列表）
   * @return 操作结果
   */
  @Operation(summary = "批量绑定设备到分组", description = "将多个设备批量绑定到指定分组")
  @ApiResponse(
      responseCode = "200",
      description = "成功绑定设备到分组",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/binds")
  @Log(
      title = "批量绑定设备到分组",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> binds(
      @Parameter(description = "设备分组信息（包含要绑定的设备列表）") @RequestBody IoTDeviceGroupBO devGroup) {
    return devGroupService.bindDevToGroup(devGroup);
  }

  /**
   * 查询分组下的设备列表
   *
   * @param groupId 分组ID
   * @return 分组下的设备列表
   */
  @Operation(summary = "查询分组下的设备列表", description = "根据分组ID查询该分组下的所有设备")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取分组设备列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/device/list")
  public TableDataInfo getDeviceList(
      @Parameter(description = "分组ID", example = "1") String groupId) {
    startPage();
    List<IoTDevice> list = devGroupService.selectDevInstanceListByGroupId(groupId);
    return getDataTable(list);
  }

  /**
   * 解除设备与分组的绑定关系
   *
   * @param groupId 分组ID
   * @param devId 设备ID数组
   * @return 操作结果
   */
  @Operation(summary = "解除绑定", description = "解除指定设备与分组的绑定关系")
  @ApiResponse(
      responseCode = "200",
      description = "成功解除绑定",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Log(
      title = "解除绑定",
      businessType = BusinessType.OTHER,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  @DeleteMapping("/{groupId}/{devId}")
  public AjaxResult<Void> unBind(
      @Parameter(description = "分组ID", example = "group1") @PathVariable("groupId") String groupId,
      @Parameter(description = "设备ID数组", example = "[\"device1\", \"device2\"]")
          @PathVariable("devId")
          String[] devId) {
    return devGroupService.unBindDevByGroupId(groupId, devId);
  }
}
