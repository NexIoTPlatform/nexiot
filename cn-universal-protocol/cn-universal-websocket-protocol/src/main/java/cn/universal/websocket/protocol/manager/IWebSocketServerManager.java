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

/**
 * WebSocket 服务器管理接口
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/14
 */
public interface IWebSocketServerManager {

    /**
     * 启动 WebSocket 服务器
     *
     * @param networkId 网络组件ID
     * @return 是否成功
     */
    boolean startServer(Integer networkId);

    /**
     * 停止 WebSocket 服务器
     *
     * @param networkId 网络组件ID
     * @return 是否成功
     */
    boolean stopServer(Integer networkId);

    /**
     * 重启 WebSocket 服务器
     *
     * @param networkId 网络组件ID
     * @return 是否成功
     */
    boolean restartServer(Integer networkId);

    /**
     * 检查 WebSocket 服务器是否正在运行
     *
     * @param networkId 网络组件ID
     * @return 是否正在运行
     */
    boolean isRunning(Integer networkId);
}
