package cn.universal.core.engine;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

public class MagicScriptEngineFactory implements ScriptEngineFactory {

  @Override
  public String getEngineName() {
    return "MagicScript";
  }

  @Override
  public String getEngineVersion() {
    return MagicScriptEngineFactory.class.getPackage().getImplementationVersion();
  }

  @Override
  public List<String> getExtensions() {
    return Collections.singletonList("ms");
  }

  @Override
  public List<String> getMimeTypes() {
    return Collections.singletonList("application/magic-engine");
  }

  @Override
  public List<String> getNames() {
    return Arrays.asList("MagicScript", "magic-engine");
  }

  @Override
  public String getLanguageName() {
    return "MagicScript";
  }

  @Override
  public String getLanguageVersion() {
    return Objects.toString(
        MagicScriptEngineFactory.class.getPackage().getImplementationVersion(), "unknow");
  }

  @Override
  public Object getParameter(String key) {
    if (ScriptEngine.ENGINE.equals(key)) {
      return getEngineName();
    } else if (ScriptEngine.ENGINE_VERSION.equals(key)) {
      return getEngineVersion();
    } else if (ScriptEngine.LANGUAGE_VERSION.equals(key)) {
      return getLanguageVersion();
    } else if (ScriptEngine.LANGUAGE.equals(key)) {
      return getLanguageName();
    } else if ("THREADING".equals(key)) {
      return null;
    }
    throw new IllegalArgumentException("Invalid key:" + key);
  }

  @Override
  public String getMethodCallSyntax(String obj, String m, String... args) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getOutputStatement(String toDisplay) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getProgram(String... statements) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ScriptEngine getScriptEngine() {
    return new MagicScriptEngine(this);
  }
}
