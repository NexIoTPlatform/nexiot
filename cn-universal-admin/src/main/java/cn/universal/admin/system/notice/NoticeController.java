package cn.universal.admin.system.notice;

import cn.universal.admin.system.service.ISysNoticeService;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.admin.SysNotice;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统通知Controller
 *
 * @author universal
 * @date 2023-01-01
 */
@RestController
@Tag(name = "系统通知管理", description = "系统通知管理接口")
@RequestMapping("/admin/system/notice")
public class NoticeController extends BaseController {

  @Autowired private ISysNoticeService noticeService;

  /** 查询系统通知列表 */
  @Operation(summary = "查询系统通知列表", description = "根据查询条件获取系统通知列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取系统通知列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo<SysNotice> list(
      @RequestParam(defaultValue = "1") Integer pageNum,
      @RequestParam(defaultValue = "10") Integer pageSize,
      SysNotice notice) {
    IoTUser iotUser = currentTenantUser();
    List<SysNotice> list = noticeService.selectNoticeList(notice);
    if (iotUser != null && !iotUser.isAdmin()) {
      list =
          list.stream()
              .filter(item -> canAccessCreatorResource(iotUser, item.getCreateBy()))
              .toList();
    }
    return getDataTable(list, pageNum, pageSize);
  }

  /** 获取系统通知详细信息 */
  @Operation(summary = "获取系统通知详细信息", description = "根据通知ID获取系统通知详细信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取系统通知详细信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = SysNotice.class)))
  @Parameter(name = "noticeId", description = "通知ID", required = true)
  @GetMapping(value = "/{noticeId}")
  public SysNotice getInfo(@PathVariable("noticeId") Long noticeId) {
    SysNotice notice = noticeService.selectNoticeById(noticeId);
    if (notice == null) {
      return null;
    }

    if (!canAccessCreatorResource(currentTenantUser(), notice.getCreateBy())) {
      return null;
    }

    return notice;
  }

  /** 新增系统通知 */
  @Operation(summary = "新增系统通知", description = "新增系统通知信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增系统通知",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Void.class)))
  @PostMapping
  public void add(@RequestBody SysNotice notice) {
    // 设置创建者
    notice.setCreateBy(SecurityUtils.getUnionId());
    noticeService.insertNotice(notice);
  }

  /** 修改系统通知 */
  @Operation(summary = "修改系统通知", description = "修改系统通知信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改系统通知",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Void.class)))
  @PutMapping
  public void edit(@RequestBody SysNotice notice) {
    String currentUser = SecurityUtils.getUnionId();

    SysNotice existingNotice = noticeService.selectNoticeById(notice.getNoticeId());
    if (existingNotice == null) {
      throw new RuntimeException("通知不存在");
    }

    if (!canAccessCreatorResource(currentTenantUser(), existingNotice.getCreateBy())) {
      throw new RuntimeException("无权限修改该通知");
    }

    // 设置更新者
    notice.setUpdateBy(currentUser);
    noticeService.updateNotice(notice);
  }

  /** 删除系统通知 */
  @Operation(summary = "删除系统通知", description = "根据通知ID数组删除系统通知")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除系统通知",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Void.class)))
  @Parameter(name = "noticeIds", description = "通知ID数组", required = true)
  @DeleteMapping("/{noticeIds}")
  public void remove(@PathVariable Long[] noticeIds) {
    IoTUser iotUser = currentTenantUser();
    if (iotUser != null && !iotUser.isAdmin()) {
      for (Long noticeId : noticeIds) {
        SysNotice notice = noticeService.selectNoticeById(noticeId);
        if (notice != null && !canAccessCreatorResource(iotUser, notice.getCreateBy())) {
          throw new RuntimeException("无权限删除该通知");
        }
      }
    }

    noticeService.deleteNoticeByIds(noticeIds);
  }
}
