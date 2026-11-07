/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权,未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.core.downlink.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.universal.core.downlink.DownlinkContext;
import cn.universal.core.downlink.DownlinkInterceptor;
import cn.universal.core.downlink.InterceptorPhase;
import cn.universal.core.downlink.converter.DownRequestConverter;
import cn.universal.core.message.DownRequest;
import cn.universal.core.message.UnifiedDownlinkCommand;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 命令转换拦截器 负责将UnifiedDownlinkCommand转换为协议特定的DownRequest对象
 *
 * <p>执行时机：MID阶段（在参数验证之后，业务处理之前）
 *
 * <p>转换流程：
 *
 * <ol>
 *   <li>检查上下文中是否存在UnifiedDownlinkCommand
 *   <li>根据协议代码查找对应的转换器
 *   <li>执行转换前的预处理（preConvert）
 *   <li>执行转换（convert）
 *   <li>执行转换后的后处理（postConvert）
 *   <li>将转换结果设置到上下文中
 * </ol>
 *
 * @version 1.0
 * @since 2025/10/25
 */
@Slf4j
@Component
@Order(100) // 在验证拦截器之后执行
public class CommandConversionInterceptor implements DownlinkInterceptor {

  /** 转换器缓存（协议代码 -> 转换器） */
  private final Map<String, DownRequestConverter<?>> converterCache = new ConcurrentHashMap<>();

  /** 所有转换器列表 */
  private List<DownRequestConverter<?>> converters;

  /**
   * 自动注入所有转换器
   *
   * @param converters 所有实现了DownRequestConverter接口的Bean
   */
  @Autowired(required = false)
  public void setConverters(List<DownRequestConverter<?>> converters) {
    this.converters = converters;
    if (converters != null && !converters.isEmpty()) {
      // 按优先级排序
      converters.sort(Comparator.comparingInt(DownRequestConverter::getPriority));

      // 初始化缓存
      for (DownRequestConverter<?> converter : converters) {
        String protocol = converter.supportedProtocol();
        if (StrUtil.isNotBlank(protocol)) {
          // 如果有多个转换器支持同一协议，优先级高的会覆盖优先级低的
          converterCache.put(protocol.toUpperCase(), converter);
        }
      }

      log.info("命令转换拦截器初始化完成，加载转换器数量: {}，支持协议: {}", converters.size(), converterCache.keySet());
    } else {
      log.warn("未找到任何DownRequestConverter实现，命令转换功能将不可用");
    }
  }

  @Override
  public String getName() {
    return "CommandConversionInterceptor";
  }

  @Override
  public int getOrder() {
    return 100; // 在验证拦截器之后执行
  }

  @Override
  public InterceptorPhase getPhase() {
    return InterceptorPhase.MID; // 中置阶段：在消息转换之后，具体处理器之前执行
  }

  @Override
  public boolean preHandle(DownlinkContext<?> context) throws Exception {
    try {
      // 1. 检查是否存在UnifiedCommand
      UnifiedDownlinkCommand command = context.getCommand();
      if (command == null) {
        log.debug("上下文中未找到UnifiedDownlinkCommand，跳过转换");
        return true; // 不阻断流程，向后兼容旧代码
      }

      // 2. 获取协议代码
      String protocolCode = context.getProtocolCode();
      if (StrUtil.isBlank(protocolCode)) {
        log.warn("协议代码为空，无法选择转换器");
        context.markIntercepted("协议代码为空");
        return false;
      }

      // 3. 查找转换器
      DownRequestConverter<?> converter = getConverter(protocolCode);
      if (converter == null) {
        log.warn("未找到协议[{}]对应的转换器", protocolCode);
        context.markIntercepted("未找到协议转换器: " + protocolCode);
        return false;
      }

      // 4. 检查转换器是否支持当前命令
      if (!converter.supports(command)) {
        log.warn("转换器[{}]不支持当前命令类型: {}", converter.getClass().getSimpleName(), command.getCmd());
        context.markIntercepted("转换器不支持当前命令: " + command.getCmd());
        return false;
      }

      // 5. 执行转换前的预处理
      converter.preConvert(command, context);

      // 6. 执行转换（使用类型安全的方式）
      @SuppressWarnings("unchecked")
      DownRequestConverter<DownRequest> typedConverter =
          (DownRequestConverter<DownRequest>) converter;
      DownRequest downRequest = typedConverter.convert(command, context);
      if (downRequest == null) {
        log.error("转换器[{}]返回null", converter.getClass().getSimpleName());
        context.markIntercepted("转换失败：转换器返回null");
        return false;
      }

      // 7. 执行转换后的后处理（使用类型安全的方式）
      typedConverter.postConvert(command, downRequest, context);

      // 8. 将转换结果设置到上下文（使用类型安全的方式）
      @SuppressWarnings("unchecked")
      DownlinkContext<DownRequest> typedContext = (DownlinkContext<DownRequest>) context;
      typedContext.setDownRequest(downRequest);
      context.setAttribute("converter", converter.getClass().getSimpleName());

      log.debug(
          "命令转换成功: {} -> {}",
          command.getClass().getSimpleName(),
          downRequest.getClass().getSimpleName());

      return true;

    } catch (IllegalArgumentException e) {
      log.error("命令转换失败：参数验证失败 - {}", e.getMessage());
      context.markIntercepted("参数验证失败: " + e.getMessage());
      context.setException(e);
      return false;

    } catch (Exception e) {
      log.error("命令转换失败：未知异常", e);
      context.markIntercepted("转换异常: " + e.getMessage());
      context.setException(e);
      return false;
    }
  }

  /**
   * 获取转换器（优先从缓存获取）
   *
   * @param protocolCode 协议代码
   * @return 转换器，如果未找到返回null
   */
  private DownRequestConverter<?> getConverter(String protocolCode) {
    if (StrUtil.isBlank(protocolCode)) {
      return null;
    }

    // 统一转为大写查找
    String key = protocolCode.trim().toUpperCase();
    return converterCache.get(key);
  }

  /**
   * 获取所有支持的协议列表
   *
   * @return 协议代码列表
   */
  public List<String> getSupportedProtocols() {
    return converters.stream()
        .map(DownRequestConverter::supportedProtocol)
        .filter(StrUtil::isNotBlank)
        .sorted()
        .collect(Collectors.toList());
  }

  /**
   * 动态注册转换器（用于扩展）
   *
   * @param converter 转换器实例
   */
  public void registerConverter(DownRequestConverter<?> converter) {
    if (converter == null) {
      throw new IllegalArgumentException("转换器不能为null");
    }

    String protocol = converter.supportedProtocol();
    if (StrUtil.isBlank(protocol)) {
      throw new IllegalArgumentException("协议代码不能为空");
    }

    String key = protocol.trim().toUpperCase();
    converterCache.put(key, converter);

    log.info("动态注册转换器成功: {} -> {}", protocol, converter.getClass().getSimpleName());
  }

  /**
   * 移除转换器
   *
   * @param protocolCode 协议代码
   */
  public void unregisterConverter(String protocolCode) {
    if (StrUtil.isBlank(protocolCode)) {
      return;
    }

    String key = protocolCode.trim().toUpperCase();
    DownRequestConverter<?> removed = converterCache.remove(key);

    if (removed != null) {
      log.info("移除转换器成功: {}", protocolCode);
    }
  }

  @Override
  public void postHandle(DownlinkContext<?> context) throws Exception {
    // 命令转换在preHandle中完成，postHandle不需要额外处理
  }
}
