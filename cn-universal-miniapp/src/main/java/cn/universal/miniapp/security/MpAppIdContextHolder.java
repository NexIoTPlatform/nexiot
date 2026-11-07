/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.security;

/**
 * 小程序AppId上下文持有者 基于ThreadLocal存储当前请求的appId，用于数据隔离
 *
 * @author NexIoT
 * @since 2025/12/15
 */
public class MpAppIdContextHolder {

  private static final ThreadLocal<MpContext> CONTEXT = new ThreadLocal<>();

  /** 设置小程序上下文 */
  public static void set(String appId, Long userId, String openId) {
    CONTEXT.set(new MpContext(appId, userId, openId));
  }

  /** 获取当前appId */
  public static String getAppId() {
    MpContext context = CONTEXT.get();
    return context != null ? context.appId : null;
  }

  /** 获取当前用户ID */
  public static Long getUserId() {
    MpContext context = CONTEXT.get();
    return context != null ? context.userId : null;
  }

  /** 获取当前OpenId */
  public static String getOpenId() {
    MpContext context = CONTEXT.get();
    return context != null ? context.openId : null;
  }

  /** 获取完整上下文 */
  public static MpContext getContext() {
    return CONTEXT.get();
  }

  /** 清除上下文 */
  public static void clear() {
    CONTEXT.remove();
  }

  /** 小程序上下文信息 */
  public record MpContext(String appId, Long userId, String openId) {}
}
