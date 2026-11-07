package cn.universal.web.service.openapi.v2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.hutool.json.JSONUtil;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.push.transform.AppPushConfigChangeNotifier;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import cn.universal.web.dto.openapi.v2.OpenApiV2ApplicationModels.HttpPushUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

class OpenApiV2ApplicationServiceTest {

  private final OpenApiV2Access access = mock(OpenApiV2Access.class);
  private final IoTUserApplicationMapper applications = mock(IoTUserApplicationMapper.class);
  private final IoTDeviceMapper deviceMapper = mock(IoTDeviceMapper.class);
  private final IoTDeviceService devices = mock(IoTDeviceService.class);
  private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
  private final AppPushConfigChangeNotifier notifier = mock(AppPushConfigChangeNotifier.class);
  private final IoTCacheInvalidationPublisher invalidation =
      mock(IoTCacheInvalidationPublisher.class);
  private final OpenApiV2ApplicationService service =
      new OpenApiV2ApplicationService(
          access, applications, deviceMapper, devices, jdbc, notifier, invalidation);
  private IoTUserApplication app;

  @BeforeEach
  void setUp() {
    app =
        IoTUserApplication.builder()
            .appUniqueId("app-1")
            .appId("client-1")
            .unionId("owner-1")
            .tenantId("tenant-1")
            .appSecret("never-expose")
            .deleted(0)
            .appStatus(0)
            .build();
    when(access.application()).thenReturn(app);
    when(access.tenantId(any())).thenReturn("tenant-1");
    when(applications.selectIotUserApplicationById("app-1")).thenReturn(app);
    when(applications.updateIotUserApplication(any())).thenReturn(1);
  }

  @Test
  void updatesRealHttpConfigurationAndPreservesOtherChannelsAndStoredSecret() {
    app.setCfg(
        "{\"http\":{\"url\":\"https://old.example/events\",\"secret\":\"stored-secret\",\"custom\":42},\"kafka\":{\"topic\":\"original\"}}");
    var result =
        service.updateHttpPush(new HttpPushUpdate(true, "https://new.example/events", null, ""));
    assertTrue(result.enabled());
    assertTrue(result.secretConfigured());
    ArgumentCaptor<IoTUserApplication> captor = ArgumentCaptor.forClass(IoTUserApplication.class);
    verify(applications).updateIotUserApplication(captor.capture());
    var patch = captor.getValue();
    var cfg = JSONUtil.parseObj(patch.getCfg());
    assertEquals("stored-secret", cfg.getJSONObject("http").getStr("secret"));
    assertEquals(42, cfg.getJSONObject("http").getInt("custom"));
    assertEquals("original", cfg.getJSONObject("kafka").getStr("topic"));
    assertEquals(1, patch.getHttpEnable());
    assertNull(patch.getAppSecret());
    assertNull(patch.getAppId());
    assertNull(patch.getUnionId());
    assertNull(patch.getTenantId());
    verify(notifier).notifyAppConfigChanged("app-1", "OPENAPI_V2_HTTP_PUSH_UPDATED");
  }

  @Test
  void readsPublicDataWithoutExposingApplicationOrPushCredentials() throws Exception {
    app.setCfg(
        "{\"http\":{\"url\":\"https://example.com/events\",\"secret\":\"push-secret\",\"enable\":true}}");
    String json = new ObjectMapper().writeValueAsString(service.current());
    assertFalse(json.contains("never-expose"));
    assertFalse(json.contains("push-secret"));
    assertTrue(json.contains("\"secretConfigured\":true"));
    HttpPushUpdate request =
        new ObjectMapper()
            .readValue("{\"enable\":true,\"secret\":\"new-secret\"}", HttpPushUpdate.class);
    assertEquals("new-secret", request.secret());
    assertFalse(new ObjectMapper().writeValueAsString(request).contains("new-secret"));
  }

  @Test
  void enablingRequiresValidDestinationHeaderAndSecret() {
    assertThrows(
        ResponseStatusException.class,
        () -> service.updateHttpPush(new HttpPushUpdate(true, "file:///tmp/callback", null, "s")));
    assertThrows(
        ResponseStatusException.class,
        () ->
            service.updateHttpPush(
                new HttpPushUpdate(true, "https://example.com/events", "Host", "s")));
    assertThrows(
        ResponseStatusException.class,
        () ->
            service.updateHttpPush(
                new HttpPushUpdate(true, "https://example.com/events", null, null)));
    assertThrows(
        ResponseStatusException.class,
        () ->
            service.updateHttpPush(
                new HttpPushUpdate(true, "https://example.com/events", null, "abc\r\ninject")));
    verify(applications, never()).updateIotUserApplication(any());
    verifyNoInteractions(notifier);
  }

  @Test
  void refusesBindingOrUnbindingDevicesOwnedByOtherTenantOwnerOrApplication() {
    IoTDevice device = ownedDevice();
    when(deviceMapper.selectOne(any())).thenReturn(device);
    device.setTenantId("other-tenant");
    assertThrows(AccessDeniedException.class, () -> service.bind("p", "d"));
    device.setTenantId("tenant-1");
    device.setCreatorId("other-owner");
    assertThrows(AccessDeniedException.class, () -> service.bind("p", "d"));
    device.setCreatorId("owner-1");
    device.setApplication("other-app");
    assertThrows(AccessDeniedException.class, () -> service.bind("p", "d"));
    assertThrows(AccessDeniedException.class, () -> service.unbind("p", "d"));
    verifyNoInteractions(jdbc, invalidation);
  }

  @Test
  void bindingIsConditionalAndConcurrentChangeDoesNotPublishSuccess() {
    when(deviceMapper.selectOne(any())).thenReturn(ownedDevice());
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
    assertThrows(ResponseStatusException.class, () -> service.bind("p", "d"));
    verifyNoInteractions(invalidation);
  }

  @Test
  void repeatedBindingAndUnbindingAreIdempotent() {
    IoTDevice device = ownedDevice();
    device.setApplication("app-1");
    when(deviceMapper.selectOne(any())).thenReturn(device);
    assertTrue(service.bind("p", "d").bound());
    device.setApplication(null);
    assertFalse(service.unbind("p", "d").bound());
    verifyNoInteractions(jdbc, invalidation);
  }

  @Test
  void bindingAndUnbindingPublishCacheInvalidation() {
    IoTDevice device = ownedDevice();
    when(deviceMapper.selectOne(any())).thenReturn(device);
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    assertTrue(service.bind("p", "d").bound());
    device.setApplication("app-1");
    assertFalse(service.unbind("p", "d").bound());
    verify(invalidation, times(2)).publishDeviceChange(any(), any());
  }

  @Test
  void bindingSqlChangesOnlyTheOwnedTenantRowAndUnbindsWithoutDeletingIt() {
    JdbcTemplate database = bindingDatabase();
    OpenApiV2ApplicationService realSqlService =
        new OpenApiV2ApplicationService(
            access, applications, deviceMapper, devices, database, notifier, invalidation);
    IoTDevice device = ownedDevice();
    when(deviceMapper.selectOne(any())).thenReturn(device);
    assertTrue(realSqlService.bind("p", "d").bound());
    assertEquals(
        "app-1",
        database.queryForObject("SELECT application FROM iot_device WHERE id = 1", String.class));
    assertEquals(
        "other-app",
        database.queryForObject("SELECT application FROM iot_device WHERE id = 2", String.class));
    device.setApplication("app-1");
    assertFalse(realSqlService.unbind("p", "d").bound());
    assertNull(
        database.queryForObject("SELECT application FROM iot_device WHERE id = 1", String.class));
    assertEquals(2, database.queryForObject("SELECT COUNT(*) FROM iot_device", Integer.class));
  }

  @Test
  void staleAuthorizationCannotOverwriteAConcurrentTenantOrApplicationChange() {
    JdbcTemplate database = bindingDatabase();
    OpenApiV2ApplicationService realSqlService =
        new OpenApiV2ApplicationService(
            access, applications, deviceMapper, devices, database, notifier, invalidation);
    when(deviceMapper.selectOne(any())).thenReturn(ownedDevice());
    database.update("UPDATE iot_device SET application = 'other-app' WHERE id = 1");
    assertThrows(ResponseStatusException.class, () -> realSqlService.bind("p", "d"));
    database.update(
        "UPDATE iot_device SET application = NULL, tenant_id = 'other-tenant' WHERE id = 1");
    assertThrows(ResponseStatusException.class, () -> realSqlService.bind("p", "d"));
    verifyNoInteractions(invalidation);
  }

  private static JdbcTemplate bindingDatabase() {
    JdbcTemplate database =
        new JdbcTemplate(
            new DriverManagerDataSource(
                "jdbc:h2:mem:openapi_v2_binding_"
                    + java.util.UUID.randomUUID()
                    + ";DB_CLOSE_DELAY=-1",
                "sa",
                ""));
    database.execute(
        "CREATE TABLE iot_device (id BIGINT PRIMARY KEY, creator_id VARCHAR(100), tenant_id"
            + " VARCHAR(100), application VARCHAR(100))");
    database.update(
        "INSERT INTO iot_device VALUES (1, 'owner-1', 'tenant-1', NULL), (2, 'owner-2', 'tenant-2',"
            + " 'other-app')");
    return database;
  }

  private static IoTDevice ownedDevice() {
    return IoTDevice.builder()
        .id(1L)
        .iotId("iot-1")
        .productKey("p")
        .deviceId("d")
        .tenantId("tenant-1")
        .creatorId("owner-1")
        .build();
  }
}
