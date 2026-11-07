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

package cn.universal.core.protocol;

/**
 * 协议模块信息接口
 *
 * <p>定义协议模块的基本元数据信息，包括名称、代码、描述等
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/2
 */
public interface ProtocolModuleInfo {

  /**
   * 获取协议代码
   *
   * @return 协议代码 (如: mqtt, tcp, http, ctwing, onenet, imoulife)
   */
  String getCode();

  /**
   * 获取协议名称
   *
   * @return 协议名称 (如: MQTT, TCP, HTTP, CT-AIoT (电信), OneNet (移动), ImouLife (乐橙))
   */
  String getName();

  /**
   * 获取协议描述
   *
   * @return 协议描述
   */
  String getDescription();

  /**
   * 获取协议版本
   *
   * @return 协议版本
   */
  default String getVersion() {
    return "1.0";
  }

  /**
   * 获取协议厂商
   *
   * @return 协议厂商
   */
  default String getVendor() {
    return "Universal IoT";
  }

  /**
   * 是否为核心协议（系统必须依赖）
   *
   * @return true-核心协议，false-可选协议
   */
  default boolean isCore() {
    return false;
  }

  /**
   * 是否支持手动创建产品
   *
   * <p>对于视频平台类协议（WVP、海康ISC、大华ICC），产品采用懒创建策略， 不允许用户手动创建产品，应返回false。 其他协议默认支持手动创建。
   *
   * @return true-支持手动创建，false-不支持手动创建
   */
  default boolean isManualCreatable() {
    return true;
  }

  /**
   * 获取不可手动创建的原因描述
   *
   * <p>当isManualCreatable()返回false时，此方法应返回明确的原因说明
   *
   * @return 不可创建的原因描述
   */
  default String getNotCreatableReason() {
    return "此协议不支持手动创建产品";
  }

  /**
   * 获取协议分类
   *
   * @return 协议分类 (如: MESSAGING, TRANSPORT, PLATFORM)
   */
  default ProtocolCategory getCategory() {
    return ProtocolCategory.TRANSPORT;
  }

  /** 协议分类枚举 */
  enum ProtocolCategory {
    /** 消息传输协议 */
    MESSAGING("消息传输"),
    /** 传输层协议 */
    TRANSPORT("传输层"),
    /** 第三方平台协议 */
    PLATFORM("第三方平台"),
    /** 应用层协议 */
    APPLICATION("应用层");

    private final String description;

    ProtocolCategory(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }
}
