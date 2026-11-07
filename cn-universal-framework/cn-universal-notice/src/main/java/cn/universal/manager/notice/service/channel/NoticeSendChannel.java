package cn.universal.manager.notice.service.channel;

import cn.universal.manager.notice.model.NoticeChannel;
import java.util.Map;

public interface NoticeSendChannel {

  boolean support(String type);

  NoticeSendResult send(
      String content, String receivers, NoticeChannel config, Map<String, Object> params);
}
