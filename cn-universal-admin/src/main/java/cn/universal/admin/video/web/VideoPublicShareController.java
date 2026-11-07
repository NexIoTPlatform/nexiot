package cn.universal.admin.video.web;

import cn.universal.admin.video.service.VideoPlatformInstanceService;
import cn.universal.common.domain.R;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/video/public/share")
public class VideoPublicShareController {

  private final VideoPlatformInstanceService instanceService;

  @Value("${video.share.overlay-event-interval-ms:1000}")
  private long overlayEventIntervalMs;

  private final ScheduledExecutorService overlayEventExecutor =
      Executors.newScheduledThreadPool(
          4,
          new ThreadFactory() {
            private final java.util.concurrent.atomic.AtomicInteger index =
                new java.util.concurrent.atomic.AtomicInteger(1);

            @Override
            public Thread newThread(Runnable runnable) {
              Thread thread = new Thread(runnable, "video-share-osd-sse-" + index.getAndIncrement());
              thread.setDaemon(true);
              return thread;
            }
          });

  @PreDestroy
  public void shutdown() {
    overlayEventExecutor.shutdownNow();
  }

  /*
   * 公开视频分享入口：
   * - token 已经包含 instance/device/channel/过期时间等信息，公网访问这里不走后台登录鉴权。
   * - service 会重新解析 token 并取当前通道配置，通常返回原始视频流 + 前端 OSD 配置。
   * - 不在 public controller 中启动后端融合任务，避免分享页访问造成 FFmpeg 资源膨胀。
   */
  @GetMapping("/{token}")
  public R<Map<String, Object>> getShareContext(@PathVariable("token") String token) {
    return R.ok(instanceService.getFrontendOsdShareContext(token));
  }

  // 分享页拉取当前 OSD 快照；快照来自设备影子事件缓存，缓存失效时 service 会按配置即时重建。
  @GetMapping("/{token}/overlay")
  public R<Map<String, Object>> getShareOverlay(@PathVariable("token") String token) {
    return R.ok(instanceService.getFrontendOsdShareOverlay(token));
  }

  /*
   * SSE 是分享页的轻量实时通道：
   * - 正常只在 overlayText/ts 变化时推送 overlay 事件。
   * - 长时间无变化时发 heartbeat，避免浏览器或代理误判连接空闲。
   * - 前端已经有 WebSocket/轮询兜底，因此这里失败时主动 complete，让前端切换兜底路径。
   */
  @GetMapping(value = "/{token}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter watchShareOverlay(@PathVariable("token") String token) {
    // 先访问一次，提前校验 token 是否有效，避免建立无意义的长连接。
    instanceService.getFrontendOsdShareOverlay(token);
    SseEmitter emitter = new SseEmitter(0L);
    AtomicReference<String> lastSignature = new AtomicReference<>("");
    AtomicLong lastHeartbeatAt = new AtomicLong(0L);
    AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();
    long intervalMs = Math.max(300L, overlayEventIntervalMs);

    Runnable task =
        () -> {
          try {
            Map<String, Object> overlay = instanceService.getFrontendOsdShareOverlay(token);
            String signature =
                String.valueOf(overlay.get("overlayText")) + ":" + String.valueOf(overlay.get("ts"));
            long now = System.currentTimeMillis();
            if (!signature.equals(lastSignature.getAndSet(signature))) {
              // 只在文本或时间戳变化时推送，减少公开分享页连接数较多时的无效流量。
              emitter.send(SseEmitter.event().name("overlay").data(overlay));
              lastHeartbeatAt.set(now);
              return;
            }
            if (now - lastHeartbeatAt.get() > 15000L) {
              // 维持链路活性，不代表 OSD 数据发生变化。
              emitter.send(SseEmitter.event().name("heartbeat").data(Map.of("serverTime", now)));
              lastHeartbeatAt.set(now);
            }
          } catch (Exception e) {
            log.warn("[视频分享] OSD事件推送结束 token={}, msg={}", token, e.getMessage());
            try {
              emitter.send(SseEmitter.event().name("error").data(Map.of("message", e.getMessage())));
            } catch (IOException ignore) {
            }
            ScheduledFuture<?> future = futureRef.get();
            if (future != null) {
              future.cancel(false);
            }
            emitter.complete();
          }
        };

    ScheduledFuture<?> future =
        overlayEventExecutor.scheduleWithFixedDelay(task, 0L, intervalMs, TimeUnit.MILLISECONDS);
    futureRef.set(future);
    emitter.onCompletion(() -> future.cancel(false));
    emitter.onTimeout(
        () -> {
          future.cancel(false);
          emitter.complete();
        });
    emitter.onError(
        error -> {
          future.cancel(false);
          log.debug("[视频分享] OSD事件连接异常 token={}, msg={}", token, error.getMessage());
        });
    return emitter;
  }
}
