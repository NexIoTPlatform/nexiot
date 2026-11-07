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

package cn.universal.core.service;

import cn.universal.common.constant.IoTConstant;
import cn.universal.common.domain.R;
import cn.universal.common.exception.IoTException;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.protocol.ProtocolModuleInfo;
import cn.universal.core.protocol.ProtocolModuleRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 多实现路由
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/8/12 19:11
 */
@Component
@Slf4j
public class IoTDownlFactory implements ApplicationContextAware {

  private static final Map<String, IDown> iDownMap = new ConcurrentHashMap<>();
  private static final Map<String, IUP> iupMap = new ConcurrentHashMap<>();

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    refresh(applicationContext);
  }

  /** 重新从 Spring 容器刷新协议服务。用于自动配置或测试场景的显式校验。 */
  public static void refresh(ApplicationContext applicationContext) {
    if (applicationContext == null) {
      return;
    }
    iDownMap.clear();
    iupMap.clear();
    // 处理下行
    Map<String, IDown> downMap = applicationContext.getBeansOfType(IDown.class);
    downMap.forEach((key, value) -> registerDown(value));
    log.info("[CORE][服务工厂] 初始化IDown服务: {}", iDownMap.keySet());
    // 处理上行
    Map<String, IUP> upMap = applicationContext.getBeansOfType(IUP.class);
    upMap.forEach((key, value) -> registerUp(value));
    log.info("[CORE][服务工厂] 初始化IUP服务: {}", iupMap.keySet());
  }

  /** 显式注册下行服务，给模块化自动配置提供一个不依赖初始化顺序的入口。 */
  public static void registerDown(IDown service) {
    if (service == null || service.code() == null || service.code().isBlank()) {
      return;
    }
    iDownMap.put(service.code(), service);
  }

  /** 显式注册上行服务，给模块化自动配置提供一个不依赖初始化顺序的入口。 */
  public static void registerUp(IUP service) {
    if (service == null || service.name() == null || service.name().isBlank()) {
      return;
    }
    iupMap.put(service.name(), service);
  }

  public static <T extends IDown> T getIDown(String code) {
    //    T service = (T) iDownMap.get(code);
    //    if (service == null) {
    //      log.warn("[CORE][服务工厂] 未找到下行服务: {}, 可用服务: {}", code, iDownMap.keySet());
    //    }
    //    return service;
    return getIDownOrThrow(code);
  }

  public static <T extends IUP> T getIUP(String code) {
    T service = (T) iupMap.get(code);
    if (service == null) {
      log.warn("[CORE][服务工厂] 未找到上行服务: {}, 可用服务: {}", code, iupMap.keySet());
    }
    return service;
  }

  /** 获取下行服务，如果不存在则抛出BizException */
  public static <T extends IDown> T getIDownOrThrow(String code) {
    T service = (T) iDownMap.get(code);
    if (service == null) {
      log.error("[CORE][服务工厂] 未找到下行服务: {}, 可用服务: {}", code, iDownMap.keySet());
      throw new IoTException(String.format("协议模块 [%s] 未启用或不可用，可用协议: %s", code, iDownMap.keySet()));
    }
    return service;
  }

  /** 获取上行服务，如果不存在则抛出BizException */
  public static <T extends IUP> T getIUPOrThrow(String code) {
    T service = (T) iupMap.get(code);
    if (service == null) {
      log.error("[CORE][服务工厂] 未找到上行服务: {}, 可用服务: {}", code, iupMap.keySet());
      throw new IoTException(String.format("协议模块 [%s] 未启用或不可用，可用协议: %s", code, iupMap.keySet()));
    }
    return service;
  }

  /** 获取已启用的下行服务列表 */
  public static Set<String> getEnabledDownServices() {
    return iDownMap.keySet();
  }

  /** 获取已启用的上行服务列表 */
  public static Set<String> getEnabledUpServices() {
    return iupMap.keySet();
  }

  /** 检查服务是否可用 */
  public static boolean isServiceAvailable(String code) {
    return iDownMap.containsKey(code) || iupMap.containsKey(code);
  }

  /** 安全调用下行服务，捕获异常并返回错误响应 */
  public static R safeInvokeDown(String code, String operation, String msg) {
    IDown service = getIDownOrThrow(code); // 使用抛出异常的版本
    try {
      switch (operation) {
        case "down":
          // 将String转换为UnifiedDownlinkCommand
          UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromString(msg);
          return service.doAction(command);
        case IoTConstant.DOWN_TO_THIRD_PLATFORM:
          return service.downToThirdPlatform(msg);
        default:
          return R.error("不支持的操作: " + operation);
      }
    } catch (IoTException e) {
      // 业务异常直接返回错误信息
      return R.error(e.getMessage());
    } catch (Exception e) {
      log.error(
          "[CORE][服务工厂] 调用下行服务异常: code={}, operation={}, error={}",
          code,
          operation,
          e.getMessage());
      return R.error("服务调用异常: " + e.getMessage());
    }
  }

  /**
   * 获取协议模块详细信息
   *
   * @param code 协议代码
   * @return 协议模块详细信息，包含元数据和运行状态
   */
  public static Map<String, Object> getProtocolModuleDetail(String code) {
    Map<String, Object> detail = new HashMap<>();

    // 获取协议元数据
    ProtocolModuleInfo moduleInfo = ProtocolModuleRegistry.getModuleInfo(code);
    if (moduleInfo != null) {
      detail.put("code", moduleInfo.getCode());
      detail.put("name", moduleInfo.getName());
      detail.put("description", moduleInfo.getDescription());
      detail.put("version", moduleInfo.getVersion());
      detail.put("vendor", moduleInfo.getVendor());
      detail.put("isCore", moduleInfo.isCore());
      detail.put("category", moduleInfo.getCategory().name());
      detail.put("categoryDescription", moduleInfo.getCategory().getDescription());
    } else {
      // 如果没有注册元数据，使用默认信息
      detail.put("code", code);
      detail.put("name", code.toUpperCase());
      detail.put("description", "未知协议");
      detail.put("version", "unknown");
      detail.put("vendor", "unknown");
      detail.put("isCore", false);
      detail.put("category", "UNKNOWN");
      detail.put("categoryDescription", "未知");
    }

    // 获取运行状态
    boolean downAvailable = iDownMap.containsKey(code);
    boolean upAvailable = iupMap.containsKey(code);
    boolean available = downAvailable || upAvailable;

    detail.put("available", available);
    detail.put("downAvailable", downAvailable);
    detail.put("upAvailable", upAvailable);
    detail.put("status", available ? "已启用" : "未启用");

    return detail;
  }

  /**
   * 获取所有协议模块的详细信息
   *
   * @return 所有协议模块的详细信息映射
   */
  public static Map<String, Map<String, Object>> getAllProtocolModuleDetails() {
    Map<String, Map<String, Object>> allDetails = new HashMap<>();

    // 获取所有已注册的协议代码
    Set<String> allCodes = ProtocolModuleRegistry.getAllProtocolCodes();

    // 添加运行时发现的协议（可能没有注册元数据）
    allCodes.addAll(iDownMap.keySet());
    allCodes.addAll(iupMap.keySet());

    for (String code : allCodes) {
      allDetails.put(code, getProtocolModuleDetail(code));
    }

    return allDetails;
  }
}
