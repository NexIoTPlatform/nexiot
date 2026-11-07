package cn.universal.databridge.engine.dialect;

import cn.universal.databridge.engine.SqlDialectAdapter;
import java.util.List;
import java.util.stream.Collectors;

/** PostgreSQL 方言适配 */
public class PostgresDialectAdapter implements SqlDialectAdapter {
  private String buildPairs(List<String> keys) {
    return keys.stream()
        .map(k -> "'" + k.replace("'", "''") + "', ?")
        .collect(Collectors.joining(", "));
  }

  @Override
  public String jsonFragment(List<String> keys, int valueCount) {
    return "json_build_object(" + buildPairs(keys) + ")";
  }

  @Override
  public String jsonTextFragment(List<String> keys, int valueCount) {
    return jsonFragment(keys, valueCount) + "::TEXT";
  }

  @Override
  public String wrapJsonParameter() {
    // PostgreSQL: 将 JSON 字符串转为 jsonb 类型
    return "?::jsonb";
  }
}
