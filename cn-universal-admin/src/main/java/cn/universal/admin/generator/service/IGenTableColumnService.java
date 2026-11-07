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

package cn.universal.admin.generator.service;

import cn.universal.admin.generator.domain.GenTableColumn;
import java.util.List;

/** 业务字段 服务层 @Author ruoyi */
public interface IGenTableColumnService {

  /**
   * 查询业务字段列表
   *
   * @param tableId 业务字段编号
   * @return 业务字段集合
   */
  public List<GenTableColumn> selectGenTableColumnListByTableId(Long tableId);

  /**
   * 新增业务字段
   *
   * @param genTableColumn 业务字段信息
   * @return 结果
   */
  public int insertGenTableColumn(GenTableColumn genTableColumn);

  /**
   * 修改业务字段
   *
   * @param genTableColumn 业务字段信息
   * @return 结果
   */
  public int updateGenTableColumn(GenTableColumn genTableColumn);

  /**
   * 删除业务字段信息
   *
   * @param ids 需要删除的数据ID
   * @return 结果
   */
  public int deleteGenTableColumnByIds(String ids);
}
