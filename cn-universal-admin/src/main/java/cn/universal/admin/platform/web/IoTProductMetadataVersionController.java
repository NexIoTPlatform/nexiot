/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 物模型版本管理Controller
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.web;

import cn.universal.admin.platform.service.IIoTProductMetadataVersionService;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.persistence.entity.IoTProductMetadataVersion;
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
import org.springframework.web.bind.annotation.*;

/**
 * 物模型版本管理Controller
 *
 * @since 2026-01-06
 */
@RestController
@RequestMapping("/admin/v1/metadata/version")
@Tag(name = "物模型版本管理", description = "物模型版本历史记录管理接口")
@RequiredArgsConstructor
public class IoTProductMetadataVersionController extends BaseController {

  private final IIoTProductMetadataVersionService versionService;

  /** 获取版本列表 */
  @GetMapping("/list")
  @Operation(summary = "获取版本列表", description = "根据条件分页查询物模型版本历史列表")
  @PreAuthorize("@ss.hasPermi('system:metadata:version:list')")
  public TableDataInfo<IoTProductMetadataVersion> list(IoTProductMetadataVersion version) {
    startPage();
    List<IoTProductMetadataVersion> list = versionService.selectVersionList(version);
    return getDataTable(list);
  }

  /** 获取产品的版本历史 */
  @GetMapping("/product/{productId}")
  @Operation(summary = "获取产品版本历史", description = "获取指定产品的版本历史列表")
  @Parameter(name = "productId", description = "产品ID", required = true)
  @PreAuthorize("@ss.hasPermi('system:metadata:version:list')")
  public TableDataInfo<IoTProductMetadataVersion> listByProduct(
      @PathVariable("productId") Long productId) {
    startPage();
    List<IoTProductMetadataVersion> list = versionService.selectVersionByProductId(productId);
    return getDataTable(list);
  }

  /** 获取产品的版本历史（按productKey） */
  @GetMapping("/productKey/{productKey}")
  @Operation(summary = "根据productKey获取版本历史", description = "根据产品标识获取版本历史列表")
  @Parameter(name = "productKey", description = "产品标识", required = true)
  @PreAuthorize("@ss.hasPermi('system:metadata:version:list')")
  public TableDataInfo<IoTProductMetadataVersion> listByProductKey(
      @PathVariable("productKey") String productKey) {
    startPage();
    List<IoTProductMetadataVersion> list = versionService.selectVersionByProductKey(productKey);
    return getDataTable(list);
  }

  /** 获取版本详情 */
  @GetMapping("/{id}")
  @Operation(summary = "获取版本详情", description = "根据ID获取版本详情，包含完整物模型数据")
  @Parameter(name = "id", description = "版本ID", required = true)
  @PreAuthorize("@ss.hasPermi('system:metadata:version:list')")
  public AjaxResult<IoTProductMetadataVersion> getInfo(@PathVariable("id") Long id) {
    IoTProductMetadataVersion version = versionService.selectVersionById(id);
    return AjaxResult.success(version);
  }

  /** 获取最新版本 */
  @GetMapping("/latest/{productId}")
  @Operation(summary = "获取最新版本", description = "获取指定产品的最新版本")
  @Parameter(name = "productId", description = "产品ID", required = true)
  @PreAuthorize("@ss.hasPermi('system:metadata:version:list')")
  public AjaxResult<IoTProductMetadataVersion> getLatest(
      @PathVariable("productId") Long productId) {
    IoTProductMetadataVersion version = versionService.selectLatestVersion(productId);
    return AjaxResult.success(version);
  }

  /** 比较两个版本 */
  @GetMapping("/compare/{versionId1}/{versionId2}")
  @Operation(summary = "比较两个版本", description = "比较两个版本之间的差异")
  @Parameter(name = "versionId1", description = "版本1 ID", required = true)
  @Parameter(name = "versionId2", description = "版本2 ID", required = true)
  @PreAuthorize("@ss.hasPermi('system:metadata:version:compare')")
  public AjaxResult<String> compare(
      @PathVariable("versionId1") Long versionId1, @PathVariable("versionId2") Long versionId2) {
    String diffResult = versionService.compareVersions(versionId1, versionId2);
    return AjaxResult.success("返回成功", diffResult);
  }

  /** 还原到指定版本 */
  @PostMapping("/restore/{versionId}/{targetProductId}")
  @Operation(summary = "还原版本", description = "将目标产品的物模型还原到指定历史版本")
  @Parameter(name = "versionId", description = "要还原的版本ID", required = true)
  @Parameter(name = "targetProductId", description = "目标产品ID", required = true)
  @PreAuthorize("@ss.hasPermi('system:metadata:version:restore')")
  @Log(title = "还原物模型版本", businessType = BusinessType.UPDATE)
  public AjaxResult<Integer> restore(
      @PathVariable("versionId") Long versionId,
      @PathVariable("targetProductId") Long targetProductId) {
    String operator = SecurityUtils.getUnionId();
    int result = versionService.restoreVersion(versionId, targetProductId, operator);
    if (result > 0) {
      return AjaxResult.success("还原成功", result);
    }
    return AjaxResult.error("还原失败", result);
  }

  /** 手动创建版本快照 */
  @PostMapping("/snapshot/{productId}")
  @Operation(summary = "创建版本快照", description = "手动创建产品物模型的版本快照")
  @Parameter(name = "productId", description = "产品ID", required = true)
  @Parameter(name = "remark", description = "版本备注")
  @PreAuthorize("@ss.hasPermi('system:metadata:version:add')")
  @Log(title = "创建物模型版本快照", businessType = BusinessType.INSERT)
  public AjaxResult<Long> createSnapshot(
      @PathVariable("productId") Long productId,
      @RequestParam(value = "remark", required = false) String remark) {
    String operator = SecurityUtils.getUnionId();
    Long versionId = versionService.createSnapshotFromProduct(productId, remark, operator);
    if (versionId != null && versionId > 0) {
      return AjaxResult.success("创建成功", versionId);
    }
    return AjaxResult.error("创建失败", versionId);
  }

  /** 删除版本 */
  @DeleteMapping("/{id}")
  @Operation(summary = "删除版本", description = "删除指定版本记录")
  @Parameter(name = "id", description = "版本ID", required = true)
  @PreAuthorize("@ss.hasPermi('system:metadata:version:remove')")
  @Log(title = "删除物模型版本", businessType = BusinessType.DELETE)
  public AjaxResult<Void> remove(@PathVariable("id") Long id) {
    return toAjax(versionService.deleteVersionById(id));
  }

  /** 批量删除版本 */
  @DeleteMapping("/batch/{ids}")
  @Operation(summary = "批量删除版本", description = "批量删除版本记录")
  @Parameter(name = "ids", description = "版本ID数组", required = true)
  @PreAuthorize("@ss.hasPermi('system:metadata:version:remove')")
  @Log(title = "批量删除物模型版本", businessType = BusinessType.DELETE)
  public AjaxResult<Void> batchRemove(@PathVariable("ids") Long[] ids) {
    return toAjax(versionService.deleteVersionByIds(ids));
  }

  /** 获取版本统计信息 */
  @GetMapping("/stats/{productId}")
  @Operation(summary = "获取版本统计", description = "获取指定产品的版本统计信息")
  @Parameter(name = "productId", description = "产品ID", required = true)
  @PreAuthorize("@ss.hasPermi('system:metadata:version:list')")
  public AjaxResult<Object> getStats(@PathVariable("productId") Long productId) {
    int totalVersions = versionService.countVersionByProductId(productId);
    IoTProductMetadataVersion latest = versionService.selectLatestVersion(productId);
    return AjaxResult.success(
        new java.util.HashMap<String, Object>() {
          {
            put("totalVersions", totalVersions);
            put("latestVersion", latest);
          }
        });
  }
}
