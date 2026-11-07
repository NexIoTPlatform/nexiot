/*
 *
 * Copyright (c) 2025, IoT-Universal. All Rights Reserved.
 *
 * @Description: 本文件由 NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.persistence.base;

import cn.universal.common.domain.R;
import cn.universal.persistence.entity.GatewayPollingCommand;
import cn.universal.persistence.entity.IoTDevice;

/**
 * Modbus 轮询执行器
 *
 * <p>下沉到 persistence.base，供 protocol 模块直接实现，避免 admin 反向依赖协议模块。
 */
public interface ModbusPollingExecutor {

  /**
   * 执行一条 Modbus 轮询指令
   *
   * @param gateway 网关设备
   * @param command 轮询指令
   * @return 执行结果
   */
  R<?> execute(IoTDevice gateway, GatewayPollingCommand command);

  /**
   * 试读一条 Modbus 指令。
   *
   * <p>默认回退到正式执行，具体协议实现可覆盖为只读调试，不进入子设备上行链路。
   *
   * @param gateway 网关设备
   * @param command 轮询指令
   * @return 试读结果
   */
  default R<?> preview(IoTDevice gateway, GatewayPollingCommand command) {
    return execute(gateway, command);
  }

  /**
   * 仅测试 Modbus 端点连通性，不发送寄存器读写帧。
   *
   * @param device 待测试设备
   * @return 连接测试结果
   */
  default R<?> probe(IoTDevice device) {
    return R.error("Modbus endpoint probe is not supported");
  }
}
