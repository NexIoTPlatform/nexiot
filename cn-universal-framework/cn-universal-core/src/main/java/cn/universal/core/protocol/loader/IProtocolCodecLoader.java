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

package cn.universal.core.protocol.loader;

import cn.universal.common.exception.CodecException;
import cn.universal.core.protocol.support.ProtocolCodecSupport.CodecMethod;

/**
 * 协议编解码加载器接口
 *
 * <p>定义协议编解码器的加载和管理方法
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/01/20
 */
public interface IProtocolCodecLoader {

  /**
   * 加载协议编解码器
   *
   * @param productKey 产品Key
   * @param codecMethod 编解码方法
   * @throws CodecException 编解码异常
   */
  void load(String productKey, CodecMethod codecMethod) throws CodecException;

  /**
   * 移除协议编解码器
   *
   * @param productKey 产品Key
   */
  void remove(String productKey);

  /**
   * 检查是否已加载
   *
   * @param productKey 产品Key
   * @return 是否已加载
   */
  boolean isLoaded(String productKey);

  /**
   * 执行编解码操作
   *
   * @param productKey 产品Key
   * @param payload 原始数据
   * @param codecMethod 编解码方法
   * @return 编解码结果
   * @throws CodecException 编解码异常
   */
  String execute(String productKey, String payload, CodecMethod codecMethod) throws CodecException;
}
