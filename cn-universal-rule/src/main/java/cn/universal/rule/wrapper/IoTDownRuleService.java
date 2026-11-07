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

package cn.universal.rule.wrapper;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.domain.R;
import cn.universal.core.message.DownRequest;
import cn.universal.persistence.base.IoTDownWrapper;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.rule.fence.service.FenceService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/8/10
 */
@Service("iotDownRuleService")
public class IoTDownRuleService implements IoTDownWrapper {

  @Resource private FenceService fenceService;

  @Override
  public R beforeFunctionOrConfigDown(
      IoTProduct product, IoTDevice ioTDevice, DownRequest downRequest) {
    // 处理电子围栏
    if (StringUtils.isNotEmpty(product.getConfiguration())) {
      JSONObject jsonObject = JSONUtil.parseObj(product.getConfiguration());
      Boolean isGps = jsonObject.getBool("isGps");
      return isGps != null && isGps
          ? fenceService.callFenceFunction(product, ioTDevice, downRequest)
          : null;
    }
    return null;
  }
}
