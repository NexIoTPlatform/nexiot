/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 物模型版本历史Mapper接口
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTProductMetadataVersion;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 物模型版本历史Mapper接口
 *
 * @since 2026-01-06
 */
public interface IoTProductMetadataVersionMapper extends BaseMapper<IoTProductMetadataVersion> {

  /**
   * 条件查询版本列表
   *
   * @param version 查询条件
   * @return 版本列表
   */
  List<IoTProductMetadataVersion> selectVersionList(IoTProductMetadataVersion version);

  /**
   * 查询产品的版本历史列表
   *
   * @param productId 产品ID
   * @return 版本列表
   */
  List<IoTProductMetadataVersion> selectVersionByProductId(@Param("productId") Long productId);

  /**
   * 查询产品的版本历史列表（按productKey）
   *
   * @param productKey 产品Key
   * @return 版本列表
   */
  List<IoTProductMetadataVersion> selectVersionListByProductKey(@Param("productKey") String productKey);

  /**
   * 根据ID查询版本
   *
   * @param id 版本ID
   * @return 版本信息
   */
  IoTProductMetadataVersion selectVersionById(@Param("id") Long id);

  /**
   * 获取产品最新版本
   *
   * @param productId 产品ID
   * @return 最新版本
   */
  IoTProductMetadataVersion selectLatestVersion(@Param("productId") Long productId);

  /**
   * 根据产品ID和版本号查询版本
   *
   * @param productId 产品ID
   * @param version 版本号
   * @return 版本信息
   */
  IoTProductMetadataVersion selectVersionByProductIdAndVersion(@Param("productId") Long productId, @Param("version") Integer version);

  /**
   * 获取产品的最新版本号
   *
   * @param productId 产品ID
   * @return 最新版本号
   */
  Integer selectMaxVersionByProductId(@Param("productId") Long productId);

  /**
   * 新增版本
   *
   * @param version 版本信息
   * @return 结果
   */
  int insertVersion(IoTProductMetadataVersion version);

  /**
   * 删除版本
   *
   * @param id 版本ID
   * @return 结果
   */
  int deleteVersionById(@Param("id") Long id);

  /**
   * 批量删除版本
   *
   * @param ids 版本ID数组
   * @return 结果
   */
  int deleteVersionByIds(@Param("ids") Long[] ids);

  /**
   * 删除产品的所有版本
   *
   * @param productId 产品ID
   * @return 结果
   */
  int deleteVersionByProductId(@Param("productId") Long productId);

  /**
   * 获取产品最新的两个版本（用于对比）
   *
   * @param productId 产品ID
   * @return 版本列表
   */
  List<IoTProductMetadataVersion> selectLatestTwoVersions(@Param("productId") Long productId);

  /**
   * 统计产品的版本数量
   *
   * @param productId 产品ID
   * @return 版本数量
   */
  int countVersionByProductId(@Param("productId") Long productId);
}
