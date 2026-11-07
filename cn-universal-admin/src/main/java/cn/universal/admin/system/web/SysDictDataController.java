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

import cn.hutool.core.lang.Validator;
import cn.universal.admin.system.service.ISysDictDataService;
import cn.universal.admin.system.service.ISysDictTypeService;
import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.entity.admin.vo.SysDictDataVo;
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
import java.util.ArrayList;
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
@RequestMapping("/admin/system/dict/data")
@Tag(name = "数据字典管理", description = "系统数据字典信息管理接口")
public class SysDictDataController extends BaseController {

  @Autowired private ISysDictDataService dictDataService;

  @Autowired private ISysDictTypeService dictTypeService;

  @Operation(summary = "查询字典数据列表", description = "分页查询数据字典列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取字典数据列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo list(SysDictDataVo dictData) {
    startPage();
    return getDataTable(dictDataService.selectPageDictDataList(dictData));
  }

  //  @GetMapping("/export")
  //  public AjaxResult export(SysDictData dictData) {
  //    List<SysDictData> list = dictDataService.selectDictDataList(dictData);
  //    ExcelUtil<SysDictData> util = new ExcelUtil<SysDictData>(SysDictData.class);
  //    return util.exportExcel(list, "字典数据");
  //  }

  /** 查询字典数据详细 */
  @Operation(summary = "查询字典数据详细", description = "根据字典编码获取字典数据详细信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取字典数据详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "dictCode", description = "字典编码", required = true)
  @GetMapping(value = "/{dictCode}")
  public AjaxResult getInfo(@PathVariable Long dictCode) {
    return AjaxResult.success(dictDataService.selectDictDataById(dictCode));
  }

  /** 根据字典类型查询字典数据信息 */
  @Operation(summary = "根据字典类型查询字典数据信息", description = "根据字典类型获取字典数据信息列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取字典数据列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "dictType", description = "字典类型", required = true)
  @GetMapping(value = "/type/{dictType}")
  public AjaxResult dictType(@PathVariable String dictType) {
    List<SysDictData> data = dictTypeService.selectDictDataByType(dictType);
    if (Validator.isNull(data)) {
      data = new ArrayList<SysDictData>();
    }
    return AjaxResult.success(data);
  }

  /** 根据字典类型数组查询字典数据信息 */
  @Operation(summary = "根据字典类型数组查询字典数据信息", description = "根据字典类型数组获取字典数据信息列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取字典数据列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "dictType", description = "字典类型数组", required = true)
  @GetMapping(value = "/types/{dictType}")
  public AjaxResult dictType(@PathVariable String[] dictType) {
    return AjaxResult.success(dictDataService.selectDictDataByTypes(dictType));
  }

  /** 新增字典类型 */
  @Operation(summary = "新增字典类型", description = "新增数据字典类型信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增字典类型",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping
  public AjaxResult add(@Validated @RequestBody SysDictData dict) {
    dict.setCreateBy(SecurityUtils.getUnionId());
    dict.setCreateTime(new Date());
    return toAjax(dictDataService.insertDictData(dict));
  }

  /** 修改保存字典类型 */
  @Operation(summary = "修改保存字典类型", description = "修改保存数据字典类型信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改字典类型",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping
  public AjaxResult edit(@Validated @RequestBody SysDictData dict) {
    dict.setUpdateBy(SecurityUtils.getUnionId());
    dict.setUpdateTime(new Date());
    return toAjax(dictDataService.updateDictData(dict));
  }

  /** 删除字典类型 */
  @Operation(summary = "删除字典类型", description = "根据字典编码数组删除字典类型")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除字典类型",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "dictCodes", description = "字典编码数组", required = true)
  @DeleteMapping("/{dictCodes}")
  public AjaxResult remove(@PathVariable Long[] dictCodes) {
    dictDataService.deleteDictDataByIds(dictCodes);
    return success();
  }
}
