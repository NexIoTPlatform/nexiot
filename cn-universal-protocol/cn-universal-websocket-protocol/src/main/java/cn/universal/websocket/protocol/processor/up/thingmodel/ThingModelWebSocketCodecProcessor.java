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

package cn.universal.websocket.protocol.processor.up.thingmodel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
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
 * WebSocket物模型编解码处理器
 *
 * <p>对应步骤THREE：如果是物模型设备，直接解析JSON格式的物模型数据
 *
 * <p>物模型编解码特点： - 消息格式已标准化，直接可用 - 支持属性、事件、服务三种消息类型 - JSON格式数据无需额外编解码
 *
 * @version 2.0
 * @Author gitee.com/NexIoT
 * @since 2026/1/15
 */
@Slf4j(topic = "websocket")
@Component
public class ThingModelWebSocketCodecProcessor extends AbstratIoTService
    implements WebSocketUPProcessor {

  @Autowired private IoTDeviceShadowService ioTDeviceShadowService;

  @Override
  public String getName() {
    return "WebSocket物模型编解码处理器";
  }

  @Override
  public String getDescription() {
    return "处理物模型数据的编解码转换";
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

    // 检查是否为物模型模式
    Object productInfo = request.getContextValue("productInfo");
    if (productInfo instanceof java.util.Map) {
      java.util.Map<String, Object> info = (java.util.Map<String, Object>) productInfo;
      String thingModel = (String) info.get("thingModel");
      // 如果定义了物模型，优先使用物模型编解码
      if (StrUtil.isNotBlank(thingModel)) {
        return true;
      }
      // 即使物模型未定义，也作为降级选项尝试处理
      // 这样可以支持消息的入库
      log.debug("[{}] 物模型未定义，物模型编解码器将作为备选方案", getName());
      return true;
    }

    return false;
  }

  @Override
  public ProcessorResult process(WebSocketUPRequest request) {
    try {
      log.debug("[{}] 开始处理物模型编解码，设备: {}", getName(), request.getDeviceId());
      
      // 1. 尝试产品编解码器解码
      List<BaseUPRequest> upRequestList = tryProductCodec(request);
      
      request.setUpRequestList(upRequestList);
      request.setContextValue("codecProcessedCount", upRequestList.size());
      request.setContextValue("codecProcessed", true);
      
      log.debug("[{}] 物模型编解码处理完成，生成请求数量: {}", getName(), upRequestList.size());
      return ProcessorResult.CONTINUE;
      
    } catch (Exception e) {
      log.error("[{}] 物模型编解码处理异常，设备: {}, 异常: ", getName(), request.getDeviceId(), e);
      return ProcessorResult.ERROR;
    }
  }

  /** 尝试使用产品编解码器解码 */
  private List<BaseUPRequest> tryProductCodec(WebSocketUPRequest request) {
    try {
      String productKey = request.getProductKey();
      String payload = request.getPayload();
      IoTProduct ioTProduct = request.getIoTProduct();
      
      // 编解码带影子
      codecWithShadow(request, ioTProduct);
      
      log.debug("[{}] 尝试产品编解码器解码 - 产品: {}", getName(), productKey);
      
      // 调用产品编解码器
      List<WebSocketUPRequest> decodedList =
          decode(productKey, payload, request.getCodecContext(), WebSocketUPRequest.class);
      
      if (CollUtil.isNotEmpty(decodedList)) {
        log.debug("[{}] 产品编解码器解码成功，解码数量: {}", getName(), decodedList.size());
        List<BaseUPRequest> upRequestList = new ArrayList<>();
        for (WebSocketUPRequest codecResult : decodedList) {
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

  /** 转换编解码器结果 - 对标MQTT实现，处理设备存在和不存在两种情况 */
  private BaseUPRequest convertCodecResult(WebSocketUPRequest request, WebSocketUPRequest codecResult) {
    try {
      if (codecResult == null) {
        return null;
      }
      IoTDeviceDTO deviceDTO = request.getIoTDeviceDTO();
      
      // 构建编解码结果
      JSONObject messageJson = parseJsonPayload(request.getPayload());
      
      BaseUPRequest upRequest;
      
      if (deviceDTO != null) {
        // 设备存在：使用标准的buildCodecNotNullBean方法
        BaseUPRequest.BaseUPRequestBuilder<?, ?> builder = getBaseUPRequest(deviceDTO);
        buildCodecNotNullBean(messageJson, deviceDTO, codecResult, builder);
        upRequest = builder.build();
      } else {
        // 设备不存在：构建简化的BaseUPRequest，不调用会访问deviceDTO的方法
        log.warn("[{}] 设备信息未填充，将生成不含设备详情的BaseUPRequest", getName());
        
        BaseUPRequest.BaseUPRequestBuilder<?, ?> builder = BaseUPRequest.builder()
                .iotId(codecResult.getIotId() != null ? codecResult.getIotId() : request.getIotId())
                .productKey(request.getProductKey())
                .deviceName(request.getDeviceName())
                .messageType(codecResult.getMessageType() != null ? codecResult.getMessageType() : IoTConstant.MessageType.PROPERTIES);
        
        // 直接设置编解码器返回的properties和data，不调用buildCodecNotNullBean
        if (codecResult.getProperties() != null && !codecResult.getProperties().isEmpty()) {
          builder.properties(codecResult.getProperties());
        } else if (messageJson != null && messageJson.containsKey("properties")) {
          builder.properties(messageJson.getJSONObject("properties"));
        }
        
        if (codecResult.getData() != null && !codecResult.getData().isEmpty()) {
          builder.data(codecResult.getData());
        } else if (messageJson != null && messageJson.containsKey("data")) {
          builder.data(messageJson.getJSONObject("data"));
        }
        
        // 设置时间戳
        if (codecResult.getTs() != null && NumberUtil.isLong(codecResult.getTs())) {
          builder.time(Long.parseLong(codecResult.getTs()));
        } else {
          builder.time(System.currentTimeMillis());
        }
        
        upRequest = builder.build();
      }
      
      // WebSocket 不需要设置 MQTT 特有的 replyPayload、qos 等字段
      
      log.debug("[{}] 编解码器结果转换成功", getName());
      return upRequest;
    } catch (Exception e) {
      log.error("[{}] 编解码器结果转换异常: ", getName(), e);
      return null;
    }
  }

  @Override
  public int getPriority() {
    return 10; // 物模型编解码优先级最高
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
