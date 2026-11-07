package cn.universal.manager.notice.service.channel;

import cn.universal.manager.notice.model.NoticeChannel;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WeComNoticeSendChannel implements NoticeSendChannel {

  @Override
  public boolean support(String type) {
    return "wecom".equalsIgnoreCase(type);
  }

  @Override
  public NoticeSendResult send(
      String content, String receivers, NoticeChannel config, Map<String, Object> params) {
    // 解析config.getConfig()为webhook等
    // TODO: 发送企业微信消息
    System.out.println("[WeCom] receivers=" + receivers + ", content=" + content);
    return NoticeSendResult.builder()
        .success(true)
        .receivers(receivers)
        .content(content)
        .errorMessage(null)
        .build();
  }
}
