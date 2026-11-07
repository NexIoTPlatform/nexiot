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

package cn.universal.admin.generator.web;

import cn.hutool.core.convert.Convert;
import cn.universal.admin.generator.domain.GenTable;
import cn.universal.admin.generator.domain.GenTableColumn;
import cn.universal.admin.generator.service.IGenTableColumnService;
import cn.universal.admin.generator.service.IGenTableService;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
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

/** 代码生成 操作处理 @Author ruoyi */
@RestController
@Tag(name = "代码生成管理", description = "代码生成管理接口")
@RequestMapping("/admin/tool/gen")
public class GenController extends BaseController {

  @Autowired private IGenTableService genTableService;

  @Autowired private IGenTableColumnService genTableColumnService;

  @Operation(summary = "查询代码生成列表", description = "根据查询条件获取代码生成列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取代码生成列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo genList(GenTable genTable) {
    startPage();
    List<GenTable> list = genTableService.selectGenTableList(genTable);
    return getDataTable(list);
  }

  /** 修改代码生成业务 */
  @Operation(summary = "修改代码生成业务", description = "根据表ID获取代码生成业务信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取代码生成业务信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "talbleId", description = "表ID", required = true)
  @GetMapping(value = "/{talbleId}")
  public AjaxResult getInfo(@PathVariable Long talbleId) {
    GenTable table = genTableService.selectGenTableById(talbleId);
    List<GenTable> tables = genTableService.selectGenTableAll();
    List<GenTableColumn> list = genTableColumnService.selectGenTableColumnListByTableId(talbleId);
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("info", table);
    map.put("rows", list);
    map.put("tables", tables);
    return AjaxResult.success(map);
  }

  /** 查询数据库列表 */
  @Operation(summary = "查询数据库列表", description = "根据查询条件获取数据库列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取数据库列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/db/list")
  public TableDataInfo dataList(GenTable genTable) {
    startPage();
    List<GenTable> list = genTableService.selectDbTableList(genTable);
    return getDataTable(list);
  }

  /** 查询数据表字段列表 */
  @Operation(summary = "查询数据表字段列表", description = "根据表ID获取数据表字段列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取数据表字段列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @Parameter(name = "tableId", description = "表ID", required = true)
  @GetMapping(value = "/column/{talbleId}")
  public TableDataInfo columnList(Long tableId) {
    TableDataInfo dataInfo = new TableDataInfo();
    List<GenTableColumn> list = genTableColumnService.selectGenTableColumnListByTableId(tableId);
    dataInfo.setRows(list);
    dataInfo.setTotal(list.size());
    return dataInfo;
  }

  /** 导入表结构（保存） */
  @Operation(summary = "导入表结构", description = "导入表结构并保存")
  @ApiResponse(
      responseCode = "200",
      description = "成功导入表结构",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "tables", description = "表名列表", required = true)
  @PostMapping("/importTable")
  public AjaxResult importTableSave(String tables) {
    String[] tableNames = Convert.toStrArray(tables);
    // 查询表信息
    List<GenTable> tableList = genTableService.selectDbTableListByNames(tableNames);
    genTableService.importGenTable(tableList);
    return AjaxResult.success();
  }

  /** 修改保存代码生成业务 */
  @Operation(summary = "修改保存代码生成业务", description = "修改并保存代码生成业务信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改保存代码生成业务",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping
  public AjaxResult editSave(@Validated @RequestBody GenTable genTable) {
    genTableService.validateEdit(genTable);
    genTableService.updateGenTable(genTable);
    return AjaxResult.success();
  }

  /** 删除代码生成 */
  @Operation(summary = "删除代码生成", description = "根据表ID数组删除代码生成")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除代码生成",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "tableIds", description = "表ID数组", required = true)
  @DeleteMapping("/{tableIds}")
  public AjaxResult remove(@PathVariable Long[] tableIds) {
    genTableService.deleteGenTableByIds(tableIds);
    return AjaxResult.success();
  }

  /** 预览代码 */
  @Operation(summary = "预览代码", description = "根据表ID预览生成的代码")
  @ApiResponse(
      responseCode = "200",
      description = "成功预览代码",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "tableId", description = "表ID", required = true)
  @GetMapping("/preview/{tableId}")
  public AjaxResult preview(@PathVariable("tableId") Long tableId) throws IOException {
    Map<String, String> dataMap = genTableService.previewCode(tableId);
    return AjaxResult.success(dataMap);
  }

  /** 生成代码（下载方式） */
  @Operation(summary = "生成代码（下载方式）", description = "根据表名生成代码并以压缩包形式下载")
  @ApiResponse(
      responseCode = "200",
      description = "成功生成代码并下载",
      content =
          @Content(
              mediaType = "application/octet-stream",
              schema = @Schema(implementation = Void.class)))
  @Parameter(name = "tableName", description = "表名", required = true)
  @GetMapping("/download/{tableName}")
  public void download(HttpServletResponse response, @PathVariable("tableName") String tableName)
      throws IOException {
    byte[] data = genTableService.downloadCode(tableName);
    genCode(response, data);
  }

  /** 生成代码（自定义路径） */
  @Operation(summary = "生成代码（自定义路径）", description = "根据表名生成代码到自定义路径")
  @ApiResponse(
      responseCode = "200",
      description = "成功生成代码",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "tableName", description = "表名", required = true)
  @GetMapping("/genCode/{tableName}")
  public AjaxResult genCode(@PathVariable("tableName") String tableName) {
    genTableService.generatorCode(tableName);
    return AjaxResult.success();
  }

  /** 同步数据库 */
  @Operation(summary = "同步数据库", description = "根据表名同步数据库结构")
  @ApiResponse(
      responseCode = "200",
      description = "成功同步数据库",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "tableName", description = "表名", required = true)
  @GetMapping("/synchDb/{tableName}")
  public AjaxResult synchDb(@PathVariable("tableName") String tableName) {
    genTableService.synchDb(tableName);
    return AjaxResult.success();
  }

  /** 批量生成代码 */
  @Operation(summary = "批量生成代码", description = "根据表名数组批量生成代码并下载")
  @ApiResponse(
      responseCode = "200",
      description = "成功批量生成代码并下载",
      content =
          @Content(
              mediaType = "application/octet-stream",
              schema = @Schema(implementation = Void.class)))
  @Parameter(name = "tables", description = "表名数组", required = true)
  @GetMapping("/batchGenCode")
  public void batchGenCode(HttpServletResponse response, String tables) throws IOException {
    String[] tableNames = Convert.toStrArray(tables);
    byte[] data = genTableService.downloadCode(tableNames);
    genCode(response, data);
  }

  /** 生成zip文件 */
  private void genCode(HttpServletResponse response, byte[] data) throws IOException {
    response.reset();
    response.addHeader("Access-Control-Allow-Origin", "*");
    response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
    response.setHeader("Content-Disposition", "attachment; filename=\"ruoyi.zip\"");
    response.addHeader("Content-Length", "" + data.length);
    response.setContentType("application/octet-stream; charset=UTF-8");
    IOUtils.write(data, response.getOutputStream());
  }
}
