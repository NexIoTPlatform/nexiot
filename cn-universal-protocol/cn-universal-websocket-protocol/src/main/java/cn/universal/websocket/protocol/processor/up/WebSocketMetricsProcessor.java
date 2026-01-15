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

package cn.universal.websocket.protocol.processor.up;

import org.springframework.stereotype.Component;

import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 指标处理器
 *
 * <p>负责收集 WebSocket 消息的处理指标，对标 MQTT 的 MqttMetricsUPProcessor
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/15
 */
@Slf4j
@Component
public class WebSocketMetricsProcessor implements WebSocketUPProcessor {

    private volatile long totalMessages = 0;
    private volatile long successMessages = 0;
    private volatile long errorMessages = 0;

    @Override
    public String getName() {
        return "WebSocket指标处理器";
    }

    @Override
    public String getDescription() {
        return "收集 WebSocket 消息处理指标";
    }

    @Override
    public int getOrder() {
        return 900; // 最后执行
    }

    @Override
    public int getPriority() {
        return 0; // 默认优先级
    }

    @Override
    public boolean preCheck(WebSocketUPRequest request) {
        return request != null;
    }

    @Override
    public boolean supports(WebSocketUPRequest request) {
        return true; // 总是支持
    }

    @Override
    public void onError(WebSocketUPRequest request, Exception e) {
        errorMessages++;
        log.error("[{}] 指标处理异常，SessionID: {}, 异常: {}",
                getName(), request.getSessionId(), e.getMessage());
    }

    @Override
    public ProcessorResult process(WebSocketUPRequest request) {
        totalMessages++;

        try {
            // 记录消息处理耗时
            if (request.getTimestamp() != null) {
                long duration = System.currentTimeMillis() - request.getTimestamp();
                log.debug(
                        "[WebSocket指标] 消息处理耗时: {}ms, sessionId: {}, category: {}",
                        duration,
                        request.getSessionId(),
                        request.getContextValue("category"));
            }

            return ProcessorResult.CONTINUE;

        } catch (Exception e) {
            log.error("[WebSocket指标] 指标收集异常: ", e);
            return ProcessorResult.CONTINUE; // 指标异常不影响消息处理
        }
    }

    @Override
    public void postProcess(WebSocketUPRequest request, ProcessorResult result) {
        // 在处理链的最后记录统计信息
        if (result == ProcessorResult.CONTINUE || result == ProcessorResult.STOP) {
            successMessages++;
            log.debug("[{}] 消息处理成功，总数: {}, 成功: {}, 失败: {}",
                    getName(), totalMessages, successMessages, errorMessages);
        } else if (result == ProcessorResult.ERROR) {
            errorMessages++;
            log.warn("[{}] 消息处理失败，总数: {}, 成功: {}, 失败: {}",
                    getName(), totalMessages, successMessages, errorMessages);
        }
    }

    /**
     * 获取总消息数
     */
    public long getTotalMessages() {
        return totalMessages;
    }

    /**
     * 获取成功消息数
     */
    public long getSuccessMessages() {
        return successMessages;
    }

    /**
     * 获取错误消息数
     */
    public long getErrorMessages() {
        return errorMessages;
    }

    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        if (totalMessages == 0) {
            return 0.0;
        }
        return (double) successMessages / totalMessages * 100;
    }

    /**
     * 重置指标
     */
    public void reset() {
        totalMessages = 0;
        successMessages = 0;
        errorMessages = 0;
    }
}
