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

import cn.hutool.core.convert.Convert;
import cn.universal.admin.generator.domain.GenTableColumn;
import cn.universal.admin.generator.mapper.GenTableColumnMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 业务字段 服务层实现 @Author ruoyi */
@Service
public class GenTableColumnServiceImpl implements IGenTableColumnService {

  @Autowired private GenTableColumnMapper genTableColumnMapper;

  /**
   * 查询业务字段列表
   *
   * @param tableId 业务字段编号
   * @return 业务字段集合
   */
  @Override
  public List<GenTableColumn> selectGenTableColumnListByTableId(Long tableId) {
    return genTableColumnMapper.selectGenTableColumnListByTableId(tableId);
  }

  /**
   * 新增业务字段
   *
   * @param genTableColumn 业务字段信息
   * @return 结果
   */
  @Override
  public int insertGenTableColumn(GenTableColumn genTableColumn) {
    return genTableColumnMapper.insertGenTableColumn(genTableColumn);
  }

  /**
   * 修改业务字段
   *
   * @param genTableColumn 业务字段信息
   * @return 结果
   */
  @Override
  public int updateGenTableColumn(GenTableColumn genTableColumn) {
    return genTableColumnMapper.updateGenTableColumn(genTableColumn);
  }

  /**
   * 删除业务字段对象
   *
   * @param ids 需要删除的数据ID
   * @return 结果
   */
  @Override
  public int deleteGenTableColumnByIds(String ids) {
    return genTableColumnMapper.deleteGenTableColumnByIds(Convert.toLongArray(ids));
  }
}
