package cn.universal.core.engine.runtime.lang;

import java.util.Iterator;

public class KeyValueIterator implements Iterator<Object>, KeyIterator {

  private final Iterator<Object> iterator;

  private int index = 0;

  public KeyValueIterator(Iterator<Object> iterator) {
    this.iterator = iterator;
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  @Override
  public Object next() {
    return iterator.next();
  }

  @Override
  public Object getKey() {
    return index++;
  }
}
