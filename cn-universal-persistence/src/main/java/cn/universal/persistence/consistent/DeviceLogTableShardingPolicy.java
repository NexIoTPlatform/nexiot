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

import cn.hutool.core.util.StrUtil;

public interface DeviceLogTableShardingPolicy {

  String HASH_SALT = "fa71207f20e2c2cd7151c578304190f5";

  /**
   * @param tableNamePrefix 表前缀名
   * @param value 值
   */
  String generateTableName(String tableNamePrefix, Object value);

  /** 验证tableNamePrefix */
  default void verificationTableNamePrefix(String tableNamePrefix) {
    if (StrUtil.isBlank(tableNamePrefix)) {
      throw new RuntimeException("tableNamePrefix is null");
    }
  }
}
