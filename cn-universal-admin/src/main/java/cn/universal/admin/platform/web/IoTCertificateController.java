package cn.universal.admin.platform.web;

import static cn.universal.common.constant.IoTConstant.CERT_DEFAULT_KEY;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.admin.platform.service.ICertificateService;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.enums.OperatorType;
import cn.universal.persistence.dto.IoTCertificateQueryDTO;
import cn.universal.persistence.dto.IoTCertificateOptionDTO;
import cn.universal.persistence.entity.IoTCertificate;
import cn.universal.persistence.entity.IoTUser;
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
import java.util.List;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/v1/certificate")
@Tag(name = "IoT证书管理", description = "IoT设备证书管理接口")
public class IoTCertificateController extends BaseController {

  @Autowired private ICertificateService certificateService;

  /**
   * 证书多条件查询，参数用DTO封装
   *
   * @param query 证书查询条件DTO
   * @return 证书列表
   */
  @Operation(summary = "证书多条件查询", description = "根据多个条件查询证书列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取证书列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo<IoTCertificate> list(
      @RequestParam(defaultValue = "1") Integer pageNum,
      @RequestParam(defaultValue = "10") Integer pageSize,
      IoTCertificateQueryDTO query) {
    String userId = SecurityUtils.getUnionId();
    IoTUser currentUser = currentTenantUser();
    boolean isAdmin = currentUser != null && currentUser.isAdmin();
    List<IoTCertificate> list =
        certificateService.searchCertificates(
            query.getName(),
            userId,
            query.getSslKey(),
            query.getExpireStart(),
            query.getExpireEnd(),
            isAdmin);
    if (CollUtil.isNotEmpty(list)) {
      list.forEach(this::sanitize);
    }
    return getDataTable(list, pageNum, pageSize);
  }

  /**
   * 根据SSL Key获取证书
   *
   * @param sslKey SSL密钥
   * @return 证书信息
   */
  @Operation(summary = "根据SSL Key获取证书", description = "根据SSL Key获取指定证书信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取证书信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = IoTCertificate.class)))
  @Parameter(name = "sslKey", description = "SSL密钥", required = true)
  @GetMapping("/get")
  public IoTCertificate get(@RequestParam String sslKey) {
    IoTCertificate cert = certificateService.getBySslKey(sslKey);
    if (cert == null || !canAccessCreatorResource(currentTenantUser(), cert.getCreateUser())) {
      return null; // 或抛出无权限异常
    }
    return sanitize(cert);
  }

  /** Secret-free certificate options for OPC UA endpoint configuration. */
  @Operation(summary = "查询可绑定的证书选项")
  @GetMapping("/options")
  public AjaxResult<List<IoTCertificateOptionDTO>> options(
      @RequestParam(defaultValue = "OPCUA") String protocol) {
    IoTUser user = currentTenantUser();
    boolean admin = user != null && user.isAdmin();
    return AjaxResult.success(
        certificateService.listOptions(
            protocol, SecurityUtils.getUnionId(), resolveTenantId(user), admin));
  }

  /**
   * 添加证书
   *
   * @param cert 证书信息
   * @return 添加结果
   */
  @Operation(summary = "添加证书", description = "添加新的证书信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功添加证书",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/add")
  @Log(
      title = "添加证书",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = false)
  public AjaxResult add(@RequestBody IoTCertificate cert) {
    // 仅管理员可添加默认证书
    if (CERT_DEFAULT_KEY.equals(cert.getSslKey())
        && (SecurityUtils.getIoTUnionUser() == null
            || !SecurityUtils.getIoTUnionUser().isAdmin())) {
      return AjaxResult.error("无权限：只有管理员可操作默认证书");
    }
    if (cert != null && StrUtil.isBlank(cert.getSslKey())) {
      cert.setSslKey(IdUtil.objectId());
    }
    cert.setCreateUser(SecurityUtils.getUnionId());
    cert.setTenantId(resolveTenantId(currentTenantUser()));
    certificateService.addCertificate(cert);
    return AjaxResult.success("添加成功");
  }

  /**
   * 更新证书
   *
   * @param cert 证书信息
   * @return 更新结果
   */
  @Operation(summary = "更新证书", description = "更新指定证书信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功更新证书",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/update")
  @Log(
      title = "更新证书",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = false)
  public AjaxResult update(@RequestBody IoTCertificate cert) {
    // 仅管理员可更新默认证书
    if (CERT_DEFAULT_KEY.equals(cert.getSslKey())
        && (SecurityUtils.getIoTUnionUser() == null
            || !SecurityUtils.getIoTUnionUser().isAdmin())) {
      return AjaxResult.error("无权限：只有管理员可操作默认证书");
    }
    IoTUser currentUser = currentTenantUser();
    IoTCertificate old = certificateService.getBySslKey(cert.getSslKey());
    if (old == null || !canAccessCreatorResource(currentUser, old.getCreateUser())) {
      return AjaxResult.error("无权限");
    }
    cert.setCreateUser(null);
    cert.setTenantId(null);
    certificateService.updateCertificate(cert);
    return AjaxResult.success("更新成功");
  }

  /**
   * 删除证书
   *
   * @param id 证书ID
   * @return 删除结果
   */
  @Operation(summary = "删除证书", description = "根据ID删除指定证书")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除证书",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "证书ID", required = true)
  @PostMapping("/delete")
  @Log(
      title = "删除证书",
      businessType = BusinessType.DELETE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult delete(@RequestParam Long id) {
    IoTCertificate cert =
        certificateService.listAll().stream()
            .filter(c -> c.getId().equals(id))
            .findFirst()
            .orElse(null);
    // 仅管理员可删除默认证书
    if (cert != null && CERT_DEFAULT_KEY.equals(cert.getSslKey())) {
      return AjaxResult.error("默认证书，只能编辑不能删除");
    }
    if (cert == null || !canAccessCreatorResource(currentTenantUser(), cert.getCreateUser())) {
      return AjaxResult.error("无权限");
    }
    List<String> references = certificateService.findReferences(cert.getSslKey());
    if (!references.isEmpty()) {
      return AjaxResult.error("证书仍被引用，不能删除: " + String.join(", ", references));
    }
    certificateService.deleteCertificate(id);
    return AjaxResult.success("删除成功");
  }

  /**
   * 证书内容直接上传接口
   *
   * @param sslKey SSL密钥
   * @param certContent 证书内容
   * @param keyContent 私钥内容
   * @param name 证书名称
   * @return 上传结果
   */
  @Operation(summary = "证书内容直接上传", description = "通过直接提供证书内容的方式上传证书")
  @ApiResponse(
      responseCode = "200",
      description = "成功上传证书",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "sslKey", description = "SSL密钥", required = true)
  @Parameter(name = "certContent", description = "证书内容", required = true)
  @Parameter(name = "keyContent", description = "私钥内容", required = true)
  @Parameter(name = "name", description = "证书名称", required = false)
  @PostMapping("/uploadContent")
  @Log(
      title = "证书内容直接上传",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = false)
  public AjaxResult uploadContent(
      @RequestParam("sslKey") String sslKey,
      @RequestParam("certContent") String certContent,
      @RequestParam("keyContent") String keyContent,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "certificateType", defaultValue = "GENERIC") String certificateType,
      @RequestParam(value = "trustMode", required = false) String trustMode,
      @RequestParam(value = "trustCertContent", required = false) String trustCertContent) {
    // 仅管理员可上传默认证书
    if (CERT_DEFAULT_KEY.equals(sslKey)
        && (SecurityUtils.getIoTUnionUser() == null
            || !SecurityUtils.getIoTUnionUser().isAdmin())) {
      return AjaxResult.error("无权限：只有管理员可操作默认证书");
    }
    IoTCertificate cert =
        IoTCertificate.builder()
            .sslKey(sslKey)
            .certContent(certContent)
            .keyContent(keyContent)
            .name(name)
            .certificateType(certificateType)
            .trustMode(trustMode)
            .trustCertContent(trustCertContent)
            .tenantId(resolveTenantId(currentTenantUser()))
            .createUser(SecurityUtils.getUnionId())
            .build();
    certificateService.addCertificate(cert);
    return AjaxResult.success("上传成功");
  }

  /**
   * 证书文件上传接口，支持pem/key文件，自动保存内容到数据库
   *
   * @param sslKey SSL密钥
   * @param files 上传的文件列表
   * @param name 证书名称
   * @return 上传结果
   */
  @Operation(summary = "证书文件上传", description = "通过上传证书文件的方式添加证书，支持pem/key文件")
  @ApiResponse(
      responseCode = "200",
      description = "成功上传证书文件",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "sslKey", description = "SSL密钥", required = true)
  @Parameter(name = "files", description = "上传的证书文件列表", required = true)
  @Parameter(name = "name", description = "证书名称", required = false)
  @PostMapping("/uploadFile")
  @Log(
      title = "证书文件上传",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = false)
  public AjaxResult uploadFile(
      @RequestParam("sslKey") String sslKey,
      @RequestParam("file") List<MultipartFile> files,
      @RequestParam(value = "name", required = false) String name) {
    // 仅管理员可上传默认证书
    if (CERT_DEFAULT_KEY.equals(sslKey)
        && (SecurityUtils.getIoTUnionUser() == null
            || !SecurityUtils.getIoTUnionUser().isAdmin())) {
      return AjaxResult.error("无权限：只有管理员可操作默认证书");
    }
    String certContent = null;
    String keyContent = null;
    try {
      for (MultipartFile file : files) {
        String filename = file.getOriginalFilename();
        String content = new String(file.getBytes());
        if (filename != null && filename.endsWith(".pem")) {
          certContent = content;
        } else if (filename != null
            && (filename.endsWith(".key") || filename.contains("private"))) {
          keyContent = content;
        }
      }
      if (certContent == null || keyContent == null) {
        return AjaxResult.error("请同时上传pem和key文件");
      }
      IoTCertificate cert =
          IoTCertificate.builder()
              .sslKey(sslKey)
              .certContent(certContent)
              .keyContent(keyContent)
              .name(name)
              .certificateType("GENERIC")
              .tenantId(resolveTenantId(currentTenantUser()))
              .createUser(SecurityUtils.getUnionId())
              .build();
      certificateService.addCertificate(cert);
      return AjaxResult.success("上传并保存成功", sanitize(cert));
    } catch (Exception e) {
      return AjaxResult.error("上传失败: " + e.getMessage());
    }
  }

  /** Uploads one unambiguous OPC UA identity and trust bundle under a single sslKey. */
  @Operation(summary = "上传OPC UA证书包")
  @PostMapping("/uploadOpcuaBundle")
  @Log(
      title = "上传OPC UA证书包",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = false)
  public AjaxResult uploadOpcuaBundle(
      @RequestParam("sslKey") String sslKey,
      @RequestParam("clientCertificate") MultipartFile clientCertificate,
      @RequestParam("clientPrivateKey") MultipartFile clientPrivateKey,
      @RequestParam("trustCertificate") MultipartFile trustCertificate,
      @RequestParam("trustMode") String trustMode,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "keyPassword", required = false) String keyPassword) {
    try {
      IoTCertificate cert =
          IoTCertificate.builder()
              .sslKey(StrUtil.blankToDefault(sslKey, IdUtil.objectId()))
              .name(name)
              .certificateType("OPCUA_BUNDLE")
              .trustMode(trustMode)
              .certContent(new String(clientCertificate.getBytes(), StandardCharsets.UTF_8))
              .keyContent(new String(clientPrivateKey.getBytes(), StandardCharsets.UTF_8))
              .keyPassword(keyPassword)
              .trustCertContent(new String(trustCertificate.getBytes(), StandardCharsets.UTF_8))
              .tenantId(resolveTenantId(currentTenantUser()))
              .createUser(SecurityUtils.getUnionId())
              .build();
      certificateService.addCertificate(cert);
      return AjaxResult.success("OPC UA证书包上传成功");
    } catch (Exception ex) {
      return AjaxResult.error("OPC UA证书包上传失败: " + ex.getMessage());
    }
  }

  private IoTCertificate sanitize(IoTCertificate certificate) {
    if (certificate == null) {
      return null;
    }
    certificate.setCertContent(null);
    certificate.setKeyContent(null);
    certificate.setCertPassword(null);
    certificate.setKeyPassword(null);
    certificate.setTrustCertContent(null);
    return certificate;
  }
}
