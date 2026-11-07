package cn.universal.admin.video.web;

import cn.universal.admin.video.service.VideoCatalogChannelPage;
import cn.universal.admin.video.service.VideoCatalogPage;
import cn.universal.admin.video.service.VideoCatalogService;
import cn.universal.admin.video.service.VideoCloudSdkTicketService;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Lightweight shared directory for the browser wall and desktop application. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/device/video/catalog")
@Tag(name = "视频设备目录")
public class VideoCatalogController {
  private final VideoCatalogService catalog;
  private final VideoCloudSdkTicketService sdkTickets;

  @GetMapping
  @Operation(summary = "分页查询轻量视频设备目录")
  public VideoCatalogPage list(
      @RequestParam(defaultValue = "1") int pageNum,
      @RequestParam(defaultValue = "50") int pageSize,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String groupId) {
    return catalog.list(pageNum, pageSize, keyword, groupId);
  }

  @GetMapping("/{iotId}/channels")
  @Operation(summary = "按需分页查询视频设备通道")
  public VideoCatalogChannelPage channels(
      @PathVariable String iotId,
      @RequestParam(defaultValue = "1") int pageNum,
      @RequestParam(defaultValue = "50") int pageSize) {
    return catalog.channels(iotId, pageNum, pageSize);
  }

  @PostMapping("/{iotId}/sdk-ticket")
  @Operation(summary = "按需获取厂家云 SDK 实时预览授权")
  public ResponseEntity<Map<String, Object>> sdkTicket(@PathVariable String iotId,
      @RequestBody Map<String, Object> request) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma", "no-cache")
        .body(sdkTickets.preview(iotId, request));
  }
}
