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

package cn.universal.cache.impl;

import cn.universal.cache.config.MultiLevelCacheProperties;
import cn.universal.cache.statistics.CacheStatistics;
import cn.universal.cache.strategy.CacheStrategy;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

/**
 * 多级缓存实现
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j
public class MultiLevelCache implements Cache {

  private final String name;
  private final Cache l1Cache;
  private final Cache l2Cache;
  private final MultiLevelCacheProperties properties;
  private final Executor asyncExecutor;
  private final CacheStatistics statistics = new CacheStatistics();

  public MultiLevelCache(
      String name, Cache l1Cache, Cache l2Cache, MultiLevelCacheProperties properties) {
    this.name = name;
    this.l1Cache = l1Cache;
    this.l2Cache = l2Cache;
    this.properties = properties;
    this.asyncExecutor =
        Executors.newFixedThreadPool(
            2,
            r -> {
              Thread t = new Thread(r, "multi-level-cache-async-" + name);
              t.setDaemon(true);
              return t;
            });
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Object getNativeCache() {
    return this;
  }

  @Override
  public ValueWrapper get(Object key) {
    if (key == null) {
      return null;
    }
    String cacheKey = key.toString();
    try {
      if (l1Cache != null) {
        ValueWrapper l1Value = l1Cache.get(key);
        if (l1Value != null) {
          statistics.recordL1Hit();
          log.debug("L1缓存命中: {} -> {}", cacheKey, name);
          return l1Value;
        } else {
          statistics.recordL1Miss();
        }
      }
    } catch (Exception e) {
      log.warn("L1缓存 get 异常: {} -> {}", cacheKey, name, e);
    }
    try {
      if (l2Cache != null) {
        ValueWrapper l2Value = l2Cache.get(key);
        if (l2Value != null) {
          statistics.recordL2Hit();
          if (l1Cache != null) {
            try {
              l1Cache.put(key, l2Value.get());
              log.debug("L2缓存命中，回填L1: {} -> {}", cacheKey, name);
            } catch (Exception e) {
              log.warn("L1缓存回填异常: {} -> {}", cacheKey, name, e);
            }
          }
          return l2Value;
        } else {
          statistics.recordL2Miss();
        }
      }
    } catch (Exception e) {
      log.warn("L2缓存 get 异常: {} -> {}", cacheKey, name, e);
    }
    log.debug("缓存未命中: {} -> {}", cacheKey, name);
    return null;
  }

  @Override
  public <T> T get(Object key, Class<T> type) {
    try {
      ValueWrapper value = get(key);
      if (value != null && type.isInstance(value.get())) {
        return type.cast(value.get());
      }
    } catch (Exception e) {
      log.warn("缓存 get(Class) 异常: {} -> {}", key, name, e);
    }
    return null;
  }

  @Override
  public <T> T get(Object key, Callable<T> valueLoader) {
    if (key == null) {
      return null;
    }
    String cacheKey = key.toString();
    try {
      ValueWrapper value = get(key);
      if (value != null) {
        @SuppressWarnings("unchecked")
        T result = (T) value.get();
        return result;
      }
    } catch (Exception e) {
      log.warn("缓存 get(Callable) 异常: {} -> {}", cacheKey, name, e);
    }
    try {
      T loadedValue = valueLoader.call();
      if (loadedValue != null) {
        try {
          put(key, loadedValue);
          log.debug("使用valueLoader加载数据并缓存: {} -> {}", cacheKey, name);
        } catch (Exception e) {
          log.warn("缓存 put 异常: {} -> {}", cacheKey, name, e);
        }
      }
      return loadedValue;
    } catch (Exception e) {
      log.error("使用valueLoader加载数据失败: {} -> {}", cacheKey, name, e);
      return null;
    }
  }

  @Override
  public void put(Object key, Object value) {
    if (key == null || value == null) {
      return;
    }
    String cacheKey = key.toString();
    CacheStrategy strategy = properties.getDefaults().getStrategy();
    try {
      switch (strategy) {
        case WRITE_THROUGH:
          if (l1Cache != null) {
            try {
              l1Cache.put(key, value);
              statistics.recordL1Put();
            } catch (Exception e) {
              log.warn("L1 put 异常: {} -> {}", cacheKey, name, e);
            }
          }
          if (l2Cache != null) {
            try {
              l2Cache.put(key, value);
              statistics.recordL2Put();
            } catch (Exception e) {
              log.warn("L2 put 异常: {} -> {}", cacheKey, name, e);
            }
          }
          log.debug("WRITE_THROUGH策略写入: {} -> {}", cacheKey, name);
          break;
        case WRITE_BEHIND:
          if (l1Cache != null) {
            try {
              l1Cache.put(key, value);
              statistics.recordL1Put();
            } catch (Exception e) {
              log.warn("L1 put 异常: {} -> {}", cacheKey, name, e);
            }
          }
          if (l2Cache != null) {
            CompletableFuture.runAsync(
                () -> {
                  try {
                    l2Cache.put(key, value);
                    statistics.recordL2Put();
                    log.debug("WRITE_BEHIND策略异步写入L2: {} -> {}", cacheKey, name);
                  } catch (Exception e) {
                    log.warn("异步写入L2缓存失败: {} -> {}", cacheKey, name, e);
                  }
                },
                asyncExecutor);
          }
          log.debug("WRITE_BEHIND策略写入L1: {} -> {}", cacheKey, name);
          break;
        case WRITE_AROUND:
          if (l2Cache != null) {
            try {
              l2Cache.put(key, value);
              statistics.recordL2Put();
              log.debug("WRITE_AROUND策略写入L2: {} -> {}", cacheKey, name);
            } catch (Exception e) {
              log.warn("L2 put 异常: {} -> {}", cacheKey, name, e);
            }
          }
          break;
      }
    } catch (Exception e) {
      log.warn("缓存 put 异常: {} -> {}", cacheKey, name, e);
    }
  }

  @Override
  public ValueWrapper putIfAbsent(Object key, Object value) {
    if (key == null || value == null) {
      return null;
    }
    try {
      ValueWrapper existing = get(key);
      if (existing != null) {
        return existing;
      }
      put(key, value);
      return new SimpleValueWrapper(value);
    } catch (Exception e) {
      log.warn("缓存 putIfAbsent 异常: {} -> {}", key, name, e);
      return null;
    }
  }

  @Override
  public void evict(Object key) {
    if (key == null) {
      return;
    }
    String cacheKey = key.toString();
    try {
      if (l1Cache != null) {
        try {
          l1Cache.evict(key);
          statistics.recordL1Evict();
        } catch (Exception e) {
          log.warn("L1 evict 异常: {} -> {}", cacheKey, name, e);
        }
      }
      if (l2Cache != null) {
        try {
          l2Cache.evict(key);
          statistics.recordL2Evict();
        } catch (Exception e) {
          log.warn("L2 evict 异常: {} -> {}", cacheKey, name, e);
        }
      }
      log.debug("缓存驱逐: {} -> {}", cacheKey, name);
    } catch (Exception e) {
      log.warn("缓存 evict 异常: {} -> {}", cacheKey, name, e);
    }
  }

  @Override
  public void clear() {
    try {
      if (l1Cache != null) {
        try {
          l1Cache.clear();
          log.debug("L1缓存清空成功: {}", name);
        } catch (Exception e) {
          log.warn("L1缓存清空失败: {}", name, e);
        }
      }
      if (l2Cache != null) {
        try {
          if (l2Cache.getNativeCache() instanceof RedisTemplate) {
            RedisTemplate<?, ?> redisTemplate = (RedisTemplate<?, ?>) l2Cache.getNativeCache();
            String pattern = name + "::*";
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            try {
              Set<byte[]> keys = new HashSet<>();
              Cursor<byte[]> cursor =
                  connection.scan(ScanOptions.scanOptions().match(pattern).count(100).build());
              while (cursor.hasNext()) {
                keys.add(cursor.next());
              }
              if (!keys.isEmpty()) {
                connection.del(keys.toArray(new byte[0][]));
                log.debug("L2缓存使用SCAN清空成功，删除{}个键: {}", keys.size(), name);
              } else {
                log.debug("L2缓存没有找到匹配的键: {}", name);
              }
            } finally {
              connection.close();
            }
          } else {
            l2Cache.clear();
            log.debug("L2缓存使用默认方式清空成功: {}", name);
          }
        } catch (Exception e) {
          log.warn("L2缓存清空失败，尝试使用默认方式: {}", name, e);
          try {
            l2Cache.clear();
            log.debug("L2缓存降级清空成功: {}", name);
          } catch (Exception fallbackException) {
            log.error("L2缓存降级清空也失败: {}", name, fallbackException);
          }
        }
      }
      log.info("缓存清空操作完成: {}", name);
    } catch (Exception e) {
      log.error("缓存清空操作失败: {}", name, e);
    }
  }

  /** 获取L1缓存 */
  public Cache getL1Cache() {
    return l1Cache;
  }

  /** 获取L2缓存 */
  public Cache getL2Cache() {
    return l2Cache;
  }

  /** 预热缓存 */
  public void warmUp(Object key, Object value) {
    if (key == null || value == null) {
      return;
    }

    String cacheKey = key.toString();

    // 预热L1缓存
    if (l1Cache != null) {
      l1Cache.put(key, value);
      log.debug("L1缓存预热: {} -> {}", cacheKey, name);
    }

    // 预热L2缓存
    if (l2Cache != null) {
      l2Cache.put(key, value);
      log.debug("L2缓存预热: {} -> {}", cacheKey, name);
    }
  }

  /** 获取缓存统计信息 */
  public CacheStatistics getCacheStatistics() {
    return statistics;
  }
}
