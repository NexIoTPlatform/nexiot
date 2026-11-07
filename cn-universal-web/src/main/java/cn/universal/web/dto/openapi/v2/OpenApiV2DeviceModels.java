package cn.universal.web.dto.openapi.v2;

import cn.universal.persistence.entity.IoTDevice;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

/** V2 使用独立 DTO，不把持久化实体、设备密钥或归属控制字段暴露为开放接口参数。 */
public final class OpenApiV2DeviceModels {
  private OpenApiV2DeviceModels() {}

  @Schema(name = "V2CreateDevice", description = "创建并绑定到当前应用；无需传 cmd、租户、创建者和 applicationId。")
  public record CreateDevice(
      @NotBlank
          @Size(max = 100)
          @Schema(description = "设备唯一编号，产品内唯一；子设备可能由既有协议按网关与从站地址生成", example = "sensor-001")
          String deviceId,
      @NotBlank @Size(max = 128) @Schema(description = "设备名称", example = "车间温湿度计")
          String deviceName,
      @Size(max = 1000) @Schema(description = "设备说明") String detail,
      @Size(max = 100) @Schema(description = "第三方设备编号；子设备如传入，必须等于父网关gwDeviceId，可省略")
          String extDeviceId,
      @Size(max = 100) @Schema(description = "父网关产品Key；子设备需指定或由产品配置提供") String gwProductKey,
      @Size(max = 100) @Schema(description = "父网关设备编号；必须已绑定当前应用") String gwDeviceId,
      @Size(max = 100) @Schema(description = "子设备从站地址，由产品协议解释", example = "1") String slaveAddress,
      @Size(max = 128) @Schema(description = "设备地址；TCP创建暂不支持，创建后可通过更新接口设置") String deviceAddress,
      @Size(max = 64) @Schema(description = "经度字符串，范围 -180..180", example = "121.4737")
          String longitude,
      @Size(max = 64) @Schema(description = "纬度字符串，范围 -90..90", example = "31.2304")
          String latitude,
      @Size(max = 100)
          @Schema(
              description = "产品协议自定义添加参数，与 V1 data 语义一致；不能包含身份、指令或网关控制字段",
              example = "{\"customSerial\":\"SN001\"}")
          Map<String, Object> data) {}

  @Schema(name = "V2UpdateDevice", description = "仅更新明确传入的非空白展示字段；省略或 null 保留原值。具体字段支持以产品协议为准。")
  public record UpdateDevice(
      @Size(max = 128) @Schema(description = "设备名称，传入时不可为空白") String deviceName,
      @Size(max = 1000) @Schema(description = "设备说明") String detail,
      @Size(max = 128) @Schema(description = "设备地址") String deviceAddress,
      @Size(max = 64) @Schema(description = "经度，需与纬度同时传入") String longitude,
      @Size(max = 64) @Schema(description = "纬度，需与经度同时传入") String latitude) {}

  @Schema(
      name = "V2FunctionInvocation",
      description = "物模型功能调用。功能标识放在路径，参数按物模型定义；不必重复提供 function/messageType/cmd。")
  public record FunctionInvocation(
      @Size(max = 100)
          @Schema(
              description =
                  "物模型输入参数；无参功能可省略或传空对象。设备目标由路径固定；ezviz的deviceSerial、TTLock的lockId若传入必须与路径设备一致，系统会补齐缺失值。统一视频平台使用V2视频专用接口",
              example = "{\"switch\":\"open\"}")
          Map<String, Object> data) {}

  @Schema(name = "V2Device", description = "可供业务子系统使用的设备信息，不含 deviceSecret 和原始配置凭据。时间戳单位为秒。")
  public record Device(
      @Schema(description = "平台统一设备标识；使用实际返回值，不自行拼接") String iotId,
      @Schema(description = "产品Key") String productKey,
      @Schema(description = "设备编号") String deviceId,
      @Schema(description = "产品名称") String productName,
      @Schema(description = "设备名称") String deviceName,
      @Schema(
              description = "0 离线，1 在线",
              allowableValues = {"0", "1"})
          Integer state,
      @Schema(description = "第三方设备编号") String extDeviceId,
      @Schema(description = "网关产品Key") String gwProductKey,
      @Schema(description = "设备地址") String deviceAddress,
      @Schema(description = "坐标，经纬度由现有产品规范定义") String coordinate,
      @Schema(description = "设备说明") String detail,
      @Schema(description = "创建时间，Unix秒") Long createTime,
      @Schema(description = "注册时间，Unix秒") Integer registryTime,
      @Schema(description = "最后在线时间，Unix秒") Long onlineTime) {
    public static Device from(IoTDevice d) {
      return new Device(
          d.getIotId(),
          d.getProductKey(),
          d.getDeviceId(),
          d.getProductName(),
          d.getDeviceName(),
          d.getState(),
          d.getExtDeviceId(),
          d.getGwProductKey(),
          d.getDeviceAddress(),
          d.getCoordinate(),
          d.getDetail(),
          d.getCreateTime(),
          d.getRegistryTime(),
          d.getOnlineTime());
    }
  }

  @Schema(name = "V2DevicePage", description = "普通分页，仅返回当前应用已绑定且同租户、同所有者的设备。")
  public record DevicePage(
      @Schema(description = "设备列表") List<Device> items,
      @Schema(description = "总记录数") long total,
      @Schema(description = "页码，从1开始") int page,
      @Schema(description = "每页条数，1..200") int pageSize) {}

  @Schema(name = "V2DeviceIdentity", description = "实际持久化设备身份；子设备编号可能由现有协议根据网关和从站地址生成。")
  public record DeviceIdentity(
      @Schema(description = "平台实际统一设备标识") String iotId,
      @Schema(description = "实际产品Key") String productKey,
      @Schema(description = "实际设备编号，后续查询与调用应使用此值") String deviceId) {}

  @Schema(name = "V2CreationReceipt", description = "创建受理回执，不返回设备密钥和原始协议结果。")
  public record CreationReceipt(
      @Schema(description = "消息关联编号") String messageId,
      @Schema(description = "平台/协议是否接受请求，不代表设备已在线") boolean accepted,
      @Schema(description = "同步创建后实际设备身份；协议异步建档且尚不可查询时为null，请通过设备列表或推送确认", nullable = true)
          DeviceIdentity device) {}

  @Schema(
      name = "V2CommandReceipt",
      description = "平台/协议处理回执；accepted 只表示请求被接受，不代表设备执行成功。设备最终状态通过影子、日志或应用推送确认。")
  public record CommandReceipt(
      @Schema(description = "平台生成的消息编号，用于日志关联，不提供自动去重保证") String messageId,
      @Schema(description = "协议服务是否已接受请求") boolean accepted,
      @Schema(description = "协议同步返回的功能结果，字段随产品而定；增删改操作不返回内部实体", nullable = true) Object result) {}
}
