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

package cn.universal.admin.system.web;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.admin.common.utils.ExcelUtil;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTDeviceRuleLog;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.SceneLinkage;
import cn.universal.persistence.entity.bo.SceneLinkageBO;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.rule.model.ExeRunContext;
import cn.universal.rule.scene.service.SceneLinkageService;
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
import java.util.Date;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 场景联动管理控制器
 *
 * @since 2023-03-01
 */
@RestController
@Tag(name = "场景联动管理", description = "场景联动管理")
@RequestMapping("/admin/v1/scene/linkage")
public class SceneLinkageController extends BaseController {

  @Resource private SceneLinkageService sceneLinkageService;

  /** 查询场景联动列表 */
  @Operation(summary = "查询场景联动列表", description = "分页查询场景联动列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取场景联动列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo list(SceneLinkage sceneLinkage) {
    IoTUser user = loginIoTUnionUser(SecurityUtils.getUnionId());
    startPage();
    sceneLinkage.setTenantId(currentTenantId(user));
    sceneLinkage.setCreateBy(user.isAdmin() ? null : user.getUnionId());
    List<SceneLinkage> list = sceneLinkageService.selectSceneLinkageList(sceneLinkage);
    return getDataTable(list);
  }

  /** 导出场景联动列表 */
  @Operation(summary = "导出场景联动列表", description = "导出场景联动列表数据到Excel文件")
  @ApiResponse(responseCode = "200", description = "成功导出场景联动列表")
  @PostMapping("/export")
  @Log(title = "导出场景联动列表", businessType = BusinessType.EXPORT)
  public void export(HttpServletResponse response, SceneLinkage sceneLinkage) {
    IoTUser user = loginIoTUnionUser(SecurityUtils.getUnionId());
    sceneLinkage.setTenantId(currentTenantId(user));
    sceneLinkage.setCreateBy(user.isAdmin() ? null : user.getUnionId());
    List<SceneLinkage> list = sceneLinkageService.selectSceneLinkageList(sceneLinkage);
    ExcelUtil<SceneLinkage> util = new ExcelUtil<SceneLinkage>(SceneLinkage.class);
    util.exportExcel(response, list, "场景联动数据");
  }

  /** 获取场景联动详细信息 */
  @Operation(summary = "获取场景联动详细信息", description = "根据场景联动ID获取详细信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取场景联动详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "场景联动ID", required = true)
  @GetMapping(value = "/{id}")
  public AjaxResult<SceneLinkage> getInfo(@PathVariable("id") Long id) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    if (!sceneLinkageService.checkSelf(id, iotUser.getUnionId(), currentTenantId(iotUser))
        && !iotUser.isAdmin()) {
      throw new IoTException("场景没有操作权限！");
    }
    return AjaxResult.success(sceneLinkageService.selectSceneLinkageById(id));
  }

  /** 新增场景联动 */
  @Operation(summary = "新增场景联动", description = "新增场景联动信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增场景联动",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping
  @Log(title = "新增场景联动", businessType = BusinessType.INSERT)
  public AjaxResult<Void> add(@RequestBody SceneLinkageBO sceneLinkage) throws Exception {
    IoTUser user = loginIoTUnionUser(SecurityUtils.getUnionId());
    String unionId = user.getUnionId();
    sceneLinkage.setTenantId(currentTenantId(user));
    sceneLinkage.setCreateBy(unionId);
    sceneLinkage.setCreateTime(new Date());
    sceneLinkage.setUpdateBy(unionId);
    sceneLinkage.setUpdateTime(new Date());
    int rows = sceneLinkageService.insertSceneLinkage(sceneLinkage);
    return AjaxResult.toAjax(rows);
  }

  /** 修改场景联动 */
  @Operation(summary = "修改场景联动", description = "修改场景联动信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改场景联动",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping
  @Log(title = "修改场景联动", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> edit(@RequestBody SceneLinkageBO sceneLinkage) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    if (!sceneLinkageService.checkSelf(
            sceneLinkage.getId(), iotUser.getUnionId(), currentTenantId(iotUser))
        && !iotUser.isAdmin()) {
      throw new IoTException("场景没有操作权限！");
    }
    sceneLinkage.setTenantId(currentTenantId(iotUser));
    sceneLinkage.setUpdateBy(SecurityUtils.getUnionId());
    sceneLinkage.setUpdateTime(new Date());
    return AjaxResult.toAjax(sceneLinkageService.updateSceneLinkage(sceneLinkage));
  }

  /** 删除场景联动 */
  @Operation(summary = "删除场景联动", description = "根据场景联动ID数组删除场景联动")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除场景联动",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "ids", description = "场景联动ID数组", required = true)
  @DeleteMapping("/{ids}")
  @Log(title = "删除场景联动", businessType = BusinessType.DELETE)
  public AjaxResult<Void> remove(@PathVariable Long[] ids) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    if (!iotUser.isAdmin()) {
      String tenantId = currentTenantId(iotUser);
      for (Long id : ids) {
        if (!sceneLinkageService.checkSelf(id, iotUser.getUnionId(), tenantId)) {
          throw new IoTException("场景没有操作权限！");
        }
      }
    }
    return AjaxResult.toAjax(sceneLinkageService.deleteSceneLinkageByIds(ids));
  }

  /** 手动触发场景 */
  @Operation(summary = "手动触发场景", description = "根据场景联动ID手动触发场景执行")
  @ApiResponse(
      responseCode = "200",
      description = "成功触发场景",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "场景联动ID", required = true)
  @GetMapping(value = "/exec/{id}")
  @Log(title = "手动触发场景", businessType = BusinessType.OTHER)
  public AjaxResult<Void> manualExec(@PathVariable("id") Long id) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    if (!sceneLinkageService.checkSelf(id, iotUser.getUnionId(), currentTenantId(iotUser))
        && !iotUser.isAdmin()) {
      throw new IoTException("场景没有操作权限！");
    }
    List<ExeRunContext> runContexts = sceneLinkageService.functionDown(id);
    logger.info("手动触发执行成功={}", runContexts);
    return AjaxResult.success("触发成功");
  }

  /** 查询Id场景联动执行日志（分页） */
  @Operation(summary = "查询场景联动执行日志", description = "根据场景ID查询场景联动执行日志（分页）")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取场景联动执行日志",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @Parameter(name = "sceneId", description = "场景ID", required = true)
  @Parameter(name = "pageNum", description = "页码", required = false)
  @Parameter(name = "pageSize", description = "每页数量", required = false)
  @GetMapping("/log/{sceneId}")
  public TableDataInfo logList(
      @PathVariable("sceneId") String sceneId,
      @RequestParam(defaultValue = "1") int pageNum,
      @RequestParam(defaultValue = "10") int pageSize) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    if (!iotUser.isAdmin()
        && !sceneLinkageService.checkSelf(
            Long.valueOf(sceneId), iotUser.getUnionId(), currentTenantId(iotUser))) {
      throw new IoTException("场景没有操作权限！");
    }
    return getDataTable(sceneLinkageService.getSceneLinkageLogPage(sceneId, pageNum, pageSize));
  }

  /** 查询场景联动执行日志（分页） */
  @Operation(summary = "查询场景联动执行日志列表", description = "分页查询场景联动执行日志列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取场景联动执行日志列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @Parameter(name = "cName", description = "场景名称", required = false)
  @Parameter(name = "cStatus", description = "场景状态", required = false)
  @Parameter(name = "cId", description = "场景ID", required = false)
  @Parameter(name = "cType", description = "场景类型", required = false)
  @Parameter(name = "triggerType", description = "触发类型", required = false)
  @Parameter(name = "createTimeStart", description = "创建时间开始", required = false)
  @Parameter(name = "createTimeEnd", description = "创建时间结束", required = false)
  @Parameter(name = "pageNum", description = "页码", required = false)
  @Parameter(name = "pageSize", description = "每页数量", required = false)
  @GetMapping("/log/page")
  public TableDataInfo getRuleLogPage(
      @RequestParam(required = false) String cName,
      @RequestParam(required = false) Byte cStatus,
      @RequestParam(required = false) String cId,
      @RequestParam(required = false) Byte cType,
      @RequestParam(required = false) String triggerType,
      @RequestParam(required = false) String createTimeStart,
      @RequestParam(required = false) String createTimeEnd,
      @RequestParam(defaultValue = "1") int pageNum,
      @RequestParam(defaultValue = "10") int pageSize) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    IoTDeviceRuleLog ioTDeviceRuleLog = new IoTDeviceRuleLog();
    if (iotUser.isAdmin()) {
      ioTDeviceRuleLog.setCreateBy(null);
    } else {
      ioTDeviceRuleLog.setCreateBy(iotUser.getUnionId());
    }
    ioTDeviceRuleLog.setCName(cName);
    ioTDeviceRuleLog.setCStatus(cStatus);
    ioTDeviceRuleLog.setCId(cId);
    ioTDeviceRuleLog.setCType(cType);
    // 处理时间范围
    if (StrUtil.isNotBlank(createTimeStart)) {
      try {
        ioTDeviceRuleLog.setCreateTime(DateUtil.parse(createTimeStart));
      } catch (Exception e) {
        // 忽略时间解析错误
      }
    }
    if (StrUtil.isNotBlank(createTimeEnd)) {
      try {
        ioTDeviceRuleLog.setUpdateTime(DateUtil.parse(createTimeEnd));
      } catch (Exception e) {
        // 忽略时间解析错误
      }
    }
    startPage();
    return getDataTable(
        sceneLinkageService.getRuleLogPageWithSceneInfo(ioTDeviceRuleLog, triggerType));
  }

  private String currentTenantId(IoTUser user) {
    if (user == null || user.isAdmin()) {
      return null;
    }
    return StrUtil.blankToDefault(SecurityUtils.getTenantIdQuietly(), resolveTenantId(user));
  }
}
