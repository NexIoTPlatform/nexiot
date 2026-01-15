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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.universal.websocket.protocol.config.WebSocketProperties;
import cn.universal.websocket.protocol.entity.WebSocketSession;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import cn.universal.websocket.protocol.enums.WebSocketStatus;
import cn.universal.websocket.protocol.service.WebSocketSessionManager;
import lombok.extern.slf4j.Slf4j;

/**
 * 认证处理器 - 验证设备身份
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/13
 */
@Slf4j
@Component
public class WebSocketAuthProcessor implements WebSocketUPProcessor {

    @Autowired
    private WebSocketProperties properties;

    @Autowired
    private WebSocketSessionManager sessionManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "WebSocket认证处理器";
    }

    @Override
    public String getDescription() {
        return "验证WebSocket客户端身份认证";
    }

    @Override
    public ProcessorResult process(WebSocketUPRequest request) {
        if (!properties.getAuthRequired()) {
            log.debug("[WebSocket认证][跳过] SessionID: {} - 认证未启用", request.getSessionId());
            return ProcessorResult.SKIP;
        }

        // MQTT客户端已经在协议层完成认证，如果请求包含productKey则跳过认证检查
        if (request.getProductKey() != null && !request.getProductKey().isEmpty()) {
            log.debug("[WebSocket认证][跳过] SessionID: {} - MQTT协议已认证, ProductKey: {}", 
                    request.getSessionId(), request.getProductKey());
            return ProcessorResult.SKIP;
        }

        WebSocketSession session = sessionManager.getSession(request.getSessionId());
        if (session == null) {
            log.warn("[WebSocket认证][失败] SessionID: {} - 会话不存在", request.getSessionId());
            return ProcessorResult.ERROR;
        }

        // 如果已认证，跳过
        if (session.getStatus() == WebSocketStatus.AUTHENTICATED) {
            return ProcessorResult.SKIP;
        }

        try {
            // 从消息中提取认证信息
            JsonNode jsonNode = objectMapper.readTree(request.getPayload());
            
            // 检查是否是认证消息
            if (jsonNode.has("type") && "auth".equals(jsonNode.get("type").asText())) {
                String deviceId = jsonNode.path("deviceId").asText();
                String productKey = jsonNode.path("productKey").asText();
                String deviceSecret = jsonNode.path("deviceSecret").asText();

                log.info("[WebSocket认证][开始] SessionID: {}, DeviceID: {}", 
                        request.getSessionId(), deviceId);

                // TODO: 实际环境中应该查询数据库验证设备凭证
                // 这里简单验证不为空
                if (deviceId != null && !deviceId.isEmpty() 
                        && productKey != null && !productKey.isEmpty()) {
                    
                    // 认证成功
                    session.setStatus(WebSocketStatus.AUTHENTICATED);
                    session.setDeviceId(deviceId);
                    
                    // 绑定设备到会话
                    sessionManager.bindDevice(request.getSessionId(), deviceId, productKey);
                    
                    // 设置设备信息（BaseUPRequest 中的字段）
                    request.setIotId(deviceId);
                    request.setProductKey(productKey);
                    request.setDeviceName(deviceId);
                    request.setStage(WebSocketUPRequest.ProcessingStage.AUTHENTICATED);

                    log.info("[WebSocket认证][成功] SessionID: {}, DeviceID: {}", 
                            request.getSessionId(), deviceId);
                    return ProcessorResult.CONTINUE;
                } else {
                    log.warn("[WebSocket认证][失败] SessionID: {} - 凭证不完整", 
                            request.getSessionId());
                    return ProcessorResult.ERROR;
                }
            }
        } catch (Exception e) {
            log.error("[WebSocket认证][异常] SessionID: {}", request.getSessionId(), e);
            return ProcessorResult.ERROR;
        }
        
        return ProcessorResult.CONTINUE;
    }

    @Override
    public boolean supports(WebSocketUPRequest request) {
        return true; // 所有消息都需要认证检查
    }

    @Override
    public int getOrder() {
        return 100; // 最先执行认证
    }

    @Override
    public int getPriority() {
        return 10; // 认证优先级较高
    }

    @Override
    public boolean preCheck(WebSocketUPRequest request) {
        // 检查必要的会话信息
        return request.getSessionId() != null;
    }

    @Override
    public void postProcess(WebSocketUPRequest request, ProcessorResult result) {
        if (result == ProcessorResult.CONTINUE) {
            log.debug("[WebSocket认证][完成] SessionID: {}, 认证状态: {}", 
                    request.getSessionId(), request.getStage());
        } else if (result == ProcessorResult.ERROR) {
            log.warn("[WebSocket认证][失败] SessionID: {}", request.getSessionId());
        }
    }

    @Override
    public void onError(WebSocketUPRequest request, Exception e) {
        log.error("[WebSocket认证][异常] SessionID: {}, 异常信息: {}", 
                request.getSessionId(), e.getMessage());
        request.setError("认证处理异常: " + e.getMessage());
    }
}
