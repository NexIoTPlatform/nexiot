/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.service;

import cn.universal.miniapp.config.MpBusinessException;
import cn.universal.miniapp.entity.MpComponentLibrary;
import cn.universal.miniapp.mapper.MpComponentLibraryMapper;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.security.utils.SecurityUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 装修组件库服务
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@Service
public class MpComponentLibraryService {

  @Autowired private MpComponentLibraryMapper componentLibraryMapper;

  /** 查询所有启用的组件 */
  public List<MpComponentLibrary> getAllEnabledComponents() {
    return componentLibraryMapper.selectEnabledComponents();
  }

  /** 根据类型查询组件 */
  public List<MpComponentLibrary> getComponentsByType(String type) {
    return componentLibraryMapper.selectByType(type);
  }

  /** 分页查询组件库（后台） */
  public PageInfo<MpComponentLibrary> queryComponentList(
      String type, Integer status, int pageNum, int pageSize) {
    PageHelper.startPage(pageNum, pageSize);

    MpComponentLibrary query =
        MpComponentLibrary.builder().componentType(type).status(status).build();
    List<MpComponentLibrary> list = componentLibraryMapper.select(query);
    return new PageInfo<>(list);
  }

  /** 获取组件详情 */
  public MpComponentLibrary getComponentById(Long id) {
    return componentLibraryMapper.selectByPrimaryKey(id);
  }

  /** 创建组件 */
  @Transactional(rollbackFor = Exception.class)
  public MpComponentLibrary createComponent(MpComponentLibrary component) {
    assertPlatformAdmin();
    component.setCreateTime(new Date());
    component.setStatus(1);
    componentLibraryMapper.insertSelective(component);
    return component;
  }

  /** 更新组件 */
  @Transactional(rollbackFor = Exception.class)
  public void updateComponent(MpComponentLibrary component) {
    assertPlatformAdmin();
    componentLibraryMapper.updateByPrimaryKeySelective(component);
  }

  /** 删除组件 */
  @Transactional(rollbackFor = Exception.class)
  public void deleteComponent(Long id) {
    assertPlatformAdmin();
    componentLibraryMapper.deleteByPrimaryKey(id);
  }

  /** 启用/禁用组件 */
  @Transactional(rollbackFor = Exception.class)
  public void toggleComponentStatus(Long id, Integer status) {
    assertPlatformAdmin();
    MpComponentLibrary component = new MpComponentLibrary();
    component.setId(id);
    component.setStatus(status);
    componentLibraryMapper.updateByPrimaryKeySelective(component);
  }

  private void assertPlatformAdmin() {
    IoTUser currentUser = SecurityUtils.getIoTUnionUser();
    if (currentUser == null || !currentUser.isAdmin()) {
      throw new MpBusinessException("仅平台管理员可维护组件库");
    }
  }
}
