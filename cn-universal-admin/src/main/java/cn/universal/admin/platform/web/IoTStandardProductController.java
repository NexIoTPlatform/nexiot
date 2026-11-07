/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 公共物模型产品管理Controller
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.web;

import cn.universal.admin.platform.service.IIoTStandardCategoryService;
import cn.universal.admin.platform.service.IIoTStandardProductService;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.persistence.entity.IoTStandardProduct;
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
 * 公共物模型产品管理Controller
 *
 * @since 2026-01-06
 */
@RestController
@RequestMapping("/admin/v1/standard/product")
@Tag(name = "公共物模型产品管理", description = "公共物模型产品管理接口")
@RequiredArgsConstructor
public class IoTStandardProductController extends BaseController {

  private final IIoTStandardProductService productService;
  private final IIoTStandardCategoryService categoryService;

  /** 获取产品列表 */
  @GetMapping("/list")
  @Operation(summary = "获取产品列表", description = "根据条件分页查询公共物模型产品列表")
  @PreAuthorize("@ss.hasPermi('standard:product:list')")
  public TableDataInfo<IoTStandardProduct> list(IoTStandardProduct product) {
    startPage();
    List<IoTStandardProduct> list = productService.selectProductList(product);
    return getDataTable(list);
  }

  /** 根据分类ID获取产品列表 */
  @GetMapping("/category/{categoryId}")
  @Operation(summary = "根据分类获取产品", description = "根据分类ID获取当前分类及子分类下的产品列表")
  @Parameter(name = "categoryId", description = "分类ID", required = true)
  @PreAuthorize("@ss.hasPermi('standard:product:list')")
  public TableDataInfo<IoTStandardProduct> listByCategory(
      @PathVariable("categoryId") Long categoryId) {
    startPage();
    List<IoTStandardProduct> list = productService.selectProductByCategoryId(categoryId);
    return getDataTable(list);
  }

  /** 获取产品详情 */
  @GetMapping("/{id}")
  @Operation(summary = "获取产品详情", description = "根据ID获取产品详情，包含物模型数据")
  @Parameter(name = "id", description = "产品ID", required = true)
  @PreAuthorize("@ss.hasPermi('standard:product:list')")
  public AjaxResult<IoTStandardProduct> getInfo(@PathVariable("id") Long id) {
    IoTStandardProduct product = productService.selectProductById(id);
    return AjaxResult.success(product);
  }

  /** 根据产品标识获取产品详情 */
  @GetMapping("/key/{productKey}")
  @Operation(summary = "根据产品标识获取详情", description = "根据产品标识获取产品详情")
  @Parameter(name = "productKey", description = "产品标识", required = true)
  @PreAuthorize("@ss.hasPermi('standard:product:list')")
  public AjaxResult<IoTStandardProduct> getByKey(@PathVariable("productKey") String productKey) {
    IoTStandardProduct product = productService.selectProductByKey(productKey);
    if (product != null && product.getCategoryId() != null) {
      product.setCategoryPath(categoryService.getCategoryPath(product.getCategoryId()));
    }
    return AjaxResult.success(product);
  }

  /** 新增产品 */
  @PostMapping
  @Operation(summary = "新增产品", description = "新增公共物模型产品")
  @PreAuthorize("@ss.hasPermi('standard:product:add')")
  @Log(title = "公共物模型产品", businessType = BusinessType.INSERT)
  public AjaxResult<Void> add(@Validated @RequestBody IoTStandardProduct product) {
    // 校验产品标识唯一性
    if (!productService.checkProductKeyUnique(product)) {
      return error("新增产品'" + product.getProductName() + "'失败，产品标识已存在");
    }
    // 允许任意层级，分类仍须存在且在当前租户的可见范围内。
    if (product.getCategoryId() == null
        || categoryService.selectCategoryById(product.getCategoryId()) == null) {
      return error("新增产品失败，请选择有效的所属分类");
    }
    product.setCreateBy(SecurityUtils.getUnionId());
    return toAjax(productService.insertProduct(product));
  }

  /** 修改产品 */
  @PutMapping
  @Operation(summary = "修改产品", description = "修改公共物模型产品")
  @PreAuthorize("@ss.hasPermi('standard:product:edit')")
  @Log(title = "公共物模型产品", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> edit(@Validated @RequestBody IoTStandardProduct product) {
    // 校验产品标识唯一性
    if (!productService.checkProductKeyUnique(product)) {
      return error("修改产品'" + product.getProductName() + "'失败，产品标识已存在");
    }
    // 允许任意层级，分类仍须存在且在当前租户的可见范围内。
    if (product.getCategoryId() == null
        || categoryService.selectCategoryById(product.getCategoryId()) == null) {
      return error("修改产品失败，请选择有效的所属分类");
    }
    product.setUpdateBy(SecurityUtils.getUnionId());
    return toAjax(productService.updateProduct(product));
  }

  /** 删除产品 */
  @DeleteMapping("/{id}")
  @Operation(summary = "删除产品", description = "删除公共物模型产品")
  @Parameter(name = "id", description = "产品ID", required = true)
  @PreAuthorize("@ss.hasPermi('standard:product:remove')")
  @Log(title = "公共物模型产品", businessType = BusinessType.DELETE)
  public AjaxResult<Void> remove(@PathVariable("id") Long id) {
    return toAjax(productService.deleteProductById(id));
  }

  /** 批量删除产品 */
  @DeleteMapping("/batch/{ids}")
  @Operation(summary = "批量删除产品", description = "批量删除公共物模型产品")
  @Parameter(name = "ids", description = "产品ID数组", required = true)
  @PreAuthorize("@ss.hasPermi('standard:product:remove')")
  @Log(title = "公共物模型产品", businessType = BusinessType.DELETE)
  public AjaxResult<Void> batchRemove(@PathVariable("ids") Long[] ids) {
    return toAjax(productService.deleteProductByIds(ids));
  }

  /** 导入标准产品物模型到目标产品 */
  @PostMapping("/import/{standardProductId}/{targetProductId}")
  @Operation(summary = "导入物模型", description = "将标准产品的物模型导入到目标产品（覆盖模式，记录版本历史）")
  @Parameter(name = "standardProductId", description = "标准产品ID", required = true)
  @Parameter(name = "targetProductId", description = "目标产品ID", required = true)
  @PreAuthorize("@ss.hasPermi('standard:product:import')")
  @Log(title = "导入标准物模型", businessType = BusinessType.IMPORT)
  public AjaxResult<Integer> importMetadata(
      @PathVariable("standardProductId") Long standardProductId,
      @PathVariable("targetProductId") Long targetProductId) {
    String operator = SecurityUtils.getUnionId();
    int result = productService.importStandardProduct(standardProductId, targetProductId, operator);
    if (result > 0) {
      return AjaxResult.success("导入成功", result);
    }
    return AjaxResult.error("导入失败", 0);
  }

  /** 更新产品物模型 */
  @PutMapping("/metadata/{id}")
  @Operation(summary = "更新物模型", description = "更新产品的物模型数据")
  @Parameter(name = "id", description = "产品ID", required = true)
  @PreAuthorize("@ss.hasPermi('standard:product:edit')")
  @Log(title = "更新产品物模型", businessType = BusinessType.UPDATE)
  public AjaxResult<Integer> updateMetadata(
      @PathVariable("id") Long id, @RequestBody String metadata) {
    String operator = SecurityUtils.getUnionId();
    int result = productService.updateProductMetadata(id, metadata, operator);
    if (result > 0) {
      return AjaxResult.success("更新成功", result);
    }
    return AjaxResult.error("更新失败", 0);
  }

  /** 设置/取消模板标记 */
  @PutMapping("/template/{id}")
  @Operation(summary = "设置模板标记", description = "将产品设置为模板或取消模板")
  @Parameter(name = "id", description = "产品ID", required = true)
  @Parameter(name = "isTemplate", description = "是否为模板（0-否，1-是）", required = true)
  @PreAuthorize("@ss.hasPermi('standard:product:edit')")
  @Log(title = "设置模板标记", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> setTemplate(
      @PathVariable("id") Long id, @RequestParam Integer isTemplate) {
    return toAjax(productService.setTemplate(id, isTemplate));
  }

  /** 获取模板列表 */
  @GetMapping("/templates")
  @Operation(summary = "获取模板列表", description = "获取所有标记为模板的产品列表")
  @PreAuthorize("@ss.hasPermi('standard:product:list')")
  public AjaxResult<List<IoTStandardProduct>> listTemplates() {
    List<IoTStandardProduct> list = productService.selectTemplateList();
    return AjaxResult.success(list);
  }

  /** 获取热门产品 */
  @GetMapping("/popular")
  @Operation(summary = "获取热门产品", description = "按使用次数获取热门产品列表")
  @Parameter(name = "limit", description = "返回数量限制（默认10）")
  @PreAuthorize("@ss.hasPermi('standard:product:list')")
  public AjaxResult<List<IoTStandardProduct>> getPopularProducts(
      @RequestParam(defaultValue = "10") Integer limit) {
    List<IoTStandardProduct> list = productService.selectPopularProducts(limit);
    return AjaxResult.success(list);
  }

  /** 增加使用次数 */
  @PutMapping("/use/{id}")
  @Operation(summary = "增加使用次数", description = "当产品被引用时增加使用次数")
  @Parameter(name = "id", description = "产品ID", required = true)
  @PreAuthorize("@ss.hasPermi('standard:product:edit')")
  @Log(title = "增加产品使用次数", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> incrementUseCount(@PathVariable("id") Long id) {
    return toAjax(productService.incrementUseCount(id));
  }
}
