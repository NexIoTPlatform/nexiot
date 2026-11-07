package cn.universal.dm.device.service.plugin;

public interface MessageProcessor {

  /** 处理器名称 */
  String getName();

  /** 处理器优先级，数字越小优先级越高 */
  int getOrder();
}
