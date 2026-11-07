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
package cn.universal.web.service.impl;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.web.service.EmqxAuthQueryService;
import java.security.SecureRandom;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

/**
 * EMQX 认证查询服务实现类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j
@Service
public class EmqxAuthQueryServiceImpl implements EmqxAuthQueryService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  @Autowired private IoTProductMapper ioTProductMapper;

  @Autowired private IoTDeviceMapper ioTDeviceMapper;

  @Autowired private IoTUserApplicationMapper ioTUserApplicationMapper;

  @Autowired private IoTUserMapper ioTUserMapper;

  @Value("${iot.register.auto.unionId:auto_register}")
  private String autoRegisterUnionId;

  @Override
  public IoTProduct queryProductByKey(String productKey) {
    try {
      Example example = new Example(IoTProduct.class);
      example
          .createCriteria()
          .andEqualTo("productKey", productKey)
          .andEqualTo("state", 0) // 产品状态正常
          .andEqualTo("isDeleted", 0); // 未删除

      return ioTProductMapper.selectOneByExample(example);
    } catch (Exception e) {
      log.error("查询产品信息失败: productKey={}, error={}", productKey, e.getMessage(), e);
      return null;
    }
  }

  @Override
  public IoTDevice queryDeviceByProductKeyAndDeviceId(String productKey, String deviceId) {
    try {
      Example example = new Example(IoTDevice.class);
      example
          .createCriteria()
          .andEqualTo("productKey", productKey)
          .andEqualTo("deviceId", deviceId);

      return ioTDeviceMapper.selectOneByExample(example);
    } catch (Exception e) {
      log.error(
          "查询设备信息失败: productKey={}, deviceId={}, error={}",
          productKey,
          deviceId,
          e.getMessage(),
          e);
      return null;
    }
  }

  @Override
  public IoTDevice createDeviceByDynamicRegister(IoTProduct product, String deviceId) {
    if (product == null || StrUtil.isBlank(product.getProductKey()) || StrUtil.isBlank(deviceId)) {
      return null;
    }

    IoTDevice existing = queryDeviceByProductKeyAndDeviceId(product.getProductKey(), deviceId);
    if (existing != null) {
      return existing;
    }

    long nowSeconds = System.currentTimeMillis() / 1000;
    IoTUser owner = queryUserByUnionId(product.getCreatorId());
    IoTUser tenantOwner = resolveTenantOwner(owner);

    IoTDevice device =
        IoTDevice.builder()
            .productKey(product.getProductKey())
            .deviceId(deviceId)
            .iotId(product.getProductKey() + deviceId)
            .deviceName(deviceId)
            .deviceSecret(generateDeviceSecret())
            .productName(product.getName())
            .creatorId(autoRegisterUnionId)
            .tenantId(resolveTenantId(product, tenantOwner))
            .deptId(tenantOwner == null ? null : tenantOwner.getDeptId())
            .state(false)
            .createTime(nowSeconds)
            .registryTime((int) nowSeconds)
            .build();

    try {
      int rows = ioTDeviceMapper.insertUseGeneratedKeys(device);
      if (rows <= 0) {
        log.warn(
            "动态注册设备入库失败: productKey={}, deviceId={}", product.getProductKey(), deviceId);
        return null;
      }
      return device;
    } catch (DuplicateKeyException e) {
      log.info(
          "动态注册设备已被并发创建: productKey={}, deviceId={}", product.getProductKey(), deviceId);
      return queryDeviceByProductKeyAndDeviceId(product.getProductKey(), deviceId);
    } catch (Exception e) {
      log.error(
          "动态注册设备失败: productKey={}, deviceId={}, error={}",
          product.getProductKey(),
          deviceId,
          e.getMessage(),
          e);
      return null;
    }
  }

  @Override
  public IoTUserApplication queryApplicationById(String appId) {
    try {
      Example example = new Example(IoTUserApplication.class);
      example
          .createCriteria()
          .andEqualTo("appId", appId)
          .andEqualTo("appStatus", 0) // 应用状态正常
          .andEqualTo("deleted", 0); // 未删除

      return ioTUserApplicationMapper.selectOneByExample(example);
    } catch (Exception e) {
      log.error("查询应用信息失败: appId={}, error={}", appId, e.getMessage(), e);
      return null;
    }
  }

  @Override
  public IoTUser queryUserByUsername(String username) {
    try {
      Example example = new Example(IoTUser.class);
      example
          .createCriteria()
          .andEqualTo("username", username)
          .andEqualTo("status", IoTConstant.NORMAL) // 用户状态正常
          .andEqualTo("deleted", 0); // 未删除

      return ioTUserMapper.selectOneByExample(example);
    } catch (Exception e) {
      log.error("查询用户信息失败: username={}, error={}", username, e.getMessage(), e);
      return null;
    }
  }

  @Override
  public List<IoTUserApplication> queryApplicationsByUnionId(String unionId) {
    try {
      Example example = new Example(IoTUserApplication.class);
      example
          .createCriteria()
          .andEqualTo("unionId", unionId)
          .andEqualTo("appStatus", 0) // 应用状态正常
          .andEqualTo("deleted", 0) // 未删除
          .andEqualTo("mqttEnable", 1); // MQTT启用

      return ioTUserApplicationMapper.selectByExample(example);
    } catch (Exception e) {
      log.error("查询用户关联应用失败: unionId={}, error={}", unionId, e.getMessage(), e);
      return null;
    }
  }

  private IoTUser queryUserByUnionId(String unionId) {
    if (StrUtil.isBlank(unionId)) {
      return null;
    }
    try {
      Example example = new Example(IoTUser.class);
      example
          .createCriteria()
          .andEqualTo("unionId", unionId)
          .andEqualTo("status", IoTConstant.NORMAL)
          .andEqualTo("deleted", 0);
      return ioTUserMapper.selectOneByExample(example);
    } catch (Exception e) {
      log.error("查询产品归属用户失败: unionId={}, error={}", unionId, e.getMessage(), e);
      return null;
    }
  }

  private IoTUser resolveTenantOwner(IoTUser owner) {
    if (owner == null || StrUtil.isBlank(owner.getParentUnionId())) {
      return owner;
    }
    IoTUser parent = queryUserByUnionId(owner.getParentUnionId());
    return parent == null ? owner : parent;
  }

  private String resolveTenantId(IoTProduct product, IoTUser tenantOwner) {
    if (StrUtil.isNotBlank(product.getTenantId())) {
      return product.getTenantId();
    }
    if (tenantOwner == null) {
      return product.getCreatorId();
    }
    return StrUtil.blankToDefault(tenantOwner.getTenantId(), tenantOwner.getUnionId());
  }

  private String generateDeviceSecret() {
    byte[] bytes = new byte[16];
    SECURE_RANDOM.nextBytes(bytes);
    return HexUtil.encodeHexStr(bytes);
  }
}
