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

package cn.universal.databridge.manager;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.databridge.engine.TemplateEngine;
import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.PluginInfo;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.databridge.enums.PluginStatus;
import cn.universal.databridge.logger.DataBridgeLogger;
import cn.universal.databridge.plugin.DataBridgePlugin;
import cn.universal.databridge.plugin.DataOutputPlugin;
import cn.universal.databridge.service.DataBridgeConfigService;
import cn.universal.databridge.service.ResourceConnectionService;
import cn.universal.persistence.base.BaseUPRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据桥接管理器
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Component
@Slf4j
public class DataBridgeManager {

    @Autowired(required = false)
    private Map<String, DataBridgePlugin> bridgePlugins;

    @Resource
    private ResourceConnectionService resourceConnectionService;

    @Resource
    private DataBridgeConfigService dataBridgeConfigService;

    @Resource
    private DataBridgeLogger dataBridgeLogger;

    @Resource
    private TemplateEngine templateEngine;

    /**
     * 处理设备数据
     */
    @Async("taskExecutor")
    public void processDeviceData(List<BaseUPRequest> requests) {
        if (CollectionUtil.isEmpty(requests)) {
            return;
        }

        try {
            // 按产品分组处理
            Map<String, List<BaseUPRequest>> productGroups =
                    requests.stream().collect(Collectors.groupingBy(BaseUPRequest::getProductKey));

            productGroups.forEach(this::processProductData);

        } catch (Exception e) {
            log.error("数据桥接处理异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理单个产品的数据
     */
    private void processProductData(String productKey, List<BaseUPRequest> requests) {
        try {
            // 1. 获取产品的桥接配置
            List<DataBridgeConfig> configs =
                    dataBridgeConfigService.getActiveConfigsByProductKey(productKey);

            if (CollectionUtil.isNotEmpty(configs)) {
                // 2. 并行处理所有桥接配置
                configs.parallelStream()
                        .forEach(
                                config -> {
                                    try {
                                        processWithConfig(requests, config);
                                    } catch (Exception e) {
                                        dataBridgeLogger.logExecution(
                                                config.getName(),
                                                "FAILED",
                                                requests.size(),
                                                requests.size(),
                                                e.getMessage(),
                                                0);
                                    }
                                });
            }
            requests.parallelStream().forEach(request -> {
                String deviceId = request.getDeviceId();
                List<DataBridgeConfig> configList =
                        dataBridgeConfigService.getActiveConfigsByProductKeyAndDeviceId(productKey, deviceId);
                if (CollectionUtil.isEmpty(configList)) {
                    return;
                }
                configList.parallelStream()
                        .forEach(
                                config -> {
                                    try {
                                        processWithConfig(requests, config);
                                    } catch (Exception e) {
                                        dataBridgeLogger.logExecution(
                                                config.getName(),
                                                "FAILED",
                                                requests.size(),
                                                requests.size(),
                                                e.getMessage(),
                                                0);
                                    }
                                });
            });

        } catch (Exception e) {
            log.error("产品数据桥接处理异常，产品: {}, 错误: {}", productKey, e.getMessage(), e);
        }
    }

    /**
     * 使用指定配置处理数据
     */
    private void processWithConfig(List<BaseUPRequest> requests, DataBridgeConfig config) {
        long startTime = System.currentTimeMillis();
        int processedCount = 0;
        int failedCount = 0;
        String errorMessage = null;

        try {
            // 1. 获取目标资源连接
            ResourceConnection connection =
                    resourceConnectionService.getResouceForRunning(config.getTargetResourceId());
            if (connection == null || connection.getStatus() != 1) {
                throw new RuntimeException("资源连接不存在或已禁用");
            }

            // 2. 获取对应的插件
            // 优先使用资源连接的插件类型，如果没有则使用配置的桥接类型
            String pluginType = connection.getPluginType();
            if (pluginType == null || pluginType.trim().isEmpty()) {
                pluginType = config.getBridgeType().name();
            }

            DataBridgePlugin plugin = bridgePlugins.get(pluginType);
            if (plugin == null) {
                throw new RuntimeException("未找到对应的桥接插件: " + pluginType);
            }

            // 3. 过滤数据
            List<BaseUPRequest> filteredRequests = filterRequests(requests, config);
            if (CollectionUtil.isEmpty(filteredRequests)) {
                return;
            }

            // 4. 执行数据处理
            // TODO: 需要重新设计数据处理逻辑，因为processData和batchProcessData方法已被移除
            // 现在应该使用DataOutputPlugin的batchProcessOutput方法
            if (plugin instanceof DataOutputPlugin) {
                ((DataOutputPlugin) plugin).batchProcessOutput(filteredRequests, config, connection);
            } else {
                log.warn("插件不支持输出处理: {}", config.getBridgeType());
            }

            processedCount = filteredRequests.size();

        } catch (Exception e) {
            failedCount = requests.size();
            errorMessage = e.getMessage();
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            String status = failedCount > 0 ? "FAILED" : "SUCCESS";
            dataBridgeLogger.logExecution(
                    config.getName(), status, processedCount, failedCount, errorMessage, executionTime);
        }
    }

    /**
     * 过滤请求
     */
    private List<BaseUPRequest> filterRequests(
            List<BaseUPRequest> requests, DataBridgeConfig config) {
        // TODO: 需要从统一配置config JSON中解析过滤条件，因为getFilterConditions()方法已被移除
        if (StrUtil.isBlank(config.getConfig())) {
            return requests;
        }

        try {
            JSONObject configJson = JSONUtil.parseObj(config.getConfig());
            if (!configJson.containsKey("filter_conditions")) {
                return requests;
            }

            JSONObject conditions = configJson.getJSONObject("filter_conditions");
            return requests.stream()
                    .filter(request -> evaluateConditions(request, conditions))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("过滤条件评估失败: {}", e.getMessage(), e);
            return requests;
        }
    }

    /**
     * 评估过滤条件
     */
    private boolean evaluateConditions(BaseUPRequest request, JSONObject conditions) {
        try {
            // 消息类型过滤
            if (conditions.containsKey("messageType")) {
                List<String> allowedTypes = conditions.getBeanList("messageType", String.class);
                if (!allowedTypes.contains(request.getMessageType().name())) {
                    return false;
                }
            }

            // 属性值过滤
            if (conditions.containsKey("properties") && request.getProperties() != null) {
                JSONObject propertyConditions = conditions.getJSONObject("properties");
                for (String key : propertyConditions.keySet()) {
                    Object expectedValue = propertyConditions.get(key);
                    Object actualValue = request.getProperties().get(key);

                    if (!isValueMatch(actualValue, expectedValue)) {
                        return false;
                    }
                }
            }

            return true;
        } catch (Exception e) {
            log.error("过滤条件评估异常: {}", e.getMessage(), e);
            return true; // 评估失败时不过滤
        }
    }

    /**
     * 判断值是否匹配
     */
    private boolean isValueMatch(Object actualValue, Object expectedValue) {
        if (actualValue == null && expectedValue == null) {
            return true;
        }
        if (actualValue == null || expectedValue == null) {
            return false;
        }

        String expectedStr = expectedValue.toString();

        // 支持比较操作符
        if (expectedStr.startsWith(">")) {
            return compareValues(actualValue, expectedStr.substring(1), ">");
        } else if (expectedStr.startsWith("<")) {
            return compareValues(actualValue, expectedStr.substring(1), "<");
        } else if (expectedStr.startsWith(">=")) {
            return compareValues(actualValue, expectedStr.substring(2), ">=");
        } else if (expectedStr.startsWith("<=")) {
            return compareValues(actualValue, expectedStr.substring(2), "<=");
        } else if (expectedStr.startsWith("!=")) {
            return !actualValue.toString().equals(expectedStr.substring(2));
        } else {
            return actualValue.toString().equals(expectedStr);
        }
    }

    /**
     * 比较数值
     */
    private boolean compareValues(Object actualValue, String expectedStr, String operator) {
        try {
            double actual = Double.parseDouble(actualValue.toString());
            double expected = Double.parseDouble(expectedStr);

            switch (operator) {
                case ">":
                    return actual > expected;
                case "<":
                    return actual < expected;
                case ">=":
                    return actual >= expected;
                case "<=":
                    return actual <= expected;
                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 获取插件状态
     */
    public Map<String, PluginStatus> getPluginStatuses() {
        Map<String, PluginStatus> statuses = new HashMap<>();
        if (bridgePlugins != null) {
            bridgePlugins.forEach(
                    (name, plugin) -> {
                        try {
                            // TODO: 需要重新设计插件状态获取逻辑，因为getStatus()方法已被移除
                            statuses.put(name, PluginStatus.RUNNING); // 临时使用默认状态
                        } catch (Exception e) {
                            log.error("获取插件状态失败: {}", e.getMessage(), e);
                            statuses.put(name, PluginStatus.ERROR);
                        }
                    });
        }
        return statuses;
    }

    /**
     * 获取插件信息（直接从插件实现获取）
     */
    public Map<String, PluginInfo> getPluginInfos() {
        Map<String, PluginInfo> allPluginInfos = new HashMap<>();

        // 直接从插件实现获取 PluginInfo
        if (bridgePlugins != null) {
            bridgePlugins.forEach(
                    (type, plugin) -> {
                        try {
                            PluginInfo pluginInfo = plugin.getPluginInfo();
                            if (pluginInfo != null) {
                                allPluginInfos.put(pluginInfo.getPluginType(), pluginInfo);
                            }
                        } catch (Exception e) {
                            log.error("获取插件信息失败: {}, 错误: {}", type, e.getMessage(), e);
                        }
                    });
        }

        return allPluginInfos;
    }

    /**
     * 测试资源连接
     */
    public Boolean testResourceConnection(Long resourceId) {
        try {
            ResourceConnection connection = resourceConnectionService.getById(resourceId);
            if (connection == null) {
                return false;
            }

            DataBridgePlugin plugin = bridgePlugins.get(connection.getType().name().toLowerCase());
            if (plugin == null) {
                return false;
            }

            Boolean result = plugin.testConnection(connection);
            dataBridgeLogger.logConnectionTest(
                    connection.getName(), connection.getType().name(), result, result ? "连接成功" : "连接失败");

            return result;
        } catch (Exception e) {
            log.error("测试资源连接失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 验证桥接配置
     */
    public Boolean validateBridgeConfig(DataBridgeConfig config) {
        try {
            DataBridgePlugin plugin = bridgePlugins.get(config.getBridgeType().name().toUpperCase());
            if (plugin == null) {
                return false;
            }

            Boolean result = plugin.validateConfig(config);
            dataBridgeLogger.logConfigValidation(config.getName(), result, result ? "验证成功" : "验证失败");

            return result;
        } catch (Exception e) {
            log.error("验证桥接配置失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取支持双向的插件类型
     */
    public List<String> getBidirectionalPluginTypes() {
        return getPluginInfos().values().stream()
                .filter(info -> info.getDataDirection() == PluginInfo.DataDirection.BIDIRECTIONAL)
                .map(PluginInfo::getPluginType)
                .collect(Collectors.toList());
    }

    /**
     * 获取仅支持输出的插件类型
     */
    public List<String> getOutputOnlyPluginTypes() {
        return getPluginInfos().values().stream()
                .filter(info -> info.getDataDirection() == PluginInfo.DataDirection.OUTPUT)
                .map(PluginInfo::getPluginType)
                .collect(Collectors.toList());
    }

    /**
     * 获取仅支持输入的插件类型
     */
    public List<String> getInputOnlyPluginTypes() {
        return getPluginInfos().values().stream()
                .filter(info -> info.getDataDirection() == PluginInfo.DataDirection.INPUT)
                .map(PluginInfo::getPluginType)
                .collect(Collectors.toList());
    }
}
