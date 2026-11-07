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

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.system.service.IoTDeviceSubscribeService;
import cn.universal.common.annotation.Log;
import cn.universal.common.domain.R;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTDeviceSubscribe;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备订阅Controller
 *
 * @since 2025-12-30
 */
@RestController
@Tag(name = "设备订阅管理", description = "设备订阅管理接口")
@RequestMapping("/admin/v1/subscribe")
public class IoTDeviceSubscribeController extends BaseController {

  @Autowired private IoTDeviceSubscribeService devSubscribeService;

  /** 查询订阅列表 */
  @Operation(summary = "查询订阅列表", description = "根据产品Key和设备ID查询订阅列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取订阅列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping(value = "/list")
  public TableDataInfo list(IoTDeviceSubscribe sub) {
    startPage();
    if (StrUtil.isBlank(sub.getProductKey()) || StrUtil.isBlank(sub.getIotId())) {
      throw new IoTException("缺少必要参数");
    }
    List<IoTDeviceSubscribe> list = devSubscribeService.selectDevSubscribeList(sub);
    return getDataTable(list);
  }

  /** 查看订阅详情 */
  @Operation(summary = "查看订阅详情", description = "根据订阅ID获取订阅详细信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取订阅详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "订阅ID", required = true)
  @GetMapping(value = "/{id}")
  public AjaxResult info(@PathVariable("id") String id) {
    return AjaxResult.success(devSubscribeService.selectDevInstanceById(id));
  }

  /** 修改订阅状态 */
  @Operation(summary = "修改订阅状态", description = "修改设备订阅状态")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改订阅状态",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @PostMapping(value = "/update")
  @Log(title = "修改订阅状态", businessType = BusinessType.OTHER)
  public R updateSubscribe(@RequestBody IoTDeviceSubscribe sub) {
    return devSubscribeService.updateDevSubscribe(sub);
  }

  /** 新增订阅 */
  @Operation(summary = "新增订阅", description = "为设备新增订阅")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增订阅",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @PostMapping(value = "/add")
  @Log(title = "设备新增订阅", businessType = BusinessType.OTHER)
  public R bindSubscribe(@RequestBody IoTDeviceSubscribe sub) {
    return devSubscribeService.insertDevSubscribe(sub);
  }

  @Operation(summary = "删除设备订阅", description = "根据产品Key、设备ID和订阅ID数组删除设备订阅")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除设备订阅",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @Parameter(name = "iotId", description = "设备ID", required = true)
  @PostMapping(value = "/delete/{productKey}/{iotId}")
  @Log(title = "设备删除订阅", businessType = BusinessType.OTHER)
  public R unBindSubscribe(
      @PathVariable("productKey") String productKey,
      @PathVariable("iotId") String iotId,
      @RequestBody Long[] ids) {
    return devSubscribeService.deleteDevSubscribe(productKey, iotId, ids);
  }
}
