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

package cn.universal.common.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/10/12
 */
public class IotMapUtil {

  public static <K, V> String sortJoinValue(Map<K, V> map) {
    return sortJoinValue(map, ":", true);
  }

  public static <K, V> String sortJoinValue(
      Map<K, V> map, String keyValueSeparator, boolean isIgnoreNull) {
    map = MapUtil.sort(map);
    final StringBuilder strBuilder = StrUtil.builder();
    if (MapUtil.isNotEmpty(map)) {
      for (Entry<K, V> entry : map.entrySet()) {
        if (false == isIgnoreNull || entry.getKey() != null && entry.getValue() != null) {
          strBuilder.append(Convert.toStr(entry.getValue())).append(keyValueSeparator);
        }
      }
    }
    if (StrUtil.isNotBlank(strBuilder)) {
      return strBuilder.substring(0, strBuilder.length() - 1);
    }
    return strBuilder.toString();
  }
}
