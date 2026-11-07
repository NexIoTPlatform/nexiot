/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.security;

import cn.hutool.core.util.StrUtil;
import cn.universal.miniapp.entity.MpApp;
import cn.universal.miniapp.entity.MpUser;
import cn.universal.miniapp.mapper.MpAppMapper;
import cn.universal.miniapp.mapper.MpUserMapper;
import cn.universal.miniapp.mapper.MpUserPermissionMapper;
import cn.universal.miniapp.vo.MpUserInfoVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 小程序Token服务 独立于主系统的Token生成和验证
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@Service
public class MpTokenService {

  /** Token前缀 */
  private static final String TOKEN_PREFIX = "mp:token:";

  /** Token有效期（秒） - 7天 */
  private static final long TOKEN_EXPIRE_SECONDS = 7 * 24 * 60 * 60;

  @Value("${miniapp.token.secret:NexIoT-MiniApp-Secret-Key-2025-Must-Be-At-Least-256-Bits}")
  private String tokenSecret;

  private SecretKey secretKey;

  @Autowired private StringRedisTemplate stringRedisTemplate;

  @Autowired private MpUserMapper mpUserMapper;

  @Autowired private MpUserPermissionMapper mpUserPermissionMapper;

  @Autowired private MpAppMapper mpAppMapper;

  @PostConstruct
  public void init() {
    this.secretKey = Keys.hmacShaKeyFor(tokenSecret.getBytes(StandardCharsets.UTF_8));
  }

  /** 生成Token */
  public String generateToken(MpUser user) {
    Date now = new Date();
    Date expireDate = new Date(now.getTime() + TOKEN_EXPIRE_SECONDS * 1000);

    String token =
        Jwts.builder()
            .subject(user.getId().toString())
            .claim("appId", user.getAppId())
            .claim("openId", user.getOpenId())
            .claim("phone", user.getPhone())
            .issuedAt(now)
            .expiration(expireDate)
            .signWith(secretKey)
            .compact();

    // 存入Redis
    String redisKey = TOKEN_PREFIX + user.getAppId() + ":" + user.getId();
    stringRedisTemplate.opsForValue().set(redisKey, token, TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);

    return token;
  }

  /** 验证并解析Token */
  public MpUserInfoVO validateAndParse(String token) {
    if (StrUtil.isBlank(token)) {
      return null;
    }

    try {
      // 移除Bearer前缀
      if (token.startsWith("Bearer ")) {
        token = token.substring(7);
      }

      Claims claims =
          Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();

      Long userId = Long.valueOf(claims.getSubject());
      String appId = claims.get("appId", String.class);

      // 验证Redis中是否存在
      String redisKey = TOKEN_PREFIX + appId + ":" + userId;
      String cachedToken = stringRedisTemplate.opsForValue().get(redisKey);
      if (cachedToken == null) {
        log.warn("Token已失效: userId={}, appId={}", userId, appId);
        return null;
      }

      // 验证App是否有效
      MpApp app = mpAppMapper.selectByAppId(appId);
      if (app == null || app.getStatus() != 1) {
        log.warn("小程序已禁用: appId={}", appId);
        return null;
      }

      // 查询用户权限
      List<String> permissions =
          mpUserPermissionMapper.selectPermissionsByUserAndApp(userId, appId);

      return MpUserInfoVO.builder()
          .userId(userId)
          .appId(appId)
          .phone(claims.get("phone", String.class))
          .permissions(permissions)
          .build();

    } catch (Exception e) {
      log.error("Token解析失败: {}", e.getMessage());
      return null;
    }
  }

  /** 刷新Token */
  public String refreshToken(String oldToken) {
    MpUserInfoVO userInfo = validateAndParse(oldToken);
    if (userInfo == null) {
      return null;
    }

    MpUser user = mpUserMapper.selectByPrimaryKey(userInfo.getUserId());
    if (user == null) {
      return null;
    }

    // 删除旧Token
    invalidateToken(userInfo.getAppId(), userInfo.getUserId());

    // 生成新Token
    return generateToken(user);
  }

  /** 使Token失效 */
  public void invalidateToken(String appId, Long userId) {
    String redisKey = TOKEN_PREFIX + appId + ":" + userId;
    stringRedisTemplate.delete(redisKey);
  }

  /** 获取Token过期时间 */
  public long getTokenExpireTime() {
    return TOKEN_EXPIRE_SECONDS;
  }
}
