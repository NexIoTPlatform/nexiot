/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 产品变更推送委托接口，用于解耦框架层与业务层
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.dm.device.service.action;

import cn.universal.persistence.entity.IoTProduct;

/**
 * 产品变更推送委托接口
 * <p>
 * 此接口定义在框架层，由业务层（admin）实现，用于向订阅应用推送产品变更事件
 */
public interface ProductChangePushDelegate {

    /**
     * 推送产品创建事件
     *
     * @param product 产品信息
     */
    void pushProductCreate(IoTProduct product);

    /**
     * 推送产品更新事件
     *
     * @param product 产品信息
     */
    void pushProductUpdate(IoTProduct product);

    /**
     * 推送产品删除事件
     *
     * @param product 产品信息
     */
    void pushProductDelete(IoTProduct product);
}
