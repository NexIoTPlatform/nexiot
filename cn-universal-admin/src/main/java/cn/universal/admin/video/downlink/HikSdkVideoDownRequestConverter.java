package cn.universal.admin.video.downlink;

import org.springframework.stereotype.Component;

@Component("hikSdkVideoDownRequestConverter")
public class HikSdkVideoDownRequestConverter extends AbstractSdkVideoDownRequestConverter {

  @Override
  public String supportedProtocol() {
    return "hik_sdk";
  }
}
