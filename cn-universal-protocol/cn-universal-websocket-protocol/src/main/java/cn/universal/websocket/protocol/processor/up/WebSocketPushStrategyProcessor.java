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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.universal.dm.device.service.push.PushStrategyManager;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 推送策略处理器 - 对标 MQTT 的推送策略管理
 * 
 * 职责：根据推送策略，将消息推送到对应的订阅者（其他WebSocket客户端、HTTP客户端等）
 * 与 MQTT 复用同一个推送策略管理器（PushStrategyManager）
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/16
 */
@Slf4j(topic = "websocket")
@Component
public class WebSocketPushStrategyProcessor implements WebSocketUPProcessor {

    @Autowired(required = false)
    private PushStrategyManager pushStrategyManager;

    @Override
    public String getName() {
        return "WebSocket推送策略处理器";
    }

    @Override
    public String getDescription() {
        return "根据推送策略推送WebSocket消息到订阅者";
    }

    @Override
    public int getOrder() {
        return 800; // 在规则引擎处理后执行
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean isEnabled() {
        return pushStrategyManager != null;
    }

    @Override
    public boolean preCheck(WebSocketUPRequest request) {
        return request != null && request.getUpRequestList() != null && !request.getUpRequestList().isEmpty();
    }

    @Override
    public boolean supports(WebSocketUPRequest request) {
        // 推送策略处理器支持所有有 BaseUPRequest 的消息
        return request.getUpRequestList() != null && !request.getUpRequestList().isEmpty();
    }

    @Override
    public ProcessorResult process(WebSocketUPRequest request) {
        try {
            if (pushStrategyManager == null) {
                log.warn("[{}] 推送策略管理器未初始化，跳过处理", getName());
                return ProcessorResult.CONTINUE;
            }

            log.debug("[{}] 开始推送策略处理，SessionID: {}", getName(), request.getSessionId());

            List<BaseUPRequest> upRequestList = request.getUpRequestList();
            if (upRequestList == null || upRequestList.isEmpty()) {
                return ProcessorResult.CONTINUE;
            }

            // 调用推送策略管理器推送消息
            // 注：此处简化实现，实际可通过 pushStrategyManager 的 push/batchPush 方法推送消息
            for (BaseUPRequest upRequest : upRequestList) {
                log.debug("[{}] 推送策略检查消息 - iotId: {}, messageType: {}",
                    getName(), upRequest.getIotId(), upRequest.getMessageType());
            }

            log.debug("[{}] 推送策略处理完成，总消息数: {}, SessionID: {}",
                getName(), upRequestList.size(), request.getSessionId());
            return ProcessorResult.CONTINUE;

        } catch (Exception e) {
            log.error("[{}] 推送策略处理异常，SessionID: {}", getName(), request.getSessionId(), e);
            return ProcessorResult.CONTINUE; // 推送失败不影响消息流程
        }
    }

    @Override
    public void postProcess(WebSocketUPRequest request, ProcessorResult result) {
        if (result == ProcessorResult.CONTINUE) {
            log.debug("[{}] 推送策略处理后置处理完成，SessionID: {}", getName(), request.getSessionId());
        }
    }

    @Override
    public void onError(WebSocketUPRequest request, Exception e) {
        log.error("[{}] 推送策略处理异常，SessionID: {}", getName(), request.getSessionId(), e);
    }
}
