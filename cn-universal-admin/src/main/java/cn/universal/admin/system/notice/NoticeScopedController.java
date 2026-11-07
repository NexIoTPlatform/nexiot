package cn.universal.admin.system.notice;

import cn.universal.admin.system.permission.SysPermissionService;
import cn.universal.manager.notice.model.NoticeOwnedResource;
import cn.universal.manager.notice.service.NoticeAccess;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

/** Console endpoints require a real active identity, including empty-result and batch paths. */
public abstract class NoticeScopedController extends BaseController {
  @Autowired protected NoticeAccess noticeAccess;
  @Autowired private SysPermissionService permissions;

  protected void requirePermission(String... allowed) {
    noticeAccess.requireUser();
    for (String permission : allowed) {
      if (permissions.hasPermission(permission)) {
        return;
      }
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无通知操作权限");
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<AjaxResult> forbidden(ResponseStatusException error) {
    return ResponseEntity.status(error.getStatusCode())
        .body(AjaxResult.error(error.getReason()));
  }

  protected String noticeError(Exception error) {
    if (error instanceof ResponseStatusException denied) {
      throw denied;
    }
    return error instanceof IllegalArgumentException || error instanceof IllegalStateException
        ? error.getMessage() : "操作失败，请稍后重试";
  }

  @Override
  protected IoTUser currentTenantUser() {
    return noticeAccess.requireUser();
  }

  protected boolean canAccessNotice(IoTUser actor, NoticeOwnedResource resource, String creator) {
    noticeAccess.checkAs(actor, resource, creator);
    return true;
  }

  @Override
  protected <T> TableDataInfo<T> getDataTable(List<T> rows, Integer page, Integer size) {
    int pageNumber = page == null || page < 1 ? 1 : page;
    int pageSize = size == null || size < 1 ? 10 : Math.min(size, 10000);
    int start = (int) Math.min((long) (pageNumber - 1) * pageSize, rows.size());
    int end = (int) Math.min((long) start + pageSize, rows.size());
    return getDataTable(rows.subList(start, end), rows.size());
  }
}
