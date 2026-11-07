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

package cn.universal.cache.annotation;

import cn.universal.cache.strategy.CacheStrategy;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.annotation.AliasFor;

/**
 * 多级缓存注解 支持L1本地缓存和L2分布式缓存的多级缓存策略
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Cacheable
public @interface MultiLevelCacheable {

  /** 缓存名称 */
  @AliasFor(annotation = Cacheable.class, attribute = "cacheNames")
  String[] cacheNames() default {};

  /** 缓存键 */
  @AliasFor(annotation = Cacheable.class, attribute = "key")
  String key() default "";

  /** 键生成器 */
  @AliasFor(annotation = Cacheable.class, attribute = "keyGenerator")
  String keyGenerator() default "";

  /** 缓存管理器 */
  @AliasFor(annotation = Cacheable.class, attribute = "cacheManager")
  String cacheManager() default "multiLevelCacheManager";

  /** 条件表达式 */
  @AliasFor(annotation = Cacheable.class, attribute = "condition")
  String condition() default "";

  /** 排除条件 */
  @AliasFor(annotation = Cacheable.class, attribute = "unless")
  String unless() default "";

  /** L1缓存过期时间（秒） */
  long l1Expire() default 300;

  /** L2缓存过期时间（秒） */
  long l2Expire() default 3600;

  /** 是否启用多级缓存 */
  boolean multiLevel() default true;

  /** 缓存策略 */
  CacheStrategy strategy() default CacheStrategy.WRITE_THROUGH;
}
