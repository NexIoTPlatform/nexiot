package cn.universal.persistence.entity.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

@Data
@Schema(description = "设备作业指令配置")
public class IoTDeviceJobCommandBO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "指令标识，前端可传入用于回显排序")
  private String commandId;

  @Schema(description = "功能标识")
  private String functionId;

  @Schema(description = "服务类型")
  private String serviceType;

  @Schema(description = "功能来源")
  private String source;

  @Schema(description = "功能参数规则")
  private Map<String, IoTDeviceJobParamSpec> functionArgsSpec = new LinkedHashMap<>();

  @Schema(description = "功能参数预览值")
  private Map<String, Object> functionData = new LinkedHashMap<>();
}
