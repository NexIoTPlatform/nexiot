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

package cn.universal.security;

import cn.hutool.json.JSONUtil;
import cn.universal.common.domain.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/** OAuth2 错误处理器 用于格式化 OAuth2 认证失败的错误响应 - 返回统一R对象格式 @Author gitee.com/NexIoT */
@Slf4j
@Component
public class OAuth2ErrorHandler implements AuthenticationFailureHandler {

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {

    log.warn("OAuth2(onAuthenticationFailure) 认证失败: {}", exception.getMessage());

    // 设置响应头 - 使用200状态码，通过R对象的code字段表示错误
    response.setStatus(HttpStatus.OK.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    // 构建统一的R对象响应
    String errorMsg = getErrorMessage(exception);
    R<Void> result = R.error(401, errorMsg);

    // 写入响应体
    response.getWriter().write(JSONUtil.toJsonStr(result));
  }

  /** 获取错误信息 */
  private String getErrorMessage(AuthenticationException exception) {
    if (exception instanceof BadCredentialsException) {
      return exception.getMessage();
    }

    if (exception instanceof OAuth2AuthenticationException) {
      OAuth2Error error = ((OAuth2AuthenticationException) exception).getError();
      return error.getDescription() != null ? error.getDescription() : error.getErrorCode();
    }

    if (exception instanceof OAuth2AuthorizationCodeRequestAuthenticationException) {
      OAuth2AuthorizationCodeRequestAuthenticationException authException =
          (OAuth2AuthorizationCodeRequestAuthenticationException) exception;
      OAuth2Error error = authException.getError();
      return error.getDescription() != null ? error.getDescription() : error.getErrorCode();
    }

    // 默认错误信息
    return "认证失败: " + exception.getMessage();
  }
}
