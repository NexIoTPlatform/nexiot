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

package cn.universal.dm.device.service.action;

import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.persistence.base.IoTProductAction;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTProductMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 产品生命周期，用于统一处理
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/6/8
 */
@Service("ioTProductActionService")
@Slf4j
public class IoTProductActionService implements IoTProductAction {

  @Resource private IoTCacheInvalidationPublisher cacheInvalidationPublisher;

  @Resource private IoTProductMapper ioTProductMapper;

  // 产品变更推送服务（可选依赖，如果存在则注入）
  @Autowired(required = false)
  private ProductChangePushDelegate productChangePushDelegate;

  @Override
  public void create(String productKey, String unionId) {
    log.info("产品创建,productKey={},创建用户={}", productKey, unionId);
    cacheInvalidationPublisher.publishProductChange(productKey, "product-create");

    // 推送产品创建事件
    if (productChangePushDelegate != null) {
      IoTProduct product = ioTProductMapper.getProductByProductKey(productKey);
      if (product != null) {
        productChangePushDelegate.pushProductCreate(product);
      }
    }
  }

  @Override
  public void update(String productKey) {
    log.info("产品更新,productKey={}", productKey);
    cacheInvalidationPublisher.publishProductChange(productKey, "product-update");

    // 推送产品更新事件 - 统一在这里查询完整对象
    if (productChangePushDelegate != null && productKey != null) {
      IoTProduct product = ioTProductMapper.getProductByProductKey(productKey);
      if (product != null) {
        productChangePushDelegate.pushProductUpdate(product);
      }
    }
  }

  @Override
  public void delete(String productKey) {
    cacheInvalidationPublisher.publishProductChange(productKey, "product-delete");

    // 推送产品删除事件
    if (productChangePushDelegate != null) {
      IoTProduct product = ioTProductMapper.getProductByProductKey(productKey);
      if (product != null) {
        productChangePushDelegate.pushProductDelete(product);
      }
    }
  }

  @Override
  public void enable(String productKey) {}

  @Override
  public void disable(String productKey) {}

  @Override
  public void publish(String productKey) {}

  @Override
  public void metadataCreate(String productKey) {
    cacheInvalidationPublisher.publishProductChange(productKey, "product-metadata-create");
  }

  @Override
  public void metadataUpdate(String productKey) {
    cacheInvalidationPublisher.publishProductChange(productKey, "product-metadata-update");
  }

  @Override
  public void metadataDelete(String productKey) {
    cacheInvalidationPublisher.publishProductChange(productKey, "product-metadata-delete");
  }
}
