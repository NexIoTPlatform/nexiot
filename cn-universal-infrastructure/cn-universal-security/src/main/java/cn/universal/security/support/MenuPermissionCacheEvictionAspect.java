package cn.universal.security.support;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
@RequiredArgsConstructor
public class MenuPermissionCacheEvictionAspect {
  private final MenuPermissionCache cache;

  @AfterReturning("@annotation(cn.universal.security.support.EvictMenuPermissions)")
  public void evict() {
    cache.invalidateAfterCommit();
  }
}
