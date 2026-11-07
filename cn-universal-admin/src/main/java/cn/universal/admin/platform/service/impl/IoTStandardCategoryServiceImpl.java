/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 公共物模型分类服务实现类
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.common.service.BaseServiceImpl;
import cn.universal.admin.platform.service.IIoTStandardCategoryService;
import cn.universal.persistence.entity.IoTStandardCategory;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.IoTStandardCategoryMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 公共物模型分类服务实现类
 *
 * @since 2026-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IoTStandardCategoryServiceImpl extends BaseServiceImpl
    implements IIoTStandardCategoryService {

  private final IoTStandardCategoryMapper categoryMapper;

  @Override
  public List<IoTStandardCategory> selectCategoryList(IoTStandardCategory category) {
    applyCurrentTenant(category);
    return categoryMapper.selectCategoryList(category);
  }

  @Override
  public List<IoTStandardCategory> selectCategoryTree() {
    IoTStandardCategory query = new IoTStandardCategory();
    applyCurrentTenant(query);
    List<IoTStandardCategory> allCategories = categoryMapper.selectCategoryList(query);
    return buildCategoryTree(allCategories, 0L);
  }

  /**
   * 构建分类树
   */
  private List<IoTStandardCategory> buildCategoryTree(List<IoTStandardCategory> categories, Long parentId) {
    return categories.stream()
        .filter(c -> parentId.equals(c.getParentId()))
        .peek(c -> {
          List<IoTStandardCategory> children = buildCategoryTree(categories, c.getId());
          c.setChildren(children);
          c.setHasChildren(!children.isEmpty());
        })
        .collect(Collectors.toList());
  }

  @Override
  public IoTStandardCategory selectCategoryById(Long id) {
    return categoryMapper.selectCategoryById(id, currentTenantId());
  }

  @Override
  public IoTStandardCategory selectCategoryByKey(String categoryKey) {
    return categoryMapper.selectCategoryByKey(categoryKey, currentTenantId());
  }

  @Override
  public List<IoTStandardCategory> selectChildrenByParentId(Long parentId) {
    String tenantId = currentTenantId();
    List<IoTStandardCategory> children = categoryMapper.selectChildrenByParentId(parentId, tenantId);
    // 设置是否有子节点
    children.forEach(c -> {
      int count = categoryMapper.countChildrenByParentId(c.getId(), tenantId);
      c.setHasChildren(count > 0);
    });
    return children;
  }

  @Override
  public List<IoTStandardCategory> selectThirdLevelCategories() {
    return categoryMapper.selectCategoryByLevel(3, currentTenantId());
  }

  @Override
  public int insertCategory(IoTStandardCategory category) {
    applyCurrentTenant(category);
    // 自动计算层级
    if (category.getParentId() != null && category.getParentId() > 0) {
      IoTStandardCategory parent =
          categoryMapper.selectCategoryById(category.getParentId(), currentTenantId());
      if (parent != null) {
        category.setLevel(parent.getLevel() + 1);
      }
    } else {
      category.setParentId(0L);
      category.setLevel(1);
    }
    
    // 默认状态为启用
    if (category.getStatus() == null) {
      category.setStatus(1);
    }
    
    return categoryMapper.insertCategory(category);
  }

  @Override
  public int updateCategory(IoTStandardCategory category) {
    applyCurrentTenant(category);
    return categoryMapper.updateCategory(category);
  }

  @Override
  public int deleteCategoryById(Long id) {
    return categoryMapper.deleteCategoryById(id, currentTenantId());
  }

  @Override
  public int deleteCategoryByIds(Long[] ids) {
    return categoryMapper.deleteCategoryByIds(ids, currentTenantId());
  }

  @Override
  public boolean checkCategoryKeyUnique(IoTStandardCategory category) {
    applyCurrentTenant(category);
    Long excludeId = category.getId() == null ? null : category.getId();
    int count =
        categoryMapper.checkCategoryKeyUnique(category.getCategoryKey(), excludeId, category.getTenantId());
    return count == 0;
  }

  @Override
  public boolean hasChildren(Long id) {
    return categoryMapper.countChildrenByParentId(id, currentTenantId()) > 0;
  }

  @Override
  public String getCategoryPath(Long categoryId) {
    List<String> pathNames = new ArrayList<>();
    String tenantId = currentTenantId();
    IoTStandardCategory category = categoryMapper.selectCategoryById(categoryId, tenantId);
    
    while (category != null) {
      pathNames.add(0, category.getCategoryName());
      if (category.getParentId() != null && category.getParentId() > 0) {
        category = categoryMapper.selectCategoryById(category.getParentId(), tenantId);
      } else {
        break;
      }
    }
    
    return String.join("/", pathNames);
  }

  @Override
  public Map<Long, String> getCategoryPaths(Collection<Long> categoryIds) {
    Map<Long, String> paths = new HashMap<>();
    if (categoryIds.isEmpty()) {
      return paths;
    }
    Map<Long, IoTStandardCategory> categories = new HashMap<>();
    // 沿用分类列表的租户过滤，一次读取后在内存中追溯父分类。
    selectCategoryList(new IoTStandardCategory())
        .forEach(category -> categories.put(category.getId(), category));
    for (Long categoryId : new HashSet<>(categoryIds)) {
      List<String> names = new ArrayList<>();
      Set<Long> visited = new HashSet<>();
      IoTStandardCategory category = categories.get(categoryId);
      while (category != null && visited.add(category.getId())) {
        names.add(0, category.getCategoryName());
        if (category.getParentId() == null || category.getParentId() <= 0) {
          break;
        }
        category = categories.get(category.getParentId());
      }
      paths.put(categoryId, String.join("/", names));
    }
    return paths;
  }

  private void applyCurrentTenant(IoTStandardCategory category) {
    if (category == null) {
      return;
    }
    String tenantId = normalizeTenantIdForCurrentUser(category.getTenantId());
    if (StrUtil.isNotBlank(tenantId)) {
      category.setTenantId(tenantId);
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
