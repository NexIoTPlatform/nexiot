/*
 *
 * Copyright (c) 2025, cn-universal. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */
package cn.universal.core.service;

public interface UniversalUP {

  /** 通用消息组件 */
  String name();

  /**
   * 上行消息入口类
   *
   * @param msg 消息内容
   */
  void up(String msg);

  /**
   * 重新发送云端指令
   *
   * @param productKey 产品key
   * @param deviceId 设备序列号
   */
  default void resendStoreCommand(String productKey, String deviceId) {}
}
