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

package cn.universal.persistence.base;

import cn.universal.persistence.entity.IoTProduct;

/** 产品（设备）全Action生命周期接口 */
public interface IoTProductAction extends IoTAction {

  /**
   * 产品创建
   *
   * @param productKey 产品唯一标识
   * @param unionId 创建用户
   */
  void create(String productKey, String unionId);

  /**
   * 产品更新
   *
   * @param productKey 产品唯一标识
   */
  void update(String productKey);

  /**
   * 产品删除
   *
   * @param productKey
   */
  void delete(String productKey);

  /**
   * 产品启用
   *
   * @param productKey
   */
  void enable(String productKey);

  /**
   * 产品禁用
   *
   * @param productKey
   */
  void disable(String productKey);

  /**
   * 产品设置为公共产品
   *
   * @param productKey
   */
  void publish(String productKey);

  /**
   * 物模型创建
   *
   * @param productKey
   */
  default void metadataCreate(String productKey) {}

  /**
   * 物模型更新
   *
   * @param productKey
   */
  default void metadataUpdate(String productKey) {}

  /**
   * 物模型删除
   *
   * @param productKey
   */
  default void metadataDelete(String productKey) {}
}
