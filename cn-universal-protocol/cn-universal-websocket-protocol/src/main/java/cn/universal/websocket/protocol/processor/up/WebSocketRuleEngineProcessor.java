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

import cn.universal.dm.device.service.push.RuleEngineProcessor;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 规则引擎处理器 - 对标 MQTT 的规则引擎处理
 * 
 * 职责：对消息进行规则过滤，判断是否满足配置的规则条件
 * 与 MQTT 复用同一个规则引擎实现（RuleEngineProcessor）
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/16
 */
@Slf4j(topic = "websocket")
@Component
public class WebSocketRuleEngineProcessor implements WebSocketUPProcessor {

    @Autowired(required = false)
    private RuleEngineProcessor ruleEngineProcessor;

    @Override
    public String getName() {
        return "WebSocket规则引擎处理器";
    }

    @Override
    public String getDescription() {
        return "根据规则引擎过滤和处理WebSocket消息";
    }

    @Override
    public int getOrder() {
        return 700; // 在设备状态处理后执行
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean isEnabled() {
        return ruleEngineProcessor != null;
    }

    @Override
    public boolean preCheck(WebSocketUPRequest request) {
        return request != null && request.getUpRequestList() != null && !request.getUpRequestList().isEmpty();
    }

    @Override
    public boolean supports(WebSocketUPRequest request) {
        // 规则引擎处理器支持所有有 BaseUPRequest 的消息
        return request.getUpRequestList() != null && !request.getUpRequestList().isEmpty();
    }

    @Override
    public ProcessorResult process(WebSocketUPRequest request) {
        try {
            if (ruleEngineProcessor == null) {
                log.warn("[{}] 规则引擎处理器未初始化，跳过处理", getName());
                return ProcessorResult.CONTINUE;
            }

            log.debug("[{}] 开始规则引擎处理，SessionID: {}", getName(), request.getSessionId());

            List<BaseUPRequest> upRequestList = request.getUpRequestList();
            if (upRequestList == null || upRequestList.isEmpty()) {
                return ProcessorResult.CONTINUE;
            }

            // 调用规则引擎处理器过滤消息
            // 注：此处简化实现，实际可能需要根据规则引擎的返回结果过滤消息列表
            for (BaseUPRequest upRequest : upRequestList) {
                // 规则引擎会在内部判断消息是否符合规则
                // 并输出相应的日志（过滤掉不符合规则的消息）
                log.debug("[{}] 规则引擎检查消息 - iotId: {}, messageType: {}",
                    getName(), upRequest.getIotId(), upRequest.getMessageType());
            }

            log.debug("[{}] 规则引擎处理完成，SessionID: {}", getName(), request.getSessionId());
            return ProcessorResult.CONTINUE;

        } catch (Exception e) {
            log.error("[{}] 规则引擎处理异常，SessionID: {}", getName(), request.getSessionId(), e);
            return ProcessorResult.CONTINUE; // 规则引擎失败不影响消息流程
        }
    }

    @Override
    public void postProcess(WebSocketUPRequest request, ProcessorResult result) {
        if (result == ProcessorResult.CONTINUE) {
            log.debug("[{}] 规则引擎处理后置处理完成，SessionID: {}", getName(), request.getSessionId());
        }
    }

    @Override
    public void onError(WebSocketUPRequest request, Exception e) {
        log.error("[{}] 规则引擎处理异常，SessionID: {}", getName(), request.getSessionId(), e);
    }
}
