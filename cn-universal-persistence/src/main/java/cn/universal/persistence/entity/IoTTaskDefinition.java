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

@Table(name = "iot_task_definition")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "IoT任务定义实体")
public class IoTTaskDefinition implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @KeySql(genId = SQenGenId.class)
  private Long id;

  @Column(name = "definition_no")
  private String definitionNo;

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

  @Column(name = "schedule_type")
  private String scheduleType;

  @Column(name = "schedule_status")
  private String scheduleStatus;

  @Column(name = "cron_expression")
  private String cronExpression;

  @Column(name = "misfire_policy")
  private String misfirePolicy;

  @Column(name = "job_id")
  private Long jobId;

  @Column(name = "last_task_id")
  private Long lastTaskId;

  @Column(name = "last_execute_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date lastExecuteTime;

  @Column(name = "creator_id")
  private String creatorId;

  @Column(name = "tenant_id")
  private String tenantId;

  @Column(name = "creator_name")
  private String creatorName;

  @Column(name = "request_snapshot")
  private String requestSnapshot;

  @Column(name = "ext_json")
  private String extJson;

  @Column(name = "remark")
  private String remark;

  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date createTime;

  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date updateTime;

  @Transient
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date nextExecuteTime;

  @Transient private Date queryBeginTime;
  @Transient private Date queryEndTime;
}
