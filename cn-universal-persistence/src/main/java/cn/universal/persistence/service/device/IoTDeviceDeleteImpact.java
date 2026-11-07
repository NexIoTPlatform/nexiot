package cn.universal.persistence.service.device;

/** Module-owned resources that will be affected by a device deletion. */
public record IoTDeviceDeleteImpact(
    String source,
    long endpointCount,
    long pointCount,
    long collectGroupCount,
    long assetCount,
    long runtimeStatCount) {

  public static IoTDeviceDeleteImpact empty(String source) {
    return new IoTDeviceDeleteImpact(source, 0, 0, 0, 0, 0);
  }
}
