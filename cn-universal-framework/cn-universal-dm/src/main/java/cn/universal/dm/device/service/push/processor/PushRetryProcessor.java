package cn.universal.dm.device.service.push.processor;

import cn.universal.dm.device.entity.IoTPushResult;
import cn.universal.dm.device.service.push.UPProcessor;
import cn.universal.persistence.base.BaseUPRequest;
import jakarta.annotation.PreDestroy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 推送重试处理器 基于UPProcessor架构实现推送重试功能
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/1/9
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "nexiot.push.retry", name = "enabled", havingValue = "true")
public class PushRetryProcessor implements UPProcessor<BaseUPRequest> {

  @Autowired private StringRedisTemplate redisTemplate;

  @Autowired private PushStatisticsProcessor pushStatisticsProcessor;

  // 配置参数
  @Value("${push.retry.redis.timeout:5}")
  private int redisTimeoutSeconds;

  // The legacy queue has no executable request/config snapshot, so enabling it cannot retry safely.
  private final boolean retryEnabled = false;

  // Redis Key 前缀
  private static final String RETRY_QUEUE_KEY_PREFIX = "push:retry:queue:";
  private static final String RETRY_COUNT_KEY_PREFIX = "push:retry:count:";
  private static final String FAILED_PUSH_KEY_PREFIX = "push:failed:";
  private static final DateTimeFormatter DATETIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  // 默认配置
  private static final int DEFAULT_MAX_RETRY_COUNT = 3;
  private static final int DEFAULT_RETRY_INTERVAL_MINUTES = 5;

  // 专用重试线程池 - 使用自定义线程池避免ForkJoinPool耗尽
  // 直接使用默认值初始化，简单可靠
  private final java.util.concurrent.ExecutorService retryExecutor =
      java.util.concurrent.Executors.newFixedThreadPool(
          10, // 默认线程池大小
          r -> {
            Thread t = new Thread(r, "push-retry-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
          });

  @Override
  public String getName() {
    return "PushRetryProcessor";
  }

  @Override
  public String getDescription() {
    return "推送重试处理器";
  }

  @Override
  public int getOrder() {
    return 200; // 较低优先级，在统计后执行
  }

  @Override
  public boolean isBeforePushEnabled() {
    return false;
  }

  /** 获取线程池状态信息 */
  public String getThreadPoolStatus() {
    if (retryExecutor instanceof java.util.concurrent.ThreadPoolExecutor) {
      java.util.concurrent.ThreadPoolExecutor executor =
          (java.util.concurrent.ThreadPoolExecutor) retryExecutor;
      return String.format(
          "PushRetry线程池状态: 核心线程数=%d, 最大线程数=%d, 当前线程数=%d, 活跃线程数=%d, 队列大小=%d, 已完成任务数=%d",
          executor.getCorePoolSize(),
          executor.getMaximumPoolSize(),
          executor.getPoolSize(),
          executor.getActiveCount(),
          executor.getQueue().size(),
          executor.getCompletedTaskCount());
    }
    return "PushRetry线程池状态: 无法获取详细信息";
  }

  /** 应用关闭时清理资源 */
  @PreDestroy
  public void destroy() {
    log.info("[推送重试] 开始关闭PushRetryProcessor...");
    if (retryExecutor != null && !retryExecutor.isShutdown()) {
      retryExecutor.shutdown();
      try {
        if (!retryExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
          log.warn("[推送重试] 线程池未能在30秒内正常关闭，强制关闭");
          retryExecutor.shutdownNow();
        }
      } catch (InterruptedException e) {
        log.warn("[推送重试] 等待线程池关闭时被中断");
        retryExecutor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
    log.info("[推送重试] PushRetryProcessor已关闭");
  }

  @Override
  public void afterPush(List<BaseUPRequest> upRequests, List<IoTPushResult> pushResults) {
    if (!retryEnabled || pushResults == null || pushResults.isEmpty()) {
      return;
    }

    // 处理失败推送
    for (IoTPushResult result : pushResults) {
      if (!result.isOk()) {
        addToRetryQueue(result);
        recordFailedPush(result);
        log.warn(
            "[推送重试] 发现失败推送，已加入重试队列: deviceId={}, channel={}, error={}",
            result.getDeviceId(),
            result.getChannel(),
            result.getErrorMessage());
      }
    }
  }

  /** 添加失败推送到重试队列 */
  public void addToRetryQueue(IoTPushResult result) {
    if (result == null || result.isOk()) {
      return;
    }

    try {
      String retryKey = generateRetryKey(result);
      String retryData = buildRetryData(result);

      // 检查重试次数
      String countKey = RETRY_COUNT_KEY_PREFIX + result.getDeviceId() + ":" + result.getChannel();
      Integer currentRetryCount = getRetryCount(countKey);

      if (currentRetryCount >= DEFAULT_MAX_RETRY_COUNT) {
        log.warn(
            "[推送重试] 设备 {} 渠道 {} 已达到最大重试次数 {}",
            result.getDeviceId(),
            result.getChannel(),
            DEFAULT_MAX_RETRY_COUNT);
        return;
      }

      // 使用异步处理，避免阻塞ForkJoinPool
      CompletableFuture<Void> addToQueueFuture =
          CompletableFuture.runAsync(
              () -> {
                try {
                  // 添加到重试队列
                  redisTemplate.opsForList().rightPush(retryKey, retryData);
                  redisTemplate.expire(retryKey, java.time.Duration.ofDays(1));
                  log.debug(
                      "[推送重试] Redis添加队列成功: deviceId={}, channel={}",
                      result.getDeviceId(),
                      result.getChannel());
                } catch (Exception e) {
                  log.error(
                      "[推送重试] Redis添加队列失败: deviceId={}, channel={}",
                      result.getDeviceId(),
                      result.getChannel(),
                      e);
                }
              },
              retryExecutor);

      CompletableFuture<Void> incrementCountFuture =
          CompletableFuture.runAsync(
              () -> {
                try {
                  // 增加重试次数
                  redisTemplate.opsForValue().increment(countKey);
                  redisTemplate.expire(countKey, java.time.Duration.ofDays(1));
                  log.debug(
                      "[推送重试] Redis增加计数成功: deviceId={}, channel={}",
                      result.getDeviceId(),
                      result.getChannel());
                } catch (Exception e) {
                  log.error(
                      "[推送重试] Redis增加计数失败: deviceId={}, channel={}",
                      result.getDeviceId(),
                      result.getChannel(),
                      e);
                }
              },
              retryExecutor);

      // 异步处理结果，不阻塞当前线程
      CompletableFuture.allOf(addToQueueFuture, incrementCountFuture)
          .thenRun(
              () -> {
                log.info(
                    "[推送重试] 添加失败推送到重试队列成功: deviceId={}, channel={}, retryCount={}",
                    result.getDeviceId(),
                    result.getChannel(),
                    currentRetryCount + 1);
              })
          .exceptionally(
              throwable -> {
                log.error(
                    "[推送重试] 添加失败推送到重试队列异常: deviceId={}, channel={}",
                    result.getDeviceId(),
                    result.getChannel(),
                    throwable);
                return null;
              });
    } catch (Exception e) {
      log.error(
          "[推送重试] 添加失败推送到重试队列失败: deviceId={}, channel={}",
          result.getDeviceId(),
          result.getChannel(),
          e);
    }
  }

  /** 定时重试任务 每5分钟执行一次 */
  public void retryFailedPushes() {
    if (!retryEnabled) {
      return;
    }
    log.debug("[推送重试] 开始执行重试任务");

    try {
      // 从重试队列获取所有待重试的推送
      List<IoTPushResult> retryPushes = getRetryQueuePushes();

      for (IoTPushResult retryPush : retryPushes) {
        CompletableFuture.runAsync(
            () -> {
              try {
                retryPush(retryPush);
              } catch (Exception e) {
                log.error(
                    "[推送重试] 重试推送失败: deviceId={}, channel={}",
                    retryPush.getDeviceId(),
                    retryPush.getChannel(),
                    e);
              }
            },
            retryExecutor);
      }
    } catch (Exception e) {
      log.error("[推送重试] 执行重试任务失败", e);
    }
  }

  /** 重试单个推送 */
  private void retryPush(IoTPushResult failedPush) {
    String countKey =
        RETRY_COUNT_KEY_PREFIX + failedPush.getDeviceId() + ":" + failedPush.getChannel();
    Integer currentRetryCount = getRetryCount(countKey);

    if (currentRetryCount >= DEFAULT_MAX_RETRY_COUNT) {
      log.warn(
          "[推送重试] 设备 {} 渠道 {} 已达到最大重试次数，跳过重试，并清理重试队列和计数",
          failedPush.getDeviceId(),
          failedPush.getChannel());
      // 直接删除重试计数和队列
      String retryKey = generateRetryKey(failedPush);
      redisTemplate.delete(java.util.Arrays.asList(countKey, retryKey));
      return;
    }

    log.info(
        "[推送重试] 开始重试推送: deviceId={}, channel={}, retryCount={}",
        failedPush.getDeviceId(),
        failedPush.getChannel(),
        currentRetryCount + 1);

    try {
      // 构建重试的推送结果
      IoTPushResult retryResult =
          IoTPushResult.retry(failedPush, "Retry attempt " + (currentRetryCount + 1));
      retryResult.setMaxRetryCount(DEFAULT_MAX_RETRY_COUNT);

      // 记录重试统计
      pushStatisticsProcessor.recordPushResult(retryResult);

      // 执行实际推送（这里需要根据你的推送策略管理器调用）
      boolean pushSuccess = executeActualPush(retryResult);

      if (pushSuccess) {
        // 重试成功，清理重试队列和计数
        log.info(
            "[推送重试] 重试推送成功: deviceId={}, channel={}",
            failedPush.getDeviceId(),
            failedPush.getChannel());

        // 清理重试数据
        String retryKey = generateRetryKey(failedPush);
        redisTemplate.delete(java.util.Arrays.asList(countKey, retryKey));

        // 从重试队列中移除
        removeFromRetryQueue(failedPush);

      } else {
        // 重试失败，继续保留在队列中
        log.warn(
            "[推送重试] 重试推送失败: deviceId={}, channel={}",
            failedPush.getDeviceId(),
            failedPush.getChannel());
      }

    } catch (Exception e) {
      log.error(
          "[推送重试] 重试推送异常: deviceId={}, channel={}",
          failedPush.getDeviceId(),
          failedPush.getChannel(),
          e);

      // 如果重试失败，继续添加到重试队列
      if (currentRetryCount < DEFAULT_MAX_RETRY_COUNT) {
        addToRetryQueue(failedPush);
      }
    }
  }

  /** 执行实际推送（需要根据你的推送策略管理器实现） */
  private boolean executeActualPush(IoTPushResult retryResult) {
    try {
      // TODO: 这里需要调用你的推送策略管理器
      // 例如：pushStrategyManager.executePush(originalRequest, config);
      // 由于原始请求信息可能已经丢失，这里提供一个框架
      // 实际实现时需要根据业务需求调整

      log.debug(
          "[推送重试] 执行实际推送: deviceId={}, channel={}",
          retryResult.getDeviceId(),
          retryResult.getChannel());

      // 临时返回false，表示需要你根据实际情况实现
      return false;
    } catch (Exception e) {
      log.error(
          "[推送重试] 执行实际推送失败: deviceId={}, channel={}",
          retryResult.getDeviceId(),
          retryResult.getChannel(),
          e);
      return false;
    }
  }

  /** 从重试队列中移除 */
  private void removeFromRetryQueue(IoTPushResult result) {
    try {
      String retryKey = generateRetryKey(result);
      String retryData = buildRetryData(result);

      // 从队列中移除指定的数据
      redisTemplate.opsForList().remove(retryKey, 1, retryData);
    } catch (Exception e) {
      log.error(
          "[推送重试] 从重试队列移除失败: deviceId={}, channel={}",
          result.getDeviceId(),
          result.getChannel(),
          e);
    }
  }

  /** 获取重试队列中的所有推送 */
  private List<IoTPushResult> getRetryQueuePushes() {
    List<IoTPushResult> retryPushes = new java.util.ArrayList<>();

    try {
      // 获取所有重试队列的key
      String retryQueuePattern = RETRY_QUEUE_KEY_PREFIX + "*";

      // 使用SCAN命令获取所有重试队列
      org.springframework.data.redis.core.ScanOptions options =
          org.springframework.data.redis.core.ScanOptions.scanOptions()
              .match(retryQueuePattern)
              .count(100)
              .build();

      org.springframework.data.redis.core.Cursor<String> cursor = redisTemplate.scan(options);

      while (cursor.hasNext()) {
        String retryKey = cursor.next();

        // 从队列中获取数据
        List<String> retryDataList = redisTemplate.opsForList().range(retryKey, 0, -1);

        if (retryDataList != null) {
          for (String retryData : retryDataList) {
            try {
              IoTPushResult result = parseRetryData(retryData);
              if (result != null) {
                retryPushes.add(result);
              }
            } catch (Exception e) {
              log.error("[推送重试] 解析重试数据失败: {}", retryData, e);
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("[推送重试] 获取重试队列失败", e);
    }

    return retryPushes;
  }

  /** 解析重试数据 */
  private IoTPushResult parseRetryData(String retryData) {
    try {
      String[] parts = retryData.split("\\|");
      if (parts.length >= 9) {
        return IoTPushResult.builder()
            .deviceId(parts[0])
            .productKey(parts[1])
            .channel(parts[2])
            .platform(parts[3].isEmpty() ? null : parts[3])
            .messageContent(parts[4])
            .errorMessage(parts[5])
            .errorCode(parts[6])
            .pushTime(LocalDateTime.parse(parts[7], DATETIME_FORMATTER))
            .retryCount(Integer.valueOf(parts[8]))
            .ok(false)
            .status(IoTPushResult.PushStatus.FAILED)
            .build();
      }
    } catch (Exception e) {
      log.error("[推送重试] 解析重试数据失败: {}", retryData, e);
    }
    return null;
  }

  /** 手动重试指定设备的推送 */
  public void manualRetry(String deviceId, String channel, String productKey) {
    String countKey = RETRY_COUNT_KEY_PREFIX + deviceId + ":" + channel;
    Integer currentRetryCount = getRetryCount(countKey);

    if (currentRetryCount >= DEFAULT_MAX_RETRY_COUNT) {
      log.warn("[推送重试] 设备 {} 渠道 {} 已达到最大重试次数", deviceId, channel);
      return;
    }

    // 创建重试推送结果
    IoTPushResult retryResult =
        IoTPushResult.builder()
            .deviceId(deviceId)
            .productKey(productKey)
            .channel(channel)
            .pushTime(LocalDateTime.now())
            .ok(false)
            .status(IoTPushResult.PushStatus.RETRYING)
            .retryCount(currentRetryCount + 1)
            .maxRetryCount(DEFAULT_MAX_RETRY_COUNT)
            .errorMessage("Manual retry")
            .build();

    // 记录重试统计
    pushStatisticsProcessor.recordPushResult(retryResult);

    log.info(
        "[推送重试] 手动重试推送: deviceId={}, channel={}, retryCount={}",
        deviceId,
        channel,
        currentRetryCount + 1);
  }

  /** 获取重试次数 - 同步方法，直接调用Redis */
  private Integer getRetryCount(String countKey) {
    try {
      // 直接调用Redis，避免异步复杂性
      String count = redisTemplate.opsForValue().get(countKey);
      return count != null ? Integer.valueOf(count) : 0;
    } catch (Exception e) {
      log.error("[推送重试] 获取重试次数失败，key={}", countKey, e);
      return 0;
    }
  }

  /** 生成重试队列Key */
  private String generateRetryKey(IoTPushResult result) {
    return RETRY_QUEUE_KEY_PREFIX + result.getDeviceId() + ":" + result.getChannel();
  }

  /** 构建重试数据 */
  private String buildRetryData(IoTPushResult result) {
    return String.format(
        "%s|%s|%s|%s|%s|%s|%s|%s|%s",
        result.getDeviceId(),
        result.getProductKey(),
        result.getChannel(),
        result.getPlatform() != null ? result.getPlatform() : "",
        result.getMessageContent(),
        result.getErrorMessage(),
        result.getErrorCode(),
        result.getPushTime().format(DATETIME_FORMATTER),
        result.getRetryCount() != null ? result.getRetryCount() : 0);
  }

  /** 获取失败推送列表 */
  public List<IoTPushResult> getFailedPushes(LocalDate date) {
    try {
      String dateKey = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      String failedKey = FAILED_PUSH_KEY_PREFIX + dateKey;

      // 使用CompletableFuture添加超时处理
      CompletableFuture<List<String>> future =
          CompletableFuture.supplyAsync(
              () -> {
                return redisTemplate.opsForList().range(failedKey, 0, -1);
              },
              retryExecutor);

      List<String> failedDataList = future.get(redisTimeoutSeconds, TimeUnit.SECONDS);
      List<IoTPushResult> failedPushes = new java.util.ArrayList<>();

      if (failedDataList != null) {
        for (String failedData : failedDataList) {
          try {
            String[] parts = failedData.split("\\|");
            if (parts.length >= 8) {
              IoTPushResult result =
                  IoTPushResult.builder()
                      .deviceId(parts[0])
                      .productKey(parts[1])
                      .channel(parts[2])
                      .platform(parts[3].isEmpty() ? null : parts[3])
                      .messageContent(parts[4])
                      .errorMessage(parts[5])
                      .errorCode(parts[6])
                      .pushTime(LocalDateTime.parse(parts[7], DATETIME_FORMATTER))
                      .ok(false)
                      .status(IoTPushResult.PushStatus.FAILED)
                      .build();
              failedPushes.add(result);
            }
          } catch (Exception e) {
            log.error("[推送重试] 解析失败推送数据失败: {}", failedData, e);
          }
        }
      }

      return failedPushes;
    } catch (TimeoutException e) {
      log.warn("[推送重试] 获取失败推送列表超时，date={}", date);
      return new java.util.ArrayList<>();
    } catch (Exception e) {
      log.error("[推送重试] 获取失败推送列表失败，date={}", date, e);
      return new java.util.ArrayList<>();
    }
  }

  /** 记录失败推送 */
  public void recordFailedPush(IoTPushResult result) {
    if (result == null || result.isOk()) {
      return;
    }

    try {
      String dateKey = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      String failedKey = FAILED_PUSH_KEY_PREFIX + dateKey;
      String failedData =
          String.format(
              "%s|%s|%s|%s|%s|%s|%s|%s",
              result.getDeviceId(),
              result.getProductKey(),
              result.getChannel(),
              result.getPlatform() != null ? result.getPlatform() : "",
              result.getMessageContent(),
              result.getErrorMessage(),
              result.getErrorCode(),
              result.getPushTime().format(DATETIME_FORMATTER));

      // 使用异步处理，避免阻塞ForkJoinPool
      CompletableFuture<Void> future =
          CompletableFuture.runAsync(
              () -> {
                try {
                  redisTemplate.opsForList().rightPush(failedKey, failedData);
                  redisTemplate.expire(failedKey, java.time.Duration.ofDays(7)); // 失败记录保留7天
                  log.debug(
                      "[推送重试] 记录失败推送成功: deviceId={}, channel={}",
                      result.getDeviceId(),
                      result.getChannel());
                } catch (Exception e) {
                  log.error(
                      "[推送重试] 记录失败推送Redis操作失败: deviceId={}, channel={}",
                      result.getDeviceId(),
                      result.getChannel(),
                      e);
                }
              },
              retryExecutor);

      // 异步处理结果，不阻塞当前线程
      future
          .thenRun(
              () -> {
                log.debug(
                    "[推送重试] 记录失败推送完成: deviceId={}, channel={}",
                    result.getDeviceId(),
                    result.getChannel());
              })
          .exceptionally(
              throwable -> {
                log.error(
                    "[推送重试] 记录失败推送异常: deviceId={}, channel={}",
                    result.getDeviceId(),
                    result.getChannel(),
                    throwable);
                return null;
              });
    } catch (Exception e) {
      log.error(
          "[推送重试] 记录失败推送失败: deviceId={}, channel={}", result.getDeviceId(), result.getChannel(), e);
    }
  }

  /** 清理过期的重试记录 每天凌晨2点执行 */
  @Scheduled(cron = "0 0 2 * * ?")
  public void cleanupExpiredRetryRecords() {
    log.info("[推送重试] 开始清理过期的重试记录");

    try {
      // 清理7天前的重试记录
      LocalDate cleanupDate = LocalDate.now().minusDays(7);
      String dateKey = cleanupDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

      // 清理失败推送记录
      String failedKey = FAILED_PUSH_KEY_PREFIX + dateKey;
      redisTemplate.delete(failedKey);

      // 清理重试队列
      String retryQueuePattern = RETRY_QUEUE_KEY_PREFIX + "*";
      deleteKeysByPattern(retryQueuePattern);

      // 清理重试计数
      String retryCountPattern = RETRY_COUNT_KEY_PREFIX + "*";
      deleteKeysByPattern(retryCountPattern);

      log.info("[推送重试] 清理过期的重试记录完成");
    } catch (Exception e) {
      log.error("[推送重试] 清理过期的重试记录失败", e);
    }
  }

  /** 获取设备的重试统计 */
  public Integer getRetryCount(String deviceId, String channel) {
    String countKey = RETRY_COUNT_KEY_PREFIX + deviceId + ":" + channel;
    return getRetryCount(countKey);
  }

  /** 重置设备的重试计数 */
  public void resetRetryCount(String deviceId, String channel) {
    String countKey = RETRY_COUNT_KEY_PREFIX + deviceId + ":" + channel;
    redisTemplate.delete(countKey);
    log.info("[推送重试] 重置设备重试计数: deviceId={}, channel={}", deviceId, channel);
  }

  /** 检查是否可以进行重试 */
  public boolean canRetry(String deviceId, String channel) {
    Integer retryCount = getRetryCount(deviceId, channel);
    return retryCount < DEFAULT_MAX_RETRY_COUNT;
  }

  /**
   * 使用SCAN命令删除匹配模式的键
   *
   * @param pattern 键模式
   */
  private void deleteKeysByPattern(String pattern) {
    try {
      long cursor = 0;
      do {
        org.springframework.data.redis.core.ScanOptions options =
            org.springframework.data.redis.core.ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build();

        org.springframework.data.redis.core.Cursor<String> cursorResult =
            redisTemplate.scan(options);

        while (cursorResult.hasNext()) {
          String key = cursorResult.next();
          redisTemplate.delete(key);
        }

        cursor = cursorResult.getCursorId();
      } while (cursor != 0);
    } catch (Exception e) {
      log.error("[推送重试] 删除匹配模式的键失败: pattern={}", pattern, e);
    }
  }
}
