package cn.universal.websocket.protocol.processor.up;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import cn.universal.websocket.protocol.processor.up.WebSocketUPProcessor.ProcessorResult;

/**
 * WebSocket 上行处理器链
 *
 * <p>使用通用的ProcessorExecutor执行处理器逻辑
 * <p>支持优先级排序和完整的生命周期管理
 * <p>专注于WebSocket业务逻辑，通用逻辑由ProcessorExecutor处理
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2026/1/14
 */
@Component
public class WebSocketUPProcessorChain {

    private static final Logger log = LoggerFactory.getLogger(WebSocketUPProcessorChain.class);

    private List<WebSocketUPProcessor> processors;

    @Autowired
    public void setProcessors(List<WebSocketUPProcessor> allProcessors) {
        // 1. 过滤：仅保留启用的处理器
        this.processors = allProcessors.stream()
                .filter(WebSocketUPProcessor::isEnabled)
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
        
        log.info("[WebSocket上行链][初始化] 共 {} 个处理器，启用 {} 个",
                allProcessors.size(), this.processors.size());
        
        // 打印处理器顺序用于调试
        if (log.isDebugEnabled()) {
            this.processors.forEach(p -> 
                log.debug("[WebSocket上行链][排序] order={}, priority={}, name={}",
                        p.getOrder(), p.getPriority(), p.getName())
            );
        }
    }

    /**
     * 执行处理器链
     *
     * @param request 上行请求
     * @return 是否处理成功
     */
    public boolean process(WebSocketUPRequest request) {
        if (request == null) {
            log.warn("[WebSocket上行链] 请求为空，跳过处理");
            return false;
        }

        // 设置初始处理阶段
        if (request.getStage() == null) {
            request.setStage(WebSocketUPRequest.ProcessingStage.INIT);
        }

        log.debug("[WebSocket上行链] 开始处理消息，SessionID: {}, MessageID: {}",
                request.getSessionId(), request.getMessageId());

        // 执行处理器链
        boolean success = true;
        for (WebSocketUPProcessor processor : processors) {
            try {
                // 1. 前置检查
                if (!processor.preCheck(request)) {
                    log.debug("[WebSocket上行链] 处理器 {} 预检查失败，SessionID: {}",
                            processor.getName(), request.getSessionId());
                    continue; // 跳过该处理器，继续下一个
                }

                // 2. 支持性检查
                if (!processor.supports(request)) {
                    log.debug("[WebSocket上行链] 处理器 {} 不支持该消息，SessionID: {}",
                            processor.getName(), request.getSessionId());
                    continue; // 跳过该处理器，继续下一个
                }

                // 3. 核心处理
                log.debug("[WebSocket上行链] 执行处理器 {}，SessionID: {}",
                        processor.getName(), request.getSessionId());
                
                ProcessorResult result = processor.process(request);

                // 4. 后处理
                processor.postProcess(request, result);

                // 5. 根据结果决定链流程
                if (result == ProcessorResult.STOP) {
                    log.debug("[WebSocket上行链] 处理器 {} 返回 STOP，停止链执行",
                            processor.getName());
                    success = true;
                    break; // 成功停止
                } else if (result == ProcessorResult.ERROR) {
                    log.error("[WebSocket上行链] 处理器 {} 返回 ERROR，链执行失败",
                            processor.getName());
                    success = false;
                    break; // 失败
                } else if (result == ProcessorResult.SKIP) {
                    log.debug("[WebSocket上行链] 处理器 {} 返回 SKIP，继续下一个处理器",
                            processor.getName());
                    continue; // 跳过，继续下一个
                }
                // CONTINUE 时继续下一个处理器

            } catch (Exception e) {
                // 异常处理
                processor.onError(request, e);
                log.error("[WebSocket上行链] 处理器 {} 执行异常，SessionID: {}",
                        processor.getName(), request.getSessionId(), e);
                success = false;
                break; // 异常视为失败
            }
        }

        // 设置最终处理阶段
        if (success && request.getStage() != WebSocketUPRequest.ProcessingStage.FAILED) {
            request.setStage(WebSocketUPRequest.ProcessingStage.COMPLETED);
        } else if (!success) {
            request.setStage(WebSocketUPRequest.ProcessingStage.FAILED);
        }

        log.debug("[WebSocket上行链] 消息处理完成，SessionID: {}, 成功: {}, 最终阶段: {}",
                request.getSessionId(), success, request.getStage());

        return success;
    }

    /**
     * 批量处理消息
     *
     * @param requests 上行请求列表
     * @return 成功处理的数量
     */
    public int processBatch(List<WebSocketUPRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            log.warn("[WebSocket上行链] 批量请求为空");
            return 0;
        }

        int successCount = 0;
        int failureCount = 0;
        
        for (WebSocketUPRequest request : requests) {
            try {
                if (process(request)) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                log.error("[WebSocket上行链] 批量处理异常，SessionID: {}", request.getSessionId(), e);
                failureCount++;
            }
        }

        log.info("[WebSocket上行链] 批量处理完成，总数: {}, 成功: {}, 失败: {}",
                requests.size(), successCount, failureCount);

        return successCount;
    }

    /**
     * 获取处理器列表（用于调试和监控）
     *
     * @return 当前启用的处理器列表
     */
    public List<WebSocketUPProcessor> getProcessors() {
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
