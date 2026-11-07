package cn.universal.http.access.web;

import cn.universal.http.access.model.HttpDriverContext;
import cn.universal.http.access.pipeline.HttpUpstreamResult;
import cn.universal.http.access.pipeline.HttpUpstreamService;
import cn.universal.http.access.runtime.HttpRouteSnapshot;
import cn.universal.http.access.security.HttpAccessAuthenticator;
import cn.universal.http.access.security.HttpRateLimiter;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Component;

@Component
public class HttpInboundProcessor {

  private final HttpAccessAuthenticator authenticator;
  private final HttpRateLimiter rateLimiter;
  private final HttpUpstreamService upstreamService;

  public HttpInboundProcessor(
      HttpAccessAuthenticator authenticator,
      HttpRateLimiter rateLimiter,
      HttpUpstreamService upstreamService) {
    this.authenticator = authenticator;
    this.rateLimiter = rateLimiter;
    this.upstreamService = upstreamService;
  }

  public HttpIngressResponse process(HttpRouteSnapshot snapshot, HttpIngressRequest request) {
    if (request.body() != null && request.body().length > snapshot.config().getInbound().getMaxBodyBytes()) {
      throw new HttpAccessException(413, "BODY_TOO_LARGE", "Request body exceeds network limit");
    }
    if (!rateLimiter.allow(snapshot.definition().id(), request.remoteAddress(),
        snapshot.config().getInbound().getRateLimitPerMinute())) {
      throw new HttpAccessException(429, "RATE_LIMITED", "Too many requests");
    }
    authenticator.authenticate(snapshot, request);
    HttpDriverContext context = HttpDriverContext.builder()
        .networkId(snapshot.definition().id())
        .networkUnionId(snapshot.definition().unionId())
        .accessCode(snapshot.config().getAccessCode())
        .operationType("INGRESS")
        .source("INGRESS")
        .method(request.method())
        .routePath(request.routePath())
        .remoteAddress(request.remoteAddress())
        .productKey(snapshot.definition().productKey())
        .headers(new LinkedHashMap<>(request.headers()))
        .query(new LinkedHashMap<>(request.query()))
        .build();
    HttpUpstreamResult result = upstreamService.processIngress(snapshot, request.bodyText(), context);
    Object body = result.replyPayload();
    if (body == null) {
      body = java.util.Map.of("success", true, "acceptedRecords", result.acceptedRecords());
    }
    return new HttpIngressResponse(200, java.util.Map.of(), body);
  }
}
