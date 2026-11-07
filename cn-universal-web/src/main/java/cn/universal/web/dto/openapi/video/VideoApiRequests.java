package cn.universal.web.dto.openapi.video;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Stable business parameters; native SDK, gateway and media node parameters stay server-side. */
public final class VideoApiRequests {
  private VideoApiRequests() {}

  public record Preview(
      @Schema(description = "码流，默认 main", allowableValues = {"main", "sub"})
      @Pattern(regexp = "main|sub") String streamType,
      @Schema(description = "播放器票据有效秒数，默认300，范围30–3600")
      @Min(30) @Max(3600) Integer expiresInSeconds) {
    public String stream() { return streamType == null ? "main" : streamType; }
    public int ttl() { return expiresInSeconds == null ? 300 : expiresInSeconds; }
  }

  public record RecordingRange(
      @Schema(description = "开始时间，Unix毫秒", example = "1792022400000")
      @NotNull @Min(1) Long startTime,
      @Schema(description = "结束时间，Unix毫秒", example = "1792022460000")
      @NotNull @Min(1) Long endTime,
      @Pattern(regexp = "main|sub") String streamType,
      @Min(30) @Max(3600) Integer expiresInSeconds) {
    public String stream() { return streamType == null ? "main" : streamType; }
    public int ttl() { return expiresInSeconds == null ? 300 : expiresInSeconds; }
  }

  public record Control(
      @Schema(description = "seek时传Unix毫秒，必须位于本次回放区间内") Long seekTime,
      @Schema(description = "speed时传倍率，是否支持由设备能力决定")
      @Min(1) @Max(16) Double speed) {}

  public record Snapshot(
      @Schema(description = "device为设备抓图，platform_frame为当前视频帧")
      @Pattern(regexp = "device|platform_frame") String source) {}

  public record Ptz(
      @NotBlank @Pattern(regexp = "left|right|up|down|upleft|upright|downleft|downright|zoomin|zoomout|stop")
      String command,
      @Min(1) @Max(100) Integer speed) {}
}
