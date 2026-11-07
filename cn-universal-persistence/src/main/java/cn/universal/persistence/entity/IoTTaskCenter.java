package cn.universal.persistence.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

@Table(name = "iot_task_center")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "IoT通用任务中心实体")
public class IoTTaskCenter implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @KeySql(genId = SQenGenId.class)
  private Long id;

  @Column(name = "task_no")
  private String taskNo;

  @Column(name = "task_type")
  private String taskType;

  @Column(name = "task_name")
  private String taskName;

  @Column(name = "biz_module")
  private String bizModule;

  @Column(name = "biz_key")
  private String bizKey;

  @Column(name = "product_key")
  private String productKey;

  @Column(name = "product_name")
  private String productName;

  @Column(name = "definition_id")
  private Long definitionId;

  @Column(name = "trigger_type")
  private String triggerType;

  private String status;

  @Column(name = "total_count")
  private Integer totalCount;

  @Column(name = "success_count")
  private Integer successCount;

  @Column(name = "fail_count")
  private Integer failCount;

  @Column(name = "creator_id")
  private String creatorId;

  @Column(name = "tenant_id")
  private String tenantId;

  @Column(name = "creator_name")
  private String creatorName;

  @Column(name = "request_snapshot")
  private String requestSnapshot;

  @Column(name = "result_snapshot")
  private String resultSnapshot;

  @Column(name = "ext_json")
  private String extJson;

  @Column(name = "error_message")
  private String errorMessage;

  @Column(name = "start_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date startTime;

  @Column(name = "end_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date endTime;

  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date createTime;

  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date updateTime;

  @Transient private Date queryBeginTime;
  @Transient private Date queryEndTime;
}
