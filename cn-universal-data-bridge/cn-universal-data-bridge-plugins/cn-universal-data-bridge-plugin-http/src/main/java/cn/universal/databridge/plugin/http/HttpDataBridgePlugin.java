package cn.universal.databridge.plugin.http;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.PluginInfo;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.databridge.plugin.AbstractDataOutputPlugin;
import cn.universal.databridge.plugin.SourceScope;
import cn.universal.persistence.base.BaseUPRequest;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component("http")
@ConditionalOnProperty(
    prefix = "databridge.plugins.http",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@Slf4j
public class HttpDataBridgePlugin extends AbstractDataOutputPlugin {

  private final RestTemplate restTemplate = new RestTemplate();

  @Override
  public PluginInfo getPluginInfo() {
    return PluginInfo.builder()
        .name("HTTP数据桥接插件")
        .version("1.0.0")
        .description("支持HTTP上报")
        .author("gitee.com/NexIoT")
        .pluginType("HTTP")
        .supportedResourceTypes(List.of("HTTP"))
        .dataDirection(PluginInfo.DataDirection.BIDIRECTIONAL)
        .category("网络")
        .icon("api")
        .build();
  }

  @Override
  public List<SourceScope> getSupportedSourceScopes() {
    return List.of(
        SourceScope.ALL_PRODUCTS, SourceScope.SPECIFIC_PRODUCTS, SourceScope.APPLICATION);
  }

  @Override
  protected void processProcessedData(
      Object processedData,
      BaseUPRequest request,
      DataBridgeConfig config,
      ResourceConnection connection) {
    try {
      String url = buildUrl(connection);
      HttpHeaders headers = buildHeaders(config);

      String payload = renderProcessedPayload(processedData);

      HttpEntity<String> entity = new HttpEntity<>(payload, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        log.debug("HTTP请求成功: status={}, body={}", response.getStatusCode(), response.getBody());
      } else {
        log.warn("HTTP请求失败: status={}, body={}", response.getStatusCode(), response.getBody());
      }

    } catch (Exception e) {
      log.error("HTTP数据处理失败: {}", e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void processTemplateResult(
      String templateResult,
      BaseUPRequest request,
      DataBridgeConfig config,
      ResourceConnection connection) {
    try {
      String url = buildUrl(connection);
      HttpHeaders headers = buildHeaders(config);

      HttpEntity<String> entity = new HttpEntity<>(templateResult, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        log.debug("HTTP请求成功: status={}, body={}", response.getStatusCode(), response.getBody());
      } else {
        log.warn("HTTP请求失败: status={}, body={}", response.getStatusCode(), response.getBody());
      }

    } catch (Exception e) {
      log.error("HTTP模板处理失败: {}", e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public Boolean testConnection(ResourceConnection connection) {
    try {
      String url = buildUrl(connection);
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<String> entity = new HttpEntity<>("{}", headers);
      ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

      return response.getStatusCode().is2xxSuccessful();
    } catch (Exception e) {
      log.error("HTTP连接测试失败: {}", e.getMessage(), e);
      return false;
    }
  }

  @Override
  public Boolean validateConfig(DataBridgeConfig config) {
    return validateMagicScript(config.getMagicScript());
  }

  private String buildUrl(ResourceConnection connection) {
    // 默认使用http协议，可以通过extraConfig配置SSL
    String protocol = "http";
    if (StrUtil.isNotBlank(connection.getExtraConfig())) {
      try {
        JSONObject extraConfig = JSONUtil.parseObj(connection.getExtraConfig());
        if (extraConfig.getBool("ssl", false)) {
          protocol = "https";
        }
      } catch (Exception e) {
        log.warn("解析SSL配置失败: {}", e.getMessage());
      }
    }

    String path = "/";
    if (StrUtil.isNotBlank(connection.getExtraConfig())) {
      try {
        JSONObject extraConfig = JSONUtil.parseObj(connection.getExtraConfig());
        path = extraConfig.getStr("path", "/");
      } catch (Exception e) {
        log.warn("解析路径配置失败: {}", e.getMessage());
      }
    }

    return String.format(
        "%s://%s:%d%s", protocol, connection.getHost(), connection.getPort(), path);
  }

  private HttpHeaders buildHeaders(DataBridgeConfig config) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    // 从统一配置中提取headers
    if (StrUtil.isNotBlank(config.getConfig())) {
      try {
        JSONObject configJson = JSONUtil.parseObj(config.getConfig());
        if (configJson.containsKey("headers")) {
          JSONObject headersJson = configJson.getJSONObject("headers");
          for (String key : headersJson.keySet()) {
            headers.set(key, headersJson.getStr(key));
          }
        }
      } catch (Exception e) {
        log.warn("解析HTTP配置失败: {}", e.getMessage());
      }
    }

    return headers;
  }

  private Map<String, Object> buildTemplateVariables(
      BaseUPRequest request, DataBridgeConfig config) {
    Map<String, Object> variables = super.buildTemplateVariables(request, parseConfig(config));
    variables.put("timestamp", System.currentTimeMillis());
    variables.putIfAbsent("deviceKey", variables.get("deviceId"));
    return variables;
  }
}
