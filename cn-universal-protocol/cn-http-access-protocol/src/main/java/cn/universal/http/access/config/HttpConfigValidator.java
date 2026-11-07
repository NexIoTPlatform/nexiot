package cn.universal.http.access.config;

import cn.universal.http.access.config.HttpIntegrationConfig.AuthType;
import cn.universal.http.access.config.HttpIntegrationConfig.HttpInboundAuth;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class HttpConfigValidator {

  private static final Pattern ACCESS_CODE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{2,63}");
  private static final Pattern HEADER_NAME =
      Pattern.compile("[!#$%&'*+.^_`|~0-9A-Za-z-]{1,128}");

  private final HttpAccessProperties properties;

  public HttpConfigValidator(HttpAccessProperties properties) {
    this.properties = properties;
  }

  public void validate(HttpIntegrationConfig config) {
    List<String> errors = new ArrayList<>();
    if (config == null) {
      throw new IllegalArgumentException("HTTP configuration is required");
    }
    if (config.getSchemaVersion() != HttpIntegrationConfig.CURRENT_SCHEMA_VERSION) {
      errors.add("schemaVersion must be " + HttpIntegrationConfig.CURRENT_SCHEMA_VERSION);
    }
    if (config.getRevision() < 1) {
      errors.add("revision must be greater than zero");
    }
    if (config.getAccessCode() == null || !ACCESS_CODE.matcher(config.getAccessCode()).matches()) {
      errors.add("accessCode must match " + ACCESS_CODE.pattern());
    }
    validateInbound(config, errors);
    validateOutbound(config, errors);
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException(String.join("; ", errors));
    }
  }

  private void validateInbound(HttpIntegrationConfig config, List<String> errors) {
    if (config.getInbound() == null) {
      errors.add("inbound is required");
      return;
    }
    if (config.getInbound().getMaxBodyBytes() < 1
        || config.getInbound().getMaxBodyBytes() > properties.getHardMaxInboundBytes()) {
      errors.add("inbound.maxBodyBytes exceeds module limit");
    }
    if (config.getInbound().getRateLimitPerMinute() < 0) {
      errors.add("inbound.rateLimitPerMinute cannot be negative");
    }
    HttpInboundAuth auth = config.getInbound().getAuth();
    if (auth == null || auth.getType() == null) {
      errors.add("inbound.auth is required");
      return;
    }
    if (AuthType.API_KEY.equals(auth.getType())) {
      if (!validHeader(auth.getHeaderName()) || blank(auth.getValue())) {
        errors.add("API key authentication requires headerName and value");
      }
    }
    if (AuthType.HMAC_SHA256.equals(auth.getType())) {
      if (blank(auth.getSecret())
          || !validHeader(auth.getSignatureHeader())
          || !validHeader(auth.getTimestampHeader())
          || !validHeader(auth.getNonceHeader())
          || auth.getClockSkewSeconds() < 1
          || auth.getClockSkewSeconds() > 3600) {
        errors.add("HMAC authentication configuration is invalid");
      }
    }
  }

  private void validateOutbound(HttpIntegrationConfig config, List<String> errors) {
    if (config.getOutbound() == null) {
      errors.add("outbound is required");
      return;
    }
    if (config.getOutbound().getConnectTimeoutMs() < 100
        || config.getOutbound().getRequestTimeoutMs() < 100) {
      errors.add("outbound timeouts must be at least 100ms");
    }
    if (config.getOutbound().getMaxRequestBytes() < 1
        || config.getOutbound().getMaxRequestBytes() > properties.getHardMaxOutboundRequestBytes()) {
      errors.add("outbound.maxRequestBytes exceeds module limit");
    }
    if (config.getOutbound().getMaxResponseBytes() < 1
        || config.getOutbound().getMaxResponseBytes() > properties.getHardMaxOutboundResponseBytes()) {
      errors.add("outbound.maxResponseBytes exceeds module limit");
    }
  }

  private boolean validHeader(String value) {
    return value != null && HEADER_NAME.matcher(value).matches();
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
