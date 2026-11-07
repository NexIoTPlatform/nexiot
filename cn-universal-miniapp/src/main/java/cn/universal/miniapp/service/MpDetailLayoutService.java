/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.universal.miniapp.config.MpBusinessException;
import cn.universal.miniapp.dto.MpLayoutConfigDTO;
import cn.universal.miniapp.entity.MpDetailLayout;
import cn.universal.miniapp.mapper.MpDetailLayoutMapper;
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
 * 设备详情装修配置服务
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@Service
public class MpDetailLayoutService {

  @Autowired private MpDetailLayoutMapper detailLayoutMapper;

  @Autowired private MpAppService mpAppService;

  /** 获取设备详情布局配置 */
  public MpDetailLayout getLayoutConfig(String appId, String productKey) {
    return detailLayoutMapper.selectByAppIdAndProductKey(appId, productKey);
  }

  /** 获取appId下所有布局配置 */
  public List<MpDetailLayout> getLayoutsByAppId(String appId) {
    return detailLayoutMapper.selectByAppId(appId);
  }

  /** 分页查询布局配置（后台） */
  public PageInfo<MpDetailLayout> queryLayoutList(String appId, int pageNum, int pageSize) {
    PageHelper.startPage(pageNum, pageSize);
    List<MpDetailLayout> list = detailLayoutMapper.selectByAppId(appId);
    return new PageInfo<>(list);
  }

  /** 分页查询后台可访问的布局配置 */
  public PageInfo<MpDetailLayout> queryAdminLayoutList(String appId, int pageNum, int pageSize) {
    mpAppService.getAccessibleAdminAppByAppId(appId);
    return queryLayoutList(appId, pageNum, pageSize);
  }

  /** 获取后台可访问的布局配置 */
  public MpDetailLayout getAdminLayoutConfig(String appId, String productKey) {
    mpAppService.getAccessibleAdminAppByAppId(appId);
    return getLayoutConfig(appId, productKey);
  }

  /** 保存布局配置 */
  @Transactional(rollbackFor = Exception.class)
  public MpDetailLayout saveLayoutConfig(MpLayoutConfigDTO dto) {
    String appId = dto.getAppId();
    String productKey = dto.getProductKey();

    // 禁用原有配置
    detailLayoutMapper.disableByAppIdAndProductKey(appId, productKey);

    // 创建新配置
    MpDetailLayout layout =
        MpDetailLayout.builder()
            .appId(appId)
            .productKey(productKey)
            .layoutName(dto.getLayoutName())
            .layoutConfig(JSONUtil.toJsonStr(dto.getLayoutConfig()))
            .status(1)
            .creatorId(SecurityUtils.getUnionId())
            .createTime(new Date())
            .updateTime(new Date())
            .build();

    detailLayoutMapper.insertSelective(layout);
    log.info("保存布局配置: appId={}, productKey={}", appId, productKey);
    return layout;
  }

  /** 保存后台可访问的布局配置 */
  @Transactional(rollbackFor = Exception.class)
  public MpDetailLayout saveAdminLayoutConfig(MpLayoutConfigDTO dto) {
    mpAppService.getAccessibleAdminAppByAppId(dto.getAppId());
    return saveLayoutConfig(dto);
  }

  /** 更新布局配置 */
  @Transactional(rollbackFor = Exception.class)
  public void updateLayoutConfig(Long id, MpLayoutConfigDTO dto) {
    MpDetailLayout layout = detailLayoutMapper.selectByPrimaryKey(id);
    if (layout == null) {
      throw new MpBusinessException("布局配置不存在");
    }

    layout.setLayoutName(dto.getLayoutName());
    layout.setLayoutConfig(JSONUtil.toJsonStr(dto.getLayoutConfig()));
    layout.setUpdateTime(new Date());
    detailLayoutMapper.updateByPrimaryKeySelective(layout);
  }

  /** 更新后台可访问的布局配置 */
  @Transactional(rollbackFor = Exception.class)
  public void updateAdminLayoutConfig(Long id, MpLayoutConfigDTO dto) {
    MpDetailLayout layout = requireAccessibleAdminLayout(id);
    if (dto != null
        && StrUtil.isNotBlank(dto.getAppId())
        && !StrUtil.equals(layout.getAppId(), dto.getAppId())) {
      throw new MpBusinessException("布局配置不属于指定的小程序应用");
    }
    updateLayoutConfig(id, dto);
  }

  /** 删除布局配置 */
  @Transactional(rollbackFor = Exception.class)
  public void deleteLayout(Long id) {
    detailLayoutMapper.deleteByPrimaryKey(id);
  }

  /** 删除后台可访问的布局配置 */
  @Transactional(rollbackFor = Exception.class)
  public void deleteAdminLayout(Long id) {
    requireAccessibleAdminLayout(id);
    deleteLayout(id);
  }

  /** 启用/禁用布局 */
  @Transactional(rollbackFor = Exception.class)
  public void toggleLayoutStatus(Long id, Integer status) {
    MpDetailLayout layout = new MpDetailLayout();
    layout.setId(id);
    layout.setStatus(status);
    layout.setUpdateTime(new Date());
    detailLayoutMapper.updateByPrimaryKeySelective(layout);
  }

  /** 启用/禁用后台可访问的布局 */
  @Transactional(rollbackFor = Exception.class)
  public void toggleAdminLayoutStatus(Long id, Integer status) {
    requireAccessibleAdminLayout(id);
    toggleLayoutStatus(id, status);
  }

  private MpDetailLayout requireAccessibleAdminLayout(Long id) {
    MpDetailLayout layout = detailLayoutMapper.selectByPrimaryKey(id);
    if (layout == null) {
      throw new MpBusinessException("布局配置不存在");
    }
    mpAppService.getAccessibleAdminAppByAppId(layout.getAppId());
    return layout;
  }
}
