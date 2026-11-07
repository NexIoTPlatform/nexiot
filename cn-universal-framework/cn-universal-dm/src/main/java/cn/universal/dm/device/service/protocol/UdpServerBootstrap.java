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
 * UDP服务器启动器接口
 *
 * <p>用于管理UDP服务器的启动、停止、重启等操作
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
public interface UdpServerBootstrap {

  /**
   * 启动产品UDP服务器
   *
   * @param productKey 产品Key
   * @return 是否启动成功
   */
  boolean startProductUdpServer(String productKey);

  /**
   * 停止产品UDP服务器
   *
   * @param productKey 产品Key
   * @return 是否停止成功
   */
  boolean stopProductUdpServer(String productKey);

  /**
   * 重启产品UDP服务器
   *
   * @param productKey 产品Key
   * @return 是否重启成功
   */
  boolean restartProductUdpServer(String productKey);

  /**
   * 检查产品UDP服务器是否存活
   *
   * @param productKey 产品Key
   * @return 是否存活
   */
  boolean isProductUdpServerAlive(String productKey);
}
