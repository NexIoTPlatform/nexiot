package cn.universal.common.event;

import cn.hutool.json.JSONUtil;
import cn.universal.common.event.processer.EventPublisher;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 延迟事件处理器 定时扫描并处理延迟事件 */
@Slf4j
@Component
public class DelayEventProcessor {

  @Autowired private StringRedisTemplate redisTemplate;

  @Autowired private EventPublisher eventPublisher;
  private final Object lifecycleMonitor = new Object();
  private volatile boolean shuttingDown;

  /** 每10秒扫描一次延迟事件 */
  @Scheduled(fixedRate = 10000)
  public void processDelayEvents() {
    synchronized (lifecycleMonitor) {
      if (shuttingDown) {
        return;
      }
      processDelayEventsInternal();
    }
  }

  private void processDelayEventsInternal() {
    try {
      // 使用SCAN命令替代KEYS命令，避免Redis环境限制
      Set<String> delayKeys = scanDelayKeys();
      if (delayKeys == null || delayKeys.isEmpty()) {
        return;
      }

      for (String delayKey : delayKeys) {
        String eventData = redisTemplate.opsForValue().get(delayKey);
        if (eventData != null) {
          try {
            EventMessage eventMessage = JSONUtil.toBean(eventData, EventMessage.class);

            // 检查是否到了执行时间
            long currentTime = System.currentTimeMillis();
            long eventTime = eventMessage.getTimestamp() + (eventMessage.getDelaySeconds() * 1000);

            if (currentTime >= eventTime) {
              // 发布事件
              eventPublisher.publishEvent(eventMessage.getEventType(), eventMessage.getData());

              // 删除延迟键
              redisTemplate.delete(delayKey);

              log.debug(
                  "[延迟事件处理] 事件已发布: eventId={}, topic={}",
                  eventMessage.getEventId(),
                  eventMessage.getEventType());
            }
          } catch (Exception e) {
            log.error("[延迟事件处理] 处理延迟事件失败: key={}, data={}", delayKey, eventData, e);
            // 删除无效的延迟键
            redisTemplate.delete(delayKey);
          }
        }
      }
    } catch (Exception e) {
      log.error("[延迟事件处理] 扫描延迟事件失败", e);
    }
  }

  @EventListener(ContextClosedEvent.class)
  public void onContextClosed(ContextClosedEvent event) {
    synchronized (lifecycleMonitor) {
      shuttingDown = true;
    }
  }

  /**
   * 使用SCAN命令扫描延迟事件键
   *
   * @return 延迟事件键集合
   */
  private Set<String> scanDelayKeys() {
    Set<String> delayKeys = new java.util.HashSet<>();
    String pattern = "delay:*";
    long cursor = 0;

    do {
      org.springframework.data.redis.core.ScanOptions options =
          org.springframework.data.redis.core.ScanOptions.scanOptions()
              .match(pattern)
              .count(100)
              .build();

      org.springframework.data.redis.core.Cursor<String> cursorResult = redisTemplate.scan(options);

      while (cursorResult.hasNext()) {
        delayKeys.add(cursorResult.next());
      }

      cursor = cursorResult.getCursorId();
    } while (cursor != 0);

    return delayKeys;
  }
}
