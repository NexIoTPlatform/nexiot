package cn.universal.admin.video.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.video.service.VideoSdkSubscriptionRecovery;
import cn.universal.common.exception.BaseException;
import cn.universal.dm.video.VideoPlatformAdapter;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.dm.video.VideoSdkEventCatalog;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import cn.universal.security.utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

class VideoSdkSubscriptionConfigTest {
  private final VideoPlatformInstanceServiceImpl service = mock(VideoPlatformInstanceServiceImpl.class, CALLS_REAL_METHODS);
  private final VideoPlatformInstanceMapper instances = mock(VideoPlatformInstanceMapper.class);
  private final VideoPlatformDeviceMapper devices = mock(VideoPlatformDeviceMapper.class);
  private final VideoPlatformAdapterRegistry registry = mock(VideoPlatformAdapterRegistry.class);
  private final VideoPlatformAdapter adapter = mock(VideoPlatformAdapter.class);
  private final ObjectMapper json = new ObjectMapper();
  private final VideoPlatformInstance instance = VideoPlatformInstance.builder()
      .id(1L).instanceKey("dahua-test").platformType("dahua_sdk").enabled(1).tenantId("tenant-one")
      .options("{\"subscription\":{\"enabled\":false}}").build();

  @BeforeEach void setup() {
    EntityHelper.initEntityNameMap(VideoPlatformDevice.class, new Config());
    var catalog = mock(VideoSdkEventCatalog.class);
    when(catalog.options("dahua_sdk")).thenReturn(List.of(Map.of("value", "motion")));
    ReflectionTestUtils.setField(service, "instanceMapper", instances);
    ReflectionTestUtils.setField(service, "deviceMapper", devices);
    ReflectionTestUtils.setField(service, "ioTDeviceMapper", mock(IoTDeviceMapper.class));
    ReflectionTestUtils.setField(service, "adapterRegistry", registry);
    ReflectionTestUtils.setField(service, "objectMapper", json);
    ReflectionTestUtils.setField(service, "sdkEventCatalog", catalog);
    ReflectionTestUtils.setField(service, "sdkSubscriptionRecovery", new VideoSdkSubscriptionRecovery(instances, registry, json, catalog));
    doReturn(instance).when(service).getByInstanceKey("dahua-test");
    doReturn(IoTUser.builder().identity(0).unionId("admin").build()).when(service).queryIotUser("admin");
    when(registry.getAdapter("dahua_sdk")).thenReturn(adapter);
    when(adapter.resolveSubscriptionDevices(any())).thenReturn(List.of());
    when(devices.selectByExample(any())).thenReturn(List.of());
    when(adapter.executeOperation(any(), eq("subscriptionStatus"), anyMap()))
        .thenReturn(Map.of("accepted", true, "attributes", Map.of("subscriptions", List.of())));
  }

  private Map<String, Object> config(String scope, List<String> ids) {
    return Map.of("enabled", true, "scope", scope, "deviceIds", ids, "topics", List.of("motion"),
        "callback", "http://localhost/api/video/sdk-events/callback");
  }
  @Test void enabledEmptySelectionReportsInactiveAndCannotBeSavedAsImplicitAll() {
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn("admin");
      instance.setOptions("{\"subscription\":{\"enabled\":true,\"topics\":[]}}");
      var status=service.getSdkSubscriptionStatus("dahua-test");
      assertEquals("inactive",status.get("status"));
      assertEquals("sdk_event_catalog",status.get("eventOptionsSource"));
      assertEquals("unknown",status.get("deviceEventCapabilityStatus"));
      assertEquals("none",((Map<?,?>)status.get("filtering")).get("emptyTopics"));
      var input=new java.util.LinkedHashMap<>(config("all",List.of()));input.put("topics",List.of());
      assertThrows(BaseException.class,()->service.saveSdkSubscription("dahua-test",input));
      verify(instances,never()).updateByPrimaryKeySelective(any());
    }
  }
  @Test void statusSeparatesSavedPolicyFromExplicitEffectiveDefaultsWithoutChangingIt() {
    try(MockedStatic<SecurityUtils> security=mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn("admin");
      var status=service.getSdkSubscriptionStatus("dahua-test");
      assertEquals(Map.of(),status.get("eventPolicy"));
      assertEquals(false,((Map<?,?>)status.get("eventPolicyEffective")).get("firstOnly"));
      assertEquals(5,((Map<?,?>)status.get("eventPolicyDefaults")).get("preSeconds"));
      assertEquals(60,((Map<?,?>)status.get("eventPolicyDefaults")).get("frameCooldownSeconds"));
      assertEquals(true,((Map<?,?>)status.get("eventPolicyDefaults")).get("snapshotFallbackEnabled"));
      instance.setOptions("{\"subscription\":{\"enabled\":false,\"eventPolicy\":{\"firstOnly\":true,\"preSeconds\":0,\"pictureSource\":\"none\",\"privateToken\":\"hidden\"}}}");
      status=service.getSdkSubscriptionStatus("dahua-test");
      assertEquals(Map.of("firstOnly",true,"preSeconds",0,"pictureSource","none"),status.get("eventPolicy"));
      assertEquals(0,((Map<?,?>)status.get("eventPolicyEffective")).get("preSeconds"));
      assertEquals(10,((Map<?,?>)status.get("eventPolicyEffective")).get("postSeconds"));
      assertFalse(status.toString().contains("hidden"));
      assertTrue(instance.getOptions().contains("hidden"));
    }
  }

  @Test void snapshotFallbackRequiresBooleanAndPersistsIndependentSubscriptionSetting() throws Exception {
    try(MockedStatic<SecurityUtils> security=mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn("admin");
      for(Object invalid:List.of(0,1,"false")) {
        var input=new java.util.LinkedHashMap<>(config("all",List.of()));
        input.put("eventPolicy",Map.of("snapshotFallbackEnabled",invalid));
        assertThrows(BaseException.class,()->service.saveSdkSubscription("dahua-test",input));
      }
      verify(instances,never()).updateByPrimaryKeySelective(any());
      for(boolean enabled:List.of(false,true)) {
        var input=new java.util.LinkedHashMap<>(config("all",List.of()));
        input.put("eventPolicy",Map.of("snapshotFallbackEnabled",enabled));
        var status=service.saveSdkSubscription("dahua-test",input);
        assertEquals(Map.of("snapshotFallbackEnabled",enabled),status.get("eventPolicy"));
        assertEquals(enabled,((Map<?,?>)status.get("eventPolicyEffective")).get("snapshotFallbackEnabled"));
        assertEquals(true,((Map<?,?>)status.get("eventPolicyDefaults")).get("snapshotFallbackEnabled"));
        assertEquals(enabled,json.readTree(instance.getOptions()).path("subscription").path("eventPolicy").path("snapshotFallbackEnabled").asBoolean());
        assertEquals("all",status.get("scope"));
        assertEquals(List.of("motion"),status.get("topics"));
      }
    }
  }

  @Test void frameCooldownMustBeBoundedIntegerAndPersistsWithoutChangingSharedDefaults() throws Exception {
    try(MockedStatic<SecurityUtils> security=mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn("admin");
      for(Object invalid:List.of(0,-1,3601,1.5,"60")) {
        var input=new java.util.LinkedHashMap<>(config("all",List.of()));input.put("eventPolicy",Map.of("pictureSource","platform_frame","frameCooldownSeconds",invalid));
        assertThrows(BaseException.class,()->service.saveSdkSubscription("dahua-test",input));
      }
      verify(instances,never()).updateByPrimaryKeySelective(any());
      for(int seconds:List.of(1,60,3600)) {
        var input=new java.util.LinkedHashMap<>(config("all",List.of()));input.put("eventPolicy",Map.of("pictureSource","platform_frame","frameCooldownSeconds",seconds));
        var status=service.saveSdkSubscription("dahua-test",input);
        assertEquals(seconds,((Map<?,?>)status.get("eventPolicy")).get("frameCooldownSeconds"));
        assertEquals(0,((Map<?,?>)status.get("eventPolicyEffective")).get("cooldownSeconds"));
        assertEquals(false,((Map<?,?>)status.get("eventPolicyEffective")).get("firstOnly"));
        assertEquals(seconds,json.readTree(instance.getOptions()).path("subscription").path("eventPolicy").path("frameCooldownSeconds").asInt());
      }
    }
  }

  @Test void allScopeCanBeSavedWithoutEnabledDevicesForFutureAdditions() throws Exception {
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn("admin");
      Map<String, Object> status = service.saveSdkSubscription("dahua-test", config("all", List.of()));
      assertEquals(true, json.readTree(instance.getOptions()).path("subscription").path("enabled").asBoolean());
      assertEquals("all", status.get("scope"));
      assertEquals(0, status.get("configuredDeviceCount"));
      assertFalse(status.containsKey("applyError"));
      verify(instances).updateByPrimaryKeySelective(instance);
      verify(adapter).unsubscribe(instance, List.of());
      verify(adapter, never()).subscribe(any(), anyList(), anyString());
    }
  }

  @Test void selectedScopeStillRejectsDisabledOrMissingDevices() {
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn("admin");
      assertThrows(BaseException.class, () -> service.saveSdkSubscription("dahua-test", config("selected", List.of("disabled"))));
      verify(instances, never()).updateByPrimaryKeySelective(any());
      verify(adapter, never()).subscribe(any(), anyList(), anyString());
    }
  }

  @Test void subscriptionInventoryExplainsDisabledDevicesWithoutIncludingThemInScope() {
    when(devices.selectByExample(any())).thenReturn(List.of(VideoPlatformDevice.builder()
        .deviceId("disabled-camera").deviceName("大华球机").deviceIp("192.0.2.1").devicePort(37777).enabled(0).build()));
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn("admin");
      var status = service.getSdkSubscriptionStatus("dahua-test");
      assertEquals(List.of(), status.get("availableDevices"));
      assertEquals(0, status.get("configuredDeviceCount"));
      assertEquals(List.of(Map.of("deviceId", "disabled-camera", "deviceName", "大华球机", "host", "192.0.2.1", "port", 37777)), status.get("disabledDevices"));
    }
  }

  @Test void deviceCommitUsesLatestSubscriptionConfigInsteadOfImportSnapshot() {
    TransactionSynchronizationManager.initSynchronization();
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn("admin");
      ReflectionTestUtils.invokeMethod(service, "applySdkSubscriptionAfterCommit", "dahua-test");
      instance.setOptions("{\"subscription\":{\"enabled\":false}}");
      TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
      verify(adapter, never()).subscribe(any(), anyList(), anyString());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }
}
