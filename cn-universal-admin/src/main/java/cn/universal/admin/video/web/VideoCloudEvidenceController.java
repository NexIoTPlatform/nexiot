package cn.universal.admin.video.web;

import cn.universal.admin.platform.web.DeviceHistoryAccess;
import cn.universal.admin.video.service.VideoCloudEvidenceTimelineService;
import cn.universal.common.domain.R;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.query.LogQuery;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VideoCloudEvidenceController extends BaseController {
  private final IoTDeviceMapper devices;
  private final IoTProductMapper products;
  private final DeviceHistoryAccess history;
  private final VideoCloudEvidenceTimelineService timeline;
  private final cn.universal.admin.video.service.VideoCloudPictureSecretService secrets;
  private final cn.universal.admin.video.service.VideoCloudDeviceDetailService details;

  @org.springframework.web.bind.annotation.GetMapping("/api/video/platforms/{productKey}/devices/{deviceId}/cloud-detail")
  @org.springframework.security.access.prepost.PreAuthorize("@sysPermissionService.hasPermission('system:instance:query')")
  public R<Map<String, Object>> detail(@PathVariable String productKey, @PathVariable String deviceId) {
    var device = requireDevice(productKey, deviceId);
    var product = products.getProductByProductKey(productKey);
    requireDetailPlatform(product.getThirdPlatform());
    return R.ok(details.detail(device, product));
  }

  @PostMapping("/api/video/platforms/{productKey}/devices/{deviceId}/cloud-detail/refresh")
  @org.springframework.security.access.prepost.PreAuthorize("@sysPermissionService.hasPermission('system:instance:edit')")
  public R<Map<String, Object>> refreshDetail(@PathVariable String productKey, @PathVariable String deviceId) {
    var device = requireDevice(productKey, deviceId);
    var product = products.getProductByProductKey(productKey);
    requireDetailPlatform(product.getThirdPlatform());
    return R.ok(details.refresh(device, product));
  }

  @PostMapping("/api/video/platforms/{productKey}/evidence/picture-secret/verify")
  @org.springframework.security.access.prepost.PreAuthorize("@sysPermissionService.hasPermission('system:instance:edit')")
  public R<Map<String, Object>> verifyPictureSecret(@PathVariable String productKey, @RequestBody Map<String, Object> body) {
    var device = requireDevice(productKey, java.util.Objects.toString(body.get("deviceId"), ""));
    var product = products.getProductByProductKey(productKey);
    requireDetailPlatform(product.getThirdPlatform());
    return R.ok(secrets.verify(device, product, java.util.Objects.toString(body.get("pictureSecret"), "")));
  }

  private void requireDetailPlatform(String platform) {
    if (!Set.of("imoulife", "dahua_cloud").contains(platform)) throw new BaseException("该平台暂不支持云设备详情");
  }

  @org.springframework.web.bind.annotation.PutMapping("/api/video/platforms/{productKey}/evidence/picture-secret")
  @org.springframework.security.access.prepost.PreAuthorize("@sysPermissionService.hasPermission('system:instance:edit')")
  public R<Map<String, Object>> pictureSecret(@PathVariable String productKey, @RequestBody Map<String, Object> body) {
    var device = requireDevice(productKey, java.util.Objects.toString(body.get("deviceId"), ""));
    return R.ok(secrets.save(device, java.util.Objects.toString(body.get("pictureSecret"), "")));
  }

  private cn.universal.persistence.entity.IoTDevice requireDevice(String productKey, String deviceId) {
    var user = loginIoTUnionUser(SecurityUtils.getUnionId());
    if (user == null) throw new BaseException("请先登录");
    var device = devices.selectIoTDevice(productKey, deviceId);
    if (device == null) throw new BaseException("设备不存在");
    history.requireAccess(user.getTenantId(), user.isAdmin(),
        LogQuery.builder().productKey(productKey).iotId(device.getIotId()).deviceId(device.getDeviceId()).build());
    var product = products.getProductByProductKey(productKey);
    if (product == null || !Set.of("imoulife", "ezviz", "dahua_cloud").contains(product.getThirdPlatform()))
      throw new BaseException("该设备不是云托管摄像头");
    return device;
  }

  @PostMapping("/api/video/platforms/{productKey}/evidence/timeline")
  public R<Map<String, Object>> timeline(@PathVariable String productKey, @RequestBody Map<String, Object> body) {
    var device = requireDevice(productKey, java.util.Objects.toString(body.get("deviceId"), ""));
    return R.ok(timeline.timeline(device, products.getProductByProductKey(productKey), body));
  }

  @PostMapping("/api/video/platforms/{productKey}/evidence/retry")
  @org.springframework.security.access.prepost.PreAuthorize("@sysPermissionService.hasPermission('system:instance:edit')")
  public R<Map<String, Object>> retry(@PathVariable String productKey, @RequestBody Map<String, Object> body) {
    var device = requireDevice(productKey, java.util.Objects.toString(body.get("deviceId"), ""));
    return R.ok(timeline.retry(device, products.getProductByProductKey(productKey), body));
  }
}
