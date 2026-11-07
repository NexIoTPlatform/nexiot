package cn.universal.core.metadata;

/**
 * 属性值来源
 *
 * <p>extended: 平台扩展字段，可同步到 deviceTag
 *
 * <p>device: 设备原生字段，不同步到 deviceTag
 */
public enum PropertySource {
  extended,
  device
}
