/// *
// *
// * Copyright (c) 2025, NexIoT. All Rights Reserved.
// *
// * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
// * @Author: gitee.com/NexIoT
// * @Email: wo8335224@gmail.com
// * @Wechat: outlookFil
// *
// *
// */
//
// package cn.universal.dm.device.service.task;
//
// import cn.hutool.core.collection.CollectionUtil;
// import cn.hutool.core.date.DateField;
// import cn.hutool.core.date.DateTime;
// import cn.hutool.core.date.DateUtil;
// import cn.universal.common.constant.IoTConstant;
// import cn.universal.dm.device.service.impl.IoTDeviceService;
// import cn.universal.dm.device.service.impl.IoTProductDeviceService;
// import cn.universal.persistence.base.IoTDeviceLifeCycle;
// import cn.universal.persistence.dto.IoTDeviceLogMaxStorageTime;
// import cn.universal.persistence.mapper.IoTDeviceLogMapper;
// import cn.universal.persistence.mapper.IoTDeviceLogMetadataMapper;
// import cn.universal.persistence.mapper.IoTDeviceMapper;
// import jakarta.annotation.Resource;
// import java.util.Date;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.Set;
// import java.util.concurrent.TimeUnit;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.data.redis.core.StringRedisTemplate;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Component;
//
/// **
// * 日志清除任务
// *
// * @author gitee.com/NexIoT
// * @version 1.0
// * @since 2023/2/15
// */
// @Component
// @Slf4j
// public class IoTDeviceLogClearTask {
//
//  private final String KEY = "job:devLogClear";
//
//  @Value("${shard.log.table.number:1}")
//  private int logTableNumber;
//
//  @Resource private StringRedisTemplate stringRedisTemplate;
//
//  @Resource private IoTProductDeviceService iotProductDeviceService;
//
//  @Resource private IoTDeviceMapper ioTDeviceMapper;
//
//  @Resource(name = "ioTDeviceActionAfterService")
//  private IoTDeviceLifeCycle ioTDeviceLifeCycle;
//
//  @Resource private IoTDeviceService iotDeviceService;
//
//  @Resource private IoTDeviceLogMapper ioTDeviceLogMapper;
//
//  @Resource private IoTDeviceLogMetadataMapper ioTDeviceLogMetadataMapper;
//
//  /** 加载第三方平台的生命周期支持情况 */
//  private Map<String, Set<String>> lifecycleSupportedConfig = new HashMap<>();
//
//
// }
