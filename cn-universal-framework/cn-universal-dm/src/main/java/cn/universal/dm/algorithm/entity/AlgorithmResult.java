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

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 算法结果
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/2/27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgorithmResult {

  /** 算法类型 */
  private String algorithmType;

  /** 算法名称 */
  private String algorithmName;

  /** 设备ID */
  private String deviceId;

  /** 产品Key */
  private String productKey;

  /** 时间戳 */
  private Long timestamp;

  /** 是否成功 */
  private Boolean success;

  /** 错误信息 */
  private String errorMessage;

  /** 结果数据 */
  private Map<String, Object> resultData;

  /** 健康分数（0-100） */
  private Double healthScore;

  /** 健康等级 */
  private String healthLevel;

  /** 是否异常 */
  private Boolean isAnomaly;

  /** 异常分数 */
  private Double anomalyScore;

  /** 建议列表 */
  private List<String> suggestions;

  /** 详细指标结果 */
  private List<IndicatorResult> indicators;

  /** 统计信息 */
  private Map<String, Object> statistics;

  /** 扩展数据 */
  private Map<String, Object> extData;

  /**
   * 指标结果
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class IndicatorResult {
    /** 指标标识 */
    private String identifier;

    /** 指标名称 */
    private String name;

    /** 当前值 */
    private Object value;

    /** 分数 */
    private Double score;

    /** 状态 */
    private String status;

    /** 估计值（用于异常检测） */
    private Double estimatedValue;

    /** 残差（用于异常检测） */
    private Double residual;
  }

  /**
   * 创建成功结果
   */
  public static AlgorithmResult success(
      String algorithmType, String algorithmName, String deviceId, String productKey) {
    return AlgorithmResult.builder()
        .algorithmType(algorithmType)
        .algorithmName(algorithmName)
        .deviceId(deviceId)
        .productKey(productKey)
        .timestamp(System.currentTimeMillis())
        .success(true)
        .build();
  }

  /**
   * 创建失败结果
   */
  public static AlgorithmResult failed(
      String algorithmType,
      String algorithmName,
      String deviceId,
      String productKey,
      String errorMessage) {
    return AlgorithmResult.builder()
        .algorithmType(algorithmType)
        .algorithmName(algorithmName)
        .deviceId(deviceId)
        .productKey(productKey)
        .timestamp(System.currentTimeMillis())
        .success(false)
        .errorMessage(errorMessage)
        .build();
  }
}
