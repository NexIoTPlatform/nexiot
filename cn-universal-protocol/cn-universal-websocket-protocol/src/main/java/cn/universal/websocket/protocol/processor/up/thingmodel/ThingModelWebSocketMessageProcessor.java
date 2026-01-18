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

import org.springframework.stereotype.Component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONObject;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import cn.universal.websocket.protocol.processor.up.WebSocketUPProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket物模型消息处理器
 *
 * <p>对应步骤FOUR：处理物模型消息的业务逻辑 - 属性上报处理 - 事件上报处理 - 触发业务规则 - 记录设备影子
 *
 * @version 2.0
 * @Author gitee.com/NexIoT
 * @since 2026/1/15
 */
@Slf4j(topic = "websocket")
@Component
public class ThingModelWebSocketMessageProcessor extends AbstratIoTService
    implements WebSocketUPProcessor {

  @Override
  public String getName() {
    return "WebSocket物模型消息处理器";
  }

  @Override
  public String getDescription() {
    return "处理物模型消息的业务逻辑";
  }

  @Override
  public int getOrder() {
    return 500; // 消息处理是第五步
  }

  @Override
  public boolean supports(WebSocketUPRequest request) {
    // 必须已经过编解码处理
    Object codecProcessed = request.getContextValue("codecProcessed");
    if (!(codecProcessed instanceof Boolean) || !(Boolean) codecProcessed) {
      return false;
    }

    // 检查是否有编解码后的请求列表
    List<BaseUPRequest> upRequestList = request.getUpRequestList();
    return CollUtil.isNotEmpty(upRequestList);
  }

  @Override
  public ProcessorResult process(WebSocketUPRequest request) {
    try {
      log.debug("[{}] 开始处理物模型消息转换，设备: {}", getName(), request.getDeviceId());

      // 1. 获取消息JSON
      JSONObject messageJson = (JSONObject) request.getContextValue("messageJson");
      if (messageJson == null) {
        log.warn("[{}] 消息JSON为空，将使用原始请求列表或创建默认请求", getName());
        // 改进：即使消息JSON为空，也不直接返回错误
        // 而是尝试使用已有的请求列表，或创建默认请求以支持消息入库
        List<BaseUPRequest> upRequestList = request.getUpRequestList();
        if (CollUtil.isNotEmpty(upRequestList)) {
          log.debug("[{}] 使用已有的请求列表，数量: {}", getName(), upRequestList.size());
          return ProcessorResult.CONTINUE;
        }
        // 创建默认请求以确保消息入库
        upRequestList = createDefaultRequest(request);
        if (CollUtil.isNotEmpty(upRequestList)) {
          request.setUpRequestList(upRequestList);
          return ProcessorResult.CONTINUE;
        }
        return ProcessorResult.SKIP;
      }

      // 2. 根据主题类型转换消息
      List<BaseUPRequest> upRequestList = convertThingModelMessage(request, messageJson);
      if (upRequestList == null || upRequestList.isEmpty()) {
        log.warn("[{}] 消息转换失败，将创建默认请求以支持入库", getName());
        // 改进：如果消息转换失败，也尝试创建默认请求而不是直接返回错误
        upRequestList = createDefaultRequest(request);
        if (CollUtil.isEmpty(upRequestList)) {
          log.error("[{}] 无法创建默认请求", getName());
          return ProcessorResult.ERROR;
        }
      }

      // 3. 设置转换结果
      request.setUpRequestList(upRequestList);
      request.setContextValue("convertedRequestCount", upRequestList.size());
      request.setContextValue("messageConverted", true);

      log.debug("[{}] 物模型消息转换完成，生成请求数量: {}", getName(), upRequestList.size());
      return ProcessorResult.CONTINUE;

    } catch (Exception e) {
      log.error("[{}] 物模型消息转换异常，设备: {}, 异常: ", getName(), request.getDeviceId(), e);
      return ProcessorResult.ERROR;
    }
  }

  /** 转换物模型消息 */
  private List<BaseUPRequest> convertThingModelMessage(
      WebSocketUPRequest request, JSONObject messageJson) {
    try {
      // 对于物模型消息，根据消息内容判断是属性还是事件
      // 如果包含"event"字段，则为事件消息；否则为属性消息
      if (messageJson.containsKey("event")) {
        return convertEventMessage(request, messageJson);
      } else {
        return convertPropertyMessage(request, messageJson);
      }
    } catch (Exception e) {
      log.error("[{}] 物模型消息转换异常: ", getName(), e);
      return null;
    }
  }

  /** 转换属性消息 格式：{"battery": "99", "ecl": "30", "switchStatus": 0} */
  private List<BaseUPRequest> convertPropertyMessage(
      WebSocketUPRequest request, JSONObject messageJson) {
    try {
      if (request.getUpRequestList() != null && request.getUpRequestList().size() > 0) {
        return request.getUpRequestList();
      }
      List<BaseUPRequest> upRequestList = new ArrayList<>();

      if (messageJson == null || messageJson.isEmpty()) {
        log.warn("[{}] 属性消息为空", getName());
        return upRequestList;
      }
      
      // 直接解析messageJson的所有键值对作为属性
      BaseUPRequest upRequest = getBaseUPRequest(request.getIoTDeviceDTO()).build();
      upRequest.setMessageType(MessageType.PROPERTIES);
      upRequest.setProperties(messageJson);
      upRequestList.add(upRequest);

      log.debug("[{}] 属性消息转换完成，属性数量: {}", getName(), upRequestList.size());
      return upRequestList;

    } catch (Exception e) {
      log.error("[{}] 属性消息转换异常: ", getName(), e);
      return null;
    }
  }

  /** 转换事件消息 格式：{"messageType": "EVENT", "event": "online"} */
  private List<BaseUPRequest> convertEventMessage(WebSocketUPRequest request, JSONObject messageJson) {
    try {
      if (request.getUpRequestList() != null && request.getUpRequestList().size() > 0) {
        return request.getUpRequestList();
      }
      List<BaseUPRequest> upRequestList = new ArrayList<>();

      String eventType = messageJson.getStr("event");
      if (eventType == null || eventType.trim().isEmpty()) {
        log.warn("[{}] 事件消息无有效事件类型", getName());
        return upRequestList;
      }

      BaseUPRequest upRequest = getBaseUPRequest(request.getIoTDeviceDTO()).build();
      upRequest.setMessageType(MessageType.EVENT);
      upRequest.setEvent(eventType);

      // 设置事件数据（如果存在）
      JSONObject eventData = messageJson.getJSONObject("data");
      if (eventData != null) {
        upRequest.setData(eventData);
      }

      upRequestList.add(upRequest);

      log.debug("[{}] 事件消息转换完成，事件类型: {}", getName(), eventType);
      return upRequestList;

    } catch (Exception e) {
      log.error("[{}] 事件消息转换异常: ", getName(), e);
      return null;
    }
  }

  @Override
  public int getPriority() {
    return 10; // 物模型消息处理优先级最高
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  /**
   * 创建默认请求 - 当物模型消息解析失败时，作为降级方案
   * 这样可以支持即使物模型未定义也能入库消息
   */
  private List<BaseUPRequest> createDefaultRequest(WebSocketUPRequest request) {
    try {
      List<BaseUPRequest> upRequestList = new ArrayList<>();
      
      if (request.getIoTDeviceDTO() == null) {
        log.warn("[{}] 设备信息为空，无法创建默认请求", getName());
        return upRequestList;
      }

      // 创建默认的属性消息请求
      BaseUPRequest upRequest = getBaseUPRequest(request.getIoTDeviceDTO()).build();
      upRequest.setMessageType(MessageType.PROPERTIES);
      
      // 尝试从原始payload中提取数据
      String payload = request.getPayload();
      if (cn.hutool.core.util.StrUtil.isNotBlank(payload)) {
        try {
          JSONObject payloadJson = cn.hutool.json.JSONUtil.parseObj(payload);
          upRequest.setProperties(payloadJson);
        } catch (Exception e) {
          log.debug("[{}] 原始payload解析失败，设置为data: {}", getName(), e.getMessage());
          // 如果不是JSON，设置为data字段
          upRequest.setData(new cn.hutool.json.JSONObject().set("raw", payload));
        }
      }

      upRequestList.add(upRequest);
      log.info("[{}] 创建默认请求以支持消息入库，iotId: {}", getName(), upRequest.getIotId());
      return upRequestList;

    } catch (Exception e) {
      log.error("[{}] 创建默认请求异常: ", getName(), e);
      return new ArrayList<>();
    }
  }
}
