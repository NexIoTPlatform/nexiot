package cn.universal.dm.device.service.push.processor;

import cn.universal.dm.device.entity.IoTPushResult;
import cn.universal.dm.device.service.push.UPProcessor;
import cn.universal.persistence.base.BaseUPRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 推送统计处理器 基于UPProcessor架构实现推送统计功能
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@Component
public class PushStatisticsProcessor implements UPProcessor<BaseUPRequest> {

  @Autowired private StringRedisTemplate redisTemplate;

  // 本地原子计数器 - 使用ConcurrentHashMap保证线程安全
  private final Map<String, AtomicLong> localCounters = new ConcurrentHashMap<>();

  // 本地平台统计缓存
  private final Map<String, Map<String, AtomicLong>> localPlatformStats = new ConcurrentHashMap<>();

  // 本地产品统计缓存
  private final Map<String, Map<String, AtomicLong>> localProductStats = new ConcurrentHashMap<>();

  // 定时刷新间隔（毫秒）
  private static final long FLUSH_INTERVAL = 300000; // 5分钟 = 300000毫秒

  // 上次刷新时间戳
  private volatile long lastFlushTime = System.currentTimeMillis();

  private static final String PUSH_PRODUCT_KEY_PREFIX = "push:product:";
  private static final String PUSH_PLATFORM_KEY_PREFIX = "push:platform:";
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  @Override
  public String getName() {
    return "PushStatisticsProcessor";
  }

  @Override
  public String getDescription() {
    return "推送统计处理器";
  }

  @Override
  public int getOrder() {
    return 100; // 较低优先级，在推送后执行
  }

  @Override
  public boolean isBeforePushEnabled() {
    return false;
  }

  @Override
  public void afterPush(List<BaseUPRequest> upRequests, List<IoTPushResult> pushResults) {
    if (upRequests == null || upRequests.isEmpty()) {
      return;
    }
    try {
      // 统计只写本地原子计数器，保持同步聚合，避免每条消息继续扇出 common pool 任务。
      for (BaseUPRequest request : upRequests) {
        recordTotalMessageFromRequest(request);
      }

      // 如果有推送结果，统计实际推送数
      // 只统计实际执行了推送操作的消息
      if (pushResults != null && !pushResults.isEmpty()) {
        for (IoTPushResult result : pushResults) {
          if (result != null) {
            recordPushMessage(result);
          }
        }
      }

      log.debug("[推送统计] 完成 {} 条推送请求的统计记录", upRequests.size());
    } catch (Exception e) {
      log.error("[推送统计] 统计处理失败", e);
    }
  }

  /** 记录推送结果统计 */
  public void recordPushResult(IoTPushResult result) {
    if (result == null) {
      log.debug("[推送统计] 跳过空结果统计");
      return;
    }

    // 检查必要字段是否为空
    if (result.getProductKey() == null || result.getChannel() == null) {
      log.warn(
          "[推送统计] 跳过无效结果统计: productKey={}, channel={}",
          result.getProductKey(),
          result.getChannel());
      return;
    }

    try {
      String productKey = result.getProductKey();
      String channel = result.getChannel();
      String dateKey = LocalDate.now().format(DATE_FORMATTER);

      // 记录到本地缓存
      recordPlatformStatsToLocal(result, dateKey);
      recordProductStatsToLocal(result, dateKey, productKey);

      // 记录消息统计
      incrementMessageStats(
          channel,
          productKey,
          result.isOk(),
          result.getResponseTime() == null ? 0L : result.getResponseTime());

      log.debug(
          "[推送统计] 记录推送结果: productKey={}, channel={}, success={}, responseTime={}ms",
          productKey,
          channel,
          result.isOk(),
          result.getResponseTime());

    } catch (Exception e) {
      log.error("[推送统计] 记录推送结果失败: {}", result, e);
    }
  }

  /** 记录消息总数统计（无论结果是否为空） */
  private void recordTotalMessage(IoTPushResult result) {
    try {
      String productKey = result != null ? result.getProductKey() : "unknown";
      String channel = result != null ? result.getChannel() : "unknown";
      String dateKey = LocalDate.now().format(DATE_FORMATTER);

      // 记录到本地缓存 - 总数统计
      recordPlatformStatsToLocalForTotal(result, dateKey);
      recordProductStatsToLocalForTotal(result, dateKey, productKey);

      log.debug("[推送统计] 记录消息总数: productKey={}, channel={}", productKey, channel);

    } catch (Exception e) {
      log.error("[推送统计] 记录消息总数失败: {}", result, e);
    }
  }

  /** 记录推送消息数统计（仅非空结果） */
  private void recordPushMessage(IoTPushResult result) {
    if (result == null) {
      return;
    }

    try {
      String productKey = result.getProductKey();
      String channel = result.getChannel();
      String dateKey = LocalDate.now().format(DATE_FORMATTER);

      // 记录到本地缓存 - 推送数统计
      // 只统计实际执行了推送操作的消息
      recordPlatformStatsToLocalForPush(result, dateKey);
      recordProductStatsToLocalForPush(result, dateKey, productKey);

      log.debug(
          "[推送统计] 记录推送消息数: productKey={}, channel={}, success={}",
          productKey,
          channel,
          result.isOk());

    } catch (Exception e) {
      log.error("[推送统计] 记录推送消息数失败: {}", result, e);
    }
  }

  /** 记录消息总数统计（基于请求） */
  private void recordTotalMessageFromRequest(BaseUPRequest request) {
    try {
      String productKey = request.getProductKey();
      String channel = getChannelFromRequest(request);
      String dateKey = LocalDate.now().format(DATE_FORMATTER);

      // 记录到本地缓存 - 总数统计
      recordPlatformStatsToLocalForTotalFromRequest(request, dateKey);
      recordProductStatsToLocalForTotalFromRequest(request, dateKey, productKey);

      log.debug("[推送统计] 记录消息总数: productKey={}, channel={}", productKey, channel);

    } catch (Exception e) {
      log.error("[推送统计] 记录消息总数失败: {}", request, e);
    }
  }

  /** 从请求中获取channel信息 */
  private String getChannelFromRequest(BaseUPRequest request) {
    // 从设备DTO中获取thirdPlatform作为channel
    if (request.getIoTDeviceDTO() != null && request.getIoTDeviceDTO().getThirdPlatform() != null) {
      return request.getIoTDeviceDTO().getThirdPlatform().toLowerCase();
    }
    return "unknown";
  }

  /** 从请求中获取platform信息 */
  private String getPlatformFromRequest(BaseUPRequest request) {
    // 从设备DTO中获取thirdPlatform作为platform
    if (request.getIoTDeviceDTO() != null && request.getIoTDeviceDTO().getThirdPlatform() != null) {
      return request.getIoTDeviceDTO().getThirdPlatform();
    }
    return "unknown";
  }

  /** 记录平台统计到本地缓存 */
  private void recordPlatformStatsToLocal(IoTPushResult result, String dateKey) {
    String platform = result.getPlatform();
    if (platform == null || platform.isEmpty()) {
      // 当平台信息为空时，使用默认值而不是跳过统计
      platform = "unknown";
      log.info("[推送统计] 推送结果中缺少平台信息，使用默认值: {}", platform);
    }

    String platformKey = dateKey + ":" + platform.toLowerCase();
    Map<String, AtomicLong> platformStats =
        localPlatformStats.computeIfAbsent(platformKey, k -> new ConcurrentHashMap<>());

    // 记录总数
    platformStats.computeIfAbsent("total_count", k -> new AtomicLong(0)).incrementAndGet();

    if (result.isOk()) {
      platformStats.computeIfAbsent("success_count", k -> new AtomicLong(0)).incrementAndGet();
    } else {
      platformStats.computeIfAbsent("failed_count", k -> new AtomicLong(0)).incrementAndGet();
    }

    // 记录重试次数
    if (result.getRetryCount() != null && result.getRetryCount() > 0) {
      platformStats
          .computeIfAbsent("retry_count", k -> new AtomicLong(0))
          .addAndGet(result.getRetryCount());
    }
  }

  /** 记录产品统计到本地缓存 */
  private void recordProductStatsToLocal(IoTPushResult result, String dateKey, String productKey) {
    String productStatsKey = dateKey + ":" + productKey;
    Map<String, AtomicLong> productStats =
        localProductStats.computeIfAbsent(productStatsKey, k -> new ConcurrentHashMap<>());

    // 记录总数
    productStats.computeIfAbsent("total", k -> new AtomicLong(0)).incrementAndGet();

    if (result.isOk()) {
      productStats.computeIfAbsent("success", k -> new AtomicLong(0)).incrementAndGet();
    } else {
      productStats.computeIfAbsent("failed", k -> new AtomicLong(0)).incrementAndGet();
    }

    // 记录重试次数
    if (result.getRetryCount() != null && result.getRetryCount() > 0) {
      productStats
          .computeIfAbsent("retry", k -> new AtomicLong(0))
          .addAndGet(result.getRetryCount());
    }
  }

  /** 记录平台统计到本地缓存 - 总数统计 */
  private void recordPlatformStatsToLocalForTotal(IoTPushResult result, String dateKey) {
    String platform = result != null ? result.getPlatform() : "unknown";
    if (platform == null || platform.isEmpty()) {
      platform = "unknown";
    }

    String platformKey = dateKey + ":" + platform.toLowerCase();
    Map<String, AtomicLong> platformStats =
        localPlatformStats.computeIfAbsent(platformKey, k -> new ConcurrentHashMap<>());

    // 记录总数
    platformStats.computeIfAbsent("total_count", k -> new AtomicLong(0)).incrementAndGet();
  }

  /** 记录产品统计到本地缓存 - 总数统计 */
  private void recordProductStatsToLocalForTotal(
      IoTPushResult result, String dateKey, String productKey) {
    String productStatsKey = dateKey + ":" + productKey;
    Map<String, AtomicLong> productStats =
        localProductStats.computeIfAbsent(productStatsKey, k -> new ConcurrentHashMap<>());

    // 记录总数
    productStats.computeIfAbsent("total", k -> new AtomicLong(0)).incrementAndGet();
  }

  /** 记录平台统计到本地缓存 - 总数统计（基于请求） */
  private void recordPlatformStatsToLocalForTotalFromRequest(
      BaseUPRequest request, String dateKey) {
    String platform = getPlatformFromRequest(request);
    if (platform == null || platform.isEmpty()) {
      platform = "unknown";
    }

    String platformKey = dateKey + ":" + platform.toLowerCase();
    Map<String, AtomicLong> platformStats =
        localPlatformStats.computeIfAbsent(platformKey, k -> new ConcurrentHashMap<>());

    // 记录总数
    platformStats.computeIfAbsent("total_count", k -> new AtomicLong(0)).incrementAndGet();
  }

  /** 记录产品统计到本地缓存 - 总数统计（基于请求） */
  private void recordProductStatsToLocalForTotalFromRequest(
      BaseUPRequest request, String dateKey, String productKey) {
    String productStatsKey = dateKey + ":" + productKey;
    Map<String, AtomicLong> productStats =
        localProductStats.computeIfAbsent(productStatsKey, k -> new ConcurrentHashMap<>());

    // 记录总数
    productStats.computeIfAbsent("total", k -> new AtomicLong(0)).incrementAndGet();
  }

  /** 记录平台统计到本地缓存 - 推送数统计 */
  private void recordPlatformStatsToLocalForPush(IoTPushResult result, String dateKey) {
    String platform = result.getPlatform();
    if (platform == null || platform.isEmpty()) {
      // 当平台信息为空时，使用默认值而不是跳过统计
      platform = "unknown";
      log.info("[推送统计] 推送结果中缺少平台信息，使用默认值: {}", platform);
    }

    String platformKey = dateKey + ":" + platform.toLowerCase();
    Map<String, AtomicLong> platformStats =
        localPlatformStats.computeIfAbsent(platformKey, k -> new ConcurrentHashMap<>());

    // 记录推送数
    platformStats.computeIfAbsent("push_count", k -> new AtomicLong(0)).incrementAndGet();

    if (result.isOk()) {
      platformStats.computeIfAbsent("success_count", k -> new AtomicLong(0)).incrementAndGet();
    } else {
      platformStats.computeIfAbsent("failed_count", k -> new AtomicLong(0)).incrementAndGet();
    }

    // 记录重试次数
    if (result.getRetryCount() != null && result.getRetryCount() > 0) {
      platformStats
          .computeIfAbsent("retry_count", k -> new AtomicLong(0))
          .addAndGet(result.getRetryCount());
    }
  }

  /** 记录产品统计到本地缓存 - 推送数统计 */
  private void recordProductStatsToLocalForPush(
      IoTPushResult result, String dateKey, String productKey) {
    String productStatsKey = dateKey + ":" + productKey;
    Map<String, AtomicLong> productStats =
        localProductStats.computeIfAbsent(productStatsKey, k -> new ConcurrentHashMap<>());

    // 记录推送数
    productStats.computeIfAbsent("push", k -> new AtomicLong(0)).incrementAndGet();

    if (result.isOk()) {
      productStats.computeIfAbsent("success", k -> new AtomicLong(0)).incrementAndGet();
    } else {
      productStats.computeIfAbsent("failed", k -> new AtomicLong(0)).incrementAndGet();
    }

    // 记录重试次数
    if (result.getRetryCount() != null && result.getRetryCount() > 0) {
      productStats
          .computeIfAbsent("retry", k -> new AtomicLong(0))
          .addAndGet(result.getRetryCount());
    }
  }

  /** 原子自增消息统计 */
  private void incrementMessageStats(
      String channel, String productKey, boolean isSuccess, long responseTime) {
    LocalDate today = LocalDate.now();
    String dateKey = today.format(DATE_FORMATTER);

    // 消息总数
    String totalKey =
        String.format(
            "dashboard:metric:%s:all:%s:%s:message_total",
            dateKey, channel != null ? channel : "all", productKey != null ? productKey : "all");
    incrementMetric(totalKey, 1);

    // 成功/失败消息数
    String successKey =
        String.format(
            "dashboard:metric:%s:all:%s:%s:message_%s",
            dateKey,
            channel != null ? channel : "all",
            productKey != null ? productKey : "all",
            isSuccess ? "success" : "failed");
    incrementMetric(successKey, 1);

    // 响应时间统计
    if (responseTime > 0) {
      String responseTimeKey =
          String.format(
              "dashboard:metric:%s:all:%s:%s:response_time_total",
              dateKey, channel != null ? channel : "all", productKey != null ? productKey : "all");
      String responseCountKey =
          String.format(
              "dashboard:metric:%s:all:%s:%s:response_count",
              dateKey, channel != null ? channel : "all", productKey != null ? productKey : "all");

      incrementMetric(responseTimeKey, responseTime);
      incrementMetric(responseCountKey, 1);
    }
  }

  /** 原子自增统计 */
  private void incrementMetric(String metricKey, long increment) {
    AtomicLong counter = localCounters.computeIfAbsent(metricKey, k -> new AtomicLong(0));
    counter.addAndGet(increment);

    log.debug(
        "[推送统计] 本地计数器自增: key={}, increment={}, current={}", metricKey, increment, counter.get());
  }

  /** 定时刷新到Redis（每5分钟执行一次） */
  @Scheduled(fixedRate = FLUSH_INTERVAL)
  public void flushToRedis() {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastFlushTime < FLUSH_INTERVAL) {
      return; // 避免重复执行
    }

    log.debug("[推送统计] 开始定时刷新到Redis");

    try {
      int flushedCount = 0;

      // 刷新平台统计
      flushedCount += flushPlatformStatsToRedis();

      // 刷新产品统计
      flushedCount += flushProductStatsToRedis();

      // 刷新消息统计
      flushedCount += flushMessageStatsToRedis();

      lastFlushTime = currentTime;
      log.info("[推送统计] 定时刷新完成，刷新了 {} 个指标", flushedCount);

    } catch (Exception e) {
      log.error("[推送统计] 定时刷新到Redis失败", e);
    }
  }

  /** 刷新平台统计到Redis */
  private int flushPlatformStatsToRedis() {
    int flushedCount = 0;

    for (Map.Entry<String, Map<String, AtomicLong>> entry : localPlatformStats.entrySet()) {
      String platformKey = entry.getKey();
      Map<String, AtomicLong> stats = entry.getValue();

      String[] parts = platformKey.split(":");
      if (parts.length != 2) {
        continue;
      }

      String dateKey = parts[0];
      String platform = parts[1];
      String redisKey = PUSH_PLATFORM_KEY_PREFIX + dateKey + ":" + platform;

      try {
        for (Map.Entry<String, AtomicLong> statEntry : stats.entrySet()) {
          String field = statEntry.getKey();
          AtomicLong counter = statEntry.getValue();

          long currentValue = counter.get();
          if (currentValue > 0) {
            // 使用Redis HINCRBY原子操作
            redisTemplate.opsForHash().increment(redisKey, field, currentValue);

            // 重置本地计数器
            counter.addAndGet(-currentValue);

            flushedCount++;
            log.debug(
                "[推送统计] 刷新平台统计到Redis: key={}, field={}, value={}", redisKey, field, currentValue);
          }
        }

        // 设置过期时间（30天）
        redisTemplate.expire(redisKey, java.time.Duration.ofDays(30));

      } catch (Exception e) {
        log.error("[推送统计] 刷新平台统计失败: {}", platformKey, e);
      }
    }

    return flushedCount;
  }

  /** 刷新产品统计到Redis */
  private int flushProductStatsToRedis() {
    int flushedCount = 0;

    for (Map.Entry<String, Map<String, AtomicLong>> entry : localProductStats.entrySet()) {
      String productStatsKey = entry.getKey();
      Map<String, AtomicLong> stats = entry.getValue();

      String[] parts = productStatsKey.split(":");
      if (parts.length != 2) {
        continue;
      }

      String dateKey = parts[0];
      String productKey = parts[1];
      String redisKey = PUSH_PRODUCT_KEY_PREFIX + dateKey + ":" + productKey;

      try {
        for (Map.Entry<String, AtomicLong> statEntry : stats.entrySet()) {
          String field = statEntry.getKey();
          AtomicLong counter = statEntry.getValue();

          long currentValue = counter.get();
          if (currentValue > 0) {
            // 使用Redis HINCRBY原子操作
            redisTemplate.opsForHash().increment(redisKey, field, currentValue);

            // 重置本地计数器
            counter.addAndGet(-currentValue);

            flushedCount++;
            log.debug(
                "[推送统计] 刷新产品统计到Redis: key={}, field={}, value={}", redisKey, field, currentValue);
          }
        }

        // 设置过期时间（30天）
        redisTemplate.expire(redisKey, java.time.Duration.ofDays(30));

      } catch (Exception e) {
        log.error("[推送统计] 刷新产品统计失败: {}", productStatsKey, e);
      }
    }

    return flushedCount;
  }

  /** 刷新消息统计到Redis */
  private int flushMessageStatsToRedis() {
    int flushedCount = 0;

    for (Map.Entry<String, AtomicLong> entry : localCounters.entrySet()) {
      String metricKey = entry.getKey();
      AtomicLong counter = entry.getValue();

      long currentValue = counter.get();
      if (currentValue > 0) {
        // 使用Redis INCRBY原子操作
        Long newValue = redisTemplate.opsForValue().increment(metricKey, currentValue);

        // 重置本地计数器
        counter.addAndGet(-currentValue);

        // 设置过期时间（7天）
        redisTemplate.expire(metricKey, java.time.Duration.ofDays(7));

        flushedCount++;
        log.debug(
            "[推送统计] 刷新指标到Redis: key={}, value={}, newTotal={}", metricKey, currentValue, newValue);
      }
    }

    return flushedCount;
  }

  /** 强制刷新到Redis */
  public void forceFlushToRedis() {
    log.info("[推送统计] 强制刷新到Redis");
    flushToRedis();
  }

  /** 获取本地计数器状态 */
  public Map<String, Long> getLocalCounterStatus() {
    Map<String, Long> status = new ConcurrentHashMap<>();
    for (Map.Entry<String, AtomicLong> entry : localCounters.entrySet()) {
      status.put(entry.getKey(), entry.getValue().get());
    }
    return status;
  }

  /** 获取本地平台统计状态 */
  public Map<String, Map<String, Long>> getLocalPlatformStatsStatus() {
    Map<String, Map<String, Long>> status = new ConcurrentHashMap<>();
    for (Map.Entry<String, Map<String, AtomicLong>> entry : localPlatformStats.entrySet()) {
      Map<String, Long> platformStatus = new ConcurrentHashMap<>();
      for (Map.Entry<String, AtomicLong> statEntry : entry.getValue().entrySet()) {
        platformStatus.put(statEntry.getKey(), statEntry.getValue().get());
      }
      status.put(entry.getKey(), platformStatus);
    }
    return status;
  }

  /** 获取本地产品统计状态 */
  public Map<String, Map<String, Long>> getLocalProductStatsStatus() {
    Map<String, Map<String, Long>> status = new ConcurrentHashMap<>();
    for (Map.Entry<String, Map<String, AtomicLong>> entry : localProductStats.entrySet()) {
      Map<String, Long> productStatus = new ConcurrentHashMap<>();
      for (Map.Entry<String, AtomicLong> statEntry : entry.getValue().entrySet()) {
        productStatus.put(statEntry.getKey(), statEntry.getValue().get());
      }
      status.put(entry.getKey(), productStatus);
    }
    return status;
  }

  /** 清理本地计数器 */
  public void clearLocalCounters() {
    localCounters.clear();
    localPlatformStats.clear();
    localProductStats.clear();
    log.info("[推送统计] 本地计数器已清理");
  }

  /** 获取统计管理器状态 */
  public Map<String, Object> getManagerStatus() {
    Map<String, Object> status = new ConcurrentHashMap<>();
    status.put("localCounterSize", localCounters.size());
    status.put("localPlatformStatsSize", localPlatformStats.size());
    status.put("localProductStatsSize", localProductStats.size());
    status.put("lastFlushTime", lastFlushTime);
    status.put("flushInterval", FLUSH_INTERVAL);
    status.put("localCounters", getLocalCounterStatus());
    status.put("localPlatformStats", getLocalPlatformStatsStatus());
    status.put("localProductStats", getLocalProductStatsStatus());
    return status;
  }

  /** 获取活跃的产品key列表 */
  public List<String> getActiveProductKeys() {
    List<String> productKeys = new ArrayList<>();
    String dateKey = LocalDate.now().format(DATE_FORMATTER);

    // 从本地产品统计缓存中获取活跃的产品key
    for (String key : localProductStats.keySet()) {
      // key格式：dateKey:productKey
      String[] parts = key.split(":");
      if (parts.length == 2 && parts[0].equals(dateKey)) {
        String productKey = parts[1];
        productKeys.add(productKey);
      }
    }

    log.info("[推送统计] 从本地缓存获取到 {} 个活跃产品key: {}", productKeys.size(), productKeys);
    return productKeys;
  }

  /** 获取活跃的平台列表 */
  public List<String> getActivePlatforms() {
    List<String> platforms = new ArrayList<>();
    String dateKey = LocalDate.now().format(DATE_FORMATTER);

    // 从本地平台统计缓存中获取活跃的平台
    for (String key : localPlatformStats.keySet()) {
      // key格式：dateKey:platform
      String[] parts = key.split(":");
      if (parts.length == 2 && parts[0].equals(dateKey)) {
        String platform = parts[1];
        platforms.add(platform);
      }
    }

    log.debug("[推送统计] 从本地缓存获取到 {} 个活跃平台: {}", platforms.size(), platforms);
    return platforms;
  }
}
