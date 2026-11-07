package cn.universal.web.dto.openapi.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Date;

/** 对外应用模型。与数据库实体隔离，避免凭据泄露和应用归属字段被批量赋值。 */
public final class OpenApiV2ApplicationModels {

  private OpenApiV2ApplicationModels() {}

  @Schema(name = "OpenApiV2Application", description = "当前 Bearer Token 对应的应用信息；不包含应用密钥和推送密钥")
  public record ApplicationInfo(
      @Schema(
              description = "应用内部唯一标识，用于设备归属；与 OAuth2 client_id 不同",
              example = "2076250261675757568")
          String applicationId,
      @Schema(description = "OAuth2 client_id，即平台应用页面的 AppId", example = "exampleAppId1234")
          String appId,
      @Schema(description = "应用名称", example = "能源管理子系统") String name,
      @Schema(description = "应用状态：0 正常，1 停用", example = "0") Integer status,
      @Schema(description = "应用授权范围，具体含义沿用平台应用配置") String scope,
      @Schema(description = "应用授权截止时间；为空表示没有单独设置截止时间") Date validEndDate,
      @Schema(description = "应用创建时间") Date createdAt,
      @Schema(description = "应用备注") String description,
      @Schema(description = "当前应用 HTTP 推送配置；密钥只返回是否已配置") HttpPushInfo httpPush,
      @Schema(description = "当前应用系统 MQTT 推送是否启用；MQTT 的其他参数仍通过应用管理维护") boolean mqttEnabled) {}

  @Schema(
      name = "OpenApiV2HttpPushUpdate",
      description = "当前应用 HTTP 推送更新。仅允许更新此模型声明的字段；其他推送通道与应用凭据不受影响")
  public record HttpPushUpdate(
      @NotNull(message = "enable 不能为空")
          @Schema(
              description = "是否启用 HTTP 推送。启用前必须已配置有效 url 和 secret；停用保留原配置",
              example = "true",
              requiredMode = Schema.RequiredMode.REQUIRED)
          Boolean enable,
      @Size(max = 2048, message = "url 长度不能超过 2048")
          @Schema(
              description = "完整 HTTP/HTTPS 回调地址。不传保留原值；首次启用必须提供。不接受含用户名密码或片段的 URL",
              example = "https://subsystem.example.com/nexiot/events",
              maxLength = 2048)
          String url,
      @Size(max = 128, message = "header 长度不能超过 128")
          @Schema(
              description = "发送推送密钥的 HTTP 请求头名。不传保留原值，首次配置默认为 X-Nexiot-Secret",
              example = "X-Nexiot-Secret",
              maxLength = 128)
          String header,
      @Size(max = 2048, message = "secret 长度不能超过 2048")
          @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
          @Schema(
              description = "HTTP 推送密钥。仅写入；不传或空白时保留原密钥，不回显。新启用且无原密钥时必填",
              example = "replace-with-your-callback-secret",
              accessMode = Schema.AccessMode.WRITE_ONLY,
              maxLength = 2048)
          String secret) {}

  @Schema(name = "OpenApiV2HttpPush", description = "HTTP 推送配置；不返回 secret，便于安全读取及再次编辑")
  public record HttpPushInfo(
      @Schema(description = "当前通道是否受支持；false 时不允许启用") boolean supported,
      @Schema(description = "当前 HTTP 推送是否实际启用") boolean enabled,
      @Schema(description = "HTTP/HTTPS 回调地址；未配置时为空") String url,
      @Schema(description = "推送密钥请求头名；默认为 X-Nexiot-Secret", example = "X-Nexiot-Secret")
          String header,
      @Schema(description = "是否已保存推送密钥；不会返回其内容") boolean secretConfigured) {}

  @Schema(name = "OpenApiV2ApplicationDeviceBinding", description = "应用与设备的最终绑定状态；同一请求重复执行具有幂等性")
  public record DeviceBinding(
      @Schema(description = "当前应用内部唯一标识") String applicationId,
      @Schema(description = "设备所属产品的 ProductKey") String productKey,
      @Schema(description = "产品内唯一设备编号") String deviceId,
      @Schema(description = "平台设备唯一标识 IoT ID") String iotId,
      @Schema(description = "true 已绑定到当前应用，false 已解绑") boolean bound) {}
}
