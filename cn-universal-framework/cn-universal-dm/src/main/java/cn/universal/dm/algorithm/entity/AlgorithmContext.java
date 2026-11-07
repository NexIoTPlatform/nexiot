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

package cn.universal.dm.algorithm.entity;

import cn.universal.persistence.base.BaseUPRequest;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 算法上下文
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/2/27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgorithmContext {

  /** 当前请求 */
  private BaseUPRequest currentRequest;

  /** 设备ID */
  private String deviceId;

  /** 产品Key */
  private String productKey;

  /** 算法配置 */
  private Map<String, Object> config;

  /** 历史数据 */
  private List<Map<String, Object>> historicalData;

  /** 当前数据 */
  private Map<String, Object> currentData;

  /** 时间戳 */
  private Long timestamp;

  /** 扩展参数 */
  private Map<String, Object> extParams;
}
