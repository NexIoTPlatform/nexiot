/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 设备日志表一致性哈希分表策略，基于iotId路由到指定分表
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 */

package cn.universal.persistence.consistent;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 设备日志表一致性哈希分表策略 核心逻辑：基于iotId的FNV1_32_HASH值，通过一致性哈希算法路由到指定数量的分表 配置前缀：shard.log */
@Slf4j
@Component
public class DeviceLogHashSharding implements DeviceLogTableShardingPolicy {

  // ========== 配置项 ==========
  /** 日志表分表总数（实际节点数） */
  private static int maxTableShard;

  /** 每个实际分表对应的虚拟节点数 */
  private static int virtualNodeCount;

  /** 是否开启日志表分表 */
  @Value("${shard.log.enable:true}")
  private boolean shardingEnabled;

  /** 日志表基础表名前缀 */
  @Value("${shard.log.table.prefix:${timescaledb.table.iot_device_log:iot_device_log}}")
  private String configuredTableNamePrefix;

  /** 日志表命名空间，MySQL 下可对应 database，PostgreSQL/Timescale 下可对应 schema */
  @Value("${shard.log.table.namespace:${timescaledb.schema:${mysql.log.database:}}}")
  private String configuredNamespace;

  // ========== 一致性哈希核心组件 ==========
  /** 一致性哈希环：key=虚拟节点Hash值，value=实际分表序号（0~maxTableShard-1） */
  private static final SortedMap<Integer, Integer> HASH_RING = new TreeMap<>();

  /** 本地缓存：iotId -> 分表序号，避免重复计算Hash，提升路由性能 */
  private static final Cache<String, Integer> IOT_ID_TABLE_CACHE =
      Caffeine.newBuilder()
          .expireAfterWrite(5, TimeUnit.MINUTES) // 5分钟过期
          .maximumSize(2048) // 最大缓存2048条
          .recordStats() // 开启缓存统计（便于监控）
          .build();

  // ========== 配置注入 ==========
  /**
   * 设置日志表分表总数
   *
   * @param maxTableShard 分表数（必须>0）
   */
  @Value("${shard.log.table.number:24}") // 增加默认值，避免配置缺失
  public void setMaxTableShard(Integer maxTableShard) {
    if (maxTableShard <= 0) {
      throw new IllegalArgumentException("日志分表数量必须大于0（shard.log.table.number）");
    }
    DeviceLogHashSharding.maxTableShard = maxTableShard;
    log.info("日志分表-实际节点数初始化完成：{}", maxTableShard);
  }

  /**
   * 设置每个实际分表的虚拟节点数
   *
   * @param virtualNodeCount 虚拟节点数（不能<0）
   */
  @Value("${shard.log.virtual.number:100}") // 增加默认值
  public void setVirtualNodeCount(Integer virtualNodeCount) {
    if (virtualNodeCount < 0) {
      throw new IllegalArgumentException("日志表虚拟节点数不能为负数（shard.log.virtual.number）");
    }
    DeviceLogHashSharding.virtualNodeCount = virtualNodeCount;
    log.info("日志分表-虚拟节点数初始化完成：{}", virtualNodeCount);
  }

  // ========== 初始化一致性哈希环 ==========
  /** 初始化日志表的一致性哈希环 时机：Spring容器初始化该Bean后执行 */
  @PostConstruct
  private void initHashRing() {
    long startTime = System.currentTimeMillis();
    HASH_RING.clear(); // 清空旧数据，避免重复初始化

    // 为每个实际分表创建虚拟节点并加入哈希环
    for (int tableSeq = 0; tableSeq < maxTableShard; tableSeq++) {
      for (int virtualSeq = 0; virtualSeq <= virtualNodeCount; virtualSeq++) {
        // 虚拟节点唯一标识：盐值 + 实际表序号 + 虚拟节点序号
        String virtualNodeKey = String.format("%s#%d_%d", HASH_SALT, tableSeq, virtualSeq);
        int virtualNodeHash = getHash(virtualNodeKey);
        HASH_RING.put(virtualNodeHash, tableSeq);
      }
    }

    long costTime = System.currentTimeMillis() - startTime;
    log.info(
        "日志表-一致性哈希环初始化完成 | 实际分表数：{} | 虚拟节点总数：{} | 耗时：{}ms",
        maxTableShard,
        HASH_RING.size(),
        costTime);
    // 打印缓存配置信息
    log.info("日志分表缓存配置：过期时间5分钟，最大容量2048");
  }

  // ========== 核心分表方法 ==========
  /**
   * 生成设备日志表的分表名
   *
   * @param tableNamePrefix 表前缀（如：iot_device_log）
   * @param value 路由键（必须是iotId字符串）
   * @return 完整分表名（如：iot_device_log_8）
   */
  @Override
  public String generateTableName(String tableNamePrefix, Object value) {
    String actualTableNamePrefix = resolveActualTableNamePrefix(tableNamePrefix);

    // 1. 分表未开启，直接返回原前缀
    if (!shardingEnabled) {
      log.debug("日志分表未开启，返回基础表名：{}", actualTableNamePrefix);
      return actualTableNamePrefix;
    }

    // 2. 校验入参合法性
    verifyTableNamePrefix(actualTableNamePrefix);
    String iotId = getValidIotId(value);
    if (StrUtil.isBlank(iotId)) {
      log.debug("日志分表路由键为空，回退到基础表：{}", actualTableNamePrefix);
      return actualTableNamePrefix;
    }

    // 3. 从缓存获取分表序号（缓存命中则直接返回）
    Integer tableSeq = IOT_ID_TABLE_CACHE.getIfPresent(iotId);
    if (tableSeq == null) {
      // 缓存未命中，通过一致性哈希计算分表序号
      tableSeq = routeTableByConsistentHash(iotId);
      IOT_ID_TABLE_CACHE.put(iotId, tableSeq);
      log.debug("日志分表-缓存未命中 | iotId：{} | 路由到分表序号：{}", iotId, tableSeq);
    } else {
      log.debug("日志分表-缓存命中 | iotId：{} | 分表序号：{}", iotId, tableSeq);
    }

    // 4. 拼接最终分表名
    String finalTableName = String.format("%s_%d", actualTableNamePrefix, tableSeq);
    log.debug("日志分表-路由完成 | iotId：{} | 目标表名：{}", iotId, finalTableName);
    return finalTableName;
  }

  // ========== 辅助方法 ==========
  /**
   * 校验表前缀合法性
   *
   * @param tableNamePrefix 表前缀
   */
  private void verifyTableNamePrefix(String tableNamePrefix) {
    if (StrUtil.isBlank(tableNamePrefix)) {
      throw new IllegalArgumentException("日志分表-表前缀不能为空");
    }
  }

  /**
   * 校验并获取合法的iotId
   *
   * @param value 入参值
   * @return 合法的iotId字符串
   */
  private String getValidIotId(Object value) {
    if (ObjectUtil.isEmpty(value)) {
      return null;
    }
    String iotId = value.toString().trim();
    if (StrUtil.isBlank(iotId)) {
      return null;
    }
    return iotId;
  }

  private String resolveActualTableNamePrefix(String fallbackPrefix) {
    String tableNamePrefix = StrUtil.blankToDefault(configuredTableNamePrefix, fallbackPrefix).trim();
    if (StrUtil.isBlank(configuredNamespace)) {
      return tableNamePrefix;
    }
    return configuredNamespace.trim() + "." + tableNamePrefix;
  }

  /**
   * 基于一致性哈希算法路由日志表序号
   *
   * @param iotId 设备唯一编码
   * @return 分表序号（0~maxTableShard-1）
   */
  private int routeTableByConsistentHash(String iotId) {
    int iotIdHash = getHash(iotId);
    // 查找哈希环上顺时针第一个≥iotIdHash的虚拟节点
    SortedMap<Integer, Integer> subRing = HASH_RING.tailMap(iotIdHash);

    int targetVirtualNodeHash;
    if (subRing.isEmpty()) {
      // 无匹配节点，取哈希环第一个节点（哈希环是环形）
      targetVirtualNodeHash = HASH_RING.firstKey();
      log.debug(
          "日志分表-哈希环匹配 | iotId：{} | Hash值：{} | 无匹配节点，取首个虚拟节点Hash：{}",
          iotId,
          iotIdHash,
          targetVirtualNodeHash);
    } else {
      // 取第一个匹配的虚拟节点
      targetVirtualNodeHash = subRing.firstKey();
      log.debug(
          "日志分表-哈希环匹配 | iotId：{} | Hash值：{} | 匹配到虚拟节点Hash：{}",
          iotId,
          iotIdHash,
          targetVirtualNodeHash);
    }

    // 返回虚拟节点对应的实际分表序号
    Integer tableSeq = HASH_RING.get(targetVirtualNodeHash);
    log.debug("日志分表-路由结果 | iotId：{} | 分表序号：{}", iotId, tableSeq);
    return tableSeq;
  }

  /**
   * FNV1_32_HASH算法计算Hash值（非负）
   *
   * @param str 待计算字符串
   * @return 非负Hash值
   */
  public static int getHash(String str) {
    final int prime = 16777619;
    int hash = (int) 2166136261L;

    for (int i = 0; i < str.length(); i++) {
      hash = (hash ^ str.charAt(i)) * prime;
    }

    // Hash混淆，提升分布均匀性
    hash += hash << 13;
    hash ^= hash >> 7;
    hash += hash << 3;
    hash ^= hash >> 17;
    hash += hash << 5;

    // 确保Hash值非负
    return Math.abs(hash);
  }

  // ========== 监控/调试方法（可选） ==========
  /** 获取日志表哈希环大小（虚拟节点总数） */
  public int getHashRingSize() {
    return HASH_RING.size();
  }

  /** 获取日志分表缓存统计信息（命中率、命中数等） */
  public String getCacheStats() {
    return IOT_ID_TABLE_CACHE.stats().toString();
  }
}
