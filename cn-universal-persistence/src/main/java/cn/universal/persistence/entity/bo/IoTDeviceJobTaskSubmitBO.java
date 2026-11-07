package cn.universal.persistence.entity.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
@Schema(description = "设备作业任务提交参数")
public class IoTDeviceJobTaskSubmitBO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "任务名称")
  private String taskName;

  @Schema(description = "产品Key")
  private String productKey;

  @Schema(description = "归属应用")
  private String application;

  @Schema(description = "设备分组ID")
  private Long devGroupId;

  @Schema(description = "设备关键字（设备序列号/设备名称）")
  private String keyword;

  @Schema(description = "手动选择的设备ID数组")
  private String[] ids;

  @Schema(description = "是否对当前筛选结果全部设备生效")
  private Boolean chooseAll;

  @Schema(description = "功能标识")
  private String functionId;

  @Schema(description = "服务类型")
  private String serviceType;

  @Schema(description = "功能来源")
  private String source;

  @Schema(description = "功能参数规则")
  private Map<String, IoTDeviceJobParamSpec> functionArgsSpec = new LinkedHashMap<>();

  @Schema(description = "功能参数")
  private Map<String, Object> functionData = new LinkedHashMap<>();

  @Schema(description = "指令执行方式：SERIAL/PARALLEL")
  private String commandExecutionMode;

  @Schema(description = "多条指令配置")
  private List<IoTDeviceJobCommandBO> commands = new ArrayList<>();

  @Schema(description = "调度类型：MANUAL/CRON")
  private String scheduleType;

  @Schema(description = "调度状态：ENABLED/PAUSED")
  private String scheduleStatus;

  @Schema(description = "Cron表达式")
  private String cronExpression;

  @Schema(description = "misfire策略（0默认 1立即触发 2触发一次 3忽略）")
  private String misfirePolicy;
}
