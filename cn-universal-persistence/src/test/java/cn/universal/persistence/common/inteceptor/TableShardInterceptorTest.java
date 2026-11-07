package cn.universal.persistence.common.inteceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.universal.persistence.query.DeviceLogQuery;
import java.lang.reflect.Method;
import org.apache.ibatis.binding.MapperMethod;
import org.junit.jupiter.api.Test;

class TableShardInterceptorTest {

  @Test
  void shouldResolveIotIdFromNamedQueryParam() throws Exception {
    TableShardInterceptor interceptor = new TableShardInterceptor();
    MapperMethod.ParamMap<Object> params = new MapperMethod.ParamMap<>();
    DeviceLogQuery query = new DeviceLogQuery.Builder().iotId("pk001dev001").build();
    params.put("query", query);
    params.put("param1", query);

    Object value = invokeResolveShardValue(interceptor, params, "iotId");

    assertEquals("pk001dev001", value);
  }

  @Test
  void shouldPreferDirectIotIdParamWhenPresent() throws Exception {
    TableShardInterceptor interceptor = new TableShardInterceptor();
    MapperMethod.ParamMap<Object> params = new MapperMethod.ParamMap<>();
    DeviceLogQuery query = new DeviceLogQuery.Builder().iotId("nested").build();
    params.put("query", query);
    params.put("param1", query);
    params.put("iotId", "direct");

    Object value = invokeResolveShardValue(interceptor, params, "iotId");

    assertEquals("direct", value);
  }

  private Object invokeResolveShardValue(
      TableShardInterceptor interceptor, Object parameterObject, String fieldName) throws Exception {
    Method method =
        TableShardInterceptor.class.getDeclaredMethod(
            "resolveShardValue", Object.class, String.class);
    method.setAccessible(true);
    return method.invoke(interceptor, parameterObject, fieldName);
  }
}
