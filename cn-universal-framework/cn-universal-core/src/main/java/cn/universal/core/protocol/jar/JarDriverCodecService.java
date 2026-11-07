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

package cn.universal.core.protocol.jar;

import cn.universal.common.exception.CodecException;

/**
 * JAR 编解码服务统一接口
 *
 * <p>定义标准的编解码方法签名，统一使用两个参数：payload, context
 *
 * <p>productKey 通过 productKey() 方法获取，不需要作为参数传递
 *
 * <p>对于本地 Spring Bean：
 *
 * <ul>
 *   <li>可以实现此接口，然后直接类型转换调用，性能最优
 *   <li>例如：if (codecInstance instanceof JarDriverCodecService) { ((JarDriverCodecService)
 *       codecInstance).decode(payload, context); }
 * </ul>
 *
 * <p>对于上传的 JAR 包：
 *
 * <ul>
 *   <li>如果实现了此接口，可以调用 productKey() 方法获取 productKey
 *   <li>如果未实现接口，但方法签名是三个参数的（向后兼容），仍然可以通过反射调用
 *   <li>反射不依赖接口类型，只要方法签名匹配就能正常调用
 * </ul>
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/11/02
 */
public interface JarDriverCodecService {

  /**
   * 产品标识
   *
   * @return 产品标识
   */
  String productKey();

  /** 添加 */
  default String codecAdd(String payload, Object context) throws CodecException {
    return null;
  }

  /** 删除 */
  default String codecDelete(String payload, Object context) throws CodecException {
    return null;
  }

  /** 更新 */
  default String codecUpdate(String payload, Object context) throws CodecException {
    return null;
  }

  /** 查询 */
  default String codecQuery(String payload, Object context) throws CodecException {
    return null;
  }

  /** 功能调用 */
  default String codecFunction(String payload, Object context) throws CodecException {
    return null;
  }

  /** 其他调用 */
  default String codecOther(String payload, Object context) throws CodecException {
    return null;
  }

  /** 预解码，解决各种设备标识的处理，必须实现 */
  String preDecode(String payload, Object context);

  /** IoT->设备，必须实现 */
  String encode(String payload, Object context);

  /** 设备-IoT，必须实现 */
  String decode(String payload, Object context);
}
