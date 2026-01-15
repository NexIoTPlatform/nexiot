/*
 *
 * Copyright (c) 2026, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.websocket.protocol.processor.up;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.domain.R;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.query.IoTDeviceQuery;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket设备自动注册处理器
 *
 * <p>对标MQTT的BaseAutoRegisterProcessor，负责处理WebSocket设备的自动注册逻辑
 * 当检测到新设备时，根据产品配置决定是否自动完成设备注册
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/15
 */
@Slf4j(topic = "websocket")
@Component
public class WebSocketAutoRegisterProcessor extends AbstratIoTService
    implements WebSocketUPProcessor {

  @Value("${iot.register.auto.unionId:defaultUnionId}")
  private String unionId;

  @Value("${iot.register.auto.latitude:0.0}")
  private String latitude;

  @Value("${iot.register.auto.longitude:0.0}")
  private String longitude;

  @Override
  public String getName() {
    return "WebSocket设备自动注册处理器";
  }

  @Override
  public String getDescription() {
    return "处理WebSocket消息的设备自动注册";
  }

  @Override
  public int getOrder() {
    return 200; // 在设备信息处理器(100)之后，编解码处理器(300)之前执行
  }

  @Override
  public int getPriority() {
    return 10;
  }

  @Override
  public ProcessorResult process(WebSocketUPRequest request) {
    try {
      log.debug(
          "[{}] 开始处理自动注册，设备: {}, 产品: {}, SessionID: {}",
          getName(),
          request.getDeviceId(),
          request.getProductKey(),
          request.getSessionId());

      // 1. 检查设备是否存在
      if (request.getIoTDeviceDTO() != null) {
        log.debug("[{}] 设备已存在，跳过自动注册: {}", getName(), request.getDeviceId());
        return ProcessorResult.CONTINUE;
      }

      // 2. 检查产品配置是否支持自动注册
      if (!isAutoRegisterEnabled(request)) {
        log.warn("[{}] 产品未开启自动注册功能，停止处理: productKey={}, deviceId={}",
                getName(), request.getProductKey(), request.getDeviceId());
        return ProcessorResult.STOP;
      }

      // 3. 执行自动注册
      if (!performAutoRegister(request)) {
        log.error("[{}] 自动注册失败: productKey={}, deviceId={}",
                getName(), request.getProductKey(), request.getDeviceId());
        return ProcessorResult.ERROR;
      }

      // 4. 重新查询和回填设备信息
      if (!refillDeviceInfo(request)) {
        log.error("[{}] 设备信息重新回填失败: productKey={}, deviceId={}",
                getName(), request.getProductKey(), request.getDeviceId());
        return ProcessorResult.ERROR;
      }

      log.info("[{}] 设备自动注册成功: productKey={}, deviceId={}",
              getName(), request.getProductKey(), request.getDeviceId());
      return ProcessorResult.CONTINUE;

    } catch (Exception e) {
      log.error("[{}] 自动注册处理异常，设备: {}, 异常: ",
              getName(), request.getDeviceId(), e);
      return ProcessorResult.ERROR;
    }
  }

  @Override
  public boolean supports(WebSocketUPRequest request) {
    // 支持设备不存在且产品配置了自动注册的情况
    return request != null
        && request.getIoTDeviceDTO() == null
        && request.getIoTProduct() != null
        && isAutoRegisterEnabled(request);
  }

  @Override
  public boolean preCheck(WebSocketUPRequest request) {
    // 检查必要字段
    return request.getProductKey() != null
        && request.getDeviceId() != null
        && request.getIoTProduct() != null;
  }

  @Override
  public void postProcess(WebSocketUPRequest request, ProcessorResult result) {
    if (result == ProcessorResult.CONTINUE) {
      log.debug("[{}] 自动注册处理完成", getName());
    }
  }

  @Override
  public void onError(WebSocketUPRequest request, Exception e) {
    log.error("[{}] 自动注册处理错误，SessionID: {}, 异常: {}",
            getName(), request.getSessionId(), e.getMessage());
  }

  /** 检查是否启用自动注册 */
  protected boolean isAutoRegisterEnabled(WebSocketUPRequest request) {
    try {
      IoTProduct ioTProduct = request.getIoTProduct();
      if (ioTProduct == null) {
        log.warn("[{}] 产品信息为空，无法检查自动注册配置", getName());
        return false;
      }

      String configuration = ioTProduct.getConfiguration();
      if (StrUtil.isBlank(configuration)) {
        log.debug("[{}] 产品配置为空，默认不启用自动注册", getName());
        return false;
      }

      JSONObject config = JSONUtil.parseObj(configuration);
      boolean allowInsert = config.getBool(IoTConstant.ALLOW_INSERT, false);

      log.debug("[{}] 产品自动注册配置: allowInsert={}", getName(), allowInsert);
      return allowInsert;

    } catch (Exception e) {
      log.error("[{}] 检查自动注册配置异常: ", getName(), e);
      return false;
    }
  }

  /** 执行自动注册 */
  protected boolean performAutoRegister(WebSocketUPRequest request) {
    try {
      String deviceId = request.getDeviceId();
      IoTProduct ioTProduct = request.getIoTProduct();

      log.info("[{}] 开始自动注册设备: deviceId={}, productKey={}",
              getName(), deviceId, ioTProduct.getProductKey());

      // 构建注册请求
      JSONObject downRequest = buildAutoRegisterRequest(request, deviceId, ioTProduct);

      // 转换为UnifiedDownlinkCommand
      UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromJson(downRequest);

      // 调用第三方平台进行注册
      R result = IoTDownlFactory.getIDown(ioTProduct.getThirdPlatform()).doAction(command);

      if (result.isSuccess()) {
        log.info("[{}] 设备自动注册成功: deviceId={}", getName(), deviceId);

        // 记录注册信息到上下文
        request.setContextValue("autoRegistered", true);
        request.setContextValue("registerTime", System.currentTimeMillis());
        request.setContextValue("registerRequest", downRequest);

        return true;
      } else {
        log.error("[{}] 设备自动注册失败: deviceId={}, 错误: {}",
                getName(), deviceId, result.getMsg());
        request.setContextValue("registerError", result.getMsg());
        return false;
      }

    } catch (Exception e) {
      log.error("[{}] 设备自动注册异常: ", getName(), e);
      return false;
    }
  }

  /** 构建自动注册请求 */
  protected JSONObject buildAutoRegisterRequest(
      WebSocketUPRequest request, String deviceId, IoTProduct ioTProduct) {
    JSONObject downRequest = new JSONObject();
    downRequest.set("appUnionId", unionId);
    downRequest.set("productKey", ioTProduct.getProductKey());
    downRequest.set("deviceId", deviceId);
    downRequest.set("cmd", DownCmd.DEV_ADD.name());

    JSONObject deviceData = new JSONObject();
    deviceData.set("deviceName", deviceId);
    deviceData.set("imei", deviceId);
    deviceData.set("latitude", latitude);
    deviceData.set("longitude", longitude);

    // WebSocket特定：如果有网络联盟ID，添加到请求中
    if (StrUtil.isNotBlank(request.getNetworkUnionId())) {
      deviceData.set("networkUnionId", request.getNetworkUnionId());
    }

    downRequest.set("data", deviceData);

    log.debug("[{}] 自动注册请求构建完成: {}", getName(), downRequest);
    return downRequest;
  }

  /** 重新回填设备信息 */
  protected boolean refillDeviceInfo(WebSocketUPRequest request) {
    try {
      // 等待一小段时间确保设备注册完成
      Thread.sleep(100);

      // 重新查询设备信息
      IoTDeviceDTO ioTDeviceDTO =
          lifeCycleDevInstance(
              IoTDeviceQuery.builder()
                  .deviceId(request.getDeviceId())
                  .productKey(request.getProductKey())
                  .build());

      if (ioTDeviceDTO == null) {
        log.warn("[{}] 自动注册后设备信息仍然为空: productKey={}, deviceId={}",
                getName(), request.getProductKey(), request.getDeviceId());
        return false;
      }

      // 更新请求中的设备信息
      request.setIoTDeviceDTO(ioTDeviceDTO);
      request.setContextValue("deviceInfo", ioTDeviceDTO);

      log.debug("[{}] 设备信息重新回填成功: productKey={}, deviceId={}",
              getName(), request.getProductKey(), request.getDeviceId());
      return true;

    } catch (Exception e) {
      log.error("[{}] 设备信息重新回填异常: ", getName(), e);
      return false;
    }
  }
}
