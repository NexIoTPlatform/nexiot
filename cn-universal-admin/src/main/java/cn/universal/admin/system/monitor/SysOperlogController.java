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

package cn.universal.admin.system.monitor;

import cn.universal.admin.common.utils.ExcelUtil;
import cn.universal.admin.system.service.ISysOperLogService;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.SysOperLog;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 操作日志记录 @Author ruoyi */
@RestController
@Tag(name = "系统操作日志管理", description = "系统操作日志管理接口")
@RequestMapping("/admin/monitor/operlog")
public class SysOperlogController extends BaseController {

  @Autowired private ISysOperLogService operLogService;

  @Operation(summary = "查询操作日志列表", description = "根据查询条件获取操作日志列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取操作日志列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo list(SysOperLog operLog) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    if (!iotUser.isAdmin()) {
      operLog.setOperName(iotUser.getUnionId());
    }
    startPage();
    List<SysOperLog> sysOperLogs = operLogService.selectPageOperLogList(operLog);
    return getDataTable(sysOperLogs);
  }

  @Operation(summary = "导出操作日志", description = "导出操作日志数据到Excel文件")
  @ApiResponse(
      responseCode = "200",
      description = "成功导出操作日志",
      content =
          @Content(
              mediaType = "application/octet-stream",
              schema = @Schema(implementation = Void.class)))
  @Log(title = "操作日志", businessType = BusinessType.EXPORT)
  @PostMapping("/export")
  public void export(HttpServletResponse response, SysOperLog operLog) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    if (!iotUser.isAdmin()) {
      operLog.setOperName(iotUser.getUnionId());
    }
    List<SysOperLog> list = operLogService.selectOperLogList(operLog);
    ExcelUtil<SysOperLog> util = new ExcelUtil<SysOperLog>(SysOperLog.class);
    util.exportExcel(response, list, "操作日志");
  }
}
