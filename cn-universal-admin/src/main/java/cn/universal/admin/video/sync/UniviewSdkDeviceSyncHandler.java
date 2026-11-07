/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 宇视直连SDK设备同步处理器
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.admin.video.sync;

import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDeviceExt;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 宇视直连SDK设备同步处理器。 */
@Component
public class UniviewSdkDeviceSyncHandler extends DirectSdkDeviceSyncHandler {

  public UniviewSdkDeviceSyncHandler(VideoPlatformAdapterRegistry adapterRegistry) {
    super(adapterRegistry);
  }

  @Override
  public String getPlatformType() {
    return "uniview_sdk";
  }

  @Override
  protected String getLogPrefix() {
    return "宇视SDK";
  }

  @Override
  protected String getDefaultManufacturer() {
    return "Uniview";
  }

  @Override
  protected void setVendorSpecificDeviceExtFields(
      VideoPlatformDeviceExt.VideoPlatformDeviceExtBuilder builder, Map<String, Object> deviceData) {
    builder
        .ownerCode(fit(firstNonBlankString(deviceData.get("ownerCode")), 100))
        .subSystem(fit(firstNonBlankString(deviceData.get("subSystem")), 100));
    try {
      Object unitsInfo = deviceData.get("unitsInfo");
      if (unitsInfo != null) {
        builder.unitsInfo(objectMapper.writeValueAsString(unitsInfo));
      }
    } catch (Exception ignore) {
    }
  }

  @Override
  protected void setVendorSpecificChannelFields(
      VideoPlatformChannel.VideoPlatformChannelBuilder builder, Map<String, Object> channelData) {
    builder
        .channelCode(fit(firstNonBlankString(channelData.get("channelCode"), channelData.get("channelId")), 100))
        .channelSeq(
            parseIntOrNull(
                firstNonBlankString(
                    channelData.get("channelSeq"), channelData.get("channelNo"), channelData.get("channelIndex"))));
  }
}
