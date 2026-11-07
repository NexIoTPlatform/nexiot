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

package cn.universal.cache.manager;

import cn.universal.cache.config.MultiLevelCacheProperties;
import cn.universal.cache.impl.MultiLevelCache;
import cn.universal.cache.statistics.CacheStatistics;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * 多级缓存管理器
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j
public class MultiLevelCacheManager implements CacheManager {

  private final ConcurrentMap<String, MultiLevelCache> caches = new ConcurrentHashMap<>();

  @Autowired(required = false)
  @Qualifier("l1CacheManager")
  private CacheManager l1CacheManager;

  @Autowired(required = false)
  @Qualifier("l2CacheManager")
  private CacheManager l2CacheManager;

  private MultiLevelCacheProperties properties;

  public void setProperties(MultiLevelCacheProperties properties) {
    this.properties = properties;
  }

  @Override
  public Cache getCache(String name) {
    return caches.computeIfAbsent(name, this::createMultiLevelCache);
  }

  @Override
  public Collection<String> getCacheNames() {
    return caches.keySet();
  }

  /** 创建多级缓存 */
  private MultiLevelCache createMultiLevelCache(String name) {
    Cache l1Cache = null;
    Cache l2Cache = null;

    // 获取L1缓存
    if (l1CacheManager != null) {
      l1Cache = l1CacheManager.getCache(name);
      log.debug("L1缓存创建成功: {}", name);
    } else {
      log.warn("L1缓存管理器未配置，跳过L1缓存");
    }

    // 获取L2缓存
    if (l2CacheManager != null) {
      l2Cache = l2CacheManager.getCache(name);
      log.debug("L2缓存创建成功: {}", name);
    } else {
      log.warn("L2缓存管理器未配置，跳过L2缓存");
    }

    // 如果两个缓存都未配置，抛出异常
    if (l1Cache == null && l2Cache == null) {
      throw new IllegalStateException("至少需要配置一个缓存管理器（L1或L2）");
    }

    MultiLevelCache multiLevelCache = new MultiLevelCache(name, l1Cache, l2Cache, properties);
    log.info(
        "多级缓存创建成功: {}, L1: {}, L2: {}",
        name,
        l1Cache != null ? "启用" : "禁用",
        l2Cache != null ? "启用" : "禁用");

    return multiLevelCache;
  }

  /** 清除指定缓存 */
  public void evictCache(String name) {
    MultiLevelCache cache = caches.remove(name);
    if (cache != null) {
      cache.clear();
      log.info("缓存已清除: {}", name);
    }
  }

  /** 清除所有缓存 */
  public void evictAllCaches() {
    caches.values().forEach(Cache::clear);
    caches.clear();
    log.info("所有缓存已清除");
  }

  /** 获取指定缓存的统计信息 */
  public CacheStatistics getCacheStatistics(String name) {
    MultiLevelCache cache = caches.get(name);
    if (cache != null) {
      return cache.getCacheStatistics();
    }
    throw new IllegalArgumentException("缓存不存在: " + name);
  }

  /** 获取所有缓存的统计信息 */
  public Map<String, CacheStatistics> getCacheStatistics() {
    Map<String, CacheStatistics> statistics = new HashMap<>();
    for (Map.Entry<String, MultiLevelCache> entry : caches.entrySet()) {
      statistics.put(entry.getKey(), entry.getValue().getCacheStatistics());
    }
    return statistics;
  }

  /** 清除所有缓存（别名方法） */
  public void clearAll() {
    evictAllCaches();
  }
}
