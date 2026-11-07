package cn.universal.admin.system.notice;

import cn.universal.manager.notice.model.NoticeOwnedResource;
import cn.universal.persistence.entity.IoTUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import cn.universal.persistence.query.AjaxResult;

/** User and client_credentials JWTs resolve to the same active tenant/dept policy. */
public class NoticeApiAccess extends NoticeScopedController {
  protected void requireApiPermission(String... permissions) {
    noticeApiUser();
    Object principal = cn.universal.security.utils.SecurityUtils.getAuthentication().getPrincipal();
    // Registered application tokens are authorized as their owner; user tokens retain menu grants.
    if (principal instanceof org.springframework.security.oauth2.jwt.Jwt jwt && jwt.getClaimAsString("clientId") != null) return;
    requirePermission(permissions);
  }

  protected IoTUser noticeApiUser() { return noticeAccess.requireUser(); }
  protected void requireNoticeAccess(NoticeOwnedResource resource, String creator) {
    noticeAccess.checkAs(noticeApiUser(), resource, creator);
  }
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<AjaxResult> invalid(IllegalArgumentException error) {
    return ResponseEntity.badRequest().body(AjaxResult.error(error.getMessage()));
  }
}
