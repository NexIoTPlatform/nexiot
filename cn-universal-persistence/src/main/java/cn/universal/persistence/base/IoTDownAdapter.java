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

package cn.universal.persistence.base;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.domain.R;
import cn.universal.core.message.DownRequest;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.SupportMapAreasMapper;
import jakarta.annotation.Resource;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;

/** 指令下发包装类 */
@Slf4j
public abstract class IoTDownAdapter<T extends BaseDownRequest> {

  @Resource private StringRedisTemplate stringRedisTemplate;

  @Resource
  @Qualifier("ioTDeviceActionBeforeService")
  private IoTDeviceLifeCycle ioTDeviceLifeCycle;

  @Resource private IoTDeviceExtendService deviceExtTemplate;

  @Resource private SupportMapAreasMapper supportMapAreasMapper;

  @Resource private IoTDeviceMapper ioTDeviceMapper;

  @Resource private IoTProductMapper ioTProductMapper;
  private String CUSTOM_FIELD = "customField";
  private String SLAVE_ADDRESS = "slaveAddress";

  /**
   * iot_product
   *
   * <p>{third_configuration}字段配置JSON
   *
   * <p>customField
   *
   * <p>[添加设备]校验->data中是否包含自定义字段
   */
  protected R beforeDownAction(IoTProduct product, Object data, DownRequest downRequest) {
    deviceExtTemplate.downExt(downRequest);
    Object effectiveData = DownRequestDataHelper.resolveBeforeDownData(downRequest, data);
    Map<String, IoTDownWrapper> downWrapperMap = SpringUtil.getBeansOfType(IoTDownWrapper.class);
    if (MapUtil.isNotEmpty(downWrapperMap)) {
      for (Map.Entry<String, IoTDownWrapper> entry : downWrapperMap.entrySet()) {
        IoTDownWrapper iotDownWrapper = entry.getValue();
        R r = iotDownWrapper.beforeDownAction(product, effectiveData, downRequest);
        if (r != null) {
          return r;
        }
      }
    }
    ioTDeviceLifeCycle.create(downRequest.getProductKey(), downRequest.getDeviceId(), downRequest);
    return null;
  }

  /** 调用全局功能函数 如果没有匹配到返回null */
  protected R callGlobalFunction(IoTProduct product, IoTDevice ioTDevice, DownRequest downRequest) {
    Map<String, IoTDownWrapper> downWrapperMap = SpringUtil.getBeansOfType(IoTDownWrapper.class);
    if (MapUtil.isNotEmpty(downWrapperMap)) {
      for (Map.Entry<String, IoTDownWrapper> entry : downWrapperMap.entrySet()) {
        IoTDownWrapper iotDownWrapper = entry.getValue();
        R r = iotDownWrapper.beforeFunctionOrConfigDown(product, ioTDevice, downRequest);
        if (r != null) {
          return r;
        }
      }
    }
    return null;
  }

  /** 设备增加-前置处理 */
  protected void finalDown_Add(
      Map<String, Object> config, IoTProduct product, DownRequest downRequest, Object data) {
    buildCustomField_Add(config, product, downRequest, data);
    buildSubDeviceAddress(config, downRequest);
  }

  /**
   * 设备新增-统一维护租户/部门归属。
   *
   * <p>主动注册优先跟随产品租户；手工新增则使用请求里显式传入的租户和部门。
   */
  public static void finalDown_Add(
      IoTDevice ioTDevice, IoTProduct product, DownRequest downRequest) {
    if (ioTDevice == null || downRequest == null) {
      return;
    }

    if (downRequest.getDeptId() != null) {
      ioTDevice.setDeptId(downRequest.getDeptId());
    }

    String tenantId;
    if (downRequest.isAllowInsert()) {
      tenantId =
          StrUtil.blankToDefault(
              product == null ? null : product.getTenantId(), downRequest.getTenantId());
    } else {
      tenantId = downRequest.getTenantId();
    }
    if (StrUtil.isNotBlank(tenantId)) {
      ioTDevice.setTenantId(tenantId);
    }
  }

  /** 设备更新-后置处理 */
  protected void finalDown_Update(IoTDevice ioTDevice, DownRequest downRequest) {
    buildSubDeviceAddress(ioTDevice, downRequest);
  }

  /** 设置子设备的地址信息 */
  private void buildSubDeviceAddress(Map<String, Object> config, DownRequest downRequest) {
    try {
      if ((DownCmd.DEV_UPDATE.equals(downRequest.getCmd())
              || DownCmd.DEV_ADD.equals(downRequest.getCmd())
              || DownCmd.DEV_ADDS.equals(downRequest.getCmd()))
          && StrUtil.isNotBlank(downRequest.getSlaveAddress())) {
        IoTProduct subDeviceProduct =
            ioTProductMapper.getProductByProductKey(downRequest.getProductKey());
        String slaveAddress = downRequest.getSlaveAddress();
        config.put("slaveAddress", slaveAddress);
        config.put("transportProtocol", subDeviceProduct.getMessageProtocol());
      }
    } catch (Exception e) {
      log.warn("buildSubDevice_Update={}", e);
    }
  }

  /** 设置子设备的地址信息 */
  private void buildSubDeviceAddress(IoTDevice ioTDevice, DownRequest downRequest) {
    try {
      if (ioTDevice != null
          && downRequest != null
          && (DownCmd.DEV_UPDATE.equals(downRequest.getCmd())
              || DownCmd.DEV_ADD.equals(downRequest.getCmd())
              || DownCmd.DEV_ADDS.equals(downRequest.getCmd()))
          && StrUtil.isNotBlank(downRequest.getSlaveAddress())) {
        IoTProduct subDeviceProduct =
            ioTProductMapper.getProductByProductKey(ioTDevice.getProductKey());
        String slaveAddress = downRequest.getSlaveAddress();
        JSONObject cfg = JSONUtil.parseObj(ioTDevice.getConfiguration());
        cfg.set("slaveAddress", slaveAddress);
        if (subDeviceProduct != null) {
          cfg.set("transportProtocol", subDeviceProduct.getMessageProtocol());
        }
        ioTDevice.setConfiguration(JSONUtil.toJsonStr(cfg));
      }
    } catch (Exception e) {
      log.warn("buildSubDevice_Update={}", e);
    }
  }

  /** 设置 */
  private void buildCustomField_Add(
      Map<String, Object> config, IoTProduct product, DownRequest downRequest, Object data) {
    try {
      if (product != null
          && StrUtil.isNotBlank(product.getThirdConfiguration())
          && DownCmd.DEV_ADD.equals(downRequest.getCmd())) {
        JSONObject jsonObject = JSONUtil.parseObj(product.getThirdConfiguration());
        JSONArray customFields = jsonObject.getJSONArray(CUSTOM_FIELD);
        if (jsonObject == null || customFields == null || customFields.size() <= 0) {
          return;
        }
        for (Object obj : customFields) {
          JSONObject object = (JSONObject) obj;
          Object fieldValue =
              DownRequestDataHelper.getFieldValue(downRequest, data, object.getStr("id"));
          config.put(object.getStr("id"), fieldValue);
        }
      }
    } catch (Exception e) {
      log.warn("buildCustomField_Add={}", e);
    }
  }

  /** 保存发送指令 */
  protected void storeCommandToRedis(String productKey, String deviceId, Object data) {
    String value =
        stringRedisTemplate
            .opsForValue()
            .get(IoTConstant.REDIS_STORE_COMMAND + ":" + productKey + ":" + deviceId);
    if (StrUtil.isBlank(value)) {
      stringRedisTemplate
          .opsForValue()
          .set(
              IoTConstant.REDIS_STORE_COMMAND + ":" + productKey + ":" + deviceId,
              JSONUtil.toJsonStr(Stream.of(data).collect(Collectors.toList())),
              3,
              TimeUnit.DAYS);
    } else {
      JSONArray ar = JSONUtil.parseArray(value);
      JSONArray ne = keepLatestFunction(ar, data);
      //      ar.add(data);
      stringRedisTemplate
          .opsForValue()
          .set(
              IoTConstant.REDIS_STORE_COMMAND + ":" + productKey + ":" + deviceId,
              JSONUtil.toJsonStr(ne),
              3,
              TimeUnit.DAYS);
    }
  }

  /** 同一function保留最后一条指令 */
  private JSONArray keepLatestFunction(JSONArray array, Object data) {
    JSONArray result = new JSONArray();
    AtomicBoolean isOldData = new AtomicBoolean(false);
    array.forEach(
        o -> {
          String originFunction = JSONUtil.parseObj(o).getJSONObject("function").getStr("function");
          String latestFunction =
              JSONUtil.parseObj(data).getJSONObject("function").getStr("function");
          if (originFunction.equals(latestFunction)) {
            result.add(data);
            isOldData.set(true);
          } else {
            result.add(o);
          }
        });
    if (!isOldData.get()) {
      result.add(data);
    }
    return result;
  }
}
