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
import cn.universal.admin.system.service.ISysLogininforService;
import cn.universal.persistence.entity.SysLogininfor;
import cn.universal.persistence.mapper.SysLogininforMapper;
import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;

/** 系统访问日志情况信息 服务层处理 @Author ruoyi */
@Service
public class SysLogininforServiceImpl extends BaseServiceImpl implements ISysLogininforService {

  @Resource private SysLogininforMapper sysLogininforMapper;

  @Override
  public List<SysLogininfor> selectPageLogininforList(SysLogininfor logininfor) {
    List<SysLogininfor> sysLogininfors = sysLogininforMapper.selectList(logininfor);
    return sysLogininfors;
  }

  /**
   * 新增系统登录日志
   *
   * @param logininfor 访问日志对象
   */
  @Override
  public void insertLogininfor(SysLogininfor logininfor) {
    logininfor.setLoginTime(new Date());
    sysLogininforMapper.insertSelective(logininfor);
  }

  /**
   * 查询系统登录日志集合
   *
   * @param logininfor 访问日志对象
   * @return 登录记录集合
   */
  @Override
  public List<SysLogininfor> selectLogininforList(SysLogininfor logininfor) {
    List<SysLogininfor> sysLogininfors = sysLogininforMapper.selectList(logininfor);
    return sysLogininfors;
  }

  /**
   * 批量删除系统登录日志
   *
   * @param infoIds 需要删除的登录日志ID
   */
  @Override
  public int deleteLogininforByIds(Long[] infoIds) {
    return sysLogininforMapper.deleteByIds(Arrays.toString(infoIds));
  }

  /** 清空系统登录日志 */
  @Override
  public void cleanLogininfor() {}
}
