package cn.universal.common.event;

import cn.hutool.json.JSONUtil;
import cn.universal.common.config.InstanceIdProvider;
import cn.universal.common.event.processer.EventPublisher;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis事件发布器实现
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@Component
public class RedisEventPublisher implements EventPublisher {

  @Autowired private StringRedisTemplate redisTemplate;

  @Autowired private InstanceIdProvider instanceIdProvider;

  @Override
  public void publishEvent(String topic, Object event) {
    try {
      EventMessage eventMessage =
          EventMessage.builder()
              .eventType(topic)
              .data(event)
              .nodeId(instanceIdProvider.getInstanceId())
              .timestamp(System.currentTimeMillis())
              .createTime(LocalDateTime.now())
              .eventId(UUID.randomUUID().toString())
              .build();

      String message = JSONUtil.toJsonStr(eventMessage);
      redisTemplate.convertAndSend(topic, message);

      log.debug("[Redis事件发布] 发布事件到主题: {}, eventId={}", topic, eventMessage.getEventId());
    } catch (Exception e) {
      log.error("[Redis事件发布] 发布事件失败: topic={}, event={}", topic, event, e);
    }
  }

  /**
   * 发布延迟事件
   *
   * @param topic 主题
   * @param event 事件数据
   * @param delaySeconds 延迟秒数
   */
  public void publishEvent(String topic, Object event, long delaySeconds) {
    try {
      EventMessage eventMessage =
          EventMessage.builder()
              .eventType(topic)
              .data(event)
              .nodeId(instanceIdProvider.getInstanceId())
              .timestamp(System.currentTimeMillis())
              .createTime(LocalDateTime.now())
              .delaySeconds(delaySeconds)
              .eventId(UUID.randomUUID().toString())
              .build();

      String message = JSONUtil.toJsonStr(eventMessage);

      if (delaySeconds > 0) {
        // 延迟事件：存储到Redis，由定时任务处理
        String delayKey = "delay:" + eventMessage.getEventId();
        redisTemplate.opsForValue().set(delayKey, message, delaySeconds, TimeUnit.SECONDS);
        log.debug(
            "[Redis事件发布] 延迟事件已存储: topic={}, delaySeconds={}, eventId={}",
            topic,
            delaySeconds,
            eventMessage.getEventId());
      } else {
        // 立即发布
        redisTemplate.convertAndSend(topic, message);
        log.debug("[Redis事件发布] 事件已发布: topic={}, eventId={}", topic, eventMessage.getEventId());
      }
    } catch (Exception e) {
      log.error("[Redis事件发布] 发布事件失败: topic={}, event={}", topic, event, e);
    }
  }

  @Override
  public void publishEvent(String topic, Object event, String excludeNodeId) {
    try {
      EventMessage eventMessage =
          EventMessage.builder()
              .eventType(topic)
              .data(event)
              .nodeId(instanceIdProvider.getInstanceId())
              .timestamp(System.currentTimeMillis())
              .createTime(LocalDateTime.now())
              .eventId(UUID.randomUUID().toString())
              .build();
      // 在消息中标记排除的节点
      eventMessage.setNodeId(excludeNodeId);

      String message = JSONUtil.toJsonStr(eventMessage);
      redisTemplate.convertAndSend(topic, message);

      log.debug(
          "[Redis事件发布] 发布事件(排除节点): topic={}, excludeNode={}, message={}",
          topic,
          excludeNodeId,
          message);
    } catch (Exception e) {
      log.error(
          "[Redis事件发布] 发布事件失败: topic={}, event={}, excludeNode={}", topic, event, excludeNodeId, e);
    }
  }
}
