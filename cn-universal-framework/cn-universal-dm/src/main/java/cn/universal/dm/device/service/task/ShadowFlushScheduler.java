package cn.universal.dm.device.service.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.config.InstanceIdProvider;
import cn.universal.common.constant.IoTConstant;
import cn.universal.dm.device.util.DistributedLockUtil;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTDeviceShadow;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTDeviceShadowMapper;
import cn.universal.persistence.query.IoTDeviceQuery;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 设备影子定时缓存
 *
 * <p>Periodically flush device shadows from Redis to DB. Uses Redis ZSET (score = nextFlushAtMs)
 * and Redisson distributed lock.
 */
@Slf4j
@Component
public class ShadowFlushScheduler {

  @Resource private StringRedisTemplate stringRedisTemplate;
  @Resource private IoTDeviceShadowMapper ioTDeviceShadowMapper;
  @Resource private IoTDeviceMapper ioTDeviceMapper;
  @Resource private DistributedLockUtil distributedLockUtil;

  @Value("${shadow.cache.enabled:true}")
  private boolean shadowCacheEnabled;

  @Value("${shadow.cache.key-prefix:shadow}")
  private String shadowKeyPrefix;

  @Value("${shadow.persisted-marker.key-prefix:shadow:persisted}")
  private String shadowPersistedMarkerKeyPrefix;

  @Value("${shadow.flush.enabled:true}")
  private boolean flushEnabled;

  @Value("${shadow.flush.zset-key:shadow:flush}")
  private String shadowFlushZsetKey;

  @Value("${shadow.flush.scan-interval-ms:300000}") // 5 minutes
  private long scanIntervalMs;

  @Value("${shadow.flush.batch-size:1000}")
  private int batchSize;

  @Value("${shadow.flush.lock-key:shadow:flush:lock}")
  private String lockKey;

  @Value("${shadow.flush.lock-wait-time:10}")
  private long lockWaitTime;

  @Value("${shadow.flush.lock-lease-time:60}")
  private long lockLeaseTime;

  @Value("${shadow.flush.max-retries:3}")
  private int maxRetries;

  @Value("${shadow.flush.missing-device-retry-delay-ms:300000}")
  private long missingDeviceRetryDelayMs;

  @Resource private InstanceIdProvider instanceIdProvider;

  @Scheduled(fixedDelayString = "${shadow.flush.scan-interval-ms:300000}")
  public void flushDueShadows() {
    if (!shadowCacheEnabled || !flushEnabled) {
      log.debug(
          "[ShadowFlush] 影子刷盘已禁用: shadowCacheEnabled={}, flushEnabled={}",
          shadowCacheEnabled,
          flushEnabled);
      return;
    }
    MDC.put(IoTConstant.TRACE_ID, IdUtil.objectId());
    try {
      long now = System.currentTimeMillis();
      String instanceId = instanceIdProvider.getInstanceId();
      log.debug("[ShadowFlush] 开始设备影子扫描: scanTime={}, instanceId={}", now, instanceId);

      // 使用分布式锁工具类执行任务，确保集群环境下只有一个实例执行
      Integer result =
          distributedLockUtil.tryLockAndExecute(
              lockKey, lockWaitTime, lockLeaseTime, TimeUnit.SECONDS, this::executeFlushTask);

      if (result != null) {
        log.debug(
            "[ShadowFlush] 影子扫描完成: processed={}, cost={}ms, instanceId={}",
            result,
            (System.currentTimeMillis() - now),
            instanceId);
      } else {
        log.debug("[ShadowFlush] 获取分布式锁失败，跳过本次扫描: instanceId={}", instanceId);
      }
    } catch (Exception e) {
      log.error("刷盘影子报错,error={}", e.getMessage(), e);
    } finally {
      MDC.remove(IoTConstant.TRACE_ID);
    }
  }

  private String buildShadowKey(String iotId) {
    return shadowKeyPrefix + ":" + iotId;
  }

  private String buildPersistedMarkerKey(String iotId) {
    return shadowPersistedMarkerKeyPrefix + ":" + iotId;
  }

  /** 获取缓存数据，并区分缓存不存在与读取失败。 */
  private CacheReadResult getCacheData(Set<String> iotIds) {
    Map<String, String> result = new HashMap<>();
    Set<String> failedIotIds = new LinkedHashSet<>();

    for (String iotId : iotIds) {
      try {
        String cacheJson = stringRedisTemplate.opsForValue().get(buildShadowKey(iotId));
        if (StrUtil.isNotBlank(cacheJson)) {
          result.put(iotId, cacheJson);
        }
      } catch (Exception e) {
        failedIotIds.add(iotId);
        log.warn("[ShadowFlush] get cache error iotId={}, err={}", iotId, e.getMessage());
      }
    }

    return new CacheReadResult(result, failedIotIds);
  }

  private static class CacheReadResult {

    private final Map<String, String> cacheData;
    private final Set<String> failedIotIds;

    private CacheReadResult(Map<String, String> cacheData, Set<String> failedIotIds) {
      this.cacheData = cacheData;
      this.failedIotIds = failedIotIds;
    }
  }

  /** 处理单个设备影子 - 带重试机制 */
  private boolean processShadowWithRetry(String iotId, String cacheJson) {
    for (int retry = 0; retry < maxRetries; retry++) {
      try {
        return processShadow(iotId, cacheJson);
      } catch (Exception e) {
        if (retry == maxRetries - 1) {
          log.error("[ShadowFlush] final retry failed iotId={}, err={}", iotId, e.getMessage());
          return false;
        } else {
          log.warn("[ShadowFlush] retry {} failed iotId={}, err={}", retry, iotId, e.getMessage());
          try {
            Thread.sleep(1000); // 短暂延迟后重试
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
          }
        }
      }
    }
    return false;
  }

  /** 处理单个设备影子 */
  private boolean processShadow(String iotId, String cacheJson) {
    IoTDeviceShadow existed =
        ioTDeviceShadowMapper.selectOne(IoTDeviceShadow.builder().iotId(iotId).build());

    if (existed == null) {
      IoTDeviceShadow entity = buildNewShadow(iotId, cacheJson, new Date());
      if (entity == null) {
        if (!discardShadowIfDeviceDeleted(iotId)) {
          requeueShadowForRetry(iotId, "device identity not ready");
        }
        return false;
      }
      ioTDeviceShadowMapper.insertSelective(entity);
    } else {
      String merged = mergeShadowJson(existed.getMetadata(), cacheJson, existed);
      existed.setMetadata(merged);
      existed.setUpdateDate(new Date());
      existed.setLastTime(new Date());

      ioTDeviceShadowMapper.updateByPrimaryKeySelective(existed);
    }
    return true;
  }

  /** 批量处理设备影子 - 优化数据库操作 */
  private int processBatchShadows(List<String> iotIds, Map<String, String> cacheData) {
    if (CollUtil.isEmpty(iotIds)) {
      return 0;
    }
    try {
      // 批量查询已存在的记录
      List<IoTDeviceShadow> existingShadows = ioTDeviceShadowMapper.selectByIotIds(iotIds);
      Map<String, IoTDeviceShadow> existingMap =
          existingShadows.stream()
              .collect(Collectors.toMap(IoTDeviceShadow::getIotId, Function.identity()));

      // 分离需要插入和更新的记录
      List<IoTDeviceShadow> toInsert = new ArrayList<>();
      List<IoTDeviceShadow> toUpdate = new ArrayList<>();
      List<String> retryIotIds = new ArrayList<>();
      Date now = new Date();

      for (String iotId : iotIds) {
        String cacheJson = cacheData.get(iotId);
        if (StrUtil.isBlank(cacheJson)) {
          continue;
        }
        if (existingMap.containsKey(iotId)) {
          IoTDeviceShadow updated = buildUpdatedShadow(existingMap.get(iotId), cacheJson, now);
          toUpdate.add(updated);
        } else {
          IoTDeviceShadow created = buildNewShadow(iotId, cacheJson, now);
          if (created != null) {
            toInsert.add(created);
          } else if (discardShadowIfDeviceDeleted(iotId)) {
            continue;
          } else {
            retryIotIds.add(iotId);
          }
        }
      }
      // 批量插入
      if (!toInsert.isEmpty()) {
        ioTDeviceShadowMapper.batchInsert(toInsert);
      }

      // 批量更新
      if (!toUpdate.isEmpty()) {
        ioTDeviceShadowMapper.batchUpdate(toUpdate);
      }

      retryIotIds.forEach(iotId -> requeueShadowForRetry(iotId, "device identity not ready"));

      return toInsert.size() + toUpdate.size();

    } catch (Exception e) {
      log.error("[ShadowFlush] batch process error: {}", e.getMessage(), e);
      // 回退到单个处理
      int processed = 0;
      for (String iotId : iotIds) {
        String cacheJson = cacheData.get(iotId);
        if (StrUtil.isNotBlank(cacheJson) && processShadowWithRetry(iotId, cacheJson)) {
          processed++;
        }
      }
      return processed;
    }
  }

  private IoTDeviceShadow buildUpdatedShadow(IoTDeviceShadow existed, String cacheJson, Date now) {
    if (existed == null) {
      return null;
    }
    String merged = mergeShadowJson(existed.getMetadata(), cacheJson, existed);
    existed.setMetadata(merged);
    existed.setUpdateDate(now);
    existed.setLastTime(now);
    return existed;
  }

  private IoTDeviceShadow buildNewShadow(String iotId, String cacheJson, Date now) {
    IoTDeviceShadow entity = new IoTDeviceShadow();
    entity.setIotId(iotId);
    entity.setMetadata(cacheJson);
    applyShadowVersion(entity, cacheJson);
    entity.setUpdateDate(now);
    entity.setLastTime(now);
    entity.setActiveTime(now);
    IoTDevice dev = null;
    boolean deviceLookupCompleted = false;
    try {
      IoTDeviceQuery query = new IoTDeviceQuery();
      query.setIotId(iotId);
      dev = ioTDeviceMapper.getOneByIotId(query);
      deviceLookupCompleted = true;
      if (dev != null) {
        entity.setProductKey(dev.getProductKey());
        entity.setDeviceId(dev.getDeviceId());
        entity.setExtDeviceId(dev.getExtDeviceId());
        entity.setInstance(dev.getApplication());
        if (dev.getOnlineTime() != null) {
          entity.setOnlineTime(DateUtil.date(dev.getOnlineTime() * 1000));
        }
      }
    } catch (Exception ignore) {
      log.error("buildNewShadow error={}", ignore.getMessage(), ignore);
    }
    if (!hasRequiredIdentity(entity)) {
      if (deviceLookupCompleted && dev == null) {
        return null;
      }
      log.warn(
          "[ShadowFlush] skip shadow insert because device identity is incomplete: iotId={}, productKey={}, deviceId={}",
          entity.getIotId(),
          entity.getProductKey(),
          entity.getDeviceId());
      return null;
    }
    return entity;
  }

  /**
   * Clears shadow Redis data only after an authoritative device lookup confirms that the device was
   * deleted. This handles stale entries created before device deletion started evicting shadow data.
   */
  private boolean discardShadowIfDeviceDeleted(String iotId) {
    try {
      IoTDeviceQuery query = new IoTDeviceQuery();
      query.setIotId(iotId);
      if (ioTDeviceMapper.getOneByIotId(query) != null) {
        return false;
      }
      stringRedisTemplate.delete(buildShadowKey(iotId));
      stringRedisTemplate.delete(buildPersistedMarkerKey(iotId));
      stringRedisTemplate.opsForZSet().remove(shadowFlushZsetKey, iotId);
      log.info("[ShadowFlush] discard stale shadow for deleted device: iotId={}", iotId);
      return true;
    } catch (Exception e) {
      log.warn(
          "[ShadowFlush] could not confirm deleted device, keep shadow for retry: iotId={}, err={}",
          iotId,
          e.getMessage());
      return false;
    }
  }

  private boolean hasRequiredIdentity(IoTDeviceShadow shadow) {
    return shadow != null
        && StrUtil.isNotBlank(shadow.getIotId())
        && StrUtil.isNotBlank(shadow.getProductKey())
        && StrUtil.isNotBlank(shadow.getDeviceId());
  }

  private void requeueShadowForRetry(String iotId, String reason) {
    if (StrUtil.isBlank(iotId)) {
      return;
    }
    try {
      long nextFlushAt = System.currentTimeMillis() + Math.max(1000L, missingDeviceRetryDelayMs);
      stringRedisTemplate.opsForZSet().add(shadowFlushZsetKey, iotId, (double) nextFlushAt);
      log.warn(
          "[ShadowFlush] requeue shadow for retry: iotId={}, nextFlushAt={}, reason={}",
          iotId,
          nextFlushAt,
          reason);
    } catch (Exception e) {
      log.error(
          "[ShadowFlush] failed to requeue shadow: iotId={}, reason={}, err={}",
          iotId,
          reason,
          e.getMessage(),
          e);
    }
  }

  /**
   * 将数据库中的影子 JSON 与缓存中的影子 JSON 进行深度合并，避免覆盖丢失。 规则： - 对象与对象递归合并； - 非对象类型以 incoming 覆盖 existing； -
   * 数组整体覆盖； - 针对顶层的 timestamp/version，取二者较大值。
   */
  private String mergeShadowJson(String existingJson, String incomingJson, IoTDeviceShadow shadow) {
    if (StrUtil.isBlank(incomingJson)) {
      return StrUtil.blankToDefault(existingJson, "{}");
    }
    if (StrUtil.isBlank(existingJson)) {
      applyShadowVersion(shadow, incomingJson);
      return incomingJson;
    }
    JSONObject existing;
    JSONObject incoming;
    try {
      existing = JSONUtil.parseObj(existingJson);
    } catch (Exception e) {
      existing = new JSONObject();
    }
    try {
      incoming = JSONUtil.parseObj(incomingJson);
    } catch (Exception e) {
      // 如果新数据都无法解析，直接返回旧数据
      log.warn("[ShadowFlush]新数据都无法解析，直接返回旧数据 error: {}", incomingJson, e);
      return existingJson;
    }

    JSONObject merged = deepMergeObject(existing, incoming);

    // 顶层特殊字段处理
    try {
      Long tsExisting = merged.getLong("timestamp");
      Long tsIncoming = incoming.getLong("timestamp");
      if (tsExisting != null || tsIncoming != null) {
        long maxTs =
            Math.max(tsExisting == null ? 0L : tsExisting, tsIncoming == null ? 0L : tsIncoming);
        merged.set("timestamp", maxTs);
      }
    } catch (Exception ignore) {
      log.warn("[ShadowFlush顶层特殊字段处理] merge shadow json error: {}", incomingJson, ignore);
    }
    try {
      Long verExisting = merged.getLong("version");
      Long verIncoming = incoming.getLong("version");
      if (verExisting != null || verIncoming != null) {
        long maxVer =
            Math.max(
                verExisting == null ? 0L : verExisting, verIncoming == null ? 0L : verIncoming);
        merged.set("version", maxVer);
        shadow.setVersion(maxVer);
      }
    } catch (Exception ignore) {
      log.warn("[ShadowFlush] merge shadow json error: {}", incomingJson);
    }
    return JSONUtil.toJsonStr(merged);
  }

  private void applyShadowVersion(IoTDeviceShadow shadow, String shadowJson) {
    if (shadow == null || StrUtil.isBlank(shadowJson)) {
      return;
    }
    try {
      Long version = JSONUtil.parseObj(shadowJson).getLong("version");
      if (version != null) {
        shadow.setVersion(version);
      }
    } catch (Exception ignore) {
      log.debug("[ShadowFlush] parse shadow version failed: {}", ignore.getMessage());
    }
  }

  /** 对 JSONObject 进行递归深度合并 */
  private JSONObject deepMergeObject(JSONObject base, JSONObject incoming) {
    if (base == null) {
      return incoming == null ? new JSONObject() : incoming;
    }
    if (incoming == null) {
      return base;
    }
    JSONObject result = JSONUtil.parseObj(base.toString());
    for (String key : incoming.keySet()) {
      Object inVal = incoming.get(key);
      Object baseVal = result.get(key);

      if (inVal instanceof JSONObject && baseVal instanceof JSONObject) {
        result.set(key, deepMergeObject((JSONObject) baseVal, (JSONObject) inVal));
      } else {
        // 数组或标量：直接覆盖
        result.set(key, inVal);
      }
    }
    return result;
  }

  /** 执行影子刷新任务 */
  private Integer executeFlushTask() {
    long start = System.currentTimeMillis();
    int processed = 0;
    String instanceId = instanceIdProvider.getInstanceId();

    try {
      long now = System.currentTimeMillis();

      // fetch due iotIds by score <= now
      Set<String> dueIds =
          stringRedisTemplate
              .opsForZSet()
              .rangeByScore(
                  shadowFlushZsetKey, Double.NEGATIVE_INFINITY, (double) now, 0, batchSize);

      if (dueIds == null || dueIds.isEmpty()) {
        log.debug("[ShadowFlush] 没有到期的设备需要刷盘: now={}, instanceId={}", now, instanceId);
        return 0;
      }

      log.info("[ShadowFlush] 发现{}个到期设备需要刷盘, instanceId={}", dueIds.size(), instanceId);

      // 先领取全部到期任务。并发上报会在成员被移除后重新安排后续刷盘时间，
      // 没有对应缓存的孤儿成员则在本轮自然清理，不会永久占据到期队列。
      List<String> claimedIotIds = new ArrayList<>(dueIds);
      Long removedCount =
          stringRedisTemplate.opsForZSet().remove(shadowFlushZsetKey, claimedIotIds.toArray());

      if (removedCount == null || removedCount == 0L) {
        log.warn(
            "[ShadowFlush] 未能领取到期设备: requested={}, instanceId={}",
            claimedIotIds.size(),
            instanceId);
        return 0;
      }

      if (removedCount < claimedIotIds.size()) {
        log.warn(
            "[ShadowFlush] 部分到期设备未领取: requested={}, claimed={}, instanceId={}",
            claimedIotIds.size(),
            removedCount,
            instanceId);
      }

      CacheReadResult cacheReadResult = getCacheData(dueIds);
      Map<String, String> cacheData = cacheReadResult.cacheData;

      // Redis读取异常不能等同于缓存不存在，重新入队等待下一轮。
      cacheReadResult.failedIotIds.forEach(
          iotId -> requeueShadowForRetry(iotId, "cache read failed"));

      int missingCacheCount =
          Math.max(0, claimedIotIds.size() - cacheData.size() - cacheReadResult.failedIotIds.size());
      if (missingCacheCount > 0) {
        log.info(
            "[ShadowFlush] 清理{}个无缓存的到期设备, instanceId={}", missingCacheCount, instanceId);
      }

      if (cacheData.isEmpty()) {
        log.debug(
            "[ShadowFlush] 本轮无有效缓存数据: claimed={}, missing={}, readFailed={}, instanceId={}",
            removedCount,
            missingCacheCount,
            cacheReadResult.failedIotIds.size(),
            instanceId);
        return 0;
      }

      List<String> validIotIds = new ArrayList<>(cacheData.keySet());
      log.debug(
          "[ShadowFlush] 已领取到期设备: claimed={}, valid={}, missing={}, readFailed={}, instanceId={}",
          removedCount,
          validIotIds.size(),
          missingCacheCount,
          cacheReadResult.failedIotIds.size(),
          instanceId);

      // 批量处理设备影子
      processed = processBatchShadows(validIotIds, cacheData);

      log.info(
          "[ShadowFlush] 刷盘任务完成: processed={}, valid={}, cost={}ms, instanceId={}",
          processed,
          validIotIds.size(),
          (System.currentTimeMillis() - start),
          instanceId);

      return processed;

    } catch (Exception e) {
      log.error("[ShadowFlush] 刷盘任务异常: instanceId={}, error={}", instanceId, e.getMessage(), e);
      return 0;
    }
  }

  /** 测试Redisson分布式锁是否正常工作 可以通过API调用此方法进行测试 */
  public String testDistributedLock() {
    String testLockKey = "test:shadow:flush:lock";

    return distributedLockUtil.tryLockAndExecute(
        testLockKey,
        5, // 等待5秒
        10, // 持有10秒
        TimeUnit.SECONDS,
        () -> {
          log.debug("[ShadowFlush] 测试锁获取成功，当前时间: {}", DateUtil.now());
          try {
            Thread.sleep(2000); // 模拟处理时间
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          return "锁测试成功，处理完成";
        });
  }

  /**
   * 检查强制刷盘状态 - 用于调试
   *
   * @param iotId 设备ID
   * @return 强制刷盘状态信息
   */
  public String checkForceFlushStatus(String iotId) {
    try {
      // 检查ZSet中的刷盘时间
      Double score = stringRedisTemplate.opsForZSet().score(shadowFlushZsetKey, iotId);
      if (score == null) {
        return "设备" + iotId + "不在刷盘队列中";
      }

      long flushTime = score.longValue();
      long now = System.currentTimeMillis();
      long timeUntilFlush = flushTime - now;

      // 检查缓存数据
      String cacheKey = buildShadowKey(iotId);
      String cacheData = stringRedisTemplate.opsForValue().get(cacheKey);
      boolean hasCacheData = StrUtil.isNotBlank(cacheData);

      return String.format(
          "设备%s刷盘状态: 刷盘时间=%d, 当前时间=%d, 距离刷盘=%dms, 有缓存数据=%s",
          iotId, flushTime, now, timeUntilFlush, hasCacheData);

    } catch (Exception e) {
      return "检查强制刷盘状态失败: " + e.getMessage();
    }
  }

  /**
   * 检查集群状态 - 用于调试和监控
   *
   * @return 集群状态信息
   */
  public String checkClusterStatus() {
    try {
      String instanceId = instanceIdProvider.getInstanceId();

      // 检查分布式锁状态
      boolean isLocked = distributedLockUtil.isLocked(lockKey);
      boolean isHeldByCurrentThread = distributedLockUtil.isHeldByCurrentThread(lockKey);

      // 检查ZSet状态
      Long totalCount =
          stringRedisTemplate
              .opsForZSet()
              .count(shadowFlushZsetKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
      Long dueCount =
          stringRedisTemplate
              .opsForZSet()
              .count(
                  shadowFlushZsetKey,
                  Double.NEGATIVE_INFINITY,
                  (double) System.currentTimeMillis());

      return String.format(
          "集群状态: 当前实例=%s, 锁状态=%s, 当前实例持有锁=%s, 待刷盘总数=%d, 到期数量=%d",
          instanceId, isLocked, isHeldByCurrentThread, totalCount, dueCount);

    } catch (Exception e) {
      return "检查集群状态失败: " + e.getMessage();
    }
  }

  /**
   * 手动触发刷盘 - 用于测试和紧急情况
   *
   * @return 处理结果
   */
  public String manualFlush() {
    try {
      String instanceId = instanceIdProvider.getInstanceId();
      log.info("[ShadowFlush] 手动触发刷盘: instanceId={}", instanceId);

      Integer result =
          distributedLockUtil.tryLockAndExecute(
              lockKey, 10, 120, TimeUnit.SECONDS, this::executeFlushTask);

      if (result != null) {
        return String.format("手动刷盘完成: processed=%d, instanceId=%s", result, instanceId);
      } else {
        return String.format("手动刷盘失败: 获取锁失败, instanceId=%s", instanceId);
      }

    } catch (Exception e) {
      return "手动刷盘异常: " + e.getMessage();
    }
  }
}
