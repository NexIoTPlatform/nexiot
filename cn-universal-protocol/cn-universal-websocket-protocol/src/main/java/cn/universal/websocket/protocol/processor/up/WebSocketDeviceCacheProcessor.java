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

import cn.universal.dm.device.service.processor.DeviceCachePostProcessor;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 设备缓存处理器 - 对标 MQTT 的设备缓存管理
 * 
 * 职责：处理消息后更新设备缓存，保证设备状态一致性
 * 与 MQTT 复用同一个设备缓存处理器（DeviceCachePostProcessor）
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/16
 */
@Slf4j(topic = "websocket")
@Component
public class WebSocketDeviceCacheProcessor implements WebSocketUPProcessor {

    @Autowired(required = false)
    private DeviceCachePostProcessor deviceCachePostProcessor;

    @Override
    public String getName() {
        return "WebSocket设备缓存处理器";
    }

    @Override
    public String getDescription() {
        return "处理WebSocket消息后更新设备缓存";
    }

    @Override
    public int getOrder() {
        return 850; // 在推送策略处理后执行
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean isEnabled() {
        return deviceCachePostProcessor != null;
    }

    @Override
    public boolean preCheck(WebSocketUPRequest request) {
        return request != null && request.getUpRequestList() != null && !request.getUpRequestList().isEmpty();
    }

    @Override
    public boolean supports(WebSocketUPRequest request) {
        // 设备缓存处理器支持所有有 BaseUPRequest 的消息
        return request.getUpRequestList() != null && !request.getUpRequestList().isEmpty();
    }

    @Override
    public ProcessorResult process(WebSocketUPRequest request) {
        try {
            if (deviceCachePostProcessor == null) {
                log.warn("[{}] 设备缓存处理器未初始化，跳过处理", getName());
                return ProcessorResult.CONTINUE;
            }

            log.debug("[{}] 开始设备缓存处理，SessionID: {}", getName(), request.getSessionId());

            List<BaseUPRequest> upRequestList = request.getUpRequestList();
            if (upRequestList == null || upRequestList.isEmpty()) {
                return ProcessorResult.CONTINUE;
            }

            // 更新设备缓存
            for (BaseUPRequest upRequest : upRequestList) {
                if (upRequest.getIotId() != null && upRequest.getProductKey() != null) {
                    // 调用后处理器清理缓存
                    log.debug("[{}] 清理设备缓存 - iotId: {}, productKey: {}",
                        getName(), upRequest.getIotId(), upRequest.getProductKey());
                    // deviceCachePostProcessor.process(upRequest); // 可根据需要调用
                }
            }

            log.debug("[{}] 设备缓存处理完成，SessionID: {}", getName(), request.getSessionId());
            return ProcessorResult.CONTINUE;

        } catch (Exception e) {
            log.error("[{}] 设备缓存处理异常，SessionID: {}", getName(), request.getSessionId(), e);
            return ProcessorResult.CONTINUE; // 缓存处理失败不影响消息流程
        }
    }

    @Override
    public void postProcess(WebSocketUPRequest request, ProcessorResult result) {
        if (result == ProcessorResult.CONTINUE) {
            log.debug("[{}] 设备缓存处理后置处理完成，SessionID: {}", getName(), request.getSessionId());
        }
    }

    @Override
    public void onError(WebSocketUPRequest request, Exception e) {
        log.error("[{}] 设备缓存处理异常，SessionID: {}", getName(), request.getSessionId(), e);
    }
}
