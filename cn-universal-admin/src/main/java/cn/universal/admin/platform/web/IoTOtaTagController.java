/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA标签管理控制器
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.web;

import cn.universal.admin.platform.service.IIoTOtaAdminService;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.persistence.entity.IoTOtaTag;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** OTA标签管理控制器 */
@RestController
@RequestMapping("/admin/v1/ota/tag")
@Tag(name = "OTA标签管理")
@RequiredArgsConstructor
public class IoTOtaTagController extends BaseController {

  private final IIoTOtaAdminService otaAdminService;

  @PreAuthorize("@ss.hasPermi('system:ota:list')")
  @GetMapping("/list")
  public TableDataInfo<IoTOtaTag> list(IoTOtaTag query) {
    startPage();
    List<IoTOtaTag> list =
        otaAdminService.selectTagList(
            query, loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId(), isAdmin());
    return getDataTable(list);
  }

  @PreAuthorize("@ss.hasPermi('system:ota:list')")
  @GetMapping("/{id}")
  public AjaxResult<IoTOtaTag> getInfo(@PathVariable("id") Long id) {
    return AjaxResult.success(otaAdminService.selectTagById(id));
  }

  @PreAuthorize("@ss.hasPermi('system:ota:tag:add')")
  @PostMapping
  @Log(title = "新增OTA标签", businessType = BusinessType.INSERT)
  public AjaxResult<Long> add(@RequestBody IoTOtaTag tag) {
    return AjaxResult.success(
        "新增成功",
        otaAdminService.saveTag(tag, loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId()));
  }

  @PreAuthorize("@ss.hasPermi('system:ota:tag:edit')")
  @PutMapping
  @Log(title = "修改OTA标签", businessType = BusinessType.UPDATE)
  public AjaxResult<Long> edit(@RequestBody IoTOtaTag tag) {
    return AjaxResult.success(
        "修改成功",
        otaAdminService.saveTag(tag, loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId()));
  }

  @PreAuthorize("@ss.hasPermi('system:ota:tag:remove')")
  @DeleteMapping("/{id}")
  @Log(title = "删除OTA标签", businessType = BusinessType.DELETE)
  public AjaxResult<Void> remove(@PathVariable("id") Long id) {
    return toAjax(otaAdminService.deleteTagById(id));
  }
}
