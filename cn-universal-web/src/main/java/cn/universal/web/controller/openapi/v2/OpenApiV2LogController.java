package cn.universal.web.controller.openapi.v2;

import cn.universal.common.domain.R;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.CursorQuery;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.HistoryPage;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.LogCursorQuery;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.LogEntry;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.LogPageQuery;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.PageQuery;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.PropertyEntry;
import cn.universal.web.service.openapi.v2.OpenApiV2LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 子系统设备历史查询专用入口。V1 地址、返回值与日志存储实现保持兼容。
 *
 * <p>调用方使用应用 client_credentials access_token；每次读取（包括每次续页）重新校验当前应用、 租户与设备绑定，游标只表示位置，不授予设备权限。
 */
@RestController
@RequestMapping(
    value = "/api/openapi/v2/devices/{productKey}/{deviceId}",
    produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "开放 API V2 - 设备历史", description = "设备完整日志及单属性历史值；支持带总数的浅分页和不计总数的稳定游标分页。")
@ApiResponses({
  @ApiResponse(
      responseCode = "200",
      description = "查询成功，data 为完整类型的历史分页结果。",
      useReturnTypeSchema = true),
  @ApiResponse(responseCode = "400", description = "参数无效：Unix秒范围、窗口超过31天、分页超限、消息类型错误或游标失效。"),
  @ApiResponse(responseCode = "401", description = "未携带有效的应用 access_token。"),
  @ApiResponse(responseCode = "403", description = "应用无效、设备未绑定当前应用或不属于当前租户。"),
  @ApiResponse(responseCode = "503", description = "历史存储暂不可用、超时或当前应用密钥无法用于安全游标。")
})
public class OpenApiV2LogController {
  private final OpenApiV2LogService logs;

  public OpenApiV2LogController(OpenApiV2LogService logs) {
    this.logs = logs;
  }

  @GetMapping("/logs")
  @Operation(
      operationId = "openApiV2DeviceLogs",
      summary = "查询设备日志（普通分页）",
      description =
          "查询当前应用已绑定设备的完整消息日志。startTime/endTime 必填，为 Unix 秒且窗口最多31天；"
              + "page 从1开始，pageSize 为1–200，最多跳过10000条。返回 items、total、page、pageSize、hasMore。"
              + "记录按 createTime DESC、id DESC 排序；对接系统持续拉取或导出大量记录请使用 /logs/cursor。"
              + "同一查询窗口内的新写入可能影响普通页码，客户端应按日志 id 去重。")
  public R<HistoryPage<LogEntry>> logs(
      @Parameter(description = "设备所属产品 Key。", required = true) @PathVariable String productKey,
      @Parameter(description = "产品内设备标识，不是 iotId。", required = true) @PathVariable String deviceId,
      @Valid @ParameterObject LogPageQuery query) {
    return R.ok(logs.logs(productKey, deviceId, query));
  }

  @GetMapping("/logs/cursor")
  @Operation(
      operationId = "openApiV2DeviceLogsCursor",
      summary = "查询设备日志（游标分页）",
      description =
          "用于子系统批量同步。首次固定 startTime/endTime 和 limit，省略 cursor；"
              + "当 hasMore=true 时，用 nextCursor 作为下一次 cursor，其他条件保持完全一致；hasMore=false 时结束。"
              + "使用 createTime+id 组合倒序续页，不执行 COUNT 或深 OFFSET；total/page 为 null。"
              + "游标经过签名，绑定当前应用、租户、设备、时间窗口、消息类型、事件和 limit；"
              + "跨应用使用、任意篡改、改变筛选条件或应用密钥轮换均返回400。每次续页仍重新校验设备授权。"
              + "游标不是快照或实时订阅；历史迟到写入可能需要重叠窗口补偿并按 id 去重。")
  public R<HistoryPage<LogEntry>> logCursor(
      @Parameter(description = "设备所属产品 Key。", required = true) @PathVariable String productKey,
      @Parameter(description = "产品内设备标识。", required = true) @PathVariable String deviceId,
      @Valid @ParameterObject LogCursorQuery query) {
    return R.ok(logs.logCursor(productKey, deviceId, query));
  }

  @GetMapping("/properties/{property}/logs")
  @Operation(
      operationId = "openApiV2PropertyLogs",
      summary = "查询单属性历史值（普通分页）",
      description =
          "按物模型 property 标识查询属性历史，不查询设备影子的当前值。"
              + "仅返回已启用历史存储的 PROPERTIES 记录；value 保留原始文本，name/unit 为存储时元数据。"
              + "startTime/endTime 为必填 Unix 秒，最多31天；page/pageSize 规则与设备日志普通分页相同。"
              + "返回精确 total；大量导出请使用对应 /logs/cursor。")
  public R<HistoryPage<PropertyEntry>> properties(
      @Parameter(description = "设备所属产品 Key。", required = true) @PathVariable String productKey,
      @Parameter(description = "产品内设备标识。", required = true) @PathVariable String deviceId,
      @Parameter(
              description = "单个物模型属性标识，最长128字符，例如 temperature。",
              required = true,
              example = "temperature")
          @PathVariable
          String property,
      @Valid @ParameterObject PageQuery query) {
    return R.ok(logs.properties(productKey, deviceId, property, query));
  }

  @GetMapping("/properties/{property}/logs/cursor")
  @Operation(
      operationId = "openApiV2PropertyLogsCursor",
      summary = "查询单属性历史值（游标分页）",
      description =
          "按单个物模型属性导出历史。首次传 startTime/endTime/limit，省略 cursor；"
              + "续页原样传回 nextCursor，且应用、设备、property、窗口和 limit 必须保持不变。"
              + "按 createTime DESC、id DESC 续页，无 COUNT/OFFSET；hasMore=false 时结束，total/page 为 null。"
              + "窗口最多31天、limit 为1–200。游标经过应用密钥签名，密钥轮换后需重新开始。"
              + "设备未绑定、跨租户或应用失效时拒绝访问；历史存储策略可能使结果为空。")
  public R<HistoryPage<PropertyEntry>> propertyCursor(
      @Parameter(description = "设备所属产品 Key。", required = true) @PathVariable String productKey,
      @Parameter(description = "产品内设备标识。", required = true) @PathVariable String deviceId,
      @Parameter(description = "单个物模型属性标识，最长128字符。", required = true, example = "temperature")
          @PathVariable
          String property,
      @Valid @ParameterObject CursorQuery query) {
    return R.ok(logs.propertyCursor(productKey, deviceId, property, query));
  }
}
