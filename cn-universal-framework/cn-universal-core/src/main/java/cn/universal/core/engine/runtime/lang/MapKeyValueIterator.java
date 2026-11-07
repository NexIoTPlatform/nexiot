package cn.universal.core.engine.runtime.lang;

import java.util.Iterator;
import java.util.Map;

public class MapKeyValueIterator implements Iterator<Object>, KeyIterator {

  private final Iterator<Map.Entry<Object, Object>> iterator;

  private Map.Entry<Object, Object> current;

  public MapKeyValueIterator(Map<Object, Object> target) {
    this.iterator = target.entrySet().iterator();
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  @Override
  public Object next() {
    current = iterator.next();
    return current.getValue();
  }

  @Override
  public Object getKey() {
    return current.getKey();
  }
}
