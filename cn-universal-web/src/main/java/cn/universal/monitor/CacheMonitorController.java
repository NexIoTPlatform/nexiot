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

package cn.universal.monitor;

import cn.universal.cache.manager.MultiLevelCacheManager;
import cn.universal.cache.statistics.CacheStatistics;
import cn.universal.dm.device.service.push.processor.PushRetryProcessor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 缓存监控控制器 提供缓存状态查询、清理等功能
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j
@RestController
@RequestMapping("/monitor/cache")
public class CacheMonitorController {

  private final CacheManager cacheManager;
  private final ThreadMonitor threadMonitor;
  private final ObjectProvider<PushRetryProcessor> pushRetryProcessorProvider;

  public CacheMonitorController(
      CacheManager cacheManager,
      ThreadMonitor threadMonitor,
      ObjectProvider<PushRetryProcessor> pushRetryProcessorProvider) {
    this.cacheManager = cacheManager;
    this.threadMonitor = threadMonitor;
    this.pushRetryProcessorProvider = pushRetryProcessorProvider;
  }

  /** 获取缓存统计信息 */
  @GetMapping("/stats")
  public Map<String, Object> getCacheStats() {
    Map<String, Object> result = new HashMap<>();

    try {
      if (cacheManager instanceof MultiLevelCacheManager) {
        MultiLevelCacheManager mlCacheManager = (MultiLevelCacheManager) cacheManager;
        Map<String, CacheStatistics> stats = mlCacheManager.getCacheStatistics();

        Map<String, Object> cacheStats = new HashMap<>();
        for (Map.Entry<String, CacheStatistics> entry : stats.entrySet()) {
          CacheStatistics stat = entry.getValue();
          Map<String, Object> statMap = new HashMap<>();
          statMap.put("l1Hits", stat.getL1Hits());
          statMap.put("l1Misses", stat.getL1Misses());
          statMap.put("l2Hits", stat.getL2Hits());
          statMap.put("l2Misses", stat.getL2Misses());
          statMap.put("l1HitRate", stat.getL1HitRate());
          statMap.put("l2HitRate", stat.getL2HitRate());
          statMap.put("totalHits", stat.getTotalHits());
          statMap.put("totalMisses", stat.getTotalMisses());
          statMap.put("overallHitRate", stat.getOverallHitRate());
          cacheStats.put(entry.getKey(), statMap);
        }
        result.put("multiLevelCacheStats", cacheStats);
      }

      // 获取线程统计
      ThreadMonitor.ThreadStats threadStats = threadMonitor.getThreadStats();
      Map<String, Object> threadInfo = new HashMap<>();
      threadInfo.put("totalThreads", threadStats.getTotalThreads());
      threadInfo.put("daemonThreads", threadStats.getDaemonThreads());
      threadInfo.put("peakThreads", threadStats.getPeakThreads());
      threadInfo.put("hasDeadlock", threadStats.isHasDeadlock());
      result.put("threadStats", threadInfo);

      result.put("success", true);

    } catch (Exception e) {
      log.error("获取缓存统计信息失败", e);
      result.put("success", false);
      result.put("error", e.getMessage());
    }

    return result;
  }

  /** 清理指定缓存 */
  @PostMapping("/clear/{cacheName}")
  public Map<String, Object> clearCache(@PathVariable String cacheName) {
    Map<String, Object> result = new HashMap<>();

    try {
      Cache cache = cacheManager.getCache(cacheName);
      if (cache != null) {
        cache.clear();
        log.info("缓存 {} 已清理", cacheName);
        result.put("success", true);
        result.put("message", "缓存 " + cacheName + " 已清理");
      } else {
        result.put("success", false);
        result.put("error", "缓存 " + cacheName + " 不存在");
      }
    } catch (Exception e) {
      log.error("清理缓存 {} 失败", cacheName, e);
      result.put("success", false);
      result.put("error", e.getMessage());
    }

    return result;
  }

  /** 清理所有缓存 */
  @PostMapping("/clear-all")
  public Map<String, Object> clearAllCaches() {
    Map<String, Object> result = new HashMap<>();

    try {
      if (cacheManager instanceof MultiLevelCacheManager) {
        MultiLevelCacheManager mlCacheManager = (MultiLevelCacheManager) cacheManager;
        mlCacheManager.clearAll();
        log.info("所有多级缓存已清理");
        result.put("success", true);
        result.put("message", "所有多级缓存已清理");
      } else {
        // 清理所有已知缓存
        final Collection<String> cacheNames = cacheManager.getCacheNames();
        for (String cacheName : cacheNames) {
          Cache cache = cacheManager.getCache(cacheName);
          if (cache != null) {
            cache.clear();
          }
        }
        log.info("所有缓存已清理");
        result.put("success", true);
        result.put("message", "所有缓存已清理");
      }
    } catch (Exception e) {
      log.error("清理所有缓存失败", e);
      result.put("success", false);
      result.put("error", e.getMessage());
    }

    return result;
  }

  /** 获取线程转储 */
  @GetMapping("/thread-dump")
  public Map<String, Object> getThreadDump() {
    Map<String, Object> result = new HashMap<>();

    try {
      String threadDump = threadMonitor.getThreadDump();
      result.put("success", true);
      result.put("threadDump", threadDump);
    } catch (Exception e) {
      log.error("获取线程转储失败", e);
      result.put("success", false);
      result.put("error", e.getMessage());
    }

    return result;
  }

  /** 检查系统健康状态 */
  @GetMapping("/health")
  public Map<String, Object> getHealthStatus() {
    Map<String, Object> result = new HashMap<>();

    try {
      ThreadMonitor.ThreadStats threadStats = threadMonitor.getThreadStats();

      Map<String, Object> health = new HashMap<>();
      health.put("threadCount", threadStats.getTotalThreads());
      health.put("hasDeadlock", threadStats.isHasDeadlock());
      health.put("status", threadStats.isHasDeadlock() ? "UNHEALTHY" : "HEALTHY");

      // 检查缓存管理器状态
      health.put("cacheManagerType", cacheManager.getClass().getSimpleName());
      health.put("cacheCount", cacheManager.getCacheNames().size());

      result.put("success", true);
      result.put("health", health);

    } catch (Exception e) {
      log.error("获取健康状态失败", e);
      result.put("success", false);
      result.put("error", e.getMessage());
    }

    return result;
  }

  /** 获取推送重试线程池状态 */
  @GetMapping("/push-retry/thread-pool")
  public Map<String, Object> getPushRetryThreadPoolStatus() {
    Map<String, Object> result = new HashMap<>();

    try {
      PushRetryProcessor pushRetryProcessor = pushRetryProcessorProvider.getIfAvailable();
      if (pushRetryProcessor == null) {
        result.put("success", true);
        result.put("enabled", false);
        result.put("threadPoolStatus", "Push retry is disabled");
        result.put("timestamp", System.currentTimeMillis());
        return result;
      }
      String threadPoolStatus = pushRetryProcessor.getThreadPoolStatus();

      result.put("success", true);
      result.put("enabled", true);
      result.put("threadPoolStatus", threadPoolStatus);
      result.put("timestamp", System.currentTimeMillis());

      log.info("[监控API] 获取推送重试线程池状态: {}", threadPoolStatus);

    } catch (Exception e) {
      log.error("[监控API] 获取推送重试线程池状态失败", e);
      result.put("success", false);
      result.put("error", e.getMessage());
    }

    return result;
  }
}
