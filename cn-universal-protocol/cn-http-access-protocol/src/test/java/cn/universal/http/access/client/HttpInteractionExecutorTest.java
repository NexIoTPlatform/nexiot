package cn.universal.http.access.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.universal.core.network.NetworkRuntimeDefinition;
import cn.universal.core.service.ICodecService;
import cn.universal.http.access.config.HttpConfigParser;
import cn.universal.http.access.config.HttpIntegrationConfig;
import cn.universal.http.access.model.HttpDriverContext;
import cn.universal.http.access.model.HttpRequestSpec;
import cn.universal.http.access.runtime.HttpRouteSnapshot;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class HttpInteractionExecutorTest {
  private final HttpConfigParser parser = new HttpConfigParser();
  private final ICodecService codec = mock(ICodecService.class);
  private final HttpRequestPolicyValidator policy = mock(HttpRequestPolicyValidator.class);
  private final HttpClient client = mock(HttpClient.class);
  private final HttpInteractionExecutor executor =
      new HttpInteractionExecutor(codec, parser, policy, new SimpleMeterRegistry());
  private final HttpDriverContext context = HttpDriverContext.builder().source("DOWNLINK").build();

  @SuppressWarnings("unchecked")
  @Test
  void preservesDriverAuthenticationHeaderAndReturnsResponseToDriver() throws Exception {
    HttpRequestSpec spec = new HttpRequestSpec();
    spec.setUrl("https://vendor.example/devices");
    spec.setHeaders(Map.of("Authorization", "Bearer driver-token"));
    when(codec.iotToYour("product-1", "command", context)).thenReturn(parser.write(spec));
    when(policy.validateAndResolve(any(), any())).thenReturn(URI.create(spec.getUrl()));
    HttpResponse<InputStream> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.headers()).thenReturn(HttpHeaders.of(
        Map.of("Content-Type", List.of("application/json; charset=UTF-8")), (a, b) -> true));
    when(response.body()).thenReturn(new ByteArrayInputStream(
        "{\"temperature\":21}".getBytes(StandardCharsets.UTF_8)));
    when(client.send(any(HttpRequest.class),
        org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
        .thenReturn(response);
    when(codec.yourToIot(eq("product-1"), eq("{\"temperature\":21}"), any()))
        .thenReturn("normalized-device-message");

    HttpInteractionResult result = executor.execute(snapshot(), "product-1", "command", context);

    var outgoing = ArgumentCaptor.forClass(HttpRequest.class);
    verify(client).send(outgoing.capture(),
        org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any());
    assertEquals("Bearer driver-token", outgoing.getValue().headers()
        .firstValue("Authorization").orElseThrow());
    assertEquals("normalized-device-message", result.normalizedPayload());
    var responseContext = ArgumentCaptor.forClass(HttpDriverContext.class);
    verify(codec).yourToIot(eq("product-1"), eq("{\"temperature\":21}"), responseContext.capture());
    assertEquals(200, responseContext.getValue().getAttributes().get("status"));
  }

  @Test
  void emptyDriverRequestFailsBeforeAnyNetworkCall() throws Exception {
    when(codec.iotToYour("product-1", "command", context)).thenReturn("");

    assertThrows(IllegalArgumentException.class,
        () -> executor.execute(snapshot(), "product-1", "command", context));

    verify(policy, never()).validateAndResolve(any(), any());
    verify(client, never()).send(any(HttpRequest.class),
        org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any());
  }

  @Test
  void driverCannotOverrideHostOrInjectHeaders() throws Exception {
    for (Map<String, String> headers : List.of(
        Map.of("Host", "attacker.example"), Map.of("X-Token", "token\r\nInjected: true"))) {
      HttpRequestSpec spec = new HttpRequestSpec();
      spec.setUrl("https://vendor.example/devices");
      spec.setHeaders(headers);
      when(codec.iotToYour("product-1", "command", context)).thenReturn(parser.write(spec));
      when(policy.validateAndResolve(any(), any())).thenReturn(URI.create(spec.getUrl()));

      assertThrows(IllegalArgumentException.class,
          () -> executor.execute(snapshot(), "product-1", "command", context));
    }
    verify(client, never()).send(any(HttpRequest.class),
        org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any());
  }

  private HttpRouteSnapshot snapshot() {
    HttpIntegrationConfig config = new HttpIntegrationConfig();
    config.setAccessCode("factory-east");
    return new HttpRouteSnapshot(new NetworkRuntimeDefinition(
        7L, "HTTP_SERVER", null, "union", "test", "{}", 1), config, client);
  }
}
