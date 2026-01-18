/*
 *
 * Copyright (c) 2026, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.websocket.protocol.processor.up.common;

import java.util.List;

import org.springframework.stereotype.Component;

import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import cn.universal.websocket.protocol.processor.up.WebSocketUPProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 消息回复处理器 - 对标 MQTT 的消息回复机制
 * 
 * 职责：如果消息需要回复（如 RPC 调用、设置属性等），则生成回复消息并通过 WebSocket 下行通道发送
 * WebSocket 不同于 MQTT，可以通过相同的连接双向通信，回复消息可直接发送到请求端
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/16
 */
@Slf4j(topic = "websocket")
@Component
public class WebSocketProtocolReplyProcessor implements WebSocketUPProcessor {

    @Override
    public String getName() {
        return "WebSocket消息回复处理器";
    }

    @Override
    public String getDescription() {
        return "处理WebSocket消息并生成对应的回复消息";
    }

    @Override
    public int getOrder() {
        return 900; // 在缓存处理后执行
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean preCheck(WebSocketUPRequest request) {
        return request != null && request.getUpRequestList() != null && !request.getUpRequestList().isEmpty();
    }

    @Override
    public boolean supports(WebSocketUPRequest request) {
        // 所有消息都检查是否需要回复
        return request.getUpRequestList() != null && !request.getUpRequestList().isEmpty();
    }

    @Override
    public ProcessorResult process(WebSocketUPRequest request) {
        try {
            log.debug("[{}] 开始消息回复处理，SessionID: {}", getName(), request.getSessionId());

            List<BaseUPRequest> upRequestList = request.getUpRequestList();
            if (upRequestList == null || upRequestList.isEmpty()) {
                return ProcessorResult.CONTINUE;
            }

            // 遍历消息，检查是否需要生成回复
            for (BaseUPRequest upRequest : upRequestList) {
                handleMessageReply(request, upRequest);
            }

            log.debug("[{}] 消息回复处理完成，SessionID: {}", getName(), request.getSessionId());
            return ProcessorResult.CONTINUE;

        } catch (Exception e) {
            log.error("[{}] 消息回复处理异常，SessionID: {}", getName(), request.getSessionId(), e);
            return ProcessorResult.CONTINUE; // 回复生成失败不影响消息流程
        }
    }

    /**
     * 处理消息回复逻辑
     */
    private void handleMessageReply(WebSocketUPRequest request, BaseUPRequest upRequest) {
        // 判断消息是否需要回复
        String messageType = upRequest.getMessageType() != null ? upRequest.getMessageType().toString() : "PROPERTIES";
        
        log.debug("[{}] 检查消息回复需求 - iotId: {}, messageType: {}",
            getName(), upRequest.getIotId(), messageType);

        // 根据消息类型决定是否需要生成回复
        // - PROPERTIES 消息通常不需要回复（属性上报）
        // - EVENT 消息通常不需要回复（事件上报）
        // - 其他消息类型可能需要回复（由上游处理器生成回复逻辑）
        
        // 此处为扩展点，实际回复逻辑可由其他组件处理
        // 例如：websocketDownService.sendReplyIfNeeded(request.getSessionId(), upRequest)
    }

    @Override
    public void postProcess(WebSocketUPRequest request, ProcessorResult result) {
        if (result == ProcessorResult.CONTINUE) {
            log.debug("[{}] 消息回复处理后置处理完成，SessionID: {}", getName(), request.getSessionId());
        }
    }

    @Override
    public void onError(WebSocketUPRequest request, Exception e) {
        log.error("[{}] 消息回复处理异常，SessionID: {}", getName(), request.getSessionId(), e);
    }
}
