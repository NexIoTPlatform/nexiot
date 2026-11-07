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
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.entity.vo.IoTUserApplicationVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 用户应用信息Mapper接口
 *
 * @since 2025-12-30
 */
public interface IoTUserApplicationMapper extends BaseMapper<IoTUserApplication> {

  /**
   * 查询用户应用信息
   *
   * @param appUniqueId 用户应用信息ID
   * @return 用户应用信息
   */
  IoTUserApplication selectIotUserApplicationById(String appUniqueId);

  /**
   * 查询用户应用信息
   *
   * @param appid 用户应用信息ID
   * @return 用户应用信息
   */
  IoTUserApplication selectIotUserApplicationByAppId(String appid);

  List<IoTUserApplication> selectIotUserApplicationByUnionId(String unionId);

  /**
   * 按应用唯一标识批量查询。
   *
   * @param appUniqueIds 应用唯一标识数组
   * @return 应用集合
   */
  List<IoTUserApplication> selectIotUserApplicationsByIds(String[] appUniqueIds);

  /**
   * 查询用户应用信息列表
   *
   * @param iotUserApplication 用户应用信息
   * @return 用户应用信息集合
   */
  List<IoTUserApplicationVO> selectIotUserApplicationList(IoTUserApplication iotUserApplication);

  /**
   * 新增用户应用信息
   *
   * @param iotUserApplication 用户应用信息
   * @return 结果
   */
  int insertIotUserApplication(IoTUserApplication iotUserApplication);

  /**
   * 修改用户应用信息
   *
   * @param iotUserApplication 用户应用信息
   * @return 结果
   */
  int updateIotUserApplication(IoTUserApplication iotUserApplication);

  /**
   * 删除用户应用信息
   *
   * @param appUniqueId 用户应用信息ID
   * @return 结果
   */
  int deleteIotUserApplicationById(String appUniqueId);

  /**
   * 批量删除用户应用信息
   *
   * @param appUniqueId 需要删除的数据ID
   * @return 结果
   */
  int deleteIotUserApplicationByIds(String[] appUniqueId);

  /**
   * 统计拥有mq地址的应用数量
   *
   * @param unionId 用户唯一标识
   * @return 结果
   */
  int countMqtt(String unionId);

  List<IoTUserApplicationVO> selectApplicationList(IoTUserApplication application);

  /**
   * 统计全部应用数量（管理员）
   *
   * @return 应用总数
   */
  long countAllApplications();

  /**
   * 统计指定用户的应用数量（普通用户）
   *
   * @param unionId 用户唯一标识
   * @return 应用数量
   */
  long countApplicationsByUnionId(String unionId);

  /** 按统一部门数据权限范围统计应用数量 */
  long countApplicationsByScope(
      @Param("tenantId") String tenantId,
      @Param("scopeUnionIds") List<String> scopeUnionIds,
      @Param("scopeDenied") boolean scopeDenied);
}
