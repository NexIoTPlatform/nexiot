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

package cn.universal.admin.system.service.impl;

import cn.universal.admin.common.service.BaseServiceImpl;
import cn.universal.admin.system.service.IndexQueryService;
import cn.universal.dm.device.service.DashboardScopeSupport;
import cn.universal.persistence.dto.IndexQueryDTO;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.RuleModelBO;
import cn.universal.security.support.DeptDataPermissionSupport.DeptDataPermissionScope;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import cn.universal.persistence.mapper.RuleModelMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/** 首页统计 */
@Service
public class IndexQueryServiceImpl extends BaseServiceImpl implements IndexQueryService {

  @Resource private IoTDeviceMapper ioTDeviceMapper;

  @Resource private RuleModelMapper ruleModelMapper;

  @Resource private IoTProductMapper ioTProductMapper;

  @Resource private IoTUserApplicationMapper iotUserApplicationMapper;

  @Resource private DashboardScopeSupport dashboardScopeSupport;

  @Override
  public IndexQueryDTO queryIndexQty(String creator) {
    IoTUser currentUser = queryIotUser(creator);
    boolean isAdmin = currentUser != null && currentUser.isAdmin();
    DeptDataPermissionScope scope = dashboardScopeSupport.resolveScope(creator, isAdmin);
    IndexQueryDTO qty = new IndexQueryDTO();
    qty.setProduct(Math.toIntExact(dashboardScopeSupport.countProducts(scope, creator, isAdmin)));

    RuleModelBO ruleModel = new RuleModelBO();
    ruleModel.setCreatorId(creator);
    ruleModel.setTenantId(resolveTenantId(creator));
    qty.setRule(ruleModelMapper.selectRuleByBo(ruleModel).size());

    qty.setApps(Math.toIntExact(dashboardScopeSupport.countApplications(scope, creator, isAdmin)));

    qty.setDevice(Math.toIntExact(dashboardScopeSupport.countDevices(scope, creator, isAdmin)));
    qty.setOnline(Math.toIntExact(dashboardScopeSupport.countOnlineDevices(scope, creator, isAdmin)));
    qty.setOffline(qty.getDevice() - qty.getOnline());

    return qty;
  }
}
