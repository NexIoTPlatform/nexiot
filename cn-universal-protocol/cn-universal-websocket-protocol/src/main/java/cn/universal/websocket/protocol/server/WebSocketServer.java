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

package cn.universal.websocket.protocol.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import cn.universal.websocket.protocol.config.WebSocketProperties;
import cn.universal.websocket.protocol.handler.WebSocketHandler;
import cn.universal.websocket.protocol.handler.WebSocketInterceptor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 服务器配置
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/13
 */
@Slf4j
@Configuration
public class WebSocketServer implements WebSocketConfigurer {

    @Autowired
    private WebSocketProperties properties;

    @Autowired
    private WebSocketHandler webSocketHandler;

    @Autowired
    private WebSocketInterceptor webSocketInterceptor;

    /**
     * 注册 WebSocket 处理器
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("[WebSocket服务器][注册] ========================================");
        log.info("[WebSocket服务器][注册] 开始注册WebSocket处理器");
        log.info("[WebSocket服务器][注册] 路径: {}", properties.getPath());
        log.info("[WebSocket服务器][注册] 端口: {}", properties.getPort());
        log.info("[WebSocket服务器][注册] 允许跨域: {}", properties.getAllowOrigins());
        log.info("[WebSocket服务器][注册] 认证要求: {}", properties.getAuthRequired());

        registry.addHandler(webSocketHandler, properties.getPath())
                .addInterceptors(webSocketInterceptor)
                .setAllowedOrigins(properties.getAllowOrigins() ? "*" : "")
                .withSockJS(); // 启用 SockJS 支持（用于不支持 WebSocket 的浏览器）

        log.info("[WebSocket服务器][注册] WebSocket处理器注册完成");
        log.info("[WebSocket服务器][启动] 服务器已启动，可以连接");
        log.info("[WebSocket服务器][启动] 端点: ws://{}:{}{}", 
                getHostAddress(), properties.getPort(), properties.getPath());
        log.info("[WebSocket服务器][注册] ========================================");
    }

    /**
     * 获取服务器地址
     */
    private String getHostAddress() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }
}
