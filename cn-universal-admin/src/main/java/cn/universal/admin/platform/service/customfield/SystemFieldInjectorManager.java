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

package cn.universal.admin.platform.service.customfield;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.persistence.entity.IoTProduct;
import jakarta.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 系统字段注入管理器
 * 统一管理所有的系统字段注入器，在产品创建/更新时自动注入系统字段
 *
 * @author NexIoT
 * @since 2025-02-05
 */
@Slf4j
@Service
public class SystemFieldInjectorManager {

  @Resource
  private List<SystemFieldInjector> injectors;

  /**
   * 对产品执行系统字段注入
   * 此方法会直接修改product的thirdConfiguration字段
   *
   * @param product 产品实体
   */
  public void injectSystemFields(IoTProduct product) {
    if (product == null) {
      return;
    }

    // 解析或初始化thirdConfiguration
    JSONObject thirdConfig;
    if (StrUtil.isBlank(product.getThirdConfiguration())) {
      thirdConfig = new JSONObject();
    } else {
      try {
        thirdConfig = JSONUtil.parseObj(product.getThirdConfiguration());
      } catch (Exception e) {
        log.warn("解析thirdConfiguration失败，将创建新配置: {}", e.getMessage());
        thirdConfig = new JSONObject();
      }
    }

    // 获取或初始化customField数组
    JSONArray customFields = thirdConfig.getJSONArray("customField");
    if (customFields == null) {
      customFields = new JSONArray();
    }

    removeSystemFields(customFields);

    // 按优先级排序后执行注入
    List<SystemFieldInjector> sortedInjectors = injectors.stream()
        .sorted(Comparator.comparingInt(SystemFieldInjector::getOrder))
        .toList();

    for (SystemFieldInjector injector : sortedInjectors) {
      try {
        if (injector.supports(product)) {
          injector.inject(product, customFields);
          log.debug("注入器[{}]已为产品[{}]执行字段注入",
              injector.getClass().getSimpleName(), product.getProductKey());
        }
      } catch (Exception e) {
        log.error("注入器[{}]执行失败: {}",
            injector.getClass().getSimpleName(), e.getMessage(), e);
      }
    }

    // 更新thirdConfiguration
    thirdConfig.set("customField", customFields);
    product.setThirdConfiguration(thirdConfig.toString());
  }

  private void removeSystemFields(JSONArray customFields) {
    if (customFields == null || customFields.isEmpty()) {
      return;
    }
    for (int i = customFields.size() - 1; i >= 0; i--) {
      JSONObject field = customFields.getJSONObject(i);
      if (field != null && Boolean.TRUE.equals(field.getBool("system"))) {
        customFields.remove(i);
      }
    }
  }

  /**
   * 检查指定字段是否为系统字段
   *
   * @param fieldId 字段ID
   * @param thirdConfiguration 第三方配置JSON字符串
   * @return true-系统字段，false-非系统字段
   */
  public boolean isSystemField(String fieldId, String thirdConfiguration) {
    if (StrUtil.isBlank(fieldId) || StrUtil.isBlank(thirdConfiguration)) {
      return false;
    }

    try {
      JSONObject config = JSONUtil.parseObj(thirdConfiguration);
      JSONArray customFields = config.getJSONArray("customField");
      if (customFields == null) {
        return false;
      }

      for (int i = 0; i < customFields.size(); i++) {
        JSONObject field = customFields.getJSONObject(i);
        if (fieldId.equals(field.getStr("id"))) {
          return Boolean.TRUE.equals(field.getBool("system"));
        }
      }
    } catch (Exception e) {
      log.warn("检查系统字段失败: {}", e.getMessage());
    }

    return false;
  }
}
