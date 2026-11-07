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

package cn.universal.rule.rulego.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RuleGo API 统一响应对象
 *
 * @param <T> 数据类型泛型
 * @author gitee.com/NexIoT
 * @since 2025/01/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RulegoApiResponse<T> {

  /** 是否成功 */
  private Boolean success;

  /** 响应消息 */
  private String message;

  /** 响应数据 */
  private T data;

  /**
   * 创建成功响应
   *
   * @param data 响应数据
   * @param <T> 数据类型泛型
   * @return 成功响应
   */
  public static <T> RulegoApiResponse<T> success(T data) {
    return RulegoApiResponse.<T>builder().success(true).message("操作成功").data(data).build();
  }

  /**
   * 创建成功响应
   *
   * @param data 响应数据
   * @param message 成功消息
   * @param <T> 数据类型泛型
   * @return 成功响应
   */
  public static <T> RulegoApiResponse<T> success(T data, String message) {
    return RulegoApiResponse.<T>builder().success(true).message(message).data(data).build();
  }

  /**
   * 创建失败响应
   *
   * @param message 错误消息
   * @param <T> 数据类型泛型
   * @return 失败响应
   */
  public static <T> RulegoApiResponse<T> error(String message) {
    return RulegoApiResponse.<T>builder().success(false).message(message).data(null).build();
  }

  /**
   * 创建失败响应
   *
   * @param message 错误消息
   * @param data 错误数据
   * @param <T> 数据类型泛型
   * @return 失败响应
   */
  public static <T> RulegoApiResponse<T> error(String message, T data) {
    return RulegoApiResponse.<T>builder().success(false).message(message).data(data).build();
  }
}
