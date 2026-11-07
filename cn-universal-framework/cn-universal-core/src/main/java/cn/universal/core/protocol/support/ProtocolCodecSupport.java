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
import cn.universal.core.protocol.request.ProtocolDecodeRequest;
import cn.universal.core.protocol.request.ProtocolEncodeRequest;

/**
 * 编解码接口
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/8/9 19:08
 */
public interface ProtocolCodecSupport {

  default String preDecode(ProtocolDecodeRequest protocolDecodeRequest) throws CodecException {
    return null;
  }

  /**
   * 解码
   *
   * @param decodeRequest 消息内容
   * @return JSON字符串
   */
  String decode(ProtocolDecodeRequest decodeRequest) throws CodecException;

  /**
   * 编码
   *
   * @param encodeRequest 消息内容
   */
  String encode(ProtocolEncodeRequest encodeRequest) throws CodecException;

  /**
   * IoT到第三方数据转换
   *
   * @param encodeRequest 消息内容
   * @return 转换后的数据
   */
  default String iotToYour(ProtocolEncodeRequest encodeRequest) throws CodecException {
    return encode(encodeRequest);
  }

  /**
   * 第三方到IoT数据转换
   *
   * @param decodeRequest 消息内容
   * @return 转换后的数据
   */
  default String yourToIot(ProtocolDecodeRequest decodeRequest) throws CodecException {
    return decode(decodeRequest);
  }

  boolean isLoaded(String provider, CodecMethod codecMethod);

  static enum CodecMethod {
    /** 解码 */
    decode,
    /** 编码 */
    encode,
    /** 预解码 */
    preDecode,
    codecAdd,
    codecDelete,
    codecUpdate,
    codecQuery,
    iotToYour,
    yourToIot,
    codecFunction,
    codecOther
  }
}
