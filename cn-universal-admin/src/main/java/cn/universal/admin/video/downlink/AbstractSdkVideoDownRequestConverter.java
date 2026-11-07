package cn.universal.admin.video.downlink;

import cn.universal.core.downlink.DownlinkContext;
import cn.universal.core.downlink.converter.DownRequestConverter;
import cn.universal.core.message.DownRequest;
import cn.universal.core.message.UnifiedDownlinkCommand;

/** 直连 SDK 视频物模型下行只需要透传统一命令，具体函数由视频执行器处理。 */
abstract class AbstractSdkVideoDownRequestConverter implements DownRequestConverter<DownRequest> {

  @Override
  public DownRequest convert(UnifiedDownlinkCommand command, DownlinkContext<?> context) {
    if (command == null) {
      throw new IllegalArgumentException("下行指令为空");
    }
    DownRequest request = new DownRequest();
    request.setProductKey(command.getProductKey());
    request.setDeviceId(command.getDeviceId());
    request.setIotId(command.getIotId());
    request.setApplicationId(command.getApplicationId());
    request.setAppUnionId(command.getAppUnionId());
    // 保留统一入口已经建立的租户及调用方上下文，后续权限拦截依赖这些字段。
    request.setTenantId(command.getTenantId());
    request.setGwProductKey(command.getGwProductKey());
    request.setGwDeviceId(command.getGwDeviceId());
    request.setExtDeviceId(command.getExtDeviceId());
    request.setSlaveAddress(command.getSlaveAddress());
    request.setDeviceAddress(command.getDeviceAddress());
    request.setCmd(command.getCmd());
    request.setMsgId(command.getMsgId());
    request.setDetail(command.getDetail());
    request.setDeptId(command.getDeptId());
    request.setFunction(command.getFunction());
    request.setDeviceReuse(command.isDeviceReuse());
    request.setAdmin(command.isAdmin());
    request.setAllowInsert(command.isAllowInsert());
    if (command.getRawData() != null) {
      request.setPayload(command.getRawData().toString());
    }
    return request;
  }

  @Override
  public boolean supports(UnifiedDownlinkCommand command) {
    return command != null;
  }

  @Override
  public int getPriority() {
    return 10;
  }
}
