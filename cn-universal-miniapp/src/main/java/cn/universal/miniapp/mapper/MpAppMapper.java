/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.mapper;

import cn.universal.miniapp.entity.MpApp;
import cn.universal.persistence.common.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 小程序应用Mapper
 *
 * @author NexIoT
 * @since 2025/12/15
 */
public interface MpAppMapper extends BaseMapper<MpApp> {

  /** 根据appId查询应用 */
  MpApp selectByAppId(@Param("appId") String appId);

  /** 根据linkedAppId查询关联的所有小程序应用 */
  List<MpApp> selectByLinkedAppId(@Param("linkedAppId") String linkedAppId);

  /** 查询应用列表（分页） */
  List<MpApp> selectAppList(MpApp query);

  /** 更新应用统计数据 */
  int updateAppStatistics(
      @Param("appId") String appId,
      @Param("userCount") Integer userCount,
      @Param("deviceCount") Integer deviceCount,
      @Param("onlineDeviceCount") Integer onlineDeviceCount);
}
