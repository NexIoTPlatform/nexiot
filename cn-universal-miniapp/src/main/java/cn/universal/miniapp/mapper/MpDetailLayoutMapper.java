/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.mapper;

import cn.universal.miniapp.entity.MpDetailLayout;
import cn.universal.persistence.common.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 设备详情装修配置Mapper
 *
 * @author NexIoT
 * @since 2025/12/15
 */
public interface MpDetailLayoutMapper extends BaseMapper<MpDetailLayout> {

  /** 根据appId和productKey查询布局配置 */
  MpDetailLayout selectByAppIdAndProductKey(
      @Param("appId") String appId, @Param("productKey") String productKey);

  /** 查询appId下所有布局配置 */
  List<MpDetailLayout> selectByAppId(@Param("appId") String appId);

  /** 禁用原有配置 */
  @Update(
      "UPDATE mp_detail_layout SET status = 0 WHERE app_id = #{appId} AND product_key = #{productKey}")
  int disableByAppIdAndProductKey(
      @Param("appId") String appId, @Param("productKey") String productKey);
}
