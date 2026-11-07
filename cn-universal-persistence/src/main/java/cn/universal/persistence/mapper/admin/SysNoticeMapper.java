package cn.universal.persistence.mapper.admin;

import cn.universal.persistence.entity.admin.SysNotice;
import java.util.List;

/**
 * 系统通知Mapper接口
 *
 * @author universal
 * @date 2023-01-01
 */
public interface SysNoticeMapper {
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
   * 删除系统通知
   *
   * @param noticeId 系统通知主键
   * @return 结果
   */
  public int deleteNoticeById(Long noticeId);

  /**
   * 批量删除系统通知
   *
   * @param noticeIds 需要删除的数据主键集合
   * @return 结果
   */
  public int deleteNoticeByIds(Long[] noticeIds);
}
