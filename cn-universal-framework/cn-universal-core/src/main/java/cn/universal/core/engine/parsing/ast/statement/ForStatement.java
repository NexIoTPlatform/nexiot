package cn.universal.core.engine.parsing.ast.statement;

import cn.universal.core.engine.asm.Label;
import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.VarIndex;
import cn.universal.core.engine.parsing.ast.Expression;
import cn.universal.core.engine.parsing.ast.Node;
import cn.universal.core.engine.runtime.handle.FunctionCallHandle;
import cn.universal.core.engine.runtime.lang.KeyIterator;
import java.util.Iterator;
import java.util.List;

public class ForStatement extends Node {

  private final VarIndex indexOrKey;
  private final VarIndex value;
  private final VarIndex anonymousVariable;
  private final Expression mapOrArray;
  private final List<Node> body;

  public ForStatement(
      Span span,
      VarIndex indexOrKey,
      VarIndex value,
      VarIndex anonymousVariable,
      Expression mapOrArray,
      List<Node> body) {
    super(span);
    this.indexOrKey = indexOrKey;
    this.anonymousVariable = anonymousVariable;
    this.value = value;
    this.mapOrArray = mapOrArray;
    this.body = body;
  }

  @Override
  public void visitMethod(MagicScriptCompiler compiler) {
    mapOrArray.visitMethod(compiler);
    body.forEach(it -> it.visitMethod(compiler));
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    Label start = new Label();
    Label end = new Label();
    compiler
        .markLabel(start, end) // 标记 continue 、 break位置
        .pre_store(anonymousVariable)
        //	初始化 iterator
        .compile(mapOrArray)
        .invoke(
            INVOKESTATIC,
            FunctionCallHandle.class,
            indexOrKey == null ? "newValueIterator" : "newKeyValueIterator",
            Iterator.class,
            Object.class)
        // 保存至临时变量
        .store(anonymousVariable)
        .label(start).debugLoopCheckpoint()
        // 判断是否有值
        .load(anonymousVariable)
        .invoke(INVOKEINTERFACE, Iterator.class, "hasNext", true, boolean.class)
        // 值为false时，跳出循环
        .jump(IFEQ, end)
        .pre_store(value)
        // 获取当前 value
        .load(anonymousVariable)
        .invoke(INVOKEINTERFACE, Iterator.class, "next", true, Object.class)
        // 存入到 value 中
        .store(value);
    if (indexOrKey != null) {
      // 获取当前 key
      compiler
          .pre_store(indexOrKey)
          .load(anonymousVariable)
          .invoke(INVOKEINTERFACE, KeyIterator.class, "getKey", true, Object.class)
          // 存入到 key 中
          .store(indexOrKey);
    }
    compiler
        .compile(body) // 执行循环体
        // 执行完毕后跳转到循环起始位置
        .jump(GOTO, start)
        .label(end)
        // 移除 key 变量
        .remove(indexOrKey)
        // 移除 value 变量
        .remove(value)
        .exitLabel();
  }
}
