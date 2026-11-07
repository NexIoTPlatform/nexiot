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

import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.persistence.common.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 数据桥接配置Mapper接口
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
public interface DataBridgeConfigMapper extends BaseMapper<DataBridgeConfig> {

  /**
   * 根据产品KEY获取活跃配置
   *
   * @param productKey 产品KEY
   * @return 活跃配置列表
   */
  List<DataBridgeConfig> selectActiveConfigsByProductKey(@Param("productKey") String productKey);

  /**
   * 根据桥接类型获取配置
   *
   * @param bridgeType 桥接类型
   * @return 配置列表
   */
  List<DataBridgeConfig> selectConfigsByBridgeType(@Param("bridgeType") String bridgeType);

  /**
   * 根据目标资源ID获取配置
   *
   * @param targetResourceId 目标资源ID
   * @return 配置列表
   */
  List<DataBridgeConfig> selectConfigsByTargetResourceId(
      @Param("targetResourceId") Long targetResourceId);

  /**
   * 批量更新配置状态
   *
   * @param ids 配置ID列表
   * @param status 状态
   * @param updateBy 更新者
   * @return 更新数量
   */
  int batchUpdateStatus(
      @Param("ids") List<Long> ids,
      @Param("status") Integer status,
      @Param("updateBy") String updateBy);

  /**
   * 根据名称查询配置（用于重名检查）
   *
   * @param name 配置名称
   * @param excludeId 排除的ID（用于更新时检查）
   * @return 配置
   */
  DataBridgeConfig selectByName(@Param("name") String name, @Param("excludeId") Long excludeId);

  /**
   * 根据创建者查询配置列表
   *
   * @param createBy 创建者
   * @return 配置列表
   */
  List<DataBridgeConfig> selectByCreateBy(@Param("createBy") String createBy);
}
