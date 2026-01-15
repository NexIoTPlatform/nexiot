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

package cn.universal.websocket.protocol.enums;

/**
 * WebSocket 消息类型枚举
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/13
 */
public enum WebSocketMessageType {

    /** 文本消息 */
    TEXT("TEXT", "文本消息"),

    /** 二进制消息 */
    BINARY("BINARY", "二进制消息"),

    /** 心跳 PING */
    PING("PING", "心跳PING"),

    /** 心跳 PONG */
    PONG("PONG", "心跳PONG"),

    /** 认证消息 */
    AUTH("AUTH", "认证消息"),

    /** 数据上报 */
    DATA_REPORT("DATA_REPORT", "数据上报"),

    /** 属性上报 */
    PROPERTY_POST("PROPERTY_POST", "属性上报"),

    /** 事件上报 */
    EVENT_POST("EVENT_POST", "事件上报"),

    /** 指令下发 */
    COMMAND("COMMAND", "指令下发"),

    /** 属性设置 */
    PROPERTY_SET("PROPERTY_SET", "属性设置"),

    /** 服务调用 */
    SERVICE_INVOKE("SERVICE_INVOKE", "服务调用");

    private final String code;
    private final String name;

    WebSocketMessageType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static WebSocketMessageType fromCode(String code) {
        for (WebSocketMessageType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return TEXT;
    }
}
