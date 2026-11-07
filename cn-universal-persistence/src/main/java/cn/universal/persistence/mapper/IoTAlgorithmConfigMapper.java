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
import cn.universal.persistence.entity.IoTAlgorithmConfig;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 算法配置Mapper接口
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/2/27
 */
public interface IoTAlgorithmConfigMapper extends BaseMapper<IoTAlgorithmConfig> {

  /**
   * 根据产品Key查询算法配置列表
   *
   * @param productKey 产品Key
   * @return 算法配置列表
   */
  List<IoTAlgorithmConfig> selectByProductKey(@Param("productKey") String productKey);

  /**
   * 根据产品Key查询启用的算法配置列表
   *
   * @param productKey 产品Key
   * @return 算法配置列表
   */
  List<IoTAlgorithmConfig> selectEnabledByProductKey(@Param("productKey") String productKey);

  /**
   * 根据产品Key和算法类型查询算法配置
   *
   * @param productKey 产品Key
   * @param algorithmType 算法类型
   * @return 算法配置
   */
  IoTAlgorithmConfig selectByProductKeyAndType(
      @Param("productKey") String productKey, @Param("algorithmType") String algorithmType);

  /**
   * 更新算法启用状态
   *
   * @param id 算法配置ID
   * @param enable 是否启用
   * @return 影响行数
   */
  int updateEnableStatus(@Param("id") Long id, @Param("enable") Boolean enable);

  /**
   * 统计指定算法类型已启用的配置数量（任意产品）
   *
   * @param algorithmType 算法类型
   * @return 已启用配置数量
   */
  int countEnabledByAlgorithmType(@Param("algorithmType") String algorithmType);
}
