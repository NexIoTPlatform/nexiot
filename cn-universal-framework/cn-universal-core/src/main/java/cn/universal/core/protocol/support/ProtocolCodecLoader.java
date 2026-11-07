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

package cn.universal.core.protocol.support;

import cn.universal.common.exception.CodecException;

/**
 * 协议编解码支持
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/8/9 19:06
 */
public interface ProtocolCodecLoader {

  /**
   * 协议全路径包名
   *
   * @return
   */
  String getProviderType();

  /**
   * 根据全路径包名加载jar包插件
   *
   * @param definition
   * @return
   */
  default void load(ProtocolSupportDefinition definition) throws CodecException {}
}
