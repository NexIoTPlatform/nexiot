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

package cn.universal.websocket.protocol.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebSocket 协议配置属性
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/13
 */
@Data
@ConfigurationProperties(prefix = "websocket.protocol")
public class WebSocketProperties {

    /** 是否启用 WebSocket 协议 */
    private Boolean enabled = true;

    /** WebSocket 路径 */
    private String path = WebSocketConstant.DEFAULT_PATH;

    /** WebSocket 端口（独立启动时使用） */
    private Integer port = WebSocketConstant.DEFAULT_PORT;

    /** 最大帧大小 */
    private Integer maxFrameSize = WebSocketConstant.MAX_FRAME_SIZE;

    /** 空闲超时时间（毫秒） */
    private Integer idleTimeout = WebSocketConstant.DEFAULT_IDLE_TIMEOUT;

    /** 心跳间隔（毫秒） */
    private Integer heartbeatInterval = WebSocketConstant.HEARTBEAT_INTERVAL;

    /** 是否允许跨域 */
    private Boolean allowOrigins = true;

    /** 允许的来源列表 */
    private String[] allowedOrigins = {"*"};

    /** 是否需要认证 */
    private Boolean authRequired = true;

    /** 最大连接数 */
    private Integer maxConnections = 10000;

    /** 消息队列大小 */
    private Integer messageQueueSize = 1000;

    /** 是否启用消息压缩 */
    private Boolean compressionEnabled = false;
}
