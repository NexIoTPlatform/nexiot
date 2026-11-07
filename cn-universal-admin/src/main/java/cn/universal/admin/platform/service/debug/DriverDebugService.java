package cn.universal.admin.platform.service.debug;

import cn.universal.common.exception.IoTException;
import cn.universal.core.engine.ScriptDebugOutput;
import cn.universal.core.engine.exception.MagicScriptException;
import cn.universal.core.protocol.magic.ProtocolCodecMagic;
import cn.universal.core.protocol.support.ProtocolCodecSupport.CodecMethod;
import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.stereotype.Service;

@Service
public class DriverDebugService {
  public record RunRequest(
      @NotBlank @Size(max = 262144) String source,
      @NotNull @Size(max = 1048576) String payload,
      @Size(max = 65536) String context,
      @NotBlank @Pattern(regexp = "decode|encode|preDecode|iotToYour|yourToIot") String method,
      boolean debug,
      @NotNull @Size(max = 200) List<@Min(1) @Max(20000) Integer> breakpoints) {}

  public record Command(
      @Pattern(regexp = "continue|step|breakpoints") @NotNull String action,
      @NotNull @Size(max = 200) List<@Min(1) @Max(20000) Integer> breakpoints) {}

  private final Map<String, Session> sessions = new ConcurrentHashMap<>();
  private final ExecutorService executor =
      new ThreadPoolExecutor(
          4,
          4,
          0,
          TimeUnit.SECONDS,
          new ArrayBlockingQueue<>(12),
          Thread.ofPlatform().daemon().name("driver-debug-", 0).factory());
  private final ScheduledExecutorService cleaner =
      Executors.newSingleThreadScheduledExecutor(
          Thread.ofPlatform().daemon().name("driver-debug-cleaner").factory());

  public DriverDebugService() {
    cleaner.scheduleWithFixedDelay(this::expire, 5, 5, TimeUnit.SECONDS);
  }

  public synchronized Map<String, Object> start(
      String owner, String protocolId, RunRequest request) {
    expire();
    if (sessions.size() >= 32) {
      sessions.values().stream()
          .filter(Session::terminal)
          .min(Comparator.comparingLong(s -> s.finished))
          .ifPresent(s -> sessions.remove(s.id, s));
    }
    if (sessions.size() >= 32
        || sessions.values().stream().filter(s -> s.owner.equals(owner) && !s.terminal()).count()
            >= 2) {
      throw new IoTException("调试会话已达上限，请先停止其他运行");
    }
    Session s = new Session(owner, protocolId, request);
    sessions.put(s.id, s);
    s.future =
        new FutureTask<>(new DelegatingSecurityContextRunnable(() -> execute(s, request)), null);
    try {
      executor.execute(s.future);
    } catch (RejectedExecutionException e) {
      sessions.remove(s.id);
      throw new IoTException("调试服务繁忙，请稍后重试");
    }
    return s.snapshot();
  }

  public Map<String, Object> poll(String id, String owner, String protocolId) {
    Session s = require(id, owner, protocolId);
    s.touched = System.currentTimeMillis();
    return s.snapshot();
  }

  public Map<String, Object> command(String id, String owner, String protocolId, Command command) {
    Session s = require(id, owner, protocolId);
    synchronized (s) {
      s.touched = System.currentTimeMillis();
      if (command.action().equals("breakpoints")) {
        if (!s.terminal()) s.context.updateBreakpoints(command.breakpoints());
        return s.snapshot();
      }
      if (!s.state.equals("PAUSED")) throw new IoTException("当前会话未停在断点");
      s.leavePause();
      s.state = "RUNNING";
      s.line = null;
      s.context.proceed(command.action().equals("step"), command.breakpoints());
    }
    return s.snapshot();
  }

  public void cancel(String id, String owner, String protocolId) {
    stop(require(id, owner, protocolId), "已停止");
  }

  private Session require(String id, String owner, String protocolId) {
    Session s = sessions.get(id);
    if (s == null || !s.owner.equals(owner) || !s.protocolId.equals(protocolId)) {
      throw new IoTException("调试会话不存在或已过期");
    }
    return s;
  }

  private void execute(Session s, RunRequest request) {
    synchronized (s) {
      if (s.terminal()) return;
      s.state = "RUNNING";
    }
    ScriptDebugOutput.bind(s::log);
    try {
      s.context.set("payload", request.payload());
      s.context.set("context", request.context());
      String result =
          ProtocolCodecMagic.getInstance()
              .executeDraft(request.source(), CodecMethod.valueOf(request.method()), s.context);
      synchronized (s) {
        if (!s.terminal()) {
          s.result = DriverDebugContext.truncate(result, 65536);
          s.finish("SUCCEEDED");
        }
      }
    } catch (Throwable e) {
      synchronized (s) {
        if (!s.terminal()) {
          s.error = DriverDebugContext.truncate(Objects.toString(e.getMessage(), "执行失败"), 8000);
          Throwable cursor = e;
          Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
          while (cursor != null && seen.add(cursor)) {
            if (cursor instanceof MagicScriptException script && script.getLine() != null) {
              s.line = script.getLine().getLineNumber();
              s.error = DriverDebugContext.truncate(script.getSimpleMessage(), 8000);
              break;
            }
            cursor = cursor.getCause();
          }
          s.finish("FAILED");
        }
      }
    } finally {
      ScriptDebugOutput.clear();
    }
  }

  private void stop(Session s, String reason) {
    synchronized (s) {
      if (s.terminal()) return;
      s.error = reason;
      s.finish("CANCELLED");
      s.context.cancel();
      if (s.future != null) s.future.cancel(true);
    }
  }

  private void expire() {
    long now = System.currentTimeMillis();
    sessions
        .values()
        .forEach(
            s -> {
              if (!s.terminal()
                  && (s.runningMillis(now) > 120000
                      || now - s.created > TimeUnit.MINUTES.toMillis(30)
                      || now - s.touched > 30000)) {
                stop(s, "会话超时或连接已断开");
              }
              if (s.terminal() && now - s.finished > 300000) sessions.remove(s.id, s);
            });
  }

  @PreDestroy
  public void close() {
    sessions.values().forEach(s -> stop(s, "服务关闭"));
    cleaner.shutdownNow();
    executor.shutdownNow();
  }

  private static final class Session {
    final String id = UUID.randomUUID().toString();
    final String owner;
    final String protocolId;
    final long created = System.currentTimeMillis();
    volatile long touched = created;
    volatile long finished;
    private long pausedSince;
    private long pausedMillis;
    volatile String state = "QUEUED";
    String result;
    String error;
    Integer line;
    List<Map<String, Object>> variables = List.of();
    final Deque<String> logs = new ArrayDeque<>();
    final DriverDebugContext context;
    FutureTask<Void> future;

    Session(String owner, String protocolId, RunRequest request) {
      this.owner = owner;
      this.protocolId = protocolId;
      context =
          new DriverDebugContext(
              request.debug() ? request.breakpoints() : List.of(),
              request.source().split("\\R", -1).length,
              (row, values) -> {
                synchronized (this) {
                  if (terminal()) return;
                  line = row;
                  variables = values;
                  pausedSince = System.currentTimeMillis();
                  state = "PAUSED";
                }
              });
    }

    boolean terminal() {
      return Set.of("SUCCEEDED", "FAILED", "CANCELLED").contains(state);
    }

    void finish(String state) {
      leavePause();
      this.state = state;
      finished = System.currentTimeMillis();
    }

    void leavePause() {
      if (pausedSince != 0) {
        pausedMillis += System.currentTimeMillis() - pausedSince;
        pausedSince = 0;
      }
    }

    synchronized long runningMillis(long now) {
      return now - created - pausedMillis - (pausedSince == 0 ? 0 : now - pausedSince);
    }

    synchronized void log(String message) {
      if (terminal()) return;
      if (logs.size() == 200) logs.removeFirst();
      logs.addLast(DriverDebugContext.truncate(message, 2000));
    }

    synchronized Map<String, Object> snapshot() {
      Map<String, Object> value = new LinkedHashMap<>();
      value.put("id", id);
      value.put("state", state);
      value.put("line", line);
      value.put("variables", variables);
      value.put("logs", List.copyOf(logs));
      value.put("result", result);
      value.put("error", error);
      value.put("elapsedMs", (finished == 0 ? System.currentTimeMillis() : finished) - created);
      return value;
    }
  }
}
