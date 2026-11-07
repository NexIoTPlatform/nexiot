package cn.universal.dm.device.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统计管理器状态DTO
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsStatusDTO {

  /**
   * 管理器是否运行中
   */
  private Boolean running;

  /**
   * 待处理消息数量
   */
  private Long pendingMessages;

  /**
   * 已处理消息数量
   */
  private Long processedMessages;

  /**
   * 失败消息数量
   */
  private Long failedMessages;

  /**
   * 最后刷新时间
   */
  private Long lastFlushTime;

  /**
   * 最后刷新时间（格式化）
   */
  private String lastFlushTimeFormatted;

  /**
   * 缓存大小
   */
  private Integer cacheSize;

  /**
   * 时间戳
   */
  private Long timestamp;
}

