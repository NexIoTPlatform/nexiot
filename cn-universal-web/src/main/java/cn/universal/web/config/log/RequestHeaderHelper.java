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

package cn.universal.web.config.log;

import java.util.HashSet;
import java.util.Set;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/04/19
 */
public class RequestHeaderHelper {

  public static final String AUTHORIZATION = "Authorization";

  public static final Set<String> headers = new HashSet<>();

  static {
    headers.add(AUTHORIZATION);
    headers.add(AUTHORIZATION.toLowerCase());
  }

  public static boolean matchHeader(String key) {
    return headers.contains(key.toLowerCase());
  }
}
