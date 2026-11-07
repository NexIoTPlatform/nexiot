package cn.universal.persistence.entity.vo;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IoTTaskSubmitResultVO implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long taskId;
  private String taskNo;
  private String taskType;
  private String taskName;
  private String status;
}
