package cn.universal.common.event.processer;

import cn.universal.common.event.EventMessage;

public interface AppPushConfigProcessor {

  void handleAppPushConfigUpdated(EventMessage message);
}
