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

package cn.universal.core.protocol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 协议模块注册器
 *
 * <p>统一管理所有协议模块的元数据信息和状态
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/2
 */
@Component
@Slf4j
public class ProtocolModuleRegistry implements ApplicationContextAware {

  private static final Map<String, ProtocolModuleInfo> moduleInfoMap = new HashMap<>();

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    // 扫描所有实现了 ProtocolModuleInfo 接口的Bean
    Map<String, ProtocolModuleInfo> moduleInfoBeans =
        applicationContext.getBeansOfType(ProtocolModuleInfo.class);

    moduleInfoBeans.forEach(
        (beanName, moduleInfo) -> {
          String code = moduleInfo.getCode();
          moduleInfoMap.put(code, moduleInfo);
          log.info("[协议模块注册] 注册协议模块: {} -> {}", code, moduleInfo.getName());
        });

    log.info("[协议模块注册] 完成协议模块注册，共注册 {} 个模块: {}", moduleInfoMap.size(), moduleInfoMap.keySet());
  }

  /**
   * 获取协议模块信息
   *
   * @param code 协议代码
   * @return 协议模块信息，如果不存在返回null
   */
  public static ProtocolModuleInfo getModuleInfo(String code) {
    return moduleInfoMap.get(code);
  }

  /**
   * 获取所有已注册的协议模块信息
   *
   * @return 协议模块信息映射
   */
  public static Map<String, ProtocolModuleInfo> getAllModuleInfo() {
    return new HashMap<>(moduleInfoMap);
  }

  /**
   * 获取所有已注册的协议代码
   *
   * @return 协议代码集合
   */
  public static Set<String> getAllProtocolCodes() {
    return moduleInfoMap.keySet();
  }

  /**
   * 获取核心协议模块
   *
   * @return 核心协议模块列表
   */
  public static List<ProtocolModuleInfo> getCoreModules() {
    return moduleInfoMap.values().stream()
        .filter(ProtocolModuleInfo::isCore)
        .collect(Collectors.toList());
  }

  /**
   * 获取可选协议模块
   *
   * @return 可选协议模块列表
   */
  public static List<ProtocolModuleInfo> getOptionalModules() {
    return moduleInfoMap.values().stream()
        .filter(info -> !info.isCore())
        .collect(Collectors.toList());
  }

  /**
   * 按分类获取协议模块
   *
   * @param category 协议分类
   * @return 指定分类的协议模块列表
   */
  public static List<ProtocolModuleInfo> getModulesByCategory(
      ProtocolModuleInfo.ProtocolCategory category) {
    return moduleInfoMap.values().stream()
        .filter(info -> info.getCategory() == category)
        .collect(Collectors.toList());
  }

  /**
   * 检查协议模块是否已注册
   *
   * @param code 协议代码
   * @return true-已注册，false-未注册
   */
  public static boolean isRegistered(String code) {
    return moduleInfoMap.containsKey(code);
  }
}
