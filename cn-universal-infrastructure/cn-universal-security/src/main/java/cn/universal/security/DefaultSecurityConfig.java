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

package cn.universal.security;

import cn.universal.security.health.IpWhitelistFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 默认安全配置类
 *
 * <p>提供IoT平台的安全配置，包括： - Spring Security 6.x 安全过滤器链配置 - 开发环境和生产环境的安全策略 - JWT认证和授权配置 - CORS跨域配置 -
 * IP白名单和访问控制 - OAuth2资源服务器配置
 *
 * <p>支持动态切换开发和生产环境的安全策略， 确保在不同环境下提供适当的安全保护级别。
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/1
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class DefaultSecurityConfig {

  /** 是否启用生产环境安全模式 */
  @Value("${security.production.enabled:false}")
  private boolean productionSecurityEnabled;

  /** 允许访问的IP地址列表（逗号分隔） */
  @Value("${security.allowed.ips:}")
  private String allowedIps;

  /** 允许访问的主机名列表（逗号分隔） */
  @Value("${security.allowed.hosts:}")
  private String allowedHosts;

  /** 是否启用Actuator端点访问 */
  @Value("${security.actuator.enabled:false}")
  private boolean actuatorEnabled;

  /**
   * 密码编码器
   *
   * <p>使用Spring Security推荐的委托密码编码器， 支持多种密码编码算法
   *
   * @return 密码编码器实例
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  /**
   * JWT认证转换器
   *
   * <p>将JWT令牌中的角色信息转换为Spring Security的权限对象
   *
   * @return JWT认证转换器
   */
  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

    // 设置权限转换器
    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          Collection<GrantedAuthority> authorities = new ArrayList<>();
          List<String> roles = jwt.getClaimAsStringList("roles");
          if (roles != null) {
            for (String role : roles) {
              authorities.add(new SimpleGrantedAuthority(role));
            }
          }
          return authorities;
        });

    return converter;
  }

  /**
   * 默认安全过滤器链
   *
   * <p>配置Spring Security的主要安全过滤器链， 根据环境配置不同的安全策略
   *
   * @param http HTTP安全配置对象
   * @return 安全过滤器链
   * @throws Exception 配置异常
   */
  @Bean
  @Order(2)
  public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
    log.info("开始配置安全过滤器链...");
    log.info("配置安全过滤器链，生产环境安全模式: {}", productionSecurityEnabled);

    if (productionSecurityEnabled) {
      log.info("配置生产环境安全...");
      configureProductionSecurity(http);
    } else {
      log.info("配置开发环境安全...");
      configureDevelopmentSecurity(http);
    }

    log.info("安全过滤器链配置完成");
    return http.build();
  }

  /**
   * 生产环境安全配置
   *
   * <p>配置生产环境的严格安全策略，包括： - 禁用CSRF保护（API服务） - 启用CORS跨域支持 - 严格的端点访问控制 - IP白名单过滤 - OAuth2资源服务器配置
   *
   * @param http HTTP安全配置对象
   * @throws Exception 配置异常
   */
  private void configureProductionSecurity(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            authz ->
                authz
                    // FreeSWITCH 目录回调由 SIP 模块校验独立服务密钥，空密钥拒绝访问。
                    .requestMatchers(org.springframework.http.HttpMethod.POST, "/protocol/sip/directory")
                    .permitAll()
                    // OAuth2相关端点
                    .requestMatchers("/oauth2/**", "/oauth/**", "/getCaptchaCode")
                    .permitAll()
                    // Flow 编辑器仅含静态页面；业务 API 仍逐项校验登录和权限。
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/flow-editor/**")
                    .permitAll()
                    // 系统配置公开接口 - 前端启动时无需认证加载配置
                    .requestMatchers("/admin/system/config/public/**")
                    .permitAll()
                    .requestMatchers("/admin/system/tenant/public/**")
                    .permitAll()
                    // 登录扫码仅公开会话接口；绑定和公众号管理继续要求已登录。
                    .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/auth/wechat/options", "/auth/wechat/sessions/*", "/wechat/callback/*",
                        "/ws/wechat-auth")
                    .permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.POST,
                        "/auth/wechat/sessions", "/auth/wechat/sessions/*/ws-ticket",
                        "/wechat/callback/*")
                    .permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                        "/auth/wechat/sessions/*")
                    .permitAll()
                    .requestMatchers(
                        "/admin/system/profile/wechat",
                        "/admin/system/profile/wechat/sessions",
                        "/admin/system/profile/wechat/sessions/*/confirm",
                        "/admin/system/wechat/config",
                        "/admin/system/wechat/users")
                    .authenticated()
                    // 通知开放 API：由应用网关/API token 做业务鉴权，微信 OAuth 回调必须可达
                    .requestMatchers("/open-api/v1/notice/wechat/callback")
                    .permitAll()
                    .requestMatchers("/open-api/v1/notice/**")
                    .authenticated()
                    // WebSocket 握手放行，由握手拦截器自行校验 token
                    .requestMatchers("/ws/**")
                    .permitAll()
                    // Node-RED Edge Agent 与编辑器隧道使用插件内的 token/session 鉴权。
                    .requestMatchers(
                        "/admin/v1/edge/**",
                        "/admin/v1/node-red/tunnel/**",
                        "/admin/v1/video-ai/runtime-nodes/agent/**")
                    .permitAll()
                    // 小程序API - 使用独立鉴权机制(@MpPermission)
                    .requestMatchers("/mp/**")
                    .permitAll()
                    // IoT相关端点 - 不需要权限验证
                    .requestMatchers("/iot/**")
                    .permitAll()
                    // IoT相关端点 - 不需要权限验证
                    .requestMatchers("/magic/**")
                    .permitAll()
                    //
                    .requestMatchers("/dashboard/**")
                    .permitAll()
                    // 视频公开分享页与受控公开视频接口 - 由分享 token 自行校验作用域和有效期
                    .requestMatchers("/api/video/public/**", "/api/video/sdk-events/**", "/api/video/hosting/**", "/video-share/**")
                    .permitAll()
                    .requestMatchers("/emqx/**")
                    .permitAll()
                    .requestMatchers("/ct/aiot/**")
                    .permitAll()
                    // 测试接口
                    .requestMatchers("/test/**")
                    .permitAll()
                    // SCADA API接口 - 使用自身的认证机制
                    .requestMatchers("/scada/**")
                    .permitAll()
                    // SCADA前端静态资源 - 放行前端页面和静态文件
                    .requestMatchers("/scada-ui/**")
                    .permitAll()
                    .requestMatchers("/monitor/**")
                    .permitAll()
                    .requestMatchers("/iot/ezviz/**","/video/hosting/ezviz/callback","/iot/imoulife/**","/iot/dahua-cloud/**","/iot/dahuaCloud/**","/hosting/**")
                    .permitAll()
                    // 静态资源
                    .requestMatchers(
                        "/js/**",
                        "/css/**",
                        "/images/**",
                        "/fonts/**",
                        "/upload/**",
                        "/MP*.txt",
                        "/callback/hik")
                    .permitAll()
                    // 健康检查端点
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    // Actuator端点 - 需要特殊权限
                    .requestMatchers("/actuator/**")
                    .access(this::hasActuatorAccess)
                    // V2 子系统开放接口：JWT 验证后由专用资源门面校验应用、租户和设备绑定。
                    .requestMatchers("/api/openapi/v2/**")
                    .authenticated()
                    // 其他所有请求拒绝
                    .anyRequest()
                    .denyAll())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .httpBasic(basic -> basic.disable())
        .formLogin(form -> form.disable())
        .addFilterBefore(new IpWhitelistFilter(allowedIps), BasicAuthenticationFilter.class);
  }

  /**
   * 开发环境安全配置
   *
   * <p>配置开发环境的宽松安全策略，便于开发和调试
   *
   * @param http HTTP安全配置对象
   * @throws Exception 配置异常
   */
  private void configureDevelopmentSecurity(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            authz ->
                authz
                    .requestMatchers(org.springframework.http.HttpMethod.POST, "/protocol/sip/directory")
                    .permitAll()
                    .requestMatchers(
                        "/oauth2/**",
                        "/oauth/**",
                        "/api/oauth/**",
                        "/getCaptchaCode",
                        "/magic/**",
                        "/dashboard/**")
                    .permitAll()
                    // Flow 编辑器仅含静态页面；业务 API 仍逐项校验登录和权限。
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/flow-editor/**")
                    .permitAll()
                    // 系统配置公开接口 - 前端启动时无需认证加载配置
                    .requestMatchers("/admin/system/config/public/**")
                    .permitAll()
                    .requestMatchers("/admin/system/tenant/public/**")
                    .permitAll()
                    // 登录扫码仅公开会话接口；绑定和公众号管理继续要求已登录。
                    .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/auth/wechat/options", "/auth/wechat/sessions/*", "/wechat/callback/*",
                        "/ws/wechat-auth")
                    .permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.POST,
                        "/auth/wechat/sessions", "/auth/wechat/sessions/*/ws-ticket",
                        "/wechat/callback/*")
                    .permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                        "/auth/wechat/sessions/*")
                    .permitAll()
                    .requestMatchers(
                        "/admin/system/profile/wechat",
                        "/admin/system/profile/wechat/sessions",
                        "/admin/system/profile/wechat/sessions/*/confirm",
                        "/admin/system/wechat/config",
                        "/admin/system/wechat/users")
                    .authenticated()
                    .requestMatchers("/open-api/v1/notice/wechat/callback")
                    .permitAll()
                    .requestMatchers("/open-api/v1/notice/**")
                    .authenticated()
                    // WebSocket 握手放行，由握手拦截器自行校验 token
                    .requestMatchers("/ws/**")
                    .permitAll()
                    // Node-RED Edge Agent 与编辑器隧道使用插件内的 token/session 鉴权。
                    .requestMatchers(
                        "/admin/v1/edge/**",
                        "/admin/v1/node-red/tunnel/**",
                        "/admin/v1/video-ai/runtime-nodes/agent/**")
                    .permitAll()
                    // 小程序API - 使用独立鉴权机制(@MpPermission)
                    .requestMatchers("/mp/**")
                    .permitAll()
                    // IoT相关端点 - 不需要权限验证
                    .requestMatchers("/iot/**", "/emqx/**")
                    .permitAll()
                    // 测试和监控点
                    .requestMatchers("/test/**", "/monitor/**")
                    .permitAll()
                    // 视频公开分享页与受控公开视频接口 - 由分享 token 自行校验作用域和有效期
                    .requestMatchers("/api/video/public/**", "/api/video/sdk-events/**", "/api/video/hosting/**", "/video-share/**")
                    .permitAll()
                    // SCADA认证接口 - 登录不需要验证
                    .requestMatchers("/scada/auth/**")
                    .permitAll()
                    // SCADA API接口 - 使用自身的认证机制
                    .requestMatchers("/scada/**")
                    .permitAll()
                    // SCADA前端静态资源 - 放行前端页面和静态文件
                    .requestMatchers("/scada-ui/**")
                    .permitAll()
                    .requestMatchers("/ct/aiot/**")
                    .permitAll()
                    .requestMatchers("/iot/ezviz/**","/video/hosting/ezviz/callback","/iot/imoulife/**","/iot/dahua-cloud/**","/iot/dahuaCloud/**","/hosting/**")
                    .permitAll()
                    // 前端相关
                    .requestMatchers(
                        "/js/**",
                        "/css/**",
                        "/images/**",
                        "/fonts/**",
                        "/upload/**",
                        "/MP*.txt",
                        "/callback/hik")
                    .permitAll()
                    // spring
                    .requestMatchers("/actuator/**")
                    .permitAll()
                    // 其他环境
                    .requestMatchers(
                        "/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/favicon.ico")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        // 认证服务器
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .httpBasic(basic -> basic.disable())
        .formLogin(form -> form.disable());
  }

  /**
   * 检查IP访问权限
   *
   * <p>验证请求IP是否在允许的IP列表中或为内网IP
   *
   * @param authentication 认证信息提供者
   * @param context 请求授权上下文
   * @return 授权决策
   */
  private AuthorizationDecision hasValidIpAccess(
      Supplier<Authentication> authentication, RequestAuthorizationContext context) {
    HttpServletRequest request = context.getRequest();
    String clientIp = getClientIpAddress(request);
    log.info("检查IP访问权限: {}", clientIp);

    boolean hasAccess = false;
    if (allowedIps == null || allowedIps.trim().isEmpty()) {
      hasAccess = isInternalNetwork(clientIp);
    } else {
      List<String> allowedIpList = Arrays.asList(allowedIps.split(","));
      hasAccess = allowedIpList.contains(clientIp) || isInternalNetwork(clientIp);
    }

    return new AuthorizationDecision(hasAccess);
  }

  /**
   * 检查Actuator访问权限
   *
   * <p>验证是否允许访问Actuator监控端点
   *
   * @param authentication 认证信息提供者
   * @param context 请求授权上下文
   * @return 授权决策
   */
  private AuthorizationDecision hasActuatorAccess(
      Supplier<Authentication> authentication, RequestAuthorizationContext context) {
    if (!actuatorEnabled) {
      log.warn("Actuator访问被禁用");
      return new AuthorizationDecision(false);
    }

    HttpServletRequest request = context.getRequest();
    String clientIp = getClientIpAddress(request);
    log.info("检查Actuator访问权限: {}", clientIp);

    // 只允许内网访问Actuator
    boolean hasAccess = isInternalNetwork(clientIp);
    return new AuthorizationDecision(hasAccess);
  }

  /**
   * 判断是否为内网IP
   *
   * <p>检查IP地址是否属于内网地址段
   *
   * @param ip IP地址
   * @return 是否为内网IP
   */
  private boolean isInternalNetwork(String ip) {
    if (ip == null) {
      return false;
    }

    // 内网IP段
    return ip.startsWith("127.")
        || // 本地回环
        ip.startsWith("10.")
        || // A类内网
        ip.startsWith("172.16.")
        || // B类内网
        ip.startsWith("172.17.")
        || ip.startsWith("172.18.")
        || ip.startsWith("172.19.")
        || ip.startsWith("172.20.")
        || ip.startsWith("172.21.")
        || ip.startsWith("172.22.")
        || ip.startsWith("172.23.")
        || ip.startsWith("172.24.")
        || ip.startsWith("172.25.")
        || ip.startsWith("172.26.")
        || ip.startsWith("172.27.")
        || ip.startsWith("172.28.")
        || ip.startsWith("172.29.")
        || ip.startsWith("172.30.")
        || ip.startsWith("172.31.")
        || ip.startsWith("192.168."); // C类内网
  }

  /** 获取客户端真实IP */
  private String getClientIpAddress(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null
        && !xForwardedFor.isEmpty()
        && !"unknown".equalsIgnoreCase(xForwardedFor)) {
      return xForwardedFor.split(",")[0].trim();
    }

    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
      return xRealIp;
    }

    return request.getRemoteAddr();
  }

  /** CORS配置 */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(Arrays.asList("*"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
