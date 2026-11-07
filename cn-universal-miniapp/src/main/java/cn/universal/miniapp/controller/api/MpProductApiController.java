/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.controller.api;

import cn.universal.core.metadata.DeviceMetadata;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.persistence.query.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序API-产品接口 提供产品物模型查询等功能
 *
 * @author NexIoT
 * @since 2025/12/19
 */
@Slf4j
@RestController
@RequestMapping("/mp/api/product")
@Tag(name = "小程序-产品接口")
public class MpProductApiController {

  @Autowired private IoTProductDeviceService ioTProductDeviceService;

  /**
   * 获取产品物模型
   *
   * @param productKey 产品Key
   * @return 物模型JSON字符串
   */
  @GetMapping("/meta/{productKey}")
  @Operation(summary = "获取产品物模型")
  public AjaxResult<DeviceMetadata> getProductMeta(@PathVariable("productKey") String productKey) {
    try {
      log.info("[小程序-产品] 获取物模型: productKey={}", productKey);
      DeviceMetadata deviceMetadata = ioTProductDeviceService.getDeviceMetadata(productKey);
      return AjaxResult.success(deviceMetadata);
    } catch (Exception e) {
      log.error("[小程序-产品] 获取物模型失败: productKey={}", productKey, e);
      return AjaxResult.error("获取物模型失败", null);
    }
  }
}
