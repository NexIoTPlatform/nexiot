package cn.universal.admin.video.hosting;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.persistence.base.IoTDevicePostProcessor.Operation;
import cn.universal.persistence.dto.IoTDeviceDTO;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.core.product.metadata.ProductMetadataProvider;
import cn.universal.core.video.hosting.HostingStatus;
import cn.universal.core.video.hosting.VideoCloudHostingCallbackHandler;
import cn.universal.core.video.hosting.VideoCloudHostingProvider;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.query.IoTDeviceQuery;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.utils.SecurityUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 视频云托管业务编排服务。
 *
 * <p>托管会话使用 Redis 保存，设备入库复用标准 IoT 设备表，保证扫码会话、设备权限和播放链路使用同一套数据。
 */
@Service
@Slf4j
public class VideoCloudHostingService implements VideoCloudHostingCallbackHandler {
  private static final Duration SESSION_TTL = Duration.ofMinutes(20);
  private final StringRedisTemplate redis;
  private final ObjectMapper mapper;
  private final IoTProductMapper productMapper;
  private final IoTDeviceMapper deviceMapper;
  private final IoTUserService userService;
  private final RedissonClient redisson;
  private final Map<String, VideoCloudHostingProvider> providers = new ConcurrentHashMap<>();
  @Autowired(required = false)
  private List<ProductMetadataProvider> metadataProviders = List.of();
  @Autowired(required = false)
  private IoTDeviceService deviceLifecycleService;
  @Autowired private IoTCacheInvalidationPublisher cacheInvalidationPublisher;

  public VideoCloudHostingService(StringRedisTemplate redis, ObjectMapper mapper, IoTProductMapper productMapper,
      IoTDeviceMapper deviceMapper, IoTUserService userService, RedissonClient redisson,
      List<VideoCloudHostingProvider> providerList) {
    this.redis = redis; this.mapper = mapper; this.productMapper = productMapper; this.deviceMapper = deviceMapper;
    this.userService = userService; this.redisson = redisson;
    providerList.forEach(p -> providers.put(p.platformType(), p));
  }

  public Map<String,Object> create(String platform, String authorizationUrl) {
    return create(platform, authorizationUrl, null);
  }

  public Map<String,Object> create(String platform, String authorizationUrl, String productKey) {
    VideoCloudHostingProvider provider = provider(platform);
    String unionId = SecurityUtils.getUnionIdQuietly();
    IoTUser user = userService.selectUserByUnionId(unionId);
    if (user == null) throw new IllegalStateException("当前用户不存在");
    IoTProduct product = StrUtil.isBlank(productKey) ? ensureProduct(platform, user, authorizationUrl) : selectedProduct(platform, productKey, user);
    if ("ezviz".equals(platform) && !"VIDEO_DEVICE".equals(product.getDeviceNode())) throw new IllegalArgumentException("请选择视频设备类型的萤石产品");
    String sessionId = UUID.randomUUID().toString().replace("-", "");
    Map<String,Object> session = new HashMap<>();
    session.put("sessionId", sessionId); session.put("platform", platform); session.put("unionId", unionId);
    session.put("tenantId", user.getTenantId()); session.put("productKey", product.getProductKey());
    session.put("status", HostingStatus.WAITING_SCAN); session.put("createdAt", System.currentTimeMillis());
    session.put("expiresAt", System.currentTimeMillis() + SESSION_TTL.toMillis());
    if (StrUtil.isNotBlank(authorizationUrl)) {
      JSONObject cfg = parse(product.getConfiguration());
      // 产品详情中的托管地址优先；请求参数只作为旧页面兼容兜底，避免覆盖管理员配置。
      if (StrUtil.isBlank(cfg.getStr("hostingUrl"))) {
        cfg.set("hostingUrl", authorizationUrl);
        product.setConfiguration(cfg.toString());
        productMapper.updateByPrimaryKeySelective(product);
      }
    }
    VideoCloudHostingProvider.ProductInfo info = info(product);
    // 萤石要求短数字追踪码，内部仍用随机会话 ID；Redis 映射把回调绑定回原租户。
    String traceability = "ezviz".equals(platform) ? allocateEzvizTrace(sessionId) : sessionId;
    session.put("traceability", traceability);
    String url = provider.createAuthorizationUrl(info, traceability);
    session.put("authorizationUrl", url);
    put(sessionId, user.getTenantId(), platform, session);
    return session;
  }

  public Map<String,Object> get(String sessionId) {
    String unionId = SecurityUtils.getUnionIdQuietly();
    String lookup = redis.opsForValue().get(traceKey(sessionId));
    // 前端会在二维码弹窗打开时恢复上一次会话；Redis TTL 到期属于正常状态，
    // 返回过期状态即可，避免全局请求拦截器把它作为业务异常弹给用户。
    if (lookup == null) return expiredSession(sessionId);
    Map<String,Object> meta = read(resolveLookupKey(lookup));
    authorize(meta, unionId);
    Map<String,Object> session = read(key((String)meta.get("tenantId"), (String)meta.get("platform"), sessionId));
    if (session == null) return expiredSession(sessionId);
    // 只有等待扫码阶段会被动标记为过期，导入中的会话由回调锁和状态机保证一致性。
    if (isExpired(session) && HostingStatus.WAITING_SCAN.equals(session.get("status"))) {
      session.put("status", HostingStatus.EXPIRED);
      write(sessionId, String.valueOf(meta.get("tenantId")), String.valueOf(meta.get("platform")), session);
    }
    return session;
  }

  private Map<String, Object> expiredSession(String sessionId) {
    Map<String, Object> session = new HashMap<>();
    session.put("sessionId", sessionId);
    session.put("status", HostingStatus.EXPIRED);
    return session;
  }

  @Transactional
  public Map<String,Object> callback(String sessionId, String platform, VideoCloudHostingProvider.HostingCallback callback) {
    if (StrUtil.isBlank(sessionId) || StrUtil.isBlank(platform) || callback == null
        || !StrUtil.equals(sessionId, callback.traceability())) {
      throw new IllegalArgumentException("托管回调会话标识无效");
    }
    String lookup = redis.opsForValue().get(traceKey(sessionId));
    if (lookup == null) throw new IllegalStateException("托管会话不存在或已过期");
    Map<String,Object> meta = read(resolveLookupKey(lookup));
    if (!StrUtil.equals(platform, String.valueOf(meta.get("platform")))) {
      throw new SecurityException("托管回调平台与会话不匹配");
    }
    String tenantId = (String)meta.get("tenantId");
    Map<String,Object> session = read(key(tenantId, platform, sessionId));
    if (session == null) throw new IllegalStateException("托管会话不存在或已过期");
    if (HostingStatus.COMPLETED.equals(session.get("status"))) return session;
    if (HostingStatus.CANCELLED.equals(session.get("status"))
        || HostingStatus.EXPIRED.equals(session.get("status"))
        || isExpired(session)) {
      session.put("status", HostingStatus.EXPIRED);
      write(sessionId, tenantId, platform, session);
      throw new IllegalStateException("托管会话已过期或已取消");
    }
    // 厂商可能重复推送回调；同一会话必须串行处理，避免重复创建设备或覆盖最终状态。
    RLock callbackLock = redisson.getLock("video:hosting:callback:" + sessionId);
    boolean callbackLockAcquired = false;
    try {
      callbackLockAcquired = callbackLock.tryLock(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("托管回调被中断", e);
    }
    if (!callbackLockAcquired) {
      throw new IllegalStateException("托管回调正在处理中");
    }
    boolean deferUnlock = TransactionSynchronizationManager.isActualTransactionActive()
        && TransactionSynchronizationManager.isSynchronizationActive();
    if (deferUnlock) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override public void afterCompletion(int status) {
        if (callbackLock.isHeldByCurrentThread()) callbackLock.unlock();
      }
    });
    try {
    session = read(key(tenantId, platform, sessionId));
    if (session == null || isExpired(session)) throw new IllegalStateException("托管会话不存在或已过期");
    if (HostingStatus.COMPLETED.equals(session.get("status"))) return session;
    if (HostingStatus.CANCELLED.equals(session.get("status"))) throw new IllegalStateException("托管会话已过期或已取消");
    IoTProduct product = productMapper.getProductByProductKey((String)session.get("productKey"));
    if (product == null || !StrUtil.equals(tenantId, product.getTenantId())) {
      throw new SecurityException("托管产品不属于当前租户");
    }
    VideoCloudHostingProvider provider = provider(platform);
    // warrantInit 只是“用户已授权”的通知，不代表平台侧已经接受托管。
    // 先保存待确认设备，等用户在后台点击接受后才调用厂商接受接口。
    if ("imoulife".equals(platform) && callback.raw() != null
        && "warrantInit".equals(String.valueOf(callback.raw().get("msgType")))) {
      session.put("status", HostingStatus.PENDING_ACCEPT);
      session.put("deviceCount", callback.devices() == null ? 0 : callback.devices().size());
      session.put("pendingDevices", callback.devices() == null ? List.of() : callback.devices().stream()
          .map(d -> hostedView(product, platform, d)).toList());
      write(sessionId, tenantId, platform, session);
      log.info("视频托管回调待用户确认: platform={}, sessionId={}, deviceCount={}",
          platform, sessionId, session.get("deviceCount"));
      return session;
    }
    // 非 warrantInit 回调进入标准授权完成流程，由协议适配器补齐设备详情并执行入库。
    session.put("status", HostingStatus.AUTHORIZED); write(sessionId, tenantId, platform, session);
    List<VideoCloudHostingProvider.HostedDevice> devices = provider.completeAuthorization(info(product), callback);
    if (devices == null) devices = List.of();
    if (devices.isEmpty()) {
      session.put("status", HostingStatus.FAILED);
      session.put("error", "授权回调未返回可入库设备，请确认授权后重新发起托管");
      session.put("failedAt", System.currentTimeMillis());
      write(sessionId, tenantId, platform, session);
      return session;
    }
    session.put("status", HostingStatus.IMPORTING); session.put("deviceCount", devices.size()); write(sessionId, tenantId, platform, session);
    List<Map<String,Object>> imported = devices.stream().map(d -> importDevice(product, platform, sessionId, d, (String)meta.get("unionId"))).toList();
    if (!devices.isEmpty() && StrUtil.isNotBlank(devices.get(0).accessToken())) redis.opsForValue().set(tokenKey(sessionId), devices.get(0).accessToken(), SESSION_TTL);
    session.put("status", HostingStatus.COMPLETED); session.put("devices", imported); session.put("completedAt", System.currentTimeMillis());
    Map<String, Object> completed = new HashMap<>(session);
    if (deferUnlock) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override public void afterCommit() { write(sessionId, tenantId, platform, completed); }
        @Override public void afterCompletion(int status) {
          if (status != STATUS_COMMITTED) {
            completed.put("status", HostingStatus.FAILED);
            completed.put("error", "托管入库未提交，请从托管设备列表重新同步");
            completed.remove("devices");
            write(sessionId, tenantId, platform, completed);
          }
        }
      });
    } else write(sessionId, tenantId, platform, completed);
    return session;
    } catch (RuntimeException error) {
      if (session != null && !HostingStatus.CANCELLED.equals(session.get("status")) && !HostingStatus.EXPIRED.equals(session.get("status"))) {
        session.put("status", HostingStatus.FAILED);
        session.put("error", "托管处理失败，请检查产品配置或从托管设备列表重新同步");
        write(sessionId, tenantId, platform, session);
      }
      throw error;
    } finally {
      if (!deferUnlock && callbackLock.isHeldByCurrentThread()) callbackLock.unlock();
    }
  }

  public Map<String,Object> cancel(String sessionId) {
    RLock lock = redisson.getLock("video:hosting:callback:" + sessionId);
    boolean acquired = false;
    try {
      acquired = lock.tryLock(10, TimeUnit.SECONDS);
      if (!acquired) throw new IllegalStateException("托管回调正在处理中，请稍后取消");
      return cancelLocked(sessionId);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("取消托管被中断", e);
    } finally {
      if (acquired && lock.isHeldByCurrentThread()) lock.unlock();
    }
  }

  private Map<String,Object> cancelLocked(String sessionId) {
    Map<String,Object> session = get(sessionId);
    if (HostingStatus.CANCELLED.equals(session.get("status")) || HostingStatus.EXPIRED.equals(session.get("status"))) return session;
    IoTProduct product = productMapper.getProductByProductKey((String) session.get("productKey"));
    VideoCloudHostingProvider p = provider((String) session.get("platform"));
    Object rawDevices = session.get("devices");
    List<VideoCloudHostingProvider.DeviceRef> refs = new ArrayList<>();
    if (rawDevices instanceof List<?> list) for (Object item : list) if (item instanceof Map<?,?> m) refs.add(new VideoCloudHostingProvider.DeviceRef(String.valueOf(m.get("deviceId")), String.valueOf(m.containsKey("channelId") ? m.get("channelId") : "1")));
    if ("ezviz".equals(session.get("platform")) && !refs.isEmpty()) {
      List<VideoCloudHostingProvider.DeviceRef> channels = new ArrayList<>();
      for (VideoCloudHostingProvider.DeviceRef ref : refs) {
        IoTDevice local = deviceMapper.selectIoTDevice(product.getProductKey(), ref.deviceId());
        if (local == null) continue;
        var list = parse(local.getConfiguration()).getJSONArray("channelList");
        if (list == null || list.isEmpty()) throw new IllegalStateException("托管设备缺少通道，请先同步设备");
        for (Object item : list) channels.add(new VideoCloudHostingProvider.DeviceRef(ref.deviceId(), JSONUtil.parseObj(item).getStr("channelId")));
      }
      refs = channels;
    }
    String providerToken = redis.opsForValue().get(tokenKey(sessionId));
    p.cancelHosting(info(product), providerToken == null ? "" : providerToken, refs);
    for (VideoCloudHostingProvider.DeviceRef ref : refs) {
      IoTDevice device = deviceMapper.selectIoTDevice(product.getProductKey(), ref.deviceId());
      if (device == null) continue;
      JSONObject config = parse(device.getConfiguration()); JSONObject hosting = config.getJSONObject("hosting");
      if (hosting == null) hosting = new JSONObject(); hosting.set("status", HostingStatus.CANCELLED); hosting.set("cancelledAt", System.currentTimeMillis()); config.set("hosting", hosting); device.setConfiguration(config.toString()); deviceMapper.updateByPrimaryKeySelective(device);
      publishDeviceChange(device, false);
    }
    session.put("status", HostingStatus.CANCELLED); write(sessionId, (String)session.get("tenantId"), (String)session.get("platform"), session); return session;
  }

  /**
   * 查询厂商侧托管记录。
   *
   * <p>该列表独立于短生命周期的二维码会话：用户关闭弹窗后，厂商仍可能异步回调，
   * 后台需要通过这里让用户查看并补确认待托管设备。
   */
  public List<Map<String, Object>> listHosted(String platform, String status, int page, int pageSize) {
    return listHosted(platform, null, status, page, pageSize);
  }

  public List<Map<String, Object>> listHosted(String platform, String productKey, String status, int page, int pageSize) {
    IoTUser user = currentUser();
    IoTProduct product = selectedProduct(platform, productKey, user);
    VideoCloudHostingProvider provider = provider(platform);
    List<VideoCloudHostingProvider.HostedDevice> records = new ArrayList<>();
    if (provider.requiresAcceptance() && "all".equalsIgnoreCase(status)) {
      // 厂商没有统一的“全部状态”接口，分别查询三种状态后在本地合并。
      for (String item : List.of("init", "accept", "refuse")) {
        records.addAll(provider.listByAuthStatus(info(product), item, page, pageSize));
      }
    } else {
      records = provider.listByAuthStatus(info(product), StrUtil.blankToDefault(status, "init"), page, pageSize);
    }
    return records.stream().filter(d -> StrUtil.isNotBlank(d.deviceId()))
        .map(d -> hostedView(product, platform, d)).toList();
  }

  /** 接受选中的待托管记录，并以当前用户作为设备所有者完成入库。 */
  @Transactional
  public Map<String, Object> acceptHosted(String platform, List<VideoCloudHostingProvider.DeviceRef> refs) {
    return acceptHosted(platform, null, refs);
  }

  @Transactional
  public Map<String, Object> acceptHosted(String platform, String productKey, List<VideoCloudHostingProvider.DeviceRef> refs) {
    IoTUser user = currentUser();
    IoTProduct product = selectedProduct(platform, productKey, user);
    if (refs == null || refs.isEmpty()) throw new IllegalArgumentException("请选择要接受的托管设备");
    if (refs.size() > 100) throw new IllegalArgumentException("单次最多导入 100 台设备");
    if (!provider(platform).requiresAcceptance()) {
      List<Map<String, Object>> imported = new ArrayList<>();
      Map<String, VideoCloudHostingProvider.HostedDevice> ezvizDevices = new HashMap<>();
      if ("ezviz".equals(platform)) provider(platform).allAuthorizedDevices(info(product)).forEach(d -> ezvizDevices.put(d.deviceId(), d));
      java.util.Set<String> importedIds = new java.util.HashSet<>();
      for (VideoCloudHostingProvider.DeviceRef ref : refs) {
        if (!importedIds.add(ref.deviceId())) continue;
        var hosted = "ezviz".equals(platform) ? ezvizDevices.get(ref.deviceId()) : provider(platform).resolveDevice(info(product), ref);
        if (hosted == null) throw new IllegalArgumentException("设备未授权或授权已撤销，请刷新列表");
        imported.add(importDevice(product, platform, "shared-import", hosted, user.getUnionId()));
      }
      return Map.of("status", HostingStatus.COMPLETED, "acceptedCount", imported.size(), "devices", imported);
    }
    List<VideoCloudHostingProvider.HostedDevice> pending = allStatusDevices(product, platform, "init");
    // 先建立待接受和已接受索引，兼容厂商最终一致性期间同一设备短暂出现在两个列表。
    Map<String, VideoCloudHostingProvider.HostedDevice> byId = new HashMap<>();
    pending.forEach(d -> byId.put(d.deviceId(), d));
    List<VideoCloudHostingProvider.HostedDevice> accepted = allStatusDevices(product, platform, "accept");
    Map<String, VideoCloudHostingProvider.HostedDevice> acceptedById = new HashMap<>();
    accepted.forEach(d -> acceptedById.put(d.deviceId(), d));
    List<VideoCloudHostingProvider.HostedDevice> selected = new ArrayList<>();
    List<VideoCloudHostingProvider.DeviceRef> refsToAccept = new ArrayList<>();
    for (VideoCloudHostingProvider.DeviceRef ref : refs) {
      // 已接受列表优先：如果设备刚被其他请求接受，重复调用接受接口会触发 AU1009。
      VideoCloudHostingProvider.HostedDevice device = acceptedById.get(ref.deviceId());
      if (device == null) {
        device = byId.get(ref.deviceId());
        if (device != null) refsToAccept.add(enrichDeviceRef(ref, device));
      }
      if (device == null) {
        throw new IllegalArgumentException("托管设备不存在或状态已变化，请刷新列表后重试: " + ref.deviceId());
      }
      selected.add(device);
    }
    // 只有仍处于 init 的设备才调用厂商接受接口；已接受设备直接进入本地入库流程。
    if (!refsToAccept.isEmpty()) provider(platform).setHostingStatus(info(product), "accept", refsToAccept);
    String sessionId = "manual-" + UUID.randomUUID().toString().replace("-", "");
    List<Map<String, Object>> imported = selected.stream()
        .map(d -> importDevice(product, platform, sessionId, d, user.getUnionId())).toList();
    return Map.of("status", HostingStatus.COMPLETED, "acceptedCount", imported.size(), "devices", imported);
  }

  VideoCloudHostingProvider.DeviceRef enrichDeviceRef(
      VideoCloudHostingProvider.DeviceRef ref, VideoCloudHostingProvider.HostedDevice device) {
    String productId = ref.productId();
    String channelId = ref.channelId();
    // 前端可能只提交设备编号，优先从厂商原始记录补齐产品和通道，保证接受参数完整。
    if (device != null && device.raw() != null) {
      Object rawProductId = device.raw().get("productId");
      Object rawChannelId = device.raw().get("channelId");
      if (StrUtil.isBlank(productId) && rawProductId != null) productId = String.valueOf(rawProductId);
      if (StrUtil.isBlank(channelId) && rawChannelId != null) channelId = String.valueOf(rawChannelId);
      if (StrUtil.isBlank(channelId)) {
        Object channels = device.raw().get("channels");
        if (channels instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
          Object rawChannel = first.get("channelId");
          if (rawChannel != null) channelId = String.valueOf(rawChannel);
        }
      }
    }
    return new VideoCloudHostingProvider.DeviceRef(ref.deviceId(), StrUtil.blankToDefault(channelId, null),
        StrUtil.blankToDefault(productId, null));
  }

  public VideoCloudHostingProvider.HostingCallback parseCallback(String platform, Map<String,String> query, String body) {
    return provider(platform).parseCallback(query, body);
  }

  private String allocateEzvizTrace(String sessionId) {
    for (int attempt = 0; attempt < 100; attempt++) {
      String trace = String.valueOf(java.util.concurrent.ThreadLocalRandom.current().nextInt(1_000_000, 10_000_000));
      if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent("video:hosting:ezviz:trace:" + trace, sessionId, SESSION_TTL))) return trace;
    }
    throw new IllegalStateException("萤石托管会话繁忙，请重试");
  }

  @Override
  @Transactional
  public void handleQuery(String platform, Map<String, String> query) {
    if (!"ezviz".equals(platform)) { handle(platform, JSONUtil.toJsonStr(query)); return; }
    String code = query.get("auth_code");
    if (StrUtil.isNotBlank(code)) {
      String trace = query.get("traceability");
      // Console probes and delayed callbacks may carry no active session. Acknowledge
      // without importing; an auth_code alone never establishes a tenant binding.
      if (trace == null || !trace.matches("[0-9]{1,7}")) return;
      String sessionId = redis.opsForValue().get("video:hosting:ezviz:trace:" + trace);
      if (sessionId == null) return;
      var parsed = parseCallback(platform, query, null);
      callback(sessionId, platform, new VideoCloudHostingProvider.HostingCallback(
          sessionId, parsed.authorizationCode(), parsed.warrantId(), parsed.devices(), parsed.raw()));
      return;
    }
    String operation = query.get("opt_type");
    if (StrUtil.isBlank(operation)) return; // Console URL reachability probe.
    if (!List.of("device_authorize", "device_cancel").contains(operation)) throw new IllegalArgumentException("未知萤石托管操作");
    String trustId = query.get("deviceTrustId"), serials = query.get("deviceSerials");
    if (StrUtil.isBlank(trustId) || StrUtil.isBlank(serials)) throw new IllegalArgumentException("萤石授权信息回调缺少设备或授权用户");
    // This callback is unsigned and carries no tenant. Only reconcile existing bindings,
    // and verify all changes against a complete, successful vendor inventory.
    java.util.Set<String> productKeys = new java.util.HashSet<>();
    for (String ref : serials.split(",")) {
      String serial = ref.split(":", 2)[0];
      for (IoTDevice device : deviceMapper.select(IoTDevice.builder().deviceId(serial).build())) {
        JSONObject hosting = parse(device.getConfiguration()).getJSONObject("hosting");
        if (hosting != null && platform.equals(hosting.getStr("provider")) && trustId.equals(hosting.getStr("deviceTrustId"))) productKeys.add(device.getProductKey());
      }
    }
    for (String productKey : productKeys) {
      IoTProduct product = productMapper.getProductByProductKey(productKey);
      if (product != null && platform.equals(product.getThirdPlatform())) reconcileAuthorized(product, platform);
    }
  }

  @Override
  public boolean handle(String platform, String body) {
    VideoCloudHostingProvider.HostingCallback callback = parseCallback(platform, Map.of(), body);
    if (callback == null || StrUtil.isBlank(callback.traceability())) return false;
    try {
      Map<String, Object> session = callback(callback.traceability(), platform, callback);
      Object rawDevices = session == null ? null : session.get("deviceCount");
      log.info("视频托管回调已处理: platform={}, sessionId={}, status={}, deviceCount={}",
          platform, callback.traceability(), session == null ? null : session.get("status"), rawDevices);
    } catch (IllegalStateException e) {
      // 厂商会重试回调；对于已过期或已消费的会话直接确认收到，避免同一消息无限重试。
      String message = e.getMessage();
      if (message != null && (message.contains("不存在或已过期") || message.contains("已过期或已取消"))) {
        log.warn("忽略已失效的视频托管回调: platform={}, sessionId={}, reason={}",
            platform, callback.traceability(), message);
        return true;
      }
      throw e;
    }
    return true;
  }

  @Override
  @Transactional
  public boolean handleRevocation(String platform, String body) {
    VideoCloudHostingProvider.HostingCallback callback = parseCallback(platform, Map.of(), body);
    if (callback == null || callback.raw() == null) return false;
    String msgType = String.valueOf(callback.raw().get("msgType"));
    if (!("deviceAuthCancel".equals(msgType) || "authorityRemove".equals(msgType))) return false;
    boolean matched = false;
    if (callback.devices() == null) return true;
    for (VideoCloudHostingProvider.HostedDevice hosted : callback.devices()) {
      if (StrUtil.isBlank(hosted.deviceId())) continue;
      IoTDevice device = deviceMapper.getOneByDeviceId(
          IoTDeviceQuery.builder().deviceId(hosted.deviceId()).build());
      if (device == null) continue;
      IoTProduct product = productMapper.getProductByProductKey(device.getProductKey());
      if (product == null || !StrUtil.equals(platform, product.getThirdPlatform())) continue;
      // 只处理属于当前视频平台且带有托管标记的设备，避免同编号设备被误删。
      JSONObject config = parse(device.getConfiguration());
      JSONObject hosting = config.getJSONObject("hosting");
      if (hosting == null || !StrUtil.equals(platform, hosting.getStr("provider"))) continue;
      String warrantId = callback.warrantId();
      String storedTrustId = hosting.getStr("deviceTrustId");
      if (StrUtil.isNotBlank(warrantId) && StrUtil.isNotBlank(storedTrustId)
          && !StrUtil.equals(warrantId, storedTrustId)) continue;
      if (deviceLifecycleService == null) {
        throw new IllegalStateException("设备生命周期服务不可用，无法清理撤销授权设备");
      }
      if (StrUtil.isBlank(device.getIotId())) {
        throw new IllegalStateException("撤销授权设备缺少统一标识，无法清理: " + hosted.deviceId());
      }
      // 撤销授权意味着本地已经失去合法访问凭据，必须走生命周期服务级联删除设备、影子、
      // 标签和订阅等关联数据，不能只改托管状态留下“僵尸设备”。
      int deleted = deviceLifecycleService.delDevInstance(device.getIotId());
      log.info("乐橙撤销授权设备已清理: platform={}, deviceId={}, iotId={}, deleted={}",
          platform, hosted.deviceId(), device.getIotId(), deleted);
      matched = true;
    }
    return matched || (callback.devices() != null && !callback.devices().isEmpty());
  }

  private Map<String,Object> importDevice(IoTProduct product, String platform, String sessionId, VideoCloudHostingProvider.HostedDevice hosted, String unionId) {
    return withProductLock(product, () -> persistHostedDevice(product, platform, sessionId, hosted, unionId));
  }

  private Map<String,Object> persistHostedDevice(IoTProduct product, String platform, String sessionId, VideoCloudHostingProvider.HostedDevice hosted, String unionId) {
    // 重新托管时厂商列表可能仍是旧快照，入库前主动刷新设备详情，确保最新通道写入 configuration。
    try {
      VideoCloudHostingProvider.HostedDevice refreshed =
          provider(platform).refreshDeviceDetails(info(product), hosted);
      if (refreshed != null) hosted = refreshed;
    } catch (RuntimeException e) {
      if (!provider(platform).requiresAcceptance()) throw e;
      // 通道刷新失败不应阻断授权入库，至少保留回调中的设备和首通道信息。
      log.warn("托管设备通道刷新失败，沿用授权回调数据: platform={}, deviceId={}",
          platform, hosted == null ? null : hosted.deviceId(), e);
    }
    String deviceId = hosted.deviceId();
    if (StrUtil.isBlank(deviceId)) return Map.of("error", "设备序列号为空");
    IoTDevice device = deviceMapper.selectIoTDevice(product.getProductKey(), deviceId);
    boolean created = device == null;
    if (device != null && StrUtil.isNotBlank(device.getTenantId())
        && !StrUtil.equals(product.getTenantId(), device.getTenantId())) throw new SecurityException("设备不属于当前租户");
    if (hosted.raw() != null && Boolean.TRUE.equals(hosted.raw().get("hostingRevoked"))) {
      if (device != null) {
        JSONObject config = parse(device.getConfiguration());
        JSONObject hosting = config.getJSONObject("hosting");
        if (hosting == null) hosting = new JSONObject();
        hosting.set("status", HostingStatus.CANCELLED);
        hosting.set("cancelledAt", System.currentTimeMillis());
        config.set("hosting", hosting);
        device.setConfiguration(config.toString());
        deviceMapper.updateByPrimaryKeySelective(device);
        publishDeviceChange(device, false);
      }
      return Map.of("deviceId", deviceId, "status", HostingStatus.CANCELLED);
    }
    JSONObject cfg = device == null ? new JSONObject() : parse(device.getConfiguration());
    // 托管信息和通道信息统一落在 configuration，播放页可直接读取并在需要时刷新厂商详情。
    JSONObject hosting = new JSONObject(); hosting.set("provider", platform); hosting.set("status", HostingStatus.ACTIVE); hosting.set("sessionId", sessionId); hosting.set("deviceTrustId", hosted.deviceTrustId()); hosting.set("authority", hosted.authority()); hosting.set("managedAt", System.currentTimeMillis()); hosting.set("lastSyncAt", System.currentTimeMillis()); if (!"ezviz".equals(platform) && StrUtil.isNotBlank(hosted.accessToken())) hosting.set("accessToken", hosted.accessToken());
    cfg.set("hosting", hosting);
    List<Map<String, Object>> channels = extractChannelList(hosted);
    if (!channels.isEmpty()) {
      cfg.set("channelList", channels);
      cfg.set("channelNum", channels.size());
      cfg.set("channelId", String.valueOf(channels.get(0).get("channelId")));
    } else if (StrUtil.isNotBlank(hosted.channelId())) {
      cfg.set("channelId", hosted.channelId());
    }
    if (java.util.Set.of("imoulife", "dahua_cloud").contains(platform))
      cn.universal.admin.video.service.VideoCloudDeviceDetailService.mergeSnapshot(cfg, hosted);
    if (device == null) {
      int online = hosted.state() != null && hosted.state() == 1 ? IoTDevice.STATE_ONLINE : IoTDevice.STATE_OFFLINE;
      device = IoTDevice.builder().deviceId(deviceId)
          .deviceName(StrUtil.blankToDefault(hosted.deviceName(), deviceId))
          .productKey(product.getProductKey()).productName(product.getName())
          .iotId(product.getProductKey() + deviceId)
          .deviceSecret(MD5.create().digestHex(product.getProductKey() + deviceId + IoTConstant.SALT))
          .creatorId(unionId).tenantId(product.getTenantId()).gwProductKey(product.getProductKey())
          .state(online == IoTDevice.STATE_ONLINE).configuration(cfg.toString()).createTime(System.currentTimeMillis() / 1000)
          .build();
      deviceMapper.insertUseGeneratedKeys(device);
    }
    else {
      device.setConfiguration(cfg.toString());
      if (hosted.state() != null) device.setState(hosted.state() == 1 ? 1 : 0);
      if (StrUtil.isNotBlank(hosted.deviceName())) device.setDeviceName(hosted.deviceName());
      // 成功授权托管代表当前用户明确接管设备；同步更新历史待入库记录的创建人，
      // 即使启用了部门数据权限，授权用户也能继续查看和使用该设备。
      if (StrUtil.isNotBlank(unionId) && !StrUtil.equals(unionId, device.getCreatorId())) {
        device.setCreatorId(unionId);
      }
      if (StrUtil.isBlank(device.getTenantId())) device.setTenantId(product.getTenantId());
      deviceMapper.updateByPrimaryKeySelective(device);
    }
    publishDeviceChange(device, created);
    Map<String,Object> result = new HashMap<>(); result.put("deviceId", deviceId); result.put("channelId", StrUtil.nullToDefault(hosted.channelId(), "1")); result.put("deviceName", device.getDeviceName()); result.put("devicePk", String.valueOf(device.getId())); return result;
  }

  List<Map<String, Object>> extractChannelList(VideoCloudHostingProvider.HostedDevice hosted) {
    // 不同版本接口可能返回 channelList 或 channels，统一转换成前端使用的字段。
    Map<String, Map<String, Object>> channelsById = new LinkedHashMap<>();
    if (hosted != null && hosted.raw() != null) {
      Object rawChannels = hosted.raw().get("channelList");
      if (rawChannels == null) rawChannels = hosted.raw().get("channels");
      if (rawChannels instanceof Iterable<?> iterable) {
        for (Object item : iterable) {
          JSONObject channel;
          try {
            channel = JSONUtil.parseObj(item);
          } catch (RuntimeException ignored) {
            continue;
          }
          String channelId = firstNonBlank(channel.get("channelId"), channel.get("id"));
          if (StrUtil.isBlank(channelId)) continue;
          Map<String, Object> normalized = new LinkedHashMap<>();
          normalized.put("channelId", channelId);
          normalized.put(
              "channelName",
              StrUtil.blankToDefault(
                  firstNonBlank(channel.get("channelName"), channel.get("name")),
                  "通道" + channelId));
          copyIfPresent(channel, normalized, "channelStatus");
          copyIfPresent(channel, normalized, "deviceTrustId");
          copyIfPresent(channel, normalized, "cameraStatus");
          copyIfPresent(channel, normalized, "channelPicUrl");
          copyIfPresent(channel, normalized, "channelAbility");
          copyIfPresent(channel, normalized, "shareFunctions");
          copyIfPresent(channel, normalized, "unitType");
          normalized.putAll(cn.universal.core.video.hosting.VideoCloudDeviceSnapshot.channel(channel));
          channelsById.putIfAbsent(channelId, normalized);
        }
      }
      // 有些接口只返回 channelNum，不返回通道明细；不能因为缺少明细就把多通道设备降级成单通道。
      Object rawChannelNum = hosted.raw().get("channelNum");
      int channelNum = rawChannelNum instanceof Number
          ? ((Number) rawChannelNum).intValue()
          : parseInt(rawChannelNum);
      if (!Boolean.TRUE.equals(hosted.raw().get("strictChannels")) && channelsById.isEmpty() && channelNum > 1) {
        for (int index = 0; index < channelNum; index++) {
          String channelId = String.valueOf(index);
          if (channelsById.containsKey(channelId)) continue;
          Map<String, Object> channel = new LinkedHashMap<>();
          channel.put("channelId", channelId);
          channel.put("channelName", "通道" + (index + 1));
          channelsById.put(channelId, channel);
        }
      }
    }
    if (channelsById.isEmpty() && hosted != null && StrUtil.isNotBlank(hosted.channelId())) {
      Map<String, Object> channel = new LinkedHashMap<>();
      channel.put("channelId", hosted.channelId());
      channel.put(
          "channelName",
          StrUtil.blankToDefault(hosted.channelName(), "通道" + hosted.channelId()));
      channelsById.put(hosted.channelId(), channel);
    }
    return new ArrayList<>(channelsById.values());
  }

  private static String firstNonBlank(Object... values) {
    for (Object value : values) {
      if (value != null && StrUtil.isNotBlank(String.valueOf(value))) return String.valueOf(value);
    }
    return null;
  }

  private static int parseInt(Object value) {
    if (value == null) return 0;
    try { return Integer.parseInt(String.valueOf(value)); }
    catch (NumberFormatException ignored) { return 0; }
  }

  private static void copyIfPresent(
      JSONObject source, Map<String, Object> target, String key) {
    if (source.containsKey(key) && source.get(key) != null) target.put(key, source.get(key));
  }

  private IoTProduct ensureProduct(String platform, IoTUser user, String authorizationUrl) {
    RLock lock = redisson.getLock("video:hosting:product:" + StrUtil.nullToEmpty(user.getTenantId()) + ":" + platform);
    boolean acquired = false;
    try {
    acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);
    if (!acquired) throw new IllegalStateException("托管产品正在创建，请稍后重试");
    List<IoTProduct> products = productMapper.select(IoTProduct.builder().thirdPlatform(platform).tenantId(user.getTenantId()).build());
    for (IoTProduct p : products) {
      JSONObject config = parse(p.getConfiguration());
      JSONObject hosting = config.getJSONObject("videoHosting");
      if ((hosting != null && hosting.getBool("enabled", false))
          || config.getBool("videoHosting", false)
          || (p.getName() != null && p.getName().contains("托管"))) {
        // 视频墙只查询 VIDEO_DEVICE 产品。历史托管产品可能被创建成普通 DEVICE，
        // 导致设备虽然存在于 iot_device，却不会出现在视频设备列表中，这里统一修正。
        if (!StrUtil.equals("VIDEO_DEVICE", p.getDeviceNode())) {
          p.setDeviceNode("VIDEO_DEVICE");
          productMapper.updateByPrimaryKeySelective(p);
        }
        return p;
      }
    }
    String key = (platform.equals("ezviz") ? "ezviz_host_" : "imou_host_") + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    JSONObject cfg = new JSONObject(); JSONObject hosting = new JSONObject(); hosting.set("enabled", true);
    if (StrUtil.isNotBlank(authorizationUrl)) { hosting.set("authorizationUrl", authorizationUrl); cfg.set("hostingUrl", authorizationUrl); }
    cfg.set("videoHosting", hosting);
    IoTProduct p = IoTProduct.builder().productId(key).productKey(key)
        .name(platform.equals("ezviz") ? "萤石云托管设备" : "乐橙云托管设备")
        .thirdPlatform(platform).transportProtocol(platform).deviceNode("VIDEO_DEVICE")
        .creatorId(user.getUnionId()).tenantId(user.getTenantId()).configuration(cfg.toString())
        .thirdConfiguration("{}").metadata(metadataFor(platform)).storePolicy("MYSQL")
        .createTime(new java.util.Date()).build();
    productMapper.insertDevProduct(p); return p;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("创建托管产品被中断", e);
    } finally {
      if (acquired && lock.isHeldByCurrentThread()) lock.unlock();
    }
  }
  private IoTUser currentUser() {
    IoTUser user = userService.selectUserByUnionId(SecurityUtils.getUnionIdQuietly());
    if (user == null) throw new IllegalStateException("当前用户不存在");
    return user;
  }


  private List<VideoCloudHostingProvider.HostedDevice> allStatusDevices(IoTProduct product, String platform, String status) {
    List<VideoCloudHostingProvider.HostedDevice> result = new ArrayList<>();
    java.util.Set<String> seen = new java.util.HashSet<>();
    for (int page = 1; page <= 1000; page++) {
      List<VideoCloudHostingProvider.HostedDevice> batch = provider(platform).listByAuthStatus(info(product), status, page, 100);
      if (batch.isEmpty()) return result;
      boolean added = false;
      for (VideoCloudHostingProvider.HostedDevice device : batch) {
        if (seen.add(device.deviceId() + ":" + device.channelId())) { result.add(device); added = true; }
      }
      if (batch.size() < 100 || !added) return result;
    }
    throw new IllegalStateException("托管列表分页超出范围");
  }

  private IoTProduct selectedProduct(String platform, String productKey, IoTUser user) {
    if (StrUtil.isBlank(productKey)) {
      if ("dahua_cloud".equals(platform)) throw new IllegalArgumentException("请选择大华云产品");
      return hostingProduct(platform, user);
    }
    IoTProduct product = productMapper.getProductByProductKey(productKey);
    if (product == null || !StrUtil.equals(platform, product.getThirdPlatform())
        || StrUtil.isBlank(user.getTenantId()) || !StrUtil.equals(user.getTenantId(), product.getTenantId()))
      throw new SecurityException("无权访问该视频产品");
    return product;
  }

  public Map<String, Object> authorizedPage(String platform, String productKey, int page, int size) {
    IoTProduct product = selectedProduct(platform, productKey, currentUser());
    var result = provider(platform).authorizedPage(info(product), page, size);
    return Map.of("records", result.records().stream().map(d -> hostedView(product, platform, d)).toList(),
        "page", result.page(), "totalPages", result.totalPages(), "total", result.total());
  }

  public Map<String, Object> authorization(String platform, String productKey) {
    IoTProduct product = selectedProduct(platform, productKey, currentUser());
    return Map.of("authorizationUrl", provider(platform).createAuthorizationUrl(info(product), ""),
        "requiresAcceptance", provider(platform).requiresAcceptance());
  }

  @Transactional
  public Map<String, Object> bindDevice(String platform, String productKey, String deviceId, String password) {
    IoTUser user = currentUser();
    IoTProduct product = selectedProduct(platform, productKey, user);
    return importDevice(product, platform, "serial-bind", provider(platform).bindDevice(info(product), deviceId, password), user.getUnionId());
  }

  @Transactional
  public Map<String, Object> cancelEzvizDevice(String productKey, String deviceId) {
    IoTProduct product = selectedProduct("ezviz", productKey, currentUser());
    var device = provider("ezviz").resolveDevice(info(product), new VideoCloudHostingProvider.DeviceRef(deviceId, null));
    var refs = extractChannelList(device).stream().map(c -> new VideoCloudHostingProvider.DeviceRef(deviceId, String.valueOf(c.get("channelId")))).toList();
    provider("ezviz").cancelHosting(info(product), "", refs);
    return reconcileAuthorized(product, "ezviz");
  }

  /** Only a complete successful cloud inventory can mark a local authorization unavailable. */
  @Transactional
  public Map<String, Object> syncAuthorized(String platform, String productKey) {
    IoTUser user = currentUser();
    IoTProduct product = selectedProduct(platform, productKey, user);
    return reconcileAuthorized(product, platform);
  }

  private Map<String, Object> reconcileAuthorized(IoTProduct product, String platform) {
    return withProductLock(product, () -> {
    List<VideoCloudHostingProvider.HostedDevice> authorized = provider(platform).allAuthorizedDevices(info(product));
    Map<String, VideoCloudHostingProvider.HostedDevice> byId = new HashMap<>();
    authorized.forEach(d -> byId.put(d.deviceId(), d));
    int updated = 0, unavailable = 0;
    List<IoTDevice> local = deviceMapper.select(IoTDevice.builder().productKey(product.getProductKey()).tenantId(product.getTenantId()).build());
    for (IoTDevice device : local) {
      JSONObject cfg = parse(device.getConfiguration());
      JSONObject hosting = cfg.getJSONObject("hosting");
      if (hosting == null || !platform.equals(hosting.getStr("provider"))) continue;
      VideoCloudHostingProvider.HostedDevice remote = byId.get(device.getDeviceId());
      if (remote != null) {
        cfg.set("channelList", extractChannelList(remote));
        cfg.set("channelNum", extractChannelList(remote).size());
        hosting.set("status", HostingStatus.ACTIVE); device.setState(remote.state() != null && remote.state() == 1 ? 1 : 0); updated++;
      } else {
        hosting.set("status", "UNAVAILABLE"); cfg.set("channelList", List.of());
        cfg.set("channelNum", 0); device.setState(0); unavailable++;
      }
      hosting.set("lastSyncAt", System.currentTimeMillis()); cfg.set("hosting", hosting);
      device.setConfiguration(cfg.toString()); deviceMapper.updateByPrimaryKeySelective(device);
      publishDeviceChange(device, false);
    }
    return Map.of("updated", updated, "unavailable", unavailable, "authorized", authorized.size());
    });
  }

  /** Keep the product lock through commit so another importer cannot read uncommitted rows. */
  private <T> T withProductLock(IoTProduct product, Supplier<T> work) {
    RLock lock = redisson.getLock("video:hosting:import:" + product.getTenantId() + ":" + product.getProductKey());
    boolean acquired = false;
    boolean deferred = false;
    try {
      acquired = lock.tryLock(10, TimeUnit.SECONDS);
      if (!acquired) throw new IllegalStateException("该产品正在导入或同步，请稍后重试");
      if (TransactionSynchronizationManager.isActualTransactionActive()
          && TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
          @Override public void afterCompletion(int status) {
            if (lock.isHeldByCurrentThread()) lock.unlock();
          }
        });
        deferred = true;
      }
      return work.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("设备导入被中断", e);
    } finally {
      if (acquired && !deferred && lock.isHeldByCurrentThread()) lock.unlock();
    }
  }

  private void publishDeviceChange(IoTDevice device, boolean created) {
    IoTDeviceDTO dto = deviceMapper.selectIoTDeviceBO(Map.of("productKey", device.getProductKey(),
        "deviceId", device.getDeviceId()));
    if (dto == null) {
      dto = BeanUtil.toBean(device, IoTDeviceDTO.class);
      dto.setDevConfiguration(device.getConfiguration());
      if (StrUtil.isBlank(dto.getThirdPlatform())) {
        IoTProduct product = productMapper.getProductByProductKey(device.getProductKey());
        if (product != null) dto.setThirdPlatform(product.getThirdPlatform());
      }
    }
    cacheInvalidationPublisher.publishDeviceChange(
        created ? Operation.CREATE : Operation.UPDATE, dto);
  }

  private IoTProduct hostingProduct(String platform, IoTUser user) {
    List<IoTProduct> products = productMapper.select(
        IoTProduct.builder().thirdPlatform(platform).tenantId(user.getTenantId()).build());
    return products.stream().filter(p -> {
      JSONObject cfg = parse(p.getConfiguration());
      JSONObject hosting = cfg.getJSONObject("videoHosting");
      return (hosting != null && hosting.getBool("enabled", false))
          || cfg.getBool("videoHosting", false)
          || (p.getName() != null && p.getName().contains("托管"));
    }).findFirst().orElseThrow(() -> new IllegalStateException("当前租户尚未配置" + platform + "托管产品"));
  }

  private Map<String, Object> hostedView(IoTProduct product, String platform,
      VideoCloudHostingProvider.HostedDevice hosted) {
    IoTDevice device = deviceMapper.selectIoTDevice(product.getProductKey(), hosted.deviceId());
    JSONObject config = device == null ? new JSONObject() : parse(device.getConfiguration());
    JSONObject hosting = config.getJSONObject("hosting");
    String persisted = hosting == null ? null : hosting.getStr("status");
    Object rawVendorStatus = null;
    if (hosted.raw() != null) {
      rawVendorStatus = hosted.raw().get("authStatus");
      if (rawVendorStatus == null) rawVendorStatus = hosted.raw().get("status");
    }
    String vendorStatus = rawVendorStatus == null ? null : String.valueOf(rawVendorStatus);
    Map<String, Object> result = new HashMap<>();
    result.put("deviceId", hosted.deviceId());
    result.put("productId", hosted.raw() == null ? null : hosted.raw().get("productId"));
    result.put("channelId", StrUtil.nullToDefault(hosted.channelId(), "0"));
    result.put("deviceName", StrUtil.blankToDefault(hosted.deviceName(), hosted.deviceId()));
    result.put("authority", hosted.authority());
    result.put("source", hosted.raw() == null ? null : hosted.raw().get("source"));
    result.put("model", hosted.model());
    result.put("channels", extractChannelList(hosted));
    result.put("authStatus", StrUtil.blankToDefault(vendorStatus, "unknown"));
    result.put("status", StrUtil.equals(HostingStatus.ACTIVE, persisted) ? "IMPORTED" : "PENDING");
    result.put("devicePk", device == null ? null : String.valueOf(device.getId()));
    result.put("productKey", product.getProductKey());
    result.put("platform", platform);
    result.put("onlineStatus", hosted.state());
    return result;
  }
  private VideoCloudHostingProvider provider(String platform) { VideoCloudHostingProvider p = providers.get(platform); if (p == null) throw new IllegalArgumentException("不支持的视频托管平台: "+platform); return p; }
  private VideoCloudHostingProvider.ProductInfo info(IoTProduct p) { return new VideoCloudHostingProvider.ProductInfo(p.getProductKey(), p.getConfiguration(), p.getThirdConfiguration()); }
  private JSONObject parse(String raw) { try { return StrUtil.isBlank(raw) ? new JSONObject() : JSONUtil.parseObj(raw); } catch (Exception e) { return new JSONObject(); } }
  private String metadataFor(String platform) {
    if (metadataProviders != null) for (ProductMetadataProvider provider : metadataProviders) {
      if (provider != null && StrUtil.equalsIgnoreCase(platform, provider.getProtocolCode())
          && StrUtil.isNotBlank(provider.getMetadata())) return provider.getMetadata();
    }
    return "{}";
  }
  private String key(String tenant,String platform,String session) { return "video:hosting:"+StrUtil.nullToEmpty(tenant)+":"+platform+":"+session; }
  private String traceKey(String session) { return "video:hosting:trace:"+session; }
  private String tokenKey(String session) { return "video:hosting:token:" + session; }
  private void put(String session,String tenant,String platform,Map<String,Object> value) { write(session,tenant,platform,value); redis.opsForValue().set(traceKey(session), tenant+"|"+platform+"|"+session, SESSION_TTL); }
  private void write(String session,String tenant,String platform,Map<String,Object> value) { try { redis.opsForValue().set(key(tenant,platform,session), mapper.writeValueAsString(value), SESSION_TTL); } catch(Exception e) { throw new IllegalStateException("托管会话写入失败",e); } }
  private Map<String,Object> read(String key) { try { String raw = redis.opsForValue().get(key); return raw == null ? null : mapper.readValue(raw, new TypeReference<>(){}); } catch(Exception e) { throw new IllegalStateException("托管会话读取失败",e); } }
  private String resolveLookupKey(String lookup) { String[] parts = lookup.split("\\|", 3); if (parts.length != 3) throw new IllegalStateException("托管会话索引损坏"); return key(parts[0], parts[1], parts[2]); }
  private void authorize(Map<String,Object> session,String unionId) { if (!StrUtil.equals(unionId, (String)session.get("unionId"))) throw new SecurityException("无权访问该托管会话"); }

  /** Redis 中的 JSON 数值在不同版本可能反序列化为数字或字符串，这里统一转换。 */
  private boolean isExpired(Map<String, Object> session) {
    Object raw = session == null ? null : session.get("expiresAt");
    if (raw instanceof Number number) return System.currentTimeMillis() > number.longValue();
    if (raw instanceof CharSequence text) {
      try { return System.currentTimeMillis() > Long.parseLong(text.toString()); }
      catch (NumberFormatException ignored) { return true; }
    }
    return true;
  }
}
