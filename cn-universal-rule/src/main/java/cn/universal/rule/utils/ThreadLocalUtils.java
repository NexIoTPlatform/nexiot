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

package cn.universal.rule.utils;

import java.util.Map;

/**
 * todo @Author gitee.com/NexIoT
 *
 * @since 2025/12/3 14:00
 */
public class ThreadLocalUtils {

  private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = new ThreadLocal<>();

  public static void set(Map<String, Object> param) {
    THREAD_LOCAL.set(param);
  }

  public static void remove() {
    THREAD_LOCAL.remove();
  }

  public static Object get(String key) {
    return THREAD_LOCAL.get().get(key);
  }
}
