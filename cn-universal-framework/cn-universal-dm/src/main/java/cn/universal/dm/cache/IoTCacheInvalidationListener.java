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

import java.util.Map;
import java.util.Set;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 统一缓存失效监听器
 *
 * <p>事务提交后异步清理 Spring Cache 与 Redis Key，确保不阻塞主业务流程。
 */
@Component
@Slf4j
public class IoTCacheInvalidationListener {

  @Resource private CacheManager cacheManager;

  @Resource private StringRedisTemplate stringRedisTemplate;

  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onCacheInvalidation(IoTCacheInvalidationEvent event) {
    if (event == null) {
      return;
    }

    int cacheKeyCount = evictSpringCacheKeys(event.getCacheKeys());
    int cacheCount = clearSpringCaches(event.getCacheNames(), event.getCacheKeys().keySet());
    long redisKeyCount = deleteRedisKeys(event.getRedisKeys());

    log.debug(
        "异步缓存失效完成, reason={}, productKey={}, deviceId={}, springCacheKeys={}, springCaches={}, redisKeys={}",
        event.getReason(),
        event.getProductKey(),
        event.getDeviceId(),
        cacheKeyCount,
        cacheCount,
        redisKeyCount);
  }

  private int evictSpringCacheKeys(Map<String, Set<Object>> cacheKeys) {
    if (cacheKeys == null || cacheKeys.isEmpty()) {
      return 0;
    }

    int evicted = 0;
    for (Map.Entry<String, Set<Object>> entry : cacheKeys.entrySet()) {
      String cacheName = entry.getKey();
      Set<Object> keys = entry.getValue();
      if (keys == null || keys.isEmpty()) {
        continue;
      }

      Cache cache = cacheManager.getCache(cacheName);
      if (cache == null) {
        log.debug("缓存不存在，跳过驱逐: {}", cacheName);
        continue;
      }

      for (Object key : keys) {
        try {
          cache.evict(key);
          evicted++;
        } catch (Exception e) {
          log.warn(
              "驱逐 Spring Cache Key 失败, cacheName={}, key={}, error={}",
              cacheName,
              key,
              e.getMessage(),
              e);
        }
      }
    }
    return evicted;
  }

  private int clearSpringCaches(Set<String> cacheNames, Set<String> keyedCacheNames) {
    if (cacheNames == null || cacheNames.isEmpty()) {
      return 0;
    }

    int cleared = 0;
    for (String cacheName : cacheNames) {
      if (keyedCacheNames != null && keyedCacheNames.contains(cacheName)) {
        log.warn("缓存同时配置了具体 Key 和整区清理，已保留精细失效: {}", cacheName);
        continue;
      }
      try {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
          log.debug("缓存不存在，跳过清理: {}", cacheName);
          continue;
        }
        cache.clear();
        cleared++;
      } catch (Exception e) {
        log.warn("清理 Spring Cache 失败, cacheName={}, error={}", cacheName, e.getMessage(), e);
      }
    }
    return cleared;
  }

  private long deleteRedisKeys(Set<String> redisKeys) {
    if (redisKeys == null || redisKeys.isEmpty()) {
      return 0L;
    }

    try {
      Long deleted = stringRedisTemplate.delete(redisKeys);
      return deleted == null ? 0L : deleted;
    } catch (Exception e) {
      log.warn("清理 Redis Key 失败, keyCount={}, error={}", redisKeys.size(), e.getMessage(), e);
      return 0L;
    }
  }
}
