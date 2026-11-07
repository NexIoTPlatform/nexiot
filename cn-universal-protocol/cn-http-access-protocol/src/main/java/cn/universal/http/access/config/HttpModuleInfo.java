package cn.universal.http.access.config;

import cn.universal.core.protocol.ProtocolModuleInfo;
import org.springframework.stereotype.Component;

@Component
public class HttpModuleInfo implements ProtocolModuleInfo {

  @Override
  public String getCode() {
    return "http";
  }

  @Override
  public String getName() {
    return "HTTP平台接入";
  }

  @Override
  public String getDescription() {
    return "产品驱动管理协议逻辑的 HTTP 设备接入与第三方交互";
  }

  @Override
  public String getVersion() {
    return "2.0";
  }

  @Override
  public ProtocolCategory getCategory() {
    return ProtocolCategory.PLATFORM;
  }
}
