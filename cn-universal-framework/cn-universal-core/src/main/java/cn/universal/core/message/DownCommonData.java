package cn.universal.core.message;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 纯JDK21实现：固定字段+动态扩展字段（无Hutool依赖、无继承、序列化/反序列化100%稳定）
 * 核心：Jackson的@JsonAnyGetter/@JsonAnySetter实现固定+动态字段平级处理
 * 新增@JsonInclude：仅序列化非空字段，解决冗余null问题
 * 适配JDK21，可直接用于生产环境
 *
 * @author gitee.com/NexIoT
 * @version 4.1
 * @since 2023/10/11
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // 核心：只序列化非空字段，去掉所有null冗余
public class DownCommonData {
  // ===================== 固定字段（JDK原生声明，类型安全，按需添加） =====================
  /** imei运营商NB号码，长度15 */
  private String imei;
  /** 长度不超过15 */
  private String imsi;
  /** 水电表 表号 */
  private String meterNo;
  /** 设备型号 */
  private String deviceModel;
  /** 设备名称 */
  private String deviceName;
  /** 公司名称 */
  private String companyNo;
  /** 配置信息（JDK原生Map，替代Hutool JSONObject，跨框架兼容） */
  private Map<String, Object> configuration;
  /** 经度 */
  private String longitude;
  /** 纬度 */
  private String latitude;
  /** 安装位置 */
  private String location;
  /** 扩展设备ID */
  private String extDeviceId;
  /** 网关设备deviceId */
  private String gwDeviceId;
  /** 密钥（乐橙、其他设备） */
  private String appKey;
  /** 从站地址 */
  private String slaveAddress;
  /** 消息ID */
  private String commandId;

  // ===================== 动态扩展字段（JDK原生HashMap，无Hutool依赖） =====================
  /** 动态字段存储：所有非固定的自定义字段都存在这里 */
  private final Map<String, Object> dynamicFields = new HashMap<>();

  /**
   * 从通用对象构建 DownCommonData。
   *
   * <p>当前重点支持 Map/JSONObject 场景，这也是下行 data 的主要来源。这样可以绕开 Hutool
   * JSONUtil.toBean 对 {@code @JsonAnySetter} 不生效的问题，确保动态字段不会丢失。
   */
  public static DownCommonData from(Object source) {
    if (source instanceof DownCommonData downCommonData) {
      return downCommonData;
    }

    DownCommonData downCommonData = new DownCommonData();
    if (!(source instanceof Map<?, ?> map)) {
      return downCommonData;
    }

    map.forEach(
        (key, value) -> {
          if (Objects.nonNull(key)) {
            downCommonData.put(String.valueOf(key), value);
          }
        });
    return downCommonData;
  }

  // ===================== 固定字段：getter/setter（JDK原生，IDEA可自动生成，无同步逻辑） =====================
  public String getImei() { return imei; }
  public void setImei(String imei) { this.imei = imei; }

  public String getImsi() { return imsi; }
  public void setImsi(String imsi) { this.imsi = imsi; }

  public String getMeterNo() { return meterNo; }
  public void setMeterNo(String meterNo) { this.meterNo = meterNo; }

  public String getDeviceModel() { return deviceModel; }
  public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }

  public String getDeviceName() { return deviceName; }
  public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

  public String getCompanyNo() { return companyNo; }
  public void setCompanyNo(String companyNo) { this.companyNo = companyNo; }

  public Map<String, Object> getConfiguration() { return configuration; }
  public void setConfiguration(Map<String, Object> configuration) { this.configuration = configuration; }

  public String getLongitude() { return longitude; }
  public void setLongitude(String longitude) { this.longitude = longitude; }

  public String getLatitude() { return latitude; }
  public void setLatitude(String latitude) { this.latitude = latitude; }

  public String getLocation() { return location; }
  public void setLocation(String location) { this.location = location; }

  public String getExtDeviceId() { return extDeviceId; }
  public void setExtDeviceId(String extDeviceId) { this.extDeviceId = extDeviceId; }

  public String getGwDeviceId() { return gwDeviceId; }
  public void setGwDeviceId(String gwDeviceId) { this.gwDeviceId = gwDeviceId; }

  public String getAppKey() { return appKey; }
  public void setAppKey(String appKey) { this.appKey = appKey; }

  public String getSlaveAddress() { return slaveAddress; }
  public void setSlaveAddress(String slaveAddress) { this.slaveAddress = slaveAddress; }

  public String getCommandId() { return commandId; }
  public void setCommandId(String commandId) { this.commandId = commandId; }

  // ===================== 核心：Jackson注解实现固定+动态字段平级处理 =====================
  /**
   * @JsonAnySetter：反序列化时，JSON中无对应固定字段的键值对，自动存入dynamicFields
   * 【关键修改】：如果是固定字段名，直接覆盖固定字段，实现"动态字段覆盖固定字段"的业务需求
   */
  @JsonAnySetter
  @SuppressWarnings("unchecked")
  public void putDynamicField(String key, Object value) {
    // 先判断是否是固定字段，若是则直接调用setter覆盖，否则存入动态字段
    switch (key) {
      case "imei" -> setImei(toNullableString(value));
      case "imsi" -> setImsi(toNullableString(value));
      case "meterNo" -> setMeterNo(toNullableString(value));
      case "deviceModel" -> setDeviceModel(toNullableString(value));
      case "deviceName" -> setDeviceName(toNullableString(value));
      case "companyNo" -> setCompanyNo(toNullableString(value));
      case "configuration" -> setConfiguration(value == null ? null : (Map<String, Object>) value);
      case "longitude" -> setLongitude(toNullableString(value));
      case "latitude" -> setLatitude(toNullableString(value));
      case "location" -> setLocation(toNullableString(value));
      case "extDeviceId" -> setExtDeviceId(toNullableString(value));
      case "gwDeviceId" -> setGwDeviceId(toNullableString(value));
      case "appKey" -> setAppKey(toNullableString(value));
      case "slaveAddress" -> setSlaveAddress(toNullableString(value));
      case "commandId" -> setCommandId(toNullableString(value));
      default -> this.dynamicFields.put(key, value);
    }
  }

  /**
   * @JsonAnyGetter：序列化时，将dynamicFields中的所有动态字段平级拼接到JSON中
   * 例：动态字段hutoolDynamic，会和deviceName平级出现在JSON中，无嵌套
   */
  @JsonAnyGetter
  public Map<String, Object> getDynamicFields() {
    return this.dynamicFields;
  }

  // ===================== 动态字段：灵活操作方法（模拟原Hutool JSONObject，使用习惯不变） =====================
  /** 新增/修改动态字段（和原Hutool put用法一致） */
  public void put(String key, Object value) {
    // 调用@JsonAnySetter的方法，保证"固定字段覆盖逻辑"统一
    this.putDynamicField(key, value);
  }

  /** 获取动态字段值（和原Hutool get用法一致） */
  public Object get(String key) {
    // 先判断固定字段是否有值，有则返回（覆盖后优先固定字段），否则返回动态字段
    return switch (key) {
      case "imei" -> getImei();
      case "imsi" -> getImsi();
      case "meterNo" -> getMeterNo();
      case "deviceModel" -> getDeviceModel();
      case "deviceName" -> getDeviceName();
      case "companyNo" -> getCompanyNo();
      case "configuration" -> getConfiguration();
      case "longitude" -> getLongitude();
      case "latitude" -> getLatitude();
      case "location" -> getLocation();
      case "extDeviceId" -> getExtDeviceId();
      case "gwDeviceId" -> getGwDeviceId();
      case "appKey" -> getAppKey();
      case "slaveAddress" -> getSlaveAddress();
      case "commandId" -> getCommandId();
      default -> this.dynamicFields.get(key);
    };
  }

  /** 类型安全获取动态字段-字符串（避免强转，推荐使用） */
  public String getStr(String key) {
    Object value = this.get(key);
    return Objects.isNull(value) ? null : value.toString();
  }

  /** 类型安全获取动态字段-整数（避免强转，推荐使用） */
  public Integer getInt(String key) {
    Object value = this.get(key);
    if (Objects.isNull(value)) throw new NullPointerException("动态字段[" + key + "]值为null，无法转换为Integer");
    if (value instanceof Integer) return (Integer) value;
    if (value instanceof Number) return ((Number) value).intValue();
    throw new ClassCastException("动态字段[" + key + "]无法转换为Integer，当前类型：" + value.getClass().getName());
  }

  /** 类型安全获取动态字段-布尔（避免强转，推荐使用） */
  public Boolean getBool(String key) {
    Object value = this.get(key);
    if (Objects.isNull(value)) throw new NullPointerException("动态字段[" + key + "]值为null，无法转换为Boolean");
    if (value instanceof Boolean) return (Boolean) value;
    throw new ClassCastException("动态字段[" + key + "]无法转换为Boolean，当前类型：" + value.getClass().getName());
  }

  /** 判断动态字段是否存在（和原Hutool containsKey用法一致） */
  public boolean containsKey(String key) {
    // 先判断固定字段是否有值，有则返回true，否则判断动态字段
    return switch (key) {
      case "imei", "imsi", "meterNo", "deviceModel", "deviceName", "companyNo",
           "configuration", "longitude", "latitude", "location", "extDeviceId",
           "gwDeviceId", "appKey", "slaveAddress", "commandId" ->
          Objects.nonNull(get(key));
      default -> this.dynamicFields.containsKey(key);
    };
  }

  /** 移除动态字段 */
  public Object remove(String key) {
    return this.dynamicFields.remove(key);
  }

  // ===================== 辅助方法：合并固定+动态字段为Map（替代原Hutool toMap） =====================
  public Map<String, Object> toMap() {
    // 1. 创建新Map，存入固定字段（仅存非空，和序列化一致）
    Map<String, Object> resultMap = new HashMap<>();
    if (Objects.nonNull(imei)) resultMap.put("imei", imei);
    if (Objects.nonNull(imsi)) resultMap.put("imsi", imsi);
    if (Objects.nonNull(meterNo)) resultMap.put("meterNo", meterNo);
    if (Objects.nonNull(deviceModel)) resultMap.put("deviceModel", deviceModel);
    if (Objects.nonNull(deviceName)) resultMap.put("deviceName", deviceName);
    if (Objects.nonNull(companyNo)) resultMap.put("companyNo", companyNo);
    if (Objects.nonNull(configuration)) resultMap.put("configuration", configuration);
    if (Objects.nonNull(longitude)) resultMap.put("longitude", longitude);
    if (Objects.nonNull(latitude)) resultMap.put("latitude", latitude);
    if (Objects.nonNull(location)) resultMap.put("location", location);
    if (Objects.nonNull(extDeviceId)) resultMap.put("extDeviceId", extDeviceId);
    if (Objects.nonNull(gwDeviceId)) resultMap.put("gwDeviceId", gwDeviceId);
    if (Objects.nonNull(appKey)) resultMap.put("appKey", appKey);
    if (Objects.nonNull(slaveAddress)) resultMap.put("slaveAddress", slaveAddress);
    if (Objects.nonNull(commandId)) resultMap.put("commandId", commandId);
    // 2. 合并动态字段（平级，同名动态字段覆盖固定字段）
    resultMap.putAll(this.dynamicFields);
    return resultMap;
  }

  private static String toNullableString(Object value) {
    return Objects.isNull(value) ? null : value.toString();
  }
}
