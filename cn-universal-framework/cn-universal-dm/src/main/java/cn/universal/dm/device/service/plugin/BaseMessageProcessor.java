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

package cn.universal.dm.device.service.plugin;

/**
 * 通用消息处理器顶层接口
 *
 * <p>定义所有消息处理器的基础契约，不同模块根据自己的需求实现具体的处理逻辑 避免在顶层接口中定义过多具体方法，保持接口的简洁性和通用性
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/01/20
 */
public interface BaseMessageProcessor extends MessageProcessor {

  /**
   * 处理器名称
   *
   * @return 处理器的可读名称
   */
  String getName();

  /**
   * 执行顺序 数字越小越先执行
   *
   * @return 执行顺序
   */
  int getOrder();

  /**
   * 处理器描述（可选）
   *
   * @return 处理器的详细描述
   */
  default String getDescription() {
    return getName();
  }

  /**
   * 处理器优先级（可选） 当order相同时，按priority排序
   *
   * @return 优先级
   */
  default int getPriority() {
    return 0;
  }

  /**
   * 是否启用此处理器
   *
   * @return true表示启用，false表示禁用
   */
  default boolean isEnabled() {
    return true;
  }

  /**
   * 获取处理器版本信息（可选）
   *
   * @return 版本信息，默认返回"1.0"
   */
  default String getVersion() {
    return "1.0";
  }

  /**
   * 获取处理器类型标识（可选） 用于分组和管理处理器
   *
   * @return 处理器类型，默认返回"GENERIC"
   */
  default String getType() {
    return "GENERIC";
  }

  /**
   * 获取处理器作者信息（可选）
   *
   * @return 作者信息，默认返回空字符串
   */
  default String getAuthor() {
    return "";
  }

  /**
   * 检查处理器是否支持异步执行（可选）
   *
   * @return true表示支持异步，false表示同步，默认为false
   */
  default boolean isAsyncSupported() {
    return false;
  }

  /**
   * 获取处理器超时时间（毫秒）（可选）
   *
   * @return 超时时间，默认为30秒
   */
  default long getTimeoutMillis() {
    return 30000L;
  }

  /**
   * 获取处理器的最大重试次数（可选）
   *
   * @return 最大重试次数，默认为0（不重试）
   */
  default int getMaxRetryCount() {
    return 0;
  }

  /** 处理器初始化方法（可选） 在处理器被加载时调用 */
  default void initialize() {
    // 默认不做任何操作
  }

  /** 处理器销毁方法（可选） 在处理器被卸载时调用 */
  default void destroy() {
    // 默认不做任何操作
  }

  /**
   * 健康检查方法（可选）
   *
   * @return true表示健康，false表示不健康，默认为true
   */
  default boolean isHealthy() {
    return true;
  }

  /**
   * 获取处理器统计信息（可选）
   *
   * @return 统计信息的字符串表示，默认返回空字符串
   */
  default String getStatistics() {
    return "";
  }
}
