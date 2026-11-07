/*
 *
 * Copyright (c) 2025, IoT-Universal. All Rights Reserved.
 *
 * @Description: 本文件由 Aleo 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: Aleo
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.admin.platform.web;

import cn.universal.admin.platform.service.IGatewayPollingService;
import cn.universal.common.domain.R;
import cn.universal.persistence.dto.GatewayPollingConfigDTO;
import cn.universal.persistence.entity.GatewayPollingCommand;
import cn.universal.security.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 网关定时任务配置控制器
 *
 * @author gitee.com/NexIoT
 * @date 2025-10-26
 */
@RestController
@RequestMapping("/admin/v1/gateway/polling")
@Tag(name = "网关定时任务管理", description = "网关定时指令与Modbus采集任务配置接口")
public class GatewayPollingController extends BaseController {

  @Autowired private IGatewayPollingService gatewayPollingService;

  /** 保存网关定时任务配置 */
  @Operation(summary = "保存定时任务配置", description = "保存或更新网关定时指令或Modbus采集任务")
  @PostMapping(value = "/save")
  @PreAuthorize("@ss.hasPermi('platform:device:edit')")
  public R saveConfig(@RequestBody GatewayPollingConfigDTO dto) {
    return gatewayPollingService.savePollingConfig(dto);
  }

  @Operation(summary = "按任务类型保存定时任务配置", description = "保存云端轮询或Modbus采集的独立配置")
  @PostMapping(value = "/{taskType}/save")
  @PreAuthorize("@ss.hasPermi('platform:device:edit')")
  public R saveConfigByTaskType(
      @Parameter(description = "任务类型", required = true) @PathVariable String taskType,
      @RequestBody GatewayPollingConfigDTO dto) {
    return gatewayPollingService.savePollingConfig(taskType, dto);
  }

  /** 获取网关定时任务配置 */
  @Operation(summary = "获取定时任务配置", description = "根据产品Key和设备ID获取定时任务配置")
  @GetMapping("/get/{productKey}/{deviceId}")
  @PreAuthorize("@ss.hasPermi('platform:device:query')")
  public R getConfig(
      @Parameter(description = "产品Key", required = true) @PathVariable String productKey,
      @Parameter(description = "设备ID", required = true) @PathVariable String deviceId) {
    return gatewayPollingService.getPollingConfig(productKey, deviceId);
  }

  @Operation(summary = "按任务类型获取定时任务配置", description = "根据产品Key、设备ID和任务类型获取独立配置")
  @GetMapping("/{taskType}/get/{productKey}/{deviceId}")
  @PreAuthorize("@ss.hasPermi('platform:device:query')")
  public R getConfigByTaskType(
      @Parameter(description = "任务类型", required = true) @PathVariable String taskType,
      @Parameter(description = "产品Key", required = true) @PathVariable String productKey,
      @Parameter(description = "设备ID", required = true) @PathVariable String deviceId) {
    return gatewayPollingService.getPollingConfig(productKey, deviceId, taskType);
  }

  /** 删除网关定时任务配置 */
  @Operation(summary = "删除定时任务配置", description = "删除指定设备的定时任务配置")
  @DeleteMapping("/delete/{productKey}/{deviceId}")
  @PreAuthorize("@ss.hasPermi('platform:device:remove')")
  public R deleteConfig(
      @Parameter(description = "产品Key", required = true) @PathVariable String productKey,
      @Parameter(description = "设备ID", required = true) @PathVariable String deviceId) {
    return gatewayPollingService.deletePollingConfig(productKey, deviceId);
  }

  @Operation(summary = "按任务类型删除定时任务配置", description = "删除云端轮询或Modbus采集的独立配置")
  @DeleteMapping("/{taskType}/delete/{productKey}/{deviceId}")
  @PreAuthorize("@ss.hasPermi('platform:device:remove')")
  public R deleteConfigByTaskType(
      @Parameter(description = "任务类型", required = true) @PathVariable String taskType,
      @Parameter(description = "产品Key", required = true) @PathVariable String productKey,
      @Parameter(description = "设备ID", required = true) @PathVariable String deviceId) {
    return gatewayPollingService.deletePollingConfig(productKey, deviceId, taskType);
  }

  /** 测试网关定时任务 */
  @Operation(summary = "测试网关定时任务", description = "立即执行一次网关定时任务")
  @PostMapping("/test/{productKey}/{deviceId}")
  @PreAuthorize("@ss.hasPermi('platform:device:edit')")
  public R testPolling(
      @Parameter(description = "产品Key", required = true) @PathVariable String productKey,
      @Parameter(description = "设备ID", required = true) @PathVariable String deviceId) {
    return gatewayPollingService.testPolling(productKey, deviceId);
  }

  @Operation(summary = "按任务类型测试网关定时任务", description = "立即执行一次云端轮询或Modbus采集任务")
  @PostMapping("/{taskType}/test/{productKey}/{deviceId}")
  @PreAuthorize("@ss.hasPermi('platform:device:edit')")
  public R testPollingByTaskType(
      @Parameter(description = "任务类型", required = true) @PathVariable String taskType,
      @Parameter(description = "产品Key", required = true) @PathVariable String productKey,
      @Parameter(description = "设备ID", required = true) @PathVariable String deviceId) {
    return gatewayPollingService.testPolling(productKey, deviceId, taskType);
  }

  /** 测试单条任务 */
  @Operation(summary = "测试单条任务", description = "立即执行一条任务，Modbus采集任务返回原始帧、寄存器和点位解析结果")
  @PostMapping("/test-command/{productKey}/{deviceId}")
  @PreAuthorize("@ss.hasPermi('platform:device:edit')")
  public R testPollingCommand(
      @Parameter(description = "产品Key", required = true) @PathVariable String productKey,
      @Parameter(description = "设备ID", required = true) @PathVariable String deviceId,
      @RequestBody GatewayPollingCommand command) {
    return gatewayPollingService.testPollingCommand(productKey, deviceId, command);
  }

  @Operation(summary = "按任务类型测试单条任务", description = "立即执行一条云端轮询指令或Modbus采集试读")
  @PostMapping("/{taskType}/test-command/{productKey}/{deviceId}")
  @PreAuthorize("@ss.hasPermi('platform:device:edit')")
  public R testPollingCommandByTaskType(
      @Parameter(description = "任务类型", required = true) @PathVariable String taskType,
      @Parameter(description = "产品Key", required = true) @PathVariable String productKey,
      @Parameter(description = "设备ID", required = true) @PathVariable String deviceId,
      @RequestBody GatewayPollingCommand command) {
    return gatewayPollingService.testPollingCommand(productKey, deviceId, taskType, command);
  }
}
