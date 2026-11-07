package cn.universal.persistence.entity.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Data;

@Data
@Schema(description = "子设备网关绑定请求")
public class IoTSubDeviceGatewayBindBO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "目标网关产品Key")
  private String gwProductKey;

  @Schema(description = "目标网关设备ID")
  private String gwDeviceId;
}
