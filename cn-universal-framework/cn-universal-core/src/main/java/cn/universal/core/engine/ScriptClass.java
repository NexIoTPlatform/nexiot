package cn.universal.core.engine;

import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.runtime.RuntimeContext;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ScriptClass {

  private String className;

  private Set<ScriptMethod> methods = new HashSet<>();

  private Set<ScriptAttribute> attributes = new LinkedHashSet<>();

  private String superClass;

  private List<String> interfaces = new ArrayList<>();

  private Object[] enums;

  private boolean module;

  public Object[] getEnums() {
    return enums;
  }

  public void setEnums(Object[] enums) {
    this.enums = enums;
  }

  public void addAttribute(ScriptAttribute attribute) {
    this.attributes.add(attribute);
  }

  public void addMethod(ScriptMethod method) {
    this.methods.add(method);
  }

  public void addInterface(String interfaceName) {
    this.interfaces.add(interfaceName);
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public Set<ScriptMethod> getMethods() {
    return methods;
  }

  public void setMethods(Set<ScriptMethod> methods) {
    this.methods = methods;
  }

  public String getSuperClass() {
    return superClass;
  }

  public void setSuperClass(String superClass) {
    this.superClass = superClass;
  }

  public List<String> getInterfaces() {
    return interfaces;
  }

  public void setInterfaces(List<String> interfaces) {
    this.interfaces = interfaces;
  }

  public Set<ScriptAttribute> getAttributes() {
    return attributes;
  }

  public void setAttributes(Set<ScriptAttribute> attributes) {
    this.attributes = attributes;
  }

  public boolean isModule() {
    return module;
  }

  public void setModule(boolean module) {
    this.module = module;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ScriptClass)) {
      return false;
    }
    ScriptClass that = (ScriptClass) o;
    return Objects.equals(className, that.className);
  }

  @Override
  public int hashCode() {
    return Objects.hash(className);
  }

  public static class ScriptAttribute {

    private final String type;

    private final String name;

    public ScriptAttribute(String type, String name) {
      this.type = type;
      this.name = name;
    }

    public String getType() {
      return type;
    }

    public String getName() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ScriptAttribute)) {
        return false;
      }
      ScriptAttribute that = (ScriptAttribute) o;
      return Objects.equals(type, that.type) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, name);
    }
  }

  public static class ScriptMethod {

    private final String name;

    private final String returnType;

    private final String comment;

    private final boolean origin;

    private final boolean deprecated;

    private final List<ScriptMethodParameter> parameters = new ArrayList<>();

    public ScriptMethod(Method method) {
      this.name = method.getName();
      Comment methodComment = method.getAnnotation(Comment.class);
      comment = methodComment == null ? null : methodComment.value();
      deprecated = method.getAnnotation(Deprecated.class) != null;
      origin = methodComment != null && methodComment.origin();
      Class<?> returnType = method.getReturnType();
      this.returnType = returnType.isArray() ? returnType.getSimpleName() : returnType.getName();
      Parameter[] parameters = method.getParameters();
      if (parameters != null) {
        for (Parameter parameter : parameters) {
          if (!RuntimeContext.class.isAssignableFrom(parameter.getType())) {
            this.parameters.add(new ScriptMethodParameter(parameter));
          }
        }
      }
    }

    public String getComment() {
      return comment;
    }

    public String getName() {
      return name;
    }

    public boolean isOrigin() {
      return origin;
    }

    public String getReturnType() {
      return returnType;
    }

    public List<ScriptMethodParameter> getParameters() {
      return parameters;
    }

    public boolean isDeprecated() {
      return deprecated;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ScriptMethod that = (ScriptMethod) o;
      return Objects.equals(name, that.name) && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, parameters);
    }
  }

  public static class ScriptMethodParameter {

    private String name;

    private final String type;

    private final boolean varArgs;

    private String comment;

    public ScriptMethodParameter(Parameter parameter) {
      Class<?> type = parameter.getType();
      this.varArgs = parameter.isVarArgs();
      Comment parameterComment = parameter.getAnnotation(Comment.class);
      this.name = parameter.getName();
      if (parameterComment != null) {
        this.comment = parameterComment.value();
        if (!parameterComment.name().isEmpty()) {
          this.name = parameterComment.name();
        }
      }
      this.type = type.isArray() ? type.getSimpleName() : type.getName();
    }

    public String getName() {
      return name;
    }

    public String getType() {
      return type;
    }

    public boolean isVarArgs() {
      return varArgs;
    }

    public String getComment() {
      return comment;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ScriptMethodParameter that = (ScriptMethodParameter) o;
      return Objects.equals(name, that.name) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, type);
    }
  }
}
