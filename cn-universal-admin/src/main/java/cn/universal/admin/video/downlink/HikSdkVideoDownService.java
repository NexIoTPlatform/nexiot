package cn.universal.admin.video.downlink;

import org.springframework.stereotype.Service;

/** 海康直连 SDK 视频物模型下行服务。 */
@Service("hikSdkVideoDownService")
public class HikSdkVideoDownService extends AbstractSdkVideoDownService {

  @Override
  public String name() {
    return "Hikvision Direct SDK";
  }

  @Override
  public String code() {
    return "hik_sdk";
  }
}
