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

package cn.universal.websocket.protocol.processor.up.passthrough;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest.ProcessingStage;
import cn.universal.websocket.protocol.processor.up.WebSocketUPProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket透传编解码处理器
 *
 * <p>对应步骤THREE：如果是透传设备，则需要调用编解码，确保消息能被物模型正确识别
 *
 * <p>透传编解码特点： - 调用产品配置的编解码器 - 将原始数据转换为标准的物模型格式 - 兼容WebSocket报文按照本平台格式的处理 - 支持多种数据格式的解码
 *
 * @version 2.0
 * @Author gitee.com/NexIoT
 * @since 2026/1/15
 */
@Slf4j(topic = "websocket")
@Component
public class PassthroughWebSocketCodecProcessor extends AbstratIoTService
    implements WebSocketUPProcessor {

  @Autowired private IoTDeviceShadowService ioTDeviceShadowService;

  @Override
  public String getName() {
    return "WebSocket透传编解码处理器";
  }

  @Override
  public String getDescription() {
    return "处理透传数据的编解码转换";
  }

  @Override
  public int getOrder() {
    return 300; // 编解码处理是第三步
  }

  @Override
  public boolean supports(WebSocketUPRequest request) {
    if (request.getIoTDeviceDTO() == null || request.getIoTProduct() == null) {
      return false;
    }

    // 检查是否为透传模式
    Object productInfo = request.getContextValue("productInfo");
    if (productInfo instanceof java.util.Map) {
      java.util.Map<String, Object> info = (java.util.Map<String, Object>) productInfo;
      String thingModel = (String) info.get("thingModel");
      return StrUtil.isBlank(thingModel);
    }

    return false;
  }

  @Override
  public ProcessorResult process(WebSocketUPRequest request) {
    try {
      log.debug("[{}] 开始处理透传编解码，SessionID: {}", getName(), request.getSessionId());

      // 1. 尝试产品编解码器解码
      List<BaseUPRequest> upRequestList = tryProductCodec(request);

      // 2. 如果编解码器解码失败，尝试平台格式兼容
      if (CollUtil.isEmpty(upRequestList)) {
        upRequestList = tryPlatformFormat(request);
      }

      // 3. 如果都失败，创建透传原始数据请求
      if (CollUtil.isEmpty(upRequestList)) {
        upRequestList = createRawDataRequest(request);
      }

      // 4. 设置解码结果
      request.setUpRequestList(upRequestList);
      request.setContextValue("codecProcessedCount", upRequestList.size());
      request.setContextValue("codecProcessed", true);

      log.debug("[{}] 透传编解码处理完成，生成请求数量: {}", getName(), upRequestList.size());
      return ProcessorResult.CONTINUE;

    } catch (Exception e) {
      log.error(
          "[{}] 透传编解码处理异常，SessionID: {}, 异常: ",
          getName(),
          request.getSessionId(),
          e);
      return ProcessorResult.ERROR;
    }
  }

  /** 尝试使用产品编解码器解码 */
  private List<BaseUPRequest> tryProductCodec(WebSocketUPRequest request) {
    try {
      String productKey = request.getProductKey();
      String payload = request.getPayload();
      IoTProduct ioTProduct = request.getIoTProduct();
      
      codecWithShadow(request, ioTProduct);
      
      log.debug("[{}] 尝试产品编解码器解码 - 产品: {}", getName(), productKey);
      
      // 调用产品编解码器
      List<WebSocketUPRequest> decodedList =
          decode(productKey, payload, request.getCodecContext(), WebSocketUPRequest.class);

      if (CollUtil.isNotEmpty(decodedList)) {
        log.debug("[{}] 产品编解码器解码成功，解码数量: {}", getName(), decodedList.size());

        List<BaseUPRequest> upRequestList = new ArrayList<>();
        for (WebSocketUPRequest codecResult : decodedList) {
          // 诊断日志：检查反序列化后的codecResult状态
          log.debug("[{}] 反序列化后的codecResult - messageType: {}, properties: {}, data: {}, payload长度: {}",
              getName(), 
              codecResult.getMessageType(),
              codecResult.getProperties() != null ? codecResult.getProperties().size() + "个字段" : "null",
              codecResult.getData() != null ? codecResult.getData().size() + "个字段" : "null",
              codecResult.getPayload() != null ? codecResult.getPayload().length() : 0);
          
          BaseUPRequest upRequest = convertCodecResult(request, codecResult);
          if (upRequest != null) {
            upRequestList.add(upRequest);
          }
        }
        request.setContextValue("codecSuccess", true);
        request.setContextValue("codecType", "PRODUCT_CODEC");
        request.setStage(ProcessingStage.DECODED);
        return upRequestList;
      } else {
        log.debug("[{}] 产品编解码器未返回解码结果", getName());
        request.setContextValue("codecSuccess", false);
        request.setStage(ProcessingStage.DECODED);
        JSONObject jsonObject = new JSONObject();
        if (JSONUtil.isTypeJSON(request.getPayload())) {
          jsonObject = JSONUtil.parseObj(request.getPayload());
        }
        return Stream.of(buildCodecNullBean(jsonObject, request)).collect(Collectors.toList());
      }

    } catch (Exception e) {
      log.warn("[{}] 产品编解码器解码异常: ", getName(), e);
      request.setContextValue("codecSuccess", false);
      request.setContextValue("codecError", e.getMessage());
      return null;
    }
  }

  /** 编解码是否带影子 */
  private void codecWithShadow(WebSocketUPRequest request, IoTProduct ioTProduct) {
    if (StringUtils.isNotEmpty(ioTProduct.getConfiguration())) {
      JSONObject jsonObject = JSONUtil.parseObj(ioTProduct.getConfiguration());
      // 上行报文是否需要附加影子
      Boolean requireUpShadow = jsonObject.getBool("requireUpShadow", false);
      if (requireUpShadow) {
        JSONObject shadowObj =
            ioTDeviceShadowService.getDeviceShadowObj(
                request.getProductKey(), request.getDeviceId());
        if (ObjectUtil.isNotNull(shadowObj)) {
          request.setShadow(shadowObj);
          request.setCodecContextValue("shadow", shadowObj);
          request.setCodecContextValue("productConfig", jsonObject);
        }
      }
      // 是否自定义了downTopic
      String downTopic = jsonObject.getStr("downTopic", null);
      if (StringUtils.isNotEmpty(downTopic)) {
        request.setDownTopic(downTopic);
      }
    }
  }

  /** 转换编解码器结果 */
  private BaseUPRequest convertCodecResult(WebSocketUPRequest request, WebSocketUPRequest codecResult) {
    try {
      if (codecResult == null) {
        return null;
      }
      IoTDeviceDTO deviceDTO = request.getIoTDeviceDTO();
      BaseUPRequest.BaseUPRequestBuilder<?, ?> builder = getBaseUPRequest(deviceDTO);

      // 关键修复：使用编解码器返回的payload，而不是原始请求的payload
      // 编解码器已经将原始数据转换为包含properties/data的JSON结构
      String payloadForParsing = StrUtil.isNotBlank(codecResult.getPayload()) 
          ? codecResult.getPayload() 
          : request.getPayload();
      JSONObject messageJson = parseJsonPayload(payloadForParsing);
      
      // 对标 MQTT 的处理逻辑，从编解码结果中提取数据
      log.debug("[{}] 编解码结果 - messageType: {}, codecResult.properties: {}, codecResult.payload 长度: {}, messageJson.properties: {}",
          getName(), codecResult.getMessageType(), 
          codecResult.getProperties() != null, 
          codecResult.getPayload() != null ? codecResult.getPayload().length() : 0,
          messageJson != null && messageJson.containsKey("properties"));
      
      buildCodecNotNullBean(messageJson, deviceDTO, codecResult, builder);

      BaseUPRequest upRequest = builder.build();

      log.debug("[{}] 编解码器结果转换成功，消息类型: {}, data字段: {}, properties字段: {}", 
          getName(), upRequest.getMessageType(), 
          upRequest.getData() != null ? "已设置" : "为空",
          upRequest.getProperties() != null ? "已设置" : "为空");
      return upRequest;
    } catch (Exception e) {
      log.error("[{}] 编解码器结果转换异常: ", getName(), e);
      return null;
    }
  }

  /** 尝试平台格式兼容 */
  private List<BaseUPRequest> tryPlatformFormat(WebSocketUPRequest request) {
    try {
      String messageContent = request.getPayload();

      log.debug("[{}] 尝试平台格式兼容解析", getName());

      // 解析WebSocket报文按照本平台格式
      JSONObject jsonObject = parseJsonPayload(messageContent);
      if (jsonObject == null || jsonObject.isEmpty()) {
        log.debug("[{}] 平台格式解析失败", getName());
        return null;
      }

      // 创建BaseUPRequest
      BaseUPRequest upRequest = buildPlatformFormatRequest(request, jsonObject);
      if (upRequest == null) {
        return null;
      }

      List<BaseUPRequest> upRequestList = new ArrayList<>();
      upRequestList.add(upRequest);

      log.debug("[{}] 平台格式兼容解析成功", getName());
      request.setContextValue("codecSuccess", true);
      request.setContextValue("codecType", "PLATFORM_FORMAT");
      return upRequestList;

    } catch (Exception e) {
      log.warn("[{}] 平台格式兼容解析异常: ", getName(), e);
      return null;
    }
  }

  /** 构建平台格式请求 */
  private BaseUPRequest buildPlatformFormatRequest(WebSocketUPRequest request, JSONObject jsonObject) {
    try {
      BaseUPRequest upRequest = buildCodecNullBean(jsonObject, request);

      log.debug("[{}] 平台格式请求构建成功", getName());
      return upRequest;

    } catch (Exception e) {
      log.error("[{}] 平台格式请求构建异常: ", getName(), e);
      return null;
    }
  }

  /** 创建原始数据请求 */
  private List<BaseUPRequest> createRawDataRequest(WebSocketUPRequest request) {
    try {
      List<BaseUPRequest> result = new ArrayList<>();
      BaseUPRequest upRequest = new BaseUPRequest();
      upRequest.setIotId(request.getIotId());
      upRequest.setProductKey(request.getProductKey());
      
      // 将原始数据包装为JSON格式
      JSONObject rawData = new JSONObject();
      rawData.set("rawData", request.getPayload());
      rawData.set("timestamp", System.currentTimeMillis());
      upRequest.setData(rawData);
      
      result.add(upRequest);
      log.debug("[{}] 创建原始数据请求", getName());
      return result;
    } catch (Exception e) {
      log.error("[{}] 创建原始数据请求失败: {}", getName(), e.getMessage());
      return null;
    }
  }

  @Override
  public int getPriority() {
    return 5; // 透传编解码优先级中等
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
