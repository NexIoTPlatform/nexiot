package cn.universal.web.controller.openapi.v2;

import cn.universal.common.domain.R;
import cn.universal.web.dto.openapi.v2.VideoApiV2Models;
import cn.universal.web.service.openapi.v2.VideoApiV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Versioned business facade for SDK/WVP channel discovery and snapshot lifecycle. */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(
    value = "/api/openapi/v2/video/devices/{productKey}/{deviceId}",
    produces = "application/json")
@Tag(
    name = "开放接口 V2 - 视频抓图",
    description = "应用绑定设备的视频通道与同步/异步抓图；" + "复用现有视频和物模型抓图能力，无需调用方提供厂家 SDK、网关或媒体节点参数")
@SecurityRequirement(name = "Authorization")
@ApiResponses({
  @ApiResponse(
      responseCode = "200",
      description = "请求已处理；抓图是否完成请检查 data.status",
      useReturnTypeSchema = true),
  @ApiResponse(responseCode = "400", description = "设备、通道、抓图参数或句柄格式不正确"),
  @ApiResponse(responseCode = "401", description = "缺少或无效的 OAuth2 Bearer Token"),
  @ApiResponse(responseCode = "403", description = "应用无绑定设备权限、跨租户/通道，或句柄属于其他应用"),
  @ApiResponse(responseCode = "409", description = "设备/实例/通道停用、能力不可用或任务已过期"),
  @ApiResponse(responseCode = "502", description = "上游视频服务不可用或未返回有效图片/任务")
})
public class VideoApiControllerV2 {
  private static final String CHANNEL = "/channels/{channelId}";
  private final VideoApiV2Service service;

  @Operation(
      operationId = "v2VideoChannels",
      summary = "查询已授权设备的视频通道",
      description =
          "仅返回当前 client_credentials 应用已绑定设备、同租户视频实例下的通道。"
              + "包含停用通道以便调用方展示状态；获取 channelId 后再调用抓图接口。")
  @GetMapping("/channels")
  public R<List<VideoApiV2Models.Channel>> channels(
      @Parameter(description = "IoT 产品标识，不是视频实例标识", required = true) @PathVariable
          String productKey,
      @Parameter(description = "IoT 设备编号，与视频平台设备编号一致", required = true) @PathVariable
          String deviceId) {
    return R.ok(service.channels(productKey, deviceId));
  }

  @Operation(
      operationId = "v2VideoCreateSnapshot",
      summary = "创建视频抓图",
      description =
          "默认使用设备原生抓图；SDK/国标按已有策略可能使用平台画面，ISUP 原生抓图异步执行。"
              + "同步完成返回 status=completed 和 imageUrl；异步受理返回 status=pending、pending=true 和 requestId，"
              + "建议每 1–2 秒查询一次，直到终态。返回 200 仅表示本次调用成功，须检查 data.status。"
              + "请求本身不保证幂等，重试 POST 可能创建新的任务；重复查询/取消应复用返回的句柄。")
  @PostMapping(CHANNEL + "/snapshots")
  public R<VideoApiV2Models.Snapshot> snapshot(
      @Parameter(description = "IoT 产品标识", required = true) @PathVariable String productKey,
      @Parameter(description = "IoT 设备编号", required = true) @PathVariable String deviceId,
      @Parameter(description = "查询通道接口返回的 channelId", required = true) @PathVariable
          String channelId,
      @Valid @RequestBody(required = false) VideoApiV2Models.SnapshotRequest body) {
    return R.ok(service.snapshot(productKey, deviceId, channelId, body));
  }

  @Operation(
      operationId = "v2VideoSnapshotStatus",
      summary = "查询异步抓图状态",
      description =
          "requestId 必须来自同一应用、设备、通道的 V2 创建抓图响应，不能填原生 SDK requestId。"
              + "pending 状态不返回图片地址；completed 才返回可用 imageUrl。失败、超时或取消均为终态。"
              + "允许在设备或通道停用后查询已有任务；仍校验当前应用绑定和租户归属。上游重启/清理后任务可能不可用。")
  @GetMapping(CHANNEL + "/snapshots/{requestId}")
  public R<VideoApiV2Models.Snapshot> snapshotStatus(
      @Parameter(description = "IoT 产品标识", required = true) @PathVariable String productKey,
      @Parameter(description = "IoT 设备编号", required = true) @PathVariable String deviceId,
      @Parameter(description = "创建抓图时的 channelId", required = true) @PathVariable String channelId,
      @Parameter(description = "V2 返回的不透明抓图句柄，32 位小写十六进制", required = true) @PathVariable
          String requestId) {
    return R.ok(service.snapshotStatus(productKey, deviceId, channelId, requestId, false));
  }

  @Operation(
      operationId = "v2VideoCancelSnapshot",
      summary = "取消异步抓图",
      description =
          "取消等待中的原生异步抓图。只接受本应用本通道的 V2 requestId；"
              + "已完成/失败/过期任务保留既有终态，不删除已生成图片。重复取消使用同一句柄；以响应 status 为准。")
  @DeleteMapping(CHANNEL + "/snapshots/{requestId}")
  public R<VideoApiV2Models.Snapshot> cancelSnapshot(
      @Parameter(description = "IoT 产品标识", required = true) @PathVariable String productKey,
      @Parameter(description = "IoT 设备编号", required = true) @PathVariable String deviceId,
      @Parameter(description = "创建抓图时的 channelId", required = true) @PathVariable String channelId,
      @Parameter(description = "V2 返回的不透明抓图句柄", required = true) @PathVariable String requestId) {
    return R.ok(service.snapshotStatus(productKey, deviceId, channelId, requestId, true));
  }
}
