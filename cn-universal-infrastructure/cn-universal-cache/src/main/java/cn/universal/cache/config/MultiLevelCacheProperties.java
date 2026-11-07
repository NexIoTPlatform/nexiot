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

package cn.universal.cache.config;

import cn.universal.cache.strategy.CacheStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 多级缓存配置属性
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Data
@ConfigurationProperties(prefix = "cache.multi-level")
public class MultiLevelCacheProperties {

  /** 多级缓存总开关 true: 启用多级缓存（L1 + L2） false: 禁用多级缓存，使用默认的 Spring Cache 策略 */
  private boolean enabled = true;

  /** L1缓存配置（本地缓存） */
  private L1Config l1 = new L1Config();

  /** L2缓存配置（分布式缓存） */
  private L2Config l2 = new L2Config();

  /** 默认配置 */
  private DefaultConfig defaults = new DefaultConfig();

  /** L1缓存配置 */
  @Data
  public static class L1Config {

    /** 写入后过期时间（秒） */
    private long expireAfterWrite = 300;

    /** 访问后过期时间（秒） */
    private long expireAfterAccess = 300;

    /** 初始容量 */
    private int initialCapacity = 100;

    /** 最大容量 */
    private int maximumSize = 5000;

    /** 是否启用L1缓存 */
    private boolean enabled = true;
  }

  /** L2缓存配置 */
  @Data
  public static class L2Config {

    /** 写入后过期时间（秒） */
    private long expireAfterWrite = 3600;

    /** 键前缀 */
    private String keyPrefix = "mlc:";

    /** 是否启用L2缓存 */
    private boolean enabled = true;
  }

  /** 默认配置 */
  @Data
  public static class DefaultConfig {

    /** 默认缓存策略 */
    private CacheStrategy strategy = CacheStrategy.WRITE_THROUGH;

    /** 是否启用多级缓存 */
    private boolean enableMultiLevel = true;

    /** 默认L1过期时间（秒） */
    private long defaultL1Expire = 300;

    /** 默认L2过期时间（秒） */
    private long defaultL2Expire = 3600;
  }
}
