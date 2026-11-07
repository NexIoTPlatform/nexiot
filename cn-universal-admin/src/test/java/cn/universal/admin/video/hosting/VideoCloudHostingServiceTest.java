package cn.universal.admin.video.hosting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.core.video.hosting.VideoCloudHostingProvider;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.query.IoTDeviceQuery;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class VideoCloudHostingServiceTest {

  @Test
  void sharedChannelsMustNeverBeInventedFromChannelCount() {
    var service = new VideoCloudHostingService(null, null, null, null, null, null, List.of());
    var hosted = new VideoCloudHostingProvider.HostedDevice("cam", "7", "shared", "channel", null,
        "cam", null, null, null, null, 0, Map.of("strictChannels", true, "channelNum", 16,
        "channels", List.of(Map.of("channelId", "7", "unitType", 1, "shareFunctions", List.of("live")))));
    var channels = service.extractChannelList(hosted);
    assertEquals(1, channels.size());
    assertEquals("7", channels.get(0).get("channelId"));
    assertEquals(List.of("live"), channels.get(0).get("shareFunctions"));
  }

  @Test
  void enrichDeviceRefShouldCopyProductAndFirstChannelFromVendorRecord() {
    VideoCloudHostingService service = new VideoCloudHostingService(null, null, null, null, null, null, List.of());
    VideoCloudHostingProvider.HostedDevice device = new VideoCloudHostingProvider.HostedDevice(
        "D1", "0", "camera", null, null, "D1", null, null, null, null, null,
        Map.of("productId", "YZZETZN6", "channels", List.of(Map.of("channelId", "C1"))));

    VideoCloudHostingProvider.DeviceRef result = service.enrichDeviceRef(
        new VideoCloudHostingProvider.DeviceRef("D1", null, null), device);

    assertEquals("D1", result.deviceId());
    assertEquals("C1", result.channelId());
    assertEquals("YZZETZN6", result.productId());
  }

  @Test
  void enrichDeviceRefShouldPreserveExplicitValues() {
    VideoCloudHostingService service = new VideoCloudHostingService(null, null, null, null, null, null, List.of());
    VideoCloudHostingProvider.HostedDevice device = new VideoCloudHostingProvider.HostedDevice(
        "D1", "vendor-channel", "camera", null, null, "D1", null, null, null, null, null,
        Map.of("productId", "vendor-product", "channelId", "vendor-channel"));

    VideoCloudHostingProvider.DeviceRef result = service.enrichDeviceRef(
        new VideoCloudHostingProvider.DeviceRef("D1", "explicit-channel", "explicit-product"), device);

    assertEquals("explicit-channel", result.channelId());
    assertEquals("explicit-product", result.productId());
  }

  @Test
  void extractChannelListShouldKeepEveryVendorChannel() {
    VideoCloudHostingService service =
        new VideoCloudHostingService(null, null, null, null, null, null, List.of());
    VideoCloudHostingProvider.HostedDevice device =
        new VideoCloudHostingProvider.HostedDevice(
            "D1",
            "0",
            "dual camera",
            "moving lens",
            null,
            "D1",
            null,
            null,
            null,
            null,
            null,
            Map.of(
                "channels",
                List.of(
                    Map.of("channelId", 0, "channelName", "moving lens"),
                    Map.of("channelId", 1, "channelName", "fixed lens"))));

    List<Map<String, Object>> channels = service.extractChannelList(device);

    assertEquals(2, channels.size());
    assertEquals("0", channels.get(0).get("channelId"));
    assertEquals("moving lens", channels.get(0).get("channelName"));
    assertEquals("1", channels.get(1).get("channelId"));
    assertEquals("fixed lens", channels.get(1).get("channelName"));
  }

  @Test
  void revocationShouldDeleteDeviceThroughLifecycleService() {
    IoTDeviceMapper deviceMapper = mock(IoTDeviceMapper.class);
    IoTProductMapper productMapper = mock(IoTProductMapper.class);
    IoTDeviceService lifecycle = mock(IoTDeviceService.class);
    IoTDevice device = IoTDevice.builder().deviceId("D1").iotId("iot-1").productKey("pk")
        .configuration("{\"hosting\":{\"provider\":\"imoulife\",\"deviceTrustId\":\"w1\"}}")
        .build();
    IoTProduct product = IoTProduct.builder().productKey("pk").thirdPlatform("imoulife").build();
    VideoCloudHostingProvider provider = new VideoCloudHostingProvider() {
      @Override public String platformType() { return "imoulife"; }
      @Override public String createAuthorizationUrl(ProductInfo p, String traceability) { return ""; }
      @Override public HostingCallback parseCallback(Map<String, String> query, String body) {
        HostedDevice hosted = new HostedDevice("D1", "0", "camera", null, null, "D1", null, null,
            "w1", null, null, Map.of("productId", "YZZETZN6"));
        return new HostingCallback(null, null, "w1", List.of(hosted), Map.of("msgType", "deviceAuthCancel"));
      }
      @Override public List<HostedDevice> completeAuthorization(ProductInfo p, HostingCallback c) { return List.of(); }
      @Override public void cancelHosting(ProductInfo p, String token, List<DeviceRef> refs) { }
    };
    VideoCloudHostingService service = new VideoCloudHostingService(null, null, productMapper,
        deviceMapper, null, null, List.of(provider));
    ReflectionTestUtils.setField(service, "deviceLifecycleService", lifecycle);
    when(deviceMapper.getOneByDeviceId(org.mockito.ArgumentMatchers.any(IoTDeviceQuery.class))).thenReturn(device);
    when(productMapper.getProductByProductKey("pk")).thenReturn(product);
    when(lifecycle.delDevInstance("iot-1")).thenReturn(1);

    assertEquals(true, service.handleRevocation("imoulife", "{}"));

    verify(lifecycle).delDevInstance("iot-1");
    verify(deviceMapper, never()).updateByPrimaryKeySelective(org.mockito.ArgumentMatchers.any());
  }
  @Test
  void importKeepsLockUntilCommitAndInvalidatesDeviceCache() throws Exception {
    var deviceMapper = mock(IoTDeviceMapper.class);
    var redisson = mock(org.redisson.api.RedissonClient.class);
    var lock = mock(org.redisson.api.RLock.class);
    when(redisson.getLock(org.mockito.ArgumentMatchers.anyString())).thenReturn(lock);
    when(lock.tryLock(10, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
    when(lock.isHeldByCurrentThread()).thenReturn(true);
    var provider = mock(VideoCloudHostingProvider.class);
    when(provider.platformType()).thenReturn("dahua_cloud");
    var cache = mock(cn.universal.dm.cache.IoTCacheInvalidationPublisher.class);
    var joined = cn.universal.persistence.dto.IoTDeviceDTO.builder().productKey("pk").deviceId("cam")
        .thirdPlatform("dahua_cloud").devConfiguration("{\"channelList\":[{\"channelId\":\"7\"}]}").build();
    when(deviceMapper.selectIoTDeviceBO(Map.of("productKey", "pk", "deviceId", "cam"))).thenReturn(joined);
    var service = new VideoCloudHostingService(null, null, null, deviceMapper, null, redisson, List.of(provider));
    ReflectionTestUtils.setField(service, "cacheInvalidationPublisher", cache);
    var product = IoTProduct.builder().productKey("pk").name("video").tenantId("tenant").build();
    var remote = new VideoCloudHostingProvider.HostedDevice("cam", "7", "camera", null, null,
        "cam", null, null, null, null, 0, Map.of("strictChannels", true,
        "channels", List.of(Map.of("channelId", "7", "unitType", 1))));
    org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
    org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
    try {
      ReflectionTestUtils.invokeMethod(service, "importDevice", product, "dahua_cloud", "test", remote, "user");
      verify(deviceMapper).insertUseGeneratedKeys(org.mockito.ArgumentMatchers.argThat(device ->
          "tenant".equals(device.getTenantId()) && "pkcam".equals(device.getIotId())
              && device.getConfiguration().contains("channelList")));
      verify(cache).publishDeviceChange(org.mockito.ArgumentMatchers.eq(cn.universal.persistence.base.IoTDevicePostProcessor.Operation.CREATE),
          org.mockito.ArgumentMatchers.argThat(dto -> "dahua_cloud".equals(dto.getThirdPlatform())
              && joined.getDevConfiguration().equals(dto.getDevConfiguration())));
      verify(lock, never()).unlock();
      for (var callback : org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations())
        callback.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED);
      verify(lock).unlock();
    } finally {
      org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
      org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(false);
    }
  }

}
