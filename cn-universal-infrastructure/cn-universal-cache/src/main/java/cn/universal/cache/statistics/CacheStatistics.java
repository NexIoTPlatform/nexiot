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

package cn.universal.cache.statistics;

import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

/**
 * 缓存统计信息
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j
public class CacheStatistics {

  private final AtomicLong l1Hits = new AtomicLong(0);
  private final AtomicLong l1Misses = new AtomicLong(0);
  private final AtomicLong l1Puts = new AtomicLong(0);
  private final AtomicLong l1Evicts = new AtomicLong(0);

  private final AtomicLong l2Hits = new AtomicLong(0);
  private final AtomicLong l2Misses = new AtomicLong(0);
  private final AtomicLong l2Puts = new AtomicLong(0);
  private final AtomicLong l2Evicts = new AtomicLong(0);

  /** 记录L1缓存命中 */
  public void recordL1Hit() {
    l1Hits.incrementAndGet();
  }

  /** 记录L1缓存未命中 */
  public void recordL1Miss() {
    l1Misses.incrementAndGet();
  }

  /** 记录L1缓存写入 */
  public void recordL1Put() {
    l1Puts.incrementAndGet();
  }

  /** 记录L1缓存驱逐 */
  public void recordL1Evict() {
    l1Evicts.incrementAndGet();
  }

  /** 记录L2缓存命中 */
  public void recordL2Hit() {
    l2Hits.incrementAndGet();
  }

  /** 记录L2缓存未命中 */
  public void recordL2Miss() {
    l2Misses.incrementAndGet();
  }

  /** 记录L2缓存写入 */
  public void recordL2Put() {
    l2Puts.incrementAndGet();
  }

  /** 记录L2缓存驱逐 */
  public void recordL2Evict() {
    l2Evicts.incrementAndGet();
  }

  public long getL1Hits() {
    return l1Hits.get();
  }

  public long getL1Misses() {
    return l1Misses.get();
  }

  public long getL1Puts() {
    return l1Puts.get();
  }

  public long getL1Evicts() {
    return l1Evicts.get();
  }

  public long getL2Hits() {
    return l2Hits.get();
  }

  public long getL2Misses() {
    return l2Misses.get();
  }

  public long getL2Puts() {
    return l2Puts.get();
  }

  public long getL2Evicts() {
    return l2Evicts.get();
  }

  public double getL1HitRate() {
    long hits = l1Hits.get();
    long misses = l1Misses.get();
    long total = hits + misses;
    return total > 0 ? (double) hits / total : 0.0;
  }

  public double getL2HitRate() {
    long hits = l2Hits.get();
    long misses = l2Misses.get();
    long total = hits + misses;
    return total > 0 ? (double) hits / total : 0.0;
  }

  public long getTotalHits() {
    return l1Hits.get() + l2Hits.get();
  }

  public long getTotalMisses() {
    return l1Misses.get() + l2Misses.get();
  }

  public double getOverallHitRate() {
    long totalHits = getTotalHits();
    long totalMisses = getTotalMisses();
    long total = totalHits + totalMisses;
    return total > 0 ? (double) totalHits / total : 0.0;
  }

  /** 重置统计信息 */
  public void reset() {
    l1Hits.set(0);
    l1Misses.set(0);
    l1Puts.set(0);
    l1Evicts.set(0);
    l2Hits.set(0);
    l2Misses.set(0);
    l2Puts.set(0);
    l2Evicts.set(0);
  }

  @Override
  public String toString() {
    return String.format(
        "CacheStatistics{L1: hits=%d, misses=%d, hitRate=%.2f%%, puts=%d, evicts=%d; "
            + "L2: hits=%d, misses=%d, hitRate=%.2f%%, puts=%d, evicts=%d; "
            + "Overall: hits=%d, misses=%d, hitRate=%.2f%%}",
        l1Hits.get(),
        l1Misses.get(),
        getL1HitRate() * 100,
        l1Puts.get(),
        l1Evicts.get(),
        l2Hits.get(),
        l2Misses.get(),
        getL2HitRate() * 100,
        l2Puts.get(),
        l2Evicts.get(),
        getTotalHits(),
        getTotalMisses(),
        getOverallHitRate() * 100);
  }
}
