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

package cn.universal.dm.device.service.wrapper;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.domain.R;
import cn.universal.common.exception.IoTErrorCode;
import cn.universal.common.exception.IoTException;
import cn.universal.core.message.DownRequest;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.persistence.base.DownRequestDataHelper;
import cn.universal.persistence.base.IoTDownWrapper;
import cn.universal.persistence.entity.IoTProduct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 设备添加的预处理，参数校验，设备编号和产品校验
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/1/20
 */
@Service("ioTDeviceAddParamIntercept")
@Slf4j
public class IoTDeviceAddParamIntercept implements IoTDownWrapper {

  private String CUSTOM_FIELD = "customField";

  private String STOCK_DEVICE_CHECK = "stockDeviceCheck";
  private String RELATED_PRODUCT_KEY = "canNotRelateProductKey";

  @Resource private IoTProductDeviceService iotProductDeviceService;

  @Override
  public R beforeDownAction(IoTProduct product, Object data, DownRequest downRequest) {
    // 参数拦截
    paramIntercept(product, data, downRequest);

    return null;
  }

  /**
   * 拦截自定义参数是否规范
   *
   * @param product
   * @param data
   * @param downRequest
   */
  private void paramIntercept(IoTProduct product, Object data, DownRequest downRequest) {
    if (downRequest.isAllowInsert()) {
      return;
    }
    if (product != null
        && StrUtil.isNotBlank(product.getThirdConfiguration())
        && DownCmd.DEV_ADD.equals(downRequest.getCmd())) {
      boolean flag = false;
      String result = null;
      try {
        JSONObject jsonObject = JSONUtil.parseObj(product.getThirdConfiguration());
        JSONArray customFields = jsonObject.getJSONArray(CUSTOM_FIELD);
        if (jsonObject == null || customFields == null || customFields.size() <= 0) {
          return;
        }
        StringBuilder builder = new StringBuilder("产品[" + product.getName() + "]data中的必填参数不能为空,");
        for (Object obj : customFields) {
          JSONObject object = (JSONObject) obj;
          String fieldName = object.getStr("id");
          Object fieldValue = DownRequestDataHelper.getFieldValue(downRequest, data, fieldName);
          if (fieldValue == null) {
            builder
                .append(fieldName)
                .append("(")
                .append(object.getStr("name"))
                .append(")")
                .append(",");
            flag = true;
          }
        }
        result = builder.substring(0, builder.length() - 1);
      } catch (Exception e) {
        log.warn("校验CUSTOM_FIELD字段出错={}", e);
      }
      if (flag) {
        throw new IoTException(result, IoTErrorCode.DEV_ADD_ERROR.getCode());
      }
    }
  }
}
