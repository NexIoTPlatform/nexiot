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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

/** EMQX 设备三元组连接参数解析工具。 */
final class EmqxDeviceCredentialResolver {

  private EmqxDeviceCredentialResolver() {}

  static DeviceCredential resolve(String username, String clientId) {
    String normalizedUsername = trimToNull(username);
    String normalizedClientId = trimToNull(stripClientIdParams(clientId));

    DeviceCredential credential = parseUsername(normalizedUsername);
    if (credential != null) {
      DeviceCredential clientCredential = parseProductDevice(normalizedClientId);
      if (clientCredential != null && !credential.matches(clientCredential)) {
        return null;
      }
      return credential;
    }

    DeviceCredential clientCredential = parseProductDevice(normalizedClientId);
    if (clientCredential != null) {
      return clientCredential;
    }

    if (StringUtils.hasText(normalizedUsername) && StringUtils.hasText(normalizedClientId)) {
      return new DeviceCredential(normalizedUsername, normalizedClientId);
    }
    return null;
  }

  static String buildClientId(String productKey, String deviceId) {
    return productKey + "." + deviceId;
  }

  static Map<String, String> parseClientIdParams(String clientId) {
    String normalized = trimToNull(clientId);
    if (normalized == null) {
      return Collections.emptyMap();
    }
    int paramIndex = normalized.indexOf('|');
    if (paramIndex < 0 || paramIndex >= normalized.length() - 1) {
      return Collections.emptyMap();
    }

    Map<String, String> params = new HashMap<>();
    String paramText = normalized.substring(paramIndex + 1);
    String[] pairs = paramText.split("[,&]");
    for (String pair : pairs) {
      int equalsIndex = pair.indexOf('=');
      if (equalsIndex <= 0 || equalsIndex >= pair.length() - 1) {
        continue;
      }
      String key = pair.substring(0, equalsIndex).trim().toLowerCase();
      String value = pair.substring(equalsIndex + 1).trim();
      if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
        params.put(key, value);
      }
    }
    return params;
  }

  private static DeviceCredential parseUsername(String username) {
    if (!StringUtils.hasText(username)) {
      return null;
    }

    int aliStyleIndex = username.indexOf('&');
    if (aliStyleIndex > 0 && aliStyleIndex < username.length() - 1) {
      String deviceId = username.substring(0, aliStyleIndex);
      String productKey = username.substring(aliStyleIndex + 1);
      return new DeviceCredential(productKey, deviceId);
    }

    return parseProductDevice(username);
  }

  private static DeviceCredential parseProductDevice(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String normalized = stripClientIdParams(value);
    if (!StringUtils.hasText(normalized)) {
      return null;
    }
    String[] separators = {"/", ":", "."};
    for (String separator : separators) {
      int index = normalized.indexOf(separator);
      if (index > 0 && index < normalized.length() - separator.length()) {
        String productKey = normalized.substring(0, index);
        String deviceId = normalized.substring(index + separator.length());
        if (StringUtils.hasText(productKey) && StringUtils.hasText(deviceId)) {
          return new DeviceCredential(productKey, deviceId);
        }
      }
    }
    return null;
  }

  private static String stripClientIdParams(String clientId) {
    String normalized = trimToNull(clientId);
    if (normalized == null) {
      return null;
    }
    int paramIndex = normalized.indexOf('|');
    if (paramIndex < 0) {
      return normalized;
    }
    return trimToNull(normalized.substring(0, paramIndex));
  }

  private static String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return HtmlUtils.htmlUnescape(value.trim());
  }

  static final class DeviceCredential {
    private final String productKey;
    private final String deviceId;

    private DeviceCredential(String productKey, String deviceId) {
      this.productKey = productKey;
      this.deviceId = deviceId;
    }

    String getProductKey() {
      return productKey;
    }

    String getDeviceId() {
      return deviceId;
    }

    boolean matches(DeviceCredential other) {
      return other != null
          && productKey.equals(other.productKey)
          && deviceId.equals(other.deviceId);
    }
  }
}
