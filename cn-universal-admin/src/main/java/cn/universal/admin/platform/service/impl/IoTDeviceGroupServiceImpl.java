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

package cn.universal.admin.platform.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.universal.admin.common.service.BaseServiceImpl;
import cn.universal.admin.platform.service.IIoTDeviceGroupService;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.persistence.base.IoTDevicePostProcessor;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTDeviceGroup;
import cn.universal.persistence.entity.IoTDeviceTags;
import cn.universal.persistence.entity.bo.IoTDeviceGroupBO;
import cn.universal.persistence.entity.vo.IoTDeviceGroupVO;
import cn.universal.persistence.mapper.IoTDeviceGroupMapper;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTDeviceTagsMapper;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 设备分组 */
@Service
@Slf4j
public class IoTDeviceGroupServiceImpl extends BaseServiceImpl implements IIoTDeviceGroupService {

  @Resource private IoTDeviceGroupMapper ioTDeviceGroupMapper;

  @Resource private IoTDeviceTagsMapper ioTDeviceTagsMapper;

  @Resource private IoTDeviceMapper ioTDeviceMapper;

  @Resource private IoTCacheInvalidationPublisher cacheInvalidationPublisher;

  /**
   * 查询设备分组
   *
   * @param id 设备分组ID
   * @return 设备分组
   */
  @Override
  public IoTDeviceGroupVO selectDevGroupById(Long id) {
    IoTDeviceGroup ioTDeviceGroup = ioTDeviceGroupMapper.selectDevGroupById(id);
    IoTDeviceGroupVO groupVo = BeanUtil.toBean(ioTDeviceGroup, IoTDeviceGroupVO.class);
    // to do 获取当前分组下的设备列表
    return groupVo;
  }

  /**
   * 查询设备分组列表
   *
   * @return 设备分组
   */
  @Override
  public List<IoTDeviceGroupVO> selectDevGroupList() {
    // 查询所有设备分组
    List<IoTDeviceGroupVO> ioTDeviceGroupVOS =
        ioTDeviceGroupMapper.selectDevGroupList(
            queryIotUser(SecurityUtils.getUnionId()).getUnionId());
    // 按照父id分组
    Map<Long, List<IoTDeviceGroupVO>> collect =
        ioTDeviceGroupVOS.stream().collect(Collectors.groupingBy(IoTDeviceGroupVO::getParentId));
    // 创建根节点
    ArrayList<IoTDeviceGroupVO> groupVos = new ArrayList<>();
    IoTDeviceGroupVO ioTDeviceGroupVO =
        IoTDeviceGroupVO.builder().id(0L).groupName("所有群组").groupLevel(0).build();
    groupVos.add(ioTDeviceGroupVO);
    // 生成设备分组树
    List<IoTDeviceGroupVO> roots = collect.get(0L);
    ioTDeviceGroupVO.setChildren(roots);
    groupTree(roots, collect);
    return groupVos;
  }

  /**
   * 新增设备分组
   *
   * @param devGroup 设备分组
   * @return 结果
   */
  @Override
  @Transactional
  public AjaxResult<Void> insertDevGroup(IoTDeviceGroupBO devGroup) {
    // 父节点为0
    if (devGroup.getParentId() == 0) {
      IoTDeviceGroup group = BeanUtil.toBean(devGroup, IoTDeviceGroup.class);
      // 是否有子节点
      group.setHasChild(0);
      // 层级
      group.setGroupLevel(1);
      // 创建者
      group.setCreatorId(queryIotUser(SecurityUtils.getUnionId()).getUnionId());
      // 添加
      int result = ioTDeviceGroupMapper.insert(group);
      if (result >= 1) {
        return AjaxResult.success();
      }
      return AjaxResult.error("添加失败");
    }
    // 获取父亲id
    IoTDeviceGroup parentGroup = ioTDeviceGroupMapper.selectDevGroupById(devGroup.getParentId());
    if (parentGroup == null) {
      return AjaxResult.error("未获取到父群组");
    }
    if (parentGroup.getGroupLevel() >= 5) {
      return AjaxResult.error("群组层级已到达上限");
    }
    IoTDeviceGroup group = BeanUtil.toBean(devGroup, IoTDeviceGroup.class);
    // 是否有子节点
    group.setHasChild(0);
    // 层级
    group.setGroupLevel(parentGroup.getGroupLevel() + 1);
    // 创建者
    group.setCreatorId(queryIotUser(SecurityUtils.getUnionId()).getUnionId());
    // 添加
    int result = ioTDeviceGroupMapper.insert(group);
    if (result >= 1) {
      // 更新父节点
      if (parentGroup.getHasChild() == 0) {
        parentGroup.setHasChild(1);
        ioTDeviceGroupMapper.updateDevGroup(parentGroup);
      }
      return AjaxResult.success();
    }
    return AjaxResult.error("添加失败");
  }

  /**
   * 修改设备分组
   *
   * @param devGroup 设备分组
   * @return 结果
   */
  @Override
  public int updateDevGroup(IoTDeviceGroupBO devGroup) {
    IoTDeviceGroup group = BeanUtil.toBean(devGroup, IoTDeviceGroup.class);
    return ioTDeviceGroupMapper.updateDevGroup(group);
  }

  /**
   * 删除设备分组信息
   *
   * @param id 设备分组ID
   * @return 结果
   */
  @Override
  @Transactional
  public AjaxResult<Void> deleteDevGroupById(Long id) {
    // 查询分组的子集
    int total = ioTDeviceGroupMapper.queryChildrenCountById(id);
    if (total >= 1) {
      return AjaxResult.error("存在子分组，无法删除");
    }
    // 分组当前有设备无法被删除
    int devCount = ioTDeviceTagsMapper.selectDevIds(String.valueOf(id));
    if (devCount > 0) {
      return AjaxResult.error("分组下绑定有设备，无法删除");
    }
    // 获取当前分组实例
    IoTDeviceGroup ioTDeviceGroup = ioTDeviceGroupMapper.selectDevGroupById(id);
    // 删除当前分组
    int result = ioTDeviceGroupMapper.deleteDevGroupById(id);
    if (result >= 1) {
      // 产出当前分组
      int count = ioTDeviceGroupMapper.queryChildrenCountById(ioTDeviceGroup.getParentId());
      // 清除分组中已删除的设备
      ioTDeviceTagsMapper.deleteByValueId(String.valueOf(id));
      // 如果父子集为0，更改状态
      if (count == 0) {
        ioTDeviceGroupMapper.updateDevGroup(
            IoTDeviceGroup.builder().id(ioTDeviceGroup.getParentId()).hasChild(0).build());
      }
      return AjaxResult.success();
    }
    return AjaxResult.error("添加失败");
  }

  /**
   * 绑定设备到分组 iot_dev_action
   *
   * @param devGroup
   * @return
   */
  @Override
  public AjaxResult<Void> bindDevToGroup(IoTDeviceGroupBO devGroup) {
    // 判断分组是否存在
    Long groupId = devGroup.getId();
    if (groupId == null) {
      return AjaxResult.error("分组序号不能为空");
    }
    IoTDeviceGroup group = ioTDeviceGroupMapper.selectDevGroupById(groupId);
    if (group == null) {
      return AjaxResult.error("要绑定的分组不存在");
    }
    // 获取设备id集合; 清除已经添加到此分组的设备id
    ArrayList<String> devIdsNewOne = new ArrayList<>();
    ArrayList<IoTDeviceTags> ioTDeviceTags = new ArrayList<>();
    StringBuffer successMsg = new StringBuffer();
    StringBuffer errorMsg = new StringBuffer();
    errorMsg.append("设备绑定分组数超过10个，");
    successMsg.append("绑定成功; 设备绑定分组数超过10个，");
    List<String> oldIotIds = Arrays.asList(devGroup.getDevIds());
    for (String iotId : oldIotIds) {
      IoTDeviceTags one =
          ioTDeviceTagsMapper.selectOne(
              IoTDeviceTags.builder().iotId(iotId).value(String.valueOf(groupId)).build());
      if (one == null) {
        devIdsNewOne.add(iotId);
      }
    }
    if (devIdsNewOne.size() == 0) {
      return AjaxResult.success("设备已绑定在该分组下");
    }
    // 清除超过或等于10个分组的设备
    for (String iotId : devIdsNewOne) {
      int count = ioTDeviceGroupMapper.queryDevCountBindGroup(iotId, SecurityUtils.getUnionId());
      HashMap<String, Object> map = new HashMap<>();
      map.put("iot_id", iotId);
      IoTDeviceDTO deviceDTO = ioTDeviceMapper.selectIoTDeviceBO(map);
      if (deviceDTO == null) {
        log.warn("设备不存在,跳过编组");
        continue;
      }
      if (count < 10) {
        ioTDeviceTags.add(
            IoTDeviceTags.builder()
                .iotId(iotId)
                .name(group.getGroupName())
                .productKey(deviceDTO.getProductKey())
                .deviceId(deviceDTO.getDeviceId())
                .type("devGroup")
                .key("devGroup")
                .value(String.valueOf(groupId))
                .build());
      } else {
        successMsg.append(deviceDTO.getDeviceName() + "、");
        errorMsg.append(deviceDTO.getDeviceName() + "、");
      }
    }
    if (ioTDeviceTags.size() == 0) {
      errorMsg.append("设备无法被绑定");
      return AjaxResult.error(errorMsg.toString());
    }
    // 添加
    int i = ioTDeviceTagsMapper.insertList(ioTDeviceTags);
    if (i >= 1) {
      publishDeviceUpdates(
          ioTDeviceTags.stream().map(IoTDeviceTags::getIotId).collect(Collectors.toSet()));
      if (devIdsNewOne.size() == ioTDeviceTags.size()) {
        return AjaxResult.success("绑定成功");
      } else {
        successMsg.append("设备无法被绑定");
        return AjaxResult.success(successMsg.toString());
      }
    }

    return AjaxResult.error("绑定失败");
  }

  /**
   * 根据分组id查询分组下的所有设备
   *
   * @param groupId
   * @return
   */
  @Override
  public List<IoTDevice> selectDevInstanceListByGroupId(String groupId) {
    List<IoTDevice> ioTDevices = ioTDeviceMapper.selectDevListByIds(groupId);
    return ioTDevices;
  }

  /**
   * 设备解绑
   *
   * @param groupId 分组id
   * @param devId 设备id
   * @return
   */
  @Override
  @Transactional
  public AjaxResult<Void> unBindDevByGroupId(String groupId, String[] devId) {
    for (int i = 0; i < devId.length; i++) {
      ioTDeviceTagsMapper.delete(IoTDeviceTags.builder().iotId(devId[i]).value(groupId).build());
    }
    publishDeviceUpdates(Arrays.asList(devId));
    return AjaxResult.success();
  }

  private void publishDeviceUpdates(Iterable<String> iotIds) {
    for (String iotId : iotIds) {
      IoTDeviceDTO deviceDTO = ioTDeviceMapper.selectIoTDeviceBO(Map.of("iotId", iotId));
      if (deviceDTO != null) {
        cacheInvalidationPublisher.publishDeviceChange(
            IoTDevicePostProcessor.Operation.UPDATE, deviceDTO);
      }
    }
  }

  /** 生成设备分组列表 */
  private void groupTree(
      List<IoTDeviceGroupVO> ioTDeviceGroupVO, Map<Long, List<IoTDeviceGroupVO>> groups) {
    if (ioTDeviceGroupVO == null) {
      return;
    }
    for (int i = 0; i < ioTDeviceGroupVO.size(); i++) {
      if (ioTDeviceGroupVO.get(i).getHasChild() == 1) {
        List<IoTDeviceGroupVO> devGroups = groups.get(ioTDeviceGroupVO.get(i).getId());
        ioTDeviceGroupVO.get(i).setChildren(devGroups);
        groupTree(devGroups, groups);
      }
    }
    return;
  }
}
