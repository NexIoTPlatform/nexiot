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

package cn.universal.dm.algorithm;

import cn.universal.dm.algorithm.entity.AlgorithmContext;
import cn.universal.dm.algorithm.entity.AlgorithmResult;
import cn.universal.persistence.base.BaseUPRequest;

/**
 * 算法引擎接口
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/2/27
 */
public interface AlgorithmEngine {

  /**
   * 获取算法类型
   *
   * @return 算法类型标识
   */
  String getAlgorithmType();

  /**
   * 获取算法名称
   *
   * @return 算法名称
   */
  String getAlgorithmName();

  /**
   * 获取算法描述
   *
   * @return 算法描述
   */
  String getDescription();

  /**
   * 执行算法计算
   *
   * @param context 算法上下文
   * @return 算法结果
   */
  AlgorithmResult execute(AlgorithmContext context);

  /**
   * 判断是否支持处理该请求
   *
   * @param request 上行请求
   * @return 是否支持
   */
  boolean supports(BaseUPRequest request);

  /**
   * 获取算法优先级，数字越小优先级越高
   *
   * @return 优先级
   */
  default int getOrder() {
    return 100;
  }
}
