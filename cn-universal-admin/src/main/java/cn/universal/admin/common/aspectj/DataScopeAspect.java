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

package cn.universal.admin.common.aspectj;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.universal.common.annotation.DataScope;
import cn.universal.common.domain.BaseEntity;
import cn.universal.common.utils.ReflectUtils;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Map;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/** 数据过滤处理 */
@Aspect
@Component
public class DataScopeAspect {

  /** 数据范围 */
  public static final String SCOPE = "scope";

  @Resource private IoTUserService ioTUserService;

  // 配置织入点
  @Pointcut("@annotation(cn.universal.common.annotation.DataScope)")
  public void dataScopePointCut() {}

  @Before("dataScopePointCut()")
  public void doBefore(JoinPoint point) throws Throwable {
    clearDataScope(point);
    handleDataScope(point);
  }

  protected void handleDataScope(final JoinPoint joinPoint) {
    // 获得注解
    DataScope controllerDataScope = getAnnotationLog(joinPoint);
    if (controllerDataScope == null) {
      return;
    }
    IoTUser iotUser = ioTUserService.selectUserByUnionId(SecurityUtils.getUnionId());
    if (!iotUser.isAdmin()) {
      dataScopeFilter(joinPoint, controllerDataScope, iotUser.getUnionId());
    }
  }

  /**
   * 数据范围过滤
   *
   * @param joinPoint 切点
   * @param dataScope 数据范围
   * @param unionId 用户unionId
   */
  public static void dataScopeFilter(JoinPoint joinPoint, DataScope dataScope, String unionId) {

    StringBuilder sqlString = new StringBuilder();
    String alias = StrUtil.isNotBlank(dataScope.alias()) ? dataScope.alias() + "." : "";
    sqlString.append(StrUtil.format(" {}union_id = '{}' ", alias, unionId));

    if (StrUtil.isNotBlank(dataScope.created())) {
      sqlString.append(StrUtil.format(" or {} = '{}' ", dataScope.created(), unionId));
    }

    putDataScope(joinPoint, sqlString.toString(), SCOPE);
  }

  /** 是否存在注解，如果存在就获取 */
  private DataScope getAnnotationLog(JoinPoint joinPoint) {
    Signature signature = joinPoint.getSignature();
    MethodSignature methodSignature = (MethodSignature) signature;
    Method method = methodSignature.getMethod();

    if (method != null) {
      return method.getAnnotation(DataScope.class);
    }
    return null;
  }

  /** 拼接权限sql前先清空params.dataScope参数防止注入 */
  private void clearDataScope(final JoinPoint joinPoint) {
    Object params = joinPoint.getArgs()[0];
    if (Validator.isNotNull(params)) {
      putDataScope(joinPoint, "", SCOPE);
    }
  }

  private static void putDataScope(JoinPoint joinPoint, String sql, String queryKey) {
    Object params = joinPoint.getArgs()[0];
    if (Validator.isNotNull(params)) {
      if (params instanceof BaseEntity) {
        BaseEntity baseEntity = (BaseEntity) params;
        baseEntity.getParams().put(queryKey, sql);
      } else {
        Map<String, Object> invoke = ReflectUtils.invokeGetter(params, "params");
        invoke.put(queryKey, sql);
      }
    }
  }
}
