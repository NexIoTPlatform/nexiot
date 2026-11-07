package cn.universal.security.support;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.utils.SpringUtils;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.utils.SecurityUtils;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

/**
 * 统一处理 tenantId 自动填充。
 *
 * <p>目标不是强改 SQL，而是在现有 tk.mybatis/查询对象约定下，自动把当前租户补齐到参数对象或 Example。
 */
@Component
@Intercepts({
  @Signature(
      type = Executor.class,
      method = "update",
      args = {MappedStatement.class, Object.class}),
  @Signature(
      type = Executor.class,
      method = "query",
      args = {MappedStatement.class, Object.class, org.apache.ibatis.session.RowBounds.class,
          org.apache.ibatis.session.ResultHandler.class}),
  @Signature(
      type = Executor.class,
      method = "query",
      args = {
        MappedStatement.class,
        Object.class,
        org.apache.ibatis.session.RowBounds.class,
        org.apache.ibatis.session.ResultHandler.class,
        org.apache.ibatis.cache.CacheKey.class,
        org.apache.ibatis.mapping.BoundSql.class
      })
})
public class TenantAutoFillInterceptor implements Interceptor {

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    if (TenantIgnoreContext.isIgnore()) {
      return invocation.proceed();
    }
    TenantRuntime runtime = resolveTenantRuntime();
    if (runtime == null || runtime.superAdmin || StrUtil.isBlank(runtime.tenantId)) {
      return invocation.proceed();
    }

    Object[] args = invocation.getArgs();
    MappedStatement mappedStatement = (MappedStatement) args[0];
    Object parameterObject = args.length > 1 ? args[1] : null;
    applyTenant(parameterObject, mappedStatement.getSqlCommandType(), runtime, new IdentityHashMap<>());
    return invocation.proceed();
  }

  @Override
  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  private void applyTenant(
      Object parameterObject,
      SqlCommandType commandType,
      TenantRuntime runtime,
      IdentityHashMap<Object, Boolean> visited) {
    if (parameterObject == null || visited.containsKey(parameterObject) || isSimpleValue(parameterObject)) {
      return;
    }
    visited.put(parameterObject, Boolean.TRUE);

    if (parameterObject instanceof Example example) {
      applyTenantToExample(example, runtime.tenantId);
      return;
    }
    if (parameterObject instanceof Map<?, ?> map) {
      for (Object value : map.values()) {
        applyTenant(value, commandType, runtime, visited);
      }
      return;
    }
    if (parameterObject instanceof Collection<?> collection) {
      for (Object item : collection) {
        applyTenant(item, commandType, runtime, visited);
      }
      return;
    }
    if (parameterObject.getClass().isArray()) {
      int length = Array.getLength(parameterObject);
      for (int i = 0; i < length; i++) {
        applyTenant(Array.get(parameterObject, i), commandType, runtime, visited);
      }
      return;
    }

    BeanWrapper beanWrapper = createBeanWrapper(parameterObject);
    if (beanWrapper == null) {
      return;
    }

    fillTenantProperty(beanWrapper, runtime.tenantId);
    if (SqlCommandType.INSERT.equals(commandType)) {
      fillDeptProperty(beanWrapper, runtime.deptId);
      fillCreateDeptProperty(beanWrapper, runtime.deptId);
    }
  }

  private void applyTenantToExample(Example example, String tenantId) {
    Map<String, ?> propertyMap = example.getPropertyMap();
    if (propertyMap == null || !propertyMap.containsKey("tenantId")) {
      return;
    }
    if (hasTenantCondition(example)) {
      return;
    }
    List<Example.Criteria> criteriaList = example.getOredCriteria();
    if (criteriaList == null || criteriaList.isEmpty()) {
      example.createCriteria().andEqualTo("tenantId", tenantId);
      return;
    }
    for (Example.Criteria criteria : criteriaList) {
      if (criteria == null) {
        continue;
      }
      criteria.andEqualTo("tenantId", tenantId);
    }
  }

  private boolean hasTenantCondition(Example example) {
    List<Example.Criteria> criteriaList = example.getOredCriteria();
    if (criteriaList == null || criteriaList.isEmpty()) {
      return false;
    }
    for (Example.Criteria criteria : criteriaList) {
      if (criteria == null || criteria.getAllCriteria() == null) {
        continue;
      }
      for (Example.Criterion criterion : criteria.getAllCriteria()) {
        if (criterion == null || criterion.getCondition() == null) {
          continue;
        }
        String condition = criterion.getCondition().toLowerCase();
        if (condition.contains("tenant_id")) {
          return true;
        }
      }
    }
    return false;
  }

  private void fillTenantProperty(BeanWrapper beanWrapper, String tenantId) {
    if (!beanWrapper.isWritableProperty("tenantId")) {
      return;
    }
    Object currentValue = readProperty(beanWrapper, "tenantId");
    if (currentValue == null || StrUtil.isBlank(String.valueOf(currentValue))) {
      beanWrapper.setPropertyValue("tenantId", tenantId);
    }
  }

  private void fillDeptProperty(BeanWrapper beanWrapper, Long deptId) {
    if (deptId == null || !beanWrapper.isWritableProperty("deptId")) {
      return;
    }
    if (readProperty(beanWrapper, "deptId") == null) {
      beanWrapper.setPropertyValue("deptId", deptId);
    }
  }

  private void fillCreateDeptProperty(BeanWrapper beanWrapper, Long deptId) {
    if (deptId == null || !beanWrapper.isWritableProperty("createDept")) {
      return;
    }
    if (readProperty(beanWrapper, "createDept") == null) {
      beanWrapper.setPropertyValue("createDept", deptId);
    }
  }

  private Object readProperty(BeanWrapper beanWrapper, String propertyName) {
    if (!beanWrapper.isReadableProperty(propertyName)) {
      return null;
    }
    return beanWrapper.getPropertyValue(propertyName);
  }

  private BeanWrapper createBeanWrapper(Object target) {
    try {
      return new BeanWrapperImpl(target);
    } catch (Exception ignored) {
      return null;
    }
  }

  private TenantRuntime resolveTenantRuntime() {
    String unionId = SecurityUtils.getUnionIdQuietly();
    String tenantIdFromToken = SecurityUtils.getTenantIdQuietly();
    if (StrUtil.isBlank(unionId)) {
      return buildRuntimeFromToken(tenantIdFromToken);
    }

    IoTUser currentUser = null;
    IoTUserService ioTUserService = resolveIoTUserService();
    if (ioTUserService != null) {
      TenantIgnoreContext.enter();
      try {
        currentUser = ioTUserService.selectUserByUnionId(unionId);
      } finally {
        TenantIgnoreContext.exit();
      }
    }
    if (currentUser == null) {
      return buildRuntimeFromToken(tenantIdFromToken);
    }
    TenantRuntime runtime = new TenantRuntime();
    runtime.superAdmin = currentUser.isAdmin();
    runtime.tenantId = StrUtil.blankToDefault(currentUser.getTenantId(), currentUser.getUnionId());
    runtime.deptId = currentUser.getDeptId();
    return runtime;
  }

  private IoTUserService resolveIoTUserService() {
    try {
      return SpringUtils.getBean(IoTUserService.class);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private TenantRuntime buildRuntimeFromToken(String tenantId) {
    if (StrUtil.isBlank(tenantId)) {
      return null;
    }
    TenantRuntime runtime = new TenantRuntime();
    runtime.tenantId = tenantId;
    return runtime;
  }

  private boolean isSimpleValue(Object value) {
    return value instanceof CharSequence
        || value instanceof Number
        || value instanceof Boolean
        || value instanceof Date
        || value instanceof Enum<?>
        || value.getClass().isPrimitive()
        || value.getClass().getName().startsWith("java.time.");
  }

  private static final class TenantRuntime {
    private String tenantId;
    private Long deptId;
    private boolean superAdmin;
  }
}
