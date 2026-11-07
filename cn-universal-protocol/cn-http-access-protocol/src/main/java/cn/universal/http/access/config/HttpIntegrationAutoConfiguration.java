package cn.universal.http.access.config;

import cn.universal.core.protocol.ProtocolModuleRuntimeRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@AutoConfiguration
@ComponentScan("cn.universal.http.access")
@EnableConfigurationProperties(HttpAccessProperties.class)
@ConditionalOnProperty(
    prefix = "nexiot.http-access",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class HttpIntegrationAutoConfiguration {

  private final HttpModuleInfo moduleInfo;

  public HttpIntegrationAutoConfiguration(HttpModuleInfo moduleInfo) {
    this.moduleInfo = moduleInfo;
  }

  @Bean("httpAccessWorkerExecutor")
  public ThreadPoolTaskExecutor httpAccessWorkerExecutor(HttpAccessProperties properties) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(properties.getWorkerThreads());
    executor.setMaxPoolSize(properties.getWorkerThreads());
    executor.setQueueCapacity(properties.getWorkerQueueCapacity());
    executor.setThreadNamePrefix("http-access-worker-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(20);
    executor.initialize();
    return executor;
  }

  @Bean("httpAccessTaskScheduler")
  public ThreadPoolTaskScheduler httpAccessTaskScheduler(HttpAccessProperties properties) {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(properties.getSchedulerThreads());
    scheduler.setThreadNamePrefix("http-access-scheduler-");
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(20);
    scheduler.initialize();
    return scheduler;
  }

  @PostConstruct
  public void register() {
    ProtocolModuleRuntimeRegistry.registerProtocol(moduleInfo);
  }

  @PreDestroy
  public void unregister() {
    ProtocolModuleRuntimeRegistry.unregisterProtocol(moduleInfo.getCode());
  }
}
