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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.websocket.protocol.enums.WebSocketMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * WebSocket 上行请求实体
 *
 * <p>包含WebSocket消息处理过程中的所有上下文信息，支持处理器链模式
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2026/1/14
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class WebSocketUPRequest extends BaseUPRequest {

    /** 消息唯一标识 */
    private String messageId;

    /** 会话ID */
    private String sessionId;

    /** WebSocket消息类型（TEXT, BINARY等） */
    private WebSocketMessageType wsMessageType;

    /** 网络唯一标识（Network 的 unionId 字段） */
    private String networkUnionId;

    /** 时间戳 */
    private Long timestamp;

    /** 当前处理阶段 */
    private ProcessingStage stage;

    /** 回复上层平台的请求列表（编解码后生成） */
    private List<BaseUPRequest> upRequestList;

    /** 处理上下文 - 用于在处理器之间传递临时数据 */
    @Builder.Default
    private Map<String, Object> context = new ConcurrentHashMap<>();

    /** 编解码上下文 - 专门用于编解码器的上下文信息 */
    @Builder.Default
    private Map<String, Object> codecContext = new ConcurrentHashMap<>();

    /**
     * 消息分类枚举
     */
    public enum MessageCategory {
        /** 认证消息 */
        AUTH,
        /** 事件消息 */
        EVENT,
        /** 数据消息 */
        DATA,
        /** 心跳消息 */
        PING,
        /** 订阅消息 */
        SUBSCRIBE,
        /** 其他消息 */
        OTHER
    }

    /**
     * 处理阶段枚举
     */
    public enum ProcessingStage {
        /** 初始化 */
        INIT,
        /** 已认证 */
        AUTHENTICATED,
        /** 已提取元数据 */
        METADATA_EXTRACTED,
        /** 已解码 */
        DECODED,
        /** 已验证 */
        VALIDATED,
        /** 已推送 */
        PUBLISHED,
        /** 处理完成 */
        COMPLETED,
        /** 处理失败 */
        FAILED
    }

    /**
     * 设置处理上下文值
     *
     * @param key 键
     * @param value 值
     */
    public void setContextValue(String key, Object value) {
        if (key != null && value != null) {
            context.put(key, value);
        }
    }

    /**
     * 获取处理上下文值
     *
     * @param key 键
     * @param <T> 值类型
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextValue(String key) {
        return (T) context.get(key);
    }

    /**
     * 设置编解码上下文值
     *
     * @param key 键
     * @param value 值
     */
    public void setCodecContextValue(String key, Object value) {
        if (key != null && value != null) {
            codecContext.put(key, value);
        }
    }

    /**
     * 获取编解码上下文值
     *
     * @param key 键
     * @param <T> 值类型
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T getCodecContextValue(String key) {
        return (T) codecContext.get(key);
    }

    // ========== 显式 getter/setter（兼容 Lombok 生成问题）==========
    
    /**
     * 获取消息ID
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * 设置消息ID
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * 获取会话ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话ID
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 获取处理阶段
     */
    public ProcessingStage getStage() {
        return stage;
    }

    /**
     * 设置处理阶段
     */
    public void setStage(ProcessingStage stage) {
        this.stage = stage;
    }

    /**
     * 获取错误信息
     */
    public String getError() {
        return (String) getContextValue("error");
    }

    /**
     * 设置错误信息
     */
    public void setError(String error) {
        setContextValue("error", error);
    }
}

