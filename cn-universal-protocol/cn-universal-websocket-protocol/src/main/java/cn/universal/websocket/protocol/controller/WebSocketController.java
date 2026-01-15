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

package cn.universal.websocket.protocol.controller;

import cn.universal.websocket.protocol.entity.WebSocketSession;
import cn.universal.websocket.protocol.service.WebSocketSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 管理控制器
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/13
 */
@Slf4j
@RestController
@RequestMapping("/api/websocket")
@Tag(name = "WebSocket管理", description = "WebSocket连接和消息管理接口")
public class WebSocketController {

    @Autowired
    private WebSocketSessionManager sessionManager;

    /**
     * 获取在线会话统计
     */
    @GetMapping("/stats")
    @Operation(summary = "获取统计信息", description = "获取WebSocket连接统计信息")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("onlineCount", sessionManager.getOnlineCount());
        stats.put("authenticatedCount", sessionManager.getAuthenticatedCount());
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/session/{sessionId}")
    @Operation(summary = "获取会话详情", description = "根据会话ID获取会话详细信息")
    public WebSocketSession getSession(@PathVariable String sessionId) {
        return sessionManager.getSession(sessionId);
    }

    /**
     * 根据设备ID获取会话
     */
    @GetMapping("/session/device/{deviceId}")
    @Operation(summary = "根据设备ID获取会话", description = "根据设备ID获取对应的会话信息")
    public WebSocketSession getSessionByDevice(@PathVariable String deviceId) {
        return sessionManager.getSessionByDeviceId(deviceId);
    }

    /**
     * 检查设备是否在线
     */
    @GetMapping("/device/{deviceId}/online")
    @Operation(summary = "检查设备在线状态", description = "检查指定设备是否在线")
    public Map<String, Object> isDeviceOnline(@PathVariable String deviceId) {
        Map<String, Object> result = new HashMap<>();
        result.put("deviceId", deviceId);
        result.put("online", sessionManager.isDeviceOnline(deviceId));
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 向指定设备发送消息
     */
    @PostMapping("/send/{deviceId}")
    @Operation(summary = "发送消息到设备", description = "向指定设备发送消息")
    public Map<String, Object> sendMessage(@PathVariable String deviceId, 
                                            @RequestBody Map<String, String> request) {
        String message = request.get("message");
        boolean success = sessionManager.sendMessageToDevice(deviceId, message);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("deviceId", deviceId);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 广播消息到所有在线设备
     */
    @PostMapping("/broadcast")
    @Operation(summary = "广播消息", description = "向所有在线设备广播消息")
    public Map<String, Object> broadcast(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        int count = sessionManager.broadcast(message);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("sentCount", count);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 关闭指定会话
     */
    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "关闭会话", description = "强制关闭指定的WebSocket会话")
    public Map<String, Object> closeSession(@PathVariable String sessionId) {
        sessionManager.closeSession(sessionId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("sessionId", sessionId);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
