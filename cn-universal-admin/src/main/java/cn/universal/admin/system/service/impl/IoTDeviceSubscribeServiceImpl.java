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

package cn.universal.admin.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.common.service.BaseServiceImpl;
import cn.universal.admin.platform.service.IIoTDeviceService;
import cn.universal.admin.system.service.IoTDeviceSubscribeService;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.DeviceSubscribe;
import cn.universal.common.domain.R;
import cn.universal.common.exception.IoTErrorCode;
import cn.universal.common.exception.IoTException;
import cn.universal.dm.device.service.impl.IoTCacheRemoveService;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTDeviceSubscribe;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTDeviceSubscribeMapper;
import cn.universal.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * subscribe @Author ruoyi
 *
 * @since 2023-01-06
 */
@Service
public class IoTDeviceSubscribeServiceImpl extends BaseServiceImpl
    implements IoTDeviceSubscribeService {

  @Resource private IoTDeviceSubscribeMapper ioTDeviceSubscribeMapper;

  @Resource private IoTDeviceMapper ioTDeviceMapper;

  @Resource private IoTCacheRemoveService iotCacheRemoveService;

  @Resource private IIoTDeviceService iIoTDeviceService;

  @Override
  public List<IoTDeviceSubscribe> selectDevSubscribeList(IoTDeviceSubscribe sub) {
    requireAccessibleDevice(sub == null ? null : sub.getProductKey(), sub == null ? null : sub.getIotId());
    List<IoTDeviceSubscribe> subscribes = ioTDeviceSubscribeMapper.selectSubscribesBO(sub);
    subscribes.forEach(
        (item) -> {
          if (StrUtil.isNotBlank(item.getUrl()) && StrUtil.isNotBlank(item.getTopic())) {
            item.setUrl(item.getUrl() + "," + item.getTopic());
          } else if (StrUtil.isNotBlank(item.getUrl())) {
            item.setUrl(item.getUrl());
          } else if (StrUtil.isNotBlank(item.getTopic())) {
            item.setUrl(item.getTopic());
          }
        });
    return subscribes;
  }

  @Override
  public IoTDeviceSubscribe selectDevInstanceById(String id) {
    IoTDeviceSubscribe subscribe = ioTDeviceSubscribeMapper.selectByPrimaryKey(id);
    requireAccessibleSubscription(subscribe);
    return subscribe;
  }

  @Override
  public R updateDevSubscribe(IoTDeviceSubscribe sub) {
    IoTDeviceSubscribe stored = ioTDeviceSubscribeMapper.selectByPrimaryKey(sub.getId());
    if (stored == null) {
      return R.error("更新失败,订阅不存在");
    }
    requireAccessibleSubscription(stored);
    sub.setProductKey(stored.getProductKey());
    sub.setIotId(stored.getIotId());
    sub.setDeviceId(stored.getDeviceId());
    sub.setSubType(stored.getSubType());
    sub.setCreater(stored.getCreater());
    sub.setCreateDate(stored.getCreateDate());
    int update = ioTDeviceSubscribeMapper.updateByPrimaryKey(sub);
    if (update >= 1) {
      iotCacheRemoveService.removeIotDeviceSubscribeCache();
      return R.ok();
    }
    return R.error("更新失败");
  }

  @Override
  public R insertDevSubscribe(IoTDeviceSubscribe sub) {
    IoTDeviceSubscribe build =
        IoTDeviceSubscribe.builder()
            .subType(DeviceSubscribe.DEVICE.name())
            .iotId(sub.getIotId())
            .productKey(sub.getProductKey())
            .build();
    int i = ioTDeviceSubscribeMapper.selectCount(build);
    if (i > IoTConstant.MAX_DEV_MSG_SUBSCRIBE_NUM) {
      return R.error(
          IoTErrorCode.DEV_SUBSCRIBE_REPEAT_ERROR.getCode(),
          IoTErrorCode.DEV_SUBSCRIBE_REPEAT_ERROR.getName());
    }
    IoTDevice ioTDevice = requireAccessibleDevice(sub.getProductKey(), sub.getIotId());
    sub.setDeviceId(ioTDevice.getDeviceId());
    sub.setCreater(SecurityUtils.getUnionId());
    sub.setInstance("0");

    sub.setCreateDate(new Date());
    sub.setEnabled(IoTDeviceSubscribe.ENABLED_YES);
    int insert = ioTDeviceSubscribeMapper.insert(sub);
    if (insert >= 1) {
      iotCacheRemoveService.removeIotDeviceSubscribeCache();
      return R.ok();
    }
    return R.error("添加失败");
  }

  @Override
  public R deleteDevSubscribe(String productKey, String iotId, Long[] ids) {
    requireAccessibleDevice(productKey, iotId);
    List<Long> verifiedIds = new ArrayList<>();
    for (Long id : ids) {
      IoTDeviceSubscribe subscribe = ioTDeviceSubscribeMapper.selectByPrimaryKey(id);
      if (subscribe == null) {
        return R.error("删除失败,订阅不存在");
      }
      requireAccessibleSubscription(subscribe);
      if (!StrUtil.equals(productKey, subscribe.getProductKey())
          || !StrUtil.equals(iotId, subscribe.getIotId())) {
        return R.error("删除失败,订阅归属不匹配");
      }
      verifiedIds.add(id);
    }
    StringBuffer str = new StringBuffer();
    for (int i = 0; i < verifiedIds.size(); i++) {
      str.append(verifiedIds.get(i));
      if ((i + 1) < verifiedIds.size()) {
        str.append(",");
      }
    }
    int delete = verifiedIds.isEmpty() ? 0 : ioTDeviceSubscribeMapper.deleteByIds(str.toString());
    if (delete >= 1) {
      iotCacheRemoveService.removeIotDeviceSubscribeCache();
      return R.ok();
    }
    return R.error("删除失败");
  }

  private IoTDevice requireAccessibleDevice(String productKey, String iotId) {
    IoTDevice ioTDevice =
        ioTDeviceMapper.selectOne(IoTDevice.builder().iotId(iotId).productKey(productKey).build());
    if (Objects.isNull(ioTDevice)) {
      throw new IoTException("设备不存在");
    }
    iIoTDeviceService.selectDevInstanceById(String.valueOf(ioTDevice.getId()));
    return ioTDevice;
  }

  private void requireAccessibleSubscription(IoTDeviceSubscribe subscribe) {
    if (subscribe == null) {
      throw new IoTException("订阅不存在");
    }
    if (StrUtil.isBlank(subscribe.getProductKey())
        || (StrUtil.isBlank(subscribe.getIotId()) && StrUtil.isBlank(subscribe.getDeviceId()))) {
      throw new IoTException("订阅关联设备信息缺失");
    }
    if (StrUtil.isNotBlank(subscribe.getIotId())) {
      requireAccessibleDevice(subscribe.getProductKey(), subscribe.getIotId());
      return;
    }
    IoTDevice ioTDevice =
        ioTDeviceMapper.selectOne(
            IoTDevice.builder()
                .productKey(subscribe.getProductKey())
                .deviceId(subscribe.getDeviceId())
                .build());
    if (Objects.isNull(ioTDevice)) {
      throw new IoTException("订阅关联设备不存在");
    }
    iIoTDeviceService.selectDevInstanceById(String.valueOf(ioTDevice.getId()));
  }
}
