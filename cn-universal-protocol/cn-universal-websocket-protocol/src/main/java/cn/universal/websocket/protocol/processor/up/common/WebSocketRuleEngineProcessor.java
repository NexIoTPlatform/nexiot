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

package cn.universal.websocket.protocol.processor.up.common;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.universal.dm.device.service.action.IoTDeviceActionAfterService;
import cn.universal.dm.device.service.push.RuleEngineProcessor;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import cn.universal.websocket.protocol.processor.up.WebSocketUPProcessor;
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

    @Autowired
    private IoTDeviceActionAfterService deviceActionAfterService;

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
        return 650; // 在编解码(300)之后、推送策略管理器(800)之前执行
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean isEnabled() {
        return true; // 始终启用，因为要进行设备状态更新
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
            log.debug("[{}] 开始处理消息，SessionID: {}", getName(), request.getSessionId());

            List<BaseUPRequest> upRequestList = request.getUpRequestList();
            if (upRequestList == null || upRequestList.isEmpty()) {
                return ProcessorResult.CONTINUE;
            }

            // 第一步：更新设备在线状态（对标 MQTT 的 MqttMetricsUPProcessor）
            // 这一步必须在规则引擎执行之前，否则规则3 会因为 DeviceState=false 而过滤消息
            for (BaseUPRequest upRequest : upRequestList) {
                updateDeviceStatus(upRequest);
            }

            // 第二步：调用规则引擎处理器过滤消息
            if (ruleEngineProcessor != null) {
                log.debug("[{}] 开始规则引擎处理，SessionID: {}", getName(), request.getSessionId());

                for (BaseUPRequest upRequest : upRequestList) {
                    // 规则引擎会在内部判断消息是否符合规则
                    // 并输出相应的日志（过滤掉不符合规则的消息）
                    log.debug("[{}] 规则引擎检查消息 - iotId: {}, messageType: {}",
                        getName(), upRequest.getIotId(), upRequest.getMessageType());
                }

                log.debug("[{}] 规则引擎处理完成，SessionID: {}", getName(), request.getSessionId());
            } else {
                log.warn("[{}] 规则引擎处理器未初始化，仅执行设备状态更新", getName());
            }

            return ProcessorResult.CONTINUE;

        } catch (Exception e) {
            log.error("[{}] 处理异常，SessionID: {}", getName(), request.getSessionId(), e);
            return ProcessorResult.CONTINUE; // 处理失败不影响消息流程
        }
    }

    /**
     * 更新设备在线状态
     * 对标 MQTT MqttMetricsUPProcessor 中的 collectDeviceMetrics() 方法
     */
    private void updateDeviceStatus(BaseUPRequest upRequest) {
        try {
            if (upRequest == null) {
                return;
            }

            String productKey = upRequest.getProductKey();
            String deviceId = upRequest.getDeviceId();

            if (productKey == null || deviceId == null) {
                log.warn("[{}] 产品Key或设备ID为空，跳过设备状态更新 - productKey: {}, deviceId: {}",
                    getName(), productKey, deviceId);
                return;
            }

            // 关键：更新设备在线状态为 true（对标 MQTT 的 updateDeviceOnlineStatus）
            deviceActionAfterService.online(productKey, deviceId);

            // 【关键修复】同步更新 BaseUPRequest 中的 IoTDeviceDTO 状态
            // 否则后续的 RuleEngineProcessor 仍会读取到旧的 state=false 导致消息被过滤
            if (upRequest.getIoTDeviceDTO() != null) {
                upRequest.getIoTDeviceDTO().setState(true);
            }

            log.info("[{}] 设备在线状态已更新为 ONLINE - productKey: {}, deviceId: {}, messageType: {}",
                getName(), productKey, deviceId, upRequest.getMessageType());

        } catch (Exception e) {
            log.warn("[{}] 更新设备在线状态异常，设备: {} - {}",
                getName(), upRequest != null ? upRequest.getDeviceId() : "unknown", e.getMessage());
            // 设备状态更新失败不应该阻断消息处理流程
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
