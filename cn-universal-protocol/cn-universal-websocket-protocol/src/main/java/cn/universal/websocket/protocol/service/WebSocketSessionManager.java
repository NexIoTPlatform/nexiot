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

package cn.universal.websocket.protocol.service;

import cn.universal.websocket.protocol.config.WebSocketConstant;
import cn.universal.websocket.protocol.entity.WebSocketSession;
import cn.universal.websocket.protocol.enums.WebSocketStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理服务
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/13
 */
@Slf4j
@Service
public class WebSocketSessionManager {

    /** 会话存储 sessionId -> WebSocketSession */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /** 设备ID索引 deviceId -> sessionId */
    private final Map<String, String> deviceIndex = new ConcurrentHashMap<>();

    /** Spring WebSocket 会话 sessionId -> WebSocketSession */
    private final Map<String, org.springframework.web.socket.WebSocketSession> springWebSocketSessions = new ConcurrentHashMap<>();

    /**
     * 添加会话
     */
    public void addSession(org.springframework.web.socket.WebSocketSession springSession) {
        String sessionId = springSession.getId();
        
        WebSocketSession session = WebSocketSession.builder()
                .sessionId(sessionId)
                .status(WebSocketStatus.CONNECTED)
                .authenticated(false)
                .remoteAddress(springSession.getRemoteAddress() != null ? 
                        springSession.getRemoteAddress().toString() : "unknown")
                .connectTime(LocalDateTime.now())
                .lastActiveTime(LocalDateTime.now())
                .receivedMessageCount(0L)
                .sentMessageCount(0L)
                .attributes(new HashMap<>())
                .build();

        sessions.put(sessionId, session);
        springWebSocketSessions.put(sessionId, springSession);
        
        log.info("[WebSocket会话管理] 添加会话: sessionId={}, remoteAddress={}", 
                sessionId, session.getRemoteAddress());
    }

    /**
     * 移除会话
     */
    public void removeSession(String sessionId) {
        WebSocketSession session = sessions.remove(sessionId);
        springWebSocketSessions.remove(sessionId);
        
        if (session != null && session.getDeviceId() != null) {
            deviceIndex.remove(session.getDeviceId());
            log.info("[WebSocket会话管理] 移除会话: sessionId={}, deviceId={}", 
                    sessionId, session.getDeviceId());
        }
    }

    /**
     * 绑定设备
     */
    public void bindDevice(String sessionId, String deviceId, String productKey) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null) {
            session.setDeviceId(deviceId);
            session.setProductKey(productKey);
            session.setAuthenticated(true);
            session.setStatus(WebSocketStatus.AUTHENTICATED);
            deviceIndex.put(deviceId, sessionId);
            
            log.info("[WebSocket会话管理] 绑定设备: sessionId={}, deviceId={}, productKey={}", 
                    sessionId, deviceId, productKey);
        }
    }

    /**
     * 根据会话ID获取会话
     */
    public WebSocketSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 根据设备ID获取会话
     */
    public WebSocketSession getSessionByDeviceId(String deviceId) {
        String sessionId = deviceIndex.get(deviceId);
        return sessionId != null ? sessions.get(sessionId) : null;
    }

    /**
     * 获取 Spring WebSocket 会话
     */
    public org.springframework.web.socket.WebSocketSession getSpringSession(String sessionId) {
        return springWebSocketSessions.get(sessionId);
    }

    /**
     * 获取所有会话
     */
    public List<WebSocketSession> getAllSessions() {
        return new ArrayList<>(sessions.values());
    }

    /**
     * 获取在线会话数
     */
    public int getOnlineCount() {
        return sessions.size();
    }

    /**
     * 获取已认证会话数
     */
    public long getAuthenticatedCount() {
        return sessions.values().stream()
                .filter(WebSocketSession::getAuthenticated)
                .count();
    }

    /**
     * 更新会话活跃时间
     */
    public void updateLastActiveTime(String sessionId) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null) {
            session.updateLastActiveTime();
        }
    }

    /**
     * 发送消息到指定会话
     */
    public boolean sendMessage(String sessionId, String message) {
        org.springframework.web.socket.WebSocketSession springSession = springWebSocketSessions.get(sessionId);
        if (springSession != null && springSession.isOpen()) {
            try {
                springSession.sendMessage(new TextMessage(message));
                
                WebSocketSession session = sessions.get(sessionId);
                if (session != null) {
                    session.incrementSentCount();
                }
                
                return true;
            } catch (Exception e) {
                log.error("[WebSocket会话管理] 发送消息失败: sessionId={}", sessionId, e);
                return false;
            }
        }
        return false;
    }

    /**
     * 发送消息到指定设备
     */
    public boolean sendMessageToDevice(String deviceId, String message) {
        String sessionId = deviceIndex.get(deviceId);
        if (sessionId != null) {
            return sendMessage(sessionId, message);
        }
        log.warn("[WebSocket会话管理] 设备不在线，无法发送消息: deviceId={}", deviceId);
        return false;
    }

    /**
     * 广播消息到所有在线会话
     * @return 发送成功的会话数量
     */
    public int broadcast(String message) {
        int count = 0;
        for (Map.Entry<String, org.springframework.web.socket.WebSocketSession> entry : springWebSocketSessions.entrySet()) {
            org.springframework.web.socket.WebSocketSession springSession = entry.getValue();
            if (springSession.isOpen()) {
                try {
                    springSession.sendMessage(new TextMessage(message));
                    count++;
                } catch (Exception e) {
                    log.error("[WebSocket会话管理] 广播消息失败: sessionId={}", entry.getKey(), e);
                }
            }
        }
        return count;
    }

    /**
     * 关闭会话
     */
    public void closeSession(String sessionId) {
        org.springframework.web.socket.WebSocketSession springSession = springWebSocketSessions.get(sessionId);
        if (springSession != null && springSession.isOpen()) {
            try {
                springSession.close();
                removeSession(sessionId);
            } catch (Exception e) {
                log.error("[WebSocket会话管理] 关闭会话失败: sessionId={}", sessionId, e);
            }
        }
    }

    /**
     * 检查设备是否在线
     */
    public boolean isDeviceOnline(String deviceId) {
        return deviceIndex.containsKey(deviceId);
    }
}
