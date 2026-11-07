package cn.universal.manager.notice.service;

import cn.universal.manager.notice.dto.NoticeTemplateDTO;
import cn.universal.manager.notice.model.NoticeTemplate;
import java.util.List;

public interface NoticeTemplateService {

  List<NoticeTemplate> list();

  void save(NoticeTemplate template);

  void saveTemplate(NoticeTemplateDTO templateDTO, String currentUser);

  void delete(Long id);

  NoticeTemplate getById(Long id);

  List<NoticeTemplate> search(String name, String channelType, String status);

  void deleteBatch(List<Long> ids);

  void testTemplate(Long templateId, String receivers, Object params);

  void enableBatch(List<Long> ids);

  void disableBatch(List<Long> ids);
}
