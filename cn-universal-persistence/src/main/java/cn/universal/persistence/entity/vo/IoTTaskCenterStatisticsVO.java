package cn.universal.persistence.entity.vo;

import java.io.Serializable;
import lombok.Data;

@Data
public class IoTTaskCenterStatisticsVO implements Serializable {

  private static final long serialVersionUID = 1L;

  private Integer totalCount;
  private Integer successCount;
  private Integer failCount;
  private Integer pendingCount;
  private Integer runningCount;
}
