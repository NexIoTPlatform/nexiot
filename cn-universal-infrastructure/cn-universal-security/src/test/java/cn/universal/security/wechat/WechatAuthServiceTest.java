package cn.universal.security.wechat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.WechatLoginConfig;
import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.persistence.mapper.admin.SysDictDataMapper;
import cn.universal.security.support.TenantDomainResolver;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/** Real shared-Redis integration; use an isolated local Redis via -Dwechat.test.redis.port. */
class WechatAuthServiceTest {
  private static LettuceConnectionFactory connectionA;
  private static LettuceConnectionFactory connectionB;
  private static StringRedisTemplate redisA;
  private static StringRedisTemplate redisB;
  private final Map<String, WechatLoginConfig> configValues = new ConcurrentHashMap<>();
  private final Map<String, IoTTenant> tenantValues = new ConcurrentHashMap<>();
  private final Map<String, IoTUser> userValues = new ConcurrentHashMap<>();
  private final Map<String, String> bindingValues = new ConcurrentHashMap<>();
  private WechatAccountConfigService configs;
  private WechatQrApiClient qrClient;
  private IoTUserMapper users;
  private IoTTenantMapper tenants;
  private TenantDomainResolver domains;
  private SysDictDataMapper dictionaries;
  private WechatAuthService nodeA;
  private WechatAuthService nodeB;
  private WechatLoginConfig config;
  private IoTTenant tenant;
  private IoTUser owner;
  private String openId;

  @BeforeAll
  static void sharedRedis() throws Exception {
    int port = Integer.getInteger("wechat.test.redis.port", 16383);
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress("127.0.0.1", port), 300);
    } catch (Exception unavailable) {
      Assumptions.assumeTrue(false, "Start an isolated Redis on 127.0.0.1:" + port
          + " or set -Dwechat.test.redis.port to run WeChat concurrency integration checks");
    }
    connectionA = connection(port);
    connectionB = connection(port);
    redisA = new StringRedisTemplate(connectionA);
    redisB = new StringRedisTemplate(connectionB);
    assertEquals("PONG", redisA.getConnectionFactory().getConnection().ping());
  }

  private static LettuceConnectionFactory connection(int port) {
    RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration("127.0.0.1", port);
    standalone.setDatabase(15);
    LettuceConnectionFactory connection = new LettuceConnectionFactory(standalone);
    connection.afterPropertiesSet();
    connection.start();
    return connection;
  }

  @AfterAll
  static void closeConnections() {
    if (connectionA != null) connectionA.destroy();
    if (connectionB != null) connectionB.destroy();
  }

  @BeforeEach
  void setup() {
    configs = mock(WechatAccountConfigService.class);
    qrClient = mock(WechatQrApiClient.class);
    users = mock(IoTUserMapper.class);
    tenants = mock(IoTTenantMapper.class);
    domains = mock(TenantDomainResolver.class);
    dictionaries = mock(SysDictDataMapper.class);
    String suffix = UUID.randomUUID().toString().replace("-", "");
    tenant = IoTTenant.builder().tenantId("wx-test-" + suffix).tenantCode("wx_" + suffix)
        .status(0).deleted(0).build();
    owner = IoTUser.builder().unionId("user-" + suffix).username("owner")
        .tenantId(tenant.getTenantId()).status(0).deleted(0).identity(1).build();
    openId = "openid_" + suffix;
    tenantValues.put(tenant.getTenantId(), tenant);
    userValues.put(owner.getUnionId(), owner);
    config = new WechatLoginConfig();
    config.setTenantId(tenant.getTenantId());
    config.setAppId("wx" + suffix.substring(0, 16));
    config.setAccountName("WeChat integration account");
    config.setAccountQrUrl("/uploads/wechat-test.png");
    config.setEnabled(true);
    config.setBindingEnabled(true);
    config.setCapabilityMode("MESSAGE_CODE");
    config.setLoginMode("BOUND_USER");
    config.setConfigVersion(1L);
    config.setExpireSeconds(180);
    configValues.put(config.getTenantId(), config);
    when(configs.findByTenantId(anyString())).thenAnswer(invocation -> configValues.get(invocation.getArgument(0)));
    when(configs.requireEnabled(anyString())).thenAnswer(invocation -> {
      WechatLoginConfig found = configValues.get(invocation.getArgument(0));
      if (found == null || !Boolean.TRUE.equals(found.getEnabled())) throw new IoTException("disabled");
      return found;
    });
    when(configs.findBoundUser(anyString(), anyString(), anyString())).thenAnswer(invocation ->
        bindingValues.get(bindingKey(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2))));
    when(tenants.selectByPrimaryKey(any())).thenAnswer(invocation -> tenantValues.get(invocation.getArgument(0)));
    when(tenants.selectOne(any(IoTTenant.class))).thenAnswer(invocation -> {
      IoTTenant query = invocation.getArgument(0);
      return tenantValues.values().stream().filter(value -> value.getTenantCode().equals(query.getTenantCode()))
          .findFirst().orElse(null);
    });
    when(users.selectOne(any(IoTUser.class))).thenAnswer(invocation ->
        userValues.get(((IoTUser) invocation.getArgument(0)).getUnionId()));
    nodeA = new WechatAuthService(redisA, configs, qrClient, users, tenants, domains, dictionaries);
    nodeB = new WechatAuthService(redisB, configs, qrClient, users, tenants, domains, dictionaries);
  }

  @Test
  void configuredLoginReplyPreservesMultilineEmojiAndLoginState() {
    String reply = "登陆成功 ✅\n请返回页面继续操作。";
    when(dictionaries.selectDictDataByType("wechat_reply")).thenReturn(List.of(
        SysDictData.builder().dictValue("login_success").status("0").remark(reply).build()));
    bind(openId, owner);
    Map<String, Object> session = login();

    assertEquals(reply, nodeB.handleMessage(config, text(session, openId)));
    assertEquals("READY", nodeA.status(id(session), proof(session)).get("status"));
    assertEquals(owner.getUnionId(), nodeA.consumeLogin(id(session), proof(session)));
  }

  @Test
  void independentNodesShareFinalStateAndPublishOnlySessionRoutingId() throws Exception {
    bind(openId, owner);
    Map<String, Object> session = login();
    RedisMessageListenerContainer notifications = new RedisMessageListenerContainer();
    notifications.setConnectionFactory(connectionA);
    CountDownLatch changed = new CountDownLatch(1);
    AtomicReference<String> notification = new AtomicReference<>();
    notifications.addMessageListener((message, pattern) -> {
      String value = new String(message.getBody(), StandardCharsets.UTF_8);
      if (id(session).equals(value)) { notification.set(value); changed.countDown(); }
    }, new ChannelTopic(WechatAuthService.STATE_TOPIC));
    notifications.afterPropertiesSet();
    notifications.start();
    try {
      nodeB.handleMessage(config, text(session, openId));
      assertTrue(changed.await(3, TimeUnit.SECONDS));
      assertEquals(id(session), notification.get());
      Map<String, Object> state = nodeA.status(id(session), proof(session));
      assertEquals("READY", state.get("status"));
      assertFalse(state.containsKey("browserProof"));
      assertFalse(state.containsKey("openId"));
      assertFalse(state.containsKey("targetUnionId"));
      assertEquals(owner.getUnionId(), nodeA.consumeLogin(id(session), proof(session)));
      assertEquals("CONSUMED", nodeB.status(id(session), proof(session)).get("status"));
    } finally {
      notifications.stop();
      notifications.destroy();
    }
  }

  @Test
  void browserProofIsRequiredForStatusCancellationLoginAndWebSocketTickets() {
    bind(openId, owner);
    Map<String, Object> session = login();
    String wrongProof = "A".repeat(43);
    assertThrows(IoTException.class, () -> nodeB.status(id(session), wrongProof));
    assertThrows(IoTException.class, () -> nodeB.cancel(id(session), wrongProof));
    assertThrows(IoTException.class, () -> nodeB.createWebSocketTicket(id(session), wrongProof));
    nodeB.handleMessage(config, text(session, openId));
    assertThrows(IoTException.class, () -> nodeB.consumeLogin(id(session), wrongProof));
    assertEquals("READY", nodeA.status(id(session), proof(session)).get("status"));
    String ticket = nodeA.createWebSocketTicket(id(session), proof(session));
    assertEquals(id(session), nodeB.consumeWebSocketTicket(ticket));
    assertThrows(IoTException.class, () -> nodeA.consumeWebSocketTicket(ticket));
  }

  @Test
  void concurrentConsumptionAcrossNodesGrantsTheLoginExactlyOnce() throws Exception {
    bind(openId, owner);
    Map<String, Object> session = login();
    nodeB.handleMessage(config, text(session, openId));
    ExecutorService workers = Executors.newFixedThreadPool(8);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Boolean>> outcomes = new ArrayList<>();
    try {
      for (int i = 0; i < 8; i++) {
        WechatAuthService node = i % 2 == 0 ? nodeA : nodeB;
        outcomes.add(workers.submit(() -> {
          start.await();
          try { return owner.getUnionId().equals(node.consumeLogin(id(session), proof(session))); }
          catch (IoTException consumed) { return false; }
        }));
      }
      start.countDown();
      int successes = 0;
      for (Future<Boolean> outcome : outcomes) if (outcome.get(5, TimeUnit.SECONDS)) successes++;
      assertEquals(1, successes);
      assertEquals("CONSUMED", nodeB.status(id(session), proof(session)).get("status"));
    } finally { workers.shutdownNow(); }
  }

  @Test
  void callbackMustMatchBothTenantAndPublicAccount() {
    bind(openId, owner);
    Map<String, Object> session = login();
    WechatLoginConfig wrongTenant = copyConfig();
    wrongTenant.setTenantId("another-tenant");
    nodeB.handleMessage(wrongTenant, text(session, openId));
    assertEquals("WAITING", nodeA.status(id(session), proof(session)).get("status"));
    WechatLoginConfig wrongApp = copyConfig();
    wrongApp.setAppId("wx0000000000000000");
    nodeB.handleMessage(wrongApp, scene(session, openId));
    assertEquals("WAITING", nodeA.status(id(session), proof(session)).get("status"));
    nodeB.handleMessage(config, text(session, openId));
    assertEquals("READY", nodeA.status(id(session), proof(session)).get("status"));
  }

  @Test
  void configurationVersionChangeInvalidatesWaitingAndReadySessions() {
    bind(openId, owner);
    Map<String, Object> ready = login();
    nodeB.handleMessage(config, text(ready, openId));
    Map<String, Object> waiting = login();
    config.setConfigVersion(2L);
    assertThrows(IoTException.class, () -> nodeA.consumeLogin(id(ready), proof(ready)));
    assertTrue(nodeB.handleMessage(config, text(waiting, openId)).contains("设置已变更"));
    assertEquals("CANCELLED", nodeB.status(id(waiting), proof(waiting)).get("status"));
    assertEquals("CANCELLED", nodeA.status(id(ready), proof(ready)).get("status"));
  }

  @Test
  void bindingRequiresOwnerConfirmationAndNeverActsAsLogin() {
    Map<String, Object> session = nodeA.createBinding(owner.getUnionId(), "binding-test");
    nodeB.handleMessage(config, text(session, openId));
    assertEquals("SCANNED", nodeA.status(id(session), proof(session)).get("status"));
    verify(configs, never()).bind(anyString(), anyString(), anyString(), anyString());
    assertThrows(IoTException.class, () -> nodeA.consumeLogin(id(session), proof(session)));
    assertThrows(IoTException.class, () -> nodeB.confirmBinding(id(session), proof(session), "another-owner"));
    assertThrows(IoTException.class, () -> nodeB.confirmBinding(id(session), "A".repeat(43), owner.getUnionId()));
    verify(configs, never()).bind(anyString(), anyString(), anyString(), anyString());
    nodeA.confirmBinding(id(session), proof(session), owner.getUnionId());
    assertEquals("BOUND", nodeB.status(id(session), proof(session)).get("status"));
    verify(configs).bind(config.getTenantId(), config.getAppId(), openId, owner.getUnionId());
    assertThrows(IoTException.class, () -> nodeB.confirmBinding(id(session), proof(session), owner.getUnionId()));
    assertThrows(IoTException.class, () -> nodeB.consumeLogin(id(session), proof(session)));
  }

  @Test
  void unboundIdentityReceivesNoLoginAndDemoModeGrantsOnlyConfiguredAccount() {
    Map<String, Object> unbound = login();
    nodeB.handleMessage(config, text(unbound, openId));
    assertEquals("UNBOUND", nodeA.status(id(unbound), proof(unbound)).get("status"));
    assertThrows(IoTException.class, () -> nodeA.consumeLogin(id(unbound), proof(unbound)));
    config.setLoginMode("DEMO_ACCOUNT");
    config.setDemoUserUnionId(owner.getUnionId());
    clearInvocations(configs);
    Map<String, Object> demo = login();
    nodeB.handleMessage(config, text(demo, "unbound_demo_" + openId));
    assertEquals(owner.getUnionId(), nodeA.consumeLogin(id(demo), proof(demo)));
    verify(configs, never()).findBoundUser(anyString(), anyString(), anyString());
  }

  @Test
  void loginConsumptionRechecksRevokedBindingAndChangedDemoAccount() {
    bind(openId, owner);
    Map<String, Object> revoked = login();
    nodeB.handleMessage(config, text(revoked, openId));
    bindingValues.clear();
    assertThrows(IoTException.class, () -> nodeA.consumeLogin(id(revoked), proof(revoked)));
    config.setLoginMode("DEMO_ACCOUNT");
    config.setDemoUserUnionId(owner.getUnionId());
    Map<String, Object> demo = login();
    nodeB.handleMessage(config, text(demo, openId));
    config.setDemoUserUnionId("another-account");
    assertThrows(IoTException.class, () -> nodeA.consumeLogin(id(demo), proof(demo)));
  }

  @Test
  void anotherTenantOrDisabledTargetCannotBeGrantedEvenWithAnExistingBinding() {
    bind(openId, owner);
    owner.setTenantId("different-tenant");
    Map<String, Object> crossTenant = login();
    nodeB.handleMessage(config, text(crossTenant, openId));
    assertEquals("FAILED", nodeA.status(id(crossTenant), proof(crossTenant)).get("status"));
    owner.setTenantId(tenant.getTenantId());
    owner.setStatus(1);
    Map<String, Object> disabled = login();
    nodeB.handleMessage(config, text(disabled, openId));
    assertEquals("FAILED", nodeA.status(id(disabled), proof(disabled)).get("status"));
  }

  @Test
  void autoFallsBackOnlyWhenSceneQrCapabilityIsUnavailable() {
    config.setCapabilityMode("AUTO");
    doThrow(new WechatQrApiClient.CapabilityUnavailableException()).when(qrClient).createQr(any(), anyString(), anyInt());
    Map<String, Object> fallback = login();
    assertEquals("MESSAGE_CODE", fallback.get("transportMode"));
    assertEquals(config.getAccountQrUrl(), fallback.get("qrUrl"));
    assertTrue(fallback.get("verificationCode").toString().matches("[0-9]{6,8}"));
    config.setCapabilityMode("SCENE_QR");
    assertThrows(WechatQrApiClient.CapabilityUnavailableException.class, this::login);
    config.setCapabilityMode("AUTO");
    doThrow(new IoTException("network failure")).when(qrClient).createQr(any(), anyString(), anyInt());
    assertThrows(IoTException.class, this::login);
  }

  @Test
  void duplicateCallbacksCannotReplaceIdentityOrReuseConsumedSession() {
    bind(openId, owner);
    Map<String, Object> session = login();
    nodeB.handleMessage(config, text(session, openId));
    nodeA.handleMessage(config, text(session, "another_" + openId));
    verify(configs, times(1)).findBoundUser(config.getTenantId(), config.getAppId(), openId);
    assertEquals(owner.getUnionId(), nodeB.consumeLogin(id(session), proof(session)));
    nodeA.handleMessage(config, text(session, openId));
    assertEquals("CONSUMED", nodeA.status(id(session), proof(session)).get("status"));
    assertThrows(IoTException.class, () -> nodeA.consumeLogin(id(session), proof(session)));
  }

  @Test
  void sceneSubscribeAndScanSupportBothWechatEventKeyForms() {
    bind(openId, owner);
    config.setCapabilityMode("SCENE_QR");
    when(qrClient.createQr(any(), anyString(), anyInt())).thenReturn("https://mp.weixin.qq.com/test-qr");
    Map<String, Object> subscribed = login();
    nodeB.handleMessage(config, Map.of("FromUserName", openId, "MsgType", "event", "Event", "subscribe",
        "EventKey", "qrscene_nx_" + id(subscribed)));
    assertEquals("READY", nodeA.status(id(subscribed), proof(subscribed)).get("status"));
    Map<String, Object> existingSubscriber = login();
    nodeB.handleMessage(config, scene(existingSubscriber, openId));
    assertEquals("READY", nodeA.status(id(existingSubscriber), proof(existingSubscriber)).get("status"));
  }

  @Test
  void cancellationAndRedisExpiryPreventLateCallbackAndConsumption() {
    bind(openId, owner);
    Map<String, Object> cancelled = login();
    nodeA.cancel(id(cancelled), proof(cancelled));
    nodeB.handleMessage(config, text(cancelled, openId));
    assertEquals("CANCELLED", nodeA.status(id(cancelled), proof(cancelled)).get("status"));
    assertThrows(IoTException.class, () -> nodeB.consumeLogin(id(cancelled), proof(cancelled)));
    Map<String, Object> expired = login();
    redisA.expire("auth:wechat:session:" + id(expired), Duration.ZERO);
    assertThrows(IoTException.class, () -> nodeB.status(id(expired), proof(expired)));
    assertThrows(IoTException.class, () -> nodeA.consumeLogin(id(expired), proof(expired)));
  }

  private Map<String, Object> login() { return nodeA.createLogin(tenant.getTenantCode(), "test-" + tenant.getTenantId()); }
  private String id(Map<String, Object> session) { return (String) session.get("sessionId"); }
  private String proof(Map<String, Object> session) { return (String) session.get("browserProof"); }
  private Map<String, String> text(Map<String, Object> session, String identity) {
    return Map.of("FromUserName", identity, "MsgType", "text", "Content", "登录 " + session.get("verificationCode"));
  }
  private Map<String, String> scene(Map<String, Object> session, String identity) {
    return Map.of("FromUserName", identity, "MsgType", "event", "Event", "SCAN", "EventKey", "nx_" + id(session));
  }
  private void bind(String identity, IoTUser user) {
    bindingValues.put(bindingKey(config.getTenantId(), config.getAppId(), identity), user.getUnionId());
  }
  private String bindingKey(String tenantId, String appId, String identity) { return tenantId + ":" + appId + ":" + identity; }
  private WechatLoginConfig copyConfig() {
    WechatLoginConfig result = new WechatLoginConfig();
    org.springframework.beans.BeanUtils.copyProperties(config, result);
    return result;
  }
}
