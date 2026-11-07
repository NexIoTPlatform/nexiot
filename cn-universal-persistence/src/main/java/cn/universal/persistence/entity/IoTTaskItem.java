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

@Table(name = "iot_task_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "IoT通用任务明细实体")
public class IoTTaskItem implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @KeySql(genId = SQenGenId.class)
  private Long id;

  @Column(name = "task_id")
  private Long taskId;

  @Column(name = "tenant_id")
  private String tenantId;

  @Column(name = "item_no")
  private String itemNo;

  @Column(name = "item_key")
  private String itemKey;

  @Column(name = "item_name")
  private String itemName;

  @Column(name = "row_no")
  private Integer rowNo;

  private String status;

  private String message;

  @Column(name = "raw_data")
  private String rawData;

  @Column(name = "result_data")
  private String resultData;

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

  @Transient private String keyword;
}
