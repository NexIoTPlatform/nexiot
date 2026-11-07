package cn.universal.admin.video.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.universal.admin.video.sync.DeviceSyncHandler;
import cn.universal.common.exception.BaseException;
import cn.universal.common.exception.IoTException;
import cn.universal.dm.video.VideoPlatformAdapter;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformDeviceExt;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceExtMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.security.service.IoTUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import tk.mybatis.mapper.mapperhelper.EntityHelper;
import tk.mybatis.mapper.entity.Config;

class VideoPlatformInstanceServiceImplDeviceChannelSyncTest {

  private static final String SERIAL = "SN-NVR-1";

  private final VideoPlatformInstanceServiceImpl service =
      mock(VideoPlatformInstanceServiceImpl.class, CALLS_REAL_METHODS);
  private final VideoPlatformAdapter adapter = mock(VideoPlatformAdapter.class);
  private final VideoPlatformDeviceMapper deviceMapper = mock(VideoPlatformDeviceMapper.class);
  private final VideoPlatformDeviceExtMapper deviceExtMapper = mock(VideoPlatformDeviceExtMapper.class);
  private final VideoPlatformChannelMapper channelMapper = mock(VideoPlatformChannelMapper.class);
  private final IoTUserService users = mock(IoTUserService.class);
  private final cn.universal.admin.video.service.DahuaIpcChannelStatusRefresh ipcStatus =
      mock(cn.universal.admin.video.service.DahuaIpcChannelStatusRefresh.class);
  private final VideoPlatformInstance instance = VideoPlatformInstance.builder()
      .instanceKey("hik-sdk-123")
      .tenantId("tenant")
      .platformType("hik_sdk")
      .build();
  private final VideoPlatformDevice device = VideoPlatformDevice.builder()
      .id(11L)
      .instanceKey("hik-sdk-123")
      .tenantId("tenant")
      .deviceId("nvr-1")
      .deviceName("NVR")
      .deviceIp("192.168.31.114")
      .devicePort(8000)
      .build();
  private final VideoPlatformDeviceExt extension = VideoPlatformDeviceExt.builder()
      .deviceId(device.getId())
      .instanceKey(instance.getInstanceKey())
      .deviceSn(SERIAL)
      .extField3("{\"username\":\"admin\",\"password\":\"stored-secret\"}")
      .build();

  @BeforeEach
  void setup() {
    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
        Jwt.withTokenValue("test-token").header("alg", "none").subject("tenant-member")
            .claim("tenantId", "tenant").build(), List.of()));
    when(users.selectUserByUnionId("tenant-member")).thenReturn(IoTUser.builder()
        .unionId("tenant-member").identity(1).tenantId("tenant").tenantRole("ADMIN").build());
    EntityHelper.initEntityNameMap(VideoPlatformDevice.class, new Config());
    EntityHelper.initEntityNameMap(VideoPlatformDeviceExt.class, new Config());
    EntityHelper.initEntityNameMap(VideoPlatformChannel.class, new Config());
    var adapterRegistry = mock(VideoPlatformAdapterRegistry.class);
    when(adapterRegistry.getAdapter(anyString())).thenReturn(adapter);
    ReflectionTestUtils.setField(service, "adapterRegistry", adapterRegistry);
    ReflectionTestUtils.setField(service, "deviceMapper", deviceMapper);
    ReflectionTestUtils.setField(service, "deviceExtMapper", deviceExtMapper);
    ReflectionTestUtils.setField(service, "channelMapper", channelMapper);
    ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
    ReflectionTestUtils.setField(service, "ioTUserService", users);
    ReflectionTestUtils.setField(service, "dahuaIpcChannelStatusRefresh", ipcStatus);
    doReturn(instance).when(service).getByInstanceKey("hik-sdk-123");
    when(deviceMapper.selectByExample(any())).thenReturn(List.of(device));
    when(deviceExtMapper.selectByExample(any())).thenReturn(List.of(extension));
    when(channelMapper.selectCountByExample(any())).thenReturn(2, 3);
    doReturn(List.of()).when(service).listChannels(eq("hik-sdk-123"), eq("nvr-1"), anyMap());
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @ParameterizedTest
  @ValueSource(strings = {"hik_sdk", "dahua_sdk", "tiandy_sdk", "uniview_sdk"})
  void directSdkRefreshReadsSavedNvrCredentialsAndSyncsOnlyThatDevice(String platformType) {
    instance.setPlatformType(platformType);
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(discoveredDevice(Map.of())));
    stubSuccessfulSync(1);

    Map<String, Object> result = service.syncDeviceChannels("hik-sdk-123", "nvr-1");

    assertEquals("hik-sdk-123", result.get("instanceKey"));
    assertEquals("nvr-1", result.get("deviceId"));
    assertEquals(2, result.get("beforeChannelCount"));
    assertEquals(3, result.get("afterChannelCount"));
    Map<?, ?> requestedDevice = capturedProbeDevice();
    assertEquals("192.168.31.114", requestedDevice.get("host"));
    assertEquals("admin", requestedDevice.get("username"));
    assertEquals("stored-secret", requestedDevice.get("password"));
    assertEquals(SERIAL, requestedDevice.get("deviceSn"));
    if ("dahua_sdk".equals(platformType)) {
      assertEquals("direct", requestedDevice.get("loginMode"));
      assertEquals("direct", requestedDevice.get("accessMode"));
    }
    Map<?, ?> selectedDevice = capturedSyncDevice();
    assertEquals("nvr-1", selectedDevice.get("deviceId"));
    assertEquals("nvr-1", selectedDevice.get("extDeviceId"));
    assertEquals(List.of(Map.of("channelId", "2", "channelName", "新通道")), selectedDevice.get("channelList"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"hik_sdk", "tiandy_sdk"})
  void incompleteSdkCatalogDoesNotWriteChannels(String platformType) {
    instance.setPlatformType(platformType);
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(discoveredDevice(Map.of(
        "channelCatalogAuthoritative", false, "channelList", List.of()))));

    BaseException error = assertThrows(BaseException.class,
        () -> service.syncDeviceChannels("hik-sdk-123", "nvr-1"));

    assertTrue(error.getMessage().contains("未能确认完整通道目录"));
    verifyNoSync();
  }

  @Test
  void singleDifferentDeviceIdCannotReplaceCurrentNvrEvenWithAnAuthoritativeEmptyCatalog() {
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(discoveredDevice(Map.of(
        "deviceId", "another-nvr", "channelList", List.of()))));

    assertThrows(BaseException.class, () -> service.syncDeviceChannels("hik-sdk-123", "nvr-1"));

    verifyNoSync();
  }

  @Test
  void matchingDeviceIdWithAnotherAddressCannotReplaceCurrentNvr() {
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(discoveredDevice(Map.of(
        "deviceIp", "192.168.31.115"))));

    assertThrows(BaseException.class, () -> service.syncDeviceChannels("hik-sdk-123", "nvr-1"));

    verifyNoSync();
  }

  @ParameterizedTest
  @ValueSource(strings = {"SN-ANOTHER-NVR", ""})
  void mismatchedOrMissingSerialCannotReplaceAnNvrWithAKnownSerial(String serial) {
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(discoveredDevice(Map.of(
        "serialNumber", serial))));

    assertThrows(BaseException.class, () -> service.syncDeviceChannels("hik-sdk-123", "nvr-1"));

    verifyNoSync();
  }

  @ParameterizedTest
  @ValueSource(strings = {"hik_sdk", "tiandy_sdk"})
  void authoritativeEmptyCatalogIsPassedThroughToRemoveChannelsFromTheCurrentNvr(String platformType) {
    instance.setPlatformType(platformType);
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(discoveredDevice(Map.of(
        "channelList", List.of()))));
    stubSuccessfulSync(0);
    when(channelMapper.selectCountByExample(any())).thenReturn(2, 0);

    Map<String, Object> result = service.syncDeviceChannels("hik-sdk-123", "nvr-1");

    assertEquals(2, result.get("beforeChannelCount"));
    assertEquals(0, result.get("afterChannelCount"));
    Map<?, ?> selectedDevice = capturedSyncDevice();
    assertEquals("nvr-1", selectedDevice.get("deviceId"));
    assertEquals(Boolean.TRUE, selectedDevice.get("channelCatalogAuthoritative"));
    assertEquals(List.of(), selectedDevice.get("channelList"));
  }

  @Test
  void instanceCredentialModeOmitsLegacyDeviceCredentialsFromProbeAndSync() {
    extension.setExtField3("{\"credentialMode\":\"instance\",\"username\":\"old-admin\",\"password\":\"old-secret\"}");
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(discoveredDevice(Map.of())));
    stubSuccessfulSync(1);

    service.syncDeviceChannels("hik-sdk-123", "nvr-1");

    for (Map<?, ?> payload : List.of(capturedProbeDevice(), capturedSyncDevice())) {
      assertEquals("instance", payload.get("credentialMode"));
      assertFalse(payload.containsKey("username"));
      assertFalse(payload.containsKey("password"));
    }
  }

  @Test
  void failedProbeDoesNotStartSync() {
    when(adapter.listDevices(eq(instance), anyMap())).thenThrow(new BaseException("SDK登录失败"));

    assertThrows(BaseException.class, () -> service.syncDeviceChannels("hik-sdk-123", "nvr-1"));

    verifyNoSync();
  }

  @Test
  void emptyProbeDoesNotStartSync() {
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of());

    assertThrows(BaseException.class, () -> service.syncDeviceChannels("hik-sdk-123", "nvr-1"));

    verifyNoSync();
  }

  @Test
  void multipleDiscoveredDevicesStillSyncOnlyTheRequestedNvr() {
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(
        discoveredDevice(Map.of("deviceId", "another-nvr", "deviceIp", "192.168.31.115")),
        discoveredDevice(Map.of())));
    stubSuccessfulSync(1);

    service.syncDeviceChannels("hik-sdk-123", "nvr-1");

    assertEquals("nvr-1", capturedSyncDevice().get("deviceId"));
  }

  @Test
  void anotherTenantCannotProbeOrSyncTheInstance() {
    instance.setTenantId("other-tenant");

    assertThrows(IoTException.class, () -> service.syncDeviceChannels("hik-sdk-123", "nvr-1"));

    verifyNoInteractions(adapter, deviceMapper, deviceExtMapper, channelMapper);
    verifyNoSync();
  }

  @Test
  void onvifRefreshKeepsItsAuthenticatedSingleDeviceProbeOptions() {
    instance.setPlatformType("onvif");
    stubSuccessfulSync(1);

    service.syncDeviceChannels("hik-sdk-123", "nvr-1");

    verify(service).forceSyncDevices(eq("hik-sdk-123"), argThat(filters ->
        Boolean.TRUE.equals(filters.get("skipWsDiscovery"))
            && Boolean.FALSE.equals(filters.get("useCache"))
            && Boolean.TRUE.equals(filters.get("requireAuthVerified"))
            && Boolean.TRUE.equals(filters.get("authVerifiedOnly"))
            && filters.get("devices") instanceof List<?> selected
            && selected.size() == 1
            && "nvr-1".equals(((Map<?, ?>) selected.getFirst()).get("deviceId"))));
    verifyNoInteractions(adapter);
  }

  @ParameterizedTest
  @ValueSource(strings = {"dahua_sdk", "hik_isup"})
  void registeredDeviceUsesItsCurrentSessionWithoutRequiringOrComparingASourceAddress(String platformType) {
    configureRegistration(platformType);
    device.setDeviceIp(null);
    device.setDevicePort(null);
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(discoveredDevice(Map.of(
        "deviceIp", "203.0.113.22", "registrationId", "registered-nvr", "authenticated", true))));
    stubSuccessfulSync(1);

    service.syncDeviceChannels("hik-sdk-123", "nvr-1");

    assertEquals("registered-nvr", capturedProbeDevice().get("registrationId"));
    assertEquals("nvr-1", capturedSyncDevice().get("deviceId"));
    assertEquals("203.0.113.22", capturedSyncDevice().get("deviceIp"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"dahua_sdk", "hik_isup"})
  void offlineRegisteredCatalogCannotDeleteChannels(String platformType) {
    configureRegistration(platformType);
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(discoveredDevice(Map.of(
        "deviceStatus", "OFF", "channelList", List.of(),
        "registrationId", "registered-nvr", "authenticated", true))));

    assertThrows(BaseException.class, () -> service.syncDeviceChannels("hik-sdk-123", "nvr-1"));

    verifyNoSync();
  }

  @ParameterizedTest
  @ValueSource(strings = {"dahua_sdk", "hik_isup"})
  void incompleteRegisteredCatalogCannotDeleteChannels(String platformType) {
    configureRegistration(platformType);
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(discoveredDevice(Map.of(
        "channelCatalogAuthoritative", false, "channelList", List.of(),
        "registrationId", "registered-nvr", "authenticated", true))));

    assertThrows(BaseException.class, () -> service.syncDeviceChannels("hik-sdk-123", "nvr-1"));

    verifyNoSync();
  }

  @ParameterizedTest
  @ValueSource(strings = {"dahua_sdk", "hik_isup"})
  void completeRegisteredEmptyCatalogRemovesDeletedChannels(String platformType) {
    configureRegistration(platformType);
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(discoveredDevice(Map.of(
        "channelList", List.of(), "registrationId", "registered-nvr", "authenticated", true))));
    stubSuccessfulSync(0);
    when(channelMapper.selectCountByExample(any())).thenReturn(2, 0);

    assertEquals(0, service.syncDeviceChannels("hik-sdk-123", "nvr-1").get("afterChannelCount"));

    assertEquals(List.of(), capturedSyncDevice().get("channelList"));
  }

  @Test
  void unauthenticatedDahuaRegistrationCannotWriteChannels() {
    configureRegistration("dahua_sdk");
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(discoveredDevice(Map.of(
        "registrationId", "registered-nvr", "authenticated", false))));

    assertThrows(BaseException.class, () -> service.syncDeviceChannels("hik-sdk-123", "nvr-1"));

    verifyNoSync();
  }

  @Test
  void anotherDahuaRegistrationCannotWriteChannelsEvenWhenThePlatformDeviceIdMatches() {
    configureRegistration("dahua_sdk");
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(discoveredDevice(Map.of(
        "registrationId", "another-registration", "authenticated", true))));

    assertThrows(BaseException.class, () -> service.syncDeviceChannels("hik-sdk-123", "nvr-1"));

    verifyNoSync();
  }

  @Test
  void legacyGatewayRegistrationIdIsExplicitlyPassedToTheCurrentSessionProbe() {
    instance.setPlatformType("dahua_sdk");
    extension.setExtField3("{\"loginMode\":\"active_registration\",\"gatewayDeviceId\":\"registered-nvr\"}");
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(discoveredDevice(Map.of(
        "registrationId", "registered-nvr", "authenticated", true))));
    stubSuccessfulSync(1);

    service.syncDeviceChannels("hik-sdk-123", "nvr-1");

    assertEquals("registered-nvr", capturedProbeDevice().get("registrationId"));
  }

  @Test
  void missingDirectoryCannotBeTreatedAsAnAuthoritativeEmptyDirectory() {
    Map<String, Object> discovered = discoveredDevice(Map.of());
    discovered.remove("channelList");
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(discovered));

    assertThrows(BaseException.class, () -> service.syncDeviceChannels("hik-sdk-123", "nvr-1"));

    verifyNoSync();
  }

  @Test
  void skippedDeviceSyncCannotBeReportedAsSuccess() {
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(discoveredDevice(Map.of())));
    doReturn(new DeviceSyncHandler.DeviceSyncResult()).when(service)
        .forceSyncDevices(eq("hik-sdk-123"), anyMap());

    assertThrows(BaseException.class, () -> service.syncDeviceChannels("hik-sdk-123", "nvr-1"));
  }

  private void configureRegistration(String platformType) {
    instance.setPlatformType(platformType);
    extension.setExtField3("{\"accessMode\":\"active_registration\",\"loginMode\":\"active_registration\","
        + "\"registrationId\":\"registered-nvr\"}");
  }

  @Test void dahuaIpcSignalOnlyRefreshReturnsPreservedDirectoryResult() {
    instance.setPlatformType("dahua_sdk");
    Map<String, Object> discovered = discoveredDevice(Map.of("channelCatalogAuthoritative", false));
    when(adapter.listDevices(eq(instance), anyMap())).thenReturn(List.of(discovered));
    when(ipcStatus.refresh(eq(device), eq(discovered))).thenReturn(2);

    Map<String, Object> result = service.syncDeviceChannels("hik-sdk-123", "nvr-1");

    assertEquals(true, result.get("statusOnly"));
    assertEquals(2, result.get("totalChannels"));
    assertTrue(String.valueOf(result.get("message")).contains("已保留全部通道和配置"));
    verifyNoSync();
  }

  private Map<String, Object> discoveredDevice(Map<String, Object> overrides) {
    Map<String, Object> discovered = new LinkedHashMap<>();
    discovered.put("deviceId", "nvr-1");
    discovered.put("deviceIp", "192.168.31.114");
    discovered.put("serialNumber", SERIAL);
    discovered.put("deviceStatus", "ON");
    discovered.put("channelCatalogAuthoritative", true);
    discovered.put("channelList", List.of(Map.of("channelId", "2", "channelName", "新通道")));
    discovered.putAll(overrides);
    return discovered;
  }

  private void stubSuccessfulSync(int channels) {
    DeviceSyncHandler.DeviceSyncResult syncResult = new DeviceSyncHandler.DeviceSyncResult();
    syncResult.setTotalDevices(1);
    syncResult.setTotalChannels(channels);
    doReturn(syncResult).when(service).forceSyncDevices(eq("hik-sdk-123"), anyMap());
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Map<?, ?> capturedProbeDevice() {
    ArgumentCaptor<Map> filters = ArgumentCaptor.forClass(Map.class);
    verify(adapter).listDevices(eq(instance), filters.capture());
    assertEquals(Boolean.TRUE, filters.getValue().get("refreshChannels"));
    return (Map<?, ?>) filters.getValue().get("device");
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Map<?, ?> capturedSyncDevice() {
    ArgumentCaptor<Map> filters = ArgumentCaptor.forClass(Map.class);
    verify(service).forceSyncDevices(eq("hik-sdk-123"), filters.capture());
    List<?> selected = (List<?>) filters.getValue().get("devices");
    assertEquals(1, selected.size());
    return (Map<?, ?>) selected.getFirst();
  }

  private void verifyNoSync() {
    verify(service, never()).forceSyncDevices(eq("hik-sdk-123"), anyMap());
  }
}
