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

package cn.universal.websocket.protocol.config;

import cn.universal.core.protocol.ProtocolModuleRuntimeRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * WebSocket 协议模块自动配置类
 *
 * <p>当 websocket.protocol.enabled=true 时，自动扫描包并创建所有WebSocket相关的Bean
 *
 * <p>通过 META-INF/spring.factories 实现 Spring Boot 自动配置
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/13
 */
@Configuration
@ConditionalOnProperty(name = "websocket.protocol.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "cn.universal.websocket.protocol")
@EnableConfigurationProperties(WebSocketProperties.class)
@EnableWebSocket
@Slf4j
public class WebSocketAutoConfiguration {

    @Autowired(required = false)
    private WebSocketModuleInfo moduleInfo;

    public WebSocketAutoConfiguration() {
        log.info("[WebSocket协议] WebSocket协议模块自动配置已启用");
    }

    @PostConstruct
    public void registerProtocol() {
        if (moduleInfo != null) {
            ProtocolModuleRuntimeRegistry.registerProtocol(moduleInfo);
            log.info("[WebSocket自动配置] 协议模块已注册到运行时注册表");
        }
    }

    @PreDestroy
    public void unregisterProtocol() {
        if (moduleInfo != null) {
            ProtocolModuleRuntimeRegistry.unregisterProtocol(moduleInfo.getCode());
            log.info("[WebSocket自动配置] 协议模块已从运行时注册表注销");
        }
    }
}
