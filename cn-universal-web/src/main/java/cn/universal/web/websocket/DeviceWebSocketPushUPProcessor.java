package cn.universal.web.websocket;

import cn.universal.dm.device.service.push.UPProcessor;
import cn.universal.persistence.base.BaseUPRequest;
import org.springframework.stereotype.Component;

@Component
public class DeviceWebSocketPushUPProcessor implements UPProcessor<BaseUPRequest> {

  @Override
  public String getName() {
    return "deviceWebSocketPush";
  }

  @Override
  public String getDescription() {
    return "设备实时 WebSocket 推送处理器";
  }

  @Override
  public int getOrder() {
    return 900;
  }

  @Override
  public boolean supports(BaseUPRequest request) {
    return true;
  }
}
