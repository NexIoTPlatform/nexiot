package cn.universal.core.engine;

import cn.universal.core.engine.exception.MagicScriptException;
import cn.universal.core.engine.runtime.MagicScriptRuntime;
import cn.universal.core.engine.runtime.RuntimeContext;
import cn.universal.core.engine.runtime.Variables;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 脚本环境上下文 编译后的类每个方法的第一个参数都是本类。 此类主要用于辅助读写变量以及设置/读取/ */
public class MagicScriptContext {

  /** 保存手动设置的环境变量 */
  private final Map<String, Object> rootVariables = new LinkedHashMap<>();

  /** 代码执行时，存放 import "xx.xx.xx.*" 的包 */
  private final List<String> importPackages = new ArrayList<>();

  private MagicScriptRuntime runtime;

  private Variables variables;

  private String scriptName;

  public MagicScriptContext() {}

  public MagicScriptContext(Map<String, Object> variables) {
    putMapIntoContext(variables);
  }

  public String getScriptName() {
    return scriptName;
  }

  public void setScriptName(String scriptName) {
    this.scriptName = scriptName;
  }

  /**
   * 获取当前作用域内的String变量值
   *
   * @param name 变量名称
   * @return 变量值
   */
  public String getString(String name) {
    return Objects.toString(get(name), null);
  }

  /**
   * 添加 .* 的导包
   *
   * @param packageName 包名 如 java.text.
   */
  public void addImport(String packageName) {
    importPackages.add(packageName);
  }

  public Class<?> getImportClass(String simpleClassName) {
    for (int i = importPackages.size() - 1; i >= 0; i--) {
      try {
        return Class.forName(importPackages.get(i) + simpleClassName);
      } catch (ClassNotFoundException ignored) {
      }
    }
    return null;
  }

  /**
   * 获取当前作用域内的变量值
   *
   * @param name 变量名称
   * @return 变量值
   */
  public Object get(String name) {
    return rootVariables.get(name);
  }

  /**
   * 设置环境变量
   *
   * @param name 变量名
   * @param value 变量值
   */
  public MagicScriptContext set(String name, Object value) {
    rootVariables.put(name, value);
    return this;
  }

  /**
   * 创建变量
   *
   * @param runtime 脚本实例
   * @param size 数组大小（变量个数）
   */
  public Variables createVariables(MagicScriptRuntime runtime, int size) {
    this.runtime = runtime;
    return this.variables = new Variables(size);
  }

  public Variables getVariables() {
    return variables;
  }

  /**
   * 从当前上下文中动态执行脚本
   *
   * @param runtimeContext
   * @param script 脚本内容
   */
  public Object eval(RuntimeContext runtimeContext, String script) {
    Map<String, Object> varMap =
        new LinkedHashMap<>(runtimeContext.getScriptContext().getRootVariables());
    varMap.putAll(runtimeContext.getVariables().getVariables(runtimeContext.getScriptContext()));
    return eval(script, varMap);
  }

  /**
   * 从当前上下文中动态执行脚本
   *
   * @param script 脚本内容
   * @param varMap 变量信息
   */
  public Object eval(String script, Map<String, Object> varMap) {
    try {
      MagicScript magicScript = MagicScript.create(true, script, null);
      MagicScriptRuntime runtime = magicScript.compile();
      MagicScriptContext context = new MagicScriptContext(varMap);
      context.setScriptName(this.getScriptName());
      return runtime.execute(context);
    } catch (Exception e) {
      Throwable throwable = MagicScriptError.unwrap(e);
      if (throwable instanceof MagicScriptException) {
        throw new RuntimeException(((MagicScriptException) throwable).getSimpleMessage());
      }
      throw new RuntimeException(throwable);
    }
  }

  public String[] getVarNames() {
    return runtime.getVarNames();
  }

  /** 获取调用时传入的变量信息 */
  public Map<String, Object> getRootVariables() {
    return rootVariables;
  }

  /** 批量设置环境变量 */
  public void putMapIntoContext(Map<String, Object> map) {
    if (map != null && !map.isEmpty()) {
      rootVariables.putAll(map);
    }
  }

  /**
   * 从环境中获取值，此方法给编译后的类专用。
   *
   * @param name 变量名
   */
  public Object getEnvironmentValue(String name) {
    Object value = get(name);
    value = value == null ? getImportClass(name) : value;
    return value == null ? MagicResourceLoader.findClass(name) : value;
  }

  /** 调试循环的协作式取消检查，正常执行无需处理。 */
  public void checkDebugCancellation() {}

  public void pause(int startRow, int startCol, int endRow, int endCol, Variables variables)
      throws InterruptedException {}
}
