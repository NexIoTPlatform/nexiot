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
import cn.universal.persistence.entity.admin.vo.SysDictDataVo;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 字典数据业务层接口 */
public interface ISysDictDataService {

  List<SysDictData> selectPageDictDataList(SysDictDataVo dictData);

  /**
   * 根据条件分页查询字典数据
   *
   * @param dictData 字典数据信息
   * @return 字典数据集合信息
   */
  public List<SysDictData> selectDictDataList(SysDictDataVo dictData);

  /**
   * 根据字典类型和字典键值查询字典数据信息
   *
   * @param dictType 字典类型
   * @param dictValue 字典键值
   * @return 字典标签
   */
  public String selectDictLabel(String dictType, String dictValue);

  /**
   * 根据字典数据ID查询信息
   *
   * @param dictCode 字典数据ID
   * @return 字典数据
   */
  public SysDictData selectDictDataById(Long dictCode);

  /**
   * 批量删除字典数据信息
   *
   * @param dictCodes 需要删除的字典数据ID
   * @return 结果
   */
  public void deleteDictDataByIds(Long[] dictCodes);

  /**
   * 新增保存字典数据信息
   *
   * @param dictData 字典数据信息
   * @return 结果
   */
  public int insertDictData(SysDictData dictData);

  /**
   * 修改保存字典数据信息
   *
   * @param dictData 字典数据信息
   * @return 结果
   */
  public int updateDictData(SysDictData dictData);

  /**
   * 根据字典类型数组查询字典数据信息
   *
   * @param dictType
   * @return
   */
  Map<String, Object> selectDictDataByTypes(String[] dictType);

  /**
   * 根据label标签，查找白名单
   *
   * @param labels
   * @return
   */
  public Set<String> selectWhitelist(List<String> labels);
}
