package cn.universal.core.engine.runtime.function;

import cn.universal.core.engine.runtime.Variables;

@FunctionalInterface
public interface MagicScriptLambdaFunction {

  Object apply(Variables variables, Object[] args);
}
