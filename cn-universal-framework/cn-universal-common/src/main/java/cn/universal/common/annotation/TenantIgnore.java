package cn.universal.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 跳过当前线程的租户自动填充与租户过滤能力。
 *
 * <p>用于平台级、公共登录、跨租户运维等明确不应自动附带 tenantId 的场景。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TenantIgnore {}
