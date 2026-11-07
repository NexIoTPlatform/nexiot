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

package cn.universal.core.service;

import cn.universal.core.message.UPRequest;
import cn.universal.core.protocol.support.ProtocolCodecSupport.CodecMethod;
import java.util.List;

/**
 * 编解码服务接口
 *
 * <p>定义统一的编解码方法，支持面向接口编程
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/01/20
 */
public interface ICodecService {

  /**
   * 解码 - 最完善的方法，支持所有协议类型
   *
   * @param productKey 产品Key
   * @param payload 原始数据
   * @param context 上下文对象
   * @param elementType 目标类型
   * @param <R> 泛型类型
   * @return 解码后的对象列表
   */
  <R> List<R> decode(String productKey, String payload, Object context, Class<R> elementType);

  /**
   * 解码 - 简化版本
   *
   * @param productKey 产品Key
   * @param payload 原始数据
   * @param elementType 目标类型
   * @param <R> 泛型类型
   * @return 解码后的对象列表
   */
  <R> List<R> decode(String productKey, String payload, Class<R> elementType);

  /**
   * 解码为UPRequest列表
   *
   * @param productKey 产品Key
   * @param payload 原始数据
   * @return UPRequest列表
   */
  List<UPRequest> decode(String productKey, String payload);

  /**
   * 编码
   *
   * @param productKey 产品Key
   * @param payload 原始数据
   * @return 编码后的字符串
   */
  String encode(String productKey, String payload);

  /**
   * 编码
   *
   * @param productKey 产品Key
   * @param payload 原始数据
   * @return 编码后的字符串
   */
  String encode(String productKey, String payload, Object context);

  /**
   * 预解码
   *
   * @param productKey 产品Key
   * @param payload 原始数据
   * @return 预解码后的UPRequest
   */
  UPRequest preDecode(String productKey, String payload, Object context);

  /**
   * 通用编解码方法 - 支持所有CodecMethod类型
   *
   * @param productKey 产品Key
   * @param payload 原始数据
   * @param codecMethod 编解码方法类型
   * @return 编解码结果
   */
  String codec(String productKey, String payload, CodecMethod codecMethod);

  /**
   * IoT到第三方数据转换编解码
   *
   * @param productKey 产品Key
   * @param payload 原始数据
   * @return 转换后的数据
   */
  String iotToYour(String productKey, String payload);

  /**
   * IoT到第三方数据转换编解码（带运行时上下文）
   *
   * @param productKey 产品Key
   * @param payload 原始数据
   * @param context 运行时上下文
   * @return 转换后的数据
   */
  default String iotToYour(String productKey, String payload, Object context) {
    return iotToYour(productKey, payload);
  }

  /**
   * 第三方到IoT数据转换编解码
   *
   * @param productKey 产品Key
   * @param payload 原始数据
   * @return 转换后的数据
   */
  String yourToIot(String productKey, String payload);

  /**
   * 第三方到IoT数据转换编解码（带运行时上下文）
   *
   * @param productKey 产品Key
   * @param payload 原始数据
   * @param context 运行时上下文
   * @return 转换后的数据
   */
  default String yourToIot(String productKey, String payload, Object context) {
    return yourToIot(productKey, payload);
  }

  /**
   * 检查是否支持指定的编解码方法
   *
   * @param productKey 产品Key
   * @param codecMethod 编解码方法类型
   * @return 是否支持
   */
  boolean isSupported(String productKey, CodecMethod codecMethod);
}
