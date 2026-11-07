/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 公共物模型分类管理Controller
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.web;

import cn.universal.admin.platform.service.IIoTStandardCategoryService;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.persistence.entity.IoTStandardCategory;
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
 * 公共物模型分类管理Controller
 *
 * @since 2026-01-06
 */
@RestController
@RequestMapping("/admin/v1/standard/category")
@Tag(name = "公共物模型分类管理", description = "公共物模型分类管理接口")
@RequiredArgsConstructor
public class IoTStandardCategoryController extends BaseController {

  private final IIoTStandardCategoryService categoryService;

  /** 获取分类树结构 */
  @GetMapping("/tree")
  @Operation(summary = "获取分类树", description = "获取公共物模型分类树结构")
  @PreAuthorize("@ss.hasPermi('standard:category:list')")
  public AjaxResult<List<IoTStandardCategory>> tree() {
    List<IoTStandardCategory> tree = categoryService.selectCategoryTree();
    return AjaxResult.success(tree);
  }

  /** 获取分类列表 */
  @GetMapping("/list")
  @Operation(summary = "获取分类列表", description = "根据条件查询公共物模型分类列表")
  @PreAuthorize("@ss.hasPermi('standard:category:list')")
  public AjaxResult<List<IoTStandardCategory>> list(IoTStandardCategory category) {
    List<IoTStandardCategory> list = categoryService.selectCategoryList(category);
    return AjaxResult.success(list);
  }

  /** 获取子分类列表（懒加载） */
  @GetMapping("/children/{parentId}")
  @Operation(summary = "获取子分类", description = "根据父级ID获取子分类列表（懒加载）")
  @Parameter(name = "parentId", description = "父级分类ID", required = true)
  @PreAuthorize("@ss.hasPermi('standard:category:list')")
  public AjaxResult<List<IoTStandardCategory>> children(@PathVariable("parentId") Long parentId) {
    List<IoTStandardCategory> list = categoryService.selectChildrenByParentId(parentId);
    return AjaxResult.success(list);
  }

  /** 获取第三级分类列表（用于产品选择） */
  @GetMapping("/third-level")
  @Operation(summary = "获取第三级分类", description = "获取所有第三级分类，用于关联产品")
  @PreAuthorize("@ss.hasPermi('standard:category:list')")
  public AjaxResult<List<IoTStandardCategory>> thirdLevel() {
    List<IoTStandardCategory> list = categoryService.selectThirdLevelCategories();
    return AjaxResult.success(list);
  }

  /** 获取分类详情 */
  @GetMapping("/{id}")
  @Operation(summary = "获取分类详情", description = "根据ID获取分类详情")
  @Parameter(name = "id", description = "分类ID", required = true)
  @PreAuthorize("@ss.hasPermi('standard:category:list')")
  public AjaxResult<IoTStandardCategory> getInfo(@PathVariable("id") Long id) {
    IoTStandardCategory category = categoryService.selectCategoryById(id);
    if (category != null) {
      category.setCategoryPath(categoryService.getCategoryPath(id));
    }
    return AjaxResult.success(category);
  }

  /** 获取分类路径 */
  @GetMapping("/path/{id}")
  @Operation(summary = "获取分类路径", description = "获取分类的完整路径（如：智能城市/公共事业/路灯照明）")
  @Parameter(name = "id", description = "分类ID", required = true)
  @PreAuthorize("@ss.hasPermi('standard:category:list')")
  public AjaxResult<String> getCategoryPath(@PathVariable("id") Long id) {
    String path = categoryService.getCategoryPath(id);
    return AjaxResult.success("获取成功", path);
  }

  /** 新增分类 */
  @PostMapping
  @Operation(summary = "新增分类", description = "新增公共物模型分类")
  @PreAuthorize("@ss.hasPermi('standard:category:add')")
  @Log(title = "公共物模型分类", businessType = BusinessType.INSERT)
  public AjaxResult<Void> add(@Validated @RequestBody IoTStandardCategory category) {
    // 校验分类标识唯一性
    if (!categoryService.checkCategoryKeyUnique(category)) {
      return error("新增分类'" + category.getCategoryName() + "'失败，分类标识已存在");
    }
    // 校验层级限制（最多3级）
    if (category.getParentId() != null && category.getParentId() > 0) {
      IoTStandardCategory parent = categoryService.selectCategoryById(category.getParentId());
      if (parent != null && parent.getLevel() >= 3) {
        return error("新增分类失败，最多支持三级分类");
      }
    }
    category.setCreateBy(SecurityUtils.getUnionId());
    return toAjax(categoryService.insertCategory(category));
  }

  /** 修改分类 */
  @PutMapping
  @Operation(summary = "修改分类", description = "修改公共物模型分类")
  @PreAuthorize("@ss.hasPermi('standard:category:edit')")
  @Log(title = "公共物模型分类", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> edit(@Validated @RequestBody IoTStandardCategory category) {
    // 校验分类标识唯一性
    if (!categoryService.checkCategoryKeyUnique(category)) {
      return error("修改分类'" + category.getCategoryName() + "'失败，分类标识已存在");
    }
    // 不能将自己设为父级
    if (category.getId().equals(category.getParentId())) {
      return error("修改分类'" + category.getCategoryName() + "'失败，上级分类不能选择自己");
    }
    category.setUpdateBy(SecurityUtils.getUnionId());
    return toAjax(categoryService.updateCategory(category));
  }

  /** 删除分类 */
  @DeleteMapping("/{id}")
  @Operation(summary = "删除分类", description = "删除公共物模型分类")
  @Parameter(name = "id", description = "分类ID", required = true)
  @PreAuthorize("@ss.hasPermi('standard:category:remove')")
  @Log(title = "公共物模型分类", businessType = BusinessType.DELETE)
  public AjaxResult<Void> remove(@PathVariable("id") Long id) {
    // 校验是否存在子分类
    if (categoryService.hasChildren(id)) {
      return error("存在子分类，不允许删除");
    }
    return toAjax(categoryService.deleteCategoryById(id));
  }

  /** 批量删除分类 */
  @DeleteMapping("/batch/{ids}")
  @Operation(summary = "批量删除分类", description = "批量删除公共物模型分类")
  @Parameter(name = "ids", description = "分类ID数组", required = true)
  @PreAuthorize("@ss.hasPermi('standard:category:remove')")
  @Log(title = "公共物模型分类", businessType = BusinessType.DELETE)
  public AjaxResult<Void> batchRemove(@PathVariable("ids") Long[] ids) {
    // 校验每个分类是否存在子分类
    for (Long id : ids) {
      if (categoryService.hasChildren(id)) {
        IoTStandardCategory category = categoryService.selectCategoryById(id);
        return error("分类'" + category.getCategoryName() + "'存在子分类，不允许删除");
      }
    }
    return toAjax(categoryService.deleteCategoryByIds(ids));
  }
}
