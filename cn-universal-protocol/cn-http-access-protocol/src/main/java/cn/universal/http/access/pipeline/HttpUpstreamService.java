package cn.universal.http.access.pipeline;

import cn.hutool.core.collection.CollUtil;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.common.domain.R;
import cn.universal.core.message.UPRequest;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.http.access.model.HttpDriverContext;
import cn.universal.http.access.model.HttpNormalizedBatch.HttpNormalizedRecord;
import cn.universal.http.access.runtime.HttpRouteSnapshot;
import cn.universal.http.access.web.HttpAccessException;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.query.IoTDeviceQuery;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Reuses the standard device lifecycle, log, shadow and subscription pipeline for HTTP. */
@Service
public class HttpUpstreamService extends AbstratIoTService {

  private final HttpUpPushService pushService;

  @Value("${iot.register.auto.unionId:auto_register}")
  private String autoRegisterUnionId;

  public HttpUpstreamService(HttpUpPushService pushService) {
    this.pushService = pushService;
  }

  public HttpUpstreamResult processIngress(
      HttpRouteSnapshot snapshot, String payload, HttpDriverContext context) {
    IoTProduct product = requireBoundProduct(snapshot);
    UPRequest identity = preDecode(product.getProductKey(), payload, context);
    if (identity == null || blank(identity.getDeviceId())) {
      throw new HttpAccessException(400, "DEVICE_ID_REQUIRED",
          "Product driver preDecode must return deviceId");
    }
    if (!blank(identity.getProductKey()) && !product.getProductKey().equals(identity.getProductKey())) {
      throw new HttpAccessException(403, "PRODUCT_NOT_BOUND", "Product driver resolved a different product");
    }
    context.getAttributes().put("preDecode", identity);
    context.getAttributes().put("resolvedDeviceId", identity.getDeviceId());
    IoTDeviceDTO device = resolveDevice(product, identity.getDeviceId(), identity.isAllowInsert());
    List<BaseUPRequest> decoded = decodeAndEnrich(product, device, payload, context);
    return publish(decoded);
  }

  /**
   * Processes the existing authenticated OpenAPI reporting endpoints.
   *
   * <p>These endpoints already resolve {@code productKey + deviceId} and perform tenant checks in
   * {@code ApiControllerV1}. They still need to use the same HTTP product driver's decode and the
   * standard upstream pipeline, so this adapter intentionally keeps that public service contract.
   */
  public HttpUpstreamResult processOpenApi(String productKey, String deviceId, String payload) {
    if (blank(productKey)) {
      throw new HttpAccessException(400, "PRODUCT_KEY_REQUIRED", "productKey is required");
    }
    if (blank(deviceId)) {
      throw new HttpAccessException(400, "DEVICE_ID_REQUIRED", "deviceId is required");
    }
    IoTProduct product = getProduct(productKey);
    if (product == null || !"http".equalsIgnoreCase(product.getThirdPlatform())) {
      throw new HttpAccessException(403, "PRODUCT_PROTOCOL_MISMATCH", "Product is not an HTTP product");
    }
    IoTDeviceDTO device = lifeCycleDevInstance(
        IoTDeviceQuery.builder().productKey(productKey).deviceId(deviceId).build());
    if (device == null) {
      throw new HttpAccessException(404, "DEVICE_NOT_FOUND", "Device does not exist");
    }
    HttpDriverContext context = HttpDriverContext.builder()
        .operationType("OPEN_API")
        .source("OPEN_API")
        .productKey(productKey)
        .deviceId(deviceId)
        .attributes(new LinkedHashMap<>(Map.of("entrypoint", "ApiControllerV1")))
        .build();
    return publish(decodeAndEnrich(product, device, payload, context));
  }

  /** Accepts poll records normalized by the same product driver through yourToIot. */
  public HttpUpstreamResult processPoll(
      HttpRouteSnapshot snapshot, List<HttpNormalizedRecord> records, HttpDriverContext context) {
    if (records == null || records.isEmpty()) {
      return new HttpUpstreamResult(0, null);
    }
    IoTProduct product = requireBoundProduct(snapshot);
    List<BaseUPRequest> accepted = new ArrayList<>();
    for (HttpNormalizedRecord record : records) {
      if (!blank(record.getProductKey()) && !product.getProductKey().equals(record.getProductKey())) {
        throw new HttpAccessException(403, "PRODUCT_NOT_BOUND", "Poll record targets a different product");
      }
      if (blank(record.getDeviceId())) {
        throw new HttpAccessException(400, "DEVICE_ID_REQUIRED", "Poll record deviceId is required");
      }
      IoTDeviceDTO device = resolveDevice(product, record.getDeviceId(), false);
      if (!blank(record.getState())) {
        applyState(product.getProductKey(), device.getDeviceId(), record.getState());
      }
      accepted.addAll(decodeAndEnrich(product, device, record.getPayload(), withDevice(context, device)));
    }
    return publish(accepted);
  }

  private IoTProduct requireBoundProduct(HttpRouteSnapshot snapshot) {
    String productKey = snapshot.definition().productKey();
    if (blank(productKey)) {
      throw new HttpAccessException(500, "PRODUCT_NOT_BOUND", "HTTP network has no bound product");
    }
    IoTProduct product = getProduct(productKey);
    if (product == null || !"http".equalsIgnoreCase(product.getThirdPlatform())) {
      throw new HttpAccessException(500, "PRODUCT_NOT_BOUND", "Bound HTTP product is unavailable");
    }
    return product;
  }

  private IoTDeviceDTO resolveDevice(IoTProduct product, String deviceId, boolean allowInsert) {
    IoTDeviceDTO device = lifeCycleDevInstance(
        IoTDeviceQuery.builder().productKey(product.getProductKey()).deviceId(deviceId).build());
    if (device != null) {
      return device;
    }
    if (!allowInsert) {
      throw new HttpAccessException(404, "DEVICE_NOT_FOUND", "Device does not exist");
    }
    registerDevice(product, deviceId);
    device = lifeCycleDevInstance(
        IoTDeviceQuery.builder().productKey(product.getProductKey()).deviceId(deviceId).build());
    if (device == null) {
      throw new HttpAccessException(502, "DEVICE_REGISTER_FAILED", "Automatic device registration failed");
    }
    return device;
  }

  private void registerDevice(IoTProduct product, String deviceId) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("deviceName", deviceId);
    data.put("imei", deviceId);
    Map<String, Object> command = new LinkedHashMap<>();
    command.put("appUnionId", autoRegisterUnionId);
    command.put("productKey", product.getProductKey());
    command.put("deviceId", deviceId);
    command.put("tenantId", product.getTenantId());
    command.put("allowInsert", true);
    command.put("cmd", DownCmd.DEV_ADD.name());
    command.put("data", data);
    R<?> result = IoTDownlFactory.getIDown(product.getThirdPlatform())
        .doAction(UnifiedDownlinkCommand.fromJson(cn.hutool.json.JSONUtil.parseObj(command)));
    if (result == null || !result.isSuccess()) {
      throw new HttpAccessException(502, "DEVICE_REGISTER_FAILED", "Automatic device registration failed");
    }
  }

  private List<BaseUPRequest> decodeAndEnrich(
      IoTProduct product, IoTDeviceDTO device, String payload, HttpDriverContext context) {
    String safePayload = payload == null ? "" : payload;
    List<BaseUPRequest> decoded = decode(product.getProductKey(), safePayload, context, BaseUPRequest.class);
    if (CollUtil.isEmpty(decoded)) {
      BaseUPRequest fallback = BaseUPRequest.builder()
          .productKey(product.getProductKey()).deviceId(device.getDeviceId()).payload(safePayload).build();
      decoded = List.of(buildCodecNullBean(parseJsonPayload(safePayload), fallback));
    }
    for (BaseUPRequest request : decoded) {
      request.setProductKey(product.getProductKey());
      request.setDeviceId(device.getDeviceId());
      request.setIotId(device.getIotId());
      request.setDeviceName(device.getDeviceName());
      request.setUserUnionId(device.getUserUnionId());
      request.setApplicationId(device.getApplicationId());
      request.setIoTProduct(product);
      request.setIoTDeviceDTO(device);
      if (request.getTime() == null || request.getTime() <= 0) request.setTime(System.currentTimeMillis());
      if (blank(request.getPayload())) request.setPayload(safePayload);
      if (request.getMessageType() == null) request.setMessageType(MessageType.PROPERTIES);
    }
    doEventNameAndSubscribe(decoded, device);
    doLogMetadataAndShadow(decoded, device, product);
    return decoded;
  }

  private HttpUpstreamResult publish(List<BaseUPRequest> requests) {
    if (requests == null || requests.isEmpty()) return new HttpUpstreamResult(0, null);
    pushService.push(requests);
    Object reply = requests.stream().map(BaseUPRequest::getReplyPayload)
        .filter(value -> value != null).findFirst().orElse(null);
    return new HttpUpstreamResult(requests.size(), reply);
  }

  private void applyState(String productKey, String deviceId, String state) {
    if ("ONLINE".equalsIgnoreCase(state)) ioTDeviceActionAfterService.online(productKey, deviceId);
    if ("OFFLINE".equalsIgnoreCase(state)) ioTDeviceActionAfterService.offline(productKey, deviceId);
  }

  private HttpDriverContext withDevice(HttpDriverContext context, IoTDeviceDTO device) {
    context.getAttributes().put("resolvedDeviceId", device.getDeviceId());
    return context;
  }

  private boolean blank(String value) { return value == null || value.isBlank(); }
}
