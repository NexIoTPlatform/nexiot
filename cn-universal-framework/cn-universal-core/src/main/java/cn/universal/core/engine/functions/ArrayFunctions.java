package cn.universal.core.engine.functions;

import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.annotation.Function;
import java.lang.reflect.Array;

public class ArrayFunctions {

  @Function
  @Comment("创建`int`数组")
  public int[] new_int_array(@Comment(name = "size", value = "数组大小") int size) {
    return new int[size];
  }

  @Function
  @Comment("创建`short`数组")
  public short[] new_short_array(@Comment(name = "size", value = "数组大小") int size) {
    return new short[size];
  }

  @Function
  @Comment("创建`double`数组")
  public double[] new_double_array(@Comment(name = "size", value = "数组大小") int size) {
    return new double[size];
  }

  @Function
  @Comment("创建`float`数组")
  public float[] new_float_array(@Comment(name = "size", value = "数组大小") int size) {
    return new float[size];
  }

  @Function
  @Comment("创建`byte`数组")
  public byte[] new_byte_array(@Comment(name = "size", value = "数组大小") int size) {
    return new byte[size];
  }

  @Function
  @Comment("创建`char`数组")
  public char[] new_char_array(@Comment(name = "size", value = "数组大小") int size) {
    return new char[size];
  }

  @Function
  @Comment("创建`boolean`数组")
  public boolean[] new_boolean_array(@Comment(name = "size", value = "数组大小") int size) {
    return new boolean[size];
  }

  @Function
  @Comment("创建`long`数组")
  public long[] new_long_array(@Comment(name = "size", value = "数组大小") int size) {
    return new long[size];
  }

  @Function
  @Comment("创建`Object`数组")
  public Object[] new_array(@Comment(name = "size", value = "数组大小") int size) {
    return new Object[size];
  }

  @Function
  @Comment("创建`Object`数组")
  public <T> T[] new_array(
      @Comment(name = "componentType", value = "数组类型") Class<T> componentType,
      @Comment(name = "size", value = "数组大小") int size) {
    return (T[]) Array.newInstance(componentType, size);
  }

  @Function
  @Comment("创建`String`数组")
  public String[] new_array(@Comment(name = "values", value = "字符串") String... array) {
    return array;
  }

  @Function
  @Comment("创建`int`数组")
  public int[] new_array(@Comment(name = "size", value = "数组大小") int... array) {
    return array;
  }

  @Function
  @Comment("创建`short`数组")
  public short[] new_array(@Comment(name = "size", value = "数组大小") short... array) {
    return array;
  }

  @Function
  @Comment("创建`double`数组")
  public double[] new_array(@Comment(name = "values", value = "double数值") double... array) {
    return array;
  }

  @Function
  @Comment("创建`float`数组")
  public float[] new_array(@Comment(name = "values", value = "float数值") float... array) {
    return array;
  }

  @Function
  @Comment("创建`char`数组")
  public char[] new_array(@Comment(name = "values", value = "char值") char... array) {
    return array;
  }

  @Function
  @Comment("创建`byte`数组")
  public byte[] new_array(@Comment(name = "values", value = "byte值") byte... array) {
    return array;
  }

  @Function
  @Comment("创建`boolean`数组")
  public boolean[] new_array(@Comment(name = "values", value = "boolean值") boolean... array) {
    return array;
  }

  @Function
  @Comment("创建`long`数组")
  public long[] new_array(@Comment(name = "values", value = "long值") long... array) {
    return array;
  }
}
