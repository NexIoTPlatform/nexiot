/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA任务管理控制器
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.web;

import cn.universal.admin.platform.service.IIoTOtaAdminService;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.dm.device.service.ota.api.OtaService.OtaServiceResult;
import cn.universal.persistence.entity.IoTOtaDeviceVersion;
import cn.universal.persistence.entity.IoTOtaTask;
import cn.universal.persistence.entity.IoTOtaTaskDevice;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** OTA任务管理控制器 */
@RestController
@RequestMapping("/admin/v1/ota/task")
@Tag(name = "OTA任务管理")
@RequiredArgsConstructor
public class IoTOtaTaskController extends BaseController {

  private final IIoTOtaAdminService otaAdminService;

  @PreAuthorize("@ss.hasPermi('system:ota:list')")
  @GetMapping("/list")
  public TableDataInfo<IoTOtaTask> list(IoTOtaTask query) {
    startPage();
    List<IoTOtaTask> list =
        otaAdminService.selectTaskList(
            query, loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId(), isAdmin());
    return getDataTable(list);
  }

  @PreAuthorize("@ss.hasPermi('system:ota:list')")
  @GetMapping("/{id}")
  public AjaxResult<IoTOtaTask> getInfo(@PathVariable("id") Long id) {
    return AjaxResult.success(otaAdminService.selectTaskById(id));
  }

  @PreAuthorize("@ss.hasPermi('system:ota:task:detail')")
  @GetMapping("/devices/{taskId}")
  public TableDataInfo<IoTOtaTaskDevice> taskDevices(@PathVariable("taskId") Long taskId, IoTOtaTaskDevice query) {
    query.setTaskId(taskId);
    startPage();
    List<IoTOtaTaskDevice> list = otaAdminService.selectTaskDeviceList(query);
    return getDataTable(list);
  }

  @PreAuthorize("@ss.hasPermi('system:ota:task:add')")
  @PostMapping
  @Log(title = "创建OTA任务", businessType = BusinessType.INSERT)
  public AjaxResult<Long> createTask(@RequestBody IoTOtaTask task) {
    return AjaxResult.success(
        "创建成功",
        otaAdminService.createTask(
            task, loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId()));
  }

  @PreAuthorize("@ss.hasPermi('system:ota:task:dispatch')")
  @PostMapping("/dispatch/{taskId}")
  @Log(title = "下发OTA任务", businessType = BusinessType.OTHER)
  public AjaxResult<String> dispatch(@PathVariable("taskId") Long taskId) {
    OtaServiceResult result = otaAdminService.dispatchTask(taskId);
    return result == OtaServiceResult.SUCCESS
        ? AjaxResult.success(result.getDescription(), result.name())
        : AjaxResult.error(result.getDescription(), result.name());
  }

  @PreAuthorize("@ss.hasPermi('system:ota:task:cancel')")
  @PostMapping("/cancel/{taskId}")
  @Log(title = "取消OTA任务", businessType = BusinessType.UPDATE)
  public AjaxResult<String> cancel(@PathVariable("taskId") Long taskId) {
    OtaServiceResult result = otaAdminService.cancelTask(taskId);
    return result == OtaServiceResult.SUCCESS
        ? AjaxResult.success(result.getDescription(), result.name())
        : AjaxResult.error(result.getDescription(), result.name());
  }

  @PreAuthorize("@ss.hasPermi('system:ota:task:retry')")
  @PostMapping("/retry/device/{taskDeviceId}")
  @Log(title = "重试OTA设备任务", businessType = BusinessType.UPDATE)
  public AjaxResult<String> retryDevice(@PathVariable("taskDeviceId") Long taskDeviceId) {
    OtaServiceResult result = otaAdminService.retryTaskDevice(taskDeviceId);
    return result == OtaServiceResult.SUCCESS
        ? AjaxResult.success(result.getDescription(), result.name())
        : AjaxResult.error(result.getDescription(), result.name());
  }

  @PreAuthorize("@ss.hasPermi('system:ota:list')")
  @GetMapping("/summary/product/{productKey}")
  public AjaxResult<Map<String, Object>> productSummary(@PathVariable("productKey") String productKey) {
    return AjaxResult.success(otaAdminService.getProductSummary(productKey));
  }

  @PreAuthorize("@ss.hasPermi('system:ota:list')")
  @GetMapping("/summary/device/{productKey}/{deviceId}")
  public AjaxResult<Map<String, Object>> deviceSummary(
      @PathVariable("productKey") String productKey, @PathVariable("deviceId") String deviceId) {
    return AjaxResult.success(otaAdminService.getDeviceSummary(productKey, deviceId));
  }

  @PreAuthorize("@ss.hasPermi('system:ota:list')")
  @GetMapping("/version/device/{productKey}/{deviceId}")
  public AjaxResult<IoTOtaDeviceVersion> latestDeviceVersion(
      @PathVariable("productKey") String productKey, @PathVariable("deviceId") String deviceId) {
    return AjaxResult.success(otaAdminService.getLatestDeviceVersion(productKey, deviceId));
  }
}
