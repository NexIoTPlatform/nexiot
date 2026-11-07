/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.controller.api;

import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.miniapp.service.MpDeviceService;
import cn.universal.miniapp.vo.MpDeviceVO;
import cn.universal.persistence.query.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序端-设备影子API 所有绑定用户统一看到所有设备，不做设备级别权限隐离
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@RestController
@RequestMapping("/mp/api/shadow")
@Tag(name = "小程序设备影子API")
public class MpShadowApiController {

  @Autowired private IoTDeviceShadowService ioTDeviceShadowService;
  @Autowired private MpDeviceService mpDeviceService;

  /** 获取设备影子（属性状态） */
  @GetMapping("/{iotId}")
  @Operation(summary = "获取设备影子（属性状态）")
  public AjaxResult<List<IoTDevicePropertiesBO>> getDeviceShadow(@PathVariable String iotId) {
    // 所有绑定用户可看到同一应用下的所有设备
    List<IoTDevicePropertiesBO> properties = ioTDeviceShadowService.getDevState(iotId);
    return AjaxResult.success(properties);
  }

  /** 获取设备详情（含影子） */
  @GetMapping("/detail/{iotId}")
  @Operation(summary = "获取设备详情（含影子）")
  public AjaxResult<MpDeviceVO> getDeviceDetail(@PathVariable String iotId) {
    MpDeviceVO device = mpDeviceService.getDeviceDetail(iotId);
    return AjaxResult.success(device);
  }
}
