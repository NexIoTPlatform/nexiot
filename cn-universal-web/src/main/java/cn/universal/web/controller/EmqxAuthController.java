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
package cn.universal.web.controller;

import cn.hutool.json.JSONUtil;
import cn.universal.web.dto.EmqxAuthRequest;
import cn.universal.web.dto.EmqxAuthResponse;
import cn.universal.web.dto.EmqxWebhookClientDisconnectedRequest;
import cn.universal.web.service.EmqxAuthService;
import cn.universal.web.service.EmqxHeaderValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EMQX 认证控制器 处理 EMQX 的 HTTP 认证请求
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j
@RestController
@RequestMapping("/emqx")
@Tag(name = "EMQX认证管理", description = "处理EMQX的HTTP认证请求")
public class EmqxAuthController {
  @Autowired private EmqxHeaderValidationService emqxHeaderValidationService;
  @Autowired private EmqxAuthService emqxAuthService;

  /**
   * EMQX 认证接口 路径：/emqx/sys/auth 对应 EMQX 配置中的 HTTP 认证地址
   *
   * @param request 认证请求
   * @return 认证响应
   */
  @PostMapping("/sys/auth")
  @Operation(summary = "EMQX认证接口", description = "EMQX认证接口，对应EMQX配置中的HTTP认证地址")
  @Parameter(name = "httpRequest", description = "HTTP请求对象")
  @ApiResponse(
      responseCode = "200",
      description = "认证成功",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = EmqxAuthResponse.class)))
  public EmqxAuthResponse authenticate(
      @RequestBody EmqxAuthRequest request, HttpServletRequest httpRequest) {
    log.info(
        "收到 EMQX 认证请求: username={}, clientId={}, ip={}",
        request.getUsername(),
        request.getClientid(),
        request.getIp_address());

    // 1. 首先验证请求头
    if (!emqxHeaderValidationService.validateHeader(httpRequest)) {
      log.warn(
          "EMQX 请求头验证失败: username={}, clientId={}, ip={}",
          request.getUsername(),
          request.getClientid(),
          request.getIp_address());
      return new EmqxAuthResponse("deny");
    }

    try {
      EmqxAuthResponse response = emqxAuthService.authenticate(request);
      log.info(
          "EMQX 认证完成: username={}, result={}", request.getUsername(), JSONUtil.toJsonStr(response));
      return response;
    } catch (Exception e) {
      log.error("EMQX 认证异常: username={}, error={}", request.getUsername(), e.getMessage(), e);
      // 发生异常时返回拒绝
      return new EmqxAuthResponse("deny");
    }
  }

  /**
   * EMQX Webhook 客户端离线事件接口
   *
   * @param request 离线事件请求
   * @param httpRequest HTTP请求
   * @return 响应结果
   */
  @PostMapping("/webhook/client/disconnected")
  @Operation(summary = "EMQX客户端离线Webhook", description = "处理EMQX客户端断开连接事件")
  @Parameter(name = "httpRequest", description = "HTTP请求对象")
  public cn.universal.common.domain.R<Void> clientDisconnected(
      @RequestBody EmqxWebhookClientDisconnectedRequest request, HttpServletRequest httpRequest) {
    log.info(
        "收到 EMQX 客户端离线 webhook: event={}, username={}, clientId={}, reason={}",
        request == null ? null : request.getEvent(),
        request == null ? null : request.getUsername(),
        request == null ? null : request.getClientid(),
        request == null ? null : request.getReason());

    if (!emqxHeaderValidationService.validateHeader(httpRequest)) {
      log.warn(
          "EMQX 离线 webhook 请求头验证失败: username={}, clientId={}, ip={}",
          request == null ? null : request.getUsername(),
          request == null ? null : request.getClientid(),
          request == null ? null : request.getIpAddress());
      return cn.universal.common.domain.R.error("deny");
    }

    try {
      emqxAuthService.handleClientDisconnected(request);
      return cn.universal.common.domain.R.ok();
    } catch (Exception e) {
      log.error(
          "EMQX 客户端离线 webhook 处理异常: username={}, clientId={}, error={}",
          request == null ? null : request.getUsername(),
          request == null ? null : request.getClientid(),
          e.getMessage(),
          e);
      return cn.universal.common.domain.R.error("deny");
    }
  }
}
