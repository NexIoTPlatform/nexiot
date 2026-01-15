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

package cn.universal.websocket.protocol.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.universal.websocket.protocol.entity.WebSocketDownRequest;
import cn.universal.websocket.protocol.processor.down.WebSocketDownProcessorChain;

/**
 * WebSocket 下行消息处理器
 *
 * <p>负责处理从服务器到设备的下行消息
 * <p>关键职责：
 * 1. 接收下行消息请求
 * 2. 路由到对应的处理器链
 * 3. 调用处理器链处理消息
 * 4. 返回处理结果
 * 5. 处理错误和异常
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/15
 */
@Component
public class WebSocketDownHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketDownHandler.class);

    @Autowired(required = false)
    private WebSocketDownProcessorChain downProcessorChain;

    /**
     * 处理下行消息
     *
     * @param request 下行消息请求
     * @return 处理结果
     */
    public boolean handle(WebSocketDownRequest request) {
        try {
            if (request == null) {
                log.warn("[WebSocket下行][参数异常] 请求对象为空");
                return false;
            }

            if (downProcessorChain == null) {
                log.warn("[WebSocket下行][链异常] 处理器链未初始化");
                return false;
            }

            log.debug("[WebSocket下行][开始处理] MessageID: {}, IotId: {}",
                    request.getMessageId(), request.getIotId());

            // 调用处理器链
            boolean result = downProcessorChain.process(request);

            if (result) {
                log.debug("[WebSocket下行][处理成功] MessageID: {}, IotId: {}",
                        request.getMessageId(), request.getIotId());
            } else {
                log.warn("[WebSocket下行][处理失败] MessageID: {}, Error: {}",
                        request.getMessageId(), request.getError());
            }

            return result;

        } catch (Exception e) {
            log.error("[WebSocket下行][处理异常] MessageID: {}, 异常: {}",
                    request != null ? request.getMessageId() : "unknown",
                    e.getMessage(), e);
            if (request != null) {
                request.setError("下行消息处理异常: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * 异步处理下行消息
     *
     * @param request 下行消息请求
     */
    public void handleAsync(WebSocketDownRequest request) {
        try {
            if (request == null) {
                log.warn("[WebSocket下行异步][参数异常] 请求对象为空");
                return;
            }

            log.debug("[WebSocket下行异步][开始处理] MessageID: {}, IotId: {}",
                    request.getMessageId(), request.getIotId());

            // 在后台线程池中异步处理
            new Thread(() -> {
                try {
                    handle(request);
                } catch (Exception e) {
                    log.error("[WebSocket下行异步][处理异常] MessageID: {}",
                            request.getMessageId(), e);
                }
            }).start();

        } catch (Exception e) {
            log.error("[WebSocket下行异步][异常] MessageID: {}, 异常: {}",
                    request != null ? request.getMessageId() : "unknown",
                    e.getMessage(), e);
        }
    }

    /**
     * 批量处理下行消息
     *
     * @param requests 下行消息请求列表
     * @return 成功处理的消息数量
     */
    public int handleBatch(java.util.List<WebSocketDownRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            log.warn("[WebSocket下行批量][参数异常] 请求列表为空");
            return 0;
        }

        int successCount = 0;
        log.debug("[WebSocket下行批量][开始处理] 总数: {}", requests.size());

        for (WebSocketDownRequest request : requests) {
            try {
                if (handle(request)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("[WebSocket下行批量][单项异常] MessageID: {}, 异常: {}",
                        request.getMessageId(), e.getMessage());
            }
        }

        log.debug("[WebSocket下行批量][处理完成] 成功: {}/{}", successCount, requests.size());
        return successCount;
    }

    /**
     * 处理带重试的下行消息
     *
     * @param request 下行消息请求
     * @param maxRetries 最大重试次数
     * @return 是否处理成功
     */
    public boolean handleWithRetry(WebSocketDownRequest request, int maxRetries) {
        if (request == null || maxRetries < 0) {
            return false;
        }

        int retryCount = 0;
        while (retryCount <= maxRetries) {
            try {
                if (handle(request)) {
                    return true;
                }
                retryCount++;

                if (retryCount <= maxRetries) {
                    log.info("[WebSocket下行重试] MessageID: {}, 重试次数: {}/{}",
                            request.getMessageId(), retryCount, maxRetries);
                    // 指数退避
                    long delay = Math.min(1000L * (long) Math.pow(2, retryCount - 1), 30000L);
                    Thread.sleep(delay);
                }
            } catch (InterruptedException e) {
                log.warn("[WebSocket下行重试][中断] MessageID: {}", request.getMessageId());
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                log.error("[WebSocket下行重试][异常] MessageID: {}, 重试: {}", 
                        request.getMessageId(), retryCount, e);
                retryCount++;
            }
        }

        log.error("[WebSocket下行重试][失败] MessageID: {}, 已达最大重试次数: {}",
                request.getMessageId(), maxRetries);
        return false;
    }

    /**
     * 获取处理器链
     */
    public WebSocketDownProcessorChain getProcessorChain() {
        return downProcessorChain;
    }

    /**
     * 设置处理器链
     */
    public void setProcessorChain(WebSocketDownProcessorChain chain) {
        this.downProcessorChain = chain;
        log.info("[WebSocket下行][初始化] 处理器链已设置");
    }
}
