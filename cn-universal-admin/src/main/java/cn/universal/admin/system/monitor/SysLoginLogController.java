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
import cn.universal.admin.system.service.ISysLogininforService;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.SysLogininfor;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 系统访问记录 @Author ruoyi */
@RestController
@Tag(name = "系统登录日志管理", description = "系统登录日志管理接口")
@RequestMapping("/admin/monitor/logininfor")
public class SysLoginLogController extends BaseController {

  @Resource private ISysLogininforService logininforService;

  @Operation(summary = "查询登录日志列表", description = "根据查询条件获取登录日志列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取登录日志列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo list(SysLogininfor logininfor) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    if (!iotUser.isAdmin()) {
      logininfor.setUserName(iotUser.getUnionId());
    }
    startPage();
    List<SysLogininfor> sysLogininfors = logininforService.selectPageLogininforList(logininfor);
    return getDataTable(sysLogininfors);
  }

  @Operation(summary = "导出登录日志", description = "导出登录日志数据到Excel文件")
  @ApiResponse(
      responseCode = "200",
      description = "成功导出登录日志",
      content =
          @Content(
              mediaType = "application/octet-stream",
              schema = @Schema(implementation = Void.class)))
  @Log(title = "登录日志", businessType = BusinessType.EXPORT)
  @PostMapping("/export")
  public void export(HttpServletResponse response, SysLogininfor logininfor) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    if (!iotUser.isAdmin()) {
      logininfor.setUserName(iotUser.getUnionId());
    }
    List<SysLogininfor> list = logininforService.selectLogininforList(logininfor);
    ExcelUtil<SysLogininfor> util = new ExcelUtil<SysLogininfor>(SysLogininfor.class);
    util.exportExcel(response, list, "登录日志");
  }

  @Operation(summary = "删除登录日志", description = "根据日志ID数组删除登录日志")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除登录日志",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "infoIds", description = "日志ID数组", required = true)
  @Log(title = "登录日志", businessType = BusinessType.DELETE)
  @DeleteMapping("/{infoIds}")
  public AjaxResult remove(@PathVariable Long[] infoIds) {
    return toAjax(logininforService.deleteLogininforByIds(infoIds));
  }

  @Operation(summary = "清空登录日志", description = "清空所有登录日志")
  @ApiResponse(
      responseCode = "200",
      description = "成功清空登录日志",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Log(title = "登录日志", businessType = BusinessType.CLEAN)
  @DeleteMapping("/clean")
  public AjaxResult clean() {
    logininforService.cleanLogininfor();
    return AjaxResult.success();
  }
}
