package cn.universal.admin.video.web;

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.video.service.VideoPlatformInstanceService;
import cn.universal.common.domain.R;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.security.BaseController;
import cn.universal.security.support.TenantAccessPolicy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 接入指引按需读取设备认证凭据，不返回 SDK 网关和告警回调的管理令牌。 */
@RestController
@RequestMapping("/api/video/platforms")
@RequiredArgsConstructor
public class VideoPlatformAccessController extends BaseController {
  private final VideoPlatformInstanceService instances;
  private final ObjectMapper json;

  @GetMapping("/{instanceKey}/access-credentials")
  @PreAuthorize("@ss.hasPermi('video:platform:edit')")
  public ResponseEntity<R<Map<String, String>>> accessCredentials(
      @PathVariable("instanceKey") String instanceKey) {
    IoTUser user = currentTenantUser();
    if (user == null) throw new BaseException("请先登录");
    VideoPlatformInstance instance = instances.getByInstanceKey(instanceKey);
    if (instance == null) throw new BaseException("平台实例不存在");
    String creatorTenantId = StrUtil.isBlank(instance.getTenantId())
        ? resolveTenantId(instance.getCreatorId()) : null;
    if (!TenantAccessPolicy.canAccessTenantResource(
        user, instance.getTenantId(), instance.getCreatorId(), creatorTenantId)) {
      throw new BaseException("无权查看该平台实例接入凭据");
    }
    Map<String, Object> auth = read(instance.getAuth());
    Map<String, String> result = new LinkedHashMap<>();
    if ("hik_isup".equals(instance.getPlatformType())) {
      String key = Objects.toString(auth.get("isupKey"), "");
      if (key.isBlank()) key = Objects.toString(read(instance.getOptions()).get("isupKey"), "");
      result.put("isupKey", key);
    } else if ("dahua_sdk".equals(instance.getPlatformType())) {
      result.put("username", Objects.toString(auth.get("activeRegistrationUsername"), ""));
      result.put("password", Objects.toString(auth.get("activeRegistrationPassword"), ""));
    } else {
      throw new BaseException("该平台没有主动注册接入凭据");
    }
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(R.ok(result));
  }

  private Map<String, Object> read(String value) {
    if (value == null || value.isBlank()) return Map.of();
    try {
      return json.readValue(value, new TypeReference<LinkedHashMap<String, Object>>() {});
    } catch (Exception e) {
      throw new BaseException("平台接入配置不是有效 JSON");
    }
  }
}
