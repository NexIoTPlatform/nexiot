package cn.universal.manager.notice.service;

import cn.universal.manager.notice.model.NoticeSendRequest;
import cn.universal.manager.notice.service.channel.NoticeSendResult;

public interface NoticeService {

  void send(NoticeSendRequest req);

  NoticeSendResult sendR(NoticeSendRequest request);
  NoticeSendResult test(NoticeSendRequest request);
  java.util.Map<String, Object> preview(NoticeSendRequest request);
}
