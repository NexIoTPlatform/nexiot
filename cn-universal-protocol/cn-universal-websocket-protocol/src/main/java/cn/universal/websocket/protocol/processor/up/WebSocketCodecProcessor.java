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

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.core.message.UPRequest;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 编解码处理器 - 处理消息的编码和解码
 *
 * <p>支持多种解码策略：
 * 1. 产品编解码器解码（优先）
 * 2. Base64解码
 * 3. JSON格式化
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2026/1/14
 */
@Slf4j
@Component
public class WebSocketCodecProcessor extends AbstratIoTService implements WebSocketUPProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "WebSocket编解码处理器";
    }

    @Override
    public String getDescription() {
        return "处理WebSocket消息的编解码转换";
    }

    @Override
    public ProcessorResult process(WebSocketUPRequest request) {
        try {
            log.debug("[{}] 开始处理编解码，SessionID: {}", getName(), request.getSessionId());

            // 1. 尝试产品编解码器解码（如果有productKey）
            List<BaseUPRequest> upRequestList = tryProductCodec(request);

            // 2. 如果编解码器解码失败，尝试Base64解码
            if (CollUtil.isEmpty(upRequestList)) {
                tryBase64Decode(request);
            }

            // 3. 格式化JSON（如果是JSON格式）
            tryJsonFormat(request);

            // 4. 设置解码结果到request对象中，供后续消息处理器使用
            if (CollUtil.isNotEmpty(upRequestList)) {
                request.setUpRequestList(upRequestList);
                request.setContextValue("codecProcessedCount", upRequestList.size());
                request.setContextValue("codecProcessed", true);
                request.setStage(WebSocketUPRequest.ProcessingStage.DECODED);
                log.debug("[{}] 编解码处理完成，生成请求数量: {}, 已设置到request.upRequestList", getName(), upRequestList.size());
            } else {
                log.debug("[{}] 未使用产品编解码器，保持原始消息", getName());
            }

            return ProcessorResult.CONTINUE;

        } catch (Exception e) {
            log.error("[{}] 编解码处理异常，SessionID: {}", getName(), request.getSessionId(), e);
            request.setContextValue("codecError", e.getMessage());
            return ProcessorResult.ERROR;
        }
    }

    @Override
    public boolean supports(WebSocketUPRequest request) {
        // 所有消息都支持编解码处理
        return true;
    }

    @Override
    public boolean preCheck(WebSocketUPRequest request) {
        return request.getPayload() != null && !request.getPayload().isEmpty()
            && request.getProductKey() != null;
    }

    @Override
    public void postProcess(WebSocketUPRequest request, ProcessorResult result) {
        if (result == ProcessorResult.CONTINUE) {
            Boolean codecSuccess = (Boolean) request.getContextValue("codecSuccess");
            if (codecSuccess != null && codecSuccess) {
                log.debug("[{}] 编解码处理成功", getName());
            }
        } else if (result == ProcessorResult.ERROR) {
            log.warn("[{}] 编解码处理失败", getName());
        }
    }

    @Override
    public void onError(WebSocketUPRequest request, Exception e) {
        log.error("[{}] 编解码异常，SessionID: {}，异常: {}",
                getName(), request.getSessionId(), e.getMessage());
        request.setError("编解码失败: " + e.getMessage());
        request.setContextValue("codecError", e.getMessage());
    }

    @Override
    public int getOrder() {
        return 300; // 编解码处理是第三步
    }

    @Override
    public int getPriority() {
        return 10; // 编解码优先级较高
    }

    /**
     * 尝试使用产品编解码器解码
     */
    private List<BaseUPRequest> tryProductCodec(WebSocketUPRequest request) {
        try {
            String productKey = request.getProductKey();
            
            // 如果没有productKey，跳过产品编解码器
            if (StrUtil.isBlank(productKey)) {
                log.debug("[{}] 未配置productKey，跳过产品编解码器", getName());
                return null;
            }

            String payload = request.getPayload();
            long codecStartTime = System.currentTimeMillis();
            
            log.debug("[{}] 尝试产品编解码器解码 - ProductKey: {}", getName(), productKey);

            // 调用产品编解码器（使用UPRequest基类）
            List<UPRequest> decodedList = decode(productKey, payload, request.getCodecContext(), UPRequest.class);

            if (CollUtil.isNotEmpty(decodedList)) {
                long codecElapsedTime = System.currentTimeMillis() - codecStartTime;
                
                // 对标MQTT的编解码日志格式
                StringBuilder decodedInfo = new StringBuilder();
                decodedInfo.append("[");
                for (int i = 0; i < decodedList.size(); i++) {
                    if (i > 0) decodedInfo.append(", ");
                    UPRequest item = decodedList.get(i);
                    decodedInfo.append("{\"messageType\":\"").append(item.getMessageType()).append("\"");
                    if (item.getProperties() != null && !item.getProperties().isEmpty()) {
                        decodedInfo.append(",\"properties\":{");
                        item.getProperties().forEach((k, v) -> {
                            decodedInfo.append("\"").append(k).append("\":").append(JSONUtil.toJsonStr(v)).append(",");
                        });
                        if (item.getProperties().size() > 0) {
                            decodedInfo.setLength(decodedInfo.length() - 1); // 移除最后的逗号
                        }
                        decodedInfo.append("}");
                    }
                    decodedInfo.append("}");
                }
                decodedInfo.append("]");
                
                // 输出对标MQTT的日志格式
                log.info("[{}] 产品编号={} 原始报文={} 解码={} 耗时={}ms",
                        getName(),
                        productKey,
                        payload,
                        decodedInfo.toString(),
                        codecElapsedTime);

                List<BaseUPRequest> upRequestList = new ArrayList<>();
                for (UPRequest codecResult : decodedList) {
                    // 诊断：打印反序列化后的结果
                    log.debug("[{}] 反序列化后的codecResult - messageType: {}, properties: {}, data: {}, payload长度: {}",
                            getName(),
                            codecResult.getMessageType(),
                            codecResult.getProperties() != null ? codecResult.getProperties().size() + "字段" : "null",
                            codecResult.getData() != null ? codecResult.getData().size() + "字段" : "null",
                            codecResult.getPayload() != null ? codecResult.getPayload().length() : 0);
                    
                    BaseUPRequest upRequest = convertCodecResult(request, codecResult);
                    if (upRequest != null) {
                        upRequestList.add(upRequest);
                    }
                }

                request.setContextValue("codecSuccess", true);
                request.setContextValue("codecType", "PRODUCT_CODEC");
                log.debug("[{}] 编解码后生成的BaseUPRequest列表，数量: {}", getName(), upRequestList.size());
                return upRequestList;

            } else {
                log.debug("[{}] 产品编解码器未返回解码结果", getName());
                request.setContextValue("codecSuccess", false);
                return null;
            }

        } catch (Exception e) {
            log.warn("[{}] 产品编解码器解码异常: {}", getName(), e.getMessage());
            request.setContextValue("codecSuccess", false);
            request.setContextValue("codecError", e.getMessage());
            return null;
        }
    }

    /**
     * 转换编解码器结果 - 对标MQTT实现，使用buildCodecNotNullBean()设置properties和data
     */
    private BaseUPRequest convertCodecResult(WebSocketUPRequest request, UPRequest codecResult) {
        try {
            IoTDeviceDTO deviceDTO = request.getIoTDeviceDTO();
            
            // 防御性检查：deviceDTO不能为null（对标MQTT处理逻辑）
            if (deviceDTO == null) {
                log.error("[{}] 设备信息未填充，无法转换编解码结果。productKey={}, iotId={}",
                        getName(), request.getProductKey(), request.getIotId());
                return null;
            }
            
            BaseUPRequest.BaseUPRequestBuilder<?, ?> builder = BaseUPRequest.builder()
                    .iotId(StrUtil.isNotBlank(codecResult.getDeviceId()) ? codecResult.getDeviceId() : request.getIotId())
                    .productKey(request.getProductKey())
                    .deviceName(request.getDeviceName());

            // 关键修复：使用编解码器返回的payload（包含转换后的properties/data）
            String payloadForParsing = codecResult.getPayload() != null 
                    ? codecResult.getPayload() 
                    : request.getPayload();
            JSONObject messageJson = parseJsonPayload(payloadForParsing);

            // 诊断日志
            log.debug("[{}] 转换编解码结果 - messageType: {}, codecResult.properties: {}, messageJson中是否有properties: {}",
                    getName(), codecResult.getMessageType(),
                    codecResult.getProperties() != null ? codecResult.getProperties().size() + "字段" : "null",
                    messageJson != null && messageJson.containsKey("properties"));

            // 调用buildCodecNotNullBean方法设置properties和data（对标MQTT实现）
            buildCodecNotNullBean(messageJson, deviceDTO, codecResult, builder);

            BaseUPRequest upRequest = builder.build();
            log.debug("[{}] 编解码器结果转换成功 - messageType: {}, data: {}, properties: {}",
                    getName(), upRequest.getMessageType(),
                    upRequest.getData() != null ? upRequest.getData().size() + "字段" : "null",
                    upRequest.getProperties() != null ? upRequest.getProperties().size() + "字段" : "null");
            return upRequest;

        } catch (Exception e) {
            log.error("[{}] 编解码器结果转换失败: ", getName(), e);
            return null;
        }
    }

    /**
     * 尝试Base64解码
     */
    private void tryBase64Decode(WebSocketUPRequest request) {
        try {
            String payload = request.getPayload();

            // 检查是否是Base64编码的消息
            if (isBase64Encoded(payload)) {
                log.debug("[{}] 检测到Base64编码，SessionID: {}", getName(), request.getSessionId());
                String decoded = decodeBase64(payload);
                request.setPayload(decoded);
                request.setContextValue("base64Decoded", true);
                log.debug("[{}] Base64解码成功，SessionID: {}", getName(), request.getSessionId());
            }
        } catch (Exception e) {
            log.debug("[{}] Base64解码失败，保持原始消息: {}", getName(), e.getMessage());
        }
    }

    /**
     * 尝试JSON格式化
     */
    private void tryJsonFormat(WebSocketUPRequest request) {
        try {
            String payload = request.getPayload();

            // 验证JSON格式
            if (isJsonFormat(payload)) {
                // 格式化JSON（可选）
                JsonNode jsonNode = objectMapper.readTree(payload);
                String formatted = objectMapper.writeValueAsString(jsonNode);
                request.setPayload(formatted);
                request.setContextValue("jsonFormatted", true);
                log.debug("[{}] JSON格式化完成，SessionID: {}", getName(), request.getSessionId());
            }
        } catch (Exception e) {
            log.debug("[{}] JSON格式化失败，保持原始消息", getName());
        }
    }

    /**
     * 判断字符串是否是Base64编码
     */
    private boolean isBase64Encoded(String str) {
        try {
            // 简单判断：Base64字符串只包含A-Z、a-z、0-9、+、/、=
            return str.matches("^[A-Za-z0-9+/]*={0,2}$") && str.length() % 4 == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Base64解码
     */
    private String decodeBase64(String encoded) {
        byte[] decodedBytes = Base64.getDecoder().decode(encoded);
        return new String(decodedBytes);
    }

    /**
     * 判断字符串是否是JSON格式
     */
    private boolean isJsonFormat(String str) {
        try {
            objectMapper.readTree(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
