package cn.universal.dm.video;

import java.util.Set;

/** SDK 管理能力与局域网直连发现是两个不同的范围。 */
public final class VideoSdkPlatforms {
  private static final Set<String> MANAGED = Set.of("hik_sdk", "dahua_sdk", "tiandy_sdk", "uniview_sdk", "hik_isup");

  private VideoSdkPlatforms() {}

  public static boolean managed(String platformType) {
    return platformType != null && MANAGED.contains(platformType.trim().toLowerCase(java.util.Locale.ROOT));
  }

  public static boolean isup(String platformType) {
    return "hik_isup".equals(platformType);
  }
}
