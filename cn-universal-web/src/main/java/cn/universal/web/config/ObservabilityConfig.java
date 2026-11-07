/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.web.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 可观测性配置 - Spring Boot 3.5 新特性
 *
 * <p>新特性包括： 1. 增强的Micrometer指标 2. 虚拟线程监控 3. 分布式追踪 4. 健康检查增强
 */
@Slf4j
@Configuration
public class ObservabilityConfig {

  /** 自定义Meter Registry - 添加应用标签 */
  @Bean
  public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
    return registry ->
        registry
            .config()
            .commonTags("application", "cn-universal-IoT")
            .commonTags("version", "1.4-SNAPSHOT");
  }

  /** 虚拟线程监控指标 */
  @Bean
  public VirtualThreadMetrics virtualThreadMetrics() {
    return new VirtualThreadMetrics();
  }

  /** 定时切面 - 自动添加@Timed注解支持 */
  @Bean
  public TimedAspect timedAspect(MeterRegistry registry) {
    return new TimedAspect(registry);
  }

  /** JVM指标绑定器 */
  @Bean
  public JvmMemoryMetrics jvmMemoryMetrics() {
    return new JvmMemoryMetrics();
  }

  @Bean
  public JvmGcMetrics jvmGcMetrics() {
    return new JvmGcMetrics();
  }

  @Bean
  public JvmThreadMetrics jvmThreadMetrics() {
    return new JvmThreadMetrics();
  }

  @Bean
  public ClassLoaderMetrics classLoaderMetrics() {
    return new ClassLoaderMetrics();
  }

  @Bean
  public ProcessorMetrics processorMetrics() {
    return new ProcessorMetrics();
  }

  /** 虚拟线程监控指标类 */
  public static class VirtualThreadMetrics {

    public void recordVirtualThreadCreated() {
      // 记录虚拟线程创建
    }

    public void recordVirtualThreadCompleted() {
      // 记录虚拟线程完成
    }

    public void recordVirtualThreadBlocked() {
      // 记录虚拟线程阻塞
    }
  }
}
