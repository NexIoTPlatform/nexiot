package cn.universal.jar.core;

import cn.universal.core.protocol.jar.JarDriverCodecService;
import cn.universal.core.protocol.jar.ProtocolCodecJar;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/** Java驱动，默认支持所有方法 */
@Component
@Slf4j
public class JarDriverCodecInitFactory implements ApplicationContextAware {

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    Map<String, JarDriverCodecService> jarDriverCodecServiceMap =
        applicationContext.getBeansOfType(JarDriverCodecService.class);
    jarDriverCodecServiceMap.forEach(
        (key, value) -> ProtocolCodecJar.getInstance().load(value.productKey(), value));
    log.info("success init JarDriverCodecService => [{}]", jarDriverCodecServiceMap);
  }
}
