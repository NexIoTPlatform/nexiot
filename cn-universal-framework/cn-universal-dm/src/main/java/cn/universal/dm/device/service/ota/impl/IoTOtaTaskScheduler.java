/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA任务调度器
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.dm.device.service.ota.impl;

import cn.universal.dm.device.service.ota.api.IoTOtaRuntimeService;
import cn.universal.persistence.entity.IoTOtaTaskDevice;
import cn.universal.persistence.mapper.IoTOtaTaskDeviceMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 周期扫描 OTA 待执行任务和分批续推任务。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IoTOtaTaskScheduler {

  private final IoTOtaRuntimeService otaRuntimeService;
  private final OtaTransferService otaTransferService;
  private final IoTOtaTaskDeviceMapper otaTaskDeviceMapper;

  @Scheduled(fixedDelayString = "${ota.task.scan-interval-ms:30000}")
  public void scanTasks() {
    try {
      otaRuntimeService.processScheduledTasks();
    } catch (Exception e) {
      log.warn("OTA任务调度扫描失败: {}", e.getMessage());
    }
  }

  /** 逐片确认有独立扫描周期；重启时读取数据库中的在途片继续执行。 */
  @Scheduled(fixedDelayString = "${ota.transfer.scan-interval-ms:1000}")
  public void scanTransfers() {
    List<IoTOtaTaskDevice> devices = otaTaskDeviceMapper.selectActiveTransfers();
    if (devices == null) return;
    for (IoTOtaTaskDevice device : devices) {
      try {
        // 已传完、等待安装的设备由任务超时扫描管理，无需每秒获取行锁。
        if (otaTransferService.hasIncompleteTransfer(device)
            && otaTransferService.resume(device.getId())) {
          otaRuntimeService.refreshTaskStats(device.getTaskId());
        }
      } catch (Exception e) {
        log.warn("OTA分包恢复失败, taskDeviceId={}, error={}", device.getId(), e.getMessage());
      }
    }
  }
}
