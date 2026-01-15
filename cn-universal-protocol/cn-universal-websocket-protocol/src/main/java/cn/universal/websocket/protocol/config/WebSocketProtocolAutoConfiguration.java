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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * WebSocket 协议自动配置类
 *
 * <p>Spring Boot自动配置入口，负责扫描和注册WebSocket模块的所有组件
 * 注册WebSocket协议到运行时协议注册表
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/14
 */
@Slf4j
@Configuration
@ComponentScan(basePackages = "cn.universal.websocket.protocol")
public class WebSocketProtocolAutoConfiguration {

    @Autowired
    private WebSocketModuleInfo moduleInfo;

    public WebSocketProtocolAutoConfiguration() {
        log.info("[CORE_WEBSOCKET] WebSocket核心模块自动配置已启用");
    }

    /**
     * 在Bean初始化后注册WebSocket协议
     */
    @PostConstruct
    public void registerProtocol() {
        if (moduleInfo != null) {
            ProtocolModuleRuntimeRegistry.registerProtocol(moduleInfo);
            log.info("[WebSocket自动配置] 核心协议模块已注册到运行时注册表, Code: {}, Name: {}", 
                    moduleInfo.getCode(), moduleInfo.getName());
        }
    }

    /**
     * 在Bean销毁前注销WebSocket协议
     */
    @PreDestroy
    public void unregisterProtocol() {
        if (moduleInfo != null) {
            ProtocolModuleRuntimeRegistry.unregisterProtocol(moduleInfo.getCode());
            log.info("[WebSocket自动配置] 核心协议模块已从运行时注册表注销, Code: {}", 
                    moduleInfo.getCode());
        }
    }
}
