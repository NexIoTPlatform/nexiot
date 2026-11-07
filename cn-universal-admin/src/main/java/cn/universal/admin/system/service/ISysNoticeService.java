package cn.universal.admin.system.service;

import cn.universal.persistence.entity.admin.SysNotice;
import java.util.List;

/**
 * 系统通知Service接口
 *
 * @author universal
 * @date 2023-01-01
 */
public interface ISysNoticeService {
  /**
   * 查询系统通知
   *
   * @param noticeId 系统通知主键
   * @return 系统通知
   */
  public SysNotice selectNoticeById(Long noticeId);

  /**
   * 查询系统通知列表
   *
   * @param notice 系统通知
   * @return 系统通知集合
   */
  public List<SysNotice> selectNoticeList(SysNotice notice);

  /**
   * 新增系统通知
   *
   * @param notice 系统通知
   * @return 结果
   */
  public int insertNotice(SysNotice notice);

  /**
   * 修改系统通知
   *
   * @param notice 系统通知
   * @return 结果
   */
  public int updateNotice(SysNotice notice);

  /**
   * 批量删除系统通知
   *
   * @param noticeIds 需要删除的系统通知主键集合
   * @return 结果
   */
  public int deleteNoticeByIds(Long[] noticeIds);

  /**
   * 删除系统通知信息
   *
   * @param noticeId 系统通知主键
   * @return 结果
   */
  public int deleteNoticeById(Long noticeId);
}
