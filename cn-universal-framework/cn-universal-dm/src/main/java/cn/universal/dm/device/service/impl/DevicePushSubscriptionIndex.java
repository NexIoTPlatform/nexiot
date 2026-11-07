package cn.universal.dm.device.service.impl;

import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.dm.cache.IoTCacheInvalidationEvent;
import cn.universal.persistence.entity.IoTDeviceSubscribe;
import cn.universal.persistence.mapper.IoTDeviceSubscribeMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

/** Product/device push subscriptions are loaded together, including empty results. */
@Component
public class DevicePushSubscriptionIndex implements AutoCloseable {
  private record Key(String product, String device, String type) {}

  private record Snapshot(Map<Key, List<IoTDeviceSubscribe>> routes, long validUntil) {}

  private volatile Snapshot snapshot = new Snapshot(Map.of(), 0);
  private final IoTDeviceSubscribeMapper mapper;
  private final MeterRegistry metrics;
  private final ScheduledExecutorService control =
      Executors.newSingleThreadScheduledExecutor(
          Thread.ofVirtual().name("device-push-subscriptions").factory());
  private final AtomicBoolean queued = new AtomicBoolean();
  private long warningAt;

  public DevicePushSubscriptionIndex(IoTDeviceSubscribeMapper mapper, MeterRegistry metrics) {
    this.mapper = mapper;
    this.metrics = metrics;
  }

  @PostConstruct
  public void start() {
    refresh();
    control.scheduleWithFixedDelay(this::refresh, 5, 5, TimeUnit.SECONDS);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void changed(IoTCacheInvalidationEvent event) {
    if (event.getCacheNames().contains("iot_dev_subscribe")
        && !control.isShutdown()
        && queued.compareAndSet(false, true))
      control.execute(
          () -> {
            queued.set(false);
            refresh();
          });
  }

  public List<IoTDeviceSubscribe> find(String product, String device, MessageType type) {
    Snapshot current = snapshot;
    if (product == null || type == null || System.currentTimeMillis() > current.validUntil())
      return List.of();
    List<IoTDeviceSubscribe> result = new ArrayList<>();
    for (String message : type == MessageType.ALL ? List.of("ALL") : List.of(type.name(), "ALL")) {
      result.addAll(current.routes().getOrDefault(new Key(product, "", message), List.of()));
      if (device != null && !device.isBlank())
        result.addAll(
            current
                .routes()
                .getOrDefault(
                    new Key(product, device.toLowerCase(Locale.ROOT), message), List.of()));
    }
    return List.copyOf(result);
  }

  void refresh() {
    try {
      Map<Key, List<IoTDeviceSubscribe>> next = new HashMap<>();
      for (var sub :
          mapper.select(
              IoTDeviceSubscribe.builder().enabled(IoTDeviceSubscribe.ENABLED_YES).build())) {
        if (sub.getProductKey() == null || sub.getMsgType() == null) continue;
        String device;
        if ("PRODUCT".equalsIgnoreCase(sub.getSubType())) device = "";
        else if ("DEVICE".equalsIgnoreCase(sub.getSubType())
            && sub.getIotId() != null
            && !sub.getIotId().isBlank()) device = sub.getIotId().toLowerCase(Locale.ROOT);
        else continue;
        next.computeIfAbsent(
                new Key(sub.getProductKey(), device, sub.getMsgType().toUpperCase(Locale.ROOT)),
                k -> new ArrayList<>())
            .add(sub);
      }
      next.replaceAll((key, values) -> List.copyOf(values));
      snapshot = new Snapshot(Map.copyOf(next), System.currentTimeMillis() + 30_000);
      metrics.counter("iot.push.subscription.refresh", "result", "success").increment();
    } catch (RuntimeException error) {
      metrics.counter("iot.push.subscription.refresh", "result", "failure").increment();
      if (System.currentTimeMillis() >= warningAt) {
        warningAt = System.currentTimeMillis() + 30_000;
        org.slf4j.LoggerFactory.getLogger(getClass()).warn("推送订阅索引刷新失败，旧快照最多保留 30 秒", error);
      }
    }
  }

  @PreDestroy
  public void close() {
    control.shutdownNow();
  }
}
