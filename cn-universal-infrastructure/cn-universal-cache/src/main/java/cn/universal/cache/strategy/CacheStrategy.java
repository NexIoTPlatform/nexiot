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

package cn.universal.cache.strategy;

/**
 * 缓存策略枚举
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
public enum CacheStrategy {

  /** 直写策略：同步写入所有缓存级别 优点：一致性最好 缺点：写入性能较低 */
  WRITE_THROUGH,

  /** 回写策略：异步写入L2缓存 优点：写入性能最好 缺点：可能存在短暂的数据不一致 */
  WRITE_BEHIND,

  /** 绕写策略：只写L2缓存，L1通过读取回填 优点：内存友好，适合读多写少的场景 缺点：首次读取需要回填L1 */
  WRITE_AROUND
}
