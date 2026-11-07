package cn.universal.common.event.processer;

import cn.universal.common.event.EventMessage;

public interface DeviceWebSocketPushProcessor {

  void handleDeviceWebSocketPush(EventMessage message);
}
