package cn.universal.core.engine.compile;

import cn.universal.core.engine.MagicScript;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class CompileCache {

  private final LinkedHashMap<String, MagicScript> cacheMap;

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  public CompileCache(int capacity) {
    this.cacheMap =
        new LinkedHashMap<String, MagicScript>((int) Math.ceil(capacity / 0.75) + 1, 0.75f, true) {

          @Override
          protected boolean removeEldestEntry(Map.Entry<String, MagicScript> eldest) {
            // lru淘汰
            return size() > capacity;
          }
        };
  }

  public void put(String key, MagicScript script) {
    lock.writeLock().lock();
    try {
      cacheMap.put(key, script);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public MagicScript get(String key) {
    lock.readLock().lock();
    try {
      return cacheMap.get(key);
    } finally {
      lock.readLock().unlock();
    }
  }

  public MagicScript get(String key, Supplier<MagicScript> value) {
    MagicScript script = get(key);
    if (script == null) {
      script = value.get();
      put(key, script);
    }
    return script;
  }
}
