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

package cn.universal.websocket.protocol.metrics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * WebSocket 指标增强器
 *
 * <p>负责收集和统计 WebSocket 协议的性能指标
 * <p>关键指标：
 * 1. 消息吞吐量（每秒消息数）
 * 2. 消息延迟（处理时间）
 * 3. 连接数统计
 * 4. 错误率统计
 * 5. 设备-消息关系统计
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/15
 */
@Component
public class WebSocketMetricsEnhancer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketMetricsEnhancer.class);

    // 全局计数器
    private final AtomicLong totalUpMessages = new AtomicLong(0);
    private final AtomicLong totalDownMessages = new AtomicLong(0);
    private final AtomicLong totalUpErrors = new AtomicLong(0);
    private final AtomicLong totalDownErrors = new AtomicLong(0);
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong currentConnections = new AtomicLong(0);

    // 总延迟统计（用于计算平均值）
    private final AtomicLong totalUpLatency = new AtomicLong(0);
    private final AtomicLong totalDownLatency = new AtomicLong(0);

    // 设备级别的指标
    private final java.util.Map<String, DeviceMetrics> deviceMetrics = new ConcurrentHashMap<>();

    // 处理器级别的指标
    private final java.util.Map<String, ProcessorMetrics> processorMetrics = new ConcurrentHashMap<>();

    // 启动时间
    private long startTime = System.currentTimeMillis();

    /**
     * 记录上行消息
     */
    public void recordUpMessage(String iotId, long latency, boolean success) {
        try {
            totalUpMessages.incrementAndGet();
            totalUpLatency.addAndGet(latency);

            if (!success) {
                totalUpErrors.incrementAndGet();
            }

            // 设备级别统计
            DeviceMetrics metrics = deviceMetrics.computeIfAbsent(iotId, k -> new DeviceMetrics());
            metrics.upMessageCount.incrementAndGet();
            metrics.upLatencyTotal.addAndGet(latency);

            if (latency > metrics.upMaxLatency.get()) {
                metrics.upMaxLatency.set(latency);
            }

            log.trace("[WebSocket指标][上行] IotId: {}, Latency: {}ms, Success: {}",
                    iotId, latency, success);

        } catch (Exception e) {
            log.error("[WebSocket指标][上行记录][异常] 异常: {}", e.getMessage());
        }
    }

    /**
     * 记录下行消息
     */
    public void recordDownMessage(String iotId, long latency, boolean success) {
        try {
            totalDownMessages.incrementAndGet();
            totalDownLatency.addAndGet(latency);

            if (!success) {
                totalDownErrors.incrementAndGet();
            }

            // 设备级别统计
            DeviceMetrics metrics = deviceMetrics.computeIfAbsent(iotId, k -> new DeviceMetrics());
            metrics.downMessageCount.incrementAndGet();
            metrics.downLatencyTotal.addAndGet(latency);

            if (latency > metrics.downMaxLatency.get()) {
                metrics.downMaxLatency.set(latency);
            }

            log.trace("[WebSocket指标][下行] IotId: {}, Latency: {}ms, Success: {}",
                    iotId, latency, success);

        } catch (Exception e) {
            log.error("[WebSocket指标][下行记录][异常] 异常: {}", e.getMessage());
        }
    }

    /**
     * 记录连接事件
     */
    public void recordConnection(String sessionId, String iotId) {
        try {
            totalConnections.incrementAndGet();
            currentConnections.incrementAndGet();

            DeviceMetrics metrics = deviceMetrics.computeIfAbsent(iotId, k -> new DeviceMetrics());
            metrics.connectionCount.incrementAndGet();

            log.debug("[WebSocket指标][连接] SessionId: {}, IotId: {}, 当前连接数: {}",
                    sessionId, iotId, currentConnections.get());

        } catch (Exception e) {
            log.error("[WebSocket指标][连接记录][异常] 异常: {}", e.getMessage());
        }
    }

    /**
     * 记录断开连接事件
     */
    public void recordDisconnection(String sessionId, String iotId) {
        try {
            currentConnections.decrementAndGet();

            DeviceMetrics metrics = deviceMetrics.get(iotId);
            if (metrics != null) {
                metrics.lastDisconnectTime = System.currentTimeMillis();
            }

            log.debug("[WebSocket指标][断开] SessionId: {}, IotId: {}, 当前连接数: {}",
                    sessionId, iotId, currentConnections.get());

        } catch (Exception e) {
            log.error("[WebSocket指标][断开记录][异常] 异常: {}", e.getMessage());
        }
    }

    /**
     * 记录处理器执行时间
     */
    public void recordProcessorMetric(String processorName, long latency, boolean success) {
        try {
            ProcessorMetrics metrics = processorMetrics.computeIfAbsent(processorName,
                    k -> new ProcessorMetrics(processorName));

            metrics.executeCount.incrementAndGet();
            metrics.totalLatency.addAndGet(latency);

            if (!success) {
                metrics.errorCount.incrementAndGet();
            }

            if (latency > metrics.maxLatency.get()) {
                metrics.maxLatency.set(latency);
            }

            log.trace("[WebSocket指标][处理器] Name: {}, Latency: {}ms",
                    processorName, latency);

        } catch (Exception e) {
            log.error("[WebSocket指标][处理器记录][异常] 异常: {}", e.getMessage());
        }
    }

    /**
     * 获取系统指标
     */
    public java.util.Map<String, Object> getSystemMetrics() {
        java.util.Map<String, Object> metrics = new java.util.HashMap<>();

        long upTime = System.currentTimeMillis() - startTime;
        long upMessageCount = totalUpMessages.get();
        long downMessageCount = totalDownMessages.get();

        metrics.put("uptime", upTime);
        metrics.put("totalUpMessages", upMessageCount);
        metrics.put("totalDownMessages", downMessageCount);
        metrics.put("totalErrors", totalUpErrors.get() + totalDownErrors.get());
        metrics.put("currentConnections", currentConnections.get());
        metrics.put("totalConnections", totalConnections.get());

        // 计算吞吐量 (消息/秒)
        double upMessageThroughput = upTime > 0 ? (upMessageCount * 1000.0 / upTime) : 0;
        double downMessageThroughput = upTime > 0 ? (downMessageCount * 1000.0 / upTime) : 0;
        metrics.put("upThroughput", String.format("%.2f msg/s", upMessageThroughput));
        metrics.put("downThroughput", String.format("%.2f msg/s", downMessageThroughput));

        // 计算平均延迟
        double avgUpLatency = upMessageCount > 0 ? (totalUpLatency.get() * 1.0 / upMessageCount) : 0;
        double avgDownLatency = downMessageCount > 0 ? (totalDownLatency.get() * 1.0 / downMessageCount) : 0;
        metrics.put("avgUpLatency", String.format("%.2f ms", avgUpLatency));
        metrics.put("avgDownLatency", String.format("%.2f ms", avgDownLatency));

        // 错误率
        double upErrorRate = upMessageCount > 0 ? (totalUpErrors.get() * 100.0 / upMessageCount) : 0;
        double downErrorRate = downMessageCount > 0 ? (totalDownErrors.get() * 100.0 / downMessageCount) : 0;
        metrics.put("upErrorRate", String.format("%.2f%%", upErrorRate));
        metrics.put("downErrorRate", String.format("%.2f%%", downErrorRate));

        return metrics;
    }

    /**
     * 获取设备指标
     */
    public java.util.Map<String, Object> getDeviceMetrics(String iotId) {
        DeviceMetrics deviceMetric = deviceMetrics.get(iotId);
        if (deviceMetric == null) {
            return java.util.Collections.emptyMap();
        }

        java.util.Map<String, Object> metrics = new java.util.HashMap<>();
        long upCount = deviceMetric.upMessageCount.get();
        long downCount = deviceMetric.downMessageCount.get();

        metrics.put("iotId", iotId);
        metrics.put("upMessageCount", upCount);
        metrics.put("downMessageCount", downCount);
        metrics.put("connectionCount", deviceMetric.connectionCount.get());
        metrics.put("avgUpLatency", upCount > 0 ?
                String.format("%.2f ms", deviceMetric.upLatencyTotal.get() * 1.0 / upCount) : "N/A");
        metrics.put("maxUpLatency", deviceMetric.upMaxLatency.get() + " ms");
        metrics.put("avgDownLatency", downCount > 0 ?
                String.format("%.2f ms", deviceMetric.downLatencyTotal.get() * 1.0 / downCount) : "N/A");
        metrics.put("maxDownLatency", deviceMetric.downMaxLatency.get() + " ms");

        return metrics;
    }

    /**
     * 获取处理器指标
     */
    public java.util.Map<String, Object> getProcessorMetrics(String processorName) {
        ProcessorMetrics procMetric = processorMetrics.get(processorName);
        if (procMetric == null) {
            return java.util.Collections.emptyMap();
        }

        java.util.Map<String, Object> metrics = new java.util.HashMap<>();
        long executeCount = procMetric.executeCount.get();

        metrics.put("processorName", processorName);
        metrics.put("executeCount", executeCount);
        metrics.put("errorCount", procMetric.errorCount.get());
        metrics.put("avgLatency", executeCount > 0 ?
                String.format("%.2f ms", procMetric.totalLatency.get() * 1.0 / executeCount) : "N/A");
        metrics.put("maxLatency", procMetric.maxLatency.get() + " ms");
        metrics.put("errorRate", executeCount > 0 ?
                String.format("%.2f%%", procMetric.errorCount.get() * 100.0 / executeCount) : "0%");

        return metrics;
    }

    /**
     * 获取所有设备指标
     */
    public java.util.List<java.util.Map<String, Object>> getAllDeviceMetrics() {
        return deviceMetrics.entrySet().stream()
                .map(entry -> getDeviceMetrics(entry.getKey()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取所有处理器指标
     */
    public java.util.List<java.util.Map<String, Object>> getAllProcessorMetrics() {
        return processorMetrics.entrySet().stream()
                .map(entry -> getProcessorMetrics(entry.getKey()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 重置所有指标
     */
    public void reset() {
        try {
            totalUpMessages.set(0);
            totalDownMessages.set(0);
            totalUpErrors.set(0);
            totalDownErrors.set(0);
            totalConnections.set(0);
            currentConnections.set(0);
            totalUpLatency.set(0);
            totalDownLatency.set(0);
            deviceMetrics.clear();
            processorMetrics.clear();
            startTime = System.currentTimeMillis();

            log.info("[WebSocket指标][重置] 所有指标已重置");

        } catch (Exception e) {
            log.error("[WebSocket指标][重置][异常] 异常: {}", e.getMessage());
        }
    }

    /**
     * 打印统计摘要
     */
    public void printSummary() {
        try {
            java.util.Map<String, Object> sysMetrics = getSystemMetrics();

            StringBuilder sb = new StringBuilder();
            sb.append("\n========== WebSocket 指标统计 ==========\n");
            sb.append(String.format("运行时长: %dms\n", sysMetrics.get("uptime")));
            sb.append(String.format("上行消息: %s\n", sysMetrics.get("totalUpMessages")));
            sb.append(String.format("下行消息: %s\n", sysMetrics.get("totalDownMessages")));
            sb.append(String.format("错误消息: %s\n", sysMetrics.get("totalErrors")));
            sb.append(String.format("当前连接: %s\n", sysMetrics.get("currentConnections")));
            sb.append(String.format("历史连接: %s\n", sysMetrics.get("totalConnections")));
            sb.append(String.format("上行吞吐: %s\n", sysMetrics.get("upThroughput")));
            sb.append(String.format("下行吞吐: %s\n", sysMetrics.get("downThroughput")));
            sb.append(String.format("平均上行延迟: %s\n", sysMetrics.get("avgUpLatency")));
            sb.append(String.format("平均下行延迟: %s\n", sysMetrics.get("avgDownLatency")));
            sb.append(String.format("上行错误率: %s\n", sysMetrics.get("upErrorRate")));
            sb.append(String.format("下行错误率: %s\n", sysMetrics.get("downErrorRate")));
            sb.append("====================================\n");

            log.info(sb.toString());

        } catch (Exception e) {
            log.error("[WebSocket指标][打印][异常] 异常: {}", e.getMessage());
        }
    }

    /**
     * 设备级别的指标容器
     */
    private static class DeviceMetrics {
        final AtomicLong upMessageCount = new AtomicLong(0);
        final AtomicLong downMessageCount = new AtomicLong(0);
        final AtomicLong connectionCount = new AtomicLong(0);
        final AtomicLong upLatencyTotal = new AtomicLong(0);
        final AtomicLong downLatencyTotal = new AtomicLong(0);
        final AtomicLong upMaxLatency = new AtomicLong(0);
        final AtomicLong downMaxLatency = new AtomicLong(0);
        long lastDisconnectTime = 0;
    }

    /**
     * 处理器级别的指标容器
     */
    private static class ProcessorMetrics {
        final String name;
        final AtomicLong executeCount = new AtomicLong(0);
        final AtomicLong errorCount = new AtomicLong(0);
        final AtomicLong totalLatency = new AtomicLong(0);
        final AtomicLong maxLatency = new AtomicLong(0);

        ProcessorMetrics(String name) {
            this.name = name;
        }
    }
}
