package cn.universal.rule.scene.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.persistence.entity.SceneLinkage;
import cn.universal.persistence.mapper.SceneLinkageMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** 缓存查找微基准，不将其吞吐量宣称为含数据库、Redis、通知的系统吞吐量。 */
@EnabledIfSystemProperty(named = "scene.performance.verify", matches = "true")
class SceneRuleCacheLoadTest {
  @Test
  void tenThousandRulesUnderConcurrentLookupNeverQueryDatabaseAfterWarmup() throws Exception {
    var mapper = mock(SceneLinkageMapper.class);
    when(mapper.selectEnabledSceneLinkageScope(anyString(), isNull()))
        .thenAnswer(
            call -> {
              List<SceneLinkage> scenes = new ArrayList<>();
              for (int i = 0; i < 1000; i++) {
                String trigger =
                    "[{\"trigger\":\"device\",\"type\":\"properties\",\"deviceId\":\"d"
                        + i
                        + "\",\"productKey\":\"p\",\"filters\":[{\"key\":\"temperature\",\"operator\":\"gt\",\"value\":\"10\"}]}]";
                scenes.add(
                    SceneLinkage.builder()
                        .id((long) i)
                        .status(0)
                        .triggerCondition(trigger)
                        .build());
              }
              return scenes;
            });
    var cache = new SceneRuleCache(mapper, 300, 100000);
    for (int tenant = 0; tenant < 10; tenant++) cache.find("p", "d0", "t" + tenant, null);
    int workers = 32, iterations = 10000;
    CountDownLatch start = new CountDownLatch(1);
    List<java.util.concurrent.Future<long[]>> tasks = new ArrayList<>();
    long elapsed;
    List<Long> samples = new ArrayList<>();
    try (var threads = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int worker = 0; worker < workers; worker++) {
        String tenant = "t" + worker % 10;
        tasks.add(
            threads.submit(
                () -> {
                  start.await();
                  long[] latency = new long[iterations / 100];
                  for (int i = 0; i < iterations; i++) {
                    long began = i % 100 == 0 ? System.nanoTime() : 0;
                    assertEquals(1, cache.find("p", "d" + i % 1000, tenant, null).size());
                    if (i % 100 == 0) latency[i / 100] = System.nanoTime() - began;
                  }
                  return latency;
                }));
      }
      long began = System.nanoTime();
      start.countDown();
      for (var task : tasks) for (long sample : task.get(30, TimeUnit.SECONDS)) samples.add(sample);
      elapsed = System.nanoTime() - began;
    }
    verify(mapper, times(10)).selectEnabledSceneLinkageScope(anyString(), isNull());
    long[] sorted = samples.stream().mapToLong(Long::longValue).toArray();
    Arrays.sort(sorted);
    System.out.printf(
        "Scene cache only: rules=10000 workers=%d lookups=%d warmupSql=10 lookupSql=0"
            + " elapsedMs=%.1f lookupsPerSecond=%.0f sampleP50us=%.1f sampleP99us=%.1f%n",
        workers,
        workers * iterations,
        elapsed / 1e6,
        workers * iterations * 1e9 / elapsed,
        sorted[sorted.length / 2] / 1e3,
        sorted[(int) (sorted.length * .99)] / 1e3);
  }
}
