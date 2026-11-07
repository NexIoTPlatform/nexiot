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
import cn.universal.core.protocol.support.ProtocolCodecSupport;
import cn.universal.core.protocol.support.ProtocolCodecSupport.CodecMethod;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象协议编解码加载器基类
 *
 * <p>提供公共的协议加载器实现，子类可以重写特定方法
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/01/20
 */
@Slf4j
public abstract class AbstractProtocolCodecLoader implements IProtocolCodecLoader {

  /** 加载协议编解码器 */
  @Override
  public void load(String productKey, CodecMethod codecMethod) throws CodecException {
    log.debug("加载协议编解码器: productKey={}, method={}", productKey, codecMethod);
    doLoad(productKey, codecMethod);
  }

  /** 移除协议编解码器 */
  @Override
  public void remove(String productKey) {
    log.debug("移除协议编解码器: productKey={}", productKey);
    doRemove(productKey);
  }

  /** 执行编解码操作 */
  @Override
  public String execute(String productKey, String payload, CodecMethod codecMethod)
      throws CodecException {
    if (!isLoaded(productKey)) {
      load(productKey, codecMethod);
    }
    return doExecute(productKey, payload, codecMethod);
  }

  /**
   * 获取协议编解码支持实例
   *
   * @return 协议编解码支持实例
   */
  protected abstract ProtocolCodecSupport getProtocolCodecSupport();

  /**
   * 执行具体的加载逻辑 - 需要子类实现
   *
   * @param productKey 产品Key
   * @param codecMethod 编解码方法
   * @throws CodecException 编解码异常
   */
  protected abstract void doLoad(String productKey, CodecMethod codecMethod) throws CodecException;

  /**
   * 执行具体的移除逻辑 - 需要子类实现
   *
   * @param productKey 产品Key
   */
  protected abstract void doRemove(String productKey);

  /**
   * 执行具体的编解码逻辑 - 需要子类实现
   *
   * @param productKey 产品Key
   * @param payload 原始数据
   * @param codecMethod 编解码方法
   * @return 编解码结果
   * @throws CodecException 编解码异常
   */
  protected abstract String doExecute(String productKey, String payload, CodecMethod codecMethod)
      throws CodecException;
}
