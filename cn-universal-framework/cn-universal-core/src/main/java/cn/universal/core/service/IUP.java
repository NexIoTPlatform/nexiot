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

package cn.universal.core.service;

import cn.universal.core.message.UPRequest;
import java.util.List;

/** 上行，进行业务处理和转发 */
public interface IUP {

  /** 返回服务名称 */
  String name();

  default void asyncUP(String request) {}

  default void asyncUP(UPRequest request) {}

  /** 上行处理 */
  default Object debugUP(String upMsg) {
    return null;
  }

  /** 上行处理 */
  default Object up(String upMsg) {
    return null;
  }

  /** */
  default List up(UPRequest request) {
    return null;
  }

  /** 模拟上行异步 */
  default void debugAsyncUP(String debugMsg) {}

  /**
   * 重新发送云端指令
   *
   * @param productKey 产品key
   * @param deviceId 设备序列号
   */
  default void resendStoreCommand(String productKey, String deviceId) {}
}
