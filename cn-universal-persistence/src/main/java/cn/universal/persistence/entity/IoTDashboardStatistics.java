package cn.universal.persistence.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * 仪表盘统计实体
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "iot_dashboard_statistics")
@Schema(description = "IoT仪表盘统计实体")
public class IoTDashboardStatistics implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 统计日期 */
  @Column(name = "stat_date")
  @Schema(description = "统计日期")
  private LocalDate statDate;

  /** 产品Key，NULL表示全产品 */
  @Column(name = "product_key")
  @Schema(description = "产品Key")
  private String productKey;

  /** 推送渠道，NULL表示全渠道 */
  @Column(name = "channel")
  @Schema(description = "推送渠道")
  private String channel;

  /** 指标类型 */
  @Column(name = "metric_type")
  @Schema(description = "指标类型")
  private String metricType;

  /** 指标值 */
  @Column(name = "metric_value")
  @Schema(description = "指标值")
  private Long metricValue;

  /** 创建时间 */
  @Column(name = "create_time")
  @Schema(description = "创建时间")
  private LocalDateTime createTime;

  /** 更新时间 */
  @Column(name = "update_time")
  @Schema(description = "更新时间")
  private LocalDateTime updateTime;

  /** 指标类型枚举 */
  public enum MetricType {
    DEVICE_TOTAL("device_total", "设备总数"),
    DEVICE_ONLINE("device_online", "在线设备数"),
    MESSAGE_TOTAL("message_total", "消息总数"),
    MESSAGE_SUCCESS("message_success", "成功消息数"),
    MESSAGE_FAILED("message_failed", "失败消息数"),
    MESSAGE_RETRY("message_retry", "重试消息数"),
    MESSAGE_PUSH("message_push", "推送消息数");

    private final String code;
    private final String description;

    MetricType(String code, String description) {
      this.code = code;
      this.description = description;
    }

    public String getCode() {
      return code;
    }

    public String getDescription() {
      return description;
    }
  }
}
