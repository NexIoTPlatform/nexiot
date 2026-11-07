/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 延迟启动工具类
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 */

package cn.universal.common.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 延迟启动工具类 提供轻量级的延迟启动功能，等待所有Web容器启动完成
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Slf4j
@Component
public class DelayedStartupUtil {

  @Autowired private ApplicationContext applicationContext;

  /**
   * 延迟执行任务
   *
   * @param task 要执行的任务
   * @param delaySeconds 延迟秒数
   * @param executorService 执行器
   */
  public void executeWithDelay(Runnable task, long delaySeconds, ExecutorService executorService) {
    CompletableFuture.runAsync(
        () -> {
          try {
            log.info("延迟启动任务，等待{}秒后执行...", delaySeconds);
            Thread.sleep(delaySeconds * 1000);
            log.info("开始执行延迟启动任务...");
            task.run();
            log.info("延迟启动任务执行完成");
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("延迟启动任务被中断", e);
          } catch (Exception e) {
            log.error("延迟启动任务执行失败", e);
          }
        },
        executorService);
  }

  /**
   * 延迟执行任务（使用默认延迟时间）
   *
   * @param task 要执行的任务
   * @param executorService 执行器
   */
  public void executeWithDelay(Runnable task, ExecutorService executorService) {
    executeWithDelay(task, 5, executorService); // 默认延迟5秒
  }

  /**
   * 等待特定Bean初始化完成后再执行任务
   *
   * @param task 要执行的任务
   * @param beanName Bean名称
   * @param maxWaitSeconds 最大等待秒数
   * @param executorService 执行器
   */
  public void executeAfterBeanReady(
      Runnable task, String beanName, int maxWaitSeconds, ExecutorService executorService) {
    CompletableFuture.runAsync(
        () -> {
          try {
            log.info("等待Bean {} 初始化完成...", beanName);

            // 等待Bean初始化完成
            for (int i = 0; i < maxWaitSeconds * 10; i++) { // 每100ms检查一次
              try {
                if (applicationContext.containsBean(beanName)) {
                  Object bean = applicationContext.getBean(beanName);
                  if (bean != null) {
                    log.info("Bean {} 已初始化完成，开始执行任务", beanName);
                    task.run();
                    log.info("任务执行完成");
                    return;
                  }
                }
              } catch (Exception e) {
                // Bean还未初始化完成，继续等待
              }
              Thread.sleep(100);
            }

            log.warn("等待Bean {} 超时，强制执行任务", beanName);
            task.run();

          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("等待Bean初始化被中断", e);
          } catch (Exception e) {
            log.error("等待Bean初始化后执行任务失败", e);
          }
        },
        executorService);
  }

  /**
   * 等待Web容器完全启动后再执行任务
   *
   * @param task 要执行的任务
   * @param executorService 执行器
   */
  public void executeAfterWebReady(Runnable task, ExecutorService executorService) {
    executeWithDelay(
        () -> {
          try {
            // 检查关键Web组件是否已启动
            String[] webBeans = {
              "dispatcherServlet",
              "requestMappingHandlerMapping",
              "requestMappingHandlerAdapter",
              "webMvcConfigurer"
            };

            boolean allReady = true;
            for (String beanName : webBeans) {
              if (!applicationContext.containsBean(beanName)) {
                allReady = false;
                log.debug("Web组件 {} 尚未就绪", beanName);
                break;
              }
            }

            // 额外检查：确保所有Controller都已注册
            try {
              Object handlerMapping = applicationContext.getBean("requestMappingHandlerMapping");
              if (handlerMapping != null) {
                // 通过反射检查HandlerMapping是否已完全初始化
                java.lang.reflect.Method getHandlerMethods =
                    handlerMapping.getClass().getMethod("getHandlerMethods");
                Object handlerMethods = getHandlerMethods.invoke(handlerMapping);
                if (handlerMethods != null) {
                  log.info("Web容器已完全启动，所有Controller已注册，开始执行任务");
                  task.run();
                  return;
                }
              }
            } catch (Exception e) {
              log.debug("检查HandlerMapping状态时出错: {}", e.getMessage());
            }

            if (allReady) {
              log.info("Web容器已完全启动，开始执行任务");
              task.run();
            } else {
              log.warn("Web容器未完全启动，但继续执行任务");
              task.run();
            }
          } catch (Exception e) {
            log.error("检查Web容器状态失败，直接执行任务", e);
            task.run();
          }
        },
        5,
        executorService); // 增加延迟时间到5秒
  }

  /**
   * 等待Web接口完全就绪（包括所有Controller和路由注册完成） 特别适用于需要Webhook回调的场景
   *
   * @param task 要执行的任务
   * @param executorService 执行器
   */
  public void executeAfterWebInterfaceReady(Runnable task, ExecutorService executorService) {
    CompletableFuture.runAsync(
        () -> {
          try {
            log.info("等待Web接口完全就绪...");

            // 等待Web容器启动
            Thread.sleep(3000);

            // 检查关键Web组件
            String[] criticalBeans = {
              "dispatcherServlet", "requestMappingHandlerMapping", "requestMappingHandlerAdapter"
            };

            for (int attempt = 0; attempt < 10; attempt++) {
              boolean allReady = true;
              for (String beanName : criticalBeans) {
                if (!applicationContext.containsBean(beanName)) {
                  allReady = false;
                  break;
                }
              }

              if (allReady) {
                // 额外等待，确保所有Controller都已注册
                Thread.sleep(2000);

                // 检查是否有注册的Handler
                try {
                  Object handlerMapping =
                      applicationContext.getBean("requestMappingHandlerMapping");
                  if (handlerMapping != null) {
                    java.lang.reflect.Method getHandlerMethods =
                        handlerMapping.getClass().getMethod("getHandlerMethods");
                    Object handlerMethods = getHandlerMethods.invoke(handlerMapping);
                    if (handlerMethods != null) {
                      log.info("Web接口已完全就绪，所有Controller已注册，开始执行任务");
                      task.run();
                      return;
                    }
                  }
                } catch (Exception e) {
                  log.debug("检查HandlerMapping时出错: {}", e.getMessage());
                }
              }

              log.debug("Web接口尚未完全就绪，等待中... (第{}次检查)", attempt + 1);
              Thread.sleep(1000);
            }

            log.warn("等待Web接口就绪超时，强制执行任务");
            task.run();

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
}
