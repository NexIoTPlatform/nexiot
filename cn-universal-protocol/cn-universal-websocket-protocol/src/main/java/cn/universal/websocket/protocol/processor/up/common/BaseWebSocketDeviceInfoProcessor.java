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

package cn.universal.websocket.protocol.processor.up.common;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.query.IoTDeviceQuery;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest.ProcessingStage;
import cn.universal.websocket.protocol.processor.up.WebSocketUPProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket公共设备信息处理器基类
 *
 * <p>步骤ONE：实现设备 IoTDeviceDTO 和产品IoTProduct的信息回填，确保上报的消息产品或设备是存在的
 *
 * <p>两种消息类型的公共处理逻辑： - 设备和产品信息查询和回填 - 消息基础信息提取 - 会话标识和设备标识提取
 *
 * @version 2.0
 * @Author gitee.com/NexIoT
 * @since 2026/1/15
 */
@Slf4j(topic = "websocket")
public abstract class BaseWebSocketDeviceInfoProcessor extends AbstratIoTService
    implements WebSocketUPProcessor {

  @Override
  public String getName() {
    return "WebSocket设备信息处理器-" + getMessageType();
  }

  @Override
  public String getDescription() {
    return "处理" + getMessageType() + "消息的设备和产品信息回填";
  }

  @Override
  public int getOrder() {
    return 100; // 设备信息处理是第一步
  }

  @Override
  public ProcessorResult process(WebSocketUPRequest request) {
    try {
      // 第一步的初始化日志 - 对标MQTT的"[MQTT] 收到消息"
      log.info(
          "[WebSocket] 收到消息 - SessionID: {}, MessageID: {}, Payload长度: {}, Payload: {}",
          request.getSessionId(),
          request.getMessageId(),
          request.getPayload() != null ? request.getPayload().length() : 0,
          request.getPayload());

      log.info(
          "[{}] [Step 1/5 设备信息提取] 开始处理设备信息，SessionID: {}, MessageID: {}, Payload长度: {}",
          getName(),
          request.getSessionId(),
          request.getMessageId(),
          request.getPayload() != null ? request.getPayload().length() : 0);

      // 1. 提取并验证设备标识信息
      if (!extractAndValidateDeviceInfo(request)) {
        log.error(
            "[{}] [Step 1/5 FAILED] 设备标识提取失败，SessionID: {}, iotId: {}, productKey: {}",
            getName(),
            request.getSessionId(),
            request.getIotId(),
            request.getProductKey());
        return ProcessorResult.ERROR;
      }

      // 2. 查询和回填产品信息
      if (!fillProductInfo(request)) {
        log.error(
            "[{}] [Step 2/5 FAILED] 产品不存在或查询失败，productKey: {}",
            getName(),
            request.getProductKey());
        return ProcessorResult.ERROR;
      }

      // 3. 查询和回填设备信息
      if (!fillDeviceInfo(request)) {
        log.error(
            "[{}] [Step 3/5 FAILED] 设备不存在或查询失败，iotId: {}, productKey: {}",
            getName(),
            request.getDeviceId(),
            request.getProductKey());
        return ProcessorResult.ERROR;
      }

      // 4. 解析消息内容（物模型特定处理）
      if (!parseMessageContent(request)) {
        log.warn("[{}] [Step 4/5] 消息内容解析失败，但继续处理", getName());
        // 不返回错误，继续处理
      }

      // 5. 更新处理阶段和提取基础信息
      request.setStage(ProcessingStage.METADATA_EXTRACTED);
      extractBasicInfo(request);

      log.info(
          "[{}] [Step 1-5 SUCCESS] 设备信息处理完成，productKey: {}, iotId: {}, DeviceUniqueId: {}",
          getName(),
          request.getProductKey(),
          request.getDeviceId(),
          request.getProductKey() + ":" + request.getDeviceId());
      return ProcessorResult.CONTINUE;

    } catch (Exception e) {
      log.error(
          "[{}] [Process EXCEPTION] 设备信息处理异常，SessionID: {}, iotId: {}, productKey: {}",
          getName(),
          request.getSessionId(),
          request.getIotId(),
          request.getProductKey(),
          e);
      return ProcessorResult.ERROR;
    }
  }

  /**
   * 提取并验证设备标识信息 对标 MQTT 的 parseTopicAndExtractDevice()
   *
   * <p>从请求中获取 iotId 和 productKey，提取策略（优先级顺序）：
   * 1. 从请求已设置的字段中获取（连接时初始化）
   * 2. 从消息有效载荷 JSON 中提取 productKey 和 iotId/deviceId
   * 3. 从消息头中提取（如果支持）
   *
   * @param request WebSocket上行请求
   * @return 是否提取成功
   */
  protected boolean extractAndValidateDeviceInfo(WebSocketUPRequest request) {
    try {
      log.debug("[{}] [提取设备标识] 开始从请求中提取 iotId 和 productKey", getName());

      String iotId = request.getIotId();
      String productKey = request.getProductKey();
      String deviceId = request.getDeviceId();

      // 策略 1: 尝试从消息有效载荷 JSON 中提取（最优先，因为 MQTT Topic 中就包含这些信息）
      if (StrUtil.isBlank(productKey) || StrUtil.isBlank(iotId)) {
        String payload = request.getPayload();
        if (StrUtil.isNotBlank(payload)) {
          try {
            JSONObject jsonPayload = JSONUtil.parseObj(payload);

            // 提取 productKey
            if (StrUtil.isBlank(productKey)) {
              productKey = jsonPayload.getStr("productKey");
              if (StrUtil.isNotBlank(productKey)) {
                request.setProductKey(productKey);
                log.debug(
                    "[{}] [提取设备标识] 从 Payload 提取 productKey: {}",
                    getName(),
                    productKey);
              }
            }

            // 提取 iotId 或 deviceId
            if (StrUtil.isBlank(iotId)) {
              iotId = jsonPayload.getStr("iotId");
              if (StrUtil.isBlank(iotId)) {
                iotId = jsonPayload.getStr("deviceId");
              }
              if (StrUtil.isNotBlank(iotId)) {
                request.setIotId(iotId);
                request.setDeviceId(iotId);
                log.debug(
                    "[{}] [提取设备标识] 从 Payload 提取 iotId: {}",
                    getName(),
                    iotId);
              }
            }

            // 保存解析后的 JSON 供后续处理
            request.setContextValue("messageJson", jsonPayload);

          } catch (Exception e) {
            log.debug(
                "[{}] [提取设备标识] Payload 解析失败，继续尝试其他来源: {}",
                getName(),
                e.getMessage());
          }
        }
      }

      // 策略 2: 如果 iotId 仍为空，尝试从 deviceId 转换
      if (StrUtil.isBlank(iotId) && StrUtil.isNotBlank(deviceId)) {
        iotId = deviceId;
        request.setIotId(iotId);
        log.debug(
            "[{}] [提取设备标识] 从 deviceId 转换得到 iotId: {}",
            getName(),
            iotId);
      }

      // 最终验证：productKey 和 iotId 都必须存在
      if (StrUtil.isBlank(productKey)) {
        log.error(
            "[{}] [提取设备标识 FAILED] productKey 为空，检查过的位置："
                + "\n  - request.productKey: {}"
                + "\n  - payload.productKey: null"
                + "\n  - sessionId: {}",
            getName(),
            productKey,
            request.getSessionId());
        return false;
      }

      if (StrUtil.isBlank(iotId)) {
        log.error(
            "[{}] [提取设备标识 FAILED] iotId 为空，无法从以下位置获得："
                + "\n  - request.iotId: null"
                + "\n  - request.deviceId: {}"
                + "\n  - payload.iotId: null"
                + "\n  - payload.deviceId: null"
                + "\n  - sessionId: {}",
            getName(),
            deviceId,
            request.getSessionId());
        return false;
      }

      // 成功提取，设置唯一标识
      String deviceUniqueId = productKey + ":" + iotId;
      request.setContextValue("deviceUniqueId", deviceUniqueId);

      log.info(
          "[{}] [提取设备标识 SUCCESS] productKey: {}, iotId: {}, uniqueId: {}",
          getName(),
          productKey,
          iotId,
          deviceUniqueId);
      return true;

    } catch (Exception e) {
      log.error("[{}] [提取设备标识 EXCEPTION] 异常信息: ", getName(), e);
      return false;
    }
  }

  /** 回填产品信息 */
  protected boolean fillProductInfo(WebSocketUPRequest request) {
    try {
      String productKey = request.getProductKey();
      IoTProduct ioTProduct = getProduct(productKey);

      if (ioTProduct == null) {
        log.warn("[{}] 产品不存在: {}", getName(), productKey);
        return false;
      }

      request.setIoTProduct(ioTProduct);
      request.setContextValue("productInfo", ioTProduct);

      log.debug("[{}] 产品信息回填成功: {}", getName(), productKey);
      return true;

    } catch (Exception e) {
      log.error("[{}] 产品信息回填异常: ", getName(), e);
      return false;
    }
  }

  /** 回填设备信息 */
  protected boolean fillDeviceInfo(WebSocketUPRequest request) {
    try {
      IoTDeviceDTO ioTDeviceDTO =
          lifeCycleDevInstance(
              IoTDeviceQuery.builder()
                  .deviceId(request.getDeviceId())
                  .productKey(request.getProductKey())
                  .build());

      request.setIoTDeviceDTO(ioTDeviceDTO);
      request.setContextValue("deviceInfo", ioTDeviceDTO);

      if (ioTDeviceDTO != null) {
        log.debug("[{}] 设备信息回填成功: productKey={}, deviceId={}", 
                getName(), request.getProductKey(), request.getDeviceId());
      } else {
        log.warn("[{}] 设备不存在，可能需要自动注册: productKey={}, deviceId={}", 
                getName(), request.getProductKey(), request.getDeviceId());
      }

      // 对标MQTT：即使设备不存在也返回true，让后续的自动注册处理器有机会执行
      return true;

    } catch (Exception e) {
      log.error("[{}] 设备信息回填异常: ", getName(), e);
      return false;
    }
  }

  /**
   * 解析消息内容 - 从有效载荷中提取和验证消息体
   *
   * <p>对标 MQTT 的消息解析流程，提取 JSON 格式的消息内容
   *
   * @param request WebSocket上行请求
   * @return 是否解析成功
   */
  protected boolean parseMessageContent(WebSocketUPRequest request) {
    try {
      String payload = request.getPayload();

      if (StrUtil.isBlank(payload)) {
        log.warn("[{}] [消息解析] 消息体为空，无法进一步处理", getName());
        return false;
      }

      // 尝试解析为 JSON（部分消息为JSON格式）
      try {
        Object messageJson = JSONUtil.parseObj(payload);
        request.setContextValue("messageJson", messageJson);
        log.debug(
            "[{}] [消息解析 SUCCESS] 消息体解析成功，长度: {}, 内容预览: {}",
            getName(),
            payload.length(),
            payload.length() > 100 ? payload.substring(0, 100) + "..." : payload);
        return true;
      } catch (Exception e) {
        // 非 JSON 格式，可能是其他格式（透传数据等）
        log.debug("[{}] [消息解析] 消息体非 JSON 格式，将作为文本处理，长度: {}", getName(), payload.length());
        request.setContextValue("messageJson", null);
        return true; // 返回 true，因为这对透传消息是正常的
      }

    } catch (Exception e) {
      log.error("[{}] [消息解析 EXCEPTION] 异常信息: ", getName(), e);
      return false;
    }
  }

  /**
   * 提取基础信息 - 为日志和跟踪设置上下文
   *
   * <p>设置处理链中所需的基础信息字段
   *
   * @param request WebSocket上行请求
   */
  protected void extractBasicInfo(WebSocketUPRequest request) {
    try {
      // 设置协议类型
      request.setContextValue("protocol", "WebSocket");

      // 设置版本信息
      request.setContextValue("protocolVersion", "2.0");

      // 设置消息类型
      request.setContextValue("messageType", getMessageType());

      // 设置消息 ID 用于链路追踪
      request.setContextValue("messageId", request.getMessageId());

      // 设置时间戳
      request.setContextValue("processTime", System.currentTimeMillis());

      // 设置唯一标识（deviceUniqueId）
      String deviceUniqueId = (String) request.getContextValue("deviceUniqueId");
      log.debug(
          "[{}] [提取基础信息] 完成设置上下文 | protocol: WebSocket | messageType: {} | messageId: {} | deviceUniqueId: {}",
          getName(),
          getMessageType(),
          request.getMessageId(),
          deviceUniqueId);

    } catch (Exception e) {
      log.warn("[{}] [提取基础信息] 设置上下文异常，但继续处理: {}", getName(), e.getMessage());
    }
  }

  /**
   * 获取消息类型（由子类实现）
   *
   * @return 消息类型描述（如"物模型"、"透传"等）
   */
  protected abstract String getMessageType();

  @Override
  public boolean isEnabled() {
    return true;
  }
}
