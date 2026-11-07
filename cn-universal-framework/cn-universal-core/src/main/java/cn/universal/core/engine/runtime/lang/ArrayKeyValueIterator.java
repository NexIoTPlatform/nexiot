package cn.universal.core.engine.runtime.lang;

public class ArrayKeyValueIterator extends ArrayValueIterator implements KeyIterator {

  public ArrayKeyValueIterator(Object target) {
    super(target);
  }

  @Override
  public Object getKey() {
    return index - 1;
  }
}
