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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import cn.universal.admin.system.service.ISysDictTypeService;
import cn.universal.common.constant.IoTUserConstants;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.entity.admin.SysDictType;
import cn.universal.persistence.mapper.admin.SysDictDataMapper;
import cn.universal.persistence.mapper.admin.SysDictTypeMapper;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

/** 字典 业务层处理 @Author ruoyi */
@Service
public class SysDictTypeServiceImpl
    implements ISysDictTypeService, ApplicationListener<ApplicationReadyEvent> {

  @Resource private SysDictDataMapper dictDataMapper;
  @Resource private SysDictTypeMapper dictTypeMapper;
  @Resource private SysDictCacheInvalidationCoordinator dictCacheInvalidationCoordinator;

  /** 项目启动时，初始化字典到缓存 */
  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    //    loadingDictCache();
  }

  @Override
  //  @Cacheable(cacheNames = "selectPageDictTypeList", unless = "#result == null", keyGenerator =
  // "redisKeyGenerate")
  public List<SysDictType> selectPageDictTypeList(SysDictType dictType) {
    return dictTypeMapper.selectDictTypeList(dictType);
  }

  /**
   * 根据条件分页查询字典类型
   *
   * @param dictType 字典类型信息
   * @return 字典类型集合信息
   */
  @Override
  //  @Cacheable(cacheNames = "selectDictTypeList", unless = "#result == null", keyGenerator =
  // "redisKeyGenerate")
  public List<SysDictType> selectDictTypeList(SysDictType dictType) {
    return dictTypeMapper.selectDictTypeList(dictType);
  }

  /**
   * 根据所有字典类型
   *
   * @return 字典类型集合信息
   */
  @Override
  @Cacheable(
      cacheNames = "selectDictTypeAll",
      unless = "#result == null",
      keyGenerator = "redisKeyGenerate")
  public List<SysDictType> selectDictTypeAll() {
    return dictTypeMapper.selectAll();
  }

  /**
   * 根据字典类型查询字典数据
   *
   * @param dictType 字典类型
   * @return 字典数据集合信息
   */
  @Override
  @Cacheable(cacheNames = "selectDictDataByType", key = "''+#dictType", unless = "#result == null")
  public List<SysDictData> selectDictDataByType(String dictType) {
    List<SysDictData> dictDatas = dictDataMapper.selectDictDataByType(dictType);
    if (CollUtil.isNotEmpty(dictDatas)) {
      return dictDatas;
    }
    // 使用 ArrayList 而非 List.of()，避免 Jackson GenericJackson2JsonRedisSerializer
    // 对不可变集合(ImmutableCollections$ListN)的空集合序列化/反序列化不兼容问题
    return new ArrayList<>();
  }

  /**
   * 根据字典类型ID查询信息
   *
   * @param dictId 字典类型ID
   * @return 字典类型
   */
  @Override
  @Cacheable(cacheNames = "selectDictTypeById", key = "''+#dictId", unless = "#result == null")
  public SysDictType selectDictTypeById(Long dictId) {
    return dictTypeMapper.selectByPrimaryKey(dictId);
  }

  /**
   * 根据字典类型查询信息
   *
   * @param dictType 字典类型
   * @return 字典类型
   */
  @Override
  @Cacheable(cacheNames = "selectDictTypeByType", key = "''+#dictType", unless = "#result == null")
  public SysDictType selectDictTypeByType(String dictType) {
    Example ex = new Example(SysDictType.class);
    ex.createCriteria().andEqualTo("dictType", dictType);
    return dictTypeMapper.selectOneByExample(ex);
  }

  /**
   * 批量删除字典类型信息
   *
   * @param dictIds 需要删除的字典ID
   * @return 结果
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteDictTypeByIds(Long[] dictIds) {
    List<SysDictType> deletedTypes = new ArrayList<>();
    for (Long dictId : dictIds) {
      SysDictType dictType = dictTypeMapper.selectByPrimaryKey(dictId);
      if (dictType == null) {
        continue;
      }
      Example ex = new Example(SysDictData.class);
      ex.createCriteria().andEqualTo("dictType", dictType.getDictType());
      if (dictDataMapper.selectCountByExample(ex) > 0) {
        throw new IoTException(String.format("%1$s已分配,不能删除", dictType.getDictName()));
      }
      deletedTypes.add(dictType);
    }
    if (deletedTypes.isEmpty()) {
      return;
    }

    List<Long> ids = deletedTypes.stream().map(SysDictType::getDictId).toList();
    String ids1 = ids.stream().map(Object::toString).collect(Collectors.joining(","));
    dictTypeMapper.deleteByIds(ids1);
    dictCacheInvalidationCoordinator.publishDictTypeDeleted(deletedTypes);
  }

  /** 加载字典缓存数据 */
  @Override
  public void loadingDictCache() {
    List<SysDictType> dictTypeList = selectDictTypeAll();
    for (SysDictType dictType : dictTypeList) {
      dictDataMapper.selectDictDataByType(dictType.getDictType());
    }
  }

  /** 清空字典缓存数据 */
  @Override
  public void clearDictCache() {
    dictCacheInvalidationCoordinator.publishRefreshAll();
  }

  /** 重置字典缓存数据 */
  @Override
  public void resetDictCache() {
    dictCacheInvalidationCoordinator.publishRefreshAll();
  }

  /**
   * 新增保存字典类型信息
   *
   * @param dict 字典类型信息
   * @return 结果
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public int insertDictType(SysDictType dict) {
    int row = dictTypeMapper.insert(dict);
    if (row > 0) {
      dictCacheInvalidationCoordinator.publishDictTypeInserted(dict);
    }
    return row;
  }

  /**
   * 修改保存字典类型信息
   *
   * @param dict 字典类型信息
   * @return 结果
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public int updateDictType(SysDictType dict) {
    SysDictType oldDict = dictTypeMapper.selectByPrimaryKey(dict.getDictId());
    if (oldDict == null) {
      return 0;
    }

    Example ex = new Example(SysDictData.class);
    ex.createCriteria().andEqualTo("dictType", oldDict.getDictType());
    List<SysDictData> relatedData = dictDataMapper.selectByExample(ex);

    SysDictData sysDictData = new SysDictData();
    sysDictData.setDictType(dict.getDictType());
    dictDataMapper.updateByExampleSelective(sysDictData, ex);
    int row = dictTypeMapper.updateByPrimaryKey(dict);
    if (row > 0) {
      dictCacheInvalidationCoordinator.publishDictTypeUpdated(oldDict, dict, relatedData);
    }
    return row;
  }

  /**
   * 校验字典类型称是否唯一
   *
   * @param dict 字典类型
   * @return 结果
   */
  @Override
  public String checkDictTypeUnique(SysDictType dict) {
    Long dictId = Validator.isNull(dict.getDictId()) ? -1L : dict.getDictId();
    Example ex = new Example(SysDictType.class);
    ex.createCriteria().andEqualTo("dictType", dict.getDictType());
    List<SysDictType> dictTypes = dictTypeMapper.selectByExample(ex);
    if (!CollectionUtils.isEmpty(dictTypes)
        && dictTypes.get(0).getDictId().longValue() != dictId.longValue()) {
      return IoTUserConstants.NOT_UNIQUE;
    }
    return IoTUserConstants.UNIQUE;
  }
}
