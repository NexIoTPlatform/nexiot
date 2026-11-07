package cn.universal.dm.device.service.sub.processor;

import cn.universal.dm.device.service.sub.context.SubDeviceRequest;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 子设备处理器责任链管理器 负责管理和执行子设备处理器的责任链
 *
 * @author system
 * @date 2025-01-16
 */
@Slf4j
@Component
public class SubDeviceProcessorChain {

  @Autowired private List<SubDeviceMessageProcessor> processors;

  private List<SubDeviceMessageProcessor> sortedProcessors;

  @PostConstruct
  public void init() {
    // 按order和priority排序处理器
    sortedProcessors = new ArrayList<>(processors);
    sortedProcessors.sort(
        Comparator.comparingInt(SubDeviceMessageProcessor::getOrder)
            .thenComparingInt(SubDeviceMessageProcessor::getPriority));

    log.info("子设备处理器链初始化完成，处理器数量: {}", sortedProcessors.size());
    for (SubDeviceMessageProcessor processor : sortedProcessors) {
      log.info(
          "  - {} (order: {}, priority: {}, required: {})",
          processor.getName(),
          processor.getOrder(),
          processor.getPriority(),
          processor.isRequired());
    }
  }

  /**
   * 执行子设备处理链
   *
   * @param context 子设备上下文
   * @return 处理结果
   */
  public ProcessResult process(SubDeviceRequest context) {
    if (context == null) {
      log.warn("子设备上下文为空，跳过处理");
      return ProcessResult.ERROR;
    }

    log.debug("开始执行子设备处理链: {}", context.getGwDeviceId());

    int processedCount = 0;
    int skippedCount = 0;
    int errorCount = 0;

    for (SubDeviceMessageProcessor processor : sortedProcessors) {
      try {
        // 检查处理器是否支持
        if (!processor.supports(context)) {
          log.debug("处理器 {} 不支持当前上下文，跳过", processor.getName());
          skippedCount++;
          continue;
        }

        // 检查处理器是否启用（默认启用）
        // if (!processor.isEnabled()) {
        //     log.debug("处理器 {} 未启用，跳过", processor.getName());
        //     skippedCount++;
        //     continue;
        // }

        // 预检查
        if (!processor.preCheck(context)) {
          log.debug("处理器 {} 预检查失败，跳过", processor.getName());
          skippedCount++;
          continue;
        }

        // 执行处理
        log.debug("执行处理器: {} (order: {})", processor.getName(), processor.getOrder());
        SubDeviceMessageProcessor.ProcessorResult result = processor.process(context);

        // 后置处理
        processor.postProcess(context, result);

        processedCount++;

        // 根据处理结果决定是否继续
        switch (result) {
          case CONTINUE:
            log.debug("处理器 {} 处理完成，继续下一个", processor.getName());
            break;
          case STOP:
            log.debug("处理器 {} 处理完成，停止处理链", processor.getName());
            context.setStage(SubDeviceRequest.ProcessingStage.COMPLETED);
            context.setSuccess(true);
            return ProcessResult.SUCCESS;
          case SKIP:
            log.debug("处理器 {} 跳过处理", processor.getName());
            skippedCount++;
            break;
          case ERROR:
            log.error("处理器 {} 处理失败", processor.getName());
            context.setStage(SubDeviceRequest.ProcessingStage.FAILED);
            context.setSuccess(false);
            return ProcessResult.ERROR;
        }

      } catch (Exception e) {
        log.error("处理器 {} 执行异常", processor.getName(), e);
        processor.onError(context, e);
        errorCount++;

        // 如果是必需的处理器出错，停止处理链
        if (processor.isRequired()) {
          context.setStage(SubDeviceRequest.ProcessingStage.FAILED);
          context.setSuccess(false);
          return ProcessResult.ERROR;
        }
      }
    }

    // 所有处理器执行完成
    context.setStage(SubDeviceRequest.ProcessingStage.COMPLETED);
    context.setSuccess(true);

    log.debug(
        "子设备处理链执行完成: {} (处理: {}, 跳过: {}, 错误: {})",
        context.getGwDeviceId(),
        processedCount,
        skippedCount,
        errorCount);

    return errorCount > 0 ? ProcessResult.PARTIAL_SUCCESS : ProcessResult.SUCCESS;
  }

  /**
   * 批量执行子设备处理链
   *
   * @param contexts 子设备上下文列表
   * @return 批量处理结果
   */
  public BatchProcessResult processBatch(List<SubDeviceRequest> contexts) {
    if (contexts == null || contexts.isEmpty()) {
      log.debug("子设备上下文列表为空，跳过批量处理");
      return new BatchProcessResult(0, 0, 0, 0);
    }

    log.debug("开始批量执行子设备处理链，数量: {}", contexts.size());

    int totalCount = contexts.size();
    int successCount = 0;
    int errorCount = 0;
    int partialSuccessCount = 0;

    for (SubDeviceRequest context : contexts) {
      try {
        ProcessResult result = process(context);
        switch (result) {
          case SUCCESS:
            successCount++;
            break;
          case ERROR:
            errorCount++;
            break;
          case PARTIAL_SUCCESS:
            partialSuccessCount++;
            break;
        }
      } catch (Exception e) {
        log.error("批量处理中子设备处理失败: {}", context.getDeviceId(), e);
        errorCount++;
      }
    }

    BatchProcessResult batchResult =
        new BatchProcessResult(totalCount, successCount, errorCount, partialSuccessCount);
    log.info("批量执行子设备处理链完成: {}", batchResult);

    return batchResult;
  }

  /** 获取处理器列表 */
  public List<SubDeviceMessageProcessor> getProcessors() {
    return new ArrayList<>(sortedProcessors);
  }

  /** 获取处理器统计信息 */
  public String getStatistics() {
    StringBuilder sb = new StringBuilder();
    sb.append("子设备处理器链统计:\n");
    sb.append("  总处理器数量: ").append(sortedProcessors.size()).append("\n");

    for (SubDeviceMessageProcessor processor : sortedProcessors) {
      sb.append("  - ")
          .append(processor.getName())
          .append(" (order: ")
          .append(processor.getOrder())
          .append(", priority: ")
          .append(processor.getPriority())
          .append(", required: ")
          .append(processor.isRequired())
          .append(", enabled: true")
          .append(")\n");
    }

    return sb.toString();
  }

  /** 处理结果枚举 */
  public enum ProcessResult {
    SUCCESS, // 完全成功
    PARTIAL_SUCCESS, // 部分成功
    ERROR // 失败
  }

  /** 批量处理结果 */
  public static class BatchProcessResult {
    private final int totalCount;
    private final int successCount;
    private final int errorCount;
    private final int partialSuccessCount;

    public BatchProcessResult(
        int totalCount, int successCount, int errorCount, int partialSuccessCount) {
      this.totalCount = totalCount;
      this.successCount = successCount;
      this.errorCount = errorCount;
      this.partialSuccessCount = partialSuccessCount;
    }

    public int getTotalCount() {
      return totalCount;
    }

    public int getSuccessCount() {
      return successCount;
    }

    public int getErrorCount() {
      return errorCount;
    }

    public int getPartialSuccessCount() {
      return partialSuccessCount;
    }

    @Override
    public String toString() {
      return String.format(
          "BatchProcessResult{total=%d, success=%d, error=%d, partial=%d}",
          totalCount, successCount, errorCount, partialSuccessCount);
    }
  }
}
