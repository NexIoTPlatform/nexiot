package cn.universal.admin.system.notice;

import cn.universal.manager.notice.model.NoticeSendRecord;
import cn.universal.manager.notice.service.NoticeRecordService;
import cn.universal.persistence.entity.IoTUser;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 通知记录 */
@RestController
@Tag(name = "通知记录管理", description = "通知记录管理接口")
@RequestMapping("/admin/v1/notice/record")
public class NoticeRecordController extends NoticeScopedController {

  private final NoticeRecordService recordService;

  @Autowired
  public NoticeRecordController(NoticeRecordService recordService) {
    this.recordService = recordService;
  }

  @Operation(summary = "查询通知记录列表", description = "根据查询条件获取通知记录列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取通知记录列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @Parameter(name = "keyword", description = "关键字", required = false)
  @Parameter(name = "type", description = "通知类型", required = false)
  @Parameter(name = "status", description = "状态", required = false)
  @GetMapping("/page")
  public TableDataInfo<NoticeSendRecord> page(
      @RequestParam(defaultValue = "1") Integer pageNum,
      @RequestParam(defaultValue = "10") Integer pageSize,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String status) {
    requirePermission("notice:template:list", "notice:template:detail");

    List<NoticeSendRecord> list = recordService.search(keyword, type, status, null);
    return getDataTable(list, pageNum, pageSize);
  }
}
