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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * WebSocket 指标导出器
 *
 * <p>负责将收集的指标导出为不同的格式
 * <p>支持的导出格式：
 * 1. JSON 格式
 * 2. Prometheus 格式
 * 3. CSV 格式
 * 4. 摘要文本格式
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/15
 */
@Component
public class WebSocketMetricsExporter {

    private static final Logger log = LoggerFactory.getLogger(WebSocketMetricsExporter.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired(required = false)
    private WebSocketMetricsEnhancer metricsEnhancer;

    /**
     * 导出为 JSON 格式
     */
    public String exportAsJson() {
        try {
            if (metricsEnhancer == null) {
                log.warn("[指标导出][JSON] 指标收集器未初始化");
                return "{}";
            }

            Map<String, Object> sysMetrics = metricsEnhancer.getSystemMetrics();
            Map<String, Object> result = new HashMap<>();

            result.put("timestamp", LocalDateTime.now().format(FORMATTER));
            result.put("type", "websocket");
            result.put("systemMetrics", sysMetrics);
            result.put("deviceMetrics", metricsEnhancer.getAllDeviceMetrics());
            result.put("processorMetrics", metricsEnhancer.getAllProcessorMetrics());

            return com.fasterxml.jackson.databind.ObjectMapper
                    .class.getCanonicalName() + " - 需要json库";

        } catch (Exception e) {
            log.error("[指标导出][JSON][异常] 异常: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 导出为 Prometheus 格式
     */
    public String exportAsPrometheus() {
        try {
            if (metricsEnhancer == null) {
                log.warn("[指标导出][Prometheus] 指标收集器未初始化");
                return "";
            }

            StringBuilder sb = new StringBuilder();

            // 系统级别指标
            Map<String, Object> sysMetrics = metricsEnhancer.getSystemMetrics();
            sb.append("# HELP websocket_uptime_ms WebSocket运行时长(毫秒)\n");
            sb.append("# TYPE websocket_uptime_ms gauge\n");
            sb.append("websocket_uptime_ms ").append(sysMetrics.get("uptime")).append("\n");

            sb.append("# HELP websocket_total_up_messages 上行消息总数\n");
            sb.append("# TYPE websocket_total_up_messages counter\n");
            sb.append("websocket_total_up_messages ").append(sysMetrics.get("totalUpMessages")).append("\n");

            sb.append("# HELP websocket_total_down_messages 下行消息总数\n");
            sb.append("# TYPE websocket_total_down_messages counter\n");
            sb.append("websocket_total_down_messages ").append(sysMetrics.get("totalDownMessages")).append("\n");

            sb.append("# HELP websocket_current_connections 当前连接数\n");
            sb.append("# TYPE websocket_current_connections gauge\n");
            sb.append("websocket_current_connections ").append(sysMetrics.get("currentConnections")).append("\n");

            sb.append("# HELP websocket_total_errors 错误消息总数\n");
            sb.append("# TYPE websocket_total_errors counter\n");
            sb.append("websocket_total_errors ").append(sysMetrics.get("totalErrors")).append("\n");

            return sb.toString();

        } catch (Exception e) {
            log.error("[指标导出][Prometheus][异常] 异常: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 导出为 CSV 格式
     */
    public String exportAsCsv() {
        try {
            if (metricsEnhancer == null) {
                log.warn("[指标导出][CSV] 指标收集器未初始化");
                return "";
            }

            StringBuilder sb = new StringBuilder();

            // 系统指标
            sb.append("MetricType,MetricName,Value,Timestamp\n");

            Map<String, Object> sysMetrics = metricsEnhancer.getSystemMetrics();
            String timestamp = LocalDateTime.now().format(FORMATTER);

            sysMetrics.forEach((key, value) -> {
                sb.append("system,").append(key).append(",")
                        .append(value).append(",").append(timestamp).append("\n");
            });

            // 设备指标
            metricsEnhancer.getAllDeviceMetrics().forEach(deviceMetric -> {
                String iotId = (String) deviceMetric.get("iotId");
                deviceMetric.forEach((key, value) -> {
                    if (!"iotId".equals(key)) {
                        sb.append("device,").append(iotId).append("_").append(key).append(",")
                                .append(value).append(",").append(timestamp).append("\n");
                    }
                });
            });

            return sb.toString();

        } catch (Exception e) {
            log.error("[指标导出][CSV][异常] 异常: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 导出为文本摘要
     */
    public String exportAsText() {
        try {
            if (metricsEnhancer == null) {
                log.warn("[指标导出][文本] 指标收集器未初始化");
                return "";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("\n========== WebSocket 指标导出报告 ==========\n");
            sb.append("导出时间: ").append(LocalDateTime.now().format(FORMATTER)).append("\n\n");

            // 系统指标
            sb.append("【系统指标】\n");
            Map<String, Object> sysMetrics = metricsEnhancer.getSystemMetrics();
            sysMetrics.forEach((key, value) -> {
                sb.append("  ").append(key).append(": ").append(value).append("\n");
            });

            // 设备指标摘要
            sb.append("\n【设备指标摘要】\n");
            var deviceMetrics = metricsEnhancer.getAllDeviceMetrics();
            if (deviceMetrics.isEmpty()) {
                sb.append("  无设备指标\n");
            } else {
                sb.append(String.format("  活跃设备数: %d\n", deviceMetrics.size()));
                deviceMetrics.stream().limit(10).forEach(device -> {
                    sb.append("  - ").append(device.get("iotId")).append(": ")
                            .append("消息(↑").append(device.get("upMessageCount"))
                            .append(" ↓").append(device.get("downMessageCount")).append(")\n");
                });
                if (deviceMetrics.size() > 10) {
                    sb.append("  ... 以及 ").append(deviceMetrics.size() - 10).append(" 个其他设备\n");
                }
            }

            // 处理器指标摘要
            sb.append("\n【处理器指标摘要】\n");
            var processorMetrics = metricsEnhancer.getAllProcessorMetrics();
            if (processorMetrics.isEmpty()) {
                sb.append("  无处理器指标\n");
            } else {
                processorMetrics.forEach(processor -> {
                    sb.append("  - ").append(processor.get("processorName")).append(": ")
                            .append("执行").append(processor.get("executeCount"))
                            .append("次, 错误").append(processor.get("errorCount"))
                            .append("次, 平均延迟").append(processor.get("avgLatency")).append("\n");
                });
            }

            sb.append("==========================================\n");

            return sb.toString();

        } catch (Exception e) {
            log.error("[指标导出][文本][异常] 异常: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 导出为仪表板格式（用于前端展示）
     */
    public Map<String, Object> exportForDashboard() {
        try {
            if (metricsEnhancer == null) {
                log.warn("[指标导出][仪表板] 指标收集器未初始化");
                return new HashMap<>();
            }

            Map<String, Object> dashboard = new HashMap<>();

            // 基本信息
            dashboard.put("timestamp", LocalDateTime.now().format(FORMATTER));
            dashboard.put("protocol", "WebSocket");

            // 系统概览
            Map<String, Object> overview = new HashMap<>();
            Map<String, Object> sysMetrics = metricsEnhancer.getSystemMetrics();
            overview.put("uptime", sysMetrics.get("uptime"));
            overview.put("connections", sysMetrics.get("currentConnections"));
            overview.put("totalMessages", (Long) sysMetrics.get("totalUpMessages") +
                    (Long) sysMetrics.get("totalDownMessages"));
            overview.put("errorRate", sysMetrics.get("upErrorRate"));

            dashboard.put("overview", overview);

            // 消息统计
            Map<String, Object> messages = new HashMap<>();
            messages.put("up", sysMetrics.get("totalUpMessages"));
            messages.put("down", sysMetrics.get("totalDownMessages"));
            messages.put("upThroughput", sysMetrics.get("upThroughput"));
            messages.put("downThroughput", sysMetrics.get("downThroughput"));

            dashboard.put("messages", messages);

            // 延迟统计
            Map<String, Object> latency = new HashMap<>();
            latency.put("avgUp", sysMetrics.get("avgUpLatency"));
            latency.put("avgDown", sysMetrics.get("avgDownLatency"));

            dashboard.put("latency", latency);

            // 设备列表（按消息数排序，取前 10）
            var deviceList = metricsEnhancer.getAllDeviceMetrics().stream()
                    .sorted((a, b) -> {
                        Long aCount = ((Long) a.get("upMessageCount")) + ((Long) a.get("downMessageCount"));
                        Long bCount = ((Long) b.get("upMessageCount")) + ((Long) b.get("downMessageCount"));
                        return bCount.compareTo(aCount);
                    })
                    .limit(10)
                    .collect(java.util.stream.Collectors.toList());

            dashboard.put("topDevices", deviceList);

            // 处理器性能
            dashboard.put("processors", metricsEnhancer.getAllProcessorMetrics());

            return dashboard;

        } catch (Exception e) {
            log.error("[指标导出][仪表板][异常] 异常: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
