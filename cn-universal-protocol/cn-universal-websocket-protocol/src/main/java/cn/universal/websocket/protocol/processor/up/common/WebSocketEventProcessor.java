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

package cn.universal.websocket.protocol.processor.up.common;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.universal.websocket.protocol.entity.WebSocketSession;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import cn.universal.websocket.protocol.processor.up.WebSocketUPProcessor;
import cn.universal.websocket.protocol.service.WebSocketSessionManager;
import lombok.extern.slf4j.Slf4j;

/**
 * 事件生成处理器 - 生成和记录事件
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/13
 */
@Slf4j
@Component
public class WebSocketEventProcessor implements WebSocketUPProcessor {

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Override
    public String getName() {
        return "WebSocket事件处理器";
    }

    @Override
    public String getDescription() {
        return "生成和记录WebSocket消息事件";
    }

    @Override
    public ProcessorResult process(WebSocketUPRequest request) {
        WebSocketSession session = sessionManager.getSession(request.getSessionId());
        if (session == null) {
            return ProcessorResult.SKIP;
        }

        // 更新会话统计
        session.setReceivedMessageCount(session.getReceivedMessageCount() + 1);
        session.setLastActiveTime(LocalDateTime.now());

        // 记录消息事件
        log.debug("[WebSocket事件][消息] SessionID: {}, DeviceID: {}, MessageType: {}, Size: {}", 
                request.getSessionId(), 
                session.getDeviceId(), 
                request.getWsMessageType(),
                request.getPayload() != null ? request.getPayload().length() : 0);

        // TODO: 可以在这里发送事件到事件总线或消息队列
        // eventPublisher.publishEvent(new WebSocketMessageEvent(request));
        
        return ProcessorResult.CONTINUE;
    }

    @Override
    public boolean supports(WebSocketUPRequest request) {
        return true; // 所有消息都记录事件
    }

    @Override
    public int getOrder() {
        return 900; // 最后执行事件记录
    }

    @Override
    public int getPriority() {
        return 0; // 默认优先级
    }

    @Override
    public boolean preCheck(WebSocketUPRequest request) {
        // 事件记录对所有请求生效
        return true;
    }

    @Override
    public void postProcess(WebSocketUPRequest request, ProcessorResult result) {
        // 记录处理结果
        log.debug("[WebSocket事件][完成] SessionID: {}, 处理结果: {}",
                request.getSessionId(), result);
    }

    @Override
    public void onError(WebSocketUPRequest request, Exception e) {
        log.error("[WebSocket事件][异常] SessionID: {}, 异常: {}",
                request.getSessionId(), e.getMessage());
    }
}
