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

package cn.universal.websocket.protocol.route;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import cn.hutool.core.util.StrUtil;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest.MessageCategory;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 路由管理器
 *
 * <p>对标 MQTT 的 MQTTTopicManager，负责解析 WebSocket 消息的路由信息
 * 包括消息分类识别、productKey 提取等
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/15
 */
@Slf4j(topic = "websocket")
@Component
public class WebSocketRouteManager {

    /** 路由模式缓存 */
    private static final Map<String, MessageCategory> ROUTE_PATTERN_CACHE = new ConcurrentHashMap<>();

    /** 初始化路由模式 */
    static {
        // 认证路由
        ROUTE_PATTERN_CACHE.put("auth", MessageCategory.AUTH);
        ROUTE_PATTERN_CACHE.put("authenticate", MessageCategory.AUTH);
        ROUTE_PATTERN_CACHE.put("login", MessageCategory.AUTH);

        // 事件路由
        ROUTE_PATTERN_CACHE.put("event", MessageCategory.EVENT);
        ROUTE_PATTERN_CACHE.put("events", MessageCategory.EVENT);

        // 数据路由
        ROUTE_PATTERN_CACHE.put("data", MessageCategory.DATA);
        ROUTE_PATTERN_CACHE.put("message", MessageCategory.DATA);
        ROUTE_PATTERN_CACHE.put("device/data", MessageCategory.DATA);

        // 心跳路由
        ROUTE_PATTERN_CACHE.put("ping", MessageCategory.PING);
        ROUTE_PATTERN_CACHE.put("heartbeat", MessageCategory.PING);

        // 订阅路由
        ROUTE_PATTERN_CACHE.put("subscribe", MessageCategory.SUBSCRIBE);
    }

    /**
     * 解析消息分类
     *
     * @param path WebSocket 路径
     * @return 消息分类，未匹配返回 OTHER
     */
    public MessageCategory parseCategory(String path) {
        if (StrUtil.isBlank(path)) {
            return MessageCategory.OTHER;
        }

        // 移除路径前缀
        String normalizedPath = path.toLowerCase().replaceFirst("^/+", "").split("/")[0];

        // 从缓存查找
        MessageCategory category = ROUTE_PATTERN_CACHE.get(normalizedPath);
        if (category != null) {
            return category;
        }

        // 尝试正则匹配
        if (Pattern.matches(".*auth.*", normalizedPath)) {
            return MessageCategory.AUTH;
        }
        if (Pattern.matches(".*event.*", normalizedPath)) {
            return MessageCategory.EVENT;
        }
        if (Pattern.matches(".*data.*", normalizedPath)) {
            return MessageCategory.DATA;
        }
        if (Pattern.matches(".*ping.*|.*heartbeat.*", normalizedPath)) {
            return MessageCategory.PING;
        }

        return MessageCategory.OTHER;
    }

    /**
     * 从 JSON 消息中提取 productKey
     *
     * @param jsonPayload JSON 载荷
     * @return productKey，未找到返回 null
     */
    public String extractProductKeyFromJson(String jsonPayload) {
        if (StrUtil.isBlank(jsonPayload)) {
            return null;
        }

        try {
            // 尝试从 JSON 中提取 productKey
            if (jsonPayload.contains("\"productKey\"")) {
                int start = jsonPayload.indexOf("\"productKey\"") + 13;
                int end = jsonPayload.indexOf("\"", start);
                if (end > start) {
                    return jsonPayload.substring(start, end);
                }
            }

            // 尝试提取 productId
            if (jsonPayload.contains("\"productId\"")) {
                int start = jsonPayload.indexOf("\"productId\"") + 11;
                int end = jsonPayload.indexOf("\"", start);
                if (end > start) {
                    return jsonPayload.substring(start, end);
                }
            }
        } catch (Exception e) {
            log.debug("[WebSocket路由] 提取 productKey 异常: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 从 JSON 消息中提取 deviceId
     *
     * @param jsonPayload JSON 载荷
     * @return deviceId，未找到返回 null
     */
    public String extractDeviceIdFromJson(String jsonPayload) {
        if (StrUtil.isBlank(jsonPayload)) {
            return null;
        }

        try {
            // 尝试从 JSON 中提取 deviceId
            if (jsonPayload.contains("\"deviceId\"")) {
                int start = jsonPayload.indexOf("\"deviceId\"") + 10;
                int end = jsonPayload.indexOf("\"", start);
                if (end > start) {
                    return jsonPayload.substring(start, end);
                }
            }

            // 尝试提取 iotId
            if (jsonPayload.contains("\"iotId\"")) {
                int start = jsonPayload.indexOf("\"iotId\"") + 7;
                int end = jsonPayload.indexOf("\"", start);
                if (end > start) {
                    return jsonPayload.substring(start, end);
                }
            }
        } catch (Exception e) {
            log.debug("[WebSocket路由] 提取 deviceId 异常: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 从路径中提取网络组件 ID
     *
     * @param path WebSocket 路径
     * @return 网络组件 ID，未找到返回 null
     */
    public Integer extractNetworkIdFromPath(String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }

        try {
            // 尝试从路径 /websocket/network/{networkId} 中提取
            String[] parts = path.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("network".equals(parts[i]) || "networkId".equals(parts[i])) {
                    try {
                        return Integer.parseInt(parts[i + 1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[WebSocket路由] 提取 networkId 异常: {}", e.getMessage());
        }

        return null;
    }
}
