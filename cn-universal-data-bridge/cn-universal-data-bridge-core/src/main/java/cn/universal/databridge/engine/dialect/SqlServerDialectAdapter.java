package cn.universal.databridge.engine.dialect;

import cn.universal.databridge.engine.SqlDialectAdapter;
import java.util.List;

/** SQL Server 方言适配 注意：SQL Server 2016之前不支持JSON_OBJECT，2016+语法也不同 这里使用字符串拼接方式构造JSON（兼容所有版本） */
public class SqlServerDialectAdapter implements SqlDialectAdapter {

  @Override
  public String jsonFragment(List<String> keys, int valueCount) {
    // SQL Server 使用 CONCAT 函数拼接 JSON 字符串
    // 简化处理：所有值都当作字符串处理（加引号）
    // 如果需要精确区分数值，应在应用层处理
    StringBuilder sb = new StringBuilder("CONCAT('{'");
    for (int i = 0; i < keys.size(); i++) {
      if (i > 0) {
        sb.append(", ','");
      }
      String key = keys.get(i).replace("'", "''");
      sb.append(", '\"" + key + "\":', COALESCE('\"' + CAST(? AS NVARCHAR(MAX)) + '\"', 'null')");
    }
    sb.append(", '}'");
    return sb.toString() + ")"; // 添加 CONCAT 的闭合括号
  }

  @Override
  public String jsonTextFragment(List<String> keys, int valueCount) {
    // SQL Server 的 JSON 构造本身就是文本
    return jsonFragment(keys, valueCount);
  }
}
