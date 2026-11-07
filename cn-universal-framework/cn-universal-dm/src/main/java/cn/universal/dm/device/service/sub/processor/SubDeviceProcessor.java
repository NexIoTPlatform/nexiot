package cn.universal.dm.device.service.sub.processor;

import cn.universal.dm.device.service.sub.context.SubDeviceRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 子设备处理器 负责子设备的完整处理流程，使用责任链模式管理各个处理步骤
 *
 * @author system
 * @date 2025-01-16
 */
@Slf4j
@Component
public class SubDeviceProcessor {

  @Autowired private SubDeviceProcessorChain processorChain;

  /**
   * 批量处理子设备上下文
   *
   * @param subDeviceRequests 子设备上下文列表
   */
  public void processBatch(List<SubDeviceRequest> subDeviceRequests) {
    if (subDeviceRequests == null || subDeviceRequests.isEmpty()) {
      log.debug("子设备上下文列表为空，跳过批量处理");
      return;
    }

    log.debug("开始批量处理子设备，数量: {}", subDeviceRequests.size());

    // 使用责任链批量处理
    SubDeviceProcessorChain.BatchProcessResult result =
        processorChain.processBatch(subDeviceRequests);

    log.info("批量处理子设备完成: {}", result);
  }
}
