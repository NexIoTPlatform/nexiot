package cn.universal.manager.notice.service;

import cn.universal.manager.notice.model.NoticeSendRecord;
import java.util.List;

public interface NoticeRecordService {

  void save(NoticeSendRecord record);

  List<NoticeSendRecord> list();

  List<NoticeSendRecord> search(String keyword, String type, String status);

  List<NoticeSendRecord> search(String keyword, String type, String status, String creator);
}
