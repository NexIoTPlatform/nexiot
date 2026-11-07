package cn.universal.common.plugin;

import java.util.Collections;
import java.util.Map;

/** 平台插件描述信息，用于统一向管理端暴露插件状态。 */
public interface PlatformPluginDescriptor {

  /** 插件唯一编码。 */
  String getPluginCode();

  /** 插件展示名称。 */
  String getPluginName();

  /** 插件是否启用。 */
  boolean isEnabled();

  /** 插件是否已安装在当前运行环境中。 */
  default boolean isInstalled() {
    return true;
  }

  /** 插件附加元数据。 */
  default Map<String, Object> getMetadata() {
    return Collections.emptyMap();
  }
}
