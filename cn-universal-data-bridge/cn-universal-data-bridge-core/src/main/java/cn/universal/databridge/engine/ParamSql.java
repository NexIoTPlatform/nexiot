package cn.universal.databridge.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 封装参数化SQL结果：SQL文本 + 有序参数列表 */
public class ParamSql {
  private final String sql;
  private final List<Object> params;

  public ParamSql(String sql, List<Object> params) {
    this.sql = sql;
    this.params = params != null ? new ArrayList<>(params) : new ArrayList<>();
  }

  public String getSql() {
    return sql;
  }

  public List<Object> getParams() {
    return Collections.unmodifiableList(params);
  }
}
