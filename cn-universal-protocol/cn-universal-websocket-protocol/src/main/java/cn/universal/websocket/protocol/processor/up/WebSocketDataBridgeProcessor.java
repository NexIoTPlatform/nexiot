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

package cn.universal.websocket.protocol.processor.up;

import java.util.List;

import org.springframework.stereotype.Component;

import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 消息发布处理器 - 对标 MQTT 的消息发布功能
 * 
 * 职责：将处理完成的 WebSocket 消息发布到系统，由消息推送适配器处理最终的推送
 * 与 MQTT 的消息推送流程统一，通过 BaseUPRequest 的发布机制处理
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/16
 */
@Slf4j(topic = "websocket")
@Component
public class WebSocketDataBridgeProcessor implements WebSocketUPProcessor {

    @Override
    public String getName() {
        return "WebSocket消息发布处理器";
    }

    @Override
    public String getDescription() {
        return "发布处理完成的WebSocket消息";
    }

    @Override
    public int getOrder() {
        return 1000; // 在回复处理后执行（最后一个业务处理器）
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
        // 数据桥接处理器支持所有有 BaseUPRequest 的消息
        return request.getUpRequestList() != null && !request.getUpRequestList().isEmpty();
    }

    @Override
    public ProcessorResult process(WebSocketUPRequest request) {
        try {
            log.debug("[{}] 开始消息发布处理，SessionID: {}", getName(), request.getSessionId());

            List<BaseUPRequest> upRequestList = request.getUpRequestList();
            if (upRequestList == null || upRequestList.isEmpty()) {
                return ProcessorResult.CONTINUE;
            }

            // 在此处可以进行最终的消息发布操作
            // 实际的推送由消息推送适配器（IoTUPPushAdapter）在Web模块处理
            for (BaseUPRequest upRequest : upRequestList) {
                handleMessagePublish(upRequest);
            }

            log.debug("[{}] 消息发布处理完成，总消息数: {}, SessionID: {}",
                getName(), upRequestList.size(), request.getSessionId());
            return ProcessorResult.CONTINUE;

        } catch (Exception e) {
            log.error("[{}] 消息发布处理异常，SessionID: {}", getName(), request.getSessionId(), e);
            return ProcessorResult.CONTINUE; // 发布失败不影响消息流程
        }
    }

    /**
     * 处理单条消息的发布
     */
    private void handleMessagePublish(BaseUPRequest upRequest) {
        try {
            if (upRequest == null || upRequest.getIotId() == null) {
                return;
            }

            log.debug("[{}] 处理消息发布 - iotId: {}, productKey: {}, messageType: {}",
                getName(), upRequest.getIotId(), upRequest.getProductKey(), upRequest.getMessageType());

            // 消息发布的实际处理由消息推送适配器完成
            // 此处仅做标记日志
            
        } catch (Exception e) {
            log.error("[{}] 处理消息发布异常 - iotId: {}", getName(), upRequest.getIotId(), e);
        }
    }

    @Override
    public void postProcess(WebSocketUPRequest request, ProcessorResult result) {
        if (result == ProcessorResult.CONTINUE) {
            log.debug("[{}] 数据桥接处理后置处理完成，SessionID: {}", getName(), request.getSessionId());
        }
    }

    @Override
    public void onError(WebSocketUPRequest request, Exception e) {
        log.error("[{}] 数据桥接处理异常，SessionID: {}", getName(), request.getSessionId(), e);
    }
}
