package cn.universal.web.dto.openapi.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** V2 专用历史查询模型；不向调用方暴露内部 LogQuery、存储策略或游标定位字段。 */
public final class OpenApiV2LogModels {
  private OpenApiV2LogModels() {}

  @Data
  @Schema(description = "固定历史窗口。时间单位均为 Unix 秒，起止时间包含边界，单次窗口最多 31 天。")
  public static class TimeRange {
    @NotNull
    @Min(0)
    @Max(253402300799L)
    @Schema(description = "窗口开始时间，Unix 秒；必填。续页必须与第一页完全一致。", example = "1792108800", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long startTime;

    @NotNull
    @Min(0)
    @Max(253402300799L)
    @Schema(description = "窗口结束时间，Unix 秒；必须不小于 startTime，且相差不超过 2678400 秒（31 天）。", example = "1792195200", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long endTime;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @Schema(description = "普通分页参数。返回精确总数；最多允许跳过 10000 条，批量同步请使用游标接口。")
  public static class PageQuery extends TimeRange {
    @Min(1)
    @Schema(description = "页码，从 1 开始；(page - 1) × pageSize 不得超过 10000。", defaultValue = "1", example = "1")
    private Integer page = 1;

    @Min(1)
    @Max(200)
    @Schema(description = "每页条数，范围 1–200。", defaultValue = "20", example = "20")
    private Integer pageSize = 20;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @Schema(description = "设备完整日志普通分页查询条件。")
  public static class LogPageQuery extends PageQuery {
    @Size(max = 32)
    @Schema(description = "消息类型，大小写不敏感；省略或 ALL 表示全部。", allowableValues = {"ALL", "PROPERTIES", "EVENT", "FUNCTIONS", "REPLY", "ACK"}, example = "PROPERTIES")
    private String messageType;

    @Size(max = 128)
    @Schema(description = "按物模型事件标识符精确筛选，例如 motionAlarm；使用时消息类型须为 EVENT 或省略。", example = "motionAlarm")
    private String event;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @Schema(description = "游标分页参数。不执行 COUNT 或深 OFFSET。首次省略 cursor；后续原样传回 nextCursor，并保留全部查询条件。")
  public static class CursorQuery extends TimeRange {
    @Min(1)
    @Max(200)
    @Schema(description = "每批最大条数，范围 1–200；续页不可改变。", defaultValue = "100", example = "100")
    private Integer limit = 100;

    @Size(max = 2048)
    @Schema(description = "上一次返回的 nextCursor。绑定应用、租户、设备、查询窗口、筛选条件和 limit，不可解析、拼接或跨应用使用。应用密钥轮换后旧游标失效。")
    private String cursor;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @Schema(description = "设备完整日志游标查询条件；按 createTime DESC、id DESC 稳定续页。")
  public static class LogCursorQuery extends CursorQuery {
    @Size(max = 32)
    @Schema(description = "消息类型，大小写不敏感；省略或 ALL 表示全部。续页必须与第一页一致。", allowableValues = {"ALL", "PROPERTIES", "EVENT", "FUNCTIONS", "REPLY", "ACK"}, example = "PROPERTIES")
    private String messageType;

    @Size(max = 128)
    @Schema(description = "物模型事件标识符，精确匹配；使用时消息类型须为 EVENT 或省略。续页不可改变。", example = "motionAlarm")
    private String event;
  }

  @Data
  @Schema(description = "统一历史分页结果。普通分页有 total/page；游标分页 total/page 为 null，以 hasMore/nextCursor 为准。日志保留策略和迟到写入可能影响后续页，游标不提供数据库快照隔离。")
  public static class HistoryPage<T> {
    @Schema(description = "本页记录，始终为数组；按创建时间倒序，同时间按日志 ID 倒序。")
    private List<T> items;
    @Schema(description = "精确总记录数；游标模式不统计，返回 null。", nullable = true, example = "120")
    private Long total;
    @Schema(description = "当前页码；游标模式返回 null。", nullable = true, example = "1")
    private Integer page;
    @Schema(description = "本页最多记录数，普通分页对应 pageSize，游标分页对应 limit。", example = "20")
    private int pageSize;
    @Schema(description = "是否存在下一页；false 时停止遍历。", example = "true")
    private boolean hasMore;
    @Schema(description = "下一页游标；仅游标模式且 hasMore=true 时返回，其余情况为 null。", nullable = true)
    private String nextCursor;
    @Schema(description = "本次固定窗口开始时间，Unix 秒。")
    private Long startTime;
    @Schema(description = "本次固定窗口结束时间，Unix 秒。")
    private Long endTime;
  }

  @Data
  @Schema(description = "设备完整消息日志。字段仅表示平台已有日志，不代表设备执行成功；功能执行结果应结合 REPLY/commandStatus 判断。")
  public static class LogEntry {
    @Schema(description = "日志唯一 ID，以字符串传输，避免 JavaScript 64 位整数精度丢失。", example = "2076250261675757568")
    private String id;
    @Schema(description = "平台设备唯一标识。")
    private String iotId;
    @Schema(description = "产品标识。")
    private String productKey;
    @Schema(description = "产品内设备标识。")
    private String deviceId;
    @Schema(description = "设备名称。")
    private String deviceName;
    @Schema(description = "消息类型：PROPERTIES/EVENT/FUNCTIONS/REPLY/ACK；以存储原值返回。")
    private String messageType;
    @Schema(description = "关联指令 ID；非指令日志可能为空。", nullable = true)
    private String commandId;
    @Schema(description = "协议原生指令状态；可能为空，含义依设备协议而定。", nullable = true)
    private Integer commandStatus;
    @Schema(description = "物模型事件标识；非事件日志可能为空。", nullable = true)
    private String event;
    @Schema(description = "日志原始内容，可能为 JSON 字符串或协议文本。")
    private String content;
    @Schema(description = "平台存储时间，沿用平台日志写入时区的本地时间，不含时区偏移。", example = "2026-10-17T10:20:30.123")
    private LocalDateTime createTime;
  }

  @Data
  @Schema(description = "单个物模型属性的历史值；只返回已启用历史存储的记录，值保留原始文本，不强制转换数字。")
  public static class PropertyEntry {
    @Schema(description = "属性日志唯一 ID，字符串形式。")
    private String id;
    @Schema(description = "平台设备唯一标识。")
    private String iotId;
    @Schema(description = "产品标识。")
    private String productKey;
    @Schema(description = "产品内设备标识。")
    private String deviceId;
    @Schema(description = "物模型属性标识符。", example = "temperature")
    private String property;
    @Schema(description = "属性名称（存储时的名称），可能为空。", nullable = true, example = "温度")
    private String name;
    @Schema(description = "属性原始值的文本表示，可能为数字、字符串或 JSON。", example = "23.5")
    private String value;
    @Schema(description = "属性单位（存储时的单位），可能为空。", nullable = true, example = "℃")
    private String unit;
    @Schema(description = "平台存储时间，沿用平台日志写入时区的本地时间，不含时区偏移。", example = "2026-10-17T10:20:30.123")
    private LocalDateTime createTime;
  }
}
