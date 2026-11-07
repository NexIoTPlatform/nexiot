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

package cn.universal.databridge.plugin;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 源范围枚举 定义数据桥接的源数据范围
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/15
 */
@Getter
@AllArgsConstructor
public enum SourceScope {
  ALL_PRODUCTS("ALL_PRODUCTS", "所有产品", "适用于所有产品的数据桥接"),
  SPECIFIC_PRODUCTS("SPECIFIC_PRODUCTS", "指定产品", "针对指定产品的数据桥接"),
  APPLICATION("APPLICATION", "应用级别", "针对特定应用的数据桥接");

  private final String code;
  private final String name;
  private final String description;

  /** 根据代码获取源范围 */
  public static SourceScope fromCode(String code) {
    for (SourceScope scope : values()) {
      if (scope.getCode().equals(code)) {
        return scope;
      }
    }
    throw new IllegalArgumentException("未知的源范围: " + code);
  }
}
