package cn.universal.mqtt.protocol.system;

import cn.universal.persistence.base.IoTUPWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("mqttUPWrapper")
@Slf4j(topic = "mqtt")
public class SysMQTTUPWrapper implements IoTUPWrapper {

  @Resource private SysMQTTManager sysMQTTManager;

  @Override
  public void mqttPush(String topic, String msg) {
    log.warn("mqttPush topic:{} msg:{}", topic, msg);
  }
}
