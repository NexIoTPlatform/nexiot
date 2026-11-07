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

package cn.universal.persistence.base;

import cn.universal.common.domain.R;

/**
 * 设备指令发送接口
 *
 * <p>各协议层可选实现此接口,用于统一的设备指令下发
 *
 * <p>支持TCP、UDP、MQTT等多种协议的设备指令发送
 *
 * @author gitee.com/NexIoT
 * @date 2025-10-26
 */
public interface DeviceCommandSender {

  /**
   * 发送设备指令
   *
   * @param productKey 产品Key (与deviceId组成复合键)
   * @param deviceId 设备ID
   * @param payload HEX格式载荷或其他格式载荷 (由具体协议处理)
   * @return 发送结果
   */
  R<?> sendCommand(String productKey, String deviceId, String payload);

  /**
   * 获取支持的协议类型
   *
   * @return 协议名称: "TCP", "UDP", "MQTT", "HTTP" 等
   */
  String getSupportedProtocol();

  /**
   * 检查协议处理器是否可用
   *
   * <p>用于运行时检查协议是否启用
   *
   * <p>默认实现返回true,子类可覆盖此方法实现自定义检查逻辑
   *
   * @return true-可用, false-不可用
   */
  default boolean isAvailable() {
    return true;
  }
}
