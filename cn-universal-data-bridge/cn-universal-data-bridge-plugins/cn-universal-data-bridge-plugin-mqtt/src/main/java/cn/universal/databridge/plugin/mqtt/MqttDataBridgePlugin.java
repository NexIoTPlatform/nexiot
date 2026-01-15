package cn.universal.databridge.plugin.mqtt;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.PluginInfo;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.databridge.plugin.AbstractDataOutputPlugin;
import cn.universal.databridge.plugin.SourceScope;
import cn.universal.persistence.base.BaseUPRequest;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                SourceScope.ALL_PRODUCTS,
                SourceScope.SPECIFIC_PRODUCTS,
                SourceScope.APPLICATION
        );
    }

    @Override
    protected void processProcessedData(Object processedData, BaseUPRequest request, DataBridgeConfig config, ResourceConnection connection) {
        try (MqttClient client = createMqttClient(connection)) {
            client.connect(buildOptions(connection));

            String payload;
            if (processedData instanceof String) {
                payload = (String) processedData;
            } else {
                // 如果Magic脚本返回的不是字符串，使用模板处理
                Map<String, Object> variables = buildTemplateVariables(request, config);
                payload = processTemplate(config.getTemplate(), variables);
            }

            String topic = extractTopic(config, connection);
            client.publish(topic, new MqttMessage(payload.getBytes()));
            client.disconnect();

        } catch (Exception e) {
            log.error("MQTT数据处理失败: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void processTemplateResult(String templateResult, BaseUPRequest request, DataBridgeConfig config, ResourceConnection connection) {
        try (MqttClient client = createMqttClient(connection)) {
            client.connect(buildOptions(connection));

            String topic = extractTopic(config, connection);
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
            MqttConnectOptions options = buildOptions(connection);
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
        if (StringUtils.isBlank(config.getMagicScript())) {
            return StrUtil.isNotBlank(extractTopic(config, null));
        }
        return StrUtil.isNotBlank(extractTopic(config, null))
                && validateMagicScript(config.getMagicScript());
    }

    private MqttClient createMqttClient(ResourceConnection connection) throws Exception {
        String broker = String.format("tcp://%s:%d", connection.getHost(), connection.getPort());
        return new MqttClient(broker, MqttClient.generateClientId());
    }

    private MqttConnectOptions buildOptions(ResourceConnection connection) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);

        if (StrUtil.isNotBlank(connection.getUsername())) {
            options.setUserName(connection.getUsername());
        }
        if (StrUtil.isNotBlank(connection.getPassword())) {
            options.setPassword(connection.getPassword().toCharArray());
        }

        return options;
    }

    private String extractTopic(DataBridgeConfig config, ResourceConnection connection) {
        // 从统一配置中提取topic

        if (connection != null && StringUtils.isNotBlank(connection.getExtraConfig())) {
            String extraConfig = connection.getExtraConfig();
            try {
                JSONObject extraJson = JSONUtil.parseObj(extraConfig);
                //{"port":1883,"qos":"1","keepAlive":60,"cleanSession":true,"host":"192.168.2.2","username":"mqtt_client","password":"OKm7e4rZe36404E6","publishTopic":"$share/device/event"}
                String topic = extraJson.getStr("publishTopic", "/iot/bridge");
                if (StrUtil.isNotBlank(topic)) {
                    return topic;
                }
            } catch (Exception e) {
                log.warn("解析MQTT额外配置失败，使用桥接配置中的topic: {}", e.getMessage());
            }
        }
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

    private Map<String, Object> buildTemplateVariables(BaseUPRequest request, DataBridgeConfig config) {
        Map<String, Object> variables = new HashMap<>();

        // 设备信息
        if (request.getIoTDeviceDTO() != null) {
            variables.put("deviceKey", request.getIoTDeviceDTO().getDeviceId());
            variables.put("productKey", request.getIoTDeviceDTO().getProductKey());
        }

        // 消息信息
        variables.put("messageType", request.getMessageType().name());
        variables.put("timestamp", System.currentTimeMillis());
        variables.put("properties", request.getProperties());

        // 配置信息
        if (StrUtil.isNotBlank(config.getConfig())) {
            try {
                variables.put("configJson", JSONUtil.parseObj(config.getConfig()));
            } catch (Exception e) {
                log.warn("解析配置JSON失败: {}", e.getMessage());
            }
        }

        return variables;
    }
}