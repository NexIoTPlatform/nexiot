package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.video.sync.HikIsupDeviceSyncHandler;
import cn.universal.dm.video.VideoPlatformAdapter;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HikIsupRegistrationSyncTest {
  private final VideoPlatformInstanceMapper instances = mock(VideoPlatformInstanceMapper.class);
  private final VideoPlatformAdapterRegistry adapters = mock(VideoPlatformAdapterRegistry.class);
  private final VideoPlatformAdapter adapter = mock(VideoPlatformAdapter.class);
  private final HikIsupDeviceSyncHandler handler = mock(HikIsupDeviceSyncHandler.class);
  private final HikIsupRegistrationSync sync = new HikIsupRegistrationSync(instances, adapters, handler);
  private VideoPlatformInstance instance(int enabled) {
    return VideoPlatformInstance.builder().id(1L).instanceKey("isup-a").platformType("hik_isup")
        .enabled(enabled).tenantId("tenant-a").creatorId("creator-a").build();
  }

  @Test void registeredDeviceIsImportedWithoutUserSessionOrManualBinding() throws Exception {
    var instance = instance(1); var camera = Map.<String, Object>of("deviceId", "camera-a", "status", "ON");
    when(instances.select(any())).thenReturn(List.of(instance)); when(instances.selectByPrimaryKey(1L)).thenReturn(instance);
    when(adapters.getAdapter("hik_isup")).thenReturn(adapter); when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(camera));
    sync.reconcileNow();
    verify(adapter).executeOperation(instance, "configure", Map.of());
    verify(handler).syncRegisteredDevice(instance, camera);
    assertEquals(1, sync.metadata("isup-a").get("syncedDevices"));
  }

  @Test void rechecksDisabledInstanceBeforeStartingAndStopsOnlyThatInstance() {
    var snapshot = instance(1); var latest = instance(0);
    when(instances.select(any())).thenReturn(List.of(snapshot)); when(instances.selectByPrimaryKey(1L)).thenReturn(latest);
    when(adapters.getAdapter("hik_isup")).thenReturn(adapter);
    sync.reconcileNow(); sync.reconcileNow();
    verify(adapter, times(1)).executeOperation(latest, "stopServices", Map.of());
    verify(adapter, never()).listDevices(any(), anyMap()); verifyNoInteractions(handler);
  }

  @Test void directoryFailureDoesNotDisableOrDeleteAnyDevice() {
    var instance = instance(1);
    when(instances.select(any())).thenReturn(List.of(instance)); when(instances.selectByPrimaryKey(1L)).thenReturn(instance);
    when(adapters.getAdapter("hik_isup")).thenReturn(adapter);
    when(adapter.listDevices(eq(instance), anyMap())).thenThrow(new IllegalStateException("unavailable"));
    sync.reconcileNow();
    assertTrue(sync.metadata("isup-a").containsKey("syncError")); verifyNoInteractions(handler);
  }
}
