package cn.universal.admin.video.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.system.permission.SysPermissionService;
import cn.universal.common.exception.BaseException;
import cn.universal.core.video.hosting.VideoCloudHostingProvider;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.mapper.IoTProductMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/** Explicit, non-cacheable SDK handoff. Vendor tokens are never fetched by catalog endpoints. */
@Service
public class VideoCloudSdkTicketService {
  private final VideoCatalogService catalog;
  private final IoTProductMapper products;
  private final List<VideoCloudHostingProvider> providers;
  private final SysPermissionService permissions;

  public VideoCloudSdkTicketService(VideoCatalogService catalog, IoTProductMapper products,
      List<VideoCloudHostingProvider> providers, SysPermissionService permissions) {
    this.catalog = catalog;
    this.products = products;
    this.providers = providers;
    this.permissions = permissions;
  }

  public Map<String, Object> preview(String iotId, Map<String, Object> request) {
    if (!"preview".equals(Objects.toString(request.getOrDefault("mode", "preview"))))
      throw new BaseException("官方云 SDK 授权目前仅支持实时预览");
    var device = catalog.requireVisibleDevice(iotId);
    String platform = device.getPlatformType();
    if (!Set.of("imoulife", "dahua_cloud").contains(Objects.toString(platform, "")))
      throw new BaseException("该设备不支持官方云 SDK 授权");
    if (!permissions.hasPermission("video:device:play")
        && !permissions.hasPermission("video:platform:edit")
        && !permissions.hasPermission("video:" + platform + ":live"))
      throw new AccessDeniedException("无权预览该视频设备");
    String channelId = Objects.toString(request.get("channelId"), "");
    var channel = catalog.requireVisibleChannel(device, channelId);
    if ("unsupported".equals(channel.getCapabilities().get("preview")))
      throw new BaseException("该通道不支持实时预览");
    String stream = Objects.toString(request.getOrDefault("streamType", "main"));
    if (!Set.of("main", "sub").contains(stream)) throw new BaseException("无效视频码流");
    Integer nativeChannel = channel.getChannelNo();
    if (nativeChannel == null || nativeChannel < 0 || nativeChannel > 65535)
      throw new BaseException("该通道缺少厂家 SDK 通道号，请先同步设备详情");
    var product = products.getProductByProductKey(device.getProductKey());
    if (product == null || !platform.equals(product.getThirdPlatform()))
      throw new BaseException("视频产品不存在或配置已变化");
    var provider = providers.stream().filter(value -> platform.equals(value.platformType())).findFirst()
        .orElseThrow(() -> new BaseException("厂家云协议模块未启用"));
    JSONObject config;
    try { config = JSONUtil.parseObj(StrUtil.blankToDefault(device.getConfiguration(), "{}")); }
    catch (RuntimeException malformed) { throw new BaseException("视频设备配置无效，请先同步设备详情"); }
    var identity = IoTDevice.builder().deviceId(device.getDeviceId()).deviceName(device.getDeviceName())
        .productKey(device.getProductKey()).state(device.getState() == null ? null : device.getState() != 0).build();
    Map<String, String> credentials = provider.desktopPreviewCredentials(
        VideoCloudDeviceDetailService.info(product), VideoCloudDeviceDetailService.hosted(identity, config), channelId);
    Set<String> allowed = "imoulife".equals(platform) ? Set.of("accessToken", "playToken", "psk", "pid")
        : Set.of("accessToken", "playInfo", "decryptKey");
    if (credentials == null || !allowed.containsAll(credentials.keySet())
        || StrUtil.isBlank(credentials.get("accessToken"))
        || ("dahua_cloud".equals(platform) && StrUtil.isBlank(credentials.get("playInfo"))))
      throw new BaseException("厂家未返回完整播放授权");
    Map<String, Object> ticket = new LinkedHashMap<>();
    ticket.put("schemaVersion", 1);
    ticket.put("vendor", "imoulife".equals(platform) ? "imou" : "cloud_dahua");
    ticket.put("deviceId", device.getDeviceId());
    ticket.put("channelId", channelId);
    ticket.put("nativeChannel", nativeChannel);
    ticket.put("streamType", stream);
    // This is a client handoff deadline, not a claim to shorten the vendor token's lifetime.
    ticket.put("expiresAt", System.currentTimeMillis() + 300_000L);
    ticket.put("credentials", credentials);
    ticket.put("sdkOptions", Map.of("apiHost", "imoulife".equals(platform)
        ? "openapi.lechange.cn" : "openapi.cloud-dahua.com", "apiPort", 443, "tls", true, "linkNvrTls", 0));
    return ticket;
  }
}
