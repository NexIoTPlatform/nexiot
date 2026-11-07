package cn.universal.security.aspect;

import cn.universal.common.annotation.TenantIgnore;
import cn.universal.security.support.TenantIgnoreContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/** {@link TenantIgnore} 的线程上下文织入。 */
@Aspect
@Component
public class TenantIgnoreAspect {

  @Around("@within(tenantIgnore) || @annotation(tenantIgnore)")
  public Object around(ProceedingJoinPoint joinPoint, TenantIgnore tenantIgnore) throws Throwable {
    TenantIgnoreContext.enter();
    try {
      return joinPoint.proceed();
    } finally {
      TenantIgnoreContext.exit();
    }
  }
}
