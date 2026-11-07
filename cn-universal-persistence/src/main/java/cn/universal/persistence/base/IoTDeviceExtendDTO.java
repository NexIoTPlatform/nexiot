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

/** 根据产品自定义扩展实现 */
public interface IoTDeviceExtendDTO {

  String productKey();

  /**
   * 下行扩展
   *
   * @param downRequest
   */
  default void downExt(DownRequest downRequest) {}

  /**
   * 上行扩展
   *
   * @param downRequest
   */
  default void upExt(BaseUPRequest downRequest) {}
}
