package cn.universal.admin.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.universal.core.engine.ScriptClass.ScriptMethod;
import cn.universal.core.engine.ScriptClass.ScriptMethodParameter;
import cn.universal.core.engine.reflection.JavaInvoker;
import cn.universal.core.engine.reflection.JavaReflection;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.aop.support.AopUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class MagicFunctionCatalogService {

  @Cacheable(
      cacheNames = "protocol_magic_registered_functions",
      key = "'catalog'",
      unless = "#result == null || #result.isEmpty()")
  public List<JSONObject> listFunctions() {
    Map<String, JSONObject> catalog = new LinkedHashMap<>();
    for (JavaInvoker<Method> invoker : JavaReflection.getFunctions()) {
      Object target = invoker.getDefaultTarget();
      Class<?> ownerClass =
          target == null
              ? invoker.getExecutable().getDeclaringClass()
              : AopUtils.getTargetClass(target);
      Method method = invoker.getExecutable();
      String key = ownerClass.getName() + "#" + buildMethodSignature(method);
      catalog.putIfAbsent(key, buildFunctionMeta(ownerClass, method));
    }
    return catalog.values().stream()
        .sorted(
            Comparator.comparing((JSONObject item) -> item.getStr("group", ""))
                .thenComparing(item -> item.getStr("signature", "")))
        .collect(Collectors.toList());
  }

  private JSONObject buildFunctionMeta(Class<?> ownerClass, Method method) {
    ScriptMethod scriptMethod = new ScriptMethod(method);
    List<JSONObject> params = buildMethodParams(scriptMethod);
    JSONObject item = new JSONObject();
    item.set("key", ownerClass.getName() + "#" + buildMethodSignature(method));
    item.set("group", ownerClass.getSimpleName());
    item.set("className", ownerClass.getName());
    item.set("packageName", ownerClass.getPackageName());
    item.set("name", scriptMethod.getName());
    item.set("description", StrUtil.blankToDefault(scriptMethod.getComment(), ""));
    item.set("deprecated", scriptMethod.isDeprecated());
    item.set("origin", scriptMethod.isOrigin());
    item.set("returnType", method.getReturnType().getSimpleName());
    item.set("returnTypeRaw", scriptMethod.getReturnType());
    item.set(
        "signature",
        scriptMethod.getName()
            + "("
            + params.stream()
                .map(param -> param.getStr("type") + " " + param.getStr("name"))
                .collect(Collectors.joining(", "))
            + ")");
    item.set("paramCount", scriptMethod.getParameters().size());
    item.set("varArgs", method.isVarArgs());
    item.set("params", params);
    item.set("searchText", buildSearchText(ownerClass, scriptMethod, params));
    return item;
  }

  private List<JSONObject> buildMethodParams(ScriptMethod scriptMethod) {
    List<JSONObject> params = new ArrayList<>();
    List<ScriptMethodParameter> parameters = scriptMethod.getParameters();
    for (int index = 0; index < parameters.size(); index += 1) {
      ScriptMethodParameter parameter = parameters.get(index);
      JSONObject param = new JSONObject();
      param.set("index", index);
      param.set("name", parameter.getName());
      String type = simpleTypeName(parameter.getType());
      param.set("type", parameter.isVarArgs() ? type.replace("[]", "...") : type);
      param.set("rawType", parameter.getType());
      param.set("description", StrUtil.blankToDefault(parameter.getComment(), ""));
      param.set("varArgs", parameter.isVarArgs());
      params.add(param);
    }
    return params;
  }

  private String buildMethodSignature(Method method) {
    return method.getName()
        + "("
        + java.util.Arrays.stream(method.getParameters())
            .map(parameter -> parameter.getType().getSimpleName())
            .collect(Collectors.joining(", "))
        + ")";
  }

  private String buildSearchText(
      Class<?> ownerClass, ScriptMethod scriptMethod, List<JSONObject> params) {
    List<String> tokens = new ArrayList<>();
    tokens.add(ownerClass.getSimpleName());
    tokens.add(ownerClass.getName());
    tokens.add(ownerClass.getPackageName());
    tokens.add(scriptMethod.getName());
    tokens.add(scriptMethod.getComment());
    tokens.add(scriptMethod.getReturnType());
    tokens.add(scriptMethod.getName() + "(" + params.size() + ")");
    for (JSONObject param : params) {
      tokens.add(param.getStr("name"));
      tokens.add(param.getStr("type"));
      tokens.add(param.getStr("rawType"));
      tokens.add(param.getStr("description"));
    }
    return tokens.stream().filter(StrUtil::isNotBlank).collect(Collectors.joining(" "));
  }

  private String simpleTypeName(String rawType) {
    if (StrUtil.isBlank(rawType)) {
      return "";
    }
    int index = rawType.lastIndexOf('.');
    return index >= 0 ? rawType.substring(index + 1) : rawType;
  }
}
