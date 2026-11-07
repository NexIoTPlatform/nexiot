package cn.universal.persistence.entity.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "设备三元组生成任务提交参数")
public class IoTDeviceCredentialGenerateTaskBO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "任务名称")
  private String taskName;

  @Schema(description = "产品Key")
  private String productKey;

  @Schema(description = "生成模式：AUTO/LIST")
  private String generateMode;

  @Schema(description = "自动生成数量")
  private Integer count;

  @Schema(description = "设备ID前缀")
  private String deviceIdPrefix;

  @Schema(description = "设备ID起始序号")
  private Long deviceIdStartNo;

  @Schema(description = "设备ID序号补零长度")
  private Integer deviceIdLength;

  @Schema(description = "设备名称前缀")
  private String deviceNamePrefix;

  @Schema(description = "指定设备ID列表")
  private List<String> deviceIds = new ArrayList<>();

  @Schema(description = "指定设备ID文本，支持逗号、空格、换行分隔")
  private String deviceIdsText;

  @Schema(description = "归属应用")
  private String application;

  @Schema(description = "部门ID")
  private Long deptId;

  @Schema(description = "设备备注")
  private String detail;
}
