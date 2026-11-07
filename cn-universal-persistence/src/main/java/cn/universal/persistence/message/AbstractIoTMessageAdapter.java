package cn.universal.persistence.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 标准消息格式适配器 */
@Component
@Slf4j
public abstract class AbstractIoTMessageAdapter implements IoTMessageAdapter {

  boolean support(String product, String userId, String appUnionId) {
    return false;
  }
}
