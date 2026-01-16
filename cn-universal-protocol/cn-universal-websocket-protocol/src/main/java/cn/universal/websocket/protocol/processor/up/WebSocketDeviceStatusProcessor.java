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

import cn.universal.dm.device.service.action.IoTDeviceActionAfterService;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 设备状态处理器 - 处理设备上线、离线等状态变化
 * 
 * 对标 MQTT 中的 IoTDeviceActionAfterService 设备生命周期管理
 * 职责：将接收到的消息转化为设备上线/离线事件，并执行相关业务逻辑
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/16
 */
@Slf4j(topic = "websocket")
@Component
public class WebSocketDeviceStatusProcessor implements WebSocketUPProcessor {

    @Autowired
    private IoTDeviceActionAfterService deviceActionAfterService;

    @Override
    public String getName() {
        return "WebSocket设备状态处理器";
    }

    @Override
    public String getDescription() {
        return "处理WebSocket设备的上线、离线等状态变化";
    }

    @Override
    public int getOrder() {
        return 600; // 在编解码后执行
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
        // 只要有 BaseUPRequest，就支持处理
        return request.getUpRequestList() != null && request.getUpRequestList().size() > 0;
    }

    @Override
    public ProcessorResult process(WebSocketUPRequest request) {
        try {
            log.debug("[{}] 开始处理设备状态，SessionID: {}", getName(), request.getSessionId());
            
            List<BaseUPRequest> upRequestList = request.getUpRequestList();
            if (upRequestList == null || upRequestList.isEmpty()) {
                return ProcessorResult.CONTINUE;
            }

            // 对标 MQTT 的设备生命周期管理
            // 根据消息类型判断是否需要处理设备上线状态
            for (BaseUPRequest upRequest : upRequestList) {
                handleDeviceStatus(request, upRequest);
            }

            log.debug("[{}] 设备状态处理完成，SessionID: {}", getName(), request.getSessionId());
            return ProcessorResult.CONTINUE;

        } catch (Exception e) {
            log.error("[{}] 设备状态处理异常，SessionID: {}", getName(), request.getSessionId(), e);
            return ProcessorResult.CONTINUE; // 状态处理失败不影响消息流程
        }
    }

    /**
     * 处理设备状态变化
     */
    private void handleDeviceStatus(WebSocketUPRequest request, BaseUPRequest upRequest) {
        // 此处可扩展为处理更复杂的状态逻辑
        // 当前实现简化版：主要由后续的规则引擎和数据处理器处理
        
        log.debug("[{}] 处理设备状态 - iotId: {}, messageType: {}", 
            getName(), upRequest.getIotId(), upRequest.getMessageType());
    }

    @Override
    public void postProcess(WebSocketUPRequest request, ProcessorResult result) {
        if (result == ProcessorResult.CONTINUE) {
            log.debug("[{}] 设备状态处理后置处理完成，SessionID: {}", getName(), request.getSessionId());
        }
    }

    @Override
    public void onError(WebSocketUPRequest request, Exception e) {
        log.error("[{}] 设备状态处理异常，SessionID: {}", getName(), request.getSessionId(), e);
    }
}
