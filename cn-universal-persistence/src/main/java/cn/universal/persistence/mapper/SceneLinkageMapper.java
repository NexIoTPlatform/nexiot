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

package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.SceneLinkage;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 场景联动Mapper接口 @Author gitee.com/NexIoT
 *
 * @since 2023-03-01
 */
public interface SceneLinkageMapper extends BaseMapper<SceneLinkage> {

  /** 运行时按租户加载启用规则；无租户时只加载创建人的历史无租户规则。 */
  List<SceneLinkage> selectEnabledSceneLinkageScope(
      @Param("tenantId") String tenantId, @Param("creatorId") String creatorId);

  List<SceneLinkage> selectTriggerByDevId(String deviceId);

  List<SceneLinkage> selectTriggerByType(String deviceId);

  default List<SceneLinkage> selectSceneLinkageListByProductKeyAndDeviceId(
      String productKey, String deviceId) {
    return selectSceneLinkageListByProductKeyAndDeviceId(productKey, deviceId, null, null);
  }

  /**
   * 查询场景联动
   *
   * @param id 场景联动ID
   * @return 场景联动
   */
  SceneLinkage selectSceneLinkageById(Long id);

  default int checkSelf(@Param("id") Long id, @Param("unionId") String unionId) {
    return checkSelf(id, unionId, null);
  }

  int checkSelf(
      @Param("id") Long id, @Param("unionId") String unionId, @Param("tenantId") String tenantId);

  /**
   * 查询场景联动列表
   *
   * @param sceneLinkage 场景联动
   * @return 场景联动集合
   */
  List<SceneLinkage> selectSceneLinkageList(SceneLinkage sceneLinkage);

  /** 查询场景联动列表 */
  List<SceneLinkage> selectSceneLinkageListByProductKeyAndDeviceId(
      @Param("productKey") String productKey,
      @Param("deviceId") String deviceId,
      @Param("tenantId") String tenantId,
      @Param("creatorId") String creatorId);

  /**
   * 新增场景联动
   *
   * @param sceneLinkage 场景联动
   * @return 结果
   */
  int insertSceneLinkage(SceneLinkage sceneLinkage);

  /**
   * 修改场景联动
   *
   * @param sceneLinkage 场景联动
   * @return 结果
   */
  int updateSceneLinkage(SceneLinkage sceneLinkage);

  /**
   * 删除场景联动
   *
   * @param id 场景联动ID
   * @return 结果
   */
  int deleteSceneLinkageById(Long id);

  /**
   * 批量删除场景联动
   *
   * @param ids 需要删除的数据ID
   * @return 结果
   */
  int deleteSceneLinkageByIds(Long[] ids);
}
