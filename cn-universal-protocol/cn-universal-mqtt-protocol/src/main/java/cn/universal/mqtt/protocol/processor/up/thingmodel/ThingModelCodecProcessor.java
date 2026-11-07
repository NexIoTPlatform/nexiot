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

package cn.universal.mqtt.protocol.processor.up.thingmodel;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.mqtt.protocol.config.MqttConstant;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.entity.ProcessingStage;
import cn.universal.mqtt.protocol.processor.MqttMessageProcessor;
import cn.universal.mqtt.protocol.topic.MQTTTopicManager;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 物模型编解码处理器
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
@Component
public class ThingModelCodecProcessor extends AbstratIoTService implements MqttMessageProcessor {

  @Autowired private IoTDeviceShadowService ioTDeviceShadowService;

  @Override
  public String getName() {
    return "物模型编解码处理器";
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
  public boolean supports(MQTTUPRequest request) {
    if (request.getIoTDeviceDTO() == null || request.getIoTProduct() == null) {
      return false;
    }

    // 优先检查第三方MQTT配置的主题分类
    MqttConstant.TopicCategory configuredCategory = request.getContextValue("topicCategory");
    if (configuredCategory != null) {
      boolean supported = configuredCategory == MqttConstant.TopicCategory.THING_MODEL;
      if (supported) {
        log.debug("[{}] 通过配置的主题分类匹配: category={}", getName(), configuredCategory);
      }
      return supported;
    }

    // 回退到系统内置的主题匹配逻辑
    return MqttConstant.TopicCategory.THING_MODEL.equals(
        MQTTTopicManager.matchCategory(request.getUpTopic()));
  }

  @Override
  public ProcessorResult process(MQTTUPRequest request) {
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
  private List<BaseUPRequest> tryProductCodec(MQTTUPRequest request) {
    try {
      String productKey = request.getProductKey();
      String payload = request.getPayload();
      IoTProduct ioTProduct = request.getIoTProduct();
      codecWithShadow(request, ioTProduct);
      log.debug("[{}] 尝试产品编解码器解码 - 产品: {}", getName(), productKey);
      // 调用产品编解码器
      List<MQTTUPRequest> decodedList =
          decode(productKey, payload, request.getCodecContext(), MQTTUPRequest.class);
      if (CollUtil.isNotEmpty(decodedList)) {
        log.debug("[{}] 产品编解码器解码成功，解码数量: {}", getName(), decodedList.size());
        List<BaseUPRequest> upRequestList = new ArrayList<>();
        for (MQTTUPRequest codecResult : decodedList) {
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
  private void codecWithShadow(MQTTUPRequest request, IoTProduct ioTProduct) {
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
  private BaseUPRequest convertCodecResult(MQTTUPRequest request, MQTTUPRequest codecResult) {
    try {
      if (codecResult == null) {
        return null;
      }
      IoTDeviceDTO deviceDTO = request.getIoTDeviceDTO();
      BaseUPRequest.BaseUPRequestBuilder<?, ?> builder = getBaseUPRequest(deviceDTO);
      // 构建编解码结果
      JSONObject messageJson = parseJsonPayload(request.getPayload());
      buildCodecNotNullBean(messageJson, deviceDTO, codecResult, builder);
      // 注释：不再设置到 request.replyPayload，避免 BaseReplyProcessor 重复发送
      // 现在统一由 MqttUPBatchAckUPProcessor 处理 upRequestList 中的 replyPayload
      // if (StrUtil.isNotBlank(codecResult.getReplyPayload())) {
      //   request.setReplyPayload(codecResult.getReplyPayload());
      // }
      if (codecResult.getSubDevice() != null) {
        request.setSubDevice(codecResult.getSubDevice());
      }
      if (StrUtil.isNotBlank(codecResult.getDownTopic())) {
        request.setDownTopic(codecResult.getDownTopic());
      }
      // 默认为1
      if (codecResult.getQos() != 1) {
        request.setQos(codecResult.getQos());
      }
      BaseUPRequest upRequest = builder.build();

      log.debug("[{}] 编解码器结果转换成功", getName());
      return upRequest;
    } catch (Exception e) {
      log.error("[{}] 编解码器结果转换异常: ", getName(), e);
      return null;
    }
  }

  /** 统计编解码结果 */
  private void collectCodecStatistics(MQTTUPRequest request) {
    try {
      Boolean codecSuccess = (Boolean) request.getContextValue("codecSuccess");
      String codecType = (String) request.getContextValue("codecType");
      Integer processedCount = (Integer) request.getContextValue("codecProcessedCount");

      request.setContextValue(
          "codecStatistics",
          "成功: " + codecSuccess + ", 类型: " + codecType + ", 数量: " + processedCount);

      log.debug(
          "[{}] 编解码统计 - 成功: {}, 类型: {}, 数量: {}",
          getName(),
          codecSuccess,
          codecType,
          processedCount != null ? processedCount : 0);

    } catch (Exception e) {
      log.warn("[{}] 编解码统计异常: ", getName(), e);
    }
  }

  @Override
  public boolean preCheck(MQTTUPRequest request) {
    // 检查必要的数据
    return request.getIoTDeviceDTO() != null
        && request.getIoTProduct() != null
        && request.getPayload() != null;
  }

  @Override
  public void postProcess(MQTTUPRequest request, ProcessorResult result) {
    if (result == ProcessorResult.CONTINUE) {
      // 收集编解码统计信息
      collectCodecStatistics(request);

      Integer processedCount = (Integer) request.getContextValue("codecProcessedCount");
      Boolean codecSuccess = (Boolean) request.getContextValue("codecSuccess");

      log.debug(
          "[{}] 物模型编解码处理成功 - 设备: {}, 成功: {}, 生成请求: {}",
          getName(),
          request.getDeviceId(),
          codecSuccess,
          processedCount != null ? processedCount : 0);
    } else {
      log.warn("[{}] 物模型编解码处理失败 - 设备: {}, 结果: {}", getName(), request.getDeviceId(), result);
    }
  }

  @Override
  public void onError(MQTTUPRequest request, Exception e) {
    log.error("[{}] 物模型编解码处理异常，设备: {}, 异常: ", getName(), request.getDeviceId(), e);
    request.setError("物模型编解码处理失败: " + e.getMessage());
  }

  @Override
  public int getPriority() {
    return 5; // 物模型处理优先级中等
  }
}
