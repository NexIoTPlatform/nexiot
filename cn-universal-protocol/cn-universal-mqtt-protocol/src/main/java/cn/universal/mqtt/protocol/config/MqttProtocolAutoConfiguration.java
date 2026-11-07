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

package cn.universal.mqtt.protocol.config;

import cn.universal.core.protocol.ProtocolModuleRuntimeRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT 自动配置类
 *
 * <p>Spring Boot自动配置入口 负责扫描和注册MQTT模块的所有组件
 *
 * <p>MQTT作为系统核心协议，总是启用，不支持禁用
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
@Configuration
@ComponentScan(basePackages = "cn.universal.mqtt.protocol")
public class MqttProtocolAutoConfiguration {

  @Autowired private MqttModuleInfo moduleInfo;

  public MqttProtocolAutoConfiguration() {
    log.info("[CORE_MQTT] MQTT核心模块自动配置已启用");
  }

  @PostConstruct
  public void registerProtocol() {
    if (moduleInfo != null) {
      ProtocolModuleRuntimeRegistry.registerProtocol(moduleInfo);
      log.info("[MQTT自动配置] 核心协议模块已注册到运行时注册表");
    }
  }

  @PreDestroy
  public void unregisterProtocol() {
    if (moduleInfo != null) {
      ProtocolModuleRuntimeRegistry.unregisterProtocol(moduleInfo.getCode());
      log.info("[MQTT自动配置] 核心协议模块已从运行时注册表注销");
    }
  }
}
