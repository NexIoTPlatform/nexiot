package cn.universal.dm.device.service;

import cn.universal.dm.device.service.push.processor.PushStatisticsProcessor;
import cn.universal.persistence.entity.IoTDashboardStatistics;
import cn.universal.persistence.mapper.IoTDashboardStatisticsMapper;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 仪表盘统计定时任务
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@Component
public class DashboardStatisticsTask implements ApplicationRunner {

  @Autowired private IoTDashboardStatisticsMapper dashboardStatisticsMapper;

  @Autowired private StringRedisTemplate redisTemplate;

  @Autowired private IoTDeviceMapper ioTDeviceMapper;

  @Autowired private DashboardService dashboardService;

  @Autowired private PushStatisticsProcessor pushStatisticsProcessor;

  // 分布式锁key前缀
  private static final String LOCK_KEY_PREFIX = "dashboard:statistics:lock:";

  // 执行状态key前缀
  private static final String EXECUTION_STATUS_PREFIX = "dashboard:statistics:status:";

  // 锁超时时间（秒）
  private static final long LOCK_TIMEOUT = 300; // 5分钟

  // 最小执行间隔（秒）
  private static final long MIN_EXECUTION_INTERVAL = 300; // 5分钟

  /** 获取所有平台列表 */
  private List<String> getAllPlatforms() {
    try {
      // 从PushStatisticsProcessor的本地缓存中获取活跃的平台
      List<String> platforms = pushStatisticsProcessor.getActivePlatforms();

      // 如果本地缓存中没有平台数据，使用默认的平台列表
      if (platforms.isEmpty()) {
        platforms = Arrays.asList("ctaiot", "ezviz", "onenet", "imoulife", "tcp", "snitcp");
        log.warn("[仪表盘统计] 本地缓存中无活跃平台，使用默认平台列表");
      }

      log.debug("[仪表盘统计] 获取到 {} 个活跃平台: {}", platforms.size(), platforms);
      return platforms;

    } catch (Exception e) {
      log.error("[仪表盘统计] 获取活跃平台失败", e);
      // 异常时返回默认平台列表
      return Arrays.asList("ctaiot", "ezviz", "onenet", "imoulife", "tcp", "snitcp");
    }
  }

  /** 应用启动时初始化统计数据 */
  @Override
  public void run(ApplicationArguments args) throws Exception {
    log.debug("[仪表盘统计] 应用启动，开始初始化统计数据");

    try {
      // 延迟5秒执行，确保所有组件都已初始化
      Thread.sleep(5000);

      // 执行一次初始刷新
      refreshDatabaseStatisticsWithLock();

      log.debug("[仪表盘统计] 统计数据初始化完成");
    } catch (Exception e) {
      log.error("[仪表盘统计] 统计数据初始化失败", e);
    }
  }

  /** 每10分钟刷新数据库统计数据（带分布式锁） */
  @Scheduled(fixedRate = 600000) // 10分钟 = 600000毫秒
  public void refreshDatabaseStatistics() {
    refreshDatabaseStatisticsWithLock();
  }

  /** 带分布式锁的数据库统计刷新 */
  private void refreshDatabaseStatisticsWithLock() {
    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    String lockKey = LOCK_KEY_PREFIX + today;
    String statusKey = EXECUTION_STATUS_PREFIX + today;

    // 检查是否已经执行过
    String lastExecutionTime = redisTemplate.opsForValue().get(statusKey);
    if (lastExecutionTime != null) {
      try {
        long lastTime = Long.parseLong(lastExecutionTime);
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime < MIN_EXECUTION_INTERVAL * 1000) {
          log.debug("[仪表盘统计] 距离上次执行时间不足{}秒，跳过本次执行", MIN_EXECUTION_INTERVAL);
          return;
        }
      } catch (NumberFormatException e) {
        log.warn("[仪表盘统计] 解析上次执行时间失败", e);
      }
    }

    // 尝试获取分布式锁
    Boolean lockAcquired =
        redisTemplate
            .opsForValue()
            .setIfAbsent(
                lockKey,
                String.valueOf(System.currentTimeMillis()),
                LOCK_TIMEOUT,
                TimeUnit.SECONDS);

    if (Boolean.TRUE.equals(lockAcquired)) {
      try {
        log.debug("[仪表盘统计] 获取到分布式锁，开始执行10分钟数据库刷新");

        LocalDate todayDate = LocalDate.now();

        // 刷新平台消息统计数据
        refreshPlatformMessageStatistics(todayDate);

        // 刷新产品消息统计数据
        refreshProductMessageStatistics(todayDate);

        // 记录执行时间
        redisTemplate
            .opsForValue()
            .set(
                statusKey,
                String.valueOf(System.currentTimeMillis()),
                LOCK_TIMEOUT,
                TimeUnit.SECONDS);

        log.debug("[仪表盘统计] 10分钟数据库刷新完成");
      } catch (Exception e) {
        log.error("[仪表盘统计] 10分钟数据库刷新失败", e);
      } finally {
        // 释放锁
        redisTemplate.delete(lockKey);
        log.debug("[仪表盘统计] 释放分布式锁");
      }
    } else {
      log.debug("[仪表盘统计] 未获取到分布式锁，跳过本次执行");
    }
  }

  /** 刷新平台消息统计数据 */
  private void refreshPlatformMessageStatistics(LocalDate date) {
    try {
      List<String> platforms = getAllPlatforms();
      String dateKey = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

      for (String platform : platforms) {
        // 查询平台消息统计 - 每个平台有独立的key
        String platformKey = String.format("push:platform:%s:%s", dateKey, platform);

        // 从Redis Hash中获取平台统计数据
        Object totalObj = redisTemplate.opsForHash().get(platformKey, "total_count");
        Object successObj = redisTemplate.opsForHash().get(platformKey, "success_count");
        Object failedObj = redisTemplate.opsForHash().get(platformKey, "failed_count");
        Object retryObj = redisTemplate.opsForHash().get(platformKey, "retry_count");
        Object pushObj = redisTemplate.opsForHash().get(platformKey, "push_count");

        long totalValue = totalObj != null ? Long.parseLong(totalObj.toString()) : 0L;
        long successValue = successObj != null ? Long.parseLong(successObj.toString()) : 0L;
        long failedValue = failedObj != null ? Long.parseLong(failedObj.toString()) : 0L;
        long retryValue = retryObj != null ? Long.parseLong(retryObj.toString()) : 0L;
        long pushValue = pushObj != null ? Long.parseLong(pushObj.toString()) : 0L;

        // 更新平台消息总数
        IoTDashboardStatistics platformTotal =
            IoTDashboardStatistics.builder()
                .statDate(date)
                .productKey(null)
                .channel(platform.toUpperCase())
                .metricType(IoTDashboardStatistics.MetricType.MESSAGE_TOTAL.getCode())
                .metricValue(totalValue)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        log.info(
            "[仪表盘统计] 平台统计 - 日期:{}, 产品:{}, 渠道:{}, 类型:{}, 值:{}",
            platformTotal.getStatDate(),
            platformTotal.getProductKey(),
            platformTotal.getChannel(),
            platformTotal.getMetricType(),
            platformTotal.getMetricValue());

        // 执行前记录
        log.debug("[仪表盘统计] 准备执行recordStatistics，平台: {}", platform);
        dashboardService.recordStatistics(platformTotal);
        log.debug("[仪表盘统计] recordStatistics执行完成，平台: {}", platform);

        // 更新平台推送消息数
        IoTDashboardStatistics platformPush =
            IoTDashboardStatistics.builder()
                .statDate(date)
                .productKey(null)
                .channel(platform.toUpperCase())
                .metricType(IoTDashboardStatistics.MetricType.MESSAGE_PUSH.getCode())
                .metricValue(pushValue)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        dashboardService.recordStatistics(platformPush);

        // 更新平台成功消息数
        IoTDashboardStatistics platformSuccess =
            IoTDashboardStatistics.builder()
                .statDate(date)
                .productKey(null)
                .channel(platform.toUpperCase())
                .metricType(IoTDashboardStatistics.MetricType.MESSAGE_SUCCESS.getCode())
                .metricValue(successValue)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        dashboardService.recordStatistics(platformSuccess);

        // 更新平台失败消息数
        IoTDashboardStatistics platformFailed =
            IoTDashboardStatistics.builder()
                .statDate(date)
                .productKey(null)
                .channel(platform.toUpperCase())
                .metricType(IoTDashboardStatistics.MetricType.MESSAGE_FAILED.getCode())
                .metricValue(failedValue)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        dashboardService.recordStatistics(platformFailed);

        // 更新平台重试消息数
        IoTDashboardStatistics platformRetry =
            IoTDashboardStatistics.builder()
                .statDate(date)
                .productKey(null)
                .channel(platform.toUpperCase())
                .metricType(IoTDashboardStatistics.MetricType.MESSAGE_RETRY.getCode())
                .metricValue(retryValue)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        dashboardService.recordStatistics(platformRetry);

        log.debug(
            "[仪表盘统计] 平台{}统计刷新完成: 总数={}, 推送={}, 成功={}, 失败={}, 重试={}",
            platform,
            totalValue,
            pushValue,
            successValue,
            failedValue,
            retryValue);
      }
    } catch (Exception e) {
      log.error("[仪表盘统计] 平台消息统计刷新失败", e);
    }
  }

  /** 刷新产品消息统计数据 */
  private void refreshProductMessageStatistics(LocalDate date) {
    try {
      // 获取所有活跃的产品key
      List<String> productKeys = getActiveProductKeys();
      String dateKey = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

      for (String productKey : productKeys) {
        // 查询产品消息统计
        String productStatsKey = String.format("push:product:%s:%s", dateKey, productKey);

        // 从Redis Hash中获取产品统计数据
        Object totalObj = redisTemplate.opsForHash().get(productStatsKey, "total");
        Object successObj = redisTemplate.opsForHash().get(productStatsKey, "success");
        Object failedObj = redisTemplate.opsForHash().get(productStatsKey, "failed");
        Object retryObj = redisTemplate.opsForHash().get(productStatsKey, "retry");
        Object pushObj = redisTemplate.opsForHash().get(productStatsKey, "push");

        long totalValue = totalObj != null ? Long.parseLong(totalObj.toString()) : 0L;
        long successValue = successObj != null ? Long.parseLong(successObj.toString()) : 0L;
        long failedValue = failedObj != null ? Long.parseLong(failedObj.toString()) : 0L;
        long retryValue = retryObj != null ? Long.parseLong(retryObj.toString()) : 0L;
        long pushValue = pushObj != null ? Long.parseLong(pushObj.toString()) : 0L;

        // 更新产品消息总数
        IoTDashboardStatistics productTotal =
            IoTDashboardStatistics.builder()
                .statDate(date)
                .productKey(productKey)
                .channel(null)
                .metricType(IoTDashboardStatistics.MetricType.MESSAGE_TOTAL.getCode())
                .metricValue(totalValue)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        dashboardService.recordStatistics(productTotal);

        // 更新产品推送消息数
        IoTDashboardStatistics productPush =
            IoTDashboardStatistics.builder()
                .statDate(date)
                .productKey(productKey)
                .channel(null)
                .metricType(IoTDashboardStatistics.MetricType.MESSAGE_PUSH.getCode())
                .metricValue(pushValue)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        dashboardService.recordStatistics(productPush);

        // 更新产品成功消息数
        IoTDashboardStatistics productSuccess =
            IoTDashboardStatistics.builder()
                .statDate(date)
                .productKey(productKey)
                .channel(null)
                .metricType(IoTDashboardStatistics.MetricType.MESSAGE_SUCCESS.getCode())
                .metricValue(successValue)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        dashboardService.recordStatistics(productSuccess);

        // 更新产品失败消息数
        IoTDashboardStatistics productFailed =
            IoTDashboardStatistics.builder()
                .statDate(date)
                .productKey(productKey)
                .channel(null)
                .metricType(IoTDashboardStatistics.MetricType.MESSAGE_FAILED.getCode())
                .metricValue(failedValue)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        dashboardService.recordStatistics(productFailed);

        // 更新产品重试消息数
        IoTDashboardStatistics productRetry =
            IoTDashboardStatistics.builder()
                .statDate(date)
                .productKey(productKey)
                .channel(null)
                .metricType(IoTDashboardStatistics.MetricType.MESSAGE_RETRY.getCode())
                .metricValue(retryValue)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        dashboardService.recordStatistics(productRetry);

        log.debug(
            "[仪表盘统计] 产品{}统计刷新完成: 总数={}, 推送={}, 成功={}, 失败={}, 重试={}",
            productKey,
            totalValue,
            pushValue,
            successValue,
            failedValue,
            retryValue);
      }
    } catch (Exception e) {
      log.error("[仪表盘统计] 产品消息统计刷新失败", e);
    }
  }

  /** 获取活跃的产品key列表 */
  private List<String> getActiveProductKeys() {
    try {
      // 从PushStatisticsProcessor的本地缓存中获取活跃的产品key
      List<String> productKeys = pushStatisticsProcessor.getActiveProductKeys();
      log.info(
          "[仪表盘统计] 从PushStatisticsProcessor获取到 {} 个活跃产品key: {}", productKeys.size(), productKeys);
      return productKeys;

    } catch (Exception e) {
      log.error("[仪表盘统计] 获取活跃产品key失败", e);
      // 异常时返回空列表，不进行任何统计
      return new ArrayList<>();
    }
  }

  /** 手动刷新统计数据 */
  public void manualRefreshStatistics() {
    log.debug("[仪表盘统计] 手动触发统计数据刷新");
    refreshDatabaseStatisticsWithLock();
  }

  /** 检查指定日期的统计数据是否存在 */
  public boolean checkStatisticsExists(LocalDate date) {
    try {
      // 检查是否存在当天的MESSAGE_TOTAL记录
      IoTDashboardStatistics query =
          IoTDashboardStatistics.builder()
              .statDate(date)
              .productKey(null)
              .channel(null)
              .metricType(IoTDashboardStatistics.MetricType.MESSAGE_TOTAL.getCode())
              .build();

      List<IoTDashboardStatistics> existingStats = dashboardStatisticsMapper.select(query);
      return !existingStats.isEmpty();
    } catch (Exception e) {
      log.error("[仪表盘统计] 检查统计数据存在性失败", e);
      return false;
    }
  }

  /** 清理重复的统计数据 */
  public void cleanupDuplicateStatistics(LocalDate date) {
    try {
      log.debug("[仪表盘统计] 开始清理{}的重复统计数据", date);

      // 删除指定日期的所有统计数据
      int deletedCount = dashboardStatisticsMapper.deleteByDate(date);

      log.debug("[仪表盘统计] 清理完成，删除了{}条重复记录", deletedCount);
    } catch (Exception e) {
      log.error("[仪表盘统计] 清理重复统计数据失败", e);
    }
  }
}
