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

import cn.universal.admin.platform.service.IIoTDevLogService;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.bo.IoTDeviceLogBO;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.security.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备日志Controller @Author ruoyi
 *
 * @since 2025-09-08
 */
@RestController
@RequestMapping("/admin/v1/device/log")
@Tag(name = "设备日志管理", description = "设备日志管理接口")
public class IoTDeviceLogV1Controller extends BaseController {

  @Autowired private IIoTDevLogService devLogService;

  /** 查询设备日志列表 */
  @Operation(summary = "查询设备日志列表", description = "根据查询条件获取设备日志列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备日志列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo<IoTDeviceLogVO> list(@Validated IoTDeviceLogBO bo) {
    startPage();
    List<IoTDeviceLogVO> ioTDeviceLogVOS = devLogService.queryPageList(bo);
    return getDataTable(ioTDeviceLogVOS);
  }

  /**
   * 获取设备事件的统计信息
   *
   * @param productId 产品Id
   * @param iotId 设备id
   */
  @Operation(summary = "获取设备事件的统计信息", description = "根据产品Id和设备Id获取设备事件的统计信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备事件统计信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @Parameter(name = "productId", description = "产品Id", required = true)
  @Parameter(name = "iotId", description = "设备Id", required = true)
  @GetMapping("/event/total/{productId}/{iotId}")
  public TableDataInfo<IoTDeviceEvents> getEventTotal(
      @PathVariable("productId") String productId, @PathVariable("iotId") String iotId) {
    List<IoTDeviceEvents> devLogVos = devLogService.queryEventTotal(productId, iotId);
    return getDataTable(devLogVos);
  }
}
