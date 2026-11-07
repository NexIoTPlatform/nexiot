package cn.universal.persistence.entity.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Data;

@Data
@Schema(description = "设备作业参数规则")
public class IoTDeviceJobParamSpec implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "参数模式：FIXED/AUTO/TEMPLATE")
  private String mode;

  @Schema(description = "固定值")
  private Object value;

  @Schema(description = "模板内容")
  private String template;
}
