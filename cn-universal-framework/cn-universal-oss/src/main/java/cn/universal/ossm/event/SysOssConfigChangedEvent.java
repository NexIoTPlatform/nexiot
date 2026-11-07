package cn.universal.ossm.event;

/** OSS运行时配置变更事件，用于跨模块清理本地缓存。 */
public class SysOssConfigChangedEvent {

  private final String provider;
  private final String tenantId;

  public SysOssConfigChangedEvent(String provider, String tenantId) {
    this.provider = provider;
    this.tenantId = tenantId;
  }

  public String getProvider() {
    return provider;
  }

  public String getTenantId() {
    return tenantId;
  }
}
