package cn.universal.security.wechat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.WechatLoginConfig;
import cn.universal.persistence.entity.WechatUserBinding;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.persistence.mapper.WechatLoginConfigMapper;
import cn.universal.persistence.mapper.WechatUserBindingMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

class WechatAccountConfigServiceTest {
  private static final String TENANT = "tenant-a";
  private static final String APP = "wx0000000000000000";
  private static final String AES = Base64.getEncoder().withoutPadding().encodeToString(new byte[32]);
  private WechatLoginConfigMapper configs;
  private WechatUserBindingMapper bindings;
  private IoTTenantMapper tenants;
  private IoTUserMapper users;
  private WechatConfigCrypto crypto;
  private WechatAccountConfigService service;
  private IoTTenant tenant;

  @BeforeEach
  void setup() {
    for (Class<?> type : List.of(WechatLoginConfig.class, WechatUserBinding.class, IoTTenant.class, IoTUser.class))
      EntityHelper.initEntityNameMap(type, new Config());
    configs = mock(WechatLoginConfigMapper.class);
    bindings = mock(WechatUserBindingMapper.class);
    tenants = mock(IoTTenantMapper.class);
    users = mock(IoTUserMapper.class);
    crypto = new WechatConfigCrypto(WechatConfigCryptoTest.key((byte) 7));
    service = new WechatAccountConfigService(configs, bindings, tenants, users, crypto);
    tenant = IoTTenant.builder().tenantId(TENANT).tenantCode("test").status(0).deleted(0).build();
    when(tenants.selectByPrimaryKey(TENANT)).thenReturn(tenant);
  }

  @Test
  void savePersistsOnlyCiphertextsAndManagementResponseNeverExposesCredentials() throws Exception {
    service.save(TENANT, enabledConfig());
    ArgumentCaptor<WechatLoginConfig> saved = ArgumentCaptor.forClass(WechatLoginConfig.class);
    verify(configs).insertSelective(saved.capture());
    WechatLoginConfig stored = saved.getValue();
    assertNotEquals("private-secret", stored.getAppSecret());
    assertNotEquals("nexiot", stored.getCallbackToken());
    assertEquals("private-secret", crypto.decrypt(stored.getAppSecret(), TENANT + ":appSecret"));
    when(configs.selectByPrimaryKey(TENANT)).thenReturn(stored);
    Map<String, Object> safe = service.safeConfig(TENANT);
    assertTrue((Boolean) safe.get("appSecretConfigured"));
    assertEquals("/wechat/callback/test", safe.get("callbackPath"));
    String managementJson = new ObjectMapper().writeValueAsString(safe);
    assertFalse(managementJson.contains("private-secret"));
    assertFalse(managementJson.contains("gcm:v1:"));
    assertFalse(managementJson.contains(AES));
    String entityJson = new ObjectMapper().writeValueAsString(service.requireEnabled(TENANT));
    assertFalse(entityJson.contains("private-secret"));
    assertFalse(entityJson.contains("callbackToken"));
    assertFalse(entityJson.contains("encodingAESKey"));
  }

  @Test
  void blankSecretsPreserveExistingValuesAndIncrementVersion() {
    WechatLoginConfig stored = encryptedConfig();
    when(configs.selectByPrimaryKey(TENANT)).thenReturn(stored);
    when(configs.updateByExample(any(), any())).thenReturn(1);
    WechatLoginConfig patch = new WechatLoginConfig();
    patch.setAccountName("new name");
    patch.setAppSecret(" ");
    patch.setCallbackToken("");
    patch.setConfigVersion(3L);
    service.save(TENANT, patch);
    ArgumentCaptor<WechatLoginConfig> saved = ArgumentCaptor.forClass(WechatLoginConfig.class);
    verify(configs).updateByExample(saved.capture(), any());
    assertEquals(4L, saved.getValue().getConfigVersion());
    assertEquals("new name", saved.getValue().getAccountName());
    assertEquals("private-secret", crypto.decrypt(saved.getValue().getAppSecret(), TENANT + ":appSecret"));
    assertEquals("nexiot", crypto.decrypt(saved.getValue().getCallbackToken(), TENANT + ":callbackToken"));
  }

  @Test
  void staleAndConcurrentConfigurationChangesAreRejected() {
    when(configs.selectByPrimaryKey(TENANT)).thenReturn(encryptedConfig());
    WechatLoginConfig request = new WechatLoginConfig();
    request.setConfigVersion(2L);
    assertThrows(IoTException.class, () -> service.save(TENANT, request));
    verify(configs, never()).updateByExample(any(), any());
    request.setConfigVersion(3L);
    when(configs.updateByExample(any(), any())).thenReturn(0);
    assertThrows(IoTException.class, () -> service.save(TENANT, request));
  }

  @Test
  void disabledConfigStillSupportsCallbackVerificationButNotLogin() {
    WechatLoginConfig stored = encryptedConfig();
    stored.setEnabled(false);
    when(configs.selectByPrimaryKey(TENANT)).thenReturn(stored);
    when(tenants.selectOneByExample(any())).thenReturn(tenant);
    assertEquals("nexiot", service.findByTenantCode("test").getCallbackToken());
    assertThrows(IoTException.class, () -> service.requireEnabled(TENANT));
    tenant.setStatus(1);
    assertThrows(IoTException.class, () -> service.findByTenantCode("test"));
    tenant.setStatus(0);
    tenant.setExpireTime(new Date(1));
    assertThrows(IoTException.class, () -> service.requireEnabled(TENANT));
  }

  @Test
  void demoCannotUsePlatformAdminOrUnavailableTenantAccount() {
    WechatLoginConfig request = enabledConfig();
    request.setLoginMode("DEMO_ACCOUNT");
    request.setDemoUserUnionId("admin");
    when(users.selectOneByExample(any())).thenReturn(IoTUser.builder().identity(0).unionId("admin").build());
    assertThrows(IoTException.class, () -> service.save(TENANT, request));
    when(users.selectOneByExample(any())).thenReturn(null);
    assertThrows(IoTException.class, () -> service.save(TENANT, request));
    verify(configs, never()).insertSelective(any());
  }

  @Test
  void bindingIdentityLookupIncludesTenantAndAppAndCannotReplaceAnotherAccount() {
    when(configs.selectByPrimaryKey(TENANT)).thenReturn(encryptedConfig());
    when(users.selectOneByExample(any())).thenReturn(IoTUser.builder().identity(1).unionId("user-b").tenantId(TENANT).build());
    WechatUserBinding existing = new WechatUserBinding();
    existing.setUserUnionId("user-a");
    when(bindings.selectOneByExample(any())).thenReturn(existing);
    assertEquals("user-a", service.findBoundUser(TENANT, APP, "openid-a"));
    assertThrows(IoTException.class, () -> service.bind(TENANT, APP, "openid-a", "user-b"));
    ArgumentCaptor<Example> query = ArgumentCaptor.forClass(Example.class);
    verify(bindings, atLeastOnce()).selectOneByExample(query.capture());
    List<Object> values = query.getValue().getOredCriteria().get(0).getAllCriteria().stream()
        .map(Example.Criterion::getValue).toList();
    assertTrue(values.containsAll(List.of(TENANT, APP, "openid-a")));
    verify(bindings, never()).insertSelective(any());
  }

  @Test
  void enabledMessageCodeDoesNotRequireAppSecretButSceneQrDoes() {
    WechatLoginConfig request = enabledConfig();
    request.setAppSecret(null);
    service.save(TENANT, request);
    request.setCapabilityMode("SCENE_QR");
    assertThrows(IoTException.class, () -> service.save(TENANT, request));
  }

  @Test
  void plaintextModeCanExplicitlyClearAnExistingAesKey() {
    WechatLoginConfig stored = encryptedConfig();
    when(configs.selectByPrimaryKey(TENANT)).thenReturn(stored);
    when(configs.updateByExample(any(), any())).thenReturn(1);
    WechatLoginConfig request = enabledConfig();
    request.setConfigVersion(stored.getConfigVersion());
    request.setEncodingAESKey(null);
    service.save(TENANT, request, true);
    ArgumentCaptor<WechatLoginConfig> saved = ArgumentCaptor.forClass(WechatLoginConfig.class);
    verify(configs).updateByExample(saved.capture(), any());
    assertNull(crypto.decrypt(saved.getValue().getEncodingAESKey(), TENANT + ":encodingAESKey"));
  }

  private WechatLoginConfig enabledConfig() {
    WechatLoginConfig value = new WechatLoginConfig();
    value.setTenantId(TENANT);
    value.setAccountName("public account");
    value.setAppId(APP);
    value.setAppSecret("private-secret");
    value.setCallbackToken("nexiot");
    value.setEncodingAESKey(AES);
    value.setAccountQrUrl("/uploads/account.png");
    value.setCapabilityMode("MESSAGE_CODE");
    value.setLoginMode("BOUND_USER");
    value.setEnabled(true);
    value.setBindingEnabled(true);
    value.setExpireSeconds(180);
    return value;
  }

  private WechatLoginConfig encryptedConfig() {
    WechatLoginConfig value = enabledConfig();
    value.setConfigVersion(3L);
    value.setCreateTime(new Date());
    value.setAppSecret(crypto.encrypt(value.getAppSecret(), TENANT + ":appSecret"));
    value.setCallbackToken(crypto.encrypt(value.getCallbackToken(), TENANT + ":callbackToken"));
    value.setEncodingAESKey(crypto.encrypt(value.getEncodingAESKey(), TENANT + ":encodingAESKey"));
    return value;
  }
}
