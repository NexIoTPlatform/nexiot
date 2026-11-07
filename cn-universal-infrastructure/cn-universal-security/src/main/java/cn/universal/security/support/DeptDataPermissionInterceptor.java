package cn.universal.security.support;

import cn.universal.security.support.DeptDataPermissionSupport.DeptDataPermissionScope;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** 在 MyBatis 查询入口统一注入部门数据权限上下文，避免各个 service 重复拼装权限条件。 */
@Component
@Intercepts({
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
        CacheKey.class,
        BoundSql.class
      })
})
public class DeptDataPermissionInterceptor implements Interceptor {

  private final ObjectProvider<DeptDataPermissionSupport> deptDataPermissionSupportProvider;

  public DeptDataPermissionInterceptor(
      ObjectProvider<DeptDataPermissionSupport> deptDataPermissionSupportProvider) {
    this.deptDataPermissionSupportProvider = deptDataPermissionSupportProvider;
  }

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    if (DataPermissionIgnoreContext.isIgnore() || TenantIgnoreContext.isIgnore()) {
      return invocation.proceed();
    }
    // Break the sqlSessionFactory -> interceptor -> mapper circular dependency on startup.
    DeptDataPermissionSupport deptDataPermissionSupport =
        deptDataPermissionSupportProvider.getIfAvailable();
    if (deptDataPermissionSupport == null) {
      return invocation.proceed();
    }
    DeptDataPermissionScope scope = deptDataPermissionSupport.resolveCurrentScope();
    if (scope == null || scope.isSuperAdmin()) {
      return invocation.proceed();
    }

    Object[] args = invocation.getArgs();
    Object parameterObject = args.length > 1 ? args[1] : null;
    deptDataPermissionSupport.applyScope(parameterObject, scope);
    return invocation.proceed();
  }

  @Override
  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }
}
