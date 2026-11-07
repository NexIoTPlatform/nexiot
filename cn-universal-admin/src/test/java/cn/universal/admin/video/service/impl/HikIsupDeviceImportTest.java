package cn.universal.admin.video.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.video.alarm.SdkVideoAlarmContextCache;
import cn.universal.admin.video.service.VideoPlatformProductService;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.*;
import cn.universal.security.utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

class HikIsupDeviceImportTest {
  private final VideoPlatformInstanceMapper instances = mock(VideoPlatformInstanceMapper.class);
  private final VideoPlatformProductService products = mock(VideoPlatformProductService.class);
  private final IoTDeviceMapper devices = mock(IoTDeviceMapper.class);
  private final VideoPlatformDeviceMapper video = mock(VideoPlatformDeviceMapper.class);
  private final VideoPlatformChannelMapper channels = mock(VideoPlatformChannelMapper.class);
  private final ObjectMapper json = new ObjectMapper();
  private final VideoDeviceImportServiceImpl service = new VideoDeviceImportServiceImpl(instances, products, devices,
      mock(IoTProductMapper.class), json, video, channels, mock(SdkVideoAlarmContextCache.class));
  private final VideoPlatformInstance instance = VideoPlatformInstance.builder().instanceKey("isup-a")
      .platformType("hik_isup").tenantId("trusted-tenant").creatorId("trusted-creator").build();

  @BeforeEach void setup() {
    for (Class<?> entity : List.of(VideoPlatformInstance.class, IoTDevice.class, VideoPlatformDevice.class))
      EntityHelper.initEntityNameMap(entity, new Config());
    when(instances.selectByExample(any())).thenReturn(List.of(instance));
    when(instances.selectOne(any())).thenReturn(instance);
    when(products.ensurePlatformProduct(instance, "VIDEO_DEVICE")).thenReturn(IoTProduct.builder()
        .productKey("isup-a").tenantId("trusted-tenant").name("ISUP").build());
    when(devices.selectByExample(any())).thenReturn(List.of());
    when(video.selectByExample(any())).thenReturn(List.of());
  }

  @Test void backgroundRegistrationCreatesTrustedTenantDeviceWithoutInventedChannel() throws Exception {
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      service.importDevice("isup-a", Map.of("deviceId", "nvr-a", "status", "ON", "tenantId", "forged"), null);
    }
    var saved = org.mockito.ArgumentCaptor.forClass(IoTDevice.class);
    verify(devices).insertSelective(saved.capture());
    assertEquals("trusted-creator", saved.getValue().getCreatorId());
    assertEquals("trusted-tenant", saved.getValue().getTenantId());
    assertEquals(1, saved.getValue().getState());
    assertEquals(0, json.readTree(saved.getValue().getConfiguration()).path("channelList").size());
    assertEquals("isup-a", json.readTree(saved.getValue().getConfiguration()).path("instanceKey").asText());
  }

  @Test void offlineRefreshMatchesExactProductAndTenantAndPreservesCustomConfiguration() throws Exception {
    var saved = IoTDevice.builder().id(99L).iotId("isup-anvr-a").productKey("isup-a").deviceId("nvr-a")
        .tenantId("trusted-tenant").configuration("{\"customMap\":{\"value\":1}}").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    assertTrue(service.updateDeviceStatusAndConfig("isup-a", Map.of("deviceId", "nvr-a", "status", "OFF"), "hik_isup"));
    assertEquals(0, saved.getState());
    assertEquals(1, json.readTree(saved.getConfiguration()).path("customMap").path("value").asInt());
    var query = org.mockito.ArgumentCaptor.forClass(Example.class);
    verify(devices).selectByExample(query.capture());
    var criteria = query.getValue().getOredCriteria().getFirst().getAllCriteria();
    assertTrue(criteria.stream().anyMatch(c -> c.getCondition().startsWith("product_key") && c.getValue().equals("isup-a")));
    assertTrue(criteria.stream().anyMatch(c -> c.getCondition().startsWith("tenant_id") && c.getValue().equals("trusted-tenant")));
    assertTrue(criteria.stream().noneMatch(c -> c.getCondition().contains("like")));
  }
}
