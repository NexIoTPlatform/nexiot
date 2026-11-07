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

package cn.universal.web.config.intercepter;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 注册拦截器 */
@Configuration
public class WebAppConfig implements WebMvcConfigurer {

  @Resource private IPWhitelistInterceptor IPWhitelistInterceptor;

  @Resource private RequestSignatureInterceptor requestSignatureInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(requestSignatureInterceptor).addPathPatterns("/api/**", "/admin/**");
    registry.addInterceptor(new AuthContextIntercepter()).addPathPatterns("/api/**", "/admin/**");
    registry.addInterceptor(new APILogPrintIntercepter()).addPathPatterns("/api/**");
    registry
        .addInterceptor(IPWhitelistInterceptor)
        .addPathPatterns("/iot/**", "/api/**", "/debug/**", "/test/**", "/emqx/**", "/ct/aiot/**");
  }
}
