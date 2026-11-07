package cn.universal.dm.device.service.push;

import cn.hutool.core.util.StrUtil;
import cn.universal.dm.device.entity.IoTPushResult;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.entity.bo.UPPushBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TcpPushStrategy implements PushStrategy<UPPushBO.TcpPushConfig> {

  private final TcpPushClientManager clientManager;

  public TcpPushStrategy(TcpPushClientManager clientManager) {
    this.clientManager = clientManager;
  }

  @Override
  public IoTPushResult execute(
      BaseUPRequest request, String messageJson, UPPushBO.TcpPushConfig config) {
    String appUniqueId = resolveAppUniqueId(request);
    long start = System.currentTimeMillis();
    try {
      clientManager.send(appUniqueId, config, messageJson);
      return IoTPushResult.success(
          platform(request),
          request.getProductKey(),
          request.getIotId(),
          "TCP",
          messageJson,
          System.currentTimeMillis() - start);
    } catch (Exception e) {
      log.debug(
          "[TCP推送] 失败: appUniqueId={}, deviceId={}, error={}",
          appUniqueId,
          request.getIotId(),
          e.getMessage());
      return IoTPushResult.failed(
          platform(request),
          request.getProductKey(),
          request.getIotId(),
          "TCP",
          messageJson,
          "TCP推送失败: " + e.getMessage(),
          "TCP_PUSH_ERROR");
    }
  }

  @Override
  public boolean isSupported(UPPushBO.TcpPushConfig config) {
    return config != null
        && config.isSupport()
        && config.isEnable()
        && StrUtil.isNotBlank(config.getHost())
        && config.getPort() != null;
  }

  private String resolveAppUniqueId(BaseUPRequest request) {
    if (StrUtil.isNotBlank(request.getApplicationId())) {
      return request.getApplicationId();
    }
    if (request.getIoTDeviceDTO() != null
        && StrUtil.isNotBlank(request.getIoTDeviceDTO().getApplicationId())) {
      return request.getIoTDeviceDTO().getApplicationId();
    }
    throw new IllegalArgumentException("应用标识不能为空");
  }

  private String platform(BaseUPRequest request) {
    return request.getIoTDeviceDTO() == null
        ? null
        : request.getIoTDeviceDTO().getThirdPlatform();
  }
}
