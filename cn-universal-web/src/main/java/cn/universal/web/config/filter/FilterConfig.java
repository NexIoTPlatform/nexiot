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

package cn.universal.web.config.filter;

import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 过滤器，xss,auth等 */
@Configuration
public class FilterConfig {

  @Bean(name = "responseHeadFilter")
  public Filter responseHeadFilter() {
    return new ResponseHeadFilter();
  }

  @Bean
  public FilterRegistrationBean responseHeadFilterInit() {
    FilterRegistrationBean registration = new FilterRegistrationBean();

    registration.setFilter(responseHeadFilter());
    registration.addUrlPatterns("/*");
    registration.setOrder(0);
    return registration;
  }

  @Bean(name = "xssFilter")
  public Filter xssFilter() {
    return new XssFilter();
  }

  @Bean
  public FilterRegistrationBean xssFilterInit() {
    FilterRegistrationBean registration = new FilterRegistrationBean();
    registration.setFilter(xssFilter());
    registration.addUrlPatterns("/*");
    registration.addUrlPatterns("!/admin/v1/protocol/*");
    registration.setOrder(1);
    return registration;
  }

  /*
   * 创建一个bean
   * @return
   */
  @Bean(name = "replaceStreamFilter")
  public Filter replaceStreamFilter() {
    return new ReplaceStreamFilter();
  }

  @Bean
  public FilterRegistrationBean replaceStreamFilterInit() {
    FilterRegistrationBean registration = new FilterRegistrationBean();
    registration.setFilter(replaceStreamFilter());
    registration.addUrlPatterns("/api/*");
    registration.setOrder(0);
    return registration;
  }
}
