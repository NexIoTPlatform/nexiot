package cn.universal.admin.video.service;

import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.dm.video.zlm.VideoZlmStreamProxyService;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** 宇视 SDK 直连预览；既有厂商继续使用各自的会话服务。 */
@Service
public class VideoUniviewPreviewService extends VideoSdkNativePreviewService {
  public VideoUniviewPreviewService(VideoPlatformAdapterRegistry adapters,
      ObjectProvider<VideoZlmStreamProxyService> media, VideoPlatformInstanceMapper instances,
      VideoPlatformDeviceMapper devices) {
    super(adapters, media, instances, devices, "uniview_sdk", "宇视", "uniview");
  }
}
