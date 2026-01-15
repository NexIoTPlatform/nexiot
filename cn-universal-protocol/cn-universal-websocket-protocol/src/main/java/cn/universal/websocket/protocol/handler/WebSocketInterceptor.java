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

package cn.universal.websocket.protocol.handler;

import cn.universal.websocket.protocol.config.WebSocketProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/13
 */
@Slf4j
@Component
public class WebSocketInterceptor implements HandshakeInterceptor {

    @Autowired
    private WebSocketProperties properties;

    /**
     * 握手前处理
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, 
                                     ServerHttpResponse response,
                                     WebSocketHandler wsHandler, 
                                     Map<String, Object> attributes) throws Exception {
        
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            String clientIp = getClientIp(servletRequest);
            String userAgent = servletRequest.getHeaders().getFirst("User-Agent");
            
            log.info("[WebSocket握手][开始] ClientIP: {}, UserAgent: {}, URI: {}", 
                    clientIp, userAgent, request.getURI());

            // 提取查询参数（用于设备认证）
            String query = request.getURI().getQuery();
            if (query != null) {
                // 解析查询参数，如 deviceId, productKey, deviceSecret
                Map<String, String> params = parseQueryParams(query);
                attributes.putAll(params);
                log.debug("[WebSocket握手][参数] {}", params);
            }

            // 存储客户端信息
            attributes.put("clientIp", clientIp);
            attributes.put("userAgent", userAgent);
            attributes.put("handshakeTime", System.currentTimeMillis());

            // 如果需要认证，可以在这里进行预验证
            if (properties.getAuthRequired()) {
                String deviceId = (String) attributes.get("deviceId");
                if (deviceId == null || deviceId.isEmpty()) {
                    log.warn("[WebSocket握手][拒绝] 缺少设备ID参数");
                    return false;
                }
            }

            return true;
        }
        
        return false;
    }

    /**
     * 握手后处理
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, 
                                ServerHttpResponse response,
                                WebSocketHandler wsHandler, 
                                Exception exception) {
        if (exception != null) {
            log.error("[WebSocket握手][异常] URI: {}", request.getURI(), exception);
        } else {
            log.info("[WebSocket握手][完成] URI: {}", request.getURI());
        }
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(ServletServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddress().getAddress().getHostAddress();
        }
        // 处理多个代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 解析查询参数
     */
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new java.util.HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0 && idx < pair.length() - 1) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                params.put(key, value);
            }
        }
        return params;
    }
}
