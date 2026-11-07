package cn.universal.monitor;

import static org.assertj.core.api.Assertions.assertThat;

import cn.universal.dm.device.service.push.processor.PushRetryProcessor;
import java.security.SecureRandom;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

class CacheMonitorControllerTest {

  @Test
  void reportsDisabledWhenPushRetryBeanIsNotRegistered() {
    StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
    CacheMonitorController controller =
        new CacheMonitorController(
            null, null, beanFactory.getBeanProvider(PushRetryProcessor.class));

    Map<String, Object> status = controller.getPushRetryThreadPoolStatus();

    assertThat(status)
        .containsEntry("success", true)
        .containsEntry("enabled", false)
        .containsEntry("threadPoolStatus", "Push retry is disabled");
  }

  @org.junit.Test
   void txx(){
    SecureRandom random = new SecureRandom();
    String code = String.format(java.util.Locale.ROOT, "%06d", random.nextInt(100_000_000));
    System.out.println(code);
  }
  public static void main(String[] args){
    SecureRandom random = new SecureRandom();
    String code = String.format(java.util.Locale.ROOT, "%06d", random.nextInt(1_000_000));
    System.out.println(code);
  }
}
