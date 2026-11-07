package cn.universal.admin.platform.web;

import cn.universal.admin.platform.service.IIoTOtaAdminService;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.persistence.query.AjaxResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 产品级厂家 OTA 适配，只保存传输参数，报文翻译复用产品协议脚本。 */
@RestController
@RequestMapping("/admin/v1/ota/profile")
@Tag(name = "OTA厂家适配")
@RequiredArgsConstructor
public class IoTOtaProfileController {

  private final IIoTOtaAdminService otaAdminService;

  @PreAuthorize("@ss.hasPermi('system:ota:list')")
  @GetMapping("/{productKey}")
  public AjaxResult<Map<String, Object>> get(@PathVariable String productKey) {
    return AjaxResult.success(otaAdminService.getTransferProfile(productKey));
  }

  @PreAuthorize("@ss.hasPermi('system:ota:package:edit')")
  @PutMapping("/{productKey}")
  @Log(title = "保存OTA厂家适配", businessType = BusinessType.UPDATE)
  public AjaxResult<Map<String, Object>> save(
      @PathVariable String productKey, @RequestBody Map<String, Object> profile) {
    return AjaxResult.success(otaAdminService.saveTransferProfile(productKey, profile));
  }
}
