package cn.universal.admin.video.web;

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.video.service.VideoDeviceImportService;
import cn.universal.admin.video.service.VideoPlatformDeviceCleanService;
import cn.universal.admin.video.service.VideoPlatformInstanceService;
import cn.universal.admin.video.sync.DeviceSyncHandler;
import cn.universal.admin.video.vo.VideoPlatformChannelVO;
import cn.universal.admin.video.vo.VideoPlatformDeviceVO;
import cn.universal.common.annotation.Log;
import cn.universal.common.domain.R;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.enums.OperatorType;
import cn.universal.common.exception.BaseException;
import cn.universal.common.utils.JsonDesensitizationUtil;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.VideoPlatformDeviceQuery;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 视频平台实例与设备接口（先行联调，逐步接入真实适配器） */
@RestController
@RequestMapping("/api/video/platforms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "视频平台管理", description = "视频平台实例与设备管理接口")
public class VideoPlatformController extends BaseController {
  private final VideoPlatformInstanceService instanceService;
  private final VideoDeviceImportService deviceImportService;
  private final VideoPlatformDeviceCleanService cleanService;
  private final ObjectMapper objectMapper;
  private final cn.universal.admin.video.service.VideoSdkStorageService sdkStorageService;

  private final cn.universal.admin.video.service.VideoSdkRecordingTaskService sdkRecordingTasks;
  @jakarta.annotation.Resource private cn.universal.admin.video.service.VideoOperationAccess videoOperationAccess;
  @jakarta.annotation.Resource private cn.universal.admin.video.service.HikIsupRegistrationSync hikIsupRegistrationSync;
  @jakarta.annotation.Resource private cn.universal.admin.video.service.VideoSdkConfigurationTaskService sdkConfigurationTasks;

  // 同步限频缓存：记录每个实例最后一次强制同步时间戳
  private static final java.util.concurrent.ConcurrentHashMap<String, Long> SYNC_RATE_LIMIT =
      new java.util.concurrent.ConcurrentHashMap<>();

  /**
   * 平台实例列表
   *
   * @return 平台实例列表
   */
  @Operation(summary = "查询平台实例列表", description = "获取所有视频平台实例信息列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取平台实例列表",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @GetMapping("")
  public R<List<VideoPlatformInstance>> listPlatforms() {
    IoTUser user = loginIoTUnionUser(SecurityUtils.getUnionId());
    List<VideoPlatformInstance> all = instanceService.listAll();
    if (user != null && !user.isAdmin()) {
      all = all.stream().filter(instance -> canAccessInstance(user, instance)).toList();
    }
    // 对敏感字段进行脱敏处理
    all.forEach(this::maskSensitiveFields);
    return R.ok(all);
  }

  /**
   * 根据instanceKey查询实例
   *
   * @param instanceKey 平台实例Key
   * @return 平台实例信息
   */
  @Operation(summary = "根据instanceKey查询实例", description = "根据平台实例Key获取特定平台实例信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取平台实例信息",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @GetMapping("/{instanceKey}")
  public R<VideoPlatformInstance> getInstance(@PathVariable("instanceKey") String instanceKey) {
    assertInstancePerm(instanceKey);
    VideoPlatformInstance instance = instanceService.getByInstanceKey(instanceKey);
    // 对敏感字段进行脱敏处理
    maskSensitiveFields(instance);
    return R.ok(instance);
  }

  /**
   * 创建/更新平台实例（若id为空则创建，否则更新）
   *
   * @param body 平台实例信息
   * @return 保存后的平台实例信息
   */
  @Operation(summary = "创建/更新平台实例", description = "创建或更新视频平台实例信息，若id为空则创建，否则更新")
  @ApiResponse(
      responseCode = "200",
      description = "成功保存平台实例信息",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @PreAuthorize("#body.id == null ? @ss.hasPermi('video:platform:add') : @ss.hasPermi('video:platform:edit')")
  @PostMapping("")
  @Log(title = "创建/更新视频平台实例", businessType = BusinessType.INSERT)
  public R<VideoPlatformInstance> savePlatform(@RequestBody VideoPlatformInstance body) {
    IoTUser user = loginIoTUnionUser(SecurityUtils.getUnionId());
    rejectSuperAdminCreate("创建/更新视频平台实例");
    VideoPlatformInstance saved;
    if (body.getId() == null) {
      body.setCreatorId(SecurityUtils.getUnionId());
      body.setTenantId(resolveTenantId(user));
      saved = instanceService.create(body);
    } else {
      VideoPlatformInstance existing = instanceService.getByInstanceKey(body.getInstanceKey());
      if (existing == null) throw new BaseException("平台实例不存在");
      assertInstancePerm(existing.getInstanceKey());
      // 合并auth配置，将前端提交的***替换为原有值
      if (body.getAuth() != null && existing.getAuth() != null) {
        String mergedAuth =
            JsonDesensitizationUtil.mergeAuthConfig(body.getAuth(), existing.getAuth());
        body.setAuth(mergedAuth);
      }
      saved = instanceService.update(body);
    }
    // 尝试应用订阅配置（若启用）
    try {
      if (!"wvp".equals(saved.getPlatformType())) instanceService.applySubscription(saved.getInstanceKey());
    } catch (Exception ignore) {
    }
    maskSensitiveFields(saved);
    return R.ok(saved);
  }

  /**
   * 删除平台实例
   *
   * @param id 平台实例ID
   * @param force 是否强制删除
   * @return 删除结果
   */
  @Operation(summary = "删除平台实例", description = "根据平台实例ID删除指定的平台实例")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除平台实例",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "id", description = "平台实例ID", required = true)
  @Parameter(name = "force", description = "是否强制删除", required = false)
  @PreAuthorize("@ss.hasPermi('video:platform:remove')")
  @DeleteMapping("/{id}")
  @Log(title = "删除视频平台实例", businessType = BusinessType.DELETE)
  public R<Void> deletePlatform(
      @PathVariable("id") Long id,
      @RequestParam(value = "force", required = false, defaultValue = "false") boolean force) {
    VideoPlatformInstance inst = instanceService.getById(id);
    if (inst == null) throw new BaseException("平台实例不存在");
    assertInstancePerm(inst.getInstanceKey());
    instanceService.delete(id, force);
    return R.ok();
  }

  /**
   * 测试连接（占位，后续接入适配器）
   *
   * @param body 请求体参数
   * @return 测试连接结果
   */
  @Operation(summary = "测试平台连接", description = "测试视频平台连接状态（占位，后续接入适配器）")
  @ApiResponse(
      responseCode = "200",
      description = "成功测试平台连接",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @PreAuthorize("@ss.hasPermi('video:platform:test')")
  @PostMapping("/test")
  public R<Map<String, Object>> testConnection(@RequestBody Map<String, Object> body) {
    String instanceKey = (String) body.get("instanceKey");
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.testConnection(instanceKey));
  }

  /**
   * 获取组织树（部分平台支持）
   *
   * @param instanceKey 平台实例Key
   * @param params 查询参数
   * @return 组织树列表
   */
  @Operation(summary = "获取组织树", description = "根据平台实例Key获取组织树信息（部分平台支持）")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取组织树列表",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @GetMapping("/{instanceKey}/orgs")
  public R<List<Map<String, Object>>> listOrganizations(
      @PathVariable("instanceKey") String instanceKey, @RequestParam Map<String, String> params) {
    Map<String, Object> filters = new java.util.HashMap<>(params);
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.listOrganizations(instanceKey, filters));
  }

  /**
   * 获取设备列表
   *
   * @param instanceKey 平台实例Key
   * @param query 查询条件
   * @return 设备列表分页数据
   */
  @Operation(summary = "获取设备列表", description = "实时拉取设备目录，返回分页数据")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备列表",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @GetMapping("/{instanceKey}/devices")
  public TableDataInfo<VideoPlatformDeviceVO> listDevices(
      @PathVariable("instanceKey") String instanceKey, VideoPlatformDeviceQuery query) {
    assertInstancePerm(instanceKey);

    // 设置instanceKey（关键：确保每次请求都有instanceKey）
    query.setInstanceKey(instanceKey);

    // 规范化分页参数
    query.normalizePage();

    // 获取设备列表（Service层会内部处理分页）
    List<VideoPlatformDeviceVO> list = instanceService.listDevices(query);

    // 包装分页数据
    return getDataTable(list);
  }

  /**
   * 订阅平台事件
   *
   * @param instanceKey 平台实例Key
   * @return 订阅结果
   */
  @Operation(summary = "订阅平台事件", description = "订阅指定平台实例的事件通知")
  @ApiResponse(
      responseCode = "200",
      description = "成功订阅平台事件",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @PreAuthorize("@ss.hasPermi('video:platform:subscribe')")
  @PostMapping("/{instanceKey}/subscribe")
  public R<Void> subscribe(@PathVariable("instanceKey") String instanceKey) {
    assertInstancePerm(instanceKey);
    instanceService.applySubscription(instanceKey);
    return R.ok();
  }

  /**
   * 取消订阅平台事件
   *
   * @param instanceKey 平台实例Key
   * @return 取消订阅结果
   */
  @Operation(summary = "取消订阅平台事件", description = "取消订阅指定平台实例的事件通知")
  @ApiResponse(
      responseCode = "200",
      description = "成功取消订阅平台事件",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @PreAuthorize("@ss.hasPermi('video:platform:subscribe')")
  @PostMapping("/{instanceKey}/unsubscribe")
  public R<Void> unsubscribe(@PathVariable("instanceKey") String instanceKey) {
    assertInstancePerm(instanceKey);
    instanceService.cancelSubscription(instanceKey);
    return R.ok();
  }

  @GetMapping("/{instanceKey}/alarm/subscription")
  @PreAuthorize("@ss.hasPermi('video:platform:query')")
  public R<Map<String, Object>> alarmSubscription(@PathVariable String instanceKey) {
    assertInstancePerm(instanceKey);
    VideoPlatformInstance instance = instanceService.getByInstanceKey(instanceKey);
    if (instance != null && "hik_isup".equalsIgnoreCase(instance.getPlatformType())) {
      return R.ok(instanceService.getSdkSubscriptionStatus(instanceKey));
    }
    return R.ok(instanceService.executePlatformOperation(instanceKey, "alarmSubscriptionStatus", Map.of()));
  }

  @PutMapping("/{instanceKey}/alarm/subscription")
  @PreAuthorize("@ss.hasPermi('video:platform:subscribe')")
  public R<Map<String, Object>> saveAlarmSubscription(
      @PathVariable String instanceKey, @RequestBody Map<String, Object> config) {
    assertInstancePerm(instanceKey);
    VideoPlatformInstance instance = instanceService.getByInstanceKey(instanceKey);
    if (instance != null && "hik_isup".equalsIgnoreCase(instance.getPlatformType())) {
      return R.ok(instanceService.saveSdkSubscription(instanceKey, config));
    }
    return R.ok(instanceService.executePlatformOperation(instanceKey, "saveAlarmSubscription", config));
  }

  /** Stable reusable SDK APIs; binary media is served by the gateway/storage directly. */
  @GetMapping("/{instanceKey}/sdk/config/templates")
  @PreAuthorize("@videoOperationAccess.readAllowed()")
  public R<Map<String,Object>> configurationTemplates(@PathVariable String instanceKey) {
    assertInstancePerm(instanceKey);return R.ok(sdkConfigurationTasks.templates(instanceService.getByInstanceKey(instanceKey)));
  }
  @PostMapping("/{instanceKey}/sdk/config/templates")
  @PreAuthorize("@videoOperationAccess.allowed('deviceConfigApply')")
  public R<Map<String,Object>> createConfigurationTemplate(@PathVariable String instanceKey,@RequestBody Map<String,Object> body) {
    assertInstancePerm(instanceKey);return R.ok(sdkConfigurationTasks.createTemplate(instanceService.getByInstanceKey(instanceKey),body,SecurityUtils.getUnionId()));
  }
  @PostMapping("/{instanceKey}/sdk/config/batches")
  @PreAuthorize("@videoOperationAccess.allowed('deviceConfigApply')")
  public R<Map<String,Object>> createConfigurationBatch(@PathVariable String instanceKey,@RequestBody Map<String,Object> body) {
    assertInstancePerm(instanceKey);return R.ok(sdkConfigurationTasks.createBatch(instanceService.getByInstanceKey(instanceKey),body,SecurityUtils.getUnionId()));
  }
  @GetMapping("/{instanceKey}/sdk/config/batches/{id}")
  @PreAuthorize("@videoOperationAccess.readAllowed()")
  public R<Map<String,Object>> configurationBatch(@PathVariable String instanceKey,@PathVariable String id) {
    assertInstancePerm(instanceKey);return R.ok(sdkConfigurationTasks.batch(instanceService.getByInstanceKey(instanceKey),id));
  }
  @GetMapping("/{instanceKey}/sdk/config/batches")
  @PreAuthorize("@videoOperationAccess.readAllowed()")
  public R<Map<String,Object>> configurationBatches(@PathVariable String instanceKey) {
    assertInstancePerm(instanceKey);return R.ok(sdkConfigurationTasks.batches(instanceService.getByInstanceKey(instanceKey)));
  }
  @PostMapping("/{instanceKey}/sdk/config/batches/{id}/cancel")
  @PreAuthorize("@videoOperationAccess.allowed('deviceConfigApply')")
  public R<Map<String,Object>> cancelConfigurationBatch(@PathVariable String instanceKey,@PathVariable String id) {
    assertInstancePerm(instanceKey);return R.ok(sdkConfigurationTasks.cancel(instanceService.getByInstanceKey(instanceKey),id));
  }
  @GetMapping("/{instanceKey}/sdk/operations")
  @PreAuthorize("@videoOperationAccess.readAllowed()")
  public R<Map<String, Object>> sdkOperations(@PathVariable String instanceKey) {
    assertInstancePerm(instanceKey);
    return R.ok(videoOperationAccess.catalog());
  }

  @PostMapping("/{instanceKey}/sdk/recordings/search")
  @PreAuthorize("@videoOperationAccess.allowed('recordingSearch')")
  public R<Map<String, Object>> sdkRecordings(@PathVariable String instanceKey, @RequestBody Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.executePlatformOperation(instanceKey, "recordingSearch", body));
  }

  @PostMapping("/{instanceKey}/sdk/evidence/timeline")
  @PreAuthorize("@videoOperationAccess.allowed('evidenceTimeline')")
  public R<Map<String, Object>> sdkEvidence(@PathVariable String instanceKey, @RequestBody Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.executePlatformOperation(instanceKey, "evidenceTimeline", body));
  }

  @PostMapping("/{instanceKey}/sdk/health/check")
  @PreAuthorize("@videoOperationAccess.allowed('deviceHealth')")
  public R<Map<String, Object>> sdkHealth(@PathVariable String instanceKey, @RequestBody Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.executePlatformOperation(instanceKey, "deviceHealth", body));
  }

  @PostMapping("/{instanceKey}/sdk/playback/start")
  @PreAuthorize("@videoOperationAccess.allowed('playbackStreamStart')")
  public R<Map<String, Object>> sdkPlayback(@PathVariable String instanceKey, @RequestBody Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.executePlatformOperation(instanceKey, "playbackStreamStart", body));
  }

  @PostMapping("/{instanceKey}/sdk/playback/{action:status|pause|resume|seek|speed|stop}")
  @PreAuthorize("@videoOperationAccess.allowed('playbackStreamStatus')")
  public R<Map<String, Object>> sdkPlaybackControl(@PathVariable String instanceKey,
      @PathVariable String action, @RequestBody Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    String operation = switch (action) {
      case "pause" -> "playbackStreamPause";
      case "resume" -> "playbackStreamResume";
      case "seek" -> "playbackStreamSeek";
      case "speed" -> "playbackStreamSpeed";
      case "stop" -> "playbackStreamStop";
      default -> "playbackStreamStatus";
    };
    return R.ok(instanceService.executePlatformOperation(instanceKey, operation, body));
  }

  @GetMapping("/{instanceKey}/sdk/recording-tasks")
  @PreAuthorize("@videoOperationAccess.readAllowed()")
  public R<Map<String, Object>> recordingTasks(@PathVariable String instanceKey,
      @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String status) {
    assertInstancePerm(instanceKey);
    return R.ok(sdkRecordingTasks.list(instanceService.getByInstanceKey(instanceKey), status, page, size));
  }
  @PostMapping("/{instanceKey}/sdk/recording-tasks")
  @PreAuthorize("@videoOperationAccess.allowed('recordingStart')")
  public R<Map<String, Object>> createRecordingTask(@PathVariable String instanceKey, @RequestBody Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    return R.ok(sdkRecordingTasks.create(instanceService.getByInstanceKey(instanceKey), body, SecurityUtils.getUnionId()));
  }
  @GetMapping("/{instanceKey}/sdk/recording-tasks/{taskId}")
  @PreAuthorize("@videoOperationAccess.readAllowed()")
  public R<Map<String, Object>> recordingTask(@PathVariable String instanceKey, @PathVariable String taskId) {
    assertInstancePerm(instanceKey);
    return R.ok(sdkRecordingTasks.get(instanceService.getByInstanceKey(instanceKey), taskId));
  }
  @PostMapping("/{instanceKey}/sdk/recording-tasks/{taskId}/cancel")
  @PreAuthorize("@videoOperationAccess.allowed('recordingCancel')")
  public R<Map<String, Object>> cancelRecordingTask(@PathVariable String instanceKey, @PathVariable String taskId) {
    assertInstancePerm(instanceKey);
    return R.ok(sdkRecordingTasks.cancel(instanceService.getByInstanceKey(instanceKey), taskId));
  }
  @PostMapping("/{instanceKey}/sdk/recording-tasks/{taskId}/retry")
  @PreAuthorize("@videoOperationAccess.allowed('recordingStart')")
  public R<Map<String, Object>> retryRecordingTask(@PathVariable String instanceKey, @PathVariable String taskId, @RequestBody Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    return R.ok(sdkRecordingTasks.retry(instanceService.getByInstanceKey(instanceKey), taskId, java.util.Objects.toString(body.get("requestId"), java.util.UUID.randomUUID().toString()), SecurityUtils.getUnionId()));
  }

  @GetMapping("/{instanceKey}/sdk/storage")
  @PreAuthorize("@videoOperationAccess.readAllowed()")
  public R<Map<String, Object>> sdkStorage(@PathVariable String instanceKey) {
    assertInstancePerm(instanceKey);
    return R.ok(sdkStorageService.read(instanceService.getByInstanceKey(instanceKey)));
  }

  @PutMapping("/{instanceKey}/sdk/storage")
  @PreAuthorize("@ss.hasPermi('video:platform:edit')")
  public R<Map<String, Object>> configureSdkStorage(@PathVariable String instanceKey,
      @RequestBody Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    return R.ok(sdkStorageService.configure(instanceService.getByInstanceKey(instanceKey), body));
  }

  @Operation(summary = "执行平台特有操作", description = "执行指定平台实例的特有能力，如 ONVIF 发现、重启、校时、预置点等")
  @PreAuthorize("@videoOperationAccess.allowed(#operation)")
  @PostMapping("/{instanceKey}/operations/{operation}")
  @Log(title = "执行视频平台特有操作", businessType = BusinessType.OTHER)
  public R<Map<String, Object>> executePlatformOperation(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("operation") String operation,
      @RequestBody(required = false) Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    if ("saveAlarmSubscription".equals(operation.trim())) {
      throw new BaseException("请使用告警订阅专用接口");
    }
    if ("subscribe".equalsIgnoreCase(operation.trim())
        || "unsubscribe".equalsIgnoreCase(operation.trim())) {
      VideoPlatformInstance instance = instanceService.getByInstanceKey(instanceKey);
      if (instance != null && "hik_isup".equalsIgnoreCase(instance.getPlatformType())) {
        throw new BaseException("请使用告警订阅专用接口");
      }
    }
    return R.ok(instanceService.executePlatformOperation(instanceKey, operation, body));
  }

  @Operation(summary = "查询国标设备配置能力", description = "按设备真实响应区分国标配置能力；未知能力可通过查询探测")
  @PostMapping("/{instanceKey}/wvp/config/capabilities")
  @PreAuthorize("@videoOperationAccess.allowed('wvpConfigCapabilities')")
  public R<Map<String, Object>> wvpConfigurationCapabilities(
      @PathVariable String instanceKey, @RequestBody Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.executePlatformOperation(instanceKey, "wvpConfigCapabilities", body));
  }

  @Operation(summary = "读取国标设备配置")
  @PostMapping("/{instanceKey}/wvp/config/read")
  @PreAuthorize("@videoOperationAccess.allowed('wvpConfigRead')")
  public R<Map<String, Object>> wvpConfigurationRead(
      @PathVariable String instanceKey, @RequestBody Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.executePlatformOperation(instanceKey, "wvpConfigRead", body));
  }

  @Operation(summary = "设置国标设备配置")
  @PostMapping("/{instanceKey}/wvp/config/apply")
  @PreAuthorize("@videoOperationAccess.allowed('wvpConfigApply')")
  @Log(title = "设置国标设备配置", businessType = BusinessType.UPDATE)
  public R<Map<String, Object>> wvpConfigurationApply(
      @PathVariable String instanceKey, @RequestBody Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.executePlatformOperation(instanceKey, "wvpConfigApply", body));
  }

  @Operation(summary = "查询国标抓拍任务状态")
  @PostMapping("/{instanceKey}/wvp/config/status")
  @PreAuthorize("@videoOperationAccess.allowed('wvpConfigStatus')")
  public R<Map<String, Object>> wvpConfigurationStatus(
      @PathVariable String instanceKey, @RequestBody Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.executePlatformOperation(instanceKey, "wvpConfigStatus", body));
  }

  @Operation(summary = "获取ONVIF扫描网段建议", description = "返回服务端所在机器可用于ONVIF扫描的IPv4网段候选")
  @GetMapping("/{instanceKey}/onvif/networks")
  public R<?> listOnvifNetworkCandidates(
      @PathVariable("instanceKey") String instanceKey) {
    assertInstancePerm(instanceKey);
    VideoPlatformInstance instance = instanceService.getByInstanceKey(instanceKey);
    if (instance == null) {
      return R.error("平台实例不存在");
    }
    if (!"onvif".equalsIgnoreCase(instance.getPlatformType())) {
      return R.error("仅ONVIF平台支持网段扫描建议");
    }
    return R.ok(buildLocalNetworkCandidates());
  }

  @Operation(summary = "保存ONVIF扫描配置", description = "保存ONVIF详情页网段、端口、并发、超时和可选凭证配置")
  @PreAuthorize("@ss.hasPermi('video:platform:sync')")
  @PostMapping("/{instanceKey}/onvif/scan-config")
  @Log(title = "保存ONVIF扫描配置", businessType = BusinessType.UPDATE)
  public R<?> saveOnvifScanConfig(
      @PathVariable("instanceKey") String instanceKey,
      @RequestBody(required = false) Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    VideoPlatformInstance instance = instanceService.getByInstanceKey(instanceKey);
    if (instance == null) {
      return R.error("平台实例不存在");
    }
    if (!"onvif".equalsIgnoreCase(instance.getPlatformType())) {
      return R.error("当前接口仅用于ONVIF扫描配置");
    }
    VideoPlatformInstance saved =
        persistOnvifScanConfig(instance, body == null ? new java.util.HashMap<>() : body);
    maskSensitiveFields(saved);
    return R.ok(saved);
  }

  @Operation(summary = "启动ONVIF异步扫描", description = "启动ONVIF详情页异步扫描任务，扫描结果通过任务状态接口刷新获取")
  @PreAuthorize("@ss.hasPermi('video:platform:sync')")
  @PostMapping("/{instanceKey}/onvif/scan/start")
  @Log(title = "启动ONVIF异步扫描", businessType = BusinessType.OTHER)
  public R<?> startOnvifScan(
      @PathVariable("instanceKey") String instanceKey,
      @RequestBody(required = false) Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.startOnvifScan(instanceKey, body));
  }

  @Operation(summary = "获取ONVIF扫描任务", description = "获取ONVIF异步扫描任务状态与发现结果")
  @GetMapping("/{instanceKey}/onvif/scan/{taskId}")
  public R<?> getOnvifScanTask(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("taskId") String taskId) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.getOnvifScanTask(instanceKey, taskId));
  }

  @Operation(summary = "获取ONVIF最近扫描结果", description = "获取ONVIF实例最近一次扫描任务状态与发现结果")
  @GetMapping("/{instanceKey}/onvif/scan/latest")
  public R<?> getLatestOnvifScan(@PathVariable("instanceKey") String instanceKey) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.getLatestOnvifScan(instanceKey));
  }

  @Operation(summary = "获取直连SDK扫描网段建议", description = "返回服务端所在机器可用于设备直连SDK扫描的IPv4网段候选")
  @GetMapping("/{instanceKey}/sdk/discovery/networks")
  public R<?> listSdkNetworkCandidates(@PathVariable("instanceKey") String instanceKey) {
    assertInstancePerm(instanceKey);
    VideoPlatformInstance instance = instanceService.getByInstanceKey(instanceKey);
    if (instance == null) {
      return R.error("平台实例不存在");
    }
    if (!isDirectSdkPlatform(instance.getPlatformType())) {
      return R.error("仅设备直连SDK平台支持智能添加");
    }
    return R.ok(buildLocalNetworkCandidates());
  }

  @Operation(summary = "启动直连SDK摄像头发现", description = "启动设备直连SDK异步发现任务")
  @PreAuthorize("@ss.hasPermi('video:platform:sync')")
  @PostMapping("/{instanceKey}/sdk/discovery/start")
  @Log(title = "启动直连SDK摄像头发现", businessType = BusinessType.OTHER)
  public R<?> startSdkDiscovery(
      @PathVariable("instanceKey") String instanceKey,
      @RequestBody(required = false) Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.startSdkDiscovery(instanceKey, body));
  }

  @Operation(summary = "获取直连SDK发现任务", description = "获取直连SDK异步发现任务状态与候选设备")
  @GetMapping("/{instanceKey}/sdk/discovery/{taskId}")
  public R<?> getSdkDiscoveryTask(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("taskId") String taskId) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.getSdkDiscoveryTask(instanceKey, taskId));
  }

  @Operation(summary = "获取直连SDK最近发现结果", description = "获取直连SDK实例最近一次发现任务状态与候选设备")
  @GetMapping("/{instanceKey}/sdk/discovery/latest")
  public R<?> getLatestSdkDiscovery(@PathVariable("instanceKey") String instanceKey) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.getLatestSdkDiscovery(instanceKey));
  }

  @Operation(summary = "验证直连SDK摄像头", description = "验证单个直连SDK摄像头连接、账号密码和通道")
  @PreAuthorize("@ss.hasPermi('video:platform:sync')")
  @PostMapping("/{instanceKey}/sdk/devices/validate")
  public R<?> validateSdkDevice(
      @PathVariable("instanceKey") String instanceKey,
      @RequestBody(required = false) Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.validateSdkDevice(instanceKey, body));
  }

  @Operation(summary = "导入直连SDK摄像头", description = "导入直连SDK发现到的摄像头，写入平台设备/通道并可同步为IoT视频设备")
  @PreAuthorize("@ss.hasPermi('video:platform:device:import')")
  @PostMapping("/{instanceKey}/sdk/devices/import")
  @Log(title = "导入直连SDK摄像头", businessType = BusinessType.IMPORT)
  public R<?> importSdkDevices(
      @PathVariable("instanceKey") String instanceKey,
      @RequestBody(required = false) Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.importSdkDevices(instanceKey, body));
  }

  @Operation(summary = "更新直连SDK默认ZLM节点", description = "保存设备直连SDK详情页预览默认流媒体节点")
  @PreAuthorize("@ss.hasPermi('video:platform:edit')")
  @PostMapping("/{instanceKey}/sdk/default-media-server")
  @Log(title = "更新直连SDK默认ZLM节点", businessType = BusinessType.UPDATE)
  public R<VideoPlatformInstance> updateSdkDefaultMediaServer(
      @PathVariable("instanceKey") String instanceKey,
      @RequestBody(required = false) Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    VideoPlatformInstance saved = instanceService.updateSdkDefaultMediaServer(instanceKey, body);
    maskSensitiveFields(saved);
    return R.ok(saved);
  }

  @PreAuthorize("@ss.hasPermi('video:platform:subscribe')")
  @PostMapping("/{instanceKey}/sdk/subscription/retry")
  public R<?> retrySdkSubscription(@PathVariable("instanceKey") String instanceKey,
      @RequestBody Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    List<String> ids = new java.util.ArrayList<>();
    if (body.get("deviceIds") instanceof List<?> values) values.forEach(value -> ids.add(String.valueOf(value)));
    return R.ok(instanceService.retrySdkSubscription(instanceKey, ids));
  }

  @PreAuthorize("@ss.hasPermi('video:platform:subscribe')")
  @PutMapping("/{instanceKey}/sdk/subscription")
  public R<?> saveSdkSubscription(@PathVariable("instanceKey") String instanceKey,
      @RequestBody Map<String, Object> config) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.saveSdkSubscription(instanceKey, config));
  }

  @Operation(summary = "查询直连SDK告警订阅状态", description = "返回设备直连SDK告警订阅配置和网关运行态")
  @GetMapping("/{instanceKey}/sdk/subscription/status")
  public R<?> getSdkSubscriptionStatus(@PathVariable("instanceKey") String instanceKey) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.getSdkSubscriptionStatus(instanceKey));
  }

  @Operation(summary = "按扫描参数同步ONVIF设备", description = "使用详情页当前扫描网段、凭证和超时参数同步ONVIF设备入库")
  @PreAuthorize("@ss.hasPermi('video:platform:sync')")
  @PostMapping("/{instanceKey}/sync/options")
  @Log(title = "按参数同步视频平台设备", businessType = BusinessType.OTHER)
  public R<?> forceSyncWithOptions(
      @PathVariable("instanceKey") String instanceKey,
      @RequestBody(required = false) Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    VideoPlatformInstance instance = instanceService.getByInstanceKey(instanceKey);
    if (instance == null) {
      return R.error("平台实例不存在");
    }
    if (!"onvif".equalsIgnoreCase(instance.getPlatformType())) {
      return R.error("当前接口仅用于ONVIF扫描同步");
    }
    Map<String, Object> syncBody = body == null ? new java.util.HashMap<>() : new java.util.HashMap<>(body);
    DeviceSyncHandler.DeviceSyncResult syncResult =
        instanceService.forceSyncDevices(instanceKey, syncBody);
    if (syncResult.getErrors() == null || syncResult.getErrors().isEmpty()) {
      Map<String, Object> persistBody = new java.util.HashMap<>(syncBody);
      if (StrUtil.isNotBlank(firstNonBlankString(persistBody.get("scanEndpoint")))) {
        persistBody.put("endpoint", persistBody.get("scanEndpoint"));
      }
      if (persistBody.containsKey("scanSkipWsDiscovery")) {
        persistBody.put("skipWsDiscovery", persistBody.get("scanSkipWsDiscovery"));
      }
      Object selectedDevices = persistBody.get("devices");
      if (StrUtil.isBlank(firstNonBlankString(persistBody.get("endpoint")))
          && selectedDevices instanceof Iterable<?> iterable) {
        String firstDeviceEndpoint = firstDeviceEndpoint(iterable);
        if (StrUtil.isNotBlank(firstDeviceEndpoint)) {
          persistBody.put("endpoint", firstDeviceEndpoint);
        }
      }
      persistOnvifScanConfig(instance, persistBody);
    }
    return R.ok(buildSyncResult(instanceKey, syncResult, 1));
  }

  /**
   * 强制同步组织与设备缓存（限频：每实例5分钟一次）
   *
   * @param instanceKey 平台实例Key
   * @return 同步结果
   */
  @Operation(summary = "强制同步组织与设备缓存", description = "强制同步指定平台实例的组织与设备缓存（限频：每实例5分钟一次）")
  @ApiResponse(
      responseCode = "200",
      description = "成功同步组织与设备缓存",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @PreAuthorize("@ss.hasPermi('video:platform:sync')")
  @PostMapping("/{instanceKey}/sync")
  @Log(title = "强制同步视频平台组织与设备缓存", businessType = BusinessType.OTHER)
  public R<?> forceSync(@PathVariable("instanceKey") String instanceKey) {
    assertInstancePerm(instanceKey);
    long now = System.currentTimeMillis();
    Long last = SYNC_RATE_LIMIT.get(instanceKey);
    if (last != null && (now - last) < 3 * 60 * 1000) {
      long remaining = 5 * 60 * 1000 - (now - last);
      return R.error("操作过于频繁，请在" + (remaining / 1000) + "秒后重试");
    }
    SYNC_RATE_LIMIT.put(instanceKey, now);

    VideoPlatformInstance instance = instanceService.getByInstanceKey(instanceKey);
    if (instance == null) {
      return R.error("平台实例不存在");
    }

    DeviceSyncHandler.DeviceSyncResult syncResult = instanceService.forceSyncDevices(instanceKey);

    // 同步组织信息
    Map<String, Object> orgParam = new java.util.HashMap<>();
    orgParam.put("forceSync", "true");
    List<Map<String, Object>> orgs = instanceService.listOrganizations(instanceKey, orgParam);

    Map<String, Object> result = buildSyncResult(instanceKey, syncResult, orgs == null ? 0 : orgs.size());
    return R.ok(result);
  }

  /**
   * 获取设备通道列表（从中间表返回）
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @return 设备通道列表
   * @deprecated 请使用 listChannels 从通道表查询
   */
  @Operation(summary = "获取设备通道列表", description = "获取指定设备的通道列表（从中间表返回）")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备通道列表",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @Deprecated
  @GetMapping("/{instanceKey}/devices/{deviceId}/channels")
  public R<List<Map<String, Object>>> listDeviceChannels(
      @PathVariable("instanceKey") String instanceKey, @PathVariable("deviceId") String deviceId) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.listDeviceChannels(instanceKey, deviceId));
  }

  @Operation(summary = "同步设备通道", description = "实时读取指定ONVIF、海康直连SDK、大华SDK或海康ISUP设备的通道目录并同步")
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @PreAuthorize("@ss.hasPermi('video:platform:sync')")
  @PostMapping("/{instanceKey}/devices/{deviceId}/channels/sync")
  @Log(title = "同步设备通道", businessType = BusinessType.OTHER)
  public R<Map<String, Object>> syncDeviceChannels(
      @PathVariable("instanceKey") String instanceKey, @PathVariable("deviceId") String deviceId) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.syncDeviceChannels(instanceKey, deviceId));
  }

  /**
   * 查询通道列表（从通道表查询） 支持筛选: keyword(通道名称/ID), status(通道状态), ptzType(PTZ类型)
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @param params 查询参数
   * @return 通道列表
   */
  @Operation(
      summary = "查询通道列表",
      description = "查询指定设备的通道列表（从通道表查询），支持筛选: keyword(通道名称/ID), status(通道状态), ptzType(PTZ类型)")
  @ApiResponse(
      responseCode = "200",
      description = "成功查询通道列表",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = false)
  @GetMapping("/{instanceKey}/channels")
  public R<List<Map<String, Object>>> listChannels(
      @PathVariable("instanceKey") String instanceKey,
      @RequestParam(required = false) String deviceId,
      @RequestParam Map<String, String> params) {
    Map<String, Object> filters = new java.util.HashMap<>(params);
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.listChannels(instanceKey, deviceId, filters));
  }

  /*
   * 视频融合入口总览：
   * - /fusion 只读写通道融合配置，不直接取流、不启动 FFmpeg。
   * - /fusion/start|refresh|stop 只服务后端融合流模式，由 service 再校验 fusionMode。
   * - /fusion/overlay 和 /share 服务前端 OSD，返回文本/样式或公开分享 token，不生成融合流。
   */
  @Operation(summary = "获取通道融合配置", description = "获取指定通道的视频融合配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取通道融合配置",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @GetMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/fusion")
  public R<Map<String, Object>> getChannelFusion(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.getChannelFusion(instanceKey, deviceId, channelId));
  }

  @Operation(summary = "更新通道融合配置", description = "更新指定通道的视频融合配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功更新通道融合配置",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @PutMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/fusion")
  @Log(title = "更新视频融合配置", businessType = BusinessType.UPDATE)
  public R<Map<String, Object>> updateChannelFusion(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId,
      @RequestBody Map<String, Object> updates) {
    assertInstancePerm(instanceKey);
    // service 会归一化 fusionMode，并在切出后端融合流时清理/停止后端任务运行态。
    return R.ok(instanceService.updateChannelFusion(instanceKey, deviceId, channelId, updates));
  }

  @Operation(summary = "获取通道融合上下文", description = "提供给独立fusion-service使用的通道融合上下文")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取通道融合上下文",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @GetMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/fusion/context")
  public R<Map<String, Object>> getChannelFusionContext(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId,
      @RequestParam(name = "includeOrigin", defaultValue = "true") boolean includeOrigin) {
    assertInstancePerm(instanceKey);
    // 前端普通预览默认传 includeOrigin=false，避免仅加载融合上下文时触发 WVP 点播取流。
    return R.ok(instanceService.getChannelFusionContext(instanceKey, deviceId, channelId, includeOrigin));
  }

  @Operation(summary = "同步通道融合任务状态", description = "查询并同步指定通道的融合任务运行状态")
  @GetMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/fusion/task")
  public R<Map<String, Object>> getChannelFusionTask(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId) {
    assertInstancePerm(instanceKey);
    // 仅后端融合流需要同步 FFmpeg/ZLM 任务状态；前端 OSD 模式 service 会直接跳过。
    return R.ok(instanceService.getChannelFusionTask(instanceKey, deviceId, channelId));
  }

  @Operation(summary = "启动通道融合任务", description = "启动指定通道的 WVP+ZLM 融合任务")
  @PostMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/fusion/start")
  @Log(title = "启动视频融合任务", businessType = BusinessType.OTHER)
  public R<Map<String, Object>> startChannelFusionTask(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId) {
    assertInstancePerm(instanceKey);
    // 只有 fusionMode=backend_fusion 才允许启动，避免前端 OSD 预览误拉起 FFmpeg。
    return R.ok(instanceService.startChannelFusionTask(instanceKey, deviceId, channelId));
  }

  @Operation(summary = "刷新通道融合热数据", description = "刷新指定通道的融合叠加文本并同步最新状态")
  @PostMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/fusion/refresh")
  @Log(title = "刷新视频融合热数据", businessType = BusinessType.OTHER)
  public R<Map<String, Object>> refreshChannelFusionTask(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId) {
    assertInstancePerm(instanceKey);
    // 刷新的是后端融合任务的叠加文本；前端 OSD 文本由影子事件/快照接口更新。
    return R.ok(instanceService.refreshChannelFusionTask(instanceKey, deviceId, channelId));
  }

  @Operation(summary = "停止通道融合任务", description = "停止指定通道的 WVP+ZLM 融合任务")
  @PostMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/fusion/stop")
  @Log(title = "停止视频融合任务", businessType = BusinessType.OTHER)
  public R<Map<String, Object>> stopChannelFusionTask(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.stopChannelFusionTask(instanceKey, deviceId, channelId));
  }

  @Operation(summary = "获取通道前端OSD快照", description = "获取指定通道的前端外挂OSD实时文本和样式上下文")
  @GetMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/fusion/overlay")
  public R<Map<String, Object>> getChannelFusionOverlay(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId) {
    assertInstancePerm(instanceKey);
    // 前端播放器周期或 WebSocket/SSE 兜底刷新该快照，只返回 OSD 文本与样式，不返回视频流。
    return R.ok(instanceService.getChannelFusionOverlay(instanceKey, deviceId, channelId));
  }

  @Operation(summary = "创建通道前端OSD分享", description = "生成带有效期的公开视频播放和前端OSD分享链接")
  @PostMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/share")
  @Log(title = "创建视频前端OSD分享", businessType = BusinessType.OTHER)
  public R<Map<String, Object>> createChannelShare(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId,
      @RequestBody(required = false) Map<String, Object> body) {
    assertInstancePerm(instanceKey);
    // 分享链接固定走公开视频入口，通常使用原始流 + 浏览器叠加 OSD，避免公开播放触发后端融合任务。
    return R.ok(instanceService.createFrontendOsdShare(instanceKey, deviceId, channelId, body));
  }

  @Operation(summary = "撤销通道前端OSD分享", description = "撤销指定公开视频分享token")
  @DeleteMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/share/{token}")
  @Log(title = "撤销视频前端OSD分享", businessType = BusinessType.DELETE)
  public R<Void> revokeChannelShare(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId,
      @PathVariable("token") String token) {
    assertInstancePerm(instanceKey);
    instanceService.revokeFrontendOsdShare(instanceKey, deviceId, channelId, token);
    return R.ok();
  }

  /**
   * 获取通道流地址（预览/回放）
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @param channelId 通道ID
   * @param params 查询参数
   * @return 流地址信息
   */
  @Operation(summary = "获取通道流地址", description = "获取指定通道的流地址（预览/回放）")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取通道流地址",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @Parameter(name = "channelId", description = "通道ID", required = true)
  @GetMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/stream")
  @PreAuthorize("@videoOperationAccess.allowed('preview')")
  public R<Map<String, String>> getStreamUrl(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId,
      @RequestParam Map<String, String> params) {
    String streamType = params.getOrDefault("streamType", "main");
    Map<String, String> options = new java.util.HashMap<>(params);
    options.remove("streamType");
    assertInstancePerm(instanceKey);
    // playMode/preferFusion/startFusionIfNeeded 都透传给 service，由统一逻辑决定原始流或后端融合流。
    return R.ok(instanceService.getStreamUrl(instanceKey, deviceId, channelId, streamType, options));
  }

  /**
   * PTZ控制
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @param channelId 通道ID
   * @param body 控制参数
   * @return 控制结果
   */
  @Operation(summary = "PTZ控制", description = "对指定通道进行PTZ控制操作")
  @ApiResponse(
      responseCode = "200",
      description = "成功执行PTZ控制",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @Parameter(name = "channelId", description = "通道ID", required = true)
  @PostMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/ptz")
  @PreAuthorize("@videoOperationAccess.allowed('ptz')")
  @Log(title = "视频设备PTZ控制", businessType = BusinessType.OTHER)
  public R<Void> controlPTZ(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId,
      @RequestBody Map<String, Object> body) {
    String command = String.valueOf(body.getOrDefault("command", "stop"));
    int speed = 50;
    try {
      Object s = body.get("speed");
      if (s != null) speed = Integer.parseInt(String.valueOf(s));
    } catch (Exception ignore) {
    }
    assertInstancePerm(instanceKey);
    instanceService.controlPTZ(instanceKey, deviceId, channelId, command, speed);
    return R.ok();
  }

  /**
   * 批量导入设备（勾选使用流程）
   *
   * @param instanceKey 平台实例Key
   * @param body 导入设备参数
   * @return 导入结果统计
   */
  @Operation(summary = "批量导入设备", description = "批量导入视频设备（勾选使用流程）")
  @ApiResponse(
      responseCode = "200",
      description = "成功导入设备",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @PreAuthorize("@ss.hasPermi('video:platform:device:import')")
  @PostMapping("/{instanceKey}/devices/import")
  @Log(title = "批量导入视频设备", businessType = BusinessType.IMPORT)
  public R<Map<String, Integer>> importDevices(
      @PathVariable("instanceKey") String instanceKey, @RequestBody Map<String, Object> body) {
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> devices = (List<Map<String, Object>>) body.get("devices");
    String productKey = (String) body.get("productKey");
    assertInstancePerm(instanceKey);
    Map<String, Integer> result =
        deviceImportService.importDevices(instanceKey, devices, productKey);
    return R.ok(result);
  }

  /**
   * 获取预览流地址
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @param channelId 通道ID
   * @param streamType 流类型
   * @return 预览流地址信息
   */
  @Operation(summary = "获取预览流地址", description = "获取指定通道的预览流地址")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取预览流地址",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @Parameter(name = "channelId", description = "通道ID", required = true)
  @Parameter(name = "streamType", description = "流类型", required = false)
  @Parameter(name = "protocol", description = "取流协议，例如 ws/wss/hls", required = false)
  @Parameter(name = "transmode", description = "传输模式", required = false)
  @GetMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/preview")
  @PreAuthorize("@videoOperationAccess.allowed('preview')")
  public R<Map<String, String>> getPreviewUrl(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId,
      @RequestParam Map<String, String> params) {
    String streamType = params.getOrDefault("streamType", "main");
    Map<String, String> options = new java.util.HashMap<>(params);
    options.remove("streamType");
    assertInstancePerm(instanceKey);
    // 普通前端 OSD 播放应传 playMode=origin；后端融合流才传 auto/fusion 并允许 startFusionIfNeeded。
    return R.ok(instanceService.getStreamUrl(instanceKey, deviceId, channelId, streamType, options));
  }

  @Operation(summary = "设备录像控制")
  @PostMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/playback/control")
  @PreAuthorize("@videoOperationAccess.allowed('playbackStreamStatus')")
  public R<Void> controlPlayback(
      @PathVariable String instanceKey, @PathVariable String deviceId, @PathVariable String channelId,
      @RequestBody Map<String, Object> data) {
    assertInstancePerm(instanceKey);
    String stream = data.get("stream") instanceof String ? (String) data.get("stream") : null;
    String command = data.get("command") instanceof String ? (String) data.get("command") : null;
    Double speed = data.get("speed") instanceof Number ? ((Number) data.get("speed")).doubleValue() : null;
    instanceService.controlPlayback(instanceKey, deviceId, channelId, stream, command, speed);
    return R.ok();
  }

  /**
   * 获取回放流地址
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @param channelId 通道ID
   * @param params 查询参数
   * @return 回放流地址信息
   */
  @Operation(summary = "获取回放流地址", description = "获取指定通道的回放流地址")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取回放流地址",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @Parameter(name = "channelId", description = "通道ID", required = true)
  @GetMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/playback")
  @PreAuthorize("@videoOperationAccess.allowed('playbackStreamStart')")
  public R<Map<String, Object>> getPlaybackUrl(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId,
      @RequestParam Map<String, String> params) {
    assertInstancePerm(instanceKey);

    // 从参数中获取开始时间和结束时间
    String startTimeStr = params.get("startTime");
    String endTimeStr = params.get("endTime");

    if (startTimeStr == null || endTimeStr == null) {
      return R.error("缺少参数: startTime 或 endTime", null);
    }

    // 解析时间字符串为时间戳（毫秒）
    long startTime;
    long endTime;
    try {
      // 支持格式：yyyy-MM-dd HH:mm:ss 或时间戳（毫秒）
      if (startTimeStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        startTime = sdf.parse(startTimeStr).getTime();
      } else {
        startTime = Long.parseLong(startTimeStr);
        // 如果是10位秒级时间戳，转换为毫秒
        if (startTimeStr.length() == 10) {
          startTime = startTime * 1000;
        }
      }

      if (endTimeStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        endTime = sdf.parse(endTimeStr).getTime();
      } else {
        endTime = Long.parseLong(endTimeStr);
        // 如果是10位秒级时间戳，转换为毫秒
        if (endTimeStr.length() == 10) {
          endTime = endTime * 1000;
        }
      }
    } catch (Exception e) {
      return R.error("时间格式错误: " + e.getMessage(), null);
    }

    Map<String, String> options = new java.util.HashMap<>(params);
    options.remove("startTime");
    options.remove("endTime");

    return R.ok(
        instanceService.getPlaybackUrl(
            instanceKey, deviceId, channelId, startTime, endTime, options));
  }

  /**
   * 查询录像记录
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @param channelId 通道ID
   * @param params 查询参数
   * @return 录像记录列表
   */
  @Operation(summary = "查询录像记录", description = "查询指定通道的录像记录")
  @ApiResponse(
      responseCode = "200",
      description = "成功查询录像记录",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @Parameter(name = "channelId", description = "通道ID", required = true)
  @GetMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/records")
  @PreAuthorize("@videoOperationAccess.allowed('recordingSearch')")
  public R<List<Map<String, Object>>> queryRecords(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId,
      @RequestParam Map<String, String> params) {
    long startTime = parseRequestTime(params.get("startTime"), 0L);
    long endTime =
        parseRequestTime(params.get("endTime"), System.currentTimeMillis());
    assertInstancePerm(instanceKey);
    Map<String, String> options = new java.util.HashMap<>(params);
    options.remove("startTime");
    options.remove("endTime");
    return R.ok(
        instanceService.queryRecords(instanceKey, deviceId, channelId, startTime, endTime, options));
  }

  // ==================== 国标录像下载 ====================

  /**
   * 开始国标录像下载
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @param channelId 通道ID
   * @param data 下载参数
   * @return 下载结果
   */
  @Operation(summary = "开始国标录像下载", description = "开始国标录像下载任务")
  @ApiResponse(
      responseCode = "200",
      description = "成功开始国标录像下载",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @Parameter(name = "channelId", description = "通道ID", required = true)
  @PostMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/gb-records/download/start")
  public R<Map<String, Object>> startGBRecordDownload(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId,
      @RequestBody Map<String, Object> data) {
    assertInstancePerm(instanceKey);
    return instanceService.startGBRecordDownload(instanceKey, deviceId, channelId, data);
  }

  /**
   * 停止国标录像下载
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @param channelId 通道ID
   * @param data 下载参数
   * @return 停止结果
   */
  @Operation(summary = "停止国标录像下载", description = "停止国标录像下载任务")
  @ApiResponse(
      responseCode = "200",
      description = "成功停止国标录像下载",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @Parameter(name = "channelId", description = "通道ID", required = true)
  @PostMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/gb-records/download/stop")
  public R<Void> stopGBRecordDownload(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId,
      @RequestBody Map<String, Object> data) {
    assertInstancePerm(instanceKey);
    return instanceService.stopGBRecordDownload(instanceKey, deviceId, channelId, data);
  }

  /**
   * 获取国标录像下载进度
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @param channelId 通道ID
   * @param stream 流标识
   * @return 下载进度信息
   */
  @Operation(summary = "获取国标录像下载进度", description = "获取国标录像下载任务的进度信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取下载进度",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @Parameter(name = "channelId", description = "通道ID", required = true)
  @Parameter(name = "stream", description = "流标识", required = true)
  @GetMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/gb-records/download/progress")
  public R<Map<String, Object>> getGBRecordDownloadProgress(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId,
      @RequestParam String stream) {
    assertInstancePerm(instanceKey);
    return instanceService.getGBRecordDownloadProgress(instanceKey, deviceId, channelId, stream);
  }

  // ==================== 云端录像 ====================

  /**
   * 查询云端录像日期列表
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @param channelId 通道ID
   * @param params 查询参数
   * @return 云端录像日期列表
   */
  @Operation(summary = "查询云端录像日期列表", description = "查询指定通道的云端录像日期列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功查询云端录像日期列表",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @Parameter(name = "channelId", description = "通道ID", required = true)
  @GetMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/cloud-records/dates")
  public R<List<String>> queryCloudRecordDates(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId,
      @RequestParam Map<String, String> params) {
    assertInstancePerm(instanceKey);
    return instanceService.queryCloudRecordDates(instanceKey, deviceId, channelId, params);
  }

  /**
   * 查询云端录像列表
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @param channelId 通道ID
   * @param params 查询参数
   * @return 云端录像列表
   */
  @Operation(summary = "查询云端录像列表", description = "查询指定通道的云端录像列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功查询云端录像列表",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @Parameter(name = "channelId", description = "通道ID", required = true)
  @GetMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/cloud-records")
  public R<Map<String, Object>> queryCloudRecords(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId,
      @RequestParam Map<String, String> params) {
    assertInstancePerm(instanceKey);
    return instanceService.queryCloudRecords(instanceKey, deviceId, channelId, params);
  }

  /**
   * 云端录像播放
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @param channelId 通道ID
   * @param data 加载参数
   * @return 加载结果
   */
  @Operation(summary = "", description = "云端录像播放")
  @ApiResponse(
      responseCode = "200",
      description = "成功加载云端录像文件",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @Parameter(name = "channelId", description = "通道ID", required = true)
  @PostMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/cloud-records/load")
  public R<Map<String, Object>> loadCloudRecordPlay(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId,
      @RequestBody Map<String, Object> data) {
    assertInstancePerm(instanceKey);
    return instanceService.loadCloudRecord(instanceKey, deviceId, channelId, data);
  }

  /**
   * 云端录像定位
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @param channelId 通道ID
   * @param data 定位参数
   * @return 定位结果
   */
  @Operation(summary = "云端录像定位", description = "对指定通道的云端录像进行定位操作")
  @ApiResponse(
      responseCode = "200",
      description = "成功执行云端录像定位",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @Parameter(name = "channelId", description = "通道ID", required = true)
  @PostMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/cloud-records/seek")
  public R<Void> seekCloudRecord(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId,
      @RequestBody Map<String, Object> data) {
    assertInstancePerm(instanceKey);
    return instanceService.seekCloudRecord(instanceKey, deviceId, channelId, data);
  }

  /**
   * 设置云端录像倍速
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @param channelId 通道ID
   * @param data 倍速参数
   * @return 设置结果
   */
  @Operation(summary = "设置云端录像倍速", description = "设置指定通道的云端录像播放倍速")
  @ApiResponse(
      responseCode = "200",
      description = "成功设置云端录像倍速",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @Parameter(name = "channelId", description = "通道ID", required = true)
  @PostMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/cloud-records/speed")
  public R<Void> setCloudRecordSpeed(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("channelId") String channelId,
      @RequestBody Map<String, Object> data) {
    assertInstancePerm(instanceKey);
    return instanceService.setCloudRecordSpeed(instanceKey, deviceId, channelId, data);
  }

  /**
   * 获取云端录像下载地址
   *
   * @param instanceKey 平台实例Key
   * @param recordId 录像记录ID
   * @return 云端录像下载地址信息
   */
  @Operation(summary = "获取云端录像下载地址", description = "获取指定录像记录的云端录像下载地址")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取云端录像下载地址",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "recordId", description = "录像记录ID", required = true)
  @GetMapping("/{instanceKey}/cloud-records/play/path")
  public R<Map<String, Object>> getCloudRecordPlayPath(
      @PathVariable("instanceKey") String instanceKey, @RequestParam Integer recordId) {
    assertInstancePerm(instanceKey);
    return instanceService.getCloudRecordPlayPath(instanceKey, recordId);
  }

  /**
   * 获取设备完整信息（跨主表和扩展表查询）
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @return 设备完整信息
   */
  @Operation(summary = "获取设备完整信息", description = "获取指定设备的完整信息（跨主表和扩展表查询）")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备完整信息",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @GetMapping("/{instanceKey}/devices/{deviceId}/detail")
  public R<Map<String, Object>> getDeviceDetail(
      @PathVariable("instanceKey") String instanceKey, @PathVariable("deviceId") String deviceId) {
    assertInstancePerm(instanceKey);
    return R.ok(instanceService.getDeviceDetail(instanceKey, deviceId));
  }

  /**
   * 更新视频设备信息
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @param updates 更新参数
   * @return 更新后的设备信息
   */
  @Operation(summary = "更新视频设备信息", description = "更新指定视频设备的信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功更新视频设备信息",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @PreAuthorize("@ss.hasPermi('video:platform:device:edit')")
  @PutMapping("/{instanceKey}/devices/{deviceId}")
  @Log(title = "更新视频设备信息", businessType = BusinessType.UPDATE)
  public R<Map<String, Object>> updateDevice(
      @PathVariable("instanceKey") String instanceKey,
      @PathVariable("deviceId") String deviceId,
      @RequestBody Map<String, Object> updates) {
    assertInstancePerm(instanceKey);
    Map<String, Object> detail = instanceService.updateDevice(instanceKey, deviceId, updates);
    return R.ok(detail);
  }

  @Operation(summary = "修改通道名称", description = "同步主辅码流及已关联物联设备；支持恢复设备原名")
  @PreAuthorize("@ss.hasPermi('video:platform:device:edit')")
  @PutMapping("/{instanceKey}/devices/{deviceId}/channels/{channelId}/name")
  @Log(title = "修改视频通道名称", businessType = BusinessType.UPDATE)
  public R<Void> updateChannelName(@PathVariable String instanceKey, @PathVariable String deviceId,
      @PathVariable String channelId, @RequestBody Map<String, Object> updates) {
    assertInstancePerm(instanceKey);
    instanceService.updateChannelName(instanceKey, deviceId, channelId, updates);
    return R.ok();
  }

  /**
   * 删除视频设备
   *
   * @param instanceKey 平台实例Key
   * @param deviceId 设备ID
   * @return 删除结果
   */
  @Operation(summary = "删除视频设备", description = "删除指定的视频设备")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除视频设备",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "instanceKey", description = "平台实例Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @PreAuthorize("@ss.hasPermi('video:platform:device:remove')")
  @DeleteMapping("/{instanceKey}/devices/{deviceId}")
  @Log(title = "删除视频设备", businessType = BusinessType.DELETE)
  public R<Void> deleteDevice(
      @PathVariable("instanceKey") String instanceKey, @PathVariable("deviceId") String deviceId) {
    assertInstancePerm(instanceKey);
    instanceService.deleteDevice(instanceKey, deviceId);
    return R.ok();
  }

  /**
   * 删除视频平台设备(级联删除)
   *
   * @param id 视频平台设备主键ID
   * @return 删除结果
   */
  @Operation(summary = "删除视频平台设备", description = "根据设备主键ID删除视频平台设备，级联删除所有关联数据")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除设备",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "deviceId", description = "设备主键ID", required = true)
  @PreAuthorize("@ss.hasPermi('video:platform:device:remove')")
  @DeleteMapping("/devices/{id}")
  @Log(
      title = "删除视频平台设备",
      businessType = BusinessType.DELETE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public R<java.util.Map<String, Object>> deleteVideoPlatformDevice(@PathVariable Long id) {
    log.info("[Controller] 删除视频平台设备请求: deviceId={}, 操作人={}", id, SecurityUtils.getUnionId());

    try {
      R<java.util.Map<String, Object>> result = cleanService.deleteVideoPlatformDevice(id);

      if (result.isSuccess()) {
        log.info(
            "[Controller] 删除视频平台设备成功: deviceId={}, 操作人={}", id, SecurityUtils.getUnionId());
      } else {
        log.warn(
            "[Controller] 删除视频平台设备失败: deviceId={}, 操作人={}, 原因={}",
            id,
            SecurityUtils.getUnionId(),
            result.getMsg());
      }

      return result;

    } catch (Exception e) {
      log.error(
          "[Controller] 删除视频平台设备异常: deviceId={}, 操作人={}, error={}",
          id,
          SecurityUtils.getUnionId(),
          e.getMessage(),
          e);
      return R.error("删除失败: " + e.getMessage(), null);
    }
  }

  private void assertInstancePerm(String instanceKey) {
    IoTUser user = loginIoTUnionUser(SecurityUtils.getUnionId());
    if (user == null || user.isAdmin()) return;
    VideoPlatformInstance inst = instanceService.getByInstanceKey(instanceKey);
    if (inst == null) throw new BaseException("平台实例不存在");
    if (!canAccessInstance(user, inst)) {
      throw new BaseException("无权操作该平台实例");
    }
  }

  private boolean canAccessInstance(IoTUser user, VideoPlatformInstance instance) {
    if (user == null || instance == null || user.isAdmin()) {
      return true;
    }
    String currentTenantId = resolveTenantId(user);
    String resourceTenantId =
        StrUtil.blankToDefault(
            instance.getTenantId(), resolveTenantIdByCreator(instance.getCreatorId()));
    if (StrUtil.isBlank(resourceTenantId)) {
      return StrUtil.equals(user.getUnionId(), instance.getCreatorId());
    }
    return StrUtil.equals(currentTenantId, resourceTenantId);
  }

  protected String resolveTenantId(IoTUser user) {
    if (user == null) {
      return null;
    }
    return StrUtil.blankToDefault(user.getTenantId(), user.getUnionId());
  }

  private String resolveTenantIdByCreator(String creatorId) {
    if (StrUtil.isBlank(creatorId)) {
      return null;
    }
    return resolveTenantId(loginIoTUnionUser(creatorId));
  }

  /**
   * 对视频平台实例的敏感字段进行脱敏处理
   *
   * @param instance 平台实例对象
   */
  private void maskSensitiveFields(VideoPlatformInstance instance) {
    if (instance == null) return;
    if ("hik_isup".equals(instance.getPlatformType()) && hikIsupRegistrationSync != null) {
      try {
        Map<String, Object> options = instance.getOptions() == null ? new java.util.LinkedHashMap<>()
            : objectMapper.readValue(instance.getOptions(), new TypeReference<java.util.LinkedHashMap<String, Object>>() {});
        options.put("applicationState", hikIsupRegistrationSync.applicationState(instance));
        instance.setOptions(objectMapper.writeValueAsString(options));
      } catch (Exception ignored) { }
    }
    instance.setAuth(cn.universal.admin.video.service.VideoPlatformSecretMask.mask(instance.getAuth()));
    instance.setOptions(cn.universal.admin.video.service.VideoPlatformSecretMask.mask(instance.getOptions()));
  }

  private Map<String, Object> buildSyncResult(
      String instanceKey, DeviceSyncHandler.DeviceSyncResult syncResult, int orgCount) {
    Map<String, Object> result = new java.util.HashMap<>();
    result.put("orgCount", Math.max(0, orgCount));
    result.put("deviceCount", syncResult.getTotalDevices());
    result.put("totalDevices", syncResult.getTotalDevices());
    result.put("channelCount", syncResult.getTotalChannels());
    result.put("totalChannels", syncResult.getTotalChannels());
    result.put("insertedDevices", syncResult.getInsertedDevices());
    result.put("updatedDevices", syncResult.getUpdatedDevices());
    result.put("insertedChannels", syncResult.getInsertedChannels());
    result.put("errorCount", syncResult.getErrors() == null ? 0 : syncResult.getErrors().size());
    result.put("errors", syncResult.getErrors());
    result.put("instanceKey", instanceKey);
    result.put("pages", (syncResult.getTotalDevices() + 99) / 100);
    return result;
  }

  private Map<String, Object> buildLocalNetworkCandidates() {
    Map<String, Object> result = new java.util.LinkedHashMap<>();
    List<Map<String, Object>> networks = new java.util.ArrayList<>();
    try {
      java.util.Enumeration<java.net.NetworkInterface> interfaces =
          java.net.NetworkInterface.getNetworkInterfaces();
      while (interfaces != null && interfaces.hasMoreElements()) {
        java.net.NetworkInterface networkInterface = interfaces.nextElement();
        if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
          continue;
        }
        for (java.net.InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
          if (!(address.getAddress() instanceof java.net.Inet4Address inet4Address)) {
            continue;
          }
          short prefixLength = address.getNetworkPrefixLength();
          if (prefixLength <= 0 || prefixLength > 30) {
            prefixLength = 24;
          }
          String ip = inet4Address.getHostAddress();
          String cidr = ipv4Network(ip, prefixLength) + "/" + prefixLength;
          Map<String, Object> item = new java.util.LinkedHashMap<>();
          item.put("interfaceName", networkInterface.getName());
          item.put("displayName", networkInterface.getDisplayName());
          item.put("ip", ip);
          item.put("prefixLength", prefixLength);
          item.put("cidr", cidr);
          item.put("label", networkInterface.getDisplayName() + " · " + cidr);
          networks.add(item);
        }
      }
    } catch (Exception e) {
      log.warn("[ONVIF] 获取服务端网段候选失败: {}", e.getMessage());
    }
    networks.sort(
        java.util.Comparator
            .<Map<String, Object>>comparingInt(item -> isPrivateIpv4(String.valueOf(item.get("ip"))) ? 0 : 1)
            .thenComparingInt(item -> isLikelyVirtualInterface(String.valueOf(item.get("interfaceName")), String.valueOf(item.get("displayName"))) ? 1 : 0)
            .thenComparing(item -> String.valueOf(item.get("cidr"))));
    result.put("networks", networks);
    result.put("recommended", networks.isEmpty() ? "" : networks.get(0).get("cidr"));
    result.put("serverTime", System.currentTimeMillis());
    return result;
  }

  private boolean isPrivateIpv4(String ip) {
    if (ip == null) {
      return false;
    }
    return ip.startsWith("10.")
        || ip.startsWith("192.168.")
        || ip.matches("^172\\.(1[6-9]|2\\d|3[0-1])\\..*");
  }

  private boolean isLikelyVirtualInterface(String name, String displayName) {
    String text = ((name == null ? "" : name) + " " + (displayName == null ? "" : displayName)).toLowerCase();
    return text.contains("docker")
        || text.contains("bridge")
        || text.contains("veth")
        || text.contains("vmnet")
        || text.contains("virtual")
        || text.contains("utun")
        || text.contains("awdl")
        || text.contains("llw");
  }

  private String ipv4Network(String ip, int prefixLength) {
    try {
      String[] parts = ip.split("\\.");
      int value = 0;
      for (String part : parts) {
        value = (value << 8) | Integer.parseInt(part);
      }
      int mask = prefixLength == 0 ? 0 : (int) (0xffffffffL << (32 - prefixLength));
      int network = value & mask;
      return String.format(
          "%d.%d.%d.%d",
          (network >>> 24) & 0xff,
          (network >>> 16) & 0xff,
          (network >>> 8) & 0xff,
          network & 0xff);
    } catch (Exception e) {
      return ip;
    }
  }

  private VideoPlatformInstance persistOnvifScanConfig(
      VideoPlatformInstance instance, Map<String, Object> body) {
    if (instance == null) {
      throw new BaseException("平台实例不存在");
    }
    Map<String, Object> safeBody = body == null ? new java.util.HashMap<>() : body;
    try {
      Map<String, Object> auth = parseJsonMap(instance.getAuth());
      String endpoint = firstNonBlankString(safeBody.get("endpoint"), safeBody.get("network"), safeBody.get("cidr"));
      if (StrUtil.isNotBlank(endpoint)) {
        auth.put("endpoint", endpoint.trim());
        instance.setEndpoint(endpoint.trim());
      }
      copyIfPresent(auth, safeBody, "username");
      copyIfPresent(auth, safeBody, "password");
      copyIfPresent(auth, safeBody, "authType");
      if (parseBoolean(safeBody.get("saveCredentials"))) {
        copyIfPresent(auth, safeBody, "credentials");
      }
      putNumberIfPresent(auth, safeBody, "timeoutMs");
      putBooleanTextIfPresent(auth, safeBody, "skipWsDiscovery");
      putBooleanTextIfPresent(auth, safeBody, "useCache");
      if (!auth.containsKey("embedCredentialInRtsp")) {
        auth.put("embedCredentialInRtsp", "true");
      }

      Map<String, Object> options = parseJsonMap(instance.getOptions());
      Map<String, Object> onvif =
          options.get("onvif") instanceof Map<?, ?> existingOnvif
              ? new java.util.LinkedHashMap<>((Map<String, Object>) existingOnvif)
              : new java.util.LinkedHashMap<>();
      Map<String, Object> scan =
          onvif.get("scan") instanceof Map<?, ?> existingScan
              ? new java.util.LinkedHashMap<>((Map<String, Object>) existingScan)
              : new java.util.LinkedHashMap<>();
      if (StrUtil.isNotBlank(endpoint)) {
        scan.put("endpoint", endpoint.trim());
      }
      putNumberIfPresent(scan, safeBody, "timeoutMs");
      putNumberIfPresent(scan, safeBody, "scanLimit");
      putNumberIfPresent(scan, safeBody, "scanConcurrency");
      putPortsIfPresent(scan, safeBody.get("ports"));
      if (parseBoolean(safeBody.get("saveCredentials"))) {
        copyIfPresent(scan, safeBody, "credentials");
      }
      scan.put("skipWsDiscovery", String.valueOf(parseBoolean(safeBody.get("skipWsDiscovery"))));
      scan.put("useCache", String.valueOf(parseBoolean(safeBody.get("useCache"))));
      scan.put("savedAt", new java.util.Date().toInstant().toString());
      onvif.put("createMode", "scan_in_detail");
      onvif.put("scan", scan);
      options.put("onvif", onvif);

      VideoPlatformInstance update = new VideoPlatformInstance();
      update.setId(instance.getId());
      update.setInstanceKey(instance.getInstanceKey());
      update.setPlatformType(instance.getPlatformType());
      update.setName(instance.getName());
      update.setEndpoint(instance.getEndpoint());
      update.setAuth(objectMapper.writeValueAsString(auth));
      update.setOptions(objectMapper.writeValueAsString(options));
      update.setEnabled(instance.getEnabled());
      update.setAutoCreateProducts(instance.getAutoCreateProducts());
      update.setVersion(instance.getVersion());
      return instanceService.update(update);
    } catch (BaseException e) {
      throw e;
    } catch (Exception e) {
      throw new BaseException("保存ONVIF扫描配置失败：" + e.getMessage());
    }
  }

  private Map<String, Object> parseJsonMap(String json) {
    if (StrUtil.isBlank(json)) {
      return new java.util.LinkedHashMap<>();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<java.util.LinkedHashMap<String, Object>>() {});
    } catch (Exception e) {
      return new java.util.LinkedHashMap<>();
    }
  }

  private void copyIfPresent(Map<String, Object> target, Map<String, Object> source, String key) {
    Object value = source == null ? null : source.get(key);
    if (value == null || JsonDesensitizationUtil.isMaskedValue(value)) {
      return;
    }
    String text = String.valueOf(value).trim();
    if (text.isEmpty()) {
      return;
    }
    target.put(key, text);
  }

  private void putNumberIfPresent(Map<String, Object> target, Map<String, Object> source, String key) {
    Object value = source == null ? null : source.get(key);
    if (value == null) {
      return;
    }
    try {
      target.put(key, Integer.parseInt(String.valueOf(value)));
    } catch (Exception ignore) {
    }
  }

  private void putBooleanTextIfPresent(Map<String, Object> target, Map<String, Object> source, String key) {
    if (source == null || !source.containsKey(key)) {
      return;
    }
    target.put(key, String.valueOf(parseBoolean(source.get(key))));
  }

  private void putPortsIfPresent(Map<String, Object> target, Object value) {
    List<Integer> ports = new java.util.ArrayList<>();
    if (value instanceof Iterable<?> iterable) {
      for (Object item : iterable) {
        addPort(ports, item);
      }
    } else if (value != null) {
      for (String item : String.valueOf(value).split("[,;\\s]+")) {
        addPort(ports, item);
      }
    }
    if (!ports.isEmpty()) {
      target.put("ports", ports.stream().distinct().limit(8).toList());
    }
  }

  private void addPort(List<Integer> ports, Object value) {
    try {
      int port = Integer.parseInt(String.valueOf(value).trim());
      if (port > 0 && port <= 65535) {
        ports.add(port);
      }
    } catch (Exception ignore) {
    }
  }

  private boolean parseBoolean(Object value) {
    if (value instanceof Boolean bool) {
      return bool;
    }
    String text = value == null ? "" : String.valueOf(value).trim().toLowerCase();
    return "true".equals(text) || "1".equals(text) || "yes".equals(text) || "y".equals(text);
  }

  private boolean isDirectSdkPlatform(String platformType) {
    String normalized = platformType == null ? "" : platformType.trim().toLowerCase();
    return "hik_sdk".equals(normalized) || "dahua_sdk".equals(normalized) || "tiandy_sdk".equals(normalized) || "uniview_sdk".equals(normalized);
  }

  private String firstNonBlankString(Object... values) {
    if (values == null) {
      return null;
    }
    for (Object value : values) {
      if (value == null) {
        continue;
      }
      String text = String.valueOf(value).trim();
      if (!text.isEmpty()) {
        return text;
      }
    }
    return null;
  }

  private String firstDeviceEndpoint(Iterable<?> devices) {
    if (devices == null) {
      return null;
    }
    for (Object device : devices) {
      if (device instanceof Map<?, ?> map) {
        String endpoint =
            firstNonBlankString(
                map.get("xAddr"), map.get("endpoint"), map.get("deviceIp"), map.get("ip"));
        if (StrUtil.isNotBlank(endpoint)) {
          return endpoint;
        }
      } else {
        String endpoint = firstNonBlankString(device);
        if (StrUtil.isNotBlank(endpoint)) {
          return endpoint;
        }
      }
    }
    return null;
  }

  private long parseRequestTime(String timeText, long defaultValue) {
    if (timeText == null || timeText.isBlank()) {
      return defaultValue;
    }
    try {
      if (timeText.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.parse(timeText).getTime();
      }
      long timestamp = Long.parseLong(timeText);
      return timeText.length() == 10 ? timestamp * 1000 : timestamp;
    } catch (Exception e) {
      throw new BaseException("时间格式错误: " + e.getMessage());
    }
  }
}
