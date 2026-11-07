package cn.universal.core.engine;

import cn.universal.core.engine.exception.MagicExitException;
import cn.universal.core.engine.exception.MagicScriptException;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.TokenStream;
import cn.universal.core.engine.runtime.MagicScriptRuntime;
import java.util.ArrayList;
import java.util.List;

/** All errors reported by the library go through the static functions of this class. */
public class MagicScriptError {

  /**
   * Create an error message based on the provided message and stream, highlighting the line on
   * which the error happened. If the stream has more tokens, the next token will be highlighted.
   * Otherwise the end of the source of the stream will be highlighted.
   *
   * <p>Throws a {@link RuntimeException}
   */
  public static void error(String message, TokenStream stream) {
    if (stream.hasMore()) {
      error(message, stream.consume().getSpan());
    } else {
      error(message, stream.getPrev().getSpan());
    }
  }

  /**
   * Create an error message based on the provided message and location, highlighting the location
   * in the line on which the error happened. Throws a {@link MagicScriptException}
   */
  public static void error(String message, Span location, Throwable cause) {
    cause = unwrap(cause);
    if (cause instanceof MagicExitException) {
      throw (MagicExitException) cause;
    }
    if (cause instanceof MagicScriptException) {
      MagicScriptException mse = ((MagicScriptException) cause);
      if (mse.getLocation() == null) {
        error(message, location, cause.getCause());
        return;
      }
      throw mse;
    }
    String errorMessage = message;
    if (location != null) {
      Span.Line line = location.getLine();
      errorMessage += " at Row:";
      errorMessage += line.getLineNumber() + "~" + line.getEndLineNumber() + ",Col:";
      errorMessage += line.getStartCol() + "~" + line.getEndCol() + "\n\n";
      errorMessage += line.getText();
      errorMessage += "\n";
      int errorStart = location.getStart() - line.getStart();
      int errorEnd = errorStart + location.getText().length() - 1;
      for (int i = 0, n = line.getText().length(); i < n; i++) {
        boolean useTab = line.getText().charAt(i) == '\t';
        errorMessage += i >= errorStart && i <= errorEnd ? "^" : useTab ? "\t" : " ";
      }
    }
    if (cause == null) {
      throw new MagicScriptException(errorMessage, message, location);
    } else {
      throw new MagicScriptException(errorMessage, message, cause, location);
    }
  }

  /**
   * Create an error message based on the provided message and location, highlighting the location
   * in the line on which the error happened. Throws a {@link MagicScriptException}
   */
  public static void error(String message, Span location) {
    error(message, location, null);
  }

  public static Throwable unwrap(Throwable root) {
    Throwable parent = root;
    while (parent != null) {
      if (parent instanceof MagicScriptException) {
        root = parent;
      }
      parent = parent.getCause();
    }
    return root;
  }

  public static void transfer(MagicScriptRuntime runtime, Throwable t) {
    StackTraceElement[] elements = t.getStackTrace();
    Throwable cause = t;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    Span span = null;
    if (runtime != null) {
      List<StackTraceElement> elementList = new ArrayList<>();
      String className = runtime.getClass().getName();
      for (StackTraceElement element : elements) {
        if (element.getLineNumber() > -1 && element.getClassName().equals(className)) {
          Span currentSpan = runtime.getSpan(element.getLineNumber());
          elementList.add(
              new StackTraceElement(
                  element.getClassName(),
                  element.getMethodName(),
                  element.getFileName(),
                  currentSpan.getLine().getLineNumber()));
          if (span == null) {
            span = currentSpan;
          }
        } else {
          elementList.add(element);
        }
      }
      cause.setStackTrace(elementList.toArray(new StackTraceElement[0]));
    }
    MagicScriptError.error(t.getMessage(), span, cause);
  }
}
