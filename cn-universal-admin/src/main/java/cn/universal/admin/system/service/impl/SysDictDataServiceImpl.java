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

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.admin.system.service.ISysDictDataService;
import cn.universal.admin.system.service.ISysDictTypeService;
import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.entity.admin.vo.SysDictDataVo;
import cn.universal.persistence.entity.admin.vo.SysDictDataVos;
import cn.universal.persistence.mapper.admin.SysDictDataMapper;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.entity.Example.Criteria;

/** 字典 业务层处理 @Author ruoyi */
@Service
public class SysDictDataServiceImpl implements ISysDictDataService {

  @Resource private SysDictDataMapper sysDictDataMapper;

  @Autowired private ISysDictTypeService dictTypeService;

  @Resource private SysDictCacheInvalidationCoordinator dictCacheInvalidationCoordinator;

  @Override
  //  @Cacheable(cacheNames = "selectPageDictDataList", unless = "#result == null", keyGenerator =
  // "redisKeyGenerate")
  public List<SysDictData> selectPageDictDataList(SysDictDataVo dictData) {
    return sysDictDataMapper.selectDictData(dictData);
  }

  /**
   * 根据条件分页查询字典数据
   *
   * @param dictData 字典数据信息
   * @return 字典数据集合信息
   */
  @Override
  //  @Cacheable(cacheNames = "selectDictDataList", unless = "#result == null", keyGenerator =
  // "redisKeyGenerate")
  public List<SysDictData> selectDictDataList(SysDictDataVo dictData) {
    return sysDictDataMapper.selectDictData(dictData);
  }

  /**
   * 根据字典类型和字典键值查询字典数据信息
   *
   * @param dictType 字典类型
   * @param dictValue 字典键值
   * @return 字典标签
   */
  @Override
  @Cacheable(
      cacheNames = "selectDictLabel",
      key = "''+#dictType+':'+#dictValue",
      unless = "#result == null")
  public String selectDictLabel(String dictType, String dictValue) {
    Example ex = new Example(SysDictData.class);
    ex.createCriteria().andEqualTo("dictType", dictType).andEqualTo("dictValue", dictValue);
    SysDictData sysDictData = sysDictDataMapper.selectOneByExample(ex);
    return sysDictData == null ? null : sysDictData.getDictLabel();
  }

  /**
   * 根据字典数据ID查询信息
   *
   * @param dictCode 字典数据ID
   * @return 字典数据
   */
  @Override
  @Cacheable(cacheNames = "selectDictDataById", key = "''+#dictCode", unless = "#result == null")
  public SysDictData selectDictDataById(Long dictCode) {
    return sysDictDataMapper.selectByPrimaryKey(dictCode);
  }

  /**
   * 批量删除字典数据信息
   *
   * @param dictCodes 需要删除的字典数据ID
   * @return 结果
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteDictDataByIds(Long[] dictCodes) {
    List<SysDictData> deletedDataList = new ArrayList<>();
    for (Long dictCode : dictCodes) {
      SysDictData data = sysDictDataMapper.selectByPrimaryKey(dictCode);
      if (data != null) {
        deletedDataList.add(data);
      }
      sysDictDataMapper.deleteByPrimaryKey(dictCode);
    }
    dictCacheInvalidationCoordinator.publishDictDataDeleted(deletedDataList);
  }

  /**
   * 新增保存字典数据信息
   *
   * @param data 字典数据信息
   * @return 结果
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public int insertDictData(SysDictData data) {
    int row = sysDictDataMapper.insert(data);
    if (row > 0) {
      dictCacheInvalidationCoordinator.publishDictDataInserted(data);
    }
    return row;
  }

  /**
   * 修改保存字典数据信息
   *
   * @param data 字典数据信息
   * @return 结果
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public int updateDictData(SysDictData data) {
    SysDictData oldData = sysDictDataMapper.selectByPrimaryKey(data.getDictCode());
    int row = sysDictDataMapper.updateByPrimaryKey(data);
    if (row > 0) {
      dictCacheInvalidationCoordinator.publishDictDataUpdated(oldData, data);
    }
    return row;
  }

  /** 根据字典类型数组查询字典数据信息 */
  @Override
  public Map<String, Object> selectDictDataByTypes(String[] dictType) {
    // 聚合结果直接复用按类型缓存，避免额外一层组合缓存导致大范围失效。
    Map<String, Object> data = new HashMap<>();
    if (dictType == null || dictType.length == 0) {
      return data;
    }
    Set<String> normalizedTypes = new LinkedHashSet<>();
    for (String type : dictType) {
      if (StrUtil.isNotBlank(type)) {
        normalizedTypes.add(type.trim());
      }
    }
    for (String type : normalizedTypes) {
      if (!"model_unit".equals(type)) {
        data.put(type, dictTypeService.selectDictDataByType(type));
      } else {
        List<SysDictData> unitDictData = dictTypeService.selectDictDataByType(type);
        List<SysDictDataVos> dataVos = new ArrayList<>();
        if (CollectionUtil.isEmpty(unitDictData)) {
          data.put(type, dataVos);
          continue;
        }
        for (SysDictData dict : unitDictData) {
          SysDictDataVos dataVo = new SysDictDataVos();
          List<SysDictData> dictData = dictTypeService.selectDictDataByType(dict.getDictLabel());
          dataVo.setId(dict.getDictLabel());
          dataVo.setName(dict.getDictValue());
          if (dictData != null && dictData.size() > 0) {
            dataVo.setArray(dictData);
          }
          dataVos.add(dataVo);
        }
        data.put(type, dataVos);
      }
    }
    return data;
  }

  @Override
  @Cacheable(
      cacheNames = "selectWhitelist",
      keyGenerator = "redisKeyGenerate",
      unless = "#result == null")
  public Set<String> selectWhitelist(List<String> labels) {
    Example ex = new Example(SysDictData.class);
    Criteria criteria = ex.createCriteria().andEqualTo("dictType", "whitelist");
    if (CollectionUtil.isNotEmpty(labels)) {
      criteria.andIn("dictLabel", labels);
    }
    List<SysDictData> sysDictData = sysDictDataMapper.selectByExample(ex);
    Set<String> whitelist = new HashSet<>();
    if (CollectionUtil.isNotEmpty(sysDictData)) {
      sysDictData.stream()
          .filter(s -> StrUtil.isNotBlank(s.getDictValue()))
          .forEach(
              s -> {
                String[] ls = s.getDictValue().split(",");
                for (String v : ls) {
                  whitelist.add(v);
                }
              });
    }
    return whitelist;
  }
}
