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

package cn.universal.websocket.protocol.entity;

import java.time.LocalDateTime;

import cn.universal.persistence.base.BaseDownRequest;
import cn.universal.websocket.protocol.enums.WebSocketMessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * WebSocket 下行请求实体
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/13
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketDownRequest extends BaseDownRequest {

    /** WebSocket消息类型 */
    private WebSocketMessageType messageType;

    /** 会话ID */
    private String sessionId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 时间戳 */
    private Long time;

    /** 是否需要响应 */
    private Boolean needResponse;

    /** 扩展属性 */
    private java.util.Map<String, Object> extras;

    /** 错误信息 */
    private String error;

    /**
     * 获取消息ID（生成或从 sessionId）
     */
    public String getMessageId() {
        return cn.hutool.core.util.IdUtil.simpleUUID();
    }

    /**
     * 设置消息ID（暂不实现）
     */
    public void setMessageId(String messageId) {
        // 消息 ID 由系统生成
    }

    /**
     * 获取错误信息
     */
    public String getError() {
        return error;
    }

    /**
     * 设置错误信息
     */
    public void setError(String error) {
        this.error = error;
    }
}
