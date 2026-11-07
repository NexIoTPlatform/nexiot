package cn.universal.admin.video.downlink;

import org.springframework.stereotype.Service;

/** 天地伟业直连 SDK 视频物模型下行服务。 */
@Service("tiandySdkVideoDownService")
public class TiandySdkVideoDownService extends AbstractSdkVideoDownService {

  @Override
  public String name() {
    return "Tiandy Direct SDK";
  }

  @Override
  public String code() {
    return "tiandy_sdk";
  }
}
