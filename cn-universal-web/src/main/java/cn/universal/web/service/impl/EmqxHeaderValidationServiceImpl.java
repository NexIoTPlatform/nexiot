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
package cn.universal.web.service.impl;

import cn.universal.web.service.EmqxHeaderValidationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * EMQX 请求头校验服务实现类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j
@Service
public class EmqxHeaderValidationServiceImpl implements EmqxHeaderValidationService {

  @Value("${mqtt.cfg.emqx.header.key:universal-emqx}")
  private String headerKey;

  @Value("${mqtt.cfg.emqx.header.value:d41d8cd98f00b204e9800998ecf8427e}")
  private String headerValue;

  @Override
  public boolean validateHeader(HttpServletRequest request) {
    if (request == null) {
      log.warn("请求对象为空，无法验证请求头");
      return false;
    }

    // 获取请求头值
    String requestHeaderValue = request.getHeader(headerKey);

    if (!StringUtils.hasText(requestHeaderValue)) {
      log.warn("请求头验证失败: 缺少请求头 {}, 请求IP: {}", headerKey, getClientIp(request));
      return false;
    }

    // 验证请求头值
    boolean isValid = headerValue.equals(requestHeaderValue);

    if (!isValid) {
      log.warn(
          "请求头验证失败: 请求头 {}={}, 期望值: {}, 请求IP: {}",
          headerKey,
          requestHeaderValue,
          headerValue,
          getClientIp(request));
    } else {
      log.debug("请求头验证成功: {}={}, 请求IP: {}", headerKey, requestHeaderValue, getClientIp(request));
    }

    return isValid;
  }

  @Override
  public String getHeaderKey() {
    return headerKey;
  }

  @Override
  public String getHeaderValue() {
    return headerValue;
  }

  /**
   * 获取客户端真实IP地址
   *
   * @param request HTTP请求
   * @return 客户端IP地址
   */
  private String getClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("Proxy-Client-IP");
    }
    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("WL-Proxy-Client-IP");
    }
    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_CLIENT_IP");
    }
    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_X_FORWARDED_FOR");
    }
    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }

    // 如果是多个IP，取第一个
    if (ip != null && ip.contains(",")) {
      ip = ip.split(",")[0].trim();
    }

    return ip;
  }
}
