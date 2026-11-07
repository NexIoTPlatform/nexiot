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

import cn.universal.common.domain.R;
import cn.universal.core.downlink.DownlinkContext;
import cn.universal.core.downlink.DownlinkInterceptorChain;
import cn.universal.core.message.UnifiedDownlinkCommand;

/** （下行）平台到设备 */
public interface IDown {

  /** 执行服务名称 */
  String name();

  /** 执行服务code */
  String code();

  /**
   * 下行处理（统一命令对象）
   *
   * <p>这是唯一的下行入口方法，提供类型安全的参数传递
   *
   * <p>此方法会自动应用拦截器链（如果已配置）
   *
   * @param command 统一下行命令对象
   * @return 处理结果
   */
  default R doAction(UnifiedDownlinkCommand command) {
    // 检查是否启用拦截器模式
    if (getInterceptorChain() != null) {
      // 启用拦截器模式
      DownlinkContext<?> context = createContext(command);
      context.setCommand(command); // 设置统一命令对象
      return doActionWithInterceptors(context);
    } else {
      // 未启用拦截器，直接调用 doProcess（向后兼容）
      DownlinkContext<?> context = createContext(command);
      context.setCommand(command);
      context.setProtocolCode(code());
      context.setProtocolName(name());
      return doProcess(context);
    }
  }

  /** 产品级下行处理 */
  default R downToThirdPlatform(String msg) {
    return null;
  }

  /**
   * 创建下行上下文 基于UnifiedDownlinkCommand创建上下文对象
   *
   * @param command 统一下行命令对象
   * @return 下行上下文
   */
  default DownlinkContext<?> createContext(UnifiedDownlinkCommand command) {
    DownlinkContext<?> context = new DownlinkContext<>();

    // 设置统一命令对象
    context.setCommand(command);
    context.setRawMessage(command.toJson().toString());
    context.setJsonMessage(command.toJson());

    // 设置关键信息到上下文
    context.setProductKey(command.getProductKey());
    context.setDeviceId(command.getDeviceId());
    context.setIotId(command.getIotId());

    return context;
  }

  /**
   * 核心处理方法（由子类实现具体逻辑） 此方法在拦截器链执行完成后被调用 默认实现：调用原有的 doAction 方法（向后兼容）
   *
   * @param context 下行上下文
   * @return 处理结果
   */
  default R doProcess(DownlinkContext<?> context) {
    // 默认实现：向后兼容，调用原有逻辑
    // 子类可以重写此方法以使用拦截器模式
    return R.error("请实现 doProcess 方法或重写 doAction 方法");
  }

  /**
   * 带拦截器的下行处理模板方法 定义了完整的执行流程：前置拦截器 -> 中置拦截器 -> 核心处理 -> 后置拦截器
   *
   * @param context 下行上下文
   * @return 处理结果
   */
  default R doActionWithInterceptors(DownlinkContext<?> context) {
    DownlinkInterceptorChain chain = getInterceptorChain();
    Exception exception = null;

    try {
      // 记录开始时间
      context.setStartTime(System.currentTimeMillis());
      context.setProtocolCode(code());
      context.setProtocolName(name());

      // 1. 执行前置拦截器（在消息转换之前）
      if (chain != null && !chain.executePreInterceptors(context)) {
        return R.error("请求被拦截: " + context.getInterruptReason());
      }

      // 2. 执行中置拦截器（在消息转换之后，核心处理之前）
      if (chain != null && !chain.executeMidInterceptors(context)) {
        return R.error("请求被拦截: " + context.getInterruptReason());
      }

      // 3. 执行核心处理逻辑
      R result = doProcess(context);
      context.setResult(result);

      // 4. 执行后置拦截器
      if (chain != null) {
        chain.executePostInterceptors(context);
      }

      return result;

    } catch (Exception e) {
      exception = e;
      context.setException(e);
      return R.error("处理异常: " + e.getMessage());
    } finally {
      // 记录结束时间
      context.setEndTime(System.currentTimeMillis());

      // 5. 触发完成拦截器（无论成功失败都会执行）
      if (chain != null) {
        chain.triggerAfterCompletion(context, exception);
      }
    }
  }

  /**
   * 获取拦截器链（由Spring注入） 子类需要通过依赖注入提供拦截器链实例 默认实现：返回 null（不使用拦截器）
   *
   * @return 拦截器链
   */
  default DownlinkInterceptorChain getInterceptorChain() {
    // 默认返回 null，doActionWithInterceptors 中已做 null 检查
    // 子类可以重写此方法以启用拦截器
    return null;
  }

  /**
   * 保存设备云端指令
   *
   * @param productKey 产品key
   * @param deviceId 设备序列号
   */
  default void storeCommand(String productKey, String deviceId, Object data) {}
}
