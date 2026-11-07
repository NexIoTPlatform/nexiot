package cn.universal.admin.system.notice;

import cn.universal.manager.notice.model.NoticeSendRequest;
import cn.universal.manager.notice.model.NoticeTemplate;
import cn.universal.manager.notice.service.NoticeService;
import cn.universal.manager.notice.service.NoticeTemplateService;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 通知发送 */
@RestController
@Tag(name = "通知发送管理", description = "通知发送管理接口")
@RequestMapping("/admin/v1/notice")
public class NoticeSendController extends NoticeScopedController {

  private final NoticeService noticeService;
  private final NoticeTemplateService templateService;

  @Autowired
  public NoticeSendController(NoticeService noticeService, NoticeTemplateService templateService) {
    this.noticeService = noticeService;
    this.templateService = templateService;
  }

  @Operation(summary = "发送通知", description = "根据模板ID、接收者和参数发送通知")
  @ApiResponse(
      responseCode = "200",
      description = "成功发送通知",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  @Parameter(name = "request", description = "发送通知请求参数", required = true)
  @PostMapping("/send")
  public Map<String, Object> send(@RequestBody Map<String, Object> request) {
    requirePermission("notice:template:edit");
    if (request.get("templateId") == null) throw new IllegalArgumentException("模板ID不能为空");
    Long templateId = Long.valueOf(request.get("templateId").toString());
    String receivers = (String) request.get("receivers");
    Object params = request.get("params");

    if (templateId == null) {
      throw new IllegalArgumentException("模板ID不能为空");
    }

    String currentUser = SecurityUtils.getUnionId();
    IoTUser iotUser = currentTenantUser();

    NoticeTemplate template = templateService.getById(templateId);
    if (template == null) {
      throw new IllegalArgumentException("模板不存在");
    }

    if (!canAccessNotice(iotUser, template, template.getCreator())) {
      throw new IllegalArgumentException("无权限使用该模板发送通知");
    }

    NoticeSendRequest req = new NoticeSendRequest();
    req.setTemplateId(templateId);
    req.setReceivers(receivers);
    req.setCreator(currentUser); // 设置创建者为当前用户
    if (params instanceof Map) {
      req.setParams((Map<String, Object>) params);
    }
    noticeService.send(req);

    return Map.of("code", 0, "msg", "发送成功");
  }
}
