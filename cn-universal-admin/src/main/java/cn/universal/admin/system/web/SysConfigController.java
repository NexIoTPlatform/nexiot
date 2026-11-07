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

import cn.universal.admin.system.service.ISysConfigService;
import cn.universal.common.annotation.Log;
import cn.universal.common.constant.IoTUserConstants;
import cn.universal.common.enums.BusinessType;
import cn.universal.persistence.entity.admin.SysConfig;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 参数配置 信息操作处理 @Author ruoyi */
@RestController
@RequestMapping("/admin/system/config")
@Tag(name = "系统配置管理", description = "系统参数配置信息管理接口")
public class SysConfigController extends BaseController {

  @Autowired private ISysConfigService configService;

  /** 获取参数配置列表 */
  @Operation(summary = "获取参数配置列表", description = "分页查询系统参数配置列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取参数配置列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo list(SysConfig config) {
    startPage();
    List<SysConfig> list = configService.selectConfigList(config);
    return getDataTable(list);
  }

  //    @PostMapping("/export")
  //    public void export(HttpServletResponse response, SysConfig config)
  //    {
  //        List<SysConfig> list = configService.selectConfigList(config);
  //        ExcelUtil<SysConfig> util = new ExcelUtil<SysConfig>(SysConfig.class);
  //        util.exportExcel(response, list, "参数数据");
  //    }

  /** 根据参数编号获取详细信息 */
  @Operation(summary = "根据参数编号获取详细信息", description = "根据参数配置ID获取详细信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取参数配置详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "configId", description = "参数配置ID", required = true)
  @GetMapping(value = "/{configId}")
  public AjaxResult getInfo(@PathVariable Long configId) {
    return AjaxResult.success(configService.selectConfigById(configId));
  }

  /** 根据参数键名查询参数值 */
  @Operation(summary = "根据参数键名查询参数值", description = "根据参数键名查询参数值")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取参数值",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "configKey", description = "参数键名", required = true)
  @GetMapping(value = "/configKey/{configKey}")
  public AjaxResult getConfigKey(@PathVariable String configKey) {
    return AjaxResult.success(configService.selectConfigByKey(configKey));
  }

  /** 新增参数配置 */
  @Operation(summary = "新增参数配置", description = "新增系统参数配置信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增参数配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping
  @Log(title = "新增参数配置", businessType = BusinessType.INSERT)
  public AjaxResult add(@Validated @RequestBody SysConfig config) {
    if (IoTUserConstants.NOT_UNIQUE.equals(configService.checkConfigKeyUnique(config))) {
      return AjaxResult.error("新增参数'" + config.getConfigName() + "'失败，参数键名已存在");
    }
    config.setCreateBy(SecurityUtils.getUnionId());
    return toAjax(configService.insertConfig(config));
  }

  /** 修改参数配置 */
  @Operation(summary = "修改参数配置", description = "修改系统参数配置信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改参数配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping
  @Log(title = "修改参数配置", businessType = BusinessType.UPDATE)
  public AjaxResult edit(@Validated @RequestBody SysConfig config) {
    if (IoTUserConstants.NOT_UNIQUE.equals(configService.checkConfigKeyUnique(config))) {
      return AjaxResult.error("修改参数'" + config.getConfigName() + "'失败，参数键名已存在");
    }
    config.setUpdateBy(SecurityUtils.getUnionId());
    return toAjax(configService.updateConfig(config));
  }

  /** 删除参数配置 */
  @Operation(summary = "删除参数配置", description = "根据参数配置ID数组删除参数配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除参数配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "configIds", description = "参数配置ID数组", required = true)
  @DeleteMapping("/{configIds}")
  @Log(title = "删除参数配置", businessType = BusinessType.DELETE)
  public AjaxResult remove(@PathVariable Long[] configIds) {
    configService.deleteConfigByIds(configIds);
    return success();
  }

  /** 刷新参数缓存 */
  @Operation(summary = "刷新参数缓存", description = "刷新系统参数配置缓存")
  @ApiResponse(
      responseCode = "200",
      description = "成功刷新参数缓存",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @DeleteMapping("/refreshCache")
  @Log(title = "刷新缓存", businessType = BusinessType.OTHER)
  public AjaxResult refreshCache() {
    configService.resetConfigCache();
    return AjaxResult.success();
  }

  /** 批量根据参数键名查询参数值(用于前端配置加载) */
  @Operation(summary = "批量查询参数值", description = "根据参数键名数组批量查询参数值")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取参数值",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "configKeys", description = "参数键名数组", required = true)
  @PostMapping(value = "/getConfigsByKeys")
  public AjaxResult getConfigsByKeys(@RequestBody List<String> configKeys) {
    Map<String, String> configMap = new HashMap<>();
    for (String configKey : configKeys) {
      String configValue = configService.selectConfigByKey(configKey);
      if (configValue != null) {
        configMap.put(configKey, configValue);
      }
    }
    return AjaxResult.success(configMap);
  }

  /**
   * 批量根据参数键名查询参数值(公开接口,无需认证)
   * 用于前端启动时加载系统配置
   */
  @Operation(summary = "公开批量查询参数值", description = "无需认证,用于前端启动时加载配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取参数值",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "configKeys", description = "参数键名数组", required = true)
  @PostMapping(value = "/public/getConfigsByKeys")
  public AjaxResult getPublicConfigsByKeys(@RequestBody List<String> configKeys) {
    // 只允许查询前端相关的配置项
    List<String> allowedPrefixes = Arrays.asList(
      "sys.site.",
      "sys.layout.",
      "sys.watermark.",
      "sys.login."  // 登录页配置
    );
    
    Map<String, String> configMap = new HashMap<>();
    for (String configKey : configKeys) {
      // 检查键名是否在允许的前缀列表中
      boolean allowed = allowedPrefixes.stream().anyMatch(configKey::startsWith);
      if (allowed) {
        String configValue = configService.selectConfigByKey(configKey);
        if (configValue != null) {
          configMap.put(configKey, configValue);
        }
      }
    }
    return AjaxResult.success(configMap);
  }
}
