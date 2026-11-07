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

package cn.universal.dm.device.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.core.message.UPRequest;
import cn.universal.dm.device.event.DeviceShadowUpdatedEvent;
import cn.universal.core.metadata.AbstractPropertyMetadata;
import cn.universal.core.metadata.PropertyMode;
import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import cn.universal.dm.device.shadow.DeviceShadowWrapper;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.dto.LogStorePolicyDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTDeviceShadow;
import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTDeviceShadowMapper;
import cn.universal.persistence.mapper.admin.SysDictDataMapper;
import cn.universal.persistence.query.IoTDeviceQuery;
import cn.universal.persistence.shadow.Shadow;
import cn.universal.persistence.shadow.State;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 设备影子处理
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/9/17
 */
@Component
@Slf4j
public class IoTDeviceShadowService {

  @Resource private IoTDeviceShadowMapper ioTDeviceShadowMapper;

  @Resource private IoTCacheRemoveService iotCacheRemoveService;

  @Resource private IoTDeviceMapper ioTDeviceMapper;

  @Resource private StringRedisTemplate stringRedisTemplate;

  @Resource private SysDictDataMapper dictDataMapper;

  @Resource private IoTProductDeviceService iotProductDeviceService;

  @Resource private ApplicationEventPublisher applicationEventPublisher;

  @Value("${shadow.cache.enabled:true}")
  private boolean shadowCacheEnabled;

  @Value("${shadow.cache.key-prefix:shadow}")
  private String shadowKeyPrefix;

  @Value("${shadow.cache.ttl-seconds:604800}") // 7 days
  private long shadowCacheTtlSeconds;

  @Value("${shadow.flush.zset-key:shadow:flush}")
  private String shadowFlushZsetKey;

  @Value("${shadow.flush.base-interval-ms:7200000}") // 2h
  private long flushBaseIntervalMs;

  @Value("${shadow.flush.jitter-rate:0.2}") // ±20%
  private double flushJitterRate;

  @Value("${shadow.persisted-marker.key-prefix:shadow:persisted}")
  private String shadowPersistedMarkerKeyPrefix;

  @Value("${shadow.persisted-marker.ttl-seconds:604800}") // 7 days
  private long shadowPersistedMarkerTtlSeconds;

  // 强制刷盘配置 - 使用默认值，不增加环境配置
  private static final long MAX_DELAY_MS = 86400000L; // 24小时最大延迟
  private static final long MIN_INTERVAL_MS = 3600000L; // 最小间隔：1小时
  private static final long FORCE_FLUSH_DELAY_MS = 30000L; // 强制刷盘延迟：30秒（确保能被5分钟扫描间隔捕获）

  private String buildShadowKey(String iotId) {
    return shadowKeyPrefix + ":" + iotId;
  }

  private String buildPersistedMarkerKey(String iotId) {
    return shadowPersistedMarkerKeyPrefix + ":" + iotId;
  }

  private boolean hasPersistedMarker(String iotId) {
    if (!shadowCacheEnabled || StrUtil.isBlank(iotId)) {
      return false;
    }
    try {
      return StrUtil.isNotBlank(
          stringRedisTemplate.opsForValue().get(buildPersistedMarkerKey(iotId)));
    } catch (Exception e) {
      log.warn("[Shadow][Cache] read persisted marker failed iotId={}, err={}", iotId, e.getMessage());
      return false;
    }
  }

  private void markShadowPersisted(String iotId) {
    if (!shadowCacheEnabled || StrUtil.isBlank(iotId)) {
      return;
    }
    try {
      stringRedisTemplate
          .opsForValue()
          .set(
              buildPersistedMarkerKey(iotId),
              "1",
              Math.max(60L, shadowPersistedMarkerTtlSeconds),
              TimeUnit.SECONDS);
    } catch (Exception e) {
      log.warn("[Shadow][Cache] write persisted marker failed iotId={}, err={}", iotId, e.getMessage());
    }
  }

  private Long calcNextFlushAtMs(String iotId) {
    long now = System.currentTimeMillis();

    // 获取当前已设置的刷盘时间
    Double currentScore = stringRedisTemplate.opsForZSet().score(shadowFlushZsetKey, iotId);

    if (currentScore != null) {
      long currentFlushTime = currentScore.longValue();

      if (currentFlushTime <= now) {
        return currentFlushTime;
      }

      // 已有未来刷盘任务时保留更早的时间，避免高频上报不断把首次刷盘向后顺延。
      // 如果版本/最大延迟等策略要求强制刷盘，则只允许把时间提前到强制窗口。
      Long forcedFlushAt = resolveForcedFlushAt(iotId, now, currentFlushTime);
      return forcedFlushAt == null ? currentFlushTime : Math.min(currentFlushTime, forcedFlushAt);
    }

    // 正常计算新的刷盘时间
    double jitter = (Math.random() * 2 * flushJitterRate) - flushJitterRate;
    long offset = (long) (flushBaseIntervalMs * (1 + jitter));
    return now + Math.max(0L, offset);
  }

  private Long resolveForcedFlushAt(String iotId, long now, long currentFlushTime) {
    long timeSinceLastFlush = now - (currentFlushTime - flushBaseIntervalMs);
    if (timeSinceLastFlush > MAX_DELAY_MS || timeSinceLastFlush >= MIN_INTERVAL_MS) {
      return now + FORCE_FLUSH_DELAY_MS;
    }
    return null;
  }

  private Shadow readShadowFromCache(String iotId) {
    if (!shadowCacheEnabled) {
      return null;
    }
    final String json = buildAndGetFromRedis(iotId);
    if (json == null) return null;
    try {
      return JSONUtil.toBean(json, Shadow.class);
    } catch (Exception e) {
      log.warn("[Shadow][Cache] parse error iotId={}, err={}", iotId, e.getMessage());
      return null;
    }
  }

  private String buildAndGetFromRedis(String iotId) {
    String key = buildShadowKey(iotId);
    String json = stringRedisTemplate.opsForValue().get(key);
    if (StrUtil.isBlank(json)) {
      return null;
    }
    return json;
  }

  private Date resolveShadowDate(Long timestamp) {
    if (timestamp == null || timestamp <= 0) {
      return null;
    }
    long ms = timestamp > 9999999999L ? timestamp : timestamp * 1000;
    return new Date(ms);
  }

  private IoTDeviceShadow cachedShadowToDeviceShadow(String iotId, Shadow cachedShadow) {
    if (cachedShadow == null) {
      return null;
    }
    IoTDeviceShadow latestShadow = new IoTDeviceShadow();
    latestShadow.setIotId(iotId);
    latestShadow.setMetadata(JSONUtil.toJsonStr(cachedShadow));
    latestShadow.setVersion(cachedShadow.getVersion());

    Date shadowDate = resolveShadowDate(cachedShadow.getTimestamp());
    if (shadowDate != null) {
      latestShadow.setLastTime(shadowDate);
      latestShadow.setUpdateDate(shadowDate);
      if (latestShadow.getOnlineTime() == null) {
        latestShadow.setOnlineTime(shadowDate);
      }
    }
    return latestShadow;
  }

  private void fillShadowDeviceIdentity(IoTDeviceShadow shadow) {
    if (shadow == null || StrUtil.isBlank(shadow.getIotId())) {
      return;
    }
    if (StrUtil.isNotBlank(shadow.getProductKey()) && StrUtil.isNotBlank(shadow.getDeviceId())) {
      return;
    }
    try {
      IoTDeviceQuery query = new IoTDeviceQuery();
      query.setIotId(shadow.getIotId());
      IoTDevice device = ioTDeviceMapper.getOneByIotId(query);
      if (device == null) {
        return;
      }
      if (StrUtil.isBlank(shadow.getProductKey())) {
        shadow.setProductKey(device.getProductKey());
      }
      if (StrUtil.isBlank(shadow.getDeviceId())) {
        shadow.setDeviceId(device.getDeviceId());
      }
      if (StrUtil.isBlank(shadow.getExtDeviceId())) {
        shadow.setExtDeviceId(device.getExtDeviceId());
      }
      if (StrUtil.isBlank(shadow.getInstance())) {
        shadow.setInstance(device.getApplication());
      }
    } catch (Exception e) {
      log.warn(
          "[Shadow][Cache] fill device identity failed iotId={}, err={}",
          shadow.getIotId(),
          e.getMessage());
    }
  }

  public IoTDeviceShadow getLatestDeviceShadow(String iotId) {
    if (StrUtil.isBlank(iotId)) {
      return null;
    }
    IoTDeviceShadow cachedShadow = cachedShadowToDeviceShadow(iotId, readShadowFromCache(iotId));
    if (cachedShadow != null) {
      fillShadowDeviceIdentity(cachedShadow);
      return cachedShadow;
    }
    return ioTDeviceShadowMapper.selectOne(IoTDeviceShadow.builder().iotId(iotId).build());
  }

  public List<IoTDeviceShadow> getLatestDeviceShadows(List<String> iotIds) {
    if (CollectionUtil.isEmpty(iotIds)) {
      return new ArrayList<>();
    }

    List<String> normalizedIds = new ArrayList<>();
    Map<String, Boolean> uniqueMap = new HashMap<>();
    for (String iotId : iotIds) {
      if (StrUtil.isBlank(iotId) || uniqueMap.containsKey(iotId)) {
        continue;
      }
      uniqueMap.put(iotId, true);
      normalizedIds.add(iotId);
    }

    if (CollectionUtil.isEmpty(normalizedIds)) {
      return new ArrayList<>();
    }

    Map<String, IoTDeviceShadow> latestShadowMap = new HashMap<>();
    List<String> cacheMissIds = new ArrayList<>();
    for (String iotId : normalizedIds) {
      IoTDeviceShadow cachedShadow = cachedShadowToDeviceShadow(iotId, readShadowFromCache(iotId));
      if (cachedShadow != null) {
        latestShadowMap.put(iotId, cachedShadow);
      } else {
        cacheMissIds.add(iotId);
      }
    }

    List<IoTDeviceShadow> persistedShadows =
        CollectionUtil.isEmpty(cacheMissIds) ? List.of() : ioTDeviceShadowMapper.selectByIotIds(cacheMissIds);
    Map<String, IoTDeviceShadow> persistedShadowMap = new HashMap<>();
    if (CollectionUtil.isNotEmpty(persistedShadows)) {
      for (IoTDeviceShadow persistedShadow : persistedShadows) {
        if (persistedShadow != null && StrUtil.isNotBlank(persistedShadow.getIotId())) {
          persistedShadowMap.put(persistedShadow.getIotId(), persistedShadow);
        }
      }
    }

    List<IoTDeviceShadow> result = new ArrayList<>();
    for (String iotId : normalizedIds) {
      IoTDeviceShadow latestShadow = latestShadowMap.get(iotId);
      if (latestShadow == null) {
        latestShadow = persistedShadowMap.get(iotId);
      }
      if (latestShadow != null) {
        result.add(latestShadow);
      }
    }
    return result;
  }

  public void writeShadowToCache(String iotId, Shadow shadow) {
    if (!shadowCacheEnabled || shadow == null) {
      return;
    }
    String key = buildShadowKey(iotId);
    stringRedisTemplate
        .opsForValue()
        .set(key, JSONUtil.toJsonStr(shadow), shadowCacheTtlSeconds, TimeUnit.SECONDS);
  }

  private void markShadowDirtyForFlush(String iotId) {
    if (!shadowCacheEnabled) {
      return;
    }
    try {
      Long nextAt = calcNextFlushAtMs(iotId);
      stringRedisTemplate.opsForZSet().add(shadowFlushZsetKey, iotId, nextAt.doubleValue());
    } catch (Exception e) {
      log.warn("[Shadow][Flush] mark dirty failed iotId={}, err={}", iotId, e.getMessage());
    }
  }

  /** Removes every Redis artifact owned by a deleted device shadow. */
  public void evictDeviceShadowCache(String iotId) {
    if (StrUtil.isBlank(iotId)) {
      return;
    }
    try {
      stringRedisTemplate.delete(buildShadowKey(iotId));
      stringRedisTemplate.delete(buildPersistedMarkerKey(iotId));
      stringRedisTemplate.opsForZSet().remove(shadowFlushZsetKey, iotId);
    } catch (Exception e) {
      log.warn("[Shadow][Cache] evict device shadow failed iotId={}, err={}", iotId, e.getMessage());
    }
  }

  public JSONObject getDeviceShadowObj(String productKey, String deviceId) {
    if (StrUtil.isBlank(productKey) || StrUtil.isBlank(deviceId)) {
      return null;
    }
    String legacyIotId = productKey + deviceId;
    JSONObject cachedShadow = parseCachedShadow(legacyIotId);
    // 缓存拿到，直接返回缓存的
    if (cachedShadow != null) {
      return cachedShadow;
    }
    String resolvedIotId = resolveIotId(productKey, deviceId);
    if (StrUtil.isNotBlank(resolvedIotId) && !legacyIotId.equals(resolvedIotId)) {
      cachedShadow = parseCachedShadow(resolvedIotId);
      if (cachedShadow != null) {
        return cachedShadow;
      }
    }
    String shadowMetadata = ioTDeviceShadowMapper.getShadowMetadata(productKey, deviceId);
    if (StrUtil.isBlank(shadowMetadata)) {
      return null;
    }
    return JSONUtil.parseObj(shadowMetadata);
  }

  private JSONObject parseCachedShadow(String iotId) {
    if (StrUtil.isBlank(iotId)) {
      return null;
    }
    String cached = buildAndGetFromRedis(iotId);
    if (StrUtil.isBlank(cached)) {
      return null;
    }
    try {
      return JSONUtil.parseObj(cached);
    } catch (Exception e) {
      log.warn("[Shadow][Cache] parse cached shadow failed iotId={}, err={}", iotId, e.getMessage());
      return null;
    }
  }

  private String resolveIotId(String productKey, String deviceId) {
    try {
      IoTDeviceQuery query = new IoTDeviceQuery();
      query.setProductKey(productKey);
      query.setDeviceId(deviceId);
      IoTDevice device = ioTDeviceMapper.getOneByDeviceId(query);
      return device == null ? null : device.getIotId();
    } catch (Exception e) {
      log.warn(
          "[Shadow][Cache] resolve iotId failed productKey={}, deviceId={}, err={}",
          productKey,
          deviceId,
          e.getMessage());
      return null;
    }
  }

  /**
   * 获取设备影子包装器（推荐使用） 提供便捷的影子数据访问方法
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @return 设备影子包装器，影子不存在返回null
   */
  public DeviceShadowWrapper getDeviceShadowWrapper(String productKey, String deviceId) {
    JSONObject shadowJson = getDeviceShadowObj(productKey, deviceId);
    return shadowJson != null ? new DeviceShadowWrapper(shadowJson) : null;
  }

  /** 格式化的属性 */
  public List<IoTDevicePropertiesBO> getDevState(String iotId) {
    List<IoTDevicePropertiesBO> result = new ArrayList<>();

    // Cache-first
    Shadow cached = readShadowFromCache(iotId);
    Shadow shadow = null;
    IoTDeviceShadow ioTDeviceShadow = null;
    if (cached != null) {
      shadow = cached;
    } else {
      ioTDeviceShadow =
          ioTDeviceShadowMapper.selectOne(IoTDeviceShadow.builder().iotId(iotId).build());
      if (ioTDeviceShadow == null) {
        log.warn("设备=[{}]影子不存在,清检查", iotId);
        return result;
      }
      shadow = JSONUtil.toBean(ioTDeviceShadow.getMetadata(), Shadow.class);
      // backfill cache
      writeShadowToCache(iotId, shadow);
    }
    JSONObject properties =
        shadow.getState() != null ? shadow.getState().getReported() : new JSONObject();
    JSONObject desireProperties =
        shadow.getState() != null ? shadow.getState().getDesired() : new JSONObject();
    Shadow finalShadow = shadow;
    JSONObject finalProperties = properties;
    JSONObject finalDesireProperties = desireProperties;
    Map<String, Object> map = new HashMap<>();
    map.put("iotId", iotId);
    IoTDeviceDTO ioTDeviceDTO = ioTDeviceMapper.selectIoTDeviceBO(map);
    if (ioTDeviceDTO == null) {
      log.warn("设备=[{}]不存在,无法格式化影子", iotId);
      return fallbackReportedProperties(iotId, null, finalShadow, finalProperties, storePolicyProperties(null));
    }
    List<AbstractPropertyMetadata> propertyMetadataList =
        ioTDeviceDTO.getDeviceMetadata().getProperties();
    LogStorePolicyDTO storePolicyDTO =
        iotProductDeviceService.getProductLogStorePolicy(ioTDeviceDTO.getProductKey());
    if (CollectionUtil.isEmpty(propertyMetadataList)) {
      result.addAll(
          fallbackReportedProperties(
              iotId, ioTDeviceDTO, finalShadow, finalProperties, storePolicyProperties(storePolicyDTO)));
      result.addAll(commonProperties(ioTDeviceDTO));
      return result;
    }
    Map<String, ?> storePolicyProperties = storePolicyProperties(storePolicyDTO);
    propertyMetadataList.stream()
        .filter(
            s -> {
              Object reported = finalProperties.get(s.getId());
              Object desired = finalDesireProperties.get(s.getId());
              if (s.getMode() != null && PropertyMode.r.name().equalsIgnoreCase(s.getMode())) {
                return reported != null;
              }
              return reported != null || desired != null;
            })
        .forEach(
            s -> {
              Object obj = finalProperties.get(s.getId());
              IoTDevicePropertiesBO entity = new IoTDevicePropertiesBO();
              Long ts =
                  finalShadow.getMetadata() != null
                          && finalShadow.getMetadata().getReported() != null
                          && finalShadow.getMetadata().getReported().getJSONObject(s.getId())
                              != null
                      ? finalShadow
                          .getMetadata()
                          .getReported()
                          .getJSONObject(s.getId())
                          .getLong("timestamp")
                      : DateUtil.currentSeconds();
              Object desiredObj = finalDesireProperties.get(s.getId());
              entity.setDesireValue(desiredObj);
              if (desiredObj != null) {
                entity.withDesire(s.getValueType(), desiredObj);
              }
              if (obj != null) {
                entity.withValue(s.getValueType(), obj);
              }
              entity.setPropertyName(s.getName());
              entity.setIotId(iotId);
              entity.setDeviceId(ioTDeviceDTO.getDeviceId());
              entity.setTimestamp(ts);
              entity.setProperty(s.getId());
              entity.setStoragePolicy(storePolicyProperties.containsKey(s.getId()));
              if (finalDesireProperties.get(s.getId()) != null) {
                entity.setCustomized(IoTConstant.DEVICE_SHADOW_DESIRED_PROPERTY);
              }
              result.add(entity);
            });
    result.addAll(commonProperties(ioTDeviceDTO));
    return result;
  }

  private Map<String, ?> storePolicyProperties(LogStorePolicyDTO storePolicyDTO) {
    if (storePolicyDTO == null || storePolicyDTO.getProperties() == null) {
      return Map.of();
    }
    return storePolicyDTO.getProperties();
  }

  private List<IoTDevicePropertiesBO> fallbackReportedProperties(
      String iotId,
      IoTDeviceDTO ioTDeviceDTO,
      Shadow shadow,
      JSONObject reported,
      Map<String, ?> storePolicyProperties) {
    List<IoTDevicePropertiesBO> result = new ArrayList<>();
    if (reported == null || reported.isEmpty()) {
      return result;
    }
    reported.forEach(
        (property, value) -> {
          IoTDevicePropertiesBO entity = new IoTDevicePropertiesBO();
          entity.withValue((AbstractPropertyMetadata) null, value);
          entity.setProperty(property);
          entity.setPropertyName(property);
          entity.setIotId(iotId);
          entity.setDeviceId(ioTDeviceDTO == null ? null : ioTDeviceDTO.getDeviceId());
          entity.setTimestamp(reportedTimestamp(shadow, property));
          entity.setStoragePolicy(storePolicyProperties.containsKey(property));
          result.add(entity);
        });
    return result;
  }

  private Long reportedTimestamp(Shadow shadow, String property) {
    if (shadow != null
        && shadow.getMetadata() != null
        && shadow.getMetadata().getReported() != null
        && shadow.getMetadata().getReported().getJSONObject(property) != null) {
      Long timestamp =
          shadow.getMetadata().getReported().getJSONObject(property).getLong("timestamp");
      if (timestamp != null) {
        return timestamp;
      }
    }
    return DateUtil.currentSeconds();
  }

  /** 查询公共属性字典配置（带Redis缓存） 用于判断哪些属性需要同步到设备configuration字段 */
  public List<SysDictData> queryCommonPropertyDict() {
    String key = "dict:" + IoTConstant.DEVICE_SHADOW_CUSTOMIZED_PROPERTY;
    String jsonStr = stringRedisTemplate.opsForValue().get(key);
    List<SysDictData> sysDictData = null;
    if (StrUtil.isNotBlank(jsonStr)) {
      try {
        sysDictData = JSONUtil.toList(jsonStr, SysDictData.class);
      } catch (Exception e) {
        log.warn("Failed to deserialize Redis data for key: {}, error: {}", key, e.getMessage());
      }
    }
    if (CollectionUtil.isEmpty(sysDictData)) {
      sysDictData =
          dictDataMapper.selectDictDataByType(IoTConstant.DEVICE_SHADOW_CUSTOMIZED_PROPERTY);
      if (CollectionUtil.isNotEmpty(sysDictData)) {
        stringRedisTemplate
            .opsForValue()
            .set(key, JSONUtil.toJsonStr(sysDictData), 2, TimeUnit.HOURS);
      }
    }
    return sysDictData;
  }

  /**
   * imei，deviceId字段
   *
   * <p>dict字典表：`device_common_property`
   */
  private List<IoTDevicePropertiesBO> commonProperties(IoTDeviceDTO ioTDeviceDTO) {
    List<IoTDevicePropertiesBO> result = new ArrayList<>();
    if (ioTDeviceDTO == null) {
      return result;
    }
    if (StrUtil.isNotBlank(ioTDeviceDTO.getDevConfiguration())) {
      JSONObject cfg = JSONUtil.parseObj(ioTDeviceDTO.getDevConfiguration());
      List<SysDictData> property = queryCommonPropertyDict();
      if (CollectionUtil.isNotEmpty(property)) {
        property.stream()
            .forEach(
                s -> {
                  if (cfg.containsKey(s.getDictValue())) {
                    IoTDevicePropertiesBO imeiValue =
                        IoTDevicePropertiesBO.builder()
                            .iotId(ioTDeviceDTO.getIotId())
                            .deviceId(ioTDeviceDTO.getDeviceId())
                            .formatValue(cfg.getStr(s.getDictValue()))
                            .property(s.getDictValue())
                            .propertyName(s.getDictLabel())
                            .value(cfg.getStr(s.getDictValue()))
                            .timestamp(0L)
                            .stringValue(cfg.getStr(s.getDictValue()))
                            .customized(IoTConstant.DEVICE_SHADOW_CUSTOMIZED_PROPERTY)
                            .build();
                    result.add(imeiValue);
                  }
                });
      }
    }
    return result;
  }

  public void doShadowOriginal(UPRequest upRequest, IoTDeviceDTO ioTDeviceDTO) {
    // 设备影子创建或更新
    boolean flag =
        stringRedisTemplate
            .opsForValue()
            .setIfAbsent("doCreateShadow:" + ioTDeviceDTO.getIotId(), "1", 10, TimeUnit.MINUTES);
    if (!ioTDeviceDTO.isShadow() && flag) {
      IoTDeviceShadow ioTDeviceShadow =
          IoTDeviceShadow.builder()
              .iotId(ioTDeviceDTO.getIotId())
              .extDeviceId(ioTDeviceDTO.getExtDeviceId())
              .productKey(ioTDeviceDTO.getProductKey())
              .deviceId(ioTDeviceDTO.getDeviceId())
              .activeTime(new Date())
              .onlineTime(new Date())
              .updateDate(new Date())
              .lastTime(new Date())
              .build();
      State state = State.builder().desired(new JSONObject()).reported(new JSONObject()).build();
      State metadata = State.builder().desired(new JSONObject()).reported(new JSONObject()).build();
      Shadow shadow =
          Shadow.builder()
              .state(state)
              .metadata(metadata)
              .timestamp(DateUtil.currentSeconds())
              .version(1L)
              .build();
      doDesired(shadow, ioTDeviceDTO, upRequest);
      ioTDeviceShadow.setMetadata(JSONUtil.toJsonStr(shadow));
      ioTDeviceShadow.setVersion(shadow.getVersion());
      ioTDeviceShadowMapper.insertSelective(ioTDeviceShadow);
      markShadowPersisted(ioTDeviceDTO.getIotId());
      publishShadowUpdatedEvent(upRequest, ioTDeviceDTO, shadow);
      // 代理调用内部方法，清除缓存
      //      iotCacheRemoveService.removeDevInstanceBOCache();
    } else {
      // 更新最后通信时间
      IoTDeviceShadow ioTDeviceShadow =
          IoTDeviceShadow.builder().iotId(upRequest.getIotId()).build();
      ioTDeviceShadow = ioTDeviceShadowMapper.selectOne(ioTDeviceShadow);
      Shadow shadow = null;
      if (ioTDeviceShadow == null || StrUtil.isBlank(ioTDeviceShadow.getMetadata())) {
        shadow = Shadow.builder().timestamp(DateUtil.currentSeconds()).version(1L).build();
      } else {
        shadow = JSONUtil.toBean(ioTDeviceShadow.getMetadata(), Shadow.class);
      }
      doDesired(shadow, ioTDeviceDTO, upRequest);
      ioTDeviceShadow.setMetadata(JSONUtil.toJsonStr(shadow));
      ioTDeviceShadow.setVersion(shadow.getVersion());
      ioTDeviceShadow.setLastTime(new Date());
      ioTDeviceShadow.setUpdateDate(new Date());
      ioTDeviceShadowMapper.updateByPrimaryKeySelective(ioTDeviceShadow);
      markShadowPersisted(ioTDeviceDTO.getIotId());
      publishShadowUpdatedEvent(upRequest, ioTDeviceDTO, shadow);
    }
  }

  /**
   * 影子期望值
   *
   * @param shadow 影子
   * @param ioTDeviceDTO 设备信息
   * @param request 消息原文
   */
  private void doDesired(Shadow shadow, IoTDeviceDTO ioTDeviceDTO, UPRequest request) {
    if (request == null) {
      return;
    }
    long shadowTimestamp = resolveShadowTimestamp(request);
    Map<String, Object> properties = request.getProperties();
    Map<String, Object> data = request.getData();
    // 属性上报
    if (CollectionUtil.isNotEmpty(properties)) {
      /** 开始处理state */
      State state = shadow.getState() != null ? shadow.getState() : new State();
      /** 开始处理metadata */
      State metadata = shadow.getMetadata() != null ? shadow.getMetadata() : new State();
      // 处理state-reported
      JSONObject stateReported =
          state.getReported() != null ? state.getReported() : new JSONObject();
      JSONObject stateDesired = state.getDesired() != null ? state.getDesired() : new JSONObject();
      JSONObject metadataReported =
          metadata.getReported() != null ? metadata.getReported() : new JSONObject();
      JSONObject metadataDesired =
          metadata.getDesired() != null ? metadata.getDesired() : new JSONObject();
      properties.forEach(
          (property, value) -> {
            updateReportedState(
                stateReported,
                stateDesired,
                metadataReported,
                metadataDesired,
                property,
                value,
                shadowTimestamp);
          });
      state.setReported(stateReported);
      state.setDesired(stateDesired);
      metadata.setDesired(metadataDesired);
      metadata.setReported(metadataReported);

      shadow.setMetadata(metadata);
      shadow.setState(state);
      // 处理时间和版本
      shadow.setTimestamp(shadowTimestamp);
      shadow.setVersion(
          (shadow.getVersion() == null || shadow.getVersion() <= 0)
              ? 1L
              : (shadow.getVersion() + 1));
    }

    // 处理消息类型为reply且data不为空
    if (MessageType.REPLY.equals(request.getMessageType()) && CollectionUtil.isNotEmpty(data)) {
      /** 开始处理state */
      State state = shadow.getState() != null ? shadow.getState() : new State();
      JSONObject stateReported =
          state.getReported() != null ? state.getReported() : new JSONObject();
      JSONObject stateDesired = state.getDesired() != null ? state.getDesired() : new JSONObject();

      // 处理metadata
      State metadata = shadow.getMetadata() != null ? shadow.getMetadata() : new State();
      JSONObject metadataReported =
          metadata.getReported() != null ? metadata.getReported() : new JSONObject();
      JSONObject metadataDesired =
          metadata.getDesired() != null ? metadata.getDesired() : new JSONObject();

      data.forEach(
          (key, value) -> {
            // 仅当reply中带有真实属性值时，才更新reported并结束活动期望
            if (hasReportedValue(value)) {
              updateReportedState(
                  stateReported,
                  stateDesired,
                  metadataReported,
                  metadataDesired,
                  key,
                  value,
                  shadowTimestamp);
            }
          });
      state.setReported(stateReported);
      state.setDesired(stateDesired);
      metadata.setDesired(metadataDesired);
      metadata.setReported(metadataReported);
      shadow.setMetadata(metadata);
      shadow.setState(state);
      // 处理时间和版本
      shadow.setTimestamp(shadowTimestamp);
      shadow.setVersion(
          (shadow.getVersion() == null || shadow.getVersion() <= 0)
              ? 1L
              : (shadow.getVersion() + 1));
    }
  }

  /**
   * 仅更新设备影子的期望值。
   *
   * <p>用于平台侧确认已受理的下行写入：写入 {@code state.desired}，保留 {@code state.reported}
   * 仅由设备真实上报或回读确认更新。
   */
  public void doDesiredShadow(
      IoTDeviceDTO ioTDeviceDTO, Map<String, Object> desiredProperties, Long timestamp) {
    if (ioTDeviceDTO == null
        || StrUtil.isBlank(ioTDeviceDTO.getIotId())
        || CollectionUtil.isEmpty(desiredProperties)) {
      return;
    }
    if (shadowCacheEnabled) {
      doDesiredShadowCache(ioTDeviceDTO, desiredProperties, timestamp);
      return;
    }
    doDesiredShadowOriginal(ioTDeviceDTO, desiredProperties, timestamp);
  }

  private void doDesiredShadowOriginal(
      IoTDeviceDTO ioTDeviceDTO, Map<String, Object> desiredProperties, Long timestamp) {
    IoTDeviceShadow ioTDeviceShadow =
        ioTDeviceShadowMapper.selectOne(
            IoTDeviceShadow.builder().iotId(ioTDeviceDTO.getIotId()).build());
    Shadow shadow;
    if (ioTDeviceShadow == null || StrUtil.isBlank(ioTDeviceShadow.getMetadata())) {
      shadow = newShadow();
      ioTDeviceShadow =
          IoTDeviceShadow.builder()
              .iotId(ioTDeviceDTO.getIotId())
              .extDeviceId(ioTDeviceDTO.getExtDeviceId())
              .productKey(ioTDeviceDTO.getProductKey())
              .deviceId(ioTDeviceDTO.getDeviceId())
              .activeTime(new Date())
              .onlineTime(new Date())
              .build();
    } else {
      shadow = JSONUtil.toBean(ioTDeviceShadow.getMetadata(), Shadow.class);
    }
    updateDesiredState(shadow, desiredProperties, timestamp);
    ioTDeviceShadow.setMetadata(JSONUtil.toJsonStr(shadow));
    ioTDeviceShadow.setVersion(shadow.getVersion());
    ioTDeviceShadow.setLastTime(new Date());
    ioTDeviceShadow.setUpdateDate(new Date());
    if (ioTDeviceShadow.getProductKey() == null) {
      ioTDeviceShadow.setProductKey(ioTDeviceDTO.getProductKey());
    }
    if (ioTDeviceShadow.getDeviceId() == null) {
      ioTDeviceShadow.setDeviceId(ioTDeviceDTO.getDeviceId());
    }
    if (ioTDeviceShadow.getIotId() == null) {
      ioTDeviceShadow.setIotId(ioTDeviceDTO.getIotId());
    }
    if (ioTDeviceShadow.getId() == null) {
      ioTDeviceShadowMapper.insertSelective(ioTDeviceShadow);
    } else {
      ioTDeviceShadowMapper.updateByPrimaryKeySelective(ioTDeviceShadow);
    }
    markShadowPersisted(ioTDeviceDTO.getIotId());
    publishShadowUpdatedEvent(desiredUpRequest(ioTDeviceDTO, desiredProperties, shadow), ioTDeviceDTO, shadow);
  }

  private void doDesiredShadowCache(
      IoTDeviceDTO ioTDeviceDTO, Map<String, Object> desiredProperties, Long timestamp) {
    String iotId = ioTDeviceDTO.getIotId();
    Shadow shadow = readShadowFromCache(iotId);
    if (shadow == null) {
      IoTDeviceShadow fromDb =
          ioTDeviceShadowMapper.selectOne(IoTDeviceShadow.builder().iotId(iotId).build());
      if (fromDb != null && StrUtil.isNotBlank(fromDb.getMetadata())) {
        shadow = JSONUtil.toBean(fromDb.getMetadata(), Shadow.class);
        markShadowPersisted(iotId);
      } else {
        shadow = newShadow();
      }
    }
    updateDesiredState(shadow, desiredProperties, timestamp);
    writeShadowToCache(iotId, shadow);
    ensureInitialShadowPersisted(
        desiredUpRequest(ioTDeviceDTO, desiredProperties, shadow), ioTDeviceDTO, shadow);
    markShadowDirtyForFlush(iotId);
    publishShadowUpdatedEvent(desiredUpRequest(ioTDeviceDTO, desiredProperties, shadow), ioTDeviceDTO, shadow);
  }

  private Shadow newShadow() {
    State state = State.builder().desired(new JSONObject()).reported(new JSONObject()).build();
    State metadata = State.builder().desired(new JSONObject()).reported(new JSONObject()).build();
    return Shadow.builder()
        .state(state)
        .metadata(metadata)
        .timestamp(DateUtil.currentSeconds())
        .version(1L)
        .build();
  }

  private void updateDesiredState(
      Shadow shadow, Map<String, Object> desiredProperties, Long timestamp) {
    if (shadow == null || CollectionUtil.isEmpty(desiredProperties)) {
      return;
    }
    long shadowTimestamp = resolveDesiredTimestamp(timestamp);
    State state = shadow.getState() != null ? shadow.getState() : new State();
    State metadata = shadow.getMetadata() != null ? shadow.getMetadata() : new State();
    JSONObject stateDesired = state.getDesired() != null ? state.getDesired() : new JSONObject();
    JSONObject stateReported = state.getReported() != null ? state.getReported() : new JSONObject();
    JSONObject metadataDesired =
        metadata.getDesired() != null ? metadata.getDesired() : new JSONObject();
    JSONObject metadataReported =
        metadata.getReported() != null ? metadata.getReported() : new JSONObject();
    desiredProperties.forEach(
        (property, value) -> {
          if (StrUtil.isBlank(property)) {
            return;
          }
          stateDesired.set(property, value);
          metadataDesired.set(property, new Timestamp(shadowTimestamp));
        });
    state.setDesired(stateDesired);
    state.setReported(stateReported);
    metadata.setDesired(metadataDesired);
    metadata.setReported(metadataReported);
    shadow.setState(state);
    shadow.setMetadata(metadata);
    shadow.setTimestamp(shadowTimestamp);
    shadow.setVersion(
        (shadow.getVersion() == null || shadow.getVersion() <= 0)
            ? 1L
            : (shadow.getVersion() + 1));
  }

  private long resolveDesiredTimestamp(Long timestamp) {
    if (timestamp == null || timestamp <= 0) {
      return DateUtil.currentSeconds();
    }
    return timestamp > 9999999999L ? timestamp / 1000 : timestamp;
  }

  private UPRequest desiredUpRequest(
      IoTDeviceDTO ioTDeviceDTO, Map<String, Object> desiredProperties, Shadow shadow) {
    UPRequest request = new UPRequest();
    request.setIotId(ioTDeviceDTO.getIotId());
    request.setProductKey(ioTDeviceDTO.getProductKey());
    request.setDeviceId(ioTDeviceDTO.getDeviceId());
    request.setDeviceName(ioTDeviceDTO.getDeviceName());
    request.setMessageType(MessageType.FUNCTIONS);
    request.setFunction(IoTConstant.SET_DEVICE_DESIRED_PROPERTIES);
    request.setData(desiredProperties);
    request.setTime(shadow == null ? DateUtil.currentSeconds() : shadow.getTimestamp());
    return request;
  }

  private void updateReportedState(
      JSONObject stateReported,
      JSONObject stateDesired,
      JSONObject metadataReported,
      JSONObject metadataDesired,
      String property,
      Object value,
      long reportedTimestamp) {
    stateReported.set(property, value);
    metadataReported.set(property, new Timestamp(reportedTimestamp));
    removeDesiredFromActiveState(stateDesired, metadataDesired, property, reportedTimestamp);
  }

  private void removeDesiredFromActiveState(
      JSONObject stateDesired, JSONObject metadataDesired, String property, long reportedTimestamp) {
    if (stateDesired == null || !stateDesired.containsKey(property)) {
      return;
    }
    Long desiredTimestamp = getMetadataTimestamp(metadataDesired, property);
    if (desiredTimestamp == null || reportedTimestamp >= desiredTimestamp) {
      stateDesired.remove(property);
      if (metadataDesired != null) {
        metadataDesired.remove(property);
      }
    }
  }

  private Long getMetadataTimestamp(JSONObject metadata, String property) {
    if (metadata == null || StrUtil.isBlank(property)) {
      return null;
    }
    JSONObject propertyMetadata = metadata.getJSONObject(property);
    return propertyMetadata != null ? propertyMetadata.getLong("timestamp") : null;
  }

  private boolean hasReportedValue(Object value) {
    return value != null && !"".equals(value);
  }

  private long resolveShadowTimestamp(UPRequest request) {
    Long requestTime = request.getTime();
    if (requestTime == null || requestTime <= 0) {
      return DateUtil.currentSeconds();
    }
    return requestTime > 9999999999L ? requestTime / 1000 : requestTime;
  }

  @AllArgsConstructor
  @Getter
  private class Timestamp {

    private Long timestamp;
  }

  // ==================== 新优化方法 ====================

  /** 优化的设备影子处理方法 - 直接调用优化类 */
  public void doShadow(UPRequest upRequest, IoTDeviceDTO ioTDeviceDTO) {
    if (shadowCacheEnabled) {
      doShadowCache(upRequest, ioTDeviceDTO);
      return;
    }
    doShadowOriginal(upRequest, ioTDeviceDTO);
  }

  /** 新增：仅缓存与标记刷盘的影子处理，不改动原有 doShadowOriginal */
  public void doShadowCache(UPRequest upRequest, IoTDeviceDTO ioTDeviceDTO) {
    if (!shadowCacheEnabled) {
      // 回退到原始逻辑
      doShadowOriginal(upRequest, ioTDeviceDTO);
      return;
    }
    String iotId = ioTDeviceDTO.getIotId();
    Shadow shadow = readShadowFromCache(iotId);
    if (shadow == null) {
      // fallback DB
      IoTDeviceShadow fromDb =
          ioTDeviceShadowMapper.selectOne(IoTDeviceShadow.builder().iotId(iotId).build());
      if (fromDb != null && StrUtil.isNotBlank(fromDb.getMetadata())) {
        shadow = JSONUtil.toBean(fromDb.getMetadata(), Shadow.class);
        markShadowPersisted(iotId);
      } else {
        shadow = Shadow.builder().timestamp(DateUtil.currentSeconds()).version(1L).build();
      }
    }
    // 合并期望/上报并自增版本
    doDesired(shadow, ioTDeviceDTO, upRequest);
    // 写缓存
    writeShadowToCache(iotId, shadow);
    ensureInitialShadowPersisted(upRequest, ioTDeviceDTO, shadow);
    // 标记刷盘
    markShadowDirtyForFlush(iotId);
    publishShadowUpdatedEvent(upRequest, ioTDeviceDTO, shadow);
    // 清除相关业务缓存
    //    iotCacheRemoveService.removeDevInstanceBOCache();
  }

  private void ensureInitialShadowPersisted(
      UPRequest upRequest, IoTDeviceDTO ioTDeviceDTO, Shadow shadow) {
    if (upRequest == null
        || ioTDeviceDTO == null
        || shadow == null
        || StrUtil.isBlank(ioTDeviceDTO.getIotId())) {
      return;
    }
    try {
      String iotId = ioTDeviceDTO.getIotId();
      if (hasPersistedMarker(iotId)) {
        return;
      }
      Boolean lockAcquired =
          stringRedisTemplate
              .opsForValue()
              .setIfAbsent("doCreateShadow:" + iotId, "1", 10, TimeUnit.MINUTES);
      if (!Boolean.TRUE.equals(lockAcquired)) {
        return;
      }
      IoTDeviceShadow existed =
          ioTDeviceShadowMapper.selectOne(IoTDeviceShadow.builder().iotId(iotId).build());
      if (existed != null) {
        markShadowPersisted(iotId);
        return;
      }
      IoTDeviceShadow created =
          IoTDeviceShadow.builder()
              .iotId(iotId)
              .extDeviceId(ioTDeviceDTO.getExtDeviceId())
              .productKey(ioTDeviceDTO.getProductKey())
              .deviceId(ioTDeviceDTO.getDeviceId())
              .activeTime(new Date())
              .onlineTime(new Date())
              .updateDate(new Date())
              .lastTime(new Date())
              .metadata(JSONUtil.toJsonStr(shadow))
              .version(shadow.getVersion())
              .build();
      ioTDeviceShadowMapper.insertSelective(created);
      markShadowPersisted(iotId);
    } catch (Exception e) {
      log.warn(
          "[Shadow][Cache] create initial shadow failed productKey={}, deviceId={}, iotId={}, err={}",
          ioTDeviceDTO.getProductKey(),
          ioTDeviceDTO.getDeviceId(),
          ioTDeviceDTO.getIotId(),
          e.getMessage());
    }
  }

  private void publishShadowUpdatedEvent(
      UPRequest upRequest, IoTDeviceDTO ioTDeviceDTO, Shadow shadow) {
    if (applicationEventPublisher == null
        || upRequest == null
        || ioTDeviceDTO == null
        || StrUtil.isBlank(ioTDeviceDTO.getIotId())) {
      return;
    }
    Map<String, Object> changed = new HashMap<>();
    if (CollectionUtil.isNotEmpty(upRequest.getProperties())) {
      changed.putAll(upRequest.getProperties());
    }
    if (MessageType.REPLY.equals(upRequest.getMessageType())
        && CollectionUtil.isNotEmpty(upRequest.getData())) {
      changed.putAll(upRequest.getData());
    }
    if (MessageType.FUNCTIONS.equals(upRequest.getMessageType())
        && IoTConstant.SET_DEVICE_DESIRED_PROPERTIES.equals(upRequest.getFunction())
        && CollectionUtil.isNotEmpty(upRequest.getData())) {
      changed.putAll(upRequest.getData());
    }
    if (changed.isEmpty()) {
      return;
    }
    long timestampMs = System.currentTimeMillis();
    if (shadow != null && shadow.getTimestamp() != null && shadow.getTimestamp() > 0) {
      timestampMs =
          shadow.getTimestamp() > 9999999999L
              ? shadow.getTimestamp()
              : shadow.getTimestamp() * 1000;
    }
    long version = shadow == null || shadow.getVersion() == null ? 0L : shadow.getVersion();
    applicationEventPublisher.publishEvent(
        new DeviceShadowUpdatedEvent(
            ioTDeviceDTO.getIotId(),
            ioTDeviceDTO.getProductKey(),
            ioTDeviceDTO.getDeviceId(),
            ioTDeviceDTO.getExtDeviceId(),
            changed,
            timestampMs,
            version));
  }

}
