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
import cn.universal.persistence.entity.SysOperLog;
import java.util.List;

/** 系统访问日志情况信息 数据层 @Author ruoyi */
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {

  List<SysOperLog> selectList(SysOperLog sysOperLog);
}
