package cn.universal.core.engine.runtime.handle;

import static java.lang.invoke.MethodHandles.catchException;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodType.methodType;

import cn.universal.core.engine.functions.DynamicAttribute;
import cn.universal.core.engine.functions.ObjectConvertExtension;
import cn.universal.core.engine.parsing.ast.literal.BooleanLiteral;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OperatorHandle {

  private static final MethodHandle FALLBACK;

  static {
    try {
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      FALLBACK =
          lookup.findStatic(
              OperatorHandle.class,
              "fallback",
              methodType(Object.class, MethodCallSite.class, Object[].class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new Error("OperatorHandle初始化失败", e);
    }
  }

  public static CallSite bootstrap(
      MethodHandles.Lookup caller, String name, MethodType type, int flags) {
    MethodCallSite callSite = new MethodCallSite(caller, name, type, OperatorHandle.class);
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
        return reject(args[0], args[1], callSite.methodName);
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

  /* byte < 操作实现 */
  public static Object less(Byte a, Byte b) {
    return a < b;
  }

  public static Object less(Byte a, Short b) {
    return a < b;
  }

  public static Object less(Byte a, Integer b) {
    return a < b;
  }

  public static Object less(Byte a, Float b) {
    return a < b;
  }

  public static Object less(Byte a, Double b) {
    return a < b;
  }

  public static Object less(Byte a, Long b) {
    return a < b;
  }

  public static Object less(Byte a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) < 0;
  }

  public static Object less(Byte a, BigInteger b) {
    return BigInteger.valueOf(a).compareTo(b) < 0;
  }

  /* byte < 操作实现 */

  /* short < 操作实现 */
  public static Object less(Short a, Byte b) {
    return a < b;
  }

  public static Object less(Short a, Short b) {
    return a < b;
  }

  public static Object less(Short a, Integer b) {
    return a < b;
  }

  public static Object less(Short a, Float b) {
    return a < b;
  }

  public static Object less(Short a, Double b) {
    return a < b;
  }

  public static Object less(Short a, Long b) {
    return a < b;
  }

  public static Object less(Short a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) < 0;
  }

  public static Object less(Short a, BigInteger b) {
    return BigInteger.valueOf(a).compareTo(b) < 0;
  }

  /* short < 操作实现 */

  /* int < 操作实现 */
  public static Object less(Integer a, Byte b) {
    return a < b;
  }

  public static Object less(Integer a, Short b) {
    return a < b;
  }

  public static Object less(Integer a, Integer b) {
    return a < b;
  }

  public static Object less(Integer a, Float b) {
    return a < b;
  }

  public static Object less(Integer a, Double b) {
    return a < b;
  }

  public static Object less(Integer a, Long b) {
    return a < b;
  }

  public static Object less(Integer a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) < 0;
  }

  public static Object less(Integer a, BigInteger b) {
    return BigInteger.valueOf(a).compareTo(b) < 0;
  }

  /* int < 操作实现 */

  /* float < 操作实现 */
  public static Object less(Float a, Byte b) {
    return a < b;
  }

  public static Object less(Float a, Short b) {
    return a < b;
  }

  public static Object less(Float a, Integer b) {
    return a < b;
  }

  public static Object less(Float a, Float b) {
    return a < b;
  }

  public static Object less(Float a, Double b) {
    return a < b;
  }

  public static Object less(Float a, Long b) {
    return a < b;
  }

  public static Object less(Float a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) < 0;
  }

  public static Object less(Float a, BigInteger b) {
    return new BigDecimal(a).compareTo(new BigDecimal(b)) < 0;
  }

  /* float < 操作实现 */

  /* double < 操作实现 */
  public static Object less(Double a, Byte b) {
    return a < b;
  }

  public static Object less(Double a, Short b) {
    return a < b;
  }

  public static Object less(Double a, Integer b) {
    return a < b;
  }

  public static Object less(Double a, Float b) {
    return a < b;
  }

  public static Object less(Double a, Double b) {
    return a < b;
  }

  public static Object less(Double a, Long b) {
    return a < b;
  }

  public static Object less(Double a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) < 0;
  }

  public static Object less(Double a, BigInteger b) {
    return new BigDecimal(a).compareTo(new BigDecimal(b)) < 0;
  }

  /* double < 操作实现 */

  /* long < 操作实现 */
  public static Object less(Long a, Byte b) {
    return a < b;
  }

  public static Object less(Long a, Short b) {
    return a < b;
  }

  public static Object less(Long a, Integer b) {
    return a < b;
  }

  public static Object less(Long a, Float b) {
    return a < b;
  }

  public static Object less(Long a, Double b) {
    return a < b;
  }

  public static Object less(Long a, Long b) {
    return a < b;
  }

  public static Object less(Long a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) < 0;
  }

  public static Object less(Long a, BigInteger b) {
    return BigInteger.valueOf(a).compareTo(b) < 0;
  }

  /* long < 操作实现 */

  /* bigecimal < 操作实现 */
  public static Object less(BigDecimal a, Byte b) {
    return a.compareTo(new BigDecimal(b)) < 0;
  }

  public static Object less(BigDecimal a, Short b) {
    return a.compareTo(new BigDecimal(b)) < 0;
  }

  public static Object less(BigDecimal a, Integer b) {
    return a.compareTo(new BigDecimal(b)) < 0;
  }

  public static Object less(BigDecimal a, Float b) {
    return a.compareTo(new BigDecimal(b)) < 0;
  }

  public static Object less(BigDecimal a, Double b) {
    return a.compareTo(new BigDecimal(b)) < 0;
  }

  public static Object less(BigDecimal a, Long b) {
    return a.compareTo(new BigDecimal(b)) < 0;
  }

  public static Object less(BigDecimal a, BigDecimal b) {
    return a.compareTo(b) < 0;
  }

  public static Object less(BigDecimal a, BigInteger b) {
    return a.compareTo(new BigDecimal(b)) < 0;
  }

  /* bigdecimal < 操作实现 */

  /* biginteger < 操作实现 */
  public static Object less(BigInteger a, Byte b) {
    return a.compareTo(BigInteger.valueOf(b)) < 0;
  }

  public static Object less(BigInteger a, Short b) {
    return a.compareTo(BigInteger.valueOf(b)) < 0;
  }

  public static Object less(BigInteger a, Integer b) {
    return a.compareTo(BigInteger.valueOf(b)) < 0;
  }

  public static Object less(BigInteger a, Float b) {
    return new BigDecimal(a).compareTo(new BigDecimal(b)) < 0;
  }

  public static Object less(BigInteger a, Double b) {
    return new BigDecimal(a).compareTo(new BigDecimal(b)) < 0;
  }

  public static Object less(BigInteger a, Long b) {
    return a.compareTo(BigInteger.valueOf(b)) < 0;
  }

  public static Object less(BigInteger a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) < 0;
  }

  public static Object less(BigInteger a, BigInteger b) {
    return a.compareTo(b) < 0;
  }

  /* biginteger < 操作实现 */

  public static Object less_fallback(Object a, Object b) {
    if (a == null || b == null) {
      return false;
    }
    if (a.getClass() == b.getClass() && a instanceof Comparable) {
      return ((Comparable) (a)).compareTo(b) < 0;
    }
    return reject(a, b, "<");
  }

  /* byte <= 操作实现 */
  public static Object less_equals(Byte a, Byte b) {
    return a <= b;
  }

  public static Object less_equals(Byte a, Short b) {
    return a <= b;
  }

  public static Object less_equals(Byte a, Integer b) {
    return a <= b;
  }

  public static Object less_equals(Byte a, Float b) {
    return a <= b;
  }

  public static Object less_equals(Byte a, Double b) {
    return a <= b;
  }

  public static Object less_equals(Byte a, Long b) {
    return a <= b;
  }

  public static Object less_equals(Byte a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) <= 0;
  }

  public static Object less_equals(Byte a, BigInteger b) {
    return BigInteger.valueOf(a).compareTo(b) <= 0;
  }

  /* byte <= 操作实现 */

  /* short <= 操作实现 */
  public static Object less_equals(Short a, Byte b) {
    return a <= b;
  }

  public static Object less_equals(Short a, Short b) {
    return a <= b;
  }

  public static Object less_equals(Short a, Integer b) {
    return a <= b;
  }

  public static Object less_equals(Short a, Float b) {
    return a <= b;
  }

  public static Object less_equals(Short a, Double b) {
    return a <= b;
  }

  public static Object less_equals(Short a, Long b) {
    return a <= b;
  }

  public static Object less_equals(Short a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) <= 0;
  }

  public static Object less_equals(Short a, BigInteger b) {
    return BigInteger.valueOf(a).compareTo(b) <= 0;
  }

  /* short <= 操作实现 */

  /* int <= 操作实现 */
  public static Object less_equals(Integer a, Byte b) {
    return a <= b;
  }

  public static Object less_equals(Integer a, Short b) {
    return a <= b;
  }

  public static Object less_equals(Integer a, Integer b) {
    return a <= b;
  }

  public static Object less_equals(Integer a, Float b) {
    return a <= b;
  }

  public static Object less_equals(Integer a, Double b) {
    return a <= b;
  }

  public static Object less_equals(Integer a, Long b) {
    return a <= b;
  }

  public static Object less_equals(Integer a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) <= 0;
  }

  public static Object less_equals(Integer a, BigInteger b) {
    return BigInteger.valueOf(a).compareTo(b) <= 0;
  }

  /* int <= 操作实现 */

  /* float <= 操作实现 */
  public static Object less_equals(Float a, Byte b) {
    return a <= b;
  }

  public static Object less_equals(Float a, Short b) {
    return a <= b;
  }

  public static Object less_equals(Float a, Integer b) {
    return a <= b;
  }

  public static Object less_equals(Float a, Float b) {
    return a <= b;
  }

  public static Object less_equals(Float a, Double b) {
    return a <= b;
  }

  public static Object less_equals(Float a, Long b) {
    return a <= b;
  }

  public static Object less_equals(Float a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) <= 0;
  }

  public static Object less_equals(Float a, BigInteger b) {
    return new BigDecimal(a).compareTo(new BigDecimal(b)) <= 0;
  }

  /* float <= 操作实现 */

  /* double <= 操作实现 */
  public static Object less_equals(Double a, Byte b) {
    return a <= b;
  }

  public static Object less_equals(Double a, Short b) {
    return a <= b;
  }

  public static Object less_equals(Double a, Integer b) {
    return a <= b;
  }

  public static Object less_equals(Double a, Float b) {
    return a <= b;
  }

  public static Object less_equals(Double a, Double b) {
    return a <= b;
  }

  public static Object less_equals(Double a, Long b) {
    return a <= b;
  }

  public static Object less_equals(Double a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) <= 0;
  }

  public static Object less_equals(Double a, BigInteger b) {
    return new BigDecimal(a).compareTo(new BigDecimal(b)) <= 0;
  }

  /* double <= 操作实现 */

  /* long <= 操作实现 */
  public static Object less_equals(Long a, Byte b) {
    return a <= b;
  }

  public static Object less_equals(Long a, Short b) {
    return a <= b;
  }

  public static Object less_equals(Long a, Integer b) {
    return a <= b;
  }

  public static Object less_equals(Long a, Float b) {
    return a <= b;
  }

  public static Object less_equals(Long a, Double b) {
    return a <= b;
  }

  public static Object less_equals(Long a, Long b) {
    return a <= b;
  }

  public static Object less_equals(Long a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) <= 0;
  }

  public static Object less_equals(Long a, BigInteger b) {
    return BigInteger.valueOf(a).compareTo(b) <= 0;
  }

  /* long <= 操作实现 */

  /* bigecimal <= 操作实现 */
  public static Object less_equals(BigDecimal a, Byte b) {
    return a.compareTo(new BigDecimal(b)) <= 0;
  }

  public static Object less_equals(BigDecimal a, Short b) {
    return a.compareTo(new BigDecimal(b)) <= 0;
  }

  public static Object less_equals(BigDecimal a, Integer b) {
    return a.compareTo(new BigDecimal(b)) <= 0;
  }

  public static Object less_equals(BigDecimal a, Float b) {
    return a.compareTo(new BigDecimal(b)) <= 0;
  }

  public static Object less_equals(BigDecimal a, Double b) {
    return a.compareTo(new BigDecimal(b)) <= 0;
  }

  public static Object less_equals(BigDecimal a, Long b) {
    return a.compareTo(new BigDecimal(b)) <= 0;
  }

  public static Object less_equals(BigDecimal a, BigDecimal b) {
    return a.compareTo(b) <= 0;
  }

  public static Object less_equals(BigDecimal a, BigInteger b) {
    return a.compareTo(new BigDecimal(b)) <= 0;
  }

  /* bigdecimal <= 操作实现 */

  /* biginteger <= 操作实现 */
  public static Object less_equals(BigInteger a, Byte b) {
    return a.compareTo(BigInteger.valueOf(b)) <= 0;
  }

  public static Object less_equals(BigInteger a, Short b) {
    return a.compareTo(BigInteger.valueOf(b)) <= 0;
  }

  public static Object less_equals(BigInteger a, Integer b) {
    return a.compareTo(BigInteger.valueOf(b)) <= 0;
  }

  public static Object less_equals(BigInteger a, Float b) {
    return new BigDecimal(a).compareTo(new BigDecimal(b)) <= 0;
  }

  public static Object less_equals(BigInteger a, Double b) {
    return new BigDecimal(a).compareTo(new BigDecimal(b)) <= 0;
  }

  public static Object less_equals(BigInteger a, Long b) {
    return a.compareTo(BigInteger.valueOf(b)) <= 0;
  }

  public static Object less_equals(BigInteger a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) <= 0;
  }

  public static Object less_equals(BigInteger a, BigInteger b) {
    return a.compareTo(b) <= 0;
  }

  /* biginteger <= 操作实现 */

  public static Object less_equals_fallback(Object a, Object b) {
    if (a == null || b == null) {
      return false;
    }
    if (a.getClass() == b.getClass() && a instanceof Comparable) {
      return ((Comparable) (a)).compareTo(b) <= 0;
    }
    return reject(a, b, "<=");
  }

  /* == 操作实现 */
  public static Object equals_fallback(Object a, Object b) {
    if (Objects.equals(a, b)) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    if (a instanceof Number || b instanceof Number) {
      BigDecimal left = ObjectConvertExtension.asDecimal(a, null);
      BigDecimal right = ObjectConvertExtension.asDecimal(b, null);
      return left != null && right != null && left.compareTo(right) == 0;
    }
    return false;
  }

  /* === 操作实现 */
  public static Object accurate_equals_fallback(Object a, Object b) {
    return Objects.equals(a, b);
  }

  /* != 操作实现 */
  public static Object not_equals_fallback(Object a, Object b) {
    return !(boolean) equals_fallback(a, b);
  }

  /* !== 操作实现 */
  public static Object not_accurate_equals_fallback(Object a, Object b) {
    return !Objects.equals(a, b);
  }

  /* byte > 操作实现 */
  public static Object greater(Byte a, Byte b) {
    return a > b;
  }

  public static Object greater(Byte a, Short b) {
    return a > b;
  }

  public static Object greater(Byte a, Integer b) {
    return a > b;
  }

  public static Object greater(Byte a, Float b) {
    return a > b;
  }

  public static Object greater(Byte a, Double b) {
    return a > b;
  }

  public static Object greater(Byte a, Long b) {
    return a > b;
  }

  public static Object greater(Byte a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) > 0;
  }

  public static Object greater(Byte a, BigInteger b) {
    return BigInteger.valueOf(a).compareTo(b) > 0;
  }

  /* byte > 操作实现 */

  /* short > 操作实现 */
  public static Object greater(Short a, Byte b) {
    return a > b;
  }

  public static Object greater(Short a, Short b) {
    return a > b;
  }

  public static Object greater(Short a, Integer b) {
    return a > b;
  }

  public static Object greater(Short a, Float b) {
    return a > b;
  }

  public static Object greater(Short a, Double b) {
    return a > b;
  }

  public static Object greater(Short a, Long b) {
    return a > b;
  }

  public static Object greater(Short a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) > 0;
  }

  public static Object greater(Short a, BigInteger b) {
    return BigInteger.valueOf(a).compareTo(b) > 0;
  }

  /* short > 操作实现 */

  /* int > 操作实现 */
  public static Object greater(Integer a, Byte b) {
    return a > b;
  }

  public static Object greater(Integer a, Short b) {
    return a > b;
  }

  public static Object greater(Integer a, Integer b) {
    return a > b;
  }

  public static Object greater(Integer a, Float b) {
    return a > b;
  }

  public static Object greater(Integer a, Double b) {
    return a > b;
  }

  public static Object greater(Integer a, Long b) {
    return a > b;
  }

  public static Object greater(Integer a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) > 0;
  }

  public static Object greater(Integer a, BigInteger b) {
    return BigInteger.valueOf(a).compareTo(b) > 0;
  }

  /* int > 操作实现 */

  /* float > 操作实现 */
  public static Object greater(Float a, Byte b) {
    return a > b;
  }

  public static Object greater(Float a, Short b) {
    return a > b;
  }

  public static Object greater(Float a, Integer b) {
    return a > b;
  }

  public static Object greater(Float a, Float b) {
    return a > b;
  }

  public static Object greater(Float a, Double b) {
    return a > b;
  }

  public static Object greater(Float a, Long b) {
    return a > b;
  }

  public static Object greater(Float a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) > 0;
  }

  public static Object greater(Float a, BigInteger b) {
    return new BigDecimal(a).compareTo(new BigDecimal(b)) > 0;
  }

  /* float > 操作实现 */

  /* double > 操作实现 */
  public static Object greater(Double a, Byte b) {
    return a > b;
  }

  public static Object greater(Double a, Short b) {
    return a > b;
  }

  public static Object greater(Double a, Integer b) {
    return a > b;
  }

  public static Object greater(Double a, Float b) {
    return a > b;
  }

  public static Object greater(Double a, Double b) {
    return a > b;
  }

  public static Object greater(Double a, Long b) {
    return a > b;
  }

  public static Object greater(Double a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) > 0;
  }

  public static Object greater(Double a, BigInteger b) {
    return new BigDecimal(a).compareTo(new BigDecimal(b)) > 0;
  }

  /* double > 操作实现 */

  /* long > 操作实现 */
  public static Object greater(Long a, Byte b) {
    return a > b;
  }

  public static Object greater(Long a, Short b) {
    return a > b;
  }

  public static Object greater(Long a, Integer b) {
    return a > b;
  }

  public static Object greater(Long a, Float b) {
    return a > b;
  }

  public static Object greater(Long a, Double b) {
    return a > b;
  }

  public static Object greater(Long a, Long b) {
    return a > b;
  }

  public static Object greater(Long a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) > 0;
  }

  public static Object greater(Long a, BigInteger b) {
    return BigInteger.valueOf(a).compareTo(b) > 0;
  }

  /* long > 操作实现 */

  /* bigecimal > 操作实现 */
  public static Object greater(BigDecimal a, Byte b) {
    return a.compareTo(new BigDecimal(b)) > 0;
  }

  public static Object greater(BigDecimal a, Short b) {
    return a.compareTo(new BigDecimal(b)) > 0;
  }

  public static Object greater(BigDecimal a, Integer b) {
    return a.compareTo(new BigDecimal(b)) > 0;
  }

  public static Object greater(BigDecimal a, Float b) {
    return a.compareTo(new BigDecimal(b)) > 0;
  }

  public static Object greater(BigDecimal a, Double b) {
    return a.compareTo(new BigDecimal(b)) > 0;
  }

  public static Object greater(BigDecimal a, Long b) {
    return a.compareTo(new BigDecimal(b)) > 0;
  }

  public static Object greater(BigDecimal a, BigDecimal b) {
    return a.compareTo(b) > 0;
  }

  public static Object greater(BigDecimal a, BigInteger b) {
    return a.compareTo(new BigDecimal(b)) > 0;
  }

  /* bigdecimal > 操作实现 */

  /* biginteger > 操作实现 */
  public static Object greater(BigInteger a, Byte b) {
    return a.compareTo(BigInteger.valueOf(b)) > 0;
  }

  public static Object greater(BigInteger a, Short b) {
    return a.compareTo(BigInteger.valueOf(b)) > 0;
  }

  public static Object greater(BigInteger a, Integer b) {
    return a.compareTo(BigInteger.valueOf(b)) > 0;
  }

  public static Object greater(BigInteger a, Float b) {
    return new BigDecimal(a).compareTo(new BigDecimal(b)) > 0;
  }

  public static Object greater(BigInteger a, Double b) {
    return new BigDecimal(a).compareTo(new BigDecimal(b)) > 0;
  }

  public static Object greater(BigInteger a, Long b) {
    return a.compareTo(BigInteger.valueOf(b)) > 0;
  }

  public static Object greater(BigInteger a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) > 0;
  }

  public static Object greater(BigInteger a, BigInteger b) {
    return a.compareTo(b) > 0;
  }

  /* biginteger > 操作实现 */

  public static Object greater_fallback(Object a, Object b) {
    if (a == null || b == null) {
      return false;
    }
    if (a.getClass() == b.getClass() && a instanceof Comparable) {
      return ((Comparable) (a)).compareTo(b) > 0;
    }
    return reject(a, b, ">");
  }

  /* byte >= 操作实现 */
  public static Object greater_equals(Byte a, Byte b) {
    return a >= b;
  }

  public static Object greater_equals(Byte a, Short b) {
    return a >= b;
  }

  public static Object greater_equals(Byte a, Integer b) {
    return a >= b;
  }

  public static Object greater_equals(Byte a, Float b) {
    return a >= b;
  }

  public static Object greater_equals(Byte a, Double b) {
    return a >= b;
  }

  public static Object greater_equals(Byte a, Long b) {
    return a >= b;
  }

  public static Object greater_equals(Byte a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) >= 0;
  }

  public static Object greater_equals(Byte a, BigInteger b) {
    return BigInteger.valueOf(a).compareTo(b) >= 0;
  }

  /* byte >= 操作实现 */

  /* short >= 操作实现 */
  public static Object greater_equals(Short a, Byte b) {
    return a >= b;
  }

  public static Object greater_equals(Short a, Short b) {
    return a >= b;
  }

  public static Object greater_equals(Short a, Integer b) {
    return a >= b;
  }

  public static Object greater_equals(Short a, Float b) {
    return a >= b;
  }

  public static Object greater_equals(Short a, Double b) {
    return a >= b;
  }

  public static Object greater_equals(Short a, Long b) {
    return a >= b;
  }

  public static Object greater_equals(Short a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) >= 0;
  }

  public static Object greater_equals(Short a, BigInteger b) {
    return BigInteger.valueOf(a).compareTo(b) >= 0;
  }

  /* short >= 操作实现 */

  /* int >= 操作实现 */
  public static Object greater_equals(Integer a, Byte b) {
    return a >= b;
  }

  public static Object greater_equals(Integer a, Short b) {
    return a >= b;
  }

  public static Object greater_equals(Integer a, Integer b) {
    return a >= b;
  }

  public static Object greater_equals(Integer a, Float b) {
    return a >= b;
  }

  public static Object greater_equals(Integer a, Double b) {
    return a >= b;
  }

  public static Object greater_equals(Integer a, Long b) {
    return a >= b;
  }

  public static Object greater_equals(Integer a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) >= 0;
  }

  public static Object greater_equals(Integer a, BigInteger b) {
    return BigInteger.valueOf(a).compareTo(b) >= 0;
  }

  /* int >= 操作实现 */

  /* float >= 操作实现 */
  public static Object greater_equals(Float a, Byte b) {
    return a >= b;
  }

  public static Object greater_equals(Float a, Short b) {
    return a >= b;
  }

  public static Object greater_equals(Float a, Integer b) {
    return a >= b;
  }

  public static Object greater_equals(Float a, Float b) {
    return a >= b;
  }

  public static Object greater_equals(Float a, Double b) {
    return a >= b;
  }

  public static Object greater_equals(Float a, Long b) {
    return a >= b;
  }

  public static Object greater_equals(Float a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) >= 0;
  }

  public static Object greater_equals(Float a, BigInteger b) {
    return new BigDecimal(a).compareTo(new BigDecimal(b)) >= 0;
  }

  /* float >= 操作实现 */

  /* double >= 操作实现 */
  public static Object greater_equals(Double a, Byte b) {
    return a >= b;
  }

  public static Object greater_equals(Double a, Short b) {
    return a >= b;
  }

  public static Object greater_equals(Double a, Integer b) {
    return a >= b;
  }

  public static Object greater_equals(Double a, Float b) {
    return a >= b;
  }

  public static Object greater_equals(Double a, Double b) {
    return a >= b;
  }

  public static Object greater_equals(Double a, Long b) {
    return a >= b;
  }

  public static Object greater_equals(Double a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) >= 0;
  }

  public static Object greater_equals(Double a, BigInteger b) {
    return new BigDecimal(a).compareTo(new BigDecimal(b)) >= 0;
  }

  /* double >= 操作实现 */

  /* long >= 操作实现 */
  public static Object greater_equals(Long a, Byte b) {
    return a >= b;
  }

  public static Object greater_equals(Long a, Short b) {
    return a >= b;
  }

  public static Object greater_equals(Long a, Integer b) {
    return a >= b;
  }

  public static Object greater_equals(Long a, Float b) {
    return a >= b;
  }

  public static Object greater_equals(Long a, Double b) {
    return a >= b;
  }

  public static Object greater_equals(Long a, Long b) {
    return a >= b;
  }

  public static Object greater_equals(Long a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) >= 0;
  }

  public static Object greater_equals(Long a, BigInteger b) {
    return BigInteger.valueOf(a).compareTo(b) >= 0;
  }

  /* long >= 操作实现 */

  /* bigdecimal >= 操作实现 */
  public static Object greater_equals(BigDecimal a, Byte b) {
    return a.compareTo(new BigDecimal(b)) >= 0;
  }

  public static Object greater_equals(BigDecimal a, Short b) {
    return a.compareTo(new BigDecimal(b)) >= 0;
  }

  public static Object greater_equals(BigDecimal a, Integer b) {
    return a.compareTo(new BigDecimal(b)) >= 0;
  }

  public static Object greater_equals(BigDecimal a, Float b) {
    return a.compareTo(new BigDecimal(b)) >= 0;
  }

  public static Object greater_equals(BigDecimal a, Double b) {
    return a.compareTo(new BigDecimal(b)) >= 0;
  }

  public static Object greater_equals(BigDecimal a, Long b) {
    return a.compareTo(new BigDecimal(b)) >= 0;
  }

  public static Object greater_equals(BigDecimal a, BigDecimal b) {
    return a.compareTo(b) >= 0;
  }

  public static Object greater_equals(BigDecimal a, BigInteger b) {
    return a.compareTo(new BigDecimal(b)) >= 0;
  }

  /* bigdecimal >= 操作实现 */

  /* biginteger > 操作实现 */
  public static Object greater_equals(BigInteger a, Byte b) {
    return a.compareTo(BigInteger.valueOf(b)) >= 0;
  }

  public static Object greater_equals(BigInteger a, Short b) {
    return a.compareTo(BigInteger.valueOf(b)) >= 0;
  }

  public static Object greater_equals(BigInteger a, Integer b) {
    return a.compareTo(BigInteger.valueOf(b)) >= 0;
  }

  public static Object greater_equals(BigInteger a, Float b) {
    return new BigDecimal(a).compareTo(new BigDecimal(b)) >= 0;
  }

  public static Object greater_equals(BigInteger a, Double b) {
    return new BigDecimal(a).compareTo(new BigDecimal(b)) >= 0;
  }

  public static Object greater_equals(BigInteger a, Long b) {
    return a.compareTo(BigInteger.valueOf(b)) >= 0;
  }

  public static Object greater_equals(BigInteger a, BigDecimal b) {
    return new BigDecimal(a).compareTo(b) >= 0;
  }

  public static Object greater_equals(BigInteger a, BigInteger b) {
    return a.compareTo(b) >= 0;
  }

  /* biginteger >= 操作实现 */

  public static Object greater_equals_fallback(Object a, Object b) {
    if (a == null || b == null) {
      return false;
    }
    if (a.getClass() == b.getClass() && a instanceof Comparable) {
      return ((Comparable) (a)).compareTo(b) >= 0;
    }
    return reject(a, b, ">=");
  }

  public static boolean isFalse(Object object) {
    return !isTrue(object);
  }

  public static boolean isTrue(Object object) {
    return BooleanLiteral.isTrue(object);
  }

  private static Object reject(Object a, Object b, String symbol) throws IllegalArgumentException {
    throw new IllegalArgumentException(
        String.format(
            "操作符 `%s` 不支持 (%s,%s) 类型", symbol, a.getClass().getName(), b.getClass().getName()));
  }

  public static Object map_or_array_access(int[] target, Number key) {
    return key.intValue() < target.length ? target[key.intValue()] : null;
  }

  public static Object map_or_array_access(byte[] target, Number key) {
    return key.intValue() < target.length ? target[key.intValue()] : null;
  }

  public static Object map_or_array_access(short[] target, Number key) {
    return key.intValue() < target.length ? target[key.intValue()] : null;
  }

  public static Object map_or_array_access(float[] target, Number key) {
    return key.intValue() < target.length ? target[key.intValue()] : null;
  }

  public static Object map_or_array_access(double[] target, Number key) {
    return key.intValue() < target.length ? target[key.intValue()] : null;
  }

  public static Object map_or_array_access(long[] target, Number key) {
    return key.intValue() < target.length ? target[key.intValue()] : null;
  }

  public static Object map_or_array_access(char[] target, Number key) {
    return key.intValue() < target.length ? target[key.intValue()] : null;
  }

  public static Object map_or_array_access(boolean[] target, Number key) {
    return key.intValue() < target.length ? target[key.intValue()] : null;
  }

  public static Object map_or_array_access(Object[] target, Number key) {
    return key.intValue() < target.length ? target[key.intValue()] : null;
  }

  public static Object map_or_array_access(String target, Number key) {
    return key.intValue() < target.length() ? target.charAt(key.intValue()) : null;
  }

  public static Object map_or_array_access_fallback(Object target, Object key) {
    if (target == null) {
      return null;
    }
    if (target instanceof Map) {
      return ((Map) target).get(key);
    } else if (key instanceof Number) {
      int index = ((Number) key).intValue();
      if (target instanceof List) {
        List list = (List) target;
        return index < list.size() ? list.get(index) : null;
      } else if (target.getClass().isArray()) {
        int length = Array.getLength(target);
        return index < length ? Array.get(target, ((Number) key).intValue()) : null;
      }
    } else if (target instanceof DynamicAttribute && key instanceof String) {
      return ((DynamicAttribute<?, ?>) target).getDynamicAttribute((String) key);
    }
    return reject(target, key, ".或[]");
  }
}
