package cn.universal.dm.device.service;

import cn.hutool.json.JSONUtil;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDeviceSubscribe;
import cn.universal.persistence.entity.IoTProduct;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Creates detached request snapshots before work crosses an asynchronous boundary. */
@Slf4j
@Component
public class UPRequestSnapshotter {

  public <T extends BaseUPRequest> List<T> snapshot(List<T> requests) {
    if (requests == null || requests.isEmpty()) {
      return List.of();
    }
    List<T> snapshots = new ArrayList<>(requests.size());
    for (T request : requests) {
      if (request == null) {
        continue;
      }
      T snapshot = snapshot(request);
      if (snapshot == null) {
        return List.of();
      }
      snapshots.add(snapshot);
    }
    return snapshots;
  }

  @SuppressWarnings("unchecked")
  public <T extends BaseUPRequest> T snapshot(T source) {
    try {
      T target =
          (T) JSONUtil.toBean(JSONUtil.toJsonStr(source), source.getClass(), false);
      target.setPayloadRaw(source.getPayloadRaw() == null ? null : source.getPayloadRaw().clone());
      target.setIoTDeviceDTO(copy(source.getIoTDeviceDTO(), IoTDeviceDTO.class));
      target.setIoTProduct(copy(source.getIoTProduct(), IoTProduct.class));
      target.setDevSubscribe(copySubscriptions(source.getDevSubscribe()));
      target.setOnlyCache(source.isOnlyCache());
      target.setAllowInsert(source.isAllowInsert());
      target.setStandardTcp(source.isStandardTcp());
      target.setPreStore(source.isPreStore());
      target.setDebug(source.isDebug());
      target.setEmptyProtocol(source.isEmptyProtocol());
      return target;
    } catch (Exception ex) {
      log.error(
          "Failed to snapshot upstream request, productKey={}, deviceId={}, requestId={}",
          source.getProductKey(),
          source.getDeviceId(),
          source.getRequestId(),
          ex);
      return null;
    }
  }

  private Object copySubscriptions(Object source) {
    if (!(source instanceof List<?> subscriptions)) {
      return source;
    }
    List<IoTDeviceSubscribe> copies = new ArrayList<>(subscriptions.size());
    for (Object item : subscriptions) {
      if (!(item instanceof IoTDeviceSubscribe subscribe)) {
        return List.copyOf(subscriptions);
      }
      copies.add(copy(subscribe, IoTDeviceSubscribe.class));
    }
    return copies;
  }

  private <T> T copy(T source, Class<T> type) {
    return source == null ? null : JSONUtil.toBean(JSONUtil.toJsonStr(source), type);
  }
}
