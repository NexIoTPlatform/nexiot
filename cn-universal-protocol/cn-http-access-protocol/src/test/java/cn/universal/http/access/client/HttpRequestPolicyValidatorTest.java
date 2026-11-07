package cn.universal.http.access.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.universal.http.access.config.HttpIntegrationConfig.HttpOutboundPolicy;
import cn.universal.http.access.model.HttpRequestSpec;
import org.junit.jupiter.api.Test;

class HttpRequestPolicyValidatorTest {
  private final HttpRequestPolicyValidator validator = new HttpRequestPolicyValidator();

  @Test
  void rejectsNonHttpSchemeBeforeResolvingHost() {
    HttpRequestSpec request = request("file:///etc/passwd");
    IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
        () -> validator.validateAndResolve(new HttpOutboundPolicy(), request));
    assertTrue(error.getMessage().contains("absolute HTTP(S)"));
  }

  @Test
  void alwaysRejectsLoopbackEvenWhenPrivateNetworksAreAllowed() {
    HttpOutboundPolicy policy = new HttpOutboundPolicy();
    policy.setAllowPrivateNetwork(true);
    IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
        () -> validator.validateAndResolve(policy, request("http://127.0.0.1:9092/admin/network")));
    assertTrue(error.getMessage().contains("blocked by network policy"));
  }

  @Test
  void privateAddressRequiresExplicitNetworkPermission() {
    HttpOutboundPolicy policy = new HttpOutboundPolicy();
    HttpRequestSpec request = request("http://192.168.1.10/telemetry");
    assertThrows(IllegalArgumentException.class, () -> validator.validateAndResolve(policy, request));
    policy.setAllowPrivateNetwork(true);
    assertDoesNotThrow(() -> validator.validateAndResolve(policy, request));
  }

  @Test
  void rejectsUserInfoUrls() {
    assertThrows(IllegalArgumentException.class,
        () -> validator.validateAndResolve(new HttpOutboundPolicy(),
            request("https://user:password@example.com/devices")));
  }

  private HttpRequestSpec request(String url) {
    HttpRequestSpec request = new HttpRequestSpec();
    request.setUrl(url);
    return request;
  }
}
