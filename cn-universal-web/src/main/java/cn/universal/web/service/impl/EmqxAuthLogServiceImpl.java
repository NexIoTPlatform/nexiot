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

import cn.universal.web.service.EmqxAuthLogService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * EMQX 认证日志服务实现类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j
@Service
public class EmqxAuthLogServiceImpl implements EmqxAuthLogService {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Override
  public void logAuthResult(
      String username, String clientId, String ipAddress, String authType, String result) {
    try {
      String timestamp = LocalDateTime.now().format(FORMATTER);

      // 记录认证结果日志
      log.info(
          "EMQX认证日志 - 时间: {}, 用户名: {}, 客户端ID: {}, IP地址: {}, 认证类型: {}, 结果: {}",
          timestamp,
          username,
          clientId,
          ipAddress,
          authType,
          result);

      // TODO: 这里可以添加数据库日志记录逻辑
      // 例如：记录到认证日志表，用于审计和监控

    } catch (Exception e) {
      log.error("记录认证日志失败: username={}, error={}", username, e.getMessage(), e);
    }
  }
}
