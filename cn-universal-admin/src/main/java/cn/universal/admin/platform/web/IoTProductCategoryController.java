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

package cn.universal.admin.platform.web;

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.platform.service.IIoTProductCategoryService;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.enums.OperatorType;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTProductCategory;
import cn.universal.persistence.entity.bo.RuleModelBO.IoTProductSortBO;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产品分类Controller @Author gitee.com/NexIoT
 *
 * @since 2025-12-29
 */
@RestController
@RequestMapping("/admin/sort")
@Tag(name = "产品分类管理", description = "产品分类管理接口")
public class IoTProductCategoryController extends BaseController {

  @Autowired private IIoTProductCategoryService devProductSortService;
  @Resource private IoTProductMapper ioTProductMapper;

  /**
   * 查询产品分类列表
   *
   * @param ioTProductCategory 产品分类查询条件
   * @return 产品分类列表
   */
  @Operation(summary = "查询产品分类列表", description = "根据查询条件获取产品分类列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取产品分类列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/list")
  public AjaxResult list(IoTProductCategory ioTProductCategory) {
    List<IoTProductCategory> list = devProductSortService.selectDevProductSortList(
        ioTProductCategory);
    return AjaxResult.success(list);
  }

  /**
   * 获取产品分类树结构
   *
   * @return 产品分类树结构
   */
  @Operation(summary = "获取产品分类树结构", description = "获取产品分类树结构")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取产品分类树结构",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/tree")
  public AjaxResult getTree() {
    List<IoTProductCategory> tree = devProductSortService.getProductSortTree();
    return AjaxResult.success(tree);
  }

  /**
   * 获取产品分类详细信息
   *
   * @param id 产品分类ID
   * @return 产品分类详细信息
   */
  @Operation(summary = "获取产品分类详细信息", description = "根据ID获取产品分类详细信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取产品分类详细信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "产品分类ID", required = true)
  @GetMapping(value = "/{id}")
  public AjaxResult getInfo(@PathVariable("id") String id) {
    return AjaxResult.success(devProductSortService.selectDevProductSortById(id));
  }

  /**
   * 获取产品分类详细信息（POST方式）
   *
   * @param ioTProductCategory 产品分类信息
   * @return 产品分类详细信息
   */
  @Operation(summary = "获取产品分类详细信息（POST方式）", description = "通过POST方式获取产品分类详细信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取产品分类详细信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping(value = "/info")
  public AjaxResult getInfo(@RequestBody IoTProductCategory ioTProductCategory) {
    if (StrUtil.isBlank(ioTProductCategory.getId())) {
      AjaxResult.success();
    }
    return AjaxResult.success(
        devProductSortService.selectDevProductSortById(ioTProductCategory.getId()));
  }

  /**
   * 新增产品分类（使用BO）
   *
   * @param bo 产品分类信息
   * @return 新增结果
   */
  @Operation(summary = "新增产品分类", description = "新增产品分类信息（使用BO）")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增产品分类",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/add")
  @Log(
      title = "新增产品分类",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult addWithBo(@Valid @RequestBody IoTProductSortBO bo) {
    rejectSuperAdminCreate("产品分类");
    try {
      int result = devProductSortService.insertDevProductSort(bo);
      return toAjax(result);
    } catch (IllegalArgumentException e) {
      return AjaxResult.error(e.getMessage());
    } catch (Exception e) {
      return AjaxResult.error("新增产品分类失败：" + e.getMessage());
    }
  }

  /**
   * 修改产品分类（使用BO）
   *
   * @param bo 产品分类信息
   * @return 修改结果
   */
  @Operation(summary = "修改产品分类", description = "修改产品分类信息（使用BO）")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改产品分类",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping("/edit")
  @Log(
      title = "修改产品分类",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult editWithBo(@Valid @RequestBody IoTProductSortBO bo) {
    try {
      int result = devProductSortService.updateDevProductSort(bo);
      return toAjax(result);
    } catch (IllegalArgumentException e) {
      return AjaxResult.error(e.getMessage());
    } catch (Exception e) {
      return AjaxResult.error("修改产品分类失败：" + e.getMessage());
    }
  }

  /**
   * 新增产品分类（原有方法，保持兼容性）
   *
   * @param ioTProductCategory 产品分类信息
   * @return 新增结果
   */
  @Operation(summary = "新增产品分类（兼容性方法）", description = "新增产品分类信息（原有方法，保持兼容性）")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增产品分类",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping
  @Log(title = "新增产品分类", businessType = BusinessType.INSERT)
  public AjaxResult add(@RequestBody IoTProductCategory ioTProductCategory) {
    rejectSuperAdminCreate("产品分类");
    return toAjax(devProductSortService.insertDevProductSort(ioTProductCategory));
  }

  /** 修改产品分类（原有方法，保持兼容性） */
  @PutMapping
  @Log(title = "修改产品分类", businessType = BusinessType.UPDATE)
  public AjaxResult edit(@RequestBody IoTProductCategory ioTProductCategory) {
    return toAjax(devProductSortService.updateDevProductSort(ioTProductCategory));
  }

  /** 删除产品分类 */
  @DeleteMapping("/{ids}")
  @Log(
      title = "删除产品分类",
      businessType = BusinessType.DELETE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult remove(@PathVariable String[] ids) {
    for (String id : ids) {
      IoTProductCategory ioTProductCategory = devProductSortService.selectDevProductSortById(id);
      if (ioTProductCategory.getHasChild() == 1) {
        throw new IoTException("该分类下有子分类");
      }
      IoTProduct ioTProduct = IoTProduct.builder().classifiedId(id).build();
      List<IoTProduct> ioTProductList = ioTProductMapper.select(ioTProduct);
      if (!ioTProductList.isEmpty()) {
        throw new IoTException("该分类下有产品");
      }
    }
    return toAjax(devProductSortService.deleteDevProductSortByIds(ids));
  }

  /** 获取菜单下拉树列表 */
  @GetMapping("/treeselect")
  public AjaxResult treeselect(IoTProductCategory productSort) {
    List<IoTProductCategory> productSortList =
        devProductSortService.selectDevProductSortList(productSort);
    return AjaxResult.success(devProductSortService.buildProductSortTree(productSortList));
  }
}
