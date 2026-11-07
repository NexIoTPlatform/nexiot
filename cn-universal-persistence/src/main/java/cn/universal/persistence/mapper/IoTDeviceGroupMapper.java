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
import cn.universal.persistence.entity.IoTDeviceGroup;
import cn.universal.persistence.entity.vo.IoTDeviceGroupVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 设备分组Mapper接口
 *
 * @since 2025-12-30
 */
public interface IoTDeviceGroupMapper extends BaseMapper<IoTDeviceGroup> {

  /**
   * 查询设备分组
   *
   * @param id 设备分组ID
   * @return 设备分组
   */
  IoTDeviceGroup selectDevGroupById(Long id);

  /**
   * 查询设备分组列表
   *
   * @param admin 操作者
   * @return
   */
  List<IoTDeviceGroupVO> selectDevGroupList(String admin);

  /**
   * 新增设备分组
   *
   * @param ioTDeviceGroup 设备分组
   * @return 结果
   */
  int insertDevGroup(IoTDeviceGroup ioTDeviceGroup);

  /**
   * 修改设备分组
   *
   * @param ioTDeviceGroup 设备分组
   * @return 结果
   */
  int updateDevGroup(IoTDeviceGroup ioTDeviceGroup);

  /**
   * 删除设备分组
   *
   * @param id 设备分组ID
   * @return 结果
   */
  int deleteDevGroupById(Long id);

  /**
   * 根据分组id获取子节点个数
   *
   * @param id 设备分组ID
   * @return 结果
   */
  int queryChildrenCountById(Long id);

  /**
   * 根据设备id查询设备是否绑定这个账号下，10个以上分组
   *
   * @param iotId
   * @return
   */
  int queryDevCountBindGroup(@Param("iotId") String iotId, @Param("admin") String admin);
}
