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

package cn.universal.admin.system.quartz;

import cn.universal.admin.common.utils.ExcelUtil;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.quartz.domain.SysJobLog;
import cn.universal.quartz.service.ISysJobLogService;
import cn.universal.security.BaseController;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 调度日志操作处理 @Author ruoyi */
@RestController
@RequestMapping("/admin/quartz/jobLog")
public class SysJobLogController extends BaseController {

  @Autowired private ISysJobLogService jobLogService;

  /** 查询定时任务调度日志列表 */
  @GetMapping("/list")
  public TableDataInfo list(SysJobLog sysJobLog) {
    startPage();
    return getDataTable(jobLogService.selectJobLogList(sysJobLog));
  }

  /** 导出定时任务调度日志列表 */
  @GetMapping("/export")
  public AjaxResult export(SysJobLog sysJobLog) {
    List<SysJobLog> list = jobLogService.selectJobLogList(sysJobLog);
    ExcelUtil<SysJobLog> util = new ExcelUtil<SysJobLog>(SysJobLog.class);
    return util.exportExcel(list, "调度日志");
  }

  /** 根据调度编号获取详细信息 */
  @GetMapping(value = "/{configId}")
  public AjaxResult getInfo(@PathVariable Long jobLogId) {
    return AjaxResult.success(jobLogService.selectJobLogById(jobLogId));
  }

  /** 删除定时任务调度日志 */
  @DeleteMapping("/{jobLogId}")
  public AjaxResult remove(@PathVariable Long jobLogId) {
    return toAjax(jobLogService.deleteJobLogById(jobLogId));
  }

  /** 清空定时任务调度日志 */
  @DeleteMapping("/clean")
  public AjaxResult clean() {
    jobLogService.cleanJobLog();
    return AjaxResult.success();
  }
}
