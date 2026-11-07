/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.mapper;

import cn.universal.miniapp.entity.MpUserPermission;
import cn.universal.persistence.common.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 小程序用户权限Mapper
 *
 * @author NexIoT
 * @since 2025/12/15
 */
public interface MpUserPermissionMapper extends BaseMapper<MpUserPermission> {

  /** 查询用户在指定appId下的所有权限 */
  List<String> selectPermissionsByUserAndApp(
      @Param("userId") Long userId, @Param("appId") String appId);

  /** 检查用户是否拥有指定权限 */
  boolean hasPermission(
      @Param("userId") Long userId,
      @Param("appId") String appId,
      @Param("permissionCode") String permissionCode);

  /** 删除用户在指定appId下的所有权限 */
  int deleteByUserAndApp(@Param("userId") Long userId, @Param("appId") String appId);

  /** 批量插入权限 */
  int batchInsert(@Param("permissions") List<MpUserPermission> permissions);
}
