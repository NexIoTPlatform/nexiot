package cn.universal.databridge.engine.dialect;

import cn.universal.databridge.engine.SqlDialectAdapter;
import java.util.List;
import java.util.stream.Collectors;

/** MySQL 方言适配 */
public class MySqlDialectAdapter implements SqlDialectAdapter {
  private String buildPairs(List<String> keys) {
    return keys.stream()
        .map(k -> "'" + k.replace("'", "''") + "', ?")
        .collect(Collectors.joining(", "));
  }

  @Override
  public String jsonFragment(List<String> keys, int valueCount) {
    return "JSON_OBJECT(" + buildPairs(keys) + ")";
  }

  @Override
  public String jsonTextFragment(List<String> keys, int valueCount) {
    // MySQL 的 JSON_OBJECT 返回 JSON 文本即可
    return jsonFragment(keys, valueCount);
  }
}
