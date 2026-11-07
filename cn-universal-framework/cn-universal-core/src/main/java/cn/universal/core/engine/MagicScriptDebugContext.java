package cn.universal.core.engine;

import cn.universal.core.engine.runtime.Variables;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MagicScriptDebugContext extends MagicScriptContext {

  private final BlockingQueue<String> producer = new LinkedBlockingQueue<>();
  private final BlockingQueue<String> consumer = new LinkedBlockingQueue<>();
  public List<Integer> breakpoints;
  private String id = UUID.randomUUID().toString().replace("-", "");
  private Consumer<Map<String, Object>> callback;

  private int[] line;

  private int timeout = 60;

  private boolean stepInto = false;

  public MagicScriptDebugContext(List<Integer> breakpoints) {
    this.breakpoints = breakpoints;
  }

  public void setCallback(Consumer<Map<String, Object>> callback) {
    this.callback = callback;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  public void setBreakpoints(List<Integer> breakpoints) {
    this.breakpoints = breakpoints;
  }

  @Override
  public synchronized void pause(
      int startRow, int startCol, int endRow, int endCol, Variables variables)
      throws InterruptedException {
    if (stepInto || breakpoints.contains(startRow)) {
      this.line = new int[] {startRow, startCol, endRow, endCol};
      consumer.offer(this.id);
      Map<String, Object> varMap = new LinkedHashMap<>(getRootVariables());
      varMap.putAll(variables.getVariables(this));
      callback.accept(getDebugInfo(varMap));
      producer.poll(timeout, TimeUnit.SECONDS);
    }
  }

  public void await() throws InterruptedException {
    consumer.take();
  }

  public void singal() throws InterruptedException {
    producer.offer(this.id);
    await();
  }

  public void setStepInto(boolean stepInto) {
    this.stepInto = stepInto;
  }

  private Map<String, Object> getDebugInfo(Map<String, Object> variables) {
    List<Map<String, Object>> varList = new ArrayList<>();
    Set<Map.Entry<String, Object>> entries = variables.entrySet();
    for (Map.Entry<String, Object> entry : entries) {
      Object value = entry.getValue();
      Map<String, Object> variable = new HashMap<>();
      variable.put("name", entry.getKey());
      if (value != null) {
        variable.put("value", value);
        variable.put("type", value.getClass());
      } else {
        variable.put("value", "null");
      }
      varList.add(variable);
    }
    Collections.reverse(varList);
    Map<String, Object> info = new HashMap<>();
    info.put("variables", varList);
    info.put("range", line);
    return info;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
