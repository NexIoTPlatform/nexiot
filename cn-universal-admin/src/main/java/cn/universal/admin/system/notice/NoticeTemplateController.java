package cn.universal.admin.system.notice;

import cn.universal.manager.notice.dto.NoticeTemplateDTO;
import cn.universal.manager.notice.model.NoticeTemplate;
import cn.universal.manager.notice.service.NoticeTemplateService;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "通知模板管理", description = "通知模板管理接口")
@RequestMapping("/admin/v1/notice/template")
public class NoticeTemplateController extends NoticeScopedController {

  private final NoticeTemplateService noticeTemplateService;
  @Autowired private cn.universal.manager.notice.service.NoticeChannelService channels;
  @Autowired private cn.universal.manager.notice.service.NoticeService noticeService;

  @Autowired
  public NoticeTemplateController(NoticeTemplateService noticeTemplateService) {
    this.noticeTemplateService = noticeTemplateService;
  }

  @Operation(summary = "查询通知模板列表", description = "根据查询条件获取通知模板列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取通知模板列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @Parameter(name = "pageNum", description = "页码", required = false)
  @Parameter(name = "pageSize", description = "每页大小", required = false)
  @Parameter(name = "name", description = "模板名称", required = false)
  @Parameter(name = "channelType", description = "渠道类型", required = false)
  @GetMapping("/list")
  public TableDataInfo<NoticeTemplateDTO> list(
      @RequestParam(defaultValue = "1") Integer pageNum,
      @RequestParam(defaultValue = "10") Integer pageSize,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String channelType,
      @RequestParam(required = false) String status) {
    requirePermission("notice:template:list", "notice:template:detail", "notice:template:add", "notice:template:edit");

    List<NoticeTemplate> list = noticeTemplateService.search(name, channelType, status);

    List<NoticeTemplateDTO> dtoList =
        list.stream()
            .map(
                template -> {
                  NoticeTemplateDTO dto = new NoticeTemplateDTO();
                  dto.setId(template.getId());
                  dto.setTenantId(template.getTenantId());
                  dto.setDeptId(template.getDeptId());
                  dto.setName(template.getName());
                  dto.setChannelType(template.getChannelType());
                  dto.setChannelId(template.getChannelId());
                  dto.setContent(template.getContent());
                  dto.setWechat(template.getWechat());
                  dto.setReceivers(template.getReceivers());
                  dto.setStatus(template.getStatus());
                  dto.setRemark(template.getRemark());
                  dto.setCreator(template.getCreator());
                  dto.setCreateTime(template.getCreateTime());
                  dto.setUpdateTime(template.getUpdateTime());

                  return dto;
                })
            .toList();
    return getDataTable(dtoList, pageNum, pageSize);
  }

  @Operation(summary = "保存通知模板", description = "保存通知模板信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功保存通知模板",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "templateDTO", description = "通知模板信息", required = true)
  @PostMapping("/save")
  public AjaxResult save(@RequestBody NoticeTemplateDTO templateDTO) {
    requirePermission(templateDTO.getId() == null ? "notice:template:add" : "notice:template:edit");
    try {
      String currentUser = SecurityUtils.getUnionId();
      IoTUser iotUser = currentTenantUser();

      if (templateDTO.getId() != null) {
        NoticeTemplate existingTemplate = noticeTemplateService.getById(templateDTO.getId());
        if (existingTemplate == null) {
          return AjaxResult.error("模板不存在");
        }
        if (!canAccessNotice(iotUser, existingTemplate, existingTemplate.getCreator())) {
          return AjaxResult.error("无权限修改该模板");
        }
      }

      cn.universal.manager.notice.model.NoticeChannel channel = channels.getById(templateDTO.getChannelId());
      if (channel == null || !canAccessNotice(iotUser, channel, channel.getCreator())) return AjaxResult.error("无权限使用该通知渠道");
      noticeTemplateService.saveTemplate(templateDTO, currentUser);
      return AjaxResult.success("操作成功");
    } catch (Exception e) {
      return AjaxResult.error("操作失败: " + noticeError(e));
    }
  }

  @Operation(summary = "删除通知模板", description = "根据ID数组删除通知模板")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除通知模板",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "ids", description = "通知模板ID数组", required = true)
  @PostMapping("/delete")
  public AjaxResult delete(@RequestBody List<Long> ids) {
    requirePermission("notice:template:del");
    try {
      if (ids != null && !ids.isEmpty()) {
        IoTUser iotUser = currentTenantUser();

        if (iotUser == null || !iotUser.isAdmin()) {
          for (Long id : ids) {
            NoticeTemplate template = noticeTemplateService.getById(id);
            if (template != null && !canAccessNotice(iotUser, template, template.getCreator())) {
              return AjaxResult.error("无权限删除该模板");
            }
          }
        }

        noticeTemplateService.deleteBatch(ids);
        return AjaxResult.success("删除成功");
      } else {
        return AjaxResult.success("未提供有效的ID");
      }
    } catch (Exception e) {
      return AjaxResult.error("删除失败: " + noticeError(e));
    }
  }

  /** 批量启用通知模板 */
  @Operation(summary = "批量启用通知模板", description = "根据ID数组启用通知模板")
  @ApiResponse(
      responseCode = "200",
      description = "成功启用通知模板",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "ids", description = "通知模板ID数组", required = true)
  @PostMapping("/enable")
  public AjaxResult enable(@RequestBody List<Long> ids) {
    requirePermission("notice:template:edit");
    try {
      if (ids != null && !ids.isEmpty()) {
        IoTUser iotUser = currentTenantUser();

        if (iotUser == null || !iotUser.isAdmin()) {
          for (Long id : ids) {
            NoticeTemplate template = noticeTemplateService.getById(id);
            if (template != null && !canAccessNotice(iotUser, template, template.getCreator())) {
              return AjaxResult.error("无权限启用该模板");
            }
          }
        }

        noticeTemplateService.enableBatch(ids);
        return AjaxResult.success("启用成功");
      } else {
        return AjaxResult.success("未提供有效的ID");
      }
    } catch (Exception e) {
      return AjaxResult.error("启用失败: " + noticeError(e));
    }
  }

  /** 批量停用通知模板 */
  @Operation(summary = "批量停用通知模板", description = "根据ID数组停用通知模板")
  @ApiResponse(
      responseCode = "200",
      description = "成功停用通知模板",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "ids", description = "通知模板ID数组", required = true)
  @PostMapping("/disable")
  public AjaxResult disable(@RequestBody List<Long> ids) {
    requirePermission("notice:template:edit");
    try {
      if (ids != null && !ids.isEmpty()) {
        IoTUser iotUser = currentTenantUser();

        if (iotUser == null || !iotUser.isAdmin()) {
          for (Long id : ids) {
            NoticeTemplate template = noticeTemplateService.getById(id);
            if (template != null && !canAccessNotice(iotUser, template, template.getCreator())) {
              return AjaxResult.error("无权限停用该模板");
            }
          }
        }

        noticeTemplateService.disableBatch(ids);
        return AjaxResult.success("停用成功");
      } else {
        return AjaxResult.success("未提供有效的ID");
      }
    } catch (Exception e) {
      return AjaxResult.error("停用失败: " + noticeError(e));
    }
  }

  @Operation(summary = "获取通知模板详情", description = "根据ID获取通知模板详情")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取通知模板详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "通知模板ID", required = true)
  @GetMapping("/get")
  public AjaxResult get(@RequestParam Long id) {
    requirePermission("notice:template:detail", "notice:template:edit");
    try {
      NoticeTemplate template = noticeTemplateService.getById(id);
      if (template == null) {
        return AjaxResult.error("模板不存在");
      }

      if (!canAccessNotice(currentTenantUser(), template, template.getCreator())) {
        return AjaxResult.error("无权限访问该模板");
      }

      NoticeTemplateDTO dto = new NoticeTemplateDTO();
      dto.setId(template.getId());
      dto.setTenantId(template.getTenantId());
      dto.setDeptId(template.getDeptId());
      dto.setName(template.getName());
      dto.setChannelType(template.getChannelType());
      dto.setChannelId(template.getChannelId());
      dto.setContent(template.getContent());
                  dto.setWechat(template.getWechat());
      dto.setReceivers(template.getReceivers());
      dto.setStatus(template.getStatus());
      dto.setRemark(template.getRemark());
      dto.setCreator(template.getCreator());
      dto.setCreateTime(template.getCreateTime());
      dto.setUpdateTime(template.getUpdateTime());
      return AjaxResult.success(dto);
    } catch (Exception e) {
      return AjaxResult.error("查询失败: " + noticeError(e));
    }
  }

  @PostMapping("/test")
  public AjaxResult test(@RequestBody Map<String, Object> request) {
    requirePermission("notice:template:edit");
    try {
      Long templateId = Long.valueOf(request.get("templateId").toString());
      String receivers = (String) request.get("receivers");
      Object params = request.get("params");

      NoticeTemplate template = noticeTemplateService.getById(templateId);
      if (template == null) {
        return AjaxResult.error("模板不存在");
      }

      if (!canAccessNotice(currentTenantUser(), template, template.getCreator())) {
        return AjaxResult.error("无权限测试该模板");
      }

      noticeTemplateService.testTemplate(templateId, receivers, params);
      return AjaxResult.success("成功");
    } catch (Exception e) {
      return AjaxResult.error("测试失败: " + noticeError(e));
    }
  }
  @PostMapping("/preview")
  public AjaxResult preview(@RequestBody cn.universal.manager.notice.model.NoticeSendRequest request) {
    requirePermission("notice:template:detail", "notice:template:edit");
    try {
      NoticeTemplate template = noticeTemplateService.getById(request.getTemplateId());
      if (template == null || !canAccessNotice(currentTenantUser(), template, template.getCreator())) return AjaxResult.error("无权限访问该模板");
      return AjaxResult.success(noticeService.preview(request));
    } catch (Exception e) { return AjaxResult.error(noticeError(e)); }
  }

}
