package cn.universal.persistence.dto;

import cn.universal.common.annotation.Excel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设备三元组导出数据")
public class IoTDeviceCredentialExportDTO {

  @Excel(name = "任务编号", sort = 1)
  @Schema(description = "任务编号")
  private String taskNo;

  @Excel(name = "产品名称", sort = 2)
  @Schema(description = "产品名称")
  private String productName;

  @Excel(name = "ProductKey", sort = 3)
  @Schema(description = "产品Key")
  private String productKey;

  @Excel(name = "DeviceId", sort = 4)
  @Schema(description = "设备ID")
  private String deviceId;

  @Excel(name = "设备名称", sort = 5)
  @Schema(description = "设备名称")
  private String deviceName;

  @Excel(name = "IoTId", sort = 6)
  @Schema(description = "IoT ID")
  private String iotId;

  @Excel(name = "ClientID", sort = 7)
  @Schema(description = "MQTT ClientID")
  private String clientId;

  @Excel(name = "Username", sort = 8)
  @Schema(description = "MQTT Username")
  private String username;

  @Excel(name = "Password", sort = 9)
  @Schema(description = "MQTT Password")
  private String password;

  @Excel(name = "DeviceSecret", sort = 10)
  @Schema(description = "设备密钥")
  private String deviceSecret;

  @Excel(name = "创建时间", sort = 11)
  @Schema(description = "创建时间")
  private String createTime;
}
