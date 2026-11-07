package cn.universal.dm.video.zlm;

import java.util.Map;

/** Common ZLMediaKit stream proxy ability for browser preview. */
public interface VideoZlmStreamProxyService {

  /** A wall viewer pins a dedicated, shared proxy; viewerKey is derived by the trusted caller. */
  default Map<String, String> acquirePreviewLease(String tenantId, String viewerKey,
      String mediaServerId, String sourceUrl, String app, String stream) {
    throw new UnsupportedOperationException("流媒体服务尚未接入视频墙保活租约");
  }

  default Map<String, Object> renewPreviewLease(String viewerKey, String previewLeaseId) {
    return Map.of("active", false);
  }

  default Map<String, Object> releasePreviewLease(String viewerKey, String previewLeaseId) {
    return Map.of("stopped", true, "alreadyClosed", true);
  }

  /** Trusted gateway target, including credentials; never serialize this map to a browser. */
  default Map<String, Object> recordingTarget(String tenantId, String mediaServerId) {
    throw new UnsupportedOperationException("流媒体服务尚未接入 ISUP 平台录像任务");
  }

  default Map<String, Object> openRtpPlayback(String tenantId, String mediaServerId, String stream) {
    throw new UnsupportedOperationException("流媒体服务尚未接入SDK录像回放");
  }

  default void closeRtpPlayback(String tenantId, String mediaServerId, String stream) {
    throw new UnsupportedOperationException("流媒体服务尚未接入SDK录像回放");
  }

  default void renewRtpPlayback(String tenantId, String mediaServerId, String stream) {
    throw new UnsupportedOperationException("流媒体服务尚未接入SDK回放容量续期");
  }

  /** Only a video track published on the owned RTP receiver can make playback ready. */
  default boolean isRtpPlaybackReady(String tenantId, String mediaServerId, String stream) {
    return false;
  }

  default void pauseRtpPlayback(String tenantId, String mediaServerId, String stream, boolean paused, int pauseSeconds) {
    throw new UnsupportedOperationException("流媒体服务尚未接入SDK回放暂停");
  }

  /**
   * Pulls an origin RTSP stream into ZLM and returns browser-playable URLs.
   *
   * @param tenantId tenant id used to select an available media server
   * @param sourceUrl origin stream URL, normally RTSP
   * @param app ZLM app name
   * @param stream ZLM stream id
   * @return play URLs such as ws_flv/flv/hls/rtsp
   */
  Map<String, String> ensureStreamProxy(String tenantId, String sourceUrl, String app, String stream);

  /**
   * Pulls an origin stream into a preferred media server when specified.
   *
   * @param tenantId tenant id used to validate/select media server
   * @param mediaServerId preferred media server primary id; blank means auto scheduling
   * @param sourceUrl origin stream URL, normally RTSP
   * @param app ZLM app name
   * @param stream ZLM stream id
   * @return play URLs such as ws_flv/flv/hls/rtsp
   */
  default Map<String, String> ensureStreamProxy(
      String tenantId, String mediaServerId, String sourceUrl, String app, String stream) {
    return ensureStreamProxy(tenantId, sourceUrl, app, stream);
  }

  /**
   * Releases a proxied origin stream from a specified media server.
   *
   * @param tenantId tenant id used to validate/select media server
   * @param mediaServerId media server primary id or ZLM mediaServerId
   * @param app ZLM app name
   * @param stream ZLM stream id
   * @return release result from the media server
   */
  default Map<String, String> releaseStreamProxy(
      String tenantId, String mediaServerId, String app, String stream) {
    return Map.of();
  }

  /**
   * Captures one image from a proxied origin stream.
   *
   * @param tenantId tenant id used to validate/select media server
   * @param mediaServerId media server primary id or ZLM mediaServerId
   * @param sourceUrl origin stream URL, normally RTSP; optional when app/stream already exists
   * @param app ZLM app name
   * @param stream ZLM stream id
   * @return snapshot payload such as snapshotBytes/snapshotUrl/contentType
   */
  default Map<String, Object> captureStreamSnapshot(
      String tenantId, String mediaServerId, String sourceUrl, String app, String stream) {
    return Map.of();
  }
}
