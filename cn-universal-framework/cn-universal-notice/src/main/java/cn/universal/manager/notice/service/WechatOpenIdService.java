package cn.universal.manager.notice.service;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.manager.notice.mapper.WechatOpenIdBindingMapper;
import cn.universal.manager.notice.model.NoticeChannel;
import cn.universal.manager.notice.model.WechatOpenIdBinding;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

/** 服务号 OAuth 授权辅助，前端扫码/微信内授权后用 code 换取 OpenID。 */
@Service
public class WechatOpenIdService {
  @Autowired private WechatOpenIdBindingMapper bindingMapper;
  @Autowired private NoticeChannelService channels;
  @Autowired private NoticeAccess access;

  private NoticeChannel channel(Long id) {
    NoticeChannel channel = channels.getById(id);
    if (channel == null || !"wechat".equals(channel.getChannelType()) || "1".equals(channel.getStatus()))
      throw new IllegalArgumentException("微信渠道不可用");
    if (channel.getTenantId() == null) throw new IllegalArgumentException("微信渠道缺少租户归属");
    appId(channel);
    return channel;
  }

  private String appId(NoticeChannel channel) {
    String appId = JSONUtil.parseObj(channel.getConfig()).getStr("appId");
    if (appId == null || !appId.matches("[a-zA-Z0-9_-]{1,128}")) throw new IllegalArgumentException("公众号 AppID 无效");
    return appId;
  }

  private Example bindingQuery(NoticeChannel channel, String userKey) {
    Example example = new Example(WechatOpenIdBinding.class);
    example.createCriteria().andEqualTo("appId", appId(channel)).andEqualTo("userKey", userKey)
        .andEqualTo("tenantId", channel.getTenantId());
    return example;
  }

  private void validateChannel(NoticeChannel channel) {
    if (channel == null || !"wechat".equals(channel.getChannelType()) || "1".equals(channel.getStatus()))
      throw new IllegalArgumentException("微信渠道不可用");
    access.check(channel, channel.getCreator());
    appId(channel);
  }

  public String authorizationUrl(NoticeChannel channel, String redirectUri, String state) {
    validateChannel(channel);
    JSONObject c = JSONUtil.parseObj(channel.getConfig());
    String appId = c.getStr("appId");
    if (!StringUtils.hasText(appId) || !StringUtils.hasText(redirectUri))
      throw new IllegalArgumentException("公众号 AppID 和回调地址不能为空");
    return "https://open.weixin.qq.com/connect/oauth2/authorize?appid="
        + appId
        + "&redirect_uri="
        + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
        + "&response_type=code&scope=snsapi_base&state="
        + URLEncoder.encode(state == null ? "" : state, StandardCharsets.UTF_8)
        + "#wechat_redirect";
  }

  public String exchange(NoticeChannel channel, String code) {
    validateChannel(channel);
    if (code == null || !code.matches("[a-zA-Z0-9_-]{1,256}")) throw new IllegalArgumentException("微信授权 code 无效");
    JSONObject c = JSONUtil.parseObj(channel.getConfig());
    String url =
        "https://api.weixin.qq.com/sns/oauth2/access_token?appid="
            + c.getStr("appId")
            + "&secret="
            + c.getStr("appSecret")
            + "&code="
            + code
            + "&grant_type=authorization_code";
    JSONObject result = JSONUtil.parseObj(HttpUtil.get(url, 15000));
    String openid = result.getStr("openid");
    if (!StringUtils.hasText(openid))
      throw new IllegalStateException("微信 OpenID 获取失败: " + result.getStr("errmsg"));
    return openid;
  }

  @org.springframework.transaction.annotation.Transactional
  public String bind(Long channelId, String userKey, String openid) {
    if (userKey == null || !userKey.matches("[a-zA-Z0-9_@.:-]{1,100}")
        || openid == null || !openid.matches("[a-zA-Z0-9_-]{1,128}"))
      throw new IllegalArgumentException("绑定标识或 OpenID 无效");
    NoticeChannel channel = channel(channelId);
    Example query = bindingQuery(channel, userKey);
    WechatOpenIdBinding existing = bindingMapper.selectOneByExample(query);
    // A business identifier must not be silently reassigned to another WeChat user.
    if (existing != null) {
      requireBindingAccess(existing);
      if (!openid.equals(existing.getOpenid())) throw new IllegalArgumentException("该绑定标识已绑定其他微信用户");
      existing.setStatus("1"); existing.setUpdateTime(new Date());
      bindingMapper.updateByPrimaryKeySelective(existing);
      return existing.getUserKey();
    }
    Example identity = new Example(WechatOpenIdBinding.class);
    identity.createCriteria().andEqualTo("appId", appId(channel)).andEqualTo("openid", openid)
        .andEqualTo("tenantId", channel.getTenantId());
    WechatOpenIdBinding registered = bindingMapper.selectOneByExample(identity);
    if (registered != null) {
      requireBindingAccess(registered);
      if (userKey.startsWith("scan_") && "1".equals(registered.getStatus())) return registered.getUserKey();
      throw new IllegalArgumentException("该微信用户已绑定，请使用原绑定标识");
    }
    WechatOpenIdBinding binding = new WechatOpenIdBinding();
    binding.setAppId(appId(channel));
    binding.setChannelId(channelId);
    binding.setUserKey(userKey);
    binding.setOpenid(openid);
    binding.setStatus("1");
    binding.setCreateTime(new Date()); binding.setUpdateTime(new Date());
    access.inherit(binding, channel);
    try {
      bindingMapper.insertSelective(binding);
    } catch (org.springframework.dao.DuplicateKeyException e) {
      // The unique (app_id, openid) also prevents another tenant claiming this identity.
      // Do not query or expose the conflicting row, and never overwrite it.
      throw new IllegalArgumentException("该 AppID 下的 OpenID 或绑定标识已存在，请使用原绑定标识", e);
    }
    return binding.getUserKey();
  }

  private void requireBindingAccess(WechatOpenIdBinding binding) {
    NoticeChannel origin = channels.getById(binding.getChannelId());
    if (origin == null || !java.util.Objects.equals(origin.getTenantId(), binding.getTenantId()))
      throw new IllegalArgumentException("无权限访问该微信绑定");
  }

  public String resolve(Long channelId, String userKey) {
    NoticeChannel channel = channel(channelId);
    Example query = bindingQuery(channel, userKey);
    query.getOredCriteria().get(0).andEqualTo("status", "1");
    WechatOpenIdBinding binding = bindingMapper.selectOneByExample(query);
    if (binding == null) return null;
    requireBindingAccess(binding);
    return binding.getOpenid();
  }
}
