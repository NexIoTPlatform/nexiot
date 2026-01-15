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

import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import cn.universal.common.util.WebInterfaceReadyChecker;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket协议启动监听器
 *
 * <p>在应用启动完成后进行WebSocket协议的初始化
 * 确保所有依赖组件都已就绪
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/14
 */
@Slf4j
@Component
public class WebSocketProtocolStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private WebSocketProperties properties;

    @Autowired
    private WebInterfaceReadyChecker webInterfaceReadyChecker;

    @Autowired(required = false)
    private ScheduledExecutorService scheduledExecutor;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 使用WebInterfaceReadyChecker，等待所有HTTP接口就绪
        // 这确保WebSocket处理器链和其他依赖都已完全初始化
        webInterfaceReadyChecker.executeAfterWebInterfaceReady(
                this::initializeWebSocketProtocol,
                scheduledExecutor);
    }

    /**
     * 初始化WebSocket协议
     */
    private void initializeWebSocketProtocol() {
        try {
            log.info("[WebSocket协议][启动] 开始初始化WebSocket协议");

            // 验证配置是否正确注入
            if (properties == null) {
                log.error("[WebSocket协议][启动] WebSocket配置未正确注入");
                return;
            }

            log.info("[WebSocket协议][配置] 路径: {}, 端口: {}, 允许跨域: {}, 认证: {}", 
                    properties.getPath(), 
                    properties.getPort(),
                    properties.getAllowOrigins(),
                    properties.getAuthRequired());

            // WebSocket服务器通过Spring的WebSocketConfigurer配置，
            // 已经在registerWebSocketHandlers中完成注册
            // 这里主要是验证配置和输出启动日志

            log.info("[WebSocket协议][启动完成] WebSocket协议已启动");
            log.info("[WebSocket协议][端点] ws://{}:{}{}", 
                    getHostname(), 
                    properties.getPort(), 
                    properties.getPath());

        } catch (Exception e) {
            log.error("[WebSocket协议][启动失败] WebSocket协议初始化异常: ", e);
        }
    }

    /**
     * 获取服务器主机名
     */
    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }
}
