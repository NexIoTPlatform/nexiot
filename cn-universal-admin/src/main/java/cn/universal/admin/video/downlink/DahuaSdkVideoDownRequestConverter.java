package cn.universal.admin.video.downlink;

import org.springframework.stereotype.Component;

@Component("dahuaSdkVideoDownRequestConverter")
public class DahuaSdkVideoDownRequestConverter extends AbstractSdkVideoDownRequestConverter {

  @Override
  public String supportedProtocol() {
    return "dahua_sdk";
  }
}
