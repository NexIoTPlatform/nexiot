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

import java.util.Base64;

import org.springframework.stereotype.Component;

import cn.universal.websocket.protocol.entity.WebSocketDownRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 下行编解码处理器 - 处理下行消息的编码
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/13
 */
@Slf4j
@Component
public class WebSocketDownCodecProcessor implements WebSocketDownProcessor {

    @Override
    public String getName() {
        return "WebSocket下行编码处理器";
    }

    @Override
    public boolean supports(WebSocketDownRequest request) {
        return request != null && request.getPayload() != null;
    }

    @Override
    public ProcessorResult process(WebSocketDownRequest request) {
        try {
            // 如果需要Base64编码（根据请求配置）
            // 默认不编码，除非特殊标记
            boolean shouldEncode = request.getExtras() != null && 
                    (Boolean) request.getExtras().getOrDefault("base64Encode", false);
            
            if (shouldEncode) {
                String encoded = encodeBase64(request.getPayload());
                request.setPayload(encoded);
                log.debug("[WebSocket下行编码][Base64] DeviceID: {} - 编码完成", 
                        request.getDeviceId());
            }

            // 记录消息大小
            int size = request.getPayload() != null ? request.getPayload().length() : 0;
            log.debug("[WebSocket下行编码][大小] DeviceID: {} - 消息大小: {} bytes", 
                    request.getDeviceId(), size);
            return ProcessorResult.CONTINUE;

        } catch (Exception e) {
            log.warn("[WebSocket下行编码][异常] DeviceID: {} - 保持原始消息", 
                    request.getDeviceId());
            return ProcessorResult.CONTINUE;
        }
    }

    @Override
    public int getOrder() {
        return 200; // 格式化后执行
    }

    /**
     * Base64编码
     */
    private String encodeBase64(String str) {
        return Base64.getEncoder().encodeToString(str.getBytes());
    }
}
