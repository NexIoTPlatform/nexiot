package cn.universal.admin.system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.system.service.impl.IoTUserApplicationServiceImpl;
import cn.universal.common.exception.IoTException;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.dm.device.service.push.transform.AppPushConfigChangeNotifier;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import cn.universal.persistence.mapper.OauthClientDetailsMapper;
import cn.universal.security.service.IoTUserService;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class IoTUserApplicationServiceImplTest {

  @InjectMocks private IoTUserApplicationServiceImpl service;

  @Mock private IoTUserApplicationMapper applicationMapper;
  @Mock private IoTUserService userService;
  @Mock private OauthClientDetailsMapper oauthClientDetailsMapper;
  @Mock private AppPushConfigChangeNotifier appPushConfigChangeNotifier;
  @Mock private IoTCacheInvalidationPublisher cacheInvalidationPublisher;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void updateShouldKeepMissingHttpEnableDisabledAndSynchronizeLegacyColumn() throws Exception {
    mockCurrentUser();
    when(applicationMapper.selectIotUserApplicationById("app-1"))
        .thenReturn(
            IoTUserApplication.builder()
                .appUniqueId("app-1")
                .unionId("owner-1")
                .tenantId("tenant-1")
                .build());
    when(applicationMapper.updateIotUserApplication(any())).thenReturn(1);

    service.updateIotUserApplication(
        IoTUserApplication.builder()
            .appUniqueId("app-1")
            .cfg(
                "{\"http\":{\"url\":\"http://www.baidu.com\",\"header\":\"AuthToken\",\"secret\":\"abc123\",\"support\":true},\"mqtt\":{},\"kafka\":{},\"rocketMQ\":{}}")
            .build());

    JSONObject http = capturedCfg().getJSONObject("http");
    assertFalse(http.getBool("enable"));
    assertTrue(http.getBool("support"));
    assertEquals("AuthToken", http.getStr("header"));
    assertEquals(0, capturedApplication().getHttpEnable());
  }

  @Test
  void updateShouldDefaultConfiguredHttpHeaderWhenHeaderIsMissing() throws Exception {
    mockCurrentUser();
    when(applicationMapper.selectIotUserApplicationById("app-1"))
        .thenReturn(
            IoTUserApplication.builder()
                .appUniqueId("app-1")
                .unionId("owner-1")
                .tenantId("tenant-1")
                .build());
    when(applicationMapper.updateIotUserApplication(any())).thenReturn(1);

    service.updateIotUserApplication(
        IoTUserApplication.builder()
            .appUniqueId("app-1")
            .cfg("{\"http\":{\"url\":\"http://www.baidu.com\",\"secret\":\"abc123\"}}")
            .build());

    JSONObject http = capturedCfg().getJSONObject("http");
    assertFalse(http.getBool("enable"));
    assertEquals("X-Nexiot-Secret", http.getStr("header"));
  }

  @Test
  void updateShouldKeepExplicitHttpDisable() throws Exception {
    mockCurrentUser();
    when(applicationMapper.selectIotUserApplicationById("app-1"))
        .thenReturn(
            IoTUserApplication.builder()
                .appUniqueId("app-1")
                .unionId("owner-1")
                .tenantId("tenant-1")
                .build());
    when(applicationMapper.updateIotUserApplication(any())).thenReturn(1);

    service.updateIotUserApplication(
        IoTUserApplication.builder()
            .appUniqueId("app-1")
            .cfg(
                "{\"http\":{\"url\":\"http://www.baidu.com\",\"secret\":\"abc123\",\"enable\":false,\"support\":true}}")
            .build());

    assertFalse(capturedCfg().getJSONObject("http").getBool("enable"));
  }

  @Test
  void updateShouldNotEnableUnsupportedHttpPush() throws Exception {
    mockCurrentUser();
    when(applicationMapper.selectIotUserApplicationById("app-1"))
        .thenReturn(
            IoTUserApplication.builder()
                .appUniqueId("app-1")
                .unionId("owner-1")
                .tenantId("tenant-1")
                .build());
    when(applicationMapper.updateIotUserApplication(any())).thenReturn(1);

    service.updateIotUserApplication(
        IoTUserApplication.builder()
            .appUniqueId("app-1")
            .cfg(
                "{\"http\":{\"url\":\"http://www.baidu.com\",\"secret\":\"abc123\",\"support\":false}}")
            .build());

    JSONObject http = capturedCfg().getJSONObject("http");
    assertFalse(http.getBool("support"));
    assertTrue(http.containsKey("enable"));
    assertFalse(http.getBool("enable"));
  }

  @Test
  void enableMqttShouldSynchronizeAuthenticationGate() throws Exception {
    mockCurrentUser();
    setField("sysMqttEnabled", true);
    when(applicationMapper.selectIotUserApplicationById("app-1"))
        .thenReturn(
            IoTUserApplication.builder()
                .appUniqueId("app-1")
                .appId("client-1")
                .appSecret("secret-1")
                .unionId("owner-1")
                .tenantId("tenant-1")
                .cfg("{}")
                .build());
    when(applicationMapper.updateIotUserApplication(any())).thenReturn(1);

    assertTrue(service.enableOrDisablePushCfg("app-1", "mqtt", true));

    IoTUserApplication updated = capturedApplication();
    assertEquals(1, updated.getMqttEnable());
    assertTrue(JSONUtil.parseObj(updated.getCfg()).getJSONObject("mqtt").getBool("enable"));
  }

  @Test
  void enableRocketMqShouldMigrateLegacyConfigKey() throws Exception {
    mockCurrentUser();
    when(applicationMapper.selectIotUserApplicationById("app-1"))
        .thenReturn(
            IoTUserApplication.builder()
                .appUniqueId("app-1")
                .unionId("owner-1")
                .tenantId("tenant-1")
                .cfg(
                    "{\"rocketmq\":{\"namesrvAddr\":\"localhost:9876\",\"topic\":\"events\",\"group\":\"app\"}}")
                .build());
    when(applicationMapper.updateIotUserApplication(any())).thenReturn(1);

    assertTrue(service.enableOrDisablePushCfg("app-1", "rocketmq", true));

    JSONObject cfg = JSONUtil.parseObj(capturedApplication().getCfg());
    assertNull(cfg.getJSONObject("rocketmq"));
    assertEquals("localhost:9876", cfg.getJSONObject("rocketMQ").getStr("namesrvAddr"));
    assertTrue(cfg.getJSONObject("rocketMQ").getBool("enable"));
  }

  @Test
  void enableKafkaShouldRejectIncompleteCredentials() throws Exception {
    mockCurrentUser();
    when(applicationMapper.selectIotUserApplicationById("app-1"))
        .thenReturn(
            IoTUserApplication.builder()
                .appUniqueId("app-1")
                .unionId("owner-1")
                .tenantId("tenant-1")
                .cfg(
                    "{\"kafka\":{\"bootstrapServers\":\"localhost:9092\",\"topic\":\"events\",\"username\":\"app\"}}")
                .build());

    IoTException error =
        assertThrows(
            IoTException.class,
            () -> service.enableOrDisablePushCfg("app-1", "kafka", true));

    assertEquals("Kafka SASL 鉴权必须同时配置用户名和密码", error.getMessage());
  }

  @Test
  void enableKafkaShouldAcceptScramOverSsl() throws Exception {
    mockCurrentUser();
    when(applicationMapper.selectIotUserApplicationById("app-1"))
        .thenReturn(
            IoTUserApplication.builder()
                .appUniqueId("app-1")
                .unionId("owner-1")
                .tenantId("tenant-1")
                .cfg(
                    "{\"kafka\":{\"bootstrapServers\":\"localhost:9093\",\"topic\":\"events\",\"securityProtocol\":\"SASL_SSL\",\"saslMechanism\":\"SCRAM-SHA-512\",\"username\":\"app\",\"password\":\"secret\"}}")
                .build());
    when(applicationMapper.updateIotUserApplication(any())).thenReturn(1);

    assertTrue(service.enableOrDisablePushCfg("app-1", "kafka", true));

    assertTrue(capturedCfg().getJSONObject("kafka").getBool("enable"));
  }

  @Test
  void enableExternalMqttShouldValidateAndPersistChannelSwitch() throws Exception {
    mockCurrentUser();
    when(applicationMapper.selectIotUserApplicationById("app-1"))
        .thenReturn(
            IoTUserApplication.builder()
                .appUniqueId("app-1")
                .unionId("owner-1")
                .tenantId("tenant-1")
                .cfg(
                    "{\"externalMqtt\":{\"brokerUrl\":\"tcp://broker:1883\",\"topic\":\"devices/up\"}}")
                .build());
    when(applicationMapper.updateIotUserApplication(any())).thenReturn(1);

    assertTrue(service.enableOrDisablePushCfg("app-1", "externalMqtt", true));

    JSONObject config = capturedCfg().getJSONObject("externalMqtt");
    assertTrue(config.getBool("enable"));
    assertTrue(config.getBool("support"));
    assertEquals("tcp://broker:1883", config.getStr("brokerUrl"));
  }

  @Test
  void enableTcpShouldRejectInvalidPort() throws Exception {
    mockCurrentUser();
    when(applicationMapper.selectIotUserApplicationById("app-1"))
        .thenReturn(
            IoTUserApplication.builder()
                .appUniqueId("app-1")
                .unionId("owner-1")
                .tenantId("tenant-1")
                .cfg("{\"tcp\":{\"host\":\"127.0.0.1\",\"port\":70000}}")
                .build());

    IoTException error =
        assertThrows(
            IoTException.class, () -> service.enableOrDisablePushCfg("app-1", "tcp", true));

    assertEquals("TCP 服务器地址不能为空，端口必须在 1-65535 之间", error.getMessage());
  }

  @Test
  void disablingUserShouldInvalidatePushAndDeviceCaches() {
    IoTUserApplication application =
        IoTUserApplication.builder()
            .appUniqueId("app-1")
            .unionId("user-1")
            .appStatus(0)
            .build();
    when(applicationMapper.select(any(IoTUserApplication.class))).thenReturn(List.of(application));
    when(applicationMapper.updateByPrimaryKeySelective(any(IoTUserApplication.class))).thenReturn(1);
    when(oauthClientDetailsMapper.select(any())).thenReturn(List.of());

    assertTrue(service.enDisableIoTUser("user-1", false));

    assertEquals(1, application.getAppStatus());
    verify(appPushConfigChangeNotifier).notifyAppConfigChanged("app-1", "APP_STATUS_UPDATED");
    verify(cacheInvalidationPublisher)
        .publishApplicationStatusChange("application-status-updated");
  }

  @Test
  void editingApplicationStatusShouldInvalidateDeviceCachesAcrossNodes() throws Exception {
    mockCurrentUser();
    when(applicationMapper.selectIotUserApplicationById("app-1"))
        .thenReturn(
            IoTUserApplication.builder()
                .appUniqueId("app-1")
                .unionId("owner-1")
                .tenantId("tenant-1")
                .appStatus(0)
                .build());
    when(applicationMapper.updateIotUserApplication(any())).thenReturn(1);

    service.updateIotUserApplication(
        IoTUserApplication.builder().appUniqueId("app-1").appStatus(1).build());

    verify(appPushConfigChangeNotifier).notifyAppConfigChanged("app-1", "APP_UPDATED");
    verify(cacheInvalidationPublisher)
        .publishApplicationStatusChange("application-status-updated");
  }

  @Test
  void creatingApplicationShouldSynchronizeHttpSwitchAndInvalidateNegativePushCache()
      throws Exception {
    mockCurrentUser();
    when(applicationMapper.insertIotUserApplication(any())).thenReturn(1);
    IoTUserApplication application =
        IoTUserApplication.builder()
            .appUniqueId("app-1")
            .unionId("admin")
            .cfg(
                "{\"http\":{\"url\":\"http://localhost/push\",\"secret\":\"secret\",\"enable\":true,\"support\":true}}")
            .build();

    assertEquals(1, service.insertIotAppUser(application));

    assertEquals(1, application.getHttpEnable());
    verify(appPushConfigChangeNotifier).notifyAppConfigChanged("app-1", "APP_CREATED");
  }

  @Test
  void deletingApplicationShouldInvalidatePositivePushCache() throws Exception {
    mockCurrentUser();
    when(applicationMapper.selectIotUserApplicationById("app-1"))
        .thenReturn(
            IoTUserApplication.builder()
                .appUniqueId("app-1")
                .unionId("owner-1")
                .tenantId("tenant-1")
                .build());
    when(applicationMapper.deleteIotUserApplicationById("app-1")).thenReturn(1);

    assertEquals(1, service.deleteIotUserApplicationById("app-1"));

    verify(appPushConfigChangeNotifier).notifyAppConfigChanged("app-1", "APP_DELETED");
  }

  private JSONObject capturedCfg() {
    return JSONUtil.parseObj(capturedApplication().getCfg());
  }

  private IoTUserApplication capturedApplication() {
    ArgumentCaptor<IoTUserApplication> captor = ArgumentCaptor.forClass(IoTUserApplication.class);
    verify(applicationMapper).updateIotUserApplication(captor.capture());
    return captor.getValue();
  }

  private void mockCurrentUser() throws Exception {
    setJwt("admin");
    when(userService.selectUserByUnionId("admin"))
        .thenReturn(IoTUser.builder().identity(0).unionId("admin").build());
    Field field = service.getClass().getSuperclass().getDeclaredField("ioTUserService");
    field.setAccessible(true);
    field.set(service, userService);
  }

  private void setField(String name, Object value) throws Exception {
    Field field = service.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(service, value);
  }

  private void setJwt(String unionId) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(unionId)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }
}
