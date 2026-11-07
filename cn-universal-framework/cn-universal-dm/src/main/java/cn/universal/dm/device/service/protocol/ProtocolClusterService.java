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

package cn.universal.dm.device.service.protocol;

/**
 * 协议集群服务接口
 *
 * <p>用于管理协议服务器的集群操作，包括启动、停止、重启等集群化操作
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
public interface ProtocolClusterService {

  /**
   * 集群启动（本地启动 + 广播）
   *
   * @param productKey 产品Key
   * @return 是否启动成功
   */
  boolean start(String productKey);

  /**
   * 集群停止（本地停止 + 广播）
   *
   * @param productKey 产品Key
   * @return 是否停止成功
   */
  boolean stop(String productKey);

  /**
   * 集群重启（本地重启 + 广播）
   *
   * @param productKey 产品Key
   * @return 是否重启成功
   */
  boolean restart(String productKey);

  /**
   * 本地启动（不广播）
   *
   * @param productKey 产品Key
   * @return 是否启动成功
   */
  boolean startLocal(String productKey);

  /**
   * 本地停止（不广播）
   *
   * @param productKey 产品Key
   * @return 是否停止成功
   */
  boolean stopLocal(String productKey);

  /**
   * 本地重启（不广播）
   *
   * @param productKey 产品Key
   * @return 是否重启成功
   */
  boolean restartLocal(String productKey);

  /**
   * 检查产品服务器是否存活
   *
   * @param productKey 产品Key
   * @return 是否存活
   */
  default boolean isProductServerAlive(String productKey) {
    return false;
  }
}
