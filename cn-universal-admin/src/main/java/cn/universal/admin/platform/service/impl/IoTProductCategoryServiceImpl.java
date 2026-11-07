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

package cn.universal.admin.platform.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.admin.common.service.BaseServiceImpl;
import cn.universal.admin.platform.service.IIoTProductCategoryService;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.persistence.entity.IoTProductCategory;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.RuleModelBO.IoTProductSortBO;
import cn.universal.persistence.mapper.IoTProductSortMapper;
import cn.universal.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 产品分类Service业务层处理 @Author gitee.com/NexIoT
 *
 * @since 2025-12-29
 */
@Slf4j
@Service
public class IoTProductCategoryServiceImpl extends BaseServiceImpl
    implements IIoTProductCategoryService {

  @Resource private IoTProductSortMapper ioTProductSortMapper;

  @Resource private IoTCacheInvalidationPublisher cacheInvalidationPublisher;

  /**
   * 查询产品分类
   *
   * @param id 产品分类主键
   * @return 产品分类
   */
  @Override
  public IoTProductCategory selectDevProductSortById(String id) {
    if (StrUtil.isBlank(id)) {
      log.warn("查询产品分类时ID为空");
      return null;
    }
    IoTProductCategory category = ioTProductSortMapper.selectDevProductSortById(id);
    assertCategoryAccess(category);
    return category;
  }

  /**
   * 查询产品分类列表
   *
   * @param ioTProductCategory 产品分类查询条件
   * @return 产品分类列表
   */
  @Override
  public List<IoTProductCategory> selectDevProductSortList(IoTProductCategory ioTProductCategory) {
    fillCategoryTenant(ioTProductCategory);
    return ioTProductSortMapper.selectDevProductSortList(ioTProductCategory);
  }

  /**
   * 新增产品分类
   *
   * @param ioTProductCategory 产品分类
   * @return 结果
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public int insertDevProductSort(IoTProductCategory ioTProductCategory) {
    // 参数验证
    if (ioTProductCategory == null) {
      throw new IllegalArgumentException("产品分类信息不能为空");
    }
    if (StrUtil.isBlank(ioTProductCategory.getClassifiedName())) {
      throw new IllegalArgumentException("分类名称不能为空");
    }
    if (StrUtil.isBlank(ioTProductCategory.getParentId())) {
      throw new IllegalArgumentException("父分类ID不能为空");
    }

    // 检查分类名称是否重复
    if (isClassNameExists(ioTProductCategory.getClassifiedName(), ioTProductCategory.getParentId(), null)) {
      throw new IllegalArgumentException("同级分类下已存在相同名称的分类");
    }

    // 生成分类ID
    String tenantId = resolveRequiredTenantId(ioTProductCategory.getTenantId());
    validateParentCategory(ioTProductCategory.getParentId(), tenantId, true);
    String newId = generateProductSortId();
    ioTProductCategory.setId(newId);

    // 设置默认值
    ioTProductCategory.setHasChild(0);
    ioTProductCategory.setCreateTime(new Date());
    ioTProductCategory.setCreateBy(SecurityUtils.getUnionId());
    ioTProductCategory.setTenantId(tenantId);

    // 插入新分类
    int result = ioTProductSortMapper.insertDevProductSort(ioTProductCategory);

    // 更新父分类的hasChild状态
    updateParentHasChildStatus(ioTProductCategory.getParentId(), tenantId);

    log.info(
        "新增产品分类成功: id={}, name={}, parentId={}",
        newId,
        ioTProductCategory.getClassifiedName(),
        ioTProductCategory.getParentId());

    return result;
  }

  /**
   * 修改产品分类
   *
   * @param ioTProductCategory 产品分类
   * @return 结果
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public int updateDevProductSort(IoTProductCategory ioTProductCategory) {
    // 参数验证
    if (ioTProductCategory == null || StrUtil.isBlank(ioTProductCategory.getId())) {
      throw new IllegalArgumentException("产品分类ID不能为空");
    }
    if (StrUtil.isBlank(ioTProductCategory.getClassifiedName())) {
      throw new IllegalArgumentException("分类名称不能为空");
    }

    // 检查分类是否存在
    IoTProductCategory existingSort =
        ioTProductSortMapper.selectDevProductSortById(ioTProductCategory.getId());
    String tenantId = resolveRequiredTenantId(ioTProductCategory.getTenantId());
    assertCategoryAccess(existingSort);
    if (existingSort == null) {
      throw new IllegalArgumentException("要修改的产品分类不存在");
    }

    // 检查分类名称是否重复（排除自己）
    if (isClassNameExists(
        ioTProductCategory.getClassifiedName(), existingSort.getParentId(), ioTProductCategory.getId())) {
      throw new IllegalArgumentException("同级分类下已存在相同名称的分类");
    }

    // 不允许修改父分类（避免复杂的树结构变更）
    if (!existingSort.getParentId().equals(ioTProductCategory.getParentId())) {
      throw new IllegalArgumentException("不允许修改分类的父级关系");
    }
    ioTProductCategory.setTenantId(tenantId);

    // 更新分类信息
    int result = ioTProductSortMapper.updateDevProductSort(ioTProductCategory);

    log.info(
        "修改产品分类成功: id={}, name={}", ioTProductCategory.getId(), ioTProductCategory.getClassifiedName());

    return result;
  }

  /**
   * 批量删除产品分类
   *
   * @param ids 需要删除的产品分类主键数组
   * @return 结果
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public int deleteDevProductSortByIds(String[] ids) {
    if (ids == null || ids.length == 0) {
      throw new IllegalArgumentException("删除的产品分类ID不能为空");
    }

    int totalDeleted = 0;
    for (String id : ids) {
      if (StrUtil.isNotBlank(id)) {
        totalDeleted += deleteDevProductSortByIdInternal(id, false);
      }
    }

    if (totalDeleted > 0) {
      cacheInvalidationPublisher.publishProductCategoryChange("product-category-delete-batch");
    }

    log.info("批量删除产品分类完成: 删除数量={}", totalDeleted);
    return totalDeleted;
  }

  /**
   * 删除产品分类信息
   *
   * @param id 产品分类主键
   * @return 结果
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public int deleteDevProductSortById(String id) {
    return deleteDevProductSortByIdInternal(id, true);
  }

  private int deleteDevProductSortByIdInternal(String id, boolean publishCacheEvent) {
    if (StrUtil.isBlank(id)) {
      throw new IllegalArgumentException("删除的产品分类ID不能为空");
    }

    // 检查分类是否存在
    IoTProductCategory ioTProductCategory = ioTProductSortMapper.selectDevProductSortById(id);
    assertCategoryAccess(ioTProductCategory);
    if (ioTProductCategory == null) {
      throw new IllegalArgumentException("要删除的产品分类不存在");
    }

    // 检查是否有子分类
    String tenantId = resolveRequiredTenantId(ioTProductCategory.getTenantId());
    List<IoTProductCategory> children = ioTProductSortMapper.getListByParentId(id, tenantId);
    if (!children.isEmpty()) {
      throw new IllegalArgumentException("该分类下还有子分类，请先删除子分类");
    }

    // 删除分类
    int result = ioTProductSortMapper.deleteDevProductSortById(id);

    // 更新父分类的hasChild状态
    updateParentHasChildStatus(ioTProductCategory.getParentId(), tenantId);

    if (result > 0 && publishCacheEvent) {
      cacheInvalidationPublisher.publishProductCategoryChange("product-category-delete-single");
    }

    log.info("删除产品分类成功: id={}, name={}", id, ioTProductCategory.getClassifiedName());

    return result;
  }

  /**
   * 构建前端所需要树结构
   *
   * @param ioTProductCategoryList 菜单列表
   * @return 树结构列表
   */
  @Override
  public List<IoTProductCategory> buildProductSortTree(List<IoTProductCategory> ioTProductCategoryList) {
    if (ioTProductCategoryList == null || ioTProductCategoryList.isEmpty()) {
      return new ArrayList<>();
    }

    // 构建ID映射
    List<String> allIds =
        ioTProductCategoryList.stream().map(IoTProductCategory::getId).collect(Collectors.toList());

    // 找出根节点
    List<IoTProductCategory> rootNodes =
        ioTProductCategoryList.stream()
            .filter(sort -> !allIds.contains(sort.getParentId()) || "0".equals(sort.getParentId()))
            .collect(Collectors.toList());

    // 递归构建树结构
    for (IoTProductCategory rootNode : rootNodes) {
      buildTreeRecursively(ioTProductCategoryList, rootNode);
    }

    return rootNodes.isEmpty() ? ioTProductCategoryList : rootNodes;
  }

  /**
   * 生成产品分类ID
   *
   * @param parentId 父分类ID
   * @return 新的分类ID
   */
  private String generateProductSortId() {
    return IdUtil.getSnowflakeNextIdStr();
  }

  /**
   * 检查同级分类下是否存在相同名称
   *
   * @param className 分类名称
   * @param parentId 父分类ID
   * @param excludeId 排除的分类ID（用于更新时排除自己）
   * @return 是否存在
   */
  private boolean isClassNameExists(String className, String parentId, String excludeId) {
    String tenantId = resolveRequiredTenantId(null);
    List<IoTProductCategory> siblings = ioTProductSortMapper.getListByParentId(parentId, tenantId);

    return siblings.stream()
        .anyMatch(
            sort -> className.equals(sort.getClassifiedName()) && !sort.getId().equals(excludeId));
  }

  /**
   * 更新父分类的hasChild状态
   *
   * @param parentId 父分类ID
   */
  private void updateParentHasChildStatus(String parentId, String tenantId) {
    if ("0".equals(parentId)) {
      return; // 根分类不需要更新
    }

    IoTProductCategory parent = ioTProductSortMapper.selectDevProductSortById(parentId);
    assertCategoryAccess(parent);
    if (parent != null) {
      List<IoTProductCategory> children = ioTProductSortMapper.getListByParentId(parentId, tenantId);
      int hasChild = children.isEmpty() ? 0 : 1;

      if (parent.getHasChild() != hasChild) {
        parent.setHasChild(hasChild);
        parent.setTenantId(tenantId);
        ioTProductSortMapper.updateDevProductSort(parent);
      }
    }
  }

  /**
   * 递归构建树结构
   *
   * @param allNodes 所有节点
   * @param currentNode 当前节点
   */
  private void buildTreeRecursively(List<IoTProductCategory> allNodes, IoTProductCategory currentNode) {
    List<IoTProductCategory> children =
        allNodes.stream()
            .filter(node -> currentNode.getId().equals(node.getParentId()))
            .collect(Collectors.toList());

    if (!children.isEmpty()) {
      currentNode.setChildren(children);
      // 递归处理子节点
      for (IoTProductCategory child : children) {
        buildTreeRecursively(allNodes, child);
      }
    }
  }

  // ==================== 新增的BO方法实现 ====================

  /**
   * 新增产品分类（使用BO）
   *
   * @param bo 产品分类业务对象
   * @return 结果
   */
  @Override
  public int insertDevProductSort(IoTProductSortBO bo) {
    if (bo == null) {
      throw new IllegalArgumentException("产品分类信息不能为空");
    }

    // 处理parentId，前端可能传入数字0，需要转换为字符串"0"
    if ("0".equals(String.valueOf(bo.getParentId()))) {
      bo.setParentId("0");
    }

    IoTProductCategory entity = bo.toEntity();
    return insertDevProductSort(entity);
  }

  /**
   * 修改产品分类（使用BO）
   *
   * @param bo 产品分类业务对象
   * @return 结果
   */
  @Override
  public int updateDevProductSort(IoTProductSortBO bo) {
    if (bo == null) {
      throw new IllegalArgumentException("产品分类信息不能为空");
    }

    // 处理parentId，前端可能传入数字0，需要转换为字符串"0"
    if ("0".equals(String.valueOf(bo.getParentId()))) {
      bo.setParentId("0");
    }

    IoTProductCategory entity = bo.toEntity();
    return updateDevProductSort(entity);
  }

  /**
   * 获取产品分类树结构
   *
   * @return 树结构列表
   */
  @Override
  public List<IoTProductCategory> getProductSortTree() {
    // 查询所有分类
    List<IoTProductCategory> allSorts =
        ioTProductSortMapper.selectDevProductSortList(new IoTProductCategory());
    // 构建树结构
    return buildProductSortTree(allSorts);
  }

  private void fillCategoryTenant(IoTProductCategory category) {
    if (category == null) {
      return;
    }
    String tenantId = resolveRequiredTenantId(category.getTenantId());
    category.setTenantId(tenantId);
  }

  private String resolveRequiredTenantId(String preferredTenantId) {
    IoTUser currentUser = currentTenantUser();
    if (currentUser != null && !currentUser.isAdmin()) {
      String currentTenantId = resolveTenantId(currentUser);
      if (StrUtil.isNotBlank(preferredTenantId)
          && !StrUtil.equals(currentTenantId, preferredTenantId)) {
        throw new IllegalArgumentException("无权操作其他租户数据");
      }
      return currentTenantId;
    }
    if (StrUtil.isNotBlank(preferredTenantId)) {
      return preferredTenantId;
    }
    if (currentUser != null) {
      return StrUtil.blankToDefault(currentUser.getTenantId(), currentUser.getUnionId());
    }
    String tenantId = SecurityUtils.getTenantIdQuietly();
    if (StrUtil.isNotBlank(tenantId)) {
      return tenantId;
    }
    throw new IllegalArgumentException("当前租户信息不存在");
  }

  private void validateParentCategory(String parentId, String tenantId, boolean requiredExisting) {
    if ("0".equals(parentId)) {
      return;
    }
    IoTProductCategory parent = ioTProductSortMapper.selectDevProductSortById(parentId);
    assertCategoryAccess(parent);
    if (requiredExisting && parent == null) {
      throw new IllegalArgumentException("父分类不存在");
    }
    if (parent != null && !StrUtil.equals(tenantId, parent.getTenantId())) {
      throw new IllegalArgumentException("父分类不属于当前租户");
    }
  }

  private void assertCategoryAccess(IoTProductCategory category) {
    if (category == null) {
      return;
    }
    IoTUser currentUser = currentTenantUser();
    if (currentUser == null || currentUser.isAdmin()) {
      return;
    }
    String currentTenantId = StrUtil.blankToDefault(currentUser.getTenantId(), currentUser.getUnionId());
    if (StrUtil.isBlank(category.getTenantId())
        || !StrUtil.equals(currentTenantId, category.getTenantId())) {
      throw new IllegalArgumentException("无权访问该产品分类");
    }
  }
}
