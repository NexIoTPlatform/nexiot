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

package cn.universal.common.poi;

/** Excel数据格式处理适配器 @Author ruoyi */
public interface ExcelHandlerAdapter {

  /**
   * 格式化
   *
   * @param value 单元格数据值
   * @param args excel注解args参数组
   * @return 处理后的值
   */
  Object format(Object value, String[] args);
}
