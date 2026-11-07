package cn.universal.admin.system.notice;

import cn.universal.manager.notice.model.NoticeSendRequest;
import cn.universal.manager.notice.model.NoticeTemplate;
import cn.universal.manager.notice.service.NoticeService;
import cn.universal.manager.notice.service.NoticeTemplateService;
import cn.universal.manager.notice.service.channel.NoticeSendResult;
import cn.universal.persistence.query.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/open-api/v1/notice")
public class NoticeOpenApiController extends NoticeApiAccess {
  private final NoticeService notices;
  private final NoticeTemplateService templates;
  public NoticeOpenApiController(NoticeService notices, NoticeTemplateService templates) {
    this.notices = notices; this.templates = templates;
  }
  private void authorize(NoticeSendRequest request) {
    if (request.getTemplateId() == null) throw new IllegalArgumentException("NexIoT 通知模板 ID 不能为空");
    NoticeTemplate template = templates.getById(request.getTemplateId());
    if (template == null) throw new IllegalArgumentException("通知模板不存在");
    requireNoticeAccess(template, template.getCreator());
    request.setCreator(noticeApiUser().getUnionId());
  }
  @Operation(summary = "发送通知", description = "Bearer 用户/应用访问令牌鉴权。templateId 是 NexIoT 通知模板 ID；receivers 为 OpenID 或 user:绑定标识；params 传业务参数，由已保存的微信字段映射生成 data。success 表示微信接口受理，messageIds 用于核对微信回执。")
  @PostMapping("/send")
  public AjaxResult send(@RequestBody NoticeSendRequest request) {
    requireApiPermission("notice:template:edit");
    authorize(request);
    NoticeSendResult result = notices.sendR(request);
    return result.isSuccess() ? AjaxResult.success(result) : AjaxResult.error(result.getErrorMessage());
  }
  @Operation(summary = "预览通知参数", description = "与发送完全相同的映射校验，不调用微信、不发送消息。")
  @PostMapping("/preview")
  public AjaxResult preview(@RequestBody NoticeSendRequest request) {
    requireApiPermission("notice:template:detail", "notice:template:edit");
    authorize(request);
    try { return AjaxResult.success(notices.preview(request)); }
    catch (IllegalArgumentException e) { return AjaxResult.error(e.getMessage()); }
  }
}
