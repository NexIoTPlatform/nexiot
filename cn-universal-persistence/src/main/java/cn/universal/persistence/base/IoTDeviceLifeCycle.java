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

import cn.universal.common.constant.IoTConstant.DevLifeCycle;
import cn.universal.core.message.DownRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;

/**
 * 设备生命周期接口
 *
 * <p>1.创建设备
 *
 * <p>2.设备上线和下线
 *
 * <p>3.禁用和启用设备
 *
 * <p>4.删除设备
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/1/20
 */
public interface IoTDeviceLifeCycle extends IoTAction {

  /**
   * 设备创建
   *
   * @param productKey 产品唯一标识
   * @param deviceId 设备编号
   */
  void create(String productKey, String deviceId, DownRequest downRequest);

  void online(String productKey, String deviceId);

  /**
   * 设备离线
   *
   * @param productKey 产品key
   * @param deviceId 设备编号
   */
  void offline(String productKey, String deviceId);

  /** 设备修改 */
  void update(String productKey, String deviceId, DownRequest downRequest);

  /**
   * 设备启用
   *
   * @param iotId
   */
  void enable(String iotId);

  /**
   * 设备禁用
   *
   * @param iotId
   */
  void disable(String iotId);

  /**
   * 设备删除
   *
   * @param ioTDeviceDTO
   */
  void delete(IoTDeviceDTO ioTDeviceDTO, DownRequest downRequest);

  /**
   * 判断物模型是否官方处理
   *
   * @param ioTDeviceDTO
   * @param devLifeCycle
   * @return
   */
  default boolean thirdSupport(IoTDeviceDTO ioTDeviceDTO, DevLifeCycle devLifeCycle) {
    return false;
  }

  /**
   * 指令下发
   *
   * <p>主要用于统一记录日志，推送消息
   *
   * @param ioTDeviceDTO 设备信息
   * @param functions 功能
   */
  default void command(IoTDeviceDTO ioTDeviceDTO, String commandId, Object functions) {}

  /**
   * 指令下发
   *
   * <p>主要用于统一记录日志，推送消息
   *
   * @param ioTDeviceDTO 设备信息
   * @param functions 功能
   */
  default void commandSuccess(IoTDeviceDTO ioTDeviceDTO, String commandId, Object functions) {}

  /**
   * 指令回复
   *
   * <p>主要用于统一记录日志，推送消息
   *
   * @param ioTDeviceDTO 设备信息
   * @param resp 功能
   */
  default void commandResp(IoTDeviceDTO ioTDeviceDTO, String commandId, Object resp) {}

}
