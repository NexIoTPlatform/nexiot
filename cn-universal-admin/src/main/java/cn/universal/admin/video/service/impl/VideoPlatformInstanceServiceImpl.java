/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 视频平台实例服务实现
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.admin.video.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.common.service.BaseServiceImpl;
import cn.universal.admin.platform.service.IIoTDeviceService;
import cn.universal.admin.video.service.VideoDeviceImportService;
import cn.universal.admin.video.service.VideoDeviceNameService;
import cn.universal.admin.video.service.VideoDeviceNames;
import cn.universal.admin.video.service.VideoPlatformInstanceService;
import cn.universal.admin.video.service.VideoSdkSubscriptionRecovery;
import cn.universal.admin.video.sync.DeviceSyncHandler;
import cn.universal.admin.video.sync.DeviceSyncHandler.DeviceSyncResult;
import cn.universal.admin.video.sync.DeviceSyncHandlerRegistry;
import cn.universal.admin.video.vo.VideoPlatformChannelVO;
import cn.universal.admin.video.vo.VideoPlatformDeviceVO;
import cn.universal.common.exception.BaseException;
import cn.universal.common.utils.JsonDesensitizationUtil;
import cn.universal.dm.device.event.DeviceShadowUpdatedEvent;
import cn.universal.dm.video.VideoPlatformAdapter;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.dm.video.zlm.VideoZlmStreamProxyService;
import cn.universal.persistence.entity.IoTDeviceShadow;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformDeviceExt;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceExtMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import cn.universal.persistence.query.VideoPlatformDeviceQuery;
import cn.universal.ossm.util.VideoSnapshotOssHelper;
import cn.universal.security.utils.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tk.mybatis.mapper.entity.Example;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoPlatformInstanceServiceImpl extends BaseServiceImpl
    implements VideoPlatformInstanceService {

  private final VideoPlatformInstanceMapper instanceMapper;
  private final VideoPlatformAdapterRegistry adapterRegistry;
  private final DeviceSyncHandlerRegistry syncHandlerRegistry;
  private final ObjectMapper objectMapper;
  private final VideoPlatformDeviceMapper deviceMapper;
  private final VideoPlatformDeviceExtMapper deviceExtMapper;
  private final VideoPlatformChannelMapper channelMapper;
  private final cn.universal.admin.video.service.HikIsupPlatformConfiguration hikIsupConfiguration;
  @jakarta.annotation.Resource private cn.universal.admin.video.alarm.VideoSdkEvidenceTimelineService sdkEvidenceTimeline;
  private final cn.universal.admin.video.service.HikIsupRegistrationSync hikIsupRegistrationSync;
  private final cn.universal.admin.video.service.VideoIsupPreviewService isupPreviewService;
  @jakarta.annotation.Resource private cn.universal.admin.video.service.DahuaActiveRegistrationConfiguration dahuaRegistrationConfiguration;
  @jakarta.annotation.Resource private cn.universal.admin.video.service.DahuaActiveRegistrationSync dahuaRegistrationSync;
  @jakarta.annotation.Resource private cn.universal.admin.video.service.DahuaIpcChannelStatusRefresh dahuaIpcChannelStatusRefresh;
  @jakarta.annotation.Resource private cn.universal.admin.video.service.VideoDahuaPreviewService dahuaPreviewService;
  @jakarta.annotation.Resource private cn.universal.admin.video.service.VideoUniviewPreviewService univiewPreviewService;
  private final VideoDeviceImportService deviceImportService;
  private final VideoDeviceNameService deviceNameService;
  private final ObjectProvider<VideoZlmStreamProxyService> zlmStreamProxyServiceProvider;
  private final VideoSnapshotOssHelper snapshotOssHelper;
  private final cn.universal.persistence.mapper.VideoPlatformOrgCacheMapper orgCacheMapper;
  private final cn.universal.persistence.mapper.IoTDeviceMapper ioTDeviceMapper;
  private final cn.universal.persistence.mapper.IoTProductMapper ioTProductMapper;
  private final IIoTDeviceService ioTDeviceService;
  private final StringRedisTemplate stringRedisTemplate;
  private final VideoSdkSubscriptionRecovery sdkSubscriptionRecovery;
  private final cn.universal.dm.video.VideoSdkEventCatalog sdkEventCatalog;
  private final cn.universal.admin.video.alarm.SdkVideoAlarmContextCache alarmContextCache;
  private final Map<String, FusionBinding> fusionTaskRegistry = new ConcurrentHashMap<>();
  @jakarta.annotation.Resource private cn.universal.admin.video.service.VideoSdkPlaybackService sdkPlaybackService;
  @jakarta.annotation.Resource private cn.universal.admin.video.service.VideoOperationAccess videoOperationAccess;
  @jakarta.annotation.Resource private cn.universal.admin.video.service.WvpConfigurationAccess wvpConfigurationAccess;
  @jakarta.annotation.Resource private cn.universal.admin.video.service.VideoSdkRecordingTaskService sdkRecordingTasks;
  private final AtomicBoolean fusionRegistryBootstrapped = new AtomicBoolean(false);
  private final Map<String, Long> fusionOverlayPublishAt = new ConcurrentHashMap<>();
  private final Map<String, Long> fusionTaskStatusSyncAt = new ConcurrentHashMap<>();
  private final Map<String, FusionBinding> fusionFrontendOsdRegistry = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> fusionFrontendOsdSensorBindingIndex = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> fusionFrontendOsdRegistrySensorIds = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> fusionSensorBindingIndex = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> fusionRegistrySensorIds = new ConcurrentHashMap<>();
  private final AtomicInteger fusionOverlayThreadIndex = new AtomicInteger(1);
  private final AtomicInteger onvifScanThreadIndex = new AtomicInteger(1);
  private final AtomicInteger sdkDiscoveryThreadIndex = new AtomicInteger(1);
  private volatile ThreadPoolExecutor fusionOverlayExecutor;
  private volatile ThreadPoolExecutor onvifScanExecutor;
  private volatile ThreadPoolExecutor sdkDiscoveryExecutor;
  private static final String FUSION_MODE_FRONTEND_OSD = "frontend_osd";
  private static final String FUSION_MODE_BACKEND = "backend_fusion";
  private static final String FUSION_MODE_DISABLED = "disabled";
  private static final String VIDEO_SHARE_REDIS_PREFIX = "video:share:";
  private static final String VIDEO_FRONTEND_OSD_SNAPSHOT_PREFIX = "video:fusion:frontend-osd:snapshot:";
  private static final String ONVIF_SCAN_LOCK_PREFIX = "video:onvif:scan:lock:";
  private static final String ONVIF_SCAN_RATE_PREFIX = "video:onvif:scan:rate:";
  private static final String ONVIF_SCAN_TASK_PREFIX = "video:onvif:scan:task:";
  private static final String ONVIF_SCAN_LATEST_PREFIX = "video:onvif:scan:latest:";
  private static final String SDK_DISCOVERY_LOCK_PREFIX = "video:sdk:discovery:lock:";
  private static final String SDK_DISCOVERY_RATE_PREFIX = "video:sdk:discovery:rate:";
  private static final String SDK_DISCOVERY_TASK_PREFIX = "video:sdk:discovery:task:";
  private static final String SDK_DISCOVERY_LATEST_PREFIX = "video:sdk:discovery:latest:";

  @Value("${fusion.overlay-stream.enabled:${video.fusion.overlay-stream.enabled:true}}")
  private boolean fusionOverlayStreamEnabled;

  @Value("${fusion.scheduler.enabled:${video.fusion.scheduler.enabled:true}}")
  private boolean fusionSchedulerEnabled;

  @Value("${fusion.overlay-stream.key:${video.fusion.overlay-stream.key:nexiot:fusion:overlay}}")
  private String fusionOverlayStreamKey;

  @Value(
      "${fusion.overlay-stream.periodic-publish-enabled:${video.fusion.overlay-stream.periodic-publish-enabled:false}}")
  private boolean fusionOverlayStreamPeriodicPublishEnabled;

  @Value("${fusion.overlay-stream.min-interval-ms:${video.fusion.overlay-stream.min-interval-ms:200}}")
  private long fusionOverlayStreamMinIntervalMs;

  @Value("${fusion.overlay-stream.message-ttl-ms:${video.fusion.overlay-stream.message-ttl-ms:2000}}")
  private long fusionOverlayStreamMessageTtlMs;

  @Value("${fusion.overlay-stream.max-len:${video.fusion.overlay-stream.max-len:100000}}")
  private long fusionOverlayStreamMaxLen;

  @Value("${fusion.overlay-stream.approximate-trim:${video.fusion.overlay-stream.approximate-trim:true}}")
  private boolean fusionOverlayStreamApproximateTrim;

  @Value("${fusion.overlay-stream.async-enabled:${video.fusion.overlay-stream.async-enabled:true}}")
  private boolean fusionOverlayStreamAsyncEnabled;

  @Value("${fusion.overlay-stream.async-threads:${video.fusion.overlay-stream.async-threads:2}}")
  private int fusionOverlayStreamAsyncThreads;

  @Value("${fusion.overlay-stream.async-queue-size:${video.fusion.overlay-stream.async-queue-size:2000}}")
  private int fusionOverlayStreamAsyncQueueSize;

  @Value("${fusion.status-sync-interval-ms:${video.fusion.status-sync-interval-ms:30000}}")
  private long fusionStatusSyncIntervalMs;

  @Value("${video.share.default-ttl-seconds:86400}")
  private long videoShareDefaultTtlSeconds;

  @Value("${video.share.max-ttl-seconds:604800}")
  private long videoShareMaxTtlSeconds;

  @Value("${video.share.min-ttl-seconds:60}")
  private long videoShareMinTtlSeconds;

  @Value("${video.share.overlay-event-interval-ms:1000}")
  private long videoShareOverlayEventIntervalMs;

  @Value("${video.fusion.frontend-osd.snapshot-ttl-ms:10000}")
  private long videoFrontendOsdSnapshotTtlMs;

  @Value("${video.fusion.shadow-event-enabled:${video.fusion.frontend-osd.event-enabled:true}}")
  private boolean videoFusionShadowEventEnabled;

  @Value("${video.onvif.scan.async-threads:2}")
  private int onvifScanAsyncThreads;

  @Value("${video.onvif.scan.async-queue-size:20}")
  private int onvifScanAsyncQueueSize;

  @Value("${video.onvif.scan.lock-ttl-seconds:900}")
  private long onvifScanLockTtlSeconds;

  @Value("${video.onvif.scan.result-ttl-seconds:3600}")
  private long onvifScanResultTtlSeconds;

  @Value("${video.onvif.scan.min-interval-seconds:8}")
  private long onvifScanMinIntervalSeconds;

  @Value("${video.sdk.discovery.async-threads:${video.onvif.scan.async-threads:2}}")
  private int sdkDiscoveryAsyncThreads;

  @Value("${video.sdk.discovery.async-queue-size:${video.onvif.scan.async-queue-size:20}}")
  private int sdkDiscoveryAsyncQueueSize;

  @Value("${video.sdk.discovery.lock-ttl-seconds:${video.onvif.scan.lock-ttl-seconds:900}}")
  private long sdkDiscoveryLockTtlSeconds;

  @Value("${video.sdk.discovery.result-ttl-seconds:${video.onvif.scan.result-ttl-seconds:3600}}")
  private long sdkDiscoveryResultTtlSeconds;

  @Value("${video.sdk.discovery.min-interval-seconds:${video.onvif.scan.min-interval-seconds:8}}")
  private long sdkDiscoveryMinIntervalSeconds;

  @Value("${video.sdk.alarm.callback-token:}")
  private String videoSdkAlarmCallbackToken;

  @PreDestroy
  public void destroyFusionOverlayPublisher() {
    ThreadPoolExecutor executor = fusionOverlayExecutor;
    if (executor != null) {
      executor.shutdown();
    }
    ThreadPoolExecutor scanExecutor = onvifScanExecutor;
    if (scanExecutor != null) {
      scanExecutor.shutdown();
    }
    ThreadPoolExecutor discoveryExecutor = sdkDiscoveryExecutor;
    if (discoveryExecutor != null) {
      discoveryExecutor.shutdown();
    }
  }

  @Override
  public List<VideoPlatformInstance> listAll() {
    Example example = new Example(VideoPlatformInstance.class);
    example.orderBy("createTime").desc();
    return instanceMapper.selectByExample(example);
  }

  @Override
  public List<VideoPlatformInstance> listByType(String platformType) {
    Example example = new Example(VideoPlatformInstance.class);
    example.createCriteria().andEqualTo("platformType", platformType);
    example.orderBy("createTime").desc();
    return instanceMapper.selectByExample(example);
  }

  @Override
  public VideoPlatformInstance getByInstanceKey(String instanceKey) {
    Example example = new Example(VideoPlatformInstance.class);
    example.createCriteria().andEqualTo("instanceKey", instanceKey);
    List<VideoPlatformInstance> list = instanceMapper.selectByExample(example);
    return list.isEmpty() ? null : list.get(0);
  }

  @Override
  public VideoPlatformInstance getById(Long id) {
    return instanceMapper.selectByPrimaryKey(id);
  }

  @Override
  @Transactional
  @CacheEvict(
      cacheNames = {"getVideoPlatformInstance"},
      allEntries = true)
  public VideoPlatformInstance create(VideoPlatformInstance instance) {
    prepareInstanceForSave(instance);
    fillInstanceTenant(instance);
    // 若未提供instanceKey则按规则自动生成：标识 + yyyyMMdd + 随机后缀
    if (instance.getInstanceKey() == null || instance.getInstanceKey().trim().isEmpty()) {
      instance.setInstanceKey(generateInstanceKey(instance.getPlatformType()));
    }
    // 校验instanceKey唯一性
    VideoPlatformInstance existing = getByInstanceKey(instance.getInstanceKey());
    if (existing != null) {
      throw new BaseException("实例标识已存在：" + instance.getInstanceKey());
    }
    Date now = new Date();
    instance.setCreateTime(now);
    instance.setUpdateTime(now);
    if (instance.getEnabled() == null) {
      instance.setEnabled(1);
    }
    hikIsupConfiguration.validate(instance);
    if (dahuaRegistrationConfiguration != null) dahuaRegistrationConfiguration.validate(instance);
    requireIsupModule(instance);
    instanceMapper.insertSelective(instance);
    requestIsupSyncAfterCommit(instance);
    return instance;
  }

  @Override
  @Transactional
  @CacheEvict(
      cacheNames = {"getVideoPlatformInstance"},
      allEntries = true)
  public VideoPlatformInstance update(VideoPlatformInstance instance) {
    if (instance.getId() == null) {
      throw new BaseException("更新时ID不能为空");
    }
    VideoPlatformInstance persisted = instanceMapper.selectByPrimaryKey(instance.getId());
    if (persisted == null) {
      throw new BaseException("平台实例不存在");
    }
    assertTenantResourceAccess(persisted.getTenantId(), persisted.getCreatorId(), "无权更新该平台实例");
    if (instance.getAuth() != null) instance.setAuth(cn.universal.common.utils.JsonDesensitizationUtil.mergeAuthConfig(instance.getAuth(), persisted.getAuth()));
    else instance.setAuth(persisted.getAuth());
    if (instance.getOptions() != null) instance.setOptions(cn.universal.common.utils.JsonDesensitizationUtil.mergeAuthConfig(instance.getOptions(), persisted.getOptions()));
    else instance.setOptions(persisted.getOptions());
    if (instance.getPlatformType() == null) instance.setPlatformType(persisted.getPlatformType());
    if (instance.getInstanceKey() == null) instance.setInstanceKey(persisted.getInstanceKey());
    if ("hik_isup".equals(persisted.getPlatformType()) && (!"hik_isup".equals(instance.getPlatformType())
        || !java.util.Objects.equals(persisted.getInstanceKey(), instance.getInstanceKey())))
      throw new BaseException("ISUP 平台的实例标识和接入类型不能修改");
    prepareInstanceForSave(instance);
    instance.setCreatorId(persisted.getCreatorId());
    instance.setTenantId(persisted.getTenantId());
    instance.setUpdateTime(new Date());
    hikIsupConfiguration.validate(instance);
    if (dahuaRegistrationConfiguration != null) dahuaRegistrationConfiguration.validate(instance);
    requireIsupModule(instance);
    instanceMapper.updateByPrimaryKeySelective(instance);
    requestIsupSyncAfterCommit(instance);
    return instanceMapper.selectByPrimaryKey(instance.getId());
  }

  @Override
  @Transactional
  @CacheEvict(
      cacheNames = {"getVideoPlatformInstance"},
      allEntries = true)
  public void delete(Long id) {
    delete(id, false);
  }

  @Override
  @Transactional
  @CacheEvict(
      cacheNames = {"getVideoPlatformInstance"},
      allEntries = true)
  public void delete(Long id, boolean force) {
    VideoPlatformInstance inst = instanceMapper.selectByPrimaryKey(id);
    if (inst == null) {
      throw new BaseException("平台实例不存在");
    }
    assertTenantResourceAccess(inst.getTenantId(), inst.getCreatorId(), "无权删除该平台实例");
    if ("wvp".equals(inst.getPlatformType())) {
      adapterRegistry.getAdapter("wvp").executeOperation(inst, "assertAlarmSubscriptionStopped", Map.of());
    }
    String instanceKey = inst.getInstanceKey();
    int devCount = ioTDeviceMapper.countByProductKey(instanceKey);
    if (devCount > 0 && !force) {
      throw new BaseException("存在关联设备，请先删除或选择强制删除");
    }
    if ("hik_isup".equals(inst.getPlatformType()))
      adapterRegistry.getAdapter("hik_isup").executeOperation(inst, "stopServices", Map.of());
    if (cn.universal.admin.video.service.DahuaActiveRegistrationConfiguration.configured(inst))
      adapterRegistry.getAdapter("dahua_sdk").executeOperation(inst, "stopActiveRegistration", Map.of());
    if (devCount > 0 && force) {
      List<cn.universal.persistence.entity.IoTDevice> devList =
          ioTDeviceMapper.selectListByProductKey(instanceKey);
      for (cn.universal.persistence.entity.IoTDevice d : devList) {
        ioTDeviceMapper.deleteDevInstanceById(String.valueOf(d.getId()));
      }
    }
    cn.universal.persistence.entity.IoTProduct product =
        ioTProductMapper.getProductByProductKey(instanceKey);
    if (product != null && product.getId() != null) {
      ioTProductMapper.deleteDevProductById(String.valueOf(product.getId()));
    }
    Example devEx = new Example(VideoPlatformDevice.class);
    devEx.createCriteria().andEqualTo("instanceKey", instanceKey);
    List<VideoPlatformDevice> vpDevices = deviceMapper.selectByExample(devEx);
    for (VideoPlatformDevice d : vpDevices) {
      Long pk = d.getId();
      if (pk != null) {
        Example chEx = new Example(VideoPlatformChannel.class);
        chEx.createCriteria().andEqualTo("deviceId", pk);
        channelMapper.deleteByExample(chEx);
        Example extEx = new Example(VideoPlatformDeviceExt.class);
        extEx.createCriteria().andEqualTo("deviceId", pk);
        deviceExtMapper.deleteByExample(extEx);
      }
      deviceMapper.deleteByPrimaryKey(d.getId());
    }
    instanceMapper.deleteByPrimaryKey(id);
    alarmContextCache.publishPlatformChange(instanceKey);
  }

  @Override
  public Map<String, Object> testConnection(String instanceKey) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    return adapter.testConnection(instance);
  }

  @Override
  public List<VideoPlatformDeviceVO> listDevices(VideoPlatformDeviceQuery query) {
    // 规范化分页参数（使用BasePage的内置方法）
    query.normalizePage();

    // 先获取实例（必须在PageHelper.startPage()之前，避免清除分页上下文）
    VideoPlatformInstance instance = requireAccessibleInstance(query.getInstanceKey());

    // 设备记录属于平台实例，列表查询必须沿用实例租户范围，避免跨租户泄漏历史脏数据。
    String tenantId = StrUtil.blankToDefault(instance.getTenantId(), resolveTenantId(instance.getCreatorId()));
    query.setTenantId(tenantId);

    // 检查是否强制同步
    if (Boolean.TRUE.equals(query.getForceSync())) {
      forceSyncDevices(query.getInstanceKey());
    }

    // 启动分页（必须在所有其他DB操作之后）
    PageHelper.startPage(query.getPageOrDefault(), query.getSizeOrDefault());

    // 执行分页查询（返回的是Page对象）
    List<VideoPlatformDevice> list = deviceMapper.selectDevicesByQuery(query);

    // 立即保存分页信息（必须在执行其他DB查询之前）
    long total = 0;
    int pageNum = query.getPageOrDefault();
    int pageSize = query.getSizeOrDefault();
    if (list instanceof com.github.pagehelper.Page) {
      com.github.pagehelper.Page<?> page = (com.github.pagehelper.Page<?>) list;
      total = page.getTotal();
      pageNum = page.getPageNum();
      pageSize = page.getPageSize();
    }

    // 构建返回VO列表（这里会执行通道查询，会清除PageHelper上下文）
    List<VideoPlatformDeviceVO> voList = buildDeviceVOList(list);

    // 用Page对象包装结果，确保getDataTable能获取正确的total
    com.github.pagehelper.Page<VideoPlatformDeviceVO> resultPage =
        new com.github.pagehelper.Page<>(pageNum, pageSize);
    resultPage.setTotal(total);
    resultPage.addAll(voList);

    return resultPage;
  }

  @Override
  public DeviceSyncResult forceSyncDevices(String instanceKey) {
    return forceSyncDevices(instanceKey, Collections.emptyMap());
  }

  @Override
  public DeviceSyncResult forceSyncDevices(String instanceKey, Map<String, Object> extraFilters) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);

    DeviceSyncHandler syncHandler = syncHandlerRegistry.getHandler(instance.getPlatformType());
    if (syncHandler == null) {
      log.warn("[设备同步] 未找到处理器: platformType={}", instance.getPlatformType());
      DeviceSyncResult result = new DeviceSyncResult();
      result.addError("未找到平台同步处理器: " + instance.getPlatformType());
      return result;
    }

    if (hasSelectedDevices(extraFilters)) {
      Map<String, Object> filters = new java.util.HashMap<>();
      if (extraFilters != null) {
        filters.putAll(extraFilters);
      }
      filters.put("__resetBeforeSync", false);
      log.info(
          "[设备同步] 开始同步选中设备: instanceKey={}, platformType={}, handler={}",
          instanceKey,
          instance.getPlatformType(),
          syncHandler.getClass().getSimpleName());
      DeviceSyncResult result = syncHandler.syncDevices(instance, filters);
      log.info(
          "[设备同步] 选中设备同步完成: instanceKey={}, 总设备={}, 新增={}, 更新={}, 通道={}",
          instanceKey,
          result.getTotalDevices(),
          result.getInsertedDevices(),
          result.getUpdatedDevices(),
          result.getTotalChannels());
      return result;
    }

    final int pageSize = 100;
    final int maxPages = 1000;
    int currentPage = 1;
    DeviceSyncResult totalResult = new DeviceSyncResult();

    while (currentPage <= maxPages) {
      Map<String, Object> filters = new java.util.HashMap<>();
      if (extraFilters != null) {
        filters.putAll(extraFilters);
      }
      filters.put("page", currentPage);
      filters.put("size", pageSize);
      filters.put("pageNo", currentPage);
      filters.put("pageSize", pageSize);
      filters.put("__resetBeforeSync", currentPage == 1);

      log.info(
          "[设备同步] 开始同步分页: instanceKey={}, platformType={}, page={}, size={}, handler={}",
          instanceKey,
          instance.getPlatformType(),
          currentPage,
          pageSize,
          syncHandler.getClass().getSimpleName());

      DeviceSyncResult pageResult = syncHandler.syncDevices(instance, filters);
      totalResult.mergeFrom(pageResult);

      if (pageResult.getTotalDevices() <= 0 || pageResult.getTotalDevices() < pageSize) {
        break;
      }
      currentPage++;
    }

    if (currentPage > maxPages) {
      totalResult.addError("同步分页超过安全上限(" + maxPages + ")，请检查平台分页配置");
    }

    log.info(
        "[设备同步] 完成: instanceKey={}, 总设备={}, 新增={}, 更新={}, 通道={}",
        instanceKey,
        totalResult.getTotalDevices(),
        totalResult.getInsertedDevices(),
        totalResult.getUpdatedDevices(),
        totalResult.getTotalChannels());
    return totalResult;
  }

  /** 构建设备VO列表（批量查询通道，避免N+1问题） */
  private List<VideoPlatformDeviceVO> buildDeviceVOList(List<VideoPlatformDevice> devices) {
    if (devices == null || devices.isEmpty()) {
      return new java.util.ArrayList<>();
    }

    // 1. 收集所有设备ID
    List<Long> deviceIds =
        devices.stream()
            .map(VideoPlatformDevice::getId)
            .collect(java.util.stream.Collectors.toList());

    // 2. 一次性查询所有通道
    Example channelEx = new Example(VideoPlatformChannel.class);
    channelEx.createCriteria().andIn("deviceId", deviceIds);
    List<VideoPlatformChannel> allChannels = channelMapper.selectByExample(channelEx);

    // 3. 按deviceId分组
    Map<Long, List<VideoPlatformChannelVO>> channelMap =
        allChannels.stream()
            .collect(
                java.util.stream.Collectors.groupingBy(
                    VideoPlatformChannel::getDeviceId,
                    java.util.stream.Collectors.mapping(
                        ch ->
                            VideoPlatformChannelVO.builder()
                                .channelId(ch.getChannelId())
                                .channelCode(ch.getChannelCode())
                                .name(ch.getChannelName())
                                .status(ch.getChannelStatus())
                                .ptzType(ch.getPtzType())
                                .manufacturer(ch.getManufacturer())
                                .model(ch.getModel())
                                .ipAddress(ch.getIpAddress())
                                .port(ch.getPort())
                                .build(),
                        java.util.stream.Collectors.toList())));

    Example extensionQuery = new Example(VideoPlatformDeviceExt.class);
    extensionQuery.createCriteria().andIn("deviceId", deviceIds);
    List<VideoPlatformDeviceExt> extensions = deviceExtMapper.selectByExample(extensionQuery);
    Map<Long, Map<String, Object>> accessMap = new java.util.HashMap<>();
    if (extensions != null) extensions.forEach(extension -> accessMap.put(extension.getDeviceId(), readObjectMap(extension.getExtField3())));

    // 4. 构建VO列表
    List<VideoPlatformDeviceVO> result = new java.util.ArrayList<>();
    for (VideoPlatformDevice d : devices) {
      Map<String, Object> access = accessMap.getOrDefault(d.getId(), Map.of());
      VideoPlatformDeviceVO vo =
          VideoPlatformDeviceVO.builder()
              .instanceKey(d.getInstanceKey())
              .id(d.getId())
              .deviceId(d.getDeviceId())
              .deviceName(d.getDeviceName())
              .deviceStatus(effectiveDeviceStatus(d, channelMap.get(d.getId())))
              .deviceModel(d.getDeviceModel())
              .manufacturer(d.getManufacturer())
              .deviceIp(d.getDeviceIp())
              .devicePort(d.getDevicePort())
              .orgId(d.getOrgId())
              .enabled(d.getEnabled())
              .orgName(d.getOrgName())
              .accessMode(stringValue(access.get("accessMode")))
              .loginMode(stringValue(access.get("loginMode")))
              .registrationId(stringValue(access.get("registrationId")))
              .channelList(channelMap.getOrDefault(d.getId(), new java.util.ArrayList<>()))
              .build();

      result.add(vo);
    }

    return result;
  }

  private String effectiveDeviceStatus(
      VideoPlatformDevice device, List<VideoPlatformChannelVO> channels) {
    String status = device == null ? "" : StrUtil.trimToEmpty(device.getDeviceStatus());
    if (isOnlineStatus(status)) {
      return status;
    }
    if (channels != null) {
      for (VideoPlatformChannelVO channel : channels) {
        if (channel != null && isOnlineStatus(channel.getStatus())) {
          return "online";
        }
      }
    }
    return StrUtil.blankToDefault(status, "offline");
  }

  private boolean isOnlineStatus(String status) {
    String normalized = StrUtil.trimToEmpty(status).toLowerCase(java.util.Locale.ROOT);
    return "online".equals(normalized)
        || "on".equals(normalized)
        || "1".equals(normalized)
        || "true".equals(normalized)
        || "success".equals(normalized);
  }

  @Override
  public List<Map<String, Object>> listOrganizations(
      String instanceKey, Map<String, Object> filters) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    boolean forceSync = false;
    if (filters != null) {
      Object v = filters.get("forceSync");
      forceSync = v != null && String.valueOf(v).equalsIgnoreCase("true");
    }
    if (forceSync) {
      Map<String, Object> passFilters = new java.util.HashMap<>();
      if (filters != null) {
        filters.forEach(
            (k, v) -> {
              if (!"forceSync".equals(k)) passFilters.put(k, v);
            });
      }
      VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
      List<Map<String, Object>> remote = adapter.listOrganizations(instance, passFilters);
      String currentUserId = SecurityUtils.getUnionId();
      Date now = new Date();
      for (Map<String, Object> org : remote) {
        String orgId = (String) org.getOrDefault("orgId", org.get("id"));
        if (orgId == null) continue;
        String parentId = (String) org.getOrDefault("parentOrgId", org.get("parentId"));
        String orgName = (String) org.getOrDefault("orgName", org.get("name"));
        String path = (String) org.getOrDefault("path", org.get("path"));
        cn.universal.persistence.entity.VideoPlatformOrgCache one = null;
        {
          tk.mybatis.mapper.entity.Example ex =
              new tk.mybatis.mapper.entity.Example(
                  cn.universal.persistence.entity.VideoPlatformOrgCache.class);
          ex.createCriteria().andEqualTo("instanceKey", instanceKey).andEqualTo("orgId", orgId);
          java.util.List<cn.universal.persistence.entity.VideoPlatformOrgCache> existList =
              orgCacheMapper.selectByExample(ex);
          one = existList.isEmpty() ? null : existList.get(0);
        }
        if (one == null) {
          one =
              cn.universal.persistence.entity.VideoPlatformOrgCache.builder()
                  .instanceKey(instanceKey)
                  .orgId(orgId)
                  .parentOrgId(parentId)
                  .orgName(orgName)
                  .path(path)
                  .tenantId(instance.getTenantId())
                  .createId(currentUserId)
                  .updateId(currentUserId)
                  .createTime(now)
                  .updateTime(now)
                  .build();
          orgCacheMapper.insertSelective(one);
        } else {
          one.setParentOrgId(parentId);
          one.setOrgName(orgName);
          one.setPath(path);
          one.setTenantId(instance.getTenantId());
          one.setUpdateId(currentUserId);
          one.setUpdateTime(now);
          orgCacheMapper.updateByPrimaryKeySelective(one);
        }
      }
    }
    // 返回缓存组织树（平铺或自行构建树）
    java.util.List<cn.universal.persistence.entity.VideoPlatformOrgCache> list =
        orgCacheMapper.selectByExample(
            new tk.mybatis.mapper.entity.Example(
                cn.universal.persistence.entity.VideoPlatformOrgCache.class) {
              {
                this.createCriteria().andEqualTo("instanceKey", instanceKey);
              }
            });
    java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
    for (cn.universal.persistence.entity.VideoPlatformOrgCache o : list) {
      java.util.Map<String, Object> item = new java.util.HashMap<>();
      item.put("instanceKey", o.getInstanceKey());
      item.put("orgId", o.getOrgId());
      item.put("parentOrgId", o.getParentOrgId());
      item.put("orgName", o.getOrgName());
      item.put("path", o.getPath());
      result.add(item);
    }
    // 服务器端筛选与分页
    java.util.List<Map<String, Object>> filtered = new java.util.ArrayList<>();
    String keyword = filters == null ? null : (String) filters.get("keyword");
    for (Map<String, Object> it : result) {
      boolean ok = true;
      if (keyword != null && !keyword.isEmpty()) {
        String name = String.valueOf(it.getOrDefault("orgName", ""));
        ok &= name.contains(keyword);
      }
      if (ok) filtered.add(it);
    }
    int page = 1;
    int pageSize = filtered.size();
    if (filters != null) {
      Object p = filters.get("page");
      Object ps = filters.get("pageSize");
      try {
        if (p != null) page = Integer.parseInt(String.valueOf(p));
      } catch (Exception ignore) {
      }
      try {
        if (ps != null) pageSize = Integer.parseInt(String.valueOf(ps));
      } catch (Exception ignore) {
      }
    }
    if (page < 1) page = 1;
    if (pageSize < 1) pageSize = 10;
    int start = (page - 1) * pageSize;
    if (start >= filtered.size()) return java.util.Collections.emptyList();
    int end = Math.min(start + pageSize, filtered.size());
    return filtered.subList(start, end);
  }

  @Override
  public void applySubscription(String instanceKey) {
    sdkSubscriptionRecovery.withLock(instanceKey, () -> {
      sdkSubscriptionRecovery.resetRetry(instanceKey);
      applySubscriptionLocked(instanceKey);
      return null;
    });
  }

  private void applySubscriptionLocked(String instanceKey) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    if ("wvp".equals(instance.getPlatformType())) {
      adapterRegistry.getAdapter("wvp").subscribe(instance, List.of("cameraAlarm"), "");
      return;
    }
    applySubscriptionInternal(instance, true);
  }

  @Override
  public Map<String, Object> restoreEnabledSubscriptions() {
    return sdkSubscriptionRecovery.recoverNow();
  }

  private boolean applySubscriptionInternal(VideoPlatformInstance instance, boolean throwOnFailure) {
    try {
      if (instance == null || instance.getOptions() == null) return false;
      JsonNode root = objectMapper.readTree(instance.getOptions());
      JsonNode sub = root.path("subscription");
      boolean enabled = sub.path("enabled").asBoolean(false);
      if (!enabled) return false;
      java.util.List<String> topics = new java.util.ArrayList<>();
      if (sub.path("topics").isArray()) {
        sub.path("topics").forEach(n -> topics.add(n.asText()));
      }
      String callback = sub.path("callback").asText("");
      if (cn.universal.dm.video.VideoSdkPlatforms.managed(instance.getPlatformType()) && StrUtil.isBlank(callback)) {
        throw new BaseException("请先在告警订阅中配置 NexIoT 回调地址");
      }
      ensureSubscriptionToken(instance, root, sub);
      VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
      if (cn.universal.dm.video.VideoSdkPlatforms.managed(instance.getPlatformType())
          && (topics.isEmpty() || adapter.resolveSubscriptionDevices(instance).isEmpty())) {
        adapter.unsubscribe(instance, List.of());
      } else {
        adapter.subscribe(instance, topics, callback);
      }
      log.info(
          "[视频SDK告警] 已应用订阅 instanceKey={}, platformType={}, topics={}, callback={}",
          instance.getInstanceKey(),
          instance.getPlatformType(),
          topics,
          callback);
      return true;
    } catch (Exception e) {
      if (throwOnFailure) {
        throw new BaseException("应用订阅失败：" + e.getMessage());
      }
      throw new BaseException(e.getMessage());
    }
  }

  private void ensureSubscriptionToken(
      VideoPlatformInstance instance, JsonNode root, JsonNode subscriptionNode) {
    if (instance == null || !(root instanceof ObjectNode rootObject)) {
      return;
    }
    boolean hasToken =
        subscriptionNode != null
            && (StrUtil.isNotBlank(subscriptionNode.path("token").asText(""))
                || StrUtil.isNotBlank(subscriptionNode.path("callbackToken").asText("")));
    if (hasToken || StrUtil.isBlank(videoSdkAlarmCallbackToken)) {
      return;
    }
    ObjectNode subscriptionObject =
        subscriptionNode instanceof ObjectNode
            ? (ObjectNode) subscriptionNode
            : objectMapper.createObjectNode();
    subscriptionObject.put("token", videoSdkAlarmCallbackToken);
    rootObject.set("subscription", subscriptionObject);
    instance.setOptions(rootObject.toString());
  }

  @Override
  @CacheEvict(cacheNames = "getVideoPlatformInstance", allEntries = true)
  public void cancelSubscription(String instanceKey) {
    sdkSubscriptionRecovery.withLock(instanceKey, () -> {
      sdkSubscriptionRecovery.resetRetry(instanceKey);
      cancelSubscriptionLocked(instanceKey);
      return null;
    });
  }

  private void cancelSubscriptionLocked(String instanceKey) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    if ("wvp".equals(instance.getPlatformType())) {
      adapterRegistry.getAdapter("wvp").unsubscribe(instance, List.of());
      return;
    }
    try {
      if (instance.getOptions() == null) return;
      JsonNode root = objectMapper.readTree(instance.getOptions());
      JsonNode sub = root.path("subscription");
      if (cn.universal.dm.video.VideoSdkPlatforms.managed(instance.getPlatformType()) && sub instanceof ObjectNode subscription) {
        subscription.put("enabled", false);
        instance.setOptions(root.toString());
        instance.setUpdateTime(new Date());
        instanceMapper.updateByPrimaryKeySelective(instance);
      }
      java.util.List<String> topics = new java.util.ArrayList<>();
      if (sub.path("topics").isArray()) {
        sub.path("topics").forEach(n -> topics.add(n.asText()));
      }
      VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
      adapter.unsubscribe(instance, topics);
    } catch (Exception e) {
      throw new BaseException("取消订阅失败：" + e.getMessage());
    }
  }

  @Override
  public Map<String, Object> getSdkSubscriptionStatus(String instanceKey) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    if (!cn.universal.dm.video.VideoSdkPlatforms.managed(instance.getPlatformType())) {
      throw new BaseException("此平台不支持 SDK 告警订阅状态查询");
    }
    Map<String, Object> options = readObjectMap(instance.getOptions());
    Map<String, Object> subscription = firstMap(options.get("subscription"));
    VideoPlatformAdapter sdkAdapter = adapterRegistry.getAdapter(instance.getPlatformType());
    List<Map<String, Object>> configuredDevices = sdkSubscriptionDeviceSummaries(instance, sdkAdapter.resolveSubscriptionDevices(instance));
    boolean enabled = parseBoolean(subscription.get("enabled"));

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("instanceKey", instance.getInstanceKey());
    result.put("platformType", instance.getPlatformType());
    result.put("enabled", enabled);
    result.put("checkedAt", System.currentTimeMillis());
    result.putAll(sdkSubscriptionRecovery.metadata(instanceKey));
    result.put("topics", subscription.getOrDefault("topics", List.of()));
    result.put("callback", stringValue(subscription.get("callback")));
    result.put("configuredDeviceCount", configuredDevices.size());
    result.put("configuredDevices", configuredDevices);
    result.put("eventOptions", sdkEventCatalog.options(instance.getPlatformType()));
    result.put("eventOptionsSource", "sdk_event_catalog");
    result.put("deviceEventCapabilityStatus", "unknown");
    result.put("filtering", Map.of("gateway", "before_callback", "backend", "before_iot_dispatch", "emptyTopics", "none"));
    result.put("scope", subscription.getOrDefault("scope", "all"));
    result.put("deviceIds", subscription.getOrDefault("deviceIds", List.of()));
    Map<String,Object> eventPolicy=new LinkedHashMap<>(firstMap(subscription.get("eventPolicy")));
    eventPolicy.keySet().retainAll(Set.of("firstOnly","cooldownSeconds","frameCooldownSeconds","alarmClip","preSeconds","postSeconds","pictureSource","snapshotFallbackEnabled"));
    Map<String,Object> policyDefaults=new LinkedHashMap<>(Map.of("firstOnly",false,"cooldownSeconds",0,"frameCooldownSeconds",60,"alarmClip",false,"preSeconds",5,"postSeconds",10,"pictureSource","device"));
    if (!"hik_isup".equals(instance.getPlatformType())) policyDefaults.put("snapshotFallbackEnabled",true);
    Map<String,Object> policyEffective=new LinkedHashMap<>(policyDefaults);policyEffective.putAll(eventPolicy);
    result.put("eventPolicy",eventPolicy);result.put("eventPolicyDefaults",policyDefaults);result.put("eventPolicyEffective",policyEffective);
    VideoPlatformInstance inventory = new VideoPlatformInstance();
    org.springframework.beans.BeanUtils.copyProperties(instance, inventory);
    Map<String, Object> inventoryOptions = new LinkedHashMap<>(options);
    inventoryOptions.put("subscription", Map.of("scope", "all"));
    inventory.setOptions(writeObjectMap(inventoryOptions));
    result.put("availableDevices", sdkSubscriptionDeviceSummaries(instance, sdkAdapter.resolveSubscriptionDevices(inventory)));
    Example disabledQuery = new Example(VideoPlatformDevice.class);
    disabledQuery.createCriteria().andEqualTo("instanceKey", instanceKey).andEqualTo("enabled", 0);
    result.put("disabledDevices", deviceMapper.selectByExample(disabledQuery).stream().map(device -> {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("deviceId", device.getDeviceId());
      row.put("deviceName", device.getDeviceName());
      row.put("host", device.getDeviceIp());
      row.put("port", device.getDevicePort());
      return row;
    }).toList());

    Map<String, Object> request = new LinkedHashMap<>();
    if (!configuredDevices.isEmpty()) {
      request.put("devices", configuredDevices);
    }
    try {
      VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
      Map<String, Object> gateway = adapter.executeOperation(instance, "subscriptionStatus", request);
      Map<String, Object> attrs = firstMap(gateway.get("attributes"));
      List<Map<String, Object>> runtimeSubscriptions = VideoSdkSubscriptionRecovery.runtimeSubscriptions(gateway).stream()
          .filter(row -> configuredDevices.stream().anyMatch(device -> VideoSdkSubscriptionRecovery.matchesDevice(device, row)))
          .toList();
      int runtimeCount = runtimeSubscriptions.size();
      result.put("gateway", gateway);
      result.put("runtimeSubscriptions", runtimeSubscriptions);
      result.put("failures", attrs.getOrDefault("failures", List.of()));
      result.put("runtimeCount", runtimeCount);
      if (!enabled) {
        result.put("status", "disabled");
        result.put("message", "订阅未启用");
      } else if (!(subscription.get("topics") instanceof List<?> selectedTopics) || selectedTopics.isEmpty()) {
        result.put("status", "inactive");
        result.put("message", "未选择告警类型，不转发任何告警");
      } else if (configuredDevices.isEmpty() && runtimeCount > 0) {
        result.put("status", "active");
        result.put("message", "订阅运行中");
      } else if (!configuredDevices.isEmpty() && runtimeCount >= configuredDevices.size()) {
        result.put("status", "active");
        result.put("message", "订阅运行中");
      } else if (runtimeCount > 0) {
        result.put("status", "partial");
        result.put("message", "部分设备已订阅");
      } else {
        boolean recovering = Boolean.TRUE.equals(result.get("autoRecoveryEnabled")) && !configuredDevices.isEmpty();
        result.put("status", recovering ? "recovering" : "inactive");
        result.put("message", configuredDevices.isEmpty() ? "当前订阅范围内没有已启用设备"
            : recovering ? "订阅配置已保存，后台将自动恢复网关监听" : "网关暂无运行态订阅，请手动恢复");
      }
      if (enabled && !extractMapList(attrs.get("failures")).isEmpty()) {
        result.put("status", runtimeCount > 0 ? "partial" : "error");
        result.put("message", "部分设备订阅失败，请查看设备明细");
      } else if (enabled && runtimeSubscriptions.stream().anyMatch(row -> "failed".equals(row.get("deliveryStatus")))) {
        result.put("status", "delivery_error");
        result.put("message", "监听已建立，但告警回调投递失败");
      }
    } catch (Exception e) {
      result.put("status", enabled ? "gateway_unavailable" : "disabled");
      result.put("message", "暂时无法查询 SDK 网关，已保存的订阅配置不受影响：" + e.getMessage());
      result.put("runtimeCount", 0);
      result.put("runtimeSubscriptions", List.of());
    }
    return result;
  }

  private List<Map<String, Object>> sdkSubscriptionDeviceSummaries(
      VideoPlatformInstance instance, List<Map<String, Object>> devices) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map<String, Object> device : devices) {
      Map<String, Object> row = new LinkedHashMap<>();
      for (String key : List.of("deviceId", "deviceName", "host", "port", "username", "credentialSource", "accessMode")) {
        row.put(key, device.get(key));
      }
      row.put("credentialReady", "hik_isup".equals(instance.getPlatformType()) || StrUtil.isNotBlank(stringValue(device.get("username")))
          && StrUtil.isNotBlank(stringValue(device.get("password")))
          && !isMaskedSecret("password", device.get("password")));
      cn.universal.persistence.entity.IoTDevice iot = ioTDeviceMapper.selectIoTDevice(instance.getInstanceKey(), stringValue(device.get("deviceId")));
      row.put("iotLinked", iot != null);
      if (iot != null) row.put("iotDeviceId", String.valueOf(iot.getId()));
      rows.add(row);
    }
    return rows;
  }

  @Override
  @CacheEvict(cacheNames = "getVideoPlatformInstance", allEntries = true)
  public Map<String, Object> saveSdkSubscription(String instanceKey, Map<String, Object> config) {
    return sdkSubscriptionRecovery.withLock(instanceKey, () -> {
      sdkSubscriptionRecovery.resetRetry(instanceKey);
      return saveSdkSubscriptionLocked(instanceKey, config);
    });
  }

  private Map<String, Object> saveSdkSubscriptionLocked(String instanceKey, Map<String, Object> config) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    if (!cn.universal.dm.video.VideoSdkPlatforms.managed(instance.getPlatformType())) throw new BaseException("此平台不支持 SDK 告警订阅");
    Map<String, Object> options = readObjectMap(instance.getOptions());
    Map<String, Object> subscription = new LinkedHashMap<>(firstMap(options.get("subscription")));
    boolean enabled = parseBoolean(config.get("enabled"));
    String callback = stringValue(config.get("callback")).trim();
    String scope = "selected".equals(config.get("scope")) ? "selected" : "all";
    List<String> topics = new ArrayList<>();
    if (config.get("topics") instanceof List<?> values) values.forEach(value -> topics.add(String.valueOf(value)));
    java.util.Set<String> supported = new java.util.HashSet<>();
    // 与上行解析共用目录；字典新增事件后，订阅保存校验无需再修改 Java 白名单。
    sdkEventCatalog.options(instance.getPlatformType())
        .forEach(event -> supported.add(String.valueOf(event.get("value"))));
    supported.add("cameraAlarm"); // legacy all-events selection
    if (!supported.containsAll(topics)) throw new BaseException("包含不支持的告警类型");
    if (enabled) {
      if (topics.isEmpty()) throw new BaseException("请选择要接收的告警类型");
      try {
        java.net.URI uri = java.net.URI.create(callback);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
            || !"/api/video/sdk-events/callback".equals(uri.getPath())) throw new IllegalArgumentException();
      } catch (Exception e) {
        throw new BaseException("请输入网关可访问的 NexIoT 告警回调地址，以 /api/video/sdk-events/callback 结尾");
      }
    }
    List<String> ids = new ArrayList<>();
    if (config.get("deviceIds") instanceof List<?> values) values.forEach(value -> ids.add(String.valueOf(value)));
    if (enabled && "selected".equals(scope) && ids.isEmpty()) throw new BaseException("请选择订阅设备");
    subscription.put("enabled", enabled);
    subscription.put("topics", topics);
    subscription.put("scope", scope);
    subscription.put("deviceIds", ids);
    subscription.put("callback", callback);
    if (config.get("eventPolicy") instanceof Map<?,?> policy) {
      if (!java.util.Set.of("firstOnly", "cooldownSeconds", "frameCooldownSeconds", "alarmClip", "preSeconds", "postSeconds", "pictureSource", "snapshotFallbackEnabled").containsAll(policy.keySet()))
        throw new BaseException("告警联动策略包含不支持字段");
      Map<String, Object> eventPolicy = toLinkedMap(policy);
      for (String key : List.of("firstOnly", "alarmClip", "snapshotFallbackEnabled"))
        if (eventPolicy.containsKey(key) && !(eventPolicy.get(key) instanceof Boolean)) throw new BaseException("告警策略开关必须为布尔值");
      for (String key : List.of("cooldownSeconds", "frameCooldownSeconds", "preSeconds", "postSeconds")) if (eventPolicy.containsKey(key)) {
        Object value = eventPolicy.get(key);
        int max = List.of("cooldownSeconds","frameCooldownSeconds").contains(key) ? 3600 : "preSeconds".equals(key) ? 30 : 120;
        int min = "frameCooldownSeconds".equals(key) ? 1 : 0;
        if (!(value instanceof Number n) || n.doubleValue() != n.intValue() || n.intValue() < min || n.intValue() > max)
          throw new BaseException("告警策略时间范围无效：" + key);
      }
      if (eventPolicy.containsKey("pictureSource") && !List.of("device", "platform_frame", "none").contains(eventPolicy.get("pictureSource")))
        throw new BaseException("告警图片来源无效");
      subscription.put("eventPolicy", eventPolicy);
    }
    subscription.remove("devices");
    subscription.remove("deviceList");
    options.put("subscription", subscription);
    instance.setOptions(writeObjectMap(options));
    if ("hik_isup".equals(instance.getPlatformType())) hikIsupConfiguration.validate(instance);
    if (enabled && "selected".equals(scope)
        && adapterRegistry.getAdapter(instance.getPlatformType()).resolveSubscriptionDevices(instance).isEmpty()) {
      throw new BaseException("所选设备已停用或不存在，请先启用设备或重新选择");
    }
    // Persist desired state even when a device is offline; status exposes each failure for retry.
    instance.setUpdateTime(new Date());
    instanceMapper.updateByPrimaryKeySelective(instance);
    requestIsupSyncAfterCommit(instance);
    String applyError = null;
    try {
      if (enabled) applySubscriptionInternal(instance, true);
      else adapterRegistry.getAdapter(instance.getPlatformType()).unsubscribe(instance, List.of());
    } catch (Exception e) {
      applyError = e.getMessage();
    }
    Map<String, Object> result = getSdkSubscriptionStatus(instanceKey);
    if (applyError != null) result.put("applyError", applyError);
    return result;
  }

  @Override
  public Map<String, Object> retrySdkSubscription(String instanceKey, List<String> deviceIds) {
    return sdkSubscriptionRecovery.withLock(instanceKey, () -> {
      sdkSubscriptionRecovery.resetRetry(instanceKey);
      return retrySdkSubscriptionLocked(instanceKey, deviceIds);
    });
  }

  private Map<String, Object> retrySdkSubscriptionLocked(String instanceKey, List<String> deviceIds) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    if (!cn.universal.dm.video.VideoSdkPlatforms.managed(instance.getPlatformType())) throw new BaseException("此平台不支持 SDK 告警订阅");
    Map<String, Object> options = readObjectMap(instance.getOptions());
    Map<String, Object> subscription = new LinkedHashMap<>(firstMap(options.get("subscription")));
    if (!parseBoolean(subscription.get("enabled"))) throw new BaseException("请先启用告警订阅");
    Set<String> allowed = new java.util.HashSet<>();
    adapterRegistry.getAdapter(instance.getPlatformType()).resolveSubscriptionDevices(instance)
        .forEach(device -> allowed.add(stringValue(device.get("deviceId"))));
    if (deviceIds == null || deviceIds.isEmpty() || !allowed.containsAll(deviceIds)) {
      throw new BaseException("请选择当前订阅范围内的设备");
    }
    subscription.put("scope", "selected");
    subscription.put("deviceIds", deviceIds);
    subscription.put("partial", true);
    options.put("subscription", subscription);
    instance.setOptions(writeObjectMap(options));
    String applyError = null;
    try {
      applySubscriptionInternal(instance, true);
    } catch (Exception e) {
      applyError = e.getMessage();
    }
    Map<String, Object> result = getSdkSubscriptionStatus(instanceKey);
    if (applyError != null) result.put("applyError", applyError);
    return result;
  }

  /** 生成平台实例唯一编号：标识 + yyyyMMdd + 随机后缀 标识：wvp | isc | icc */
  private void prepareInstanceForSave(VideoPlatformInstance instance) {
    if (instance == null) {
      return;
    }

    instance.setPlatformType(normalizePlatformType(instance.getPlatformType()));
    if (StrUtil.isBlank(instance.getCreatorId())) {
      instance.setCreatorId(SecurityUtils.getUnionId());
    }
    if (StrUtil.isBlank(instance.getTenantId())) {
      instance.setTenantId(resolveTenantId(instance.getCreatorId()));
    }
    if (!"isc".equalsIgnoreCase(instance.getPlatformType())) {
      return;
    }

    String normalizedAuth = normalizeIscAuth(instance.getAuth());
    instance.setAuth(normalizedAuth);
    if (isBlank(instance.getEndpoint())) {
      String endpoint = getAuthValue(normalizedAuth, "endpoint");
      if (!isBlank(endpoint)) {
        instance.setEndpoint(endpoint);
      }
    }
  }

  private String normalizePlatformType(String platformType) {
    if (platformType == null) {
      return null;
    }
    String normalized = platformType.trim().toLowerCase();
    return "ics".equals(normalized) ? "isc" : normalized;
  }

  private String normalizeIscAuth(String auth) {
    if (isBlank(auth)) {
      return auth;
    }

    try {
      JsonNode authNode = objectMapper.readTree(auth);
      if (!(authNode instanceof ObjectNode)) {
        return auth;
      }

      ObjectNode authObject = (ObjectNode) authNode;
      putIfBlank(authObject, "appKey", firstNonBlank(getText(authObject, "appKey"), getText(authObject, "clientId")));
      putIfBlank(
          authObject,
          "appSecret",
          firstNonBlank(getText(authObject, "appSecret"), getText(authObject, "clientSecret")));
      putIfBlank(
          authObject,
          "accessToken",
          firstNonBlank(
              getText(authObject, "accessToken"),
              getText(authObject, "access_token"),
              getText(authObject, "token"),
              getText(authObject, "jwtToken"),
              getText(authObject, "authorization")));
      authObject.remove("username");
      authObject.remove("password");
      authObject.remove("clientId");
      authObject.remove("clientSecret");
      authObject.remove("access_token");
      authObject.remove("token");
      authObject.remove("jwtToken");
      authObject.remove("authorization");
      return objectMapper.writeValueAsString(authObject);
    } catch (Exception e) {
      log.warn("[视频平台] 归一化 ISC 鉴权参数失败: {}", e.getMessage());
      return auth;
    }
  }

  private String getAuthValue(String auth, String fieldName) {
    if (isBlank(auth) || isBlank(fieldName)) {
      return null;
    }
    try {
      JsonNode authNode = objectMapper.readTree(auth);
      return getText(authNode, fieldName);
    } catch (Exception e) {
      return null;
    }
  }

  private void putIfBlank(ObjectNode target, String fieldName, String value) {
    if (target == null || isBlank(fieldName) || isBlank(value)) {
      return;
    }
    if (target.hasNonNull(fieldName) && !isBlank(target.path(fieldName).asText())) {
      return;
    }
    target.put(fieldName, value.trim());
  }

  private String getText(JsonNode node, String fieldName) {
    if (node == null || isBlank(fieldName)) {
      return null;
    }
    JsonNode valueNode = node.get(fieldName);
    if (valueNode == null || valueNode.isNull()) {
      return null;
    }
    String value = valueNode.asText();
    return isBlank(value) ? null : value.trim();
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (!isBlank(value)) {
        return value.trim();
      }
    }
    return null;
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private String generateInstanceKey(String platformType) {
    String prefix;
    if ("wvp".equalsIgnoreCase(platformType)) {
      prefix = "wvp";
    } else if ("isc".equalsIgnoreCase(platformType) || "ics".equalsIgnoreCase(platformType)) {
      prefix = "isc";
    } else if ("icc".equalsIgnoreCase(platformType)) {
      prefix = "icc";
    } else {
      // 兜底：使用平台类型本身作为前缀
      prefix =
          String.valueOf(platformType == null ? "plat" : platformType)
              .replaceAll("[^a-zA-Z0-9]", "");
    }
    java.time.format.DateTimeFormatter fmt =
        java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");
    String day = java.time.LocalDate.now().format(fmt);
    // 随机后缀：三位字母或数字
    String charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    java.util.Random r = new java.util.Random();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 3; i++) {
      sb.append(charset.charAt(r.nextInt(charset.length())));
    }
    return prefix + day + sb.toString();
  }

  private void fillInstanceTenant(VideoPlatformInstance instance) {
    if (instance == null) {
      return;
    }
    if (StrUtil.isBlank(instance.getCreatorId())) {
      instance.setCreatorId(SecurityUtils.getUnionId());
    }
    if (StrUtil.isBlank(instance.getTenantId())) {
      instance.setTenantId(resolveTenantId(instance.getCreatorId()));
    }
  }

  private VideoPlatformInstance requireAccessibleInstance(String instanceKey) {
    VideoPlatformInstance instance = getByInstanceKey(instanceKey);
    if (instance == null) {
      throw new BaseException("平台实例不存在：" + instanceKey);
    }
    if (!cn.universal.admin.video.service.OpenApiVideoScope.assertInstanceIfPresent(instance))
      assertTenantResourceAccess(instance.getTenantId(), instance.getCreatorId(), "无权操作该平台实例");
    return instance;
  }

  @Override
  public Map<String, String> getStreamUrl(
      String instanceKey, String deviceId, String channelId, String streamType) {
    return getStreamUrl(
        instanceKey, deviceId, channelId, streamType, java.util.Collections.emptyMap());
  }

  @Override
  public Map<String, String> getStreamUrl(
      String instanceKey,
      String deviceId,
      String channelId,
      String streamType,
      Map<String, String> options) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    VideoPlatformChannel channel = requireChannel(instanceKey, deviceId, channelId);
    if (videoOperationAccess != null) videoOperationAccess.assertUserOperation("preview");
    assertIsupDeviceEnabled(instance,channel);
    Map<String, Object> fusionConfig = extractFusionConfig(channel, instanceKey, deviceId);
    String playMode = resolvePlayMode(options);
    String fusionMode = resolveFusionMode(fusionConfig);
    // 只有后端融合模式才允许返回/启动融合流；前端 OSD 始终走原始视频流再由浏览器叠加。
    boolean tryFusion = shouldTryFusion(playMode) && isBackendFusionMode(fusionConfig);
    boolean requireFusion = shouldRequireFusion(playMode);
    boolean fusionEnabled = isFusionEnabled(fusionConfig);
    boolean startFusionIfNeeded =
        isBackendFusionMode(fusionConfig)
            && (requireFusion || parseBoolean(options == null ? null : options.get("startFusionIfNeeded")));
    String preferredProtocol =
        StrUtil.blankToDefault(
            stringValue(options == null ? null : options.get("preferProtocol")),
            String.valueOf(fusionConfig.getOrDefault("preferProtocol", "ws_flv")));

    Map<String, String> fusionUrls =
        ensureFusionUrlsForPreview(
            instance, channel, instanceKey, deviceId, channelId, fusionConfig, tryFusion, startFusionIfNeeded);
    boolean fusionReady = fusionEnabled && !fusionUrls.isEmpty();

    // auto/fusion 模式命中已发布后端融合流时，优先把融合流作为播放地址返回。
    if (tryFusion && fusionReady) {
      Map<String, String> result = new LinkedHashMap<>(fusionUrls);
      result.put("preferredUrl", pickPreferredUrl(fusionUrls, preferredProtocol));
      result.put("streamSource", "fusion");
      result.put("playMode", playMode);
      result.put("fusionMode", fusionMode);
      result.put("fusionEnabled", "true");
      result.put("fusionReady", "true");
      result.put("frontendOsdEnabled", "false");
      result.put("backendFusionEnabled", "true");
      return result;
    }

    if (requireFusion) {
      throw new BaseException(
          isBackendFusionMode(fusionConfig)
              ? "融合流未就绪，请先检查融合任务状态"
              : "当前通道未启用后端融合流");
    }

    // 未要求融合流或融合流不可用时回退平台原始流。这里可能触发 WVP 点播取流。
    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    Map<String, Object> adapterOptions = new java.util.HashMap<>();
    if (options != null) {
      adapterOptions.putAll(options);
    }
    adapterOptions.putAll(extractChannelStreamConfig(channel));
    adapterOptions.putAll(extractChannelRuntimeConfig(channel));
    Map<String, String> result;
    if ("hik_isup".equals(instance.getPlatformType())) {
      String mediaNode = resolvePreviewMediaServerId(instance, deviceId, options);
      result = isupPreviewService.start(instance, deviceId, channelId, streamType, adapterOptions,
          resolvePreviewMediaTenantScope(instance, mediaNode), mediaNode);
    } else if ("uniview_sdk".equals(instance.getPlatformType())) {
      if (parseBoolean(options == null ? null : options.get("disableZlmProxy"))) {
        // Native clients explicitly request the authorized device endpoint. Do not allocate
        // SDK/RTP resources or accept caller-supplied hosts, credentials or channel aliases.
        VideoPlatformDevice saved = deviceMapper.selectByPrimaryKey(channel.getDeviceId());
        if (!Integer.valueOf(1).equals(instance.getEnabled()) || saved == null
            || !Integer.valueOf(1).equals(saved.getEnabled())
            || !java.util.Objects.equals(saved.getTenantId(), instance.getTenantId()))
          throw new BaseException("宇视实例或设备已停用、或无权访问");
        Map<String, Object> originOptions = new LinkedHashMap<>();
        originOptions.putAll(extractChannelStreamConfig(channel));
        originOptions.putAll(extractChannelRuntimeConfig(channel));
        originOptions.put("streamMode", "rtsp_first");
        originOptions.put("forceSdkFallback", false);
        originOptions.put("useSdkFallback", false);
        originOptions.put("disableRtspFirst", false);
        result = adapter.getStreamUrl(instance, deviceId, channelId, streamType, originOptions);
      } else {
        String mediaNode = resolvePreviewMediaServerId(instance, deviceId, options);
        result = univiewPreviewService.start(instance, deviceId, channelId, streamType, adapterOptions,
            resolvePreviewMediaTenantScope(instance, mediaNode), mediaNode);
      }
    } else if (isDahuaRegisteredDevice(instance, deviceId)) {
      String mediaNode = resolvePreviewMediaServerId(instance, deviceId, options);
      result = dahuaPreviewService.start(instance, deviceId, channelId, streamType, adapterOptions,
          resolvePreviewMediaTenantScope(instance, mediaNode), mediaNode);
    } else {
      result = adapter.getStreamUrl(instance, deviceId, channelId, streamType, adapterOptions);
    }
    if (result == null) {
      result = new LinkedHashMap<>();
    } else {
      result = new LinkedHashMap<>(result);
    }
    if (shouldProxyOriginStream(instance, result, options)) {
      result = ensureBrowserPlayableOriginStream(instance, deviceId, channelId, streamType, result, options);
    }
    result.put("preferredUrl", pickPreferredUrl(result, preferredProtocol));
    result.putIfAbsent("streamSource", "origin");
    result.put("playMode", playMode);
    result.put("fusionMode", fusionMode);
    result.put("fusionEnabled", String.valueOf(fusionEnabled));
    result.put("fusionReady", String.valueOf(fusionReady));
    result.put("frontendOsdEnabled", String.valueOf(isFrontendOsdMode(fusionConfig)));
    result.put("backendFusionEnabled", String.valueOf(isBackendFusionMode(fusionConfig)));
    return result;
  }

  @Override
  public Map<String, Object> getPlaybackUrl(
      String instanceKey, String deviceId, String channelId, long startTime, long endTime) {
    return getPlaybackUrl(
        instanceKey, deviceId, channelId, startTime, endTime, java.util.Collections.emptyMap());
  }

  private String resolvePlayMode(Map<String, String> options) {
    String value = stringValue(options == null ? null : options.get("playMode"));
    if (StrUtil.isBlank(value)) {
      if (parseBoolean(options == null ? null : options.get("requireFusion"))) {
        return "fusion";
      }
      if (parseBoolean(options == null ? null : options.get("preferFusion"))) {
        return "auto";
      }
      return "origin";
    }
    String normalized = value.trim().toLowerCase();
    if ("fusion".equals(normalized) || "origin".equals(normalized)) {
      return normalized;
    }
    return "auto";
  }

  private boolean shouldTryFusion(String playMode) {
    return "auto".equalsIgnoreCase(playMode) || "fusion".equalsIgnoreCase(playMode);
  }

  private boolean shouldRequireFusion(String playMode) {
    return "fusion".equalsIgnoreCase(playMode);
  }

  private Map<String, String> ensureFusionUrlsForPreview(
      VideoPlatformInstance instance,
      VideoPlatformChannel channel,
      String instanceKey,
      String deviceId,
      String channelId,
      Map<String, Object> fusionConfig,
      boolean tryFusion,
      boolean startFusionIfNeeded) {
    Map<String, String> fusionUrls = extractPublishUrls(fusionConfig);
    // 前端 OSD 或 disabled 模式即使传了 preferFusion/startFusionIfNeeded，也不能触发 FFmpeg。
    if (!tryFusion || !isBackendFusionMode(fusionConfig)) {
      return new LinkedHashMap<>();
    }
    if (isFusionReady(fusionConfig, fusionUrls)) {
      return fusionUrls;
    }
    if (!startFusionIfNeeded || !"wvp".equalsIgnoreCase(instance.getPlatformType())) {
      return new LinkedHashMap<>();
    }
    // 真正启动前先同步一次任务状态，避免重复启动已经在跑但本地状态滞后的任务。
    Map<String, Object> refreshedConfig =
        syncFusionTaskStatusInternal(instanceKey, deviceId, channelId, false, true);
    Map<String, Object> effectiveFusionConfig = refreshedConfig.isEmpty() ? fusionConfig : refreshedConfig;
    fusionUrls = extractPublishUrls(effectiveFusionConfig);
    if (isFusionReady(effectiveFusionConfig, fusionUrls)) {
      return fusionUrls;
    }
    if (isFusionTaskActive(effectiveFusionConfig)) {
      return new LinkedHashMap<>();
    }
    // 只有明确允许 startFusionIfNeeded 时，预览接口才会兜底拉起后端融合任务。
    Map<String, Object> startedConfig =
        startChannelFusionTaskInternal(instance, channel, instanceKey, deviceId, channelId, effectiveFusionConfig);
    fusionUrls = extractPublishUrls(startedConfig);
    return isFusionReady(startedConfig, fusionUrls) ? fusionUrls : new LinkedHashMap<>();
  }

  private boolean shouldProxyOriginStream(
      VideoPlatformInstance instance, Map<String, String> streamUrls, Map<String, String> options) {
    // SDK RTP 预览已拥有 ZLM 会话，返回的 RTSP 是该会话的播放入口。
    if (streamUrls != null && ("gateway_dahua_registration".equals(streamUrls.get("streamSource"))
        || "sdk_zlm_rtp".equals(streamUrls.get("streamMode")))) {
      return false;
    }
    if (instance == null || !supportsOriginRtspProxy(instance.getPlatformType())) {
      return false;
    }
    if (streamUrls == null || StrUtil.isBlank(streamUrls.get("rtsp"))) {
      return false;
    }
    if (parseBoolean(options == null ? null : options.get("disableZlmProxy"))) {
      return false;
    }
    Object proxyValue = options == null ? null : options.get("proxyRtsp");
    return proxyValue == null || parseBoolean(proxyValue);
  }

  private boolean supportsOriginRtspProxy(String platformType) {
    String normalized = normalizePlatformType(platformType);
    return "onvif".equals(normalized) || "hik_sdk".equals(normalized) || "dahua_sdk".equals(normalized) || "tiandy_sdk".equals(normalized) || "uniview_sdk".equals(normalized);
  }

  private boolean isDirectSdkPlatform(String platformType) {
    String normalized = normalizePlatformType(platformType);
    return "hik_sdk".equals(normalized) || "dahua_sdk".equals(normalized) || "tiandy_sdk".equals(normalized) || "uniview_sdk".equals(normalized);
  }

  private Map<String, String> ensureBrowserPlayableOriginStream(
      VideoPlatformInstance instance,
      String deviceId,
      String channelId,
      String streamType,
      Map<String, String> originUrls,
      Map<String, String> options) {
    Map<String, String> result = new LinkedHashMap<>(originUrls);
    String originRtsp = originUrls == null ? "" : originUrls.get("rtsp");
    String platformType = normalizePlatformType(instance.getPlatformType());
    String app = "nexiot-" + safeStreamSegment(platformType);
    String stream =
        buildOriginProxyStreamName(instance.getInstanceKey(), deviceId, channelId, streamType);
    if (cn.universal.admin.video.service.OpenApiVideoScope.isPresent())
      stream += "_openapi_" + UUID.randomUUID().toString().replace("-", "");
    String mediaServerId = null;
    try {
      mediaServerId = resolvePreviewMediaServerId(instance, deviceId, options);
      result.put("originRtsp", originRtsp);
      if (StrUtil.isNotBlank(mediaServerId)) {
        result.put("requestedMediaServerId", mediaServerId);
      }
      result.put("zlmApp", app);
      result.put("zlmStream", stream);
      result.put("proxyRtsp", "true");
      VideoZlmStreamProxyService zlmStreamProxyService = zlmStreamProxyServiceProvider.getIfAvailable();
      if (zlmStreamProxyService == null) {
        result.put("streamSource", "origin");
        result.put("zlmProxyState", "DISABLED");
        result.putIfAbsent("playHint", "未接入可用ZLM代理，当前仅返回RTSP地址");
        return result;
      }
      boolean keepAlive = parseBoolean(options == null ? null : options.get("keepAlive"))
          && !cn.universal.admin.video.service.OpenApiVideoScope.isPresent();
      if (keepAlive) stream += "_" + cn.hutool.crypto.digest.DigestUtil.sha256Hex(
          cn.hutool.json.JSONUtil.toJsonStr(java.util.Arrays.asList(instance.getTenantId(),
              instance.getInstanceKey(), deviceId, channelId, streamType))).substring(0, 16);
      Map<String, String> proxyUrls = keepAlive
          ? zlmStreamProxyService.acquirePreviewLease(resolvePreviewMediaTenantScope(instance, mediaServerId),
              previewViewerKey(instance, deviceId, channelId), mediaServerId, originRtsp, app, stream)
          : zlmStreamProxyService.ensureStreamProxy(
              resolvePreviewMediaTenantScope(instance, mediaServerId), mediaServerId, originRtsp, app, stream);
      result.putAll(proxyUrls);
      if (StrUtil.isNotBlank(mediaServerId)) {
        result.put("requestedMediaServerId", mediaServerId);
      }
      result.put("zlmApp", proxyUrls.getOrDefault("zlmApp", app));
      result.put("zlmStream", proxyUrls.getOrDefault("zlmStream", stream));
      result.put("proxyRtsp", "true");
      result.put("streamSource", "zlm_proxy");
      String proxyState = StrUtil.blankToDefault(proxyUrls.get("proxyState"), "READY");
      result.put("zlmProxyState", proxyState);
      result.put("originStreamSource", platformType);
      result.put("streamProtocol", StrUtil.blankToDefault(firstPlayableProtocol(proxyUrls), "ws_flv"));
      String defaultPlayHint = "WAITING".equalsIgnoreCase(proxyState)
          ? "ZLM代理已创建，正在等待源流注册和视频帧，请稍后重试"
          : "READY".equalsIgnoreCase(proxyState) ? "ZLM代理拉流已就绪" : "ZLM代理尚未就绪";
      result.put("playHint", StrUtil.blankToDefault(proxyUrls.get("playHint"), defaultPlayHint));
    } catch (Exception e) {
      log.warn(
          "[视频直连] ZLM代理预览失败，回退原始RTSP: platformType={}, instanceKey={}, deviceId={}, channelId={}, error={}",
          platformType,
          instance.getInstanceKey(),
          deviceId,
          channelId,
          e.getMessage());
      result.put("originRtsp", originRtsp);
      if (StrUtil.isNotBlank(mediaServerId)) {
        result.put("requestedMediaServerId", mediaServerId);
      }
      result.put("zlmApp", app);
      result.put("zlmStream", stream);
      result.put("proxyRtsp", "true");
      result.put("streamSource", "origin");
      result.put("zlmProxyState", "FAILED");
      result.put("zlmProxyError", StrUtil.sub(e.getMessage(), 0, 300));
      result.putIfAbsent("playHint", "未接入可用ZLM代理，当前仅返回RTSP地址");
    }
    return result;
  }

  private String previewViewerKey(VideoPlatformInstance instance, String deviceId, String channelId) {
    String userId = SecurityUtils.getUnionIdQuietly();
    if (StrUtil.isBlank(userId)) throw new BaseException("视频墙保活需要已登录用户");
    // JSON array avoids ambiguous resource boundaries; never trust a client supplied viewer identity.
    return cn.hutool.json.JSONUtil.toJsonStr(java.util.Arrays.asList(
        instance.getTenantId(), instance.getInstanceKey(), deviceId, channelId, userId));
  }

  private String resolvePreviewMediaTenantScope(
      VideoPlatformInstance instance, String mediaServerId) {
    // 与媒体节点选择器一致：仅当前已认证的真实超级管理员可访问指定的跨租户节点。
    // 不持久化权限，也不从请求参数或平台 tenant=0 推断管理员；后台调度保持平台租户。
    var authentication = SecurityUtils.getAuthentication();
    if (StrUtil.isNotBlank(mediaServerId)
        && authentication != null
        && authentication.isAuthenticated()) {
      IoTUser user = currentTenantUser();
      if (user != null && user.isAdmin()) {
        return null;
      }
    }
    return instance.getTenantId();
  }

  private String resolvePreviewMediaServerId(
      VideoPlatformInstance instance, String deviceId, Map<String, String> options) {
    return resolvePreviewMediaServerId(instance,deviceId,options,false);
  }

  private String resolvePreviewMediaServerId(
      VideoPlatformInstance instance,String deviceId,Map<String,String> options,boolean trustedWorker) {
    String requested =
        firstNonBlank(
            stringValue(options == null ? null : options.get("mediaServerId")),
            stringValue(options == null ? null : options.get("zlmMediaServerId")),
            stringValue(options == null ? null : options.get("previewMediaServerId")));
    if (StrUtil.isNotBlank(requested)) {
      return requested;
    }
    String instanceKey = instance == null ? null : instance.getInstanceKey();
    String deviceMediaServerId;
    if(trustedWorker) {
      VideoPlatformDevice device = requireDeviceEntity(instanceKey, deviceId);
      Example extensionQuery = new Example(VideoPlatformDeviceExt.class);
      extensionQuery.createCriteria().andEqualTo("deviceId", device.getId()).andEqualTo("instanceKey", instanceKey);
      List<VideoPlatformDeviceExt> extensions = deviceExtMapper.selectByExample(extensionQuery);
      deviceMediaServerId = extensions.isEmpty() ? null : extensions.get(0).getMediaServerId();
    } else {
      Map<String,Object> detail=getDeviceDetail(instanceKey,deviceId);
      Map<String,Object> extInfo=firstMap(detail.get("extInfo"));
      deviceMediaServerId=firstNonBlank(stringValue(extInfo.get("mediaServerId")),stringValue(extInfo.get("zlmMediaServerId")),stringValue(extInfo.get("previewMediaServerId")));
    }
    if (StrUtil.isNotBlank(deviceMediaServerId)) {
      return deviceMediaServerId;
    }
    Map<String, Object> auth = readObjectMap(instance == null ? null : instance.getAuth());
    Map<String, Object> instanceOptions = readObjectMap(instance == null ? null : instance.getOptions());
    Map<String, Object> sdkOptions =
        instanceOptions.get("sdk") instanceof Map<?, ?> map ? toLinkedMap(map) : new LinkedHashMap<>();
    return firstNonBlank(
        stringValue(auth.get("zlmMediaServerId")),
        stringValue(auth.get("mediaServerId")),
        stringValue(auth.get("previewMediaServerId")),
        stringValue(instanceOptions.get("zlmMediaServerId")),
        stringValue(instanceOptions.get("mediaServerId")),
        stringValue(instanceOptions.get("previewMediaServerId")),
        stringValue(sdkOptions.get("zlmMediaServerId")),
        stringValue(sdkOptions.get("mediaServerId")),
        stringValue(sdkOptions.get("previewMediaServerId")));
  }

  private String firstPlayableProtocol(Map<String, String> urls) {
    if (urls == null || urls.isEmpty()) {
      return "";
    }
    for (String key :
        List.of(
            "ws_flv",
            "wss_flv",
            "flv",
            "http_flv",
            "https_flv",
            "hls",
            "https_hls",
            "rtc",
            "rtcs",
            "rtmp")) {
      if (isPlayableUrl(urls.get(key))) {
        return key;
      }
    }
    return "";
  }

  private String safeStreamSegment(String value) {
    String normalized =
        StrUtil.blankToDefault(value, "stream").replaceAll("[^A-Za-z0-9_\\-.]", "_");
    return StrUtil.blankToDefault(normalized, "stream");
  }

  private String buildOriginProxyStreamName(
      String instanceKey, String deviceId, String channelId, String streamType) {
    return String.join(
        "_",
        safeStreamSegment(instanceKey),
        safeStreamSegment(deviceId),
        safeStreamSegment(channelId),
        safeStreamSegment(StrUtil.blankToDefault(streamType, "main")));
  }

  private String playbackSessionKey(String instanceKey, String deviceId, String channelId, String stream) {
    return "video:playback:" + instanceKey + ":" + deviceId + ":" + channelId + ":" + stream;
  }

  @Override
  public void controlPlayback(String instanceKey, String deviceId, String channelId,
      String stream, String command, Double speed) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    if (videoOperationAccess != null) videoOperationAccess.assertUserOperation("playbackStreamStatus");
    requireChannel(instanceKey, deviceId, channelId);
    if (stream == null || stream.isBlank()) throw new BaseException("缺少录像流标识");
    String key = playbackSessionKey(instanceKey, deviceId, channelId, stream);
    if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
      throw new BaseException("录像会话已过期或不属于当前通道，请重新播放");
    }
    adapterRegistry.getAdapter(instance.getPlatformType())
        .controlPlayback(instance, deviceId, channelId, stream, command, speed);
    if ("stop".equals(command)) stringRedisTemplate.delete(key);
    else stringRedisTemplate.expire(key, 24, TimeUnit.HOURS);
  }

  @Override
  public Map<String, Object> getPlaybackUrl(
      String instanceKey,
      String deviceId,
      String channelId,
      long startTime,
      long endTime,
      Map<String, String> options) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    if (videoOperationAccess != null) videoOperationAccess.assertUserOperation("playbackStreamStart");
    VideoPlatformChannel channel = requireChannel(instanceKey, deviceId, channelId);
    assertIsupDeviceEnabled(instance,channel);
    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    Map<String, Object> adapterOptions = new java.util.HashMap<>();
    if (options != null) {
      adapterOptions.putAll(options);
    }
    adapterOptions.putAll(extractChannelStreamConfig(channel));
    adapterOptions.putAll(extractChannelRuntimeConfig(channel));
    try {
      if ("hik_isup".equals(instance.getPlatformType())) {
        Map<String, Object> request = new LinkedHashMap<>();
        if (options != null) request.putAll(options);
        request.put("deviceId", deviceId); request.put("channelId", channelId);
        request.put("startTime", startTime); request.put("endTime", endTime);
        return executePlatformOperation(instanceKey, "playbackStreamStart", request);
      }
      Map<String, Object> result = new java.util.HashMap<>(adapter.getPlaybackUrl(
          instance, deviceId, channelId, startTime, endTime, adapterOptions));
      if ("wvp".equalsIgnoreCase(instance.getPlatformType()) && result.get("stream") != null) {
        String stream = String.valueOf(result.get("stream"));
        stringRedisTemplate.opsForValue().set(
            playbackSessionKey(instanceKey, deviceId, channelId, stream), "1", 24, TimeUnit.HOURS);
        result.put("playbackControlSupported", true);
      }
      return result;
    } catch (RuntimeException e) {
      if (!isDirectSdkPlatform(instance.getPlatformType())) {
        throw e;
      }
      log.warn(
          "[视频物模型] 直连SDK回放流暂不可用，返回结构化结果: platformType={}, instanceKey={}, deviceId={}, channelId={}, error={}",
          instance.getPlatformType(),
          instanceKey,
          deviceId,
          channelId,
          e.getMessage());
      return buildDirectSdkPlaybackUnavailable(
          instance, deviceId, channelId, startTime, endTime, adapterOptions, e.getMessage());
    }
  }

  @Override
  public Map<String, Object> stopPreview(
      String instanceKey, String deviceId, String channelId, Map<String, String> options) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    cn.universal.admin.video.service.OpenApiVideoScope.assertChannelIfPresent(instanceKey, deviceId, channelId);
    if (videoOperationAccess != null) videoOperationAccess.assertUserOperation("stopPreview");
    if (options != null && StrUtil.isNotBlank(options.get("previewLeaseId"))) {
      requireChannel(instanceKey, deviceId, channelId);
      VideoZlmStreamProxyService proxy = zlmStreamProxyServiceProvider.getIfAvailable();
      if (proxy == null) return Map.of("stopped", true, "alreadyClosed", true);
      return proxy.releasePreviewLease(previewViewerKey(instance, deviceId, channelId), options.get("previewLeaseId"));
    }
    if ("hik_isup".equals(instance.getPlatformType())) {
      requireChannel(instanceKey, deviceId, channelId);
      return isupPreviewService.stop(instance, deviceId, channelId, options);
    }
    if ("uniview_sdk".equals(instance.getPlatformType())) {
      requireChannel(instanceKey, deviceId, channelId);
      return univiewPreviewService.stop(instance, deviceId, channelId, options);
    }
    if (isDahuaRegisteredDevice(instance, deviceId)) {
      requireChannel(instanceKey, deviceId, channelId);
      return dahuaPreviewService.stop(instance, deviceId, channelId, options);
    }
    Map<String, String> safeOptions =
        options == null ? new LinkedHashMap<>() : new LinkedHashMap<>(options);
    String streamType = StrUtil.blankToDefault(safeOptions.get("streamType"), "main");
    String platformType = normalizePlatformType(instance.getPlatformType());
    String app =
        firstNonBlank(
            safeOptions.get("app"),
            safeOptions.get("zlmApp"),
            "nexiot-" + safeStreamSegment(platformType));
    String stream =
        firstNonBlank(
            safeOptions.get("stream"),
            safeOptions.get("zlmStream"),
            buildOriginProxyStreamName(instanceKey, deviceId, channelId, streamType));
    String mediaServerId =
        firstNonBlank(
            safeOptions.get("mediaServerPrimaryId"),
            safeOptions.get("mediaServerId"),
            safeOptions.get("requestedMediaServerId"),
            safeOptions.get("zlmMediaServerId"),
            safeOptions.get("previewMediaServerId"));
    if (StrUtil.isBlank(mediaServerId)) {
      mediaServerId = resolvePreviewMediaServerId(instance, deviceId, safeOptions);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("deviceId", deviceId);
    result.put("channelId", channelId);
    result.put("streamType", streamType);
    result.put("app", app);
    result.put("stream", stream);
    result.put("mediaServerId", mediaServerId);
    result.put("ts", System.currentTimeMillis());

    VideoZlmStreamProxyService zlmStreamProxyService = zlmStreamProxyServiceProvider.getIfAvailable();
    if (zlmStreamProxyService == null) {
      result.put("stopped", false);
      result.put("skipped", true);
      result.put("message", "未接入可用ZLM代理服务");
      return result;
    }
    if (StrUtil.isBlank(mediaServerId)) {
      Map<String, String> located = locatePreviewStreamForStop(instanceKey, deviceId, channelId, streamType, safeOptions);
      mediaServerId =
          firstNonBlank(
              located.get("mediaServerPrimaryId"),
              located.get("mediaServerId"),
              located.get("requestedMediaServerId"),
              located.get("zlmMediaServerId"),
              located.get("previewMediaServerId"));
      app = firstNonBlank(located.get("app"), located.get("zlmApp"), app);
      stream = firstNonBlank(located.get("stream"), located.get("zlmStream"), stream);
      result.put("app", app);
      result.put("stream", stream);
      result.put("mediaServerId", mediaServerId);
      if (located.containsKey("locateError")) {
        result.put("locateError", located.get("locateError"));
      }
      if (StrUtil.isBlank(mediaServerId)) {
        result.put("stopped", false);
        result.put("skipped", true);
        result.put("message", "未指定或未配置流媒体节点，无法定位要释放的ZLM代理流");
        return result;
      }
    }

    try {
      Map<String, String> releaseResult =
          zlmStreamProxyService.releaseStreamProxy(
              resolvePreviewMediaTenantScope(instance, mediaServerId), mediaServerId, app, stream);
      result.put("stopped", true);
      result.put("releaseResult", releaseResult);
      if (releaseResult != null) {
        result.putIfAbsent("countHit", releaseResult.get("count_hit"));
        result.putIfAbsent("countClosed", releaseResult.get("count_closed"));
        result.putIfAbsent("mediaServerPrimaryId", releaseResult.get("mediaServerPrimaryId"));
        result.putIfAbsent("zlmMediaServerId", releaseResult.get("mediaServerId"));
        result.putIfAbsent("alreadyClosed", releaseResult.get("alreadyClosed"));
      }
    } catch (Exception e) {
      log.warn(
          "[视频物模型] 停止预览释放ZLM代理流失败，按幂等清理返回: instanceKey={}, deviceId={}, channelId={}, mediaServerId={}, app={}, stream={}, error={}",
          instanceKey,
          deviceId,
          channelId,
          mediaServerId,
          app,
          stream,
          e.getMessage());
      result.put("stopped", false);
      result.put("skipped", true);
      result.put("releaseError", StrUtil.sub(e.getMessage(), 0, 300));
      result.put("message", "ZLM代理流可能已释放或不存在");
    }
    return result;
  }

  private Map<String, String> locatePreviewStreamForStop(
      String instanceKey,
      String deviceId,
      String channelId,
      String streamType,
      Map<String, String> options) {
    Map<String, String> locateOptions =
        options == null ? new LinkedHashMap<>() : new LinkedHashMap<>(options);
    locateOptions.put("streamType", StrUtil.blankToDefault(streamType, "main"));
    locateOptions.put("proxyRtsp", "true");
    try {
      return getStreamUrl(instanceKey, deviceId, channelId, streamType, locateOptions);
    } catch (Exception e) {
      log.warn(
          "[视频物模型] 停止预览定位ZLM节点失败: instanceKey={}, deviceId={}, channelId={}, streamType={}, error={}",
          instanceKey,
          deviceId,
          channelId,
          streamType,
          e.getMessage());
      return Map.of(
          "locateError",
          StrUtil.sub(StrUtil.blankToDefault(e.getMessage(), e.getClass().getSimpleName()), 0, 300));
    }
  }

  @Override
  public Map<String, Object> captureSnapshot(
      String instanceKey, String deviceId, String channelId, Map<String, Object> options) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    if (videoOperationAccess != null) videoOperationAccess.assertUserOperation("snapshot");
    VideoPlatformChannel channel = requireChannel(instanceKey, deviceId, channelId);
    assertIsupDeviceEnabled(instance,channel);
    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    Map<String, Object> params = options == null ? new LinkedHashMap<>() : new LinkedHashMap<>(options);
    params.put("deviceId", deviceId);
    params.put("channelId", channelId);
    Map<String, Object> adapterOptions = new java.util.HashMap<>(params);
    adapterOptions.putAll(extractChannelStreamConfig(channel));
    adapterOptions.putAll(extractChannelRuntimeConfig(channel));
    String platformType = normalizePlatformType(instance.getPlatformType());
    boolean forceZlmSnapshot =
        "platform_frame".equals(params.get("source")) || parseBoolean(params.get("forceZlmSnapshot")) || parseBoolean(params.get("disableSdkSnapshot"));

    String sdkSnapshotError = null;
    if (!forceZlmSnapshot) {
      try {
        Map<String, Object> sdkResult = adapter.snapshot(instance, deviceId, channelId, adapterOptions);
        if ("hik_isup".equals(platformType) && sdkResult != null && sdkResult.get("requestId") != null) {
          Map<String, Object> result = new LinkedHashMap<>(sdkResult);
          result.put("imageSource", "device_snapshot"); result.put("provider", "hik_isup_sdk");
          return result;
        }
        if (hasSnapshotPayload(sdkResult)) {
          Map<String, Object> result =
              hasOssSnapshot(sdkResult)
                  ? new LinkedHashMap<>(sdkResult)
                  : snapshotOssHelper.uploadSnapshotIfPresent(instance, params, sdkResult, platformType);
          result.putIfAbsent("provider", platformType + "_sdk");
          result.putIfAbsent("fallback", false);
          return result;
        }
      } catch (Exception e) {
        sdkSnapshotError = e.getMessage();
        log.warn(
            "[视频物模型] SDK抓图不可用，准备使用ZLM抓图: platformType={}, instanceKey={}, deviceId={}, channelId={}, error={}",
            platformType,
            instanceKey,
            deviceId,
            channelId,
            e.getMessage());
      }
    }

    try {
      if ("hik_isup".equals(platformType) && !forceZlmSnapshot)
        throw new BaseException("ISUP 原生抓图未受理，请选择平台画面抓图");
      Map<String, Object> zlmResult =
          captureSnapshotViaZlm(
              instance, instanceKey, deviceId, channelId, StrUtil.blankToDefault(stringValue(params.get("streamType")), "main"), params);
      if (StrUtil.isNotBlank(sdkSnapshotError)) {
        zlmResult.put("sdkSnapshotError", StrUtil.sub(sdkSnapshotError, 0, 300));
      }
      zlmResult.putIfAbsent("provider", "zlm_snapshot");
      zlmResult.putIfAbsent("fallback", true);
      if ("hik_isup".equals(platformType)) zlmResult.put("imageSource", "platform_frame");
      return snapshotOssHelper.uploadSnapshotIfPresent(instance, params, zlmResult, platformType);
    } catch (Exception e) {
      String message =
          StrUtil.isBlank(sdkSnapshotError)
              ? e.getMessage()
              : "SDK抓图失败: " + sdkSnapshotError + "；ZLM抓图失败: " + e.getMessage();
      throw new BaseException("视频抓图失败: " + message);
    }
  }

  @Override
  public Map<String,Object> captureAlarmPlatformFrame(String instanceKey,String tenantId,String deviceId,String channelId) {
    // A background worker has no user session. Authorization is the durable receipt's saved scope
    // and the current database policy; public captureSnapshot/getStreamUrl keep their user guards.
    VideoPlatformInstance instance=instanceMapper.selectOne(VideoPlatformInstance.builder().instanceKey(instanceKey).build());
    if(instance==null||StrUtil.isBlank(tenantId)||!java.util.Objects.equals(instance.getTenantId(),tenantId)
        ||!"hik_isup".equals(instance.getPlatformType())||!Integer.valueOf(1).equals(instance.getEnabled()))
      throw new BaseException("告警画面所属实例不存在、已停用或租户已变更");
    Map<String,Object> subscription=firstMap(readObjectMap(instance.getOptions()).get("subscription"));
    if(!Boolean.TRUE.equals(subscription.get("enabled"))||!"platform_frame".equals(firstMap(subscription.get("eventPolicy")).get("pictureSource"))
        ||"selected".equals(subscription.get("scope"))&&(!(subscription.get("deviceIds") instanceof List<?> ids)||!ids.contains(deviceId)))
      throw new BaseException("告警平台画面策略已停用或设备不在订阅范围");
    VideoPlatformChannel channel=requireChannel(instanceKey,deviceId,channelId);
    assertIsupDeviceEnabled(instance,channel);
    Map<String,Object> params=new LinkedHashMap<>(Map.of("source","platform_frame","streamType","main","deviceId",deviceId,"channelId",channelId,"failOnOssUpload",true));
    Map<String,Object> nativeOptions=new LinkedHashMap<>(params);
    nativeOptions.putAll(extractChannelStreamConfig(channel));nativeOptions.putAll(extractChannelRuntimeConfig(channel));
    VideoZlmStreamProxyService zlm=zlmStreamProxyServiceProvider.getIfAvailable();
    if(zlm==null)throw new BaseException("未接入可用ZLM代理服务");
    String node=resolvePreviewMediaServerId(instance,deviceId,Map.of(),true);
    Map<String,String> streamUrls=isupPreviewService.start(instance,deviceId,channelId,"main",nativeOptions,tenantId,node);
    Map<String,Object> result=captureSnapshotFromStream(instance,instanceKey,deviceId,channelId,"main",zlm,streamUrls,toStringMap(params),tenantId);
    result.put("imageSource","platform_frame");result.put("provider","zlm_snapshot");
    return cn.universal.ossm.oss.context.OssTenantContext.run(tenantId,
        ()->snapshotOssHelper.uploadSnapshotIfPresent(instance,params,result,"hik_isup"));
  }

  private Map<String, Object> buildDirectSdkPlaybackUnavailable(
      VideoPlatformInstance instance,
      String deviceId,
      String channelId,
      long startTime,
      long endTime,
      Map<String, Object> options,
      String errorMessage) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("deviceId", deviceId);
    result.put("channelId", channelId);
    result.put("startTime", startTime);
    result.put("endTime", endTime);
    result.put("platformType", normalizePlatformType(instance == null ? null : instance.getPlatformType()));
    result.put("playbackSupported", false);
    result.put("nativeBridgeImplemented", false);
    result.put("streamSource", "direct_sdk_unimplemented");
    result.put("message", "直连设备 SDK 本地录像回放流暂未实现，请使用平台录像能力或配置厂商回放RTSP模板后再启用");
    result.put("sdkGatewayError", StrUtil.sub(errorMessage, 0, 500));
    result.put("ts", System.currentTimeMillis());
    if (options != null) {
      putIfNotBlank(result, "recordLocation", options.get("recordLocation"));
      putIfNotBlank(result, "recordSource", options.get("recordSource"));
      putIfNotBlank(result, "playbackRtspTemplate", options.get("playbackRtspTemplate"));
    }
    return result;
  }

  private Map<String, Object> captureSnapshotViaZlm(
      VideoPlatformInstance instance,
      String instanceKey,
      String deviceId,
      String channelId,
      String streamType,
      Map<String, Object> params) {
    VideoZlmStreamProxyService zlmStreamProxyService = zlmStreamProxyServiceProvider.getIfAvailable();
    if (zlmStreamProxyService == null) {
      throw new BaseException("未接入可用ZLM代理服务");
    }
    Map<String, String> streamOptions = toStringMap(params);
    streamOptions.put("streamType", StrUtil.blankToDefault(streamType, "main"));
    streamOptions.put("proxyRtsp", "true");
    Map<String, String> streamUrls =
        getStreamUrl(instanceKey, deviceId, channelId, StrUtil.blankToDefault(streamType, "main"), streamOptions);
    return captureSnapshotFromStream(instance,instanceKey,deviceId,channelId,streamType,
        zlmStreamProxyService,streamUrls,streamOptions,null);
  }

  private Map<String,Object> captureSnapshotFromStream(VideoPlatformInstance instance,String instanceKey,
      String deviceId,String channelId,String streamType,VideoZlmStreamProxyService zlmStreamProxyService,
      Map<String,String> streamUrls,Map<String,String> streamOptions,String trustedMediaScope) {
    String platformType = normalizePlatformType(instance.getPlatformType());
    String app =
        firstNonBlank(
            streamUrls.get("app"),
            streamUrls.get("zlmApp"),
            "nexiot-" + safeStreamSegment(platformType));
    String stream =
        firstNonBlank(
            streamUrls.get("stream"),
            streamUrls.get("zlmStream"),
            buildOriginProxyStreamName(instanceKey, deviceId, channelId, streamType));
    String mediaServerId =
        firstNonBlank(
            streamUrls.get("mediaServerPrimaryId"),
            streamUrls.get("mediaServerId"),
            streamUrls.get("requestedMediaServerId"),
            streamOptions.get("mediaServerId"),
            streamOptions.get("zlmMediaServerId"),
            streamOptions.get("previewMediaServerId"));
    String sourceUrl =
        firstNonBlank(
            streamUrls.get("originRtsp"),
            streamUrls.get("originUrl"),
            streamUrls.get("rtsp"));
    boolean univiewNativePreview = "uniview_sdk".equals(platformType)
        && StrUtil.isNotBlank(streamUrls.get("previewId"));
    Map<String, Object> snapshot;
    try {
      String mediaScope = trustedMediaScope==null ? resolvePreviewMediaTenantScope(instance, mediaServerId) : trustedMediaScope;
      if ("hik_isup".equals(platformType) || univiewNativePreview) {
        long deadline = System.currentTimeMillis() + 10000;
        while (!zlmStreamProxyService.isRtpPlaybackReady(mediaScope, mediaServerId, stream)) {
          if (System.currentTimeMillis() >= deadline) throw new BaseException("平台画面尚未就绪，抓图未完成");
          try { Thread.sleep(200); }
          catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new BaseException("平台抓图已中断"); }
        }
      }
      snapshot = zlmStreamProxyService.captureStreamSnapshot(mediaScope, mediaServerId, sourceUrl, app, stream);
    } finally {
      if ("hik_isup".equals(platformType)) {
        // This call created an independent preview; release only that stream, including failed captures.
        try { isupPreviewService.stop(instance, deviceId, channelId, Map.of("stream", stream, "streamType", streamType)); }
        catch (RuntimeException cleanup) { log.warn("ISUP抓图预览释放待巡检 stream={}", stream); }
      } else if (univiewNativePreview) {
        Map<String, String> stopOptions = new LinkedHashMap<>();
        stopOptions.put("stream", stream);
        stopOptions.put("streamType", StrUtil.blankToDefault(streamUrls.get("streamType"), StrUtil.blankToDefault(streamType, "main")));
        stopOptions.put("previewId", streamUrls.get("previewId"));
        if (StrUtil.isNotBlank(streamUrls.get("ownerNode"))) stopOptions.put("ownerNode", streamUrls.get("ownerNode"));
        try { univiewPreviewService.stop(instance, deviceId, channelId, stopOptions); }
        catch (RuntimeException cleanup) { log.warn("宇视抓图预览释放待巡检 previewId={} stream={}", streamUrls.get("previewId"), stream); }
      }
    }
    if (!hasSnapshotPayload(snapshot)) {
      throw new BaseException("ZLM未返回抓图图片数据");
    }
    Map<String, Object> result = new LinkedHashMap<>(snapshot);
    result.put("deviceId", deviceId);
    result.put("channelId", channelId);
    result.put("streamType", streamType);
    result.put("app", app);
    result.put("stream", stream);
    result.put("mediaServerId", mediaServerId);
    result.put("streamSource", "zlm_proxy");
    result.put("originStreamSource", platformType);
    result.put("ts", System.currentTimeMillis());
    return result;
  }

  private Map<String, String> toStringMap(Map<String, Object> values) {
    Map<String, String> result = new LinkedHashMap<>();
    if (values == null) {
      return result;
    }
    values.forEach(
        (key, value) -> {
          if (StrUtil.isNotBlank(key) && value != null) {
            result.put(key, String.valueOf(value));
          }
        });
    return result;
  }

  private boolean hasSnapshotPayload(Map<String, Object> result) {
    if (result == null || result.isEmpty()) {
      return false;
    }
    for (String key :
        List.of(
            "ossUrl",
            "snapshotUrl",
            "snapshotOssUrl",
            "imageUrl",
            "pictureUrl",
            "picUrl",
            "url",
            "snapshotBytes",
            "imageBytes",
            "snapshotBase64",
            "snapshotData",
            "imageBase64",
            "imageData",
            "base64")) {
      Object value = result.get(key);
      if (value instanceof byte[] bytes && bytes.length > 0) {
        return true;
      }
      if (value != null && StrUtil.isNotBlank(String.valueOf(value))) {
        return true;
      }
    }
    Object data = result.get("data");
    if (data instanceof Map<?, ?> map) {
      Map<String, Object> nested = new LinkedHashMap<>();
      map.forEach((key, value) -> {
        if (key != null) {
          nested.put(String.valueOf(key), value);
        }
      });
      return hasSnapshotPayload(nested);
    }
    return false;
  }

  private boolean hasOssSnapshot(Map<String, Object> result) {
    if (result == null || result.isEmpty()) {
      return false;
    }
    return StrUtil.isNotBlank(stringValue(result.get("snapshotOssUrl")))
        || StrUtil.isNotBlank(stringValue(result.get("ossUrl")))
        || StrUtil.isNotBlank(stringValue(result.get("ossId")))
        || Boolean.TRUE.equals(result.get("ossUploadSuccess"));
  }

  @Override
  public void controlPTZ(
      String instanceKey, String deviceId, String channelId, String command, int speed) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    if (videoOperationAccess != null) videoOperationAccess.assertUserOperation("ptz");
    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    assertIsupDeviceEnabled(instance,requireChannel(instanceKey, deviceId, channelId));
    adapter.controlPTZ(instance, deviceId, channelId, command, speed);
  }

  @Override
  public List<Map<String, Object>> queryRecords(
      String instanceKey, String deviceId, String channelId, long startTime, long endTime) {
    return queryRecords(
        instanceKey, deviceId, channelId, startTime, endTime, java.util.Collections.emptyMap());
  }

  @Override
  public List<Map<String, Object>> queryRecords(
      String instanceKey,
      String deviceId,
      String channelId,
      long startTime,
      long endTime,
      Map<String, String> options) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    if (videoOperationAccess != null) videoOperationAccess.assertUserOperation("recordingSearch");
    VideoPlatformChannel channel = requireChannel(instanceKey, deviceId, channelId);
    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    Map<String, Object> adapterOptions = new java.util.HashMap<>();
    if (options != null) {
      adapterOptions.putAll(options);
    }
    adapterOptions.putAll(extractChannelStreamConfig(channel));
    adapterOptions.putAll(extractChannelRuntimeConfig(channel));
    return adapter.queryRecords(instance, deviceId, channelId, startTime, endTime, adapterOptions);
  }

  @Override
  public List<Map<String, Object>> listDeviceChannels(String instanceKey, String deviceId) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    Example ex = new Example(VideoPlatformDevice.class);
    ex.createCriteria().andEqualTo("instanceKey", instanceKey).andEqualTo("deviceId", deviceId)
        .andEqualTo("tenantId", instance.getTenantId());
    List<VideoPlatformDevice> found = deviceMapper.selectByExample(ex);
    if (found.isEmpty()) return List.of();
    Map<String, Object> config = VideoDeviceNames.configuration(found.get(0).getConfiguration());
    Map<String, Object> sources = VideoDeviceNames.object(config.get(VideoDeviceNames.SOURCE_CHANNELS));
    Map<String, Object> aliases = VideoDeviceNames.object(config.get(VideoDeviceNames.CHANNEL_OVERRIDES));
    Map<String, Map<String, Object>> snapshots = new LinkedHashMap<>();
    if (config.get("channelList") instanceof List<?> list) for (Object value : list) {
      Map<String, Object> channel = VideoDeviceNames.object(value);
      snapshots.put(VideoDeviceNames.text(channel.get("channelId")), channel);
    }
    List<Map<String, Object>> result = new ArrayList<>();
    for (Map<String, Object> row : listChannels(instanceKey, deviceId, null)) {
      String id = String.valueOf(row.get("channelId"));
      Map<String, Object> channel = new LinkedHashMap<>(snapshots.getOrDefault(id, Map.of()));
      channel.putAll(row);
      channel.put("name", row.get("channelName"));
      channel.put("status", row.get("channelStatus"));
      channel.put("sourceChannelName", VideoDeviceNames.text(sources.get(id)));
      channel.put("nameOverridden", aliases.containsKey(VideoDeviceNames.physicalId(id)));
      result.add(channel);
    }
    return result;
  }

  @Override
  public void updateChannelName(String instanceKey, String deviceId, String channelId, Map<String, Object> updates) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    requireChannel(instanceKey, deviceId, channelId);
    deviceNameService.renameChannel(instance, deviceId, channelId, updates);
  }

  @Override
  public Map<String, Object> executePlatformOperation(
      String instanceKey, String operation, Map<String, Object> params) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    if (StrUtil.isBlank(operation)) {
      throw new BaseException("平台操作标识不能为空");
    }
    operation = operation.trim();
    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    Map<String, Object> safeParams = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
    cn.universal.admin.video.service.OpenApiVideoScope.assertChannelIfPresent(instanceKey,
        stringValue(safeParams.get("deviceId")), stringValue(safeParams.get("channelId")));
    if (videoOperationAccess != null) videoOperationAccess.assertUserOperation(operation);
    if (StrUtil.isNotBlank(stringValue(safeParams.get("previewLeaseId")))
        && List.of("previewHeartbeat", "stopPreview", "previewStop", "stopStream").contains(operation)) {
      String deviceId = stringValue(safeParams.get("deviceId"));
      String channelId = stringValue(safeParams.get("channelId"));
      requireChannel(instanceKey, deviceId, channelId);
      if ("previewHeartbeat".equals(operation)) {
        VideoZlmStreamProxyService proxy = zlmStreamProxyServiceProvider.getIfAvailable();
        return proxy == null ? Map.of("active", false)
            : proxy.renewPreviewLease(previewViewerKey(instance, deviceId, channelId),
                stringValue(safeParams.get("previewLeaseId")));
      }
      Map<String, String> stopOptions = new LinkedHashMap<>();
      for (Map.Entry<String, Object> entry : safeParams.entrySet()) {
        if (entry.getValue() != null) stopOptions.put(entry.getKey(), String.valueOf(entry.getValue()));
      }
      return stopPreview(instanceKey, deviceId, channelId, stopOptions);
    }
    if (cn.universal.admin.video.service.WvpConfigurationAccess.handles(operation)) {
      safeParams = wvpConfigurationAccess.validate(instance, operation, safeParams);
    }
    if ("uniview_sdk".equals(instance.getPlatformType())) {
      if (List.of("previewStreamStart", "previewStreamStatus", "previewStreamStop").contains(operation))
        throw new BaseException("请使用平台预览接口");
      String deviceId = stringValue(safeParams.get("deviceId"));
      String channelId = stringValue(safeParams.get("channelId"));
      if (List.of("stopPreview", "previewStop", "stopStream").contains(operation)) {
        requireChannel(instanceKey, deviceId, channelId);
        Map<String, String> stopOptions = new LinkedHashMap<>();
        safeParams.forEach((key, value) -> { if (value != null) stopOptions.put(key, String.valueOf(value)); });
        return stopPreview(instanceKey, deviceId, channelId, stopOptions);
      }
      if ("previewHeartbeat".equals(operation)) {
        requireChannel(instanceKey, deviceId, channelId);
        univiewPreviewService.assertOwner(safeParams.get("ownerNode"));
        String stream = firstNonBlank(stringValue(safeParams.get("zlmStream")), stringValue(safeParams.get("stream")), stringValue(safeParams.get("streamKey")));
        return univiewPreviewService.heartbeat(instance, deviceId, channelId, stream, safeParams.get("previewId"));
      }
    }
    if ("dahua_sdk".equals(instance.getPlatformType())) {
      if (List.of("configureActiveRegistration", "stopActiveRegistration", "previewStreamStart", "previewStreamStatus", "previewStreamStop").contains(operation.trim()))
        throw new BaseException("请使用已保存的实例配置或平台预览接口");
      if ("activeRegistrationDevices".equals(operation.trim()))
        return Map.of("devices", maskSdkDeviceSecrets(adapter.listDevices(instance, Map.of("accessMode", "active_registration"))));
      if ("activeRegistrationStatus".equals(operation.trim())) {
        Map<String, Object> result = new LinkedHashMap<>(adapter.executeOperation(instance, operation.trim(), Map.of()));
        result.put("registrationSync", dahuaRegistrationSync.metadata(instanceKey));
        return result;
      }
      String deviceId = java.util.Objects.toString(safeParams.get("deviceId"), "");
      if (List.of("stopPreview", "previewStop", "stopStream").contains(operation.trim())) {
        String channelId = java.util.Objects.toString(safeParams.get("channelId"), "");
        requireChannel(instanceKey, deviceId, channelId);
        Map<String, String> stopOptions = new LinkedHashMap<>();
        safeParams.forEach((key, value) -> { if (value != null) stopOptions.put(key, String.valueOf(value)); });
        return stopPreview(instanceKey, deviceId, channelId, stopOptions);
      }
      if (!deviceId.isBlank() && "previewHeartbeat".equals(operation.trim())
          && isDahuaRegisteredDevice(instance, deviceId)) {
        String channelId = java.util.Objects.toString(safeParams.get("channelId"), "");
        requireChannel(instanceKey, deviceId, channelId);
        dahuaPreviewService.assertOwner(safeParams.get("ownerNode"));
        String stream = firstNonBlank(stringValue(safeParams.get("zlmStream")), stringValue(safeParams.get("stream")), stringValue(safeParams.get("streamKey")));
        return dahuaPreviewService.heartbeat(instance, deviceId, channelId, stream, safeParams.get("previewId"));
      }
    }
    if ("hik_isup".equals(instance.getPlatformType())) {
      var definition = cn.universal.dm.video.VideoOperationRegistry.requirePublic(operation);
      safeParams = cn.universal.dm.video.VideoOperationRegistry.validate(definition, safeParams);
      operation = definition.id();
      validateIsupOperationResource(instance, definition, safeParams);
      if ("preview".equals(operation)) return new LinkedHashMap<>(getStreamUrl(instanceKey,
          stringValue(safeParams.get("deviceId")), stringValue(safeParams.get("channelId")),
          java.util.Objects.toString(safeParams.get("streamType"), "main")));
      if ("ptz".equals(operation)) {
        controlPTZ(instanceKey, stringValue(safeParams.get("deviceId")), stringValue(safeParams.get("channelId")),
            stringValue(safeParams.get("command")), safeParams.get("speed") instanceof Number n ? n.intValue() : 50);
        return Map.of("accepted", true);
      }
      if ("snapshot".equals(operation)) return captureSnapshot(instanceKey, stringValue(safeParams.get("deviceId")),
          stringValue(safeParams.get("channelId")), safeParams);
      if (List.of("recordingStart", "playbackStart", "platformRecordingStart").contains(operation)) {
        Map<String, Object> task = new LinkedHashMap<>(safeParams);
        task.put("mode", "platformRecordingStart".equals(operation) ? "platform" : "playbackStart".equals(operation) ? "instant" : "export");
        return sdkRecordingTasks.create(instance, task, SecurityUtils.getUnionId());
      }
    }
    if ("hik_isup".equals(instance.getPlatformType()) && "previewHeartbeat".equals(operation.trim())) {
      isupPreviewService.assertOwner(safeParams.get("ownerNode"));
      String deviceId = java.util.Objects.toString(safeParams.get("deviceId"), "");
      String channelId = java.util.Objects.toString(safeParams.get("channelId"), "");
      requireChannel(instanceKey, deviceId, channelId);
      String stream = firstNonBlank(stringValue(safeParams.get("zlmStream")), stringValue(safeParams.get("stream")), stringValue(safeParams.get("streamKey")));
      return isupPreviewService.heartbeat(instance, deviceId, channelId, stream);
    }
    if ("hik_isup".equals(instance.getPlatformType()) && List.of("stopPreview", "previewStop", "stopStream").contains(operation.trim())) {
      Map<String, String> stopOptions = new LinkedHashMap<>();
      safeParams.forEach((key, value) -> { if (value != null) stopOptions.put(key, String.valueOf(value)); });
      return stopPreview(instanceKey, java.util.Objects.toString(safeParams.get("deviceId"), ""),
          java.util.Objects.toString(safeParams.get("channelId"), ""), stopOptions);
    }
    if ("playbackStreamStart".equals(operation.trim())) {
      String deviceId = java.util.Objects.toString(safeParams.get("deviceId"), "");
      requireChannel(instanceKey, deviceId, java.util.Objects.toString(safeParams.get("channelId"), ""));
      Map<String, String> mediaOptions = new LinkedHashMap<>();
      for (String key : List.of("mediaServerId", "zlmMediaServerId", "previewMediaServerId")) {
        if (safeParams.get(key) != null) mediaOptions.put(key, String.valueOf(safeParams.get(key)));
      }
      String node = resolvePreviewMediaServerId(instance, deviceId, mediaOptions);
      return sdkPlaybackService.start(instance, safeParams, resolvePreviewMediaTenantScope(instance, node), node);
    }
    if (List.of("playbackStreamStatus", "playbackStreamPause", "playbackStreamResume", "playbackStreamSeek", "playbackStreamSpeed", "playbackStreamStop").contains(operation.trim()))
      return sdkPlaybackService.control(instance, operation.trim(), safeParams);
    Map<String,Object> gatewayParams=new LinkedHashMap<>(safeParams);
    if("hik_isup".equals(instance.getPlatformType())&&"evidenceTimeline".equals(operation))gatewayParams.put("onlyEvidence",false);
    Map<String, Object> result = adapter.executeOperation(instance, operation.trim(), gatewayParams);
    if("hik_isup".equals(instance.getPlatformType())&&"evidenceTimeline".equals(operation)&&sdkEvidenceTimeline!=null)
      result=sdkEvidenceTimeline.enrich(instance,safeParams,result);
    if ("hik_isup".equals(instance.getPlatformType()) && result.get("operations") instanceof List<?> rows && videoOperationAccess != null) {
      result = new LinkedHashMap<>(result);
      List<Map<String, Object>> operations = new ArrayList<>();
      for (Object row : rows) if (row instanceof Map<?,?> map) {
        Map<String, Object> item = toLinkedMap(map);
        String id = java.util.Objects.toString(item.getOrDefault("aliasOf", item.get("id")), "");
        var definition = cn.universal.dm.video.VideoOperationRegistry.find(id);
        item.put("allowed", definition.isPresent() && videoOperationAccess.allowed(id));
        definition.ifPresent(value -> item.put("permission", value.permission()));
        operations.add(item);
      }
      result.put("operations", operations); result.put("catalogVersion", cn.universal.dm.video.VideoOperationRegistry.VERSION);
    }
    if ("hik_isup".equals(instance.getPlatformType()) && List.of("isupStatus", "instanceStatus").contains(operation.trim())) {
      result = new LinkedHashMap<>(result);
      result.put("registrationSync", hikIsupRegistrationSync.metadata(instanceKey));
      result.put("applicationState", hikIsupRegistrationSync.applicationState(instance));
    }
    return result;
  }

  private void validateIsupOperationResource(VideoPlatformInstance instance,
      cn.universal.dm.video.VideoOperationRegistry.Operation definition, Map<String, Object> parameters) {
    String deviceId = java.util.Objects.toString(parameters.get("deviceId"), "");
    String channelId = java.util.Objects.toString(parameters.get("channelId"), "");
    if (deviceId.isBlank()) {
      if (!channelId.isBlank()) throw new BaseException("查询通道时必须指定设备");
      return;
    }
    VideoPlatformDevice device = deviceMapper.selectOne(VideoPlatformDevice.builder()
        .instanceKey(instance.getInstanceKey()).deviceId(deviceId).build());
    if (device == null || !java.util.Objects.equals(device.getTenantId(), instance.getTenantId()))
      throw new BaseException("设备不存在或无权访问");
    if (!definition.offlineAllowed() && (!Integer.valueOf(1).equals(instance.getEnabled())
        || Integer.valueOf(0).equals(device.getEnabled()))) throw new BaseException("实例或设备已停用");
    if (!channelId.isBlank() && definition.scope() != cn.universal.dm.video.VideoOperationRegistry.Scope.SESSION
        && !Set.of("evidenceTimeline", "snapshotStatus", "snapshotCancel", "recordingStatus", "recordingCancel").contains(definition.id()))
      requireChannel(instance.getInstanceKey(), deviceId, channelId);
    if (("deviceConfigRead".equals(definition.id()) || "deviceConfigApply".equals(definition.id()))
        && !List.of("deviceInfo", "network", "time", "clock", "serial232", "alarmInput", "alarmIo", "alarmOutput", "alarmOutputStatus").contains(java.util.Objects.toString(parameters.get("resource"), ""))
        && channelId.isBlank()) throw new BaseException("此配置资源必须指定通道");
    if (("deviceConfigRead".equals(definition.id()) || "deviceConfigApply".equals(definition.id()))
        && "alarmInput".equals(parameters.get("resource"))) {
      Number selector=(Number)parameters.get("ptzChannelNo");
      if(selector==null||selector.longValue()>Integer.MAX_VALUE||channelMapper.selectCount(VideoPlatformChannel.builder()
          .instanceKey(instance.getInstanceKey()).deviceId(device.getId()).channelNo(Integer.toString(selector.intValue())).build())==0)
        throw new BaseException("告警输入联动视频通道不存在或无权访问");
    }
  }

  private void requestIsupSyncAfterCommit(VideoPlatformInstance instance) {
    if ("dahua_sdk".equals(instance.getPlatformType()) && dahuaRegistrationSync != null) {
      if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
          @Override public void afterCommit() { dahuaRegistrationSync.requestSync(); }
        });
      } else { dahuaRegistrationSync.requestSync(); }
      return;
    }
    if (!"hik_isup".equals(instance.getPlatformType())) return;
    if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
      org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
          new org.springframework.transaction.support.TransactionSynchronization() {
            @Override public void afterCommit() { hikIsupRegistrationSync.requestSync(); }
          });
    } else { hikIsupRegistrationSync.requestSync(); }
  }

  private void requireIsupModule(VideoPlatformInstance instance) {
    if ("hik_isup".equals(instance.getPlatformType()) && !adapterRegistry.isSupported("hik_isup"))
      throw new BaseException("当前后端未启用 ISUP 接入，请设置 hik.isup.protocol.enabled=true 后重启后端");
  }

  @Override
  public Map<String, Object> startOnvifScan(String instanceKey, Map<String, Object> params) {
    VideoPlatformInstance instance = requireOnvifInstance(instanceKey);
    Map<String, Object> safeParams = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
    String lockKey = buildOnvifScanLockKey(instanceKey);
    String rateKey = buildOnvifScanRateKey(instanceKey);
    String latestKey = buildOnvifScanLatestKey(instanceKey);

    String runningTaskId = stringRedisTemplate.opsForValue().get(lockKey);
    if (StrUtil.isNotBlank(runningTaskId)) {
      Map<String, Object> running = getOnvifScanTask(instanceKey, runningTaskId);
      running.put("accepted", false);
      running.put("running", true);
      return running;
    }

    String rateTaskId = stringRedisTemplate.opsForValue().get(rateKey);
    if (StrUtil.isNotBlank(rateTaskId)) {
      Map<String, Object> latest = readJsonMapValue(latestKey);
      if (latest != null && !latest.isEmpty()) {
        latest.put("accepted", false);
        latest.put("throttled", true);
        latest.put("message", "扫描操作过于频繁，请稍后刷新结果");
        return latest;
      }
    }

    String taskId = UUID.randomUUID().toString().replace("-", "");
    Boolean locked =
        stringRedisTemplate
            .opsForValue()
            .setIfAbsent(lockKey, taskId, Math.max(30L, onvifScanLockTtlSeconds), TimeUnit.SECONDS);
    if (!Boolean.TRUE.equals(locked)) {
      String lockedTaskId = stringRedisTemplate.opsForValue().get(lockKey);
      Map<String, Object> running = getOnvifScanTask(instanceKey, lockedTaskId);
      running.put("accepted", false);
      running.put("running", true);
      return running;
    }

    stringRedisTemplate
        .opsForValue()
        .set(rateKey, taskId, Math.max(1L, onvifScanMinIntervalSeconds), TimeUnit.SECONDS);

    long startedAt = System.currentTimeMillis();
    Map<String, Object> task = new LinkedHashMap<>();
    task.put("taskId", taskId);
    task.put("instanceKey", instanceKey);
    task.put("status", "running");
    task.put("running", true);
    task.put("accepted", true);
    task.put("startedAt", startedAt);
    task.put("updatedAt", startedAt);
    task.put("params", sanitizeOnvifScanParams(safeParams));
    task.put("devices", List.of());
    task.put("count", 0);
    task.put("message", "扫描中");
    cacheOnvifScanTask(instanceKey, taskId, task);

    try {
      getOnvifScanExecutor().execute(() -> runOnvifScanTask(instance, taskId, safeParams));
    } catch (RejectedExecutionException e) {
      releaseOnvifScanLock(instanceKey, taskId);
      task.put("status", "failed");
      task.put("running", false);
      task.put("accepted", false);
      task.put("finishedAt", System.currentTimeMillis());
      task.put("message", "扫描队列繁忙，请稍后重试");
      cacheOnvifScanTask(instanceKey, taskId, task);
      throw new BaseException("ONVIF扫描队列繁忙，请稍后重试");
    }
    return task;
  }

  @Override
  public Map<String, Object> getOnvifScanTask(String instanceKey, String taskId) {
    requireOnvifInstance(instanceKey);
    if (StrUtil.isBlank(taskId)) {
      return getLatestOnvifScan(instanceKey);
    }
    Map<String, Object> task = readJsonMapValue(buildOnvifScanTaskKey(instanceKey, taskId));
    if (task == null || task.isEmpty()) {
      Map<String, Object> empty = new LinkedHashMap<>();
      empty.put("taskId", taskId);
      empty.put("instanceKey", instanceKey);
      empty.put("status", "none");
      empty.put("running", false);
      empty.put("count", 0);
      empty.put("devices", List.of());
      empty.put("message", "暂无扫描结果");
      return empty;
    }
    fillOnvifTaskRuntime(instanceKey, task);
    return task;
  }

  @Override
  public Map<String, Object> getLatestOnvifScan(String instanceKey) {
    requireOnvifInstance(instanceKey);
    Map<String, Object> task = readJsonMapValue(buildOnvifScanLatestKey(instanceKey));
    if (task == null || task.isEmpty()) {
      Map<String, Object> empty = new LinkedHashMap<>();
      empty.put("instanceKey", instanceKey);
      empty.put("status", "none");
      empty.put("running", false);
      empty.put("count", 0);
      empty.put("devices", List.of());
      empty.put("message", "暂无扫描结果");
      return empty;
    }
    fillOnvifTaskRuntime(instanceKey, task);
    return task;
  }

  @Override
  public Map<String, Object> startSdkDiscovery(String instanceKey, Map<String, Object> params) {
    VideoPlatformInstance instance = requireDirectSdkInstance(instanceKey);
    Map<String, Object> safeParams = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
    String lockKey = buildSdkDiscoveryLockKey(instanceKey);
    String rateKey = buildSdkDiscoveryRateKey(instanceKey);
    String latestKey = buildSdkDiscoveryLatestKey(instanceKey);

    String runningTaskId = stringRedisTemplate.opsForValue().get(lockKey);
    if (StrUtil.isNotBlank(runningTaskId)) {
      Map<String, Object> running = getSdkDiscoveryTask(instanceKey, runningTaskId);
      running.put("accepted", false);
      running.put("running", true);
      return running;
    }

    String rateTaskId = stringRedisTemplate.opsForValue().get(rateKey);
    if (StrUtil.isNotBlank(rateTaskId)) {
      Map<String, Object> latest = readJsonMapValue(latestKey);
      if (latest != null && !latest.isEmpty()) {
        latest.put("accepted", false);
        latest.put("throttled", true);
        latest.put("message", "搜索操作过于频繁，请稍后刷新结果");
        return latest;
      }
    }

    String taskId = UUID.randomUUID().toString().replace("-", "");
    Boolean locked =
        stringRedisTemplate
            .opsForValue()
            .setIfAbsent(lockKey, taskId, Math.max(30L, sdkDiscoveryLockTtlSeconds), TimeUnit.SECONDS);
    if (!Boolean.TRUE.equals(locked)) {
      String lockedTaskId = stringRedisTemplate.opsForValue().get(lockKey);
      Map<String, Object> running = getSdkDiscoveryTask(instanceKey, lockedTaskId);
      running.put("accepted", false);
      running.put("running", true);
      return running;
    }

    stringRedisTemplate
        .opsForValue()
        .set(rateKey, taskId, Math.max(1L, sdkDiscoveryMinIntervalSeconds), TimeUnit.SECONDS);

    long startedAt = System.currentTimeMillis();
    Map<String, Object> task = new LinkedHashMap<>();
    task.put("taskId", taskId);
    task.put("instanceKey", instanceKey);
    task.put("platformType", instance.getPlatformType());
    task.put("status", "running");
    task.put("running", true);
    task.put("accepted", true);
    task.put("startedAt", startedAt);
    task.put("updatedAt", startedAt);
    task.put("params", sanitizeSdkDiscoveryParams(safeParams));
    task.put("devices", List.of());
    task.put("count", 0);
    task.put("message", "搜索中");
    cacheSdkDiscoveryTask(instanceKey, taskId, task);

    try {
      getSdkDiscoveryExecutor().execute(() -> runSdkDiscoveryTask(instance, taskId, safeParams));
    } catch (RejectedExecutionException e) {
      releaseSdkDiscoveryLock(instanceKey, taskId);
      task.put("status", "failed");
      task.put("running", false);
      task.put("accepted", false);
      task.put("finishedAt", System.currentTimeMillis());
      task.put("message", "搜索队列繁忙，请稍后重试");
      cacheSdkDiscoveryTask(instanceKey, taskId, task);
      throw new BaseException("直连SDK搜索队列繁忙，请稍后重试");
    }
    return task;
  }

  @Override
  public Map<String, Object> getSdkDiscoveryTask(String instanceKey, String taskId) {
    requireDirectSdkInstance(instanceKey);
    if (StrUtil.isBlank(taskId)) {
      return getLatestSdkDiscovery(instanceKey);
    }
    Map<String, Object> task = readJsonMapValue(buildSdkDiscoveryTaskKey(instanceKey, taskId));
    if (task == null || task.isEmpty()) {
      Map<String, Object> empty = new LinkedHashMap<>();
      empty.put("taskId", taskId);
      empty.put("instanceKey", instanceKey);
      empty.put("status", "none");
      empty.put("running", false);
      empty.put("count", 0);
      empty.put("devices", List.of());
      empty.put("message", "暂无搜索结果");
      return empty;
    }
    fillSdkDiscoveryTaskRuntime(instanceKey, task);
    return task;
  }

  @Override
  public Map<String, Object> getLatestSdkDiscovery(String instanceKey) {
    requireDirectSdkInstance(instanceKey);
    Map<String, Object> task = readJsonMapValue(buildSdkDiscoveryLatestKey(instanceKey));
    if (task == null || task.isEmpty()) {
      Map<String, Object> empty = new LinkedHashMap<>();
      empty.put("instanceKey", instanceKey);
      empty.put("status", "none");
      empty.put("running", false);
      empty.put("count", 0);
      empty.put("devices", List.of());
      empty.put("message", "暂无搜索结果");
      return empty;
    }
    fillSdkDiscoveryTaskRuntime(instanceKey, task);
    return task;
  }

  @Override
  public Map<String, Object> validateSdkDevice(String instanceKey, Map<String, Object> params) {
    VideoPlatformInstance instance = requireDirectSdkInstance(instanceKey);
    Map<String, Object> payload = normalizeSdkProbePayload(instance, params);
    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    Map<String, Object> result = adapter.executeOperation(instance, "probe", payload);
    return sanitizeSdkDiscoveryResult(normalizeSdkDiscoveryResponse(instance, result), true);
  }

  @Override
  @Transactional
  public Map<String, Object> importSdkDevices(String instanceKey, Map<String, Object> body) {
    VideoPlatformInstance instance = requireDirectSdkInstance(instanceKey);
    Map<String, Object> safeBody = body == null ? new LinkedHashMap<>() : new LinkedHashMap<>(body);
    List<Map<String, Object>> devices = extractMapList(safeBody.get("devices"));
    if (devices.isEmpty()) {
      throw new BaseException("请选择要添加的摄像头");
    }
    Map<String, Object> credential = firstCredential(safeBody);
    List<Map<String, Object>> syncDevices = new ArrayList<>();
    for (Map<String, Object> device : devices) {
      Map<String, Object> normalized = normalizeSdkCandidateForSync(instance, device, credential);
      if (!normalized.isEmpty()) {
        mergeSdkImportDefault(normalized, safeBody, "mediaServerId");
        mergeSdkImportDefault(normalized, safeBody, "zlmMediaServerId");
        mergeSdkImportDefault(normalized, safeBody, "previewMediaServerId");
        syncDevices.add(normalized);
      }
    }
    if (syncDevices.isEmpty()) {
      throw new BaseException("没有可导入的摄像头");
    }

    Map<String, Object> filters = new LinkedHashMap<>();
    filters.put("devices", syncDevices);
    filters.put("__resetBeforeSync", false);
    DeviceSyncResult syncResult = forceSyncDevices(instanceKey, filters);

    Map<String, Integer> importResult = Map.of("success", 0, "failed", 0, "exists", 0);
    boolean importToIot = !"false".equalsIgnoreCase(String.valueOf(safeBody.getOrDefault("importToIot", "true")));
    if (importToIot) {
      importResult =
          deviceImportService.importDevices(
              instanceKey, syncDevices, stringValue(safeBody.get("productKey")));
    }

    Map<String, Object> result = buildSdkImportResult(instanceKey, syncResult);
    result.put("devices", maskSdkDeviceSecrets(syncDevices));
    result.put("importToIot", importToIot);
    result.put("iotImport", importResult);
    result.put("success", importResult.getOrDefault("success", 0));
    result.put("failed", importResult.getOrDefault("failed", 0));
    result.put("exists", importResult.getOrDefault("exists", 0));
    applySdkSubscriptionAfterCommit(instanceKey);
    return result;
  }

  private void applySdkSubscriptionAfterCommit(String instanceKey) {
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        try {
          // 在锁内读取最新范围，避免导入/编辑使用旧配置重新开启已停用的订阅。
          applySubscription(instanceKey);
        } catch (Exception e) {
          log.warn("[视频SDK告警] 设备已保存，订阅待重试 instanceKey={}, reason={}", instanceKey, e.getMessage());
        }
      }
    });
  }

  @Override
  public List<Map<String, Object>> listChannels(
      String instanceKey, String deviceId, Map<String, Object> filters) {
    requireAccessibleInstance(instanceKey);
    // 构建查询条件
    Example ex = new Example(VideoPlatformChannel.class);
    Example.Criteria criteria = ex.createCriteria();
    criteria.andEqualTo("instanceKey", instanceKey);

    if (deviceId != null && !deviceId.isEmpty()) {
      // 如果指定了设备ID,需要先查询设备的主表ID
      Example deviceEx = new Example(VideoPlatformDevice.class);
      deviceEx
          .createCriteria()
          .andEqualTo("instanceKey", instanceKey)
          .andEqualTo("deviceId", deviceId);
      List<VideoPlatformDevice> deviceList = deviceMapper.selectByExample(deviceEx);
      if (!deviceList.isEmpty()) {
        criteria.andEqualTo("deviceId", deviceList.get(0).getId());
      } else {
        return java.util.Collections.emptyList();
      }
    }

    // 应用筛选条件
    if (filters != null) {
      String status = (String) filters.get("status");
      if (status != null && !status.isEmpty()) {
        criteria.andEqualTo("channelStatus", status);
      }

      Object ptzType = filters.get("ptzType");
      if (ptzType != null) {
        criteria.andEqualTo("ptzType", parseIntOrNull(ptzType));
      }
    }

    ex.orderBy("createTime").desc();
    List<VideoPlatformChannel> channels = channelMapper.selectByExample(ex);

    // 转换为Map列表
    List<Map<String, Object>> result = new java.util.ArrayList<>();
    for (VideoPlatformChannel ch : channels) {
      Map<String, Object> item = new java.util.HashMap<>();
      item.put("id", ch.getId());
      item.put("deviceId", ch.getDeviceId());
      item.put("channelId", ch.getChannelId());
      item.put("channelName", ch.getChannelName());
      item.put("channelStatus", ch.getChannelStatus());
      item.put("channelType", ch.getChannelType());
      item.put("ptzType", ch.getPtzType());
      item.put("manufacturer", ch.getManufacturer());
      item.put("model", ch.getModel());
      item.put("address", ch.getAddress());
      item.put("ipAddress", ch.getIpAddress());
      item.put("port", ch.getPort());

      // 平台特有字段
      item.put("cameraIndexCode", ch.getCameraIndexCode()); // 海康
      item.put("channelNo", ch.getChannelNo()); // 海康
      item.put("channelSeq", ch.getChannelSeq());
      item.put("ptz", ch.getPtz()); // 海康
      item.put("streamId", ch.getStreamId()); // WVP
      item.put("channelCode", ch.getChannelCode()); // 大华
      Map<String, Object> fusionConfig = extractFusionConfig(ch, instanceKey, deviceId);
      Map<String, String> fusionUrls = extractPublishUrls(fusionConfig);
      boolean fusionReady = isFusionReady(fusionConfig, fusionUrls);
      item.put("fusionConfig", fusionConfig);
      item.put("fusionReady", fusionReady);
      item.put(
          "fusionPreferredUrl",
          fusionReady
              ? pickPreferredUrl(
                  fusionUrls,
                  String.valueOf(fusionConfig.getOrDefault("preferProtocol", "ws_flv")))
              : "");

      result.add(item);
    }

    // 关键词筛选
    if (filters != null) {
      String keyword = (String) filters.get("keyword");
      if (keyword != null && !keyword.isEmpty()) {
        result =
            result.stream()
                .filter(
                    item -> {
                      String name = String.valueOf(item.getOrDefault("channelName", ""));
                      String chId = String.valueOf(item.getOrDefault("channelId", ""));
                      return name.contains(keyword) || chId.contains(keyword);
                    })
                .collect(java.util.stream.Collectors.toList());
      }
    }

    return result;
  }

  @Override
  public Map<String, Object> syncDeviceChannels(String instanceKey, String deviceId) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    if (List.of("hik_sdk", "dahua_sdk", "tiandy_sdk", "uniview_sdk", "hik_isup")
        .contains(StrUtil.trimToEmpty(instance.getPlatformType()).toLowerCase(java.util.Locale.ROOT))) {
      return syncSdkDeviceChannels(instance, deviceId);
    }
    if (!"onvif".equalsIgnoreCase(instance.getPlatformType())) {
      throw new BaseException("当前接口支持ONVIF、海康直连SDK、大华SDK、天地伟业SDK、宇视SDK和海康ISUP设备");
    }
    VideoPlatformDevice device = requireDeviceEntity(instanceKey, deviceId);
    int beforeCount = countChannelsByDevicePk(device.getId());

    Map<String, Object> selectedDevice = buildStoredOnvifDeviceSyncPayload(instance, device);
    Map<String, Object> filters = new LinkedHashMap<>();
    filters.put("devices", List.of(selectedDevice));
    filters.put("skipWsDiscovery", true);
    filters.put("useCache", false);
    filters.put("requireAuthVerified", true);
    filters.put("authVerifiedOnly", true);

    DeviceSyncResult syncResult = forceSyncDevices(instanceKey, filters);
    int afterCount = countChannelsByDevicePk(device.getId());

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("instanceKey", instanceKey);
    result.put("deviceId", deviceId);
    result.put("beforeChannelCount", beforeCount);
    result.put("afterChannelCount", afterCount);
    result.put("totalDevices", syncResult.getTotalDevices());
    result.put("insertedDevices", syncResult.getInsertedDevices());
    result.put("updatedDevices", syncResult.getUpdatedDevices());
    result.put("totalChannels", syncResult.getTotalChannels());
    result.put("errors", syncResult.getErrors());
    result.put("channels", listChannels(instanceKey, deviceId, Collections.emptyMap()));
    return result;
  }

  private Map<String, Object> syncSdkDeviceChannels(
      VideoPlatformInstance instance, String deviceId) {
    VideoPlatformDevice device = requireDeviceEntity(instance.getInstanceKey(), deviceId);
    Map<String, Object> storedDevice = buildStoredSdkDeviceSyncPayload(device);
    boolean isup = "hik_isup".equalsIgnoreCase(instance.getPlatformType());
    boolean dahuaRegistration = "dahua_sdk".equalsIgnoreCase(instance.getPlatformType())
        && ("active_registration".equalsIgnoreCase(stringValue(storedDevice.get("loginMode")))
            || "active_registration".equalsIgnoreCase(stringValue(storedDevice.get("accessMode"))));
    boolean registration = isup || dahuaRegistration;
    if (dahuaRegistration) {
      storedDevice.put("registrationId", firstNonBlankString(storedDevice.get("registrationId"),
          storedDevice.get("registerCode"), storedDevice.get("gatewayDeviceId"), deviceId));
    }
    if ("dahua_sdk".equalsIgnoreCase(instance.getPlatformType()) && !dahuaRegistration) {
      storedDevice.put("loginMode", "direct");
      storedDevice.put("accessMode", "direct");
    }
    if (!registration && StrUtil.isBlank(device.getDeviceIp())) {
      throw new BaseException("设备未配置直连地址，请先完善SDK登录配置");
    }
    int beforeCount = countChannelsByDevicePk(device.getId());

    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    List<Map<String, Object>> discoveredDevices =
        adapter.listDevices(instance, Map.of("device", storedDevice, "refreshChannels", true));
    if (discoveredDevices == null || discoveredDevices.isEmpty()) {
      throw new BaseException("未能从设备读取到设备信息，请检查设备在线状态和接入配置");
    }

    Map<String, Object> discovered = discoveredDevices.stream()
        .filter(item -> item != null && deviceId.equals(firstNonBlankString(
            item.get("deviceId"), item.get("extDeviceId"), item.get("deviceSn"), item.get("serialNumber"))))
        .findFirst()
        .orElse(null);
    if (discovered == null) {
      throw new BaseException("SDK返回的设备与当前设备不匹配，已停止同步");
    }
    String discoveredHost = firstNonBlankString(
        discovered.get("deviceIp"), discovered.get("ip"), discovered.get("host"));
    String storedSerial = firstNonBlankString(storedDevice.get("serialNumber"), storedDevice.get("deviceSn"));
    String discoveredSerial = firstNonBlankString(discovered.get("serialNumber"), discovered.get("deviceSn"));
    if ((!registration && !device.getDeviceIp().equalsIgnoreCase(discoveredHost))
        || (!StrUtil.isBlank(storedSerial) && !storedSerial.equals(discoveredSerial))) {
      throw new BaseException("SDK返回的设备地址或序列号与当前设备不匹配，已停止同步");
    }
    if (registration && !isOnlineStatus(firstNonBlankString(
        discovered.get("deviceStatus"), discovered.get("status"), discovered.get("online")))) {
      throw new BaseException("注册设备当前不在线，已保留现有通道；请待设备重新注册后重试");
    }
    if (dahuaRegistration) {
      String expectedRegistrationId = firstNonBlankString(storedDevice.get("registrationId"),
          storedDevice.get("registerCode"), storedDevice.get("gatewayDeviceId"), deviceId);
      String actualRegistrationId = firstNonBlankString(discovered.get("registrationId"),
          discovered.get("registerCode"), discovered.get("gatewayDeviceId"));
      if (!Boolean.TRUE.equals(discovered.get("authenticated"))
          || !expectedRegistrationId.equals(actualRegistrationId)) {
        throw new BaseException("主动注册身份与当前设备不匹配或认证未完成，已停止同步");
      }
    }
    if (!Boolean.TRUE.equals(discovered.get("channelCatalogAuthoritative"))
        || !(discovered.get("channelList") instanceof List<?>)) {
      if ("dahua_sdk".equalsIgnoreCase(instance.getPlatformType())) {
        int updated = dahuaIpcChannelStatusRefresh.refresh(device, discovered);
        if (updated > 0) {
          Map<String, Object> result = new LinkedHashMap<>();
          result.put("instanceKey", instance.getInstanceKey());
          result.put("deviceId", deviceId);
          result.put("beforeChannelCount", beforeCount);
          result.put("afterChannelCount", countChannelsByDevicePk(device.getId()));
          result.put("totalDevices", 1);
          result.put("insertedDevices", 0);
          result.put("updatedDevices", 1);
          result.put("totalChannels", updated);
          result.put("statusOnly", true);
          result.put("message", "已刷新已有通道状态；设备未确认完整目录，已保留全部通道和配置");
          result.put("errors", List.of());
          result.put("channels", listChannels(instance.getInstanceKey(), deviceId, Collections.emptyMap()));
          return result;
        }
      }
      throw new BaseException("设备未能确认完整通道目录，已保留现有通道；请检查NVR的通道接入配置后重试");
    }

    Map<String, Object> syncDevice = new LinkedHashMap<>(storedDevice);
    syncDevice.putAll(discovered);
    syncDevice.put("deviceId", device.getDeviceId());
    syncDevice.put("extDeviceId", device.getDeviceId());
    for (String key : List.of("username", "password", "authType", "credentialMode", "sdkPort", "rtspPort",
        "timeoutMs", "mediaServerId", "previewMediaServerId", "streamMode", "loginMode", "accessMode",
        "registrationId", "registerCode", "gatewayDeviceId")) {
      Object storedValue = storedDevice.get(key);
      Object discoveredValue = syncDevice.get(key);
      if (storedValue != null && (discoveredValue == null || String.valueOf(discoveredValue).isBlank()
          || String.valueOf(discoveredValue).matches("\\*+"))) {
        syncDevice.put(key, storedValue);
      }
    }

    DeviceSyncResult syncResult = forceSyncDevices(
        instance.getInstanceKey(), Map.of("devices", List.of(syncDevice)));
    if (syncResult.getErrors() != null && !syncResult.getErrors().isEmpty()) {
      throw new BaseException("设备通道同步失败：" + String.join("；", syncResult.getErrors()));
    }
    if (syncResult.getTotalDevices() != 1) {
      throw new BaseException("目标设备通道未完成同步，请重试");
    }

    int afterCount = countChannelsByDevicePk(device.getId());
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("instanceKey", instance.getInstanceKey());
    result.put("deviceId", deviceId);
    result.put("beforeChannelCount", beforeCount);
    result.put("afterChannelCount", afterCount);
    result.put("totalDevices", syncResult.getTotalDevices());
    result.put("insertedDevices", syncResult.getInsertedDevices());
    result.put("updatedDevices", syncResult.getUpdatedDevices());
    result.put("totalChannels", syncResult.getTotalChannels());
    result.put("errors", syncResult.getErrors());
    result.put("channels", listChannels(instance.getInstanceKey(), deviceId, Collections.emptyMap()));
    return result;
  }

  private Map<String, Object> buildStoredSdkDeviceSyncPayload(VideoPlatformDevice device) {
    Map<String, Object> payload = new LinkedHashMap<>();
    putIfNotBlank(payload, "deviceId", device.getDeviceId());
    putIfNotBlank(payload, "deviceName", device.getDeviceName());
    putIfNotBlank(payload, "name", device.getDeviceName());
    putIfNotBlank(payload, "deviceStatus", device.getDeviceStatus());
    putIfNotBlank(payload, "status", device.getDeviceStatus());
    putIfNotBlank(payload, "host", device.getDeviceIp());
    putIfNotBlank(payload, "ip", device.getDeviceIp());
    putIfNotBlank(payload, "deviceIp", device.getDeviceIp());
    putIfNotBlank(payload, "manufacturer", device.getManufacturer());
    putIfNotBlank(payload, "deviceModel", device.getDeviceModel());
    if (device.getDevicePort() != null) {
      payload.put("port", device.getDevicePort());
      payload.put("devicePort", device.getDevicePort());
      payload.put("sdkPort", device.getDevicePort());
    }

    VideoPlatformDeviceExt extension = findDeviceExt(device.getId());
    Map<String, Object> stored = extension == null
        ? Collections.emptyMap()
        : readObjectMap(extension.getExtField3());
    if (extension != null) {
      putIfNotBlank(payload, "deviceSn", extension.getDeviceSn());
    }
    for (String key : List.of("username", "password", "authType", "credentialMode", "deviceType", "deviceKind",
        "deviceSn", "serialNumber", "sdkPort", "rtspPort", "timeoutMs", "requestTimeoutMs", "mediaServerId",
        "previewMediaServerId", "streamMode", "loginMode", "accessMode", "registrationId", "registerCode",
        "gatewayDeviceId", "vendor")) {
      putIfNotBlank(payload, key, stored.get(key));
    }
    if ("instance".equalsIgnoreCase(firstNonBlankString(payload.get("credentialMode")))) {
      payload.remove("username");
      payload.remove("password");
    }
    return payload;
  }

  @Override
  public Map<String, Object> getChannelFusion(
      String instanceKey, String deviceId, String channelId) {
    requireAccessibleInstance(instanceKey);
    VideoPlatformChannel channel = requireChannel(instanceKey, deviceId, channelId);
    Map<String, Object> fusionConfig = extractFusionConfig(channel, instanceKey, deviceId);
    fusionConfig.put("overlayPreview", buildFusionOverlayText(channel, fusionConfig));
    fusionConfig.put("bindingCount", countFusionBindingItems(fusionConfig));
    Map<String, String> fusionUrls = extractPublishUrls(fusionConfig);
    boolean fusionReady = isFusionReady(fusionConfig, fusionUrls);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("instanceKey", instanceKey);
    result.put("deviceId", deviceId);
    result.put("channelId", channelId);
    result.put("fusionConfig", fusionConfig);
    result.put("fusionUrls", fusionUrls);
    result.put("fusionReady", fusionReady);
    result.put("overlayPreview", fusionConfig.get("overlayPreview"));
    result.put("placeholderTokens", buildFusionPlaceholderTokens(channel, fusionConfig));
    result.put(
        "preferredUrl",
        fusionReady
            ? pickPreferredUrl(
                fusionUrls, String.valueOf(fusionConfig.getOrDefault("preferProtocol", "ws_flv")))
            : "");
    return result;
  }

  @Override
  @Transactional
  public Map<String, Object> updateChannelFusion(
      String instanceKey, String deviceId, String channelId, Map<String, Object> updates) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    VideoPlatformChannel channel = requireChannel(instanceKey, deviceId, channelId);
    Map<String, Object> previousFusionConfig = extractFusionConfig(channel, instanceKey, deviceId);
    Map<String, Object> rawFusion = updates;
    if (updates != null && updates.get("fusionConfig") instanceof Map) {
      rawFusion = objectMapper.convertValue(updates.get("fusionConfig"), LinkedHashMap.class);
    }
    Map<String, Object> fusionConfig = normalizeFusionConfig(instanceKey, deviceId, channelId, rawFusion);
    // 配置层面保持互斥：disabled/frontend_osd 都必须清掉后端融合任务的发布地址和状态。
    if (!isFusionEnabled(fusionConfig)) {
      clearFusionRuntimeState(fusionConfig, "融合未启用，默认回退原始流");
      stopFusionTaskWhenDisabled(instance, deviceId, channelId, previousFusionConfig, fusionConfig);
    } else if (!isBackendFusionMode(fusionConfig)) {
      clearFusionRuntimeState(fusionConfig, "未启用后端融合流，默认回退原始流");
      stopFusionTaskWhenDisabled(instance, deviceId, channelId, previousFusionConfig, fusionConfig);
    }
    fusionConfig.put("updatedBy", SecurityUtils.getUnionId());
    fusionConfig.put("updatedAt", System.currentTimeMillis());
    saveFusionConfig(channel, fusionConfig);
    updateFusionRegistry(instanceKey, deviceId, channelId, fusionConfig);
    return getChannelFusion(instanceKey, deviceId, channelId);
  }

  @Override
  public Map<String, Object> getChannelFusionContext(
      String instanceKey, String deviceId, String channelId, boolean includeOriginUrls) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    VideoPlatformChannel channel = requireChannel(instanceKey, deviceId, channelId);
    Map<String, Object> fusionConfig = extractFusionConfig(channel, instanceKey, deviceId);
    fusionConfig.put("overlayPreview", buildFusionOverlayText(channel, fusionConfig));
    fusionConfig.put("bindingCount", countFusionBindingItems(fusionConfig));
    Map<String, String> fusionUrls = extractPublishUrls(fusionConfig);
    boolean fusionReady = isFusionReady(fusionConfig, fusionUrls);

    Map<String, String> originUrls = new LinkedHashMap<>();
    // includeOriginUrls=true 会真实调用适配器取流；前端加载配置上下文时默认关闭以避免误点播。
    if (includeOriginUrls) {
      VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
      originUrls = adapter.getStreamUrl(instance, deviceId, channelId, "main", new LinkedHashMap<>());
      if (originUrls == null) {
        originUrls = new LinkedHashMap<>();
      } else {
        originUrls = new LinkedHashMap<>(originUrls);
      }
      originUrls.put(
          "preferredUrl",
          pickPreferredUrl(
              originUrls, String.valueOf(fusionConfig.getOrDefault("preferProtocol", "ws_flv"))));
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("instanceKey", instanceKey);
    result.put("deviceId", deviceId);
    result.put("channelId", channelId);
    result.put("channel", buildChannelSummary(channel));
    result.put("fusionConfig", fusionConfig);
    result.put("originUrls", originUrls);
    result.put("originIncluded", includeOriginUrls);
    result.put("fusionUrls", fusionUrls);
    result.put("fusionReady", fusionReady);
    result.put("overlayPreview", fusionConfig.get("overlayPreview"));
    result.put("placeholderTokens", buildFusionPlaceholderTokens(channel, fusionConfig));
    result.put(
        "preferredUrl",
        fusionReady
            ? pickPreferredUrl(
                fusionUrls, String.valueOf(fusionConfig.getOrDefault("preferProtocol", "ws_flv")))
            : stringValue(originUrls.get("preferredUrl")));
    result.put("shadowSummary", buildFusionShadowSummary(fusionConfig));
    return result;
  }

  @Override
  @Transactional
  public Map<String, Object> getChannelFusionTask(
      String instanceKey, String deviceId, String channelId) {
    VideoPlatformInstance instance = resolveFusionInstance(instanceKey, true);
    VideoPlatformChannel channel = requireChannel(instanceKey, deviceId, channelId);
    Map<String, Object> fusionConfig = extractFusionConfig(channel, instanceKey, deviceId);
    if (!isBackendFusionMode(fusionConfig)) {
      clearFusionRuntimeState(fusionConfig, "未启用后端融合流，跳过后端任务状态同步");
      saveFusionConfig(channel, fusionConfig);
      updateFusionRegistry(instanceKey, deviceId, channelId, fusionConfig);
      Map<String, Object> result = getChannelFusion(instanceKey, deviceId, channelId);
      result.put("task", buildFusionTaskSnapshot(fusionConfig));
      return result;
    }
    Map<String, Object> taskData =
        requireFusionAdapter(instance).getFusionTaskStatus(instance, deviceId, channelId);
    mergeFusionTaskRuntime(fusionConfig, taskData);
    saveFusionConfig(channel, fusionConfig);
    updateFusionRegistry(instanceKey, deviceId, channelId, fusionConfig);
    Map<String, Object> result = getChannelFusion(instanceKey, deviceId, channelId);
    result.put("task", taskData);
    return result;
  }

  @Override
  @Transactional
  public Map<String, Object> startChannelFusionTask(
      String instanceKey, String deviceId, String channelId) {
    VideoPlatformInstance instance = resolveFusionInstance(instanceKey, true);
    VideoPlatformChannel channel = requireChannel(instanceKey, deviceId, channelId);
    Map<String, Object> fusionConfig = extractFusionConfig(channel, instanceKey, deviceId);
    fusionConfig =
        startChannelFusionTaskInternal(
            instance, channel, instanceKey, deviceId, channelId, fusionConfig);
    Map<String, Object> result = getChannelFusion(instanceKey, deviceId, channelId);
    result.put("task", buildFusionTaskSnapshot(fusionConfig));
    return result;
  }

  @Override
  @Transactional
  public Map<String, Object> refreshChannelFusionTask(
      String instanceKey, String deviceId, String channelId) {
    VideoPlatformInstance instance = resolveFusionInstance(instanceKey, true);
    VideoPlatformChannel channel = requireChannel(instanceKey, deviceId, channelId);
    Map<String, Object> fusionConfig = extractFusionConfig(channel, instanceKey, deviceId);
    if (!isBackendFusionMode(fusionConfig)) {
      throw new BaseException("当前通道未启用后端融合流，无需同步后端热数据");
    }
    Map<String, Object> taskData =
        requireFusionAdapter(instance)
            .refreshFusionTask(
                instance,
                deviceId,
                channelId,
                buildFusionTaskOptions(instance, channel, instanceKey, deviceId, fusionConfig));
    mergeFusionTaskRuntime(fusionConfig, taskData);
    saveFusionConfig(channel, fusionConfig);
    updateFusionRegistry(instanceKey, deviceId, channelId, fusionConfig);
    Map<String, Object> result = getChannelFusion(instanceKey, deviceId, channelId);
    result.put("task", taskData);
    return result;
  }

  @Override
  @Transactional
  public Map<String, Object> stopChannelFusionTask(
      String instanceKey, String deviceId, String channelId) {
    VideoPlatformInstance instance = resolveFusionInstance(instanceKey, true);
    VideoPlatformChannel channel = requireChannel(instanceKey, deviceId, channelId);
    Map<String, Object> fusionConfig = extractFusionConfig(channel, instanceKey, deviceId);
    Map<String, Object> taskData =
        requireFusionAdapter(instance).stopFusionTask(instance, deviceId, channelId);
    mergeFusionTaskRuntime(fusionConfig, taskData);
    fusionConfig.put("publishUrls", new LinkedHashMap<>());
    saveFusionConfig(channel, fusionConfig);
    updateFusionRegistry(instanceKey, deviceId, channelId, fusionConfig);
    Map<String, Object> result = getChannelFusion(instanceKey, deviceId, channelId);
    result.put("task", taskData);
    return result;
  }

  @Override
  public Map<String, Object> getChannelFusionOverlay(
      String instanceKey, String deviceId, String channelId) {
    return buildFrontendOsdOverlaySnapshot(instanceKey, deviceId, channelId, true, false);
  }

  @Override
  public Map<String, Object> createFrontendOsdShare(
      String instanceKey, String deviceId, String channelId, Map<String, Object> options) {
    requireAccessibleInstance(instanceKey);
    VideoPlatformChannel channel = requireChannel(instanceKey, deviceId, channelId);
    Map<String, Object> fusionConfig = extractFusionConfig(channel, instanceKey, deviceId);
    // 分享记录只保存必要索引和播放偏好，播放地址与 OSD 快照在访问分享时重新生成。
    long ttlSeconds =
        normalizeShareTtlSeconds(
            parseLongOrDefault(
                firstNonNull(
                    options == null ? null : options.get("expiresInSeconds"),
                    options == null ? null : options.get("ttlSeconds"),
                    options == null ? null : options.get("expiresIn")),
                videoShareDefaultTtlSeconds));
    long now = System.currentTimeMillis();
    long expiresAt = now + ttlSeconds * 1000L;
    String token = UUID.randomUUID().toString().replace("-", "");

    Map<String, Object> record = new LinkedHashMap<>();
    record.put("token", token);
    record.put("instanceKey", instanceKey);
    record.put("deviceId", deviceId);
    record.put("channelId", channelId);
    record.put("streamType", normalizeShareStreamType(stringValue(options == null ? null : options.get("streamType"))));
    record.put("playMode", normalizeSharePlayMode(stringValue(options == null ? null : options.get("playMode"))));
    record.put(
        "preferProtocol",
        StrUtil.blankToDefault(
            stringValue(options == null ? null : options.get("preferProtocol")),
            String.valueOf(fusionConfig.getOrDefault("preferProtocol", "ws_flv"))));
    record.put("osdEnabled", !Boolean.FALSE.equals(parseBooleanObject(options == null ? null : options.get("osdEnabled"))));
    record.put("audioEnabled", !Boolean.FALSE.equals(parseBooleanObject(options == null ? null : options.get("audioEnabled"))));
    record.put("createdBy", SecurityUtils.getUnionId());
    record.put("createdAt", now);
    record.put("expiresAt", expiresAt);
    record.put("revoked", false);

    writeShareRecord(buildShareRedisKey(token), record, ttlSeconds);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("token", token);
    result.put("path", "/video-share/" + token);
    result.put("expiresAt", expiresAt);
    result.put("ttlSeconds", ttlSeconds);
    result.put("playMode", record.get("playMode"));
    result.put("osdEnabled", record.get("osdEnabled"));
    result.put("channel", buildChannelSummary(channel));
    return result;
  }

  @Override
  public Map<String, Object> getFrontendOsdShareContext(String token) {
    Map<String, Object> record = requireShareRecord(token);
    String instanceKey = stringValue(record.get("instanceKey"));
    String deviceId = stringValue(record.get("deviceId"));
    String channelId = stringValue(record.get("channelId"));
    VideoPlatformInstance instance = getByInstanceKey(instanceKey);
    if (instance == null) {
      throw new BaseException("分享对应的平台实例不存在");
    }
    VideoPlatformChannel channel = requireChannel(instanceKey, deviceId, channelId);
    Map<String, Object> fusionConfig = extractFusionConfig(channel, instanceKey, deviceId);
    Map<String, String> streamUrls = buildShareStreamUrls(instance, channel, record, fusionConfig);
    // 分享页首屏需要一份快照；后续更新走 WebSocket/SSE/轮询，避免每次刷新都重新取流。
    Map<String, Object> overlay = buildFrontendOsdOverlaySnapshot(instanceKey, deviceId, channelId, false, true);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("token", token);
    result.put("instanceKey", instanceKey);
    result.put("deviceId", deviceId);
    result.put("channelId", channelId);
    result.put("channel", buildChannelSummary(channel));
    result.put("streamUrls", streamUrls);
    result.put("preferredUrl", streamUrls.get("preferredUrl"));
    result.put("streamSource", streamUrls.get("streamSource"));
    result.put("playMode", record.get("playMode"));
    result.put("streamType", record.get("streamType"));
    result.put("audioEnabled", parseBoolean(record.get("audioEnabled")));
    result.put("osdEnabled", parseBoolean(record.get("osdEnabled")));
    result.put("overlay", overlay);
    result.put("fusionConfig", buildFrontendOsdConfig(fusionConfig));
    result.put("overlayEventIntervalMs", resolveShareOverlayEventIntervalMs());
    result.put("expiresAt", record.get("expiresAt"));
    result.put("serverTime", System.currentTimeMillis());
    return result;
  }

  @Override
  public Map<String, Object> getFrontendOsdShareOverlay(String token) {
    Map<String, Object> record = requireShareRecord(token);
    return buildFrontendOsdOverlaySnapshot(
        stringValue(record.get("instanceKey")),
        stringValue(record.get("deviceId")),
        stringValue(record.get("channelId")),
        false,
        false);
  }

  @Override
  public void revokeFrontendOsdShare(
      String instanceKey, String deviceId, String channelId, String token) {
    if (StrUtil.isBlank(token)) {
      return;
    }
    Map<String, Object> record = requireShareRecord(token);
    if (!StrUtil.equals(instanceKey, stringValue(record.get("instanceKey")))
        || !StrUtil.equals(deviceId, stringValue(record.get("deviceId")))
        || !StrUtil.equals(channelId, stringValue(record.get("channelId")))) {
      throw new BaseException("分享链接与当前通道不匹配");
    }
    stringRedisTemplate.delete(buildShareRedisKey(token));
  }

  @Scheduled(fixedDelayString = "${video.fusion.refresh-interval-ms:3000}")
  public void refreshRunningFusionTasks() {
    if (!fusionSchedulerEnabled) {
      return;
    }
    bootstrapFusionRegistryIfNeeded();
    if (fusionTaskRegistry.isEmpty()) {
      return;
    }
    for (FusionBinding binding : new ArrayList<>(fusionTaskRegistry.values())) {
      try {
        if (fusionOverlayStreamEnabled) {
          // Redis Stream 已由影子事件实时驱动；周期投递默认关闭，只保留兼容兜底开关。
          if (fusionOverlayStreamPeriodicPublishEnabled) {
            publishFusionOverlayStreamInternal(
                binding.instanceKey, binding.deviceId, binding.channelId, "schedule", false);
          }
          // 定时器主要职责是巡检后端融合任务状态，避免任务退出后本地仍认为可播放。
          syncFusionTaskStatusIfDue(binding);
        } else {
          refreshChannelFusionTaskInternal(
              binding.instanceKey, binding.deviceId, binding.channelId, false, true);
        }
      } catch (Exception e) {
        log.warn(
            "[视频融合] 定时刷新失败 instanceKey={}, deviceId={}, channelId={}, msg={}",
            binding.instanceKey,
            binding.deviceId,
            binding.channelId,
            e.getMessage());
        try {
          syncFusionTaskStatusInternal(
              binding.instanceKey, binding.deviceId, binding.channelId, false, true);
        } catch (Exception ignore) {
        }
      }
    }
  }

  @EventListener
  public void onDeviceShadowUpdated(DeviceShadowUpdatedEvent event) {
    if (!videoFusionShadowEventEnabled || event == null || StrUtil.isBlank(event.getIotId())) {
      return;
    }
    if (fusionOverlayStreamAsyncEnabled) {
      try {
        getFusionOverlayExecutor().execute(() -> handleDeviceShadowUpdated(event));
        return;
      } catch (RejectedExecutionException e) {
        log.warn("[视频融合] overlay异步队列已关闭，退回同步投递 iotId={}", event.getIotId());
      }
    }
    handleDeviceShadowUpdated(event);
  }

  private void handleDeviceShadowUpdated(DeviceShadowUpdatedEvent event) {
    bootstrapFusionRegistryIfNeeded();
    // 一次设备影子事件可能同时影响多个通道；前端 OSD 更新快照，后端融合写 Redis Stream。
    List<FusionBinding> frontendBindings = findFrontendOsdBindingsForEvent(event);
    List<FusionBinding> backendBindings =
        fusionOverlayStreamEnabled ? findBackendFusionBindingsForEvent(event) : Collections.emptyList();
    if (frontendBindings.isEmpty() && backendBindings.isEmpty()) {
      return;
    }
    for (FusionBinding binding : frontendBindings) {
      try {
        VideoPlatformChannel channel =
            requireChannel(binding.instanceKey, binding.deviceId, binding.channelId);
        Map<String, Object> fusionConfig =
            extractFusionConfig(channel, binding.instanceKey, binding.deviceId);
        if (!isFusionShadowEventRelevant(fusionConfig, event)) {
          continue;
        }
        cacheFrontendOsdOverlaySnapshotForBinding(
            binding.instanceKey,
            binding.deviceId,
            binding.channelId,
            channel,
            fusionConfig,
            "shadow");
      } catch (Exception e) {
        log.warn(
            "[视频融合] 前端OSD影子事件缓存失败 iotId={}, instanceKey={}, deviceId={}, channelId={}, msg={}",
            event.getIotId(),
            binding.instanceKey,
            binding.deviceId,
            binding.channelId,
            e.getMessage());
      }
    }
    for (FusionBinding binding : backendBindings) {
      try {
        VideoPlatformChannel channel =
            requireChannel(binding.instanceKey, binding.deviceId, binding.channelId);
        Map<String, Object> fusionConfig =
            extractFusionConfig(channel, binding.instanceKey, binding.deviceId);
        if (!isFusionShadowEventRelevant(fusionConfig, event)) {
          continue;
        }
        publishFusionOverlayStreamInternal(
            binding.instanceKey,
            binding.deviceId,
            binding.channelId,
            "shadow",
            false,
            channel,
            fusionConfig,
            event);
      } catch (Exception e) {
        log.warn(
            "[视频融合] 后端融合Redis Stream投递失败 iotId={}, instanceKey={}, deviceId={}, channelId={}, msg={}",
            event.getIotId(),
            binding.instanceKey,
            binding.deviceId,
            binding.channelId,
            e.getMessage());
      }
    }
  }

  private ThreadPoolExecutor getFusionOverlayExecutor() {
    ThreadPoolExecutor executor = fusionOverlayExecutor;
    if (executor != null) {
      return executor;
    }
    synchronized (this) {
      executor = fusionOverlayExecutor;
      if (executor == null) {
        int threads = Math.max(1, fusionOverlayStreamAsyncThreads);
        int queueSize = Math.max(100, fusionOverlayStreamAsyncQueueSize);
        executor =
            new ThreadPoolExecutor(
                threads,
                threads,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueSize),
                runnable -> {
                  Thread thread =
                      new Thread(
                          runnable,
                          "fusion-overlay-publisher-" + fusionOverlayThreadIndex.getAndIncrement());
                  thread.setDaemon(true);
                  return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy());
        fusionOverlayExecutor = executor;
      }
      return executor;
    }
  }

  private ThreadPoolExecutor getOnvifScanExecutor() {
    ThreadPoolExecutor executor = onvifScanExecutor;
    if (executor != null) {
      return executor;
    }
    synchronized (this) {
      executor = onvifScanExecutor;
      if (executor == null) {
        int threads = Math.max(1, onvifScanAsyncThreads);
        int queueSize = Math.max(1, onvifScanAsyncQueueSize);
        executor =
            new ThreadPoolExecutor(
                threads,
                threads,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueSize),
                runnable -> {
                  Thread thread =
                      new Thread(
                          runnable, "onvif-scan-worker-" + onvifScanThreadIndex.getAndIncrement());
                  thread.setDaemon(true);
                  return thread;
                },
                new ThreadPoolExecutor.AbortPolicy());
        onvifScanExecutor = executor;
      }
      return executor;
    }
  }

  private ThreadPoolExecutor getSdkDiscoveryExecutor() {
    ThreadPoolExecutor executor = sdkDiscoveryExecutor;
    if (executor != null) {
      return executor;
    }
    synchronized (this) {
      executor = sdkDiscoveryExecutor;
      if (executor == null) {
        int threads = Math.max(1, sdkDiscoveryAsyncThreads);
        int queueSize = Math.max(1, sdkDiscoveryAsyncQueueSize);
        executor =
            new ThreadPoolExecutor(
                threads,
                threads,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueSize),
                runnable -> {
                  Thread thread =
                      new Thread(
                          runnable,
                          "sdk-discovery-worker-" + sdkDiscoveryThreadIndex.getAndIncrement());
                  thread.setDaemon(true);
                  return thread;
                },
                new ThreadPoolExecutor.AbortPolicy());
        sdkDiscoveryExecutor = executor;
      }
      return executor;
    }
  }

  private VideoPlatformInstance requireOnvifInstance(String instanceKey) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    if (!"onvif".equalsIgnoreCase(instance.getPlatformType())) {
      throw new BaseException("当前接口仅用于ONVIF扫描");
    }
    return instance;
  }

  private VideoPlatformInstance requireDirectSdkInstance(String instanceKey) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    String platformType = instance == null ? "" : StrUtil.trimToEmpty(instance.getPlatformType()).toLowerCase();
    if (!"hik_sdk".equals(platformType) && !"dahua_sdk".equals(platformType) && !"tiandy_sdk".equals(platformType) && !"uniview_sdk".equals(platformType)) {
      throw new BaseException("当前接口仅用于设备直连SDK平台");
    }
    return instance;
  }

  private void runSdkDiscoveryTask(
      VideoPlatformInstance instance, String taskId, Map<String, Object> params) {
    String instanceKey = instance.getInstanceKey();
    long startedAt = System.currentTimeMillis();
    Map<String, Object> task = readJsonMapValue(buildSdkDiscoveryTaskKey(instanceKey, taskId));
    if (task == null || task.isEmpty()) {
      task = new LinkedHashMap<>();
      task.put("taskId", taskId);
      task.put("instanceKey", instanceKey);
      task.put("startedAt", startedAt);
      task.put("params", sanitizeSdkDiscoveryParams(params));
    }
    try {
      VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
      Map<String, Object> request = normalizeSdkDiscoveryPayload(instance, params);
      List<String> hosts = resolveSdkDiscoveryHosts(request);
      if (hosts.isEmpty()) {
        throw new BaseException("搜索范围为空，请填写网段或摄像头IP");
      }
      Map<String, Object> normalized =
          runSegmentedSdkDiscoveryTask(instance, taskId, task, adapter, request, hosts, startedAt);
      List<Map<String, Object>> devices = extractMapList(normalized.get("devices"));
      long finishedAt = System.currentTimeMillis();
      task.put("status", "success");
      task.put("running", false);
      task.put("accepted", false);
      task.put("devices", maskSdkDeviceSecrets(devices));
      task.put("count", devices.size());
      task.put("result", sanitizeSdkDiscoveryResult(normalized, true));
      task.put("processedHosts", hosts.size());
      task.put("totalHosts", hosts.size());
      task.put("updatedAt", finishedAt);
      task.put("finishedAt", finishedAt);
      task.put("durationMs", finishedAt - startedAt);
      task.put("message", "搜索完成，发现" + devices.size() + "台设备");
      cacheSdkDiscoveryTask(instanceKey, taskId, task);
    } catch (Exception e) {
      long finishedAt = System.currentTimeMillis();
      task.put("status", "failed");
      task.put("running", false);
      task.put("accepted", false);
      task.put("devices", List.of());
      task.put("count", 0);
      task.put("updatedAt", finishedAt);
      task.put("finishedAt", finishedAt);
      task.put("durationMs", finishedAt - startedAt);
      task.put("message", e.getMessage());
      cacheSdkDiscoveryTask(instanceKey, taskId, task);
      log.warn(
          "[直连SDK搜索] 异步搜索失败: instanceKey={}, taskId={}, error={}",
          instanceKey,
          taskId,
          e.getMessage(),
          e);
    } finally {
      releaseSdkDiscoveryLock(instanceKey, taskId);
    }
  }

  private Map<String, Object> runSegmentedSdkDiscoveryTask(
      VideoPlatformInstance instance,
      String taskId,
      Map<String, Object> task,
      VideoPlatformAdapter adapter,
      Map<String, Object> request,
      List<String> hosts,
      long startedAt) {
    String instanceKey = instance.getInstanceKey();
    int totalHosts = hosts.size();
    int concurrency = resolveSdkSegmentConcurrency(request);
    List<Map<String, Object>> devices = new ArrayList<>();
    Set<String> seenDeviceKeys = new LinkedHashSet<>();
    AtomicInteger processed = new AtomicInteger();
    AtomicInteger failed = new AtomicInteger();
    long progressAt = System.currentTimeMillis();
    task.put("totalHosts", totalHosts);
    task.put("processedHosts", 0);
    task.put("failedHosts", 0);
    task.put("message", "搜索中：0/" + totalHosts);
    cacheSdkDiscoveryTask(instanceKey, taskId, task);
    refreshSdkDiscoveryLock(instanceKey, taskId);

    ExecutorService probeExecutor =
        Executors.newFixedThreadPool(
            concurrency,
            runnable -> {
              Thread thread = new Thread(runnable);
              thread.setName("sdk-discovery-probe-" + sdkDiscoveryThreadIndex.getAndIncrement());
              thread.setDaemon(true);
              return thread;
            });
    ExecutorCompletionService<Map<String, Object>> completionService =
        new ExecutorCompletionService<>(probeExecutor);
    try {
      for (String host : hosts) {
        completionService.submit(() -> probeSdkDiscoveryHost(instance, adapter, request, host));
      }
      for (int i = 0; i < totalHosts; i++) {
        Future<Map<String, Object>> future = completionService.take();
        Map<String, Object> result;
        try {
          result = future.get();
        } catch (Exception e) {
          failed.incrementAndGet();
          result = new LinkedHashMap<>();
          result.put("error", e.getMessage());
        }
        if (result != null && Boolean.TRUE.equals(result.get("failed"))) {
          failed.incrementAndGet();
        }
        List<Map<String, Object>> foundDevices =
            extractMapList(firstNonNull(result == null ? null : result.get("devices"), result == null ? null : result.get("device")));
        for (Map<String, Object> device : foundDevices) {
          Map<String, Object> normalized = normalizeSdkCandidateForDisplay(instance, device);
          String key = sdkDiscoveryDeviceKey(normalized);
          if (StrUtil.isNotBlank(key) && seenDeviceKeys.add(key)) {
            devices.add(normalized);
          }
        }
        int done = processed.incrementAndGet();
        long now = System.currentTimeMillis();
        if (done == totalHosts || now - progressAt >= 1000L) {
          updateSdkDiscoveryProgress(
              instanceKey, taskId, task, devices, done, totalHosts, failed.get(), startedAt);
          progressAt = now;
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new BaseException("直连SDK搜索任务被中断");
    } finally {
      probeExecutor.shutdownNow();
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("vendor", sdkVendor(instance));
    result.put("hosts", totalHosts);
    result.put("processedHosts", processed.get());
    result.put("failedHosts", failed.get());
    result.put("ports", request.get("ports"));
    result.put("devices", devices);
    result.put("count", devices.size());
    result.put("segmented", true);
    return result;
  }

  private Map<String, Object> probeSdkDiscoveryHost(
      VideoPlatformInstance instance,
      VideoPlatformAdapter adapter,
      Map<String, Object> request,
      String host) {
    Map<String, Object> probe = new LinkedHashMap<>(request);
    probe.remove("network");
    probe.remove("cidr");
    probe.remove("endpoint");
    probe.remove("devices");
    probe.put("host", host);
    probe.put("ip", host);
    probe.put("deviceIp", host);
    probe.put("gatewayTimeoutMs", resolveSdkProbeGatewayTimeoutMs(request));
    try {
      return normalizeSdkDiscoveryResponse(instance, adapter.executeOperation(instance, "probe", probe));
    } catch (Exception e) {
      log.info(
          "[直连SDK搜索] 单IP探测失败: instanceKey={}, platformType={}, host={}, error={}",
          instance.getInstanceKey(),
          instance.getPlatformType(),
          host,
          e.getMessage());
      Map<String, Object> failed = new LinkedHashMap<>();
      failed.put("host", host);
      failed.put("ip", host);
      failed.put("deviceIp", host);
      failed.put("failed", true);
      failed.put("message", e.getMessage());
      failed.put("devices", List.of());
      return failed;
    }
  }

  private void updateSdkDiscoveryProgress(
      String instanceKey,
      String taskId,
      Map<String, Object> task,
      List<Map<String, Object>> devices,
      int processedHosts,
      int totalHosts,
      int failedHosts,
      long startedAt) {
    long now = System.currentTimeMillis();
    task.put("status", "running");
    task.put("running", true);
    task.put("accepted", false);
    task.put("processedHosts", processedHosts);
    task.put("totalHosts", totalHosts);
    task.put("failedHosts", failedHosts);
    task.put("devices", maskSdkDeviceSecrets(devices));
    task.put("count", devices.size());
    task.put("updatedAt", now);
    task.put("durationMs", now - startedAt);
    task.put("message", "搜索中：" + processedHosts + "/" + totalHosts + "，发现" + devices.size() + "台");
    cacheSdkDiscoveryTask(instanceKey, taskId, task);
    refreshSdkDiscoveryLock(instanceKey, taskId);
  }

  private void refreshSdkDiscoveryLock(String instanceKey, String taskId) {
    String lockKey = buildSdkDiscoveryLockKey(instanceKey);
    String lockedTaskId = stringRedisTemplate.opsForValue().get(lockKey);
    if (StrUtil.isNotBlank(taskId) && taskId.equals(lockedTaskId)) {
      stringRedisTemplate
          .opsForValue()
          .set(lockKey, taskId, Math.max(30L, sdkDiscoveryLockTtlSeconds), TimeUnit.SECONDS);
    }
  }

  private int resolveSdkSegmentConcurrency(Map<String, Object> request) {
    int configured = parseIntOrDefault(request == null ? null : request.get("concurrency"), 16);
    return clampInt(configured, 1, 16);
  }

  private int resolveSdkProbeGatewayTimeoutMs(Map<String, Object> request) {
    int timeoutMs = clampInt(parseIntOrDefault(request == null ? null : request.get("timeoutMs"), 1500), 200, 8000);
    int portCount = estimateSdkDiscoveryPortCount(request == null ? null : request.get("ports"));
    long waitMs = Math.max(1, portCount) * (long) timeoutMs + 15000L;
    return (int) Math.max(8000L, Math.min(45000L, waitMs));
  }

  private List<String> resolveSdkDiscoveryHosts(Map<String, Object> payload) {
    Set<String> hosts = new LinkedHashSet<>();
    for (Map<String, Object> device : extractMapList(payload == null ? null : payload.get("devices"))) {
      addSdkDiscoveryHost(hosts, firstNonBlankString(device.get("host"), device.get("ip"), device.get("deviceIp")));
    }
    addSdkDiscoveryHost(
        hosts,
        firstNonBlankString(
            payload == null ? null : payload.get("host"),
            payload == null ? null : payload.get("ip"),
            payload == null ? null : payload.get("deviceIp")));
    String scope =
        firstNonBlankString(
            payload == null ? null : payload.get("network"),
            payload == null ? null : payload.get("cidr"),
            payload == null ? null : payload.get("endpoint"));
    if (StrUtil.isNotBlank(scope)) {
      hosts.addAll(expandSdkDiscoveryNetwork(scope));
    }
    return hosts.stream().limit(512).toList();
  }

  private void addSdkDiscoveryHost(Set<String> hosts, String host) {
    if (hosts == null || StrUtil.isBlank(host)) {
      return;
    }
    String text = host.trim();
    if (isIpv4Address(text)) {
      hosts.add(text);
    }
  }

  private List<String> expandSdkDiscoveryNetwork(String value) {
    String text = StrUtil.trimToEmpty(value);
    if (StrUtil.isBlank(text)) {
      return List.of();
    }
    if (!text.contains("/")) {
      return isIpv4Address(text) ? List.of(text) : List.of();
    }
    String[] parts = text.split("/", 2);
    if (parts.length < 2 || !isIpv4Address(parts[0])) {
      return List.of();
    }
    int prefix = clampInt(parseIntOrDefault(parts[1], 24), 16, 32);
    long base = ipv4ToLong(parts[0]);
    long mask = prefix == 0 ? 0L : (0xffffffffL << (32 - prefix)) & 0xffffffffL;
    long start = base & mask;
    long size = 1L << (32 - prefix);
    long first = size <= 2 ? start : start + 1;
    long last = size <= 2 ? start + size - 1 : start + size - 2;
    List<String> hosts = new ArrayList<>();
    for (long ip = first; ip <= last && hosts.size() < 512; ip++) {
      hosts.add(longToIpv4(ip));
    }
    return hosts;
  }

  private boolean isIpv4Address(String value) {
    if (StrUtil.isBlank(value)) {
      return false;
    }
    String[] parts = value.trim().split("\\.");
    if (parts.length != 4) {
      return false;
    }
    for (String part : parts) {
      Integer number = parseIntOrNull(part);
      if (number == null || number < 0 || number > 255) {
        return false;
      }
    }
    return true;
  }

  private long ipv4ToLong(String ip) {
    long result = 0L;
    for (String part : ip.split("\\.")) {
      result = (result << 8) + parseIntOrDefault(part, 0);
    }
    return result & 0xffffffffL;
  }

  private String longToIpv4(long value) {
    return ((value >> 24) & 0xff)
        + "."
        + ((value >> 16) & 0xff)
        + "."
        + ((value >> 8) & 0xff)
        + "."
        + (value & 0xff);
  }

  private String sdkDiscoveryDeviceKey(Map<String, Object> device) {
    return firstNonBlankString(
        device == null ? null : device.get("deviceId"),
        device == null ? null : device.get("extDeviceId"),
        device == null ? null : device.get("serialNumber"),
        device == null ? null : device.get("deviceSn"),
        device == null ? null : device.get("deviceIp"),
        device == null ? null : device.get("ip"),
        device == null ? null : device.get("host"));
  }

  private Map<String, Object> normalizeSdkDiscoveryPayload(
      VideoPlatformInstance instance, Map<String, Object> params) {
    Map<String, Object> payload = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
    payload.putIfAbsent("vendor", sdkVendor(instance));
    payload.putIfAbsent("platformType", instance.getPlatformType());
    Map<String, Object> options = readObjectMap(instance.getOptions());
    Map<String, Object> sdkOptions = options.get("sdk") instanceof Map<?, ?> map ? toLinkedMap(map) : new LinkedHashMap<>();
    Object network = firstNonNull(payload.get("network"), payload.get("cidr"), payload.get("endpoint"), sdkOptions.get("network"));
    if (network != null) {
      payload.put("network", network);
    }
    if (!payload.containsKey("ports")) {
      payload.put("ports", defaultSdkDiscoveryPorts(instance));
    }
    payload.putIfAbsent("timeoutMs", firstNonNull(sdkOptions.get("timeoutMs"), 1500));
    payload.putIfAbsent("concurrency", firstNonNull(sdkOptions.get("concurrency"), 64));
    payload.putIfAbsent(
        "gatewayTimeoutMs",
        firstNonNull(sdkOptions.get("gatewayTimeoutMs"), estimateSdkGatewayTimeoutMs(payload)));
    payload.putIfAbsent("strategies", List.of("tcp_probe", "sdk_login", "rtsp_probe"));
    mergeDefaultCredential(instance, payload);
    return payload;
  }

  private Map<String, Object> buildSdkImportResult(String instanceKey, DeviceSyncResult syncResult) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("instanceKey", instanceKey);
    result.put("deviceCount", syncResult.getTotalDevices());
    result.put("totalDevices", syncResult.getTotalDevices());
    result.put("channelCount", syncResult.getTotalChannels());
    result.put("totalChannels", syncResult.getTotalChannels());
    result.put("insertedDevices", syncResult.getInsertedDevices());
    result.put("updatedDevices", syncResult.getUpdatedDevices());
    result.put("insertedChannels", syncResult.getInsertedChannels());
    result.put("errorCount", syncResult.getErrors() == null ? 0 : syncResult.getErrors().size());
    result.put("errors", syncResult.getErrors());
    return result;
  }

  private void mergeSdkImportDefault(
      Map<String, Object> device, Map<String, Object> body, String key) {
    if (device == null || body == null || device.containsKey(key)) {
      return;
    }
    Object value = body.get(key);
    if (StrUtil.isNotBlank(stringValue(value))) {
      device.put(key, value);
    }
  }

  private Map<String, Object> normalizeSdkProbePayload(
      VideoPlatformInstance instance, Map<String, Object> params) {
    Map<String, Object> payload = normalizeSdkDiscoveryPayload(instance, params);
    Map<String, Object> device = firstMap(payload.get("device"));
    String host = firstNonBlankString(payload.get("host"), payload.get("ip"), payload.get("deviceIp"), device.get("host"), device.get("ip"));
    if (StrUtil.isBlank(host)) {
      throw new BaseException("摄像头IP不能为空");
    }
    payload.put("host", host);
    payload.put("ip", host);
    payload.putIfAbsent("port", defaultSdkPort(instance));
    return payload;
  }

  private Map<String, Object> normalizeSdkDiscoveryResponse(
      VideoPlatformInstance instance, Map<String, Object> result) {
    Map<String, Object> normalized = result == null ? new LinkedHashMap<>() : new LinkedHashMap<>(result);
    List<Map<String, Object>> devices = extractMapList(firstNonNull(
        normalized.get("devices"), normalized.get("list"), normalized.get("rows"), normalized.get("items"), normalized.get("data")));
    if (devices.isEmpty() && hasSdkDeviceIdentity(normalized)) {
      devices = List.of(normalized);
    }
    List<Map<String, Object>> normalizedDevices = new ArrayList<>();
    for (Map<String, Object> device : devices) {
      Map<String, Object> item = normalizeSdkCandidateForDisplay(instance, device);
      if (!item.isEmpty()) {
        normalizedDevices.add(item);
      }
    }
    normalized.put("devices", normalizedDevices);
    normalized.put("count", normalizedDevices.size());
    normalized.putIfAbsent("vendor", sdkVendor(instance));
    return normalized;
  }

  private Map<String, Object> normalizeSdkCandidateForDisplay(
      VideoPlatformInstance instance, Map<String, Object> source) {
    Map<String, Object> device = source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    device.putAll(firstMap(device.get("attributes")));
    String ip = firstNonBlankString(device.get("deviceIp"), device.get("ip"), device.get("host"));
    String serial = firstNonBlankString(device.get("serialNumber"), device.get("deviceSn"), device.get("serial"), device.get("sn"));
    String deviceId = firstNonBlankString(device.get("deviceId"), device.get("id"), serial, ip);
    if (StrUtil.isBlank(deviceId)) {
      return Map.of();
    }
    String vendor = firstNonBlankString(device.get("vendor"), sdkVendor(instance));
    String manufacturer = switch (vendor.toLowerCase(java.util.Locale.ROOT)) {
      case "dahua" -> "Dahua";
      case "tiandy" -> "Tiandy";
      case "uniview" -> "Uniview";
      default -> "Hikvision";
    };
    device.put("deviceId", deviceId);
    device.put("extDeviceId", deviceId);
    device.put("deviceName", firstNonBlankString(device.get("deviceName"), device.get("name"), manufacturer + " " + firstNonBlankString(ip, deviceId)));
    device.put("name", device.get("deviceName"));
    device.put("vendor", vendor);
    String reportedManufacturer = firstNonBlankString(device.get("manufacturer"), manufacturer);
    // Correct candidates cached before Uniview had its own manufacturer default.
    if ("uniview_sdk".equalsIgnoreCase(instance.getPlatformType())
        && "Hikvision".equalsIgnoreCase(reportedManufacturer)) {
      reportedManufacturer = "Uniview";
    }
    device.put("manufacturer", reportedManufacturer);
    device.put("deviceIp", ip);
    device.put("ip", ip);
    device.put("devicePort", firstNonNull(device.get("devicePort"), device.get("port"), device.get("sdkPort"), defaultSdkPort(instance)));
    device.put("sdkPort", firstNonNull(device.get("sdkPort"), device.get("port"), device.get("devicePort"), defaultSdkPort(instance)));
    device.put("rtspPort", firstNonNull(device.get("rtspPort"), defaultSdkRtspPort(instance)));
    device.put("deviceStatus", normalizeOnlineText(firstNonNull(device.get("deviceStatus"), device.get("status"), device.get("online"), true)));
    device.put("status", device.get("deviceStatus"));
    if (StrUtil.isNotBlank(serial)) {
      device.put("serialNumber", serial);
      device.put("deviceSn", serial);
    }
    List<Map<String, Object>> channels = normalizeSdkChannelsForDisplay(instance, device, extractMapList(firstNonNull(device.get("channelList"), device.get("channels"))));
    boolean authoritativeCatalog = supportsSdkChannelCatalog(instance.getPlatformType())
        && parseBoolean(device.get("channelCatalogAuthoritative"));
    device.put("channelList", channels.isEmpty() && !authoritativeCatalog ? List.of(defaultSdkChannel(instance, device)) : channels);
    device.put("channels", device.get("channelList"));
    device.put("channelCount", authoritativeCatalog
        ? (int) channels.stream().map(channel -> VideoDeviceNames.physicalId(
            String.valueOf(channel.get("channelId")))).distinct().count()
        : ((List<?>) device.get("channelList")).size());
    return device;
  }

  private Map<String, Object> normalizeSdkCandidateForSync(
      VideoPlatformInstance instance, Map<String, Object> source, Map<String, Object> credential) {
    Map<String, Object> device = normalizeSdkCandidateForDisplay(instance, source);
    if (device.isEmpty()) {
      return device;
    }
    Map<String, Object> access = new LinkedHashMap<>();
    access.putAll(sdkDefaultCredential(instance));
    access.putAll(removeMaskedCredentialSecrets(credential == null ? Map.of() : credential));
    access.putAll(removeMaskedCredentialSecrets(firstMap(source.get("access"))));
    access.putAll(removeMaskedCredentialSecrets(firstMap(source.get("credential"))));
    access.putAll(removeMaskedCredentialSecrets(firstMap(source.get("auth"))));

    for (String key :
        List.of(
            "username",
            "password",
            "authType",
            "rtspPort",
            "sdkPort",
            "timeoutMs",
            "mediaServerId",
            "zlmMediaServerId",
            "previewMediaServerId")) {
      Object value = firstNonMaskedSecret(key, source.get(key), access.get(key), device.get(key));
      if (value != null && !isMaskedSecret(key, value)) {
        device.put(key, value);
      }
    }
    device.put("port", firstNonNull(device.get("sdkPort"), device.get("devicePort"), device.get("port"), defaultSdkPort(instance)));
    device.put("devicePort", device.get("port"));
    device.putIfAbsent("streamMode", firstNonNull(sdkDefaultOption(instance, "streamMode"),
        "uniview_sdk".equalsIgnoreCase(instance.getPlatformType()) ? "sdk_only" : "rtsp_first"));
    device.putIfAbsent("loginMode", "direct_sdk");
    device.putIfAbsent("gatewayEndpoint", instance.getEndpoint());
    return device;
  }

  private List<Map<String, Object>> normalizeSdkChannelsForDisplay(
      VideoPlatformInstance instance, Map<String, Object> device, List<Map<String, Object>> channels) {
    List<Map<String, Object>> normalized = new ArrayList<>();
    for (Map<String, Object> source : channels) {
      Map<String, Object> channel = new LinkedHashMap<>(source);
      channel.putAll(firstMap(channel.get("attributes")));
      String channelId =
          firstNonBlankString(
              channel.get("channelId"),
              channel.get("channelCode"),
              channel.get("cameraIndexCode"),
              channel.get("id"),
              channel.get("channelNo"),
              channel.get("channelSeq"));
      if (StrUtil.isBlank(channelId)) {
        continue;
      }
      channel.put("channelId", channelId);
      channel.put("channelName", firstNonBlankString(channel.get("channelName"), channel.get("name"), "CH" + channelId));
      channel.put("name", channel.get("channelName"));
      Object channelStatus = firstNonNull(channel.get("channelStatus"), channel.get("status"), channel.get("online"), true);
      if (supportsSdkChannelCatalog(instance.getPlatformType())) {
        String onlineState = StrUtil.trimToEmpty(stringValue(channel.get("onlineState"))).toLowerCase(java.util.Locale.ROOT);
        if (List.of("online", "offline", "unknown").contains(onlineState)) {
          channelStatus = onlineState;
        }
      }
      channel.put("channelStatus", normalizeOnlineText(channelStatus));
      channel.put("status", channel.get("channelStatus"));
      channel.put("ipAddress", firstNonBlankString(channel.get("ipAddress"), channel.get("ip"), device.get("deviceIp")));
      channel.put("port", firstNonNull(channel.get("port"), device.get("rtspPort"), 554));
      channel.put("manufacturer", device.get("manufacturer"));
      if ("dahua_sdk".equalsIgnoreCase(instance.getPlatformType()) || "tiandy_sdk".equalsIgnoreCase(instance.getPlatformType()) || "uniview_sdk".equalsIgnoreCase(instance.getPlatformType())) {
        channel.put("channelCode", firstNonBlankString(channel.get("channelCode"), channelId));
        channel.put("channelSeq", firstNonNull(parseIntOrNull(firstNonBlankString(channel.get("channelSeq"), channel.get("channelNo"), channel.get("rtspChannel"))), 1));
        channel.put("rtspChannel", firstNonBlankString(channel.get("rtspChannel"), channel.get("channelSeq"), channel.get("channelNo"), "1"));
      } else {
        channel.put("cameraIndexCode", firstNonBlankString(channel.get("cameraIndexCode"), channelId));
        channel.put("channelNo", firstNonBlankString(channel.get("channelNo"), channel.get("channelSeq"), "1"));
      }
      channel.put("parentId", firstNonBlankString(channel.get("parentId"), device.get("deviceId")));
      normalized.add(channel);
    }
    return normalized;
  }

  private Map<String, Object> defaultSdkChannel(VideoPlatformInstance instance, Map<String, Object> device) {
    Map<String, Object> channel = new LinkedHashMap<>();
    channel.put("channelId", "1");
    channel.put("channelName", "通道1");
    channel.put("name", "通道1");
    channel.put("channelStatus", "online");
    channel.put("status", "online");
    channel.put("ipAddress", device.get("deviceIp"));
    channel.put("port", firstNonNull(device.get("rtspPort"), 554));
    channel.put("manufacturer", device.get("manufacturer"));
    channel.put("channelNo", "1");
    channel.put("channelSeq", 1);
    channel.put("parentId", device.get("deviceId"));
    if ("dahua_sdk".equalsIgnoreCase(instance.getPlatformType()) || "tiandy_sdk".equalsIgnoreCase(instance.getPlatformType()) || "uniview_sdk".equalsIgnoreCase(instance.getPlatformType())) {
      channel.put("channelCode", "1");
      channel.put("rtspChannel", "1");
    } else {
      channel.put("cameraIndexCode", "1");
    }
    return channel;
  }

  private boolean supportsSdkChannelCatalog(String platformType) {
    return "dahua_sdk".equalsIgnoreCase(platformType) || "hik_sdk".equalsIgnoreCase(platformType)
        || "tiandy_sdk".equalsIgnoreCase(platformType) || "uniview_sdk".equalsIgnoreCase(platformType);
  }

  private Map<String, Object> sanitizeSdkDiscoveryParams(Map<String, Object> params) {
    Map<String, Object> safe = sanitizeOnvifScanParams(params);
    if (safe.containsKey("auth")) {
      safe.put("auth", maskNestedSecrets(safe.get("auth")));
    }
    if (safe.containsKey("device")) {
      safe.put("device", maskNestedSecrets(safe.get("device")));
    }
    return safe;
  }

  private Map<String, Object> sanitizeSdkDiscoveryResult(Map<String, Object> result, boolean maskSecrets) {
    Map<String, Object> safe = result == null ? new LinkedHashMap<>() : new LinkedHashMap<>(result);
    if (maskSecrets) {
      safe.put("devices", maskSdkDeviceSecrets(extractMapList(safe.get("devices"))));
      Object device = safe.get("device");
      if (device instanceof Map<?, ?> map) {
        Map<String, Object> masked = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
          if (entry.getKey() != null) {
            masked.put(String.valueOf(entry.getKey()), entry.getValue());
          }
        }
        maskSdkSecretKeys(masked);
        safe.put("device", masked);
      }
    }
    return safe;
  }

  private Object maskNestedSecrets(Object value) {
    if (!(value instanceof Map<?, ?> map)) {
      return value;
    }
    Map<String, Object> masked = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey());
      Object item = entry.getValue();
      if (key.toLowerCase().contains("password") || key.toLowerCase().contains("secret")) {
        masked.put(key, StrUtil.isBlank(stringValue(item)) ? "" : "***");
      } else {
        masked.put(key, item);
      }
    }
    return masked;
  }

  private List<Map<String, Object>> maskSdkDeviceSecrets(List<Map<String, Object>> devices) {
    List<Map<String, Object>> masked = new ArrayList<>();
    for (Map<String, Object> device : devices) {
      Map<String, Object> item = new LinkedHashMap<>(device);
      maskSdkSecretKeys(item);
      masked.add(item);
    }
    return masked;
  }

  private void maskSdkSecretKeys(Map<String, Object> map) {
    for (String key : new ArrayList<>(map.keySet())) {
      Object value = map.get(key);
      String lower = key == null ? "" : key.toLowerCase();
      if (lower.contains("password") || lower.contains("secret")) {
        map.put(key, StrUtil.isBlank(stringValue(value)) ? "" : "***");
      } else if (value instanceof Map<?, ?> nested) {
        map.put(key, maskNestedSecrets(nested));
      }
    }
  }

  private Map<String, Object> firstCredential(Map<String, Object> body) {
    Map<String, Object> credential = new LinkedHashMap<>();
    List<Map<String, Object>> credentials = extractMapList(body == null ? null : body.get("credentials"));
    if (!credentials.isEmpty()) {
      credential.putAll(removeMaskedCredentialSecrets(credentials.get(0)));
    }
    if (body != null) {
      putFirstNonBlank(
          credential,
          "username",
          body.get("username"),
          body.get("defaultUsername"),
          body.get("userName"),
          body.get("user"));
      putFirstNonBlank(
          credential,
          "password",
          body.get("password"),
          body.get("defaultPassword"),
          body.get("psw"));
      putIfNotBlank(credential, "authType", body.get("authType"));
      putIfNotBlank(credential, "timeoutMs", body.get("timeoutMs"));
    }
    return credential;
  }

  private void mergeDefaultCredential(VideoPlatformInstance instance, Map<String, Object> payload) {
    Map<String, Object> auth = readObjectMap(instance.getAuth());
    if (!payload.containsKey("credentials")) {
      List<Map<String, Object>> credentials = extractMapList(auth.get("credentials"));
      if (!credentials.isEmpty()) {
        payload.put("credentials", credentials.stream().map(this::removeMaskedCredentialSecrets).toList());
      } else if (StrUtil.isNotBlank(firstNonBlankString(auth.get("username"), auth.get("defaultUsername"), auth.get("userName"), auth.get("user")))) {
        Map<String, Object> credential = new LinkedHashMap<>();
        credential.put(
            "username",
            firstNonBlankString(auth.get("username"), auth.get("defaultUsername"), auth.get("userName"), auth.get("user")));
        credential.put("password", firstNonBlankString(auth.get("password"), auth.get("defaultPassword"), auth.get("psw")));
        payload.put("credentials", List.of(credential));
      }
    }
    if (!payload.containsKey("username")) {
      putFirstNonBlank(payload, "username", auth.get("username"), auth.get("defaultUsername"), auth.get("userName"), auth.get("user"));
    }
    if (!payload.containsKey("password")) {
      putFirstNonBlank(payload, "password", auth.get("password"), auth.get("defaultPassword"), auth.get("psw"));
    }
  }

  private Map<String, Object> sdkDefaultCredential(VideoPlatformInstance instance) {
    Map<String, Object> auth = readObjectMap(instance == null ? null : instance.getAuth());
    Map<String, Object> credential = new LinkedHashMap<>();
    putFirstNonBlank(credential, "username", auth.get("username"), auth.get("defaultUsername"), auth.get("userName"), auth.get("user"));
    putFirstNonBlank(credential, "password", auth.get("password"), auth.get("defaultPassword"), auth.get("psw"));
    return credential;
  }

  private List<Integer> defaultSdkDiscoveryPorts(VideoPlatformInstance instance) {
    if ("dahua_sdk".equalsIgnoreCase(instance.getPlatformType())) {
      return List.of(37777, 554, 80);
    }
    if ("tiandy_sdk".equalsIgnoreCase(instance.getPlatformType())) {
      return List.of(3000, 3001, 3002, 554, 80);
    }
    if ("uniview_sdk".equalsIgnoreCase(instance.getPlatformType())) {
      return List.of(80, 554);
    }
    return List.of(8000, 554, 80);
  }

  private int estimateSdkGatewayTimeoutMs(Map<String, Object> payload) {
    int timeoutMs = clampInt(parseIntOrDefault(payload == null ? null : payload.get("timeoutMs"), 1500), 200, 8000);
    int concurrency = clampInt(parseIntOrDefault(payload == null ? null : payload.get("concurrency"), 64), 1, 96);
    int hostCount = estimateSdkDiscoveryHostCount(payload);
    int portCount = estimateSdkDiscoveryPortCount(payload == null ? null : payload.get("ports"));
    long batches = (Math.max(1, hostCount) + Math.max(1, concurrency) - 1L) / Math.max(1, concurrency);
    long waitMs = batches * (Math.max(1, portCount) * (long) timeoutMs + 12000L) + 15000L;
    return (int) Math.max(25000L, Math.min(180000L, waitMs));
  }

  private int estimateSdkDiscoveryHostCount(Map<String, Object> payload) {
    if (payload == null || payload.isEmpty()) {
      return 128;
    }
    List<Map<String, Object>> devices = extractMapList(payload.get("devices"));
    if (!devices.isEmpty()) {
      return Math.min(512, devices.size());
    }
    if (StrUtil.isNotBlank(firstNonBlankString(payload.get("host"), payload.get("ip"), payload.get("deviceIp")))) {
      return 1;
    }
    String network = firstNonBlankString(payload.get("network"), payload.get("cidr"), payload.get("endpoint"));
    if (StrUtil.isBlank(network)) {
      return 128;
    }
    if (!network.contains("/")) {
      return 1;
    }
    String[] parts = network.split("/", 2);
    Integer prefix = parseIntOrNull(parts.length > 1 ? parts[1] : null);
    if (prefix == null) {
      return 128;
    }
    int normalizedPrefix = clampInt(prefix, 16, 32);
    long size = 1L << (32 - normalizedPrefix);
    long usable = size <= 2 ? size : size - 2;
    return (int) Math.max(1L, Math.min(512L, usable));
  }

  private int estimateSdkDiscoveryPortCount(Object portsValue) {
    Set<Integer> ports = new LinkedHashSet<>();
    if (portsValue instanceof Iterable<?> iterable) {
      for (Object item : iterable) {
        Integer port = parseIntOrNull(item);
        if (port != null && port > 0 && port <= 65535) {
          ports.add(port);
        }
      }
    } else if (portsValue != null) {
      for (String item : String.valueOf(portsValue).split("[,;\\s]+")) {
        Integer port = parseIntOrNull(item);
        if (port != null && port > 0 && port <= 65535) {
          ports.add(port);
        }
      }
    }
    return ports.isEmpty() ? 3 : Math.min(8, ports.size());
  }

  private int defaultSdkPort(VideoPlatformInstance instance) {
    Integer configuredPort = parseIntOrNull(sdkDefaultOption(instance, "sdkPortDefault"));
    if (configuredPort != null && configuredPort > 0 && configuredPort <= 65535) {
      return configuredPort;
    }
    return switch (StrUtil.trimToEmpty(instance.getPlatformType()).toLowerCase(java.util.Locale.ROOT)) {
      case "dahua_sdk" -> 37777;
      case "tiandy_sdk" -> 3000;
      case "uniview_sdk" -> 80;
      default -> 8000;
    };
  }

  private int defaultSdkRtspPort(VideoPlatformInstance instance) {
    Integer port = parseIntOrNull(sdkDefaultOption(instance, "rtspPortDefault"));
    return port != null && port > 0 && port <= 65535 ? port : 554;
  }

  private Object sdkDefaultOption(VideoPlatformInstance instance, String key) {
    return firstNonNull(readObjectMap(instance.getOptions()).get(key), readObjectMap(instance.getAuth()).get(key));
  }

  private String sdkVendor(VideoPlatformInstance instance) {
    return switch (StrUtil.trimToEmpty(instance.getPlatformType()).toLowerCase(java.util.Locale.ROOT)) {
      case "dahua_sdk" -> "dahua";
      case "tiandy_sdk" -> "tiandy";
      case "uniview_sdk" -> "uniview";
      default -> "hik";
    };
  }

  @Override
  @Transactional
  @CacheEvict(
      cacheNames = {"getVideoPlatformInstance"},
      allEntries = true)
  public VideoPlatformInstance updateSdkDefaultMediaServer(
      String instanceKey, Map<String, Object> body) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);
    if (!cn.universal.dm.video.VideoSdkPlatforms.managed(instance.getPlatformType()))
      throw new BaseException("当前接口仅用于视频 SDK 或 ISUP 平台");
    String mediaServerId =
        firstNonBlankString(
            body == null ? null : body.get("zlmMediaServerId"),
            body == null ? null : body.get("mediaServerId"),
            body == null ? null : body.get("previewMediaServerId"));
    mediaServerId = StrUtil.isBlank(mediaServerId) ? null : StrUtil.sub(mediaServerId, 0, 100);

    Map<String, Object> auth = readObjectMap(instance.getAuth());
    Map<String, Object> options = readObjectMap(instance.getOptions());
    applyDefaultMediaServer(auth, mediaServerId);
    applyDefaultMediaServer(options, mediaServerId);
    if ("hik_isup".equals(instance.getPlatformType()) && options.get("sdk") instanceof Map<?, ?> nested) {
      Map<String, Object> sdk = toLinkedMap(nested);
      applyDefaultMediaServer(sdk, mediaServerId);
      options.put("sdk", sdk);
    }

    instance.setAuth(writeObjectMap(auth));
    instance.setOptions(writeObjectMap(options));
    instance.setUpdateTime(new Date());
    instanceMapper.updateByPrimaryKeySelective(instance);
    return instanceMapper.selectByPrimaryKey(instance.getId());
  }

  private void applyDefaultMediaServer(Map<String, Object> target, String mediaServerId) {
    if (target == null) {
      return;
    }
    target.remove("zlmMediaServerId");
    target.remove("mediaServerId");
    target.remove("previewMediaServerId");
    if (StrUtil.isBlank(mediaServerId)) {
      return;
    }
    target.put("zlmMediaServerId", mediaServerId);
    target.put("mediaServerId", mediaServerId);
    target.put("previewMediaServerId", mediaServerId);
  }

  private boolean hasSdkDeviceIdentity(Map<String, Object> device) {
    return StrUtil.isNotBlank(
        firstNonBlankString(
            device == null ? null : device.get("deviceId"),
            device == null ? null : device.get("id"),
            device == null ? null : device.get("serialNumber"),
            device == null ? null : device.get("deviceSn"),
            device == null ? null : device.get("ip"),
            device == null ? null : device.get("host")));
  }

  private List<Map<String, Object>> extractMapList(Object value) {
    if (value == null) {
      return List.of();
    }
    if (value instanceof List<?> list) {
      List<Map<String, Object>> result = new ArrayList<>();
      for (Object item : list) {
        Map<String, Object> map = firstMap(item);
        if (!map.isEmpty()) {
          result.add(map);
        }
      }
      return result;
    }
    if (value instanceof Iterable<?> iterable) {
      List<Map<String, Object>> result = new ArrayList<>();
      for (Object item : iterable) {
        Map<String, Object> map = firstMap(item);
        if (!map.isEmpty()) {
          result.add(map);
        }
      }
      return result;
    }
    Map<String, Object> map = firstMap(value);
    return map.isEmpty() ? List.of() : List.of(map);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> firstMap(Object value) {
    if (value == null) {
      return new LinkedHashMap<>();
    }
    if (value instanceof Map<?, ?> map) {
      return toLinkedMap(map);
    }
    if (value instanceof JsonNode node && node.isObject()) {
      Map<String, Object> result = new LinkedHashMap<>();
      node.fields().forEachRemaining(entry -> result.put(entry.getKey(), jsonNodeToPlainValue(entry.getValue())));
      return result;
    }
    if (value instanceof String text && StrUtil.isNotBlank(text) && text.trim().startsWith("{")) {
      return readObjectMap(text);
    }
    return new LinkedHashMap<>();
  }

  private Map<String, Object> toLinkedMap(Map<?, ?> map) {
    Map<String, Object> result = new LinkedHashMap<>();
    if (map == null) {
      return result;
    }
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (entry.getKey() != null) {
        result.put(String.valueOf(entry.getKey()), entry.getValue());
      }
    }
    return result;
  }

  private String normalizeOnlineText(Object value) {
    if (value instanceof Boolean bool) {
      return bool ? "online" : "offline";
    }
    String text = StrUtil.trimToEmpty(stringValue(value)).toLowerCase(java.util.Locale.ROOT);
    if (text.isEmpty() || "true".equals(text) || "1".equals(text) || "on".equals(text) || "online".equals(text) || "success".equals(text)) {
      return "online";
    }
    if ("false".equals(text) || "0".equals(text) || "off".equals(text) || "offline".equals(text) || "failed".equals(text)) {
      return "offline";
    }
    return text;
  }

  private String firstNonBlankString(Object... values) {
    if (values == null) {
      return null;
    }
    for (Object value : values) {
      String text = stringValue(value);
      if (StrUtil.isNotBlank(text)) {
        return text;
      }
    }
    return null;
  }

  private void cacheSdkDiscoveryTask(String instanceKey, String taskId, Map<String, Object> task) {
    if (task == null) {
      return;
    }
    long ttlSeconds = Math.max(60L, sdkDiscoveryResultTtlSeconds);
    writeJsonValue(buildSdkDiscoveryTaskKey(instanceKey, taskId), task, ttlSeconds, TimeUnit.SECONDS);
    writeJsonValue(buildSdkDiscoveryLatestKey(instanceKey), task, ttlSeconds, TimeUnit.SECONDS);
  }

  private void fillSdkDiscoveryTaskRuntime(String instanceKey, Map<String, Object> task) {
    if (task == null) {
      return;
    }
    String taskId = stringValue(task.get("taskId"));
    String status = stringValue(task.get("status"));
    String lockedTaskId = stringRedisTemplate.opsForValue().get(buildSdkDiscoveryLockKey(instanceKey));
    boolean running =
        "running".equalsIgnoreCase(status)
            && StrUtil.isNotBlank(taskId)
            && taskId.equals(lockedTaskId);
    if (running) {
      task.put("running", true);
      task.put("status", "running");
      if (StrUtil.isBlank(stringValue(task.get("message")))) {
        task.put("message", "搜索中");
      }
    } else if ("running".equalsIgnoreCase(status)) {
      task.put("running", false);
      task.put("status", "failed");
      task.put("message", "搜索任务已结束，未返回结果");
    } else {
      task.put("running", false);
    }
  }

  private void releaseSdkDiscoveryLock(String instanceKey, String taskId) {
    String lockKey = buildSdkDiscoveryLockKey(instanceKey);
    String lockedTaskId = stringRedisTemplate.opsForValue().get(lockKey);
    if (StrUtil.isBlank(taskId) || taskId.equals(lockedTaskId)) {
      stringRedisTemplate.delete(lockKey);
    }
  }

  private String buildSdkDiscoveryLockKey(String instanceKey) {
    return SDK_DISCOVERY_LOCK_PREFIX + StrUtil.blankToDefault(instanceKey, "");
  }

  private String buildSdkDiscoveryRateKey(String instanceKey) {
    return SDK_DISCOVERY_RATE_PREFIX + StrUtil.blankToDefault(instanceKey, "");
  }

  private String buildSdkDiscoveryTaskKey(String instanceKey, String taskId) {
    return SDK_DISCOVERY_TASK_PREFIX
        + StrUtil.blankToDefault(instanceKey, "")
        + ":"
        + StrUtil.blankToDefault(taskId, "");
  }

  private String buildSdkDiscoveryLatestKey(String instanceKey) {
    return SDK_DISCOVERY_LATEST_PREFIX + StrUtil.blankToDefault(instanceKey, "");
  }

  private void runOnvifScanTask(
      VideoPlatformInstance instance, String taskId, Map<String, Object> params) {
    String instanceKey = instance.getInstanceKey();
    long startedAt = System.currentTimeMillis();
    Map<String, Object> task = readJsonMapValue(buildOnvifScanTaskKey(instanceKey, taskId));
    if (task == null || task.isEmpty()) {
      task = new LinkedHashMap<>();
      task.put("taskId", taskId);
      task.put("instanceKey", instanceKey);
      task.put("startedAt", startedAt);
      task.put("params", sanitizeOnvifScanParams(params));
    }
    try {
      VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
      Map<String, Object> result = adapter.executeOperation(instance, "discover", params);
      List<?> devices =
          result == null || !(result.get("devices") instanceof List<?> list)
              ? List.of()
              : list;
      long finishedAt = System.currentTimeMillis();
      task.put("status", "success");
      task.put("running", false);
      task.put("accepted", false);
      task.put("devices", devices);
      task.put("count", devices.size());
      task.put("result", result == null ? Map.of() : result);
      task.put("updatedAt", finishedAt);
      task.put("finishedAt", finishedAt);
      task.put("durationMs", finishedAt - startedAt);
      task.put("message", "扫描完成");
      cacheOnvifScanTask(instanceKey, taskId, task);
    } catch (Exception e) {
      long finishedAt = System.currentTimeMillis();
      task.put("status", "failed");
      task.put("running", false);
      task.put("accepted", false);
      task.put("devices", List.of());
      task.put("count", 0);
      task.put("updatedAt", finishedAt);
      task.put("finishedAt", finishedAt);
      task.put("durationMs", finishedAt - startedAt);
      task.put("message", e.getMessage());
      cacheOnvifScanTask(instanceKey, taskId, task);
      log.warn("[ONVIF扫描] 异步扫描失败: instanceKey={}, taskId={}, error={}", instanceKey, taskId, e.getMessage(), e);
    } finally {
      releaseOnvifScanLock(instanceKey, taskId);
    }
  }

  private void cacheOnvifScanTask(String instanceKey, String taskId, Map<String, Object> task) {
    if (task == null) {
      return;
    }
    long ttlSeconds = Math.max(60L, onvifScanResultTtlSeconds);
    writeJsonValue(buildOnvifScanTaskKey(instanceKey, taskId), task, ttlSeconds, TimeUnit.SECONDS);
    writeJsonValue(buildOnvifScanLatestKey(instanceKey), task, ttlSeconds, TimeUnit.SECONDS);
  }

  private void fillOnvifTaskRuntime(String instanceKey, Map<String, Object> task) {
    if (task == null) {
      return;
    }
    String taskId = stringValue(task.get("taskId"));
    String status = stringValue(task.get("status"));
    String lockedTaskId = stringRedisTemplate.opsForValue().get(buildOnvifScanLockKey(instanceKey));
    boolean running =
        "running".equalsIgnoreCase(status)
            && StrUtil.isNotBlank(taskId)
            && taskId.equals(lockedTaskId);
    if (running) {
      task.put("running", true);
      task.put("status", "running");
      task.put("message", "扫描中");
    } else if ("running".equalsIgnoreCase(status)) {
      task.put("running", false);
      task.put("status", "failed");
      task.put("message", "扫描任务已结束，未返回结果");
    } else {
      task.put("running", false);
    }
  }

  private void releaseOnvifScanLock(String instanceKey, String taskId) {
    String lockKey = buildOnvifScanLockKey(instanceKey);
    String lockedTaskId = stringRedisTemplate.opsForValue().get(lockKey);
    if (StrUtil.isBlank(taskId) || taskId.equals(lockedTaskId)) {
      stringRedisTemplate.delete(lockKey);
    }
  }

  private Map<String, Object> sanitizeOnvifScanParams(Map<String, Object> params) {
    Map<String, Object> safe = new LinkedHashMap<>();
    if (params == null || params.isEmpty()) {
      return safe;
    }
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      String key = entry.getKey();
      if (key == null) {
        continue;
      }
      String lower = key.toLowerCase();
      Object value = entry.getValue();
      if (lower.contains("password") || lower.contains("secret") || lower.contains("token")) {
        safe.put(key, StrUtil.isBlank(stringValue(value)) ? "" : "***");
      } else if ("credentials".equals(lower) && value instanceof Iterable<?> iterable) {
        List<Map<String, Object>> credentials = new ArrayList<>();
        for (Object item : iterable) {
          if (item instanceof Map<?, ?> map) {
            Map<String, Object> credential = new LinkedHashMap<>();
            credential.put("username", stringValue(map.get("username")));
            credential.put("password", StrUtil.isBlank(stringValue(map.get("password"))) ? "" : "***");
            credential.put("authType", stringValue(map.get("authType")));
            credentials.add(credential);
          }
        }
        safe.put(key, credentials);
      } else {
        safe.put(key, value);
      }
    }
    return safe;
  }

  private boolean hasSelectedDevices(Map<String, Object> filters) {
    Object devices = filters == null ? null : filters.get("devices");
    if (devices instanceof Iterable<?> iterable) {
      return iterable.iterator().hasNext();
    }
    return false;
  }

  private String buildOnvifScanLockKey(String instanceKey) {
    return ONVIF_SCAN_LOCK_PREFIX + StrUtil.blankToDefault(instanceKey, "");
  }

  private String buildOnvifScanRateKey(String instanceKey) {
    return ONVIF_SCAN_RATE_PREFIX + StrUtil.blankToDefault(instanceKey, "");
  }

  private String buildOnvifScanTaskKey(String instanceKey, String taskId) {
    return ONVIF_SCAN_TASK_PREFIX
        + StrUtil.blankToDefault(instanceKey, "")
        + ":"
        + StrUtil.blankToDefault(taskId, "");
  }

  private String buildOnvifScanLatestKey(String instanceKey) {
    return ONVIF_SCAN_LATEST_PREFIX + StrUtil.blankToDefault(instanceKey, "");
  }

  private List<FusionBinding> findFrontendOsdBindingsForEvent(DeviceShadowUpdatedEvent event) {
    return findFusionBindingsForEvent(event, fusionFrontendOsdSensorBindingIndex, fusionFrontendOsdRegistry);
  }

  private List<FusionBinding> findBackendFusionBindingsForEvent(DeviceShadowUpdatedEvent event) {
    return findFusionBindingsForEvent(event, fusionSensorBindingIndex, fusionTaskRegistry);
  }

  private List<FusionBinding> findFusionBindingsForEvent(
      DeviceShadowUpdatedEvent event,
      Map<String, Set<String>> sensorBindingIndex,
      Map<String, FusionBinding> registry) {
    if (event == null || StrUtil.isBlank(event.getIotId())) {
      return Collections.emptyList();
    }
    Set<String> registryKeys = sensorBindingIndex.get(event.getIotId());
    if (registryKeys == null || registryKeys.isEmpty()) {
      return Collections.emptyList();
    }
    List<FusionBinding> result = new ArrayList<>();
    for (String registryKey : new ArrayList<>(registryKeys)) {
      FusionBinding binding = registry.get(registryKey);
      if (binding != null) {
        result.add(binding);
      }
    }
    return result;
  }

  private void syncFusionTaskStatusIfDue(FusionBinding binding) {
    if (binding == null || fusionStatusSyncIntervalMs <= 0) {
      return;
    }
    String registryKey = buildFusionRegistryKey(binding.instanceKey, binding.deviceId, binding.channelId);
    long now = System.currentTimeMillis();
    Long last = fusionTaskStatusSyncAt.get(registryKey);
    if (last != null && now - last < fusionStatusSyncIntervalMs) {
      return;
    }
    fusionTaskStatusSyncAt.put(registryKey, now);
    syncFusionTaskStatusInternal(binding.instanceKey, binding.deviceId, binding.channelId, false, true);
  }

  private Map<String, Object> publishFusionOverlayStreamInternal(
      String instanceKey, String deviceId, String channelId, String reason, boolean force) {
    VideoPlatformChannel channel = requireChannel(instanceKey, deviceId, channelId);
    Map<String, Object> fusionConfig = extractFusionConfig(channel, instanceKey, deviceId);
    return publishFusionOverlayStreamInternal(
        instanceKey, deviceId, channelId, reason, force, channel, fusionConfig, null, null);
  }

  private Map<String, Object> publishFusionOverlayStreamInternal(
      String instanceKey,
      String deviceId,
      String channelId,
      String reason,
      boolean force,
      VideoPlatformChannel channel,
      Map<String, Object> fusionConfig,
      DeviceShadowUpdatedEvent event) {
    return publishFusionOverlayStreamInternal(
        instanceKey, deviceId, channelId, reason, force, channel, fusionConfig, event, null);
  }

  private Map<String, Object> publishFusionOverlayStreamInternal(
      String instanceKey,
      String deviceId,
      String channelId,
      String reason,
      boolean force,
      VideoPlatformChannel channel,
      Map<String, Object> fusionConfig,
      DeviceShadowUpdatedEvent event,
      Map<String, Object> overlaySnapshot) {
    // Redis Stream 只服务后端融合流的文字更新；前端 OSD 使用快照接口/浏览器叠加。
    if (!parseBoolean(fusionConfig == null ? null : fusionConfig.get("enabled"))) {
      updateFusionRegistry(instanceKey, deviceId, channelId, fusionConfig);
      return fusionConfig == null ? Collections.emptyMap() : fusionConfig;
    }
    if (!isBackendFusionMode(fusionConfig)) {
      updateFusionRegistry(instanceKey, deviceId, channelId, fusionConfig);
      return fusionConfig;
    }
    String status = stringValue(fusionConfig.get("taskStatus"));
    if (!"RUNNING".equalsIgnoreCase(status) && !"STARTING".equalsIgnoreCase(status)) {
      updateFusionRegistry(instanceKey, deviceId, channelId, fusionConfig);
      return fusionConfig;
    }
    String registryKey = buildFusionRegistryKey(instanceKey, deviceId, channelId);
    // schedule 和 shadow 分开限流，避免同一个通道高频传感器事件压垮 Redis Stream。
    if (!force && !shouldPublishFusionOverlay(registryKey, reason)) {
      return fusionConfig;
    }
    List<Map<String, Object>> shadowSummary = Collections.emptyList();
    String overlayText = stringValue(overlaySnapshot == null ? null : overlaySnapshot.get("overlayText"));
    if (StrUtil.isBlank(overlayText)) {
      shadowSummary = buildFusionShadowSummary(fusionConfig);
      overlayText = buildFusionOverlayText(channel, fusionConfig, shadowSummary);
    }
    long now = System.currentTimeMillis();
    // 下游 fusion-service/WVP 只需要通道定位、发布流标识、叠加文本和短 TTL。
    Map<String, String> record = new LinkedHashMap<>();
    putStreamField(record, "type", "overlay");
    putStreamField(record, "reason", StrUtil.blankToDefault(reason, "unknown"));
    putStreamField(record, "instanceKey", instanceKey);
    putStreamField(record, "deviceId", deviceId);
    putStreamField(record, "channelId", channelId);
    putStreamField(record, "taskId", stringValue(fusionConfig.get("taskId")));
    putStreamField(record, "publishApp", stringValue(fusionConfig.get("publishApp")));
    putStreamField(record, "publishStream", stringValue(fusionConfig.get("publishStream")));
    putStreamField(record, "overlayText", overlayText);
    putStreamField(record, "ts", String.valueOf(now));
    putStreamField(record, "ttlMs", String.valueOf(resolveFusionOverlayMessageTtlMs(fusionConfig)));
    if (event != null) {
      putStreamField(record, "iotId", event.getIotId());
      putStreamField(record, "productKey", event.getProductKey());
      putStreamField(record, "sensorDeviceId", event.getDeviceId());
      putStreamField(record, "shadowVersion", String.valueOf(event.getVersion()));
      putStreamField(record, "changedFields", String.join(",", event.getProperties().keySet()));
    }
    addFusionOverlayStreamRecord(record);
    Map<String, Object> snapshot =
        overlaySnapshot == null
            ? buildFrontendOsdOverlaySnapshotPayload(
                instanceKey,
                deviceId,
                channelId,
                overlayText,
                fusionConfig,
                shadowSummary,
                StrUtil.blankToDefault(reason, "stream"))
            : overlaySnapshot;
    cacheFrontendOsdOverlaySnapshot(
        instanceKey,
        deviceId,
        channelId,
        snapshot);
    log.debug(
        "[视频融合] 已投递overlay到Redis Stream key={}, instanceKey={}, deviceId={}, channelId={}, reason={}",
        fusionOverlayStreamKey,
        instanceKey,
        deviceId,
        channelId,
        reason);
    return fusionConfig;
  }

  private long resolveFusionOverlayMessageTtlMs(Map<String, Object> fusionConfig) {
    long configured =
        parseLongOrDefault(
            fusionConfig == null ? null : fusionConfig.get("messageTtlMs"),
            fusionOverlayStreamMessageTtlMs);
    return Math.max(500L, configured);
  }

  private void addFusionOverlayStreamRecord(Map<String, String> record) {
    if (fusionOverlayStreamMaxLen <= 0) {
      stringRedisTemplate.opsForStream().add(fusionOverlayStreamKey, record);
      return;
    }
    stringRedisTemplate.execute(
        (RedisCallback<RecordId>)
            connection -> {
              RedisSerializer<String> serializer = stringRedisTemplate.getStringSerializer();
              byte[] streamKeyBytes = serializeRedisString(serializer, fusionOverlayStreamKey);
              Map<byte[], byte[]> body = new LinkedHashMap<>();
              for (Map.Entry<String, String> entry : record.entrySet()) {
                body.put(
                    serializeRedisString(serializer, entry.getKey()),
                    serializeRedisString(serializer, entry.getValue()));
              }
              MapRecord<byte[], byte[], byte[]> rawRecord =
                  StreamRecords.rawBytes(body).withStreamKey(streamKeyBytes);
              RedisStreamCommands.XAddOptions options =
                  RedisStreamCommands.XAddOptions.maxlen(fusionOverlayStreamMaxLen)
                      .approximateTrimming(fusionOverlayStreamApproximateTrim);
              return connection.streamCommands().xAdd(rawRecord, options);
            });
  }

  private byte[] serializeRedisString(RedisSerializer<String> serializer, String value) {
    byte[] bytes = serializer == null ? null : serializer.serialize(value);
    return bytes == null ? String.valueOf(value).getBytes(StandardCharsets.UTF_8) : bytes;
  }

  private boolean shouldPublishFusionOverlay(String registryKey, String reason) {
    if (fusionOverlayStreamMinIntervalMs <= 0) {
      return true;
    }
    String throttleKey = registryKey + "|" + StrUtil.blankToDefault(reason, "unknown");
    long now = System.currentTimeMillis();
    Long last = fusionOverlayPublishAt.get(throttleKey);
    if (last != null && now - last < fusionOverlayStreamMinIntervalMs) {
      return false;
    }
    fusionOverlayPublishAt.put(throttleKey, now);
    return true;
  }

  private boolean isFusionShadowEventRelevant(
      Map<String, Object> fusionConfig, DeviceShadowUpdatedEvent event) {
    if (fusionConfig == null || event == null || StrUtil.isBlank(event.getIotId())) {
      return false;
    }
    List<Map<String, Object>> sensorBindings = normalizeSensorBindings(fusionConfig.get("sensorBindings"));
    if (!sensorBindings.isEmpty()) {
      for (Map<String, Object> binding : sensorBindings) {
        if (!event.getIotId().equals(stringValue(binding.get("iotId")))) {
          continue;
        }
        List<Map<String, Object>> fields = normalizeBindingFields(binding.get("fields"));
        if (fields.isEmpty() || event.getProperties().isEmpty()) {
          return true;
        }
        for (Map<String, Object> field : fields) {
          String fieldId = stringValue(field.get("id"));
          if (StrUtil.isNotBlank(fieldId) && event.getProperties().containsKey(fieldId)) {
            return true;
          }
        }
      }
      return false;
    }
    return normalizeStringList(fusionConfig.get("iotIds")).contains(event.getIotId());
  }

  private void putStreamField(Map<String, String> record, String key, String value) {
    if (StrUtil.isNotBlank(key) && value != null) {
      record.put(key, value);
    }
  }

  private Map<String, Object> buildFrontendOsdOverlaySnapshot(
      String instanceKey, String deviceId, String channelId, boolean checkAccess, boolean preferCache) {
    // 前端 OSD 快照缓存的是已经渲染好的文本和样式，适合播放器高频刷新读取。
    if (preferCache) {
      Map<String, Object> cached = readJsonMapValue(buildFrontendOsdSnapshotKey(instanceKey, deviceId, channelId));
      if (cached != null && !cached.isEmpty()) {
        return cached;
      }
    }

    if (checkAccess) {
      requireAccessibleInstance(instanceKey);
    } else if (getByInstanceKey(instanceKey) == null) {
      throw new BaseException("平台实例不存在：" + instanceKey);
    }
    VideoPlatformChannel channel = requireChannel(instanceKey, deviceId, channelId);
    Map<String, Object> fusionConfig = extractFusionConfig(channel, instanceKey, deviceId);
    return cacheFrontendOsdOverlaySnapshotForBinding(
        instanceKey, deviceId, channelId, channel, fusionConfig, "snapshot");
  }

  private Map<String, Object> cacheFrontendOsdOverlaySnapshotForBinding(
      String instanceKey,
      String deviceId,
      String channelId,
      VideoPlatformChannel channel,
      Map<String, Object> fusionConfig,
      String reason) {
    List<Map<String, Object>> shadowSummary = buildFusionShadowSummary(fusionConfig);
    String overlayText = buildFusionOverlayText(channel, fusionConfig, shadowSummary);
    Map<String, Object> snapshot =
        buildFrontendOsdOverlaySnapshotPayload(
            instanceKey,
            deviceId,
            channelId,
            overlayText,
            fusionConfig,
            shadowSummary,
            StrUtil.blankToDefault(reason, "snapshot"));
    cacheFrontendOsdOverlaySnapshot(instanceKey, deviceId, channelId, snapshot);
    return snapshot;
  }

  private Map<String, Object> buildFrontendOsdOverlaySnapshotPayload(
      String instanceKey,
      String deviceId,
      String channelId,
      String overlayText,
      Map<String, Object> fusionConfig,
      List<Map<String, Object>> shadowSummary,
      String reason) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("instanceKey", instanceKey);
    snapshot.put("deviceId", deviceId);
    snapshot.put("channelId", channelId);
    snapshot.put("overlayText", StrUtil.blankToDefault(overlayText, ""));
    snapshot.put("shadowSummary", shadowSummary == null ? Collections.emptyList() : shadowSummary);
    snapshot.put("template", fusionConfig == null ? "right_panel_v1" : fusionConfig.get("template"));
    snapshot.put("stylePreset", fusionConfig == null ? "panel_dark" : fusionConfig.get("stylePreset"));
    snapshot.put("style", fusionConfig == null ? createFusionDefaultStyleMap() : normalizeFusionStyle(fusionConfig.get("style")));
    snapshot.put("displayMode", fusionConfig == null ? "custom_template" : fusionConfig.get("displayMode"));
    snapshot.put("enabled", fusionConfig != null && parseBoolean(fusionConfig.get("enabled")));
    snapshot.put(
        "staleThresholdMs",
        fusionConfig == null ? 5000L : parseLongOrDefault(fusionConfig.get("staleThresholdMs"), 5000L));
    snapshot.put("reason", StrUtil.blankToDefault(reason, "snapshot"));
    snapshot.put("ts", System.currentTimeMillis());
    return snapshot;
  }

  private Map<String, Object> buildFrontendOsdConfig(Map<String, Object> fusionConfig) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("enabled", fusionConfig != null && parseBoolean(fusionConfig.get("enabled")));
    result.put("template", fusionConfig == null ? "right_panel_v1" : fusionConfig.get("template"));
    result.put("stylePreset", fusionConfig == null ? "panel_dark" : fusionConfig.get("stylePreset"));
    result.put("style", fusionConfig == null ? createFusionDefaultStyleMap() : normalizeFusionStyle(fusionConfig.get("style")));
    result.put("displayMode", fusionConfig == null ? "custom_template" : fusionConfig.get("displayMode"));
    result.put(
        "staleThresholdMs",
        fusionConfig == null ? 5000L : parseLongOrDefault(fusionConfig.get("staleThresholdMs"), 5000L));
    return result;
  }

  private Map<String, String> buildShareStreamUrls(
      VideoPlatformInstance instance,
      VideoPlatformChannel channel,
      Map<String, Object> shareRecord,
      Map<String, Object> fusionConfig) {
    String instanceKey = stringValue(shareRecord.get("instanceKey"));
    String deviceId = stringValue(shareRecord.get("deviceId"));
    String channelId = stringValue(shareRecord.get("channelId"));
    String streamType = normalizeShareStreamType(stringValue(shareRecord.get("streamType")));
    String preferredProtocol =
        StrUtil.blankToDefault(
            stringValue(shareRecord.get("preferProtocol")),
            String.valueOf(fusionConfig.getOrDefault("preferProtocol", "ws_flv")));
    Map<String, String> options = new LinkedHashMap<>();
    options.put("playMode", normalizeSharePlayMode(stringValue(shareRecord.get("playMode"))));
    options.put("preferProtocol", preferredProtocol);
    String playMode = resolvePlayMode(options);
    String fusionMode = resolveFusionMode(fusionConfig);
    // 分享页默认不启动后端融合任务；只有已有可用融合流且 token 要求融合时才返回融合流。
    boolean backendMode = isBackendFusionMode(fusionConfig);
    boolean tryFusion = shouldTryFusion(playMode) && backendMode;
    boolean requireFusion = shouldRequireFusion(playMode);
    boolean fusionEnabled = isFusionEnabled(fusionConfig);

    Map<String, String> fusionUrls =
        ensureFusionUrlsForPreview(
            instance, channel, instanceKey, deviceId, channelId, fusionConfig, tryFusion, false);
    boolean fusionReady = fusionEnabled && backendMode && !fusionUrls.isEmpty();
    if (tryFusion && fusionReady) {
      Map<String, String> result = new LinkedHashMap<>(fusionUrls);
      result.put("preferredUrl", pickPreferredUrl(fusionUrls, preferredProtocol));
      result.put("streamSource", "fusion");
      result.put("playMode", playMode);
      result.put("fusionMode", fusionMode);
      result.put("fusionEnabled", "true");
      result.put("fusionReady", "true");
      result.put("frontendOsdEnabled", "false");
      result.put("backendFusionEnabled", "true");
      return result;
    }
    if (requireFusion) {
      throw new BaseException(backendMode ? "融合流未就绪" : "当前通道未启用后端融合流");
    }
    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    if (adapter == null) {
      throw new BaseException("未找到视频平台适配器：" + instance.getPlatformType());
    }
    Map<String, Object> adapterOptions = new LinkedHashMap<>();
    adapterOptions.put("playMode", playMode);
    adapterOptions.put("preferProtocol", preferredProtocol);
    adapterOptions.putAll(extractChannelStreamConfig(channel));
    adapterOptions.putAll(extractChannelRuntimeConfig(channel));
    Map<String, String> result = adapter.getStreamUrl(instance, deviceId, channelId, streamType, adapterOptions);
    result = result == null ? new LinkedHashMap<>() : new LinkedHashMap<>(result);
    result.put("preferredUrl", pickPreferredUrl(result, preferredProtocol));
    result.put("streamSource", "origin");
    result.put("playMode", playMode);
    result.put("fusionMode", fusionMode);
    result.put("fusionEnabled", String.valueOf(fusionEnabled));
    result.put("fusionReady", String.valueOf(fusionReady));
    result.put("frontendOsdEnabled", String.valueOf(isFrontendOsdMode(fusionConfig)));
    result.put("backendFusionEnabled", String.valueOf(backendMode));
    return result;
  }

  private void cacheFrontendOsdOverlaySnapshot(
      String instanceKey, String deviceId, String channelId, Map<String, Object> snapshot) {
    long ttlMs = Math.max(1000L, videoFrontendOsdSnapshotTtlMs);
    writeJsonValue(buildFrontendOsdSnapshotKey(instanceKey, deviceId, channelId), snapshot, ttlMs, TimeUnit.MILLISECONDS);
  }

  private String buildFrontendOsdSnapshotKey(String instanceKey, String deviceId, String channelId) {
    return VIDEO_FRONTEND_OSD_SNAPSHOT_PREFIX
        + StrUtil.blankToDefault(instanceKey, "-")
        + ":"
        + StrUtil.blankToDefault(deviceId, "-")
        + ":"
        + StrUtil.blankToDefault(channelId, "-");
  }

  private String buildShareRedisKey(String token) {
    return VIDEO_SHARE_REDIS_PREFIX + StrUtil.blankToDefault(token, "");
  }

  private Map<String, Object> requireShareRecord(String token) {
    if (StrUtil.isBlank(token)) {
      throw new BaseException("分享链接无效");
    }
    Map<String, Object> record = readJsonMapValue(buildShareRedisKey(token));
    if (record == null || record.isEmpty()) {
      throw new BaseException("分享链接已过期或不存在");
    }
    if (parseBoolean(record.get("revoked"))) {
      throw new BaseException("分享链接已撤销");
    }
    long expiresAt = parseLongOrDefault(record.get("expiresAt"), 0L);
    if (expiresAt > 0 && expiresAt < System.currentTimeMillis()) {
      stringRedisTemplate.delete(buildShareRedisKey(token));
      throw new BaseException("分享链接已过期");
    }
    return record;
  }

  private void writeJsonValue(String key, Object value, long ttl, TimeUnit unit) {
    if (StrUtil.isBlank(key) || value == null) {
      return;
    }
    try {
      stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl, unit);
    } catch (Exception e) {
      log.warn("[视频融合] 写入Redis缓存失败 key={}, msg={}", key, e.getMessage());
    }
  }

  private void writeShareRecord(String key, Map<String, Object> record, long ttlSeconds) {
    try {
      stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(record), ttlSeconds, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new BaseException("创建视频分享失败，无法写入Redis：" + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> readJsonMapValue(String key) {
    if (StrUtil.isBlank(key)) {
      return null;
    }
    String raw = stringRedisTemplate.opsForValue().get(key);
    if (StrUtil.isBlank(raw)) {
      return null;
    }
    try {
      Object value = objectMapper.readValue(raw, Map.class);
      if (value instanceof Map) {
        return new LinkedHashMap<>((Map<String, Object>) value);
      }
    } catch (Exception e) {
      log.warn("[视频融合] 读取Redis缓存失败 key={}, msg={}", key, e.getMessage());
    }
    return null;
  }

  private long normalizeShareTtlSeconds(long ttlSeconds) {
    long min = Math.max(1L, videoShareMinTtlSeconds);
    long max = Math.max(min, videoShareMaxTtlSeconds);
    if (ttlSeconds <= 0) {
      ttlSeconds = videoShareDefaultTtlSeconds;
    }
    return Math.max(min, Math.min(ttlSeconds, max));
  }

  private long resolveShareOverlayEventIntervalMs() {
    return Math.max(300L, videoShareOverlayEventIntervalMs);
  }

  private String normalizeSharePlayMode(String value) {
    if ("fusion".equalsIgnoreCase(value) || "auto".equalsIgnoreCase(value)) {
      return value.trim().toLowerCase();
    }
    return "origin";
  }

  private String normalizeShareStreamType(String value) {
    return StrUtil.blankToDefault(value, "main");
  }

  private Boolean parseBooleanObject(Object value) {
    if (value == null) {
      return null;
    }
    return parseBoolean(value);
  }

  private Object firstNonNull(Object... values) {
    if (values == null) {
      return null;
    }
    for (Object value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private VideoPlatformChannel requireChannel(String instanceKey, String deviceId, String channelId) {
    cn.universal.admin.video.service.OpenApiVideoScope.assertChannelIfPresent(instanceKey, deviceId, channelId);
    if (StrUtil.isBlank(instanceKey) || StrUtil.isBlank(deviceId) || StrUtil.isBlank(channelId)) {
      throw new BaseException("通道信息不完整");
    }
    Example deviceEx = new Example(VideoPlatformDevice.class);
    deviceEx
        .createCriteria()
        .andEqualTo("instanceKey", instanceKey)
        .andEqualTo("deviceId", deviceId);
    List<VideoPlatformDevice> devices = deviceMapper.selectByExample(deviceEx);
    if (devices.isEmpty()) {
      throw new BaseException("设备不存在：" + deviceId);
    }
    VideoPlatformDevice device = devices.get(0);

    cn.universal.admin.video.service.OpenApiVideoScope.assertResourceTenantIfPresent(device.getTenantId());

    Example channelEx = new Example(VideoPlatformChannel.class);
    channelEx
        .createCriteria()
        .andEqualTo("instanceKey", instanceKey)
        .andEqualTo("deviceId", device.getId())
        .andEqualTo("channelId", channelId);
    List<VideoPlatformChannel> channels = channelMapper.selectByExample(channelEx);
    if (channels.isEmpty()) {
      throw new BaseException("通道不存在：" + channelId);
    }
    cn.universal.admin.video.service.OpenApiVideoScope.assertResourceTenantIfPresent(channels.get(0).getTenantId());
    return channels.get(0);
  }

  private void assertIsupDeviceEnabled(VideoPlatformInstance instance, VideoPlatformChannel channel) {
    if(!"hik_isup".equals(instance.getPlatformType()))return;
    VideoPlatformDevice device=deviceMapper.selectByPrimaryKey(channel.getDeviceId());
    if(!Integer.valueOf(1).equals(instance.getEnabled())||device==null||Integer.valueOf(0).equals(device.getEnabled())
        ||!java.util.Objects.equals(device.getTenantId(),instance.getTenantId()))throw new BaseException("ISUP实例或设备已停用、或无权访问");
  }

  private Map<String, Object> buildChannelSummary(VideoPlatformChannel channel) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", channel.getId());
    result.put("channelId", channel.getChannelId());
    result.put("channelName", channel.getChannelName());
    result.put("channelStatus", channel.getChannelStatus());
    result.put("channelType", channel.getChannelType());
    result.put("ptzType", channel.getPtzType());
    result.put("manufacturer", channel.getManufacturer());
    result.put("model", channel.getModel());
    result.put("address", channel.getAddress());
    result.put("ipAddress", channel.getIpAddress());
    result.put("port", channel.getPort());
    result.put("streamId", channel.getStreamId());
    return result;
  }

  private List<Map<String, Object>> buildFusionShadowSummary(Map<String, Object> fusionConfig) {
    List<Map<String, Object>> sensorBindings =
        normalizeSensorBindings(fusionConfig == null ? null : fusionConfig.get("sensorBindings"));
    if (!sensorBindings.isEmpty()) {
      List<String> bindingIotIds = new ArrayList<>();
      for (Map<String, Object> binding : sensorBindings) {
        String iotId = stringValue(binding.get("iotId"));
        if (StrUtil.isNotBlank(iotId)) {
          bindingIotIds.add(iotId);
        }
      }
      if (bindingIotIds.isEmpty()) {
        return Collections.emptyList();
      }
      List<IoTDeviceShadow> shadows = ioTDeviceService.getDeviceShadows(bindingIotIds);
      Map<String, IoTDeviceShadow> shadowMap = new LinkedHashMap<>();
      for (IoTDeviceShadow shadow : shadows) {
        if (shadow != null && StrUtil.isNotBlank(shadow.getIotId())) {
          shadowMap.put(shadow.getIotId(), shadow);
        }
      }
      List<Map<String, Object>> result = new ArrayList<>();
      for (Map<String, Object> binding : sensorBindings) {
        String iotId = stringValue(binding.get("iotId"));
        if (StrUtil.isBlank(iotId)) {
          continue;
        }
        IoTDeviceShadow shadow = shadowMap.get(iotId);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("iotId", iotId);
        item.put("deviceName", stringValue(binding.get("deviceName")));
        item.put("alias", stringValue(binding.get("alias")));
        item.put("productKey", stringValue(binding.get("productKey")));
        if (shadow != null) {
          item.put("lastTime", shadow.getLastTime());
          item.put("version", shadow.getVersion());
          item.put("metadata", shadow.getMetadata());
          item.put("onlineTime", shadow.getOnlineTime());
        }
        List<Map<String, Object>> fields = normalizeBindingFields(binding.get("fields"));
        List<Map<String, Object>> fieldValues = new ArrayList<>();
        JsonNode reportedNode = extractReportedNode(shadow == null ? null : shadow.getMetadata());
        for (Map<String, Object> field : fields) {
          String fieldId = stringValue(field.get("id"));
          if (StrUtil.isBlank(fieldId)) {
            continue;
          }
          Map<String, Object> valueItem = new LinkedHashMap<>(field);
          valueItem.put("value", resolveFieldDisplayValue(reportedNode, field));
          fieldValues.add(valueItem);
        }
        item.put("fields", fieldValues);
        result.add(item);
      }
      return result;
    }

    Object iotIdsObj = fusionConfig == null ? null : fusionConfig.get("iotIds");
    List<String> iotIds = normalizeStringList(iotIdsObj);
    if (iotIds.isEmpty()) {
      return Collections.emptyList();
    }
    List<IoTDeviceShadow> shadows = ioTDeviceService.getDeviceShadows(iotIds);
    List<Map<String, Object>> result = new ArrayList<>();
    for (IoTDeviceShadow shadow : shadows) {
      if (shadow == null || StrUtil.isBlank(shadow.getIotId())) {
        continue;
      }
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("iotId", shadow.getIotId());
      item.put("lastTime", shadow.getLastTime());
      item.put("version", shadow.getVersion());
      item.put("metadata", shadow.getMetadata());
      item.put("onlineTime", shadow.getOnlineTime());
      result.add(item);
    }
    return result;
  }

  private VideoPlatformInstance resolveFusionInstance(String instanceKey, boolean checkAccess) {
    VideoPlatformInstance instance =
        checkAccess ? requireAccessibleInstance(instanceKey) : getByInstanceKey(instanceKey);
    if (instance == null) {
      throw new BaseException("平台实例不存在：" + instanceKey);
    }
    if (!"wvp".equalsIgnoreCase(instance.getPlatformType())) {
      throw new BaseException("当前仅支持 WVP 平台融合任务控制");
    }
    return instance;
  }

  private VideoPlatformAdapter requireFusionAdapter(VideoPlatformInstance instance) {
    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    if (adapter != null) {
      return adapter;
    }
    throw new BaseException("未找到平台融合适配器: " + instance.getPlatformType());
  }

  private Map<String, Object> buildFusionTaskOptions(
      VideoPlatformInstance instance,
      VideoPlatformChannel channel,
      String instanceKey,
      String deviceId,
      Map<String, Object> fusionConfig) {
    Map<String, Object> options = new LinkedHashMap<>();
    options.put("instanceKey", instanceKey);
    options.put("publishApp", fusionConfig.get("publishApp"));
    options.put("publishStream", fusionConfig.get("publishStream"));
    options.put("template", fusionConfig.get("template"));
    options.put("stylePreset", fusionConfig.get("stylePreset"));
    options.put("style", normalizeFusionStyle(fusionConfig.get("style")));
    options.put("overlayText", buildFusionOverlayText(channel, fusionConfig));
    return options;
  }

  private String buildFusionOverlayText(
      VideoPlatformChannel channel, Map<String, Object> fusionConfig) {
    List<Map<String, Object>> shadowSummary = buildFusionShadowSummary(fusionConfig);
    return buildFusionOverlayText(channel, fusionConfig, shadowSummary);
  }

  private String buildFusionOverlayText(
      VideoPlatformChannel channel,
      Map<String, Object> fusionConfig,
      List<Map<String, Object>> shadowSummary) {
    String displayMode =
        StrUtil.blankToDefault(
            stringValue(fusionConfig == null ? null : fusionConfig.get("displayMode")),
            "custom_template");
    if ("custom_template".equalsIgnoreCase(displayMode)
        || StrUtil.isNotBlank(stringValue(fusionConfig == null ? null : fusionConfig.get("overlayTemplate")))) {
      return buildCustomTemplateOverlayText(channel, fusionConfig, shadowSummary);
    }
    return buildAutoSummaryOverlayText(channel, fusionConfig, shadowSummary);
  }

  private String buildCustomTemplateOverlayText(
      VideoPlatformChannel channel,
      Map<String, Object> fusionConfig,
      List<Map<String, Object>> shadowSummary) {
    String template = stringValue(fusionConfig == null ? null : fusionConfig.get("overlayTemplate"));
    if (StrUtil.isBlank(template)) {
      template = buildDefaultOverlayTemplate(channel, shadowSummary);
    }
    Map<String, String> valueMap = buildFusionTemplateValueMap(channel, shadowSummary);
    return trimOverlayLines(renderOverlayTemplate(template, valueMap), 6, 72);
  }

  private String buildAutoSummaryOverlayText(
      VideoPlatformChannel channel,
      Map<String, Object> fusionConfig,
      List<Map<String, Object>> shadowSummary) {
    List<String> lines = new ArrayList<>();
    String notes = stringValue(fusionConfig == null ? null : fusionConfig.get("notes"));
    String title =
        StrUtil.blankToDefault(
            notes,
            StrUtil.blankToDefault(
                channel == null ? null : channel.getChannelName(),
                channel == null ? null : channel.getChannelId()));
    if (StrUtil.isNotBlank(title)) {
      lines.add(truncateText(title, 48));
    }
    if (shadowSummary.isEmpty()) {
      lines.add("未获取到传感器热数据");
    } else {
      int maxLines = 5;
      for (Map<String, Object> shadow : shadowSummary) {
        if (lines.size() > maxLines) {
          break;
        }
        String line = buildShadowSummaryLine(shadow, parseLongOrDefault(fusionConfig.get("staleThresholdMs"), 5000L));
        if (StrUtil.isNotBlank(line)) {
          lines.add(line);
        }
      }
    }
    lines.add(
        "SYNC " + formatFusionNow());
    return trimOverlayLines(String.join("\n", lines), 6, 72);
  }

  private String buildDefaultOverlayTemplate(
      VideoPlatformChannel channel, List<Map<String, Object>> shadowSummary) {
    List<String> lines = new ArrayList<>();
    String title =
        StrUtil.blankToDefault(
            channel == null ? null : channel.getChannelName(),
            channel == null ? null : channel.getChannelId());
    if (StrUtil.isNotBlank(title)) {
      lines.add(title);
    }
    for (Map<String, Object> shadow : shadowSummary) {
      String alias =
          StrUtil.blankToDefault(
              stringValue(shadow.get("alias")),
              sanitizePlaceholderName(
                  StrUtil.blankToDefault(
                      stringValue(shadow.get("deviceName")), stringValue(shadow.get("iotId")))));
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> fields = (List<Map<String, Object>>) shadow.get("fields");
      if (fields == null || fields.isEmpty()) {
        continue;
      }
      for (Map<String, Object> field : fields) {
        if (lines.size() >= 5) {
          break;
        }
        String fieldId = stringValue(field.get("id"));
        String fieldName = StrUtil.blankToDefault(stringValue(field.get("name")), fieldId);
        lines.add(fieldName + "：${" + alias + "." + fieldId + "}");
      }
      if (lines.size() >= 5) {
        break;
      }
    }
    if (lines.size() <= 1) {
      lines.add("暂无传感器热数据");
    }
    lines.add("SYNC ${now}");
    return String.join("\n", lines);
  }

  private Map<String, String> buildFusionTemplateValueMap(
      VideoPlatformChannel channel, List<Map<String, Object>> shadowSummary) {
    Map<String, String> values = new LinkedHashMap<>();
    values.put("channelName", stringValue(channel == null ? null : channel.getChannelName()));
    values.put("channelId", stringValue(channel == null ? null : channel.getChannelId()));
    values.put("channelStatus", stringValue(channel == null ? null : channel.getChannelStatus()));
    values.put(
        "now",
        formatFusionNow());
    for (Map<String, Object> shadow : shadowSummary) {
      String alias =
          StrUtil.blankToDefault(
              stringValue(shadow.get("alias")),
              sanitizePlaceholderName(
                  StrUtil.blankToDefault(
                      stringValue(shadow.get("deviceName")), stringValue(shadow.get("iotId")))));
      values.put(alias + ".deviceName", stringValue(shadow.get("deviceName")));
      values.put(alias + ".iotId", stringValue(shadow.get("iotId")));
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> fields = (List<Map<String, Object>>) shadow.get("fields");
      if (fields == null) {
        continue;
      }
      for (Map<String, Object> field : fields) {
        String fieldId = stringValue(field.get("id"));
        if (StrUtil.isBlank(fieldId)) {
          continue;
        }
        values.put(alias + "." + fieldId, stringValue(field.get("value")));
      }
    }
    return values;
  }

  private String buildShadowSummaryLine(Map<String, Object> shadow, long staleThresholdMs) {
    if (shadow == null) {
      return null;
    }
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> fields = (List<Map<String, Object>>) shadow.get("fields");
    if (fields != null) {
      List<String> segments = new ArrayList<>();
      for (Map<String, Object> field : fields) {
        String fieldName = StrUtil.blankToDefault(stringValue(field.get("name")), stringValue(field.get("id")));
        String value = StrUtil.blankToDefault(stringValue(field.get("value")), "--");
        segments.add(fieldName + "=" + value);
        if (segments.size() >= 3) {
          break;
        }
      }
      String prefix =
          StrUtil.blankToDefault(
              stringValue(shadow.get("deviceName")),
              StrUtil.blankToDefault(stringValue(shadow.get("alias")), stringValue(shadow.get("iotId"))));
      if (isShadowStale(shadow.get("lastTime"), staleThresholdMs)) {
        prefix = prefix + "[STALE]";
      }
      if (segments.isEmpty()) {
        return truncateText(prefix + " no-data", 90);
      }
      return truncateText(prefix + " " + String.join(" | ", segments), 90);
    }
    String iotId = stringValue(shadow.get("iotId"));
    JsonNode reportedNode = extractReportedNode(stringValue(shadow.get("metadata")));
    List<String> segments = new ArrayList<>();
    collectReportedPairs(reportedNode, "", segments, 4);
    String prefix = StrUtil.blankToDefault(iotId, "sensor");
    if (isShadowStale(shadow.get("lastTime"), staleThresholdMs)) {
      prefix = prefix + "[STALE]";
    }
    if (segments.isEmpty()) {
      return truncateText(prefix + " no-data", 90);
    }
    return truncateText(prefix + " " + String.join(" | ", segments), 90);
  }

  private List<Map<String, Object>> buildFusionPlaceholderTokens(
      VideoPlatformChannel channel, Map<String, Object> fusionConfig) {
    List<Map<String, Object>> result = new ArrayList<>();
    result.add(buildPlaceholderToken("${channelName}", "通道名称"));
    result.add(buildPlaceholderToken("${channelId}", "通道编号"));
    result.add(buildPlaceholderToken("${channelStatus}", "通道状态"));
    result.add(buildPlaceholderToken("${now}", "当前时间"));
    for (Map<String, Object> binding : normalizeSensorBindings(fusionConfig == null ? null : fusionConfig.get("sensorBindings"))) {
      String alias =
          StrUtil.blankToDefault(
              stringValue(binding.get("alias")),
              sanitizePlaceholderName(
                  StrUtil.blankToDefault(
                      stringValue(binding.get("deviceName")), stringValue(binding.get("iotId")))));
      if (StrUtil.isBlank(alias)) {
        continue;
      }
      result.add(buildPlaceholderToken("${" + alias + ".deviceName}", alias + " 设备名称"));
      for (Map<String, Object> field : normalizeBindingFields(binding.get("fields"))) {
        String fieldId = stringValue(field.get("id"));
        if (StrUtil.isBlank(fieldId)) {
          continue;
        }
        String fieldName = StrUtil.blankToDefault(stringValue(field.get("name")), fieldId);
        result.add(buildPlaceholderToken("${" + alias + "." + fieldId + "}", alias + " / " + fieldName));
      }
    }
    return result;
  }

  private Map<String, Object> buildPlaceholderToken(String token, String label) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("token", token);
    item.put("label", label);
    return item;
  }

  private int countFusionBindingItems(Map<String, Object> fusionConfig) {
    List<Map<String, Object>> bindings =
        normalizeSensorBindings(fusionConfig == null ? null : fusionConfig.get("sensorBindings"));
    if (!bindings.isEmpty()) {
      return bindings.size();
    }
    return normalizeStringList(fusionConfig == null ? null : fusionConfig.get("iotIds")).size();
  }

  private String renderOverlayTemplate(String template, Map<String, String> values) {
    if (StrUtil.isBlank(template)) {
      return "";
    }
    java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile("\\$\\{([A-Za-z0-9_.-]+)}").matcher(template);
    StringBuffer buffer = new StringBuffer();
    while (matcher.find()) {
      String key = matcher.group(1);
      String replacement = StrUtil.blankToDefault(values == null ? null : values.get(key), "--");
      matcher.appendReplacement(buffer, java.util.regex.Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(buffer);
    return buffer.toString();
  }

  private String trimOverlayLines(String value, int maxLines, int maxLineLength) {
    if (StrUtil.isBlank(value)) {
      return "";
    }
    String[] rawLines = value.replace("\r", "").split("\n");
    List<String> lines = new ArrayList<>();
    for (String rawLine : rawLines) {
      if (lines.size() >= Math.max(maxLines, 1)) {
        break;
      }
      String line = truncateText(StrUtil.blankToDefault(rawLine, "").trim(), maxLineLength);
      if (StrUtil.isNotBlank(line)) {
        lines.add(line);
      }
    }
    return String.join("\n", lines);
  }

  private String sanitizePlaceholderName(String value) {
    if (StrUtil.isBlank(value)) {
      return "";
    }
    String normalized = value.replaceAll("[^A-Za-z0-9_]", "_");
    normalized = normalized.replaceAll("_+", "_");
    if (normalized.matches("^[0-9].*")) {
      normalized = "sensor_" + normalized;
    }
    return normalized;
  }

  private List<Map<String, Object>> normalizeSensorBindings(Object value) {
    List<Map<String, Object>> result = new ArrayList<>();
    if (!(value instanceof List)) {
      return result;
    }
    for (Object item : (List<?>) value) {
      if (!(item instanceof Map)) {
        continue;
      }
      Map<?, ?> source = (Map<?, ?>) item;
      Map<String, Object> binding = new LinkedHashMap<>();
      binding.put("iotId", stringValue(source.get("iotId")));
      binding.put("productKey", stringValue(source.get("productKey")));
      binding.put("deviceName", stringValue(source.get("deviceName")));
      String alias = stringValue(source.get("alias"));
      binding.put("alias", StrUtil.blankToDefault(alias, sanitizePlaceholderName(stringValue(source.get("deviceName")))));
      binding.put("fields", normalizeBindingFields(source.get("fields")));
      if (StrUtil.isBlank(stringValue(binding.get("iotId")))) {
        continue;
      }
      result.add(binding);
    }
    return result;
  }

  private List<Map<String, Object>> normalizeBindingFields(Object value) {
    List<Map<String, Object>> result = new ArrayList<>();
    if (!(value instanceof List)) {
      return result;
    }
    for (Object item : (List<?>) value) {
      if (!(item instanceof Map)) {
        continue;
      }
      Map<?, ?> source = (Map<?, ?>) item;
      String fieldId = stringValue(source.get("id"));
      if (StrUtil.isBlank(fieldId)) {
        continue;
      }
      Map<String, Object> field = new LinkedHashMap<>();
      field.put("id", fieldId);
      field.put("name", StrUtil.blankToDefault(stringValue(source.get("name")), fieldId));
      field.put("unit", stringValue(source.get("unit")));
      field.put("maxLen", parseLongOrDefault(source.get("maxLen"), 12L));
      result.add(field);
    }
    return result;
  }

  private Map<String, Object> normalizeFusionStyle(Object value) {
    Map<String, Object> result = new LinkedHashMap<>();
    if (!(value instanceof Map)) {
      return result;
    }
    Map<?, ?> source = (Map<?, ?>) value;
    putFusionStyleValue(result, "fontColor", source.get("fontColor"));
    putFusionStyleValue(result, "boxColor", source.get("boxColor"));
    putFusionStyleValue(result, "borderColor", source.get("borderColor"));
    putFusionStyleValue(result, "shadowColor", source.get("shadowColor"));
    putFusionStyleBoolean(result, "boxEnabled", source.get("boxEnabled"));
    putFusionStyleLong(result, "fontSize", source.get("fontSize"), 16, 160);
    putFusionStyleLong(result, "boxBorderW", source.get("boxBorderW"), 0, 48);
    putFusionStyleLong(result, "borderW", source.get("borderW"), 0, 8);
    putFusionStyleLong(result, "lineSpacing", source.get("lineSpacing"), 0, 36);
    putFusionStyleLong(result, "shadowX", source.get("shadowX"), 0, 16);
    putFusionStyleLong(result, "shadowY", source.get("shadowY"), 0, 16);
    putFusionSignedStyleLong(result, "offsetX", source.get("offsetX"), -160, 160);
    putFusionSignedStyleLong(result, "offsetY", source.get("offsetY"), -160, 160);
    return result;
  }

  private Map<String, Object> createFusionDefaultStyleMap() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("fontColor", "#F8FAFC");
    result.put("borderColor", "0x020617F0");
    result.put("borderW", 2);
    result.put("shadowColor", "0x020617CC");
    result.put("shadowX", 2);
    result.put("shadowY", 2);
    result.put("boxEnabled", false);
    result.put("boxColor", "0x08121C66");
    result.put("fontSize", 30);
    result.put("boxBorderW", 10);
    result.put("lineSpacing", 6);
    result.put("offsetX", 0);
    result.put("offsetY", 0);
    return result;
  }

  private void putFusionStyleValue(Map<String, Object> target, String key, Object value) {
    String normalized = stringValue(value);
    if (StrUtil.isNotBlank(normalized)) {
      target.put(key, normalized);
    }
  }

  private void putFusionStyleBoolean(Map<String, Object> target, String key, Object value) {
    if (value == null) {
      return;
    }
    target.put(key, parseBoolean(value));
  }

  private void putFusionStyleLong(
      Map<String, Object> target, String key, Object value, long min, long max) {
    long parsed = parseLongOrDefault(value, -1L);
    if (parsed < 0) {
      return;
    }
    target.put(key, Math.max(min, Math.min(max, parsed)));
  }

  private void putFusionSignedStyleLong(
      Map<String, Object> target, String key, Object value, long min, long max) {
    if (value == null) {
      return;
    }
    long parsed = parseLongOrDefault(value, Long.MIN_VALUE);
    if (parsed == Long.MIN_VALUE) {
      return;
    }
    target.put(key, Math.max(min, Math.min(max, parsed)));
  }

  private String formatFusionNow() {
    return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        .format(new Date(System.currentTimeMillis()));
  }

  private String resolveFieldDisplayValue(JsonNode reportedNode, Map<String, Object> field) {
    String fieldId = stringValue(field.get("id"));
    JsonNode valueNode = resolveReportedValueNode(reportedNode, fieldId);
    String value = resolveNodeDisplayValue(valueNode);
    if (StrUtil.isBlank(value)) {
      return "--";
    }
    value = truncateText(value, (int) parseLongOrDefault(field.get("maxLen"), 12L));
    String unit = stringValue(field.get("unit"));
    return StrUtil.isNotBlank(unit) ? value + unit : value;
  }

  private JsonNode resolveReportedValueNode(JsonNode reportedNode, String fieldPath) {
    if (reportedNode == null || StrUtil.isBlank(fieldPath)) {
      return null;
    }
    JsonNode current = reportedNode;
    for (String segment : fieldPath.split("\\.")) {
      if (current == null || current.isMissingNode() || current.isNull()) {
        return null;
      }
      current = current.get(segment);
    }
    return current;
  }

  private String resolveNodeDisplayValue(JsonNode node) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      return "";
    }
    if (node.isTextual()) {
      return node.asText();
    }
    if (node.isNumber() || node.isBoolean()) {
      return node.asText();
    }
    return node.toString();
  }

  private JsonNode extractReportedNode(String metadata) {
    if (StrUtil.isBlank(metadata)) {
      return null;
    }
    try {
      JsonNode root = objectMapper.readTree(metadata);
      JsonNode stateNode = root.path("state");
      JsonNode reportedNode = stateNode.path("reported");
      return reportedNode.isMissingNode() || reportedNode.isNull() ? null : reportedNode;
    } catch (Exception e) {
      return null;
    }
  }

  private void collectReportedPairs(
      JsonNode node, String prefix, List<String> segments, int maxSegments) {
    if (node == null || segments.size() >= maxSegments) {
      return;
    }
    if (node.isValueNode()) {
      String key = StrUtil.blankToDefault(prefix, "value");
      String value = node.isTextual() ? node.asText() : node.toString();
      if (StrUtil.isNotBlank(value)) {
        segments.add(key + "=" + value);
      }
      return;
    }
    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    while (fields.hasNext() && segments.size() < maxSegments) {
      Map.Entry<String, JsonNode> entry = fields.next();
      String nextPrefix =
          StrUtil.isBlank(prefix) ? entry.getKey() : prefix + "." + entry.getKey();
      collectReportedPairs(entry.getValue(), nextPrefix, segments, maxSegments);
    }
  }

  private boolean isShadowStale(Object lastTimeValue, long staleThresholdMs) {
    long lastTime = parseTimeMillis(lastTimeValue);
    return lastTime > 0
        && staleThresholdMs > 0
        && System.currentTimeMillis() - lastTime > staleThresholdMs;
  }

  private long parseTimeMillis(Object value) {
    if (value == null) {
      return 0L;
    }
    if (value instanceof Date) {
      return ((Date) value).getTime();
    }
    return parseLongOrDefault(value, 0L);
  }

  private String truncateText(String value, int maxLength) {
    if (StrUtil.isBlank(value) || maxLength <= 0 || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength - 3) + "...";
  }

  private void mergeFusionTaskRuntime(
      Map<String, Object> fusionConfig, Map<String, Object> taskData) {
    if (fusionConfig == null) {
      return;
    }
    Map<String, String> publishUrls = normalizeStringMap(taskData == null ? null : taskData.get("publishUrls"));
    String status = StrUtil.blankToDefault(stringValue(taskData == null ? null : taskData.get("status")), "STOPPED");
    fusionConfig.put("taskId", stringValue(taskData == null ? null : taskData.get("taskId")));
    fusionConfig.put("taskStatus", status.toUpperCase());
    fusionConfig.put("taskMessage", stringValue(taskData == null ? null : taskData.get("message")));
    fusionConfig.put("overlayText", stringValue(taskData == null ? null : taskData.get("overlayText")));
    fusionConfig.put("taskStartedAt", parseLongOrDefault(taskData == null ? null : taskData.get("startedAt"), 0L));
    fusionConfig.put("taskUpdatedAt", parseLongOrDefault(taskData == null ? null : taskData.get("updatedAt"), 0L));
    fusionConfig.put("taskStoppedAt", parseLongOrDefault(taskData == null ? null : taskData.get("stoppedAt"), 0L));
    fusionConfig.put("taskExitCode", parseLongOrDefault(taskData == null ? null : taskData.get("exitCode"), 0L));
    fusionConfig.put("lastSyncAt", System.currentTimeMillis());
    if (!publishUrls.isEmpty()) {
      fusionConfig.put("publishUrls", publishUrls);
    } else if (!"RUNNING".equalsIgnoreCase(status) && !"STARTING".equalsIgnoreCase(status)) {
      fusionConfig.put("publishUrls", new LinkedHashMap<>());
    }
  }

  private Map<String, String> normalizeStringMap(Object value) {
    Map<String, String> result = new LinkedHashMap<>();
    if (!(value instanceof Map)) {
      return result;
    }
    Map<?, ?> source = (Map<?, ?>) value;
    for (Map.Entry<?, ?> entry : source.entrySet()) {
      if (entry.getKey() == null || entry.getValue() == null) {
        continue;
      }
      String key = String.valueOf(entry.getKey()).trim();
      String itemValue = String.valueOf(entry.getValue()).trim();
      if (StrUtil.isNotBlank(key) && StrUtil.isNotBlank(itemValue)) {
        result.put(key, itemValue);
      }
    }
    return result;
  }

  private void updateFusionRegistry(
      String instanceKey, String deviceId, String channelId, Map<String, Object> fusionConfig) {
    String registryKey = buildFusionRegistryKey(instanceKey, deviceId, channelId);
    removeFusionSensorIndex(registryKey);
    removeFrontendOsdSensorIndex(registryKey);
    String status = stringValue(fusionConfig == null ? null : fusionConfig.get("taskStatus"));
    boolean frontendMode = isFrontendOsdMode(fusionConfig);
    boolean backendMode = isBackendFusionMode(fusionConfig);
    boolean active = "RUNNING".equalsIgnoreCase(status) || "STARTING".equalsIgnoreCase(status);
    FusionBinding binding = new FusionBinding(instanceKey, deviceId, channelId);
    if (frontendMode) {
      fusionFrontendOsdRegistry.put(registryKey, binding);
      addFrontendOsdSensorIndex(registryKey, fusionConfig);
    } else {
      fusionFrontendOsdRegistry.remove(registryKey);
    }
    if (backendMode && active) {
      fusionTaskRegistry.put(registryKey, binding);
      addFusionSensorIndex(registryKey, fusionConfig);
    } else {
      fusionTaskRegistry.remove(registryKey);
    }
  }

  private void addFrontendOsdSensorIndex(String registryKey, Map<String, Object> fusionConfig) {
    Set<String> sensorIds = resolveFusionSensorIotIds(fusionConfig);
    if (sensorIds.isEmpty()) {
      return;
    }
    fusionFrontendOsdRegistrySensorIds.put(registryKey, sensorIds);
    for (String iotId : sensorIds) {
      fusionFrontendOsdSensorBindingIndex
          .computeIfAbsent(iotId, key -> ConcurrentHashMap.newKeySet())
          .add(registryKey);
    }
  }

  private void removeFrontendOsdSensorIndex(String registryKey) {
    Set<String> previousSensorIds = fusionFrontendOsdRegistrySensorIds.remove(registryKey);
    if (previousSensorIds == null || previousSensorIds.isEmpty()) {
      return;
    }
    for (String iotId : previousSensorIds) {
      fusionFrontendOsdSensorBindingIndex.computeIfPresent(
          iotId,
          (key, registryKeys) -> {
            registryKeys.remove(registryKey);
            return registryKeys.isEmpty() ? null : registryKeys;
          });
    }
  }

  private void addFusionSensorIndex(String registryKey, Map<String, Object> fusionConfig) {
    Set<String> sensorIds = resolveFusionSensorIotIds(fusionConfig);
    if (sensorIds.isEmpty()) {
      return;
    }
    fusionRegistrySensorIds.put(registryKey, sensorIds);
    for (String iotId : sensorIds) {
      fusionSensorBindingIndex
          .computeIfAbsent(iotId, key -> ConcurrentHashMap.newKeySet())
          .add(registryKey);
    }
  }

  private void removeFusionSensorIndex(String registryKey) {
    Set<String> previousSensorIds = fusionRegistrySensorIds.remove(registryKey);
    if (previousSensorIds == null || previousSensorIds.isEmpty()) {
      return;
    }
    for (String iotId : previousSensorIds) {
      fusionSensorBindingIndex.computeIfPresent(
          iotId,
          (key, registryKeys) -> {
            registryKeys.remove(registryKey);
            return registryKeys.isEmpty() ? null : registryKeys;
          });
    }
  }

  private Set<String> resolveFusionSensorIotIds(Map<String, Object> fusionConfig) {
    Set<String> result = new LinkedHashSet<>();
    for (Map<String, Object> binding :
        normalizeSensorBindings(fusionConfig == null ? null : fusionConfig.get("sensorBindings"))) {
      String iotId = stringValue(binding.get("iotId"));
      if (StrUtil.isNotBlank(iotId)) {
        result.add(iotId);
      }
    }
    if (!result.isEmpty()) {
      return result;
    }
    result.addAll(normalizeStringList(fusionConfig == null ? null : fusionConfig.get("iotIds")));
    return result;
  }

  private String buildFusionRegistryKey(String instanceKey, String deviceId, String channelId) {
    return instanceKey + "|" + deviceId + "|" + channelId;
  }

  private void bootstrapFusionRegistryIfNeeded() {
    if (!fusionRegistryBootstrapped.compareAndSet(false, true)) {
      return;
    }
    Example instanceEx = new Example(VideoPlatformInstance.class);
    instanceEx.createCriteria().andEqualTo("platformType", "wvp");
    List<VideoPlatformInstance> instances = instanceMapper.selectByExample(instanceEx);
    for (VideoPlatformInstance instance : instances) {
      if (instance == null || StrUtil.isBlank(instance.getInstanceKey())) {
        continue;
      }
      Example deviceEx = new Example(VideoPlatformDevice.class);
      deviceEx.createCriteria().andEqualTo("instanceKey", instance.getInstanceKey());
      List<VideoPlatformDevice> devices = deviceMapper.selectByExample(deviceEx);
      if (devices.isEmpty()) {
        continue;
      }
      Map<Long, String> deviceIdMap = new LinkedHashMap<>();
      List<Long> devicePkList = new ArrayList<>();
      for (VideoPlatformDevice device : devices) {
        if (device.getId() != null) {
          devicePkList.add(device.getId());
          deviceIdMap.put(device.getId(), device.getDeviceId());
        }
      }
      if (devicePkList.isEmpty()) {
        continue;
      }
      Example channelEx = new Example(VideoPlatformChannel.class);
      channelEx
          .createCriteria()
          .andEqualTo("instanceKey", instance.getInstanceKey())
          .andIn("deviceId", devicePkList);
      List<VideoPlatformChannel> channels = channelMapper.selectByExample(channelEx);
      for (VideoPlatformChannel channel : channels) {
        String deviceId = deviceIdMap.get(channel.getDeviceId());
        if (StrUtil.isBlank(deviceId)) {
          continue;
        }
        Map<String, Object> fusionConfig =
            extractFusionConfig(channel, instance.getInstanceKey(), deviceId);
        updateFusionRegistry(instance.getInstanceKey(), deviceId, channel.getChannelId(), fusionConfig);
      }
    }
  }

  @Transactional
  protected Map<String, Object> refreshChannelFusionTaskInternal(
      String instanceKey,
      String deviceId,
      String channelId,
      boolean checkAccess,
      boolean silent) {
    try {
      VideoPlatformInstance instance = resolveFusionInstance(instanceKey, checkAccess);
      VideoPlatformChannel channel = requireChannel(instanceKey, deviceId, channelId);
      Map<String, Object> fusionConfig = extractFusionConfig(channel, instanceKey, deviceId);
      if (!parseBoolean(fusionConfig.get("enabled"))) {
        updateFusionRegistry(instanceKey, deviceId, channelId, fusionConfig);
        return fusionConfig;
      }
      if (!isBackendFusionMode(fusionConfig)) {
        updateFusionRegistry(instanceKey, deviceId, channelId, fusionConfig);
        return fusionConfig;
      }
      Map<String, Object> taskData =
          requireFusionAdapter(instance)
              .refreshFusionTask(
                  instance,
                  deviceId,
                  channelId,
                  buildFusionTaskOptions(instance, channel, instanceKey, deviceId, fusionConfig));
      mergeFusionTaskRuntime(fusionConfig, taskData);
      saveFusionConfig(channel, fusionConfig);
      updateFusionRegistry(instanceKey, deviceId, channelId, fusionConfig);
      return fusionConfig;
    } catch (Exception e) {
      if (!silent) {
        throw e;
      }
      log.warn(
          "[视频融合] 后台刷新失败 instanceKey={}, deviceId={}, channelId={}, msg={}",
          instanceKey,
          deviceId,
          channelId,
          e.getMessage());
      return Collections.emptyMap();
    }
  }

  private Map<String, Object> startChannelFusionTaskInternal(
      VideoPlatformInstance instance,
      VideoPlatformChannel channel,
      String instanceKey,
      String deviceId,
      String channelId,
      Map<String, Object> fusionConfig) {
    if (!parseBoolean(fusionConfig.get("enabled"))) {
      throw new BaseException("请先启用融合配置");
    }
    if (!isBackendFusionMode(fusionConfig)) {
      throw new BaseException("当前通道启用的是前端OSD，未启用后端融合流");
    }
    // 到这里才真正调用 WVP/ZLM 启动后端融合任务，前面的所有预览/配置路径都不应绕过此校验。
    Map<String, Object> taskData =
        requireFusionAdapter(instance)
            .startFusionTask(
                instance,
                deviceId,
                channelId,
                buildFusionTaskOptions(instance, channel, instanceKey, deviceId, fusionConfig));
    mergeFusionTaskRuntime(fusionConfig, taskData);
    saveFusionConfig(channel, fusionConfig);
    updateFusionRegistry(instanceKey, deviceId, channelId, fusionConfig);
    return fusionConfig;
  }

  private Map<String, Object> buildFusionTaskSnapshot(Map<String, Object> fusionConfig) {
    Map<String, Object> task = new LinkedHashMap<>();
    if (fusionConfig == null) {
      return task;
    }
    task.put("taskId", fusionConfig.get("taskId"));
    task.put("status", fusionConfig.get("taskStatus"));
    task.put("message", fusionConfig.get("taskMessage"));
    task.put("overlayText", fusionConfig.get("overlayText"));
    task.put("publishUrls", extractPublishUrls(fusionConfig));
    task.put("startedAt", fusionConfig.get("taskStartedAt"));
    task.put("updatedAt", fusionConfig.get("taskUpdatedAt"));
    task.put("stoppedAt", fusionConfig.get("taskStoppedAt"));
    task.put("exitCode", fusionConfig.get("taskExitCode"));
    return task;
  }

  @Transactional
  protected Map<String, Object> syncFusionTaskStatusInternal(
      String instanceKey,
      String deviceId,
      String channelId,
      boolean checkAccess,
      boolean silent) {
    try {
      VideoPlatformInstance instance = resolveFusionInstance(instanceKey, checkAccess);
      VideoPlatformChannel channel = requireChannel(instanceKey, deviceId, channelId);
      Map<String, Object> fusionConfig = extractFusionConfig(channel, instanceKey, deviceId);
      if (!isBackendFusionMode(fusionConfig)) {
        updateFusionRegistry(instanceKey, deviceId, channelId, fusionConfig);
        return fusionConfig;
      }
      Map<String, Object> taskData =
          requireFusionAdapter(instance).getFusionTaskStatus(instance, deviceId, channelId);
      mergeFusionTaskRuntime(fusionConfig, taskData);
      saveFusionConfig(channel, fusionConfig);
      updateFusionRegistry(instanceKey, deviceId, channelId, fusionConfig);
      return fusionConfig;
    } catch (Exception e) {
      if (!silent) {
        throw e;
      }
      return Collections.emptyMap();
    }
  }

  private Map<String, Object> extractFusionConfig(
      VideoPlatformChannel channel, String instanceKey, String deviceId) {
    ObjectNode root = parseChannelExtField3(channel == null ? null : channel.getExtField3());
    JsonNode fusionNode = root.get("fusion");
    Map<String, Object> rawFusion = new LinkedHashMap<>();
    if (fusionNode != null && fusionNode.isObject()) {
      rawFusion = objectMapper.convertValue(fusionNode, LinkedHashMap.class);
    }
    return normalizeFusionConfig(instanceKey, deviceId, channel == null ? null : channel.getChannelId(), rawFusion);
  }

  private void saveFusionConfig(VideoPlatformChannel channel, Map<String, Object> fusionConfig) {
    ObjectNode root = parseChannelExtField3(channel.getExtField3());
    JsonNode fusionNode = objectMapper.valueToTree(fusionConfig == null ? new LinkedHashMap<>() : fusionConfig);
    root.set("fusion", fusionNode);
    try {
      channel.setExtField3(objectMapper.writeValueAsString(root));
    } catch (Exception e) {
      throw new BaseException("保存融合配置失败: " + e.getMessage());
    }
    String updateUnionId = SecurityUtils.getUnionIdQuietly();
    if (StrUtil.isNotBlank(updateUnionId)) {
      channel.setUpdateId(updateUnionId);
    }
    channel.setUpdateTime(new Date());
    channelMapper.updateByPrimaryKeySelective(channel);
  }

  private ObjectNode parseChannelExtField3(String extField3) {
    if (StrUtil.isBlank(extField3)) {
      return objectMapper.createObjectNode();
    }
    try {
      JsonNode root = objectMapper.readTree(extField3);
      if (root instanceof ObjectNode) {
        return (ObjectNode) root;
      }
    } catch (Exception e) {
      log.warn("通道扩展字段解析失败，将重建JSON: {}", e.getMessage());
    }
    return objectMapper.createObjectNode();
  }

  private Map<String, Object> extractChannelStreamConfig(VideoPlatformChannel channel) {
    Map<String, Object> result = new LinkedHashMap<>();
    if (channel == null || StrUtil.isBlank(channel.getStreamConfig())) {
      return result;
    }
    try {
      JsonNode root = objectMapper.readTree(channel.getStreamConfig());
      if (root == null || !root.isObject()) {
        return result;
      }
      Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        result.put(entry.getKey(), jsonNodeToPlainValue(entry.getValue()));
      }
    } catch (Exception e) {
      log.debug("解析通道流配置失败: channelId={}, error={}", channel.getChannelId(), e.getMessage());
    }
    return result;
  }

  private Map<String, Object> extractChannelRuntimeConfig(VideoPlatformChannel channel) {
    Map<String, Object> result = new LinkedHashMap<>();
    if (channel == null) {
      return result;
    }
    putIfNotBlank(result, "manufacturer", channel.getManufacturer());
    putIfNotBlank(result, "model", channel.getModel());
    putIfNotBlank(result, "ipAddress", channel.getIpAddress());
    putIfNotBlank(result, "deviceIp", channel.getIpAddress());
    putIfNotBlank(result, "platformDeviceId", channel.getPlatformDeviceId());
    putIfNotBlank(result, "xAddr", channel.getExtField1());
    if (channel.getPort() != null) {
      result.put("port", channel.getPort());
      result.put("devicePort", channel.getPort());
    }
    return result;
  }

  private void putIfNotBlank(Map<String, Object> target, String key, Object value) {
    if (target == null || value == null) {
      return;
    }
    if (isMaskedSecret(key, value)) {
      return;
    }
    if (value instanceof String text && StrUtil.isBlank(text)) {
      return;
    }
    target.put(key, value);
  }

  private void putFirstNonBlank(Map<String, Object> target, String key, Object... values) {
    if (target == null || key == null || values == null) {
      return;
    }
    for (Object value : values) {
      if (value == null) {
        continue;
      }
      if (value instanceof String text) {
        if (StrUtil.isNotBlank(text) && !isMaskedSecret(key, text)) {
          target.put(key, text);
          return;
        }
      } else if (!isMaskedSecret(key, value)) {
        target.put(key, value);
        return;
      }
    }
  }

  private void mergeIfAbsent(Map<String, Object> target, Map<String, Object> source) {
    if (target == null || source == null) {
      return;
    }
    source.forEach(
        (key, value) -> {
          if (!target.containsKey(key) && value != null && !isMaskedSecret(key, value)) {
            target.put(key, value);
          }
        });
  }

  private Object firstNonMaskedSecret(String key, Object... values) {
    if (values == null) {
      return null;
    }
    for (Object value : values) {
      if (value != null && !isMaskedSecret(key, value)) {
        return value;
      }
    }
    return null;
  }

  private Map<String, Object> removeMaskedCredentialSecrets(Map<String, Object> credential) {
    Map<String, Object> safe = credential == null ? new LinkedHashMap<>() : new LinkedHashMap<>(credential);
    for (String key : new ArrayList<>(safe.keySet())) {
      if (isMaskedSecret(key, safe.get(key))) {
        safe.remove(key);
      }
    }
    return safe;
  }

  private boolean isMaskedSecret(String key, Object value) {
    if (key == null) {
      return false;
    }
    String lower = key.toLowerCase(java.util.Locale.ROOT);
    return (lower.contains("password") || lower.contains("secret") || lower.contains("token"))
        && JsonDesensitizationUtil.isMaskedValue(value);
  }

  private Object jsonNodeToPlainValue(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isBoolean()) {
      return node.booleanValue();
    }
    if (node.isInt() || node.isLong()) {
      return node.longValue();
    }
    if (node.isFloat() || node.isDouble() || node.isBigDecimal()) {
      return node.doubleValue();
    }
    if (node.isTextual()) {
      return node.asText();
    }
    return node.toString();
  }

  private Map<String, Object> normalizeFusionConfig(
      String instanceKey, String deviceId, String channelId, Map<String, Object> rawFusion) {
    Map<String, Object> normalized = new LinkedHashMap<>();
    if (rawFusion != null) {
      normalized.putAll(rawFusion);
    }
    boolean enabled = parseBoolean(rawFusion == null ? null : rawFusion.get("enabled"));
    normalized.put("enabled", enabled);
    // 兼容老配置：缺少 fusionMode 时根据历史运行态/发布地址推断，避免升级后丢失后端融合任务。
    String fusionMode = resolveFusionMode(rawFusion, enabled);
    normalized.put("fusionMode", fusionMode);
    normalized.put("frontendOsdEnabled", enabled && FUSION_MODE_FRONTEND_OSD.equals(fusionMode));
    normalized.put("backendFusionEnabled", enabled && FUSION_MODE_BACKEND.equals(fusionMode));
    normalized.put(
        "template",
        StrUtil.blankToDefault(
            stringValue(rawFusion == null ? null : rawFusion.get("template")), "right_panel_v1"));
    normalized.put(
        "displayMode",
        StrUtil.blankToDefault(
            stringValue(rawFusion == null ? null : rawFusion.get("displayMode")),
            StrUtil.isNotBlank(stringValue(rawFusion == null ? null : rawFusion.get("overlayTemplate")))
                ? "custom_template"
                : "auto_summary"));
    normalized.put(
        "overlayTemplate",
        rawFusion == null || rawFusion.get("overlayTemplate") == null
            ? ""
            : String.valueOf(rawFusion.get("overlayTemplate")));
    normalized.put(
        "sensorBindings",
        normalizeSensorBindings(rawFusion == null ? null : rawFusion.get("sensorBindings")));
    normalized.put(
        "stylePreset",
        StrUtil.blankToDefault(
            stringValue(rawFusion == null ? null : rawFusion.get("stylePreset")), "panel_dark"));
    normalized.put("style", normalizeFusionStyle(rawFusion == null ? null : rawFusion.get("style")));
    normalized.put(
        "preferProtocol",
        StrUtil.blankToDefault(
            stringValue(rawFusion == null ? null : rawFusion.get("preferProtocol")), "ws_flv"));
    normalized.put(
        "staleThresholdMs",
        parseLongOrDefault(rawFusion == null ? null : rawFusion.get("staleThresholdMs"), 5000L));
    normalized.put("iotIds", normalizeStringList(rawFusion == null ? null : rawFusion.get("iotIds")));
    normalized.put(
        "publishApp",
        StrUtil.blankToDefault(
            stringValue(rawFusion == null ? null : rawFusion.get("publishApp")), "fusion"));
    normalized.put(
        "publishStream",
        StrUtil.blankToDefault(
            stringValue(rawFusion == null ? null : rawFusion.get("publishStream")),
            buildFusionPublishStream(instanceKey, deviceId, channelId)));
    normalized.put(
        "publishUrls",
        enabled && FUSION_MODE_BACKEND.equals(fusionMode)
            ? extractPublishUrls(rawFusion)
            : new LinkedHashMap<>());
    normalized.put(
        "notes", rawFusion == null || rawFusion.get("notes") == null ? "" : String.valueOf(rawFusion.get("notes")));
    normalized.put(
        "taskId",
        rawFusion == null || rawFusion.get("taskId") == null ? "" : String.valueOf(rawFusion.get("taskId")));
    normalized.put(
        "taskStatus",
        enabled && FUSION_MODE_BACKEND.equals(fusionMode)
            ? StrUtil.blankToDefault(
                stringValue(rawFusion == null ? null : rawFusion.get("taskStatus")), "STOPPED")
            : "STOPPED");
    normalized.put(
        "taskMessage",
        enabled && FUSION_MODE_BACKEND.equals(fusionMode)
            ? (rawFusion == null || rawFusion.get("taskMessage") == null
                ? ""
                : String.valueOf(rawFusion.get("taskMessage")))
            : "");
    normalized.put(
        "overlayText",
        rawFusion == null || rawFusion.get("overlayText") == null
            ? ""
            : String.valueOf(rawFusion.get("overlayText")));
    normalized.put(
        "overlayPreview",
        rawFusion == null || rawFusion.get("overlayPreview") == null
            ? ""
            : String.valueOf(rawFusion.get("overlayPreview")));
    normalized.put(
        "lastSyncAt",
        parseLongOrDefault(rawFusion == null ? null : rawFusion.get("lastSyncAt"), 0L));
    normalized.put(
        "taskStartedAt",
        parseLongOrDefault(rawFusion == null ? null : rawFusion.get("taskStartedAt"), 0L));
    normalized.put(
        "taskUpdatedAt",
        parseLongOrDefault(rawFusion == null ? null : rawFusion.get("taskUpdatedAt"), 0L));
    normalized.put(
        "taskStoppedAt",
        parseLongOrDefault(rawFusion == null ? null : rawFusion.get("taskStoppedAt"), 0L));
    normalized.put(
        "taskExitCode",
        parseLongOrDefault(rawFusion == null ? null : rawFusion.get("taskExitCode"), 0L));
    return normalized;
  }

  private String normalizeFusionMode(String mode) {
    String normalized = StrUtil.blankToDefault(mode, "").trim().toLowerCase();
    if (FUSION_MODE_FRONTEND_OSD.equals(normalized)
        || FUSION_MODE_BACKEND.equals(normalized)
        || FUSION_MODE_DISABLED.equals(normalized)) {
      return normalized;
    }
    return "";
  }

  private String resolveFusionMode(Map<String, Object> fusionConfig) {
    return resolveFusionMode(fusionConfig, isFusionEnabled(fusionConfig));
  }

  private String resolveFusionMode(Map<String, Object> fusionConfig, boolean enabled) {
    if (!enabled) {
      return FUSION_MODE_DISABLED;
    }
    // 新配置优先；老配置再由 backendFusionEnabled/frontendOsdEnabled/任务状态推断。
    String configuredMode =
        normalizeFusionMode(stringValue(fusionConfig == null ? null : fusionConfig.get("fusionMode")));
    if (StrUtil.isNotBlank(configuredMode)) {
      return configuredMode;
    }
    if (parseBoolean(fusionConfig == null ? null : fusionConfig.get("backendFusionEnabled"))) {
      return FUSION_MODE_BACKEND;
    }
    if (parseBoolean(fusionConfig == null ? null : fusionConfig.get("frontendOsdEnabled"))) {
      return FUSION_MODE_FRONTEND_OSD;
    }
    String taskStatus = stringValue(fusionConfig == null ? null : fusionConfig.get("taskStatus"));
    if ("RUNNING".equalsIgnoreCase(taskStatus) || "STARTING".equalsIgnoreCase(taskStatus)) {
      return FUSION_MODE_BACKEND;
    }
    if (!extractPublishUrls(fusionConfig).isEmpty()) {
      return FUSION_MODE_BACKEND;
    }
    return FUSION_MODE_FRONTEND_OSD;
  }

  private boolean isFrontendOsdMode(Map<String, Object> fusionConfig) {
    return FUSION_MODE_FRONTEND_OSD.equals(resolveFusionMode(fusionConfig));
  }

  private boolean isBackendFusionMode(Map<String, Object> fusionConfig) {
    return FUSION_MODE_BACKEND.equals(resolveFusionMode(fusionConfig));
  }

  private String buildFusionPublishStream(String instanceKey, String deviceId, String channelId) {
    StringBuilder builder = new StringBuilder();
    builder.append(sanitizeFusionKey(instanceKey));
    if (StrUtil.isNotBlank(deviceId)) {
      builder.append("_").append(sanitizeFusionKey(deviceId));
    }
    if (StrUtil.isNotBlank(channelId)) {
      builder.append("_").append(sanitizeFusionKey(channelId));
    }
    return builder.toString();
  }

  private String sanitizeFusionKey(String value) {
    if (StrUtil.isBlank(value)) {
      return "fusion";
    }
    return value.replaceAll("[^a-zA-Z0-9_-]", "_");
  }

  private List<String> normalizeStringList(Object value) {
    Set<String> ids = new LinkedHashSet<>();
    if (value instanceof List) {
      List<?> listValue = (List<?>) value;
      for (Object item : listValue) {
        if (item != null && StrUtil.isNotBlank(String.valueOf(item))) {
          ids.add(String.valueOf(item).trim());
        }
      }
    } else if (value instanceof String && StrUtil.isNotBlank((String) value)) {
      String[] parts = ((String) value).split("[,\\n\\r]");
      for (String part : parts) {
        if (StrUtil.isNotBlank(part)) {
          ids.add(part.trim());
        }
      }
    }
    return new ArrayList<>(ids);
  }

  private Map<String, String> extractPublishUrls(Map<String, Object> fusionConfig) {
    Map<String, String> urls = new LinkedHashMap<>();
    if (fusionConfig == null) {
      return urls;
    }
    Object publishUrlsObj = fusionConfig.get("publishUrls");
    if (publishUrlsObj instanceof Map) {
      Map<?, ?> publishUrlsMap = (Map<?, ?>) publishUrlsObj;
      for (Map.Entry<?, ?> entry : publishUrlsMap.entrySet()) {
        if (entry.getKey() == null || entry.getValue() == null) {
          continue;
        }
        String key = String.valueOf(entry.getKey()).trim();
        String value = String.valueOf(entry.getValue()).trim();
        if (StrUtil.isNotBlank(key) && StrUtil.isNotBlank(value)) {
          urls.put(key, value);
        }
      }
    }
    return urls;
  }

  private boolean isFusionEnabled(Map<String, Object> fusionConfig) {
    return parseBoolean(fusionConfig == null ? null : fusionConfig.get("enabled"));
  }

  private boolean isFusionTaskActive(Map<String, Object> fusionConfig) {
    String taskStatus = stringValue(fusionConfig == null ? null : fusionConfig.get("taskStatus"));
    return "RUNNING".equalsIgnoreCase(taskStatus) || "STARTING".equalsIgnoreCase(taskStatus);
  }

  private boolean isFusionReady(Map<String, Object> fusionConfig, Map<String, String> fusionUrls) {
    // 只有后端融合流才有“可播放融合流”的概念；前端 OSD 即使启用也不应该返回 fusionReady。
    return isFusionEnabled(fusionConfig)
        && isBackendFusionMode(fusionConfig)
        && isFusionTaskActive(fusionConfig)
        && fusionUrls != null
        && !fusionUrls.isEmpty();
  }

  private void clearFusionRuntimeState(Map<String, Object> fusionConfig, String message) {
    if (fusionConfig == null) {
      return;
    }
    fusionConfig.put("publishUrls", new LinkedHashMap<>());
    fusionConfig.put("taskStatus", "STOPPED");
    fusionConfig.put("taskMessage", StrUtil.blankToDefault(message, ""));
    fusionConfig.put("overlayText", "");
    fusionConfig.put("taskStoppedAt", System.currentTimeMillis());
    fusionConfig.put("taskUpdatedAt", System.currentTimeMillis());
  }

  private void stopFusionTaskWhenDisabled(
      VideoPlatformInstance instance,
      String deviceId,
      String channelId,
      Map<String, Object> previousFusionConfig,
      Map<String, Object> fusionConfig) {
    if (instance == null || !"wvp".equalsIgnoreCase(instance.getPlatformType())) {
      return;
    }
    if (!isFusionEnabled(previousFusionConfig) && !isFusionTaskActive(previousFusionConfig)) {
      return;
    }
    try {
      Map<String, Object> taskData =
          requireFusionAdapter(instance).stopFusionTask(instance, deviceId, channelId);
      mergeFusionTaskRuntime(fusionConfig, taskData);
    } catch (Exception e) {
      log.warn(
          "[视频融合] 关闭融合配置时自动停止任务失败 instanceKey={}, deviceId={}, channelId={}, msg={}",
          instance == null ? "" : instance.getInstanceKey(),
          deviceId,
          channelId,
          e.getMessage());
    }
    clearFusionRuntimeState(fusionConfig, "融合未启用，默认回退原始流");
  }

  private String pickPreferredUrl(Map<String, String> urls, String preferredProtocol) {
    if (urls == null || urls.isEmpty()) {
      return "";
    }
    List<String> candidates = new ArrayList<>();
    if (StrUtil.isNotBlank(preferredProtocol)) {
      candidates.add(preferredProtocol);
    }
    Collections.addAll(
        candidates, "ws_flv", "wss_flv", "flv", "https_flv", "hls", "https_hls", "rtc", "rtcs", "rtmp", "rtsp");
    for (String key : candidates) {
      String value = urls.get(key);
      if (isPlayableUrl(value)) {
        return value;
      }
    }
    return "";
  }

  private boolean isPlayableUrl(String value) {
    if (StrUtil.isBlank(value)) {
      return false;
    }
    String lower = value.trim().toLowerCase();
    return lower.startsWith("http://")
        || lower.startsWith("https://")
        || lower.startsWith("ws://")
        || lower.startsWith("wss://")
        || lower.startsWith("rtsp://")
        || lower.startsWith("rtsps://")
        || lower.startsWith("rtmp://")
        || lower.startsWith("rtmps://")
        || lower.startsWith("webrtc://")
        || lower.startsWith("rtc://");
  }

  private boolean parseBoolean(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    String text = String.valueOf(value).trim();
    return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
  }

  private long parseLongOrDefault(Object value, long defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (Exception ignore) {
      return defaultValue;
    }
  }

  private int parseIntOrDefault(Object value, int defaultValue) {
    Integer parsed = parseIntOrNull(value);
    return parsed == null ? defaultValue : parsed;
  }

  private int clampInt(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  private String stringValue(Object value) {
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    return "null".equalsIgnoreCase(text) ? null : text;
  }

  private Map<String, Object> readObjectMap(String json) {
    if (StrUtil.isBlank(json)) {
      return new LinkedHashMap<>();
    }
    try {
      return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String, Object>>() {});
    } catch (Exception e) {
      return new LinkedHashMap<>();
    }
  }

  private String writeObjectMap(Map<String, Object> value) {
    try {
      return value == null || value.isEmpty() ? null : objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      return null;
    }
  }

  private boolean containsAny(Map<String, Object> source, String... keys) {
    if (source == null || keys == null) {
      return false;
    }
    for (String key : keys) {
      if (source.containsKey(key)) {
        return true;
      }
    }
    return false;
  }

  private void mergeIfPresent(Map<String, Object> target, Map<String, Object> source, String key) {
    if (source != null && source.containsKey(key)) {
      target.put(key, source.get(key));
    }
  }

  private void mergeIfNotBlank(Map<String, Object> target, Map<String, Object> source, String key) {
    if (source == null || !source.containsKey(key)) {
      return;
    }
    String value = stringValue(source.get(key));
    if (StrUtil.isNotBlank(value)) {
      target.put(key, value);
    }
  }

  private void mergeAliasIfPresent(
      Map<String, Object> target, Map<String, Object> source, String targetKey, String... sourceKeys) {
    if (source == null || sourceKeys == null) {
      return;
    }
    for (String sourceKey : sourceKeys) {
      if (source.containsKey(sourceKey)) {
        target.put(targetKey, source.get(sourceKey));
        return;
      }
    }
  }


  private Integer firstNonNullInt(Integer... values) {
    if (values == null) {
      return null;
    }
    for (Integer value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private String buildOnvifXAddr(String ip, Integer port) {
    if (StrUtil.isBlank(ip)) {
      return null;
    }
    if (ip.startsWith("http://") || ip.startsWith("https://")) {
      return ip;
    }
    return "http://" + ip.trim() + ":" + (port == null || port <= 0 ? 80 : port) + "/onvif/device_service";
  }

  /** 同步设备通道数据到通道表 */
  private void syncDeviceChannels(
      Long deviceId,
      String instanceKey,
      String platformDeviceId,
      Map<String, Object> deviceData,
      String platformType,
      Date now) {
    VideoPlatformInstance instance = getByInstanceKey(instanceKey);
    String tenantId = instance == null ? null : instance.getTenantId();
    // 提取channelList
    Object channelListObj = deviceData.get("channelList");
    if (channelListObj == null || !(channelListObj instanceof List)) {
      return;
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> channelList = (List<Map<String, Object>>) channelListObj;

    // ========== 第一步：先删除该设备下所有通道数据 ==========
    Example deleteExample = new Example(VideoPlatformChannel.class);
    deleteExample.createCriteria().andEqualTo("deviceId", deviceId);
    channelMapper.deleteByExample(deleteExample);

    // ========== 第二步：批量插入新通道数据 ==========
    String currentUserId = SecurityUtils.getUnionId();

    for (Map<String, Object> ch : channelList) {
      String channelId = String.valueOf(ch.getOrDefault("channelId", ch.get("id")));
      if (channelId == null || channelId.equals("null")) continue;

      // 通道公共字段
      String channelName = (String) ch.getOrDefault("channelName", ch.get("name"));
      String channelStatus = (String) ch.getOrDefault("channelStatus", ch.get("status"));
      String channelType = (ch.get("channelType") + "");
      String parentId = (String) ch.get("parentId");
      String manufacturer = (String) ch.get("manufacturer");
      String model = (String) ch.get("model");
      String owner = (String) ch.get("owner");
      String civilCode = (String) ch.get("civilCode");
      String address = (String) ch.get("address");
      Integer parental = parseIntOrNull(ch.get("parental"));
      Integer safetyWay = parseIntOrNull(ch.get("safetyWay"));
      Integer registerWay = parseIntOrNull(ch.get("registerWay"));
      Integer secrecy = parseIntOrNull(ch.get("secrecy"));
      String ipAddress = (String) ch.get("ipAddress");
      Integer port = parseIntOrNull(ch.get("port"));
      String longitude = (String) ch.get("longitude");
      String latitude = (String) ch.get("latitude");
      Integer ptzType = parseIntOrNull(ch.get("ptzType"));
      Integer positionType = parseIntOrNull(ch.get("positionType"));

      // 构建 Builder (全部新插入)
      VideoPlatformChannel.VideoPlatformChannelBuilder builder =
          VideoPlatformChannel.builder()
              .deviceId(deviceId)
              .instanceKey(instanceKey)
              .platformDeviceId(platformDeviceId)
              .tenantId(tenantId)
              .channelId(channelId);

      // 设置公共字段
      builder
          .channelName(channelName)
          .channelStatus(channelStatus)
          .channelType(channelType)
          .parentId(parentId)
          .manufacturer(manufacturer)
          .model(model)
          .owner(owner)
          .civilCode(civilCode)
          .address(address)
          .parental(parental)
          .safetyWay(safetyWay)
          .registerWay(registerWay)
          .secrecy(secrecy)
          .ipAddress(ipAddress)
          .port(port)
          .longitude(longitude)
          .latitude(latitude)
          .ptzType(ptzType)
          .positionType(positionType);

      // 根据平台类型设置特有字段
      if ("wvp".equalsIgnoreCase(platformType)) {
        // WVP通道级特有字段
        builder.streamId((String) ch.get("streamId"));
        builder.gbStreamId((String) ch.get("gbStreamId"));
        builder.hasAudio(parseIntOrNull(ch.get("hasAudio")));
      } else if ("isc".equalsIgnoreCase(platformType) || "ics".equalsIgnoreCase(platformType)) {
        // 海康ISC通道级特有字段
        builder.cameraIndexCode((String) ch.get("cameraIndexCode"));
        builder.channelNo((String) ch.get("channelNo"));
        builder.cameraType(parseIntOrNull(ch.get("cameraType")));
        builder.ptz(parseIntOrNull(ch.get("ptz")));
        builder.capabilitySet((String) ch.get("capabilitySet"));
        builder.installLocation((String) ch.get("installLocation"));
      } else if ("hik_sdk".equalsIgnoreCase(platformType)) {
        // 海康直连SDK通道级特有字段
        builder.cameraIndexCode((String) ch.get("cameraIndexCode"));
        builder.channelCode((String) ch.get("channelCode"));
        builder.channelNo((String) ch.get("channelNo"));
        builder.channelSeq(parseIntOrNull(ch.get("channelSeq")));
        builder.cameraType(parseIntOrNull(ch.get("cameraType")));
        builder.ptz(parseIntOrNull(ch.get("ptz")));
        builder.capabilitySet((String) ch.get("capabilitySet"));
        builder.installLocation((String) ch.get("installLocation"));
      } else if ("icc".equalsIgnoreCase(platformType)) {
        // 大华ICC通道级特有字段
        builder.channelCode((String) ch.get("channelCode"));
        builder.channelSeq(parseIntOrNull(ch.get("channelSeq")));
        builder.encodeFormat((String) ch.get("encodeFormat"));
        builder.resolution((String) ch.get("resolution"));
      } else if ("dahua_sdk".equalsIgnoreCase(platformType) || "tiandy_sdk".equalsIgnoreCase(platformType) || "uniview_sdk".equalsIgnoreCase(platformType)) {
        // 大华、天地伟业、宇视直连SDK通道级字段（保留SDK返回的通道序号）
        builder.channelCode((String) ch.get("channelCode"));
        builder.channelSeq(parseIntOrNull(ch.get("channelSeq")));
        builder.channelNo((String) ch.get("channelNo"));
        builder.encodeFormat((String) ch.get("encodeFormat"));
        builder.resolution((String) ch.get("resolution"));
      }

      // 能力集和流配置(如果JON格式)
      try {
        Object capabilities = ch.get("capabilities");
        if (capabilities != null) {
          builder.capabilities(objectMapper.writeValueAsString(capabilities));
        }
        Object streamConfig = ch.get("streamConfig");
        if (streamConfig != null) {
          builder.streamConfig(objectMapper.writeValueAsString(streamConfig));
        }
      } catch (Exception ignore) {
      }

      builder
          .enabled(1)
          .createId(currentUserId)
          .updateId(currentUserId)
          .createTime(now)
          .updateTime(now);

      VideoPlatformChannel finalChannel = builder.build();
      channelMapper.insertSelective(finalChannel);
    }
  }

  /** 安全解析Integer */
  private Integer parseIntOrNull(Object obj) {
    if (obj == null) return null;
    try {
      return Integer.parseInt(String.valueOf(obj));
    } catch (Exception e) {
      return null;
    }
  }

  // ==================== 国标录像下载 ====================

  @Override
  public cn.universal.common.domain.R<Map<String, Object>> startGBRecordDownload(
      String instanceKey, String deviceId, String channelId, Map<String, Object> data) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);

    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    try {
      // 反射调用WvpPlatformAdapter的公开方法
      java.lang.reflect.Method method =
          adapter
              .getClass()
              .getMethod(
                  "startGBRecordDownload",
                  VideoPlatformInstance.class,
                  String.class,
                  String.class,
                  Map.class);
      @SuppressWarnings("unchecked")
      cn.universal.common.domain.R<Map<String, Object>> result =
          (cn.universal.common.domain.R<Map<String, Object>>)
              method.invoke(adapter, instance, deviceId, channelId, data);
      return result;
    } catch (NoSuchMethodException e) {
      return cn.universal.common.domain.R.error("当前平台不支持国标录像下载", null);
    } catch (Exception e) {
      return cn.universal.common.domain.R.error("调用失败：" + e.getMessage(), null);
    }
  }

  @Override
  public cn.universal.common.domain.R<Void> stopGBRecordDownload(
      String instanceKey, String deviceId, String channelId, Map<String, Object> data) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);

    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    try {
      java.lang.reflect.Method method =
          adapter
              .getClass()
              .getMethod(
                  "stopGBRecordDownload",
                  VideoPlatformInstance.class,
                  String.class,
                  String.class,
                  Map.class);
      @SuppressWarnings("unchecked")
      cn.universal.common.domain.R<Void> result =
          (cn.universal.common.domain.R<Void>)
              method.invoke(adapter, instance, deviceId, channelId, data);
      return result;
    } catch (NoSuchMethodException e) {
      return cn.universal.common.domain.R.error("当前平台不支持国标录像下载");
    } catch (Exception e) {
      return cn.universal.common.domain.R.error("调用失败：" + e.getMessage());
    }
  }

  @Override
  public cn.universal.common.domain.R<Map<String, Object>> getGBRecordDownloadProgress(
      String instanceKey, String deviceId, String channelId, String stream) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);

    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    try {
      java.lang.reflect.Method method =
          adapter
              .getClass()
              .getMethod(
                  "getGBRecordDownloadProgress",
                  VideoPlatformInstance.class,
                  String.class,
                  String.class,
                  String.class);
      @SuppressWarnings("unchecked")
      cn.universal.common.domain.R<Map<String, Object>> result =
          (cn.universal.common.domain.R<Map<String, Object>>)
              method.invoke(adapter, instance, deviceId, channelId, stream);
      return result;
    } catch (NoSuchMethodException e) {
      return cn.universal.common.domain.R.error("当前平台不支持国标录像下载", null);
    } catch (Exception e) {
      return cn.universal.common.domain.R.error("调用失败：" + e.getMessage(), null);
    }
  }

  // ==================== 云端录像 ====================

  @Override
  public cn.universal.common.domain.R<List<String>> queryCloudRecordDates(
      String instanceKey, String deviceId, String channelId, Map<String, String> params) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);

    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    try {
      java.lang.reflect.Method method =
          adapter
              .getClass()
              .getMethod(
                  "queryCloudRecordDates",
                  VideoPlatformInstance.class,
                  String.class,
                  String.class,
                  Map.class);
      @SuppressWarnings("unchecked")
      cn.universal.common.domain.R<List<String>> result =
          (cn.universal.common.domain.R<List<String>>)
              method.invoke(adapter, instance, deviceId, channelId, params);
      return result;
    } catch (NoSuchMethodException e) {
      return cn.universal.common.domain.R.error("当前平台不支持云端录像", null);
    } catch (Exception e) {
      return cn.universal.common.domain.R.error("调用失败：" + e.getMessage(), null);
    }
  }

  @Override
  public cn.universal.common.domain.R<Map<String, Object>> queryCloudRecords(
      String instanceKey, String deviceId, String channelId, Map<String, String> params) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);

    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    try {
      java.lang.reflect.Method method =
          adapter
              .getClass()
              .getMethod(
                  "queryCloudRecords",
                  VideoPlatformInstance.class,
                  String.class,
                  String.class,
                  Map.class);
      @SuppressWarnings("unchecked")
      cn.universal.common.domain.R<Map<String, Object>> result =
          (cn.universal.common.domain.R<Map<String, Object>>)
              method.invoke(adapter, instance, deviceId, channelId, params);
      return result;
    } catch (NoSuchMethodException e) {
      return cn.universal.common.domain.R.error("当前平台不支持云端录像", null);
    } catch (Exception e) {
      return cn.universal.common.domain.R.error("调用失败：" + e.getMessage(), null);
    }
  }

  @Override
  public cn.universal.common.domain.R<Map<String, Object>> loadCloudRecord(
      String instanceKey, String deviceId, String channelId, Map<String, Object> data) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);

    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    try {
      java.lang.reflect.Method method =
          adapter
              .getClass()
              .getMethod(
                  "loadCloudRecord",
                  VideoPlatformInstance.class,
                  String.class,
                  String.class,
                  Map.class);
      @SuppressWarnings("unchecked")
      cn.universal.common.domain.R<Map<String, Object>> result =
          (cn.universal.common.domain.R<Map<String, Object>>)
              method.invoke(adapter, instance, deviceId, channelId, data);
      return result;
    } catch (NoSuchMethodException e) {
      return cn.universal.common.domain.R.error("当前平台不支持云端录像", null);
    } catch (Exception e) {
      return cn.universal.common.domain.R.error("调用失败：" + e.getMessage(), null);
    }
  }

  @Override
  public cn.universal.common.domain.R<Void> seekCloudRecord(
      String instanceKey, String deviceId, String channelId, Map<String, Object> data) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);

    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    try {
      java.lang.reflect.Method method =
          adapter
              .getClass()
              .getMethod(
                  "seekCloudRecord",
                  VideoPlatformInstance.class,
                  String.class,
                  String.class,
                  Map.class);
      @SuppressWarnings("unchecked")
      cn.universal.common.domain.R<Void> result =
          (cn.universal.common.domain.R<Void>)
              method.invoke(adapter, instance, deviceId, channelId, data);
      return result;
    } catch (NoSuchMethodException e) {
      return cn.universal.common.domain.R.error("当前平台不支持云端录像");
    } catch (Exception e) {
      return cn.universal.common.domain.R.error("调用失败：" + e.getMessage());
    }
  }

  @Override
  public cn.universal.common.domain.R<Void> setCloudRecordSpeed(
      String instanceKey, String deviceId, String channelId, Map<String, Object> data) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);

    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    try {
      java.lang.reflect.Method method =
          adapter
              .getClass()
              .getMethod(
                  "setCloudRecordSpeed",
                  VideoPlatformInstance.class,
                  String.class,
                  String.class,
                  Map.class);
      @SuppressWarnings("unchecked")
      cn.universal.common.domain.R<Void> result =
          (cn.universal.common.domain.R<Void>)
              method.invoke(adapter, instance, deviceId, channelId, data);
      return result;
    } catch (NoSuchMethodException e) {
      return cn.universal.common.domain.R.error("当前平台不支持云端录像");
    } catch (Exception e) {
      return cn.universal.common.domain.R.error("调用失败：" + e.getMessage());
    }
  }

  @Override
  public cn.universal.common.domain.R<Map<String, Object>> getCloudRecordPlayPath(
      String instanceKey, Integer recordId) {
    VideoPlatformInstance instance = requireAccessibleInstance(instanceKey);

    VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
    try {
      java.lang.reflect.Method method =
          adapter
              .getClass()
              .getMethod("getCloudRecordPlayPath", VideoPlatformInstance.class, Integer.class);
      @SuppressWarnings("unchecked")
      cn.universal.common.domain.R<Map<String, Object>> result =
          (cn.universal.common.domain.R<Map<String, Object>>)
              method.invoke(adapter, instance, recordId);
      return result;
    } catch (NoSuchMethodException e) {
      return cn.universal.common.domain.R.error("当前平台不支持云端录像下载", null);
    } catch (Exception e) {
      return cn.universal.common.domain.R.error("调用失败：" + e.getMessage(), null);
    }
  }

  @Override
  public Map<String, Object> getDeviceDetail(String instanceKey, String deviceId) {
    requireAccessibleInstance(instanceKey);
    VideoPlatformDevice device = requireDeviceEntity(instanceKey, deviceId);
    Map<String, Object> result = new java.util.HashMap<>();

    // 主表字段
    result.put("id", device.getId());
    result.put("instanceKey", device.getInstanceKey());
    result.put("deviceId", device.getDeviceId());
    result.put("deviceName", device.getDeviceName());
    result.put("deviceStatus", device.getDeviceStatus());
    result.put("deviceModel", device.getDeviceModel());
    result.put("deviceIp", device.getDeviceIp());
    result.put("devicePort", device.getDevicePort());
    result.put("manufacturer", device.getManufacturer());
    result.put("orgId", device.getOrgId());
    result.put("orgName", device.getOrgName());
    result.put("gpsX", device.getGpsX());
    result.put("gpsY", device.getGpsY());
    result.put("gpsZ", device.getGpsZ());
    result.put("remark", device.getRemark());
    result.put("configuration", device.getConfiguration());
    result.put("enabled", device.getEnabled());
    result.put("createTime", device.getCreateTime());
    result.put("updateTime", device.getUpdateTime());

    // 查询扩展表信息
    Example extEx = new Example(VideoPlatformDeviceExt.class);
    extEx.createCriteria().andEqualTo("deviceId", device.getId());
    List<VideoPlatformDeviceExt> extList = deviceExtMapper.selectByExample(extEx);

    if (!extList.isEmpty()) {
      VideoPlatformDeviceExt ext = extList.get(0);
      Map<String, Object> extInfo = new java.util.HashMap<>();

      // WVP特有字段
      if (ext.getCharset() != null) extInfo.put("charset", ext.getCharset());
      if (ext.getTransport() != null) extInfo.put("transport", ext.getTransport());
      if (ext.getStreamMode() != null) extInfo.put("streamMode", ext.getStreamMode());
      if (ext.getHostAddress() != null) extInfo.put("hostAddress", ext.getHostAddress());
      if (ext.getExpires() != null) extInfo.put("expires", ext.getExpires());
      if (ext.getKeepaliveTime() != null) extInfo.put("keepaliveTime", ext.getKeepaliveTime());
      if (ext.getRegisterTime() != null) extInfo.put("registerTime", ext.getRegisterTime());
      if (ext.getMediaServerId() != null) extInfo.put("mediaServerId", ext.getMediaServerId());

      // 海康ISC特有字段
      if (ext.getEncodeDevIndexCode() != null)
        extInfo.put("encodeDevIndexCode", ext.getEncodeDevIndexCode());
      if (ext.getDeviceCapabilitySet() != null)
        extInfo.put("deviceCapabilitySet", ext.getDeviceCapabilitySet());

      // 大华ICC特有字段
      if (ext.getDeviceSn() != null) extInfo.put("deviceSn", ext.getDeviceSn());
      if (ext.getDeviceCategory() != null) extInfo.put("deviceCategory", ext.getDeviceCategory());
      if (ext.getDeviceType() != null) extInfo.put("deviceType", ext.getDeviceType());
      if (ext.getOwnerCode() != null) extInfo.put("ownerCode", ext.getOwnerCode());
      if (ext.getIsOnline() != null) extInfo.put("isOnline", ext.getIsOnline());
      if (ext.getSleepStat() != null) extInfo.put("sleepStat", ext.getSleepStat());
      if (ext.getThirdProxyPort() != null) extInfo.put("thirdProxyPort", ext.getThirdProxyPort());
      if (ext.getThirdProxyServerCode() != null)
        extInfo.put("thirdProxyServerCode", ext.getThirdProxyServerCode());
      if (ext.getLicenseLimit() != null) extInfo.put("licenseLimit", ext.getLicenseLimit());
      if (ext.getOfflineReason() != null) extInfo.put("offlineReason", ext.getOfflineReason());
      if (ext.getSubSystem() != null) extInfo.put("subSystem", ext.getSubSystem());
      if (ext.getUnitsInfo() != null) extInfo.put("unitsInfo", ext.getUnitsInfo());
      if (ext.getExtField1() != null) extInfo.put("extField1", ext.getExtField1());
      if (ext.getExtField2() != null) extInfo.put("extField2", ext.getExtField2());
      if (ext.getExtField3() != null) extInfo.put("extField3", writeObjectMap((Map<String, Object>) maskNestedSecrets(readObjectMap(ext.getExtField3()))));

      Map<String, Object> extPayload = readObjectMap(ext.getExtField3());
      if (!extPayload.isEmpty()) {
        for (String key :
            List.of(
                "xAddr",
                "deviceServiceUrl",
                "mediaServiceUrl",
                "username",
                "authType",
                "deviceType",
                "deviceKind",
                "vendor",
                "remoteChannel",
                "channelTotal",
                "decodePolicy",
                "encrypted",
                "rtspPort",
                "gatewayDeviceId",
                "registerCode",
                "registrationId",
                "serialNumber",
                "deviceSn",
                "sdkPort",
                "cmsPort",
                "smsPort",
                "smsBackPort",
                "alarmTcpPort",
                "alarmUdpPort",
                "picPort",
                "streamMode",
                "loginMode",
                "accessMode",
                "firmwareVersion",
                "gatewayEndpoint",
                "zlmMediaServerId",
                "mediaServerId",
                "isupUserId")) {
          Object value = extPayload.get(key);
          if (value != null) {
            extInfo.put(key, value);
          }
        }
        extInfo.put("hasPassword", StrUtil.isNotBlank(stringValue(extPayload.get("password"))));
      }

      result.put("extInfo", extInfo);
    }

    return result;
  }

  private VideoPlatformDevice requireDeviceEntity(String instanceKey, String deviceId) {
    Example deviceEx = new Example(VideoPlatformDevice.class);
    deviceEx
        .createCriteria()
        .andEqualTo("instanceKey", instanceKey)
        .andEqualTo("deviceId", deviceId);
    List<VideoPlatformDevice> deviceList = deviceMapper.selectByExample(deviceEx);
    if (deviceList.isEmpty()) {
      throw new BaseException("设备不存在：" + deviceId);
    }
    return deviceList.get(0);
  }

  private boolean isDahuaRegisteredDevice(VideoPlatformInstance instance, String deviceId) {
    if (!"dahua_sdk".equals(instance.getPlatformType())) return false;
    VideoPlatformDevice device = requireDeviceEntity(instance.getInstanceKey(), deviceId);
    VideoPlatformDeviceExt extension = findDeviceExt(device.getId());
    Map<String, Object> access = extension == null ? Map.of() : readObjectMap(extension.getExtField3());
    return "active_registration".equals(access.get("accessMode")) || "active_registration".equals(access.get("loginMode"));
  }

  private int countChannelsByDevicePk(Long devicePk) {
    if (devicePk == null) {
      return 0;
    }
    Example channelEx = new Example(VideoPlatformChannel.class);
    channelEx.createCriteria().andEqualTo("deviceId", devicePk);
    return channelMapper.selectCountByExample(channelEx);
  }

  private Map<String, Object> buildStoredOnvifDeviceSyncPayload(
      VideoPlatformInstance instance, VideoPlatformDevice device) {
    Map<String, Object> payload = new LinkedHashMap<>();
    putIfNotBlank(payload, "deviceId", device.getDeviceId());
    putIfNotBlank(payload, "platformDeviceId", device.getDeviceId());
    putIfNotBlank(payload, "deviceName", device.getDeviceName());
    putIfNotBlank(payload, "name", device.getDeviceName());
    putIfNotBlank(payload, "status", device.getDeviceStatus());
    putIfNotBlank(payload, "deviceStatus", device.getDeviceStatus());
    putIfNotBlank(payload, "ip", device.getDeviceIp());
    putIfNotBlank(payload, "deviceIp", device.getDeviceIp());
    if (device.getDevicePort() != null) {
      payload.put("port", device.getDevicePort());
      payload.put("devicePort", device.getDevicePort());
    }
    putIfNotBlank(payload, "vendor", device.getManufacturer());
    putIfNotBlank(payload, "manufacturer", device.getManufacturer());
    putIfNotBlank(payload, "model", device.getDeviceModel());

    VideoPlatformDeviceExt ext = findDeviceExt(device.getId());
    Map<String, Object> extPayload = ext == null ? new LinkedHashMap<>() : readObjectMap(ext.getExtField3());
    for (String key :
        List.of(
            "username",
            "password",
            "authType",
            "deviceType",
            "deviceKind",
            "vendor",
            "remoteChannel",
            "channelTotal",
            "rtspPort",
            "decodePolicy",
            "encrypted",
            "mediaServerId",
            "previewMediaServerId",
            "timeoutMs",
            "requestTimeoutMs",
            "embedCredentialInRtsp")) {
      putIfNotBlank(payload, key, extPayload.get(key));
    }

    String xAddr =
        firstNonBlank(
            stringValue(extPayload.get("xAddr")),
            stringValue(extPayload.get("deviceServiceUrl")),
            ext == null ? null : ext.getExtField1(),
            buildOnvifXAddr(device.getDeviceIp(), device.getDevicePort()),
            instance == null ? null : instance.getEndpoint());
    if (StrUtil.isBlank(xAddr)) {
      throw new BaseException("设备未配置ONVIF地址，无法同步通道");
    }
    payload.put("xAddr", xAddr);
    payload.put("endpoint", xAddr);
    payload.put("deviceServiceUrl", firstNonBlank(stringValue(extPayload.get("deviceServiceUrl")), xAddr));
    payload.put("skipWsDiscovery", true);
    payload.put("useCache", false);
    payload.put("requireAuthVerified", true);
    return payload;
  }

  private VideoPlatformDeviceExt findDeviceExt(Long devicePk) {
    if (devicePk == null) {
      return null;
    }
    Example extEx = new Example(VideoPlatformDeviceExt.class);
    extEx.createCriteria().andEqualTo("deviceId", devicePk);
    List<VideoPlatformDeviceExt> extList = deviceExtMapper.selectByExample(extEx);
    return extList.isEmpty() ? null : extList.get(0);
  }

  @Override
  @Transactional
  public Map<String, Object> updateDevice(
      String instanceKey, String deviceId, Map<String, Object> updates) {
    VideoPlatformInstance accessibleInstance = requireAccessibleInstance(instanceKey);
    Example ex = new Example(VideoPlatformDevice.class);
    ex.createCriteria().andEqualTo("instanceKey", instanceKey).andEqualTo("deviceId", deviceId)
        .andEqualTo("tenantId", accessibleInstance.getTenantId());
    ex.setForUpdate(true);
    List<VideoPlatformDevice> list = deviceMapper.selectByExample(ex);
    if (list.isEmpty()) {
      throw new BaseException("设备不存在：" + deviceId);
    }
    VideoPlatformDevice device = list.get(0);
    VideoPlatformInstance savedInstance = getByInstanceKey(instanceKey);
    boolean registrationAddress = savedInstance != null && isDahuaRegisteredDevice(savedInstance, deviceId);
    String previousDeviceIp = device.getDeviceIp();
    String previousMediaServerId = currentOnvifMediaServerId(device.getId());
    if (updates != null) {
      Object v;
      v = updates.get("deviceName");
      if (updates.containsKey("deviceName")) deviceNameService.renameDevice(accessibleInstance, device, v);
      v = updates.get("orgId");
      if (v != null) device.setOrgId(String.valueOf(v));
      v = updates.get("orgName");
      if (v != null) device.setOrgName(String.valueOf(v));
      v = updates.get("gpsX");
      if (v != null) device.setGpsX(String.valueOf(v));
      v = updates.get("gpsY");
      if (v != null) device.setGpsY(String.valueOf(v));
      v = updates.get("gpsZ");
      if (v != null) device.setGpsZ(String.valueOf(v));
      v = updates.get("remark");
      if (v != null) device.setRemark(String.valueOf(v));
      v = updates.get("deviceIp");
      if (v == null) v = updates.get("ip");
      if (v != null && !registrationAddress) device.setDeviceIp(String.valueOf(v));
      v = updates.get("devicePort");
      if (v == null) v = updates.get("port");
      if (v != null && !registrationAddress) {
        Integer port = parseIntOrNull(v);
        if (port != null) device.setDevicePort(port);
      }
      v = updates.get("manufacturer");
      if (v == null) v = updates.get("vendor");
      if (v != null) device.setManufacturer(String.valueOf(v));
      v = updates.get("deviceModel");
      if (v == null) v = updates.get("model");
      if (v != null) device.setDeviceModel(String.valueOf(v));
      v = updates.get("enabled");
      if (v != null) {
        Integer enabled = parseIntOrNull(v);
        if (enabled == null || !List.of(0, 1).contains(enabled)) throw new BaseException("设备启用状态仅支持0或1");
        device.setEnabled(enabled);
      }
      v = updates.get("deviceStatus");
      if (v == null) v = updates.get("status");
      if (v != null) device.setDeviceStatus(String.valueOf(v));
    }
    device.setUpdateId(SecurityUtils.getUnionId());
    device.setUpdateTime(new Date());
    deviceMapper.updateByPrimaryKeySelective(device);
    alarmContextCache.publishPlatformChange(instanceKey);
    updateSdkDeviceAccessConfig(instanceKey, device, updates);
    updateSdkChannelAddress(instanceKey, device, previousDeviceIp);
    String updatedMediaServerId = updateOnvifDeviceAccessConfig(instanceKey, device, updates);
    updateOnvifChannelAccessConfig(instanceKey, device, updates);
    releaseOldOnvifProxyAfterMediaServerChanged(
        instanceKey, device, previousMediaServerId, updatedMediaServerId, updates);
    VideoPlatformInstance instance = getByInstanceKey(instanceKey);
    if (instance != null && isDirectSdkPlatform(instance.getPlatformType())) {
      applySdkSubscriptionAfterCommit(instanceKey);
    }
    if (instance != null) {
      if ("hik_isup".equals(instance.getPlatformType()) && updates != null && updates.containsKey("enabled")) {
        hikIsupConfiguration.validate(instance);instance.setUpdateTime(new Date());instanceMapper.updateByPrimaryKeySelective(instance);
      }
      requestIsupSyncAfterCommit(instance);
    }
    return getDeviceDetail(instanceKey, deviceId);
  }

  /** SDK 通道使用设备地址播放；修改设备 IP 时同步原地址，保留 NVR 的独立通道地址。 */
  private void updateSdkChannelAddress(
      String instanceKey, VideoPlatformDevice device, String previousIp) {
    VideoPlatformInstance instance = getByInstanceKey(instanceKey);
    if (instance == null || !isDirectSdkPlatform(instance.getPlatformType())
        || StrUtil.isBlank(previousIp) || StrUtil.isBlank(device.getDeviceIp())
        || StrUtil.equals(previousIp, device.getDeviceIp())) return;
    Example example = new Example(VideoPlatformChannel.class);
    example.createCriteria().andEqualTo("instanceKey", instanceKey)
        .andEqualTo("deviceId", device.getId()).andEqualTo("ipAddress", previousIp);
    VideoPlatformChannel update = new VideoPlatformChannel();
    update.setIpAddress(device.getDeviceIp());
    update.setUpdateTime(new Date());
    update.setUpdateId(SecurityUtils.getUnionId());
    channelMapper.updateByExampleSelective(update, example);
  }

  private void updateSdkDeviceAccessConfig(
      String instanceKey, VideoPlatformDevice device, Map<String, Object> updates) {
    VideoPlatformInstance instance = getByInstanceKey(instanceKey);
    if (instance == null || !isDirectSdkPlatform(instance.getPlatformType()) || updates == null) return;
    Example ex = new Example(VideoPlatformDeviceExt.class);
    ex.createCriteria().andEqualTo("instanceKey", instanceKey).andEqualTo("deviceId", device.getId());
    List<VideoPlatformDeviceExt> rows = deviceExtMapper.selectByExample(ex);
    VideoPlatformDeviceExt ext = rows.isEmpty() ? new VideoPlatformDeviceExt() : rows.get(0);
    Map<String, Object> access = readObjectMap(ext.getExtField3());
    if ("instance".equals(stringValue(updates.get("credentialMode")))) {
      access.remove("username");
      access.remove("password");
    } else {
      for (String key : List.of("username", "password")) {
        Object value = updates.get(key);
        if (value != null && StrUtil.isNotBlank(String.valueOf(value)) && !isMaskedSecret(key, value)) {
          access.put(key, value);
        }
      }
    }
    access.put("sdkPort", device.getDevicePort());
    if (updates.containsKey("credentialMode")) access.put("credentialMode", updates.get("credentialMode"));
    try {
      ext.setExtField3(objectMapper.writeValueAsString(access));
    } catch (Exception e) {
      throw new BaseException("保存设备凭据失败");
    }
    ext.setUpdateTime(new Date());
    if (ext.getId() == null) {
      ext.setInstanceKey(instanceKey);
      ext.setDeviceId(device.getId());
      ext.setPlatformDeviceId(device.getDeviceId());
      ext.setTenantId(instance.getTenantId());
      ext.setCreateTime(new Date());
      deviceExtMapper.insertSelective(ext);
    } else {
      deviceExtMapper.updateByPrimaryKeySelective(ext);
    }
  }

  private String updateOnvifDeviceAccessConfig(
      String instanceKey, VideoPlatformDevice device, Map<String, Object> updates) {
    if (device == null || updates == null || updates.isEmpty()) {
      return currentOnvifMediaServerId(device == null ? null : device.getId());
    }
    VideoPlatformInstance instance = getByInstanceKey(instanceKey);
    if (instance == null || !"onvif".equalsIgnoreCase(instance.getPlatformType())) {
      return currentOnvifMediaServerId(device.getId());
    }
    boolean hasOnvifUpdate =
        containsAny(
            updates,
            "xAddr",
            "endpoint",
            "deviceServiceUrl",
            "username",
            "password",
            "authType",
            "deviceType",
            "deviceKind",
            "vendor",
            "remoteChannel",
            "channelTotal",
            "decodePolicy",
            "rtspPort",
            "mediaServerId",
            "previewMediaServerId",
            "ip",
            "deviceIp",
            "port",
            "devicePort");
    if (!hasOnvifUpdate) {
      return currentOnvifMediaServerId(device.getId());
    }

    Example extEx = new Example(VideoPlatformDeviceExt.class);
    extEx.createCriteria().andEqualTo("deviceId", device.getId());
    List<VideoPlatformDeviceExt> extList = deviceExtMapper.selectByExample(extEx);
    VideoPlatformDeviceExt ext = extList.isEmpty() ? new VideoPlatformDeviceExt() : extList.get(0);
    boolean creating = ext.getId() == null;
    if (creating) {
      ext.setDeviceId(device.getId());
      ext.setInstanceKey(instanceKey);
      ext.setPlatformDeviceId(device.getDeviceId());
      ext.setCreateId(SecurityUtils.getUnionId());
      ext.setCreateTime(new Date());
      ext.setTenantId(instance.getTenantId());
      ext.setDeviceType("onvif");
    }

    Map<String, Object> payload = readObjectMap(ext.getExtField3());
    mergeIfPresent(payload, updates, "xAddr");
    mergeAliasIfPresent(payload, updates, "deviceServiceUrl", "endpoint", "deviceServiceUrl");
    mergeIfPresent(payload, updates, "username");
    mergeIfNotBlank(payload, updates, "password");
    mergeIfPresent(payload, updates, "authType");
    mergeIfPresent(payload, updates, "deviceType");
    mergeIfPresent(payload, updates, "deviceKind");
    mergeIfPresent(payload, updates, "vendor");
    mergeIfPresent(payload, updates, "remoteChannel");
    mergeIfPresent(payload, updates, "channelTotal");
    mergeIfPresent(payload, updates, "decodePolicy");
    mergeIfPresent(payload, updates, "rtspPort");
    mergeIfPresent(payload, updates, "mediaServerId");
    mergeAliasIfPresent(payload, updates, "mediaServerId", "previewMediaServerId", "mediaServerId");
    String mediaServerId = stringValue(payload.get("mediaServerId"));
    ext.setMediaServerId(StrUtil.isBlank(mediaServerId) ? null : StrUtil.sub(mediaServerId, 0, 100));

    String ip = firstNonBlank(stringValue(updates.get("deviceIp")), stringValue(updates.get("ip")), device.getDeviceIp());
    Integer port =
        firstNonNullInt(
            parseIntOrNull(updates.get("devicePort")),
            parseIntOrNull(updates.get("port")),
            device.getDevicePort());
    if (StrUtil.isNotBlank(ip)) {
      payload.put("ip", ip);
      payload.put("deviceIp", ip);
    }
    if (port != null) {
      payload.put("port", port);
      payload.put("devicePort", port);
    }
    String xAddr =
        firstNonBlank(
            stringValue(payload.get("xAddr")),
            stringValue(payload.get("deviceServiceUrl")),
            buildOnvifXAddr(ip, port));
    if (StrUtil.isNotBlank(xAddr)) {
      payload.put("xAddr", xAddr);
      payload.putIfAbsent("deviceServiceUrl", xAddr);
      ext.setExtField1(StrUtil.sub(xAddr, 0, 500));
    }
    if (StrUtil.isNotBlank(stringValue(payload.get("deviceServiceUrl")))) {
      ext.setExtField1(StrUtil.sub(stringValue(payload.get("deviceServiceUrl")), 0, 500));
    }
    if (StrUtil.isNotBlank(stringValue(payload.get("mediaServiceUrl")))) {
      ext.setExtField2(StrUtil.sub(stringValue(payload.get("mediaServiceUrl")), 0, 500));
    }
    ext.setExtField3(writeObjectMap(payload));
    ext.setUpdateId(SecurityUtils.getUnionId());
    ext.setUpdateTime(new Date());
    if (creating) {
      deviceExtMapper.insertSelective(ext);
    } else {
      deviceExtMapper.updateByPrimaryKeySelective(ext);
    }
    return ext.getMediaServerId();
  }

  private String currentOnvifMediaServerId(Long devicePrimaryId) {
    if (devicePrimaryId == null) {
      return null;
    }
    Example extEx = new Example(VideoPlatformDeviceExt.class);
    extEx.createCriteria().andEqualTo("deviceId", devicePrimaryId);
    List<VideoPlatformDeviceExt> extList = deviceExtMapper.selectByExample(extEx);
    if (extList.isEmpty()) {
      return null;
    }
    VideoPlatformDeviceExt ext = extList.get(0);
    Map<String, Object> payload = readObjectMap(ext.getExtField3());
    return firstNonBlank(
        stringValue(ext.getMediaServerId()),
        stringValue(payload.get("mediaServerId")),
        stringValue(payload.get("previewMediaServerId")));
  }

  private void releaseOldOnvifProxyAfterMediaServerChanged(
      String instanceKey,
      VideoPlatformDevice device,
      String previousMediaServerId,
      String updatedMediaServerId,
      Map<String, Object> updates) {
    if (device == null
        || updates == null
        || !containsAny(updates, "mediaServerId", "previewMediaServerId")) {
      return;
    }
    String previous = StrUtil.trimToNull(previousMediaServerId);
    String updated = StrUtil.trimToNull(updatedMediaServerId);
    if (StrUtil.isBlank(previous) || StrUtil.equals(previous, updated)) {
      return;
    }
    VideoPlatformInstance instance = getByInstanceKey(instanceKey);
    if (instance == null || !"onvif".equalsIgnoreCase(instance.getPlatformType())) {
      return;
    }
    List<String> streams = buildOnvifOriginProxyStreams(instanceKey, device);
    if (streams.isEmpty()) {
      return;
    }
    runAfterCommit(
        () ->
            releaseOldOnvifProxyStreams(
                instance.getTenantId(), previous, "nexiot-onvif", streams, instanceKey, device.getDeviceId()));
  }

  private List<String> buildOnvifOriginProxyStreams(String instanceKey, VideoPlatformDevice device) {
    if (device == null || StrUtil.isBlank(device.getDeviceId())) {
      return Collections.emptyList();
    }
    Example channelEx = new Example(VideoPlatformChannel.class);
    channelEx
        .createCriteria()
        .andEqualTo("instanceKey", instanceKey)
        .andEqualTo("deviceId", device.getId());
    List<VideoPlatformChannel> channels = channelMapper.selectByExample(channelEx);
    Set<String> channelIds = new LinkedHashSet<>();
    for (VideoPlatformChannel channel : channels) {
      if (StrUtil.isNotBlank(channel.getChannelId())) {
        channelIds.add(channel.getChannelId());
      }
    }
    if (channelIds.isEmpty()) {
      channelIds.add(device.getDeviceId() + ":main");
    }
    Set<String> streams = new LinkedHashSet<>();
    for (String channelId : channelIds) {
      streams.add(buildOriginProxyStreamName(instanceKey, device.getDeviceId(), channelId, "main"));
      streams.add(buildOriginProxyStreamName(instanceKey, device.getDeviceId(), channelId, "sub"));
    }
    return new ArrayList<>(streams);
  }

  private void releaseOldOnvifProxyStreams(
      String tenantId,
      String mediaServerId,
      String app,
      List<String> streams,
      String instanceKey,
      String deviceId) {
    VideoZlmStreamProxyService zlmStreamProxyService = zlmStreamProxyServiceProvider.getIfAvailable();
    if (zlmStreamProxyService == null) {
      log.debug("[ONVIF] 未接入ZLM代理服务，跳过旧节点代理流释放: mediaServerId={}", mediaServerId);
      return;
    }
    for (String stream : streams) {
      try {
        Map<String, String> releaseResult =
            zlmStreamProxyService.releaseStreamProxy(tenantId, mediaServerId, app, stream);
        log.info(
            "[ONVIF] 已请求释放旧ZLM代理流: instanceKey={}, deviceId={}, mediaServerId={}, app={}, stream={}, countHit={}, countClosed={}",
            instanceKey,
            deviceId,
            mediaServerId,
            app,
            stream,
            releaseResult.get("count_hit"),
            releaseResult.get("count_closed"));
      } catch (Exception e) {
        log.warn(
            "[ONVIF] 释放旧ZLM代理流失败: instanceKey={}, deviceId={}, mediaServerId={}, app={}, stream={}, error={}",
            instanceKey,
            deviceId,
            mediaServerId,
            app,
            stream,
            e.getMessage());
      }
    }
  }

  private void runAfterCommit(Runnable action) {
    if (action == null) {
      return;
    }
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      action.run();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            action.run();
          }
        });
  }

  private void updateOnvifChannelAccessConfig(
      String instanceKey, VideoPlatformDevice device, Map<String, Object> updates) {
    if (device == null || updates == null || updates.isEmpty()) {
      return;
    }
    VideoPlatformInstance instance = getByInstanceKey(instanceKey);
    if (instance == null || !"onvif".equalsIgnoreCase(instance.getPlatformType())) {
      return;
    }
    if (!containsAny(
        updates,
        "vendor",
        "remoteChannel",
        "channelTotal",
        "rtspPort",
        "deviceIp",
        "ip",
        "devicePort",
        "port",
        "deviceStatus",
        "status")) {
      return;
    }

    Example channelEx = new Example(VideoPlatformChannel.class);
    channelEx
        .createCriteria()
        .andEqualTo("instanceKey", instanceKey)
        .andEqualTo("deviceId", device.getId());
    List<VideoPlatformChannel> channels = channelMapper.selectByExample(channelEx);
    if (channels.isEmpty()) {
      return;
    }

    String vendor = firstNonBlank(stringValue(updates.get("vendor")), device.getManufacturer(), "onvif");
    Integer rtspPort = firstNonNullInt(parseIntOrNull(updates.get("rtspPort")), 554);
    Integer remoteChannelStart = firstNonNullInt(parseIntOrNull(updates.get("remoteChannel")), 1);
    String ip = firstNonBlank(stringValue(updates.get("deviceIp")), stringValue(updates.get("ip")), device.getDeviceIp());
    Integer port =
        firstNonNullInt(
            parseIntOrNull(updates.get("devicePort")),
            parseIntOrNull(updates.get("port")),
            device.getDevicePort());
    String status = firstNonBlank(stringValue(updates.get("deviceStatus")), stringValue(updates.get("status")));

    int index = 0;
    for (VideoPlatformChannel channel : channels) {
      Map<String, Object> streamConfig = extractChannelStreamConfig(channel);
      int remoteChannel =
          firstNonNullInt(parseIntOrNull(streamConfig.get("remoteChannel")), remoteChannelStart + index);
      streamConfig.put("vendor", vendor);
      streamConfig.put("rtspPort", rtspPort);
      streamConfig.put("remoteChannel", remoteChannel);
      streamConfig.put("channelNo", remoteChannel);
      if (StrUtil.isNotBlank(ip)) {
        channel.setIpAddress(StrUtil.sub(ip, 0, 50));
      }
      if (port != null) {
        channel.setPort(port);
      }
      if (StrUtil.isNotBlank(status)) {
        channel.setChannelStatus(status);
      } else if ("online".equalsIgnoreCase(device.getDeviceStatus())) {
        channel.setChannelStatus("online");
      }
      channel.setManufacturer(StrUtil.sub(vendor, 0, 100));
      channel.setStreamConfig(writeObjectMap(streamConfig));
      channel.setUpdateId(SecurityUtils.getUnionId());
      channel.setUpdateTime(new Date());
      channelMapper.updateByPrimaryKeySelective(channel);
      index++;
    }
  }

  @Override
  @Transactional
  public void deleteDevice(String instanceKey, String deviceId) {
    requireAccessibleInstance(instanceKey);
    Example ex = new Example(VideoPlatformDevice.class);
    ex.createCriteria().andEqualTo("instanceKey", instanceKey).andEqualTo("deviceId", deviceId);
    List<VideoPlatformDevice> list = deviceMapper.selectByExample(ex);
    if (list.isEmpty()) {
      throw new BaseException("设备不存在：" + deviceId);
    }
    VideoPlatformDevice device = list.get(0);
    Long pk = device.getId();
    if (pk != null) {
      Example chEx = new Example(VideoPlatformChannel.class);
      chEx.createCriteria().andEqualTo("deviceId", pk);
      channelMapper.deleteByExample(chEx);

      Example extEx = new Example(VideoPlatformDeviceExt.class);
      extEx.createCriteria().andEqualTo("deviceId", pk);
      deviceExtMapper.deleteByExample(extEx);
    }
    deviceMapper.deleteByPrimaryKey(device.getId());
    alarmContextCache.publishPlatformChange(instanceKey);
  }

  private static class FusionBinding {
    private final String instanceKey;
    private final String deviceId;
    private final String channelId;

    private FusionBinding(String instanceKey, String deviceId, String channelId) {
      this.instanceKey = instanceKey;
      this.deviceId = deviceId;
      this.channelId = channelId;
    }
  }
}
