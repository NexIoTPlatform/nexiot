package cn.universal.common.event.processer;

import cn.universal.common.event.EventMessage;

/**
 * UDP下行指令处理器接口
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
public interface UdpDownProcessor {

  /**
   * 处理UDP下行指令事件
   *
   * @param message 事件消息字符串
   */
  void handleUdpDownEvent(EventMessage message);
}
