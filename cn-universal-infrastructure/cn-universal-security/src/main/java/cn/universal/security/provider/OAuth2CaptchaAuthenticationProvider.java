/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoTXin 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoTXin
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.security.provider;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.Constants;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.utils.RSAUtils;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.security.support.TenantLoginResolver;
import cn.universal.security.service.AdminLogService;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.token.OAuth2CaptchaAuthenticationToken;
import java.security.Principal;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import cn.universal.common.utils.ServletUtils;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OAuth2CaptchaAuthenticationProvider implements AuthenticationProvider {

  @Autowired private StringRedisTemplate stringRedisTemplate;
  @Autowired private UserDetailsService userDetailsService;

  @Autowired private AdminLogService adminLogService;
  @Autowired private IoTUserService ioTUserService;
  @Autowired private TenantLoginResolver tenantLoginResolver;
  @Autowired private OAuth2AuthorizationService authorizationService;
  @Autowired private OAuth2TokenGenerator<?> tokenGenerator;

  private static final Integer MAX_RETRY_COUNT = 5;
  private static final Integer MAX_IP_RETRY_COUNT = 6;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    OAuth2CaptchaAuthenticationToken captchaToken =
        (OAuth2CaptchaAuthenticationToken) authentication;

    // 获取客户端信息
    OAuth2ClientAuthenticationToken clientPrincipal =
        (OAuth2ClientAuthenticationToken) captchaToken.getPrincipal();
    RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

    String username = captchaToken.getUsername();
    String password = captchaToken.getPassword();
    String captcha = captchaToken.getCode();
    String uuid = captchaToken.getUuid();
    String tenantCode = captchaToken.getTenantCode();
    String loginAccountKey = StrUtil.isBlank(tenantCode) ? username : tenantCode + ":" + username;

    // 获取IP地址（支持Nginx等反向代理，从X-Forwarded-For等请求头读取真实IP）
    String ip = "unknown";
    try {
      ip = ServletUtils.getClientIP();
    } catch (Exception e) {
      Object details = authentication.getDetails();
      if (details instanceof WebAuthenticationDetails) {
        ip = ((WebAuthenticationDetails) details).getRemoteAddress();
      }
    }

    // 校验验证码
    if (!StringUtils.hasText(captcha)) {
      throw new OAuth2AuthenticationException(new OAuth2Error("code empty", "验证码不能为空", null));
    }
    String code = stringRedisTemplate.opsForValue().get("captcha_codes:" + uuid);
    if (!StringUtils.hasText(code)) {
      throw new OAuth2AuthenticationException(new OAuth2Error("invalid code", "验证码已过期", null));
    }
    if (!code.equalsIgnoreCase(captcha)) {
      throw new OAuth2AuthenticationException(new OAuth2Error("invalid code", "验证码输入错误", null));
    }

    // 登录重试次数/IP 限制
    String retryKey = "loginCheck:retryCount:" + loginAccountKey;
    String ipKey = "loginCheck:retryIpCount:" + ip;
    Integer retryCount =
        stringRedisTemplate.opsForValue().get(retryKey) != null
            ? Integer.parseInt(stringRedisTemplate.opsForValue().get(retryKey))
            : 0;
    Integer ipCount =
        stringRedisTemplate.opsForValue().get(ipKey) != null
            ? Integer.parseInt(stringRedisTemplate.opsForValue().get(ipKey))
            : 0;

    if (ipCount > MAX_IP_RETRY_COUNT) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("invalid_grant", "IP已锁定，请一小时后再试", null));
    }
    if (retryCount > MAX_RETRY_COUNT) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("invalid_grant", "账号已锁定，请15分钟后再试", null));
    }

    try {
      String privateKey = stringRedisTemplate.opsForValue().get("RSAPrivateKey");
      if (privateKey != null) {
        password = RSAUtils.decrypt(password, privateKey);
      }
    } catch (Exception e) {
      stringRedisTemplate
          .opsForValue()
          .set(retryKey, String.valueOf(retryCount + 1), 10, TimeUnit.MINUTES);
      stringRedisTemplate
          .opsForValue()
          .set(ipKey, String.valueOf(ipCount + 1), 60, TimeUnit.MINUTES);
      throw new OAuth2AuthenticationException(new OAuth2Error("invalid_grant", "密钥已过期", null));
    }

    Matcher matcher = IoTConstant.pattern.matcher(password);
    if (!matcher.matches()) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("invalid_grant", "密码中必须包含字母、数字、特殊字符，至少8个字符，最多30个字符", null));
    }

    // 独占式登录判断
    IoTUser user = tenantLoginResolver.resolveLoginUser(username, tenantCode);
    if (ObjectUtil.isEmpty(user)) {
      stringRedisTemplate
          .opsForValue()
          .set(ipKey, String.valueOf(ipCount + 1), 60, TimeUnit.MINUTES);
      throw new OAuth2AuthenticationException(new OAuth2Error("invalid_grant", "用户名或密码错误", null));
    }
    if (IoTConstant.UN_NORMAL.equals(user.getStatus())) {
      throw new OAuth2AuthenticationException(new OAuth2Error("invalid_grant", "用户已冻结", null));
    }

    boolean isExclusive =
        "true".equals(JSONUtil.parseObj(user.getCfg()).getStr(IoTConstant.EXCLUSIVE_FIRST_LOGIN));
    if (isExclusive) {
      String loginedIp =
          stringRedisTemplate
              .opsForValue()
              .get(IoTConstant.EXCLUSIVE_LOGIN + ":" + user.getUnionId());
      if (StrUtil.isNotBlank(loginedIp)) {
        throw new OAuth2AuthenticationException(
            new OAuth2Error("invalid_grant", "账号已登录，请先退出已登录的账号,IP：" + loginedIp, null));
      }
    }

    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUnionId());
    // 注释密码验证，专注解决类型转换问题
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    if (!passwordEncoder.matches(password, userDetails.getPassword())) {
      adminLogService.recordLogininfor(
          user.getUnionId(), Constants.LOGIN_FAIL, "用户名或密码错误", null);
      stringRedisTemplate
          .opsForValue()
          .set(retryKey, String.valueOf(retryCount + 1), 10, TimeUnit.MINUTES);
      stringRedisTemplate
          .opsForValue()
          .set(ipKey, String.valueOf(ipCount + 1), 60, TimeUnit.MINUTES);
      throw new OAuth2AuthenticationException(new OAuth2Error("invalid_grant", "用户名或密码错误", null));
    }

    // 登录成功
    adminLogService.recordLogininfor(
        user.getUnionId(), Constants.LOGIN_SUCCESS, "验证码登录成功", null);
    stringRedisTemplate.delete(retryKey);
    stringRedisTemplate.delete(ipKey);

    if (isExclusive) {
      stringRedisTemplate
          .opsForValue()
          .set(IoTConstant.EXCLUSIVE_LOGIN + ":" + user.getUnionId(), ip, 1800, TimeUnit.SECONDS);
    }

    // 生成 OAuth2 令牌
    UsernamePasswordAuthenticationToken userAuth =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    DefaultOAuth2TokenContext.Builder tokenContextBuilder =
        DefaultOAuth2TokenContext.builder()
            .registeredClient(registeredClient)
            .principal(userAuth)
            .authorizationServerContext(AuthorizationServerContextHolder.getContext())
            .authorizationGrantType(OAuth2CaptchaAuthenticationToken.CAPTCHA_GRANT_TYPE)
            .authorizationGrant(captchaToken);

    OAuth2TokenContext tokenContext =
        tokenContextBuilder.tokenType(OAuth2TokenType.ACCESS_TOKEN).build();
    OAuth2Token generatedAccessToken = tokenGenerator.generate(tokenContext);
    if (generatedAccessToken == null) {
      throw new OAuth2AuthenticationException(new OAuth2Error("invalid_grant", "请联系管理员", null));
    }

    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            generatedAccessToken.getTokenValue(),
            generatedAccessToken.getIssuedAt(),
            generatedAccessToken.getExpiresAt(),
            registeredClient.getScopes());

    OAuth2RefreshToken refreshToken = null;
    if (registeredClient
        .getAuthorizationGrantTypes()
        .contains(org.springframework.security.oauth2.core.AuthorizationGrantType.REFRESH_TOKEN)) {
      tokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.REFRESH_TOKEN).build();
      OAuth2Token generatedRefreshToken = tokenGenerator.generate(tokenContext);
      if (generatedRefreshToken != null) {
        refreshToken =
            new OAuth2RefreshToken(
                generatedRefreshToken.getTokenValue(),
                generatedRefreshToken.getIssuedAt(),
                generatedRefreshToken.getExpiresAt());
      }
    }

    // 保存授权信息
    OAuth2Authorization.Builder authorizationBuilder =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName(userDetails.getUsername())
            .attribute(Principal.class.getName(), userAuth)
            .authorizationGrantType(OAuth2CaptchaAuthenticationToken.CAPTCHA_GRANT_TYPE)
            .authorizedScopes(registeredClient.getScopes());

    if (accessToken != null) {
      authorizationBuilder.accessToken(accessToken);
    }
    if (refreshToken != null) {
      authorizationBuilder.refreshToken(refreshToken);
    }

    OAuth2Authorization authorization = authorizationBuilder.build();
    authorizationService.save(authorization);

    return new OAuth2AccessTokenAuthenticationToken(
        registeredClient, clientPrincipal, accessToken, refreshToken);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return OAuth2CaptchaAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
