package cn.universal.core.video.hosting;

import java.util.List;

/**
 * Describes the product-level configuration owned by a video cloud protocol.
 * The admin UI can use this metadata without embedding vendor-specific keys.
 */
public interface VideoCloudHostingConfigProvider {

  String platformType();

  List<HostingConfigField> fields();

  record HostingConfigField(
      String key,
      String label,
      String valueType,
      boolean secret,
      String description,
      String placeholder) {}
}
