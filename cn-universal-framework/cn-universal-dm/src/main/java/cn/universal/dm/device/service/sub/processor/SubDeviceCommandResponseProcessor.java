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

package cn.universal.dm.device.service.sub.processor;

import cn.hutool.core.util.StrUtil;
import cn.universal.dm.device.service.action.IoTDeviceActionAfterService;
import cn.universal.dm.device.service.sub.context.SubDeviceRequest;
import cn.universal.persistence.base.BaseUPRequest;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 子设备异步指令响应关联处理器
 *
 * <p>职责：
 *
 * <ul>
 *   <li>关联网关子设备上报数据与下发指令的commandId
 *   <li>更新指令执行状态(commandStatus)
 *   <li>删除Redis缓存，避免重复匹配
 *   <li>标记指令响应或主动上报
 * </ul>
 *
 * <p>处理逻辑：
 *
 * <ol>
 *   <li>检查解析结果中是否包含 data.replyFunction (由decode脚本设置)
 *   <li>查询Redis: iot:cmd:{子设备ProductKey}:{子设备DeviceId}:{functionId}
 *   <li>如果找到commandId，关联到上报消息
 *   <li>更新指令状态为成功(commandStatus=1)
 *   <li>删除Redis缓存
 * </ol>
 *
 * <p>关键设计：
 *
 * <ul>
 *   <li>Order=500，在SubDeviceCodecProcessor(300)之后，SubDeviceLogShadowProcessor(888)之前
 *   <li>不修改原始messageType，保持数据语义完整性
 *   <li>与直连设备处理器(TcpUPCommandResponseProcessor)隔离，职责清晰
 * </ul>
 *
 * <p>网关子设备场景：
 *
 * <ul>
 *   <li>下发：使用子设备productKey+deviceId写入Redis
 *   <li>上报：网关收到子设备回复，解析为子设备数据
 *   <li>匹配：使用子设备productKey+deviceId查询Redis，完成关联
 * </ul>
 *
 * @version 1.0
 * @since 2025/12/14
 */
@Slf4j
@Component
public class SubDeviceCommandResponseProcessor implements SubDeviceMessageProcessor {

  @Resource private StringRedisTemplate stringRedisTemplate;

  @Resource private IoTDeviceActionAfterService ioTDeviceActionAfterService;

  /** 标识回复的功能ID字段 */
  private static final String REPLY_FUNCTION = "replyFunction";

  /** Redis Key前缀 */
  private static final String REDIS_KEY_PREFIX = "iot:cmd:";

  @Override
  public String getName() {
    return "子设备异步指令响应关联处理器";
  }

  @Override
  public String getDescription() {
    return "子设备异步指令响应关联处理器 - 关联网关子设备的异步指令响应";
  }

  @Override
  public int getOrder() {
    return 500;
  }

  @Override
  public boolean supports(SubDeviceRequest request) {
    if (request.getUpRequestList() == null || request.getUpRequestList().isEmpty()) {
      return false;
    }

    if (request.getProductKey() == null
        || request.getDeviceId() == null
        || request.getIoTDeviceDTO() == null) {
      return false;
    }

    return request.getUpRequestList().stream()
        .anyMatch(
            upRequest ->
                upRequest.getData() != null
                    && upRequest.getData().containsKey(REPLY_FUNCTION)
                    && StrUtil.isNotBlank((String) upRequest.getData().get(REPLY_FUNCTION)));
  }

  @Override
  public ProcessorResult process(SubDeviceRequest request) {
    try {
      log.debug(
          "[{}] 开始处理子设备异步指令响应: gateway={}, subDevice={}",
          getName(),
          request.getGwDeviceId(),
          request.getDeviceId());

      int matchedCount = 0;

      for (BaseUPRequest upRequest : request.getUpRequestList()) {
        if (upRequest.getData() == null || !upRequest.getData().containsKey(REPLY_FUNCTION)) {
          continue;
        }

        String functionId = (String) upRequest.getData().get(REPLY_FUNCTION);
        if (StrUtil.isBlank(functionId)) {
          continue;
        }

        String commandId =
            matchCommandId(request.getProductKey(), request.getDeviceId(), functionId);

        if (StrUtil.isNotBlank(commandId)) {
          upRequest.setCommandId(commandId);
          upRequest.setCommandStatus(1);

          matchedCount++;

          log.info(
              "[{}] 成功关联子设备异步指令响应: gateway={}, subDevice={}, functionId={}, commandId={}",
              getName(),
              request.getGwDeviceId(),
              request.getDeviceId(),
              functionId,
              commandId);

          updateCommandStatus(commandId, functionId, upRequest, request);
          deleteRedisCache(request.getProductKey(), request.getDeviceId(), functionId);

        } else {
          log.debug(
              "[{}] 未找到匹配的指令，子设备主动上报: gateway={}, subDevice={}, functionId={}",
              getName(),
              request.getGwDeviceId(),
              request.getDeviceId(),
              functionId);

          if (upRequest.getData() != null) {
            upRequest.getData().put("replyType", "periodic_report");
          }
        }
      }

      if (matchedCount > 0) {
        log.info(
            "[{}] 子设备异步指令响应关联完成: gateway={}, subDevice={}, 匹配数量={}",
            getName(),
            request.getGwDeviceId(),
            request.getDeviceId(),
            matchedCount);
      }

      return ProcessorResult.CONTINUE;

    } catch (Exception e) {
      log.error(
          "[{}] 处理子设备异步指令响应异常: gateway={}, subDevice={}, error={}",
          getName(),
          request.getGwDeviceId(),
          request.getDeviceId(),
          e.getMessage(),
          e);
      return ProcessorResult.CONTINUE;
    }
  }

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

  private void updateCommandStatus(
      String commandId, String functionId, BaseUPRequest upRequest, SubDeviceRequest request) {
    try {
      Map<String, Object> map = new HashMap<>();
      map.put("commandId", commandId);
      map.put("function", functionId);

      ioTDeviceActionAfterService.commandResp(request.getIoTDeviceDTO(), commandId, map);

      log.debug(
          "[{}] 子设备指令状态更新成功: commandId={}, subDevice={}",
          getName(),
          commandId,
          request.getDeviceId());

    } catch (Exception e) {
      log.error(
          "[{}] 更新子设备指令状态异常: commandId={}, subDevice={}, error={}",
          getName(),
          commandId,
          request.getDeviceId(),
          e.getMessage(),
          e);
    }
  }

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

  private String buildRedisKey(String productKey, String deviceId, String functionId) {
    return REDIS_KEY_PREFIX + productKey + ":" + deviceId + ":" + functionId;
  }

  @Override
  public boolean preCheck(SubDeviceRequest request) {
    return true;
  }

  @Override
  public void postProcess(SubDeviceRequest request, ProcessorResult result) {
    if (result == ProcessorResult.CONTINUE) {
      log.debug("[{}] 子设备指令响应关联处理完成: subDevice={}", getName(), request.getDeviceId());
    }
  }

  @Override
  public void onError(SubDeviceRequest request, Exception e) {
    log.error(
        "[{}] 子设备指令响应关联处理异常: gateway={}, subDevice={}, error={}",
        getName(),
        request.getGwDeviceId(),
        request.getDeviceId(),
        e.getMessage(),
        e);
    request.setErrorMessage("子设备指令响应关联失败: " + e.getMessage());
  }

  @Override
  public int getPriority() {
    return 5;
  }

  @Override
  public boolean isRequired() {
    return false;
  }
}
