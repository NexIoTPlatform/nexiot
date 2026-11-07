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

package cn.universal.admin.system.oss;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.admin.system.service.ISysDictTypeService;
import cn.universal.common.exception.IoTException;
import cn.universal.common.utils.FileUtils;
import cn.universal.ossm.entity.SysOss;
import cn.universal.ossm.entity.SysOssConfig;
import cn.universal.ossm.entity.bo.SysOssBo;
import cn.universal.ossm.oss.enumd.CloudServiceEnumd;
import cn.universal.ossm.oss.context.OssTenantResolver;
import cn.universal.ossm.service.ISysOssConfigService;
import cn.universal.ossm.service.ISysOssService;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 文件上传 控制层 @Author Lion Li */
@Validated
@Tag(name = "OSS云存储控制器", description = "OSS云存储管理")
@RestController
@RequestMapping("/admin/system/oss")
@Slf4j
public class SysOssController extends BaseController {

  private static final String OSS_STORAGE_PROVIDER_DICT_TYPE = "oss_storage_provider";

  @Resource private ISysOssService iSysOssService;

  @Resource private ISysOssConfigService sysOssConfigService;

  @Resource private ISysDictTypeService sysDictTypeService;

  @Value("${codec.path:cn-universal}")
  private String prePath;

  /** 查询OSS云存储列表 */
  @Operation(summary = "查询OSS云存储列表", description = "根据查询条件获取OSS云存储列表")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "成功获取OSS云存储列表",
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              mediaType = "application/json",
              schema =
                  @io.swagger.v3.oas.annotations.media.Schema(
                      implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo list(@Validated SysOssBo bo) {
    bo.setTenantId(currentResourceTenantId());
    startPage();
    return getDataTable(iSysOssService.queryPageList(bo));
  }

  /** 获取OSS存储配置状态 */
  @GetMapping("/config")
  public AjaxResult<Map<String, Object>> config() {
    Map<String, Object> result = new HashMap<>(8);
    String tenantId = currentConfigTenantId();
    String current = sysOssConfigService.getActiveProvider(tenantId);
    result.put("current", current);
    result.put("providers", providerOptions());
    result.put("configs", sysOssConfigService.listMaskedConfigs(tenantId));
    return AjaxResult.success(result);
  }

  /** 获取单个OSS存储配置 */
  @GetMapping("/config/{provider}")
  public AjaxResult<SysOssConfig> getStorageConfig(@PathVariable String provider) {
    return AjaxResult.success(sysOssConfigService.getMaskedConfig(provider, currentConfigTenantId()));
  }

  /** 保存OSS存储配置 */
  @PutMapping("/config")
  public AjaxResult<SysOssConfig> saveStorageConfig(@RequestBody SysOssConfig config) {
    return AjaxResult.success(
        sysOssConfigService.saveConfig(config, SecurityUtils.getUnionId(), currentConfigTenantId()));
  }

  /** 切换当前上传使用的OSS服务商 */
  @PutMapping("/config/switch")
  public AjaxResult<Void> switchStorage(@RequestBody Map<String, String> body) {
    String type = body == null ? null : body.get("type");
    sysOssConfigService.activate(type, SecurityUtils.getUnionId(), currentConfigTenantId());
    return AjaxResult.success();
  }

  /** 测试指定OSS服务商配置是否可用 */
  @PostMapping("/config/test")
  public AjaxResult<Map<String, Object>> testStorage(@RequestBody Map<String, String> body) {
    String tenantId = currentConfigTenantId();
    String type = body == null ? sysOssConfigService.getActiveProvider(tenantId) : body.get("type");
    sysOssConfigService.test(type, tenantId);
    Map<String, Object> result = new HashMap<>(4);
    SysOssConfig config = sysOssConfigService.getMaskedConfig(type, tenantId);
    result.put("type", type);
    result.put("endpoint", config.getPublicEndpoint() == null ? config.getEndpoint() : config.getPublicEndpoint());
    return AjaxResult.success(result);
  }

  /** 上传OSS云存储 */
  @Operation(summary = "上传OSS云存储", description = "上传文件到OSS云存储")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "成功上传OSS云存储",
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              mediaType = "application/json",
              schema =
                  @io.swagger.v3.oas.annotations.media.Schema(implementation = AjaxResult.class)))
  @PostMapping("/upload")
  public AjaxResult<Map<String, Object>> upload(
      @Parameter(name = "file", description = "文件", required = true) @RequestPart("file")
          MultipartFile file) {
    if (file.isEmpty()) {
      throw new IoTException("上传文件不能为空");
    }
    List<SysOss> sysOsses =
        iSysOssService.queryPageList(
            SysOssBo.builder()
                .originalName(prePath + file.getOriginalFilename())
                .tenantId(currentResourceTenantId())
                .build());
    IoTUser user = loginIoTUnionUser(SecurityUtils.getUnionId());
    String unionId = user.getUnionId();
    String tenantId = currentResourceTenantId(user);
    if (!CollectionUtils.isEmpty(sysOsses)) {
      // 过滤出不是自己创建的同名文件
      List<SysOss> existOss =
          sysOsses.stream()
              .filter(sysOss -> !unionId.equals(sysOss.getCreateBy()))
              .collect(Collectors.toList());
      if (!CollectionUtils.isEmpty(existOss)) {
        throw new IoTException("已存在同名文件！请修改后重试！");
      }
    }
    SysOss oss = iSysOssService.upload(file, unionId, tenantId);
    Map<String, Object> map = new HashMap<>(4);
    map.put("url", oss.getUrl());
    String name = oss.getOriginalName();
    map.put("fileName", name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf(".")));

    // 如果是JAR文件，尝试自动检测驱动类
    if (file.getOriginalFilename() != null
        && file.getOriginalFilename().toLowerCase().endsWith(".jar")) {
      try {
        java.util.List<java.util.Map<String, Object>> candidates = extractCodecCandidatesFromJar(file);
        if (candidates != null && !candidates.isEmpty()) {
          map.put("codecCandidates", candidates);
          // 如果只有一个候选类，则自动填充为默认包名
          if (candidates.size() == 1) {
            java.util.Map<String, Object> candidate = candidates.get(0);
            Object className = candidate.get("className");
            if (className != null) {
              map.put("packageName", className.toString());
            }
          }
        }
      } catch (Exception e) {
        // 检测失败不影响上传，只记录日志
        log.warn("自动检测JAR驱动类失败: {}", e.getMessage());
      }
    }

    return AjaxResult.success(map);
  }

  /**
   * 从DrJAR文件中提取主类名
   *
   * @param file JAR文件
   * @return 主类名，如果未找到则返回null
   */
  private String extractMainClassFromJar(MultipartFile file) {
    try (java.util.jar.JarInputStream jarStream =
        new java.util.jar.JarInputStream(file.getInputStream())) {
      java.util.jar.Manifest manifest = jarStream.getManifest();
      if (manifest != null) {
        java.util.jar.Attributes attributes = manifest.getMainAttributes();
        String mainClass = attributes.getValue("Main-Class");
        if (mainClass != null && !mainClass.isEmpty()) {
          return mainClass;
        }
      }

      // 如果MANIFEST中没有Main-Class，尝试找第一个实现JarDriverCodecService的类
      java.util.jar.JarEntry entry;
      while ((entry = jarStream.getNextJarEntry()) != null) {
        String entryName = entry.getName();
        if (entryName.endsWith(".class") && !entryName.contains("$")) {
          String className = entryName.replace("/", ".").replace(".class", "");
          // 返回第一个非内部类
          if (!className.contains(".")) {
            continue; // 跳过默认包的类
          }
          return className;
        }
      }
    } catch (Exception e) {
      log.error("解析JAR文件失败", e);
    }
    return null;
  }

  /**
   * 从上传的JAR文件中扫描可能的驱动类候选
   *
   * <p>候选类判定规则：
   * <ul>
   *   <li>非接口、非抽象类、非枚举、非内部类
   *   <li>同时具有 preDecode(String,Object)、encode(String,Object)、decode(String,Object) 且返回值为
   *       String
   * </ul>
   *
   * @param file 上传的JAR文件
   * @return 驱动类候选列表
   */
  private java.util.List<java.util.Map<String, Object>> extractCodecCandidatesFromJar(
      MultipartFile file) {
    java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
    java.io.File tempFile = null;
    try {
      tempFile = java.io.File.createTempFile("nexiot-driver-", ".jar");
      file.transferTo(tempFile);

      java.net.URL jarUrl = tempFile.toURI().toURL();
      try (java.net.URLClassLoader loader =
              new java.net.URLClassLoader(new java.net.URL[] {jarUrl}, this.getClass().getClassLoader());
          java.util.jar.JarFile jarFile = new java.util.jar.JarFile(tempFile)) {

        java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          java.util.jar.JarEntry entry = entries.nextElement();
          String entryName = entry.getName();
          if (!entryName.endsWith(".class") || entryName.contains("$")) {
            continue;
          }

          String className = entryName.replace("/", ".").replace(".class", "");
          try {
            Class<?> clazz = loader.loadClass(className);
            if (!isCodecCandidateClass(clazz)) {
              continue;
            }

            java.util.Map<String, Object> candidate = new java.util.HashMap<>();
            candidate.put("className", className);
            candidate.put("simpleName", clazz.getSimpleName());

            // 尝试获取 productKey 和 desc
            try {
              Object instance = clazz.getDeclaredConstructor().newInstance();
              try {
                java.lang.reflect.Method productKeyMethod = clazz.getMethod("productKey");
                if (productKeyMethod.getReturnType() == String.class) {
                  Object value = productKeyMethod.invoke(instance);
                  if (value != null) {
                    candidate.put("productKey", value.toString());
                  }
                }
              } catch (NoSuchMethodException ignore) {
                // 没有productKey方法，忽略
              }

              try {
                java.lang.reflect.Method descMethod = clazz.getMethod("desc");
                if (descMethod.getReturnType() == String.class) {
                  Object value = descMethod.invoke(instance);
                  if (value != null) {
                    candidate.put("desc", value.toString());
                  }
                }
              } catch (NoSuchMethodException ignore) {
                // 没有desc方法，忽略
              }
            } catch (Exception e) {
              log.debug("实例化候选驱动类失败: {}", className, e);
            }

            result.add(candidate);
          } catch (ClassNotFoundException | NoClassDefFoundError e) {
            log.debug("无法加载类 {}: {}", className, e.getMessage());
          }
        }
      }
    } catch (Exception e) {
      log.error("解析JAR文件获取驱动类失败", e);
    } finally {
      if (tempFile != null && tempFile.exists()) {
        if (!tempFile.delete()) {
          log.warn("删除临时JAR文件失败: {}", tempFile.getAbsolutePath());
        }
      }
    }
    return result;
  }

  /**
   * 判断一个Class是否符合驱动类候选条件
   *
   * @param clazz 目标类
   * @return 是否为候选驱动类
   */
  private boolean isCodecCandidateClass(Class<?> clazz) {
    if (clazz.isInterface() || java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())
        || clazz.isEnum()) {
      return false;
    }

    try {
      java.lang.reflect.Method preDecode =
          clazz.getMethod("preDecode", String.class, Object.class);
      java.lang.reflect.Method encode = clazz.getMethod("encode", String.class, Object.class);
      java.lang.reflect.Method decode = clazz.getMethod("decode", String.class, Object.class);

      return preDecode.getReturnType() == String.class
          && encode.getReturnType() == String.class
          && decode.getReturnType() == String.class;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  @Operation(summary = "下载OSS云存储", description = "根据OSS ID下载文件")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "成功下载OSS云存储文件",
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              mediaType = "application/octet-stream",
              schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = Void.class)))
  @Parameter(name = "ossId", description = "OSS ID", required = true)
  @GetMapping("/download/{ossId}")
  public void download(@PathVariable Long ossId, HttpServletResponse response) throws IOException {
    SysOss sysOss = iSysOssService.getById(ossId);
    if (sysOss == null) {
      throw new IoTException("文件数据不存在!");
    }
    assertOssAccess(sysOss);
    response.reset();
    response.addHeader("Access-Control-Allow-Origin", "*");
    response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
    FileUtils.setAttachmentResponseHeader(
        response, URLEncoder.encode(sysOss.getOriginalName(), StandardCharsets.UTF_8.toString()));
    response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE + "; charset=UTF-8");
    long data = HttpUtil.download(sysOss.getUrl(), response.getOutputStream(), false);
    response.setContentLength(Convert.toInt(data));
  }

  /** 预览OSS云存储资源 */
  @GetMapping("/resource/{ossId}")
  public void resource(@PathVariable Long ossId, HttpServletResponse response) throws IOException {
    SysOss sysOss = iSysOssService.getById(ossId);
    if (sysOss == null) {
      throw new IoTException("文件数据不存在!");
    }
    assertOssAccess(sysOss);
    response.reset();
    response.addHeader("Access-Control-Allow-Origin", "*");
    response.setContentType(resolveContentType(sysOss.getFileSuffix()));
    HttpUtil.download(sysOss.getUrl(), response.getOutputStream(), false);
  }

  /** 删除OSS云存储 */
  @Operation(summary = "删除OSS云存储", description = "根据OSS ID数组删除文件")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "成功删除OSS云存储",
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              mediaType = "application/json",
              schema =
                  @io.swagger.v3.oas.annotations.media.Schema(implementation = AjaxResult.class)))
  @Parameter(name = "ossIds", description = "OSS ID数组", required = true)
  @DeleteMapping("/{ossIds}")
  public AjaxResult<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ossIds) {
    Arrays.stream(ossIds)
        .map(iSysOssService::getById)
        .forEach(
            oss -> {
              if (oss == null) {
                throw new IoTException("文件数据不存在!");
              }
              assertOssAccess(oss);
            });
    return toAjax(iSysOssService.deleteWithValidByIds(Arrays.asList(ossIds), true) ? 1 : 0);
  }

  private String currentConfigTenantId() {
    return OssTenantResolver.resolveConfigTenantId(currentTenantUser());
  }

  private String currentResourceTenantId() {
    return currentResourceTenantId(currentTenantUser());
  }

  private String currentResourceTenantId(IoTUser user) {
    if (user == null) {
      return null;
    }
    if (user.isAdmin()) {
      return "system";
    }
    return resolveTenantId(user);
  }

  private void assertOssAccess(SysOss sysOss) {
    IoTUser currentUser = currentTenantUser();
    if (currentUser == null || currentUser.isAdmin()) {
      return;
    }
    String currentTenantId = resolveTenantId(currentUser);
    String resourceTenantId = StrUtil.blankToDefault(sysOss.getTenantId(), resolveTenantId(sysOss.getCreateBy()));
    if (!StrUtil.equals(currentTenantId, resourceTenantId)) {
      throw new IoTException("无权访问该文件");
    }
  }

  private List<Map<String, String>> providerOptions() {
    Map<String, String> dictLabels = new LinkedHashMap<>();
    List<SysDictData> dictData = sysDictTypeService.selectDictDataByType(OSS_STORAGE_PROVIDER_DICT_TYPE);
    if (CollUtil.isNotEmpty(dictData)) {
      dictData.forEach(item -> dictLabels.put(item.getDictValue(), item.getDictLabel()));
    }
    return Arrays.stream(CloudServiceEnumd.values())
        .map(item -> providerOption(item.getValue(), dictLabels.get(item.getValue())))
        .collect(Collectors.toList());
  }

  private Map<String, String> providerOption(String provider, String label) {
    Map<String, String> option = new LinkedHashMap<>();
    option.put("label", label == null ? provider : label);
    option.put("value", provider);
    return option;
  }

  private String resolveContentType(String suffix) {
    if (suffix == null) {
      return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
    return switch (suffix.toLowerCase()) {
      case ".jpg", ".jpeg" -> MediaType.IMAGE_JPEG_VALUE;
      case ".png" -> MediaType.IMAGE_PNG_VALUE;
      case ".gif" -> MediaType.IMAGE_GIF_VALUE;
      case ".webp" -> "image/webp";
      case ".svg" -> "image/svg+xml";
      case ".pdf" -> MediaType.APPLICATION_PDF_VALUE;
      case ".mp4" -> "video/mp4";
      default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
    };
  }
}
