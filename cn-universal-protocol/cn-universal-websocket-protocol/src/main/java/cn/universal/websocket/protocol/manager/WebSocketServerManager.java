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

package cn.universal.websocket.protocol.manager;

import java.net.URI;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.persistence.entity.Network;
import cn.universal.persistence.mapper.NetworkMapper;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import cn.universal.websocket.protocol.enums.WebSocketMessageType;
import cn.universal.websocket.protocol.handle.WebSocketUPHandle;
import cn.universal.websocket.protocol.mqtt.MqttWebSocketClient;
import cn.universal.websocket.protocol.processor.up.WebSocketUPProcessorChain;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 服务器管理器实现
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/14
 */
@Slf4j
@Service
public class WebSocketServerManager implements IWebSocketServerManager {

    @Autowired(required = false)
    private NetworkMapper networkMapper;
    
    @Autowired(required = false)
    private WebSocketUPProcessorChain upProcessorChain;
    
    @Autowired(required = false)
    private WebSocketUPHandle upHandle;
    
    /**
     * 命名虚拟线程执行器 - 用于异步处理消息
     */
    @Resource(name = "namedVirtualThreadExecutor")
    private ExecutorService executorService;

    /** 存储 WebSocket 服务器运行状态：networkId -> isRunning */
    private final ConcurrentMap<Integer, Boolean> serverStatus = new ConcurrentHashMap<>();
    
    /** 存储 WebSocket 客户端会话：networkId -> WebSocketSession */
    private final ConcurrentMap<Integer, WebSocketSession> clientSessions = new ConcurrentHashMap<>();
    
    /** 存储 MQTT 客户端：networkId -> MqttWebSocketClient */
    private final ConcurrentMap<Integer, MqttWebSocketClient> mqttClients = new ConcurrentHashMap<>();
    
    /** 存储 networkId 与 Network 配置的映射 */
    private final ConcurrentMap<Integer, Network> networkConfigs = new ConcurrentHashMap<>();
    
    /** 存储心跳定时任务：networkId -> ScheduledFuture */
    private final ConcurrentMap<Integer, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();
    
    /** 心跳定时器线程池 */
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(5);
    
    /** WebSocket 客户端 */
    private final WebSocketClient webSocketClient = new StandardWebSocketClient();

    /**
     * 启动 WebSocket 服务器
     */
    @Override
    public boolean startServer(Integer networkId) {
        if (networkId == null) {
            log.error("[WebSocket服务器管理] 启动失败：networkId为空");
            return false;
        }

        try {
            Network network = getNetworkById(networkId);
            if (network == null) {
                log.error("[WebSocket服务器管理] 启动失败：网络组件不存在，networkId={}", networkId);
                return false;
            }

            // 验证配置
            if (!validateConfig(network)) {
                log.error("[WebSocket服务器管理] 启动失败：配置验证失败，networkId={}", networkId);
                return false;
            }

            // 解析配置
            JSONObject config = JSONUtil.parseObj(network.getConfiguration());
            Integer port = config.getInt("port");
            String path = config.getStr("path");
            String host = config.getStr("host");

            // 根据类型判断启动方式
            if ("WEB_SOCKET_SERVER".equals(network.getType())) {
                log.info("[WebSocket服务器管理] 启动服务端: networkId={}, port={}, path={}", 
                        networkId, port, path);
                
                // WebSocket 服务端通过 Spring WebSocket 配置自动启动
                // 这里标记为已启动
                serverStatus.put(networkId, true);
                log.info("[WebSocket服务器管理] 服务端启动成功: networkId={}", networkId);
                return true;
                
            } else if ("WEB_SOCKET_CLIENT".equals(network.getType())) {
                if (StrUtil.isBlank(host)) {
                    log.error("[WebSocket服务器管理] 启动失败：客户端模式缺少host配置，networkId={}", networkId);
                    return false;
                }
                
                log.info("[WebSocket服务器管理] 启动客户端: networkId={}, host={}, port={}, path={}", 
                        networkId, host, port, path);
                
                // 获取认证信息
                String username = config.getStr("username");
                String password = config.getStr("password");
                String clientId = config.getStr("clientId");
                String subProtocol = config.getStr("subProtocol"); // 可配置的子协议（如mqtt、stomp等）
                
                // 判断是否为 MQTT 协议
                boolean isMqttProtocol = "mqtt".equalsIgnoreCase(subProtocol);
                
                if (isMqttProtocol) {
                    // 使用 Eclipse Paho MQTT 客户端
                    log.info("[WebSocket服务器管理] 使用MQTT协议，启动Eclipse Paho客户端");
                    return startMqttClient(networkId, network);
                }
                
                // 标准 WebSocket 客户端
                // 构建 WebSocket URL
                String wsUrl = buildWebSocketUrl(config);
                
                // 构建认证 Headers
                WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
                
                // 1. 处理子协议（如果配置了）
                if (StrUtil.isNotBlank(subProtocol)) {
                    headers.setSecWebSocketProtocol(Collections.singletonList(subProtocol));
                    log.info("[WebSocket服务器管理] 使用子协议: {}", subProtocol);
                } else {
                    log.info("[WebSocket服务器管理] 标准WebSocket模式（无子协议）");
                }
                
                // 2. 处理HTTP认证
                if (StrUtil.isNotBlank(username) && StrUtil.isNotBlank(password)) {
                    // 标准WebSocket：可以使用HTTP Basic认证
                    String auth = username + ":" + password;
                    String encodedAuth = Base64.encode(auth);
                    headers.add(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
                    log.info("[WebSocket服务器管理] 添加 HTTP Basic 认证: username={}", username);
                }
                
                // 3. 添加自定义头
                if (StrUtil.isNotBlank(clientId)) {
                    headers.add("X-Client-Id", clientId);
                }
                if (StrUtil.isNotBlank(username)) {
                    headers.add("X-Username", username);
                }
                if (StrUtil.isNotBlank(password)) {
                    headers.add("X-Password", password);
                }
                
                // 添加其他常用头
                headers.add("User-Agent", "UniversalIoT-WebSocket-Client/1.0");
                
                // 创建客户端处理器
                WebSocketClientHandler handler = new WebSocketClientHandler(networkId, network.getName());
                
                // ⚠️ 重要：在握手前先保存配置，确保 afterConnectionEstablished 能访问
                networkConfigs.put(networkId, network);
                log.debug("[WebSocket服务器管理] 配置已保存到缓存: networkId={}", networkId);
                
                try {
                    log.info("[WebSocket服务器管理] 开始握手连接: networkId={}, url={}", networkId, wsUrl);
                    // 执行握手连接（带上认证 Headers）
                    WebSocketSession session = webSocketClient.doHandshake(
                            handler, headers, URI.create(wsUrl))
                            .get(10, TimeUnit.SECONDS); // 10秒超时
                    log.info("[WebSocket服务器管理] 握手连接成功: networkId={}, sessionId={}", networkId, session.getId());
                    
                    // 保存会话和配置
                    clientSessions.put(networkId, session);
                    networkConfigs.put(networkId, network);
                    serverStatus.put(networkId, true);
                    
                    // 启动心跳定时器
                    startHeartbeat(networkId, session);
                    
                    // 同步更新数据库状态为已连接
                    try {
                        network.setState(true);
                        networkMapper.updateNetwork(network);
                        log.debug("[WebSocket服务器管理] 数据库状态已同步为已连接: networkId={}", networkId);
                    } catch (Exception dbError) {
                        log.error("[WebSocket服务器管理] 更新数据库状态失败: networkId={}, 但内存状态已更新", 
                                networkId, dbError);
                    }
                    
                    log.info("[WebSocket服务器管理] 客户端连接成功: networkId={}, sessionId={}", 
                            networkId, session.getId());
                    return true;
                    
                } catch (Exception e) {
                    String errorMsg = e.getMessage();
                    if (e.getCause() != null) {
                        errorMsg += " (原因: " + e.getCause().getMessage() + ")";
                    }
                    log.error("[WebSocket服务器管理] 客户端连接失败: networkId={}, url={}, 错误: {}", 
                            networkId, wsUrl, errorMsg);
                    log.error("[WebSocket服务器管理] 连接失败详情", e);
                    
                    // 如果是 MQTT over WebSocket，给出提示
                    if (path != null && path.toLowerCase().contains("mqtt")) {
                        log.warn("[WebSocket服务器管理] MQTT 连接提示: 确保服务器启用了 WebSocket 支持，且路径正确（通常为 /mqtt）");
                        log.warn("[WebSocket服务器管理] 如果服务器拒绝连接，可能需要配置正确的 subProtocol（通常为空或 'mqtt'）");
                    }
                    
                    // 清理已保存的配置
                    networkConfigs.remove(networkId);
                    serverStatus.put(networkId, false);
                    return false;
                }
            }

            return false;

        } catch (Exception e) {
            log.error("[WebSocket服务器管理] 启动异常: networkId={}, 错误: {}", networkId, e.getMessage(), e);
            serverStatus.put(networkId, false);
            return false;
        }
    }

    /**
     * 启动 MQTT 客户端
     */
    private boolean startMqttClient(Integer networkId, Network network) {
        try {
            // 保存配置
            networkConfigs.put(networkId, network);
            
            // 创建 MQTT 客户端
            MqttWebSocketClient mqttClient = new MqttWebSocketClient(
                    networkId, network, upProcessorChain, executorService);
            
            // 连接
            boolean connected = mqttClient.connect();
            if (connected) {
                mqttClients.put(networkId, mqttClient);
                serverStatus.put(networkId, true);
                
                // 同步更新数据库状态
                try {
                    network.setState(true);
                    networkMapper.updateNetwork(network);
                    log.debug("[MQTT客户端] 数据库状态已同步为已连接: networkId={}", networkId);
                } catch (Exception dbError) {
                    log.error("[MQTT客户端] 更新数据库状态失败: networkId={}", networkId, dbError);
                }
                
                log.info("[MQTT客户端] 启动成功: networkId={}, clientId={}", 
                        networkId, mqttClient.getClientId());
                return true;
            } else {
                networkConfigs.remove(networkId);
                serverStatus.put(networkId, false);
                log.error("[MQTT客户端] 连接失败: networkId={}", networkId);
                return false;
            }
        } catch (Exception e) {
            log.error("[MQTT客户端] 启动异常: networkId={}, 错误: {}", networkId, e.getMessage(), e);
            networkConfigs.remove(networkId);
            serverStatus.put(networkId, false);
            return false;
        }
    }

    /**
     * 停止 WebSocket 服务器
     */
    @Override
    public boolean stopServer(Integer networkId) {
        if (networkId == null) {
            log.error("[WebSocket服务器管理] 停止失败：networkId为空");
            return false;
        }

        try {
            Network network = getNetworkById(networkId);
            if (network == null) {
                log.error("[WebSocket服务器管理] 停止失败：网络组件不存在，networkId={}", networkId);
                return false;
            }

            log.info("[WebSocket服务器管理] 停止: networkId={}, 类型={}", networkId, network.getType());

            // 根据类型判断停止方式
            if ("WEB_SOCKET_SERVER".equals(network.getType())) {
                // WebSocket 服务端停止
                serverStatus.put(networkId, false);
                log.info("[WebSocket服务器管理] 服务端停止成功: networkId={}", networkId);
                return true;
                
            } else if ("WEB_SOCKET_CLIENT".equals(network.getType())) {
                // 检查是否为 MQTT 客户端
                MqttWebSocketClient mqttClient = mqttClients.get(networkId);
                if (mqttClient != null) {
                    // 停止 MQTT 客户端
                    mqttClient.disconnect();
                    mqttClients.remove(networkId);
                    log.info("[MQTT客户端] 已停止: networkId={}", networkId);
                } else {
                    // 停止标准 WebSocket 客户端
                    stopHeartbeat(networkId);
                    
                    WebSocketSession session = clientSessions.get(networkId);
                    if (session != null && session.isOpen()) {
                        try {
                            session.close(CloseStatus.NORMAL);
                            log.info("[WebSocket服务器管理] 客户端连接已关闭: networkId={}, sessionId={}", 
                                    networkId, session.getId());
                        } catch (Exception e) {
                            log.warn("[WebSocket服务器管理] 关闭客户端连接异常: networkId={}, 错误: {}", 
                                    networkId, e.getMessage());
                        } finally {
                            clientSessions.remove(networkId);
                        }
                    } else {
                        log.warn("[WebSocket服务器管理] 客户端会话不存在或已关闭: networkId={}", networkId);
                    }
                }
                
                serverStatus.put(networkId, false);
                networkConfigs.remove(networkId);
                
                // 同步更新数据库状态为已断开
                try {
                    network.setState(false);
                    networkMapper.updateNetwork(network);
                    log.debug("[WebSocket服务器管理] 数据库状态已同步为已断开: networkId={}", networkId);
                } catch (Exception dbError) {
                    log.error("[WebSocket服务器管理] 更新数据库状态失败: networkId={}, 但内存状态已更新", 
                            networkId, dbError);
                }
                
                log.info("[WebSocket服务器管理] 客户端停止成功: networkId={}", networkId);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("[WebSocket服务器管理] 停止异常: networkId={}, 错误: {}", networkId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 重启 WebSocket 服务器
     */
    @Override
    public boolean restartServer(Integer networkId) {
        if (networkId == null) {
            log.error("[WebSocket服务器管理] 重启失败：networkId为空");
            return false;
        }

        try {
            log.info("[WebSocket服务器管理] 重启开始: networkId={}", networkId);

            // 先停止
            boolean stopSuccess = stopServer(networkId);
            if (!stopSuccess) {
                log.warn("[WebSocket服务器管理] 重启：停止失败，继续尝试启动");
            }

            // 等待资源释放
            Thread.sleep(500);

            // 再启动
            boolean startSuccess = startServer(networkId);
            log.info("[WebSocket服务器管理] 重启完成: networkId={}, 结果: {}", networkId, startSuccess);
            return startSuccess;

        } catch (Exception e) {
            log.error("[WebSocket服务器管理] 重启异常: networkId={}, 错误: {}", networkId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查 WebSocket 服务器是否正在运行
     */
    @Override
    public boolean isRunning(Integer networkId) {
        if (networkId == null) {
            return false;
        }
        return serverStatus.getOrDefault(networkId, false);
    }

    /**
     * 验证配置
     */
    private boolean validateConfig(Network network) {
        if (network == null) {
            return false;
        }

        try {
            JSONObject config = JSONUtil.parseObj(network.getConfiguration());
            
            // 检查端口
            Integer port = config.getInt("port");
            if (port == null || port <= 0 || port > 65535) {
                log.warn("[WebSocket服务器管理] 配置验证失败：端口无效 - {}", port);
                return false;
            }

            // 检查路径
            String path = config.getStr("path");
            if (StrUtil.isBlank(path) || !path.startsWith("/")) {
                log.warn("[WebSocket服务器管理] 配置验证失败：路径无效 - {}", path);
                return false;
            }

            // 客户端模式检查 host
            if ("WEB_SOCKET_CLIENT".equals(network.getType())) {
                String host = config.getStr("host");
                if (StrUtil.isBlank(host)) {
                    log.warn("[WebSocket服务器管理] 配置验证失败：客户端模式缺少host");
                    return false;
                }
                
                // topics 是可选的，如果配置了则验证格式
                String topics = config.getStr("topics");
                if (StrUtil.isNotBlank(topics)) {
                    String[] topicArray = topics.split(",");
                    if (topicArray.length == 0) {
                        log.warn("[WebSocket服务器管理] 配置验证失败：topics 格式无效");
                        return false;
                    }
                    log.debug("[WebSocket服务器管理] 检测到订阅主题配置：{} 个主题", topicArray.length);
                }
            }

            return true;

        } catch (Exception e) {
            log.warn("[WebSocket服务器管理] 配置验证异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取网络组件
     */
    private Network getNetworkById(Integer networkId) {
        if (networkMapper == null) {
            return null;
        }
        return networkMapper.selectNetworkById(networkId);
    }
    
    /**
     * 构建 WebSocket URL
     */
    private String buildWebSocketUrl(JSONObject config) {
        return buildWebSocketUrl(config, null, null, null, false);
    }
    
    /**
     * 构建 WebSocket URL（支持 MQTT 认证参数）
     */
    private String buildWebSocketUrl(JSONObject config, String username, String password, String clientId, boolean isMqtt) {
        String host = config.getStr("host");
        Integer port = config.getInt("port");
        String path = config.getStr("path");
        
        // 判断是否使用 SSL
        boolean useSSL = config.getBool("ssl", false);
        String protocol = useSSL ? "wss" : "ws";
        
        // 基础 URL
        StringBuilder urlBuilder = new StringBuilder(String.format("%s://%s:%d%s", protocol, host, port, path));
        
        // 如果是 MQTT，在 URL 中添加认证查询参数
        if (isMqtt) {
            boolean hasParam = path != null && path.contains("?");
            char separator = hasParam ? '&' : '?';
            
            if (StrUtil.isNotBlank(username)) {
                urlBuilder.append(separator).append("username=").append(username);
                separator = '&';
            }
            if (StrUtil.isNotBlank(password)) {
                urlBuilder.append(separator).append("password=").append(password);
                separator = '&';
            }
            if (StrUtil.isNotBlank(clientId)) {
                urlBuilder.append(separator).append("clientId=").append(clientId);
            }
            log.info("[WebSocket服务器管理] MQTT URL 已添加认证参数: username={}, clientId={}", 
                    StrUtil.isNotBlank(username) ? "***" : "无", 
                    StrUtil.isNotBlank(clientId) ? clientId : "无");
        }
        
        return urlBuilder.toString();
    }
    
    /**
     * WebSocket 客户端处理器
     */
    private class WebSocketClientHandler extends TextWebSocketHandler {
        
        private final Integer networkId;
        private final String networkName;
        
        public WebSocketClientHandler(Integer networkId, String networkName) {
            this.networkId = networkId;
            this.networkName = networkName;
        }
        
        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            log.info("[WebSocket客户端][连接建立] 网络组件: {}, networkId: {}, sessionId: {}", 
                    networkName, networkId, session.getId());
            
            try {
                // 连接建立后，发送认证信息（对于 MQTT over WebSocket 很关键）
                Network network = networkConfigs.get(networkId);
                if (network == null) {
                    log.error("[WebSocket客户端][致命错误] 网络配置不存在: networkId={}", networkId);
                    return;
                }
                
                JSONObject config = JSONUtil.parseObj(network.getConfiguration());
                String clientId = config.getStr("clientId");
                String username = config.getStr("username");
                String password = config.getStr("password");
                String subProtocol = config.getStr("subProtocol"); // 获取子协议配置
                String topics = config.getStr("topics");
                
                log.info("[WebSocket客户端] 连接建立: networkId={}, subProtocol={}, clientId={}, username={}",
                        networkId, subProtocol, clientId, username);
                
                // 注意：MQTT协议已经使用Eclipse Paho客户端，不会走到这里
                // 这里只处理标准WebSocket的认证
                if (StrUtil.isNotBlank(clientId) || StrUtil.isNotBlank(username)) {
                    // 标准 WebSocket 连接，发送JSON格式认证消息
                    JSONObject authMsg = new JSONObject();
                    authMsg.set("type", "auth");
                    if (StrUtil.isNotBlank(clientId)) {
                        authMsg.set("clientId", clientId);
                    }
                    if (StrUtil.isNotBlank(username)) {
                        authMsg.set("username", username);
                    }
                    if (StrUtil.isNotBlank(password)) {
                        authMsg.set("password", password);
                    }
                    if (StrUtil.isNotBlank(topics)) {
                        authMsg.set("topics", topics);
                        log.info("[WebSocket客户端] 认证消息中包含订阅主题: networkId={}, topics={}", networkId, topics);
                    }
                    
                    session.sendMessage(new TextMessage(authMsg.toString()));
                    log.info("[WebSocket客户端][✓ 认证消息已发送] networkId={}, clientId={}, username={}, hasTopics={}", 
                            networkId, clientId, username, StrUtil.isNotBlank(topics));
                } else {
                    log.info("[WebSocket客户端] 连接建立，无需发送认证消息: networkId={}", networkId);
                }
            } catch (Exception e) {
                log.error("[WebSocket客户端][认证失败] networkId={}, 错误: {}", networkId, e.getMessage(), e);
                throw e;
            }
        }
        
        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            String payload = message.getPayload();
            log.debug("[WebSocket客户端][收到消息] networkId: {}, 消息: {}", networkId, payload);
            
            try {
                // 获取网络配置（从保存的配置中获取）
                Network network = networkConfigs.get(networkId);
                if (network == null) {
                    log.warn("[WebSocket客户端][消息处理] 网络配置不存在: networkId={}, 尝试从数据库查询", networkId);
                    network = getNetworkById(networkId);
                    if (network == null) {
                        log.error("[WebSocket客户端][消息处理] 数据库中也找不到配置: networkId={}", networkId);
                        return;
                    }
                    networkConfigs.put(networkId, network);
                }
                
                // 构建 WebSocket UP 请求
                WebSocketUPRequest upRequest = WebSocketUPRequest.builder()
                        .sessionId(session.getId())
                        .wsMessageType(WebSocketMessageType.TEXT)
                        .payload(payload)
                        .productKey(network.getProductKey())
                        .networkUnionId(network.getUnionId())
                        .messageId(IdUtil.simpleUUID())
                        .time(System.currentTimeMillis())
                        .build();
                
                // 通过处理链异步处理消息（参考 MQTT 模式）
                if (upProcessorChain != null && executorService != null) {
                    executorService.submit(() -> {
                        try {
                            upProcessorChain.process(upRequest);
                        } catch (Exception e) {
                            log.error("[WebSocket客户端][消息处理异常] networkId: {}, 错误: {}", 
                                    networkId, e.getMessage(), e);
                        }
                    });
                } else {
                    log.warn("[WebSocket客户端] 处理链未注入，跳过消息处理: networkId={}", networkId);
                }
                
            } catch (Exception e) {
                log.error("[WebSocket客户端][消息处理异常] networkId: {}, sessionId: {}, 错误: {}", 
                        networkId, session.getId(), e.getMessage(), e);
            }
        }
        
        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
            log.error("[WebSocket客户端][传输错误] networkId: {}, sessionId: {}, 错误: {}", 
                    networkId, session.getId(), exception.getMessage(), exception);
            
            // 标记为断开状态
            serverStatus.put(networkId, false);
            clientSessions.remove(networkId);
        }
        
        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
            log.info("[WebSocket客户端][连接关闭] networkId: {}, sessionId: {}, 状态码: {}, 原因: {}", 
                    networkId, session.getId(), status.getCode(), status.getReason());
            
            // 分析断开原因
            if (status.getCode() == 1000) {
                log.info("[WebSocket客户端][正常关闭] 服务器或客户端正常断开: networkId={}", networkId);
            } else if (status.getCode() == 1002) {
                log.warn("[WebSocket客户端][协议错误] 可能是认证失败或握手错误: networkId={}", networkId);
            } else if (status.getCode() == 1008) {
                log.warn("[WebSocket客户端][策略错误] 服务器拒绝了连接: networkId={}", networkId);
            } else if (status.getCode() == 1011) {
                log.error("[WebSocket客户端][服务器错误] 服务器发生异常: networkId={}", networkId);
            }
            
            // 清理状态和配置缓存
            serverStatus.put(networkId, false);
            clientSessions.remove(networkId);
            
            // 停止心跳任务
            stopHeartbeat(networkId);
            
            // 同步更新数据库状态
            try {
                Network network = networkConfigs.get(networkId);
                if (network == null) {
                    network = getNetworkById(networkId);
                }
                if (network != null) {
                    network.setState(false);
                    networkMapper.updateNetwork(network);
                    log.info("[WebSocket客户端] 数据库状态已同步为断开: networkId={}", networkId);
                } else {
                    log.warn("[WebSocket客户端] 无法找到网络配置，跳过数据库更新: networkId={}", networkId);
                }
            } catch (Exception e) {
                log.error("[WebSocket客户端] 更新数据库状态失败: networkId={}, 错误: {}", 
                        networkId, e.getMessage(), e);
            }
            
            networkConfigs.remove(networkId);
        }
    }
    
    /**
     * 启动心跳定时器
     */
    private void startHeartbeat(Integer networkId, WebSocketSession session) {
        // 停止已存在的心跳任务
        stopHeartbeat(networkId);
        
        // 每30秒发送一次心跳
        ScheduledFuture<?> heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                if (session != null && session.isOpen()) {
                    JSONObject pingMsg = new JSONObject();
                    pingMsg.set("type", "ping");
                    pingMsg.set("timestamp", System.currentTimeMillis());
                    
                    session.sendMessage(new TextMessage(pingMsg.toString()));
                    log.debug("[WebSocket客户端][心跳] networkId={}, sessionId={}", networkId, session.getId());
                } else {
                    log.warn("[WebSocket客户端][心跳失败] 连接已关闭: networkId={}", networkId);
                    stopHeartbeat(networkId);
                }
            } catch (Exception e) {
                log.error("[WebSocket客户端][心跳异常] networkId={}, 错误: {}", networkId, e.getMessage());
            }
        }, 10, 30, TimeUnit.SECONDS); // 首次延迟10秒，之后每30秒一次
        
        heartbeatTasks.put(networkId, heartbeatTask);
        log.info("[WebSocket客户端] 心跳定时器已启动: networkId={}, 间隔=30秒", networkId);
    }
    
    /**
     * 停止心跳定时器
     */
    private void stopHeartbeat(Integer networkId) {
        ScheduledFuture<?> task = heartbeatTasks.remove(networkId);
        if (task != null && !task.isCancelled()) {
            task.cancel(true);
            log.info("[WebSocket客户端] 心跳定时器已停止: networkId={}", networkId);
        }
    }
}
