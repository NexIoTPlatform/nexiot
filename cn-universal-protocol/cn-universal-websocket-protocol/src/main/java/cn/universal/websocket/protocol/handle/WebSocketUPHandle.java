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

package cn.universal.websocket.protocol.handle;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import cn.universal.dm.device.entity.IoTPushResult;
import cn.universal.dm.device.service.IoTUPPushAdapter;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;

/**
 * WebSocket 上行消息处理类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/13
 */
@Component
public class WebSocketUPHandle extends IoTUPPushAdapter<WebSocketUPRequest> {

    private static final Logger log = LoggerFactory.getLogger(WebSocketUPHandle.class);

    /**
     * 处理上行消息
     */
    public String up(List<WebSocketUPRequest> upRequests) {
        try {
            return doUp(upRequests);
        } catch (Exception e) {
            log.error("[WebSocket上行][处理异常]", e);
        }
        return null;
    }

    /**
     * 推送前扩展：消息转换、规则引擎等处理
     *
     * @param upRequests 上行请求列表
     */
    @Override
    protected void onBeforePush(List<WebSocketUPRequest> upRequests) {
        log.info("[WebSocket上行][推送前处理] 开始处理 {} 条消息", upRequests.size());

        // 数据增强 - 添加协议标识
        upRequests.forEach(request -> {
            // 记录消息来源渠道
            log.debug("[WebSocket上行][消息转换] 处理消息: sessionId={}, deviceId={}", 
                    request.getSessionId(), request.getIotId());
        });

        // 消息验证 - 过滤不完整的消息
        upRequests.removeIf(request -> {
            boolean shouldFilter = request.getIotId() == null 
                    || request.getProductKey() == null;
            if (shouldFilter) {
                log.warn("[WebSocket上行][规则验证] 消息不完整，过滤: sessionId={}", 
                        request.getSessionId());
            }
            return shouldFilter;
        });

        log.info("[WebSocket上行][推送前处理] 处理完成，剩余 {} 条消息", upRequests.size());
    }

    /**
     * 推送后扩展：结果处理、日志记录等
     *
     * @param upRequests 上行请求列表
     * @param pushResults 推送结果列表
     */
    @Override
    protected void onAfterPush(List<WebSocketUPRequest> upRequests, List<IoTPushResult> pushResults) {
        log.info("[WebSocket上行][推送后处理] 推送完成，消息数量: {}, 结果数量: {}", 
                upRequests.size(), pushResults != null ? pushResults.size() : 0);

        // 推送结果记录
        if (pushResults != null) {
            pushResults.forEach(result -> {
                log.debug("[WebSocket上行][推送记录] 设备 {} 渠道 {} 推送结果: {}", 
                        result.getDeviceId(), result.getChannel(), result.isOk());
            });

            // 统计推送结果
            long successCount = pushResults.stream().filter(IoTPushResult::isOk).count();
            long failureCount = pushResults.size() - successCount;
            log.info("[WebSocket上行][推送统计] 成功: {}, 失败: {}", successCount, failureCount);
        }

        // 异常处理 - 记录失败的消息
        if (pushResults != null) {
            pushResults.stream()
                    .filter(result -> !result.isOk())
                    .forEach(result -> {
                        log.error("[WebSocket上行][推送失败] 设备 {} 推送失败: {}", 
                                result.getDeviceId(), result.getErrorMessage());
                    });
        }
    }
}
