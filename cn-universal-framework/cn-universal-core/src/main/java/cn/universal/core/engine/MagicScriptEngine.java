package cn.universal.core.engine;

import cn.universal.core.engine.ScriptClass.ScriptAttribute;
import cn.universal.core.engine.ScriptClass.ScriptMethod;
import cn.universal.core.engine.reflection.JavaReflection;
import java.beans.Transient;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

public class MagicScriptEngine extends AbstractScriptEngine implements ScriptEngine, Compilable {

  private static final Map<String, Object> DEFAULT_IMPORTS = new ConcurrentHashMap<>();

  private static Map<String, ScriptClass> classMap = null;

  private final MagicScriptEngineFactory magicScriptEngineFactory;

  public MagicScriptEngine(MagicScriptEngineFactory magicScriptEngineFactory) {
    this.magicScriptEngineFactory = magicScriptEngineFactory;
  }

  public static void addScriptClass(Class<?> clazz) {
    if (classMap == null) {
      getScriptClassMap();
    }
    getScriptClass(clazz)
        .forEach(scriptClass -> classMap.put(scriptClass.getClassName(), scriptClass));
  }

  public static synchronized Map<String, ScriptClass> getScriptClassMap() {
    if (classMap == null) {
      classMap = new HashMap<>();
      Arrays.asList(
              String.class,
              Object.class,
              Date.class,
              Integer.class,
              Double.class,
              Float.class,
              Long.class,
              List.class,
              Short.class,
              Byte.class,
              Boolean.class,
              BigDecimal.class)
          .forEach(
              clazz ->
                  getScriptClass(clazz)
                      .forEach(
                          scriptClass -> classMap.put(scriptClass.getClassName(), scriptClass)));
    }
    return classMap;
  }

  public static List<ScriptMethod> getFunctions() {
    return JavaReflection.getFunctions().stream()
        .map(it -> new ScriptMethod(it.getExecutable()))
        .collect(Collectors.toList());
  }

  public static Map<String, ScriptClass> getExtensionScriptClass() {
    Map<Class<?>, List<Class<?>>> extensionMap = JavaReflection.getExtensionMap();
    Map<String, ScriptClass> classMap = new HashMap<>();
    for (Map.Entry<Class<?>, List<Class<?>>> entry : extensionMap.entrySet()) {
      ScriptClass clazz = classMap.get(entry.getKey().getName());
      if (clazz == null) {
        clazz = new ScriptClass();
        classMap.put(entry.getKey().getName(), clazz);
      }
      for (Class<?> extensionClass : entry.getValue()) {
        for (ScriptMethod method : getMethod(extensionClass)) {
          clazz.addMethod(method);
        }
      }
    }
    return classMap;
  }

  public static ScriptClass getScriptClassFromClass(Class<?> clazz) {
    Class<?> superClass = clazz.getSuperclass();
    ScriptClass scriptClass = new ScriptClass();
    scriptClass.setClassName(clazz.getName());
    scriptClass.setSuperClass(superClass != null ? superClass.getName() : null);
    appendMethod(clazz, scriptClass);
    return scriptClass;
  }

  public static List<ScriptClass> getScriptClass(Class<?> clazz) {
    List<ScriptClass> classList = new ArrayList<>();
    Class<?> superClass;
    do {
      superClass = clazz.getSuperclass();
      ScriptClass scriptClass = new ScriptClass();
      scriptClass.setClassName(clazz.getName());
      scriptClass.setSuperClass(superClass != null ? superClass.getName() : null);
      if (clazz.isEnum()) {
        scriptClass.setEnums(clazz.getEnumConstants());
      } else {
        appendAttributes(clazz, scriptClass);
      }
      appendMethod(clazz, scriptClass);
      classList.add(scriptClass);
      Class<?>[] interfaces = clazz.getInterfaces();
      List<String> interfaceList = new ArrayList<>();
      for (Class<?> interfaceClazz : interfaces) {
        classList.addAll(getScriptClass(interfaceClazz));
        interfaceList.add(interfaceClazz.getName());
      }
      scriptClass.setInterfaces(interfaceList);
      clazz = superClass;
    } while (superClass != null && superClass != Object.class && superClass != Class.class);
    return classList;
  }

  private static void appendMethod(Class<?> clazz, ScriptClass scriptClass) {
    getMethod(clazz)
        .forEach(
            method -> {
              scriptClass.addMethod(method);
              String methodName = method.getName();
              if (method.getParameters().isEmpty()
                  && ((methodName.startsWith("get") && methodName.length() > 3)
                      || (methodName.startsWith("is") && methodName.length() > 2))) {
                String attributeName = method.getName().substring(3);
                attributeName =
                    attributeName.substring(0, 1).toLowerCase() + attributeName.substring(1);
                if (!"class".equalsIgnoreCase(attributeName)) {
                  scriptClass.addAttribute(
                      new ScriptAttribute(method.getReturnType(), attributeName));
                }
              }
            });
  }

  public static Set<ScriptClass> getScriptClass(String className) {
    try {
      return new LinkedHashSet<>(getScriptClass(Class.forName(className)));
    } catch (ClassNotFoundException e) {
      return Collections.emptySet();
    }
  }

  private static List<ScriptMethod> getMethod(Class<?> clazz) {
    List<ScriptMethod> methods = new ArrayList<>();
    try {
      Method[] declaredMethods = clazz.getDeclaredMethods();
      for (Method declaredMethod : declaredMethods) {
        if (!Modifier.isVolatile(declaredMethod.getModifiers())) {
          if (Modifier.isPublic(declaredMethod.getModifiers())
              && declaredMethod.getAnnotation(Transient.class) == null) {
            if (Modifier.isPublic(declaredMethod.getModifiers())) {
              methods.add(new ScriptMethod(declaredMethod));
            }
          }
        }
      }
    } catch (Exception ignored) {
    }
    return methods;
  }

  private static void appendAttributes(Class<?> clazz, ScriptClass target) {
    try {
      Field[] fields = clazz.getDeclaredFields();
      for (Field field : fields) {
        if (Modifier.isPublic(field.getModifiers())
            && field.getAnnotation(Transient.class) == null) {
          target.addAttribute(new ScriptAttribute(field.getType().getName(), field.getName()));
        }
      }
    } catch (Exception ignored) {
    }
  }

  public static void addDefaultImport(String name, Object target) {
    DEFAULT_IMPORTS.put(name, target);
  }

  public static Map<String, Object> getDefaultImports() {
    return DEFAULT_IMPORTS;
  }

  public static Object execute(MagicScript magicScript, MagicScriptContext context) {
    SimpleScriptContext simpleScriptContext = new SimpleScriptContext();
    simpleScriptContext.setAttribute(MagicScript.CONTEXT_ROOT, context, ScriptContext.ENGINE_SCOPE);
    return magicScript.eval(simpleScriptContext);
  }

  @Override
  public Object eval(String script, ScriptContext context) throws ScriptException {
    return compile(script).eval(context);
  }

  @Override
  public Object eval(Reader reader, ScriptContext context) throws ScriptException {
    return compile(reader).eval(context);
  }

  @Override
  public Bindings createBindings() {
    return new SimpleBindings();
  }

  @Override
  public ScriptEngineFactory getFactory() {
    return magicScriptEngineFactory;
  }

  @Override
  public CompiledScript compile(String script) {
    return MagicScript.create(script, this);
  }

  @Override
  public CompiledScript compile(Reader script) {
    return compile(readString(script));
  }

  private String readString(Reader reader) {
    StringBuilder builder = new StringBuilder();
    char[] buf = new char[1024];
    int len;
    try {
      while ((len = reader.read(buf, 0, buf.length)) != -1) {
        builder.append(buf, 0, len);
      }
    } catch (IOException ignored) {

    }
    return builder.toString();
  }
}
