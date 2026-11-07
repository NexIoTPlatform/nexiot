package cn.universal.security.wechat;

import cn.hutool.json.JSONUtil;
import cn.universal.common.annotation.TenantIgnore;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.WechatLoginConfig;
import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.persistence.mapper.admin.SysDictDataMapper;
import cn.universal.security.support.TenantDomainResolver;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/** 短期扫码身份凭据与浏览器凭据分别保存；回调不签发系统 Token。 */
@Slf4j
@Service
@TenantIgnore
public class WechatAuthService {
  public static final String STATE_TOPIC = "auth:wechat:state-changed";
  private static final String SESSION_PREFIX = "auth:wechat:session:";
  private static final String TICKET_PREFIX = "auth:wechat:ws-ticket:";
  private static final String REPLY_DICT_TYPE = "wechat_reply";
  private static final String KEYWORD_REPLY_DICT_TYPE = "wechat_keyword_reply";
  private static final String BINDING_SCANNED_MESSAGE = "已识别微信，请确认绑定当前账号";
  private static final String UNBOUND_MESSAGE = "该微信尚未绑定，请先使用账号登录，在个人中心绑定微信";
  private static final String ACCOUNT_UNAVAILABLE_MESSAGE = "目标账号或租户暂不可用，请联系管理员";
  private static final String LOGIN_READY_MESSAGE = "微信验证成功，正在完成登录";

  /** 微信端回复的键值与缺省文案统一维护于此；字典 remark 非空时覆盖文案。 */
  private enum Reply {
    DEFAULT("default", "你好，欢迎关注 NexIoT 👋\n如需登录或绑定，请在页面扫码，或发送页面显示的验证码。\n回复“帮助”查看使用说明。"),
    SUBSCRIBE("subscribe", null),
    SCAN("scan", null),
    EVENT("event", null),
    LOGIN_SUCCESS("login_success", "登陆成功。"),
    CODE_INVALID("code_invalid", "验证码无效或已过期，请在刷新后重新发送。"),
    SESSION_EXPIRED("session_expired", "二维码或验证码已过期，请回到页面刷新。"),
    ACCOUNT_MISMATCH("account_mismatch", "当前验证码不属于此公众号，请核对登录页面。"),
    SESSION_PROCESSED("session_processed", "当前扫码会话已处理，请回到页面继续操作。"),
    CONFIG_CHANGED("config_changed", "公众号设置已变更，请重新生成二维码。"),
    BINDING_DISABLED("binding_disabled", "当前租户已关闭微信绑定。"),
    BINDING_PENDING("binding_pending", "已识别本次绑定请求，请回到发起操作的个人中心确认绑定。"),
    UNBOUND("unbound", "该微信尚未绑定当前租户账号，请先使用账号密码登录，在个人中心绑定微信。"),
    ACCOUNT_UNAVAILABLE("account_unavailable", "当前账号或租户暂不可用，请联系管理员。");

    private final String key;
    private final String defaultText;

    Reply(String key, String defaultText) {
      this.key = key;
      this.defaultText = defaultText;
    }
  }

  private static final Pattern MESSAGE_CODE =
      Pattern.compile("^(?:登录|绑定|login|bind)?\\s*([0-9]{6,8})$", Pattern.CASE_INSENSITIVE);
  private static final DefaultRedisScript<Long> CAS =
      new DefaultRedisScript<>(
          "local old=redis.call('get',KEYS[1]); if old~=ARGV[1] then return 0 end; "
              + "local ttl=redis.call('pttl',KEYS[1]); if ttl<=0 then return 0 end; "
              + "redis.call('psetex',KEYS[1],ttl,ARGV[2]); return 1",
          Long.class);
  private static final DefaultRedisScript<Long> RATE =
      new DefaultRedisScript<>(
          "local n=redis.call('incr',KEYS[1]); if n==1 then redis.call('expire',KEYS[1],ARGV[1]) end; return n",
          Long.class);
  private final StringRedisTemplate redis;
  private final WechatAccountConfigService configs;
  private final WechatQrApiClient qrClient;
  private final IoTUserMapper users;
  private final IoTTenantMapper tenants;
  private final TenantDomainResolver domains;
  private final SysDictDataMapper dictionaries;
  private final SecureRandom random = new SecureRandom();

  public WechatAuthService(
      StringRedisTemplate redis,
      WechatAccountConfigService configs,
      WechatQrApiClient qrClient,
      IoTUserMapper users,
      IoTTenantMapper tenants,
      TenantDomainResolver domains,
      SysDictDataMapper dictionaries) {
    this.redis = redis;
    this.configs = configs;
    this.qrClient = qrClient;
    this.users = users;
    this.tenants = tenants;
    this.domains = domains;
    this.dictionaries = dictionaries;
  }

  public Map<String, Object> options(String tenantCode) {
    if ((tenantCode == null || tenantCode.isBlank()) && domains.resolveCurrentTenant() == null) {
      return Map.of("enabled", false, "bindingEnabled", false);
    }
    IoTTenant tenant = resolveTenant(tenantCode);
    WechatLoginConfig config = configs.findByTenantId(tenant.getTenantId());
    if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
      return Map.of("enabled", false, "bindingEnabled", false);
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("enabled", true);
    result.put("bindingEnabled", Boolean.TRUE.equals(config.getBindingEnabled()));
    result.put("accountName", value(config.getAccountName()));
    result.put("appId", config.getAppId());
    result.put("loginMode", config.getLoginMode());
    result.put("capabilityMode", config.getCapabilityMode());
    result.put("accountQrUrl", value(config.getAccountQrUrl()));
    result.put("expireSeconds", config.getExpireSeconds());
    return result;
  }

  public Map<String, Object> createLogin(String tenantCode, String clientAddress) {
    IoTTenant tenant = resolveTenant(tenantCode);
    rate("create:ip:" + digest(value(clientAddress)), 30, 60);
    rate("create:tenant:" + tenant.getTenantId(), 500, 60);
    return create(configs.requireEnabled(tenant.getTenantId()), "LOGIN", null);
  }

  public Map<String, Object> createBinding(String ownerUnionId, String clientAddress) {
    IoTUser user = requireUser(ownerUnionId, null);
    rate("create:owner:" + digest(ownerUnionId), 10, 60);
    WechatLoginConfig config = configs.requireEnabled(user.getTenantId());
    if (!Boolean.TRUE.equals(config.getBindingEnabled())) throw new IoTException("当前租户未开启微信绑定");
    return create(config, "BIND", user.getUnionId());
  }

  private Map<String, Object> create(WechatLoginConfig config, String purpose, String owner) {
    SessionState session = new SessionState();
    session.setSessionId(randomHex(16));
    String proof = randomToken();
    session.setProofHash(digest(proof));
    session.setTenantId(config.getTenantId());
    session.setAppId(config.getAppId());
    session.setPurpose(purpose);
    session.setOwnerUnionId(owner);
    session.setConfigVersion(config.getConfigVersion());
    session.setLoginMode(config.getLoginMode());
    session.setStatus("WAITING");
    int ttl =
        config.getExpireSeconds() == null
            ? 180
            : Math.max(60, Math.min(600, config.getExpireSeconds()));
    session.setExpiresAt(System.currentTimeMillis() + ttl * 1000L);
    String transport = config.getCapabilityMode();
    String qrUrl = null;
    if (!"MESSAGE_CODE".equals(transport)) {
      try {
        qrUrl = qrClient.createQr(config, "nx_" + session.getSessionId(), ttl);
        transport = "SCENE_QR";
      } catch (WechatQrApiClient.CapabilityUnavailableException e) {
        if ("SCENE_QR".equals(transport)) throw e;
        transport = "MESSAGE_CODE";
      }
    }
    if ("MESSAGE_CODE".equals(transport)) {
      if (config.getAccountQrUrl() == null || config.getAccountQrUrl().isBlank()) {
        throw new IoTException("请先在公众号设置中上传或填写公众号关注二维码");
      }
      qrUrl = config.getAccountQrUrl();
      boolean allocated = false;
      for (int i = 0; i < 10; i++) {
        String code = String.format(java.util.Locale.ROOT, "%06d", random.nextInt(1_000_000));
        if (Boolean.TRUE.equals(
            redis
                .opsForValue()
                .setIfAbsent(
                    codeKey(config.getAppId(), code),
                    session.getSessionId(),
                    Duration.ofSeconds(ttl)))) {
          session.setVerificationCode(code);
          allocated = true;
          break;
        }
      }
      if (!allocated) throw new IoTException("验证码生成繁忙，请稍后重试");
    }
    session.setTransportMode(transport);
    session.setQrUrl(qrUrl);
    redis
        .opsForValue()
        .set(
            SESSION_PREFIX + session.getSessionId(),
            JSONUtil.toJsonStr(session),
            Duration.ofSeconds(ttl));
    Map<String, Object> result = publicState(session);
    result.put("browserProof", proof);
    result.put("qrUrl", qrUrl);
    result.put("verificationCode", value(session.getVerificationCode()));
    result.put("transportMode", transport);
    result.put(
        "instructions",
        "MESSAGE_CODE".equals(transport)
            ? "使用微信扫描公众号二维码，关注后发送："
                + ("BIND".equals(purpose) ? "绑定 " : "登录 ")
                + session.getVerificationCode()
            : "使用微信扫描二维码，首次使用请先关注公众号");
    return result;
  }

  public Map<String, Object> status(String id, String proof) {
    SessionState session = read(id);
    verifyProof(session, proof);
    if (!terminal(session.getStatus())) {
      try {
        requireCurrent(session);
      } catch (IoTException e) {
        transition(
            session,
            s -> {
              s.setStatus("CANCELLED");
              s.setMessage("公众号登录设置已变更，请重新生成二维码");
            });
      }
    }
    return publicState(read(id));
  }

  public void cancel(String id, String proof) {
    SessionState session = read(id);
    verifyProof(session, proof);
    if (!terminal(session.getStatus())) {
      transition(
          session,
          s -> {
            s.setStatus("CANCELLED");
            s.setMessage("二维码已取消");
          });
    }
  }

  public String createWebSocketTicket(String id, String proof) {
    SessionState session = read(id);
    verifyProof(session, proof);
    if (terminal(session.getStatus())) throw new IoTException("扫码会话已结束");
    requireCurrent(session);
    rate("socket:" + id, 20, 60);
    String ticket = randomToken();
    redis.opsForValue().set(TICKET_PREFIX + ticket, id, Duration.ofSeconds(20));
    return ticket;
  }

  public String consumeWebSocketTicket(String ticket) {
    if (ticket == null || !ticket.matches("[A-Za-z0-9_-]{43}")) throw new IoTException("扫码连接票据无效");
    String id = redis.opsForValue().getAndDelete(TICKET_PREFIX + ticket);
    if (id == null) throw new IoTException("扫码连接票据已失效");
    SessionState session = read(id);
    requireCurrent(session);
    return id;
  }

  /** 仅供已通过一次性握手票据认证的本机 WS 使用，不向公共接口开放。 */
  public Map<String, Object> socketState(String id) {
    return publicState(read(id));
  }

  public String consumeLogin(String id, String proof) {
    SessionState session = read(id);
    verifyProof(session, proof);
    if (!"LOGIN".equals(session.getPurpose()) || !"READY".equals(session.getStatus())) {
      throw new IoTException("扫码尚未完成，或登录凭据已使用");
    }
    WechatLoginConfig config = requireCurrent(session);
    IoTUser user = requireUser(session.getTargetUnionId(), session.getTenantId());
    if ("DEMO_ACCOUNT".equals(config.getLoginMode())) {
      if (!Objects.equals(user.getUnionId(), config.getDemoUserUnionId()))
        throw new IoTException("演示账号已变更");
    } else if (!Objects.equals(
        user.getUnionId(),
        configs.findBoundUser(session.getTenantId(), session.getAppId(), session.getOpenId()))) {
      throw new IoTException("微信绑定已变更，请重新扫码");
    }
    if (!transition(
        session,
        s -> {
          s.setStatus("CONSUMED");
          s.setMessage("登录凭据已使用");
        })) {
      throw new IoTException("登录凭据已使用，请重新扫码");
    }
    return user.getUnionId();
  }

  public void confirmBinding(String id, String proof, String ownerUnionId) {
    SessionState session = read(id);
    verifyProof(session, proof);
    if (!"BIND".equals(session.getPurpose())
        || !Objects.equals(ownerUnionId, session.getOwnerUnionId())
        || !"SCANNED".equals(session.getStatus())) throw new IoTException("绑定会话无效或尚未扫码");
    WechatLoginConfig config = requireCurrent(session);
    if (!Boolean.TRUE.equals(config.getBindingEnabled())) throw new IoTException("微信绑定已关闭");
    requireUser(ownerUnionId, session.getTenantId());
    if (!transition(session, s -> s.setStatus("BINDING"))) throw new IoTException("绑定已处理，请刷新页面");
    SessionState claimed = read(id);
    try {
      configs.bind(session.getTenantId(), session.getAppId(), session.getOpenId(), ownerUnionId);
      transition(
          claimed,
          s -> {
            s.setStatus("BOUND");
            s.setMessage("微信绑定成功");
          });
    } catch (RuntimeException e) {
      transition(
          claimed,
          s -> {
            s.setStatus("FAILED");
            s.setMessage("微信绑定冲突或配置已变更，请重新操作");
          });
      throw e;
    }
  }

  /** 登录/绑定凭据优先处理；普通消息、扫码与事件按关键词回复，未命中使用默认回复。 */
  public String handleMessage(WechatLoginConfig callbackConfig, Map<String, String> message) {
    String openId = message.get("FromUserName");
    if (openId == null || !openId.matches("[A-Za-z0-9_-]{1,128}"))
      throw new IoTException("微信消息身份无效");
    String event = message.getOrDefault("Event", "");
    if ("unsubscribe".equalsIgnoreCase(event)) {
      configs.clearBindings(callbackConfig.getAppId(), openId);
      return null;
    }
    String id;
    if ("text".equalsIgnoreCase(message.get("MsgType"))) {
      Matcher matcher = MESSAGE_CODE.matcher(message.getOrDefault("Content", "").trim());
      if (!matcher.matches()) return messageReply(message);
      rate("message:openid:" + digest(callbackConfig.getAppId() + ":" + openId), 10, 60);
      id = redis.opsForValue().get(codeKey(callbackConfig.getAppId(), matcher.group(1)));
      if (id == null) return reply(Reply.CODE_INVALID);
    } else if ("event".equalsIgnoreCase(message.get("MsgType"))
        && ("SCAN".equalsIgnoreCase(event) || "subscribe".equalsIgnoreCase(event))) {
      String scene = message.getOrDefault("EventKey", "");
      if ("subscribe".equalsIgnoreCase(event) && scene.startsWith("qrscene_"))
        scene = scene.substring(8);
      if (!scene.matches("nx_[a-f0-9]{32}")) return messageReply(message);
      id = scene.substring(3);
    } else {
      return messageReply(message);
    }
    SessionState session;
    try {
      session = read(id);
    } catch (IoTException e) {
      return reply(Reply.SESSION_EXPIRED);
    }
    if (!Objects.equals(callbackConfig.getAppId(), session.getAppId())
        || !Objects.equals(callbackConfig.getTenantId(), session.getTenantId())) {
      return reply(Reply.ACCOUNT_MISMATCH);
    }
    if (!"WAITING".equals(session.getStatus())) return reply(Reply.SESSION_PROCESSED);
    WechatLoginConfig config;
    try {
      config = requireCurrent(session);
    } catch (IoTException e) {
      return reply(Reply.CONFIG_CHANGED);
    }
    if ("BIND".equals(session.getPurpose())) {
      if (!Boolean.TRUE.equals(config.getBindingEnabled())) return reply(Reply.BINDING_DISABLED);
      requireUser(session.getOwnerUnionId(), session.getTenantId());
      transition(
          session,
          s -> {
            s.setStatus("SCANNED");
            s.setOpenId(openId);
            s.setMessage(BINDING_SCANNED_MESSAGE);
          });
      return reply(Reply.BINDING_PENDING);
    }
    String target =
        "DEMO_ACCOUNT".equals(config.getLoginMode())
            ? config.getDemoUserUnionId()
            : configs.findBoundUser(session.getTenantId(), session.getAppId(), openId);
    if (target == null || target.isBlank()) {
      transition(
          session,
          s -> {
            s.setStatus("UNBOUND");
            s.setMessage(UNBOUND_MESSAGE);
          });
      return reply(Reply.UNBOUND);
    }
    try {
      requireUser(target, session.getTenantId());
    } catch (IoTException e) {
      transition(
          session,
          s -> {
            s.setStatus("FAILED");
            s.setMessage(ACCOUNT_UNAVAILABLE_MESSAGE);
          });
      return reply(Reply.ACCOUNT_UNAVAILABLE);
    }
    transition(
        session,
        s -> {
          s.setStatus("READY");
          s.setOpenId(openId);
          s.setTargetUnionId(target);
          s.setMessage(LOGIN_READY_MESSAGE);
        });
    return reply(Reply.LOGIN_SUCCESS);
  }

  private String messageReply(Map<String, String> message) {
    boolean eventMessage = "event".equalsIgnoreCase(message.get("MsgType"));
    String content = eventMessage ? value(message.get("EventKey")) : value(message.get("Content"));
    if (eventMessage
        && "subscribe".equalsIgnoreCase(message.get("Event"))
        && content.startsWith("qrscene_")) content = content.substring(8);
    if (content.isBlank() && eventMessage) {
      content = value(message.get("Event"));
    }
    String keyword = content.trim();
    if (!keyword.isEmpty()) {
      for (SysDictData item : enabledReplies(KEYWORD_REPLY_DICT_TYPE)) {
        // 标签是关键词，键值是管理标识；完整匹配，不让普通关键词拦截登录/绑定凭据。
        if (keyword.equalsIgnoreCase(value(item.getDictLabel()).trim())
            && item.getRemark() != null
            && !item.getRemark().isBlank()) {
          return item.getRemark();
        }
      }
    }
    if (eventMessage) {
      String event = message.get("Event");
      if ("subscribe".equalsIgnoreCase(event)) return reply(Reply.SUBSCRIBE);
      if ("SCAN".equalsIgnoreCase(event)) return reply(Reply.SCAN);
      return reply(Reply.EVENT);
    }
    return reply(Reply.DEFAULT);
  }

  private String reply(Reply reply) {
    for (SysDictData item : enabledReplies(REPLY_DICT_TYPE)) {
      if (reply.key.equals(item.getDictValue())
          && item.getRemark() != null
          && !item.getRemark().isBlank()) {
        return item.getRemark();
      }
    }
    return reply.defaultText == null ? reply(Reply.DEFAULT) : reply.defaultText;
  }

  private List<SysDictData> enabledReplies(String dictType) {
    try {
      // 不增加本地缓存，后台更新字典后下一条消息即可使用新文案。
      List<SysDictData> items = dictionaries.selectDictDataByType(dictType);
      return items == null
          ? List.of()
          : items.stream().filter(item -> item != null && "0".equals(item.getStatus())).toList();
    } catch (DataAccessException e) {
      log.warn("微信回复字典 {} 暂不可用，使用默认回复", dictType);
      return List.of();
    }
  }

  private IoTTenant resolveTenant(String tenantCode) {
    IoTTenant domainTenant = domains.resolveCurrentTenant();
    IoTTenant tenant = domainTenant;
    if (tenantCode != null && !tenantCode.isBlank()) {
      if (!tenantCode.matches("[A-Za-z0-9_-]{1,64}")) throw new IoTException("租户标识无效");
      IoTTenant query = new IoTTenant();
      query.setTenantCode(tenantCode);
      tenant = tenants.selectOne(query);
      if (domainTenant != null
          && (tenant == null
              || !Objects.equals(domainTenant.getTenantId(), tenant.getTenantId()))) {
        throw new IoTException("所选租户与当前访问域名不一致");
      }
    }
    requireTenant(tenant);
    return tenant;
  }

  private void requireTenant(IoTTenant tenant) {
    if (tenant == null
        || Integer.valueOf(1).equals(tenant.getDeleted())
        || !Integer.valueOf(0).equals(tenant.getStatus())
        || (tenant.getExpireTime() != null && tenant.getExpireTime().before(new Date()))) {
      throw new IoTException("租户不存在、已停用或已到期");
    }
  }

  private IoTUser requireUser(String unionId, String tenantId) {
    if (unionId == null || unionId.isBlank()) throw new IoTException("账号不可用");
    IoTUser query = new IoTUser();
    query.setUnionId(unionId);
    IoTUser user = users.selectOne(query);
    if (user == null
        || user.isAdmin()
        || !Integer.valueOf(0).equals(user.getDeleted())
        || !Integer.valueOf(0).equals(user.getStatus())
        || user.getTenantId() == null
        || (tenantId != null && !Objects.equals(tenantId, user.getTenantId())))
      throw new IoTException("账号不可用");
    requireTenant(tenants.selectByPrimaryKey(user.getTenantId()));
    return user;
  }

  private WechatLoginConfig requireCurrent(SessionState session) {
    WechatLoginConfig config = configs.requireEnabled(session.getTenantId());
    requireTenant(tenants.selectByPrimaryKey(session.getTenantId()));
    if (!Objects.equals(config.getAppId(), session.getAppId())
        || !Objects.equals(config.getConfigVersion(), session.getConfigVersion())
        || !Objects.equals(config.getLoginMode(), session.getLoginMode())) {
      throw new IoTException("公众号设置已变更，请重新扫码");
    }
    return config;
  }

  private SessionState read(String id) {
    if (id == null || !id.matches("[a-f0-9]{32}")) throw new IoTException("扫码会话无效");
    String raw = redis.opsForValue().get(SESSION_PREFIX + id);
    if (raw == null) throw new IoTException("二维码已过期，请重新生成");
    SessionState session = JSONUtil.toBean(raw, SessionState.class);
    session.setRaw(raw);
    if (session.getExpiresAt() <= System.currentTimeMillis())
      throw new IoTException("二维码已过期，请重新生成");
    return session;
  }

  private void verifyProof(SessionState session, String proof) {
    if (proof == null
        || !proof.matches("[A-Za-z0-9_-]{43}")
        || !MessageDigest.isEqual(
            digest(proof).getBytes(StandardCharsets.US_ASCII),
            session.getProofHash().getBytes(StandardCharsets.US_ASCII)))
      throw new IoTException("扫码会话凭据无效");
  }

  private boolean transition(SessionState session, Consumer<SessionState> mutation) {
    String before = session.getRaw() == null ? JSONUtil.toJsonStr(session) : session.getRaw();
    mutation.accept(session);
    session.setRaw(null);
    String after = JSONUtil.toJsonStr(session);
    Long changed =
        redis.execute(CAS, List.of(SESSION_PREFIX + session.getSessionId()), before, after);
    if (!Long.valueOf(1).equals(changed)) return false;
    session.setRaw(after);
    try {
      // 消息仅携带路由 ID，各节点读取共享最终状态；不广播系统 Token 或微信身份。
      redis.convertAndSend(STATE_TOPIC, session.getSessionId());
    } catch (RuntimeException e) {
      log.warn("微信扫码状态通知暂不可用，浏览器将通过状态查询恢复");
    }
    return true;
  }

  private Map<String, Object> publicState(SessionState session) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("sessionId", session.getSessionId());
    result.put("status", session.getStatus());
    result.put("message", value(session.getMessage()));
    result.put(
        "expiresIn",
        Math.max(0, (session.getExpiresAt() - System.currentTimeMillis() + 999) / 1000));
    return result;
  }

  private boolean terminal(String status) {
    return List.of("CONSUMED", "BOUND", "UNBOUND", "FAILED", "CANCELLED", "EXPIRED")
        .contains(status);
  }

  private void rate(String suffix, int limit, int seconds) {
    Long count =
        redis.execute(RATE, List.of("auth:wechat:rate:" + suffix), Integer.toString(seconds));
    if (count == null || count > limit) throw new IoTException("操作过于频繁，请稍后重试");
  }

  private String codeKey(String appId, String code) {
    return "auth:wechat:code:" + appId + ":" + code;
  }

  private String randomToken() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String randomHex(int size) {
    byte[] bytes = new byte[size];
    random.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  private static String value(String value) {
    return value == null ? "" : value;
  }

  private static String digest(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("摘要计算失败");
    }
  }

  @Getter
  @Setter
  public static class SessionState {
    private String sessionId;
    private String proofHash;
    private String tenantId;
    private String appId;
    private String purpose;
    private String ownerUnionId;
    private Long configVersion;
    private String loginMode;
    private String status;
    private String message;
    private String openId;
    private String targetUnionId;
    private String verificationCode;
    private String transportMode;
    private String qrUrl;
    private long expiresAt;

    @com.fasterxml.jackson.annotation.JsonIgnore @cn.hutool.core.annotation.PropIgnore
    private transient String raw;
  }
}
