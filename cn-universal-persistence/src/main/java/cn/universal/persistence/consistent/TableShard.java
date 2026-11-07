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

package cn.universal.persistence.consistent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = {ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TableShard {

  // 表前缀名
  String tableNamePrefix();

  // 值
  String value() default "";

  // 是否是字段名，如果是需要解析请求参数改字段名的值（默认否）
  boolean fieldFlag() default false;

  // 对应的分表策略类
  Class<? extends DeviceLogTableShardingPolicy> shardStrategy();
}
