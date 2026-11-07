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

import cn.hutool.core.util.StrUtil;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.entity.admin.SysDictType;
import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 字典缓存失效协调器
 *
 * <p>将 sysDict 的写操作收敛为精确的缓存 key 驱逐，并复用统一的异步失效事件机制。
 */
@Component
public class SysDictCacheInvalidationCoordinator {

  private static final String CACHE_DICT_DATA_BY_ID = "selectDictDataById";
  private static final String CACHE_DICT_LABEL = "selectDictLabel";
  private static final String CACHE_DICT_DATA_BY_TYPE = "selectDictDataByType";
  private static final String CACHE_DICT_TYPE_ALL = "selectDictTypeAll";
  private static final String CACHE_DICT_TYPE_BY_ID = "selectDictTypeById";
  private static final String CACHE_DICT_TYPE_BY_TYPE = "selectDictTypeByType";
  private static final String CACHE_WHITELIST = "selectWhitelist";

  private static final String WHITELIST_DICT_TYPE = "whitelist";

  @Resource private IoTCacheInvalidationPublisher cacheInvalidationPublisher;

  public void publishDictDataInserted(SysDictData data) {
    if (data == null) {
      return;
    }

    Set<String> cacheNames = new LinkedHashSet<>();
    Map<String, Set<Object>> cacheKeys = new LinkedHashMap<>();

    addCacheKey(
        cacheKeys, CACHE_DICT_LABEL, buildDictLabelKey(data.getDictType(), data.getDictValue()));
    addCacheKey(cacheKeys, CACHE_DICT_DATA_BY_TYPE, buildSimpleKey(data.getDictType()));
    addWhitelistCache(cacheNames, data.getDictType());

    publish("sys-dict-data-insert", cacheNames, cacheKeys);
  }

  public void publishDictDataUpdated(SysDictData oldData, SysDictData newData) {
    if (oldData == null && newData == null) {
      return;
    }

    Set<String> cacheNames = new LinkedHashSet<>();
    Map<String, Set<Object>> cacheKeys = new LinkedHashMap<>();

    SysDictData effectiveOldData = oldData != null ? oldData : newData;
    SysDictData effectiveNewData = newData != null ? newData : oldData;

    addCacheKey(cacheKeys, CACHE_DICT_DATA_BY_ID, buildSimpleKey(effectiveOldData.getDictCode()));
    addCacheKey(
        cacheKeys,
        CACHE_DICT_LABEL,
        buildDictLabelKey(effectiveOldData.getDictType(), effectiveOldData.getDictValue()));
    addCacheKey(
        cacheKeys,
        CACHE_DICT_LABEL,
        buildDictLabelKey(effectiveNewData.getDictType(), effectiveNewData.getDictValue()));
    addCacheKey(cacheKeys, CACHE_DICT_DATA_BY_TYPE, buildSimpleKey(effectiveOldData.getDictType()));
    addCacheKey(cacheKeys, CACHE_DICT_DATA_BY_TYPE, buildSimpleKey(effectiveNewData.getDictType()));

    addWhitelistCache(cacheNames, effectiveOldData.getDictType());
    addWhitelistCache(cacheNames, effectiveNewData.getDictType());

    publish("sys-dict-data-update", cacheNames, cacheKeys);
  }

  public void publishDictDataDeleted(Collection<SysDictData> deletedDataList) {
    if (deletedDataList == null || deletedDataList.isEmpty()) {
      return;
    }

    Set<String> cacheNames = new LinkedHashSet<>();
    Map<String, Set<Object>> cacheKeys = new LinkedHashMap<>();

    for (SysDictData dictData : deletedDataList) {
      if (dictData == null) {
        continue;
      }
      addCacheKey(cacheKeys, CACHE_DICT_DATA_BY_ID, buildSimpleKey(dictData.getDictCode()));
      addCacheKey(
          cacheKeys,
          CACHE_DICT_LABEL,
          buildDictLabelKey(dictData.getDictType(), dictData.getDictValue()));
      addCacheKey(cacheKeys, CACHE_DICT_DATA_BY_TYPE, buildSimpleKey(dictData.getDictType()));
      addWhitelistCache(cacheNames, dictData.getDictType());
    }

    publish("sys-dict-data-delete", cacheNames, cacheKeys);
  }

  public void publishDictTypeInserted(SysDictType dictType) {
    if (dictType == null) {
      return;
    }

    Set<String> cacheNames = new LinkedHashSet<>();
    cacheNames.add(CACHE_DICT_TYPE_ALL);

    publish("sys-dict-type-insert", cacheNames, new LinkedHashMap<>());
  }

  public void publishDictTypeDeleted(Collection<SysDictType> deletedTypes) {
    if (deletedTypes == null || deletedTypes.isEmpty()) {
      return;
    }

    Set<String> cacheNames = new LinkedHashSet<>();
    Map<String, Set<Object>> cacheKeys = new LinkedHashMap<>();
    cacheNames.add(CACHE_DICT_TYPE_ALL);

    for (SysDictType dictType : deletedTypes) {
      if (dictType == null) {
        continue;
      }
      addCacheKey(cacheKeys, CACHE_DICT_TYPE_BY_ID, buildSimpleKey(dictType.getDictId()));
      addCacheKey(cacheKeys, CACHE_DICT_TYPE_BY_TYPE, buildSimpleKey(dictType.getDictType()));
      addWhitelistCache(cacheNames, dictType.getDictType());
    }

    publish("sys-dict-type-delete", cacheNames, cacheKeys);
  }

  public void publishDictTypeUpdated(
      SysDictType oldDictType, SysDictType newDictType, Collection<SysDictData> relatedData) {
    if (oldDictType == null && newDictType == null) {
      return;
    }

    Set<String> cacheNames = new LinkedHashSet<>();
    Map<String, Set<Object>> cacheKeys = new LinkedHashMap<>();

    SysDictType effectiveOldType = oldDictType != null ? oldDictType : newDictType;
    SysDictType effectiveNewType = newDictType != null ? newDictType : oldDictType;

    cacheNames.add(CACHE_DICT_TYPE_ALL);
    addCacheKey(cacheKeys, CACHE_DICT_TYPE_BY_ID, buildSimpleKey(effectiveOldType.getDictId()));
    addCacheKey(cacheKeys, CACHE_DICT_TYPE_BY_TYPE, buildSimpleKey(effectiveOldType.getDictType()));
    addCacheKey(cacheKeys, CACHE_DICT_TYPE_BY_TYPE, buildSimpleKey(effectiveNewType.getDictType()));
    addCacheKey(cacheKeys, CACHE_DICT_DATA_BY_TYPE, buildSimpleKey(effectiveOldType.getDictType()));
    addCacheKey(cacheKeys, CACHE_DICT_DATA_BY_TYPE, buildSimpleKey(effectiveNewType.getDictType()));

    addWhitelistCache(cacheNames, effectiveOldType.getDictType());
    addWhitelistCache(cacheNames, effectiveNewType.getDictType());

    if (relatedData != null) {
      for (SysDictData dictData : relatedData) {
        if (dictData == null) {
          continue;
        }
        addCacheKey(cacheKeys, CACHE_DICT_DATA_BY_ID, buildSimpleKey(dictData.getDictCode()));
        addCacheKey(
            cacheKeys,
            CACHE_DICT_LABEL,
            buildDictLabelKey(effectiveOldType.getDictType(), dictData.getDictValue()));
        addCacheKey(
            cacheKeys,
            CACHE_DICT_LABEL,
            buildDictLabelKey(effectiveNewType.getDictType(), dictData.getDictValue()));
      }
    }

    publish("sys-dict-type-update", cacheNames, cacheKeys);
  }

  public void publishRefreshAll() {
    Set<String> cacheNames = new LinkedHashSet<>();
    cacheNames.add(CACHE_DICT_TYPE_ALL);
    cacheNames.add(CACHE_DICT_DATA_BY_TYPE);
    cacheNames.add(CACHE_DICT_TYPE_BY_ID);
    cacheNames.add(CACHE_DICT_TYPE_BY_TYPE);
    cacheNames.add(CACHE_DICT_LABEL);
    cacheNames.add(CACHE_DICT_DATA_BY_ID);
    cacheNames.add(CACHE_WHITELIST);

    publish("sys-dict-refresh-all", cacheNames, new LinkedHashMap<>());
  }

  private void publish(String reason, Set<String> cacheNames, Map<String, Set<Object>> cacheKeys) {
    if ((cacheNames == null || cacheNames.isEmpty()) && (cacheKeys == null || cacheKeys.isEmpty())) {
      return;
    }
    cacheInvalidationPublisher.publishCustomCacheChange(reason, cacheNames, cacheKeys);
  }

  private void addWhitelistCache(Set<String> cacheNames, String dictType) {
    if (StrUtil.equals(WHITELIST_DICT_TYPE, dictType)) {
      cacheNames.add(CACHE_WHITELIST);
    }
  }

  private void addCacheKey(Map<String, Set<Object>> cacheKeys, String cacheName, Object key) {
    if (StrUtil.isBlank(cacheName) || key == null) {
      return;
    }
    cacheKeys.computeIfAbsent(cacheName, ignored -> new LinkedHashSet<>()).add(key);
  }

  private Object buildSimpleKey(Object value) {
    if (value == null) {
      return null;
    }
    String key = String.valueOf(value);
    return StrUtil.isBlank(key) ? null : key;
  }

  private String buildDictLabelKey(String dictType, String dictValue) {
    if (StrUtil.isBlank(dictType) || StrUtil.isBlank(dictValue)) {
      return null;
    }
    return dictType + ":" + dictValue;
  }
}
