package cn.universal.databridge.starter;

import cn.universal.databridge.plugin.DataBridgePlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 数据桥接自动配置类
 *
 * <p>负责自动装配数据桥接相关的组件和插件
 *
 * <p><b>注意：</b>@EnableAsync 已在主启动类中全局启用，此处无需重复配置
 *
 * @author NexIoT
 * @since 1.0
 */
@AutoConfiguration
@ComponentScan(basePackages = {"cn.universal.databridge"})
@ConditionalOnProperty(
    prefix = "databridge",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
public class DataBridgeAutoConfiguration implements ApplicationContextAware {

  private ApplicationContext applicationContext;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Bean(name = "dataBridgeExecutor")
  public Executor dataBridgeExecutor(
      @Value("${databridge.executor.core-size:2}") int coreSize,
      @Value("${databridge.executor.max-size:4}") int maxSize,
      @Value("${databridge.executor.queue-capacity:512}") int queueCapacity) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(Math.max(1, coreSize));
    executor.setMaxPoolSize(Math.max(Math.max(1, coreSize), maxSize));
    executor.setQueueCapacity(Math.max(1, queueCapacity));
    executor.setThreadNamePrefix("databridge-");
    executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(10);
    executor.initialize();
    return executor;
  }

  @Bean
  public Map<String, DataBridgePlugin> bridgePlugins() {
    Map<String, DataBridgePlugin> pluginMap = new HashMap<>();

    try {
      // 获取所有实现了DataBridgePlugin接口的bean
      Map<String, DataBridgePlugin> allPlugins =
          applicationContext.getBeansOfType(DataBridgePlugin.class);

      // 将插件按bean名称放入map
      allPlugins.forEach(
          (beanName, plugin) -> {
            try {
              String pluginType = plugin.getPluginInfo().getPluginType();
              pluginMap.put(pluginType, plugin);
              System.out.println("已注册插件: " + pluginType + " (bean: " + beanName + ")");
            } catch (Exception e) {
              System.err.println("注册插件失败: " + beanName + ", 错误: " + e.getMessage());
            }
          });

      System.out.println("总共注册了 " + pluginMap.size() + " 个插件");

    } catch (Exception e) {
      System.err.println("获取插件失败: " + e.getMessage());
      e.printStackTrace();
    }

    return pluginMap;
  }
}
