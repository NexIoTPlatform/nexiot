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

package cn.universal.mqtt.protocol.processor.up;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.dm.device.service.action.IoTDeviceActionAfterService;
import cn.universal.dm.device.service.log.IIoTDeviceDataService;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.processor.MqttMessageProcessor;
import cn.universal.persistence.base.BaseUPRequest;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * MQTT异步指令响应关联处理器
 *
 * <p>职责：
 *
 * <ul>
 *   <li>关联设备上报数据与下发指令的commandId
 *   <li>更新指令执行状态(commandStatus)
 *   <li>删除Redis缓存，避免重复匹配
 *   <li>记录REPLY类型日志
 * </ul>
 *
 * <p>处理逻辑：
 *
 * <ol>
 *   <li>检查解析结果中是否包含 data.functionId (由decode脚本设置)
 *   <li>查询Redis: iot:cmd:{productKey}:{deviceId}:{functionId}
 *   <li>如果找到commandId，关联到上报消息
 *   <li>更新指令状态为成功(commandStatus=1)
 *   <li>删除Redis缓存
 *   <li>记录MessageType=REPLY的日志
 * </ol>
 *
 * <p>注意：
 *
 * <ul>
 *   <li>Order=2500，在MqttUPBatchReplyUPProcessor(2000)之后执行
 *   <li>不处理replyPayload(即时回复)，仅处理异步响应
 *   <li>与BaseLogShadowProcessorUP职责分离，专注指令追踪
 * </ul>
 *
 * @version 1.0
 * @since 2025/12/13
 */
@Slf4j(topic = "mqtt")
@Component
public class MqttUPCommandResponseProcessor implements MqttMessageProcessor {
  // 标识回复类型
  private String REPLY_FUNCTION = "replyFunction";
  @Resource private StringRedisTemplate stringRedisTemplate;

  @Resource private IoTDeviceActionAfterService ioTDeviceActionAfterService;

  @Resource private IIoTDeviceDataService iIoTDeviceDataService;

  /** Redis Key前缀 */
  private static final String REDIS_KEY_PREFIX = "iot:cmd:";

  @Override
  public String getName() {
    return "MQTT异步指令响应关联处理器";
  }

  @Override
  public int getOrder() {
    return 2500;
  }

  @Override
  public boolean supports(MQTTUPRequest request) {
    // 仅处理包含upRequestList的情况
    if (request.getUpRequestList() == null || request.getUpRequestList().isEmpty()) {
      return false;
    }

    // 至少有一个upRequest包含functionId(指令响应标识)
    return request.getUpRequestList().stream()
        .anyMatch(
            upRequest ->
                upRequest.getData() != null
                    && upRequest.getData().containsKey(REPLY_FUNCTION)
                    && ObjectUtil.isNotNull(upRequest.getData().get(REPLY_FUNCTION)));
  }

  @Override
  public ProcessorResult process(MQTTUPRequest request) {
    try {
      log.debug(
          "[{}] 开始处理异步指令响应: productKey={}, deviceId={}",
          getName(),
          request.getProductKey(),
          request.getDeviceId());

      int matchedCount = 0;

      // 遍历所有解析结果
      for (BaseUPRequest upRequest : request.getUpRequestList()) {
        // 检查是否包含functionId
        if (upRequest.getData() == null || !upRequest.getData().containsKey(REPLY_FUNCTION)) {
          continue;
        }

        String functionId = (String) upRequest.getData().get(REPLY_FUNCTION);
        if (StrUtil.isBlank(functionId)) {
          continue;
        }

        // 尝试关联commandId
        String commandId =
            matchCommandId(request.getProductKey(), request.getDeviceId(), functionId);

        if (StrUtil.isNotBlank(commandId)) {
          // 关联成功
          // 注意: 不修改 upRequest 的 messageType,保持原始语义(PROPERTIES/EVENT)
          // 仅设置 commandId 用于关联,让后续处理知道这是指令响应
          upRequest.setCommandId(commandId);
          upRequest.setCommandStatus(1); // 标记为成功

          matchedCount++;

          log.info(
              "[{}] 成功关联异步指令响应: productKey={}, deviceId={}, functionId={}, commandId={}",
              getName(),
              request.getProductKey(),
              request.getDeviceId(),
              functionId,
              commandId);

          // 更新指令状态
          updateCommandStatus(commandId, functionId, upRequest);

          // 删除Redis缓存(避免重复匹配)
          deleteRedisCache(request.getProductKey(), request.getDeviceId(), functionId);

        } else {
          // 未找到匹配的指令，标记为设备主动上报
          log.debug(
              "[{}] 未找到匹配的指令，设备主动上报: productKey={}, deviceId={}, functionId={}",
              getName(),
              request.getProductKey(),
              request.getDeviceId(),
              functionId);

          // 保持原有的MessageType(通常是PROPERTIES)
          if (upRequest.getData() != null) {
            upRequest.getData().put("replyType", "periodic_report"); // 标记为主动上报
          }
        }
      }

      if (matchedCount > 0) {
        log.info(
            "[{}] 异步指令响应关联完成: productKey={}, deviceId={}, 匹配数量={}",
            getName(),
            request.getProductKey(),
            request.getDeviceId(),
            matchedCount);
      }

    } catch (Exception e) {
      log.error(
          "[{}] 处理异步指令响应异常: productKey={}, deviceId={}, error={}",
          getName(),
          request.getProductKey(),
          request.getDeviceId(),
          e.getMessage(),
          e);
      // 不中断处理链，继续后续处理
    }
    return ProcessorResult.CONTINUE;
  }

  /**
   * 从Redis匹配commandId
   *
   * @param productKey 产品标识
   * @param deviceId 设备ID
   * @param functionId 功能ID
   * @return commandId，未找到返回null
   */
  private String matchCommandId(String productKey, String deviceId, String functionId) {
    try {
      String redisKey = buildRedisKey(productKey, deviceId, functionId);
      String commandId = stringRedisTemplate.opsForValue().get(redisKey);

      if (StrUtil.isNotBlank(commandId)) {
        log.debug("[{}] Redis匹配成功: key={}, commandId={}", getName(), redisKey, commandId);
        return commandId;
      } else {
        log.debug("[{}] Redis未找到匹配: key={}", getName(), redisKey);
        return null;
      }

    } catch (Exception e) {
      log.error(
          "[{}] Redis查询异常: productKey={}, deviceId={}, functionId={}, error={}",
          getName(),
          productKey,
          deviceId,
          functionId,
          e.getMessage());
      return null;
    }
  }

  /**
   * 更新指令状态
   *
   * @param commandId 指令ID
   * @param upRequest 上报请求
   */
  private void updateCommandStatus(String commandId, String function, BaseUPRequest upRequest) {
    try {
      Map<String, Object> map = new HashMap<>();
      map.put("commandId", commandId);
      map.put("function", function);
      // 调用现有的指令响应处理逻辑
      ioTDeviceActionAfterService.commandResp(upRequest.getIoTDeviceDTO(), commandId, map);

      log.debug("[{}] 指令状态更新成功: commandId={}", getName(), commandId);

    } catch (Exception e) {
      log.error("[{}] 更新指令状态异常: commandId={}, error={}", getName(), commandId, e.getMessage(), e);
    }
  }

  /**
   * 删除Redis缓存
   *
   * @param productKey 产品标识
   * @param deviceId 设备ID
   * @param functionId 功能ID
   */
  private void deleteRedisCache(String productKey, String deviceId, String functionId) {
    try {
      String redisKey = buildRedisKey(productKey, deviceId, functionId);
      Boolean deleted = stringRedisTemplate.delete(redisKey);

      if (Boolean.TRUE.equals(deleted)) {
        log.debug("[{}] Redis缓存删除成功: key={}", getName(), redisKey);
      } else {
        log.debug("[{}] Redis缓存删除失败或不存在: key={}", getName(), redisKey);
      }

    } catch (Exception e) {
      log.error(
          "[{}] 删除Redis缓存异常: productKey={}, deviceId={}, functionId={}, error={}",
          getName(),
          productKey,
          deviceId,
          functionId,
          e.getMessage());
    }
  }

  /**
   * 构建Redis Key
   *
   * @param productKey 产品标识
   * @param deviceId 设备ID
   * @param functionId 功能ID
   * @return Redis Key
   */
  private String buildRedisKey(String productKey, String deviceId, String functionId) {
    return REDIS_KEY_PREFIX + productKey + ":" + deviceId + ":" + functionId;
  }
}
