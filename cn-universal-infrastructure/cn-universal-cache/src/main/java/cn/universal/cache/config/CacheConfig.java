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

import cn.universal.cache.manager.MultiLevelCacheManager;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 多级缓存配置
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MultiLevelCacheProperties.class)
@ConditionalOnProperty(
    prefix = "cache.multi-level",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class CacheConfig {

  private final MultiLevelCacheProperties properties;

  public CacheConfig(MultiLevelCacheProperties properties) {
    this.properties = properties;
  }

  /** 创建全局Jackson配置的Redis序列化器，与RedisConfig保持一致 解决序列化配置不一致导致的类型冲突问题 */
  @Bean
  public GenericJackson2JsonRedisSerializer redisJsonSerializer(ObjectMapper objectMapper) {
    // 使用全局配置的ObjectMapper，确保与RedisConfig的一致性
    ObjectMapper cacheObjectMapper = objectMapper.copy();

    // 启用类型信息，与RedisConfig保持一致
    cacheObjectMapper.activateDefaultTyping(
        LaissezFaireSubTypeValidator.instance,
        ObjectMapper.DefaultTyping.NON_FINAL,
        JsonTypeInfo.As.PROPERTY);

    // 全局忽略未知字段
    cacheObjectMapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // 防御循环引用
    cacheObjectMapper.disable(SerializationFeature.FAIL_ON_SELF_REFERENCES);

    // 配置时间模块
    cacheObjectMapper.registerModule(new JavaTimeModule());
    cacheObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // 全局配置：忽略所有未知字段
    cacheObjectMapper.addMixIn(Object.class, IgnoreUnknownPropertiesMixin.class);

    // 增强反序列化支持
    cacheObjectMapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
        false);
    cacheObjectMapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES,
        false);
    cacheObjectMapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
    cacheObjectMapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
        true);
    cacheObjectMapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
        true);

    // 支持内部类反序列化
    cacheObjectMapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);

    // 配置序列化特性
    cacheObjectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    cacheObjectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);

    log.info("✅ 统一Redis序列化器已配置：与RedisConfig保持一致，启用类型信息，增强反序列化支持");

    return new GenericJackson2JsonRedisSerializer(cacheObjectMapper);
  }

  /** L1缓存管理器（Caffeine） */
  @Bean("l1CacheManager")
  @ConditionalOnProperty(prefix = "cache.multi-level", name = "enabled", havingValue = "true")
  public CacheManager l1CacheManager() {
    if (!properties.getL1().isEnabled()) {
      log.warn("L1缓存已禁用");
      return null;
    }
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.setCaffeine(
        Caffeine.newBuilder()
            .expireAfterWrite(properties.getL1().getExpireAfterWrite(), TimeUnit.SECONDS)
            .expireAfterAccess(properties.getL1().getExpireAfterAccess(), TimeUnit.SECONDS)
            .initialCapacity(properties.getL1().getInitialCapacity())
            .maximumSize(properties.getL1().getMaximumSize()));

    log.info(
        "L1缓存管理器初始化完成，过期时间：{}秒，最大容量：{}",
        properties.getL1().getExpireAfterWrite(),
        properties.getL1().getMaximumSize());

    return cacheManager;
  }

  /** 修复 L2 缓存管理器：强制启用 SCAN 策略，使用统一的序列化配置 */
  @Bean("l2CacheManager")
  @ConditionalOnProperty(prefix = "cache.multi-level", name = "enabled", havingValue = "true")
  public CacheManager l2CacheManager(
      RedisConnectionFactory redisConnectionFactory,
      GenericJackson2JsonRedisSerializer redisJsonSerializer) {
    if (!properties.getL2().isEnabled()) {
      log.warn("L2缓存已禁用");
      return null;
    }

    RedisCacheConfiguration config =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(properties.getL2().getExpireAfterWrite()))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(redisJsonSerializer))
            .disableCachingNullValues();

    // 关键修复：注入 SCAN 策略的 RedisCacheWriter [1,6](@ref)
    RedisCacheWriter cacheWriter =
        RedisCacheWriter.lockingRedisCacheWriter(
            redisConnectionFactory, BatchStrategies.scan(1000) // 每批扫描 1000 个键
            );

    RedisCacheManager cacheManager =
        RedisCacheManager.builder(cacheWriter) // 使用自定义 CacheWriter
            .cacheDefaults(config)
            .transactionAware()
            .build();

    log.info("✅ L2缓存管理器已启用SCAN策略，批次大小：1000，过期时间：{}秒", properties.getL2().getExpireAfterWrite());
    return cacheManager;
  }

  //  /**
  //   * L2缓存管理器（Redis）
  //   */
  //  @Bean("l2CacheManager")
  //  @ConditionalOnProperty(prefix = "cache.multi-level", name = "enabled", havingValue = "true")
  //  public CacheManager l2CacheManager(RedisConnectionFactory redisConnectionFactory) {
  //    if (!properties.getL2().isEnabled()) {
  //      log.warn("L2缓存已禁用");
  //      return null;
  //    }
  //
  //    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
  //        .entryTtl(Duration.ofSeconds(properties.getL2().getExpireAfterWrite()))
  //        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new
  // StringRedisSerializer()))
  //        .serializeValuesWith(
  //            RedisSerializationContext.SerializationPair.fromSerializer(new
  // GenericJackson2JsonRedisSerializer()))
  //        .disableCachingNullValues();
  //
  //    // 强制使用 SCAN 策略，避免 KEYS 命令
  //    RedisCacheManager cacheManager = RedisCacheManager.builder(redisConnectionFactory)
  //        .cacheDefaults(config)
  //        .transactionAware()
  //        .withInitialCacheConfigurations(java.util.Map.of(
  //            "default", config
  //        ))
  //        .build();
  //
  //    log.info("L2缓存管理器初始化完成，过期时间：{}秒，键前缀：{}，使用SCAN策略",
  //        properties.getL2().getExpireAfterWrite(), properties.getL2().getKeyPrefix());
  //
  //    return cacheManager;
  //  }

  /** 多级缓存管理器（主缓存管理器） 只有当多级缓存启用时才创建 */
  @Bean("multiLevelCacheManager")
  @Primary
  @ConditionalOnProperty(prefix = "cache.multi-level", name = "enabled", havingValue = "true")
  public CacheManager multiLevelCacheManager() {
    MultiLevelCacheManager cacheManager = new MultiLevelCacheManager();
    cacheManager.setProperties(properties);
    log.info("多级缓存管理器初始化完成");
    return cacheManager;
  }

  /** 默认缓存管理器（当多级缓存禁用时使用） 使用统一的序列化配置 */
  @Bean("defaultCacheManager")
  @Primary
  @ConditionalOnProperty(prefix = "cache.multi-level", name = "enabled", havingValue = "false")
  public CacheManager defaultCacheManager(
      RedisConnectionFactory redisConnectionFactory,
      GenericJackson2JsonRedisSerializer redisJsonSerializer) {
    RedisCacheConfiguration config =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(3600)) // 默认1小时
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(redisJsonSerializer))
            .disableCachingNullValues();

    RedisCacheManager cacheManager =
        RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(config)
            .transactionAware()
            .build();

    log.info("默认缓存管理器初始化完成（多级缓存已禁用）");
    return cacheManager;
  }

  /** 全局缓存异常处理器，防止缓存异常影响主业务 */
  @Bean
  public CacheErrorHandler errorHandler() {
    return new SimpleCacheErrorHandler() {
      @Override
      public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        // 1. 区分反序列化错误和其他错误
        if (exception instanceof SerializationException) {
          log.error("[Cache] 反序列化失败 → 触发降级查询. Key={}, Cache={}", key, cache.getName(), exception);
          // 关键降级：返回null使Spring Cache调用原方法
        }
        // 其他错误仅记录日志
        log.warn("[Cache] Get操作失败: {} Key={}", exception.getMessage(), key);
      }

      @Override
      public void handleCachePutError(
          RuntimeException exception, Cache cache, Object key, Object value) {
        // 写入失败时仅告警（防止坏数据污染缓存）
        log.warn("[Cache] Put操作失败: {} Key={}", exception.getMessage(), key);
      }

      @Override
      public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        // 清除失败不影响主流程
        log.warn("[Cache] Evict操作失败: {} Key={}", exception.getMessage(), key);
      }

      @Override
      public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("[Cache] Clear操作失败: {}", exception.getMessage());
        // 识别KEYS命令阻塞问题（如Redis集群限制）
        if (exception.getMessage().contains("KEYS")) {
          log.error("Redis禁止KEYS命令! 请改用SCAN策略[8](@ref)");
        }
      }
    };
  }
}

/** 全局忽略未知字段的Mixin */
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class IgnoreUnknownPropertiesMixin {}

/** 安全的对象反序列化器，处理反序列化异常 */
@Slf4j
class SafeObjectDeserializer extends StdDeserializer<Object> {

  public SafeObjectDeserializer() {
    super(Object.class);
  }

  @Override
  public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    try {
      // 尝试正常反序列化
      return ctxt.readValue(p, Object.class);
    } catch (Exception e) {
      log.warn("反序列化失败，返回null: {}", e.getMessage());
      // 反序列化失败时返回null，而不是抛出异常
      return null;
    }
  }
}
