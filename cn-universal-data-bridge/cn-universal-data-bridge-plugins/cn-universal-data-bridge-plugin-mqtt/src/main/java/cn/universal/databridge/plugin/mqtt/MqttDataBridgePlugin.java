package cn.universal.databridge.plugin.mqtt;

import cn.hutool.core.util.IdUtil;
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
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("mqtt")
@ConditionalOnProperty(
    prefix = "databridge.plugins.mqtt",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@Slf4j
public class MqttDataBridgePlugin extends AbstractDataOutputPlugin {

  @Override
  public PluginInfo getPluginInfo() {
    return PluginInfo.builder()
        .name("MQTT数据桥接插件")
        .version("1.0.0")
        .description("支持MQTT消息桥接")
        .author("gitee.com/NexIoT")
        .pluginType("MQTT")
        .supportedResourceTypes(List.of("MQTT"))
        .dataDirection(PluginInfo.DataDirection.BIDIRECTIONAL)
        .category("消息队列")
        .icon("message")
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
      org.eclipse.paho.mqttv5.client.MqttClient client = createMqttClient(connection);
      client.connect(buildOptions(connection));

      String payload = renderProcessedPayload(processedData);

      String topic = extractTopic(config);
      client.publish(topic, new MqttMessage(payload.getBytes()));
      client.disconnect();

    } catch (Exception e) {
      log.error("MQTT数据处理失败: {}", e.getMessage(), e);
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
      MqttClient client = createMqttClient(connection);
      client.connect(buildOptions(connection));
      String topic = extractTopic(config);
      client.publish(topic, new MqttMessage(templateResult.getBytes()));
      client.disconnect();

    } catch (Exception e) {
      log.error("MQTT模板处理失败: {}", e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public Boolean testConnection(ResourceConnection connection) {
    try {
      MqttClient client = createMqttClient(connection);
      MqttConnectionOptions options = buildOptions(connection);
      client.connect(options);
      client.disconnect();
      client.close();
      return true;
    } catch (Exception e) {
      log.error("MQTT连接测试失败: {}", e.getMessage(), e);
      return false;
    }
  }

  @Override
  public Boolean validateConfig(DataBridgeConfig config) {
    return StrUtil.isNotBlank(extractTopic(config)) && validateMagicScript(config.getMagicScript());
  }

  private org.eclipse.paho.mqttv5.client.MqttClient createMqttClient(ResourceConnection connection)
      throws Exception {
    String broker = String.format("tcp://%s:%d", connection.getHost(), connection.getPort());
    return new MqttClient(broker, IdUtil.objectId());
  }

  private MqttConnectionOptions buildOptions(ResourceConnection connection) {
    MqttConnectionOptions options = new MqttConnectionOptions();
    options.setCleanStart(true);
    options.setConnectionTimeout(30);
    options.setKeepAliveInterval(60);

    if (StrUtil.isNotBlank(connection.getUsername())) {
      options.setUserName(connection.getUsername());
    }
    if (StrUtil.isNotBlank(connection.getPassword())) {
      options.setPassword(connection.getPassword().getBytes());
    }

    return options;
  }

  private String extractTopic(DataBridgeConfig config) {
    // 从统一配置中提取topic
    if (StrUtil.isBlank(config.getConfig())) {
      return "/iot/bridge"; // 默认topic
    }

    try {
      JSONObject configJson = JSONUtil.parseObj(config.getConfig());
      return configJson.getStr("topic", "/iot/bridge");
    } catch (Exception e) {
      log.warn("解析MQTT配置失败，使用默认topic: {}", e.getMessage());
      return "/iot/bridge";
    }
  }

  private Map<String, Object> buildTemplateVariables(
      BaseUPRequest request, DataBridgeConfig config) {
    Map<String, Object> variables = super.buildTemplateVariables(request, parseConfig(config));
    variables.put("timestamp", System.currentTimeMillis());
    variables.putIfAbsent("deviceKey", variables.get("deviceId"));
    return variables;
  }
}
