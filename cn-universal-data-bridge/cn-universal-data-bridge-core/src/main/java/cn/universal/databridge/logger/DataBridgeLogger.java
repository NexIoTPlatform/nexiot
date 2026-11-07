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

package cn.universal.databridge.logger;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 数据桥接日志记录器 轻量级实现，仅打印日志，不存储数据库
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Component
@Slf4j
public class DataBridgeLogger {

  /** 记录执行日志（仅打印，不存储数据库） */
  public void logExecution(
      String configName,
      String status,
      int processedCount,
      int failedCount,
      String errorMessage,
      long executionTime) {

    if ("SUCCESS".equals(status)) {
      log.info(
          "[数据桥接] 配置: {}, 状态: {}, 处理数量: {}, 耗时: {}ms",
          configName,
          status,
          processedCount,
          executionTime);
    } else if ("FAILED".equals(status)) {
      log.error(
          "[数据桥接] 配置: {}, 状态: {}, 处理数量: {}, 失败数量: {}, 错误: {}, 耗时: {}ms",
          configName,
          status,
          processedCount,
          failedCount,
          errorMessage,
          executionTime);
    } else {
      log.warn(
          "[数据桥接] 配置: {}, 状态: {}, 处理数量: {}, 失败数量: {}, 错误: {}, 耗时: {}ms",
          configName,
          status,
          processedCount,
          failedCount,
          errorMessage,
          executionTime);
    }
  }

  /** 记录插件状态 */
  public void logPluginStatus(String pluginName, String status, String message) {
    log.info("[数据桥接插件] 插件: {}, 状态: {}, 信息: {}", pluginName, status, message);
  }

  /** 记录连接测试结果 */
  public void logConnectionTest(
      String resourceName, String resourceType, boolean success, String message) {
    if (success) {
      log.info("[数据桥接] 资源连接测试成功 - 名称: {}, 类型: {}", resourceName, resourceType);
    } else {
      log.error("[数据桥接] 资源连接测试失败 - 名称: {}, 类型: {}, 错误: {}", resourceName, resourceType, message);
    }
  }

  /** 记录配置验证结果 */
  public void logConfigValidation(String configName, boolean success, String message) {
    if (success) {
      log.info("[数据桥接] 配置验证成功 - 配置: {}", configName);
    } else {
      log.error("[数据桥接] 配置验证失败 - 配置: {}, 错误: {}", configName, message);
    }
  }
}
