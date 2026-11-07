package cn.universal.web.dto.openapi.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

/** Public video contracts. Native SDK credentials, URLs and job identifiers stay server-side. */
public final class VideoApiV2Models {
  private VideoApiV2Models() {}

  @Schema(name = "OpenApiV2SnapshotRequest", description = "抓图参数；不传请求体时使用设备抓图和主码流")
  public record SnapshotRequest(
      @Schema(
              description =
                  "device：优先设备原生抓图，直连 SDK 按已有策略可能回退平台画面；"
                      + "platform_frame：从当前视频流截取画面。平台画面不是设备原始事件图，原生能力由设备决定",
              allowableValues = {"device", "platform_frame"},
              defaultValue = "device")
          @Pattern(regexp = "device|platform_frame")
          String source,
      @Schema(
              description = "码流类型；platform_frame 使用所选码流，原生抓图是否区分码流由设备决定",
              allowableValues = {"main", "sub"},
              defaultValue = "main")
          @Pattern(regexp = "main|sub")
          String streamType) {
    public String sourceOrDefault() {
      return source == null ? "device" : source;
    }

    public String streamOrDefault() {
      return streamType == null ? "main" : streamType;
    }
  }

  @Schema(name = "OpenApiV2VideoChannel", description = "当前应用已授权视频设备的通道；仅返回公共字段")
  public record Channel(
      @Schema(description = "平台侧通道标识；后续抓图接口原样使用，不按通道序号推测", example = "1") String channelId,
      @Schema(description = "通道名称", example = "前门") String channelName,
      @Schema(description = "通道当前上报状态；online/offline/未知，由视频平台提供", example = "online") String status,
      @Schema(description = "管理启用状态：1 启用、0 停用，null 表示源数据未指定", example = "1") Integer enabled,
      @Schema(
              description =
                  "设备上报或观测到的公共能力。值可为布尔值，或含 support/source/checkedAt 的对象；"
                      + "缺失和 unknown 均不表示已支持，抓图实际结果以调用返回为准")
          Map<String, Object> capabilities) {}

  @Schema(
      name = "OpenApiV2Snapshot",
      description =
          "统一抓图结果；仅 status=completed 且 imageUrl 非空表示图片已生成。"
              + "HTTP 200 和 accepted=true 仅表示请求被受理，不证明抓图完成")
  public record Snapshot(
      @Schema(
              description =
                  "统一状态：pending 等待设备/上传，completed 图片可访问，failed 失败，" + "expired 超时，cancelled 已取消",
              allowableValues = {"pending", "completed", "failed", "expired", "cancelled"})
          String status,
      @Schema(description = "本次请求已被服务受理；不等同于抓图成功") boolean accepted,
      @Schema(description = "是否仍需轮询；为 true 时 imageUrl 为空") boolean pending,
      @Schema(
              description =
                  "异步抓图的不透明句柄，仅当前应用、设备和通道可用。同步结果为空。"
                      + "通过 GET/DELETE snapshots/{requestId} 查询或取消；有效期受上游任务保留时间和服务重启影响",
              example = "f35a83b2a4ca4b5c96544fdabff8c01f")
          String requestId,
      @Schema(
              description = "仅 completed 返回的 HTTP(S) 图片地址；可能含短期签名，应及时获取，勿长期缓存地址",
              example = "https://images.example.com/snapshot.jpg")
          String imageUrl,
      @Schema(
              description = "实际图片来源，例如 device_snapshot/platform_frame；为空表示上游未提供，" + "不从请求意图推断实际来源",
              example = "device_snapshot")
          String imageSource,
      @Schema(description = "实际执行能力提供方；例如 hik_isup_sdk/zlm_snapshot") String provider,
      @Schema(description = "是否从原生抓图回退平台画面；null 表示上游未声明") Boolean fallback,
      @Schema(description = "图片 MIME 类型；上游未提供时为空", example = "image/jpeg") String mimeType,
      @Schema(description = "图片宽度，像素；上游未提供时为空", example = "1920") Integer width,
      @Schema(description = "图片高度，像素；上游未提供时为空", example = "1080") Integer height,
      @Schema(description = "图片文件大小，字节；上游未提供时为空") Long size,
      @Schema(description = "平台记录的采集时间，Unix 毫秒；不是设备帧 PTS 或告警发生时间，" + "上游未提供时为空") Long capturedAt,
      @Schema(description = "失败原因代码，仅失败/过期时返回，不包含 SDK 凭据或原始错误堆栈") String errorCode,
      @Schema(description = "可供调用方展示的处理说明") String message) {}
}
