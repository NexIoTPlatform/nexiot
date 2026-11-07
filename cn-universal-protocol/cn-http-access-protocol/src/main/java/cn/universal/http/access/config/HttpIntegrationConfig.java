package cn.universal.http.access.config;

import lombok.Data;

/**
 * Network-owned HTTP access boundary.
 *
 * <p>The HTTP network deliberately does not describe vendor operations, routes, device resolution,
 * or outbound credentials. Those belong to the single product driver bound to this network.
 */
@Data
public class HttpIntegrationConfig {

  public static final int CURRENT_SCHEMA_VERSION = 2;

  private int schemaVersion = CURRENT_SCHEMA_VERSION;
  private long revision = 1;
  private String accessCode;
  private HttpInboundConfig inbound = new HttpInboundConfig();
  private HttpOutboundPolicy outbound = new HttpOutboundPolicy();

  public enum AuthType {
    NONE,
    API_KEY,
    HMAC_SHA256
  }

  @Data
  public static class HttpInboundConfig {
    private HttpInboundAuth auth = new HttpInboundAuth();
    private int maxBodyBytes = 256 * 1024;
    private int rateLimitPerMinute = 600;
  }

  @Data
  public static class HttpInboundAuth {
    private AuthType type = AuthType.API_KEY;
    private String headerName = "X-API-Key";
    private String value;
    private String secret;
    private String signatureHeader = "X-Signature";
    private String timestampHeader = "X-Timestamp";
    private String nonceHeader = "X-Nonce";
    private int clockSkewSeconds = 300;
  }

  /**
   * Outbound policy is a network boundary, not a vendor request definition. The product driver
   * returns the request URL, method, headers and payload through {@code iotToYour}.
   */
  @Data
  public static class HttpOutboundPolicy {
    private boolean allowPrivateNetwork;
    private int connectTimeoutMs = 5000;
    private int requestTimeoutMs = 10000;
    private int maxRequestBytes = 1024 * 1024;
    private int maxResponseBytes = 4 * 1024 * 1024;
  }
}
