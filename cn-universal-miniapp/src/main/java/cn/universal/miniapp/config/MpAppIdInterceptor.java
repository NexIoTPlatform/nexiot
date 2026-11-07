/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.config;

import cn.universal.miniapp.security.MpAppIdContextHolder;
import java.lang.reflect.Field;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * 小程序AppId数据隔离拦截器 仅对小程序模块的Mapper生效，自动注入app_id条件
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@Intercepts({
  @Signature(
      type = Executor.class,
      method = "query",
      args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
  @Signature(
      type = Executor.class,
      method = "update",
      args = {MappedStatement.class, Object.class})
})
public class MpAppIdInterceptor implements Interceptor {

  /** 小程序Mapper包前缀 */
  private static final String MP_MAPPER_PREFIX = "cn.universal.miniapp.mybatis";

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
    String mapperId = mappedStatement.getId();

    // 仅对小程序模块的Mapper生效
    if (!mapperId.startsWith(MP_MAPPER_PREFIX)) {
      return invocation.proceed();
    }

    // 获取当前appId
    String appId = MpAppIdContextHolder.getAppId();
    if (appId == null) {
      // 没有设置appId上下文，直接放行（可能是后台管理接口调用）
      return invocation.proceed();
    }

    Object parameter = invocation.getArgs()[1];

    // 如果参数对象有appId字段，自动设置
    if (parameter != null) {
      setAppIdField(parameter, appId);
    }

    return invocation.proceed();
  }

  /** 设置参数对象的appId字段 */
  private void setAppIdField(Object parameter, String appId) {
    if (parameter == null) {
      return;
    }

    try {
      Class<?> clazz = parameter.getClass();

      // 尝试设置appId字段
      Field appIdField = findField(clazz, "appId");
      if (appIdField != null) {
        appIdField.setAccessible(true);
        Object currentValue = appIdField.get(parameter);
        // 只有当前值为空时才设置
        if (currentValue == null) {
          appIdField.set(parameter, appId);
          log.debug("自动注入appId: mapper参数={}, appId={}", clazz.getSimpleName(), appId);
        }
      }
    } catch (Exception e) {
      log.warn("设置appId字段失败: {}", e.getMessage());
    }
  }

  /** 查找字段（包括父类） */
  private Field findField(Class<?> clazz, String fieldName) {
    while (clazz != null && clazz != Object.class) {
      try {
        return clazz.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    }
    return null;
  }

  @Override
  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  @Override
  public void setProperties(Properties properties) {
    // 可以从配置中读取属性
  }
}
