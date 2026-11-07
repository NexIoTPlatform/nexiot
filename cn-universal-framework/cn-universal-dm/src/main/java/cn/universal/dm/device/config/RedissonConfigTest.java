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
package cn.universal.dm.device.config;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

/**
 * Redisson 配置测试类 用于验证不同模式的连接是否正常
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j
// @Component
public class RedissonConfigTest implements CommandLineRunner {

  @Autowired private RedissonClient redissonClient;

  @Override
  public void run(String... args) throws Exception {
    log.info("开始测试 Redisson 连接...");

    try {
      // 测试基本连接
      testBasicConnection();

      // 测试分布式锁
      testDistributedLock();

      // 测试Redis操作
      testRedisOperations();

      log.info("Redisson 连接测试完成，所有测试通过！");

    } catch (Exception e) {
      log.error("Redisson 连接测试失败: {}", e.getMessage(), e);
      // 不抛出异常，避免影响应用启动
    }
  }

  /** 测试基本连接 */
  private void testBasicConnection() {
    log.info("测试基本连接...");

    // 测试基本操作
    String testKey = "test:redisson:connection";
    redissonClient.getBucket(testKey).set("test");
    Object value = redissonClient.getBucket(testKey).get();
    log.info("Redis 连接测试通过: {} = {}", testKey, value);

    // 清理测试数据
    redissonClient.getBucket(testKey).delete();
  }

  /** 测试分布式锁 */
  private void testDistributedLock() {
    log.info("测试分布式锁...");

    String lockKey = "test:redisson:lock";
    RLock lock = redissonClient.getLock(lockKey);

    try {
      // 尝试获取锁
      boolean locked = lock.tryLock(5, 10, TimeUnit.SECONDS);
      if (locked) {
        log.info("成功获取分布式锁: {}", lockKey);

        // 模拟业务处理
        Thread.sleep(1000);

        // 释放锁
        lock.unlock();
        log.info("成功释放分布式锁: {}", lockKey);
      } else {
        log.warn("获取分布式锁失败: {}", lockKey);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("分布式锁测试被中断: {}", lockKey);
    } catch (Exception e) {
      log.error("分布式锁测试失败: {}", e.getMessage());
    }
  }

  /** 测试Redis操作 */
  private void testRedisOperations() {
    log.info("测试Redis操作...");

    try {
      // 测试字符串操作
      String testKey = "test:redisson:string";
      String testValue = "Hello Redisson!";

      redissonClient.getBucket(testKey).set(testValue);
      String retrievedValue = redissonClient.getBucket(testKey).get().toString();

      if (testValue.equals(retrievedValue)) {
        log.info("字符串操作测试通过: {} = {}", testKey, retrievedValue);
      } else {
        log.warn("字符串操作测试失败: 期望={}, 实际={}", testValue, retrievedValue);
      }

      // 清理测试数据
      redissonClient.getBucket(testKey).delete();

      // 测试Hash操作
      String hashKey = "test:redisson:hash";
      redissonClient.getMap(hashKey).put("field1", "value1");
      redissonClient.getMap(hashKey).put("field2", "value2");

      int hashSize = redissonClient.getMap(hashKey).size();
      log.info("Hash操作测试通过: {} 包含 {} 个字段", hashKey, hashSize);

      // 清理测试数据
      redissonClient.getMap(hashKey).delete();

      // 测试Set操作
      String setKey = "test:redisson:set";
      redissonClient.getSet(setKey).add("item1");
      redissonClient.getSet(setKey).add("item2");
      redissonClient.getSet(setKey).add("item3");

      int setSize = redissonClient.getSet(setKey).size();
      log.info("Set操作测试通过: {} 包含 {} 个元素", setKey, setSize);

      // 清理测试数据
      redissonClient.getSet(setKey).delete();

    } catch (Exception e) {
      log.error("Redis操作测试失败: {}", e.getMessage());
    }
  }
}
