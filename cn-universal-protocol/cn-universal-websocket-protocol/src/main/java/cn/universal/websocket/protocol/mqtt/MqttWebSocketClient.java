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

package cn.universal.websocket.protocol.mqtt;

import java.util.concurrent.ExecutorService;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.universal.persistence.entity.Network;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import cn.universal.websocket.protocol.enums.WebSocketMessageType;
import cn.universal.websocket.protocol.processor.up.WebSocketUPProcessorChain;

/**
 * MQTT over WebSocket 客户端
 * 使用 Eclipse Paho MQTT 客户端库实现标准 MQTT 协议
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/14
 */
public class MqttWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(MqttWebSocketClient.class);

    private final Integer networkId;
    private final Network network;
    private final WebSocketUPProcessorChain upProcessorChain;
    private final ExecutorService executorService;
    
    private MqttClient mqttClient;
    private volatile boolean connected = false;

    public MqttWebSocketClient(Integer networkId, Network network, 
                               WebSocketUPProcessorChain upProcessorChain,
                               ExecutorService executorService) {
        this.networkId = networkId;
        this.network = network;
        this.upProcessorChain = upProcessorChain;
        this.executorService = executorService;
    }

    /**
     * 连接到 MQTT broker
     */
    public boolean connect() {
        try {
            var config = JSONUtil.parseObj(network.getConfiguration());
            String host = config.getStr("host");
            Integer port = config.getInt("port");
            String path = config.getStr("path", "/mqtt");
            String clientId = config.getStr("clientId");
            String username = config.getStr("username");
            String password = config.getStr("password");
            String topics = config.getStr("topics");
            
            // 如果没有配置clientId，生成一个
            if (StrUtil.isBlank(clientId)) {
                clientId = "mqtt_client_" + IdUtil.simpleUUID();
                log.info("[MQTT客户端] 自动生成ClientId: {}", clientId);
            }

            // 构建 MQTT WebSocket URL
            String brokerUrl = String.format("ws://%s:%d%s", host, port, path);
            log.info("[MQTT客户端] 连接到broker: {}, clientId: {}", brokerUrl, clientId);

            // 创建 MQTT 客户端
            mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

            // 配置连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(60);
            options.setAutomaticReconnect(true);
            
            if (StrUtil.isNotBlank(username)) {
                options.setUserName(username);
            }
            if (StrUtil.isNotBlank(password)) {
                options.setPassword(password.toCharArray());
            }

            // 设置回调
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    connected = false;
                    log.warn("[MQTT客户端] 连接丢失: networkId={}, 原因: {}", 
                            networkId, cause != null ? cause.getMessage() : "未知");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    log.debug("[MQTT客户端] 收到消息: topic={}, payload={}", topic, payload);
                    
                    // 异步处理消息
                    if (upProcessorChain != null && executorService != null) {
                        executorService.submit(() -> {
                            try {
                                WebSocketUPRequest upRequest = new WebSocketUPRequest();
                                upRequest.setSessionId(mqttClient.getClientId());
                                upRequest.setWsMessageType(WebSocketMessageType.TEXT);
                                upRequest.setPayload(payload);
                                upRequest.setProductKey(network.getProductKey());
                                upRequest.setNetworkUnionId(network.getUnionId());
                                upRequest.setMessageId(IdUtil.simpleUUID());
                                upRequest.setTimestamp(System.currentTimeMillis());
                                
                                upProcessorChain.process(upRequest);
                            } catch (Exception e) {
                                log.error("[MQTT客户端] 处理消息异常: {}", e.getMessage(), e);
                            }
                        });
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    log.debug("[MQTT客户端] 消息发送完成: messageId={}", token.getMessageId());
                }
            });

            // 连接到 broker
            log.info("[MQTT客户端] 开始连接...");
            mqttClient.connect(options);
            connected = true;
            log.info("[MQTT客户端] 连接成功: networkId={}, clientId={}", networkId, clientId);

            // 订阅主题
            if (StrUtil.isNotBlank(topics)) {
                String[] topicArray = topics.split(",");
                for (String topic : topicArray) {
                    topic = topic.trim();
                    if (StrUtil.isNotBlank(topic)) {
                        mqttClient.subscribe(topic, 1);
                        log.info("[MQTT客户端] 已订阅主题: {}", topic);
                    }
                }
            }

            return true;

        } catch (MqttException e) {
            connected = false;
            log.error("[MQTT客户端] 连接失败: networkId={}, 错误代码: {}, 原因: {}", 
                    networkId, e.getReasonCode(), e.getMessage(), e);
            return false;
        } catch (Exception e) {
            connected = false;
            log.error("[MQTT客户端] 连接异常: networkId={}, 错误: {}", networkId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                log.info("[MQTT客户端] 已断开连接: networkId={}", networkId);
            } catch (MqttException e) {
                log.error("[MQTT客户端] 断开连接异常: {}", e.getMessage(), e);
            } finally {
                connected = false;
            }
        }
    }

    /**
     * 发布消息
     */
    public void publish(String topic, String payload, int qos) throws MqttException {
        if (mqttClient != null && mqttClient.isConnected()) {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(qos);
            mqttClient.publish(topic, message);
            log.debug("[MQTT客户端] 已发布消息: topic={}, qos={}", topic, qos);
        } else {
            log.warn("[MQTT客户端] 未连接，无法发布消息");
        }
    }

    /**
     * 订阅主题
     */
    public void subscribe(String topic, int qos) throws MqttException {
        if (mqttClient != null && mqttClient.isConnected()) {
            mqttClient.subscribe(topic, qos);
            log.info("[MQTT客户端] 已订阅主题: {}, qos={}", topic, qos);
        } else {
            log.warn("[MQTT客户端] 未连接，无法订阅主题");
        }
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(String topic) throws MqttException {
        if (mqttClient != null && mqttClient.isConnected()) {
            mqttClient.unsubscribe(topic);
            log.info("[MQTT客户端] 已取消订阅: {}", topic);
        }
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return connected && mqttClient != null && mqttClient.isConnected();
    }

    /**
     * 获取客户端ID
     */
    public String getClientId() {
        return mqttClient != null ? mqttClient.getClientId() : null;
    }
}
