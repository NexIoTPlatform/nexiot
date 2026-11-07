/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 小程序安全配置 配置小程序API的独立认证过滤器链
 *
 * @author NexIoT
 * @since 2025/12/16
 */
@Slf4j
@Configuration
public class MpSecurityConfig {

  @Autowired private MpAuthenticationFilter mpAuthenticationFilter;

  /** 小程序API安全过滤器链 优先级高于默认配置，专门处理 /mp/** 路径 */
  @Bean
  @Order(1)
  public SecurityFilterChain mpSecurityFilterChain(HttpSecurity http) throws Exception {
    log.info("配置小程序安全过滤器链...");

    http
        // 只匹配小程序API路径
        .securityMatcher("/mp/**")
        // 禁用CSRF
        .csrf(AbstractHttpConfigurer::disable)
        // 所有请求放行（由 MpAuthenticationFilter 处理认证）
        .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
        // 添加小程序认证过滤器
        .addFilterBefore(mpAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    log.info("小程序安全过滤器链配置完成");
    return http.build();
  }
}
