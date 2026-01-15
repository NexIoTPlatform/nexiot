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

package cn.universal.mqtt.protocol.controller;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.domain.R;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.mqtt.protocol.entity.MQTTProductConfig;
import cn.universal.mqtt.protocol.entity.MQTTPublishMessage;
import cn.universal.mqtt.protocol.service.MqttMockDataService;
import cn.universal.mqtt.protocol.system.SysMQTTManager;
import cn.universal.mqtt.protocol.third.ThirdMQTTConfigService;
import cn.universal.mqtt.protocol.third.ThirdMQTTServerManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MQTT主动推送控制器
 *
 * <p>提供MQTT主动推送功能，支持： - 系统内置MQTT和客户端MQTT自动选择 - 根据ProductKey和DeviceId推送消息 - 根据ProductKey批量推送 -
 * 自动构建合适的主题格式
 *
 * <p>与TCP不同，MQTT基于pub/sub模式，无需判断设备归属服务器
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
@RestController
@RequestMapping("/monitor/mqtt/v2/push")
public class MqttActivePushController {

    @Autowired
    private ThirdMQTTServerManager mqttServerManager;

    @Autowired
    private ThirdMQTTConfigService mqttConfigService;

    @Autowired
    private SysMQTTManager sysMQTTManager;

    @Autowired
    private IoTProductDeviceService ioTProductDeviceService;

    @Autowired(required = false)
    private MqttMockDataService mqttMockDataService;

    @PostConstruct
    @Profile("test")
    public void init() {
        if (mqttMockDataService != null) {
            mqttMockDataService.startMockDataSending();
        }
    }

    /**
     * 主动推送消息（GET方式）
     *
     * @param productKey 产品Key
     * @param deviceId   设备ID
     * @param payload    消息内容
     * @param topic      可选的自定义主题
     * @param qos        可选的QoS级别，默认为1
     * @param retained   可选的保留标志，默认为false
     * @return 推送结果
     */
    @GetMapping("/send")
    public R<?> sendToDeviceGet(
            @RequestParam String productKey,
            @RequestParam String deviceId,
            @RequestParam String payload,
            @RequestParam(required = false) String topic,
            @RequestParam(defaultValue = "1") int qos,
            @RequestParam(defaultValue = "false") boolean retained) {

        try {
            String networkUnionId = ioTProductDeviceService.selectNetworkUnionId(productKey);
            if (StrUtil.isBlank(networkUnionId)) {
                return R.error("没有绑定网络");
            }
            // 构建推送主题
            String publishTopic = buildPushTopic(productKey, deviceId, topic);

            // 构建发布消息
            MQTTPublishMessage publishMessage =
                    MQTTPublishMessage.builder()
                            .topic(publishTopic)
                            .payload(StrUtil.bytes(payload))
                            .qos(qos)
                            .retained(retained)
                            .productKey(productKey)
                            .deviceId(deviceId)
                            .messageType("ACTIVE_PUSH")
                            .build();

            boolean success;
            // 优先推送到内置MQTT（System MQTT）
            if (sysMQTTManager.isEnabled() && sysMQTTManager.isProductCovered(productKey)) {
                success =
                        sysMQTTManager.publishMessage(
                                publishTopic,
                                publishMessage.getPayload(),
                                publishMessage.getQos(),
                                publishMessage.isRetained());
            } else {
                success = mqttServerManager.publishMessage(networkUnionId, publishMessage);
            }

            if (success) {
                log.info("[MQTT_PUSH] 消息推送成功 - 产品: {}, 设备: {}, 主题: {}", productKey, deviceId, publishTopic);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "消息推送成功");
                result.put("productKey", productKey);
                result.put("deviceId", deviceId);
                result.put("topic", publishTopic);
                result.put("payloadLength", payload.length());
                result.put("qos", qos);
                result.put("retained", retained);
                result.put("timestamp", System.currentTimeMillis());

                return R.ok(result);
            } else {
                return R.error("消息推送失败，请检查MQTT连接状态");
            }

        } catch (Exception e) {
            log.error("[MQTT_PUSH] 消息推送异常 - 产品: {}, 设备: {}, 异常: ", productKey, deviceId, e);
            return R.error("消息推送异常: " + e.getMessage());
        }
    }

    /**
     * 主动推送消息（POST方式）
     *
     * @param request 推送请求
     * @return 推送结果
     */
    @PostMapping("/send")
    public R<?> sendToDevice(@RequestBody PushRequest request) {
        try {
            String networkUnionId = ioTProductDeviceService.selectNetworkUnionId(request.productKey);
            if (StrUtil.isBlank(networkUnionId)) {
                return R.error("没有绑定网络");
            }
            if (StrUtil.isBlank(networkUnionId)) {
                return R.error("没有绑定网络");
            }
            // 参数校验
            if (request.getProductKey() == null || request.getProductKey().trim().isEmpty()) {
                return R.error("产品Key不能为空");
            }
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                return R.error("设备ID不能为空");
            }
            if (request.getPayload() == null) {
                return R.error("消息内容不能为空");
            }
            // 构建推送主题
            String publishTopic =
                    buildPushTopic(request.getProductKey(), request.getDeviceId(), request.getTopic());

            // 构建发布消息
            MQTTPublishMessage publishMessage =
                    MQTTPublishMessage.builder()
                            .topic(publishTopic)
                            .payload(StrUtil.bytes(request.getPayload()))
                            .qos(request.getQos() != null ? request.getQos() : 1)
                            .retained(request.getRetained() != null ? request.getRetained() : false)
                            .productKey(request.getProductKey())
                            .deviceId(request.getDeviceId())
                            .messageType("ACTIVE_PUSH")
                            .messageExpiryInterval(request.getTtl())
                            .userProperties(request.getHeaders())
                            .build();

            boolean success;
            // 优先推送到内置MQTT（System MQTT）
            if (sysMQTTManager.isEnabled() && sysMQTTManager.isProductCovered(request.getProductKey())) {
                success =
                        sysMQTTManager.publishMessage(
                                publishTopic,
                                publishMessage.getPayload(),
                                publishMessage.getQos(),
                                publishMessage.isRetained());
            } else {
                success = mqttServerManager.publishMessage(networkUnionId, publishMessage);
            }

            if (success) {
                log.info(
                        "[MQTT_PUSH] 消息推送成功 - 产品: {}, 设备: {}, 主题: {}",
                        request.getProductKey(),
                        request.getDeviceId(),
                        publishTopic);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "消息推送成功");
                result.put("productKey", request.getProductKey());
                result.put("deviceId", request.getDeviceId());
                result.put("topic", publishTopic);
                result.put("payloadLength", request.getPayload().length());
                result.put("qos", publishMessage.getQos());
                result.put("retained", publishMessage.isRetained());
                result.put("timestamp", System.currentTimeMillis());

                return R.ok(result);
            } else {
                return R.error("消息推送失败，请检查MQTT连接状态");
            }

        } catch (Exception e) {
            log.error(
                    "[MQTT_PUSH] 消息推送异常 - 产品: {}, 设备: {}, 异常: ",
                    request.getProductKey(),
                    request.getDeviceId(),
                    e);
            return R.error("消息推送异常: " + e.getMessage());
        }
    }

    /**
     * 根据ProductKey批量推送消息
     *
     * @param productKey 产品Key
     * @param request    批量推送请求
     * @return 推送结果
     */
    @PostMapping("/broadcast/{productKey}")
    public R<?> broadcastToProduct(
            @PathVariable String productKey, @RequestBody BroadcastRequest request) {
        try {
            String networkUnionId = ioTProductDeviceService.selectNetworkUnionId(productKey);
            if (StrUtil.isBlank(networkUnionId)) {
                return R.error("没有绑定网络");
            }
            // 检查产品配置是否存在
            if (!mqttConfigService.supportMQTTNetwork(productKey)) {
                return R.error("产品配置不存在或未启用: " + productKey);
            }

            // 构建广播主题
            String broadcastTopic = buildBroadcastTopic(productKey, request.getTopic());

            // 构建发布消息
            MQTTPublishMessage publishMessage =
                    MQTTPublishMessage.builder()
                            .topic(broadcastTopic)
                            .payload(StrUtil.bytes(request.getPayload()))
                            .qos(request.getQos() != null ? request.getQos() : 1)
                            .retained(request.getRetained() != null ? request.getRetained() : false)
                            .productKey(productKey)
                            .messageType("BROADCAST")
                            .messageExpiryInterval(request.getTtl())
                            .userProperties(request.getHeaders())
                            .build();

            // 发布消息
            boolean success = mqttServerManager.publishMessage(networkUnionId, publishMessage);

            if (success) {
                log.info("[MQTT_BROADCAST] 广播消息推送成功 - 产品: {}, 主题: {}", productKey, broadcastTopic);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "广播消息推送成功");
                result.put("productKey", productKey);
                result.put("topic", broadcastTopic);
                result.put("payloadLength", request.getPayload().length());
                result.put("qos", publishMessage.getQos());
                result.put("retained", publishMessage.isRetained());
                result.put("timestamp", System.currentTimeMillis());

                return R.ok(result);
            } else {
                return R.error("广播消息推送失败，请检查MQTT连接状态");
            }

        } catch (Exception e) {
            log.error("[MQTT_BROADCAST] 广播消息推送异常 - 产品: {}, 异常: ", productKey, e);
            return R.error("广播消息推送异常: " + e.getMessage());
        }
    }

    /**
     * 获取产品推送状态
     *
     * @param productKey 产品Key
     * @return 产品状态信息
     */
    @GetMapping("/status/{productKey}")
    public R<?> getProductStatus(@PathVariable String productKey) {
        try {
            boolean hasConfig = mqttConfigService.supportMQTTNetwork(productKey);
            boolean isConnected = mqttServerManager.isConnected(productKey);

            Map<String, Object> status = new HashMap<>();
            status.put("productKey", productKey);
            status.put("hasConfig", hasConfig);
            status.put("isConnected", isConnected);
            status.put("canPush", hasConfig && isConnected);
            status.put("timestamp", System.currentTimeMillis());

            if (hasConfig) {
                // 获取配置详情
                Map<String, MQTTProductConfig> configs = mqttConfigService.loadAllConfigs();
                MQTTProductConfig config = configs.get(productKey);
                if (config != null) {
                    status.put("networkType", config.getNetworkType());
                    status.put("host", config.getHost());
                    status.put("enabled", config.isEnabled());
                }
            }

            return R.ok(status);

        } catch (Exception e) {
            log.error("[MQTT_PUSH] 获取产品状态异常 - 产品: {}, 异常: ", productKey, e);
            return R.error("获取产品状态异常: " + e.getMessage());
        }
    }

    /**
     * 获取所有可推送的产品列表
     *
     * @return 产品列表
     */
    @GetMapping("/products")
    public R<?> getAvailableProducts() {
        try {
            List<String> productKeys = mqttConfigService.getAllProductKeys();

            Map<String, Object> result = new HashMap<>();
            result.put("products", productKeys);
            result.put("total", productKeys.size());
            result.put("timestamp", System.currentTimeMillis());

            return R.ok(result);

        } catch (Exception e) {
            log.error("[MQTT_PUSH] 获取产品列表异常: ", e);
            return R.error("获取产品列表异常: " + e.getMessage());
        }
    }

    /**
     * 构建推送主题
     */
    private String buildPushTopic(String productKey, String deviceId, String customTopic) {
        if (customTopic != null && !customTopic.trim().isEmpty()) {
            // 使用自定义主题，支持变量替换
            return customTopic.replace("{productKey}", productKey).replace("{deviceId}", deviceId);
        }

        // 使用默认的下行推送主题格式
        return "/" + productKey + "/" + deviceId + "/down";
    }

    /**
     * 构建广播主题
     */
    private String buildBroadcastTopic(String productKey, String customTopic) {
        if (customTopic != null && !customTopic.trim().isEmpty()) {
            // 使用自定义主题，支持变量替换
            return customTopic.replace("{productKey}", productKey);
        }

        // 使用默认的广播主题格式
        return "/" + productKey + "/broadcast";
    }

    /**
     * 推送请求实体
     */
    public static class PushRequest {

        private String productKey;
        private String deviceId;
        private String payload;
        private String topic;
        private Integer qos;
        private Boolean retained;
        private Long ttl;
        private Map<String, String> headers;

        // Getters and Setters
        public String getProductKey() {
            return productKey;
        }

        public void setProductKey(String productKey) {
            this.productKey = productKey;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public Integer getQos() {
            return qos;
        }

        public void setQos(Integer qos) {
            this.qos = qos;
        }

        public Boolean getRetained() {
            return retained;
        }

        public void setRetained(Boolean retained) {
            this.retained = retained;
        }

        public Long getTtl() {
            return ttl;
        }

        public void setTtl(Long ttl) {
            this.ttl = ttl;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
    }

    /**
     * 广播请求实体
     */
    public static class BroadcastRequest {

        private String payload;
        private String topic;
        private Integer qos;
        private Boolean retained;
        private Long ttl;
        private Map<String, String> headers;

        // Getters and Setters
        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public Integer getQos() {
            return qos;
        }

        public void setQos(Integer qos) {
            this.qos = qos;
        }

        public Boolean getRetained() {
            return retained;
        }

        public void setRetained(Boolean retained) {
            this.retained = retained;
        }

        public Long getTtl() {
            return ttl;
        }

        public void setTtl(Long ttl) {
            this.ttl = ttl;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
    }

    /**
     * Mock数据测试方法（仅在test环境生效）
     *
     * <p>发送模拟的设备数据到MQTT主题
     *
     * @return 推送结果
     */
    @GetMapping("/mock/test")
    @org.springframework.context.annotation.Profile("test")
    public R<?> sendMockData() {
        return mqttMockDataService.sendSingleMockData();
    }

    /**
     * 启动自动发送Mock数据（每2秒发送一次）
     *
     * @return 启动结果
     */
    @GetMapping("/mock/start")
    @org.springframework.context.annotation.Profile("test")
    public R<?> startMockDataSending() {
        return mqttMockDataService.startMockDataSending();
    }

    /**
     * 停止自动发送Mock数据
     *
     * @return 停止结果
     */
    @GetMapping("/mock/stop")
    @org.springframework.context.annotation.Profile("test")
    public R<?> stopMockDataSending() {
        return mqttMockDataService.stopMockDataSending();
    }

    /**
     * 获取Mock数据发送状态
     *
     * @return 发送状态
     */
    @GetMapping("/mock/status")
    @org.springframework.context.annotation.Profile("test")
    public R<?> getMockDataStatus() {
        return mqttMockDataService.getMockDataStatus();
    }
}
