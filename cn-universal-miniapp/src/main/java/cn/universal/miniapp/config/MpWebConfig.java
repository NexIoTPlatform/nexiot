/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.config;

import cn.universal.miniapp.security.MpAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 小程序模块配置类
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Configuration
public class MpWebConfig {

  @Autowired private MpAuthenticationFilter mpAuthenticationFilter;

  /** 注册小程序认证过滤器 */
  @Bean
  public FilterRegistrationBean<MpAuthenticationFilter> mpAuthFilterRegistration() {
    FilterRegistrationBean<MpAuthenticationFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(mpAuthenticationFilter);
    registration.addUrlPatterns("/mp/api/*");
    registration.setName("mpAuthenticationFilter");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    return registration;
  }

  /** 注册MyBatis AppId拦截器 */
  @Bean
  public MpAppIdInterceptor mpAppIdInterceptor() {
    return new MpAppIdInterceptor();
  }
}
