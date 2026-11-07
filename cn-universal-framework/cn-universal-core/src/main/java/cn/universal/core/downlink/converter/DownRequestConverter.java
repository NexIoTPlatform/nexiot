/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权,未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.core.downlink.converter;

import cn.universal.core.downlink.DownlinkContext;
import cn.universal.core.message.DownRequest;
import cn.universal.core.message.UnifiedDownlinkCommand;

/**
 * 下行请求转换器接口 负责将统一的UnifiedDownlinkCommand转换为协议特定的DownRequest对象
 *
 * <p>设计原则：
 *
 * <ul>
 *   <li>单一职责：每个转换器只负责一种协议的转换
 *   <li>无状态：转换器应该是无状态的，可以被多次复用
 *   <li>向后兼容：转换器应该能处理旧格式的数据
 *   <li>异常处理：转换失败应该抛出明确的异常
 * </ul>
 *
 * <p>实现示例：
 *
 * <pre>{@code
 * @Component("mqttConverter")
 * public class MQTTDownRequestConverter implements DownRequestConverter<MQTTDownRequest> {
 *
 *     @Override
 *     public MQTTDownRequest convert(UnifiedDownlinkCommand command, DownlinkContext<?> context) {
 *         MQTTDownRequest request = new MQTTDownRequest();
 *         // 执行转换逻辑
 *         return request;
 *     }
 *
 *     @Override
 *     public String supportedProtocol() {
 *         return "MQTT";
 *     }
 * }
 * }</pre>
 *
 * <p>注意：泛型参数T使用{@link DownRequest}作为上界，以保持核心模块的独立性。 实际使用时，建议使用{@link
 * cn.universal.persistence.base.BaseDownRequest}的子类。
 *
 * @param <T> 协议特定的DownRequest类型（通常是BaseDownRequest的子类）
 * @version 1.0
 * @since 2025/10/25
 */
public interface DownRequestConverter<T extends DownRequest> {

  /**
   * 转换统一命令为协议特定请求
   *
   * <p>转换过程应该包括：
   *
   * <ol>
   *   <li>复制通用字段（productKey、deviceId等）
   *   <li>设置协议特定字段
   *   <li>执行编解码（如果需要）
   *   <li>加载产品配置
   *   <li>加载设备信息
   * </ol>
   *
   * @param command 统一下行命令对象
   * @param context 下行上下文（可用于存储中间结果）
   * @return 协议特定的下行请求对象
   * @throws IllegalArgumentException 参数验证失败
   * @throws IllegalStateException 转换过程中状态异常
   */
  T convert(UnifiedDownlinkCommand command, DownlinkContext<?> context);

  /**
   * 获取支持的协议代码
   *
   * <p>协议代码应该与{@link cn.universal.core.service.IDown#code()}返回值一致
   *
   * @return 协议代码（如：MQTT、HTTP、TCP、UDP等）
   */
  String supportedProtocol();

  /**
   * 获取转换器优先级
   *
   * <p>当多个转换器支持同一协议时，优先级高的会被优先使用
   *
   * <p>数字越小优先级越高，默认为100
   *
   * @return 优先级（0-最高，Integer.MAX_VALUE-最低）
   */
  default int getPriority() {
    return 100;
  }

  /**
   * 检查是否支持指定的命令
   *
   * <p>默认实现：支持所有命令
   *
   * <p>子类可以重写此方法以实现更细粒度的控制
   *
   * @param command 统一下行命令
   * @return true-支持，false-不支持
   */
  default boolean supports(UnifiedDownlinkCommand command) {
    return true;
  }

  /**
   * 转换前的预处理钩子
   *
   * <p>在执行convert之前调用，可用于：
   *
   * <ul>
   *   <li>参数验证
   *   <li>参数标准化
   *   <li>日志记录
   * </ul>
   *
   * @param command 统一下行命令
   * @param context 下行上下文
   */
  default void preConvert(UnifiedDownlinkCommand command, DownlinkContext<?> context) {
    // 默认空实现
  }

  /**
   * 转换后的后处理钩子
   *
   * <p>在执行convert之后调用，可用于：
   *
   * <ul>
   *   <li>结果验证
   *   <li>数据增强
   *   <li>日志记录
   * </ul>
   *
   * @param command 统一下行命令
   * @param request 转换后的请求对象
   * @param context 下行上下文
   */
  default void postConvert(UnifiedDownlinkCommand command, T request, DownlinkContext<?> context) {
    // 默认空实现
  }
}
