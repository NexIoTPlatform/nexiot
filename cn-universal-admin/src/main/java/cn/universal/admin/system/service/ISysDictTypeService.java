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

import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.entity.admin.SysDictType;
import java.util.List;

/** 字典类型业务层接口 */
public interface ISysDictTypeService {

  List<SysDictType> selectPageDictTypeList(SysDictType dictType);

  /**
   * 根据条件分页查询字典类型
   *
   * @param dictType 字典类型信息
   * @return 字典类型集合信息
   */
  public List<SysDictType> selectDictTypeList(SysDictType dictType);

  /**
   * 根据所有字典类型
   *
   * @return 字典类型集合信息
   */
  public List<SysDictType> selectDictTypeAll();

  /**
   * 根据字典类型查询字典数据
   *
   * @param dictType 字典类型
   * @return 字典数据集合信息
   */
  public List<SysDictData> selectDictDataByType(String dictType);

  /**
   * 根据字典类型ID查询信息
   *
   * @param dictId 字典类型ID
   * @return 字典类型
   */
  public SysDictType selectDictTypeById(Long dictId);

  /**
   * 根据字典类型查询信息
   *
   * @param dictType 字典类型
   * @return 字典类型
   */
  public SysDictType selectDictTypeByType(String dictType);

  /**
   * 批量删除字典信息
   *
   * @param dictIds 需要删除的字典ID
   * @return 结果
   */
  public void deleteDictTypeByIds(Long[] dictIds);

  /** 加载字典缓存数据 */
  public void loadingDictCache();

  /** 清空字典缓存数据 */
  public void clearDictCache();

  /** 重置字典缓存数据 */
  public void resetDictCache();

  /**
   * 新增保存字典类型信息
   *
   * @param dictType 字典类型信息
   * @return 结果
   */
  public int insertDictType(SysDictType dictType);

  /**
   * 修改保存字典类型信息
   *
   * @param dictType 字典类型信息
   * @return 结果
   */
  public int updateDictType(SysDictType dictType);

  /**
   * 校验字典类型称是否唯一
   *
   * @param dictType 字典类型
   * @return 结果
   */
  public String checkDictTypeUnique(SysDictType dictType);
}
