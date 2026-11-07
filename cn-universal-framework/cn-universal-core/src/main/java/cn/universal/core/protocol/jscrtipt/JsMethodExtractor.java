package cn.universal.core.protocol.jscrtipt;

import cn.hutool.core.util.StrUtil;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;

/** 提取 JS 脚本中的方法名 */
public class JsMethodExtractor {

  /**
   * 构建安全沙箱 Context：
   * - 禁止读取环境变量
   * - 禁止文件 I/O
   * - 禁止加载任何宿主（Java）类
   * - 禁止创建子进程和线程
   * - 禁止 native 访问
   */
  static Context buildSandboxContext() {
    HostAccess hostAccess =
        HostAccess.newBuilder()
            .allowPublicAccess(false) // 禁止访问所有 public Java 成员
            .allowArrayAccess(false)
            .allowListAccess(false)
            .allowMapAccess(false)
            .allowBufferAccess(false)
            .allowIterableAccess(false)
            .allowIteratorAccess(false)
            .build();

    return Context.newBuilder("js")
        // ── 环境变量：完全隔离 ──────────────────────────
        .allowEnvironmentAccess(EnvironmentAccess.NONE)
        // ── 文件系统：禁止 ─────────────────────────────
        .allowIO(IOAccess.NONE)
        // ── Java 宿主类：全部拒绝 ───────────────────────
        .allowHostClassLookup(className -> false)
        .allowHostAccess(hostAccess)
        // ── 进程 / 线程 / Native：禁止 ─────────────────
        .allowCreateProcess(false)
        .allowCreateThread(false)
        .allowNativeAccess(false)
        // ── 实验性选项：禁用 eval() 和 load() ──────────
        .allowExperimentalOptions(true)
        .option("js.disable-eval", "true")
        // ── 屏蔽标准流，防止信息通过 console 泄露 ───────
        .out(OutputStream.nullOutputStream())
        .err(OutputStream.nullOutputStream())
        .in(InputStream.nullInputStream())
        .build();
  }

  /**
   * 从 JS 脚本内容中提取所有方法名
   *
   * @param scriptContent JS 脚本字符串
   * @return 方法名集合（仅包含函数类型的标识符）
   */
  public static Set<String> extractMethodNames(String scriptContent) {
    if (StrUtil.isEmpty(scriptContent)) {
      return new HashSet<>();
    }
    Set<String> methodNames = new HashSet<>();
    // 使用沙箱 Context 执行脚本并获取全局对象
    try (Context context = buildSandboxContext()) {
      // 执行脚本（加载所有定义）
      context.eval("js", scriptContent);
      // 获取 JS 全局对象（包含所有顶级定义）
      Value global = context.getBindings("js");
      // 遍历全局对象的所有成员
      for (String key : global.getMemberKeys()) {
        Value member = global.getMember(key);
        // 判断是否为可执行函数（排除变量、常量等非函数类型）
        if (isFunction(member)) {
          methodNames.add(key);
        }
      }
    }
    return methodNames;
  }

  /** 判断 GraalVM Value 是否为 JS 函数 */
  private static boolean isFunction(Value value) {
    // 函数必须满足：可执行 + 不是 null + 不是 undefined + 类型名称为 "function"
    return value.canExecute()
        && !value.isNull()
        && "function".equalsIgnoreCase(value.getMetaObject().getMetaSimpleName());
  }
}
