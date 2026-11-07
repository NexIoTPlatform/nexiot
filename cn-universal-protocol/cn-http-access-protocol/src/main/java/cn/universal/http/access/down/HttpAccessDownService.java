package cn.universal.http.access.down;

import cn.universal.common.domain.R;
import cn.universal.core.downlink.DownlinkContext;
import cn.universal.core.downlink.DownlinkInterceptorChain;
import cn.universal.core.service.IDown;
import cn.universal.dm.device.service.AbstractDownService;
import cn.universal.http.access.client.HttpInteractionExecutor;
import cn.universal.http.access.client.HttpInteractionResult;
import cn.universal.http.access.model.HttpDriverContext;
import cn.universal.http.access.runtime.HttpOperationRegistry;
import cn.universal.http.access.runtime.HttpRouteSnapshot;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Delegates every HTTP control interaction to the product driver's encode and iotToYour hooks. */
@Service
public class HttpAccessDownService extends AbstractDownService<HttpDownRequest> implements IDown {

  private final HttpOperationRegistry registry;
  private final HttpInteractionExecutor interactionExecutor;
  private final DownlinkInterceptorChain interceptorChain;
  private final HttpDeviceLifecycleService lifecycleService;

  public HttpAccessDownService(
      HttpOperationRegistry registry,
      HttpInteractionExecutor interactionExecutor,
      DownlinkInterceptorChain interceptorChain,
      HttpDeviceLifecycleService lifecycleService) {
    this.registry = registry;
    this.interactionExecutor = interactionExecutor;
    this.interceptorChain = interceptorChain;
    this.lifecycleService = lifecycleService;
  }

  @Override public String name() { return "HTTP接入"; }
  @Override public String code() { return "http"; }
  @Override public DownlinkInterceptorChain getInterceptorChain() { return interceptorChain; }

  @Override
  protected HttpDownRequest convert(String request) {
    throw new UnsupportedOperationException("Use UnifiedDownlinkCommand for HTTP downlink");
  }

  @Override
  public R<?> doProcess(DownlinkContext<?> context) {
    if (!(context.getDownRequest() instanceof HttpDownRequest request)) {
      return R.error("HTTP downlink conversion failed");
    }
    if (HttpDownRequest.isDeviceLifecycle(request.getCmd())) {
      return lifecycleService.process(request);
    }
    if (HttpDownRequest.isProductLifecycle(request.getCmd())) {
      // Product persistence is owned by the platform; HTTP has no remote product registration.
      return R.ok();
    }
    HttpRouteSnapshot snapshot = registry.getByUnionId(request.getProduct().getNetworkUnionId())
        .orElse(null);
    if (snapshot == null) {
      return R.error("Bound HTTP network is not running");
    }
    Map<String, Object> attributes = new LinkedHashMap<>();
    attributes.put("command", request.getCommand());
    attributes.put("device", request.getDevice());
    attributes.put("downlinkAttributes", context.getAttributes());
    HttpDriverContext driverContext = HttpDriverContext.builder()
        .networkId(snapshot.definition().id())
        .networkUnionId(snapshot.definition().unionId())
        .accessCode(snapshot.config().getAccessCode())
        .operationType("CONTROL")
        .source("CONTROL")
        .productKey(request.getProductKey())
        .deviceId(request.getDeviceId())
        .attributes(attributes)
        .build();
    try {
      HttpInteractionResult result = interactionExecutor.execute(
          snapshot, request.getProductKey(), request.getEncodedPayload(), driverContext);
      return R.ok(Map.of("httpStatus", result.response().status(), "body", result.response().body()));
    } catch (Exception e) {
      return R.error("HTTP command failed: " + safeMessage(e));
    }
  }

  private String safeMessage(Exception e) {
    String message = e.getMessage();
    return message == null ? e.getClass().getSimpleName() : message.substring(0, Math.min(500, message.length()));
  }
}
