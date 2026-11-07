/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 物模型版本历史服务接口
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.service;

import cn.universal.persistence.entity.IoTProductMetadataVersion;
import java.util.List;

/**
 * 物模型版本历史服务接口
 *
 * @since 2026-01-06
 */
public interface IIoTProductMetadataVersionService {

  /**
   * 查询版本列表（条件查询）
   *
   * @param version 查询条件
   * @return 版本列表
   */
  List<IoTProductMetadataVersion> selectVersionList(IoTProductMetadataVersion version);

  /**
   * 根据产品ID查询版本历史列表
   *
   * @param productId 产品ID
   * @return 版本列表
   */
  List<IoTProductMetadataVersion> selectVersionByProductId(Long productId);

  /**
   * 根据产品Key查询版本历史列表
   *
   * @param productKey 产品Key
   * @return 版本列表
   */
  List<IoTProductMetadataVersion> selectVersionByProductKey(String productKey);

  /**
   * 根据ID查询版本
   *
   * @param id 版本ID
   * @return 版本信息
   */
  IoTProductMetadataVersion selectVersionById(Long id);

  /**
   * 获取产品最新版本
   *
   * @param productId 产品ID
   * @return 最新版本
   */
  IoTProductMetadataVersion selectLatestVersion(Long productId);

  /**
   * 创建版本记录
   *
   * @param productId 产品ID
   * @param productKey 产品Key
   * @param metadataSnapshot 物模型快照
   * @param changeType 变更类型
   * @param changeDescription 变更描述
   * @param operator 操作人
   * @return 版本记录
   */
  IoTProductMetadataVersion createVersion(
      Long productId,
      String productKey,
      String metadataSnapshot,
      String changeType,
      String changeDescription,
      String operator);

  /**
   * 从产品创建版本快照
   *
   * @param productId 产品ID
   * @param remark 备注
   * @param operator 操作人
   * @return 版本ID
   */
  Long createSnapshotFromProduct(Long productId, String remark, String operator);

  /**
   * 还原到指定版本
   *
   * @param versionId 版本ID
   * @param targetProductId 目标产品ID
   * @param operator 操作人
   * @return 结果
   */
  int restoreVersion(Long versionId, Long targetProductId, String operator);

  /**
   * 比较两个版本的差异
   *
   * @param versionId1 版本ID1
   * @param versionId2 版本ID2
   * @return 差异信息JSON
   */
  String compareVersions(Long versionId1, Long versionId2);

  /**
   * 获取产品最新的两个版本（用于对比）
   *
   * @param productKey 产品Key
   * @return 版本列表
   */
  List<IoTProductMetadataVersion> selectLatestTwoVersions(String productKey);

  /**
   * 删除版本
   *
   * @param id 版本ID
   * @return 结果
   */
  int deleteVersionById(Long id);

  /**
   * 批量删除版本
   *
   * @param ids 版本ID数组
   * @return 结果
   */
  int deleteVersionByIds(Long[] ids);

  /**
   * 统计产品版本数量
   *
   * @param productId 产品ID
   * @return 版本数量
   */
  int countVersionByProductId(Long productId);

  /**
   * 删除产品的所有版本历史
   *
   * @param productId 产品ID
   * @return 结果
   */
  int deleteVersionByProductId(Long productId);
}
