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

import cn.hutool.core.collection.CollUtil;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.dm.device.service.sub.context.SubDeviceRequest;
import cn.universal.persistence.base.BaseUPRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 子设备日志影子处理器
 *
 * <p>子设备日志和入库
 *
 * <p>保存子设备日志数据 - 更新子设备影子状态 - 处理子设备数据持久化
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/10/20
 */
@Slf4j
@Component
public class SubDeviceLogShadowProcessor extends AbstratIoTService
    implements SubDeviceMessageProcessor {

  @Override
  public String getName() {
    return "子设备日志影子处理器";
  }

  @Override
  public String getDescription() {
    return "子设备日志影子处理器 - 处理子设备日志记录和影子更新";
  }

  @Override
  public int getOrder() {
    return 888; // 日志影子处理是第六步
  }

  @Override
  public ProcessorResult process(SubDeviceRequest request) {
    try {
      log.debug(
          "[{}] 开始处理子设备日志和影子，网关: {}, 子设备: {}",
          getName(),
          request.getGwDeviceId(),
          request.getDeviceId());

      // 1. 获取处理后的请求列表
      List<BaseUPRequest> requestList = request.getUpRequestList();
      if (CollUtil.isEmpty(requestList)) {
        log.debug("[{}] 子设备请求列表为空，跳过日志影子处理", getName());
        return ProcessorResult.CONTINUE;
      }

      // 2. 过滤非调试消息进行处理
      int processedCount = 0;
      int shadowUpdatedCount = 0;

      for (BaseUPRequest upRequest : requestList) {
        if (upRequest != null && !upRequest.isDebug()) {
          // 保存子设备日志
          if (saveSubDeviceLog(upRequest, request)) {
            processedCount++;
          }

          // 更新子设备影子
          if (updateSubDeviceShadow(upRequest, request)) {
            shadowUpdatedCount++;
          }

          // 子设备事件
          if (updateSubDeviceEventName(upRequest)) {}
        }
      }

      // 3. 子设备特定的日志影子处理
      if (!processSubDeviceSpecificLogShadow(request)) {
        log.error("[{}] 子设备特定日志影子处理失败", getName());
        return ProcessorResult.ERROR;
      }

      // 4. 更新处理统计
      request.setContextValue("logProcessedCount", processedCount);
      request.setContextValue("shadowUpdatedCount", shadowUpdatedCount);
      request.setContextValue("logShadowProcessed", true);
      request.setStage(SubDeviceRequest.ProcessingStage.LOGGED);

      log.debug("[{}] 子设备日志影子处理完成，日志: {}, 影子: {}", getName(), processedCount, shadowUpdatedCount);
      return ProcessorResult.CONTINUE;

    } catch (Exception e) {
      log.error(
          "[{}] 子设备日志影子处理异常，网关: {}, 子设备: {}, 异常: ",
          getName(),
          request.getGwDeviceId(),
          request.getDeviceId(),
          e);
      return ProcessorResult.ERROR;
    }
  }

  @Override
  public boolean supports(SubDeviceRequest request) {
    // 支持有设备信息和请求列表的消息
    return request.getIoTDeviceDTO() != null
        && request.getIoTProduct() != null
        && CollUtil.isNotEmpty(request.getUpRequestList())
        && request.getSubDevice() != null;
  }

  protected boolean updateSubDeviceEventName(BaseUPRequest upRequest) {
    // 如果是事件，则完善事件名称
    if (cn.universal.common.constant.IoTConstant.MessageType.EVENT.equals(
        upRequest.getMessageType())) {
      cn.universal.core.metadata.DeviceMetadata deviceMetadata =
          iotProductDeviceService.getDeviceMetadata(upRequest.getProductKey());
      cn.universal.core.metadata.AbstractEventMetadata metadata =
          deviceMetadata.getEventOrNull(upRequest.getEvent());
      if (metadata != null) {
        upRequest.setEventName(metadata.getName());
      }
    }
    return true;
  }

  /** 保存子设备日志 */
  protected boolean saveSubDeviceLog(BaseUPRequest upRequest, SubDeviceRequest subDeviceRequest) {
    try {
      if (upRequest == null) {
        return false;
      }

      // 调用IoT服务保存设备日志
      iIoTDeviceDataService.saveDeviceLog(
          upRequest, subDeviceRequest.getIoTDeviceDTO(), subDeviceRequest.getIoTProduct());

      log.debug(
          "[{}] 子设备日志保存成功 - 产品: {}, 设备: {}, 消息类型: {}",
          getName(),
          upRequest.getProductKey(),
          upRequest.getIotId(),
          upRequest.getMessageType());
      return true;

    } catch (Exception e) {
      log.error(
          "[{}] 子设备日志保存异常 - 产品: {}, 设备: {}, 异常: ",
          getName(),
          upRequest.getProductKey(),
          upRequest.getIotId(),
          e);
      return false;
    }
  }

  /** 更新子设备影子 */
  protected boolean updateSubDeviceShadow(
      BaseUPRequest upRequest, SubDeviceRequest subDeviceRequest) {
    try {
      if (upRequest == null) {
        return false;
      }

      // 调用影子服务更新设备影子
      iotDeviceShadowService.doShadow(upRequest, subDeviceRequest.getIoTDeviceDTO());

      log.debug(
          "[{}] 子设备影子更新成功 - 产品: {}, 设备: {}",
          getName(),
          upRequest.getProductKey(),
          upRequest.getIotId());
      return true;

    } catch (Exception e) {
      log.error(
          "[{}] 子设备影子更新异常 - 产品: {}, 设备: {}, 异常: ",
          getName(),
          upRequest.getProductKey(),
          upRequest.getIotId(),
          e);
      return false;
    }
  }

  /** 验证子设备日志数据 */
  protected boolean validateSubDeviceLogData(BaseUPRequest upRequest) {
    if (upRequest == null) {
      return false;
    }

    // 检查必要字段
    if (upRequest.getProductKey() == null || upRequest.getIotId() == null) {
      log.warn(
          "[{}] 子设备日志数据缺少必要字段 - 产品: {}, 设备: {}",
          getName(),
          upRequest.getProductKey(),
          upRequest.getIotId());
      return false;
    }

    return true;
  }

  /** 子设备特定的日志影子处理 */
  protected boolean processSubDeviceSpecificLogShadow(SubDeviceRequest request) {
    try {
      // 子设备特定的处理逻辑
      log.debug("[{}] 执行子设备特定的日志影子处理", getName());

      // 可以在这里添加子设备特有的处理逻辑
      // 例如：子设备状态同步、网关关系更新等

      return true;
    } catch (Exception e) {
      log.error("[{}] 子设备特定日志影子处理异常: ", getName(), e);
      return false;
    }
  }

  @Override
  public boolean preCheck(SubDeviceRequest request) {
    // 检查必要的数据
    return true;
  }

  @Override
  public void postProcess(SubDeviceRequest request, ProcessorResult result) {
    if (result == ProcessorResult.CONTINUE) {
      Integer processedCount = (Integer) request.getContextValue("logProcessedCount");
      Integer shadowUpdatedCount = (Integer) request.getContextValue("shadowUpdatedCount");

      log.debug(
          "[{}] 子设备日志影子处理成功 - 网关: {}, 子设备: {}, 日志: {}, 影子: {}",
          getName(),
          request.getGwDeviceId(),
          request.getDeviceId(),
          processedCount != null ? processedCount : 0,
          shadowUpdatedCount != null ? shadowUpdatedCount : 0);
    } else {
      log.warn(
          "[{}] 子设备日志影子处理失败 - 网关: {}, 子设备: {}, 结果: {}",
          getName(),
          request.getGwDeviceId(),
          request.getDeviceId(),
          result);
    }
  }

  @Override
  public void onError(SubDeviceRequest request, Exception e) {
    log.error(
        "[{}] 子设备日志影子处理异常，网关: {}, 子设备: {}, 异常: ",
        getName(),
        request.getGwDeviceId(),
        request.getDeviceId(),
        e);
    request.setErrorMessage("子设备日志影子处理失败: " + e.getMessage());
  }

  @Override
  public int getPriority() {
    return 10; // 日志影子处理优先级较低
  }

  @Override
  public boolean isRequired() {
    return false; // 日志影子处理不是必需的，失败不影响主流程
  }
}
