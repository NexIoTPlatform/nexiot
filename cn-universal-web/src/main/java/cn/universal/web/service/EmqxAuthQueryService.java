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
package cn.universal.web.service;

import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.IoTUserApplication;
import java.util.List;

/**
 * EMQX 认证查询服务接口 用于查询认证相关的数据库信息
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
public interface EmqxAuthQueryService {

  /**
   * 根据产品Key查询产品信息
   *
   * @param productKey 产品Key
   * @return 产品信息
   */
  IoTProduct queryProductByKey(String productKey);

  /**
   * 根据产品 Key 和设备 ID 查询设备信息
   *
   * @param productKey 产品 Key
   * @param deviceId 设备 ID
   * @return 设备信息
   */
  IoTDevice queryDeviceByProductKeyAndDeviceId(String productKey, String deviceId);

  /**
   * 动态注册设备
   *
   * @param product 产品信息
   * @param deviceId 设备 ID
   * @return 注册后的设备信息
   */
  IoTDevice createDeviceByDynamicRegister(IoTProduct product, String deviceId);

  /**
   * 根据应用ID查询应用信息
   *
   * @param appId 应用ID
   * @return 应用信息
   */
  IoTUserApplication queryApplicationById(String appId);

  /**
   * 根据用户名查询用户信息
   *
   * @param username 用户名
   * @return 用户信息
   */
  IoTUser queryUserByUsername(String username);

  /**
   * 根据unionId查询关联的应用列表
   *
   * @param unionId 联合ID
   * @return 应用列表
   */
  List<IoTUserApplication> queryApplicationsByUnionId(String unionId);
}
