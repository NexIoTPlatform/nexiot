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

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.universal.websocket.protocol.entity.WebSocketDownRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 下行处理器链
 * 
 * <p>使用通用的处理器模式执行下行消息处理链
 * <p>支持完整的生命周期管理和优先级排序
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2026/1/15
 */
@Slf4j
@Component
public class WebSocketDownProcessorChain {

    private List<WebSocketDownProcessor> processors;

    @Autowired
    public WebSocketDownProcessorChain(List<WebSocketDownProcessor> processors) {
        // 1. 过滤：仅保留启用的处理器
        this.processors = processors.stream()
                .filter(WebSocketDownProcessor::isEnabled)
                .collect(Collectors.toList());
        
        // 2. 排序：先按 order（asc），再按 priority（desc）
        this.processors.sort((p1, p2) -> {
            int orderCmp = Integer.compare(p1.getOrder(), p2.getOrder());
            if (orderCmp != 0) {
                return orderCmp; // 按 order 升序
            }
            // 同一 order 下，按 priority 降序（优先级高的先执行）
            return Integer.compare(p2.getPriority(), p1.getPriority());
        });
        
        log.info("[WebSocket下行链][初始化] 共 {} 个处理器，启用 {} 个",
                processors.size(), this.processors.size());
        
        // 打印处理器顺序用于调试
        if (log.isDebugEnabled()) {
            this.processors.forEach(p -> 
                log.debug("[WebSocket下行链][排序] order={}, priority={}, name={}",
                        p.getOrder(), p.getPriority(), p.getName())
            );
        }
    }

    /**
     * 执行处理器链
     *
     * @param request 下行请求
     * @return 是否处理成功
     */
    public boolean process(WebSocketDownRequest request) {
        if (request == null) {
            log.warn("[WebSocket下行链] 请求为空，跳过处理");
            return false;
        }

        log.debug("[WebSocket下行链] 开始处理下行消息，DeviceID: {}, MessageID: {}",
                request.getDeviceId(), request.getMessageId());

        for (WebSocketDownProcessor processor : processors) {
            try {
                // 1. 前置检查
                if (!processor.preCheck(request)) {
                    log.debug("[WebSocket下行链] 处理器 {} 预检查失败，DeviceID: {}",
                            processor.getName(), request.getDeviceId());
                    continue; // 跳过该处理器，继续下一个
                }

                // 2. 支持性检查
                if (!processor.supports(request)) {
                    log.debug("[WebSocket下行链] 处理器 {} 不支持该消息，DeviceID: {}",
                            processor.getName(), request.getDeviceId());
                    continue; // 跳过该处理器，继续下一个
                }

                // 3. 核心处理
                log.debug("[WebSocket下行链] 执行处理器 {}，DeviceID: {}",
                        processor.getName(), request.getDeviceId());
                
                WebSocketDownProcessor.ProcessorResult result = processor.process(request);

                // 4. 后处理
                processor.postProcess(request, result);

                // 5. 根据结果决定链流程
                if (result == WebSocketDownProcessor.ProcessorResult.STOP) {
                    log.debug("[WebSocket下行链] 处理器 {} 返回 STOP，停止链执行",
                            processor.getName());
                    return true; // 成功停止
                } else if (result == WebSocketDownProcessor.ProcessorResult.ERROR) {
                    log.error("[WebSocket下行链] 处理器 {} 返回 ERROR，链执行失败",
                            processor.getName());
                    return false; // 失败
                } else if (result == WebSocketDownProcessor.ProcessorResult.SKIP) {
                    log.debug("[WebSocket下行链] 处理器 {} 返回 SKIP，继续下一个处理器",
                            processor.getName());
                    continue; // 跳过，继续下一个
                }
                // CONTINUE 时继续下一个处理器

            } catch (Exception e) {
                // 异常处理
                processor.onError(request, e);
                log.error("[WebSocket下行链] 处理器 {} 执行异常，DeviceID: {}",
                        processor.getName(), request.getDeviceId(), e);
                return false; // 异常视为失败
            }
        }

        log.debug("[WebSocket下行链] 处理器链执行完成，DeviceID: {}", request.getDeviceId());
        return true;
    }

    /**
     * 批量处理下行请求
     *
     * @param requests 下行请求列表
     * @return 成功处理的数量
     */
    public int processBatch(List<WebSocketDownRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            log.warn("[WebSocket下行链] 批量请求为空");
            return 0;
        }

        int successCount = 0;
        int failureCount = 0;
        
        for (WebSocketDownRequest request : requests) {
            try {
                if (process(request)) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                log.error("[WebSocket下行链] 批量处理异常，DeviceID: {}", request.getDeviceId(), e);
                failureCount++;
            }
        }

        log.info("[WebSocket下行链] 批量处理完成，总数: {}, 成功: {}, 失败: {}",
                requests.size(), successCount, failureCount);

        return successCount;
    }

    /**
     * 获取处理器列表（用于调试和监控）
     *
     * @return 当前启用的处理器列表
     */
    public List<WebSocketDownProcessor> getProcessors() {
        return processors;
    }

    /**
     * 获取处理器数量
     *
     * @return 启用的处理器数量
     */
    public int getProcessorCount() {
        return processors.size();
    }
}
