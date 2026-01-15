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

package cn.universal.websocket.protocol.processor.down;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import cn.universal.websocket.protocol.entity.WebSocketDownRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 格式化处理器 - 格式化下行消息
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/13
 */
@Slf4j
@Component
public class WebSocketFormatProcessor implements WebSocketDownProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "WebSocket格式化处理器";
    }

    @Override
    public boolean supports(WebSocketDownRequest request) {
        return request != null && request.getPayload() != null;
    }

    @Override
    public ProcessorResult process(WebSocketDownRequest request) {
        try {
            // 如果payload为空，创建默认消息结构
            if (request.getPayload() == null || request.getPayload().isEmpty()) {
                Map<String, Object> message = new HashMap<>();
                message.put("type", "command");
                message.put("deviceId", request.getDeviceId());
                message.put("timestamp", System.currentTimeMillis());
                
                String payload = objectMapper.writeValueAsString(message);
                request.setPayload(payload);
                log.debug("[WebSocket格式化][创建] DeviceID: {} - 创建默认消息结构", 
                        request.getDeviceId());
                return ProcessorResult.CONTINUE;
            }

            // 尝试解析为JSON并美化
            try {
                Object json = objectMapper.readValue(request.getPayload(), Object.class);
                String formatted = objectMapper.writeValueAsString(json);
                request.setPayload(formatted);
                log.debug("[WebSocket格式化][美化] DeviceID: {} - JSON格式化完成", 
                        request.getDeviceId());
            } catch (Exception e) {
                // 不是JSON格式，保持原样
                log.debug("[WebSocket格式化][跳过] DeviceID: {} - 非JSON格式", 
                        request.getDeviceId());
            }
            return ProcessorResult.CONTINUE;

        } catch (Exception e) {
            log.warn("[WebSocket格式化][异常] DeviceID: {} - 保持原始消息", 
                    request.getDeviceId());
            return ProcessorResult.CONTINUE;
        }
    }

    @Override
    public int getOrder() {
        return 100; // 第一个执行
    }
}
