package cn.universal.core.protocol.magic;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.universal.core.engine.MagicScript;
import cn.universal.core.engine.runtime.MagicScriptRuntime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/** 基于 magic-script 提取JS脚本中的方法名工具类 */
@Slf4j
public class MagicScriptMethodExtractor {

  private static final Pattern LINE_SPLIT_PATTERN = Pattern.compile("\\R");

  /**
   * 从magic-script脚本中提取所有变量/方法名（兼容原逻辑）
   *
   * @param scriptContent 脚本内容字符串
   * @return 脚本中定义的所有变量/方法名集合
   * @throws Exception 脚本编译失败时抛出异常
   */
  public static Set<String> extractVarNames(String scriptContent) {
    MagicScriptRuntime compiled = compile(scriptContent);
    String[] varNames = compiled.getVarNames();
    return varNames != null ? new HashSet<>(Arrays.asList(varNames)) : new HashSet<>();
  }

  /**
   * 从脚本中提取与目标方法集合匹配的支持方法
   *
   * @param scriptContent 脚本内容
   * @param targetMethods 目标方法集合（如{decode, encode}）
   * @return 脚本中存在的目标方法交集
   * @throws Exception 脚本编译失败时抛出异常
   */
  public static Set<String> extractSupportMethods(String scriptContent, Set<String> targetMethods) {
    if (targetMethods == null || targetMethods.isEmpty()) {
      return new HashSet<>();
    }
    Set<String> allVarNames = extractVarNames(scriptContent);
    Set<String> supportMethods = new HashSet<>();
    for (String varName : allVarNames) {
      if (targetMethods.contains(varName)) {
        supportMethods.add(varName);
      }
    }
    if (!supportMethods.isEmpty()) {
      return supportMethods;
    }

    Set<String> sourceMethods = extractSupportMethodsBySource(scriptContent, targetMethods);
    if (!sourceMethods.isEmpty()) {
      log.info("MagicScriptMethodExtractor fallback to source scan, methods={}", sourceMethods);
      supportMethods.addAll(sourceMethods);
    }
    return supportMethods;
  }

  private static MagicScriptRuntime compile(String scriptContent) {
    try {
      MagicScript script = MagicScript.create(scriptContent, null);
      return script.compile();
    } catch (Exception e) {
      String message = ExceptionUtil.getRootCauseMessage(e);
      log.warn("MagicScriptMethodExtractor.compile error: {}", message, e);
      throw new IllegalStateException("MagicScript预编译失败: " + message, e);
    }
  }

  private static Set<String> extractSupportMethodsBySource(
      String scriptContent, Set<String> targetMethods) {
    Set<String> supportMethods = new HashSet<>();
    if (scriptContent == null || scriptContent.isEmpty()) {
      return supportMethods;
    }
    for (String methodName : targetMethods) {
      if (containsMethodDefinition(scriptContent, methodName)) {
        supportMethods.add(methodName);
      }
    }
    return supportMethods;
  }

  private static boolean containsMethodDefinition(String scriptContent, String methodName) {
    Pattern declarationPattern =
        Pattern.compile(
            "^\\s*(?:var|let|const)\\s+"
                + Pattern.quote(methodName)
                + "\\s*=");
    Pattern functionPattern =
        Pattern.compile("^\\s*function\\s+" + Pattern.quote(methodName) + "\\s*\\(");
    Pattern assignmentPattern = Pattern.compile("^\\s*" + Pattern.quote(methodName) + "\\s*=");

    for (String line : LINE_SPLIT_PATTERN.split(scriptContent)) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()
          || trimmed.startsWith("//")
          || trimmed.startsWith("/*")
          || trimmed.startsWith("*")) {
        continue;
      }
      if (declarationPattern.matcher(line).find()
          || functionPattern.matcher(line).find()
          || assignmentPattern.matcher(line).find()) {
        return true;
      }
    }
    return false;
  }
}
