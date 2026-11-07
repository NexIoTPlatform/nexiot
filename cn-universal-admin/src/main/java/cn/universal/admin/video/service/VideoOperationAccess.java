package cn.universal.admin.video.service;

import cn.universal.admin.system.permission.SysPermissionService;
import cn.universal.dm.video.VideoOperationRegistry;
import cn.universal.security.utils.SecurityUtils;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** Uses the same menu grants as REST and IoT command screens. */
@Component("videoOperationAccess")
@RequiredArgsConstructor
public class VideoOperationAccess {
  private final SysPermissionService permissions;

  public boolean allowed(String operation) {
    Boolean openApiAllowed = OpenApiVideoScope.operationAllowed(operation);
    if (openApiAllowed != null) return openApiAllowed;
    var definition = VideoOperationRegistry.find(operation);
    return definition.map(op -> op.allowed(permissions.getCurrentPermissions()))
        .orElseGet(() -> permissions.hasPermission("video:platform:edit"));
  }
  public boolean readAllowed() {
    Set<String> grants = permissions.getCurrentPermissions();
    return grants.contains("*:*:*") || grants.contains("video:platform:view")
        || grants.contains("video:platform:query") || grants.contains("video:platform:edit");
  }
  public Map<String,Object> catalog() {
    return Map.of("catalogVersion", VideoOperationRegistry.VERSION,
        "operations", VideoOperationRegistry.descriptors(permissions.getCurrentPermissions()));
  }
  public void assertUserOperation(String operation) {
    Boolean openApiAllowed = OpenApiVideoScope.operationAllowed(operation);
    if (openApiAllowed != null) {
      if (!openApiAllowed) throw new AccessDeniedException("无权执行视频操作：" + operation);
      return;
    }
    // Scheduled reconcile and authenticated IoT/Flow workers have no user authentication.
    // Their resource/tenant authority remains checked by the business service and adapter.
    if (SecurityUtils.getAuthentication() != null && !allowed(operation))
      throw new AccessDeniedException("无权执行视频操作：" + operation);
  }
}
