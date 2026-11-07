/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 标准物模型字段服务实现类
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.common.service.BaseServiceImpl;
import cn.universal.admin.platform.service.IIoTStandardMetadataFieldService;
import cn.universal.persistence.entity.IoTStandardMetadataField;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.IoTStandardMetadataFieldMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 标准物模型字段服务实现类
 *
 * @since 2026-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IoTStandardMetadataFieldServiceImpl extends BaseServiceImpl
    implements IIoTStandardMetadataFieldService {

  private final IoTStandardMetadataFieldMapper fieldMapper;

  @Override
  public List<IoTStandardMetadataField> selectFieldList(IoTStandardMetadataField field) {
    applyCurrentTenant(field);
    return fieldMapper.selectFieldList(field);
  }

  @Override
  public List<IoTStandardMetadataField> selectFieldByType(String fieldType) {
    return fieldMapper.selectFieldByType(fieldType, currentTenantId());
  }

  @Override
  public IoTStandardMetadataField selectFieldById(Long id) {
    return fieldMapper.selectFieldById(id, currentTenantId());
  }

  @Override
  public IoTStandardMetadataField selectFieldByFieldId(String fieldId) {
    return fieldMapper.selectFieldByFieldId(fieldId, currentTenantId());
  }

  @Override
  public List<IoTStandardMetadataField> searchFieldByKeyword(String keyword, String fieldType, Integer limit) {
    if (limit == null || limit <= 0) {
      limit = 10;
    }
    
    // 优先使用LIKE搜索，如果没有结果则尝试全文搜索
    List<IoTStandardMetadataField> results =
        fieldMapper.searchFieldByKeyword(fieldType, keyword, limit, currentTenantId());
    
    if (results.isEmpty()) {
      // 尝试全文搜索
      try {
        results = fieldMapper.fullTextSearchField(fieldType, keyword, currentTenantId());
        if (results.size() > limit) {
          results = results.subList(0, limit);
        }
      } catch (Exception e) {
        log.warn("全文搜索失败，使用LIKE搜索结果: {}", e.getMessage());
      }
    }
    
    return results;
  }

  @Override
  public List<IoTStandardMetadataField> fulltextSearch(String keyword, String fieldType, Integer limit) {
    if (limit == null || limit <= 0) {
      limit = 20;
    }
    
    try {
      List<IoTStandardMetadataField> results =
          fieldMapper.fullTextSearchField(fieldType, keyword, currentTenantId());
      if (results.size() > limit) {
        results = results.subList(0, limit);
      }
      return results;
    } catch (Exception e) {
      log.warn("全文搜索失败，回退到LIKE搜索: {}", e.getMessage());
      return fieldMapper.searchFieldByKeyword(fieldType, keyword, limit, currentTenantId());
    }
  }

  @Override
  public List<IoTStandardMetadataField> selectPopularFields(String fieldType, Integer limit) {
    if (limit == null || limit <= 0) {
      limit = 20;
    }
    return fieldMapper.selectPopularFields(fieldType, limit, currentTenantId());
  }

  @Override
  public int insertField(IoTStandardMetadataField field) {
    applyCurrentTenant(field);
    // 默认状态为启用
    if (field.getStatus() == null) {
      field.setStatus(1);
    }
    // 初始化使用次数
    if (field.getUseCount() == null) {
      field.setUseCount(0);
    }
    return fieldMapper.insertField(field);
  }

  @Override
  public int updateField(IoTStandardMetadataField field) {
    applyCurrentTenant(field);
    return fieldMapper.updateField(field);
  }

  @Override
  public int incrementUseCount(Long id) {
    return fieldMapper.incrementUseCount(id, currentTenantId());
  }

  @Override
  public int deleteFieldById(Long id) {
    return fieldMapper.deleteFieldById(id, currentTenantId());
  }

  @Override
  public int deleteFieldByIds(Long[] ids) {
    return fieldMapper.deleteFieldByIds(ids, currentTenantId());
  }

  @Override
  public boolean checkFieldIdUnique(IoTStandardMetadataField field) {
    applyCurrentTenant(field);
    Long excludeId = field.getId() == null ? null : field.getId();
    int count =
        fieldMapper.checkFieldUnique(
            field.getFieldType(), field.getFieldId(), excludeId, field.getTenantId());
    return count == 0;
  }

  @Override
  public List<IoTStandardMetadataField> selectRecommendFieldsByCategory(
      Long categoryId, String fieldType, Integer limit) {
    if (limit == null || limit <= 0) {
      limit = 10; // 默认返回10条
    }
    return fieldMapper.selectRecommendFieldsByCategory(categoryId, fieldType, limit, currentTenantId());
  }

  private void applyCurrentTenant(IoTStandardMetadataField field) {
    if (field == null) {
      return;
    }
    String tenantId = normalizeTenantIdForCurrentUser(field.getTenantId());
    if (StrUtil.isNotBlank(tenantId)) {
      field.setTenantId(tenantId);
    }
  }

  private String currentTenantId() {
    IoTUser currentUser = currentTenantUser();
    if (currentUser == null || currentUser.isAdmin()) {
      return null;
    }
    return resolveTenantId(currentUser);
  }
}
