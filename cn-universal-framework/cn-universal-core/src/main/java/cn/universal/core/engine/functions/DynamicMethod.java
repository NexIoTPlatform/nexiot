package cn.universal.core.engine.functions;

import java.util.List;

public interface DynamicMethod {

  Object execute(String methodName, List<Object> parameters);
}
