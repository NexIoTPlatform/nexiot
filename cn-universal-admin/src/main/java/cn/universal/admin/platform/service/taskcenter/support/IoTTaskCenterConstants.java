package cn.universal.admin.platform.service.taskcenter.support;

public final class IoTTaskCenterConstants {

  private IoTTaskCenterConstants() {}

  public static final class TaskType {
    public static final String DEVICE_IMPORT = "DEVICE_IMPORT";
    public static final String DEVICE_FUNCTION_JOB = "DEVICE_FUNCTION_JOB";
    public static final String DEVICE_CREDENTIAL_GENERATE = "DEVICE_CREDENTIAL_GENERATE";

    private TaskType() {}
  }

  public static final class TaskStatus {
    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String COMPLETED = "COMPLETED";
    public static final String PARTIAL_SUCCESS = "PARTIAL_SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String CANCELED = "CANCELED";

    private TaskStatus() {}
  }

  public static final class DefinitionScheduleType {
    public static final String MANUAL = "MANUAL";
    public static final String CRON = "CRON";

    private DefinitionScheduleType() {}
  }

  public static final class DefinitionScheduleStatus {
    public static final String ENABLED = "ENABLED";
    public static final String PAUSED = "PAUSED";

    private DefinitionScheduleStatus() {}
  }

  public static final class TriggerType {
    public static final String DIRECT = "DIRECT";
    public static final String MANUAL = "MANUAL";
    public static final String SCHEDULE = "SCHEDULE";

    private TriggerType() {}
  }

  public static final class ItemStatus {
    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    private ItemStatus() {}
  }

  public static final class CommandExecutionMode {
    public static final String SERIAL = "SERIAL";
    public static final String PARALLEL = "PARALLEL";

    private CommandExecutionMode() {}
  }

  public static final class LogLevel {
    public static final String INFO = "INFO";
    public static final String WARN = "WARN";
    public static final String ERROR = "ERROR";

    private LogLevel() {}
  }

  public static final class LogType {
    public static final String SUBMIT = "SUBMIT";
    public static final String EXECUTE = "EXECUTE";
    public static final String ITEM = "ITEM";
    public static final String FINISH = "FINISH";

    private LogType() {}
  }

  public static final class LockKey {
    public static final String TASK_DEFINITION_RUN = "task:center:definition:run:";

    private LockKey() {}
  }
}
