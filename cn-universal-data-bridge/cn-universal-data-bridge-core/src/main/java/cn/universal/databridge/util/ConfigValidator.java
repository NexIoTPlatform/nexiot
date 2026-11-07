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

package cn.universal.databridge.util;

import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.databridge.exception.DataBridgeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * 配置验证工具类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Slf4j
public class ConfigValidator {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  // 名称正则表达式：字母、数字、下划线、中划线，长度1-100
  private static final Pattern NAME_PATTERN =
      Pattern.compile("^[\\u4e00-\\u9fa5a-zA-Z0-9_-]{1,100}$");

  // 产品KEY正则表达式：字母、数字、下划线，长度1-50
  private static final Pattern PRODUCT_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,50}$");

  // 主机地址正则表达式
  private static final Pattern HOST_PATTERN = Pattern.compile("^[a-zA-Z0-9.-]{1,255}$");

  // 数据库名正则表达式：字母、数字、下划线，长度1-64
  private static final Pattern DATABASE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,64}$");

  /** 验证桥接配置 */
  public static void validateDataBridgeConfig(DataBridgeConfig config) {
    if (config == null) {
      throw new DataBridgeException("CONFIG_NULL", "桥接配置不能为空");
    }

    // 验证名称
    validateName(config.getName(), "桥接配置名称");

    // 验证源范围
    if (config.getSourceScope() == null) {
      throw new DataBridgeException("INVALID_SOURCE_SCOPE", "源范围不能为空");
    }

    // 验证目标资源ID
    if (config.getTargetResourceId() == null || config.getTargetResourceId() <= 0) {
      throw new DataBridgeException("INVALID_TARGET_RESOURCE_ID", "目标资源ID无效");
    }

    // 验证桥接类型
    if (config.getBridgeType() == null) {
      throw new DataBridgeException("INVALID_BRIDGE_TYPE", "桥接类型不能为空");
    }

    // 验证模板
    if (config.getTemplate() == null || config.getTemplate().trim().isEmpty()) {
      throw new DataBridgeException("INVALID_TEMPLATE", "模板内容不能为空");
    }

    // 验证统一配置JSON格式
    if (config.getConfig() != null && !config.getConfig().trim().isEmpty()) {
      validateJsonFormat(config.getConfig(), "统一配置");
    }

    // 验证Magic脚本
    if (config.getMagicScript() != null && !config.getMagicScript().trim().isEmpty()) {
      // Magic脚本内容验证可以在这里添加
    }

    // 验证状态
    if (config.getStatus() != null && config.getStatus() != 0 && config.getStatus() != 1) {
      throw new DataBridgeException("INVALID_STATUS", "状态值无效，只能为0或1");
    }

    // 验证描述长度
    if (config.getDescription() != null && config.getDescription().length() > 500) {
      throw new DataBridgeException("INVALID_DESCRIPTION", "描述长度不能超过500个字符");
    }
  }

  /** 验证资源连接 */
  public static void validateResourceConnection(ResourceConnection connection) {
    if (connection == null) {
      throw new DataBridgeException("CONNECTION_NULL", "资源连接不能为空");
    }

    // 验证名称
    validateName(connection.getName(), "资源连接名称");

    // 验证资源类型
    if (connection.getType() == null) {
      throw new DataBridgeException("INVALID_RESOURCE_TYPE", "资源类型不能为空");
    }

    // 验证数据流向
    if (connection.getDataDirection() == null) {
      throw new DataBridgeException("INVALID_DATA_DIRECTION", "数据流向不能为空");
    }

    // 验证扩展配置JSON格式（包含动态配置）
    if (connection.getExtraConfig() != null && !connection.getExtraConfig().trim().isEmpty()) {
      validateJsonFormat(connection.getExtraConfig(), "扩展配置");
    }

    // 验证状态
    if (connection.getStatus() != null
        && connection.getStatus() != 0
        && connection.getStatus() != 1) {
      throw new DataBridgeException("INVALID_STATUS", "状态值无效，只能为0或1");
    }

    // 验证描述长度
    if (connection.getDescription() != null && connection.getDescription().length() > 500) {
      throw new DataBridgeException("INVALID_DESCRIPTION", "描述长度不能超过500个字符");
    }

    // 验证方向
    if (connection.getDirection() == null) {
      throw new DataBridgeException("INVALID_DIRECTION", "连接方向不能为空");
    }
  }

  /** 验证名称 */
  private static void validateName(String name, String fieldName) {
    if (name == null || name.trim().isEmpty()) {
      throw new DataBridgeException("INVALID_NAME", fieldName + "不能为空");
    }
    if (!NAME_PATTERN.matcher(name).matches()) {
      throw new DataBridgeException(
          "INVALID_NAME_FORMAT", fieldName + "格式无效，只能包含中文、字母、数字、下划线、中划线，长度1-100");
    }
  }

  /** 验证产品KEY */
  private static void validateProductKey(String productKey) {
    if (productKey == null || productKey.trim().isEmpty()) {
      throw new DataBridgeException("INVALID_PRODUCT_KEY", "产品KEY不能为空");
    }
    if (!PRODUCT_KEY_PATTERN.matcher(productKey).matches()) {
      throw new DataBridgeException("INVALID_PRODUCT_KEY_FORMAT", "产品KEY格式无效，只能包含字母、数字、下划线，长度1-50");
    }
  }

  /** 验证主机地址 */
  private static void validateHost(String host) {
    if (host == null || host.trim().isEmpty()) {
      throw new DataBridgeException("INVALID_HOST", "主机地址不能为空");
    }
    if (!HOST_PATTERN.matcher(host).matches()) {
      throw new DataBridgeException("INVALID_HOST_FORMAT", "主机地址格式无效");
    }
  }

  /** 验证数据库名 */
  private static void validateDatabaseName(String databaseName) {
    if (databaseName == null || databaseName.trim().isEmpty()) {
      throw new DataBridgeException("INVALID_DATABASE_NAME", "数据库名不能为空");
    }
    if (!DATABASE_NAME_PATTERN.matcher(databaseName).matches()) {
      throw new DataBridgeException(
          "INVALID_DATABASE_NAME_FORMAT", "数据库名格式无效，只能包含字母、数字、下划线，长度1-64");
    }
  }

  /** 验证JSON格式 */
  private static void validateJsonFormat(String json, String fieldName) {
    try {
      objectMapper.readTree(json);
    } catch (JsonProcessingException e) {
      throw new DataBridgeException(
          "INVALID_JSON_FORMAT", fieldName + "JSON格式无效: " + e.getMessage());
    }
  }
}
