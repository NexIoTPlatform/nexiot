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

package cn.universal.websocket.protocol.registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import cn.universal.websocket.protocol.processor.down.WebSocketDownProcessor;
import cn.universal.websocket.protocol.processor.up.WebSocketUPProcessor;

/**
 * WebSocket 处理器注册表
 *
 * <p>负责自动扫描和注册上下行处理器
 * <p>关键职责：
 * 1. 自动发现容器中的处理器 Bean
 * 2. 根据优先级和顺序排序处理器
 * 3. 提供处理器的查询接口
 * 4. 支持动态注册和注销处理器
 * 5. 生成处理器执行链
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/15
 */
@Component
public class ProcessorRegistry {

    private static final Logger log = LoggerFactory.getLogger(ProcessorRegistry.class);

    private final List<WebSocketUPProcessor> upProcessors = new CopyOnWriteArrayList<>();
    private final List<WebSocketDownProcessor> downProcessors = new CopyOnWriteArrayList<>();

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 初始化处理器注册表
     * 扫描并注册所有处理器 Bean
     */
    public void initialize() {
        try {
            log.info("[处理器注册][初始化] 开始扫描处理器...");

            // 扫描并注册上行处理器
            registerUpProcessors();

            // 扫描并注册下行处理器
            registerDownProcessors();

            log.info("[处理器注册][初始化] 初始化完成，上行处理器数: {}, 下行处理器数: {}",
                    upProcessors.size(), downProcessors.size());

            // 打印处理器执行序列
            printProcessorChain();

        } catch (Exception e) {
            log.error("[处理器注册][初始化][异常] 异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 注册上行处理器
     */
    private void registerUpProcessors() {
        try {
            java.util.Map<String, WebSocketUPProcessor> processors =
                    applicationContext.getBeansOfType(WebSocketUPProcessor.class);

            processors.forEach((name, processor) -> {
                if (processor.isEnabled()) {
                    upProcessors.add(processor);
                    log.debug("[处理器注册][上行] 注册处理器: {} (Order: {}, Priority: {})",
                            processor.getName(), processor.getOrder(), processor.getPriority());
                }
            });

            // 按 Order 升序排序，Order 相同则按 Priority 降序排序
            upProcessors.sort(new ProcessorComparator());

            log.info("[处理器注册][上行] 共注册 {} 个上行处理器", upProcessors.size());

        } catch (Exception e) {
            log.error("[处理器注册][上行][异常] 异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 注册下行处理器
     */
    private void registerDownProcessors() {
        try {
            java.util.Map<String, WebSocketDownProcessor> processors =
                    applicationContext.getBeansOfType(WebSocketDownProcessor.class);

            processors.forEach((name, processor) -> {
                if (processor.isEnabled()) {
                    downProcessors.add(processor);
                    log.debug("[处理器注册][下行] 注册处理器: {} (Order: {}, Priority: {})",
                            processor.getName(), processor.getOrder(), processor.getPriority());
                }
            });

            // 按 Order 升序排序，Order 相同则按 Priority 降序排序
            downProcessors.sort(new ProcessorComparator());

            log.info("[处理器注册][下行] 共注册 {} 个下行处理器", downProcessors.size());

        } catch (Exception e) {
            log.error("[处理器注册][下行][异常] 异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取所有上行处理器
     */
    public List<WebSocketUPProcessor> getUpProcessors() {
        return new ArrayList<>(upProcessors);
    }

    /**
     * 获取所有下行处理器
     */
    public List<WebSocketDownProcessor> getDownProcessors() {
        return new ArrayList<>(downProcessors);
    }

    /**
     * 动态注册上行处理器
     */
    public void registerUpProcessor(WebSocketUPProcessor processor) {
        if (processor == null || !processor.isEnabled()) {
            return;
        }
        if (!upProcessors.contains(processor)) {
            upProcessors.add(processor);
            upProcessors.sort(new ProcessorComparator());
            log.info("[处理器注册][动态上行] 注册处理器: {}", processor.getName());
        }
    }

    /**
     * 动态注册下行处理器
     */
    public void registerDownProcessor(WebSocketDownProcessor processor) {
        if (processor == null || !processor.isEnabled()) {
            return;
        }
        if (!downProcessors.contains(processor)) {
            downProcessors.add(processor);
            downProcessors.sort(new ProcessorComparator());
            log.info("[处理器注册][动态下行] 注册处理器: {}", processor.getName());
        }
    }

    /**
     * 注销上行处理器
     */
    public void unregisterUpProcessor(WebSocketUPProcessor processor) {
        if (upProcessors.remove(processor)) {
            log.info("[处理器注册][注销上行] 注销处理器: {}", processor.getName());
        }
    }

    /**
     * 注销下行处理器
     */
    public void unregisterDownProcessor(WebSocketDownProcessor processor) {
        if (downProcessors.remove(processor)) {
            log.info("[处理器注册][注销下行] 注销处理器: {}", processor.getName());
        }
    }

    /**
     * 打印处理器执行链
     */
    private void printProcessorChain() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== 处理器执行链 ==========\n");

        sb.append("【上行处理器链】\n");
        for (int i = 0; i < upProcessors.size(); i++) {
            WebSocketUPProcessor p = upProcessors.get(i);
            sb.append(String.format("%d. [Order:%d, Priority:%d] %s - %s\n",
                    i + 1, p.getOrder(), p.getPriority(), p.getName(), p.getDescription()));
        }

        sb.append("\n【下行处理器链】\n");
        for (int i = 0; i < downProcessors.size(); i++) {
            WebSocketDownProcessor p = downProcessors.get(i);
            sb.append(String.format("%d. [Order:%d, Priority:%d] %s - %s\n",
                    i + 1, p.getOrder(), p.getPriority(), p.getName(), p.getDescription()));
        }

        sb.append("================================\n");

        log.info(sb.toString());
    }

    /**
     * 处理器比较器（Order升序 + Priority降序）
     */
    private static class ProcessorComparator implements Comparator<Object> {

        @Override
        public int compare(Object o1, Object o2) {
            int order1 = 0, order2 = 0;
            int priority1 = 0, priority2 = 0;

            if (o1 instanceof WebSocketUPProcessor) {
                WebSocketUPProcessor p1 = (WebSocketUPProcessor) o1;
                order1 = p1.getOrder();
                priority1 = p1.getPriority();
            } else if (o1 instanceof WebSocketDownProcessor) {
                WebSocketDownProcessor p1 = (WebSocketDownProcessor) o1;
                order1 = p1.getOrder();
                priority1 = p1.getPriority();
            }

            if (o2 instanceof WebSocketUPProcessor) {
                WebSocketUPProcessor p2 = (WebSocketUPProcessor) o2;
                order2 = p2.getOrder();
                priority2 = p2.getPriority();
            } else if (o2 instanceof WebSocketDownProcessor) {
                WebSocketDownProcessor p2 = (WebSocketDownProcessor) o2;
                order2 = p2.getOrder();
                priority2 = p2.getPriority();
            }

            // Order 升序
            int orderCompare = Integer.compare(order1, order2);
            if (orderCompare != 0) {
                return orderCompare;
            }

            // Order 相同，Priority 降序
            return Integer.compare(priority2, priority1);
        }
    }
}
