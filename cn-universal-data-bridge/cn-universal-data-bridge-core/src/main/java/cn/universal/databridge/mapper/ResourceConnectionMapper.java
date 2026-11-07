/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.databridge.mapper;

import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.persistence.common.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 资源连接Mapper接口
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
public interface ResourceConnectionMapper extends BaseMapper<ResourceConnection> {

  /**
   * 根据资源类型获取活跃连接
   *
   * @param type 资源类型
   * @return 活跃连接列表
   */
  List<ResourceConnection> selectActiveConnectionsByType(@Param("type") String type);

  /**
   * 根据主机和端口查询连接
   *
   * @param host 主机地址
   * @param port 端口号
   * @param excludeId 排除的ID（用于更新时检查）
   * @return 连接
   */
  ResourceConnection selectByHostAndPort(
      @Param("host") String host, @Param("port") Integer port, @Param("excludeId") Long excludeId);

  /**
   * 根据名称查询连接（用于重名检查）
   *
   * @param name 连接名称
   * @param excludeId 排除的ID（用于更新时检查）
   * @return 连接
   */
  ResourceConnection selectByName(@Param("name") String name, @Param("excludeId") Long excludeId);

  /**
   * 批量更新连接状态
   *
   * @param ids 连接ID列表
   * @param status 状态
   * @param updateBy 更新者
   * @return 更新数量
   */
  int batchUpdateStatus(
      @Param("ids") List<Long> ids,
      @Param("status") Integer status,
      @Param("updateBy") String updateBy);

  /**
   * 测试连接可用性（通过查询系统表）
   *
   * @param id 连接ID
   * @return 是否可用
   */
  int testConnection(@Param("id") Long id);
}
