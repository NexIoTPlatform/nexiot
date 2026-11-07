package cn.universal.admin.video.downlink;

import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.domain.R;
import cn.universal.core.downlink.DownlinkContext;
import cn.universal.core.downlink.DownlinkInterceptorChain;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.service.IDown;
import cn.universal.dm.device.service.AbstractDownService;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.query.IoTDeviceQuery;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/** 设备直连 SDK 视频协议的物模型下行桥接基类。 */
@Slf4j
public abstract class AbstractSdkVideoDownService extends AbstractDownService<UnifiedDownlinkCommand>
    implements IDown {

  @Resource private SdkVideoThingModelFunctionExecutor functionExecutor;
  @Resource private DownlinkInterceptorChain downlinkInterceptorChain;
  @Resource private IoTDeviceService iotDeviceLifecycleService;

  @Override
  public DownlinkInterceptorChain getInterceptorChain() {
    return downlinkInterceptorChain;
  }

  @Override
  @SuppressWarnings("unchecked")
  public R<?> doProcess(DownlinkContext<?> context) {
    try {
      UnifiedDownlinkCommand command = resolveCommand(context);
      if (command == null) {
        return R.error("消息为空");
      }
      IoTProduct product = getProduct(command.getProductKey());
      if (product == null) {
        return R.error("产品不存在: " + command.getProductKey());
      }
      IoTDeviceDTO device =
          getIoTDeviceDTO(
              IoTDeviceQuery.builder()
                  .productKey(command.getProductKey())
                  .deviceId(command.getDeviceId())
                  .iotId(command.getIotId())
                  .build());
      if (device != null) {
        command.setDeviceId(device.getDeviceId());
        command.setIotId(device.getIotId());
      }
      ((DownlinkContext<UnifiedDownlinkCommand>) context).setDownRequest(command);
      context.setProductKey(command.getProductKey());
      context.setDeviceId(command.getDeviceId());
      context.setIotId(command.getIotId());
      // 删除走统一设备生命周期，连带清理影子及缓存；普通视频函数才路由到 SDK。
      if (DownCmd.DEV_DEL.equals(command.getCmd())) {
        String iotId = command.getIotId();
        if (iotId == null || iotId.isBlank()) {
          return R.error("设备不存在");
        }
        int deleted = iotDeviceLifecycleService.delDevInstance(iotId);
        return deleted > 0 ? R.ok("删除成功") : R.error("设备不存在或已被删除");
      }
      log.info(
          "[{}下行] function={}, productKey={}, deviceId={}",
          code(),
          command.getFunction() == null ? null : command.getFunction().get("function"),
          command.getProductKey(),
          command.getDeviceId());
      return functionExecutor.execute(code(), product, command);
    } catch (Exception e) {
      log.error("[{}下行] 处理异常", code(), e);
      return R.error("处理异常: " + e.getMessage());
    }
  }

  @Override
  protected UnifiedDownlinkCommand convert(String request) {
    return UnifiedDownlinkCommand.fromString(request);
  }

  private UnifiedDownlinkCommand resolveCommand(DownlinkContext<?> context) {
    if (context == null) {
      return null;
    }
    if (context.getCommand() != null) {
      return context.getCommand();
    }
    if (context.getDownRequest() instanceof UnifiedDownlinkCommand command) {
      return command;
    }
    if (context.getRawMessage() != null) {
      return UnifiedDownlinkCommand.fromString(context.getRawMessage());
    }
    if (context.getJsonMessage() != null) {
      return UnifiedDownlinkCommand.fromJson(context.getJsonMessage());
    }
    return null;
  }
}
