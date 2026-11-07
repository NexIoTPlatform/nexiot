package cn.universal.dm.device.service;

import cn.universal.core.event.*;
import cn.universal.persistence.base.BaseUPRequest;
import java.util.*;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Publishes before legacy wrappers and application filters, using already resolved device data. */
@Component
@Slf4j
public class DeviceUplinkPublisher {
  private final List<DeviceUplinkSink> sinks;
  private final io.micrometer.core.instrument.Counter failures;
  private final java.util.concurrent.atomic.AtomicLong nextWarning =
      new java.util.concurrent.atomic.AtomicLong();

  public DeviceUplinkPublisher(
      List<DeviceUplinkSink> sinks, io.micrometer.core.instrument.MeterRegistry metrics) {
    this.sinks = List.copyOf(sinks);
    failures = metrics.counter("iot.uplink.flow.publish.failure");
  }

  public void publish(List<? extends BaseUPRequest> requests) {
    if (sinks.isEmpty() || requests == null) return;
    for (BaseUPRequest request : requests) {
      if (request == null || request.getIoTDeviceDTO() == null || request.getMessageType() == null)
        continue;
      var device = request.getIoTDeviceDTO();
      // Routing identity comes exclusively from the resolved device, never protocol payload fields.
      String tenant = present(device.getTenantId(), device.getUserUnionId());
      if (tenant.isBlank() || blank(device.getProductKey()) || blank(device.getDeviceId()))
        continue;
      var identity =
          new DeviceUplinkEvent.Identity(
              tenant,
              device.getIotId(),
              device.getProductKey(),
              device.getDeviceId(),
              device.getProductName(),
              device.getDeviceName(),
              device.getApplicationId(),
              device.getAppId(),
              device.getDevGroupId(),
              request.getMessageType().getValue(),
              request.getEvent());
      Supplier<DeviceUplinkEvent> snapshot =
          new Supplier<>() {
            private DeviceUplinkEvent value;

            public DeviceUplinkEvent get() {
              if (value == null) value = snapshot(identity, request);
              return value;
            }
          };
      for (var sink : sinks) {
        try {
          sink.accept(identity, snapshot);
        } catch (RuntimeException error) {
          failures.increment();
          // Durable alarm inputs must not be silently acknowledged when their inbox write fails.
          if (sink.durableAcceptance()) throw error;
          long now = System.currentTimeMillis(), previous = nextWarning.get();
          if (now >= previous && nextWarning.compareAndSet(previous, now + 30_000))
            log.warn(
                "设备事件接收失败（日志限频 30 秒）: productKey={}, deviceId={}, reason={}",
                identity.productKey(),
                identity.deviceId(),
                error.toString());
        }
      }
    }
  }

  private DeviceUplinkEvent snapshot(DeviceUplinkEvent.Identity identity, BaseUPRequest request) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("schemaVersion", 1);
    data.put("eventId", UUID.randomUUID().toString());
    if (request.getData() instanceof Map<?,?> eventData) {
      Object source = eventData.get("sourceEventId");
      if (source == null) source = eventData.get("eventId");
      if (source instanceof String value && !value.isBlank() && value.length() <= 200)
        data.put("sourceEventId", value);
      if (eventData.get("receiptId") instanceof String receipt && receipt.length() <= 200)
        data.put("receiptId", receipt);
    }
    data.put("tenantId", identity.tenantId());
    data.put("iotId", identity.iotId());
    data.put("productKey", identity.productKey());
    data.put("deviceId", identity.deviceId());
    data.put("productName", present(identity.productName(), identity.productKey()));
    data.put("deviceName", present(identity.deviceName(), identity.deviceId()));
    data.put("applicationId", identity.applicationId());
    data.put("appId", identity.appId());
    data.put("groupIds", identity.groupIds());
    data.put("messageType", identity.messageType());
    data.put("event", identity.event());
    data.put("eventName", request.getEventName());
    data.put("properties", request.getProperties());
    data.put("data", request.getData());
    data.put("rawData", request.getRawData());
    data.put("tags", request.getTags());
    data.put("function", request.getFunction());
    data.put("functionName", request.getFunctionName());
    data.put("replyPayload", request.getReplyPayload());
    data.put("requestId", request.getRequestId());
    data.put("time", request.getTime());
    data.put("receivedAt", System.currentTimeMillis());
    return DeviceUplinkEvent.snapshot(identity, data);
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static String present(String value, String fallback) {
    return blank(value) ? Objects.toString(fallback, "") : value;
  }
}
