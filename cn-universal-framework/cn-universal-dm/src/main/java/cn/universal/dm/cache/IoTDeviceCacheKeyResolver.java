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

package cn.universal.dm.cache;

import cn.hutool.core.util.StrUtil;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.query.IoTAPIQuery;
import cn.universal.persistence.query.IoTDeviceQuery;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 设备身份缓存键解析器。查询写入和变更淘汰必须共用这里的键规则。 */
@Component("iotDeviceCacheKeyResolver")
public class IoTDeviceCacheKeyResolver {

  public static final String DEVICE_MAP_CACHE = "selectDevInstanceBOMap";
  public static final String DEVICE_API_CACHE = "apiIoTDeviceVOInfo";
  public static final String DEVICE_PD_DTO_CACHE = "selectDevInstanceBOPD";
  public static final String DEVICE_PD_ENTITY_CACHE = "selectDevInstance";
  public static final String DEVICE_IOT_ID_CACHE = "selectDevInstanceBOIoTId";
  public static final String DEVICE_GROUP_NAMES_CACHE = "iot_dev_tag_group_name";

  public boolean canCacheMap(Map<String, ?> query) {
    return StrUtil.isNotBlank(resolveMapKey(query));
  }

  public boolean canCacheApiQuery(IoTAPIQuery query) {
    return StrUtil.isNotBlank(resolveApiQueryKey(query));
  }

  public String resolveMapKey(Map<String, ?> query) {
    if (query == null || query.isEmpty()) {
      return null;
    }
    String iotId = text(query.get("iotId"));
    String productKey = text(query.get("productKey"));
    String deviceId = text(query.get("deviceId"));
    String thirdPlatform = text(query.get("thirdPlatform"));
    String extDeviceId = text(query.get("extDeviceId"));
    String gwProductKey = text(query.get("gwProductKey"));
    int filterCount =
        countNotBlank(iotId, productKey, deviceId, thirdPlatform, extDeviceId, gwProductKey);
    if (filterCount == 1 && StrUtil.isNotBlank(iotId)) {
      return buildIotIdKey(iotId);
    }
    if (filterCount == 2 && StrUtil.isAllNotBlank(productKey, deviceId)) {
      return buildProductDeviceKey(productKey, deviceId);
    }
    if (filterCount == 2 && StrUtil.isAllNotBlank(thirdPlatform, extDeviceId)) {
      return buildThirdPlatformExtDeviceKey(thirdPlatform, extDeviceId);
    }
    if (filterCount == 2 && StrUtil.isAllNotBlank(thirdPlatform, deviceId)) {
      return buildThirdPlatformDeviceKey(thirdPlatform, deviceId);
    }
    return null;
  }

  public String resolveApiQueryKey(IoTAPIQuery query) {
    if (query == null) {
      return null;
    }
    int filterCount =
        countNotBlank(
            query.getIotId(),
            query.getProductKey(),
            query.getDeviceId(),
            query.getGwProductKey(),
            query.getProductId(),
            query.getCompanyNo(),
            query.getIotUnionId(),
            query.getApplicationId());
    if (filterCount == 1 && StrUtil.isNotBlank(query.getIotId())) {
      return buildIotIdKey(query.getIotId());
    }
    return filterCount == 2
        ? buildProductDeviceKey(query.getProductKey(), query.getDeviceId())
        : null;
  }

  public String resolveDeviceQueryKey(IoTDeviceQuery query) {
    if (query == null) {
      return null;
    }
    int filterCount =
        countNotBlank(
            query.getIotId(),
            query.getProductKey(),
            query.getDeviceId(),
            query.getThirdPlatform(),
            query.getExtDeviceId(),
            query.getGwProductKey());
    if (filterCount == 1 && StrUtil.isNotBlank(query.getIotId())) {
      return buildIotIdKey(query.getIotId());
    }
    if (filterCount == 2 && StrUtil.isAllNotBlank(query.getProductKey(), query.getDeviceId())) {
      return buildProductDeviceKey(query.getProductKey(), query.getDeviceId());
    }
    if (filterCount == 2
        && StrUtil.isAllNotBlank(query.getThirdPlatform(), query.getExtDeviceId())) {
      return buildThirdPlatformExtDeviceKey(query.getThirdPlatform(), query.getExtDeviceId());
    }
    return filterCount == 2
        ? buildThirdPlatformDeviceKey(query.getThirdPlatform(), query.getDeviceId())
        : null;
  }

  public Set<Object> resolveDeviceIdentityKeys(IoTDeviceDTO deviceDTO) {
    if (deviceDTO == null) {
      return Collections.emptySet();
    }
    Set<Object> keys = new LinkedHashSet<>();
    addKey(keys, buildIotIdKey(deviceDTO.getIotId()));
    addKey(keys, buildProductDeviceKey(deviceDTO.getProductKey(), deviceDTO.getDeviceId()));
    addKey(
        keys,
        buildThirdPlatformExtDeviceKey(deviceDTO.getThirdPlatform(), deviceDTO.getExtDeviceId()));
    addKey(
        keys,
        buildThirdPlatformDeviceKey(deviceDTO.getThirdPlatform(), deviceDTO.getDeviceId()));
    return keys;
  }

  public Map<String, Set<Object>> buildDeviceCacheKeys(IoTDeviceDTO deviceDTO) {
    Set<Object> identityKeys = resolveDeviceIdentityKeys(deviceDTO);
    if (identityKeys.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, Set<Object>> cacheKeys = new LinkedHashMap<>();
    cacheKeys.put(DEVICE_MAP_CACHE, identityKeys);
    cacheKeys.put(DEVICE_API_CACHE, identityKeys);
    addSingleKey(
        cacheKeys,
        DEVICE_PD_DTO_CACHE,
        buildLegacyProductDeviceKey(deviceDTO.getProductKey(), deviceDTO.getDeviceId()));
    addSingleKey(
        cacheKeys,
        DEVICE_PD_ENTITY_CACHE,
        buildLegacyProductDeviceKey(deviceDTO.getProductKey(), deviceDTO.getDeviceId()));
    addSingleKey(cacheKeys, DEVICE_IOT_ID_CACHE, StrUtil.trim(deviceDTO.getIotId()));
    addSingleKey(cacheKeys, DEVICE_GROUP_NAMES_CACHE, StrUtil.trim(deviceDTO.getIotId()));
    return cacheKeys;
  }

  public String buildIotIdKey(String iotId) {
    return StrUtil.isBlank(iotId) ? null : "iot:" + StrUtil.trim(iotId);
  }

  public String buildProductDeviceKey(String productKey, String deviceId) {
    return StrUtil.hasBlank(productKey, deviceId)
        ? null
        : "pd:" + StrUtil.trim(productKey) + ':' + StrUtil.trim(deviceId);
  }

  public String buildThirdPlatformExtDeviceKey(String thirdPlatform, String extDeviceId) {
    return StrUtil.hasBlank(thirdPlatform, extDeviceId)
        ? null
        : "tpe:" + StrUtil.trim(thirdPlatform) + ':' + StrUtil.trim(extDeviceId);
  }

  public String buildThirdPlatformDeviceKey(String thirdPlatform, String deviceId) {
    return StrUtil.hasBlank(thirdPlatform, deviceId)
        ? null
        : "tpd:" + StrUtil.trim(thirdPlatform) + ':' + StrUtil.trim(deviceId);
  }

  private String buildLegacyProductDeviceKey(String productKey, String deviceId) {
    return StrUtil.hasBlank(productKey, deviceId)
        ? null
        : StrUtil.trim(productKey) + ':' + StrUtil.trim(deviceId);
  }

  private String text(Object value) {
    return value == null ? null : StrUtil.trim(String.valueOf(value));
  }

  private int countNotBlank(String... values) {
    int count = 0;
    for (String value : values) {
      if (StrUtil.isNotBlank(value)) {
        count++;
      }
    }
    return count;
  }

  private void addKey(Set<Object> keys, String key) {
    if (StrUtil.isNotBlank(key)) {
      keys.add(key);
    }
  }

  private void addSingleKey(Map<String, Set<Object>> cacheKeys, String cacheName, String key) {
    if (StrUtil.isNotBlank(key)) {
      cacheKeys.put(cacheName, new LinkedHashSet<>(Set.of(key)));
    }
  }
}
