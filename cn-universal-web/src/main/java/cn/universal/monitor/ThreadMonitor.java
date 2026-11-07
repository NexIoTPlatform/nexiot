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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 线程监控器 用于检测死锁、线程阻塞等问题
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j
@Component
public class ThreadMonitor {

  private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
  private final AtomicLong lastThreadCount = new AtomicLong(0);
  private final AtomicLong lastDeadlockCount = new AtomicLong(0);

  @Value("${nexiot.monitor.blocked-thread-warn-threshold-ms:1000}")
  private long blockedThreadWarnThresholdMs;

  public ThreadMonitor() {
    if (threadMXBean.isThreadContentionMonitoringSupported()
        && !threadMXBean.isThreadContentionMonitoringEnabled()) {
      try {
        threadMXBean.setThreadContentionMonitoringEnabled(true);
      } catch (UnsupportedOperationException | SecurityException e) {
        log.debug("线程阻塞耗时统计未启用: {}", e.getMessage());
      }
    }
  }

  /** 每30秒检查一次线程状态 */
  @Scheduled(fixedRate = 30000)
  public void monitorThreads() {
    try {
      // 检查死锁
      long[] deadlockedThreadIds = threadMXBean.findDeadlockedThreads();
      if (deadlockedThreadIds != null && deadlockedThreadIds.length > 0) {
        log.error("检测到死锁！死锁线程数量: {}", deadlockedThreadIds.length);
        for (long threadId : deadlockedThreadIds) {
          ThreadInfo threadInfo = threadMXBean.getThreadInfo(threadId);
          if (threadInfo != null) {
            log.error("死锁线程: {} - {}", threadInfo.getThreadName(), threadInfo.getThreadState());
          }
        }
      }

      // 检查线程数量变化
      int threadCount = threadMXBean.getThreadCount();
      long lastCount = lastThreadCount.get();
      if (lastCount > 0 && threadCount > lastCount + 50) {
        log.warn(
            "线程数量异常增长！当前: {}, 上次: {}, 增长: {}", threadCount, lastCount, threadCount - lastCount);
      }
      lastThreadCount.set(threadCount);

      // 检查阻塞线程。只取线程概要，避免定时监控频繁生成全量线程栈。
      ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 0);
      for (ThreadInfo threadInfo : threadInfos) {
        if (threadInfo == null || threadInfo.getThreadState() != Thread.State.BLOCKED) {
          continue;
        }
        long blockedTime = threadInfo.getBlockedTime();
        if (blockedTime >= blockedThreadWarnThresholdMs) {
          log.warn(
              "检测到阻塞线程: {} - 阻塞时间: {}ms", threadInfo.getThreadName(), threadInfo.getBlockedTime());
        } else if (blockedTime < 0) {
          log.debug("检测到阻塞线程但JVM未提供阻塞耗时: {}", threadInfo.getThreadName());
        }
      }

      // 定期打印线程统计信息
      if (System.currentTimeMillis() % 300000 < 30000) { // 每5分钟打印一次
        log.debug(
            "线程统计 - 总数: {}, 守护线程: {}, 峰值: {}",
            threadMXBean.getThreadCount(),
            threadMXBean.getDaemonThreadCount(),
            threadMXBean.getPeakThreadCount());
      }

    } catch (Exception e) {
      log.error("线程监控异常", e);
    }
  }

  /** 获取线程转储 */
  public String getThreadDump() {
    try {
      ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
      StringBuilder sb = new StringBuilder();
      sb.append("=== 线程转储 ===\n");

      for (ThreadInfo threadInfo : threadInfos) {
        sb.append(
            String.format(
                "线程: %s (ID: %d, 状态: %s)\n",
                threadInfo.getThreadName(), threadInfo.getThreadId(), threadInfo.getThreadState()));

        if (threadInfo.getLockName() != null) {
          sb.append(String.format("  等待锁: %s\n", threadInfo.getLockName()));
        }

        if (threadInfo.getLockOwnerName() != null) {
          sb.append(String.format("  锁持有者: %s\n", threadInfo.getLockOwnerName()));
        }

        StackTraceElement[] stackTrace = threadInfo.getStackTrace();
        if (stackTrace.length > 0) {
          sb.append("  堆栈:\n");
          for (int i = 0; i < Math.min(stackTrace.length, 10); i++) {
            sb.append(String.format("    %s\n", stackTrace[i]));
          }
        }
        sb.append("\n");
      }

      return sb.toString();
    } catch (Exception e) {
      log.error("获取线程转储失败", e);
      return "获取线程转储失败: " + e.getMessage();
    }
  }

  /** 检查是否有死锁 */
  public boolean hasDeadlock() {
    try {
      long[] deadlockedThreadIds = threadMXBean.findDeadlockedThreads();
      return deadlockedThreadIds != null && deadlockedThreadIds.length > 0;
    } catch (Exception e) {
      log.error("检查死锁失败", e);
      return false;
    }
  }

  /** 获取线程统计信息 */
  public ThreadStats getThreadStats() {
    return ThreadStats.builder()
        .totalThreads(threadMXBean.getThreadCount())
        .daemonThreads(threadMXBean.getDaemonThreadCount())
        .peakThreads(threadMXBean.getPeakThreadCount())
        .hasDeadlock(hasDeadlock())
        .build();
  }

  /** 线程统计信息 */
  public static class ThreadStats {

    private final int totalThreads;
    private final int daemonThreads;
    private final int peakThreads;
    private final boolean hasDeadlock;

    public ThreadStats(int totalThreads, int daemonThreads, int peakThreads, boolean hasDeadlock) {
      this.totalThreads = totalThreads;
      this.daemonThreads = daemonThreads;
      this.peakThreads = peakThreads;
      this.hasDeadlock = hasDeadlock;
    }

    public static Builder builder() {
      return new Builder();
    }

    public int getTotalThreads() {
      return totalThreads;
    }

    public int getDaemonThreads() {
      return daemonThreads;
    }

    public int getPeakThreads() {
      return peakThreads;
    }

    public boolean isHasDeadlock() {
      return hasDeadlock;
    }

    public static class Builder {

      private int totalThreads;
      private int daemonThreads;
      private int peakThreads;
      private boolean hasDeadlock;

      public Builder totalThreads(int totalThreads) {
        this.totalThreads = totalThreads;
        return this;
      }

      public Builder daemonThreads(int daemonThreads) {
        this.daemonThreads = daemonThreads;
        return this;
      }

      public Builder peakThreads(int peakThreads) {
        this.peakThreads = peakThreads;
        return this;
      }

      public Builder hasDeadlock(boolean hasDeadlock) {
        this.hasDeadlock = hasDeadlock;
        return this;
      }

      public ThreadStats build() {
        return new ThreadStats(totalThreads, daemonThreads, peakThreads, hasDeadlock);
      }
    }
  }
}
