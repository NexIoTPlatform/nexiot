package cn.universal.core.alarm;

import java.util.Map;

/**
 * Trusted server-side alarm boundary. Tenant/actor must come from authenticated runtime identity.
 */
public interface AlarmOperations {

  /**
   * 调用方已经判断异常，直接创建或合并告警。
   *
   * @param tenant
   * @param actor
   * @param signal
   * @return
   */
  Map<String, Object> raise(String tenant, String actor, AlarmSignal signal);

  /**
   * 调用方已有明确恢复依据，恢复对应异常。
   *
   * @param tenant
   * @param actor
   * @param signal
   * @return
   */
  Map<String, Object> recover(String tenant, String actor, AlarmSignal signal);

  /**
   * 接收事实，交给策略判断；接收成功不代表一定生成告警。 Persist an input before acknowledging it; matching and side effects
   *
   * @param tenant
   * @param actor
   * @param signal
   * @return
   */
  String ingest(String tenant, String actor, AlarmSignal signal);
}
