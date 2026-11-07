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
import lombok.extern.slf4j.Slf4j;

/**
 * 算法引擎抽象基类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/2/27
 */
@Slf4j
public abstract class AbstractAlgorithmEngine implements AlgorithmEngine {

  @Override
  public AlgorithmResult execute(AlgorithmContext context) {
    long startTime = System.currentTimeMillis();

    try {
      log.debug(
          "[算法引擎][{}] 开始执行算法，设备: {}",
          getAlgorithmName(),
          context.getDeviceId());

      // 参数校验
      if (!validateContext(context)) {
        return AlgorithmResult.failed(
            getAlgorithmType(),
            getAlgorithmName(),
            context.getDeviceId(),
            context.getProductKey(),
            "算法上下文参数校验失败");
      }

      // 执行算法
      AlgorithmResult result = doExecute(context);

      long duration = System.currentTimeMillis() - startTime;
      log.debug(
          "[算法引擎][{}] 算法执行完成，设备: {}, 耗时: {}ms",
          getAlgorithmName(),
          context.getDeviceId(),
          duration);

      return result;

    } catch (Exception e) {
      log.error(
          "[算法引擎][{}] 算法执行异常，设备: {}",
          getAlgorithmName(),
          context.getDeviceId(),
          e);

      return AlgorithmResult.failed(
          getAlgorithmType(),
          getAlgorithmName(),
          context.getDeviceId(),
          context.getProductKey(),
          "算法执行异常: " + e.getMessage());
    }
  }

  /**
   * 校验上下文参数
   *
   * @param context 算法上下文
   * @return 是否通过校验
   */
  protected boolean validateContext(AlgorithmContext context) {
    if (context == null) {
      log.warn("[算法引擎][{}] 上下文为空", getAlgorithmName());
      return false;
    }

    if (context.getDeviceId() == null) {
      log.warn("[算法引擎][{}] 设备ID为空", getAlgorithmName());
      return false;
    }

    if (context.getConfig() == null || context.getConfig().isEmpty()) {
      log.warn("[算法引擎][{}] 算法配置为空", getAlgorithmName());
      return false;
    }

    return true;
  }

  /**
   * 执行算法的具体实现
   *
   * @param context 算法上下文
   * @return 算法结果
   */
  protected abstract AlgorithmResult doExecute(AlgorithmContext context);

  /**
   * 从配置中获取参数
   *
   * @param context 算法上下文
   * @param key 参数键
   * @param defaultValue 默认值
   * @param <T> 参数类型
   * @return 参数值
   */
  @SuppressWarnings("unchecked")
  protected <T> T getConfigValue(AlgorithmContext context, String key, T defaultValue) {
    if (context.getConfig() == null) {
      return defaultValue;
    }

    Object value = context.getConfig().get(key);
    if (value == null) {
      return defaultValue;
    }

    try {
      return (T) value;
    } catch (ClassCastException e) {
      log.warn(
          "[算法引擎][{}] 配置参数类型转换失败: key={}, value={}",
          getAlgorithmName(),
          key,
          value);
      return defaultValue;
    }
  }

  /**
   * 安全读取 double 类型配置值
   *
   * <p>JSON 反序列化后整数值会被解析为 {@code Integer}，直接泛型强转 {@code (Double)} 会抛出
   * {@link ClassCastException}。本方法通过 {@link Number#doubleValue()} 统一处理
   * Integer/Long/Double/Float 等各数字类型。
   *
   * @param context 算法上下文
   * @param key 配置键
   * @param defaultValue 默认值
   * @return double 值
   */
  protected double getDoubleConfigValue(
      AlgorithmContext context, String key, double defaultValue) {
    if (context.getConfig() == null) {
      return defaultValue;
    }
    Object value = context.getConfig().get(key);
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    return defaultValue;
  }

  /**
   * 安全读取 int 类型配置值
   *
   * <p>JSON 反序列化后数值可能为 Integer/Double/Long，直接泛型强转会抛出
   * {@link ClassCastException}。本方法通过 {@link Number#intValue()} 统一处理各数字类型。
   *
   * @param context 算法上下文
   * @param key 配置键
   * @param defaultValue 默认值
   * @return int 值
   */
  protected int getIntConfigValue(AlgorithmContext context, String key, int defaultValue) {
    if (context.getConfig() == null) {
      return defaultValue;
    }
    Object value = context.getConfig().get(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    return defaultValue;
  }

  /**
   * 安全读取 boolean 类型配置值
   *
   * <p>支持 Boolean、字符串（true/false/1/0/yes/no）和数字（0/非0）。
   *
   * @param context 算法上下文
   * @param key 配置键
   * @param defaultValue 默认值
   * @return boolean 值
   */
  protected boolean getBooleanConfigValue(
      AlgorithmContext context, String key, boolean defaultValue) {
    if (context.getConfig() == null) {
      return defaultValue;
    }

    Object value = context.getConfig().get(key);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    if (value instanceof Number) {
      return ((Number) value).intValue() != 0;
    }
    if (value instanceof String) {
      String normalized = ((String) value).trim().toLowerCase();
      if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized)) {
        return true;
      }
      if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized)) {
        return false;
      }
    }
    return defaultValue;
  }
}
