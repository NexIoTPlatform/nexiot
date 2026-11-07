/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 公共物模型产品服务实现类
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.common.service.BaseServiceImpl;
import cn.universal.admin.platform.service.IIoTProductMetadataVersionService;
import cn.universal.admin.platform.service.IIoTStandardCategoryService;
import cn.universal.admin.platform.service.IIoTStandardProductService;
import cn.universal.admin.platform.support.ProductMetadataNormalizer;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTStandardProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.IoTStandardProductMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 公共物模型产品服务实现类
 *
 * @since 2026-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IoTStandardProductServiceImpl extends BaseServiceImpl
    implements IIoTStandardProductService {

  private final IoTStandardProductMapper productMapper;
  private final IoTProductMapper ioTProductMapper;
  private final IIoTStandardCategoryService categoryService;
  private final IIoTProductMetadataVersionService versionService;

  @Override
  public List<IoTStandardProduct> selectProductList(IoTStandardProduct product) {
    applyCurrentTenant(product);
    List<IoTStandardProduct> list = productMapper.selectProductList(product);
    fillCategoryPaths(list);
    return list;
  }

  @Override
  public List<IoTStandardProduct> selectProductByCategoryId(Long categoryId) {
    List<IoTStandardProduct> list =
        productMapper.selectProductByCategoryId(categoryId, currentTenantId());
    fillCategoryPaths(list);
    return list;
  }

  @Override
  public IoTStandardProduct selectProductById(Long id) {
    IoTStandardProduct product = productMapper.selectProductById(id, currentTenantId());
    if (product != null && product.getCategoryId() != null) {
      product.setCategoryPath(categoryService.getCategoryPath(product.getCategoryId()));
    }
    return product;
  }

  @Override
  public IoTStandardProduct selectProductByKey(String productKey) {
    return productMapper.selectProductByKey(productKey, currentTenantId());
  }

  @Override
  public int insertProduct(IoTStandardProduct product) {
    applyCurrentTenant(product);
    // 默认状态为启用
    if (product.getStatus() == null) {
      product.setStatus(1);
    }
    if (StrUtil.isNotBlank(product.getMetadata())) {
      product.setMetadata(ProductMetadataNormalizer.normalizeMetadataJson(product.getMetadata()));
    }
    return productMapper.insertProduct(product);
  }

  @Override
  public int updateProduct(IoTStandardProduct product) {
    applyCurrentTenant(product);
    if (StrUtil.isNotBlank(product.getMetadata())) {
      product.setMetadata(ProductMetadataNormalizer.normalizeMetadataJson(product.getMetadata()));
    }
    return productMapper.updateProduct(product);
  }

  @Override
  public int deleteProductById(Long id) {
    return productMapper.deleteProductById(id, currentTenantId());
  }

  @Override
  public int deleteProductByIds(Long[] ids) {
    return productMapper.deleteProductByIds(ids, currentTenantId());
  }

  @Override
  public boolean checkProductKeyUnique(IoTStandardProduct product) {
    applyCurrentTenant(product);
    Long excludeId = product.getId() == null ? null : product.getId();
    int count =
        productMapper.checkProductKeyUnique(product.getProductKey(), excludeId, product.getTenantId());
    return count == 0;
  }

  @Override
  public List<IoTStandardProduct> searchProductByKeyword(String keyword) {
    return productMapper.searchProductByKeyword(keyword, currentTenantId());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public int importStandardProduct(Long standardProductId, Long targetProductId, String operator) {
    // 获取标准产品
    IoTStandardProduct standardProduct =
        productMapper.selectProductById(standardProductId, currentTenantId());
    if (standardProduct == null) {
      throw new RuntimeException("标准产品不存在");
    }

    // 获取目标产品
    IoTProduct targetProduct = ioTProductMapper.selectDevProductById(String.valueOf(targetProductId));
    if (targetProduct == null) {
      throw new RuntimeException("目标产品不存在");
    }

    // 记录版本历史（变更前）
    String oldMetadata = targetProduct.getMetadata();

    // 覆盖物模型数据
    String normalizedMetadata =
        ProductMetadataNormalizer.normalizeMetadataJson(standardProduct.getMetadata());
    targetProduct.setMetadata(normalizedMetadata);
    int result = ioTProductMapper.updateDevProduct(targetProduct);

    // 记录版本历史（变更后）
    if (result > 0) {
      versionService.createVersion(
          targetProduct.getId(),
          targetProduct.getProductKey(),
          normalizedMetadata,
          "import",
          "导入标准产品物模型：" + standardProduct.getProductName(),
          operator
      );
    }

    return result;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public int updateProductMetadata(Long id, String metadata, String operator) {
    // 获取标准产品
    IoTStandardProduct product = productMapper.selectProductById(id, currentTenantId());
    if (product == null) {
      throw new RuntimeException("产品不存在");
    }

    // 校验物模型ID不能重复
    validateMetadataIds(metadata);

    String normalizedMetadata = ProductMetadataNormalizer.normalizeMetadataJson(metadata);

    // 更新物模型
    product.setMetadata(normalizedMetadata);
    product.setUpdateBy(operator);
    int result = productMapper.updateProduct(product);

    // 记录版本历史
    if (result > 0) {
      versionService.createVersion(
          product.getId(),
          product.getProductKey(),
          normalizedMetadata,
          "update",
          "更新标准产品物模型",
          operator
      );
    }

    return result;
  }

  @Override
  public int setTemplate(Long id, Integer isTemplate) {
    if (isTemplate == null || (isTemplate != 0 && isTemplate != 1)) {
      throw new IllegalArgumentException("模板标记参数不合法");
    }
    return productMapper.updateTemplateStatus(id, isTemplate, currentTenantId());
  }

  @Override
  public int incrementUseCount(Long id) {
    return productMapper.incrementUseCount(id, currentTenantId());
  }

  @Override
  public List<IoTStandardProduct> selectTemplateList() {
    List<IoTStandardProduct> list = productMapper.selectTemplateList(currentTenantId());
    fillCategoryPaths(list);
    return list;
  }

  @Override
  public List<IoTStandardProduct> selectPopularProducts(Integer limit) {
    if (limit == null || limit <= 0) {
      limit = 10; // 默认返回10条
    }
    List<IoTStandardProduct> list = productMapper.selectPopularProducts(limit, currentTenantId());
    fillCategoryPaths(list);
    return list;
  }

  private void fillCategoryPaths(List<IoTStandardProduct> products) {
    List<Long> categoryIds = products.stream()
        .map(IoTStandardProduct::getCategoryId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    if (categoryIds.isEmpty()) {
      return;
    }
    Map<Long, String> paths = categoryService.getCategoryPaths(categoryIds);
    // 原地补充字段，保留 PageHelper 返回的分页对象及总数。
    products.forEach(product -> {
      if (product.getCategoryId() != null) {
        product.setCategoryPath(paths.getOrDefault(product.getCategoryId(), ""));
      }
    });
  }

  /**
   * 校验物模型中各类型的ID不能重复
   * @param metadata 物模型JSON字符串
   */
  private void validateMetadataIds(String metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return;
    }
    
    try {
      JSONObject metadataObj = JSONUtil.parseObj(metadata);
      
      // 校验属性ID不重复
      JSONArray properties = metadataObj.getJSONArray("properties");
      if (properties != null && !properties.isEmpty()) {
        validateNoDuplicateIds(properties, "属性");
      }
      
      // 校验事件ID不重复
      JSONArray events = metadataObj.getJSONArray("events");
      if (events != null && !events.isEmpty()) {
        validateNoDuplicateIds(events, "事件");
      }
      
      // 校验功能ID不重复
      JSONArray functions = metadataObj.getJSONArray("functions");
      if (functions != null && !functions.isEmpty()) {
        validateNoDuplicateIds(functions, "功能");
      }
    } catch (Exception e) {
      throw new RuntimeException("物模型JSON格式错误: " + e.getMessage());
    }
  }
  
  /**
   * 校验JSON数组中的ID不重复
   * @param array JSON数组
   * @param typeName 类型名称（用于错误提示）
   */
  private void validateNoDuplicateIds(JSONArray array, String typeName) {
    Set<String> ids = new HashSet<>();
    for (int i = 0; i < array.size(); i++) {
      JSONObject item = array.getJSONObject(i);
      String id = item.getStr("id");
      if (id != null && !id.isEmpty()) {
        if (ids.contains(id)) {
          throw new RuntimeException(typeName + "ID重复: " + id);
        }
        ids.add(id);
      }
    }
  }

  private void applyCurrentTenant(IoTStandardProduct product) {
    if (product == null) {
      return;
    }
    String tenantId = normalizeTenantIdForCurrentUser(product.getTenantId());
    if (StrUtil.isNotBlank(tenantId)) {
      product.setTenantId(tenantId);
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
