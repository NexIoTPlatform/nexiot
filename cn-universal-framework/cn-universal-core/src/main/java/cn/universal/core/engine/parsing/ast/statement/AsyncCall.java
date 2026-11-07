package cn.universal.core.engine.parsing.ast.statement;

import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.VarIndex;
import cn.universal.core.engine.parsing.ast.Expression;
import cn.universal.core.engine.runtime.Variables;
import cn.universal.core.engine.runtime.function.MagicScriptLambdaFunction;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** 异步调用 */
public class AsyncCall extends Expression {

  /** 默认线程池大小(CPU核心数 * 2) */
  private static ThreadPoolExecutor threadPoolExecutor =
      setThreadPoolExecutorSize(Runtime.getRuntime().availableProcessors() * 2);

  private final LambdaFunction expression;

  public AsyncCall(Span span, Expression expression) {
    super(span);
    if (expression instanceof LambdaFunction) {
      this.expression = (LambdaFunction) expression;
    } else {
      this.expression =
          new LambdaFunction(
              span,
              Collections.emptyList(),
              Collections.singletonList(new Return(span, expression)));
    }
    this.expression.setAsync(true);
  }

  public static ThreadPoolExecutor setThreadPoolExecutorSize(int size) {
    if (size > 0) {
      threadPoolExecutor =
          new ThreadPoolExecutor(
              size,
              size,
              0L,
              TimeUnit.MILLISECONDS,
              new ArrayBlockingQueue<>(size * 2),
              new AsyncThreadFactory(),
              new ThreadPoolExecutor.CallerRunsPolicy());
    }
    return threadPoolExecutor;
  }

  public static FutureTask<Object> execute(
      MagicScriptLambdaFunction function, Variables variables, Object[] args) {
    FutureTask<Object> futureTask = new FutureTask<>(() -> function.apply(variables, args));
    //	判断当前是否在线程池中，如果是的话，则直接运行，防止线程嵌套造成的"死锁"
    if (Thread.currentThread().getThreadGroup() == AsyncThreadFactory.ASYNC_THREAD_GROUP) {
      futureTask.run();
    } else {
      threadPoolExecutor.submit(futureTask);
    }
    return futureTask;
  }

  @Override
  public void visitMethod(MagicScriptCompiler compiler) {
    expression.visitMethod(compiler);
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    List<VarIndex> parameters = expression.getParameters();
    compiler.compile(expression).load2();
    parameters.forEach(compiler::load);
    compiler.call("call_async", parameters.size() + 2);
  }

  static class AsyncThreadFactory implements ThreadFactory {

    private static final ThreadGroup ASYNC_THREAD_GROUP = new ThreadGroup("magic-async-group");
    private final AtomicLong threadNumber = new AtomicLong(1);
    private final String namePrefix = "magic-async-";

    @Override
    public Thread newThread(Runnable r) {
      Thread t = new Thread(ASYNC_THREAD_GROUP, r, namePrefix + threadNumber.getAndIncrement(), 0);
      if (t.isDaemon()) {
        t.setDaemon(false);
      }
      if (t.getPriority() != Thread.NORM_PRIORITY) {
        t.setPriority(Thread.NORM_PRIORITY);
      }
      return t;
    }
  }
}
