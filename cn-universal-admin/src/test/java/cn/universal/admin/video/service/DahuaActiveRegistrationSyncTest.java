package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.video.sync.DahuaSdkDeviceSyncHandler;
import cn.universal.dm.video.VideoPlatformAdapter;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformDeviceExt;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformDeviceExtMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

class DahuaActiveRegistrationSyncTest {
  private final VideoPlatformInstanceMapper instances = mock(VideoPlatformInstanceMapper.class);
  private final VideoPlatformAdapterRegistry adapters = mock(VideoPlatformAdapterRegistry.class);
  private final VideoPlatformAdapter adapter = mock(VideoPlatformAdapter.class);
  private final DahuaSdkDeviceSyncHandler handler = mock(DahuaSdkDeviceSyncHandler.class);
  private final VideoPlatformDeviceMapper devices = mock(VideoPlatformDeviceMapper.class);
  private final VideoPlatformDeviceExtMapper extensions = mock(VideoPlatformDeviceExtMapper.class);
  private final DahuaActiveRegistrationSync sync = new DahuaActiveRegistrationSync(instances, adapters, handler);
  @BeforeEach void setup() {
    EntityHelper.initEntityNameMap(VideoPlatformDevice.class, new Config());
    EntityHelper.initEntityNameMap(VideoPlatformDeviceExt.class, new Config());
    ReflectionTestUtils.setField(sync, "devices", devices);
    ReflectionTestUtils.setField(sync, "extensions", extensions);
    when(devices.selectByExample(any())).thenReturn(List.of());
    when(extensions.selectByExample(any())).thenReturn(List.of());
  }
  private VideoPlatformInstance saved() {
    var instance = VideoPlatformInstance.builder().id(1L).instanceKey("dh-a").platformType("dahua_sdk")
        .options("{\"activeRegistration\":{\"enabled\":true}}")
        .enabled(1).tenantId("tenant-a").creatorId("creator-a").build();
    when(instances.select(any())).thenReturn(List.of(instance)); when(instances.selectByPrimaryKey(1L)).thenReturn(instance);
    when(adapters.getAdapter("dahua_sdk")).thenReturn(adapter); return instance;
  }
  @Test void onlyAuthenticatedRegistrationAutomaticallyReachesTrustedTenant() throws Exception {
    var instance = saved(); var camera = Map.<String, Object>of("deviceId", "register-a", "authenticated", true, "status", "online");
    when(adapter.executeOperation(instance, "configureActiveRegistration", Map.of())).thenReturn(Map.of("status", "listening", "enabled", true));
    var awaiting = Map.<String, Object>of("deviceId", "register-b", "status", "awaiting_credentials");
    var invalid = Map.<String, Object>of("deviceId", "register-c", "authenticated", false, "status", "online");
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(camera, awaiting, invalid));
    sync.reconcileNow();
    verify(handler).syncRegisteredDevice(instance, camera); verifyNoMoreInteractions(handler);
    assertEquals(1, sync.metadata("dh-a").get("syncedDevices"));
  }
  @Test void firstReconcileStopsSavedDisabledListenerAfterBackendRestart() {
    var instance = saved(); instance.setEnabled(0);
    when(adapter.executeOperation(instance, "stopActiveRegistration", Map.of())).thenReturn(Map.of("status", "stopped", "enabled", false));
    sync.reconcileNow(); sync.reconcileNow();
    verify(adapter, times(1)).executeOperation(instance, "stopActiveRegistration", Map.of());
    verify(adapter, never()).listDevices(any(), anyMap()); verifyNoInteractions(handler);
  }
  @Test void directoryFailureLeavesCatalogUntouchedAndExposesRetryableError() {
    var instance = saved(); instance.setEndpoint("http://unchanged-gateway:5102");
    when(adapter.listDevices(eq(instance), anyMap()))
        .thenReturn(List.of()).thenThrow(new IllegalStateException("offline gateway"));
    when(adapter.executeOperation(instance, "configureActiveRegistration", Map.of())).thenReturn(Map.of("status", "listening", "enabled", true));
    sync.reconcileNow(); assertEquals("applied", sync.metadata("dh-a").get("applicationStatus"));
    sync.reconcileNow(); assertEquals("error", sync.metadata("dh-a").get("applicationStatus"));
    verifyNoInteractions(devices, extensions, handler);
    verify(adapter, never()).executeOperation(any(), eq("stopActiveRegistration"), anyMap());
  }

  @Test void configurationFailureWithoutListenerChangeLeavesCatalogUntouched() {
    var instance = saved(); instance.setEndpoint("http://unchanged-gateway:5102");
    when(adapter.executeOperation(instance, "configureActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "listening", "enabled", true))
        .thenThrow(new IllegalStateException("gateway unavailable"));
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of());
    sync.reconcileNow(); assertEquals("applied", sync.metadata("dh-a").get("applicationStatus"));

    sync.reconcileNow();

    assertEquals("error", sync.metadata("dh-a").get("applicationStatus"));
    verifyNoInteractions(devices, extensions, handler);
    verify(adapter, times(1)).listDevices(eq(instance), anyMap());
    verify(adapter, never()).executeOperation(any(), eq("stopActiveRegistration"), anyMap());
  }
  @Test void directInstanceWithoutRegistrationNeverStartsOrStopsListener() {
    var instance = saved(); instance.setOptions("{}"); sync.reconcileNow();
    verifyNoInteractions(adapter, handler, devices, extensions);
  }
  @Test void nativeStoppingListenerKeepsRetryingUntilItHasActuallyStopped() {
    var instance = saved(); instance.setEnabled(0);
    when(adapter.executeOperation(instance, "stopActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "stopping", "enabled", true)).thenReturn(Map.of("status", "stopped", "enabled", false));
    sync.reconcileNow(); assertEquals("stopping", sync.metadata("dh-a").get("applicationStatus"));
    sync.reconcileNow(); sync.reconcileNow();
    verify(adapter, times(2)).executeOperation(instance, "stopActiveRegistration", Map.of());
    assertEquals("stopped", sync.metadata("dh-a").get("applicationStatus"));
  }
  @Test void changingListenerConfigurationDoesNotReportAppliedWhileOldSessionsStop() {
    var instance = saved(); when(adapter.executeOperation(instance, "configureActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "stopping", "enabled", true));
    sync.reconcileNow(); assertEquals("stopping", sync.metadata("dh-a").get("applicationStatus"));
    verify(adapter, never()).listDevices(any(), anyMap()); verifyNoInteractions(handler);
  }
  @Test void disabledRegistrationOptionStopsSavedListenerAndReportsStoppedOnFreshProcess() {
    var instance = saved(); instance.setOptions("{\"activeRegistration\":{\"enabled\":false}}");
    when(adapter.executeOperation(instance, "stopActiveRegistration", Map.of())).thenReturn(Map.of("status", "stopped", "enabled", false));
    sync.reconcileNow(); assertEquals("stopped", sync.metadata("dh-a").get("applicationStatus"));
    verify(adapter, never()).listDevices(any(), anyMap()); verifyNoInteractions(handler);
  }
  @Test void movingGatewayWaitsForOldListenerReleaseBeforeStartingNewGateway() {
    var previous = saved(); previous.setEndpoint("http://old-gateway:5102");
    when(adapter.executeOperation(previous, "configureActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "listening", "enabled", true));
    when(adapter.listDevices(eq(previous), anyMap())).thenReturn(List.of()); sync.reconcileNow();
    var next = VideoPlatformInstance.builder().id(1L).instanceKey("dh-a").platformType("dahua_sdk")
        .options(previous.getOptions()).endpoint("http://new-gateway:5102")
        .enabled(1).tenantId("tenant-a").creatorId("creator-a").build();
    when(instances.selectByPrimaryKey(1L)).thenReturn(next);
    when(adapter.executeOperation(previous, "stopActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "stopping", "enabled", true));
    sync.reconcileNow(); assertEquals("stopping", sync.metadata("dh-a").get("applicationStatus"));
    verify(adapter, never()).executeOperation(next, "configureActiveRegistration", Map.of());
  }

  @Test void movingOptionGatewayWaitsForOldGatewayToStopBeforeApplyingNewGateway() {
    var previous = saved(); previous.setEndpoint("http://unchanged-endpoint:5102");
    previous.setOptions(registrationOptions("http://old-gateway:5102"));
    when(adapter.executeOperation(previous, "configureActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "listening", "enabled", true));
    when(adapter.listDevices(eq(previous), anyMap())).thenReturn(List.of());
    sync.reconcileNow();
    clearInvocations(adapter);

    var next = replacement(previous, previous.getEndpoint(), registrationOptions("http://new-gateway:5102"));
    when(instances.selectByPrimaryKey(1L)).thenReturn(next);
    when(adapter.executeOperation(previous, "stopActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "stopping", "enabled", true))
        .thenReturn(Map.of("status", "stopped", "enabled", false));
    when(adapter.executeOperation(next, "configureActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "listening", "enabled", true));
    when(adapter.listDevices(eq(next), anyMap())).thenReturn(List.of());

    sync.reconcileNow();
    assertEquals("stopping", sync.metadata("dh-a").get("applicationStatus"));
    verify(adapter, never()).executeOperation(same(next), eq("configureActiveRegistration"), anyMap());
    verify(adapter, never()).listDevices(same(next), anyMap());

    sync.reconcileNow();
    assertEquals("applied", sync.metadata("dh-a").get("applicationStatus"));
    var order = inOrder(adapter);
    order.verify(adapter, times(2)).executeOperation(same(previous), eq("stopActiveRegistration"), eq(Map.of()));
    order.verify(adapter).executeOperation(same(next), eq("configureActiveRegistration"), eq(Map.of()));
    order.verify(adapter).listDevices(same(next), anyMap());
    order.verifyNoMoreInteractions();
  }

  @Test void equivalentOptionGatewayUrlsDoNotStopAnExistingListener() {
    var previous = saved(); previous.setEndpoint("http://fallback:5102");
    previous.setOptions(registrationOptions("HTTP://GATEWAY.EXAMPLE:80/"));
    when(adapter.executeOperation(any(), eq("configureActiveRegistration"), anyMap()))
        .thenReturn(Map.of("status", "listening", "enabled", true));
    when(adapter.listDevices(any(), anyMap())).thenReturn(List.of());
    sync.reconcileNow();

    var next = replacement(previous, previous.getEndpoint(), registrationOptions("http://gateway.example"));
    when(instances.selectByPrimaryKey(1L)).thenReturn(next);
    sync.reconcileNow();

    verify(adapter, never()).executeOperation(any(), eq("stopActiveRegistration"), anyMap());
    verify(adapter).executeOperation(same(next), eq("configureActiveRegistration"), eq(Map.of()));
    assertEquals("applied", sync.metadata("dh-a").get("applicationStatus"));
  }

  @Test void changingFallbackEndpointDoesNotStopGatewaySelectedByOptions() {
    var previous = saved(); previous.setEndpoint("http://old-fallback:5102");
    previous.setOptions(registrationOptions("http://actual-gateway:5102"));
    when(adapter.executeOperation(any(), eq("configureActiveRegistration"), anyMap()))
        .thenReturn(Map.of("status", "listening", "enabled", true));
    when(adapter.listDevices(any(), anyMap())).thenReturn(List.of());
    sync.reconcileNow();

    var next = replacement(previous, "http://new-fallback:5102", previous.getOptions());
    when(instances.selectByPrimaryKey(1L)).thenReturn(next);
    sync.reconcileNow();

    verify(adapter, never()).executeOperation(any(), eq("stopActiveRegistration"), anyMap());
    verify(adapter).executeOperation(same(next), eq("configureActiveRegistration"), eq(Map.of()));
    assertEquals("applied", sync.metadata("dh-a").get("applicationStatus"));
  }

  @Test void movingBaseUrlAliasWaitsForOldGatewayWhenHigherPriorityAliasesAreBlank() {
    var previous = saved(); previous.setEndpoint("http://unchanged-fallback:5102");
    previous.setOptions("{\"gatewayBaseUrl\":\" \",\"gatewayEndpoint\":\"\","
        + "\"baseUrl\":\"http://old-base-gateway:5102\",\"activeRegistration\":{\"enabled\":true}}");
    when(adapter.executeOperation(previous, "configureActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "listening", "enabled", true));
    when(adapter.listDevices(eq(previous), anyMap())).thenReturn(List.of());
    sync.reconcileNow();
    clearInvocations(adapter);

    var next = replacement(previous, previous.getEndpoint(),
        "{\"gatewayBaseUrl\":\" \",\"gatewayEndpoint\":\"\","
            + "\"baseUrl\":\"http://new-base-gateway:5102\",\"activeRegistration\":{\"enabled\":true}}");
    when(instances.selectByPrimaryKey(1L)).thenReturn(next);
    when(adapter.executeOperation(previous, "stopActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "stopping", "enabled", true))
        .thenReturn(Map.of("status", "stopped", "enabled", false));
    when(adapter.executeOperation(next, "configureActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "listening", "enabled", true));
    when(adapter.listDevices(eq(next), anyMap())).thenReturn(List.of());

    sync.reconcileNow();
    assertEquals("stopping", sync.metadata("dh-a").get("applicationStatus"));
    verify(adapter, never()).executeOperation(same(next), eq("configureActiveRegistration"), anyMap());

    sync.reconcileNow();
    assertEquals("applied", sync.metadata("dh-a").get("applicationStatus"));
    var order = inOrder(adapter);
    order.verify(adapter, times(2)).executeOperation(same(previous), eq("stopActiveRegistration"), eq(Map.of()));
    order.verify(adapter).executeOperation(same(next), eq("configureActiveRegistration"), eq(Map.of()));
    order.verify(adapter).listDevices(same(next), anyMap());
    order.verifyNoMoreInteractions();
  }

  @Test void movingGatewayPathOnSameOriginStopsOldListenerBeforeConfiguringNewPath() {
    var previous = saved(); previous.setEndpoint("http://unchanged-fallback:5102");
    previous.setOptions(registrationOptions("http://shared-proxy:5102/gateway-a"));
    when(adapter.executeOperation(previous, "configureActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "listening", "enabled", true));
    when(adapter.listDevices(eq(previous), anyMap())).thenReturn(List.of());
    sync.reconcileNow();
    clearInvocations(adapter);

    var next = replacement(previous, previous.getEndpoint(),
        registrationOptions("http://shared-proxy:5102/gateway-b"));
    when(instances.selectByPrimaryKey(1L)).thenReturn(next);
    when(adapter.executeOperation(previous, "stopActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "stopped", "enabled", false));
    when(adapter.executeOperation(next, "configureActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "listening", "enabled", true));
    when(adapter.listDevices(eq(next), anyMap())).thenReturn(List.of());

    sync.reconcileNow();

    assertEquals("applied", sync.metadata("dh-a").get("applicationStatus"));
    var order = inOrder(adapter);
    order.verify(adapter).executeOperation(same(previous), eq("stopActiveRegistration"), eq(Map.of()));
    order.verify(adapter).executeOperation(same(next), eq("configureActiveRegistration"), eq(Map.of()));
    order.verify(adapter).listDevices(same(next), anyMap());
    order.verifyNoMoreInteractions();
  }

  @Test void movingListenerBindAndPortMarksExistingRegistrationsOfflineBeforeConfiguration() throws Exception {
    var previous = saved(); previous.setEndpoint("http://fixed-gateway:5102");
    previous.setOptions("{\"activeRegistration\":{\"enabled\":true,\"listenIp\":\"0.0.0.0\",\"listenPort\":9500}}");
    when(adapter.executeOperation(previous, "configureActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "listening", "enabled", true));
    when(adapter.listDevices(eq(previous), anyMap())).thenReturn(List.of());
    sync.reconcileNow();
    verifyNoInteractions(devices, extensions, handler);
    clearInvocations(adapter);

    var ipChanged = replacement(previous, previous.getEndpoint(),
        "{\"activeRegistration\":{\"enabled\":true,\"listenIp\":\"192.0.2.100\",\"listenPort\":9500}}");
    var portChanged = replacement(ipChanged, ipChanged.getEndpoint(),
        "{\"activeRegistration\":{\"enabled\":true,\"listenIp\":\"192.0.2.100\",\"listenPort\":9600}}");
    existingMixedCatalog();
    for (var next : List.of(ipChanged, portChanged)) {
      when(instances.selectByPrimaryKey(1L)).thenReturn(next);
      when(adapter.executeOperation(next, "configureActiveRegistration", Map.of()))
          .thenReturn(Map.of("status", "listening", "enabled", true));
      when(adapter.listDevices(eq(next), anyMap())).thenReturn(List.of());

      sync.reconcileNow();

      var order = inOrder(handler, adapter);
      order.verify(handler, times(2)).syncRegisteredDevice(same(next), anyMap());
      order.verify(adapter).executeOperation(same(next), eq("configureActiveRegistration"), eq(Map.of()));
      order.verify(adapter).listDevices(same(next), anyMap());
      order.verifyNoMoreInteractions();
      assertExistingRegistrationsMarkedOffline(next);
      assertCatalogQueriesStayWithinExistingInstanceDevices();
      assertEquals("applied", sync.metadata("dh-a").get("applicationStatus"));
      clearInvocations(adapter, handler, devices, extensions);
    }
  }

  @Test void movingEffectiveGatewayMarksExistingRegistrationsOfflineBeforeStoppingOldGateway() throws Exception {
    var previous = saved(); previous.setEndpoint("http://unchanged-fallback:5102");
    previous.setOptions(registrationOptions("http://shared-proxy:5102/gateway-a"));
    when(adapter.executeOperation(previous, "configureActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "listening", "enabled", true));
    when(adapter.listDevices(eq(previous), anyMap())).thenReturn(List.of());
    sync.reconcileNow();
    verifyNoInteractions(devices, extensions, handler);
    clearInvocations(adapter);

    var next = replacement(previous, previous.getEndpoint(),
        registrationOptions("http://shared-proxy:5102/gateway-b"));
    when(instances.selectByPrimaryKey(1L)).thenReturn(next);
    existingMixedCatalog();
    when(adapter.executeOperation(previous, "stopActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "stopped", "enabled", false));
    when(adapter.executeOperation(next, "configureActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "listening", "enabled", true));
    when(adapter.listDevices(eq(next), anyMap())).thenReturn(List.of());

    sync.reconcileNow();

    var order = inOrder(handler, adapter);
    order.verify(handler, times(2)).syncRegisteredDevice(same(next), anyMap());
    order.verify(adapter).executeOperation(same(previous), eq("stopActiveRegistration"), eq(Map.of()));
    order.verify(adapter).executeOperation(same(next), eq("configureActiveRegistration"), eq(Map.of()));
    order.verify(adapter).listDevices(same(next), anyMap());
    order.verifyNoMoreInteractions();
    assertExistingRegistrationsMarkedOffline(next);
    assertCatalogQueriesStayWithinExistingInstanceDevices();
    assertEquals("applied", sync.metadata("dh-a").get("applicationStatus"));
  }

  @Test void equivalentAliasesAndDefaultListenerValuesNeverMarkExistingDevicesOffline() {
    var previous = saved(); previous.setEndpoint("http://unused-fallback:5102");
    previous.setOptions(registrationOptions("HTTP://GATEWAY.EXAMPLE:80/sdk/"));
    when(adapter.executeOperation(any(), eq("configureActiveRegistration"), anyMap()))
        .thenReturn(Map.of("status", "listening", "enabled", true));
    when(adapter.listDevices(any(), anyMap())).thenReturn(List.of());
    sync.reconcileNow();

    var explicit = replacement(previous, previous.getEndpoint(),
        "{\"gatewayBaseUrl\":\"http://gateway.example/sdk\",\"activeRegistration\":{"
            + "\"enabled\":true,\"listenIp\":\" 0.0.0.0 \",\"listenPort\":9500}}");
    when(instances.selectByPrimaryKey(1L)).thenReturn(explicit);
    sync.reconcileNow();

    var stringPort = replacement(previous, previous.getEndpoint(),
        "{\"baseUrl\":\"http://gateway.example:80/sdk/\",\"activeRegistration\":{"
            + "\"enabled\":true,\"listenIp\":\"0.0.0.0\",\"listenPort\":\"9500\"}}");
    when(instances.selectByPrimaryKey(1L)).thenReturn(stringPort);
    sync.reconcileNow();

    verifyNoInteractions(devices, extensions, handler);
    verify(adapter, never()).executeOperation(any(), eq("stopActiveRegistration"), anyMap());
    verify(adapter).executeOperation(same(explicit), eq("configureActiveRegistration"), eq(Map.of()));
    verify(adapter).executeOperation(same(stringPort), eq("configureActiveRegistration"), eq(Map.of()));
    assertEquals("applied", sync.metadata("dh-a").get("applicationStatus"));
  }

  @Test void freshProcessDisablingRegistrationMarksOnlyExistingRegisteredDevicesOffline() throws Exception {
    var instance = saved(); instance.setOptions("{\"activeRegistration\":{\"enabled\":false}}");
    existingMixedCatalog();
    when(adapter.executeOperation(instance, "stopActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "stopped", "enabled", false));

    sync.reconcileNow();

    assertExistingRegistrationsMarkedOffline(instance);
    assertCatalogQueriesStayWithinExistingInstanceDevices();
    assertEquals("stopped", sync.metadata("dh-a").get("applicationStatus"));
    verify(adapter, never()).listDevices(any(), anyMap());
  }

  @Test void freshProcessDisablingInstanceMarksOnlyExistingRegisteredDevicesOffline() throws Exception {
    var instance = saved(); instance.setEnabled(0);
    existingMixedCatalog();
    when(adapter.executeOperation(instance, "stopActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "stopped", "enabled", false));

    sync.reconcileNow();

    assertExistingRegistrationsMarkedOffline(instance);
    assertCatalogQueriesStayWithinExistingInstanceDevices();
    assertEquals("stopped", sync.metadata("dh-a").get("applicationStatus"));
    verify(adapter, never()).listDevices(any(), anyMap());
  }

  @Test void freshProcessDisablingInstanceWithoutRegistrationOptionsStillMarksExistingRegistrationsOffline()
      throws Exception {
    var instance = saved(); instance.setEnabled(0); instance.setOptions("{}");
    existingMixedCatalog();

    sync.reconcileNow();

    assertExistingRegistrationsMarkedOffline(instance);
    assertCatalogQueriesStayWithinExistingInstanceDevices();
    verifyNoInteractions(adapter);
  }

  @Test void disablingRegistrationDoesNotRecreateDeviceWithOnlyAnOrphanedExtension() {
    var instance = saved(); instance.setOptions("{\"activeRegistration\":{\"enabled\":false}}");
    var direct = catalogDevice(13L, "direct-a", "直连设备", "192.0.2.13");
    when(devices.selectByExample(any())).thenReturn(List.of(direct));
    when(extensions.selectByExample(any())).thenReturn(List.of(
        catalogExtension(13L, "direct-a", "{\"loginMode\":\"direct\"}"),
        catalogExtension(99L, "deleted-registration", "{\"loginMode\":\"active_registration\"}")));
    when(adapter.executeOperation(instance, "stopActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "stopped", "enabled", false));

    sync.reconcileNow();

    verifyNoInteractions(handler);
    verify(devices, never()).insertSelective(any());
    verify(extensions, never()).insertSelective(any());
    verify(adapter, never()).listDevices(any(), anyMap());
    assertEquals("ON", direct.getDeviceStatus());
  }

  @Test void disabledRegistrationMarksCatalogOfflineEvenWhenGatewayCannotBeStopped() throws Exception {
    var instance = saved(); instance.setOptions("{\"activeRegistration\":{\"enabled\":false}}");
    existingMixedCatalog();
    when(adapter.executeOperation(instance, "stopActiveRegistration", Map.of()))
        .thenThrow(new IllegalStateException("gateway unavailable"));

    sync.reconcileNow();

    assertExistingRegistrationsMarkedOffline(instance);
    assertEquals("error", sync.metadata("dh-a").get("applicationStatus"));
    verify(adapter, never()).listDevices(any(), anyMap());
  }

  @Test void oneOfflineDeviceFailureStillMarksOtherRegistrationsAndStopsNativeListener() throws Exception {
    var instance = saved(); instance.setOptions("{\"activeRegistration\":{\"enabled\":false}}");
    existingMixedCatalog();
    doThrow(new IllegalStateException("device persistence failed"))
        .when(handler).syncRegisteredDevice(same(instance),
            argThat(device -> "register-login".equals(device.get("deviceId"))));
    when(adapter.executeOperation(instance, "stopActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "stopped", "enabled", false));

    sync.reconcileNow();

    var order = inOrder(handler, adapter);
    order.verify(handler).syncRegisteredDevice(same(instance),
        argThat(device -> "register-login".equals(device.get("deviceId"))));
    order.verify(handler).syncRegisteredDevice(same(instance),
        argThat(device -> "register-access".equals(device.get("deviceId"))
            && Boolean.FALSE.equals(device.get("online")) && "OFF".equals(device.get("status"))));
    order.verify(adapter).executeOperation(same(instance), eq("stopActiveRegistration"), eq(Map.of()));
    order.verifyNoMoreInteractions();
    assertExistingRegistrationsMarkedOffline(instance);
    assertEquals("error", sync.metadata("dh-a").get("applicationStatus"));
    verify(adapter, never()).listDevices(any(), anyMap());
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  void disablingRegistrationPreservesMultichannelMetadataWhileClearingEveryOnlineFlag() throws Exception {
    var instance = saved(); instance.setOptions("{\"activeRegistration\":{\"enabled\":false}}");
    var json = new ObjectMapper();
    var originalChannels = List.of(
        Map.<String, Object>of("channelId", "0", "channelNo", 0, "channelName", "仓库入口",
            "channelStatus", "ON", "status", "online", "online", true, "isOnline", "1",
            "streamConfig", Map.of("transport", "TCP", "streamType", "main", "customProfile", "warehouse")),
        Map.<String, Object>of("channelId", "1", "channelNo", 1, "channelName", "装卸区域",
            "channelStatus", "ON", "status", "online", "online", true, "isOnline", "1",
            "streamConfig", Map.of("transport", "UDP", "streamType", "sub", "customProfile", "loading")));
    var configuration = json.writeValueAsString(Map.of("channelList", originalChannels,
        "fusion", Map.of("enabled", true, "productKey", "existing-product")));
    var registered = catalogDevice(11L, "register-multi", "多通道注册设备", "192.0.2.11");
    registered.setConfiguration(configuration); registered.setEnabled(0);
    registered.setDeviceModel("DH-NVR"); registered.setManufacturer("Dahua");
    var extension = catalogExtension(11L, "register-multi",
        "{\"loginMode\":\"active_registration\",\"firmwareVersion\":\"V4.1\",\"channelTotal\":2,"
            + "\"gatewayEndpoint\":\"http://registered-gateway:5102\","
            + "\"deviceStatus\":\"ON\",\"status\":\"online\",\"online\":true,\"isOnline\":\"1\"}");
    extension.setDeviceSn("DH-NVR-SERIAL"); extension.setMediaServerId("existing-media-node");
    extension.setDeviceCapabilitySet("preview,playback,ptz");
    when(devices.selectByExample(any())).thenReturn(List.of(registered));
    when(extensions.selectByExample(any())).thenReturn(List.of(extension));
    when(adapter.executeOperation(instance, "stopActiveRegistration", Map.of()))
        .thenReturn(Map.of("status", "stopped", "enabled", false));

    sync.reconcileNow();

    ArgumentCaptor<Map<String, Object>> capture = (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
    verify(handler).syncRegisteredDevice(same(instance), capture.capture());
    var data = json.valueToTree(capture.getValue());
    assertEquals("register-multi", data.path("deviceId").asText());
    assertEquals("DH-NVR-SERIAL", data.path("deviceSn").asText());
    assertEquals("existing-media-node", data.path("mediaServerId").asText());
    assertEquals("preview,playback,ptz", data.path("deviceCapabilitySet").asText());
    assertEquals("DH-NVR", data.path("deviceModel").asText());
    assertEquals("Dahua", data.path("manufacturer").asText());
    assertEquals("V4.1", data.path("firmwareVersion").asText());
    assertEquals(2, data.path("channelTotal").asInt());
    assertEquals("http://registered-gateway:5102", data.path("gatewayEndpoint").asText());
    assertEquals("OFF", data.path("deviceStatus").asText());
    assertEquals("OFF", data.path("status").asText());
    assertEquals("0", data.path("isOnline").asText());
    assertEquals(json.valueToTree(false), data.path("online"));
    var channels = data.path("channelList");
    assertTrue(channels.isArray()); assertEquals(2, channels.size());
    for (int index = 0; index < 2; index++) {
      var channel = channels.get(index); var original = originalChannels.get(index);
      assertEquals(String.valueOf(index), channel.path("channelId").asText());
      assertEquals(index, channel.path("channelNo").asInt());
      assertEquals(original.get("channelName"), channel.path("channelName").asText());
      assertEquals(json.valueToTree(original.get("streamConfig")), channel.path("streamConfig"));
      assertEquals("offline", channel.path("channelStatus").asText());
      assertEquals("offline", channel.path("status").asText());
      assertEquals("0", channel.path("isOnline").asText());
      assertEquals(json.valueToTree(false), channel.path("online"));
    }
    assertEquals(0, registered.getEnabled()); assertEquals(configuration, registered.getConfiguration());
    assertTrue(json.readTree(registered.getConfiguration()).path("fusion").path("enabled").asBoolean());
    verifyNoMoreInteractions(handler);
    verify(adapter, never()).listDevices(any(), anyMap());
  }

  private String registrationOptions(String gatewayEndpoint) {
    return "{\"gatewayEndpoint\":\"" + gatewayEndpoint + "\",\"activeRegistration\":{\"enabled\":true}}";
  }

  private VideoPlatformInstance replacement(VideoPlatformInstance previous, String endpoint, String options) {
    return VideoPlatformInstance.builder().id(previous.getId()).instanceKey(previous.getInstanceKey())
        .platformType(previous.getPlatformType()).endpoint(endpoint).options(options)
        .enabled(previous.getEnabled()).tenantId(previous.getTenantId()).creatorId(previous.getCreatorId()).build();
  }

  private VideoPlatformDevice catalogDevice(Long id, String deviceId, String name, String ip) {
    return VideoPlatformDevice.builder().id(id).instanceKey("dh-a").deviceId(deviceId).deviceName(name)
        .deviceIp(ip).devicePort(37777).deviceStatus("ON").enabled(1)
        .tenantId("tenant-a").createId("creator-a").build();
  }

  private VideoPlatformDeviceExt catalogExtension(Long id, String deviceId, String metadata) {
    return VideoPlatformDeviceExt.builder().deviceId(id).instanceKey("dh-a")
        .platformDeviceId(deviceId).extField3(metadata).build();
  }

  private void existingMixedCatalog() {
    when(devices.selectByExample(any())).thenReturn(List.of(
        catalogDevice(11L, "register-login", "登录模式注册设备", "192.0.2.11"),
        catalogDevice(12L, "register-access", "接入模式注册设备", "192.0.2.12"),
        catalogDevice(13L, "direct-a", "直连设备", "192.0.2.13"),
        catalogDevice(14L, "legacy-direct", "旧直连设备", "192.0.2.14")));
    when(extensions.selectByExample(any())).thenReturn(List.of(
        catalogExtension(11L, "register-login", "{\"loginMode\":\"active_registration\"}"),
        catalogExtension(12L, "register-access", "{\"accessMode\":\"active_registration\"}"),
        catalogExtension(13L, "direct-a", "{\"loginMode\":\"direct\"}"),
        catalogExtension(99L, "deleted-registration", "{\"loginMode\":\"active_registration\"}")));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void assertExistingRegistrationsMarkedOffline(VideoPlatformInstance instance) throws Exception {
    ArgumentCaptor<Map<String, Object>> capture = (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
    verify(handler, times(2)).syncRegisteredDevice(same(instance), capture.capture());
    var offline = capture.getAllValues();
    assertEquals(java.util.Set.of("register-login", "register-access"),
        offline.stream().map(device -> device.get("deviceId")).collect(java.util.stream.Collectors.toSet()));
    for (Map<String, Object> device : offline) {
      assertEquals("active_registration", device.get("loginMode"));
      assertEquals("active_registration", device.get("accessMode"));
      assertEquals(false, device.get("online"));
      assertEquals("OFF", device.get("status"));
      boolean login = "register-login".equals(device.get("deviceId"));
      assertEquals(login ? "登录模式注册设备" : "接入模式注册设备", device.get("deviceName"));
      assertEquals(login ? "192.0.2.11" : "192.0.2.12", device.get("deviceIp"));
      assertEquals(37777, device.get("devicePort"));
    }
    verifyNoMoreInteractions(handler);
  }

  private void assertCatalogQueriesStayWithinExistingInstanceDevices() {
    var deviceQuery = ArgumentCaptor.forClass(Example.class);
    verify(devices).selectByExample(deviceQuery.capture());
    assertTrue(deviceQuery.getValue().getOredCriteria().getFirst().getAllCriteria().stream()
        .anyMatch(criterion -> criterion.getCondition().startsWith("instance_key")
            && "dh-a".equals(criterion.getValue())));
    var extensionQuery = ArgumentCaptor.forClass(Example.class);
    verify(extensions).selectByExample(extensionQuery.capture());
    assertTrue(extensionQuery.getValue().getOredCriteria().getFirst().getAllCriteria().stream()
        .anyMatch(criterion -> criterion.getCondition().startsWith("device_id")
            && criterion.getValue() instanceof Collection<?> ids
            && ids.size() == 4 && ids.containsAll(List.of(11L, 12L, 13L, 14L))));
  }
}
