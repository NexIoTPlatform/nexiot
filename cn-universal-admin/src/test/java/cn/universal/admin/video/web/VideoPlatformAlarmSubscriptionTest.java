package cn.universal.admin.video.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.video.service.VideoPlatformInstanceService;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.security.utils.SecurityUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class VideoPlatformAlarmSubscriptionTest {
  private final VideoPlatformInstanceService service = mock(VideoPlatformInstanceService.class);
  private final VideoPlatformController controller = mock(VideoPlatformController.class, CALLS_REAL_METHODS);

  private void setup(String platformType, String tenantId) {
    ReflectionTestUtils.setField(controller, "instanceService", service);
    doReturn(IoTUser.builder().identity(0).unionId("admin").build())
        .when(controller).loginIoTUnionUser(any());
    when(service.getByInstanceKey("camera-platform")).thenReturn(VideoPlatformInstance.builder()
        .instanceKey("camera-platform").platformType(platformType).tenantId(tenantId).build());
  }

  @Test void isupLegacyRoutesUsePersistedSdkSubscriptionAndExposeEventCatalog() {
    setup("hik_isup", "tenant-one");
    Map<String, Object> config = Map.of("enabled", true, "scope", "all",
        "topics", List.of("motion"), "callback", "http://example.test/api/video/sdk-events/callback");
    Map<String, Object> status = Map.of("enabled", true, "topics", List.of("motion"),
        "eventOptions", List.of(Map.of("value", "motion")), "runtimeCount", 1);
    when(service.getSdkSubscriptionStatus("camera-platform")).thenReturn(status);
    when(service.saveSdkSubscription("camera-platform", config)).thenReturn(status);
    try (var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionId).thenReturn("admin");
      assertSame(status, controller.alarmSubscription("camera-platform").getData());
      assertSame(status, controller.saveAlarmSubscription("camera-platform", config).getData());
    }
    verify(service).getSdkSubscriptionStatus("camera-platform");
    verify(service).saveSdkSubscription("camera-platform", config);
    verify(service, never()).executePlatformOperation(anyString(), anyString(), anyMap());
  }

  @Test void legacyDirectSdkRoutesRetainTheirExistingOperations() {
    for (String platformType : List.of("hik_sdk", "dahua_sdk")) {
      reset(service);
      setup(platformType, "tenant-one");
      Map<String, Object> config = Map.of("enabled", false);
      Map<String, Object> status = Map.of("enabled", false);
      when(service.executePlatformOperation(eq("camera-platform"), anyString(), anyMap())).thenReturn(status);
      try (var security = mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getUnionId).thenReturn("admin");
        assertSame(status, controller.alarmSubscription("camera-platform").getData());
        assertSame(status, controller.saveAlarmSubscription("camera-platform", config).getData());
      }
      verify(service).executePlatformOperation("camera-platform", "alarmSubscriptionStatus", Map.of());
      verify(service).executePlatformOperation("camera-platform", "saveAlarmSubscription", config);
      verify(service, never()).saveSdkSubscription(anyString(), anyMap());
      verify(service, never()).getSdkSubscriptionStatus(anyString());
    }
  }

  @Test void isupLegacyRoutesRejectCrossTenantAccessBeforeSubscriptionOperations() {
    setup("hik_isup", "tenant-two");
    doReturn(IoTUser.builder().identity(1).unionId("viewer").tenantId("tenant-one").build())
        .when(controller).loginIoTUnionUser(any());
    try (var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionId).thenReturn("viewer");
      assertThrows(BaseException.class, () -> controller.alarmSubscription("camera-platform"));
      assertThrows(BaseException.class, () -> controller.saveAlarmSubscription("camera-platform", Map.of()));
    }
    verify(service, never()).getSdkSubscriptionStatus(anyString());
    verify(service, never()).saveSdkSubscription(anyString(), anyMap());
    verify(service, never()).executePlatformOperation(anyString(), anyString(), anyMap());
  }

  @Test void isupLegacySavePropagatesTopicAndCallbackValidation() {
    setup("hik_isup", "tenant-one");
    Map<String, Object> config = Map.of("enabled", true, "topics", List.of("unsupported"));
    when(service.saveSdkSubscription("camera-platform", config))
        .thenThrow(new BaseException("包含不支持的告警类型"));
    try (var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionId).thenReturn("admin");
      assertThrows(BaseException.class, () -> controller.saveAlarmSubscription("camera-platform", config));
    }
    verify(service, never()).executePlatformOperation(anyString(), anyString(), anyMap());
  }

  @Test void isupGenericSubscribeCannotBypassDedicatedPermissionOrDeviceScope() {
    setup("hik_isup", "tenant-one");
    try (var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionId).thenReturn("admin");
      for (String operation : List.of("subscribe", "unsubscribe", " Subscribe ", " UNSUBSCRIBE ")) {
        assertThrows(BaseException.class, () -> controller.executePlatformOperation(
            "camera-platform", operation, Map.of("devices", List.of(Map.of("deviceId", "outside-scope")))));
      }
    }
    verify(service, never()).executePlatformOperation(anyString(), anyString(), anyMap());
  }

  @Test void existingDirectGenericSubscribeKeepsItsContract() {
    setup("hik_sdk", "tenant-one");
    Map<String, Object> body = Map.of("devices", List.of());
    Map<String, Object> result = Map.of("accepted", true);
    when(service.executePlatformOperation("camera-platform", "subscribe", body)).thenReturn(result);
    try (var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionId).thenReturn("admin");
      assertSame(result, controller.executePlatformOperation("camera-platform", "subscribe", body).getData());
    }
    verify(service).executePlatformOperation("camera-platform", "subscribe", body);
  }
}
