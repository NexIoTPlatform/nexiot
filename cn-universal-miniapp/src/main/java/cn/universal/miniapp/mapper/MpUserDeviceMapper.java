package cn.universal.miniapp.mapper;

import cn.universal.miniapp.entity.MpUserDevice;
import cn.universal.persistence.common.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 小程序用户设备绑定Mapper
 */
public interface MpUserDeviceMapper extends BaseMapper<MpUserDevice> {

  /**
   * 查询指定小程序应用下某个用户绑定的设备ID列表
   *
   * @param appId 小程序APPID
   * @param userId 小程序用户ID
   * @return 该用户绑定的设备iotId列表
   */
  List<String> selectIotIdsByUser(@Param("appId") String appId, @Param("userId") Long userId);

  /**
   * 删除指定应用下某个用户的所有设备绑定关系
   */
  int deleteByUserAndApp(@Param("appId") String appId, @Param("userId") Long userId);

  /**
   * 批量插入用户设备绑定关系
   *
   * @param list 绑定记录列表
   */
  int batchInsert(@Param("list") List<MpUserDevice> list);
}
