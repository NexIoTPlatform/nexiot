package cn.universal.core.engine.parsing.ast;

import cn.universal.core.engine.parsing.Span;

/** 表达式 */
public abstract class Expression extends Node {

  public Expression(Span span) {
    super(span);
  }
}
