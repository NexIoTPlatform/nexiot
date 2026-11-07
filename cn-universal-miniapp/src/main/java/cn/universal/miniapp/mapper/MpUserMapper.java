/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.mapper;

import cn.universal.miniapp.entity.MpUser;
import cn.universal.persistence.common.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 小程序用户Mapper
 *
 * @author NexIoT
 * @since 2025/12/15
 */
public interface MpUserMapper extends BaseMapper<MpUser> {

  /** 根据appId和openId查询用户 */
  MpUser selectByAppIdAndOpenId(@Param("appId") String appId, @Param("openId") String openId);

  /** 根据appId和手机号查询用户 */
  MpUser selectByAppIdAndPhone(@Param("appId") String appId, @Param("phone") String phone);

  /** 根据appId和用户名查询用户 */
  MpUser selectByAppIdAndUsername(@Param("appId") String appId, @Param("username") String username);

  /** 查询用户列表（分页） */
  List<MpUser> selectUserList(MpUser query);

  /** 统计appId下的用户数量 */
  int countByAppId(@Param("appId") String appId);

  /** 更新用户绑定信息 */
  int updateBindInfo(
      @Param("id") Long id,
      @Param("openId") String openId,
      @Param("unionId") String unionId,
      @Param("status") Integer status);
}
