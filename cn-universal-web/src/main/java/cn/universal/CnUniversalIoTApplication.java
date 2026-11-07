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

package cn.universal;

import cn.universal.cache.config.MultiLevelCacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * platform Universal 平台主应用启动类
 *
 * <p>这是整个IoT平台的主入口，集成了以下核心功能： - Spring Boot 自动配置 - 异步任务处理 - AOP 切面编程支持 - 缓存功能 - 定时任务调度
 *
 * <p>该应用采用微服务架构，整合了设备管理、认证授权、消息队列、 文件存储等核心模块，为IoT设备提供统一的管理平台。
 *
 * @version 1.4-SNAPSHOT @Author gitee.com/NexIoT
 * @since 2025/1/1
 */
@Slf4j
@SpringBootApplication
@EnableAsync(proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties({MultiLevelCacheProperties.class})
public class CnUniversalIoTApplication {

  /**
   * 应用程序主入口方法
   *
   * <p>启动Spring Boot应用，初始化所有配置的组件和服务 包括：Web服务器、数据库连接、缓存、消息队列等
   *
   * @param args 命令行参数
   */
  public static void main(String[] args) {
    log.info("Universal-IoT  平台正在启动...");
    SpringApplication.run(CnUniversalIoTApplication.class, args);
  }
}
