package cn.universal.admin.video.downlink;

import org.springframework.stereotype.Service;

/** 大华直连 SDK 视频物模型下行服务。 */
@Service("dahuaSdkVideoDownService")
public class DahuaSdkVideoDownService extends AbstractSdkVideoDownService {

  @Override
  public String name() {
    return "Dahua Direct SDK";
  }

  @Override
  public String code() {
    return "dahua_sdk";
  }
}
