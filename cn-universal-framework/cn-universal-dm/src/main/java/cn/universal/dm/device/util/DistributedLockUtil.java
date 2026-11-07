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
package cn.universal.dm.device.util;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 分布式锁工具类 提供便捷的锁操作方法
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j
@Component
public class DistributedLockUtil {

  @Autowired private RedissonClient redissonClient;

  /**
   * 尝试获取锁并执行操作
   *
   * @param lockKey 锁的键
   * @param waitTime 等待获取锁的时间
   * @param leaseTime 锁的持有时间
   * @param timeUnit 时间单位
   * @param supplier 要执行的操作
   * @param <T> 返回值类型
   * @return 操作结果，如果获取锁失败返回null
   */
  public <T> T tryLockAndExecute(
      String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> supplier) {
    RLock lock = redissonClient.getLock(lockKey);

    try {
      boolean lockAcquired = lock.tryLock(waitTime, leaseTime, timeUnit);
      if (lockAcquired) {
        try {
          log.debug("成功获取分布式锁: {}", lockKey);
          return supplier.get();
        } finally {
          if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("释放分布式锁: {}", lockKey);
          }
        }
      } else {
        log.warn("获取分布式锁失败: {}", lockKey);
        return null;
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("获取分布式锁被中断: {}", lockKey);
      return null;
    } catch (Exception e) {
      log.error("执行分布式锁操作异常: {}", lockKey, e);
      return null;
    }
  }

  /**
   * 尝试获取锁并执行操作（无返回值）
   *
   * @param lockKey 锁的键
   * @param waitTime 等待获取锁的时间
   * @param leaseTime 锁的持有时间
   * @param timeUnit 时间单位
   * @param runnable 要执行的操作
   * @return 是否成功执行
   */
  public boolean tryLockAndExecute(
      String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Runnable runnable) {
    RLock lock = redissonClient.getLock(lockKey);

    try {
      boolean lockAcquired = lock.tryLock(waitTime, leaseTime, timeUnit);
      if (lockAcquired) {
        try {
          log.debug("成功获取分布式锁: {}", lockKey);
          runnable.run();
          return true;
        } finally {
          if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("释放分布式锁: {}", lockKey);
          }
        }
      } else {
        log.warn("获取分布式锁失败: {}", lockKey);
        return false;
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("获取分布式锁被中断: {}", lockKey);
      return false;
    } catch (Exception e) {
      log.error("执行分布式锁操作异常: {}", lockKey, e);
      return false;
    }
  }

  /**
   * 检查锁是否被持有
   *
   * @param lockKey 锁的键
   * @return 是否被持有
   */
  public boolean isLocked(String lockKey) {
    RLock lock = redissonClient.getLock(lockKey);
    return lock.isLocked();
  }

  /**
   * 检查锁是否被当前线程持有
   *
   * @param lockKey 锁的键
   * @return 是否被当前线程持有
   */
  public boolean isHeldByCurrentThread(String lockKey) {
    RLock lock = redissonClient.getLock(lockKey);
    return lock.isHeldByCurrentThread();
  }

  /**
   * 强制释放锁（谨慎使用）
   *
   * @param lockKey 锁的键
   */
  public void forceUnlock(String lockKey) {
    RLock lock = redissonClient.getLock(lockKey);
    if (lock.isLocked()) {
      lock.forceUnlock();
      log.warn("强制释放分布式锁: {}", lockKey);
    }
  }
}
