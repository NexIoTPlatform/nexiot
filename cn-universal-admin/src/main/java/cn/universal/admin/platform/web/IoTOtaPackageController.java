/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA升级包管理控制器
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.web;

import cn.universal.admin.platform.service.IIoTOtaAdminService;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.exception.IoTException;
import cn.universal.ossm.entity.SysOss;
import cn.universal.ossm.service.ISysOssService;
import cn.universal.persistence.entity.IoTOtaPackage;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** OTA升级包管理控制器 */
@RestController
@RequestMapping("/admin/v1/ota/package")
@Tag(name = "OTA升级包管理")
@RequiredArgsConstructor
public class IoTOtaPackageController extends BaseController {

  private final IIoTOtaAdminService otaAdminService;
  private final ISysOssService ossService;

  @Value("${ota.package.upload.hash-max-bytes:209715200}")
  private long hashMaxBytes;

  @PreAuthorize("@ss.hasPermi('system:ota:list')")
  @GetMapping("/list")
  public TableDataInfo<IoTOtaPackage> list(IoTOtaPackage query) {
    startPage();
    List<IoTOtaPackage> list =
        otaAdminService.selectPackageList(
            query, loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId(), isAdmin());
    return getDataTable(list);
  }

  @PreAuthorize("@ss.hasPermi('system:ota:list')")
  @GetMapping("/{id}")
  public AjaxResult<IoTOtaPackage> getInfo(@PathVariable("id") Long id) {
    return AjaxResult.success(otaAdminService.selectPackageById(id));
  }

  @PreAuthorize("@ss.hasPermi('system:ota:package:add')")
  @PostMapping
  @Log(title = "新增OTA升级包", businessType = BusinessType.INSERT)
  public AjaxResult<Long> add(@RequestBody IoTOtaPackage otaPackage) {
    return AjaxResult.success(
        "新增成功",
        otaAdminService.savePackage(
            otaPackage, loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId()));
  }

  @PreAuthorize("@ss.hasPermi('system:ota:package:edit')")
  @PutMapping
  @Log(title = "修改OTA升级包", businessType = BusinessType.UPDATE)
  public AjaxResult<Long> edit(@RequestBody IoTOtaPackage otaPackage) {
    return AjaxResult.success(
        "修改成功",
        otaAdminService.savePackage(
            otaPackage, loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId()));
  }

  @PreAuthorize("@ss.hasPermi('system:ota:package:publish')")
  @PutMapping("/status/{id}")
  @Log(title = "更新OTA升级包状态", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> updateStatus(
      @PathVariable("id") Long id, @RequestParam("status") String status) {
    return toAjax(otaAdminService.updatePackageStatus(id, status));
  }

  @PreAuthorize("@ss.hasPermi('system:ota:package:upload')")
  @PostMapping("/upload")
  @Log(title = "上传OTA升级包文件", businessType = BusinessType.IMPORT)
  public AjaxResult<Map<String, Object>> upload(@RequestPart("file") MultipartFile file)
      throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IoTException("上传文件不能为空");
    }
    boolean hashSkipped = shouldSkipHash(file);
    Map<String, String> hashes = hashSkipped ? new LinkedHashMap<>() : calculateHashes(file);
    var user = loginIoTUnionUser(SecurityUtils.getUnionId());
    String unionId = user.getUnionId();
    SysOss oss = ossService.upload(file, unionId, user.isAdmin() ? "system" : resolveTenantId(user));
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("ossId", oss.getOssId());
    result.put("url", oss.getUrl());
    result.put("fileName", file.getOriginalFilename());
    result.put("fileSize", file.getSize());
    result.put("contentType", file.getContentType());
    result.put("md5", hashes.get("md5"));
    result.put("sha256", hashes.get("sha256"));
    result.put("sha512", hashes.get("sha512"));
    result.put("hashSkipped", hashSkipped);
    result.put("hashMaxBytes", hashMaxBytes);
    if (hashSkipped) {
      result.put("hashSkipReason", buildHashSkipReason(file));
    }
    return AjaxResult.success(result);
  }

  private boolean shouldSkipHash(MultipartFile file) {
    return hashMaxBytes <= 0 || file.getSize() > hashMaxBytes;
  }

  private String buildHashSkipReason(MultipartFile file) {
    if (hashMaxBytes <= 0) {
      return "服务端已关闭上传时自动计算校验值，请手工填写SHA256/SHA512";
    }
    return "文件大小"
        + file.getSize()
        + "字节超过自动计算阈值"
        + hashMaxBytes
        + "字节，请手工填写SHA256/SHA512";
  }

  private Map<String, String> calculateHashes(MultipartFile file) throws IOException {
    try {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
      byte[] buffer = new byte[8192];
      int read;
      // 这里的 hash 只用于上传后辅助回填表单；大文件会跳过，发布/下发阶段仍强制要求 SHA256/SHA512。
      // 即使未跳过也必须流式计算，避免把整个固件读进堆内存。
      try (InputStream inputStream = file.getInputStream()) {
        while ((read = inputStream.read(buffer)) != -1) {
          md5.update(buffer, 0, read);
          sha256.update(buffer, 0, read);
          sha512.update(buffer, 0, read);
        }
      }
      Map<String, String> hashes = new LinkedHashMap<>();
      hashes.put("md5", HexFormat.of().formatHex(md5.digest()));
      hashes.put("sha256", HexFormat.of().formatHex(sha256.digest()));
      hashes.put("sha512", HexFormat.of().formatHex(sha512.digest()));
      return hashes;
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("系统缺少固件校验算法", e);
    }
  }

  @PreAuthorize("@ss.hasPermi('system:ota:package:remove')")
  @DeleteMapping("/{id}")
  @Log(title = "删除OTA升级包", businessType = BusinessType.DELETE)
  public AjaxResult<Void> remove(@PathVariable("id") Long id) {
    return toAjax(otaAdminService.deletePackageById(id));
  }
}
