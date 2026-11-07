/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.common.constant;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** IoT常量 */
public interface IoTConstant {

  public static final int ZERO_FALSE = 0;
  public static final int ONE_TRUE = 1;
  // nexiot
  String SALT = "fa71207f20e2c2cd7151c578304190f5";
  // 如果开启了主动注册，但是没有写编解码或者返回为null，那么把消息丢给设备默认设备Id,便于调试
  String NEXIOT_DEBUG_DEVICE_ID = "nexiotDebugDeviceId";
  /* 设置物模型期望属性 */
  String SET_DEVICE_DESIRED_PROPERTIES = "setDeviceDesiredProperties";
  /* 下行 payload 来源: 经过业务编解码 */
  String PAYLOAD_SOURCE_CODEC = "codec";
  /* 下行 payload 来源: 未命中业务编解码，使用原始 JSON 兜底 */
  String PAYLOAD_SOURCE_RAW_FALLBACK = "raw-fallback";
  /* 下行 payload 来源: 仅更新平台影子/期望值，不向设备继续下发 */
  String PAYLOAD_SOURCE_SHADOW_ONLY = "shadow-only";
  /* OTA 升级，作为平台内置标准功能下发 */
  String OTA_UPGRADE = "otaUpgrade";
  /*发送直通第三方平台*/
  String DOWN_TO_THIRD_PLATFORM = "downToThirdPlatform";
  String CERT_DEFAULT_KEY = "default-tcp";
  String FENCE_DELAY_SIGN = "fenceDelaySign";
  String FENCE_TRIGGER_SIGN = "fenceTriggerSign";

  String EXCLUSIVE_FIRST_LOGIN = "exclusiveFirstLogin";
  String EXCLUSIVE_LOGIN = "exclusiveLogin";
  String EXCLUSIVE_LOGIN_TOKEN = "exclusiveLoginToken";
  String MAGIC_REDIS_SIGN = "magicRedisSign:";
  // Redis Key 常量
  String TCP_DEVICE_ROUTES_KEY = "tcp:device:routes";
  String TCP_INSTANCE_DEVICES_KEY_PREFIX = "tcp:instance:devices:";
  String UDP_DEVICE_ROUTES_KEY = "udp:device:routes";
  String UDP_INSTANCE_DEVICES_KEY_PREFIX = "udp:instance:devices:";
  String DEVICE_ROUTES_KEY = TCP_DEVICE_ROUTES_KEY;
  String INSTANCE_DEVICES_KEY_PREFIX = TCP_INSTANCE_DEVICES_KEY_PREFIX;
  String TCP_ERROR_MONITOR = "tcpErrorMonitor";
  Pattern pattern = Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[^a-zA-Z0-9]).{8,30}$");

  /** REDIS KEY */
  String REDIS_STORE_COMMAND = "storeCommand";

  String REDIS_REPLY_COMMAND = "replyCommand";

  /** 是否是定位设备 */
  String IS_GPS_PRODUCT = "isGps";

  /** 不存在时是否新增 */
  String ALLOW_INSERT = "allowInsert";

  /**
   * customized
   *
   * <p>设备公共字段，下发可能要用到的，deivceId,imei,meterNo
   */
  String DEVICE_SHADOW_CUSTOMIZED_PROPERTY = "device_common_property";

  String DEVICE_SHADOW_DESIRED_PROPERTY = "device_desired_property";

  /** 默认离线阈值1440分钟（24小时） */
  int DEFAULT_OFFLINE_THRESHOLD_VALUE = 24 * 60;

  String DEFAULT_OFFLINE_THRESHOLD = "offlineThreshold";

  /** 日志最大存储时间，单位天 */
  int DEFAULT_LOG_MAX_STORAGE_TIME = 100;

  String DEFAULT_LOG_MAX_STORAGE = "logMaxStorage";
  Integer NORMAL = 0;
  Integer UN_NORMAL = 1;

  /***
   * 设备订阅最大数量
   */
  int MAX_DEV_MSG_SUBSCRIBE_NUM = 5;

  /** http请求超时时间 */
  Integer HTTP_TIME_OUT = 8000;

  String HTTP_HEADER_APP = "NEXIOT_INNER";
  String HTTP_HEADER_AUTH = "NEXIOT_INNER_AUTH";
  String HTTP_HEADER_TIME = "NEXIOT_INNER_TIME";

  /** 不解析，直接透传 */
  String DATA_PASS_THROUGH = "passThrough";

  /** 中台上行带上原始串 */
  String REQUIRE_PAYLOAD = "requirePayload";

  /** 日志写入指定数据库时，是否默认双写一份到MySQL；布尔类型，默认值false（关闭） */
  String ENABLE_MYSQL_WRITE = "enableMysqlWrite";

  String MYSQL = "mysql";
  String TIMESCALEDB = "timescaledb";

  /** iot_device_log_metadata单属性最大存储10条 */
  Integer maxStorage = 10;

  /** 超时120s */
  Integer HTTP_AUTH_TIMEOUT = 120;

  /** UTC时区+8 */
  String HTTP_UTC_8 = "+8";

  String TRACE_ID = "traceId";

  String PRODUCT_METADATA =
      """
      {"events":[{"id":"online","name":"上线","valueType":{"type":"string"}},{"id":"offline","name":"下线","valueType":{"type":"string"}}],"functions":[{"id":"setDeviceDesiredProperties","name":"设置期望值","config":true,"inputs":[],"output":{},"description":"对所有可写的属性设置期望值，可用于标签、位置、个性化扩展使用"},{"id":"otaUpgrade","name":"OTA升级","config":false,"inputs":[],"output":{},"description":"平台内置 OTA 标准功能。设备根据 data 中的升级元数据自行下载、校验并安装升级包"}],"properties":[],"tags":[]}""";

  enum ProtocolModule {
    ctaiot,
    ezviz,
    onenet,
    imoulife,
    dahua_cloud,
    tcp,
    udp,
    mqtt,
    modbus,
    http,
    wvp,
    isc,
    icc
  }

  /** 非设备真实上报事件 */
  Set<String> notDeviceDataReportEvent =
      Stream.of("create", "offline", "enable", "disable", "delete", "update")
          .collect(Collectors.toCollection(HashSet::new));

  /** 标签类型 */
  enum DevLifeCycle {
    /** 创建 */
    create("设备创建"),
    /** 上线 */
    online("设备上线"),

    /** 离线 */
    offline("设备离线"),
    /** 上下线 */
    onOffline("设备上下线"),

    /** 启用 */
    enable("设备启用"),

    /** 离线 */
    disable("设备停用"),

    /** 删除 */
    delete("设备删除"),
    /** 修改 */
    update("设备修改");

    private String value;

    DevLifeCycle(String value) {
      this.value = value;
    }

    @JsonCreator
    public static DevLifeCycle find(String value) {
      return Stream.of(values())
          .filter(e -> e.getValue().equalsIgnoreCase(value))
          .findFirst()
          .orElse(null);
    }

    @JsonValue
    public String getValue() {
      return this.value;
    }
  }

  /** 上下线枚举，保持物模型一致，使用小写 */
  enum DeviceStatus {
    online(true),
    offline(false);

    private boolean code;

    private DeviceStatus(boolean code) {
      this.code = code;
    }

    public boolean getCode() {
      return code;
    }
  }

  /** 下行指令 - 重新设计为更通俗易懂的指令 */
  enum DownCmd {
    // 设备管理指令
    DEVICE_ADD("设备添加"),
    DEVICE_DELETE("设备删除"),
    DEVICE_UPDATE("设备更新"),
    DEVICE_INFO("设备信息查询"),
    DEVICE_STATUS("设备状态查询"),
    DEVICE_ONLINE_CHECK("设备在线检查"),

    // 摄像头控制指令
    CAMERA_TURN("摄像头转动"),
    CAMERA_PTZ_CONTROL("云台控制"),
    CAMERA_SNAPSHOT("摄像头截图"),
    CAMERA_LIVE_STREAM("摄像头直播"),
    CAMERA_PLAYBACK("摄像头回放"),
    CAMERA_RECORD_START("开始录像"),
    CAMERA_RECORD_STOP("停止录像"),
    CAMERA_FLIP_SET("画面翻转设置"),

    // 存储相关物模型函数
    STORAGE_LOCAL_STREAM_SET_FUNC("storageLocalStreamSet"),
    STORAGE_LOCAL_STREAM_QUERY_FUNC("storageLocalStreamQuery"),
    STORAGE_LOCAL_PLAN_SET_FUNC("storageLocalPlanSet"),
    STORAGE_LOCAL_PLAN_QUERY_FUNC("storageLocalPlanQuery"),
    STORAGE_CLOUD_RECORDS_QUERY_FUNC("storageCloudRecordsQuery"),
    STORAGE_CLOUD_UNUSED_LIST_FUNC("storageCloudUnusedList"),
    STORAGE_CLOUD_CALL_COUNT_QUERY_FUNC("storageCloudCallCountQuery"),
    STORAGE_CLOUD_SERVICE_SET_FUNC("storageCloudServiceSet"),
    STORAGE_CLOUD_LIST_QUERY_FUNC("storageCloudListQuery"),
    STORAGE_CLOUD_UNBIND_FUNC("storageCloudUnbind"),
    STORAGE_CLOUD_OPEN_FUNC("storageCloudOpen"),
    STORAGE_FREE_CLOUD_SET_FUNC("storageFreeCloudSet"),
    STORAGE_SDCARD_FORMAT_FUNC("storageSdcardFormat"),
    STORAGE_SDCARD_INFO_GET_FUNC("storageSdcardInfoGet"),
    STORAGE_SDCARD_STATUS_GET_FUNC("storageSdcardStatusGet"),
    STORAGE_CLOUD_VIDEO_COUNT_QUERY_FUNC("storageCloudVideoCountQuery"),
    STORAGE_LOCAL_VIDEO_COUNT_QUERY_FUNC("storageLocalVideoCountQuery"),
    VIDEO_DOWNLOAD_FUNC("videoDownload"),

    // 设备相关物模型函数
    DEVICE_SOUND_VOLUME_GET_FUNC("deviceSoundVolumeGet"),
    DEVICE_SOUND_VOLUME_SET_FUNC("deviceSoundVolumeSet"),
    DEVICE_CAMERA_STATUS_GET_FUNC("deviceCameraStatusGet"),
    DEVICE_WIFI_SET_FUNC("deviceWifiSet"),
    DEVICE_WIFI_SCAN_FUNC("deviceWifiScan"),
    DEVICE_UPGRADE_FUNC("deviceUpgrade"),
    DEVICE_VERSION_QUERY_FUNC("deviceVersionQuery"),
    DEVICE_CLOUD_INFO_GET_FUNC("deviceCloudInfoGet"),
    DEVICE_ENABLE_SET_FUNC("deviceEnableSet"),
    DEVICE_RESTART_FUNC("deviceRestart"),

    // 报警相关物模型函数
    ALARM_MESSAGE_QUERY_FUNC("alarmMessageQuery"),

    // 存储管理指令
    STORAGE_INFO("存储信息查询"),
    STORAGE_FORMAT("存储格式化"),
    STORAGE_CLOUD_ENABLE("云存储开启"),
    STORAGE_CLOUD_DISABLE("云存储关闭"),
    STORAGE_LOCAL_ENABLE("本地存储开启"),
    STORAGE_LOCAL_DISABLE("本地存储关闭"),
    STORAGE_LOCAL_STREAM_SET("设置本地录像视频流"),
    STORAGE_LOCAL_STREAM_QUERY("查询本地录像视频流"),
    STORAGE_LOCAL_PLAN_SET("设置本地录像计划"),
    STORAGE_LOCAL_PLAN_QUERY("查询本地录像计划"),
    STORAGE_CLOUD_RECORDS_QUERY("查询云录像片段"),
    STORAGE_CLOUD_UNUSED_LIST("获取未启用的云存储服务"),
    STORAGE_CLOUD_CALL_COUNT_QUERY("查询云存储开通接口剩余调用次数"),
    STORAGE_CLOUD_SERVICE_SET("设置云存储服务开关"),
    STORAGE_CLOUD_LIST_QUERY("查询设备云存储服务"),
    STORAGE_CLOUD_UNBIND("解绑设备云存储"),
    STORAGE_CLOUD_OPEN("开通设备云存储"),
    STORAGE_FREE_CLOUD_SET("设置免费云存储服务"),
    STORAGE_SDCARD_FORMAT("格式化SD卡"),
    STORAGE_SDCARD_INFO_GET("获取SD卡信息"),
    STORAGE_SDCARD_STATUS_GET("获取SD卡状态"),
    STORAGE_CLOUD_VIDEO_COUNT_QUERY("查询云录像数量"),
    STORAGE_LOCAL_VIDEO_COUNT_QUERY("查询本地录像数量"),
    VIDEO_DOWNLOAD("下载录像"),

    // 设备设置指令
    DEVICE_RESTART("设备重启"),
    DEVICE_UPGRADE("设备升级"),
    DEVICE_WIFI_SET("WiFi设置"),
    DEVICE_WIFI_SCAN("WiFi扫描"),
    DEVICE_SOUND_VOLUME_GET("获取设备音量"),
    DEVICE_SOUND_VOLUME_SET("设置设备音量"),
    DEVICE_CAMERA_STATUS_GET("获取摄像头状态"),
    DEVICE_VERSION_QUERY("查询设备版本信息"),
    DEVICE_CLOUD_INFO_GET("获取设备云存储信息"),
    DEVICE_ENABLE_SET("设置设备使能开关"),

    // 报警管理指令
    ALARM_MESSAGE_QUERY("查询报警消息"),

    // 产品管理指令
    PRODUCT_ADD("产品添加"),
    PRODUCT_UPDATE("产品更新"),
    PRODUCT_DELETE("产品删除"),
    PRODUCT_PUBLISH("产品发布"),

    // 兼容旧版本指令（保留）
    DEV_ADD,
    DEV_ADDS,
    DEV_FUNCTION,
    DEV_DEL,
    DEV_UPDATE,
    DEV_MONITOR_TURN,
    MANUAL_CAPTURE,
    DEV_CONTROLLING,
    DEV_MONITOR_PLAY,
    DEV_ELECTRIC_QUANTITY,
    DEV_DOOR_KEYS,
    DEV_OPENDOOR_RECORD,
    DEV_GENERATE_SNAPKEY,
    DEV_DELETE_DOORKEY,
    DEV_SNAPKEY_LIST,
    DEV_WAKE_UP,
    DEV_LIVE,
    DEV_LIVE_STREAMINFO,
    DEV_SETTING,
    DEV_PLAY_BACK,
    DEV_PLAY_BACKCLOUD,
    DEV_VIDEO_LOCAL,
    DEV_VIDEO_CLOUD,
    DEV_SNAP,
    PRO_ADD,
    PRO_UPDATE,
    PRO_DEL,
    PUBPRO_GET,
    PUBPRO_ADD,
    DEV_SDCARD_STATUS,
    DEV_SDCARD,
    RECOVER_SDCARD,
    CAPTURE,
    GET_DEVICE_CLOUD,
    CREATR_DEVICE_RTMP,
    CREATR_DEVICE_FLV,
    INTERCOMBYIDS,
    QUERY_DEVICE_FLV,
    LIST_DEVICE_DETAILS_ID,
    SET_NIGHT_VISION_MODE,
    UP_GRADE_PROCESS_DEVICE,
    SET_LOCAL_RECORD_STREAM,
    QUERY_LOCAL_RECORD_STREAM,
    SET_LOCAL_RECORD_PLAN_RULES,
    GUERY_LOCAL_RECORD_PLAN,
    GET_CLOUD_RECORDS,
    GET_DOWN_LOAD_LIST,
    UN_USED_CLOUD_LIST,
    QUERY_CLOUD_RECORD_CALL_NUM,
    SET_ALL_STORAGE_STRATEGY,
    DEVICE_CLOUD_LIST,
    UN_BIND_DEVICE_CLOUD,
    DOWNLOAD_RECORD,
    OPEN_CLOUD_RECORD,
    SET_STORAGE_STRATEGY,
    FRAME_REVERSE_STATUS,
    SOUND_VOLUME_SIZE_GET,
    CAMERA_STATUS,
    CONTROL_DEVICE_WIFI,
    WIFI_AROUND,
    UPGRADE_DEVICE,
    DEVICE_VERSIONLIST,
    DEVICE_CLOUD,
    ENABLE_CONFIG,
    REVERSE_STATUS,
    RESTART_DEVICE,
    SOUND_VOLUME_SIZE,
    ALARM_MESSAGE,
    DEV_MONITOR_CHECK_ONLINE;

    private String description;

    DownCmd() {
      this.description = super.toString().toLowerCase();
    }

    DownCmd(String description) {
      this.description = description;
    }

    @JsonCreator
    public static DownCmd find(String value) {
      return Stream.of(values())
          .filter(e -> e.getValue().equalsIgnoreCase(value))
          .findFirst()
          .orElse(null);
    }

    @JsonValue
    public String getValue() {
      return super.toString().toLowerCase();
    }

    public String getDescription() {
      return description;
    }
  }

  /** 设备订阅 */
  enum DeviceSubscribe {
    PRODUCT,

    DEVICE;

    private DeviceSubscribe() {}

    @JsonCreator
    public static DeviceSubscribe find(String value) {
      return Stream.of(values())
          .filter(e -> e.getValue().equalsIgnoreCase(value))
          .findFirst()
          .orElse(null);
    }

    @JsonValue
    public String getValue() {
      return super.toString().toLowerCase();
    }
  }

  /** 设备节点，网关，直连，网关子设备 */
  enum DeviceNode {
    /** 直连 */
    DEVICE,
    /** 网关 */
    GATEWAY,
    /** 网关子设备 */
    GATEWAY_SUB_DEVICE,
    /** 视频平台实例作为网关 */
    VIDEO_GATEWAY,
    /** 物理视频设备（IPC/NVR） */
    VIDEO_DEVICE,
    /** 视频通道（GB28181通道/设备通道） */
    VIDEO_CHANNEL,
    /** 非视频传感器设备（门禁、一卡通等） */
    SENSOR_DEVICE;

    private DeviceNode() {}

    @JsonCreator
    public static DeviceNode find(String value) {
      return Stream.of(values())
          .filter(e -> e.getValue().equalsIgnoreCase(value))
          .findFirst()
          .orElse(null);
    }

    @JsonValue
    public String getValue() {
      return this.name();
    }
  }

  /** 消息类型 */
  enum MessageType {
    ALL,
    /** 属性上报 - 设备主动上报属性数据 (设备→平台) */
    PROPERTIES,
    /** 事件上报 - 设备主动上报事件 (设备→平台) */
    EVENT,
    /** 服务调用 - 平台主动下发指令/服务 (平台→设备) */
    FUNCTIONS,
    /** 指令响应 - 设备回复平台下发的指令 (设备→平台, 响应FUNCTIONS) */
    REPLY,
    /** 上报应答 - 平台应答设备的上报消息 (平台→设备, 如JT808协议确认) */
    ACK;

    private MessageType() {}

    @JsonCreator
    public static MessageType find(String value) {
      return Stream.of(values())
          .filter(e -> e.getValue().equalsIgnoreCase(value))
          .findFirst()
          .orElse(MessageType.PROPERTIES);
    }

    public static boolean devReallyReport(MessageType messageType) {
      return MessageType.EVENT.equals(messageType)
          || MessageType.PROPERTIES.equals(messageType)
          || MessageType.REPLY.equals(messageType);
    }

    @JsonValue
    public String getValue() {
      return super.toString().toLowerCase();
    }
  }

  /** 事件类型 */
  public static enum EventType {
    ;

    private EventType() {}

    @JsonCreator
    public static EventType find(String value) {
      return Stream.of(values())
          .filter(e -> e.getValue().equalsIgnoreCase(value))
          .findFirst()
          .orElse(null);
    }

    @JsonValue
    public String getValue() {
      return super.toString().toLowerCase();
    }
  }

  enum ERROR_CODE {
    /** Lora终端不存在 */
    DATA_NOT_FIND(700, "数据不存在"),
    DEV_NOT_FIND(700, "设备不存在"),
    DEV_NOT_FOR_YOU(701, "设备没有操作权限！"),
    DEV_ADD_DEVICE_ID_EXIST(702, "deivceId已存在"),
    DEV_DEL_DEVICE_NO_ID_EXIST(702, "deivceId不存在"),
    DEV_DEL_ERROR(703, "设备删除错误"),
    DEV_UPDATE_ERROR(704, "设备修改错误"),
    DEV_UPDATE_DEVICE_NO_ID_EXIST(705, "deviceId不存在"),
    DEV_CONFIG_DEVICE_NO_ID_EXIST(706, "deviceId不存在"),
    DEV_CONFIG_DEVICE_PARA_NULL(707, "参数为空"),
    DEV_CONFIG_DEVICE_PARA_MISS(765, "缺少参数"),
    DEV_CONFIG_DEVICE_LEVEL_ERROR(708, "level不合法"),
    DEV_CONFIG_DEVICE_STATE_ERROR(709, "设备未激活"),
    DEV_CONFIG_DEVICE_PARA_ERROR(710, "参数校验失败"),
    DEV_CONFIG_DEVICE_MASTERKEY_ERROR(711, "MasterKey不匹配"),
    DEV_CONFIG_DEVICE_MESSAGE_ERROR(712, "指令应为偶数"),
    DEV_CONFIG_DEVICE_NULL(713, "下发指令不能为空"),
    DEV_CONFIG_PAYLOAD_NULL(714, "下发报文(payload)不能为空"),
    DEV_CONFIG_ERROR(715, "设备配置错误"),
    DEV_DOWN_ADD_ERROR(717, "指令 cmd 配置错误(指令 cmd 不能为空)"),
    DEV_PARA_RANGE_ERROR(721, "指令下发内容与服务模型参数范围不匹配"),
    DEV_DOWN_CMD_ERROR(715, "下发指令错误"),
    DEV_CONFIG_DEVICE_PARA_FAIL(716, "参数解析失败"),
    DEV_B_SERVICE_EXIST(720, "B端设备不能添加到C端"),
    DEV_DOWN_NOT_SUPPORT(721, "设备不支持功能下发"),
    DEV_DOWN_NO_MQTT_TOPIC(723, "mqtt接入，产品`端云配置`未配置指令下行主题项"),
    DEV_METADATA_NOT_FIND(750, "物模型不存在"),
    DEV_METADATA_FUNCTION_NOT_FIND(751, "物模型定义功能不存在"),
    DEV_ADD_ERROR(799, "设备添加错误,请检查参数"),
    DEV_WAIT_ADD(754, "存在等待回调设备，请勿重复操作"),
    HTTP_TIME_OUT(788, "请求超时"),
    WVP_DOWNLOAD_LIMIT(789, "同时下载数量到达上限"),
    WVP_THIRD_APPLICATION_NOT_EXIST(790, "wvp第三方平台不存在");

    //    DEV_ADD_LZ_MODEL_NOT_EXIST(701, "Lora终端不存在");

    private Integer code;
    private String name;

    private ERROR_CODE(Integer code, String name) {
      this.code = code;
      this.name = name;
    }

    public Integer getCode() {
      return code;
    }

    public String getName() {
      return name;
    }
  }

  public static enum TcpFlushType {
    start,
    reload,
    close
  }

  public static enum ProductFlushType {
    script,
    server,
    client,
    delete
  }

  enum NetwotkType {
    MQTT_CLIENT("MQTT_CLIENT", "mqtt客户端"),
    TCP_CLIENT("TCP_CLIENT", "tcp客户端"),
    HTTP_CLIENT("HTTP_CLIENT", "http客户端"),
    TCP_SERVER("TCP_SERVER", "tcp服务端"),
    WEB_SOCKET_CLIENT("WEB_SOCKET_CLIENT", "socket客户端");
    private String type;
    private String name;

    private NetwotkType(String type, String name) {
      this.type = type;
      this.name = name;
    }

    public String getType() {
      return this.type;
    }

    public String getName() {
      return this.name;
    }

    public static String findType(String type) {
      NetwotkType t =
          Stream.of(values())
              .filter(netwotkType -> netwotkType.getType().equals(type))
              .findFirst()
              .orElse(null);
      if (ObjectUtil.isNull(t)) {
        return "该网络类型不存在";
      }
      return t.getName();
    }
  }
}
