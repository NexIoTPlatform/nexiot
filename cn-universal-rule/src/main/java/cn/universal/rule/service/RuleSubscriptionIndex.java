package cn.universal.rule.service;

import cn.hutool.json.JSONUtil;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.rule.model.RuleConfig;
import cn.universal.rule.model.RuleTarget;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;

/** Legacy SQL rules are preloaded, not queried/parsed for every uplink. No SQL-to-Flow bridge. */
@Component
public class RuleSubscriptionIndex implements AutoCloseable {
  public record Rule(Long id, String name, String sql, List<RuleTarget> targets) {}

  private record Key(String creator, String relation) {}

  private record Snapshot(Map<Key, List<Rule>> routes, long validUntil) {}

  private volatile Snapshot snapshot = new Snapshot(Map.of(), 0);
  private final JdbcTemplate jdbc;
  private final MeterRegistry metrics;
  private final ScheduledExecutorService control =
      Executors.newSingleThreadScheduledExecutor(
          Thread.ofVirtual().name("legacy-rule-index").factory());
  private final AtomicBoolean queued = new AtomicBoolean();
  private long warningAt;

  public RuleSubscriptionIndex(JdbcTemplate jdbc, MeterRegistry metrics) {
    this.jdbc = jdbc;
    this.metrics = metrics;
  }

  @PostConstruct
  public void start() {
    refresh();
    control.scheduleWithFixedDelay(this::refresh, 5, 5, TimeUnit.SECONDS);
  }

  public List<Rule> match(IoTDeviceDTO device) {
    Snapshot current = snapshot;
    if (device == null || System.currentTimeMillis() > current.validUntil()) return List.of();
    Set<Rule> result = new LinkedHashSet<>();
    String creator = device.getUserUnionId();
    result.addAll(
        current.routes().getOrDefault(new Key(creator, device.getProductKey()), List.of()));
    result.addAll(current.routes().getOrDefault(new Key(creator, device.getIotId()), List.of()));
    if (device.getDevGroupId() != null)
      for (String group : device.getDevGroupId())
        result.addAll(current.routes().getOrDefault(new Key(creator, group), List.of()));
    return List.copyOf(result);
  }

  public void changedAfterCommit() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              changed();
            }
          });
    } else changed();
  }

  private void changed() {
    if (!control.isShutdown() && queued.compareAndSet(false, true))
      control.execute(
          () -> {
            queued.set(false);
            refresh();
          });
  }

  void refresh() {
    try {
      Map<Long, Rule> rules = new HashMap<>();
      Map<Key, List<Rule>> routes = new HashMap<>();
      Set<Long> invalid = new HashSet<>();
      jdbc.query(
          "SELECT rm.id,rm.creator_id,rm.rule_name,rm.config,rmi.relation_id FROM rule_model rm"
              + " JOIN rule_model_instance rmi ON rmi.model_id=rm.id WHERE rm.status='run'",
          row -> {
            long id = row.getLong("id");
            if (invalid.contains(id)) return;
            Rule rule = rules.get(id);
            if (rule == null) {
              try {
                var config = JSONUtil.toBean(row.getString("config"), RuleConfig.class);
                rule =
                    new Rule(
                        id,
                        row.getString("rule_name"),
                        config.getSql(),
                        config.getTargets() == null ? List.of() : List.copyOf(config.getTargets()));
                rules.put(id, rule);
              } catch (RuntimeException malformed) {
                invalid.add(id);
                return;
              }
            }
            routes
                .computeIfAbsent(
                    new Key(row.getString("creator_id"), row.getString("relation_id")),
                    k -> new ArrayList<>())
                .add(rule);
          });
      routes.replaceAll((key, values) -> List.copyOf(values));
      snapshot = new Snapshot(Map.copyOf(routes), System.currentTimeMillis() + 30_000);
      metrics.counter("iot.rule.index.refresh", "result", "success").increment();
      if (!invalid.isEmpty()) metrics.counter("iot.rule.index.invalid").increment(invalid.size());
    } catch (RuntimeException error) {
      metrics.counter("iot.rule.index.refresh", "result", "failure").increment();
      if (System.currentTimeMillis() >= warningAt) {
        warningAt = System.currentTimeMillis() + 30_000;
        org.slf4j.LoggerFactory.getLogger(getClass()).warn("SQL 规则索引刷新失败，旧快照最多保留 30 秒", error);
      }
    }
  }

  @PreDestroy
  public void close() {
    control.shutdownNow();
  }
}
