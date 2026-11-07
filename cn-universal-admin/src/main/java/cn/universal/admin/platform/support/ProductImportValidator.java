package cn.universal.admin.platform.support;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTProductCategory;
import cn.universal.persistence.entity.IoTProductMetadataVersion;
import cn.universal.persistence.entity.dto.ProductExportPackageDTO;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/** 导入必须严格校验，不能将无效JSON归一化为空配置后写入。 */
public final class ProductImportValidator {
  private static final ObjectMapper JSON =
      new ObjectMapper()
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);

  private ProductImportValidator() {}

  public static void packages(String value) {
    try {
      JsonNode node = JSON.readTree(value);
      if (node == null || !node.isArray() || node.isEmpty())
        throw new IoTException("导入文件必须是非空产品包JSON数组");
      for (int i = 0; i < node.size(); i++) {
        JsonNode entry = node.get(i);
        knownFields(entry, ProductExportPackageDTO.class, "第" + (i + 1) + "个产品包");
        if (entry.hasNonNull("product"))
          knownFields(
              entry.get("product"), ProductExportPackageDTO.ProductInfoDTO.class, "product");
        if (entry.hasNonNull("protocol"))
          knownFields(
              entry.get("protocol"), ProductExportPackageDTO.DeviceProtocolDTO.class, "protocol");
        if (entry.hasNonNull("network"))
          knownFields(
              entry.get("network"), ProductExportPackageDTO.NetworkConfigDTO.class, "network");
        if (entry.hasNonNull("networks")) {
          if (!entry.get("networks").isArray()) throw new IoTException("networks必须是数组");
          for (JsonNode network : entry.get("networks"))
            knownFields(network, ProductExportPackageDTO.NetworkConfigDTO.class, "networks");
        }
        if (entry.hasNonNull("categories")) {
          if (!entry.get("categories").isArray()) throw new IoTException("categories必须是数组");
          for (JsonNode category : entry.get("categories"))
            knownFields(category, IoTProductCategory.class, "categories");
        }
        if (entry.hasNonNull("metadataVersions")) {
          if (!entry.get("metadataVersions").isArray())
            throw new IoTException("metadataVersions必须是数组");
          for (JsonNode version : entry.get("metadataVersions"))
            knownFields(version, IoTProductMetadataVersion.class, "metadataVersions");
        }
      }
    } catch (IoTException e) {
      throw e;
    } catch (Exception e) {
      throw new IoTException("导入文件不是有效的JSON，请检查JSON语法");
    }
  }

  private static void knownFields(JsonNode object, Class<?> type, String label) {
    if (!object.isObject()) throw new IoTException(label + "必须是JSON对象");
    Set<String> names = new HashSet<>();
    for (var field : type.getDeclaredFields())
      if (!Modifier.isStatic(field.getModifiers())) names.add(field.getName());
    object
        .fieldNames()
        .forEachRemaining(
            name -> {
              if (!names.contains(name))
                throw new IoTException(label + "包含不支持的字段「" + name + "」，为避免遗漏配置已取消导入，请使用匹配版本的完整包");
            });
  }

  public static JsonNode object(String value, String label, boolean required) {
    if (StrUtil.isBlank(value)) {
      if (required) throw new IoTException(label + "不能为空");
      return JSON.createObjectNode();
    }
    try {
      JsonNode node = JSON.readTree(value);
      if (node == null || !node.isObject()) throw new IoTException(label + "必须是JSON对象");
      return node;
    } catch (IoTException e) {
      throw e;
    } catch (Exception e) {
      throw new IoTException(label + "不是有效的JSON对象，请检查JSON语法");
    }
  }

  public static void metadata(String value, boolean required) {
    if (StrUtil.isBlank(value) && !required) return;
    JsonNode document = object(value, "物模型", true);
    for (String section : new String[] {"properties", "events", "functions"}) {
      JsonNode items = document.get(section);
      if (items == null && !required) continue; // 兼容1.0文件的省略空类型。
      if (items == null || !items.isArray()) throw new IoTException("物模型 " + section + " 必须是数组");
      Set<String> ids = new HashSet<>();
      for (JsonNode item : items) {
        if (!item.isObject()
            || !item.path("id").isTextual()
            || StrUtil.isBlank(item.path("id").asText())
            || !item.path("name").isTextual()
            || StrUtil.isBlank(item.path("name").asText())) {
          throw new IoTException("物模型 " + section + " 每个字段必须包含非空id和name");
        }
        if (!ids.add(item.path("id").asText()))
          throw new IoTException("物模型 " + section + " 标识重复：" + item.path("id").asText());
        if (item.has("inputs") && !item.get("inputs").isArray())
          throw new IoTException("物模型功能inputs必须是数组");
      }
    }
  }
}
