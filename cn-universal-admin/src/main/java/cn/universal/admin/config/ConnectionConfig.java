package cn.universal.admin.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 连接配置管理 复用现有配置，减少重复配置项 */
@Data
@Component
public class ConnectionConfig {

  @Value("${network.tcp.port-min:1}")
  private int tcpPortMin = 1;

  @Value("${network.tcp.port-max:65535}")
  private int tcpPortMax = 65535;

  @Value("${network.tcp.public-host:}")
  private String tcpPublicHost = "";

  // TCP配置 - 复用现有配置
  @Value("${tcp.protocol.sni.host.out:#productKey.tcp.nexiot.cc}")
  private String tcpSniHostTemplate;

  @Value("${tcp.protocol.sni.port.out:38883}")
  private Integer tcpSniPort;

  @Value("${connection.tcp.connection-type:TLS SNI方式}")
  private String tcpConnectionType;

  @Value("${connection.tcp.description:}")
  private String tcpDescription;

  // MQTT配置 - 复用现有配置
  @Value("${mqtt.cfg.host.out:tcp://localhost:1883}")
  private String mqttHostOut;

  @Value("${mqtt.cfg.host:tcp://localhost:1883}")
  private String mqttHost;

  @Value("${mqtt.cfg.clientIdPrefix:univ_iot_test}")
  private String mqttClientIdPrefix;

  @Value("${connection.mqtt.connection-type:内置MQTT Broker}")
  private String mqttConnectionType;

  @Value("${connection.mqtt.description:}")
  private String mqttDescription;

  // MQTT主题配置 - 直接在代码中定义默认值，避免Spring占位符解析问题
  @Value("${connection.mqtt.default-topics.thing-topics.property-up:}")
  private String mqttThingPropertyUp;

  @Value("${connection.mqtt.default-topics.thing-topics.event-up:}")
  private String mqttThingEventUp;

  @Value("${connection.mqtt.default-topics.thing-topics.command-down:}")
  private String mqttThingCommandDown;

  @Value("${connection.mqtt.default-topics.passthrough-topics.data-up:}")
  private String mqttPassthroughDataUp;

  @Value("${connection.mqtt.default-topics.passthrough-topics.command-down:}")
  private String mqttPassthroughCommandDown;

  // 默认主题模板常量
  private static final String DEFAULT_THING_PROPERTY_UP =
      "$thing/up/property/{productKey}/{deviceId}";
  private static final String DEFAULT_THING_EVENT_UP = "$thing/up/event/{productKey}/{deviceId}";
  private static final String DEFAULT_THING_COMMAND_DOWN = "$thing/down/{productKey}/{deviceId}";
  private static final String DEFAULT_PASSTHROUGH_DATA_UP = "$thing/up/{productKey}/{deviceId}";
  private static final String DEFAULT_PASSTHROUGH_COMMAND_DOWN =
      "$thing/down/{productKey}/{deviceId}";

  // UDP配置
  @Value("${connection.udp.connection-type:}")
  private String udpConnectionType;

  @Value("${connection.udp.description:}")
  private String udpDescription;

  @Value("${udp.cfg.port:1884}")
  private Integer udpPort;

  /** 获取TCP主机地址 */
  public String getTcpHost(String productKey) {
    return tcpSniHostTemplate.replace("#productKey", productKey);
  }

  /** 获取MQTT主机地址（优先使用外部配置） */
  public String getMqttHost() {
    return mqttHostOut != null && !mqttHostOut.isEmpty() ? mqttHostOut : mqttHost;
  }

  /** 从主机地址中提取端口 */
  public Integer extractPortFromHost(String host) {
    if (host == null || host.isEmpty()) {
      return null;
    }

    if (host.contains(":")) {
      String[] parts = host.split(":");
      if (parts.length >= 2) {
        try {
          return Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
          // 忽略解析错误
        }
      }
    }

    return null;
  }

  /** 获取MQTT物模型属性上报主题 */
  public String getMqttThingPropertyUp() {
    return mqttThingPropertyUp.isEmpty() ? DEFAULT_THING_PROPERTY_UP : mqttThingPropertyUp;
  }

  /** 获取MQTT物模型事件上报主题 */
  public String getMqttThingEventUp() {
    return mqttThingEventUp.isEmpty() ? DEFAULT_THING_EVENT_UP : mqttThingEventUp;
  }

  /** 获取MQTT物模型指令下发主题 */
  public String getMqttThingCommandDown() {
    return mqttThingCommandDown.isEmpty() ? DEFAULT_THING_COMMAND_DOWN : mqttThingCommandDown;
  }

  /** 获取MQTT透传数据上报主题 */
  public String getMqttPassthroughDataUp() {
    return mqttPassthroughDataUp.isEmpty() ? DEFAULT_PASSTHROUGH_DATA_UP : mqttPassthroughDataUp;
  }

  /** 获取MQTT透传指令下发主题 */
  public String getMqttPassthroughCommandDown() {
    return mqttPassthroughCommandDown.isEmpty()
        ? DEFAULT_PASSTHROUGH_COMMAND_DOWN
        : mqttPassthroughCommandDown;
  }
}
