/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 产品变更推送委托实现，桥接框架层和推送服务
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.service.push;

import cn.universal.dm.device.service.action.ProductChangePushDelegate;
import cn.universal.persistence.entity.IoTProduct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 产品变更推送委托实现
 * <p>
 * 实现框架层定义的接口，调用实际的推送服务
 */
@Service
@Slf4j
public class ProductChangePushDelegateImpl implements ProductChangePushDelegate {

    @Resource
    private ProductChangePushService productChangePushService;

    @Override
    public void pushProductCreate(IoTProduct product) {
        log.debug("触发产品创建推送: productKey={}", product.getProductKey());
        productChangePushService.pushProductChange(product, ProductChangePushService.ProductChangeType.CREATE);
    }

    @Override
    public void pushProductUpdate(IoTProduct product) {
        log.debug("触发产品更新推送: productKey={}", product.getProductKey());
        productChangePushService.pushProductChange(product, ProductChangePushService.ProductChangeType.UPDATE);
    }

    @Override
    public void pushProductDelete(IoTProduct product) {
        log.debug("触发产品删除推送: productKey={}", product.getProductKey());
        productChangePushService.pushProductChange(product, ProductChangePushService.ProductChangeType.DELETE);
    }
}
