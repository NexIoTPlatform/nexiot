package cn.universal.core.engine.runtime.handle;

import static java.lang.invoke.MethodHandles.catchException;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class BitHandle {

  private static final MethodHandle FALLBACK;

  private static final Map<String, String> OPERATORS = new HashMap<>(5);

  static {
    try {
      OPERATORS.put("and", "&");
      OPERATORS.put("or", "|");
      OPERATORS.put("left_shift", "<<");
      OPERATORS.put("right_shift2", ">>>");
      OPERATORS.put("right_shift", ">>");
      OPERATORS.put("xor", "^");
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      FALLBACK =
          lookup.findStatic(
              BitHandle.class,
              "fallback",
              methodType(Object.class, MethodCallSite.class, Object[].class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new Error("BitHandle初始化失败", e);
    }
  }

  public static CallSite bootstrap(
      MethodHandles.Lookup caller, String name, MethodType type, int flags) {
    MethodCallSite callSite = new MethodCallSite(caller, name, type, BitHandle.class);
    MethodHandle fallback =
        FALLBACK.bindTo(callSite).asCollector(Object[].class, type.parameterCount()).asType(type);
    callSite.setTarget(fallback);
    callSite.fallback = fallback;
    return callSite;
  }

  public static Object fallback(MethodCallSite callSite, Object[] args) throws Throwable {
    Class<?> arg0Class = args[0] == null ? Object.class : args[0].getClass();
    Class<?> arg1Class = args[1] == null ? Object.class : args[1].getClass();
    MethodHandle target;
    try {
      target = callSite.findStatic(methodType(Object.class, arg0Class, arg1Class));
    } catch (Throwable ignored) {
      try {
        target =
            callSite.findStatic(
                callSite.methodName + "_fallback",
                methodType(Object.class, Object.class, Object.class));
      } catch (Throwable t) {
        return reject(
            args[0], args[1], OPERATORS.getOrDefault(callSite.methodName, callSite.methodName));
      }
    }
    target = target.asType(methodType(Object.class, Object.class, Object.class));
    target =
        catchException(
            target,
            ClassCastException.class,
            dropArguments(callSite.fallback, 0, ClassCastException.class));
    callSite.setTarget(target);
    return target.invokeWithArguments(args);
  }

  /* byte << 操作实现 */
  public static Object left_shift(Byte a, Byte b) {
    return a << b;
  }

  public static Object left_shift(Byte a, Short b) {
    return a << b;
  }

  public static Object left_shift(Byte a, Integer b) {
    return a << b;
  }

  public static Object left_shift(Byte a, Long b) {
    return a << b;
  }

  public static Object left_shift(Byte a, BigDecimal b) {
    return a << b.longValue();
  }

  /* byte << 操作实现 */

  /* short << 操作实现 */
  public static Object left_shift(Short a, Byte b) {
    return a << b;
  }

  public static Object left_shift(Short a, Short b) {
    return a << b;
  }

  public static Object left_shift(Short a, Integer b) {
    return a << b;
  }

  public static Object left_shift(Short a, Long b) {
    return a << b;
  }

  public static Object left_shift(Short a, BigDecimal b) {
    return a << b.longValue();
  }

  /* short << 操作实现 */

  /* int << 操作实现 */
  public static Object left_shift(Integer a, Byte b) {
    return a << b;
  }

  public static Object left_shift(Integer a, Short b) {
    return a << b;
  }

  public static Object left_shift(Integer a, Integer b) {
    return a << b;
  }

  public static Object left_shift(Integer a, Long b) {
    return a << b;
  }

  public static Object left_shift(Integer a, BigDecimal b) {
    return a << b.longValue();
  }

  /* int << 操作实现 */

  /* long < 操作实现 */
  public static Object left_shift(Long a, Byte b) {
    return a << b;
  }

  public static Object left_shift(Long a, Short b) {
    return a << b;
  }

  public static Object left_shift(Long a, Integer b) {
    return a << b;
  }

  public static Object left_shift(Long a, Long b) {
    return a << b;
  }

  public static Object left_shift(Long a, BigDecimal b) {
    return a << b.longValue();
  }

  /* long < 操作实现 */

  /* bigecimal < 操作实现 */
  public static Object left_shift(BigDecimal a, Byte b) {
    return a.longValue() << b;
  }

  public static Object left_shift(BigDecimal a, Short b) {
    return a.longValue() << b;
  }

  public static Object left_shift(BigDecimal a, Integer b) {
    return a.longValue() << b;
  }

  public static Object left_shift(BigDecimal a, Long b) {
    return a.longValue() << b;
  }

  public static Object left_shift(BigDecimal a, BigDecimal b) {
    return a.longValue() << b.longValue();
  }

  /* bigdecimal < 操作实现 */

  /* byte >> 操作实现 */
  public static Object right_shift(Byte a, Byte b) {
    return a >> b;
  }

  public static Object right_shift(Byte a, Short b) {
    return a >> b;
  }

  public static Object right_shift(Byte a, Integer b) {
    return a >> b;
  }

  public static Object right_shift(Byte a, Long b) {
    return a >> b;
  }

  public static Object right_shift(Byte a, BigDecimal b) {
    return a >> b.longValue();
  }

  /* byte >> 操作实现 */

  /* short >> 操作实现 */
  public static Object right_shift(Short a, Byte b) {
    return a >> b;
  }

  public static Object right_shift(Short a, Short b) {
    return a >> b;
  }

  public static Object right_shift(Short a, Integer b) {
    return a >> b;
  }

  public static Object right_shift(Short a, Long b) {
    return a >> b;
  }

  public static Object right_shift(Short a, BigDecimal b) {
    return a >> b.longValue();
  }

  /* short >> 操作实现 */

  /* int >> 操作实现 */
  public static Object right_shift(Integer a, Byte b) {
    return a >> b;
  }

  public static Object right_shift(Integer a, Short b) {
    return a >> b;
  }

  public static Object right_shift(Integer a, Integer b) {
    return a >> b;
  }

  public static Object right_shift(Integer a, Long b) {
    return a >> b;
  }

  public static Object right_shift(Integer a, BigDecimal b) {
    return a >> b.longValue();
  }

  /* int >> 操作实现 */

  /* long >> 操作实现 */
  public static Object right_shift(Long a, Byte b) {
    return a >> b;
  }

  public static Object right_shift(Long a, Short b) {
    return a >> b;
  }

  public static Object right_shift(Long a, Integer b) {
    return a >> b;
  }

  public static Object right_shift(Long a, Long b) {
    return a >> b;
  }

  public static Object right_shift(Long a, BigDecimal b) {
    return a >> b.longValue();
  }

  /* long >> 操作实现 */

  /* bigecimal >> 操作实现 */
  public static Object right_shift(BigDecimal a, Byte b) {
    return a.longValue() >> b;
  }

  public static Object right_shift(BigDecimal a, Short b) {
    return a.longValue() >> b;
  }

  public static Object right_shift(BigDecimal a, Integer b) {
    return a.longValue() >> b;
  }

  public static Object right_shift(BigDecimal a, Long b) {
    return a.longValue() >> b;
  }

  public static Object right_shift(BigDecimal a, BigDecimal b) {
    return a.longValue() >> b.longValue();
  }

  /* bigdecimal > 操作实现 */

  /* byte >>> 操作实现 */
  public static Object right_shift2(Byte a, Byte b) {
    return a >>> b;
  }

  public static Object right_shift2(Byte a, Short b) {
    return a >>> b;
  }

  public static Object right_shift2(Byte a, Integer b) {
    return a >>> b;
  }

  public static Object right_shift2(Byte a, Long b) {
    return a >>> b;
  }

  public static Object right_shift2(Byte a, BigDecimal b) {
    return a >>> b.longValue();
  }

  /* byte >>> 操作实现 */

  /* short >>> 操作实现 */
  public static Object right_shift2(Short a, Byte b) {
    return a >>> b;
  }

  public static Object right_shift2(Short a, Short b) {
    return a >>> b;
  }

  public static Object right_shift2(Short a, Integer b) {
    return a >>> b;
  }

  public static Object right_shift2(Short a, Long b) {
    return a >>> b;
  }

  public static Object right_shift2(Short a, BigDecimal b) {
    return a >>> b.longValue();
  }

  /* short >>> 操作实现 */

  /* int >>> 操作实现 */
  public static Object right_shift2(Integer a, Byte b) {
    return a >>> b;
  }

  public static Object right_shift2(Integer a, Short b) {
    return a >>> b;
  }

  public static Object right_shift2(Integer a, Integer b) {
    return a >>> b;
  }

  public static Object right_shift2(Integer a, Long b) {
    return a >>> b;
  }

  public static Object right_shift2(Integer a, BigDecimal b) {
    return a >>> b.longValue();
  }

  /* int >>> 操作实现 */

  /* long >>> 操作实现 */
  public static Object right_shift2(Long a, Byte b) {
    return a >>> b;
  }

  public static Object right_shift2(Long a, Short b) {
    return a >>> b;
  }

  public static Object right_shift2(Long a, Integer b) {
    return a >>> b;
  }

  public static Object right_shift2(Long a, Long b) {
    return a >>> b;
  }

  public static Object right_shift2(Long a, BigDecimal b) {
    return a >>> b.longValue();
  }

  /* long >>> 操作实现 */

  /* bigecimal >>> 操作实现 */
  public static Object right_shift2(BigDecimal a, Byte b) {
    return a.longValue() >>> b;
  }

  public static Object right_shift2(BigDecimal a, Short b) {
    return a.longValue() >>> b;
  }

  public static Object right_shift2(BigDecimal a, Integer b) {
    return a.longValue() >>> b;
  }

  public static Object right_shift2(BigDecimal a, Long b) {
    return a.longValue() >>> b;
  }

  public static Object right_shift2(BigDecimal a, BigDecimal b) {
    return a.longValue() >>> b.longValue();
  }

  /* bigdecimal > 操作实现 */

  /* byte & 操作实现 */
  public static Object and(Byte a, Byte b) {
    return a & b;
  }

  public static Object and(Byte a, Short b) {
    return a & b;
  }

  public static Object and(Byte a, Integer b) {
    return a & b;
  }

  public static Object and(Byte a, Long b) {
    return a & b;
  }

  public static Object and(Byte a, BigDecimal b) {
    return a & b.longValue();
  }

  /* byte & 操作实现 */

  /* short & 操作实现 */
  public static Object and(Short a, Byte b) {
    return a & b;
  }

  public static Object and(Short a, Short b) {
    return a & b;
  }

  public static Object and(Short a, Integer b) {
    return a & b;
  }

  public static Object and(Short a, Long b) {
    return a & b;
  }

  public static Object and(Short a, BigDecimal b) {
    return a & b.longValue();
  }

  /* short & 操作实现 */

  /* int & 操作实现 */
  public static Object and(Integer a, Byte b) {
    return a & b;
  }

  public static Object and(Integer a, Short b) {
    return a & b;
  }

  public static Object and(Integer a, Integer b) {
    return a & b;
  }

  public static Object and(Integer a, Long b) {
    return a & b;
  }

  public static Object and(Integer a, BigDecimal b) {
    return a & b.longValue();
  }

  /* int & 操作实现 */

  /* long & 操作实现 */
  public static Object and(Long a, Byte b) {
    return a & b;
  }

  public static Object and(Long a, Short b) {
    return a & b;
  }

  public static Object and(Long a, Integer b) {
    return a & b;
  }

  public static Object and(Long a, Long b) {
    return a & b;
  }

  public static Object and(Long a, BigDecimal b) {
    return a & b.longValue();
  }

  /* long & 操作实现 */

  /* bigecimal & 操作实现 */
  public static Object and(BigDecimal a, Byte b) {
    return a.longValue() & b;
  }

  public static Object and(BigDecimal a, Short b) {
    return a.longValue() & b;
  }

  public static Object and(BigDecimal a, Integer b) {
    return a.longValue() & b;
  }

  public static Object and(BigDecimal a, Long b) {
    return a.longValue() & b;
  }

  public static Object and(BigDecimal a, BigDecimal b) {
    return a.longValue() & b.longValue();
  }

  /* bigdecimal & 操作实现 */

  public static Object and(Boolean a, Boolean b) {
    return a & b;
  }

  /* byte | 操作实现 */
  public static Object or(Byte a, Byte b) {
    return a | b;
  }

  public static Object or(Byte a, Short b) {
    return a | b;
  }

  public static Object or(Byte a, Integer b) {
    return a | b;
  }

  public static Object or(Byte a, Long b) {
    return a | b;
  }

  public static Object or(Byte a, BigDecimal b) {
    return a | b.longValue();
  }

  /* byte | 操作实现 */

  /* short | 操作实现 */
  public static Object or(Short a, Byte b) {
    return a | b;
  }

  public static Object or(Short a, Short b) {
    return a | b;
  }

  public static Object or(Short a, Integer b) {
    return a | b;
  }

  public static Object or(Short a, Long b) {
    return a | b;
  }

  public static Object or(Short a, BigDecimal b) {
    return a | b.longValue();
  }

  /* short | 操作实现 */

  /* int | 操作实现 */
  public static Object or(Integer a, Byte b) {
    return a | b;
  }

  public static Object or(Integer a, Short b) {
    return a | b;
  }

  public static Object or(Integer a, Integer b) {
    return a | b;
  }

  public static Object or(Integer a, Long b) {
    return a | b;
  }

  public static Object or(Integer a, BigDecimal b) {
    return a | b.longValue();
  }

  /* int | 操作实现 */

  /* long | 操作实现 */
  public static Object or(Long a, Byte b) {
    return a | b;
  }

  public static Object or(Long a, Short b) {
    return a | b;
  }

  public static Object or(Long a, Integer b) {
    return a | b;
  }

  public static Object or(Long a, Long b) {
    return a | b;
  }

  public static Object or(Long a, BigDecimal b) {
    return a | b.longValue();
  }

  /* long | 操作实现 */

  /* bigecimal | 操作实现 */
  public static Object or(BigDecimal a, Byte b) {
    return a.longValue() | b;
  }

  public static Object or(BigDecimal a, Short b) {
    return a.longValue() | b;
  }

  public static Object or(BigDecimal a, Integer b) {
    return a.longValue() | b;
  }

  public static Object or(BigDecimal a, Long b) {
    return a.longValue() | b;
  }

  public static Object or(BigDecimal a, BigDecimal b) {
    return a.longValue() | b.longValue();
  }

  /* bigdecimal | 操作实现 */

  public static Object or(Boolean a, Boolean b) {
    return a | b;
  }

  /* byte ^ 操作实现 */
  public static Object xor(Byte a, Byte b) {
    return a ^ b;
  }

  public static Object xor(Byte a, Short b) {
    return a ^ b;
  }

  public static Object xor(Byte a, Integer b) {
    return a ^ b;
  }

  public static Object xor(Byte a, Long b) {
    return a ^ b;
  }

  public static Object xor(Byte a, BigDecimal b) {
    return a ^ b.longValue();
  }

  /* byte ^ 操作实现 */

  /* short ^ 操作实现 */
  public static Object xor(Short a, Byte b) {
    return a ^ b;
  }

  public static Object xor(Short a, Short b) {
    return a ^ b;
  }

  public static Object xor(Short a, Integer b) {
    return a ^ b;
  }

  public static Object xor(Short a, Long b) {
    return a ^ b;
  }

  public static Object xor(Short a, BigDecimal b) {
    return a ^ b.longValue();
  }

  /* short ^ 操作实现 */

  /* int ^ 操作实现 */
  public static Object xor(Integer a, Byte b) {
    return a ^ b;
  }

  public static Object xor(Integer a, Short b) {
    return a ^ b;
  }

  public static Object xor(Integer a, Integer b) {
    return a ^ b;
  }

  public static Object xor(Integer a, Long b) {
    return a ^ b;
  }

  public static Object xor(Integer a, BigDecimal b) {
    return a ^ b.longValue();
  }

  /* int ^ 操作实现 */

  /* long ^ 操作实现 */
  public static Object xor(Long a, Byte b) {
    return a ^ b;
  }

  public static Object xor(Long a, Short b) {
    return a ^ b;
  }

  public static Object xor(Long a, Integer b) {
    return a ^ b;
  }

  public static Object xor(Long a, Long b) {
    return a ^ b;
  }

  public static Object xor(Long a, BigDecimal b) {
    return a ^ b.longValue();
  }

  /* long ^ 操作实现 */

  /* bigecimal ^ 操作实现 */
  public static Object xor(BigDecimal a, Byte b) {
    return a.longValue() ^ b;
  }

  public static Object xor(BigDecimal a, Short b) {
    return a.longValue() ^ b;
  }

  public static Object xor(BigDecimal a, Integer b) {
    return a.longValue() ^ b;
  }

  public static Object xor(BigDecimal a, Long b) {
    return a.longValue() ^ b;
  }

  public static Object xor(BigDecimal a, BigDecimal b) {
    return a.longValue() ^ b.longValue();
  }

  /* bigdecimal ^ 操作实现 */

  public static Object xor(Boolean a, Boolean b) {
    return a ^ b;
  }

  public static Object not(Object object) {
    if (object == null) {
      return null;
    } else if (object instanceof Byte) {
      return ~(Byte) object;
    } else if (object instanceof Short) {
      return ~(Short) object;
    } else if (object instanceof Integer) {
      return ~(Integer) object;
    } else if (object instanceof Long) {
      return ~(Long) object;
    } else if (object instanceof BigDecimal) {
      return ~((BigDecimal) object).longValue();
    }
    throw new IllegalArgumentException(
        String.format("操作符 `~` 不支持 (%s) 类型", object.getClass().getName()));
  }

  private static Object reject(Object a, Object b, String symbol) throws IllegalArgumentException {
    throw new IllegalArgumentException(
        String.format(
            "操作符 `%s` 不支持 (%s,%s) 类型", symbol, a.getClass().getName(), b.getClass().getName()));
  }
}
