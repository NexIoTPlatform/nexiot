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

package cn.universal.persistence.base;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.Header;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonRequest {

  /** http请求超时时间 */
  private static final Integer HTTP_TIME_OUT = 1200;

  /** URL状态信息 */
  public static class UrlStatus {

    private int failureCount = 0;
    private long lastFailureTime = 0;
    private long nextAllowTime = 0;

    public UrlStatus() {}

    public int getFailureCount() {
      return failureCount;
    }

    public void setFailureCount(int failureCount) {
      this.failureCount = failureCount;
    }

    public long getLastFailureTime() {
      return lastFailureTime;
    }

    public void setLastFailureTime(long lastFailureTime) {
      this.lastFailureTime = lastFailureTime;
    }

    public long getNextAllowTime() {
      return nextAllowTime;
    }

    public void setNextAllowTime(long nextAllowTime) {
      this.nextAllowTime = nextAllowTime;
    }
  }

  private static String CACHE_NOTICE = "DNotice:";

  // 存储URL状态信息，30分钟后过期
  private static Cache<String, UrlStatus> urlStatusCache =
      Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).maximumSize(256).build();

  /** 分级阈值配置 */
  private static final int LEVEL_1_THRESHOLD = 3; // 轻微故障阈值

  private static final int LEVEL_2_THRESHOLD = 6; // 中等故障阈值
  private static final int LEVEL_3_THRESHOLD = 10; // 严重故障阈值

  /** 基础延迟时间（秒） */
  private static final int BASE_DELAY_SECONDS = 30;

  /** 最大延迟时间（分钟） */
  private static final int MAX_DELAY_MINUTES = 10;

  private static final int DOWN_TRANSFER_MAX_CONCURRENCY =
      Integer.getInteger("nexiot.common-request.max-concurrency", 32);

  private static final Semaphore DOWN_TRANSFER_PERMITS =
      new Semaphore(Math.max(1, DOWN_TRANSFER_MAX_CONCURRENCY));

  private static final AtomicLong DOWN_TRANSFER_WAIT_COUNT = new AtomicLong();

  private static ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  public static void downTransfer(String url, String body) {
    boolean acquired = false;
    try {
      if (!DOWN_TRANSFER_PERMITS.tryAcquire()) {
        long waitCount = DOWN_TRANSFER_WAIT_COUNT.incrementAndGet();
        if (waitCount == 1 || waitCount % 1000 == 0) {
          log.warn(
              "第三方异步下发并发已满，等待可用额度 maxConcurrency={}, waitCount={}",
              DOWN_TRANSFER_MAX_CONCURRENCY,
              waitCount);
        }
        DOWN_TRANSFER_PERMITS.acquire();
      }
      acquired = true;
      executor.execute(
          () -> {
            try {
              doRequest(url, body);
            } finally {
              DOWN_TRANSFER_PERMITS.release();
            }
          });
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("第三方异步下发等待并发额度被中断,url={}", url);
    } catch (RuntimeException e) {
      if (acquired) {
        DOWN_TRANSFER_PERMITS.release();
      }
      throw e;
    }
  }

  public static String downTransferSync(String url, String body) {
    return doRequest(url, body);
  }

  public static String doRequest(String url, String body) {
    return doRequest(url, body, false);
  }

  public static String doRequest(String url, String body, boolean needMd5) {
    // 检查URL是否被限制访问
    if (isUrlBlocked(url)) {
      UrlStatus status = urlStatusCache.getIfPresent(url);
      long remainingSeconds = (status.getNextAllowTime() - System.currentTimeMillis()) / 1000;
      log.warn("当前地址[{}]因连续失败{}次，暂时限制访问，还需等待{}秒", url, status.getFailureCount(), remainingSeconds);
      return null;
    }

    try {
      HttpRequest request = HttpUtil.createPost(url);
      request.timeout(HTTP_TIME_OUT);
      String timestamp = String.valueOf(System.currentTimeMillis());
      String signature = MD5.create().digestHex16((timestamp + body));
      request.header("X-Timestamp", timestamp);
      request.header("X-Signature", signature);
      request.header(Header.CONTENT_TYPE, "application/json");
      request.body(body);
      HttpResponse postBody = request.execute();
      String result = postBody.body();
      if (postBody == null || postBody.getStatus() != 200) {
        log.warn("推送第三方失败,url={} body={} 返回={}", url, body, result);
        handleFailure(url);
        return result;
      }
      log.info("推送第三方成功={},内容={}", url, body);
      handleSuccess(url);
      return result;
    } catch (HttpException e) {
      log.warn("推送第三方失败,url={} body={} 异常={}", url, body, ExceptionUtil.getRootCause(e));
      handleFailure(url);
    }
    return null;
  }

  /** 检查URL是否被阻止 */
  private static boolean isUrlBlocked(String url) {
    UrlStatus status = urlStatusCache.getIfPresent(url);
    if (status == null) {
      return false;
    }

    long currentTime = System.currentTimeMillis();
    return currentTime < status.getNextAllowTime();
  }

  /** 处理请求成功 */
  private static void handleSuccess(String url) {
    UrlStatus status = urlStatusCache.getIfPresent(url);
    if (status != null) {
      // 成功后重置失败计数，但保留状态以避免频繁创建对象
      status.setFailureCount(0);
      status.setNextAllowTime(0);
      log.debug("URL[{}]请求成功，重置失败计数", url);
    }
  }

  /** 处理请求失败 - 智能退避策略 */
  private static void handleFailure(String url) {
    UrlStatus status = urlStatusCache.getIfPresent(url);
    if (status == null) {
      status = new UrlStatus();
      urlStatusCache.put(url, status);
    }

    status.setFailureCount(status.getFailureCount() + 1);
    status.setLastFailureTime(System.currentTimeMillis());

    int failureCount = status.getFailureCount();
    long delayMillis = calculateDelay(failureCount);
    status.setNextAllowTime(System.currentTimeMillis() + delayMillis);

    String delayInfo = formatDelayInfo(delayMillis);
    log.warn(
        "URL[{}]第{}次失败，采用{}策略，下次可访问时间：{}",
        url,
        failureCount,
        getStrategyLevel(failureCount),
        delayInfo);

    // 严重故障时发送通知
    if (failureCount >= LEVEL_3_THRESHOLD) {
      sendFailureNotification(url, failureCount);
    }
  }

  /** 计算延迟时间 - 指数退避算法 */
  private static long calculateDelay(int failureCount) {
    if (failureCount <= 1) {
      return 0; // 首次失败不延迟
    }

    long delaySeconds;

    if (failureCount <= LEVEL_1_THRESHOLD) {
      // 轻微故障：线性增长 30s, 60s, 90s
      delaySeconds = failureCount * BASE_DELAY_SECONDS;
    } else if (failureCount <= LEVEL_2_THRESHOLD) {
      // 中等故障：指数增长，但有上限
      delaySeconds = (long) (BASE_DELAY_SECONDS * Math.pow(2, failureCount - LEVEL_1_THRESHOLD));
    } else {
      // 严重故障：固定长时间延迟
      delaySeconds = MAX_DELAY_MINUTES * 60;
    }

    // 限制最大延迟时间
    delaySeconds = Math.min(delaySeconds, MAX_DELAY_MINUTES * 60);

    return delaySeconds * 1000; // 转换为毫秒
  }

  /** 获取策略级别描述 */
  private static String getStrategyLevel(int failureCount) {
    if (failureCount <= LEVEL_1_THRESHOLD) {
      return "轻微故障-线性退避";
    } else if (failureCount <= LEVEL_2_THRESHOLD) {
      return "中等故障-指数退避";
    } else {
      return "严重故障-长时间限制";
    }
  }

  /** 格式化延迟信息 */
  private static String formatDelayInfo(long delayMillis) {
    long seconds = delayMillis / 1000;
    if (seconds < 60) {
      return seconds + "秒后";
    } else {
      long minutes = seconds / 60;
      long remainingSeconds = seconds % 60;
      if (remainingSeconds == 0) {
        return minutes + "分钟后";
      } else {
        return minutes + "分" + remainingSeconds + "秒后";
      }
    }
  }

  /** 发送失败通知 */
  private static void sendFailureNotification(String url, int failureCount) {
    String noticeKey = CACHE_NOTICE + url;
    UrlStatus noticeStatus = urlStatusCache.getIfPresent(noticeKey);

    // 每小时最多发送一次通知
    if (noticeStatus == null
        || (System.currentTimeMillis() - noticeStatus.getLastFailureTime())
            > TimeUnit.HOURS.toMillis(1)) {

      log.error("严重故障警告：URL[{}]连续失败{}次，已进入长时间限制状态", url, failureCount);

      // 记录通知时间
      UrlStatus newNoticeStatus = new UrlStatus();
      newNoticeStatus.setLastFailureTime(System.currentTimeMillis());
      urlStatusCache.put(noticeKey, newNoticeStatus);

      // TODO: 集成钉钉/企微等通知
      // DingTalkUtil.send("严重故障：地址 " + url + " 连续失败 " + failureCount + " 次");
    }
  }

  private static void md5(String body, boolean needMd5, HttpRequest request) {
    if (needMd5) {
      String now = DateUtil.now();
      request.header("times", now);
      request.header("cn-universal-sign", MD5.create().digestHex(body + now));
    }
  }
}
