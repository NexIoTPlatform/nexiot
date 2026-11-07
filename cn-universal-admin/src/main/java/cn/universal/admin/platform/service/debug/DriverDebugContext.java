package cn.universal.admin.platform.service.debug;

import cn.universal.core.engine.MagicScriptDebugContext;
import cn.universal.core.engine.runtime.Variables;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/** 有界、可取消的断点等待，不使用旧 IDE 的阻塞握手协议。 */
final class DriverDebugContext extends MagicScriptDebugContext {
  private final Semaphore resume = new Semaphore(0);
  private final BiConsumer<Integer, List<Map<String, Object>>> onPause;
  private long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(2);
  private volatile Set<Integer> lines;
  private volatile boolean step;
  private volatile boolean cancelled;
  private final int sourceLines;

  DriverDebugContext(
      List<Integer> lines,
      int sourceLines,
      BiConsumer<Integer, List<Map<String, Object>>> onPause) {
    super(List.of());
    this.lines = Set.copyOf(lines);
    this.sourceLines = sourceLines;
    this.onPause = onPause;
  }

  @Override
  public void pause(int row, int col, int endRow, int endCol, Variables variables)
      throws InterruptedException {
    checkCancelled();
    if (row > sourceLines || (!step && !lines.contains(row))) return;
    Map<String, Object> values = new LinkedHashMap<>(getRootVariables());
    values.putAll(variables.getVariables(this));
    long pausedAt = System.nanoTime();
    onPause.accept(row, DriverVariableInspector.inspect(values));
    // 阅读变量的时间不消耗脚本执行预算；会话总时限与连接租期由服务统一管理。
    if (!resume.tryAcquire(30, TimeUnit.MINUTES)) {
      throw new CancellationException("调试会话超时，请重新运行");
    }
    deadline += System.nanoTime() - pausedAt;
    checkCancelled();
  }

  void proceed(boolean step, List<Integer> lines) {
    this.step = step;
    updateBreakpoints(lines);
    resume.release();
  }

  void updateBreakpoints(List<Integer> lines) {
    this.lines = Set.copyOf(lines);
  }

  void cancel() {
    cancelled = true;
    resume.release();
  }

  @Override
  public void checkDebugCancellation() {
    checkCancelled();
  }

  private void checkCancelled() {
    if (cancelled || Thread.currentThread().isInterrupted() || System.nanoTime() > deadline) {
      throw new CancellationException("调试已停止或超时");
    }
  }

  static String truncate(String text, int limit) {
    return text.length() > limit ? text.substring(0, limit) + "\n…（内容已截断）" : text;
  }
}
