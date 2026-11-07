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
package cn.universal.web.service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * EMQX 请求头校验服务接口 用于验证 EMQX 请求的合法性
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
public interface EmqxHeaderValidationService {

  /**
   * 验证请求头
   *
   * @param request HTTP请求
   * @return 验证结果
   */
  boolean validateHeader(HttpServletRequest request);

  /**
   * 获取配置的请求头键名
   *
   * @return 请求头键名
   */
  String getHeaderKey();

  /**
   * 获取配置的请求头值
   *
   * @return 请求头值
   */
  String getHeaderValue();
}
