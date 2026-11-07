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

package cn.universal.common.utils;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Payload 编解码工具类
 *
 * <p>用于协议层的字节数组与字符串之间的转换，支持 HEX 和 STRING 两种编码方式
 *
 * <p>适用于 TCP/MQTT/UDP 等协议的 payload 编解码
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/12/05
 */
public class PayloadCodecUtils {

  private static final Map<String, PayloadCodec> codecs = new HashMap<>();

  static {
    register(new HexCodec());
    register(new StringCodec());
  }

  /**
   * 注册编解码器
   *
   * @param codec 编解码器实例
   */
  public static void register(PayloadCodec codec) {
    codecs.put(codec.getType(), codec);
  }

  /**
   * 获取编解码器
   *
   * @param codecType 编解码类型（HEX/STRING）
   * @return 编解码器实例
   */
  public static Optional<PayloadCodec> getCodec(String codecType) {
    return Optional.ofNullable(codecs.get(codecType));
  }

  /**
   * 解码：byte[] -> String
   *
   * @param codecType 编解码类型（HEX/STRING）
   * @param bytes 字节数组
   * @return 解码后的字符串
   */
  public static String decode(String codecType, byte[] bytes) {
    if (codecType == null || bytes == null) {
      return null;
    }
    return getCodec(codecType).map(codec -> codec.decode(bytes)).orElse(null);
  }

  /**
   * 编码：String -> byte[]
   *
   * @param codecType 编解码类型（HEX/STRING）
   * @param payload 字符串
   * @return 编码后的字节数组
   */
  public static byte[] encode(String codecType, String payload) {
    if (codecType == null) {
      // 默认使用 STRING 编码
      return getCodec(CodecType.STRING.name()).map(codec -> codec.encode(payload)).orElse(null);
    }
    return getCodec(codecType).map(codec -> codec.encode(payload)).orElse(null);
  }

  /** Payload 编解码器接口 */
  public interface PayloadCodec {

    /** 获取编解码类型标识 */
    String getType();

    /** 解码：byte[] -> String */
    String decode(byte[] bytes);

    /** 编码：String -> byte[] */
    byte[] encode(String payload);
  }

  /** HEX 编解码器 */
  static class HexCodec implements PayloadCodec {

    @Override
    public String getType() {
      return CodecType.HEX.name();
    }

    @Override
    public String decode(byte[] bytes) {
      return HexUtil.encodeHexStr(bytes);
    }

    @Override
    public byte[] encode(String payload) {
      return HexUtil.decodeHex(payload);
    }
  }

  /** STRING 编解码器（UTF-8） */
  static class StringCodec implements PayloadCodec {

    @Override
    public String getType() {
      return CodecType.STRING.name();
    }

    @Override
    public String decode(byte[] bytes) {
      return StrUtil.str(bytes, CharsetUtil.UTF_8);
    }

    @Override
    public byte[] encode(String payload) {
      try {
        return StrUtil.isNotBlank(payload) ? payload.getBytes(CharsetUtil.UTF_8) : null;
      } catch (Exception e) {
      }
      return null;
    }
  }

  /** 编解码类型枚举 */
  public enum CodecType {
    /** HEX 16进制编码 */
    HEX,
    /** STRING UTF-8字符串编码 */
    STRING
  }
}
