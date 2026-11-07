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

package cn.universal.admin.system.service;

import cn.universal.persistence.entity.SysLogininfor;
import java.util.List;

/** 系统访问日志情况信息 服务层 @Author ruoyi */
public interface ISysLogininforService {

  List<SysLogininfor> selectPageLogininforList(SysLogininfor logininfor);

  /**
   * 新增系统登录日志
   *
   * @param logininfor 访问日志对象
   */
  public void insertLogininfor(SysLogininfor logininfor);

  /**
   * 查询系统登录日志集合
   *
   * @param logininfor 访问日志对象
   * @return 登录记录集合
   */
  public List<SysLogininfor> selectLogininforList(SysLogininfor logininfor);

  /**
   * 批量删除系统登录日志
   *
   * @param infoIds 需要删除的登录日志ID
   */
  public int deleteLogininforByIds(Long[] infoIds);

  /** 清空系统登录日志 */
  public void cleanLogininfor();
}
