package cn.universal.security.support;

import cn.hutool.json.JSONUtil;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Redis 共享权限集合。版本键失效避免并发旧查询在清缓存后重新写回有效键。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MenuPermissionCache {
  private static final String PREFIX = "security:menu-permissions:";
  private static final String VERSION = PREFIX + "version";
  private final StringRedisTemplate redis;

  @Value("${security.menu-permissions.cache-seconds:300}")
  private long cacheSeconds = 300;

  public Set<String> get(String unionId, Supplier<Set<String>> loader) {
    if (cacheSeconds <= 0 || TransactionSynchronizationManager.isActualTransactionActive())
      return loader.get();
    String key;
    try {
      String version = redis.opsForValue().get(VERSION);
      key = PREFIX + (version == null ? "0" : version) + ":" + unionId;
      String cached = redis.opsForValue().get(key);
      if (cached != null) return new HashSet<>(JSONUtil.toList(cached, String.class));
    } catch (RuntimeException e) {
      log.warn("权限缓存读取失败，回源查询", e);
      return loader.get();
    }
    Set<String> permissions = loader.get();
    try {
      redis.opsForValue().set(key, JSONUtil.toJsonStr(permissions), Duration.ofSeconds(cacheSeconds));
    } catch (RuntimeException e) {
      log.warn("权限缓存写入失败", e);
    }
    return permissions;
  }

  public void invalidateAfterCommit() {
    if (cacheSeconds <= 0) return;
    if (TransactionSynchronizationManager.isSynchronizationActive()
        && TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override public void afterCommit() { invalidate(); }
      });
    } else {
      invalidate();
    }
  }

  private void invalidate() {
    redis.opsForValue().set(VERSION, UUID.randomUUID().toString());
  }
}
