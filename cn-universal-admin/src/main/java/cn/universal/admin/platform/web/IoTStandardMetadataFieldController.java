/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 标准物模型字段维护Controller
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.web;

import cn.universal.admin.platform.service.IIoTStandardFieldCategoryRelService;
import cn.universal.admin.platform.service.IIoTStandardMetadataFieldService;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.persistence.entity.IoTStandardMetadataField;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 标准物模型字段维护Controller
 *
 * @since 2026-01-06
 */
@RestController
@RequestMapping("/admin/v1/standard/field")
@Tag(name = "标准物模型字段维护", description = "标准物模型字段维护接口")
@RequiredArgsConstructor
public class IoTStandardMetadataFieldController extends BaseController {

  private final IIoTStandardMetadataFieldService fieldService;
  private final IIoTStandardFieldCategoryRelService fieldCategoryRelService;

  /** 获取字段列表 */
  @GetMapping("/list")
  @Operation(summary = "获取字段列表", description = "根据条件分页查询标准物模型字段列表")
  @PreAuthorize("@ss.hasPermi('standard:field:list')")
  public TableDataInfo<IoTStandardMetadataField> list(IoTStandardMetadataField field) {
    startPage();
    List<IoTStandardMetadataField> list = fieldService.selectFieldList(field);
    return getDataTable(list);
  }

  /** 根据类型获取字段列表 */
  @GetMapping("/type/{fieldType}")
  @Operation(summary = "根据类型获取字段", description = "根据字段类型获取字段列表（property/event/function）")
  @Parameter(name = "fieldType", description = "字段类型：property-属性，event-事件，function-功能", required = true)
  @PreAuthorize("@ss.hasPermi('standard:field:list')")
  public TableDataInfo<IoTStandardMetadataField> listByType(@PathVariable("fieldType") String fieldType) {
    startPage();
    List<IoTStandardMetadataField> list = fieldService.selectFieldByType(fieldType);
    return getDataTable(list);
  }

  /** 关键字搜索字段（联想功能） */
  @GetMapping("/search")
  @Operation(summary = "关键字搜索", description = "根据关键字搜索字段，用于输入时联想功能")
  @Parameter(name = "keyword", description = "搜索关键字", required = true)
  @Parameter(name = "fieldType", description = "字段类型（可选）：property-属性，event-事件，function-功能")
  @Parameter(name = "limit", description = "返回数量限制，默认10")
  @PreAuthorize("@ss.hasPermi('standard:field:search')")
  public AjaxResult<List<IoTStandardMetadataField>> search(
      @RequestParam("keyword") String keyword,
      @RequestParam(value = "fieldType", required = false) String fieldType,
      @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
    List<IoTStandardMetadataField> list = fieldService.searchFieldByKeyword(keyword, fieldType, limit);
    return AjaxResult.success(list);
  }

  /** 全文搜索字段 */
  @GetMapping("/fulltext")
  @Operation(summary = "全文搜索", description = "使用全文索引搜索字段")
  @Parameter(name = "keyword", description = "搜索关键字", required = true)
  @Parameter(name = "fieldType", description = "字段类型（可选）：property-属性，event-事件，function-功能")
  @Parameter(name = "limit", description = "返回数量限制，默认20")
  @PreAuthorize("@ss.hasPermi('standard:field:search')")
  public AjaxResult<List<IoTStandardMetadataField>> fulltextSearch(
      @RequestParam("keyword") String keyword,
      @RequestParam(value = "fieldType", required = false) String fieldType,
      @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
    List<IoTStandardMetadataField> list = fieldService.fulltextSearch(keyword, fieldType, limit);
    return AjaxResult.success(list);
  }

  /** 获取字段详情 */
  @GetMapping("/{id}")
  @Operation(summary = "获取字段详情", description = "根据ID获取字段详情")
  @Parameter(name = "id", description = "字段ID", required = true)
  @PreAuthorize("@ss.hasPermi('standard:field:list')")
  public AjaxResult<IoTStandardMetadataField> getInfo(@PathVariable("id") Long id) {
    IoTStandardMetadataField field = fieldService.selectFieldById(id);
    return AjaxResult.success(field);
  }

  /** 根据字段标识获取详情 */
  @GetMapping("/key/{fieldId}")
  @Operation(summary = "根据字段标识获取详情", description = "根据字段标识获取字段详情")
  @Parameter(name = "fieldId", description = "字段标识", required = true)
  @PreAuthorize("@ss.hasPermi('standard:field:list')")
  public AjaxResult<IoTStandardMetadataField> getByFieldId(@PathVariable("fieldId") String fieldId) {
    IoTStandardMetadataField field = fieldService.selectFieldByFieldId(fieldId);
    return AjaxResult.success(field);
  }

  /** 新增字段 */
  @PostMapping
  @Operation(summary = "新增字段", description = "新增标准物模型字段")
  @PreAuthorize("@ss.hasPermi('standard:field:add')")
  @Log(title = "标准物模型字段", businessType = BusinessType.INSERT)
  public AjaxResult<Void> add(@Validated @RequestBody IoTStandardMetadataField field) {
    // 校验字段标识唯一性
    if (!fieldService.checkFieldIdUnique(field)) {
      return error("新增字段'" + field.getFieldName() + "'失败，字段标识已存在");
    }
    field.setCreateBy(SecurityUtils.getUnionId());
    return toAjax(fieldService.insertField(field));
  }

  /** 修改字段 */
  @PutMapping
  @Operation(summary = "修改字段", description = "修改标准物模型字段")
  @PreAuthorize("@ss.hasPermi('standard:field:edit')")
  @Log(title = "标准物模型字段", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> edit(@Validated @RequestBody IoTStandardMetadataField field) {
    // 校验字段标识唯一性
    if (!fieldService.checkFieldIdUnique(field)) {
      return error("修改字段'" + field.getFieldName() + "'失败，字段标识已存在");
    }
    field.setUpdateBy(SecurityUtils.getUnionId());
    return toAjax(fieldService.updateField(field));
  }

  /** 删除字段 */
  @DeleteMapping("/{id}")
  @Operation(summary = "删除字段", description = "删除标准物模型字段")
  @Parameter(name = "id", description = "字段ID", required = true)
  @PreAuthorize("@ss.hasPermi('standard:field:remove')")
  @Log(title = "标准物模型字段", businessType = BusinessType.DELETE)
  public AjaxResult<Void> remove(@PathVariable("id") Long id) {
    return toAjax(fieldService.deleteFieldById(id));
  }

  /** 批量删除字段 */
  @DeleteMapping("/batch/{ids}")
  @Operation(summary = "批量删除字段", description = "批量删除标准物模型字段")
  @Parameter(name = "ids", description = "字段ID数组", required = true)
  @PreAuthorize("@ss.hasPermi('standard:field:remove')")
  @Log(title = "标准物模型字段", businessType = BusinessType.DELETE)
  public AjaxResult<Void> batchRemove(@PathVariable("ids") Long[] ids) {
    return toAjax(fieldService.deleteFieldByIds(ids));
  }

  /** 增加字段使用次数 */
  @PutMapping("/use/{id}")
  @Operation(summary = "增加使用次数", description = "增加字段的使用次数计数")
  @Parameter(name = "id", description = "字段ID", required = true)
  @PreAuthorize("@ss.hasPermi('standard:field:edit')")
  public AjaxResult<Integer> incrementUseCount(@PathVariable("id") Long id) {
    int result = fieldService.incrementUseCount(id);
    return AjaxResult.success(result);
  }

  /** 获取热门字段 */
  @GetMapping("/popular")
  @Operation(summary = "获取热门字段", description = "获取使用次数最多的字段")
  @Parameter(name = "fieldType", description = "字段类型（可选）：property-属性，event-事件，function-功能")
  @Parameter(name = "limit", description = "返回数量限制，默认20")
  @PreAuthorize("@ss.hasPermi('standard:field:list')")
  public AjaxResult<List<IoTStandardMetadataField>> getPopular(
      @RequestParam(value = "fieldType", required = false) String fieldType,
      @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
    IoTStandardMetadataField query = new IoTStandardMetadataField();
    query.setFieldType(fieldType);
    query.setStatus(1);
    List<IoTStandardMetadataField> list = fieldService.selectFieldList(query);
    // 按使用次数排序后取前limit个
    list.sort((a, b) -> (b.getUseCount() == null ? 0 : b.getUseCount()) 
        - (a.getUseCount() == null ? 0 : a.getUseCount()));
    if (list.size() > limit) {
      list = list.subList(0, limit);
    }
    return AjaxResult.success(list);
  }

  /** 根据分类推荐字段 */
  @GetMapping("/recommend")
  @Operation(summary = "根据分类推荐字段", description = "根据产品分类推荐相关的字段")
  @Parameter(name = "categoryId", description = "分类ID", required = true)
  @Parameter(name = "fieldType", description = "字段类型：property-属性，event-事件，function-功能", required = true)
  @Parameter(name = "limit", description = "返回数量限制，默认10")
  @PreAuthorize("@ss.hasPermi('standard:field:list')")
  public AjaxResult<List<IoTStandardMetadataField>> getRecommendFields(
      @RequestParam Long categoryId,
      @RequestParam String fieldType,
      @RequestParam(defaultValue = "10") Integer limit) {
    List<IoTStandardMetadataField> list = 
        fieldService.selectRecommendFieldsByCategory(categoryId, fieldType, limit);
    return AjaxResult.success(list);
  }

  /** 设置字段与分类的关联 */
  @PutMapping("/categories/{fieldId}")
  @Operation(summary = "设置字段分类关联", description = "设置字段与多个分类的关联关系")
  @Parameter(name = "fieldId", description = "字段ID", required = true)
  @PreAuthorize("@ss.hasPermi('standard:field:edit')")
  @Log(title = "设置字段分类", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> setFieldCategories(
      @PathVariable Long fieldId,
      @RequestBody List<Long> categoryIds) {
    return toAjax(fieldCategoryRelService.setFieldCategories(fieldId, categoryIds));
  }

  /** 获取字段关联的分类 */
  @GetMapping("/categories/{fieldId}")
  @Operation(summary = "获取字段关联的分类", description = "获取字段关联的所有分类ID列表")
  @Parameter(name = "fieldId", description = "字段ID", required = true)
  @PreAuthorize("@ss.hasPermi('standard:field:list')")
  public AjaxResult<List<Long>> getFieldCategories(@PathVariable Long fieldId) {
    List<Long> categoryIds = fieldCategoryRelService.getFieldCategories(fieldId);
    return AjaxResult.success(categoryIds);
  }
}
