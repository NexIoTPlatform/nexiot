package cn.universal.admin.video.web;

import cn.universal.admin.video.alarm.SdkVideoAlarmCallbackRequest;
import cn.universal.admin.video.alarm.SdkVideoAlarmUpService;
import cn.universal.common.domain.R;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/video/sdk-events")
@RequiredArgsConstructor
@Slf4j
public class VideoSdkAlarmCallbackController {

  private final SdkVideoAlarmUpService alarmUpService;

  @PostMapping("/callback")
  public R<Map<String, Object>> callback(
      @RequestHeader(value = "X-NexIoT-Video-Alarm-Token", required = false) String token,
      @RequestBody SdkVideoAlarmCallbackRequest request) {
    // 网关回调无用户登录态，由上行服务校验实例 token，再确定真实设备和租户归属。
    log.info("收到SDK订阅事件 instanceKey={}, deviceId={}, channelId={}, alarmType={}",
        request.getInstanceKey(), request.getDeviceId(), request.getChannelId(), request.getAlarmType());
    return R.ok(alarmUpService.publish(request, token));
  }
}
