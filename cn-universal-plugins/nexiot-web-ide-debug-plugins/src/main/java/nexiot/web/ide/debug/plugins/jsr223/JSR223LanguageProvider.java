package nexiot.web.ide.debug.plugins.jsr223;

import java.util.Map;
import javax.script.Compilable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import nexiot.web.ide.debug.plugins.core.exception.MagicAPIException;

/**
 * JSR223规范支持
 *
 * @author mxd
 */
public class JSR223LanguageProvider implements LanguageProvider {

  ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

  @Override
  public boolean support(String languageName) {
    return scriptEngineManager.getEngineByName(languageName) != null;
  }

  @Override
  public Object execute(String languageName, String script, Map<String, Object> context)
      throws Exception {
    ScriptEngine scriptEngine = scriptEngineManager.getEngineByName(languageName);
    if (scriptEngine instanceof Compilable) {
      Compilable compilable = (Compilable) scriptEngine;
      try {
        return compilable.compile(script).eval(new SimpleBindings(context));
      } catch (Exception e) {
        throw new MagicAPIException(String.format("编译%s出错", languageName), e);
      }
    } else {
      return scriptEngine.eval(script, new SimpleBindings(context));
    }
  }
}
