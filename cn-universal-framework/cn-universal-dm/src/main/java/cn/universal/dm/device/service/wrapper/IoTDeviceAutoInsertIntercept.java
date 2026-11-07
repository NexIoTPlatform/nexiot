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

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.ERROR_CODE;
import cn.universal.common.domain.R;
import cn.universal.core.message.DownRequest;
import cn.universal.persistence.base.IoTDownWrapper;
import cn.universal.persistence.base.DownRequestDataHelper;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.persistence.mapper.SupportMapAreasMapper;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 设备自动注册 */
@Service("ioTDeviceAutoInsertIntercept")
@Slf4j
public class IoTDeviceAutoInsertIntercept implements IoTDownWrapper {

  @Resource private IoTDeviceMapper ioTDeviceMapper;
  @Resource private SupportMapAreasMapper supportMapAreasMapper;
  @Resource private IoTUserMapper ioTUserMapper;

  @Value("${iot.register.auto.unionId:auto_register}")
  private String autoRegister;

  @Value("${iot.register.auto.latitude:31.081598}")
  private String latitude;

  @Value("${iot.register.auto.longitude:81.314141}")
  private String longitude;

  @Override
  public R beforeDownAction(IoTProduct product, Object data, DownRequest downRequest) {
    // 产品为空或者没有开启自动注册直接略过
    if (ObjectUtil.isEmpty(product)
        || (JSONUtil.isTypeJSON(product.getConfiguration())
            && !JSONUtil.parseObj(product.getConfiguration())
                .getBool(IoTConstant.ALLOW_INSERT, false))) {
      return null;
    }
    R r = null;
    switch (downRequest.getCmd()) {
      case DEV_ADD:
        r = autoRegisterAdd(product, downRequest, data);
        break;
//      case DEV_DEL:
//        r = autoRegisterRestore(downRequest, data);
//        break;
    }
    return r;
  }

  // 变更为设备自由状态
  private R autoRegisterRestore(DownRequest downRequest, Object data) {
    IoTDevice ioTDevice =
        IoTDevice.builder()
            .productKey(downRequest.getProductKey())
            .deviceId(downRequest.getDeviceId())
            .build();
    // 访问数据库获取设备信息
    IoTDevice dev = ioTDeviceMapper.selectOne(ioTDevice);
    ioTDevice.setId(dev.getId());
    ioTDevice.setDeviceName(downRequest.getDeviceId());
    ioTDevice.setCreatorId(autoRegister);
    ioTDevice.setDeptId(null);
    ioTDevice.setTenantId(null);
    ioTDevice.getParams().put("clearDeptId", true);
    ioTDevice.getParams().put("clearTenantId", true);
    ioTDevice.setCoordinate(StrUtil.join(",", longitude, latitude));
    ioTDevice.setApplication("");
    int row = ioTDeviceMapper.updateDevInstance(ioTDevice);

    //    if (row == 0) {
    //      return R.error(ERROR_CODE.DEV_DEL_ERROR.getCode(), ERROR_CODE.DEV_DEL_ERROR.getName());
    //    }
    //    return R.ok("删除成功");
    // 不做任何返回，才能执行后续的数据库删除
    return null;
  }

  // 主动注册设备更新设备拥有者
  private R autoRegisterAdd(IoTProduct product, DownRequest downRequest, Object data) {
    JSONObject object = DownRequestDataHelper.toJsonObject(data);
    IoTDevice ioTDevice =
        IoTDevice.builder()
            .productKey(downRequest.getProductKey())
            .creatorId(autoRegister)
            .deviceId(downRequest.getDeviceId())
            .build();
    // 访问数据库获取设备信息
    IoTDevice dev = ioTDeviceMapper.selectOne(ioTDevice);
    if (ObjectUtil.isEmpty(dev)) {
      return null;
    }
    // 修改设备信息
    ioTDevice.setId(dev.getId());
    ioTDevice.setCreatorId(downRequest.getAppUnionId());
    ioTDevice.setDeptId(downRequest.getDeptId());
    ioTDevice.setTenantId(resolveTenantId(downRequest.getAppUnionId()));
    ioTDevice.setCreateTime(System.currentTimeMillis() / 1000);
    ioTDevice.setDeviceName(object.getStr("deviceName"));
    ioTDevice.setApplication(downRequest.getApplicationId());
    ioTDevice.setTenantId(product.getTenantId());
    ioTDevice.setDetail(downRequest.getDetail());
    if (StrUtil.isBlank(dev.getDeviceSecret())) {
      ioTDevice.setDeviceSecret(
          buildDeviceSecret(downRequest.getProductKey(), downRequest.getDeviceId()));
    }
    Object extDeviceId = DownRequestDataHelper.getFieldValue(downRequest, data, "extDeviceId");
    ioTDevice.setExtDeviceId(extDeviceId == null ? null : String.valueOf(extDeviceId));
    if (StrUtil.isNotBlank(object.getStr("latitude"))
        && StrUtil.isNotBlank(object.getStr("longitude"))) {
      String latitude = object.getStr("latitude");
      String longitude = object.getStr("longitude");
      ioTDevice.setCoordinate(StrUtil.join(",", longitude, latitude));
    }
    int row = ioTDeviceMapper.updateDevInstance(ioTDevice);
    // 组件返回字段
    if (row <= 0) {
      return R.error(ERROR_CODE.DEV_ADD_ERROR.getCode(), ERROR_CODE.DEV_ADD_ERROR.getName());
    }
    Map<String, Object> result = new HashMap<>();
    result.put("iotId", dev.getIotId());
    result.put("areasId", ioTDevice.getAreasId() == null ? "" : ioTDevice.getAreasId());
    result.put(
        "deviceSecret",
        StrUtil.blankToDefault(dev.getDeviceSecret(), ioTDevice.getDeviceSecret()));
    if (StrUtil.isNotBlank(product.getMetadata())) {
      result.put("metadata", JSONUtil.parseObj(product.getMetadata()));
    }
    result.put("productKey", downRequest.getProductKey());
    result.put("deviceNode", product.getDeviceNode());
    return R.ok(result);
  }

  private String buildDeviceSecret(String productKey, String deviceId) {
    return MD5.create().digestHex(productKey + deviceId + IoTConstant.SALT);
  }

  private String resolveTenantId(String unionId) {
    if (StrUtil.isBlank(unionId)) {
      return null;
    }
    IoTUser iotUser = queryUserByUnionId(unionId);
    if (iotUser == null) {
      return null;
    }
    if (StrUtil.isNotBlank(iotUser.getParentUnionId())) {
      IoTUser parentUser = queryUserByUnionId(iotUser.getParentUnionId());
      if (parentUser != null) {
        iotUser = parentUser;
      }
    }
    return StrUtil.blankToDefault(iotUser.getTenantId(), iotUser.getUnionId());
  }

  private IoTUser queryUserByUnionId(String unionId) {
    if (StrUtil.isBlank(unionId)) {
      return null;
    }
    return ioTUserMapper.selectOne(IoTUser.builder().unionId(unionId).build());
  }
}
