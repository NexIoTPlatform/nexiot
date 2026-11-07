package cn.universal.core.engine.runtime.linq;

import cn.universal.core.engine.runtime.function.MagicScriptLambdaFunction;

public class LinQOrder {

  private final MagicScriptLambdaFunction function;

  private final int order;

  public LinQOrder(MagicScriptLambdaFunction function, int order) {
    this.function = function;
    this.order = order;
  }

  public MagicScriptLambdaFunction getFunction() {
    return function;
  }

  public int getOrder() {
    return order;
  }
}
