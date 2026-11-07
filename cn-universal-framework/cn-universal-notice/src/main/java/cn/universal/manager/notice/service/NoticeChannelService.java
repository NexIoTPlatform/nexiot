package cn.universal.manager.notice.service;

import cn.universal.manager.notice.model.NoticeChannel;
import java.util.List;

public interface NoticeChannelService {

  List<NoticeChannel> list();

  List<NoticeChannel> search(String name, String channelType, String status);

  void save(NoticeChannel channel);

  void delete(Long id);

  void deleteBatch(List<Long> ids);

  NoticeChannel getById(Long id);
}
