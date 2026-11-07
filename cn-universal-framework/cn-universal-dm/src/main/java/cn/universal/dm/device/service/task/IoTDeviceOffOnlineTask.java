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

package cn.universal.dm.device.service.task;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.DevLifeCycle;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.persistence.base.IoTDeviceLifeCycle;
import cn.universal.persistence.dto.IoTDeviceOfflineThesholdBO;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 离线定制任务
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/2/9
 */
@Component
@Slf4j
public class IoTDeviceOffOnlineTask {

  /** 离线定制任务redis-key */
  private final String KEY = "job:Offline:task:";

  @Resource private StringRedisTemplate stringRedisTemplate;

  @Resource private IoTProductDeviceService iotProductDeviceService;

  @Resource private IoTDeviceMapper ioTDeviceMapper;

  @Resource(name = "ioTDeviceActionAfterService")
  private IoTDeviceLifeCycle ioTDeviceLifeCycle;

  /** 加载第三方平台的生命周期支持情况 */
  private Map<String, Set<String>> lifecycleSupportedConfig = new HashMap<>();

  @Autowired
  void setLifecycleConfig(
      @Value("${universal.iot.third.lifecycle.supported:{\"ctaiot\":[]}}")
          String lifecyclesupported) {
    if (StrUtil.isBlank(lifecyclesupported)) {
      return;
    }
    JSONObject obj = JSONUtil.parseObj(lifecyclesupported);
    obj.keySet().stream()
        .forEach(
            s -> {
              lifecycleSupportedConfig.put(
                  s,
                  obj.getJSONArray(s).stream().map(c -> c.toString()).collect(Collectors.toSet()));
            });
  }

  /** 校验其他平台是否支持设备上线离线的生命周期 */
  public boolean thirdSupport(String platform, DevLifeCycle devLifeCycle) {
    if (lifecycleSupportedConfig.containsKey(platform)) {
      return lifecycleSupportedConfig.get(platform).contains(devLifeCycle.name());
    }
    return false;
  }

  @Scheduled(cron = "0 0/2 * * * ?")
  public void doOfflineTask() {
    try {
      boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(KEY, "0", 5, TimeUnit.MINUTES);
      if (!flag) {
        return;
      }
      log.debug("开始执行设备离线任务");
      // 查询所有产品的离线配置项
      List<IoTDeviceOfflineThesholdBO> productOfflineThresholds =
          iotProductDeviceService.getProductOfflineThresholds();
      if (CollectionUtil.isNotEmpty(productOfflineThresholds)) {
        // 开始按照不同产品
        for (IoTDeviceOfflineThesholdBO s : productOfflineThresholds) {
          if (s == null) {
            log.warn("getOfflineThreshold null s={}", productOfflineThresholds);
            continue;
          }
          long currentSeconds = DateUtil.currentSeconds();
          // 单位分钟，转换为秒
          long configThresholds = s.getOfflineThreshold() * 60;
          // 差额
          long difference = currentSeconds - configThresholds;
          // 最后一次通信时间（online_time）比差额还小，则离线
          List<String> deviceIds =
              ioTDeviceMapper.selectOfflineThresholdIotIds(s.getProductKey(), difference);
          if (CollectionUtil.isEmpty(deviceIds)) {
            continue;
          }
          // 第三方不支持生命周期的，进入方法
          if (!thirdSupport(s.getPlatform(), DevLifeCycle.offline)) {
            deviceIds.forEach(
                deviceId -> {
                  // 执行离线事件
                  log.info(
                      "productKey={} ,deviceId={} ,当前时间={},设定离线值(min)={},与当前差额={} 满足条件",
                      s.getProductKey(),
                      deviceId,
                      currentSeconds,
                      configThresholds,
                      difference);
                  ioTDeviceLifeCycle.offline(s.getProductKey(), deviceId);
                });
          } else {
            log.info("有第三方的配置，productKey={}", s.getProductKey());
          }
        }
      }
    } catch (Exception e) {
      log.error("设备生命周期执行异常={}", e);
    }

    log.debug("结束执行设备离线任务");
  }
}
