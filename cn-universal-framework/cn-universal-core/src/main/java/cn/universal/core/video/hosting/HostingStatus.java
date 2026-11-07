package cn.universal.core.video.hosting;

/** State names persisted in the existing device/product JSON configuration. */
public final class HostingStatus {
  private HostingStatus() {}
  public static final String WAITING_SCAN = "WAITING_SCAN";
  public static final String AUTHORIZED = "AUTHORIZED";
  public static final String PENDING_ACCEPT = "PENDING_ACCEPT";
  public static final String IMPORTING = "IMPORTING";
  public static final String COMPLETED = "COMPLETED";
  public static final String CANCELLED = "CANCELLED";
  public static final String EXPIRED = "EXPIRED";
  public static final String FAILED = "FAILED";
  public static final String ACTIVE = "ACTIVE";
}
