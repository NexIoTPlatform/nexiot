package cn.universal.common.event.processer;

import cn.universal.common.event.EventMessage;

public interface DeviceCacheInvalidationProcessor {

  void handleDeviceCacheInvalidated(EventMessage message);
}
