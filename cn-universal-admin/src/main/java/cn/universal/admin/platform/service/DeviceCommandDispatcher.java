/*
 *
 * Copyright (c) 2025, IoT-Universal. All Rights Reserved.
 *
 * @Description: 本文件由 Aleo 开发并拥有版权,未经授权严禁擅自商用、复制或传播。
 * @Author: Aleo
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.admin.platform.service;

import cn.universal.common.domain.R;
import cn.universal.persistence.base.DeviceCommandSender;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTProductMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 设备指令统一调度服务
 *
 * <p>自动发现并路由到可用的协议处理器
 *
 * <p>支持TCP、UDP、MQTT等多种协议的自动识别和调度
 *
 * @author gitee.com/NexIoT
 * @date 2025-10-26
 */
@Service
@Slf4j
public class DeviceCommandDispatcher {

  @Resource private IoTProductMapper productMapper;

  /** 自动注入所有实现了 DeviceCommandSender 的 Bean required=false 避免找不到时启动失败 */
  @Autowired(required = false)
  private List<DeviceCommandSender> commandSenders;

  /** 协议类型 -> 处理器映射 */
  private final Map<String, DeviceCommandSender> senderMap = new HashMap<>();

  /**
   * 初始化协议处理器映射
   *
   * <p>在Spring容器启动后自动扫描并注册所有可用的协议处理器
   */
  @PostConstruct
  public void init() {
    if (commandSenders == null || commandSenders.isEmpty()) {
      log.warn("[DeviceCommandDispatcher] 未找到任何协议处理器实现,网关轮询功能可能不可用");
      log.warn("[DeviceCommandDispatcher] 请确保TCP/UDP/MQTT协议模块已正确引入并实现DeviceCommandSender接口");
      return;
    }

    for (DeviceCommandSender sender : commandSenders) {
      try {
        if (sender.isAvailable()) {
          String protocol = sender.getSupportedProtocol();
          if (protocol != null && !protocol.isEmpty()) {
            senderMap.put(protocol.toUpperCase(), sender);
            log.info(
                "[DeviceCommandDispatcher] 注册协议处理器: {} -> {}",
                protocol.toUpperCase(),
                sender.getClass().getSimpleName());
          } else {
            log.warn(
                "[DeviceCommandDispatcher] 协议处理器返回空协议名称,跳过注册: {}",
                sender.getClass().getSimpleName());
          }
        } else {
          log.info(
              "[DeviceCommandDispatcher] 协议处理器不可用,跳过注册: {}", sender.getClass().getSimpleName());
        }
      } catch (Exception e) {
        log.error("[DeviceCommandDispatcher] 注册协议处理器失败: {}", sender.getClass().getSimpleName(), e);
      }
    }

    log.info("[DeviceCommandDispatcher] 初始化完成,可用协议: {}", senderMap.keySet());
  }

  /**
   * 发送设备指令
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @param payload HEX载荷或其他格式载荷
   * @return 发送结果
   */
  public R<?> sendCommand(String productKey, String deviceId, String payload) {
    try {
      // 1. 查询产品协议类型
      IoTProduct product =
          productMapper.selectOne(IoTProduct.builder().productKey(productKey).build());

      if (product == null) {
        log.warn("[DeviceCommandDispatcher] 产品不存在: productKey={}", productKey);
        return R.error("产品不存在");
      }

      String protocol = product.getThirdPlatform();
      if (protocol == null || protocol.isEmpty()) {
        log.warn(
            "[DeviceCommandDispatcher] 产品未配置协议: productKey={}, deviceId={}", productKey, deviceId);
        return R.error("产品未配置协议类型");
      }

      // 2. 查找协议处理器
      DeviceCommandSender sender = senderMap.get(protocol.toUpperCase());
      if (sender == null) {
        log.warn(
            "[DeviceCommandDispatcher] 不支持的协议或协议模块未安装: productKey={}, deviceId={}, protocol={}, available={}",
            productKey,
            deviceId,
            protocol,
            senderMap.keySet());
        return R.error("不支持的协议: " + protocol + ", 可用协议: " + senderMap.keySet());
      }

      // 3. 发送指令
      log.info(
          "[DeviceCommandDispatcher] 路由指令: productKey={}, deviceId={}, protocol={}, payloadLength={}",
          productKey,
          deviceId,
          protocol,
          payload != null ? payload.length() : 0);

      return sender.sendCommand(productKey, deviceId, payload);

    } catch (Exception e) {
      log.error(
          "[DeviceCommandDispatcher] 发送指令异常: productKey={}, deviceId={}", productKey, deviceId, e);
      return R.error("发送失败: " + e.getMessage());
    }
  }

  /**
   * 获取可用的协议列表
   *
   * @return 协议列表
   */
  public List<String> getAvailableProtocols() {
    return List.copyOf(senderMap.keySet());
  }

  /**
   * 检查协议是否可用
   *
   * @param protocol 协议名称
   * @return true-可用, false-不可用
   */
  public boolean isProtocolAvailable(String protocol) {
    return protocol != null && senderMap.containsKey(protocol.toUpperCase());
  }

  /**
   * 获取协议处理器数量
   *
   * @return 处理器数量
   */
  public int getHandlerCount() {
    return senderMap.size();
  }
}
