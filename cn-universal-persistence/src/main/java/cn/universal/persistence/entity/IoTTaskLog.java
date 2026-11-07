package cn.universal.persistence.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

@Table(name = "iot_task_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "IoT通用任务日志实体")
public class IoTTaskLog implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @KeySql(genId = SQenGenId.class)
  private Long id;

  @Column(name = "task_id")
  private Long taskId;

  @Column(name = "log_level")
  private String logLevel;

  @Column(name = "log_type")
  private String logType;

  private String content;

  @Column(name = "ext_json")
  private String extJson;

  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date createTime;
}
