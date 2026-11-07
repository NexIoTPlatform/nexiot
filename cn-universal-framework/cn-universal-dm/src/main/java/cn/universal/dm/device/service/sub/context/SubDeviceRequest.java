package cn.universal.dm.device.service.sub.context;

import cn.universal.core.message.SubDevice;
import cn.universal.persistence.base.BaseUPRequest;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 子设备上下文 包含子设备处理所需的所有信息
 *
 * @author system
 * @date 2025-01-16
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class SubDeviceRequest extends BaseUPRequest {
  /** 网关设备 */
  private String gwProductKey;

  /** 网关设备Id */
  private String gwDeviceId;

  /** 子设备信息 */
  private SubDevice subDevice;

  /** 处理阶段 */
  private ProcessingStage stage;

  /** 处理是否成功 */
  private Boolean success;

  /** 错误信息 */
  private String errorMessage;

  /** 上下文值存储 */
  @Builder.Default private java.util.Map<String, Object> contextValues = new java.util.HashMap<>();

  /** 设置上下文值 */
  public void setContextValue(String key, Object value) {
    if (contextValues == null) {
      contextValues = new java.util.HashMap<>();
    }
    contextValues.put(key, value);
  }

  /** 获取上下文值 */
  public Object getContextValue(String key) {
    return contextValues != null ? contextValues.get(key) : null;
  }

  /** 设置编解码上下文值 */
  public void setCodecContextValue(String key, Object value) {
    setContextValue("codec_" + key, value);
  }

  /** 获取编解码上下文值 */
  public Object getCodecContextValue(String key) {
    return getContextValue("codec_" + key);
  }

  /** 获取编解码上下文 */
  public java.util.Map<String, Object> getCodecContext() {
    java.util.Map<String, Object> codecContext = new java.util.HashMap<>();
    if (contextValues != null) {
      contextValues.entrySet().stream()
          .filter(entry -> entry.getKey().startsWith("codec_"))
          .forEach(entry -> codecContext.put(entry.getKey().substring(6), entry.getValue()));
    }
    return codecContext;
  }

  /** 设置上行请求列表 */
  public void setUpRequestList(
      java.util.List<cn.universal.persistence.base.BaseUPRequest> upRequestList) {
    setContextValue("upRequestList", upRequestList);
  }

  /** 获取上行请求列表 */
  @SuppressWarnings("unchecked")
  public java.util.List<cn.universal.persistence.base.BaseUPRequest> getUpRequestList() {
    Object value = getContextValue("upRequestList");
    return value instanceof java.util.List
        ? (java.util.List<cn.universal.persistence.base.BaseUPRequest>) value
        : null;
  }

  /** 处理阶段枚举 */
  public enum ProcessingStage {
    INITIALIZED,
    DECODED,
    PROTOCOL_DECODED,
    SHADOW_UPDATED,
    LOGGED,
    COMPLETED,
    FAILED
  }
}
