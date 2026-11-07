/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 物模型版本历史服务实现类
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.service.impl;

import cn.universal.admin.platform.service.IIoTProductMetadataVersionService;
import cn.universal.admin.platform.support.ProductMetadataNormalizer;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTProductMetadataVersion;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.IoTProductMetadataVersionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 物模型版本历史服务实现类
 *
 * @since 2026-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IoTProductMetadataVersionServiceImpl implements IIoTProductMetadataVersionService {

  private final IoTProductMetadataVersionMapper versionMapper;
  private final IoTProductMapper productMapper;
  private final ObjectMapper objectMapper;

  @Override
  public List<IoTProductMetadataVersion> selectVersionList(IoTProductMetadataVersion version) {
    return versionMapper.selectVersionList(version);
  }

  @Override
  public List<IoTProductMetadataVersion> selectVersionByProductId(Long productId) {
    return versionMapper.selectVersionByProductId(productId);
  }

  @Override
  public List<IoTProductMetadataVersion> selectVersionByProductKey(String productKey) {
    return versionMapper.selectVersionListByProductKey(productKey);
  }

  @Override
  public IoTProductMetadataVersion selectVersionById(Long id) {
    return versionMapper.selectVersionById(id);
  }

  @Override
  public IoTProductMetadataVersion selectLatestVersion(Long productId) {
    return versionMapper.selectLatestVersion(productId);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public IoTProductMetadataVersion createVersion(Long productId, String productKey, String metadataSnapshot,
      String changeType, String changeDescription, String operator) {
    
    // 获取当前最大版本号
    Integer maxVersion = versionMapper.selectMaxVersionByProductId(productId);
    int newVersion = (maxVersion == null ? 0 : maxVersion) + 1;

    IoTProductMetadataVersion version = IoTProductMetadataVersion.builder()
        .productId(productId)
        .productKey(productKey)
        .version(newVersion)
        .metadataSnapshot(metadataSnapshot)
        .changeType(changeType)
        .changeDescription(changeDescription)
        .operator(operator)
        .build();

    versionMapper.insertVersion(version);
    return version;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Long createSnapshotFromProduct(Long productId, String remark, String operator) {
    // 获取产品信息
    IoTProduct product = productMapper.selectDevProductById(String.valueOf(productId));
    if (product == null) {
      throw new RuntimeException("产品不存在");
    }

    // 创建版本快照
    IoTProductMetadataVersion version = createVersion(
        productId,
        product.getProductKey(),
        product.getMetadata(),
        "snapshot",
        remark != null ? remark : "手动创建快照",
        operator
    );

    return version.getId();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public int restoreVersion(Long versionId, Long targetProductId, String operator) {
    // 获取版本信息
    IoTProductMetadataVersion version = versionMapper.selectVersionById(versionId);
    if (version == null) {
      throw new RuntimeException("版本不存在");
    }

    // 获取目标产品信息
    IoTProduct product = productMapper.selectDevProductById(String.valueOf(targetProductId));
    if (product == null) {
      throw new RuntimeException("目标产品不存在");
    }

    String normalizedMetadata =
        ProductMetadataNormalizer.normalizeMetadataJson(version.getMetadataSnapshot());

    // 更新产品的物模型
    product.setMetadata(normalizedMetadata);
    int result = productMapper.updateDevProduct(product);

    // 记录新版本（还原操作）
    if (result > 0) {
      createVersion(
          targetProductId,
          product.getProductKey(),
          normalizedMetadata,
          "restore",
          "还原到版本 V" + version.getVersion(),
          operator
      );
    }

    return result;
  }

  @Override
  public List<IoTProductMetadataVersion> selectLatestTwoVersions(String productKey) {
    IoTProduct product = productMapper.getProductByProductKey(productKey);
    if (product == null) {
      return List.of();
    }
    return versionMapper.selectLatestTwoVersions(product.getId());
  }

  @Override
  public int deleteVersionById(Long id) {
    return versionMapper.deleteVersionById(id);
  }

  @Override
  public int deleteVersionByIds(Long[] ids) {
    return versionMapper.deleteVersionByIds(ids);
  }

  @Override
  public int countVersionByProductId(Long productId) {
    return versionMapper.countVersionByProductId(productId);
  }

  @Override
  public String compareVersions(Long versionId1, Long versionId2) {
    IoTProductMetadataVersion version1 = versionMapper.selectVersionById(versionId1);
    IoTProductMetadataVersion version2 = versionMapper.selectVersionById(versionId2);

    if (version1 == null || version2 == null) {
      throw new RuntimeException("版本不存在");
    }

    try {
      JsonNode json1 = objectMapper.readTree(version1.getMetadataSnapshot());
      JsonNode json2 = objectMapper.readTree(version2.getMetadataSnapshot());

      ObjectNode diffResult = objectMapper.createObjectNode();
      diffResult.put("version1", version1.getVersion());
      diffResult.put("version2", version2.getVersion());

      // 比较properties
      diffResult.set("properties", compareArrays(
          json1.path("properties"),
          json2.path("properties"),
          "id"
      ));

      // 比较events
      diffResult.set("events", compareArrays(
          json1.path("events"),
          json2.path("events"),
          "id"
      ));

      // 比较functions
      diffResult.set("functions", compareArrays(
          json1.path("functions"),
          json2.path("functions"),
          "id"
      ));

      return objectMapper.writeValueAsString(diffResult);
    } catch (Exception e) {
      log.error("比较版本失败", e);
      throw new RuntimeException("比较版本失败: " + e.getMessage());
    }
  }

  /**
   * 比较两个数组的差异
   */
  private ObjectNode compareArrays(JsonNode array1, JsonNode array2, String keyField) {
    ObjectNode result = objectMapper.createObjectNode();
    ArrayNode added = objectMapper.createArrayNode();
    ArrayNode removed = objectMapper.createArrayNode();
    ArrayNode modified = objectMapper.createArrayNode();

    // 构建version1的map
    ObjectNode map1 = objectMapper.createObjectNode();
    if (array1.isArray()) {
      for (JsonNode item : array1) {
        String key = item.path(keyField).asText();
        map1.set(key, item);
      }
    }

    // 构建version2的map
    ObjectNode map2 = objectMapper.createObjectNode();
    if (array2.isArray()) {
      for (JsonNode item : array2) {
        String key = item.path(keyField).asText();
        map2.set(key, item);
      }
    }

    // 找出新增和修改的
    Iterator<String> keys2 = map2.fieldNames();
    while (keys2.hasNext()) {
      String key = keys2.next();
      if (!map1.has(key)) {
        added.add(map2.get(key));
      } else if (!map1.get(key).equals(map2.get(key))) {
        ObjectNode modifiedItem = objectMapper.createObjectNode();
        modifiedItem.set("old", map1.get(key));
        modifiedItem.set("new", map2.get(key));
        modified.add(modifiedItem);
      }
    }

    // 找出删除的
    Iterator<String> keys1 = map1.fieldNames();
    while (keys1.hasNext()) {
      String key = keys1.next();
      if (!map2.has(key)) {
        removed.add(map1.get(key));
      }
    }

    result.set("added", added);
    result.set("removed", removed);
    result.set("modified", modified);

    return result;
  }

  @Override
  public int deleteVersionByProductId(Long productId) {
    return versionMapper.deleteVersionByProductId(productId);
  }
}
