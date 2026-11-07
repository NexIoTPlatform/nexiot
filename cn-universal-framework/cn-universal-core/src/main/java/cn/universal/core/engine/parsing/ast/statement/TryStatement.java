package cn.universal.core.engine.parsing.ast.statement;

import cn.universal.core.engine.asm.Label;
import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.exception.MagicExitException;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.VarIndex;
import cn.universal.core.engine.parsing.ast.Node;
import java.util.Collections;
import java.util.List;

public class TryStatement extends Node {

  private final VarIndex exceptionVarNode;
  private final List<Node> tryBlock;
  private final List<VariableDefine> tryResources;
  private final List<Node> catchBlock;
  private final List<Node> finallyBlock;

  public TryStatement(
      Span span,
      VarIndex exceptionVarNode,
      List<Node> tryBlock,
      List<VariableDefine> tryResources,
      List<Node> catchBlock,
      List<Node> finallyBlock) {
    super(span);
    this.exceptionVarNode = exceptionVarNode;
    this.tryBlock = tryBlock;
    this.tryResources = tryResources;
    Collections.reverse(this.tryResources);
    this.catchBlock = catchBlock;
    this.finallyBlock = finallyBlock;
    this.finallyBlock.add(
        0,
        new Node(new Span("auto close")) {
          @Override
          public void visitMethod(MagicScriptCompiler compiler) {
            tryResources.forEach(it -> it.visitMethod(compiler));
          }

          @Override
          public void compile(MagicScriptCompiler compiler) {
            tryResources.forEach(
                it ->
                    compiler
                        .load(it.getVarIndex())
                        .invoke(
                            INVOKESTATIC,
                            TryStatement.class,
                            "autoClose",
                            void.class,
                            Object.class));
          }
        });
  }

  @Override
  public void visitMethod(MagicScriptCompiler compiler) {
    tryBlock.forEach(it -> it.visitMethod(compiler));
    catchBlock.forEach(it -> it.visitMethod(compiler));
    finallyBlock.forEach(it -> it.visitMethod(compiler));
  }

  public static void autoClose(Object object) {
    if (object instanceof AutoCloseable) {
      try {
        ((AutoCloseable) object).close();
      } catch (Exception ignored) {
      }
    }
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    Label l0 = new Label();
    Label l1 = new Label();
    Label l2 = new Label();
    Label l3 = new Label();
    Label l4 = new Label();
    Label l5 = new Label();
    Label end = new Label();
    boolean hasCatch = exceptionVarNode != null;
    boolean hasFinally = !finallyBlock.isEmpty();
    if (hasFinally) { // try + catch + finally
      compiler.putFinallyBlock(finallyBlock);
    }
    compiler
        .label(l0)
        .compile(tryResources)
        .compile(tryBlock) // try
        .label(l1);
    if (hasFinally) {
      compiler.compile(finallyBlock).jump(GOTO, end); // 跳转至结束
    }
    compiler
        .label(l2) // catch MagicExitException
        .store(3)
        //				.compile(this.autoClose)
        .load3()
        .insn(ATHROW); // throw e
    if (hasCatch) {
      compiler
          .label(l3)
          .store(3)
          .pre_store(exceptionVarNode) // 保存异常变量前准备
          .load3()
          .store(exceptionVarNode)
          .compile(catchBlock); // 编译catch代码块
    }
    if (hasFinally) {
      compiler.label(l5).compile(finallyBlock).jump(GOTO, end); // 跳转至结束
      compiler.label(l4).store(3).compile(finallyBlock).load3().insn(ATHROW);
      compiler.getFinallyBlock();
    }
    compiler.label(end);
    compiler.tryCatch(l0, l1, l2, MagicExitException.class);
    if (hasCatch) {
      compiler.tryCatch(l0, l1, l3, Throwable.class);
    }
    if (hasFinally) { // try + catch + finally
      compiler.tryCatch(l0, l1, l4, null);
      compiler.tryCatch(l2, l5, l4, null);
    }
  }
}
