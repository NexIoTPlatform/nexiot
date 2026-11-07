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

package cn.universal.web.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger/OpenAPI 3 配置 */
@Configuration
@EnableKnife4j
@ConditionalOnProperty(name = "swagger.enable", havingValue = "true", matchIfMissing = false)
public class SwaggerConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    // 全局安全配置
    SecurityScheme securityScheme =
        new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .in(SecurityScheme.In.HEADER)
            .name("Authorization");

    return new OpenAPI()
        .info(
            new Info()
                .title("奈科斯（NexIoT)")
                .description(
                    "以 “底层平台筑基 + 应用伙伴共创” 为核心模式，专注解决物联网硬件异构、软件兼容的接入难题。与开发者、企业携手打造可落地、可复用的物联网解决方案生态。")
                .version("1")
                .version("1")
                .contact(
                    new Contact()
                        .name("nexiot")
                        .email("nexiot@qq.com")
                        .url("https://docs.nexiot.cc")))
        .addSecurityItem(new SecurityRequirement().addList("Authorization"))
        .components(new Components().addSecuritySchemes("Authorization", securityScheme));
  }
}
