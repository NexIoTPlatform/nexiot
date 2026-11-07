package cn.universal.common.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Web接口就绪检查器 专门用于检查Web接口是否完全就绪，特别适用于Webhook场景
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Slf4j
@Component
public class WebInterfaceReadyChecker {

  @Autowired private ApplicationContext applicationContext;

  /**
   * 检查Web接口是否完全就绪
   *
   * @return true如果Web接口已就绪，false否则
   */
  public boolean isWebInterfaceReady() {
    try {
      // 1. 检查关键Web组件
      String[] criticalBeans = {
        "dispatcherServlet", "requestMappingHandlerMapping", "requestMappingHandlerAdapter"
      };

      for (String beanName : criticalBeans) {
        if (!applicationContext.containsBean(beanName)) {
          log.debug("Web组件 {} 尚未就绪", beanName);
          return false;
        }
      }

      // 2. 检查RequestMappingHandlerMapping是否已完全初始化
      // ========== 关键修改：按Bean名称获取，而非类型 ==========
      RequestMappingHandlerMapping handlerMapping =
          applicationContext.getBean(
              "requestMappingHandlerMapping", RequestMappingHandlerMapping.class);

      if (handlerMapping == null) {
        log.debug("RequestMappingHandlerMapping 尚未就绪");
        return false;
      }

      // 3. 检查是否有注册的Handler
      try {
        var handlerMethods = handlerMapping.getHandlerMethods();
        if (handlerMethods == null || handlerMethods.isEmpty()) {
          log.debug("尚未有Handler注册");
          return false;
        }

        // 4. 检查是否有Webhook相关的Controller
        boolean hasWebhookController =
            handlerMethods.entrySet().stream()
                .anyMatch(
                    entry -> {
                      String path = entry.getKey().toString();
                      return path.contains("webhook")
                          || path.contains("callback")
                          || path.contains("hook");
                    });

        if (hasWebhookController) {
          log.info("检测到Webhook相关Controller已注册");
        } else {
          log.debug("未检测到Webhook相关Controller");
        }

        log.info("Web接口已完全就绪，共注册 {} 个Handler", handlerMethods.size());
        return true;

      } catch (Exception e) {
        log.debug("检查HandlerMapping时出错: {}", e.getMessage());
        return false;
      }

    } catch (Exception e) {
      log.debug("检查Web接口状态时出错: {}", e.getMessage());
      return false;
    }
  }

  /**
   * 等待Web接口就绪
   *
   * @param maxWaitSeconds 最大等待秒数
   * @param checkIntervalMs 检查间隔毫秒数
   * @return true如果Web接口在指定时间内就绪，false否则
   */
  public boolean waitForWebInterfaceReady(int maxWaitSeconds, long checkIntervalMs) {
    long startTime = System.currentTimeMillis();
    long maxWaitMs = maxWaitSeconds * 1000L;

    while (System.currentTimeMillis() - startTime < maxWaitMs) {
      if (isWebInterfaceReady()) {
        return true;
      }

      try {
        Thread.sleep(checkIntervalMs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("等待Web接口就绪被中断");
        return false;
      }
    }

    log.warn("等待Web接口就绪超时 ({}秒)", maxWaitSeconds);
    return false;
  }

  /**
   * 异步等待Web接口就绪后执行任务
   *
   * @param task 要执行的任务
   * @param executorService 执行器
   * @param maxWaitSeconds 最大等待秒数
   */
  public void executeAfterWebInterfaceReady(
      Runnable task, ExecutorService executorService, int maxWaitSeconds) {
    CompletableFuture.runAsync(
        () -> {
          try {
            log.info("等待Web接口完全就绪...");

            // 先等待一段时间让Web容器启动
            Thread.sleep(2000);

            // 等待Web接口就绪
            boolean ready = waitForWebInterfaceReady(maxWaitSeconds, 1000);

            if (ready) {
              log.info("Web接口已完全就绪，开始执行任务");
              task.run();
            } else {
              log.warn("Web接口就绪超时，强制执行任务");
              task.run();
            }

          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("等待Web接口就绪被中断", e);
          } catch (Exception e) {
            log.error("等待Web接口就绪失败", e);
            task.run();
          }
        },
        executorService);
  }

  /**
   * 异步等待Web接口就绪后执行任务（使用默认参数）
   *
   * @param task 要执行的任务
   * @param executorService 执行器
   */
  public void executeAfterWebInterfaceReady(Runnable task, ExecutorService executorService) {
    executeAfterWebInterfaceReady(task, executorService, 15); // 默认等待15秒
  }
}
