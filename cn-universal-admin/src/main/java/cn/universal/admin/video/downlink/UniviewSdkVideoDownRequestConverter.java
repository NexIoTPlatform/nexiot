package cn.universal.admin.video.downlink;

import org.springframework.stereotype.Component;

@Component("univiewSdkVideoDownRequestConverter")
public class UniviewSdkVideoDownRequestConverter extends AbstractSdkVideoDownRequestConverter {

  @Override
  public String supportedProtocol() {
    return "uniview_sdk";
  }
}
