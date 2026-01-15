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
 * WebSocket 连接状态枚举
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/13
 */
public enum WebSocketStatus {

    /** 连接中 */
    CONNECTING("CONNECTING", "连接中"),

    /** 已连接 */
    CONNECTED("CONNECTED", "已连接"),

    /** 已认证 */
    AUTHENTICATED("AUTHENTICATED", "已认证"),

    /** 断开连接中 */
    DISCONNECTING("DISCONNECTING", "断开连接中"),

    /** 已断开 */
    DISCONNECTED("DISCONNECTED", "已断开"),

    /** 异常 */
    ERROR("ERROR", "异常");

    private final String code;
    private final String name;

    WebSocketStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static WebSocketStatus fromCode(String code) {
        for (WebSocketStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return DISCONNECTED;
    }
}
