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
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;
//
/// **
// * 透传处理器
// * <p>
// * 专门处理透传级MQTT消息：
// * - 透传协议上行 ($thing/up/${productKey}/${deviceId})
// * - 透传协议下行 ($thing/down/${productKey}/${deviceId})
// * <p>
// * 透传消息不做业务逻辑解析，直接转发给相应的处理链
// *
// * @Author gitee.com/NexIoT
// * @version 1.0
// * @since 2025/1/20
// */
// @Slf4j(topic = "mqtt")
// @Component
// public class PassthroughProcessor implements MqttMessageProcessor {
//
//    @Autowired
//    private MQTTTopicManager topicManager;
//
//    @Override
//    public String getName() {
//        return "透传处理器";
//    }
//
//    @Override
//    public String getDescription() {
//        return "处理透传协议上行和下行消息，不做业务逻辑解析";
//    }
//
//    @Override
//    public int getOrder() {
//        return 300; // 透传处理优先级较低
//    }
//
//    @Override
//    public boolean supports(MQTTUPRequest request) {
//        MQTTTopicManager.TopicInfo topicInfo = topicManager.extractTopicInfo(request.getTopic());
//        return topicInfo.isValid() && topicInfo.getCategory() ==
// MqttConstant.TopicCategory.PASSTHROUGH;
//    }
//
//    @Override
//    public ProcessorResult process(MQTTUPRequest request) {
//        try {
//            MQTTTopicManager.TopicInfo topicInfo =
// topicManager.extractTopicInfo(request.getTopic());
//
//            log.debug("[{}] 开始处理透传消息 - 类型: {}, 设备: {}",
//                getName(), topicInfo.getTopicType(), topicInfo.getDeviceUniqueId());
//
//            // 根据透传消息类型分发处理
//            switch (topicInfo.getTopicType()) {
//                case PASSTHROUGH_UP:
//                    return processUpstreamPassthrough(request, topicInfo);
//
//                case PASSTHROUGH_DOWN:
//                    return processDownstreamPassthrough(request, topicInfo);
//
//                default:
//                    log.warn("[{}] 不支持的透传类型: {}", getName(), topicInfo.getTopicType());
//                    return ProcessorResult.CONTINUE;
//            }
//
//        } catch (Exception e) {
//            log.error("[{}] 透传消息处理异常: ", getName(), e);
//            return ProcessorResult.ERROR;
//        }
//    }
//
//    /**
//     * 处理上行透传消息
//     */
//    private ProcessorResult processUpstreamPassthrough(MQTTUPRequest request,
// MQTTTopicManager.TopicInfo topicInfo) {
//        try {
//            byte[] rawData = request.getMessageContentAsBytes();
//            String payload = request.getMessageContentAsString();
//
//            log.info("[{}] 处理上行透传 - 设备: {}, 数据长度: {}字节",
//                getName(), topicInfo.getDeviceUniqueId(), rawData.length);
//
//            // 设置上下文信息
//            request.setContextValue("messageType", "PASSTHROUGH_UPSTREAM");
//            request.setContextValue("topicType", "PASSTHROUGH_UP");
//            request.setContextValue("dataLength", rawData.length);
//            request.setContextValue("rawData", rawData);
//
//            // 透传数据验证
//            if (validatePassthroughData(rawData)) {
//                // 记录数据统计
//                incrementUpstreamCounter(topicInfo.getProductKey(), rawData.length);
//
//                // 标记为透传数据，供后续处理器识别
//                request.setContextValue("isPassthrough", true);
//                request.setContextValue("needDecode", true); // 标记需要解码
//
//                log.debug("[{}] 上行透传处理成功 - 设备: {}, 数据: {}",
//                    getName(), topicInfo.getDeviceUniqueId(), payload);
//                return ProcessorResult.CONTINUE;
//            } else {
//                log.warn("[{}] 透传数据验证失败 - 设备: {}", getName(), topicInfo.getDeviceUniqueId());
//                request.setContextValue("validationError", "透传数据格式不正确");
//                return ProcessorResult.CONTINUE;
//            }
//
//        } catch (Exception e) {
//            log.error("[{}] 上行透传处理异常 - 设备: {}, 异常: ",
//                getName(), topicInfo.getDeviceUniqueId(), e);
//            return ProcessorResult.ERROR;
//        }
//    }
//
//    /**
//     * 处理下行透传消息
//     */
//    private ProcessorResult processDownstreamPassthrough(MQTTUPRequest request,
// MQTTTopicManager.TopicInfo
//    topicInfo) {
//        try {
//            byte[] rawData = request.getMessageContentAsBytes();
//            String payload = request.getMessageContentAsString();
//
//            log.info("[{}] 处理下行透传 - 设备: {}, 数据长度: {}字节",
//                getName(), topicInfo.getDeviceUniqueId(), rawData.length);
//
//            // 设置上下文信息
//            request.setContextValue("messageType", "PASSTHROUGH_DOWNSTREAM");
//            request.setContextValue("topicType", "PASSTHROUGH_DOWN");
//            request.setContextValue("dataLength", rawData.length);
//            request.setContextValue("rawData", rawData);
//
//            // 透传数据验证
//            if (validatePassthroughData(rawData)) {
//                // 记录数据统计
//                incrementDownstreamCounter(topicInfo.getProductKey(), rawData.length);
//
//                // 标记为透传数据
//                request.setContextValue("isPassthrough", true);
//                request.setContextValue("needEncode", true); // 标记需要编码
//
//                // 下行透传可能需要回复确认
//                if (needAcknowledge(rawData)) {
//                    request.setContextValue("needReply", true);
//                    request.setContextValue("replyTopic", buildAckTopic(topicInfo));
//                }
//
//                log.debug("[{}] 下行透传处理成功 - 设备: {}, 数据: {}",
//                    getName(), topicInfo.getDeviceUniqueId(), payload);
//                return ProcessorResult.CONTINUE;
//            } else {
//                log.warn("[{}] 透传数据验证失败 - 设备: {}", getName(), topicInfo.getDeviceUniqueId());
//                request.setContextValue("validationError", "透传数据格式不正确");
//                return ProcessorResult.CONTINUE;
//            }
//
//        } catch (Exception e) {
//            log.error("[{}] 下行透传处理异常 - 设备: {}, 异常: ",
//                getName(), topicInfo.getDeviceUniqueId(), e);
//            return ProcessorResult.ERROR;
//        }
//    }
//
//    /**
//     * 验证透传数据
//     */
//    private boolean validatePassthroughData(byte[] data) {
//        // 基本验证：数据不能为空
//        if (data == null || data.length == 0) {
//            return false;
//        }
//
//        // 数据长度限制（例如不超过64KB）
//        if (data.length > 64 * 1024) {
//            log.warn("[{}] 透传数据过大: {}字节", getName(), data.length);
//            return false;
//        }
//
//        return true;
//    }
//
//    /**
//     * 检查是否需要确认回复
//     */
//    private boolean needAcknowledge(byte[] data) {
//        // 透传数据的确认逻辑可以根据协议定制
//        // 这里简单判断：如果数据以特定字节开头，则需要确认
//        return data.length > 0 && (data[0] & 0x80) != 0; // 最高位为1表示需要确认
//    }
//
//    /**
//     * 构建确认回复主题
//     */
//    private String buildAckTopic(MQTTTopicManager.TopicInfo topicInfo) {
//        // 透传确认主题：$thing/up/ack/${productKey}/${deviceId}
//        return "$thing/up/ack/" + topicInfo.getProductKey() + "/" + topicInfo.getDeviceId();
//    }
//
//    /**
//     * 增加上行计数器
//     */
//    private void incrementUpstreamCounter(String productKey, int dataLength) {
//        // TODO: 实现上行透传统计逻辑
//        log.debug("[{}] 上行透传计数器增加 - 产品: {}, 数据长度: {}字节",
//            getName(), productKey, dataLength);
//    }
//
//    /**
//     * 增加下行计数器
//     */
//    private void incrementDownstreamCounter(String productKey, int dataLength) {
//        // TODO: 实现下行透传统计逻辑
//        log.debug("[{}] 下行透传计数器增加 - 产品: {}, 数据长度: {}字节",
//            getName(), productKey, dataLength);
//    }
//
//    /**
//     * 尝试解析透传数据的类型
//     */
//    private String parsePassthroughDataType(byte[] data) {
//        if (data == null || data.length == 0) {
//            return "UNKNOWN";
//        }
//
//        // 根据数据特征判断类型
//        if (data.length >= 2) {
//            int header = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
//
//            switch (header & 0xF000) {
//                case 0x1000:
//                    return "SENSOR_DATA";
//                case 0x2000:
//                    return "CONTROL_CMD";
//                case 0x3000:
//                    return "CONFIG_DATA";
//                default:
//                    return "RAW_DATA";
//            }
//        }
//
//        return "RAW_DATA";
//    }
//
//    /**
//     * 提取设备信息（如果透传数据中包含）
//     */
//    private void extractDeviceInfo(MQTTUPRequest request, byte[] data) {
//        try {
//            // 尝试从透传数据中提取设备相关信息
//            String dataType = parsePassthroughDataType(data);
//            request.setContextValue("passthroughDataType", dataType);
//
//            // 如果是传感器数据，可能包含时间戳等信息
//            if ("SENSOR_DATA".equals(dataType) && data.length >= 8) {
//                // 假设后4个字节是时间戳
//                long timestamp = 0;
//                for (int i = 4; i < 8; i++) {
//                    timestamp = (timestamp << 8) | (data[i] & 0xFF);
//                }
//                request.setContextValue("dataTimestamp", timestamp);
//            }
//
//        } catch (Exception e) {
//            log.debug("[{}] 设备信息提取失败: ", getName(), e);
//        }
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
//            // 透传处理完成后，提取额外的设备信息
//            byte[] data = (byte[]) request.getContextValue("rawData");
//            if (data != null) {
//                extractDeviceInfo(request, data);
//            }
//
//            log.debug("[{}] 透传处理完成 - 设备: {}", getName(), request.getDeviceUniqueId());
//        } else {
//            log.warn("[{}] 透传处理异常 - 设备: {}, 结果: {}",
//                getName(), request.getDeviceUniqueId(), result);
//        }
//    }
//
//    @Override
//    public void onError(MQTTUPRequest request, Exception e) {
//        log.error("[{}] 透传处理器异常 - 设备: {}, 异常: ",
//            getName(), request.getDeviceUniqueId(), e);
//        request.setError("透传处理失败: " + e.getMessage());
//    }
//
//    @Override
//    public int getPriority() {
//        // 透传处理优先级中等
//        return 5;
//    }
// }
