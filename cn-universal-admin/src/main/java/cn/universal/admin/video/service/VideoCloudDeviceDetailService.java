package cn.universal.admin.video.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.exception.BaseException;
import cn.universal.core.video.hosting.VideoCloudDeviceSnapshot;
import cn.universal.core.video.hosting.VideoCloudHostingProvider;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.persistence.base.IoTDevicePostProcessor;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Local cloud-camera diagnostic snapshot; vendor queries run outside database row locks. */
@Service
public class VideoCloudDeviceDetailService {
  private final IoTDeviceMapper devices;
  private final IoTCacheInvalidationPublisher invalidation;
  private final List<VideoCloudHostingProvider> providers;
  private final TransactionTemplate transactions;

  public VideoCloudDeviceDetailService(IoTDeviceMapper devices, IoTCacheInvalidationPublisher invalidation,
      List<VideoCloudHostingProvider> providers, PlatformTransactionManager transactionManager) {
    this.devices = devices;
    this.invalidation = invalidation;
    this.providers = providers;
    this.transactions = new TransactionTemplate(transactionManager);
  }

  public Map<String, Object> detail(IoTDevice device, IoTProduct product) {
    JSONObject config = config(device);
    Map<String, Object> identity = new LinkedHashMap<>();
    identity.put("id", String.valueOf(device.getId()));
    identity.put("iotId", device.getIotId());
    identity.put("productKey", device.getProductKey());
    identity.put("deviceId", device.getDeviceId());
    identity.put("deviceName", device.getDeviceName());
    identity.put("state", device.getState());
    identity.put("onlineTime", device.getOnlineTime());
    identity.put("createTime", device.getCreateTime());
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("device", identity);
    result.put("platform", product.getThirdPlatform());
    // Reapply the whitelist even for legacy configuration values.
    Map<String, Object> raw = config.getJSONObject("cloudDeviceSnapshot");
    if (raw == null) raw = config;
    result.put("snapshot", VideoCloudDeviceSnapshot.device(hosted(device, raw)));
    result.put("channelList", VideoCloudDeviceSnapshot.channels(config));
    result.put("syncedAt", config.getLong("cloudDeviceSyncedAt"));
    result.put("pictureSecretConfigured", !config.getStr("pictureSecret", "").isBlank());
    result.put("pictureSecretVerification", verification(config));
    return result;
  }

  public Map<String, Object> refresh(IoTDevice authorized, IoTProduct product) {
    VideoCloudHostingProvider provider = provider(product);
    var remote = provider.refreshDeviceDetails(info(product), hosted(authorized, config(authorized)));
    if (remote == null || !Objects.equals(authorized.getDeviceId(), remote.deviceId()))
      throw new BaseException("厂家未返回该设备详情，已保留本地快照");
    return transactions.execute(status -> {
      IoTDevice current = devices.selectForUpdateByProductKeyAndDeviceId(authorized.getProductKey(), authorized.getDeviceId());
      if (current == null || !Objects.equals(current.getTenantId(), authorized.getTenantId()))
        throw new BaseException("设备不存在或归属已变化");
      JSONObject config = config(current);
      mergeSnapshot(config, remote);
      var patch = new IoTDevice();
      patch.setId(current.getId());
      patch.setConfiguration(config.toString());
      devices.updateByPrimaryKeySelective(patch);
      current.setConfiguration(config.toString());
      publishDeviceChange(current, product.getThirdPlatform());
      return detail(current, product);
    });
  }

  public static void mergeSnapshot(JSONObject config, VideoCloudHostingProvider.HostedDevice remote) {
    Map<String, Object> snapshot = VideoCloudDeviceSnapshot.device(remote);
    config.set("cloudDeviceSnapshot", snapshot);
    config.set("cloudDeviceSyncedAt", System.currentTimeMillis());
    // These read-only flags are also consumed by the picture decoder.
    for (String key : List.of("deviceAbility", "isTcmDevice", "encryptMode"))
      if (snapshot.containsKey(key)) config.set(key, snapshot.get(key));
    var channels = VideoCloudDeviceSnapshot.channels(remote.raw());
    if (!channels.isEmpty()) {
      config.set("channelList", channels);
      config.set("channelNum", channels.size());
      config.set("channelId", channels.get(0).get("channelId"));
    }
  }

  void publishDeviceChange(IoTDevice device) {
    publishDeviceChange(device, device.getThirdPlatform());
  }

  void publishDeviceChange(IoTDevice device, String platform) {
    // The mapper joins the product, including thirdPlatform. Converting the device entity alone
    // omits that identity and leaves the callback's tpd/tpe cache aliases stale after a password save.
    IoTDeviceDTO dto = devices.selectIoTDeviceBO(Map.of("productKey", device.getProductKey(),
        "deviceId", device.getDeviceId()));
    if (dto == null) {
      dto = cn.hutool.core.bean.BeanUtil.toBean(device, IoTDeviceDTO.class);
      dto.setDevConfiguration(device.getConfiguration());
      dto.setThirdPlatform(platform);
    }
    invalidation.publishDeviceChange(IoTDevicePostProcessor.Operation.UPDATE, dto);
  }

  public VideoCloudHostingProvider provider(IoTProduct product) {
    return providers.stream().filter(p -> p.platformType().equals(product.getThirdPlatform())).findFirst()
        .orElseThrow(() -> new BaseException("未找到云平台适配器"));
  }

  public static VideoCloudHostingProvider.ProductInfo info(IoTProduct product) {
    return new VideoCloudHostingProvider.ProductInfo(product.getProductKey(), product.getConfiguration(), product.getThirdConfiguration());
  }

  public static VideoCloudHostingProvider.HostedDevice hosted(IoTDevice device, Map<String, Object> config) {
    return new VideoCloudHostingProvider.HostedDevice(device.getDeviceId(), Objects.toString(config.get("channelId"), "0"),
        device.getDeviceName(), null, Objects.toString(config.get("deviceModel"), ""), device.getDeviceId(),
        null, null, null, null, device.getState(), config);
  }

  public static JSONObject config(IoTDevice device) {
    return device.getConfiguration() == null || device.getConfiguration().isBlank()
        ? new JSONObject() : JSONUtil.parseObj(device.getConfiguration());
  }

  public static Map<String, Object> verification(JSONObject config) {
    var raw = config.getJSONObject("pictureSecretVerification");
    if (raw == null) return Map.of("status", "unverified", "message", "尚未验证");
    Map<String, Object> result = new LinkedHashMap<>();
    for (String key : List.of("status", "checkedAt", "message", "verificationMode")) {
      Object value = raw.get(key);
      if (value instanceof String || value instanceof Number) result.put(key, value);
    }
    result.putIfAbsent("status", "unverified");
    return result;
  }
}
