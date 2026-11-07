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
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.dm.device.service.sub.context.SubDeviceRequest;
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
 * 子设备编解码处理器
 *
 * <p>对应步骤THREE：子设备数据编解码，确保消息能被物模型正确识别
 *
 * <p>子设备编解码特点： - 调用子设备产品配置的编解码器 - 将原始数据转换为标准的物模型格式 - 兼容报文按照本平台格式的处理 - 支持多种数据格式的解码
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Slf4j
@Component
public class SubDeviceCodecProcessor extends AbstratIoTService
    implements SubDeviceMessageProcessor {

  @Autowired private IoTDeviceShadowService ioTDeviceShadowService;

  @Override
  public String getName() {
    return "子设备编解码处理器";
  }

  @Override
  public String getDescription() {
    return "子设备编解码处理器 - 处理子设备数据的编解码";
  }

  @Override
  public int getOrder() {
    return 300; // 编解码处理是第三步
  }

  @Override
  public boolean supports(SubDeviceRequest request) {
    if (request.getIoTDeviceDTO() == null || request.getIoTProduct() == null) {
      return false;
    }
    return request.getGwDeviceId() != null
        && request.getGwProductKey() != null
        && request.getSubDevice() != null;
  }

  @Override
  public ProcessorResult process(SubDeviceRequest request) {
    try {
      log.debug(
          "[{}] 开始处理子设备编解码，网关: {}, 子设备: {}",
          getName(),
          request.getGwDeviceId(),
          request.getDeviceId());

      // 1. 尝试子设备产品编解码器解码
      List<BaseUPRequest> upRequestList = trySubDeviceProductCodec(request);

      // 2. 如果编解码器解码失败，尝试平台格式兼容
      if (CollUtil.isEmpty(upRequestList)) {
        upRequestList = tryPlatformFormat(request);
      }

      // 4. 设置解码结果
      request.setUpRequestList(upRequestList);

      request.setContextValue("codecProcessedCount", upRequestList.size());
      request.setContextValue("codecProcessed", true);
      request.setStage(SubDeviceRequest.ProcessingStage.DECODED);

      log.debug("[{}] 子设备编解码处理器完成，生成请求数量: {}", getName(), upRequestList.size());
      return ProcessorResult.CONTINUE;
    } catch (Exception e) {
      log.error(
          "[{}] 子设备编解码处理器异常，网关: {}, 子设备: {}, 异常: ",
          getName(),
          request.getGwDeviceId(),
          request.getDeviceId(),
          e);
      return ProcessorResult.ERROR;
    }
  }

  /** 尝试使用子设备产品编解码器解码 */
  private List<BaseUPRequest> trySubDeviceProductCodec(SubDeviceRequest request) {
    try {
      String subDeviceId = request.getDeviceId();
      String payload = request.getPayload();
      IoTProduct ioTProduct = request.getIoTProduct();

      if (StringUtils.isNotEmpty(ioTProduct.getConfiguration())) {
        JSONObject jsonObject = JSONUtil.parseObj(ioTProduct.getConfiguration());
        // 上行报文是否需要附加影子
        Boolean requireUpShadow = jsonObject.getBool("requireUpShadow", false);
        if (requireUpShadow) {
          JSONObject shadowObj =
              ioTDeviceShadowService.getDeviceShadowObj(request.getProductKey(), subDeviceId);
          if (ObjectUtil.isNotNull(shadowObj)) {
            request.setShadow(shadowObj);
            request.setCodecContextValue("shadow", shadowObj);
            request.setCodecContextValue("productConfig", jsonObject);
          }
        }
      }
      log.debug(
          "[{}] 尝试子设备产品编解码器解码 - 产品: {}, 子设备: {}", getName(), request.getProductKey(), subDeviceId);

      // 调用子设备产品编解码器
      List<SubDeviceRequest> decodedList =
          decode(
              request.getProductKey(), payload, request.getCodecContext(), SubDeviceRequest.class);

      if (CollUtil.isNotEmpty(decodedList)) {
        log.debug("[{}] 子设备产品编解码器解码成功，解码数量: {}", getName(), decodedList.size());
        List<BaseUPRequest> upRequestList = new ArrayList<>();
        for (SubDeviceRequest codecResult : decodedList) {
          BaseUPRequest upRequest = convertSubDeviceCodecResult(request, codecResult);
          if (upRequest != null) {
            upRequestList.add(upRequest);
          }
        }
        request.setContextValue("codecSuccess", true);
        request.setContextValue("codecType", "SUB_DEVICE_PRODUCT_CODEC");
        request.setStage(SubDeviceRequest.ProcessingStage.PROTOCOL_DECODED);
        return upRequestList;
      } else {
        log.debug("[{}] 子设备产品编解码器未返回解码结果", getName());
        request.setContextValue("codecSuccess", false);
        request.setStage(SubDeviceRequest.ProcessingStage.DECODED);
        JSONObject jsonObject = new JSONObject();
        if (JSONUtil.isTypeJSON(request.getPayload())) {
          jsonObject = JSONUtil.parseObj(request.getPayload());
        }
        return Stream.of(buildCodecNullBean(jsonObject, request)).collect(Collectors.toList());
      }

    } catch (Exception e) {
      log.warn("[{}] 子设备产品编解码器解码异常: ", getName(), e);
      request.setContextValue("codecSuccess", false);
      request.setContextValue("codecError", e.getMessage());
      return null;
    }
  }

  /** 尝试平台格式兼容 */
  private List<BaseUPRequest> tryPlatformFormat(SubDeviceRequest request) {
    try {
      String messageContent = request.getPayload();

      log.debug("[{}] 尝试子设备平台格式兼容解析", getName());

      // 解析payload
      JSONObject jsonObject = parseJsonPayload(messageContent);
      if (jsonObject == null || jsonObject.isEmpty()) {
        log.debug("[{}] 子设备平台格式解析失败", getName());
        return null;
      }

      // 创建BaseUPRequest
      BaseUPRequest upRequest = buildPlatformFormatRequest(request, jsonObject);
      if (upRequest == null) {
        return null;
      }

      List<BaseUPRequest> upRequestList = new ArrayList<>();
      upRequestList.add(upRequest);

      log.debug("[{}] 子设备平台格式兼容解析成功", getName());
      request.setContextValue("codecSuccess", true);
      request.setContextValue("codecType", "SUB_DEVICE_PLATFORM_FORMAT");
      return upRequestList;

    } catch (Exception e) {
      log.warn("[{}] 子设备平台格式兼容解析异常: ", getName(), e);
      return null;
    }
  }

  /** 转换子设备编解码器结果 */
  private BaseUPRequest convertSubDeviceCodecResult(
      SubDeviceRequest request, SubDeviceRequest codecResult) {
    try {
      if (codecResult == null) {
        return null;
      }
      IoTDeviceDTO deviceDTO = request.getIoTDeviceDTO();
      BaseUPRequest.BaseUPRequestBuilder<?, ?> builder = getBaseUPRequest(deviceDTO);

      // 构建编解码结果
      JSONObject messageJson = parseJsonPayload(request.getPayload());
      buildCodecNotNullBean(messageJson, deviceDTO, codecResult, builder);

      BaseUPRequest upRequest = builder.build();

      log.debug("[{}] 子设备编解码器结果转换成功", getName());
      return upRequest;
    } catch (Exception e) {
      log.error("[{}] 子设备编解码器结果转换异常: ", getName(), e);
      return null;
    }
  }

  /** 构建子设备平台格式请求 */
  private BaseUPRequest buildPlatformFormatRequest(
      SubDeviceRequest request, JSONObject jsonObject) {
    try {
      BaseUPRequest upRequest = buildCodecNullBean(jsonObject, request);

      log.debug("[{}] 子设备平台格式请求构建成功", getName());
      return upRequest;

    } catch (Exception e) {
      log.error("[{}] 子设备平台格式请求构建异常: ", getName(), e);
      return null;
    }
  }

  /** 统计子设备编解码结果 */
  private void collectSubDeviceCodecStatistics(SubDeviceRequest request) {
    try {
      Boolean codecSuccess = (Boolean) request.getContextValue("codecSuccess");
      String codecType = (String) request.getContextValue("codecType");
      Integer processedCount = (Integer) request.getContextValue("codecProcessedCount");

      request.setContextValue(
          "codecStatistics",
          "成功: " + codecSuccess + ", 类型: " + codecType + ", 数量: " + processedCount);

      log.debug(
          "[{}] 子设备编解码统计 - 成功: {}, 类型: {}, 数量: {}",
          getName(),
          codecSuccess,
          codecType,
          processedCount != null ? processedCount : 0);

    } catch (Exception e) {
      log.warn("[{}] 子设备编解码统计异常: ", getName(), e);
    }
  }

  @Override
  public boolean preCheck(SubDeviceRequest request) {
    return true;
  }

  @Override
  public void postProcess(SubDeviceRequest request, ProcessorResult result) {
    if (result == ProcessorResult.CONTINUE) {
      // 收集编解码统计信息
      collectSubDeviceCodecStatistics(request);

      Integer processedCount = (Integer) request.getContextValue("codecProcessedCount");
      Boolean codecSuccess = (Boolean) request.getContextValue("codecSuccess");

      log.debug(
          "[{}] 子设备编解码处理成功 - 网关: {}, 子设备: {}, 成功: {}, 生成请求: {}",
          getName(),
          request.getGwDeviceId(),
          request.getDeviceId(),
          codecSuccess,
          processedCount != null ? processedCount : 0);
    } else {
      log.warn(
          "[{}] 子设备编解码处理失败 - 网关: {}, 子设备: {}, 结果: {}",
          getName(),
          request.getGwDeviceId(),
          request.getDeviceId(),
          result);
    }
  }

  @Override
  public void onError(SubDeviceRequest request, Exception e) {
    log.error(
        "[{}] 子设备编解码处理异常，网关: {}, 子设备: {}, 异常: ",
        getName(),
        request.getGwDeviceId(),
        request.getDeviceId(),
        e);
    request.setErrorMessage("子设备编解码处理失败: " + e.getMessage());
  }

  @Override
  public int getPriority() {
    return 5; // 子设备编解码处理优先级中等
  }

  @Override
  public boolean isRequired() {
    return true; // 编解码处理是必需的
  }
}
