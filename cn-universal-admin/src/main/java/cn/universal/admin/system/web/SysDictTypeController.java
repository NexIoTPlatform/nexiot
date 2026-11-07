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

import cn.universal.admin.system.service.ISysDictTypeService;
import cn.universal.common.constant.IoTUserConstants;
import cn.universal.persistence.entity.admin.SysDictType;
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
import java.util.Date;
import java.util.List;
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

/** 数据字典信息 @Author ruoyi */
@RestController
@RequestMapping("/admin/system/dict/type")
@Tag(name = "字典类型管理", description = "系统字典类型信息管理接口")
public class SysDictTypeController extends BaseController {

  @Autowired private ISysDictTypeService dictTypeService;

  @Operation(summary = "查询字典类型列表", description = "分页查询字典类型列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取字典类型列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo list(SysDictType dictType) {
    startPage();
    return getDataTable(dictTypeService.selectPageDictTypeList(dictType));
  }

  //  @GetMapping("/export")
  //  public AjaxResult export(SysDictType dictType) {
  //    List<SysDictType> list = dictTypeService.selectDictTypeList(dictType);
  //    ExcelUtil<SysDictType> util = new ExcelUtil<SysDictType>(SysDictType.class);
  //    return util.exportExcel(list, "字典类型");
  //  }

  /** 查询字典类型详细 */
  @Operation(summary = "查询字典类型详细", description = "根据字典ID获取字典类型详细信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取字典类型详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "dictId", description = "字典ID", required = true)
  @GetMapping(value = "/{dictId}")
  public AjaxResult getInfo(@PathVariable Long dictId) {
    return AjaxResult.success(dictTypeService.selectDictTypeById(dictId));
  }

  /** 新增字典类型 */
  @Operation(summary = "新增字典类型", description = "新增字典类型信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增字典类型",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping
  public AjaxResult add(@Validated @RequestBody SysDictType dict) {
    if (IoTUserConstants.NOT_UNIQUE.equals(dictTypeService.checkDictTypeUnique(dict))) {
      return error("新增字典'" + dict.getDictName() + "'失败，字典类型已存在");
    }
    dict.setCreateBy(SecurityUtils.getUnionId());
    dict.setCreateTime(new Date());
    return toAjax(dictTypeService.insertDictType(dict));
  }

  /** 修改字典类型 */
  @Operation(summary = "修改字典类型", description = "修改字典类型信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改字典类型",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping
  public AjaxResult edit(@Validated @RequestBody SysDictType dict) {
    if (IoTUserConstants.NOT_UNIQUE.equals(dictTypeService.checkDictTypeUnique(dict))) {
      return error("修改字典'" + dict.getDictName() + "'失败，字典类型已存在");
    }
    dict.setUpdateBy(SecurityUtils.getUnionId());
    dict.setUpdateTime(new Date());
    return toAjax(dictTypeService.updateDictType(dict));
  }

  /** 删除字典类型 */
  @Operation(summary = "删除字典类型", description = "根据字典ID数组删除字典类型")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除字典类型",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "dictIds", description = "字典ID数组", required = true)
  @DeleteMapping("/{dictIds}")
  public AjaxResult remove(@PathVariable Long[] dictIds) {
    dictTypeService.deleteDictTypeByIds(dictIds);
    return success();
  }

  /** 刷新字典缓存 */
  @Operation(summary = "刷新字典缓存", description = "刷新字典类型缓存")
  @ApiResponse(
      responseCode = "200",
      description = "成功刷新字典缓存",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @DeleteMapping("/refreshCache")
  public AjaxResult refreshCache() {
    dictTypeService.resetDictCache();
    return success();
  }

  /** 获取字典选择框列表 */
  @Operation(summary = "获取字典选择框列表", description = "获取所有字典类型选择框列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取字典选择框列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/optionselect")
  public AjaxResult optionselect() {
    List<SysDictType> dictTypes = dictTypeService.selectDictTypeAll();
    return AjaxResult.success(dictTypes);
  }
}
