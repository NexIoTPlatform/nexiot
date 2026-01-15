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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 元数据提取处理器 - 提取消息中的设备和产品信息
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/13
 */
@Slf4j
@Component
public class WebSocketMetadataProcessor implements WebSocketUPProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "WebSocket元数据提取处理器";
    }

    @Override
    public String getDescription() {
        return "提取消息中的设备和产品信息";
    }

    @Override
    public ProcessorResult process(WebSocketUPRequest request) {
        // 如果已经有设备ID和产品密钥，跳过
        if (request.getIotId() != null && request.getProductKey() != null) {
            return ProcessorResult.SKIP;
        }

        try {
            // 尝试从消息中提取元数据
            JsonNode jsonNode = objectMapper.readTree(request.getPayload());

            // 提取设备ID
            if (jsonNode.has("deviceId") && request.getIotId() == null) {
                String deviceId = jsonNode.get("deviceId").asText();
                request.setIotId(deviceId);
                log.debug("[WebSocket元数据][提取] SessionID: {}, DeviceID: {}", 
                        request.getSessionId(), deviceId);
            }

            // 提取产品密钥
            if (jsonNode.has("productKey") && request.getProductKey() == null) {
                String productKey = jsonNode.get("productKey").asText();
                request.setProductKey(productKey);
                log.debug("[WebSocket元数据][提取] SessionID: {}, ProductKey: {}", 
                        request.getSessionId(), productKey);
            }

            // 提取消息主题（如果有）
            if (jsonNode.has("topic")) {
                String topic = jsonNode.get("topic").asText();
                // 可以存储到request的扩展字段中
                log.debug("[WebSocket元数据][提取] SessionID: {}, Topic: {}", 
                        request.getSessionId(), topic);
            }

        } catch (Exception e) {
            log.debug("[WebSocket元数据][跳过] SessionID: {} - 消息不是JSON格式或无元数据", 
                    request.getSessionId());
        }
        return ProcessorResult.CONTINUE;
    }

    @Override
    public boolean supports(WebSocketUPRequest request) {
        return true; // 所有消息都尝试提取元数据
    }

    @Override
    public int getOrder() {
        return 200; // 认证后执行
    }

    @Override
    public int getPriority() {
        return 5; // 中等优先级
    }

    @Override
    public boolean preCheck(WebSocketUPRequest request) {
        // 检查 payload 是否存在
        return request.getPayload() != null && !request.getPayload().isEmpty();
    }

    @Override
    public void postProcess(WebSocketUPRequest request, ProcessorResult result) {
        if (result == ProcessorResult.CONTINUE) {
            log.debug("[WebSocket元数据][完成] SessionID: {}, ProductKey: {}, DeviceID: {}",
                    request.getSessionId(), request.getProductKey(), request.getIotId());
        }
    }

    @Override
    public void onError(WebSocketUPRequest request, Exception e) {
        log.warn("[WebSocket元数据][异常] SessionID: {} - {}", 
                request.getSessionId(), e.getMessage());
        // 元数据提取异常不中断流程，仅记录
    }
}
