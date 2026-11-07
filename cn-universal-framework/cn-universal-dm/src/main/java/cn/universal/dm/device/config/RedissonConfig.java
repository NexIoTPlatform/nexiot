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

import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Redisson 配置类 用于配置分布式锁和 Redis 连接 支持单机模式、集群模式、哨兵模式、主从模式
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Slf4j
@Configuration
public class RedissonConfig {

  // ================================
  // 基础配置
  // ================================
  @Value("${spring.data.redis.host:localhost}")
  private String redisHost;

  @Value("${spring.data.redis.port:6379}")
  private int redisPort;

  @Value("${spring.data.redis.password:}")
  private String redisPassword;

  @Value("${spring.data.redis.database:0}")
  private int redisDatabase;

  @Value("${spring.data.redis.timeout:3000}")
  private int redisTimeout;

  // ================================
  // 连接池配置
  // ================================
  @Value("${spring.data.redis.lettuce.pool.max-active:8}")
  private int maxActive;

  @Value("${spring.data.redis.lettuce.pool.max-idle:8}")
  private int maxIdle;

  @Value("${spring.data.redis.lettuce.pool.min-idle:0}")
  private int minIdle;

  @Value("${spring.data.redis.lettuce.pool.max-wait:-1}")
  private long maxWait;

  // ================================
  // Redisson 特定配置
  // ================================
  @Value("${redisson.mode:single}")
  private String redissonMode;

  @Value("${redisson.lock.watchdog-timeout:30000}")
  private int lockWatchdogTimeout;

  @Value("${redisson.lock.wait-time:100}")
  private int lockWaitTime;

  @Value("${redisson.lock.lease-time:30}")
  private int lockLeaseTime;

  @Value("${redisson.connection-pool.size:8}")
  private int redissonPoolSize;

  @Value("${redisson.connection-pool.min-idle:0}")
  private int redissonMinIdle;

  @Value("${redisson.connection.timeout:3000}")
  private int redissonTimeout;

  @Value("${redisson.connection.retry-attempts:3}")
  private int redissonRetryAttempts;

  @Value("${redisson.connection.retry-interval:1500}")
  private int redissonRetryInterval;

  // ================================
  // 集群模式配置
  // ================================
  @Value("${redisson.cluster.nodes:}")
  private String clusterNodes;

  @Value("${redisson.cluster.scan-interval:2000}")
  private int clusterScanInterval;

  @Value("${redisson.cluster.slave-connection-minimum-idle-size:1}")
  private int clusterSlaveMinIdle;

  @Value("${redisson.cluster.slave-connection-pool-size:64}")
  private int clusterSlavePoolSize;

  @Value("${redisson.cluster.master-connection-minimum-idle-size:1}")
  private int clusterMasterMinIdle;

  @Value("${redisson.cluster.master-connection-pool-size:64}")
  private int clusterMasterPoolSize;

  // ================================
  // 哨兵模式配置
  // ================================
  @Value("${redisson.sentinel.master-name:mymaster}")
  private String sentinelMasterName;

  @Value("${redisson.sentinel.nodes:}")
  private String sentinelNodes;

  @Value("${redisson.sentinel.password:}")
  private String sentinelPassword;

  @Value("${redisson.sentinel.database:0}")
  private int sentinelDatabase;

  // ================================
  // 主从模式配置
  // ================================
  @Value("${redisson.master-slave.master-address:}")
  private String masterAddress;

  @Value("${redisson.master-slave.slave-addresses:}")
  private String slaveAddresses;

  @Value("${redisson.master-slave.database:0}")
  private int masterSlaveDatabase;

  /** 配置 Redisson 客户端 */
  @Bean
  @Primary
  public RedissonClient redissonClient() {
    try {
      Config config = new Config();

      // 根据配置模式选择不同的连接方式
      switch (redissonMode.toLowerCase()) {
        case "cluster":
          configClusterMode(config);
          break;
        case "sentinel":
          configSentinelMode(config);
          break;
        case "master-slave":
          configMasterSlaveMode(config);
          break;
        case "single":
        default:
          configSingleMode(config);
          break;
      }

      // 分布式锁配置
      config.setLockWatchdogTimeout(lockWatchdogTimeout);

      RedissonClient redissonClient = Redisson.create(config);
      log.info("Redisson 客户端初始化成功: mode={}, host={}:{}", redissonMode, redisHost, redisPort);

      return redissonClient;

    } catch (Exception e) {
      log.error(
          "Redisson 客户端初始化失败: mode={}, host={}:{}, error={}",
          redissonMode,
          redisHost,
          redisPort,
          e.getMessage(),
          e);
      throw new RuntimeException("Redisson 初始化失败", e);
    }
  }

  /** 配置单机模式 */
  private void configSingleMode(Config config) {
    log.info("配置 Redisson 单机模式: {}:{}", redisHost, redisPort);

    config
        .useSingleServer()
        .setAddress("redis://" + redisHost + ":" + redisPort)
        .setDatabase(redisDatabase)
        .setConnectionPoolSize(redissonPoolSize)
        .setConnectionMinimumIdleSize(redissonMinIdle)
        .setConnectTimeout(redissonTimeout)
        .setIdleConnectionTimeout(redissonTimeout)
        .setRetryAttempts(redissonRetryAttempts)
        .setRetryInterval(redissonRetryInterval)
        .setKeepAlive(true)
        .setTcpNoDelay(true);

    // 如果设置了密码
    if (redisPassword != null && !redisPassword.trim().isEmpty()) {
      config.useSingleServer().setPassword(redisPassword);
    }
  }

  /** 配置集群模式 */
  private void configClusterMode(Config config) {
    if (clusterNodes == null || clusterNodes.trim().isEmpty()) {
      throw new IllegalArgumentException("集群模式需要配置 redisson.cluster.nodes");
    }

    log.info("配置 Redisson 集群模式: {}", clusterNodes);

    List<String> nodes = Arrays.asList(clusterNodes.split(","));
    config
        .useClusterServers()
        .setScanInterval(clusterScanInterval)
        .setMasterConnectionMinimumIdleSize(clusterMasterMinIdle)
        .setMasterConnectionPoolSize(clusterMasterPoolSize)
        .setSlaveConnectionMinimumIdleSize(clusterSlaveMinIdle)
        .setSlaveConnectionPoolSize(clusterSlavePoolSize)
        .setConnectTimeout(redissonTimeout)
        .setIdleConnectionTimeout(redissonTimeout)
        .setRetryAttempts(redissonRetryAttempts)
        .setRetryInterval(redissonRetryInterval)
        .setKeepAlive(true)
        .setTcpNoDelay(true);

    // 添加集群节点
    for (String node : nodes) {
      config.useClusterServers().addNodeAddress("redis://" + node.trim());
    }

    // 如果设置了密码
    if (redisPassword != null && !redisPassword.trim().isEmpty()) {
      config.useClusterServers().setPassword(redisPassword);
    }
  }

  /** 配置哨兵模式 */
  private void configSentinelMode(Config config) {
    if (sentinelNodes == null || sentinelNodes.trim().isEmpty()) {
      throw new IllegalArgumentException("哨兵模式需要配置 redisson.sentinel.nodes");
    }

    log.info("配置 Redisson 哨兵模式: master={}, sentinels={}", sentinelMasterName, sentinelNodes);

    List<String> sentinels = Arrays.asList(sentinelNodes.split(","));
    config
        .useSentinelServers()
        .setMasterName(sentinelMasterName)
        .setDatabase(sentinelDatabase)
        .setConnectTimeout(redissonTimeout)
        .setIdleConnectionTimeout(redissonTimeout)
        .setRetryAttempts(redissonRetryAttempts)
        .setRetryInterval(redissonRetryInterval)
        .setKeepAlive(true)
        .setTcpNoDelay(true);

    // 添加哨兵节点
    for (String sentinel : sentinels) {
      config.useSentinelServers().addSentinelAddress("redis://" + sentinel.trim());
    }

    // 如果设置了密码
    if (redisPassword != null && !redisPassword.trim().isEmpty()) {
      config.useSentinelServers().setPassword(redisPassword);
    }

    // 如果设置了哨兵密码
    if (sentinelPassword != null && !sentinelPassword.trim().isEmpty()) {
      config.useSentinelServers().setSentinelPassword(sentinelPassword);
    }
  }

  /** 配置主从模式 */
  private void configMasterSlaveMode(Config config) {
    if (masterAddress == null || masterAddress.trim().isEmpty()) {
      throw new IllegalArgumentException("主从模式需要配置 redisson.master-slave.master-address");
    }

    log.info("配置 Redisson 主从模式: master={}, slaves={}", masterAddress, slaveAddresses);

    config
        .useMasterSlaveServers()
        .setMasterAddress("redis://" + masterAddress.trim())
        .setDatabase(masterSlaveDatabase)
        .setConnectTimeout(redissonTimeout)
        .setIdleConnectionTimeout(redissonTimeout)
        .setRetryAttempts(redissonRetryAttempts)
        .setRetryInterval(redissonRetryInterval)
        .setKeepAlive(true)
        .setTcpNoDelay(true);

    // 添加从节点
    if (slaveAddresses != null && !slaveAddresses.trim().isEmpty()) {
      List<String> slaves = Arrays.asList(slaveAddresses.split(","));
      for (String slave : slaves) {
        config.useMasterSlaveServers().addSlaveAddress("redis://" + slave.trim());
      }
    }

    // 如果设置了密码
    if (redisPassword != null && !redisPassword.trim().isEmpty()) {
      config.useMasterSlaveServers().setPassword(redisPassword);
    }
  }
}
