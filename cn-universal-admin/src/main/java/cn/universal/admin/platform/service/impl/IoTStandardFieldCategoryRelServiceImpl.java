/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 标准字段与分类关联服务实现类
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.service.impl;

import cn.universal.admin.common.service.BaseServiceImpl;
import cn.universal.admin.platform.service.IIoTStandardCategoryService;
import cn.universal.admin.platform.service.IIoTStandardFieldCategoryRelService;
import cn.universal.admin.platform.service.IIoTStandardMetadataFieldService;
import cn.universal.persistence.entity.IoTStandardFieldCategoryRel;
import cn.universal.persistence.mapper.IoTStandardFieldCategoryRelMapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 标准字段与分类关联服务实现类
 *
 * @since 2026-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IoTStandardFieldCategoryRelServiceImpl extends BaseServiceImpl
    implements IIoTStandardFieldCategoryRelService {

  private final IoTStandardFieldCategoryRelMapper relMapper;
  private final IIoTStandardMetadataFieldService fieldService;
  private final IIoTStandardCategoryService categoryService;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public int setFieldCategories(Long fieldId, List<Long> categoryIds) {
    if (fieldId == null) {
      throw new IllegalArgumentException("字段ID不能为空");
    }
    if (fieldService.selectFieldById(fieldId) == null) {
      throw new IllegalArgumentException("字段不存在或无权访问");
    }

    // 删除原有关联
    relMapper.deleteByFieldId(fieldId);

    // 如果新的分类列表为空，则只删除不添加
    if (categoryIds == null || categoryIds.isEmpty()) {
      return 0;
    }

    // 批量插入新的关联
    List<IoTStandardFieldCategoryRel> relations = new ArrayList<>();
    for (Long categoryId : categoryIds) {
      if (categoryService.selectCategoryById(categoryId) == null) {
        throw new IllegalArgumentException("分类不存在或无权访问");
      }
      IoTStandardFieldCategoryRel rel = IoTStandardFieldCategoryRel.builder()
          .fieldId(fieldId)
          .categoryId(categoryId)
          .createTime(new Date())
          .build();
      relations.add(rel);
    }

    return relMapper.batchInsert(relations);
  }

  @Override
  public List<Long> getFieldCategories(Long fieldId) {
    if (fieldId == null) {
      throw new IllegalArgumentException("字段ID不能为空");
    }
    if (fieldService.selectFieldById(fieldId) == null) {
      throw new IllegalArgumentException("字段不存在或无权访问");
    }
    return relMapper.selectCategoryIdsByFieldId(fieldId);
  }

  @Override
  public int deleteByFieldId(Long fieldId) {
    if (fieldId == null) {
      throw new IllegalArgumentException("字段ID不能为空");
    }
    if (fieldService.selectFieldById(fieldId) == null) {
      throw new IllegalArgumentException("字段不存在或无权访问");
    }
    return relMapper.deleteByFieldId(fieldId);
  }

  @Override
  public int deleteByCategoryId(Long categoryId) {
    if (categoryId == null) {
      throw new IllegalArgumentException("分类ID不能为空");
    }
    if (categoryService.selectCategoryById(categoryId) == null) {
      throw new IllegalArgumentException("分类不存在或无权访问");
    }
    return relMapper.deleteByCategoryId(categoryId);
  }
}
