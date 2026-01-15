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

package cn.universal.websocket.protocol.converter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import cn.hutool.json.JSONUtil;
import cn.universal.core.downlink.DownlinkContext;
import cn.universal.core.downlink.converter.DownRequestConverter;
import cn.universal.core.message.DownCommonData;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.dm.device.service.AbstractDownService;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.query.IoTDeviceQuery;
import cn.universal.websocket.protocol.config.WebSocketModuleInfo;
import cn.universal.websocket.protocol.entity.WebSocketDownRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 协议下行请求转换器
 *
 * 负责将统一的下行命令（UnifiedDownlinkCommand）转换为 WebSocket 协议特定的下行请求（WebSocketDownRequest）
 * 
 * 转换流程：
 * 1. 验证产品和设备存在性
 * 2. 加载设备和产品配置信息
 * 3. 进行协议编解码
 * 4. 构建 WebSocket 下行请求对象
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/13
 */
@Slf4j
@Component("websocketConverter")
public class WebSocketDownRequestConverter extends AbstractDownService<WebSocketDownRequest>
        implements DownRequestConverter<WebSocketDownRequest> {

    @Resource
    private WebSocketModuleInfo websocketModuleInfo;

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
        // 旧的转换逻辑已移至 WebSocketDownRequestConverter 的 convert 方法
        // 这里只做基本的 JSON 解析
        return JSONUtil.toBean(request, WebSocketDownRequest.class);
    }

    @Override
    public WebSocketDownRequest convert(UnifiedDownlinkCommand command, DownlinkContext<?> context) {
        try {
            log.debug("[WebSocket转换器][开始转换] productKey={}, deviceId={}, cmd={}",
                    command.getProductKey(), command.getDeviceId(), command.getCmd());

            // 1. 加载产品信息
            IoTProduct ioTProduct = getProduct(command.getProductKey());
            if (ioTProduct == null) {
                throw new IllegalArgumentException("产品不存在: " + command.getProductKey());
            }

            // 2. 加载设备信息（设备添加命令时可能不存在）
            IoTDeviceDTO device = null;
            
            // 只有在非设备添加命令时才要求设备必须存在
            if (command.getCmd() != cn.universal.common.constant.IoTConstant.DownCmd.DEV_ADD 
                    && command.getCmd() != cn.universal.common.constant.IoTConstant.DownCmd.DEV_ADDS) {
                device = getIoTDeviceDTO(
                        IoTDeviceQuery.builder()
                                .productKey(command.getProductKey())
                                .deviceId(command.getDeviceId())
                                .iotId(command.getIotId())
                                .build());

                if (device == null) {
                    throw new IllegalArgumentException(
                            "设备不存在: productKey=" + command.getProductKey() 
                            + ", deviceId=" + command.getDeviceId());
                }
            } else {
                // 对于设备添加命令，尝试加载但不强制存在
                device = getIoTDeviceDTO(
                        IoTDeviceQuery.builder()
                                .productKey(command.getProductKey())
                                .deviceId(command.getDeviceId())
                                .iotId(command.getIotId())
                                .build());
                log.debug("[WebSocket转换器] 设备添加命令，设备{}存在", 
                        device != null ? "" : "不");
            }

            // 3. 进行协议编解码（仅 DEV_FUNCTION 需要编码）
            String payload = null;
            
            // 只有功能调用命令才需要编码 payload
            if (command.getCmd() == cn.universal.common.constant.IoTConstant.DownCmd.DEV_FUNCTION
                    && command.getFunction() != null && !command.getFunction().isEmpty()) {
                String functionJson = JSONUtil.toJsonStr(command.getFunction());
                try {
                    payload = encodeWithShadow(command.getProductKey(), command.getDeviceId(), functionJson);
                    log.debug("[WebSocket转换器] DEV_FUNCTION 编码成功，payloadSize={}", payload.length());
                } catch (Exception e) {
                    log.warn("[WebSocket转换器] DEV_FUNCTION 编码失败，使用原始数据: {}", e.getMessage());
                    payload = functionJson;
                }
            } else {
                // 其他命令（DEV_ADD/DEV_UPDATE/DEV_DELETE 等）不需要编码
                log.debug("[WebSocket转换器] 命令 {} 不需要编码，payload 设为 null", command.getCmd());
                payload = null;
            }

            // 4. 构建 WebSocket 下行请求对象
            WebSocketDownRequest.WebSocketDownRequestBuilder<?, ?> builder = WebSocketDownRequest.builder()
                    .deviceId(command.getDeviceId())
                    .productKey(command.getProductKey())
                    .cmd(command.getCmd())
                    .ioTProduct(ioTProduct)  // ← 设置产品信息
                    .payload(payload)
                    .msgId(command.getMsgId())
                    .time(System.currentTimeMillis())
                    // WebSocket 特定字段
                    .sessionId(extractSessionId(context))
                    .createTime(LocalDateTime.now())
                    .needResponse(true)
                    .extras(buildExtras(command));
            
            // 如果设备存在则设置设备相关字段
            if (device != null) {
                builder.iotId(device.getIotId())
                       .ioTDeviceDTO(device);  // ← 设置设备信息
            } else {
                builder.iotId(command.getIotId());
            }
            
            WebSocketDownRequest downRequest = builder.build();

            // 设置公共数据（通过 data 字段）
            DownCommonData downCommonData = new DownCommonData();
            downCommonData.setConfiguration(parseProductConfigurationSafely(ioTProduct));
            downRequest.setData(JSONUtil.parseObj(JSONUtil.toJsonStr(downCommonData)));

            log.info("[WebSocket转换器][转换成功] deviceId={}, msgId={}, payloadSize={}, cmd={}",
                    command.getDeviceId(), command.getMsgId(), 
                    payload != null ? payload.length() : 0, command.getCmd());

            return downRequest;

        } catch (IllegalArgumentException e) {
            log.error("[WebSocket转换器][参数验证失败] {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[WebSocket转换器][转换失败]", e);
            throw new IllegalStateException("WebSocket 请求转换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取支持的协议代码
     *
     * @return 协议代码 (WEBSOCKET)
     */
    @Override
    public String supportedProtocol() {
        return websocketModuleInfo.getCode();
    }

    /**
     * 从下行上下文中提取会话 ID
     *
     * @param context 下行上下文
     * @return 会话 ID，如果不存在则返回空字符串
     */
    private String extractSessionId(DownlinkContext<?> context) {
        if (context == null) {
            return "";
        }

        // 尝试从上下文的属性中获取会话 ID
        Object sessionId = context.getAttribute("sessionId");
        if (sessionId != null) {
            return sessionId.toString();
        }

        return "";
    }

    /**
     * 构建扩展属性
     *
     * 用于传递协议相关的配置信息，如编码方式、超时设置等
     *
     * @param command 统一下行命令
     * @return 扩展属性映射
     */
    private Map<String, Object> buildExtras(UnifiedDownlinkCommand command) {
        Map<String, Object> extras = new HashMap<>();

        // 添加命令类型
        if (command.getCmd() != null) {
            extras.put("cmd", command.getCmd().getValue());
        }

        // 添加超时设置
        if (command.getMetadata() != null && command.getMetadata().getTimeout() != null) {
            extras.put("timeout", command.getMetadata().getTimeout());
        }

        // 添加优先级
        if (command.getMetadata() != null && command.getMetadata().getPriority() != null) {
            extras.put("priority", command.getMetadata().getPriority());
        }

        // 添加其他扩展属性（如果有）
        if (command.getExtensions() != null) {
            extras.putAll(command.getExtensions());
        }

        return extras;
    }
}
