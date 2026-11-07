package cn.universal.core.video.hosting;

import java.util.List;
import java.util.Map;

/** Vendor-neutral contract for cloud camera hosting (trusteeship). */
public interface VideoCloudHostingProvider {
  String platformType();

  record HostedPage(List<HostedDevice> records, int page, int totalPages, long total) {}

  default HostedPage authorizedPage(ProductInfo product, int page, int size) {
    throw new UnsupportedOperationException("该平台不支持授权分页");
  }

  default boolean requiresAcceptance() { return true; }

  default HostedDevice resolveDevice(ProductInfo product, DeviceRef device) {
    throw new UnsupportedOperationException("该平台不支持按设备查询授权");
  }

  default HostedDevice bindDevice(ProductInfo product, String deviceId, String password) {
    throw new UnsupportedOperationException("该平台不支持序列号绑定");
  }

  default List<HostedDevice> allAuthorizedDevices(ProductInfo product) {
    throw new UnsupportedOperationException("该平台不支持全量授权同步");
  }

  /** Build the vendor authorization URL. The returned URL may be scanned directly as a QR code. */
  String createAuthorizationUrl(ProductInfo product, String traceability);

  /** Parse a vendor callback into a normalized callback payload. */
  HostingCallback parseCallback(Map<String, String> query, String body);

  /** 完成厂商授权回调，并返回统一格式的托管设备。 */
  List<HostedDevice> completeAuthorization(ProductInfo product, HostingCallback callback);

  /**
   * 在托管设备写入本地前，从厂商重新读取设备详情。
   * 未提供设备详情接口的平台直接沿用原始托管记录。
   */
  default HostedDevice refreshDeviceDetails(ProductInfo product, HostedDevice device) {
    return device;
  }

  /** Called only after application resource and playback permission checks; never for directory reads. */
  default Map<String, String> desktopPreviewCredentials(ProductInfo product, HostedDevice device,
      String channelId) {
    throw new UnsupportedOperationException("该云平台尚未支持桌面官方 SDK 播放");
  }

  /** Verify a candidate without persisting it or exposing the password in the result. */
  default PictureSecretVerification verifyPictureSecret(
      ProductInfo product, HostedDevice device, String password) {
    return new PictureSecretVerification(false, false, "unsupported", "该平台暂无独立密码验证接口，可保存后重试图片解密");
  }

  record PictureSecretVerification(
      boolean supported, boolean valid, String verificationMode, String message) {}

  /** 按授权状态查询厂商侧的托管设备。 */
  default List<HostedDevice> listByAuthStatus(ProductInfo product, String status, int page, int pageSize) {
    throw new UnsupportedOperationException("该视频平台不支持托管状态查询");
  }

  /** 修改选中设备在厂商侧的托管状态。 */
  default void setHostingStatus(ProductInfo product, String status, List<DeviceRef> devices) {
    throw new UnsupportedOperationException("该视频平台不支持托管状态变更");
  }

  /** 撤销一个或多个设备的托管授权。 */
  void cancelHosting(ProductInfo product, String accessToken, List<DeviceRef> devices);

  record ProductInfo(String productKey, String configuration, String thirdConfiguration) {}

  record HostingCallback(String traceability, String authorizationCode, String warrantId,
                         List<HostedDevice> devices, Map<String, Object> raw) {}

  record HostedDevice(String deviceId, String channelId, String deviceName, String channelName,
                      String model, String providerDeviceId, String accessToken,
                      String refreshToken, String deviceTrustId, String authority,
                      Integer state, Map<String, Object> raw) {}

  /** Device reference accepted by vendor status APIs. productId is required by
   * some model/channel hosting records (for example, LeChange物模型设备). */
  record DeviceRef(String deviceId, String channelId, String productId) {
    public DeviceRef(String deviceId, String channelId) {
      this(deviceId, channelId, null);
    }
  }
}
