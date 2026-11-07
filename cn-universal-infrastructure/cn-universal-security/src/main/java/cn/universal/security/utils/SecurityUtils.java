/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.security.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import cn.universal.common.exception.BaseException;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.security.service.IoTUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 安全服务工具类 */
@Component
public class SecurityUtils {

  private static ApplicationContext applicationContext;
  private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);

  @Autowired
  public void setApplicationContext(ApplicationContext applicationContext) {
    SecurityUtils.applicationContext = applicationContext;
  }

  /** 获取unionId用户 */
  public static String getUnionId() {
    try {
      Jwt jwt = getCurrentJwt();
      if (jwt == null) {
        throw new BaseException("当前未获取到认证信息", HttpStatus.HTTP_UNAUTHORIZED + "");
      }
      String unionId = resolveUnionId(jwt);
      String currentJti = jwt.getId();
      log.debug("oauth2 subject={}, currentJti={}", unionId, currentJti);
      if (StringUtils.hasText(unionId)) {
        return unionId;
      }
      throw new BaseException("无法获取用户信息", HttpStatus.HTTP_UNAUTHORIZED + "");
    } catch (BaseException e) {
      throw e;
    } catch (Exception e) {
      log.error("无法获取用户信息", e);
      throw new IoTException("获取用户信息异常", HttpStatus.HTTP_UNAUTHORIZED);
    }
  }

  /** 安静地获取 unionId，未登录或无上下文时返回 null */
  public static String getUnionIdQuietly() {
    try {
      return resolveUnionId(getCurrentJwt());
    } catch (Exception e) {
      log.debug("当前线程无可用认证上下文，返回空 unionId", e);
    }
    return null;
  }

  /**
   * 尝试从当前 JWT 中获取租户 ID，认证上下文或 tenantId claim 不存在时返回 {@code null}。
   *
   * <p>历史令牌及部分自定义授权流程可能只包含用户标识而没有 tenantId，因此这里不能把缺少 claim
   * 直接视为未认证。需要租户 ID 的业务应优先使用本方法，并在返回空值时根据当前 unionId 查询用户表，
   * 再以用户 tenantId（历史账号可回退 unionId）补全。涉及请求参数指定租户时，还必须校验当前用户是否有权
   * 访问该租户，不能直接信任请求值。
   */
  public static String getTenantIdQuietly() {
    return readCurrentClaimQuietly("tenantId");
  }

  /** 获取租户编码 */
  public static String getTenantCode() {
    String tenantCode = getTenantCodeQuietly();
    if (StringUtils.hasText(tenantCode)) {
      return tenantCode;
    }
    throw new IoTException("获取租户编码异常", HttpStatus.HTTP_UNAUTHORIZED);
  }

  /** 安静地获取租户编码 */
  public static String getTenantCodeQuietly() {
    String tenantCode = readCurrentClaimQuietly("tenantCode");
    return StringUtils.hasText(tenantCode) ? tenantCode : getTenantIdQuietly();
  }

  /** 获取租户角色 */
  public static String getTenantRole() {
    String tenantRole = getTenantRoleQuietly();
    if (StringUtils.hasText(tenantRole)) {
      return tenantRole;
    }
    throw new IoTException("获取租户角色异常", HttpStatus.HTTP_UNAUTHORIZED);
  }

  /** 安静地获取租户角色 */
  public static String getTenantRoleQuietly() {
    return readCurrentClaimQuietly("tenantRole");
  }

  /** 获取当前认证用户信息（从 JWT 中获取） */
  public static IoTUser getIoTUnionUser() {
    try {
      String unionId = resolveUnionId(getCurrentJwt());
      if (StringUtils.hasText(unionId)) {
        return getIotUserByUnionId(unionId);
      }
    } catch (Exception e) {
      log.error("获取当前用户信息失败", e);
    }
    return null;
  }

  /** 根据用户名获取 IoTUser */
  private static IoTUser getIotUserByUsername(String username) {
    if (StrUtil.isBlank(username)) {
      throw new BaseException("获取用户信息异常", HttpStatus.HTTP_UNAUTHORIZED + "");
    }
    try {
      IoTUserService ioTUserService = applicationContext.getBean(IoTUserService.class);
      return ioTUserService.selectUserByUserName(username);
    } catch (Exception e) {
      throw new BaseException("获取用户信息异常", HttpStatus.HTTP_UNAUTHORIZED + "");
    }
  }

  /** 根据 unionId 获取 IoTUser */
  private static IoTUser getIotUserByUnionId(String unionId) {
    if (StrUtil.isBlank(unionId)) {
      throw new BaseException("获取用户信息异常", HttpStatus.HTTP_UNAUTHORIZED + "");
    }
    try {
      IoTUserService ioTUserService = applicationContext.getBean(IoTUserService.class);
      return ioTUserService.selectUserByUnionId(unionId);
    } catch (Exception e) {
      throw new BaseException("获取用户信息异常", HttpStatus.HTTP_UNAUTHORIZED + "");
    }
  }

  /** 获取Authentication */
  public static Authentication getAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }

  private static Jwt getCurrentJwt() {
    Authentication authentication = getAuthentication();
    if (authentication == null) {
      return null;
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof Jwt jwt) {
      return jwt;
    }
    return null;
  }

  private static String resolveUnionId(Jwt jwt) {
    if (jwt == null) {
      return null;
    }
    String subject = jwt.getSubject();
    if (StringUtils.hasText(subject)) {
      return subject;
    }
    return readClaim(jwt, "unionId");
  }

  private static String readCurrentClaimQuietly(String claimName) {
    return readClaim(getCurrentJwt(), claimName);
  }

  private static String readClaim(Jwt jwt, String claimName) {
    if (jwt == null || !StringUtils.hasText(claimName)) {
      return null;
    }
    String claimValue = jwt.getClaimAsString(claimName);
    if (StringUtils.hasText(claimValue)) {
      return claimValue;
    }
    Object rawValue = jwt.getClaims().get(claimName);
    if (rawValue == null) {
      return null;
    }
    String text = String.valueOf(rawValue);
    return StringUtils.hasText(text) ? text : null;
  }

  /**
   * 生成BCryptPasswordEncoder密码
   *
   * @param password 密码
   * @return 加密字符串
   */
  public static String encryptPassword(String password) {
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    return passwordEncoder.encode(password);
  }

  /**
   * 判断密码是否相同
   *
   * @param realPwd 真实密码
   * @param hexPwd 加密后字符
   */
  public static boolean matchesPassword(String realPwd, String hexPwd) {
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    return passwordEncoder.matches(realPwd, hexPwd);
  }
}
