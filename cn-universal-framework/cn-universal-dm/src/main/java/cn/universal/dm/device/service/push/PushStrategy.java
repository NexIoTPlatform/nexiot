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

package cn.universal.dm.device.service.push;

import cn.universal.dm.device.entity.IoTPushResult;
import cn.universal.persistence.base.BaseUPRequest;

/**
 * 推送策略接口
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
public interface PushStrategy<C> {

  /**
   * 执行推送
   *
   * @param request 上行请求
   * @param messageJson 消息JSON字符串
   */
  IoTPushResult execute(BaseUPRequest request, String messageJson, C config);

  /**
   * 是否支持该推送类型
   *
   * @return 是否支持
   */
  boolean isSupported(C config);
}
