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

package cn.universal.common.enums;

import java.util.HashMap;
import java.util.Map;
import org.springframework.lang.Nullable;

/** HTTP请求方式枚举 */
public enum HttpMethod {
  GET,
  HEAD,
  POST,
  PUT,
  PATCH,
  DELETE,
  OPTIONS,
  TRACE;

  private static final Map<String, HttpMethod> mappings = new HashMap<>(16);

  static {
    for (HttpMethod httpMethod : values()) {
      mappings.put(httpMethod.name(), httpMethod);
    }
  }

  @Nullable
  public static HttpMethod resolve(@Nullable String method) {
    return (method != null ? mappings.get(method) : null);
  }

  public boolean matches(String method) {
    return (this == resolve(method));
  }
}
