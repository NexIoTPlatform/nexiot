package cn.universal.admin.video.alarm;

import java.util.List;
import java.util.Map;
import lombok.Data;

/** 原生 SDK 网关解码后的回调载荷；租户和平台归属由 instanceKey 在服务端确认。 */
@Data
public class SdkVideoAlarmCallbackRequest {

  /** 必填：NexIoT 平台实例标识，同时作为直连 SDK 产品 key；不是网关进程 ID。 */
  private String instanceKey;

  /** 网关声明的平台类型（含 hik_isup），实际路由以数据库中的实例类型为准。 */
  private String platformType;

  /** 厂商展示标识，仅透传展示，不参与鉴权或租户判断。 */
  private String vendor;

  /** 实例内的平台设备标识，优先用于查找已同步设备；不是 IoT 的 iotId。 */
  private String deviceId;

  /** 网关提供的设备名称；为空时使用已同步设备名称。 */
  private String deviceName;

  /** 兼容仅回报 IP 的 SDK；仅在 deviceId 未命中且实例内 IP 唯一时回退。 */
  private String deviceIp;

  /** 厂商通道标识，按字符串原样保留；不在回调层转换 0/1 起始编号。 */
  private String channelId;

  /** 可选的厂商通道名称。 */
  private String channelName;

  /** 网关解码后的业务类型，仅用于无编号旧回调兼容；新回调优先按 SDK 原始编号匹配。 */
  private String alarmType;

  /** 厂商事件名，如 ISUP ISAPI 的 VMD；用于独立 ISUP 语义解析，原值保留供追溯。 */
  private String cameraEventType;

  /** 厂商告警名称，保留在事件数据；物模型 eventName 由字典决定。 */
  private String alarmName;

  /** start=触发，stop=恢复，pulse=瞬时，unknown=未知；保留网关原值。 */
  private String alarmState;

  /** 厂商告警等级，字符串透传，各 SDK 的等级编码可能不同。 */
  private String alarmLevel;

  /** 可选的告警描述。 */
  private String description;

  /** Unix 时间戳，单位毫秒；缺失或非正数时使用服务端接收时间。 */
  private Long alarmTime;

  /** 网关稳定事件追踪 ID，不是物模型事件标识；ISUP 收据按此标识及阶段/证据版本去重。 */
  private String eventId;

  /** 原始事件关联标识；由网关解析，不能依赖已被 XSS 过滤的 XML 原文恢复。 */
  private String sourceEventId;

  /** 取证更新对应的原始事件类型，供现有订阅类型过滤使用。 */
  private String sourceEventType;

  /** 网关已归档的图片引用，不在回调中接收二进制图片。data 中同名字段优先。 */
  private List<Map<String, Object>> images;

  /** 兼容图片引用数组及现有 SDK 录像取证对象，原值经凭证过滤后保存。 */
  private Object evidence;

  private String imageStatus;
  private Integer pictureCount;
  private Integer activePostCount;

  /** SDK 原始告警编号，可传十进制整数或 0x 前缀十六进制字符串。 */
  private String vendorEventCode;

  /** SDK 原始告警编号的十六进制表示，与 vendorEventCode 表示同一个数值。 */
  private String vendorEventCodeHex;

  /** 厂商告警子类型：HCNetSDK 和 ISUP 使用独立编号体系，由可信实例类型决定。 */
  private String vendorAlarmType;

  /** ALARM 普通告警、IVS 智能事件或 ISUP 事件，防止同值编号跨事件空间误匹配。 */
  private String sdkEventFamily;

  /** 已解码的扩展字段，例如 diskNo、ruleId、vendorEventCode；公共字段由服务端覆盖。 */
  private Map<String, Object> data;

  /** 可选的原始厂商载荷，用于问题追溯；缺失时使用整理后的 data 作为 rawData。 */
  private Map<String, Object> raw;
}
