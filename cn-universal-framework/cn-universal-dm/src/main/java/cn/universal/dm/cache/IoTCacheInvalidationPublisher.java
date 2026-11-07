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
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.event.EventTopics;
import cn.universal.common.event.RedisEventPublisher;
import cn.universal.persistence.base.IoTDevicePostProcessor;
import cn.universal.persistence.dto.IoTDeviceDTO;
import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 统一缓存失效事件发布器
 *
 * <p>负责将设备、产品、分类相关的缓存失效需求收敛为统一事件，后续由监听器异步执行。
 */
@Component
public class IoTCacheInvalidationPublisher {

  private static final List<String> PRODUCT_KEY_CACHES =
      List.of(
          "iot_product_device",
          "getDeviceMetadata",
          "selectNetworkUnionId",
          "selectTransportProtocol",
          "getProductConfiguration",
          "getProductEncoderType",
          "getProductDecoderType",
          "iot_product_log_store_policy",
          IoTConstant.ENABLE_MYSQL_WRITE);

  // 列表、计数或固定键缓存无法从单个 productKey 反推出实际缓存键，只在产品变更时整区失效。
  private static final List<String> PRODUCT_AGGREGATE_CACHES =
      List.of(
          "selectAllEnableNetworkProductKey",
          "supportMQTTNetwork",
          "iot_product_offline_threshold",
          "selectDevProductV4ListNoPage",
          "countDevNumberByProductKey");

  private static final List<String> PRODUCT_STORE_POLICY_CACHES =
      List.of("iot_product_log_store_policy", "iot_product_device");

  private static final List<String> DEVICE_STATE_CACHES =
      List.of(
          "iot_dev_action",
          "selectDevInstanceBOMap",
          "apiIoTDeviceVOInfo",
          "selectDevInstanceBOPD",
          "selectDevInstance",
          "selectDevInstanceBOIoTId");

  private static final List<String> DEVICE_AGGREGATE_CACHES =
      List.of("selectDevCount", "countDevNumberByProductKey", "selectDevProductV4ListNoPage");

  @Resource private ApplicationEventPublisher applicationEventPublisher;
  @Resource private IoTDevActionCacheKeyResolver iotDevActionCacheKeyResolver;
  @Resource private IoTDeviceCacheKeyResolver iotDeviceCacheKeyResolver;
  @Resource private RedisEventPublisher redisEventPublisher;

  public void publishProductChange(String productKey, String reason) {
    publish(
        reason,
        productKey,
        null,
        null,
        PRODUCT_AGGREGATE_CACHES,
        productCacheKeys(PRODUCT_KEY_CACHES, productKey),
        Collections.emptySet());
  }

  public void publishProductCategoryChange(String reason) {
    publish(
        reason,
        null,
        null,
        null,
        List.of("selectDevProductV4ListNoPage"),
        Collections.emptyMap(),
        Collections.emptySet());
  }

  public void publishProductStorePolicyChange(String productKey, String reason) {
    publish(
        reason,
        productKey,
        null,
        null,
        Collections.emptySet(),
        productCacheKeys(PRODUCT_STORE_POLICY_CACHES, productKey),
        Collections.emptySet());
  }

  public void publishDeviceCountChange(String reason) {
    publish(
        reason,
        null,
        null,
        null,
        List.of("countDevNumberByProductKey", "selectDevProductV4ListNoPage"),
        Collections.emptyMap(),
        Collections.emptySet());
  }

  public void publishApplicationStatusChange(String reason) {
    Set<String> cacheNames = new LinkedHashSet<>(DEVICE_STATE_CACHES);
    cacheNames.add("selectDevCount");
    publish(
        reason,
        null,
        null,
        null,
        cacheNames,
        Collections.emptyMap(),
        Collections.emptySet());
  }

  public void publishDeviceChange(IoTDevicePostProcessor.Operation operation, IoTDeviceDTO deviceDTO) {
    if (operation == null || deviceDTO == null) {
      return;
    }

    Set<String> cacheNames = new LinkedHashSet<>();
    switch (operation) {
      case CREATE, DELETE, UPDATE, ENABLE, DISABLE ->
          cacheNames.addAll(DEVICE_AGGREGATE_CACHES);
      case ONLINE, OFFLINE, COMMAND_RESP -> {
      }
    }

    Map<String, Set<Object>> cacheKeys = new LinkedHashMap<>();
    mergeCacheKeys(cacheKeys, iotDeviceCacheKeyResolver.buildDeviceCacheKeys(deviceDTO));
    mergeCacheKeys(cacheKeys, iotDevActionCacheKeyResolver.buildCacheKeys(deviceDTO));

    publish(
        "device-" + operation.name().toLowerCase(),
        deviceDTO.getProductKey(),
        deviceDTO.getDeviceId(),
        deviceDTO.getIotId(),
        cacheNames,
        cacheKeys,
        buildDeviceRedisKeys(deviceDTO));
  }

  public void publishCustomCacheChange(
      String reason, Collection<String> cacheNames, Map<String, ? extends Collection<?>> cacheKeys) {
    publish(reason, null, null, null, cacheNames, cacheKeys, Collections.emptySet());
  }

  public void publishCustomCacheChange(
      String reason,
      Collection<String> cacheNames,
      Map<String, ? extends Collection<?>> cacheKeys,
      Collection<String> redisKeys) {
    publish(reason, null, null, null, cacheNames, cacheKeys, redisKeys);
  }

  private Set<String> buildDeviceRedisKeys(IoTDeviceDTO deviceDTO) {
    if (deviceDTO == null
        || StrUtil.isBlank(deviceDTO.getProductKey())
        || StrUtil.isBlank(deviceDTO.getDeviceId())) {
      return Collections.emptySet();
    }

    String deviceKey = deviceDTO.getProductKey() + deviceDTO.getDeviceId();
    return new LinkedHashSet<>(
        List.of(
            "device:" + deviceKey,
            "device_metadata:" + deviceKey,
            "device_shadow:" + deviceKey,
            "online:" + deviceKey,
            "offline:" + deviceKey));
  }

  private void publish(
      String reason,
      String productKey,
      String deviceId,
      String iotId,
      Iterable<String> cacheNames,
      Map<String, ? extends Collection<?>> cacheKeys,
      Iterable<String> redisKeys) {
    IoTCacheInvalidationEvent event =
        new IoTCacheInvalidationEvent(
            reason,
            productKey,
            deviceId,
            iotId,
            toSet(cacheNames),
            toKeyMap(cacheKeys),
            toSet(redisKeys));
    applicationEventPublisher.publishEvent(event);
    Runnable clusterNotification =
        () -> redisEventPublisher.publishEvent(EventTopics.DEVICE_CACHE_INVALIDATED, event);
    if (TransactionSynchronizationManager.isActualTransactionActive()
        && TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              clusterNotification.run();
            }
          });
    } else {
      clusterNotification.run();
    }
  }

  private Map<String, Set<Object>> productCacheKeys(
      Iterable<String> cacheNames, String productKey) {
    if (StrUtil.isBlank(productKey)) {
      return Collections.emptyMap();
    }
    Map<String, Set<Object>> cacheKeys = new LinkedHashMap<>();
    cacheNames.forEach(
        cacheName -> cacheKeys.put(cacheName, new LinkedHashSet<>(Set.of(productKey))));
    return cacheKeys;
  }

  private void mergeCacheKeys(
      Map<String, Set<Object>> target, Map<String, ? extends Collection<?>> source) {
    source.forEach(
        (cacheName, keys) ->
            target.computeIfAbsent(cacheName, ignored -> new LinkedHashSet<>()).addAll(keys));
  }

  private Set<String> toSet(Iterable<String> values) {
    if (values == null) {
      return Collections.emptySet();
    }

    Set<String> result = new LinkedHashSet<>();
    values.forEach(
        value -> {
          if (StrUtil.isNotBlank(value)) {
            result.add(value);
          }
        });
    return result;
  }

  private Map<String, Set<Object>> toKeyMap(Map<String, ? extends Collection<?>> values) {
    if (values == null || values.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, Set<Object>> result = new LinkedHashMap<>();
    for (Map.Entry<String, ? extends Collection<?>> entry : values.entrySet()) {
      String cacheName = entry.getKey();
      if (StrUtil.isBlank(cacheName)) {
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
        result.put(cacheName, keySet);
      }
    }
    return result;
  }
}
