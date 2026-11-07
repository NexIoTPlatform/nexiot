package cn.universal.persistence.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仪表盘地图设备视图对象
 *
 * @author gitee.com/NexIoT
 * @since 2026/03/28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "仪表盘地图设备视图对象")
public class DashboardMapDeviceVO {

  @Schema(description = "主键ID")
  private Long id;

  @Schema(description = "IoT ID")
  private String iotId;

  @Schema(description = "设备编号")
  private String deviceId;

  @Schema(description = "设备名称")
  private String deviceName;

  @Schema(description = "产品Key")
  private String productKey;

  @Schema(description = "产品名称")
  private String productName;

  @Schema(description = "在线状态")
  private Integer state;

  @Schema(description = "最近在线时间")
  private Long onlineTime;

  @Schema(description = "设备坐标原始值")
  private String coordinate;

  @Schema(description = "经度")
  private Double longitude;

  @Schema(description = "纬度")
  private Double latitude;

  @Schema(description = "第三方平台")
  private String thirdPlatform;

  @Schema(description = "设备节点类型")
  private String deviceNode;
}
