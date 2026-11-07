package cn.universal.security.wechat;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.WechatLoginConfig;
import cn.universal.persistence.entity.WechatUserBinding;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.persistence.mapper.WechatLoginConfigMapper;
import cn.universal.persistence.mapper.WechatUserBindingMapper;
import cn.universal.security.support.TenantIgnoreContext;
import java.net.URI;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

/** 显式租户约束的公众号配置及账号身份关系，不使用通知收件人映射授予登录权限。 */
@Service
public class WechatAccountConfigService {
  private final WechatLoginConfigMapper configs;
  private final WechatUserBindingMapper bindings;
  private final IoTTenantMapper tenants;
  private final IoTUserMapper users;
  private final WechatConfigCrypto crypto;
  @Value("${NEXIOT_WECHAT_CALLBACK_BASE_URL:}") private String callbackBaseUrl;

  public WechatAccountConfigService(WechatLoginConfigMapper configs, WechatUserBindingMapper bindings,
      IoTTenantMapper tenants, IoTUserMapper users, WechatConfigCrypto crypto) {
    this.configs = configs;
    this.bindings = bindings;
    this.tenants = tenants;
    this.users = users;
    this.crypto = crypto;
  }

  public WechatLoginConfig findByTenantId(String tenantId) {
    requireIdentifier(tenantId, "租户标识");
    WechatLoginConfig stored = unscoped(() -> configs.selectByPrimaryKey(tenantId));
    return stored == null ? null : decrypt(stored);
  }

  public WechatLoginConfig findByTenantCode(String tenantCode) {
    if (!StringUtils.hasText(tenantCode) || !tenantCode.matches("[a-zA-Z0-9_-]{1,100}")) return null;
    Example query = new Example(IoTTenant.class);
    query.createCriteria().andEqualTo("tenantCode", tenantCode).andEqualTo("deleted", 0);
    IoTTenant tenant = unscoped(() -> tenants.selectOneByExample(query));
    if (tenant == null) return null;
    requireActiveTenant(tenant.getTenantId());
    return findByTenantId(tenant.getTenantId());
  }

  public WechatLoginConfig requireEnabled(String tenantId) {
    requireActiveTenant(tenantId);
    WechatLoginConfig config = findByTenantId(tenantId);
    if (config == null || !Boolean.TRUE.equals(config.getEnabled()))
      throw new IoTException("当前租户未启用微信公众号登录");
    return config;
  }

  /** 仅此管理投影可以返回浏览器；实体内包含已解密的内部凭据。 */
  public Map<String, Object> safeConfig(String tenantId) {
    requireIdentifier(tenantId, "租户标识");
    WechatLoginConfig stored = unscoped(() -> configs.selectByPrimaryKey(tenantId));
    WechatLoginConfig value = stored == null ? defaults(tenantId) : stored;
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("tenantId", tenantId);
    result.put("accountName", value.getAccountName());
    result.put("appId", value.getAppId());
    result.put("accountQrUrl", value.getAccountQrUrl());
    result.put("capabilityMode", value.getCapabilityMode());
    result.put("loginMode", value.getLoginMode());
    result.put("enabled", value.getEnabled());
    result.put("bindingEnabled", value.getBindingEnabled());
    result.put("demoUserUnionId", value.getDemoUserUnionId());
    result.put("expireSeconds", value.getExpireSeconds());
    result.put("configVersion", value.getConfigVersion());
    result.put("appSecretConfigured", StringUtils.hasText(value.getAppSecret()));
    result.put("callbackTokenConfigured", StringUtils.hasText(value.getCallbackToken()));
    result.put("encodingAESKeyConfigured", StringUtils.hasText(value.getEncodingAESKey()));
    result.put("createTime", value.getCreateTime());
    result.put("updateTime", value.getUpdateTime());
    IoTTenant tenant = unscoped(() -> tenants.selectByPrimaryKey(tenantId));
    String path = tenant == null ? "" : "/wechat/callback/" + tenant.getTenantCode();
    String base = callbackBaseUrl == null ? "" : callbackBaseUrl.trim().replaceAll("/+$", "");
    result.put("callbackPath", path);
    result.put("callbackUrl", base + path);
    return result;
  }

  @Transactional
  public void save(String tenantId, WechatLoginConfig request) {
    save(tenantId, request, false);
  }

  /**
   * 保存公众号配置；密钥留空保留原值，回调按入站消息自动识别加密方式。
   * clearEncodingAESKey 仅表达管理员显式删除已保存密钥的意图。
   */
  @Transactional
  public void save(String tenantId, WechatLoginConfig request, boolean clearEncodingAESKey) {
    requireActiveTenant(tenantId);
    if (request == null) throw new IoTException("公众号配置不能为空");
    WechatLoginConfig stored = unscoped(() -> configs.selectByPrimaryKey(tenantId));
    WechatLoginConfig previous = stored == null ? null : decrypt(stored);
    if (stored != null && request.getConfigVersion() != null
        && !Objects.equals(request.getConfigVersion(), stored.getConfigVersion()))
      throw new IoTException("公众号配置已被更新，请刷新后重试");
    WechatLoginConfig next = defaults(tenantId);
    if (previous != null) BeanUtils.copyProperties(previous, next);
    if (request.getAccountName() != null) next.setAccountName(request.getAccountName().trim());
    if (request.getAppId() != null) next.setAppId(request.getAppId().trim());
    if (request.getAccountQrUrl() != null) next.setAccountQrUrl(request.getAccountQrUrl().trim());
    if (request.getCapabilityMode() != null) next.setCapabilityMode(request.getCapabilityMode());
    if (request.getLoginMode() != null) next.setLoginMode(request.getLoginMode());
    if (request.getEnabled() != null) next.setEnabled(request.getEnabled());
    if (request.getBindingEnabled() != null) next.setBindingEnabled(request.getBindingEnabled());
    if (request.getDemoUserUnionId() != null) next.setDemoUserUnionId(request.getDemoUserUnionId().trim());
    if (request.getExpireSeconds() != null) next.setExpireSeconds(request.getExpireSeconds());
    if (StringUtils.hasText(request.getAppSecret())) next.setAppSecret(request.getAppSecret().trim());
    if (StringUtils.hasText(request.getCallbackToken())) next.setCallbackToken(request.getCallbackToken().trim());
    if (clearEncodingAESKey) next.setEncodingAESKey(null);
    else if (StringUtils.hasText(request.getEncodingAESKey())) next.setEncodingAESKey(request.getEncodingAESKey().trim());
    if (previous != null && !Objects.equals(previous.getAppId(), next.getAppId())
        && !bindings(tenantId, null).isEmpty())
      throw new IoTException("当前公众号仍有账号绑定，请先解除绑定后再更换 AppID");
    validate(next);
    next.setTenantId(tenantId);
    next.setConfigVersion(stored == null ? 1L : stored.getConfigVersion() + 1L);
    Date now = new Date();
    next.setCreateTime(stored == null ? now : stored.getCreateTime());
    next.setUpdateTime(now);
    WechatLoginConfig encrypted = encrypt(next);
    try {
      if (stored == null) unscoped(() -> configs.insertSelective(encrypted));
      else {
        Example match = new Example(WechatLoginConfig.class);
        match.createCriteria().andEqualTo("tenantId", tenantId).andEqualTo("configVersion", stored.getConfigVersion());
        if (unscoped(() -> configs.updateByExample(encrypted, match)) != 1)
          throw new IoTException("公众号配置已被更新，请刷新后重试");
      }
    } catch (DuplicateKeyException e) {
      throw new IoTException("公众号配置已被创建，请刷新后重试");
    }
  }

  public String findBoundUser(String tenantId, String appId, String openId) {
    Example query = identityQuery(tenantId, appId, openId);
    WechatUserBinding binding = unscoped(() -> bindings.selectOneByExample(query));
    return binding == null ? null : binding.getUserUnionId();
  }

  @Transactional
  public void bind(String tenantId, String appId, String openId, String userUnionId) {
    WechatLoginConfig config = requireEnabled(tenantId);
    if (!Boolean.TRUE.equals(config.getBindingEnabled()) || !Objects.equals(config.getAppId(), appId))
      throw new IoTException("当前公众号不可绑定，请重新生成二维码");
    requireAvailableUser(tenantId, userUnionId);
    Example identity = identityQuery(tenantId, appId, openId);
    WechatUserBinding existing = unscoped(() -> bindings.selectOneByExample(identity));
    if (existing != null) {
      if (!Objects.equals(existing.getUserUnionId(), userUnionId))
        throw new IoTException("该微信已绑定本租户其他账号，请先在原账号解除绑定");
      return;
    }
    WechatUserBinding binding = new WechatUserBinding();
    binding.setId(UUID.randomUUID().toString().replace("-", ""));
    binding.setTenantId(tenantId);
    binding.setAppId(appId);
    binding.setOpenId(openId);
    binding.setUserUnionId(userUnionId);
    binding.setCreateTime(new Date());
    binding.setUpdateTime(binding.getCreateTime());
    try { unscoped(() -> bindings.insertSelective(binding)); }
    catch (DuplicateKeyException e) { throw new IoTException("该账号或微信已绑定，请先解除原绑定后重试"); }
  }

  @Transactional
  public void unbind(String tenantId, String userUnionId) {
    requireIdentifier(tenantId, "租户标识");
    requireIdentifier(userUnionId, "账号标识");
    Example query = new Example(WechatUserBinding.class);
    query.createCriteria().andEqualTo("tenantId", tenantId).andEqualTo("userUnionId", userUnionId);
    unscoped(() -> bindings.deleteByExample(query));
  }

  public List<WechatUserBinding> bindings(String tenantId, String userUnionId) {
    requireIdentifier(tenantId, "租户标识");
    Example query = new Example(WechatUserBinding.class);
    Example.Criteria criteria = query.createCriteria().andEqualTo("tenantId", tenantId);
    if (userUnionId != null) {
      requireIdentifier(userUnionId, "账号标识");
      criteria.andEqualTo("userUnionId", userUnionId);
    }
    return unscoped(() -> bindings.selectByExample(query));
  }

  /** 来自已验签公众号取消关注事件，清除该公众号身份在所有租户的关系。 */
  @Transactional
  public void clearBindings(String appId, String openId) {
    requireWechatIdentity(appId, openId);
    Example query = new Example(WechatUserBinding.class);
    query.createCriteria().andEqualTo("appId", appId).andEqualTo("openId", openId);
    unscoped(() -> bindings.deleteByExample(query));
  }

  public List<Map<String, String>> availableDemoUsers(String tenantId, String keyword) {
    requireIdentifier(tenantId, "租户标识");
    Example query = new Example(IoTUser.class);
    query.selectProperties("unionId", "username", "alias");
    query.createCriteria().andEqualTo("tenantId", tenantId).andEqualTo("deleted", 0)
        .andEqualTo("status", 0).andNotEqualTo("identity", 0);
    if (StringUtils.hasText(keyword)) {
      String search = keyword.trim();
      if (search.length() > 100) throw new IoTException("搜索关键字过长");
      String escaped = search.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
      query.and().andLike("username", "%" + escaped + "%").orLike("alias", "%" + escaped + "%");
    }
    query.orderBy("username").asc();
    return unscoped(() -> users.selectByExampleAndRowBounds(query, new org.apache.ibatis.session.RowBounds(0, 20)))
        .stream().map(user -> Map.of("id", user.getUnionId(), "unionId", user.getUnionId(),
            "username", user.getUsername(), "nickname", StringUtils.hasText(user.getAlias()) ? user.getAlias() : user.getUsername(),
            "name", StringUtils.hasText(user.getAlias()) ? user.getAlias() + " (" + user.getUsername() + ")" : user.getUsername()))
        .toList();
  }

  private void validate(WechatLoginConfig config) {
    if (!Set.of("AUTO", "SCENE_QR", "MESSAGE_CODE").contains(config.getCapabilityMode()))
      throw new IoTException("公众号接入方式无效");
    if (!Set.of("BOUND_USER", "DEMO_ACCOUNT").contains(config.getLoginMode()))
      throw new IoTException("公众号登录模式无效");
    if (config.getExpireSeconds() < 60 || config.getExpireSeconds() > 600)
      throw new IoTException("二维码或一次码有效期须为 60 至 600 秒");
    if (config.getAccountName() != null && config.getAccountName().length() > 100)
      throw new IoTException("公众号名称不能超过 100 字符");
    if (StringUtils.hasText(config.getAppId()) && !config.getAppId().matches("wx[a-zA-Z0-9]{16}"))
      throw new IoTException("公众号 AppID 格式无效");
    if (StringUtils.hasText(config.getCallbackToken()) && !config.getCallbackToken().matches("[a-zA-Z0-9]{3,32}"))
      throw new IoTException("公众号 Token 须为 3 至 32 位字母或数字");
    if (StringUtils.hasText(config.getEncodingAESKey())) {
      try {
        if (!config.getEncodingAESKey().matches("[a-zA-Z0-9+/]{43}")
            || Base64.getDecoder().decode(config.getEncodingAESKey() + "=").length != 32)
          throw new IllegalArgumentException();
      } catch (IllegalArgumentException e) { throw new IoTException("EncodingAESKey 须为微信平台生成的 43 位密钥"); }
    }
    validateQrUrl(config.getAccountQrUrl());
    if ("DEMO_ACCOUNT".equals(config.getLoginMode()))
      requireAvailableUser(config.getTenantId(), config.getDemoUserUnionId());
    if (!Boolean.TRUE.equals(config.getEnabled())) return;
    if (!StringUtils.hasText(config.getAccountName()) || !StringUtils.hasText(config.getAppId())
        || !StringUtils.hasText(config.getCallbackToken()))
      throw new IoTException("启用前请填写公众号名称、AppID 和 Token");
    if ("SCENE_QR".equals(config.getCapabilityMode()) && !StringUtils.hasText(config.getAppSecret()))
      throw new IoTException("带参数二维码模式需要已认证服务号的 AppSecret");
    if (!"SCENE_QR".equals(config.getCapabilityMode()) && !StringUtils.hasText(config.getAccountQrUrl()))
      throw new IoTException("请上传公众号静态二维码，供用户扫码后发送一次码");
  }

  private void validateQrUrl(String value) {
    if (!StringUtils.hasText(value)) return;
    if (value.length() > 2048) throw new IoTException("公众号二维码地址过长");
    try {
      URI uri = URI.create(value);
      if (value.startsWith("/") && !value.startsWith("//") && uri.getHost() == null) return;
      if (("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
          && uri.getHost() != null && uri.getUserInfo() == null) return;
    } catch (IllegalArgumentException ignored) { }
    throw new IoTException("公众号二维码须使用 HTTP(S) 地址或站内资源路径");
  }

  private void requireActiveTenant(String tenantId) {
    requireIdentifier(tenantId, "租户标识");
    IoTTenant tenant = unscoped(() -> tenants.selectByPrimaryKey(tenantId));
    if (tenant == null || Integer.valueOf(1).equals(tenant.getDeleted())) throw new IoTException("租户不存在");
    if (!Integer.valueOf(0).equals(tenant.getStatus())) throw new IoTException("当前租户已停用");
    if (tenant.getExpireTime() != null && !tenant.getExpireTime().after(new Date()))
      throw new IoTException("当前租户已过期");
  }

  private void requireAvailableUser(String tenantId, String userUnionId) {
    requireIdentifier(userUnionId, "账号标识");
    Example query = new Example(IoTUser.class);
    query.createCriteria().andEqualTo("tenantId", tenantId).andEqualTo("unionId", userUnionId)
        .andEqualTo("deleted", 0).andEqualTo("status", 0);
    IoTUser user = unscoped(() -> users.selectOneByExample(query));
    if (user == null || user.isAdmin()) throw new IoTException("请选择本租户内正常启用的普通账号，不能使用平台管理员");
  }

  private Example identityQuery(String tenantId, String appId, String openId) {
    requireIdentifier(tenantId, "租户标识");
    requireWechatIdentity(appId, openId);
    Example query = new Example(WechatUserBinding.class);
    query.createCriteria().andEqualTo("tenantId", tenantId).andEqualTo("appId", appId).andEqualTo("openId", openId);
    return query;
  }

  private void requireWechatIdentity(String appId, String openId) {
    if (appId == null || !appId.matches("wx[a-zA-Z0-9]{16}")
        || openId == null || !openId.matches("[a-zA-Z0-9_-]{1,128}"))
      throw new IoTException("微信身份标识无效");
  }

  private void requireIdentifier(String value, String label) {
    if (!StringUtils.hasText(value) || value.length() > 100) throw new IoTException(label + "无效");
  }

  private WechatLoginConfig encrypt(WechatLoginConfig value) {
    WechatLoginConfig result = copy(value);
    result.setAppSecret(crypto.encrypt(value.getAppSecret(), value.getTenantId() + ":appSecret"));
    result.setCallbackToken(crypto.encrypt(value.getCallbackToken(), value.getTenantId() + ":callbackToken"));
    result.setEncodingAESKey(crypto.encrypt(value.getEncodingAESKey(), value.getTenantId() + ":encodingAESKey"));
    return result;
  }

  private WechatLoginConfig decrypt(WechatLoginConfig value) {
    WechatLoginConfig result = copy(value);
    result.setAppSecret(crypto.decrypt(value.getAppSecret(), value.getTenantId() + ":appSecret"));
    result.setCallbackToken(crypto.decrypt(value.getCallbackToken(), value.getTenantId() + ":callbackToken"));
    result.setEncodingAESKey(crypto.decrypt(value.getEncodingAESKey(), value.getTenantId() + ":encodingAESKey"));
    return result;
  }

  private WechatLoginConfig copy(WechatLoginConfig value) {
    WechatLoginConfig result = new WechatLoginConfig();
    BeanUtils.copyProperties(value, result);
    return result;
  }

  private WechatLoginConfig defaults(String tenantId) {
    WechatLoginConfig config = new WechatLoginConfig();
    config.setTenantId(tenantId);
    config.setCapabilityMode("MESSAGE_CODE");
    config.setLoginMode("BOUND_USER");
    config.setEnabled(false);
    config.setBindingEnabled(true);
    config.setExpireSeconds(180);
    config.setConfigVersion(1L);
    return config;
  }

  private <T> T unscoped(Supplier<T> action) {
    TenantIgnoreContext.enter();
    try { return action.get(); } finally { TenantIgnoreContext.exit(); }
  }
}
