/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA状态常量
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.dm.device.service.ota.support;

import cn.universal.common.constant.IoTConstant;
import java.util.Set;

/**
 * OTA 常量定义。
 *
 * <p>该类集中约束 OTA 运行时、任务管理、协议模板之间共享的关键枚举值与字段名，避免前后端、管理端与协议端各自维护不同的字符串。
 *
 * <p>这里的常量主要分三类：
 *
 * <ul>
 *   <li>默认业务值：如默认升级包类型、默认模块、默认功能标识
 *   <li>策略快照字段：保存到 {@code strategyJson} 中，用于调度、灰度、分批、回滚
 *   <li>状态枚举：升级包状态、任务状态、设备执行状态
 * </ul>
 */
public final class OtaConstants {

  private OtaConstants() {}

  /** 默认升级包类型。当前平台把未显式指定类型的升级资源视为固件包。 */
  public static final String DEFAULT_PACKAGE_TYPE = "FIRMWARE";

  /** 默认升级模块。用于标识设备应用区或主程序模块。 */
  public static final String DEFAULT_MODULE = "APPLICATION";

  /**
   * OTA 下发到协议层的统一功能 ID。
   *
   * <p>该功能严格复用平台现有物模型函数下发结构：
   *
   * <pre>{@code
   * {
   *   "messageType": "FUNCTIONS",
   *   "function": "otaUpgrade",
   *   "data": { ... }
   * }
   * }</pre>
   *
   * <p>协议模板可以基于这个标识识别“这是 OTA 指令”，再映射成 MQTT Topic、TCP 指令帧或 HTTP 请求。
   */
  public static final String DEFAULT_FUNCTION_ID = IoTConstant.OTA_UPGRADE;

  /** 启动升级动作。表示平台通知设备开始一次 OTA 任务。 */
  public static final String ACTION_START = "start";

  /** 取消升级动作。表示平台尝试终止设备当前执行中的 OTA。 */
  public static final String ACTION_CANCEL = "cancel";

  /** OTA 上报事件与报告类型常量。 */
  public static final class ReportType {
    public static final String TRANSFER_ACK = "TRANSFER_ACK";
    public static final String TRANSFER_REQUEST = "TRANSFER_REQUEST";
    /** 固件/软件版本确认。通常对应 messageType = PROPERTIES。 */
    public static final String FIRMWARE_INFO = "FIRMWARE_INFO";

    /** 升级过程进度。通常对应 messageType = EVENT, event = otaProgress。 */
    public static final String UPGRADE_PROGRESS = "UPGRADE_PROGRESS";

    /** 升级最终结果。通常对应 messageType = EVENT, event = otaResult。 */
    public static final String UPGRADE_RESULT = "UPGRADE_RESULT";

    private ReportType() {}
  }

  /** OTA 统一事件名。 */
  public static final class Event {
    /** 兼容旧示例保留的版本确认事件名，当前更推荐直接使用 PROPERTIES。 */
    public static final String OTA_INFO = "otaInfo";

    /** OTA 进度事件。 */
    public static final String OTA_PROGRESS = "otaProgress";

    /** OTA 最终结果事件。 */
    public static final String OTA_RESULT = "otaResult";

    /** 兼容旧协议保留的泛化事件名。 */
    public static final String OTA_REPORT = "otaReport";

    private Event() {}
  }

  /** {@code strategyJson} 中的字段定义。每个任务创建时会固化一份策略快照。 */
  public static final class StrategyKey {
    /** 是否自动下发。为 false 时任务创建后需人工点击“下发”。 */
    public static final String AUTO_DISPATCH = "autoDispatch";

    /** 计划开始时间，单位毫秒时间戳。为空表示可立即执行。 */
    public static final String PLANNED_START_TIME = "plannedStartTime";

    /** 单批次最多下发多少台设备。小于等于 0 视为一次性全量下发。 */
    public static final String BATCH_SIZE = "batchSize";

    /** 批次之间的最小间隔秒数，仅分批发布时生效。 */
    public static final String BATCH_INTERVAL_SECONDS = "batchIntervalSeconds";

    /** 上一批次实际下发时间，用于调度器判断下一批何时放量。 */
    public static final String LAST_BATCH_DISPATCH_TIME = "lastBatchDispatchTime";

    /** 当前已下发到第几批，便于任务追踪与页面展示。 */
    public static final String CURRENT_BATCH_NO = "currentBatchNo";

    /** 灰度比例。按候选设备总数计算应选中的设备数量。 */
    public static final String GRAY_PERCENT = "grayPercent";

    /** 灰度设备上限。用于在比例之外再加一层绝对数量控制。 */
    public static final String GRAY_DEVICE_LIMIT = "grayDeviceLimit";

    /** 候选设备中最终被本次任务选中的设备数量。 */
    public static final String SELECTED_DEVICE_COUNT = "selectedDeviceCount";

    /** 灰度裁剪前的候选设备总数。 */
    public static final String CANDIDATE_DEVICE_COUNT = "candidateDeviceCount";

    /** 失败阈值百分比。达到阈值后可触发自动回滚。 */
    public static final String FAILURE_THRESHOLD_PERCENT = "failureThresholdPercent";

    /** 是否开启失败自动回滚。 */
    public static final String ROLLBACK_ON_FAILURE = "rollbackOnFailure";

    /** 自动回滚时要切回的升级包 ID。 */
    public static final String ROLLBACK_PACKAGE_ID = "rollbackPackageId";

    /** 当前任务是否已经触发过回滚。 */
    public static final String ROLLBACK_TRIGGERED = "rollbackTriggered";

    /** 回滚被触发的时间，单位毫秒时间戳。 */
    public static final String ROLLBACK_TRIGGERED_TIME = "rollbackTriggeredTime";

    /** 自动创建的回滚任务 ID。 */
    public static final String ROLLBACK_TASK_ID = "rollbackTaskId";

    /** 若当前任务是回滚任务，则记录其来源任务 ID。 */
    public static final String SOURCE_TASK_ID = "sourceTaskId";

    /** 是否要求设备当前版本必须低于目标版本才允许下发。 */
    public static final String REQUIRE_VERSION_CHECK = "requireVersionCheck";

    /** 单设备最大重试次数。 */
    public static final String MAX_RETRY_COUNT = "maxRetryCount";

    /** 单设备执行 OTA 的超时时间，单位秒。 */
    public static final String TIMEOUT_SECONDS = "timeoutSeconds";

    private StrategyKey() {}
  }

  /** 升级包生命周期状态。 */
  public static final class PackageStatus {
    /** 草稿状态。可编辑但不允许直接创建升级任务。 */
    public static final String DRAFT = "DRAFT";

    /** 已发布状态。允许被任务引用。 */
    public static final String PUBLISHED = "PUBLISHED";

    /** 已停用状态。保留历史信息，但不再允许新的任务引用。 */
    public static final String DISABLED = "DISABLED";

    private PackageStatus() {}
  }

  /** OTA 任务整体状态。 */
  public static final class TaskStatus {
    /** 无有效设备或尚未完整初始化时的草稿态。 */
    public static final String DRAFT = "DRAFT";

    /** 已创建但尚未执行。可用于定时任务等待或手动下发前状态。 */
    public static final String PENDING = "PENDING";

    /** 任务已有设备进入等待/执行阶段。 */
    public static final String RUNNING = "RUNNING";

    /** 全部设备成功或被安全跳过。 */
    public static final String COMPLETED = "COMPLETED";

    /** 有成功也有失败/取消，属于部分成功。 */
    public static final String PARTIAL_SUCCESS = "PARTIAL_SUCCESS";

    /** 整体失败，或已触发失败阈值导致任务终止。 */
    public static final String FAILED = "FAILED";

    /** 任务被人工或系统取消。 */
    public static final String CANCELED = "CANCELED";

    private TaskStatus() {}
  }

  /** 设备在 OTA 任务中的细粒度执行状态。 */
  public static final class DeviceStatus {
    /** 已加入本次任务，但还未真正下发到设备。 */
    public static final String WAITING_PUSH = "WAITING_PUSH";

    /** OTA 指令已通过协议层下发，等待设备开始执行。 */
    public static final String PUSHED = "PUSHED";

    /** 设备正在下载升级包。通常指设备自行通过 HTTP/HTTPS 拉取文件。 */
    public static final String DOWNLOADING = "DOWNLOADING";

    /** 设备已下载完成，正在进行校验或签名验证。 */
    public static final String VERIFYING = "VERIFYING";

    /** 设备正在安装升级包。 */
    public static final String INSTALLING = "INSTALLING";

    /** 安装完成后设备正在重启或切换版本。 */
    public static final String REBOOTING = "REBOOTING";

    /** 设备已确认升级成功。 */
    public static final String SUCCESS = "SUCCESS";

    /** 设备明确返回失败。 */
    public static final String FAILED = "FAILED";

    /** 在约定时间窗口内未收到成功结果。 */
    public static final String TIMEOUT = "TIMEOUT";

    /** 升级过程被取消。 */
    public static final String CANCELED = "CANCELED";

    /** 平台根据规则跳过该设备，例如版本已高于目标版本。 */
    public static final String SKIPPED = "SKIPPED";

    private DeviceStatus() {}
  }

  /** 仍可能继续推进的设备状态集合。 */
  public static final Set<String> ACTIVE_DEVICE_STATUSES =
      Set.of(
          DeviceStatus.WAITING_PUSH,
          DeviceStatus.PUSHED,
          DeviceStatus.DOWNLOADING,
          DeviceStatus.VERIFYING,
          DeviceStatus.INSTALLING,
          DeviceStatus.REBOOTING);

  /** 已经结束、不会再自动推进的设备状态集合。 */
  public static final Set<String> FINAL_DEVICE_STATUSES =
      Set.of(
          DeviceStatus.SUCCESS,
          DeviceStatus.FAILED,
          DeviceStatus.TIMEOUT,
          DeviceStatus.CANCELED,
          DeviceStatus.SKIPPED);

  /** 判断设备状态是否已经结束。 */
  public static boolean isFinalDeviceStatus(String status) {
    return status != null && FINAL_DEVICE_STATUSES.contains(status);
  }

  /** 判断设备状态是否仍处于等待或执行阶段。 */
  public static boolean isActiveDeviceStatus(String status) {
    return status != null && ACTIVE_DEVICE_STATUSES.contains(status);
  }
}
