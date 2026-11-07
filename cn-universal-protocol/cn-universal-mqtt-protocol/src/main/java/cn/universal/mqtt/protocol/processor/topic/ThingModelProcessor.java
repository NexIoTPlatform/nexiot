/// *
// *
// * Copyright (c) 2025, NexIoT. All Rights Reserved.
// *
// * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
// * @Author: gitee.com/NexIoT
// * @Email: wo8335224@gmail.com
// * @Wechat: outlookFil
// *
// *
// */
//
// package cn.universal.protocol.mqtt.v2.processor.topic;
//
// import entity.cn.universal.protocol.mqtt.MQTTUPRequest;
// import processor.cn.universal.protocol.mqtt.MqttMessageProcessor;
// import topic.cn.universal.protocol.mqtt.MQTTTopicManager;
// import topic.cn.universal.protocol.mqtt.MQTTTopicType;
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;
//
/// **
// * 物模型处理器
// * <p>
// * 专门处理物模型相关的MQTT消息：
// * - 属性上报 ($thing/up/property/${productKey}/${deviceId})
// * - 事件上报 ($thing/up/event/${productKey}/${deviceId})
// * - 下行控制 ($thing/down/${productKey}/${deviceId})
// *
// * @Author gitee.com/NexIoT
// * @version 1.0
// * @since 2025/1/20
// */
// @Slf4j(topic = "mqtt")
// @Component
// public class ThingModelProcessor implements MqttMessageProcessor {
//
//    @Autowired
//    private MQTTTopicManager topicManager;
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    @Override
//    public String getName() {
//        return "物模型处理器";
//    }
//
//    @Override
//    public String getDescription() {
//        return "处理设备属性上报、事件上报和下行控制消息";
//    }
//
//    @Override
//    public int getOrder() {
//        return 100; // 物模型处理优先级较高
//    }
//
//    @Override
//    public boolean supports(MQTTUPRequest request) {
//        MQTTTopicManager.TopicInfo topicInfo = topicManager.extractTopicInfo(request.getTopic());
//        return topicInfo.isValid() && topicInfo.getCategory() ==
// MqttConstant.TopicCategory.THING_MODEL;
//    }
//
//    @Override
//    public ProcessorResult process(MQTTUPRequest request) {
//        try {
//            MQTTTopicManager.TopicInfo topicInfo =
// topicManager.extractTopicInfo(request.getTopic());
//
//            log.debug("[{}] 开始处理物模型消息 - 类型: {}, 设备: {}",
//                getName(), topicInfo.getTopicType(), topicInfo.getDeviceUniqueId());
//
//            // 根据具体的物模型类型分发处理
//            switch (topicInfo.getTopicType()) {
//                case THING_PROPERTY_UP:
//                    return processPropertyReport(request, topicInfo);
//
//                case THING_EVENT_UP:
//                    return processEventReport(request, topicInfo);
//
//                case THING_DOWN:
//                    return processDownstreamControl(request, topicInfo);
//
//                default:
//                    log.warn("[{}] 不支持的物模型类型: {}", getName(), topicInfo.getTopicType());
//                    return ProcessorResult.CONTINUE;
//            }
//
//        } catch (Exception e) {
//            log.error("[{}] 物模型消息处理异常: ", getName(), e);
//            return ProcessorResult.ERROR;
//        }
//    }
//
//    /**
//     * 处理属性上报
//     */
//    private ProcessorResult processPropertyReport(MQTTUPRequest request,
// MQTTTopicManager.TopicInfo topicInfo) {
//        try {
//            String payload = request.getMessageContentAsString();
//            JsonNode propertyData = objectMapper.readTree(payload);
//
//            log.info("[{}] 处理属性上报 - 设备: {}, 属性数量: {}",
//                getName(), topicInfo.getDeviceUniqueId(), propertyData.size());
//
//            // 设置上下文信息
//            request.setContextValue("messageType", "PROPERTY_REPORT");
//            request.setContextValue("topicType", "THING_PROPERTY_UP");
//            request.setContextValue("propertyCount", propertyData.size());
//
//            // 解析和验证属性数据
//            if (validatePropertyData(propertyData)) {
//                // 存储属性数据到上下文
//                request.setContextValue("propertyData", propertyData);
//
//                // 记录处理统计
//                incrementPropertyCounter(topicInfo.getProductKey());
//
//                log.debug("[{}] 属性上报处理成功 - 设备: {}", getName(), topicInfo.getDeviceUniqueId());
//                return ProcessorResult.CONTINUE;
//            } else {
//                log.warn("[{}] 属性数据验证失败 - 设备: {}", getName(), topicInfo.getDeviceUniqueId());
//                request.setContextValue("validationError", "属性数据格式不正确");
//                return ProcessorResult.CONTINUE;
//            }
//
//        } catch (Exception e) {
//            log.error("[{}] 属性上报处理异常 - 设备: {}, 异常: ",
//                getName(), topicInfo.getDeviceUniqueId(), e);
//            return ProcessorResult.ERROR;
//        }
//    }
//
//    /**
//     * 处理事件上报
//     */
//    private ProcessorResult processEventReport(MQTTUPRequest request, MQTTTopicManager.TopicInfo
// topicInfo) {
//        try {
//            String payload = request.getMessageContentAsString();
//            JsonNode eventData = objectMapper.readTree(payload);
//
//            log.info("[{}] 处理事件上报 - 设备: {}, 事件: {}",
//                getName(), topicInfo.getDeviceUniqueId(),
// eventData.path("eventType").asText("unknown"));
//
//            // 设置上下文信息
//            request.setContextValue("messageType", "EVENT_REPORT");
//            request.setContextValue("topicType", "THING_EVENT_UP");
//            request.setContextValue("eventType", eventData.path("eventType").asText());
//            request.setContextValue("eventLevel", eventData.path("level").asText("INFO"));
//
//            // 解析事件数据
//            if (validateEventData(eventData)) {
//                // 存储事件数据到上下文
//                request.setContextValue("eventData", eventData);
//
//                // 检查是否需要告警
//                String eventLevel = eventData.path("level").asText("INFO");
//                if ("ERROR".equals(eventLevel) || "CRITICAL".equals(eventLevel)) {
//                    request.setContextValue("needAlert", true);
//                    log.warn("[{}] 高级别事件上报 - 设备: {}, 级别: {}",
//                        getName(), topicInfo.getDeviceUniqueId(), eventLevel);
//                }
//
//                // 记录处理统计
//                incrementEventCounter(topicInfo.getProductKey(), eventLevel);
//
//                log.debug("[{}] 事件上报处理成功 - 设备: {}", getName(), topicInfo.getDeviceUniqueId());
//                return ProcessorResult.CONTINUE;
//            } else {
//                log.warn("[{}] 事件数据验证失败 - 设备: {}", getName(), topicInfo.getDeviceUniqueId());
//                request.setContextValue("validationError", "事件数据格式不正确");
//                return ProcessorResult.CONTINUE;
//            }
//
//        } catch (Exception e) {
//            log.error("[{}] 事件上报处理异常 - 设备: {}, 异常: ",
//                getName(), topicInfo.getDeviceUniqueId(), e);
//            return ProcessorResult.ERROR;
//        }
//    }
//
//    /**
//     * 处理下行控制
//     */
//    private ProcessorResult processDownstreamControl(MQTTUPRequest request,
// MQTTTopicManager.TopicInfo topicInfo) {
//        try {
//            String payload = request.getMessageContentAsString();
//            JsonNode controlData = objectMapper.readTree(payload);
//
//            log.info("[{}] 处理下行控制 - 设备: {}, 命令: {}",
//                getName(), topicInfo.getDeviceUniqueId(),
// controlData.path("command").asText("unknown"));
//
//            // 设置上下文信息
//            request.setContextValue("messageType", "DOWNSTREAM_CONTROL");
//            request.setContextValue("topicType", "THING_DOWN");
//            request.setContextValue("command", controlData.path("command").asText());
//            request.setContextValue("commandId", controlData.path("commandId").asText());
//
//            // 解析控制指令
//            if (validateControlData(controlData)) {
//                // 存储控制数据到上下文
//                request.setContextValue("controlData", controlData);
//
//                // 记录控制指令
//                String command = controlData.path("command").asText();
//                log.info("[{}] 下行控制指令 - 设备: {}, 命令: {}",
//                    getName(), topicInfo.getDeviceUniqueId(), command);
//
//                // 记录处理统计
//                incrementControlCounter(topicInfo.getProductKey(), command);
//
//                // 标记需要回复确认
//                request.setContextValue("needReply", true);
//                request.setContextValue("replyTopic", buildReplyTopic(topicInfo));
//
//                log.debug("[{}] 下行控制处理成功 - 设备: {}", getName(), topicInfo.getDeviceUniqueId());
//                return ProcessorResult.CONTINUE;
//            } else {
//                log.warn("[{}] 控制数据验证失败 - 设备: {}", getName(), topicInfo.getDeviceUniqueId());
//                request.setContextValue("validationError", "控制数据格式不正确");
//                return ProcessorResult.CONTINUE;
//            }
//
//        } catch (Exception e) {
//            log.error("[{}] 下行控制处理异常 - 设备: {}, 异常: ",
//                getName(), topicInfo.getDeviceUniqueId(), e);
//            return ProcessorResult.ERROR;
//        }
//    }
//
//    /**
//     * 验证属性数据格式
//     */
//    private boolean validatePropertyData(JsonNode propertyData) {
//        // 基本验证：必须是对象类型且非空
//        if (!propertyData.isObject() || propertyData.size() == 0) {
//            return false;
//        }
//
//        // 验证每个属性是否包含必要字段
//        return propertyData.fields().hasNext();
//    }
//
//    /**
//     * 验证事件数据格式
//     */
//    private boolean validateEventData(JsonNode eventData) {
//        // 基本验证：必须包含eventType字段
//        if (!eventData.isObject() || !eventData.has("eventType")) {
//            return false;
//        }
//
//        String eventType = eventData.path("eventType").asText();
//        return eventType != null && !eventType.trim().isEmpty();
//    }
//
//    /**
//     * 验证控制数据格式
//     */
//    private boolean validateControlData(JsonNode controlData) {
//        // 基本验证：必须包含command字段
//        if (!controlData.isObject() || !controlData.has("command")) {
//            return false;
//        }
//
//        String command = controlData.path("command").asText();
//        return command != null && !command.trim().isEmpty();
//    }
//
//    /**
//     * 构建回复主题
//     */
//    private String buildReplyTopic(MQTTTopicManager.TopicInfo topicInfo) {
//        // 下行控制的回复主题：$thing/up/reply/${productKey}/${deviceId}
//        return "$thing/up/reply/" + topicInfo.getProductKey() + "/" + topicInfo.getDeviceId();
//    }
//
//    /**
//     * 增加属性计数器
//     */
//    private void incrementPropertyCounter(String productKey) {
//        // TODO: 实现属性统计逻辑
//        log.debug("[{}] 属性计数器增加 - 产品: {}", getName(), productKey);
//    }
//
//    /**
//     * 增加事件计数器
//     */
//    private void incrementEventCounter(String productKey, String eventLevel) {
//        // TODO: 实现事件统计逻辑
//        log.debug("[{}] 事件计数器增加 - 产品: {}, 级别: {}", getName(), productKey, eventLevel);
//    }
//
//    /**
//     * 增加控制计数器
//     */
//    private void incrementControlCounter(String productKey, String command) {
//        // TODO: 实现控制统计逻辑
//        log.debug("[{}] 控制计数器增加 - 产品: {}, 命令: {}", getName(), productKey, command);
//    }
//
//    @Override
//    public boolean preCheck(MQTTUPRequest request) {
//        return supports(request);
//    }
//
//    @Override
//    public void postProcess(MQTTUPRequest request, ProcessorResult result) {
//        if (result == ProcessorResult.CONTINUE) {
//            log.debug("[{}] 物模型处理完成 - 设备: {}", getName(), request.getDeviceUniqueId());
//        } else {
//            log.warn("[{}] 物模型处理异常 - 设备: {}, 结果: {}",
//                getName(), request.getDeviceUniqueId(), result);
//        }
//    }
//
//    @Override
//    public void onError(MQTTUPRequest request, Exception e) {
//        log.error("[{}] 物模型处理器异常 - 设备: {}, 异常: ",
//            getName(), request.getDeviceUniqueId(), e);
//        request.setError("物模型处理失败: " + e.getMessage());
//    }
//
//    @Override
//    public int getPriority() {
//        // 物模型处理优先级较高
//        return 10;
//    }
// }
