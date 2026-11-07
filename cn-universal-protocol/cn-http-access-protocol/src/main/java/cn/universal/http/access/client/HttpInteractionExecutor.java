package cn.universal.http.access.client;

import cn.universal.core.service.ICodecService;
import cn.universal.http.access.config.HttpConfigParser;
import cn.universal.http.access.model.HttpClientResponse;
import cn.universal.http.access.model.HttpDriverContext;
import cn.universal.http.access.model.HttpRequestSpec;
import cn.universal.http.access.runtime.HttpRouteSnapshot;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Executes only the request specification returned by the bound product driver. Network settings
 * provide security limits; they never contain vendor endpoints or operation definitions.
 */
@Component
public class HttpInteractionExecutor {

  private static final Set<String> BLOCKED_HEADERS = Set.of(
      "host", "content-length", "connection", "keep-alive", "proxy-authenticate",
      "proxy-authorization", "te", "trailer", "transfer-encoding", "upgrade");

  private final ICodecService codecService;
  private final HttpConfigParser parser;
  private final HttpRequestPolicyValidator policyValidator;
  private final MeterRegistry meterRegistry;

  public HttpInteractionExecutor(
      ICodecService codecService,
      HttpConfigParser parser,
      HttpRequestPolicyValidator policyValidator,
      MeterRegistry meterRegistry) {
    this.codecService = codecService;
    this.parser = parser;
    this.policyValidator = policyValidator;
    this.meterRegistry = meterRegistry;
  }

  public HttpInteractionResult execute(
      HttpRouteSnapshot snapshot, String productKey, String payload, HttpDriverContext context) {
    Timer.Sample sample = Timer.start(meterRegistry);
    String outcome = "success";
    try {
      String requestJson = codecService.iotToYour(productKey, payload, context);
      if (requestJson == null || requestJson.isBlank()) {
        throw new IllegalArgumentException("Product driver iotToYour returned an empty HttpRequestSpec");
      }
      HttpRequestSpec spec = parser.mapper().readValue(requestJson, HttpRequestSpec.class);
      HttpClientResponse response = send(snapshot, spec);
      HttpDriverContext responseContext = copyContext(context, response);
      String normalized = codecService.yourToIot(productKey, response.body(), responseContext);
      return new HttpInteractionResult(response, normalized);
    } catch (Exception e) {
      outcome = "failure";
      throw e instanceof RuntimeException runtime ? runtime : new IllegalStateException(e);
    } finally {
      sample.stop(Timer.builder("nexiot.http.access.interaction")
          .tag("network", Long.toString(snapshot.definition().id()))
          .tag("source", context.getSource() == null ? "UNKNOWN" : context.getSource())
          .tag("outcome", outcome)
          .register(meterRegistry));
    }
  }

  private HttpClientResponse send(HttpRouteSnapshot snapshot, HttpRequestSpec spec) throws Exception {
    URI uri = policyValidator.validateAndResolve(snapshot.config().getOutbound(), spec);
    byte[] body = spec.getBody() == null ? new byte[0] : spec.getBody().getBytes(StandardCharsets.UTF_8);
    if (body.length > snapshot.config().getOutbound().getMaxRequestBytes()) {
      throw new IllegalArgumentException("Outbound request exceeds configured maximum");
    }
    String method = spec.getMethod() == null ? "GET" : spec.getMethod().toUpperCase(Locale.ROOT);
    HttpRequest.BodyPublisher publisher = body.length == 0
        ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body);
    HttpRequest.Builder request = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofMillis(spec.getTimeoutMs() == null
            ? snapshot.config().getOutbound().getRequestTimeoutMs() : spec.getTimeoutMs()))
        .method(method, publisher);
    spec.getHeaders().forEach((name, value) -> {
      validateHeader(name, value);
      request.header(name, value);
    });

    long started = System.nanoTime();
    HttpResponse<InputStream> response = snapshot.httpClient().send(
        request.build(), HttpResponse.BodyHandlers.ofInputStream());
    int limit = snapshot.config().getOutbound().getMaxResponseBytes();
    byte[] responseBytes;
    try (InputStream input = response.body()) {
      responseBytes = input.readNBytes(limit + 1);
    }
    if (responseBytes.length > limit) {
      throw new IllegalArgumentException("Outbound response exceeds configured maximum");
    }
    Charset charset = response.headers().firstValue("Content-Type")
        .map(this::charset).orElse(StandardCharsets.UTF_8);
    return new HttpClientResponse(response.statusCode(), response.headers().map(),
        new String(responseBytes, charset), uri, Duration.ofNanos(System.nanoTime() - started).toMillis());
  }

  private HttpDriverContext copyContext(HttpDriverContext source, HttpClientResponse response) {
    var attributes = new java.util.LinkedHashMap<>(source.getAttributes());
    attributes.put("status", response.status());
    attributes.put("responseHeaders", response.headers());
    attributes.put("durationMs", response.durationMs());
    return HttpDriverContext.builder()
        .networkId(source.getNetworkId())
        .networkUnionId(source.getNetworkUnionId())
        .accessCode(source.getAccessCode())
        .operationType(source.getOperationType())
        .source(source.getSource())
        .method(source.getMethod())
        .routePath(source.getRoutePath())
        .remoteAddress(source.getRemoteAddress())
        .productKey(source.getProductKey())
        .deviceId(source.getDeviceId())
        .pathVariables(new java.util.LinkedHashMap<>(source.getPathVariables()))
        .headers(new java.util.LinkedHashMap<>(source.getHeaders()))
        .query(new java.util.LinkedHashMap<>(source.getQuery()))
        .attributes(attributes)
        .build();
  }

  private Charset charset(String contentType) {
    for (String part : contentType.split(";")) {
      String item = part.trim();
      if (item.regionMatches(true, 0, "charset=", 0, 8)) {
        try {
          return Charset.forName(item.substring(8).trim());
        } catch (Exception ignored) {
          return StandardCharsets.UTF_8;
        }
      }
    }
    return StandardCharsets.UTF_8;
  }

  private void validateHeader(String name, String value) {
    if (name == null || name.isBlank() || BLOCKED_HEADERS.contains(name.toLowerCase(Locale.ROOT))
        || name.indexOf('\r') >= 0 || name.indexOf('\n') >= 0 || value == null
        || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
      throw new IllegalArgumentException("Invalid outbound header");
    }
  }
}
