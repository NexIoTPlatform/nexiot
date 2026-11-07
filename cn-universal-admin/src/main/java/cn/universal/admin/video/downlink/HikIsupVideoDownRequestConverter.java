package cn.universal.admin.video.downlink;

import org.springframework.stereotype.Component;

@Component("hikIsupVideoDownRequestConverter")
public class HikIsupVideoDownRequestConverter extends AbstractSdkVideoDownRequestConverter {
  @Override public String supportedProtocol() { return "hik_isup"; }
}
