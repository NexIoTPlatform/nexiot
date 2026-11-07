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
package cn.universal.persistence.message;

import cn.universal.persistence.base.BaseDownRequest;
import cn.universal.persistence.base.BaseUPRequest;

/**
 * 消息适配器名称，用于有特定需求的情况
 *
 * <p>用处不大
 */
public interface IoTMessageAdapter {

  /**
   * @return 适配器名称
   */
  String name();

  /** 格式化上行消息 */
  String formatUpMessage(BaseUPRequest upRequest);

  /** 格式化下行消息 */
  String formatDownMessage(BaseDownRequest downRequest);
}
