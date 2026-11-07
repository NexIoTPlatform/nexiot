package cn.universal.admin.video.downlink;

import org.springframework.stereotype.Component;

@Component("tiandySdkVideoDownRequestConverter")
public class TiandySdkVideoDownRequestConverter extends AbstractSdkVideoDownRequestConverter {

  @Override
  public String supportedProtocol() {
    return "tiandy_sdk";
  }
}
