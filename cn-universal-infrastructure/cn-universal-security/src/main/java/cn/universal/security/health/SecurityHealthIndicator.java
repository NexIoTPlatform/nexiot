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

package cn.universal.security.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** 安全健康检查 @Author gitee.com/NexIoT */
@Slf4j
@Component
public class SecurityHealthIndicator implements HealthIndicator {

  @Value("${security.production.enabled:false}")
  private boolean productionSecurityEnabled;

  @Value("${security.allowed.ips:}")
  private String allowedIps;

  @Value("${security.actuator.enabled:false}")
  private boolean actuatorEnabled;

  @Override
  public Health health() {
    try {
      if (productionSecurityEnabled) {
        return Health.up()
            .withDetail("security_mode", "production")
            .withDetail("allowed_ips", allowedIps)
            .withDetail("actuator_enabled", actuatorEnabled)
            .withDetail("status", "Production security mode enabled")
            .build();
      } else {
        return Health.up()
            .withDetail("security_mode", "development")
            .withDetail("allowed_ips", "all")
            .withDetail("actuator_enabled", true)
            .withDetail("status", "Development mode - relaxed security")
            .build();
      }
    } catch (Exception e) {
      log.error("Security health check failed", e);
      return Health.down().withDetail("error", e.getMessage()).build();
    }
  }
}
