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

package cn.universal.persistence.base;

import cn.universal.core.message.DownRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;

/**
 * 设备后置处理器接口
 *
 * <p>用于在设备生命周期操作完成后执行额外的处理逻辑
 *
 * <p>支持的操作类型：
 *
 * <ul>
 *   <li>CREATE - 设备创建后
 *   <li>UPDATE - 设备更新后
 *   <li>DELETE - 设备删除后
 *   <li>ONLINE - 设备上线后
 *   <li>OFFLINE - 设备离线后
 *   <li>ENABLE - 设备启用后
 *   <li>DISABLE - 设备禁用后
 *   <li>COMMAND_RESP - 设备指令回复后
 * </ul>
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
public interface IoTDevicePostProcessor {

  /**
   * 获取处理器名称
   *
   * @return 处理器名称
   */
  String getName();

  /**
   * 获取处理器优先级 数值越小优先级越高
   *
   * @return 优先级
   */
  default int getOrder() {
    return 0;
  }

  /**
   * 判断是否支持指定的操作类型
   *
   * @param operation 操作类型
   * @return 是否支持
   */
  boolean supports(Operation operation);

  /**
   * 执行后置处理
   *
   * @param operation 操作类型
   * @param deviceDTO 设备信息
   * @param downRequest 下行请求（可能为null）
   */
  void process(Operation operation, IoTDeviceDTO deviceDTO, DownRequest downRequest);

  /** 操作类型枚举 */
  enum Operation {
    CREATE("设备创建"),
    UPDATE("设备更新"),
    DELETE("设备删除"),
    ONLINE("设备上线"),
    OFFLINE("设备离线"),
    ENABLE("设备启用"),
    DISABLE("设备禁用"),
    COMMAND_RESP("设备指令回复");

    private final String description;

    Operation(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }
}
