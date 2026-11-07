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

package cn.universal.web.controller.common;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OAuth2Controller {

  /** 向后兼容的 /oauth/token 端点 转发到新的 Spring Authorization Server 的 /oauth2/token 端点 */
  @PostMapping("/oauth/token")
  public void forwardLegacyTokenEndpoint(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    RequestDispatcher dispatcher = request.getRequestDispatcher("/oauth2/token");
    dispatcher.forward(request, response);
  }

  /** 向后兼容的 /api/oauth/token 端点 转发到新的 Spring Authorization Server 的 /oauth2/token 端点 */
  @PostMapping("/api/oauth/token")
  public void forwardApiTokenEndpoint(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    RequestDispatcher dispatcher = request.getRequestDispatcher("/oauth2/token");
    dispatcher.forward(request, response);
  }
}
