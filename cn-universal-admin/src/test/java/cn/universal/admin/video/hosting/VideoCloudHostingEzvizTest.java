package cn.universal.admin.video.hosting;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.core.video.hosting.VideoCloudHostingProvider;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

class VideoCloudHostingEzvizTest {
  @Test void consoleProbeNeedsNoSessionAndExpiredOrInvalidTraceNeverImports() {
    var redis = mock(StringRedisTemplate.class);
    var values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    var provider = mock(VideoCloudHostingProvider.class);
    when(provider.platformType()).thenReturn("ezviz");
    var service = new VideoCloudHostingService(redis, new ObjectMapper(), null, null, null, null, List.of(provider));
    service.handleQuery("ezviz", Map.of());
    service.handleQuery("ezviz", Map.of("auth_code", "code", "traceability", "uuid"));
    service.handleQuery("ezviz", Map.of("auth_code", "code", "traceability", "1234567"));
    verify(provider, never()).completeAuthorization(any(), any());
  }

  @Test void anonymousCancelVerifiesTrustAndCloudTruthBeforeUpdating() throws Exception {
    var devices = mock(IoTDeviceMapper.class);
    var products = mock(IoTProductMapper.class);
    var provider = mock(VideoCloudHostingProvider.class);
    when(provider.platformType()).thenReturn("ezviz");
    var redisson = mock(org.redisson.api.RedissonClient.class);
    var lock = mock(org.redisson.api.RLock.class);
    when(redisson.getLock(anyString())).thenReturn(lock);
    when(lock.tryLock(10, TimeUnit.SECONDS)).thenReturn(true);
    when(lock.isHeldByCurrentThread()).thenReturn(true);
    var cache = mock(cn.universal.dm.cache.IoTCacheInvalidationPublisher.class);
    var service = new VideoCloudHostingService(null, null, products, devices, null, redisson, List.of(provider));
    ReflectionTestUtils.setField(service, "cacheInvalidationPublisher", cache);
    var local = IoTDevice.builder().productKey("p").tenantId("tenant").deviceId("NVR").configuration("{\"hosting\":{\"provider\":\"ezviz\",\"deviceTrustId\":\"trust\",\"status\":\"ACTIVE\"},\"channelList\":[{\"channelId\":\"7\"}]}").build();
    when(devices.select(any(IoTDevice.class))).thenReturn(List.of(local));
    when(products.getProductByProductKey("p")).thenReturn(IoTProduct.builder().productKey("p").tenantId("tenant").thirdPlatform("ezviz").build());
    service.handleQuery("ezviz", Map.of("opt_type", "device_cancel", "deviceSerials", "NVR:7", "deviceTrustId", "wrong"));
    verify(provider, never()).allAuthorizedDevices(any());
    when(provider.allAuthorizedDevices(any())).thenThrow(new IllegalStateException("cloud unavailable"));
    assertThrows(IllegalStateException.class, () -> service.handleQuery("ezviz", Map.of("opt_type", "device_cancel", "deviceSerials", "NVR:7", "deviceTrustId", "trust")));
    verify(devices, never()).updateByPrimaryKeySelective(any());
    doReturn(List.of()).when(provider).allAuthorizedDevices(any());
    service.handleQuery("ezviz", Map.of("opt_type", "device_cancel", "deviceSerials", "NVR:7", "deviceTrustId", "trust"));
    verify(devices).updateByPrimaryKeySelective(argThat(d -> d.getConfiguration().contains("UNAVAILABLE") && d.getState() == 0));
  }
}
