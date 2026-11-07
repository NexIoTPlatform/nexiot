package cn.universal.admin.video.web;

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.video.alarm.VideoSdkAlarmProcessService;
import cn.universal.admin.video.service.VideoPlatformInstanceService;
import cn.universal.common.domain.R;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Read-only process view uses database instance ownership, never tenant query input. */
@RestController
@RequestMapping("/api/video/platforms/{instanceKey}/alarm/processes")
@RequiredArgsConstructor
public class VideoSdkAlarmProcessController extends BaseController {
  private final VideoPlatformInstanceService instances;
  private final VideoSdkAlarmProcessService processes;

  @GetMapping
  @PreAuthorize("@videoOperationAccess.readAllowed()")
  public R<Map<String, Object>> list(
      @PathVariable String instanceKey, @RequestParam Map<String, String> parameters) {
    return R.ok(processes.list(requireInstance(instanceKey), parameters));
  }

  @GetMapping("/{processId}/events")
  @PreAuthorize("@videoOperationAccess.readAllowed()")
  public R<Map<String, Object>> events(
      @PathVariable String instanceKey,
      @PathVariable String processId,
      @RequestParam Map<String, String> parameters) {
    return R.ok(processes.events(requireInstance(instanceKey), processId, parameters));
  }

  private VideoPlatformInstance requireInstance(String key) {
    var user = loginIoTUnionUser(SecurityUtils.getUnionId());
    if (user == null) throw new BaseException("请先登录");
    var instance = instances.getByInstanceKey(key);
    if (instance == null
        || instance.getPlatformType() == null
        || !java.util.List.of("hik_isup", "hik_sdk", "dahua_sdk", "tiandy_sdk", "uniview_sdk")
            .contains(instance.getPlatformType())) throw new BaseException("视频平台实例不存在");
    String tenant = StrUtil.blankToDefault(user.getTenantId(), user.getUnionId());
    if (!user.isAdmin() && !java.util.Objects.equals(tenant, instance.getTenantId()))
      throw new BaseException("无权查看该平台告警过程");
    return instance;
  }
}
