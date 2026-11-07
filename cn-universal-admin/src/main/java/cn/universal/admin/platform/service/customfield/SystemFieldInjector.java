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

package cn.universal.admin.platform.service.customfield;

import cn.hutool.json.JSONArray;
import cn.universal.persistence.entity.IoTProduct;

/**
 * 系统字段注入器接口
 * 用于根据产品类型自动注入系统内置的扩展字段
 *
 * @author NexIoT
 * @since 2025-02-05
 */
public interface SystemFieldInjector {

  /**
   * 判断是否支持该产品
   *
   * @param product 产品信息
   * @return true-支持，false-不支持
   */
  boolean supports(IoTProduct product);

  /**
   * 注入系统字段到customField数组
   *
   * @param product 产品信息
   * @param customFields 现有的customField数组
   */
  void inject(IoTProduct product, JSONArray customFields);

  /**
   * 获取注入器优先级，数值越小优先级越高
   *
   * @return 优先级
   */
  default int getOrder() {
    return 100;
  }
}
