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

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.common.utils.ExcelUtil;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.quartz.constant.Constants;
import cn.universal.quartz.domain.SysJob;
import cn.universal.quartz.service.ISysJobService;
import cn.universal.quartz.util.CronUtils;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 调度任务信息操作处理 @Author ruoyi */
@RestController
@Tag(name = "调度任务管理", description = "调度任务管理接口")
@RequestMapping("/admin/quartz/job")
public class SysJobController extends BaseController {

  @Autowired private ISysJobService jobService;

  /** 查询定时任务列表 */
  @Operation(summary = "查询定时任务列表", description = "根据查询条件获取定时任务列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取定时任务列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo list(SysJob sysJob) {
    startPage();
    return getDataTable(jobService.selectJobList(sysJob));
  }

  /** 导出定时任务列表 */
  @Operation(summary = "导出定时任务列表", description = "导出定时任务列表数据到Excel文件")
  @ApiResponse(
      responseCode = "200",
      description = "成功导出定时任务列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/export")
  public AjaxResult export(SysJob sysJob) {
    List<SysJob> list = jobService.selectJobList(sysJob);
    ExcelUtil<SysJob> util = new ExcelUtil<SysJob>(SysJob.class);
    return util.exportExcel(list, "定时任务");
  }

  /** 获取定时任务详细信息 */
  @Operation(summary = "获取定时任务详细信息", description = "根据任务ID获取定时任务详细信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取定时任务详细信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "jobId", description = "任务ID", required = true)
  @GetMapping(value = "/{jobId}")
  public AjaxResult getInfo(@PathVariable("jobId") Long jobId) {
    return AjaxResult.success(jobService.selectJobById(jobId));
  }

  /** 新增定时任务 */
  @Operation(summary = "新增定时任务", description = "新增定时任务信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增定时任务",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping
  public AjaxResult add(@RequestBody SysJob sysJob) throws SchedulerException {
    if (!CronUtils.isValid(sysJob.getCronExpression())) {
      return AjaxResult.error("新增任务'" + sysJob.getJobName() + "'失败，Cron表达式不正确");
    } else if (StrUtil.containsIgnoreCase(sysJob.getInvokeTarget(), Constants.LOOKUP_RMI)) {
      return AjaxResult.error("新增任务'" + sysJob.getJobName() + "'失败，目标字符串不允许'rmi://'调用");
    }
    sysJob.setCreateBy(loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId());
    return toAjax(jobService.insertJob(sysJob));
  }

  /** 修改定时任务 */
  @Operation(summary = "修改定时任务", description = "修改定时任务信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改定时任务",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping
  public AjaxResult edit(@RequestBody SysJob sysJob) throws SchedulerException {
    if (!CronUtils.isValid(sysJob.getCronExpression())) {
      return AjaxResult.error("修改任务'" + sysJob.getJobName() + "'失败，Cron表达式不正确");
    } else if (StrUtil.containsIgnoreCase(sysJob.getInvokeTarget(), Constants.LOOKUP_RMI)) {
      return AjaxResult.error("修改任务'" + sysJob.getJobName() + "'失败，目标字符串不允许'rmi://'调用");
    }
    sysJob.setUpdateBy(loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId());
    return toAjax(jobService.updateJob(sysJob));
  }

  /** 定时任务状态修改 */
  @Operation(summary = "定时任务状态修改", description = "修改定时任务的状态")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改定时任务状态",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping("/changeStatus")
  public AjaxResult changeStatus(@RequestBody SysJob job) throws SchedulerException {
    SysJob newJob = jobService.selectJobById(job.getJobId());
    newJob.setStatus(job.getStatus());
    return toAjax(jobService.changeStatus(newJob));
  }

  /** 定时任务立即执行一次 */
  @Operation(summary = "定时任务立即执行一次", description = "立即执行指定的定时任务")
  @ApiResponse(
      responseCode = "200",
      description = "成功执行定时任务",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping("/run")
  public AjaxResult run(@RequestBody SysJob job) throws SchedulerException {
    jobService.run(job);
    return AjaxResult.success();
  }

  /** 删除定时任务 */
  @Operation(summary = "删除定时任务", description = "根据任务ID数组删除定时任务")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除定时任务",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "jobIds", description = "任务ID数组", required = true)
  @DeleteMapping("/{jobIds}")
  public AjaxResult remove(@PathVariable Long[] jobIds) throws SchedulerException {
    jobService.deleteJobByIds(jobIds);
    return AjaxResult.success();
  }
}
