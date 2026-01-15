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

package cn.universal.websocket.protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import cn.hutool.json.JSONUtil;
import cn.universal.common.domain.R;
import cn.universal.core.downlink.DownlinkInterceptorChain;
import cn.universal.core.service.IDown;
import cn.universal.dm.device.service.AbstractDownService;
import cn.universal.websocket.protocol.config.WebSocketModuleInfo;
import cn.universal.websocket.protocol.entity.WebSocketDownRequest;
import cn.universal.websocket.protocol.handle.WebSocketDownHandle;
import cn.universal.websocket.protocol.processor.down.WebSocketDownProcessorChain;
import jakarta.annotation.Resource;

/**
 * WebSocket 下行协议处理服务
 *
 * 负责处理从云平台/应用向设备通过 WebSocket 发送的下行消息。
 * 流程：数据校验 → 拦截器链处理 → 处理器链处理 → 消息发送
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/13
 */
@Service("websocketDownService")
@Slf4j(topic = "websocket")
public class WebSocketDownService extends AbstractDownService<WebSocketDownRequest> implements IDown {

    @Resource
    private WebSocketModuleInfo websocketModuleInfo;

    @Resource
    private WebSocketDownHandle websocketDownHandle;

    @Resource
    private WebSocketDownProcessorChain websocketDownProcessorChain;

    /**
     * 注入拦截器链管理器（包含所有拦截器）
     */
    @Resource
    private DownlinkInterceptorChain downlinkInterceptorChain;

    /**
     * 获取拦截器链
     *
     * @return 返回拦截器链供 IDown 接口使用
     */
    @Override
    public DownlinkInterceptorChain getInterceptorChain() {
        return downlinkInterceptorChain;
    }

    /**
     * 将 JSON 字符串转换为 WebSocketDownRequest 对象
     *
     * @deprecated 该方法已废弃，请使用 WebSocketDownRequestConverter 进行转换
     * 保留此方法仅为了兼容遗留代码
     *
     * @param request JSON 字符串格式的请求
     * @return 转换后的 WebSocketDownRequest 对象
     */
    @Deprecated
    @Override
    protected WebSocketDownRequest convert(String request) {
        // 旧的转换逻辑已移至 WebSocketDownRequestConverter
        // 这里只做基本的 JSON 解析
        return JSONUtil.toBean(request, WebSocketDownRequest.class);
    }

    /**
     * 获取协议代码
     *
     * @return 协议代码（WEBSOCKET）
     */
    @Override
    public String code() {
        return websocketModuleInfo.getCode();
    }

    /**
     * 获取协议名称
     *
     * @return 协议名称（WebSocket协议）
     */
    @Override
    public String name() {
        return websocketModuleInfo.getName();
    }

    /**
     * 处理下行消息的主入口方法
     *
     * 处理流程：
     * 1. 从下行上下文中提取已转换的下行请求对象
     * 2. 验证请求对象合法性
     * 3. 记录请求信息
     * 4. 执行处理器链（验证、转码、路由等）
     * 5. 通过下行处理器发送消息到设备
     * 6. 返回处理结果
     *
     * @param context 下行上下文，包含已转换的请求对象
     * @return 处理结果（R.ok() / R.error()）
     */
    @Override
    @SuppressWarnings("unchecked")
    public R doProcess(cn.universal.core.downlink.DownlinkContext<?> context) {
        try {
            // 从上下文获取已转换好的请求对象（由 Converter 自动转换）
            WebSocketDownRequest downRequest = (WebSocketDownRequest) context.getDownRequest();

            if (downRequest == null) {
                log.warn("[WebSocket下行] 请求对象为空，请检查 Converter 配置");
                return R.error("请求对象为空，请检查 Converter 配置");
            }

            // 记录下行请求的关键信息
            log.info("[WebSocket下行][开始处理] deviceId={}, productKey={}, sessionId={}, payloadSize={}",
                    downRequest.getDeviceId(),
                    downRequest.getProductKey(),
                    downRequest.getSessionId(),
                    downRequest.getPayload() != null ? downRequest.getPayload().length() : 0);

            // 执行处理链（包括验证、转码等）
            websocketDownProcessorChain.process(downRequest);

            // 通过下行处理器发送消息（新的实现方式，返回 R<?>）
            R<?> result = websocketDownHandle.down(downRequest);

            if (result != null && !result.isError()) {
                log.info("[WebSocket下行][发送成功] deviceId={}, requestId={}", 
                        downRequest.getDeviceId(), downRequest.getRequestId());
                return result;
            } else {
                log.warn("[WebSocket下行][发送失败] deviceId={}, requestId={}", 
                        downRequest.getDeviceId(), downRequest.getRequestId());
                return result != null ? result : R.error("消息发送失败");
            }

        } catch (Exception e) {
            log.error("[WebSocket下行] 处理异常", e);
            return R.error("处理异常: " + e.getMessage());
        }
    }
}
