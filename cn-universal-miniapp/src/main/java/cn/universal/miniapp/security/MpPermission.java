/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.security;

import java.lang.annotation.*;

/**
 * 小程序权限注解 用于Controller方法上，标记该接口需要的权限
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MpPermission {

  /** 需要的权限码 可选值：DEVICE_ADD, DEVICE_DELETE, FUNCTION_CALL, DEVICE_UNBIND */
  String value();

  /** 权限不足时的提示信息 */
  String message() default "无操作权限";
}
