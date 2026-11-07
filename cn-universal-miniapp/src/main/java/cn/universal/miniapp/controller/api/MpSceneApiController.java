/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 小程序场景联动API接口
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.miniapp.controller.api;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.exception.IoTException;
import cn.universal.miniapp.config.MpUserContext;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.SceneLinkage;
import cn.universal.persistence.entity.bo.SceneLinkageBO;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.rule.model.ExeRunContext;
import cn.universal.rule.scene.service.SceneLinkageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序场景联动API接口
 *
 * <p>复制自 SceneLinkageController，适配小程序端调用 路由前缀: /mp/api/scene/linkage
 */
@Slf4j
@RestController
@Tag(name = "小程序-场景联动", description = "小程序场景联动管理接口")
@RequestMapping("/mp/api/scene/linkage")
public class MpSceneApiController {

  @Resource private SceneLinkageService sceneLinkageService;

  /** 查询场景联动列表 */
  @Operation(summary = "查询场景联动列表")
  @GetMapping("/list")
  public TableDataInfo list(
      @RequestParam(required = false) String sceneName,
      @RequestParam(required = false) Integer status,
      @RequestParam(defaultValue = "1") int pageNum,
      @RequestParam(defaultValue = "20") int pageSize) {

    IoTUser user = MpUserContext.getCurrentUser();
    if (user == null) {
      throw new IoTException("用户未登录");
    }

    SceneLinkage query = new SceneLinkage();
    query.setSceneName(sceneName);
    query.setStatus(status);
    // 非管理员只能查看自己创建的场景
    query.setTenantId(currentTenantId(user));
    query.setCreateBy(user.isAdmin() ? null : user.getUnionId());

    // 手动分页
    com.github.pagehelper.PageHelper.startPage(pageNum, pageSize);
    List<SceneLinkage> list = sceneLinkageService.selectSceneLinkageList(query);

    TableDataInfo dataInfo = new TableDataInfo();
    dataInfo.setCode(0);
    dataInfo.setMsg("查询成功");
    dataInfo.setRows(list);
    dataInfo.setTotal((int) new com.github.pagehelper.PageInfo<>(list).getTotal());
    return dataInfo;
  }

  /** 获取场景联动详细信息 */
  @Operation(summary = "获取场景联动详情")
  @GetMapping("/{id}")
  public AjaxResult<SceneLinkage> getInfo(@PathVariable("id") Long id) {
    IoTUser user = MpUserContext.getCurrentUser();
    if (user == null) {
      throw new IoTException("用户未登录");
    }

    // 权限检查
    if (!sceneLinkageService.checkSelf(id, user.getUnionId(), currentTenantId(user))
        && !user.isAdmin()) {
      throw new IoTException("无权查看该场景");
    }

    return AjaxResult.success(sceneLinkageService.selectSceneLinkageById(id));
  }

  /** 新增场景联动 */
  @Operation(summary = "新增场景联动")
  @PostMapping
  public AjaxResult<Void> add(@RequestBody SceneLinkageBO sceneLinkage) throws Exception {
    IoTUser user = MpUserContext.getCurrentUser();
    if (user == null) {
      throw new IoTException("用户未登录");
    }

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
  @Operation(summary = "修改场景联动")
  @PutMapping
  public AjaxResult<Void> edit(@RequestBody SceneLinkageBO sceneLinkage) {
    IoTUser user = MpUserContext.getCurrentUser();
    if (user == null) {
      throw new IoTException("用户未登录");
    }

    // 权限检查
    if (!sceneLinkageService.checkSelf(
            sceneLinkage.getId(), user.getUnionId(), currentTenantId(user))
        && !user.isAdmin()) {
      throw new IoTException("无权修改该场景");
    }

    sceneLinkage.setTenantId(currentTenantId(user));
    sceneLinkage.setUpdateBy(user.getUnionId());
    sceneLinkage.setUpdateTime(new Date());

    return AjaxResult.toAjax(sceneLinkageService.updateSceneLinkage(sceneLinkage));
  }

  /** 删除场景联动 */
  @Operation(summary = "删除场景联动")
  @DeleteMapping("/{ids}")
  public AjaxResult<Void> remove(@PathVariable Long[] ids) {
    IoTUser user = MpUserContext.getCurrentUser();
    if (user == null) {
      throw new IoTException("用户未登录");
    }

    // 权限检查（检查第一个）
    if (ids.length > 0
        && !sceneLinkageService.checkSelf(ids[0], user.getUnionId(), currentTenantId(user))
        && !user.isAdmin()) {
      throw new IoTException("无权删除该场景");
    }

    return AjaxResult.toAjax(sceneLinkageService.deleteSceneLinkageByIds(ids));
  }

  /** 手动触发场景 */
  @Operation(summary = "手动触发场景")
  @GetMapping("/exec/{id}")
  public AjaxResult<Void> manualExec(@PathVariable("id") Long id) {
    IoTUser user = MpUserContext.getCurrentUser();
    if (user == null) {
      throw new IoTException("用户未登录");
    }

    // 权限检查
    if (!sceneLinkageService.checkSelf(id, user.getUnionId(), currentTenantId(user))
        && !user.isAdmin()) {
      throw new IoTException("无权执行该场景");
    }

    List<ExeRunContext> runContexts = sceneLinkageService.functionDown(id);
    log.info("小程序手动触发场景成功, sceneId={}, result={}", id, runContexts);

    return AjaxResult.success("触发成功");
  }

  /** 查询场景联动执行日志 */
  @Operation(summary = "查询场景执行日志")
  @GetMapping("/log/{sceneId}")
  public TableDataInfo logList(
      @PathVariable("sceneId") String sceneId,
      @RequestParam(defaultValue = "1") int pageNum,
      @RequestParam(defaultValue = "10") int pageSize) {

    IoTUser user = MpUserContext.getCurrentUser();
    if (user == null) {
      throw new IoTException("用户未登录");
    }

    // 权限检查
    try {
      Long id = Long.parseLong(sceneId);
      if (!sceneLinkageService.checkSelf(id, user.getUnionId(), currentTenantId(user))
          && !user.isAdmin()) {
        throw new IoTException("无权查看该场景日志");
      }
    } catch (NumberFormatException e) {
      // sceneId不是数字，跳过权限检查
    }

    var logs = sceneLinkageService.getSceneLinkageLogPage(sceneId, pageNum, pageSize);

    TableDataInfo dataInfo = new TableDataInfo();
    dataInfo.setCode(0);
    dataInfo.setMsg("查询成功");
    dataInfo.setRows(logs);
    dataInfo.setTotal(logs.size());
    return dataInfo;
  }

  private String currentTenantId(IoTUser user) {
    if (user == null || user.isAdmin()) {
      return null;
    }
    return StrUtil.blankToDefault(user.getTenantId(), user.getUnionId());
  }
}
