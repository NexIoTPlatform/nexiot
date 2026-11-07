package cn.universal.admin.video.downlink;

import org.springframework.stereotype.Service;

/** 宇视直连 SDK 视频物模型下行服务。 */
@Service("univiewSdkVideoDownService")
public class UniviewSdkVideoDownService extends AbstractSdkVideoDownService {

  @Override
  public String name() {
    return "Uniview Direct SDK";
  }

  @Override
  public String code() {
    return "uniview_sdk";
  }
}
