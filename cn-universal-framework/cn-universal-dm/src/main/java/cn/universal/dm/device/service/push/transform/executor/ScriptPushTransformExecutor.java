package cn.universal.dm.device.service.push.transform.executor;

import cn.hutool.core.util.StrUtil;
import cn.universal.core.engine.MagicScript;
import cn.universal.core.engine.MagicScriptContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ScriptPushTransformExecutor {

  private static final Pattern FUNCTION_DECLARATION_PATTERN =
      Pattern.compile("(?s)^\\s*function\\s+transform\\s*\\((.*?)\\)\\s*\\{");

  private static final Pattern FUNCTION_EXPRESSION_PATTERN =
      Pattern.compile("(?s)^\\s*var\\s+transform\\s*=\\s*function\\s*\\((.*?)\\)\\s*\\{");

  private final Map<String, MagicScript> scriptCache = new ConcurrentHashMap<>();

  public Object execute(
      Long ruleId, Integer version, String scriptContent, Map<String, Object> scriptBindings) {
    MagicScriptContext context = new MagicScriptContext(scriptBindings);
    MagicScript magicScript = getOrCreateScript(ruleId, version, scriptContent);
    return magicScript.execute(context);
  }

  public void validate(String scriptContent) {
    if (StrUtil.isBlank(scriptContent)) {
      throw new IllegalArgumentException("脚本内容不能为空");
    }
    MagicScript magicScript = MagicScript.create(wrapScript(htmlDecode(scriptContent)), null);
    magicScript.compile();
  }

  private MagicScript getOrCreateScript(Long ruleId, Integer version, String scriptContent) {
    if (StrUtil.isBlank(scriptContent)) {
      throw new IllegalArgumentException("脚本内容不能为空");
    }
    String decodedScript = htmlDecode(scriptContent);
    String normalizedScript = wrapScript(decodedScript);
    String cacheKey =
        (ruleId == null ? 0L : ruleId)
            + ":"
            + (version == null ? 1 : version)
            + ":"
            + normalizedScript.hashCode();
    return scriptCache.computeIfAbsent(
        cacheKey,
        key -> {
          MagicScript magicScript = MagicScript.create(normalizedScript, null);
          magicScript.compile();
          log.debug("[数据转换] MagicScript缓存创建成功: {}", cacheKey);
          return magicScript;
        });
  }

  private String wrapScript(String scriptContent) {
    String normalizedScript = normalizeTransformDefinition(scriptContent);
    String trimmedContent = normalizedScript.trim();
    boolean functionDefined = trimmedContent.matches("(?s).*(var|function)\\s+transform\\s*[=(].*");
    boolean hasFunctionInvocation = trimmedContent.matches("(?s).*\\breturn\\s+transform\\s*\\(.*");
    if (functionDefined && !hasFunctionInvocation) {
      return normalizedScript + "\nreturn transform(ctx, request, source);";
    }
    return normalizedScript;
  }

  private String normalizeTransformDefinition(String scriptContent) {
    if (StrUtil.isBlank(scriptContent)) {
      return scriptContent;
    }
    if (FUNCTION_DECLARATION_PATTERN.matcher(scriptContent).find()) {
      return FUNCTION_DECLARATION_PATTERN
          .matcher(scriptContent)
          .replaceFirst("var transform = ($1) => {");
    }
    if (FUNCTION_EXPRESSION_PATTERN.matcher(scriptContent).find()) {
      return FUNCTION_EXPRESSION_PATTERN
          .matcher(scriptContent)
          .replaceFirst("var transform = ($1) => {");
    }
    return scriptContent;
  }

  private String htmlDecode(String text) {
    if (text == null) {
      return null;
    }
    String current = text;
    for (int index = 0; index < 2; index++) {
      String previous = current;
      current =
          current
              .replace("&lt;", "<")
              .replace("&gt;", ">")
              .replace("&quot;", "\"")
              .replace("&#39;", "'")
              .replace("&amp;", "&");
      if (previous.equals(current)) {
        break;
      }
    }
    return current;
  }
}
