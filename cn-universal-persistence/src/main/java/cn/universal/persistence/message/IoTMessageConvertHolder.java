package cn.universal.persistence.message;

import cn.universal.persistence.base.BaseDownRequest;
import cn.universal.persistence.base.BaseUPRequest;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IoTMessageConvertHolder implements ApplicationContextAware {

  private Map<String, IoTMessageAdapter> adapterMap = new HashMap<>();

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    Map<String, IoTMessageAdapter> iotMessageAdapterMap =
        applicationContext.getBeansOfType(IoTMessageAdapter.class);
    log.info("init IoTMessageAdapter third ,[{}]", iotMessageAdapterMap);
    adapterMap.forEach((key, value) -> adapterMap.put(value.name(), value));
  }

  /** 格式化上行消息 */
  public String formatUpMessage(BaseUPRequest upRequest) {
    return null;
  }

  /** 格式化下行消息 */
  public String formatDownMessage(BaseDownRequest downRequest) {
    return null;
  }
}
