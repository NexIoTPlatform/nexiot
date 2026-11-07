package cn.universal.databridge.engine;

import java.util.List;

/** SQL方言适配器接口：不同数据库的JSON宏、文本转换等差异在此适配 */
public interface SqlDialectAdapter {

  /**
   * 生成 JSON 片段（返回 JSON 类型/对象），参数位置使用占位符 '?'
   *
   * @param keys 键名（静态字符串，已安全）
   * @param valueCount 值数量（等于 keys.size()）
   * @return SQL 片段，如 JSON_OBJECT('k1', ?, 'k2', ?)
   */
  String jsonFragment(List<String> keys, int valueCount);

  /**
   * 生成 JSON 文本片段（返回文本），参数位置使用占位符 '?'
   *
   * @param keys 键名（静态字符串，已安全）
   * @param valueCount 值数量（等于 keys.size()）
   * @return SQL 片段，如 json_build_object(...)::TEXT
   */
  String jsonTextFragment(List<String> keys, int valueCount);

  /**
   * 包装JSON字符串参数以转换为特定数据库的JSON类型
   *
   * @return 转换表达式，默认不转换直接返回 ?
   */
  default String wrapJsonParameter() {
    return "?";
  }
}
