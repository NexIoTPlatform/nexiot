package cn.universal.dm.device.service.sub.processor;

import cn.universal.dm.device.service.sub.context.SubDeviceRequest;

/**
 * 子设备消息处理器接口
 *
 * @author system
 * @date 2025-01-16
 */
public interface SubDeviceMessageProcessor {

  /**
   * 处理子设备消息
   *
   * @param subDeviceRequest 子设备上下文
   * @return 处理结果
   */
  ProcessorResult process(SubDeviceRequest subDeviceRequest);

  /**
   * 是否支持处理该消息
   *
   * @param subDeviceRequest 子设备上下文
   * @return true表示支持，false表示不支持
   */
  boolean supports(SubDeviceRequest subDeviceRequest);

  /**
   * 处理器名称
   *
   * @return 处理器的可读名称
   */
  String getName();

  /**
   * 执行顺序，数字越小越先执行
   *
   * @return 执行顺序
   */
  int getOrder();

  /** 处理器描述（可选） */
  default String getDescription() {
    return getName();
  }

  /** 处理器优先级（可选，用于细粒度排序） */
  default int getPriority() {
    return 0;
  }

  /** 是否必须执行（可选） */
  default boolean isRequired() {
    return false;
  }

  /** 处理前的预检查（可选） */
  default boolean preCheck(SubDeviceRequest subDeviceRequest) {
    return true;
  }

  /** 处理后的后置操作（可选） */
  default void postProcess(SubDeviceRequest subDeviceRequest, ProcessorResult result) {
    // 默认不做任何操作
  }

  /** 异常处理（可选） */
  default void onError(SubDeviceRequest subDeviceRequest, Exception e) {}

  /** 处理结果枚举 */
  enum ProcessorResult {
    /** 继续处理 - 传递给下一个处理器 */
    CONTINUE,

    /** 停止处理 - 成功完成，不再传递给后续处理器 */
    STOP,

    /** 跳过当前消息 - 忽略该消息，不进行后续处理 */
    SKIP,

    /** 处理失败 - 发生错误，停止处理链 */
    ERROR
  }
}
