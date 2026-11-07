/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.security;

import cn.universal.miniapp.mapper.MpUserPermissionMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 小程序权限校验切面
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@Aspect
@Component
public class MpPermissionAspect {

  @Autowired private MpUserPermissionMapper mpUserPermissionMapper;

  @Around("@annotation(mpPermission)")
  public Object checkPermission(ProceedingJoinPoint joinPoint, MpPermission mpPermission)
      throws Throwable {
    String requiredPermission = mpPermission.value();
    String message = mpPermission.message();

    // 获取当前用户信息
    String appId = MpAppIdContextHolder.getAppId();
    Long userId = MpAppIdContextHolder.getUserId();

    if (appId == null || userId == null) {
      throw new MpAccessDeniedException("用户未登录");
    }

    // 检查权限
    boolean hasPermission = mpUserPermissionMapper.hasPermission(userId, appId, requiredPermission);
    if (!hasPermission) {
      log.warn("权限校验失败: userId={}, appId={}, required={}", userId, appId, requiredPermission);
      throw new MpAccessDeniedException(message);
    }

    log.debug("权限校验通过: userId={}, appId={}, permission={}", userId, appId, requiredPermission);
    return joinPoint.proceed();
  }
}
