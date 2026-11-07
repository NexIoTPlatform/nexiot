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

package cn.universal.ossm.mapper;

import cn.universal.ossm.entity.SysOss;
import cn.universal.persistence.common.BaseMapper;
import java.util.Collection;
import java.util.List;

/** 文件上传 数据层 @Author Lion Li */
public interface SysOssMapper extends BaseMapper<SysOss> {

  List<SysOss> listByIds(Collection<Long> ids);

  List<SysOss> listByUrls(String[] urls);

  int removeByIds(Collection<Long> ids);

  int removeByUrls(String[] ids);
}
