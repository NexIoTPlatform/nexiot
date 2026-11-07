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

package cn.universal.listener;

import cn.universal.core.protocol.ProtocolModuleInfo;
import cn.universal.core.protocol.ProtocolModuleRuntimeRegistry;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 协议模块启动监听器 在应用完全启动后显示已启用的协议模块信息
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/2
 */
@Slf4j
@Component
public class ProtocolModuleStartupListener implements ApplicationListener<ApplicationReadyEvent> {

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    displayProtocolModules();
  }

  private void displayProtocolModules() {
    Collection<ProtocolModuleInfo> runtimeProtocols =
        ProtocolModuleRuntimeRegistry.getAllRuntimeProtocols();
    ProtocolModuleRuntimeRegistry.RuntimeStatistics stats =
        ProtocolModuleRuntimeRegistry.getStatistics();

    log.info("==================== 协议模块启动信息 ====================");

    if (runtimeProtocols.isEmpty()) {
      log.warn("⚠️  未检测到任何已启用的协议模块!");
      log.info("请检查配置文件中的协议开关设置");
    } else {
      log.info("🚀 协议模块启动统计:");
      log.info("   总计启用: {} 个协议模块", stats.getTotalRunning());
      log.info("   核心协议: {} 个", stats.getCoreCount());
      log.info("   可选协议: {} 个", stats.getOptionalCount());

      // 显示核心协议
      displayCoreProtocols();

      // 显示可选协议
      displayOptionalProtocols();

      // 显示分类统计
      displayCategoryStatistics(stats);
    }

    log.info("======================================================");
  }

  private void displayCoreProtocols() {
    List<ProtocolModuleInfo> coreProtocols = ProtocolModuleRuntimeRegistry.getCoreProtocols();
    if (!coreProtocols.isEmpty()) {
      log.info("🔒 核心协议模块:");
      coreProtocols.forEach(
          protocol ->
              log.info(
                  "   ✅ {} - {} ({})",
                  protocol.getCode().toUpperCase(),
                  protocol.getName(),
                  protocol.getDescription()));
    }
  }

  private void displayOptionalProtocols() {
    List<ProtocolModuleInfo> optionalProtocols =
        ProtocolModuleRuntimeRegistry.getOptionalProtocols();
    if (!optionalProtocols.isEmpty()) {
      log.info("🔧 可选协议模块:");
      optionalProtocols.forEach(
          protocol ->
              log.info(
                  "   ✅ {} - {} ({})",
                  protocol.getCode().toUpperCase(),
                  protocol.getName(),
                  protocol.getDescription()));
    }
  }

  private void displayCategoryStatistics(ProtocolModuleRuntimeRegistry.RuntimeStatistics stats) {
    if (!stats.getCategoryStats().isEmpty()) {
      log.info("📊 分类统计:");
      stats
          .getCategoryStats()
          .forEach((category, count) -> log.info("   {} 类协议: {} 个", category.name(), count));
    }
  }
}
