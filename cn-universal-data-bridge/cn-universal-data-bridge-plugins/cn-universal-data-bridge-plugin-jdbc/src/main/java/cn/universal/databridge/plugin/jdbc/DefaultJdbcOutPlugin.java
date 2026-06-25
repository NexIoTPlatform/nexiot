package cn.universal.databridge.plugin.jdbc;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.universal.databridge.engine.ParamSql;
import cn.universal.databridge.engine.ParamTemplateEngine;
import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.PluginInfo;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.databridge.plugin.AbstractDataOutputPlugin;
import cn.universal.databridge.plugin.SourceScope;
import cn.universal.databridge.util.DataBridgeConnectionManager;
import cn.universal.persistence.base.BaseUPRequest;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 默认JDBC数据桥接插件 - 输出方向（优化版） */
@Component("defaultJdbcOutPlugin")
@ConditionalOnMissingBean(name = "jdbcOutPlugin")
@Slf4j
public class DefaultJdbcOutPlugin extends AbstractDataOutputPlugin {

  // 数据桥接连接池管理器 - 独立管理，避免与框架冲突
  private final DataBridgeConnectionManager connectionManager = new DataBridgeConnectionManager();

  @Override
  public PluginInfo getPluginInfo() {
    return PluginInfo.builder()
        .name("默认JDBC数据桥接插件")
        .version("2.1.0") // 升级版本号
        .description("默认的JDBC数据桥接实现，支持安全的模板变量替换")
        .author("gitee.com/NexIoT")
        .pluginType("JDBC")
        .supportedResourceTypes(List.of("MYSQL", "POSTGRESQL", "H2", "ORACLE", "SQLSERVER"))
        .dataDirection(PluginInfo.DataDirection.OUTPUT)
        .category("数据库")
        .icon("database")
        .build();
  }

  @Override
  public Boolean testConnection(ResourceConnection connection) {
    try {
      DataSource dataSource = connectionManager.getOrCreateDataSource(connection);
      try (Connection conn = dataSource.getConnection()) {
        return conn.isValid(5);
      }
    } catch (Exception e) {
      log.error("JDBC连接测试失败: {}", e.getMessage(), e);
      return false;
    }
  }

  @Override
  public Boolean validateConfig(DataBridgeConfig config) {
    if (config == null) {
      return false;
    }
    // 验证模板中的占位符格式（可选，增强健壮性）
    if (config.getTemplate() != null
        && config.getTemplate().contains(LEFT_PLACEHOLDER_PREFIX)
        && !config.getTemplate().contains(RIGHT_PLACEHOLDER_SUFFIX)) {
      log.error("模板中存在未闭合的占位符: {}", config.getTemplate());
      return false;
    }
    return true;
  }

  @Override
  public List<SourceScope> getSupportedSourceScopes() {
    return List.of(
        SourceScope.ALL_PRODUCTS, SourceScope.SPECIFIC_PRODUCTS, SourceScope.APPLICATION);
  }

  @Override
  protected void processWithDefaultTemplate(
      List<BaseUPRequest> requests,
      DataBridgeConfig config,
      ResourceConnection connection,
      JSONObject configJson) {
    try {
      DataSource dataSource = connectionManager.getOrCreateDataSource(connection);
      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
      for (BaseUPRequest request : requests) {
        Map<String, Object> variables = buildTemplateVariables(request, configJson);
        executeParamSql(jdbcTemplate, config, variables, connection);
      }
    } catch (Exception e) {
      connectionManager.removeDataSource(connection);
      throw new RuntimeException("SQL执行失败: " + e.getMessage(), e);
    }
  }

  @Override
  protected void processProcessedData(
      Object processedData,
      BaseUPRequest request,
      DataBridgeConfig config,
      ResourceConnection connection) {
    try {
      DataSource dataSource = connectionManager.getOrCreateDataSource(connection);
      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

      List<ParamSql> sqlList = generateExecutableSqlList(processedData, config, connection);
      for (ParamSql sql : sqlList) {
        executeParamSql(jdbcTemplate, sql);
      }

    } catch (Exception e) {
      log.error("处理Magic脚本返回数据失败: {}", e.getMessage());
      throw new RuntimeException("处理Magic脚本返回数据失败: " + e.getMessage(), e);
    }
  }

  @Override
  protected void processTemplateResult(
      String templateResult,
      BaseUPRequest request,
      DataBridgeConfig config,
      ResourceConnection connection) {
    try {
      javax.sql.DataSource dataSource = connectionManager.getOrCreateDataSource(connection);
      org.springframework.jdbc.core.JdbcTemplate jdbcTemplate =
          new org.springframework.jdbc.core.JdbcTemplate(dataSource);

      // 构建变量并进行参数化模板解析（避免引号问题，兼容多方言）
      java.util.Map<String, Object> variables = buildTemplateVariables(request, parseConfig(config));
      ParamTemplateEngine engine = new ParamTemplateEngine();
      cn.universal.databridge.engine.SqlDialectAdapter adapter = getAdapter(connection.getType());
      ParamSql ps = engine.process(config.getTemplate(), variables, adapter);

      executeParamSql(jdbcTemplate, ps);
    } catch (Exception e) {
      log.error("执行SQL失败: {} - {}", templateResult, e.getMessage());
      connectionManager.removeDataSource(connection);
      throw new RuntimeException("SQL执行失败: " + e.getMessage(), e);
    }
  }

  private cn.universal.databridge.engine.SqlDialectAdapter getAdapter(
      cn.universal.databridge.entity.ResourceConnection.ResourceType type) {
    switch (type) {
      case MYSQL:
      case H2:
        return new cn.universal.databridge.engine.dialect.MySqlDialectAdapter();
      case POSTGRESQL:
        return new cn.universal.databridge.engine.dialect.PostgresDialectAdapter();
      case ORACLE:
        return new cn.universal.databridge.engine.dialect.OracleDialectAdapter();
      case SQLSERVER:
        return new cn.universal.databridge.engine.dialect.SqlServerDialectAdapter();
      default:
        return new cn.universal.databridge.engine.dialect.MySqlDialectAdapter();
    }
  }

  private List<ParamSql> generateExecutableSqlList(
      Object processedData, DataBridgeConfig config, ResourceConnection connection) {
    List<ParamSql> sqlList = new ArrayList<>();

    try {
      if (processedData instanceof String) {
        throw new IllegalArgumentException("JDBC输出Magic脚本不允许直接返回SQL字符串");
      } else if (processedData instanceof List) {
        @SuppressWarnings("unchecked")
        List<Object> dataList = (List<Object>) processedData;
        for (Object item : dataList) {
          if (item instanceof String) {
            throw new IllegalArgumentException("JDBC输出Magic脚本不允许直接返回SQL字符串");
          } else if (item instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) item;
            sqlList.add(buildParamSql(config, dataMap, connection));
          }
        }
      } else if (processedData instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> dataMap = (Map<String, Object>) processedData;
        sqlList.add(buildParamSql(config, dataMap, connection));
      } else {
        log.warn(
            "Magic脚本返回的数据类型不支持: {}",
            processedData != null ? processedData.getClass().getSimpleName() : "null");
      }

    } catch (Exception e) {
      log.error("处理Magic脚本返回数据失败: {}", e.getMessage());
      throw new RuntimeException("处理Magic脚本返回数据失败: " + e.getMessage(), e);
    }

    return sqlList;
  }

  private ParamSql buildParamSql(
      DataBridgeConfig config, Map<String, Object> variables, ResourceConnection connection) {
    ParamTemplateEngine engine = new ParamTemplateEngine();
    return engine.process(config.getTemplate(), variables, getAdapter(connection.getType()));
  }

  private void executeParamSql(
      JdbcTemplate jdbcTemplate,
      DataBridgeConfig config,
      Map<String, Object> variables,
      ResourceConnection connection) {
    executeParamSql(jdbcTemplate, buildParamSql(config, variables, connection));
  }

  private void executeParamSql(JdbcTemplate jdbcTemplate, ParamSql sql) {
    if (sql == null || StrUtil.isBlank(sql.getSql())) {
      return;
    }
    jdbcTemplate.update(sql.getSql(), sql.getParams().toArray());
  }
}
