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

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * SQL值转换工具类 提供将Java对象转换为SQL安全格式的通用方法
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Slf4j
public class SqlValueConverter {

  /**
   * 将变量值转换为SQL安全格式 1. 字符串类型：添加单引号，转义内部单引号 2. 数字/布尔类型：直接转换为字符串 3. null：转换为SQL的NULL关键字 4.
   * 日期类型：转换为SQL标准格式 5. 其他类型：转为JSON字符串
   *
   * @param value 要转换的值
   * @return SQL安全格式的字符串
   */
  public static String convertToSqlValue(Object value) {
    if (value == null) {
      return "NULL";
    }

    if (value instanceof String) {
      // 转义单引号（防止SQL注入和语法错误）
      String strValue = (String) value;
      return "'" + strValue.replace("'", "''") + "'";
    } else if (value instanceof Number || value instanceof Boolean) {
      // 数字和布尔类型直接转换
      return value.toString();
    } else if (value instanceof java.util.Date) {
      // 日期类型转换为SQL标准格式（带单引号）
      return "'" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value) + "'";
    } else {
      // 其他类型：转为JSON字符串（带单引号）
      String jsonValue = JSONUtil.toJsonStr(value);
      return "'" + jsonValue.replace("'", "''") + "'";
    }
  }

  /**
   * 批量转换多个值为SQL安全格式
   *
   * @param values 要转换的值数组
   * @return SQL安全格式的字符串数组
   */
  public static String[] convertToSqlValues(Object... values) {
    if (values == null) {
      return new String[0];
    }

    String[] result = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = convertToSqlValue(values[i]);
    }
    return result;
  }

  /**
   * 将变量值转换为SQL安全格式（JSON字符串专用） 使用双引号包围JSON字符串，避免单引号冲突
   *
   * @param value 要转换的值
   * @return SQL安全格式的字符串
   */
  public static String convertToSqlJsonValue(Object value) {
    if (value == null) {
      return "NULL";
    }

    if (value instanceof String) {
      // JSON字符串：使用双引号包围，转义内部双引号
      String strValue = (String) value;
      return "\"" + strValue.replace("\"", "\\\"") + "\"";
    } else if (value instanceof Number || value instanceof Boolean) {
      // 数字和布尔类型直接转换
      return value.toString();
    } else if (value instanceof java.util.Date) {
      // 日期类型转换为SQL标准格式（带双引号）
      return "\"" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value) + "\"";
    } else {
      // 其他类型：转为JSON字符串（带双引号）
      String jsonValue = JSONUtil.toJsonStr(value);
      return "\"" + jsonValue.replace("\"", "\\\"") + "\"";
    }
  }

  /**
   * 检查值是否需要SQL转义
   *
   * @param value 要检查的值
   * @return 是否需要转义
   */
  public static boolean needsEscaping(Object value) {
    if (value == null) {
      return false;
    }

    if (value instanceof String) {
      String strValue = (String) value;
      return strValue.contains("'") || strValue.contains("\\") || strValue.contains("\"");
    }

    return !(value instanceof Number || value instanceof Boolean);
  }
}
