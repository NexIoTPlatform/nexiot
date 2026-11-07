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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.Getter;

/**
 * 统一缓存失效事件
 *
 * <p>用于承载需要异步清理的 Spring Cache 与 Redis Key 信息。
 */
@Getter
public class IoTCacheInvalidationEvent {

  private final String reason;
  private final String productKey;
  private final String deviceId;
  private final String iotId;
  private final Set<String> cacheNames;
  private final Map<String, Set<Object>> cacheKeys;
  private final Set<String> redisKeys;

  public IoTCacheInvalidationEvent(
      String reason,
      String productKey,
      String deviceId,
      String iotId,
      Collection<String> cacheNames,
      Map<String, ? extends Collection<?>> cacheKeys,
      Collection<String> redisKeys) {
    this.reason = reason;
    this.productKey = productKey;
    this.deviceId = deviceId;
    this.iotId = iotId;
    this.cacheNames = unmodifiableSet(cacheNames);
    this.cacheKeys = unmodifiableMap(cacheKeys);
    this.redisKeys = unmodifiableSet(redisKeys);
  }

  public IoTCacheInvalidationEvent(
      String reason,
      String productKey,
      String deviceId,
      Collection<String> cacheNames,
      Map<String, ? extends Collection<?>> cacheKeys,
      Collection<String> redisKeys) {
    this(reason, productKey, deviceId, null, cacheNames, cacheKeys, redisKeys);
  }

  private Set<String> unmodifiableSet(Collection<String> values) {
    if (values == null || values.isEmpty()) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(new LinkedHashSet<>(values));
  }

  private Map<String, Set<Object>> unmodifiableMap(Map<String, ? extends Collection<?>> values) {
    if (values == null || values.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, Set<Object>> result = new LinkedHashMap<>();
    for (Map.Entry<String, ? extends Collection<?>> entry : values.entrySet()) {
      String cacheName = entry.getKey();
      if (cacheName == null || cacheName.isBlank()) {
        continue;
      }

      Collection<?> keys = entry.getValue();
      if (keys == null || keys.isEmpty()) {
        continue;
      }

      Set<Object> keySet = new LinkedHashSet<>();
      for (Object key : keys) {
        if (key != null) {
          keySet.add(key);
        }
      }
      if (!keySet.isEmpty()) {
        result.put(cacheName, Collections.unmodifiableSet(keySet));
      }
    }

    if (result.isEmpty()) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(result);
  }
}
