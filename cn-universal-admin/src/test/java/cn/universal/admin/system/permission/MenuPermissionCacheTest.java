package cn.universal.admin.system.permission;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.security.support.MenuPermissionCache;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class MenuPermissionCacheTest {
  Map<String, String> data;
  ValueOperations<String, String> values;
  MenuPermissionCache cache;

  @BeforeEach
  void setup() {
    data = new HashMap<>();
    var redis = mock(StringRedisTemplate.class);
    values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(values.get(anyString())).thenAnswer(i -> data.get(i.getArgument(0)));
    doAnswer(i -> { data.put(i.getArgument(0), i.getArgument(1)); return null; })
        .when(values).set(anyString(), anyString());
    doAnswer(i -> { data.put(i.getArgument(0), i.getArgument(1)); return null; })
        .when(values).set(anyString(), anyString(), any(Duration.class));
    cache = new MenuPermissionCache(redis);
  }

  @Test void cachesByUserAndReloadsAfterRevocation() {
    AtomicInteger reads = new AtomicInteger();
    assertEquals(Set.of("alarm:record:list"), cache.get("alice", () -> {
      reads.incrementAndGet(); return Set.of("alarm:record:list");
    }));
    assertEquals(Set.of("alarm:record:list"), cache.get("alice", () -> {
      fail("cache hit must not reload permissions"); return Set.of();
    }));
    assertEquals(Set.of(), cache.get("bob", Set::of));
    cache.invalidateAfterCommit();
    assertEquals(Set.of(), cache.get("alice", () -> { reads.incrementAndGet(); return Set.of(); }));
    assertEquals(2, reads.get());
    verify(values, atLeastOnce()).set(anyString(), anyString(), eq(Duration.ofSeconds(300)));
  }

  @Test void staleConcurrentLoadCannotResurrectRevokedPermissions() {
    cache.get("alice", () -> {
      cache.invalidateAfterCommit();
      return Set.of("alarm:record:handle");
    });
    assertEquals(Set.of(), cache.get("alice", Set::of));
  }

  @Test void invalidationWaitsForCommitAndRollbackKeepsCache() {
    cache.get("alice", () -> Set.of("before"));
    TransactionSynchronizationManager.initSynchronization();
    TransactionSynchronizationManager.setActualTransactionActive(true);
    try {
      cache.invalidateAfterCommit();
      assertFalse(data.containsKey("security:menu-permissions:version"));
      var callbacks = TransactionSynchronizationManager.getSynchronizations();
      assertEquals(1, callbacks.size());
      callbacks.forEach(c -> c.afterCompletion(1));
      assertFalse(data.containsKey("security:menu-permissions:version"));
      callbacks.forEach(c -> c.afterCommit());
      assertTrue(data.containsKey("security:menu-permissions:version"));
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
      TransactionSynchronizationManager.setActualTransactionActive(false);
    }
    assertEquals(Set.of("after"), cache.get("alice", () -> Set.of("after")));
  }

  @Test void evictionAnnotationInterceptsSuccessfulWritesButNotFailedWrites() {
    try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
      context.registerBean(MenuPermissionCache.class, () -> cache);
      context.registerBean(cn.universal.security.support.MenuPermissionCacheEvictionAspect.class);
      context.register(AspectConfig.class);
      context.refresh();
      var writes = context.getBean(PermissionWrites.class);
      cache.get("alice", () -> Set.of("before"));
      assertThrows(IllegalStateException.class, () -> writes.update(true));
      assertFalse(data.containsKey("security:menu-permissions:version"));
      writes.update(false);
      assertTrue(data.containsKey("security:menu-permissions:version"));
      assertEquals(Set.of(), cache.get("alice", Set::of));
    }
  }

  @org.springframework.context.annotation.Configuration
  @org.springframework.context.annotation.EnableAspectJAutoProxy(proxyTargetClass = true)
  static class AspectConfig {
    @org.springframework.context.annotation.Bean
    PermissionWrites permissionWrites() { return new PermissionWrites(); }
  }

  static class PermissionWrites {
    @cn.universal.security.support.EvictMenuPermissions
    public void update(boolean fail) { if (fail) throw new IllegalStateException("rollback"); }
  }

  @Test void revokedAdministratorIdentityIsNotRecoveredFromOldUserCache() {
    var users = mock(cn.universal.security.service.IoTUserService.class);
    var menus = mock(cn.universal.admin.system.service.ISysMenuService.class);
    var service = new SysPermissionService();
    org.springframework.test.util.ReflectionTestUtils.setField(service, "userService", users);
    org.springframework.test.util.ReflectionTestUtils.setField(service, "menuService", menus);
    org.springframework.test.util.ReflectionTestUtils.setField(service, "menuPermissionCache", cache);
    var stale = new cn.universal.persistence.entity.IoTUser();
    stale.setUnionId("alice"); stale.setIdentity(0); stale.setStatus(0);
    var current = new cn.universal.persistence.entity.IoTUser();
    current.setUnionId("alice"); current.setIdentity(0); current.setStatus(0);
    when(users.queryOne(any())).thenReturn(current);
    assertEquals(Set.of("*:*:*"), service.getMenuPermission(stale));
    current.setIdentity(1);
    when(menus.selectMenuPermsByUserId("alice")).thenReturn(Set.of("alarm:record:list"));
    cache.invalidateAfterCommit();
    assertEquals(Set.of("alarm:record:list"), service.getMenuPermission(stale));
    current.setStatus(1);
    cache.invalidateAfterCommit();
    assertEquals(Set.of(), service.getMenuPermission(stale));
  }

  @Test void unavailableRedisFallsBackToCurrentDatabasePermissions() {
    when(values.get(anyString())).thenThrow(new IllegalStateException("offline"));
    assertEquals(Set.of(), cache.get("alice", Set::of));
  }
}
