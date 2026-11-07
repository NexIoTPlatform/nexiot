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

package cn.universal.rule.transmit;

import cn.hutool.json.JSONObject;
import cn.universal.rule.model.RuleTarget;

/**
 * 规则转发 @Author gitee.com/NexIoT
 *
 * @since 2023/1/14 16:29
 */
public interface RuleTransmit {

  /**
   * 获取转发类型
   *
   * @return 类型
   */
  String type();

  /**
   * 数据转发
   *
   * @param data 数据
   * @param target 转发目标
   */
  String transmit(JSONObject data, RuleTarget target);

  /**
   * 测试数据转发
   *
   * @param data 数据
   * @param target 转发目标
   */
  String testTransmit(JSONObject data, RuleTarget target);
}
