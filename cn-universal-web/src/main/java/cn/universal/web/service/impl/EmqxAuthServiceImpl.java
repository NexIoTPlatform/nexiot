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
package cn.universal.web.service.impl;

import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.persistence.base.IoTDeviceLifeCycle;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.web.dto.EmqxAuthRequest;
import cn.universal.web.dto.EmqxAuthResponse;
import cn.universal.web.dto.EmqxWebhookClientDisconnectedRequest;
import cn.universal.web.service.EmqxAuthLogService;
import cn.universal.web.service.EmqxAuthQueryService;
import cn.universal.web.service.EmqxAuthService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * EMQX 认证服务实现类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j
@Service
public class EmqxAuthServiceImpl implements EmqxAuthService {

  private final EmqxAuthQueryService emqxAuthQueryService;
  private final EmqxAuthLogService emqxAuthLogService;
  private final IoTDeviceLifeCycle ioTDeviceLifeCycle;
  private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

  @Value("${mqtt.cfg.client.username:admin}")
  private String adminUsername;

  @Value("${mqtt.cfg.client.password:admin123456}")
  private String adminPassword;

  @Value("${mqtt.cfg.enable:true}")
  private boolean adminEnable;

  @Value("${mqtt.cfg.defined.thing:$thing}")
  private String thingPrefix;

  @Value("${mqtt.device.dynamic-register.enable:true}")
  private boolean dynamicRegisterEnable;

  @Value("${mqtt.device.dynamic-register.timestamp-window-seconds:600}")
  private long dynamicRegisterTimestampWindowSeconds;

  public EmqxAuthServiceImpl(
      EmqxAuthQueryService emqxAuthQueryService,
      EmqxAuthLogService emqxAuthLogService,
      @Qualifier("ioTDeviceActionAfterService") IoTDeviceLifeCycle ioTDeviceLifeCycle) {
    this.emqxAuthQueryService = emqxAuthQueryService;
    this.emqxAuthLogService = emqxAuthLogService;
    this.ioTDeviceLifeCycle = ioTDeviceLifeCycle;
  }

  @Override
  public EmqxAuthResponse authenticate(EmqxAuthRequest request) {
    if (request == null
        || !StringUtils.hasText(request.getUsername())
        || !StringUtils.hasText(request.getPassword())) {
      log.warn(
          "认证请求参数无效: username={}, password={}",
          request != null ? request.getUsername() : "null",
          request != null ? request.getPassword() : "null");
      return new EmqxAuthResponse("deny");
    }

    String username = request.getUsername();
    String password = request.getPassword();
    String clientId = request.getClientid();
    String ipAddress = request.getIp_address();

    log.info("开始处理 EMQX 认证请求: username={}, clientId={}, ip={}", username, clientId, ipAddress);

    EmqxAuthResponse response = null;
    String authType = null;

    try {
      // 1. 尝试配置账号认证
      response = authenticateAdmin(username, password);
      if ("allow".equals(response.getResult())) {
        authType = "ADMIN";
        log.info("配置账号认证成功: username={}, clientId={}", username, clientId);
      }
    } catch (Exception e) {
      log.warn("配置账号认证异常: username={}, error={}", username, e.getMessage(), e);
    }

    // 2. 如果配置账号认证失败，尝试设备三元组认证
    if (response == null || !"allow".equals(response.getResult())) {
      try {
        response = authenticateDevice(username, password, clientId);
        if ("allow".equals(response.getResult())) {
          authType = "DEVICE";
          log.info("设备三元组认证成功: username={}, clientId={}", username, clientId);
        }
      } catch (Exception e) {
        log.warn(
            "设备三元组认证异常: username={}, clientId={}, error={}",
            username,
            clientId,
            e.getMessage(),
            e);
      }
    }

    // 3. 如果设备认证失败，尝试产品认证
    if (response == null || !"allow".equals(response.getResult())) {
      try {
        response = authenticateProduct(username, password);
        if ("allow".equals(response.getResult())) {
          authType = "PRODUCT";
          log.info("产品认证成功: username={}, clientId={}", username, clientId);
        }
      } catch (Exception e) {
        log.warn("产品认证异常: username={}, error={}", username, e.getMessage(), e);
      }
    }

    // 4. 如果产品认证失败，尝试应用认证
    if (response == null || !"allow".equals(response.getResult())) {
      try {
        response = authenticateApplication(username, password);
        if ("allow".equals(response.getResult())) {
          authType = "APPLICATION";
          log.info("应用认证成功: username={}, clientId={}", username, clientId);
        }
      } catch (Exception e) {
        log.warn("应用认证异常: username={}, error={}", username, e.getMessage(), e);
      }
    }

    // 5. 如果应用认证失败，尝试用户认证
    if (response == null || !"allow".equals(response.getResult())) {
      try {
        response = authenticateUser(username, password);
        if ("allow".equals(response.getResult())) {
          authType = "USER";
          log.info("用户认证成功: username={}, clientId={}", username, clientId);
        }
      } catch (Exception e) {
        log.warn("用户认证异常: username={}, error={}", username, e.getMessage(), e);
      }
    }

    // 6. 如果所有认证都失败，返回拒绝
    if (response == null || !"allow".equals(response.getResult())) {
      response = new EmqxAuthResponse("deny");
      log.warn("所有认证方式都失败: username={}, clientId={}", username, clientId);
    }

    // 7. 记录认证日志
    try {
      emqxAuthLogService.logAuthResult(
          username, clientId, ipAddress, authType, response.getResult());
    } catch (Exception e) {
      log.error("记录认证日志失败: username={}, error={}", username, e.getMessage(), e);
    }

    return response;
  }

  /** 设备三元组认证 */
  private EmqxAuthResponse authenticateDevice(String username, String password, String clientId) {
    EmqxDeviceCredentialResolver.DeviceCredential credential =
        EmqxDeviceCredentialResolver.resolve(username, clientId);
    if (credential == null) {
      log.debug("设备三元组格式不匹配: username={}, clientId={}", username, clientId);
      return new EmqxAuthResponse("deny");
    }

    String productKey = credential.getProductKey();
    String deviceId = credential.getDeviceId();
    IoTProduct product = emqxAuthQueryService.queryProductByKey(productKey);
    if (product == null) {
      log.warn("设备所属产品不存在或不可用: productKey={}, deviceId={}", productKey, deviceId);
      return new EmqxAuthResponse("deny");
    }

    IoTDevice device =
        emqxAuthQueryService.queryDeviceByProductKeyAndDeviceId(productKey, deviceId);

    if (device != null
        && StringUtils.hasText(device.getDeviceSecret())
        && password.equals(device.getDeviceSecret())) {
      markDeviceOnline(productKey, deviceId, "DEVICE_SECRET");
      List<EmqxAuthResponse.AclRule> acl = buildDeviceAcl(productKey, deviceId);
      log.info(
          "设备一机一密认证成功: productKey={}, deviceId={}, clientId={}",
          productKey,
          deviceId,
          clientId);
      return new EmqxAuthResponse("allow", false, acl);
    }

    if (verifyDynamicRegisterSignature(product, deviceId, password, clientId)) {
      if (device == null) {
        device = emqxAuthQueryService.createDeviceByDynamicRegister(product, deviceId);
        if (device == null) {
          log.warn("设备动态注册失败: productKey={}, deviceId={}", productKey, deviceId);
          return new EmqxAuthResponse("deny");
        }
        log.info("设备动态注册成功: productKey={}, deviceId={}", productKey, deviceId);
      }
      markDeviceOnline(productKey, deviceId, "DYNAMIC_REGISTER");
      List<EmqxAuthResponse.AclRule> acl = buildDeviceAcl(productKey, deviceId);
      log.info(
          "设备一型一密签名认证成功: productKey={}, deviceId={}, clientId={}",
          productKey,
          deviceId,
          clientId);
      return new EmqxAuthResponse("allow", false, acl);
    }

    if (device == null) {
      log.debug("设备不存在且动态注册签名不通过: productKey={}, deviceId={}", productKey, deviceId);
    } else {
      log.warn("设备密钥验证失败: productKey={}, deviceId={}", productKey, deviceId);
    }
    return new EmqxAuthResponse("deny");
  }

  /** 验证一型一密动态注册签名。 */
  private boolean verifyDynamicRegisterSignature(
      IoTProduct product, String deviceId, String password, String clientId) {
    if (!dynamicRegisterEnable || !isDynamicRegisterEnabled(product)) {
      return false;
    }
    if (!StringUtils.hasText(product.getProductSecret())) {
      return false;
    }

    Map<String, String> params = EmqxDeviceCredentialResolver.parseClientIdParams(clientId);
    String signMethod = params.get("signmethod");
    String timestamp = params.get("timestamp");
    String nonce = params.get("nonce");
    if (!"hmacsha256".equalsIgnoreCase(signMethod)
        || !StringUtils.hasText(timestamp)
        || !StringUtils.hasText(nonce)
        || !isTimestampValid(timestamp)) {
      return false;
    }

    String signContent = product.getProductKey() + deviceId + timestamp + nonce;
    String expected = hmacSha256Hex(product.getProductSecret(), signContent);
    return constantTimeEquals(expected, password);
  }

  private boolean isDynamicRegisterEnabled(IoTProduct product) {
    if (product == null || !StringUtils.hasText(product.getConfiguration())) {
      return false;
    }
    try {
      return JSONUtil.isTypeJSON(product.getConfiguration())
          && JSONUtil.parseObj(product.getConfiguration()).getBool(IoTConstant.ALLOW_INSERT, false);
    } catch (Exception e) {
      log.warn(
          "解析产品动态注册配置失败: productKey={}, error={}",
          product.getProductKey(),
          e.getMessage());
      return false;
    }
  }

  private boolean isTimestampValid(String timestamp) {
    try {
      long time = Long.parseLong(timestamp);
      long timeMillis = timestamp.length() >= 13 ? time : time * 1000;
      long diff = Math.abs(System.currentTimeMillis() - timeMillis);
      return diff <= dynamicRegisterTimestampWindowSeconds * 1000;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private String hmacSha256Hex(String secret, String content) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] digest = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        builder.append(String.format("%02x", b));
      }
      return builder.toString();
    } catch (Exception e) {
      log.warn("计算动态注册签名失败: error={}", e.getMessage(), e);
      return "";
    }
  }

  private boolean constantTimeEquals(String expected, String actual) {
    if (!StringUtils.hasText(expected) || !StringUtils.hasText(actual)) {
      return false;
    }
    byte[] expectedBytes = expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
    byte[] actualBytes = actual.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(expectedBytes, actualBytes);
  }

  private void markDeviceOnline(String productKey, String deviceId, String authMode) {
    try {
      ioTDeviceLifeCycle.online(productKey, deviceId);
    } catch (Exception e) {
      log.warn(
          "设备认证成功但触发上线失败: productKey={}, deviceId={}, authMode={}, error={}",
          productKey,
          deviceId,
          authMode,
          e.getMessage(),
          e);
    }
  }

  /** 配置账号认证 */
  private EmqxAuthResponse authenticateAdmin(String username, String password) {
    // 检查配置账号是否启用
    if (!adminEnable) {
      log.debug("配置账号认证未启用");
      return new EmqxAuthResponse("deny");
    }

    // 验证配置账号用户名和密码
    if (!adminUsername.equals(username) || !adminPassword.equals(password)) {
      log.debug("配置账号认证失败: username={}", username);
      return new EmqxAuthResponse("deny");
    }

    // 构建配置账号权限 ACL
    List<EmqxAuthResponse.AclRule> acl = buildAdminAcl();

    log.info("配置账号认证成功: username={}", username);
    return new EmqxAuthResponse("allow", true, acl);
  }

  @Override
  public EmqxAuthResponse authenticateProduct(String username, String password) {
    log.debug("开始产品认证: username={}", username);

    // 查询产品信息
    IoTProduct product = emqxAuthQueryService.queryProductByKey(username);
    if (product == null) {
      log.debug("产品不存在: username={}", username);
      return new EmqxAuthResponse("deny");
    }

    // 检查产品状态
    if (product.getState() != 0 || product.getIsDeleted() != 0) {
      log.warn(
          "产品状态异常: username={}, state={}, isDeleted={}",
          username,
          product.getState(),
          product.getIsDeleted());
      return new EmqxAuthResponse("deny");
    }

    // 验证产品密钥
    if (!password.equals(product.getProductSecret())) {
      log.warn("产品密钥验证失败: username={}", username);
      return new EmqxAuthResponse("deny");
    }

    // 构建产品权限
    List<EmqxAuthResponse.AclRule> acl = buildProductAcl(username);

    log.info("产品认证成功: username={}, productName={}", username, product.getName());
    return new EmqxAuthResponse("allow", false, acl);
  }

  @Override
  public EmqxAuthResponse authenticateApplication(String username, String password) {
    log.debug("开始应用认证: username={}", username);

    // 查询应用信息
    IoTUserApplication application = emqxAuthQueryService.queryApplicationById(username);
    if (application == null) {
      log.debug("应用不存在: username={}", username);
      return new EmqxAuthResponse("deny");
    }

    // 检查应用状态
    if (application.getAppStatus() != 0 || application.getDeleted() != 0) {
      log.warn(
          "应用状态异常: username={}, appStatus={}, deleted={}",
          username,
          application.getAppStatus(),
          application.getDeleted());
      return new EmqxAuthResponse("deny");
    }

    // 检查应用有效期
    if (application.getValidEndDate() != null
        && System.currentTimeMillis() > application.getValidEndDate().getTime()) {
      log.warn("应用已过期: username={}, validEndDate={}", username, application.getValidEndDate());
      return new EmqxAuthResponse("deny");
    }

    // 检查MQTT是否启用
    if (!(IoTConstant.ONE_TRUE==application.getMqttEnable())) {
      log.warn("应用MQTT未启用: username={}, mqttEnable={}", username, application.getMqttEnable());
      return new EmqxAuthResponse("deny");
    }

    // 验证应用密钥
    if (!password.equals(application.getAppSecret())) {
      log.warn("应用密钥验证失败: username={}", username);
      return new EmqxAuthResponse("deny");
    }

    // 构建应用权限
    List<EmqxAuthResponse.AclRule> acl = buildApplicationAcl(username);

    log.info("应用认证成功: username={}, appName={}", username, application.getAppName());
    return new EmqxAuthResponse("allow", false, acl);
  }

  @Override
  public EmqxAuthResponse authenticateUser(String username, String password) {
    log.debug("开始用户认证: username={}", username);

    // 查询用户信息
    IoTUser user = emqxAuthQueryService.queryUserByUsername(username);
    if (user == null) {
      log.debug("用户不存在: username={}", username);
      return new EmqxAuthResponse("deny");
    }

    // 验证用户密码
    if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
      log.warn("用户密码验证失败: username={}", username);
      return new EmqxAuthResponse("deny");
    }

    // 查询用户关联的应用
    List<IoTUserApplication> applications =
        emqxAuthQueryService.queryApplicationsByUnionId(username);
    if (applications == null || applications.isEmpty()) {
      log.warn("用户未关联任何应用: username={}", username);
      return new EmqxAuthResponse("deny");
    }

    // 构建用户权限
    List<EmqxAuthResponse.AclRule> acl = buildUserAcl(applications);

    log.info("用户认证成功: username={}, 关联应用数量={}", username, applications.size());
    return new EmqxAuthResponse("allow", false, acl);
  }

  @Override
  public void handleClientDisconnected(EmqxWebhookClientDisconnectedRequest request) {
    if (request == null) {
      log.warn("EMQX 离线 webhook 请求为空");
      return;
    }

    String event = request.getEvent();
    if (StringUtils.hasText(event) && !"client.disconnected".equalsIgnoreCase(event)) {
      log.debug("忽略非离线事件: event={}", event);
      return;
    }

    EmqxDeviceCredentialResolver.DeviceCredential credential =
        EmqxDeviceCredentialResolver.resolve(request.getUsername(), request.getClientid());
    if (credential == null) {
      log.warn(
          "无法解析 EMQX 离线设备标识: username={}, clientId={}, reason={}",
          request.getUsername(),
          request.getClientid(),
          request.getReason());
      return;
    }

    String productKey = credential.getProductKey();
    String deviceId = credential.getDeviceId();
    IoTDevice device = emqxAuthQueryService.queryDeviceByProductKeyAndDeviceId(productKey, deviceId);
    if (device == null) {
      log.warn(
          "EMQX 离线事件对应设备不存在: productKey={}, deviceId={}, reason={}",
          productKey,
          deviceId,
          request.getReason());
      return;
    }

    Long connectedAtSeconds =
        request.getConnectedAt() == null ? null : request.getConnectedAt() / 1000L;
    if (connectedAtSeconds != null
        && device.getOnlineTime() != null
        && !connectedAtSeconds.equals(device.getOnlineTime())) {
      log.info(
          "忽略过期的 EMQX 离线事件: productKey={}, deviceId={}, connectedAt={}, currentOnlineTime={}, reason={}",
          productKey,
          deviceId,
          request.getConnectedAt(),
          device.getOnlineTime(),
          request.getReason());
      return;
    }

    try {
      ioTDeviceLifeCycle.offline(productKey, deviceId);
      log.info(
          "EMQX 离线事件已处理: productKey={}, deviceId={}, disconnectedAt={}, reason={}",
          productKey,
          deviceId,
          request.getDisconnectedAt(),
          request.getReason());
    } catch (Exception e) {
      log.warn(
          "设备离线处理失败: productKey={}, deviceId={}, disconnectedAt={}, reason={}, error={}",
          productKey,
          deviceId,
          request.getDisconnectedAt(),
          request.getReason(),
          e.getMessage(),
          e);
    }
  }

  /** 构建配置账号权限 ACL */
  private List<EmqxAuthResponse.AclRule> buildAdminAcl() {
    List<EmqxAuthResponse.AclRule> acl = new ArrayList<>();

    // 订阅权限：$thing/# 和 $ota/#
    acl.add(new EmqxAuthResponse.AclRule(thingPrefix + "/#", "allow", "subscribe"));
    acl.add(new EmqxAuthResponse.AclRule("$ota/#", "allow", "subscribe"));

    // 发布权限：$thing/down/#
    acl.add(new EmqxAuthResponse.AclRule(thingPrefix + "/down/#", "allow", "publish"));

    return acl;
  }

  /** 构建产品权限 ACL */
  private List<EmqxAuthResponse.AclRule> buildProductAcl(String productKey) {
    List<EmqxAuthResponse.AclRule> acl = new ArrayList<>();

    // 产品只能发布自己的产品信息（$thing/up/property/${productKey}/# 物模型格式）
    addAllowAcl(acl, thingPrefix + "/up/property/" + productKey + "/#", "publish");
    addAllowAcl(acl, thingPrefix + "/up/event/" + productKey + "/#", "publish");

    // 产品只能发布自己的产品信息（$thing/up/${productKey}/# 透传格式）
    addAllowAcl(acl, thingPrefix + "/up/" + productKey + "/#", "publish");

    // 产品只能订阅下行消息（$thing/down/${productKey}/# 格式）
    addAllowAcl(acl, thingPrefix + "/down/" + productKey + "/#", "subscribe");

    // 产品只能发布OTA更新消息（$ota/update/${productKey}/# 格式）
    addAllowAcl(acl, "$ota/update/" + productKey + "/#", "publish");

    appendDenyFallbackAcl(acl);
    return acl;
  }

  /** 构建设备权限 ACL */
  private List<EmqxAuthResponse.AclRule> buildDeviceAcl(String productKey, String deviceId) {
    List<EmqxAuthResponse.AclRule> acl = new ArrayList<>();

    // 设备只能发布自己的上行消息。
    addAllowAcl(
        acl, thingPrefix + "/up/property/" + productKey + "/" + deviceId + "/#", "publish");
    addAllowAcl(
        acl, thingPrefix + "/up/event/" + productKey + "/" + deviceId + "/#", "publish");
    addAllowAcl(acl, thingPrefix + "/up/" + productKey + "/" + deviceId + "/#", "publish");
    addAllowAcl(acl, "$ota/report/" + productKey + "/" + deviceId + "/#", "publish");

    // 设备只能订阅自己的下行消息。
    addAllowAcl(acl, thingPrefix + "/down/" + productKey + "/" + deviceId + "/#", "subscribe");
    addAllowAcl(acl, "$ota/update/" + productKey + "/" + deviceId + "/#", "subscribe");

    appendDenyFallbackAcl(acl);
    return acl;
  }

  /** 构建应用权限 ACL */
  private List<EmqxAuthResponse.AclRule> buildApplicationAcl(String appId) {
    List<EmqxAuthResponse.AclRule> acl = new ArrayList<>();

    // 应用只能订阅自己的主题（$thing/${app_id} 格式）
    addSubscribeAllowAcl(acl, thingPrefix + "/" + appId + "/#");

    appendDenyFallbackAcl(acl);

    return acl;
  }

  /** 构建用户权限 ACL */
  private List<EmqxAuthResponse.AclRule> buildUserAcl(List<IoTUserApplication> applications) {
    List<EmqxAuthResponse.AclRule> acl = new ArrayList<>();

    // 用户只能订阅关联应用的主题（$thing/${app_id} 格式）
    for (IoTUserApplication app : applications) {
      String appId = app.getAppId();
      if (appId != null) {
        addSubscribeAllowAcl(acl, thingPrefix + "/" + appId + "/#");
      }
    }

    appendDenyFallbackAcl(acl);

    return acl;
  }

  private void addSubscribeAllowAcl(List<EmqxAuthResponse.AclRule> acl, String topicFilter) {
    addAllowAcl(acl, topicFilter, "subscribe");
  }

  private void addAllowAcl(List<EmqxAuthResponse.AclRule> acl, String topicFilter, String action) {
    acl.add(new EmqxAuthResponse.AclRule(topicFilter, "allow", action));
    acl.add(new EmqxAuthResponse.AclRule("eq " + topicFilter, "allow", action));
  }

  private void appendDenyFallbackAcl(List<EmqxAuthResponse.AclRule> acl) {
    // MQTT wildcard filters starting with # do not cover $-prefixed topics.
    acl.add(new EmqxAuthResponse.AclRule(thingPrefix + "/#", "deny", "all"));
    acl.add(new EmqxAuthResponse.AclRule("$ota/#", "deny", "all"));
    acl.add(new EmqxAuthResponse.AclRule("$SYS/#", "deny", "all"));
    acl.add(new EmqxAuthResponse.AclRule("$share/#", "deny", "all"));
    acl.add(new EmqxAuthResponse.AclRule("#", "deny", "all"));
  }
}
