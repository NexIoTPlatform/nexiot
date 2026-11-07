package cn.universal.admin.system.service.impl;

import cn.universal.admin.system.service.ISysNoticeService;
import cn.universal.persistence.entity.admin.SysNotice;
import cn.universal.persistence.mapper.admin.SysNoticeMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 系统通知Service业务层处理
 *
 * @author universal
 * @date 2023-01-01
 */
@Service
public class SysNoticeServiceImpl implements ISysNoticeService {
  @Autowired private SysNoticeMapper noticeMapper;

  /**
   * 查询系统通知
   *
   * @param noticeId 系统通知主键
   * @return 系统通知
   */
  @Override
  public SysNotice selectNoticeById(Long noticeId) {
    return noticeMapper.selectNoticeById(noticeId);
  }

  /**
   * 查询系统通知列表
   *
   * @param notice 系统通知
   * @return 系统通知
   */
  @Override
  public List<SysNotice> selectNoticeList(SysNotice notice) {
    return noticeMapper.selectNoticeList(notice);
  }

  /**
   * 新增系统通知
   *
   * @param notice 系统通知
   * @return 结果
   */
  @Override
  public int insertNotice(SysNotice notice) {
    return noticeMapper.insertNotice(notice);
  }

  /**
   * 修改系统通知
   *
   * @param notice 系统通知
   * @return 结果
   */
  @Override
  public int updateNotice(SysNotice notice) {
    return noticeMapper.updateNotice(notice);
  }

  /**
   * 批量删除系统通知
   *
   * @param noticeIds 需要删除的系统通知主键
   * @return 结果
   */
  @Override
  public int deleteNoticeByIds(Long[] noticeIds) {
    return noticeMapper.deleteNoticeByIds(noticeIds);
  }

  /**
   * 删除系统通知信息
   *
   * @param noticeId 系统通知主键
   * @return 结果
   */
  @Override
  public int deleteNoticeById(Long noticeId) {
    return noticeMapper.deleteNoticeById(noticeId);
  }
}
