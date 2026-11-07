package cn.universal.common.event.processer;

/**
 * 事件发布器接口
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
public interface EventPublisher {

  /**
   * 发布事件
   *
   * @param topic 事件主题
   * @param event 事件对象
   */
  void publishEvent(String topic, Object event);

  /**
   * 发布延迟事件
   *
   * @param topic 事件主题
   * @param event 事件对象
   * @param delaySeconds 延迟秒数
   */
  void publishEvent(String topic, Object event, long delaySeconds);

  /**
   * 发布事件（带节点过滤）
   *
   * @param topic 事件主题
   * @param event 事件对象
   * @param excludeNodeId 排除的节点ID
   */
  void publishEvent(String topic, Object event, String excludeNodeId);
}
