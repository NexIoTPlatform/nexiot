package cn.universal.databridge.engine.dialect;

import cn.universal.databridge.engine.SqlDialectAdapter;
import java.util.List;
import java.util.stream.Collectors;

/** Oracle 方言适配 */
public class OracleDialectAdapter implements SqlDialectAdapter {
  private String buildPairs(List<String> keys) {
    // Oracle: JSON_OBJECT('k1' VALUE ?, 'k2' VALUE ?)
    return keys.stream()
        .map(k -> "'" + k.replace("'", "''") + "' VALUE ?")
        .collect(Collectors.joining(", "));
  }

  @Override
  public String jsonFragment(List<String> keys, int valueCount) {
    return "JSON_OBJECT(" + buildPairs(keys) + ")";
  }

  @Override
  public String jsonTextFragment(List<String> keys, int valueCount) {
    // Oracle 的 JSON_OBJECT 返回 JSON 文本字符串
    return jsonFragment(keys, valueCount);
  }
}
