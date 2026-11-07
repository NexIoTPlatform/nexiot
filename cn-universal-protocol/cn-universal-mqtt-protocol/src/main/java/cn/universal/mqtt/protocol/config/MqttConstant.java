package cn.universal.mqtt.protocol.config;

import java.util.regex.Pattern;

/** MQTT 主题常量统一定义 */
public interface MqttConstant {

  // ================= 固定前缀 =================
  String TOPIC_THING_PREFIX = "$thing";
  String TOPIC_OTA_PREFIX = "$ota";

  // ================= 动态前缀正则 =================
  String DYNAMIC_PREFIX_REGEX = "\\$[^/]+";

  // ================= 物模型 =================
  // 格式字符串
  String THING_PROPERTY_UP_FORMAT = "%s/up/property/%s/%s"; // 前缀, productKey, deviceId
  String THING_EVENT_UP_FORMAT = "%s/up/event/%s/%s";
  String THING_DOWN_FORMAT = "%s/down/%s/%s";
  // 正则
  Pattern THING_PROPERTY_UP_PATTERN =
      Pattern.compile(DYNAMIC_PREFIX_REGEX + "/up/property/([^/]+)/([^/]+)");
  Pattern THING_EVENT_UP_PATTERN =
      Pattern.compile(DYNAMIC_PREFIX_REGEX + "/up/event/([^/]+)/([^/]+)");
  Pattern THING_DOWN_PATTERN = Pattern.compile(DYNAMIC_PREFIX_REGEX + "/down/([^/]+)/([^/]+)");

  // ================= 系统级 =================
  // 格式字符串
  String OTA_REPORT_FORMAT = "%s/report/%s/%s";
  String OTA_UPDATE_FORMAT = "%s/update/%s/%s";
  // 正则
  Pattern OTA_REPORT_PATTERN = Pattern.compile(DYNAMIC_PREFIX_REGEX + "/report/([^/]+)/([^/]+)");
  Pattern OTA_UPDATE_PATTERN = Pattern.compile(DYNAMIC_PREFIX_REGEX + "/update/([^/]+)/([^/]+)");

  // ================= 透传 =================
  // 格式字符串
  String PASSTHROUGH_UP_FORMAT = "%s/up/%s/%s";
  String PASSTHROUGH_DOWN_FORMAT = "%s/down/%s/%s";
  // 正则
  Pattern PASSTHROUGH_UP_PATTERN = Pattern.compile(DYNAMIC_PREFIX_REGEX + "/up/([^/]+)/([^/]+)");
  Pattern PASSTHROUGH_DOWN_PATTERN =
      Pattern.compile(DYNAMIC_PREFIX_REGEX + "/down/([^/]+)/([^/]+)");

  // ================= 主题类型字符串常量 =================
  String TYPE_THING_PROPERTY_UP = "THING_PROPERTY_UP";
  String TYPE_THING_EVENT_UP = "THING_EVENT_UP";
  String TYPE_DOWN = "THING_DOWN";
  String TYPE_PASSTHROUGH_UP = "PASSTHROUGH_UP";

  /** 主题分类枚举 */
  enum TopicCategory {
    /** 物模型类别 */
    THING_MODEL,

    /** 透传类别 */
    PASSTHROUGH,

    /** 系统级类别 */
    SYSTEM_LEVEL,

    /** 未知类别 */
    UNKNOWN
  }
}
