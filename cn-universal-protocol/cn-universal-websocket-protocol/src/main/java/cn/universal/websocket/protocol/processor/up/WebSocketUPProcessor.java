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

package cn.universal.websocket.protocol.processor.up;

import cn.universal.dm.device.service.plugin.BaseMessageProcessor;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;

/**
 * WebSocket 上行处理器接口
 *
 * <p>继承通用的BaseMessageProcessor，定义WebSocket模块特有的处理方法
 * 各WebSocket处理器实现此接口，提供具体的处理逻辑
 * 
 * <p>处理器生命周期（按执行顺序）：
 * 1. preCheck()      - 前置检查，验证必要数据
 * 2. supports()      - 支持性检查，判断是否处理
 * 3. process()       - 核心处理逻辑
 * 4. postProcess()   - 后处理，清理工作
 * 5. onError()       - 异常处理
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2026/1/14
 */
public interface WebSocketUPProcessor extends BaseMessageProcessor {

    /**
     * 处理结果枚举
     * 
     * <p>处理流程控制：
     * - CONTINUE：继续执行下一个处理器
     * - STOP：停止执行链（成功完成）
     * - SKIP：跳过当前处理器，继续下一个
     * - ERROR：处理异常，链执行失败
     */
    enum ProcessorResult {
        /** 继续下一个处理器 */
        CONTINUE,
        /** 停止处理（成功完成） */
        STOP,
        /** 跳过当前处理器 */
        SKIP,
        /** 处理错误 */
        ERROR
    }

    /**
     * 前置检查 - 检查必要的数据是否存在
     * 
     * <p>执行顺序：在 process() 之前
     * <p>返回 false 将跳过该处理器
     * <p>默认实现：返回 true（通过检查）
     *
     * @param request 上行请求
     * @return true 表示通过检查，false 表示跳过该处理器
     */
    default boolean preCheck(WebSocketUPRequest request) {
        return true;
    }

    /**
     * 处理上行请求
     *
     * @param request 上行请求
     * @return 处理结果
     */
    ProcessorResult process(WebSocketUPRequest request);

    /**
     * 是否支持处理该消息
     * 
     * <p>执行顺序：在 preCheck() 后，process() 前
     * <p>用于路由判断，决定是否调用 process() 方法
     *
     * @param request 上行请求对象
     * @return true 表示支持，false 表示不支持
     */
    boolean supports(WebSocketUPRequest request);

    /**
     * 后处理 - 处理完成后的清理工作
     * 
     * <p>执行顺序：在 process() 之后
     * <p>无论 process() 结果如何都会执行
     * <p>用于记录日志、清理资源等
     * <p>默认实现：无操作
     *
     * @param request 上行请求
     * @param result 处理结果
     */
    default void postProcess(WebSocketUPRequest request, ProcessorResult result) {
        // 子类可选重写此方法
    }

    /**
     * 异常处理 - 处理过程中的异常
     * 
     * <p>执行顺序：在 process() 抛出异常时
     * <p>用于统一的异常处理和日志记录
     * <p>默认实现：打印错误日志
     *
     * @param request 上行请求
     * @param e 异常对象
     */
    default void onError(WebSocketUPRequest request, Exception e) {
        // 子类可选重写此方法进行自定义异常处理
    }

    /**
     * 获取处理器顺序（执行优先级）
     * 
     * <p>处理器按 order 值升序排列
     * <p>同一 order 下按 priority 值降序排列
     * <p>标准的 order 值：
     * <ul>
     *   <li>100: 身份认证</li>
     *   <li>200: 自动注册</li>
     *   <li>300: 编解码处理</li>
     *   <li>400-500: 业务处理</li>
     *   <li>800: 消息回复</li>
     *   <li>900: 指标收集</li>
     * </ul>
     *
     * @return 顺序值，越小越先执行
     */
    int getOrder();

    /**
     * 获取处理器优先级
     * 
     * <p>当多个处理器的 order 相同时，按优先级降序排列
     * <p>优先级高的处理器先执行
     * <p>与 MQTT MqttMessageProcessor 保持一致
     * <p>默认实现：返回 0（默认优先级）
     *
     * @return 优先级值，越大优先级越高
     */
    default int getPriority() {
        return 0;
    }

    /**
     * 是否启用此处理器
     * 
     * <p>允许动态启用/禁用处理器
     * <p>默认实现：返回 true（启用）
     *
     * @return true 表示启用，false 表示禁用
     */
    default boolean isEnabled() {
        return true;
    }
}
