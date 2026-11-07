package cn.universal.security.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.admin.SysDept;
import cn.universal.persistence.entity.admin.SysRole;
import cn.universal.persistence.entity.admin.SysRoleDept;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.persistence.mapper.admin.SysDeptMapper;
import cn.universal.persistence.mapper.admin.SysRoleDeptMapper;
import cn.universal.persistence.mapper.admin.SysRoleMapper;
import cn.universal.persistence.page.PageContextRunner;
import cn.universal.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import tk.mybatis.mapper.entity.Example;

/** 统一计算部门数据权限范围，口径与芋道 DeptDataPermissionRule 对齐到“部门 + 本人”组合。 */
@Component
public class DeptDataPermissionSupport {

  private static final String DATA_SCOPE_ALL = "1";
  private static final String DATA_SCOPE_CUSTOM = "2";
  private static final String DATA_SCOPE_DEPT = "3";
  private static final String DATA_SCOPE_DEPT_AND_CHILD = "4";
  private static final String DATA_SCOPE_SELF = "5";
  private static final String TENANT_ROLE_ADMIN = "ADMIN";
  private static final String REQUEST_CACHE_PREFIX =
      DeptDataPermissionSupport.class.getName() + ".scope.";

  @Resource private IoTUserMapper userMapper;
  @Resource private SysRoleMapper roleMapper;
  @Resource private SysRoleDeptMapper roleDeptMapper;
  @Resource private SysDeptMapper deptMapper;

  public DeptDataPermissionScope resolveCurrentScope() {
    return resolveScope(SecurityUtils.getUnionIdQuietly());
  }

  public DeptDataPermissionScope resolveScope(String unionId) {
    if (StrUtil.isBlank(unionId)) {
      return null;
    }
    DeptDataPermissionScope cached = getCachedScope(unionId);
    if (cached != null) {
      return cached;
    }

    IoTUser currentUser = selectActiveUserByUnionId(unionId);
    if (currentUser == null) {
      return null;
    }

    DeptDataPermissionScope scope = new DeptDataPermissionScope();
    scope.currentUnionId = unionId;
    scope.tenantId = resolveTenantId(currentUser);
    scope.tenantAdmin = isTenantAdmin(currentUser);
    if (currentUser.isAdmin()) {
      scope.superAdmin = true;
      scope.all = true;
      cacheScope(unionId, scope);
      return scope;
    }

    List<SysRole> roles = ignoreAll(() -> roleMapper.selectRolePermissionByUnionId(unionId));
    boolean hasActiveRole = false;
    boolean selfVisible = false;
    List<SysDept> tenantDepts = null;
    for (SysRole role : roles) {
      if (role == null || StrUtil.equals(role.getStatus(), "1")) {
        continue;
      }
      hasActiveRole = true;
      String dataScope = StrUtil.blankToDefault(role.getDataScope(), DATA_SCOPE_SELF);
      if (DATA_SCOPE_ALL.equals(dataScope)) {
        scope.all = true;
        break;
      }
      if (DATA_SCOPE_CUSTOM.equals(dataScope)) {
        tenantDepts = ensureTenantDepts(scope.tenantId, tenantDepts);
        addCustomDeptScope(scope, role, tenantDepts);
        continue;
      }
      if (DATA_SCOPE_DEPT.equals(dataScope)) {
        addDeptScope(scope, currentUser.getDeptId());
        continue;
      }
      if (DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope)) {
        tenantDepts = ensureTenantDepts(scope.tenantId, tenantDepts);
        addDeptAndChildrenScope(scope, currentUser.getDeptId(), tenantDepts);
        continue;
      }
      selfVisible = true;
    }

    if (!hasActiveRole) {
      selfVisible = true;
    }
    if (scope.all) {
      fillAllTenantUnionScope(scope);
    } else {
      if (selfVisible) {
        scope.scopeUnionIds.add(unionId);
      }
      if (CollUtil.isNotEmpty(scope.scopeDeptIds)) {
        fillDeptUnionScope(scope);
      }
    }
    if (scope.scopeUnionIds.isEmpty() && selfVisible) {
      scope.scopeUnionIds.add(unionId);
    }
    scope.denied =
        !scope.all
            && !scope.tenantAdmin
            && scope.scopeDeptIds.isEmpty()
            && scope.scopeUnionIds.isEmpty();
    cacheScope(unionId, scope);
    return scope;
  }

  public void applyScope(Object parameterObject) {
    applyScope(parameterObject, resolveCurrentScope());
  }

  public void applyScope(Object parameterObject, String unionId) {
    applyScope(parameterObject, resolveScope(unionId));
  }

  public void applyScope(Object parameterObject, DeptDataPermissionScope scope) {
    if (scope == null || scope.isSuperAdmin()) {
      return;
    }
    applyScope(parameterObject, scope, new IdentityHashMap<>());
  }

  public boolean canAccessResource(String tenantId, Long deptId, String creatorUnionId) {
    return canAccessResource(resolveCurrentScope(), tenantId, deptId, creatorUnionId);
  }

  public boolean canAccessResource(
      DeptDataPermissionScope scope, String tenantId, Long deptId, String creatorUnionId) {
    if (scope == null || scope.superAdmin) {
      return true;
    }
    String normalizedTenantId =
        StrUtil.blankToDefault(tenantId, resolveCreatorTenantId(creatorUnionId));
    if (StrUtil.isNotBlank(scope.tenantId)
        && StrUtil.isNotBlank(normalizedTenantId)
        && !StrUtil.equals(scope.tenantId, normalizedTenantId)) {
      return false;
    }
    if (scope.all) {
      return true;
    }
    if (scope.tenantAdmin) {
      return StrUtil.isNotBlank(normalizedTenantId)
          && StrUtil.equals(scope.tenantId, normalizedTenantId);
    }
    if (StrUtil.isNotBlank(creatorUnionId) && scope.scopeUnionIds.contains(creatorUnionId)) {
      return true;
    }
    return deptId != null && scope.scopeDeptIds.contains(deptId);
  }

  public static final class DeptDataPermissionScope {

    private String currentUnionId;
    private String tenantId;
    private boolean superAdmin;
    private boolean tenantAdmin;
    private boolean all;
    private boolean denied;
    private final Set<Long> scopeDeptIds = new LinkedHashSet<>();
    private final Set<String> scopeUnionIds = new LinkedHashSet<>();

    public String getCurrentUnionId() {
      return currentUnionId;
    }

    public String getTenantId() {
      return tenantId;
    }

    public boolean isSuperAdmin() {
      return superAdmin;
    }

    public boolean isTenantAdmin() {
      return tenantAdmin;
    }

    public boolean isAll() {
      return all;
    }

    public boolean isDenied() {
      return denied;
    }

    public Set<Long> getScopeDeptIds() {
      return Collections.unmodifiableSet(scopeDeptIds);
    }

    public Set<String> getScopeUnionIds() {
      return Collections.unmodifiableSet(scopeUnionIds);
    }
  }

  private DeptDataPermissionScope getCachedScope(String unionId) {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes == null) {
      return null;
    }
    try {
      Object cached = requestAttributes.getAttribute(cacheKey(unionId), RequestAttributes.SCOPE_REQUEST);
      return cached instanceof DeptDataPermissionScope scope ? scope : null;
    } catch (IllegalStateException ignored) {
      return null;
    }
  }

  private void cacheScope(String unionId, DeptDataPermissionScope scope) {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes == null || scope == null) {
      return;
    }
    try {
      requestAttributes.setAttribute(cacheKey(unionId), scope, RequestAttributes.SCOPE_REQUEST);
    } catch (IllegalStateException ignored) {
      // Request may already be completed when code runs in a detached/background context.
    }
  }

  private String cacheKey(String unionId) {
    return REQUEST_CACHE_PREFIX + unionId;
  }

  private void applyScope(
      Object parameterObject,
      DeptDataPermissionScope scope,
      IdentityHashMap<Object, Boolean> visited) {
    if (parameterObject == null
        || scope == null
        || visited.containsKey(parameterObject)
        || isSimpleValue(parameterObject)) {
      return;
    }
    visited.put(parameterObject, Boolean.TRUE);

    if (parameterObject instanceof Example) {
      return;
    }
    if (parameterObject instanceof Map<?, ?> map) {
      for (Object value : map.values()) {
        applyScope(value, scope, visited);
      }
      return;
    }
    if (parameterObject instanceof Collection<?> collection) {
      for (Object item : collection) {
        applyScope(item, scope, visited);
      }
      return;
    }
    if (parameterObject.getClass().isArray()) {
      int length = Array.getLength(parameterObject);
      for (int i = 0; i < length; i++) {
        applyScope(Array.get(parameterObject, i), scope, visited);
      }
      return;
    }

    BeanWrapper beanWrapper = createBeanWrapper(parameterObject);
    if (beanWrapper == null) {
      return;
    }
    fillTenantProperty(beanWrapper, scope);
    fillDirectScopeProperties(beanWrapper, scope);
    fillParamsScope(beanWrapper, scope);
  }

  private void fillTenantProperty(BeanWrapper beanWrapper, DeptDataPermissionScope scope) {
    if (scope == null
        || scope.isSuperAdmin()
        || StrUtil.isBlank(scope.getTenantId())
        || !beanWrapper.isWritableProperty("tenantId")) {
      return;
    }
    Object currentValue = readProperty(beanWrapper, "tenantId");
    if (currentValue == null || StrUtil.isBlank(String.valueOf(currentValue))) {
      beanWrapper.setPropertyValue("tenantId", scope.getTenantId());
    }
  }

  private void fillDirectScopeProperties(BeanWrapper beanWrapper, DeptDataPermissionScope scope) {
    List<Long> deptIds = new ArrayList<>(scope.getScopeDeptIds());
    List<String> unionIds = new ArrayList<>(scope.getScopeUnionIds());
    if (beanWrapper.isWritableProperty("scopeDeptIds")) {
      beanWrapper.setPropertyValue("scopeDeptIds", deptIds);
    }
    if (beanWrapper.isWritableProperty("scopeUnionIds")) {
      beanWrapper.setPropertyValue("scopeUnionIds", unionIds);
    }
    if (beanWrapper.isWritableProperty("scopeUnionId")) {
      beanWrapper.setPropertyValue(
          "scopeUnionId",
          unionIds.contains(scope.getCurrentUnionId()) ? scope.getCurrentUnionId() : null);
    }
    if (beanWrapper.isWritableProperty("scopeDenied")) {
      beanWrapper.setPropertyValue("scopeDenied", scope.isDenied());
    }
  }

  @SuppressWarnings("unchecked")
  private void fillParamsScope(BeanWrapper beanWrapper, DeptDataPermissionScope scope) {
    if (!beanWrapper.isReadableProperty("params")) {
      return;
    }
    Object paramsObject = beanWrapper.getPropertyValue("params");
    if (!(paramsObject instanceof Map<?, ?> rawMap)) {
      return;
    }
    Map<String, Object> params = (Map<String, Object>) rawMap;
    params.remove("scopeDeptIds");
    params.remove("scopeUnionIds");
    params.remove("scopeUnionId");
    params.remove("scopeDenied");
    params.remove("tenantAdminScope");
    if (scope.isDenied()) {
      params.put("scopeDenied", true);
      return;
    }
    if (scope.isTenantAdmin()) {
      params.put("tenantAdminScope", true);
    }
    if (!scope.getScopeDeptIds().isEmpty()) {
      params.put("scopeDeptIds", new ArrayList<>(scope.getScopeDeptIds()));
    }
    if (!scope.getScopeUnionIds().isEmpty()) {
      params.put("scopeUnionIds", new ArrayList<>(scope.getScopeUnionIds()));
    }
    if (scope.getScopeUnionIds().contains(scope.getCurrentUnionId())) {
      params.put("scopeUnionId", scope.getCurrentUnionId());
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

  private boolean isSimpleValue(Object value) {
    return value instanceof String
        || value instanceof Number
        || value instanceof Boolean
        || value instanceof Character
        || value instanceof Enum<?>;
  }

  private void fillDeptUnionScope(DeptDataPermissionScope scope) {
    if (scope == null || StrUtil.isBlank(scope.tenantId) || CollectionUtils.isEmpty(scope.scopeDeptIds)) {
      return;
    }
    List<IoTUser> tenantUsers = selectTenantUsers(scope.tenantId);
    if (CollectionUtils.isEmpty(tenantUsers)) {
      return;
    }
    for (IoTUser user : tenantUsers) {
      if (user == null
          || user.getDeptId() == null
          || StrUtil.isBlank(user.getUnionId())
          || !scope.scopeDeptIds.contains(user.getDeptId())) {
        continue;
      }
      scope.scopeUnionIds.add(user.getUnionId());
    }
  }

  private void fillAllTenantUnionScope(DeptDataPermissionScope scope) {
    if (scope == null || StrUtil.isBlank(scope.tenantId)) {
      return;
    }
    List<IoTUser> tenantUsers = selectTenantUsers(scope.tenantId);
    if (CollectionUtils.isEmpty(tenantUsers)) {
      return;
    }
    for (IoTUser user : tenantUsers) {
      if (user != null && StrUtil.isNotBlank(user.getUnionId())) {
        scope.scopeUnionIds.add(user.getUnionId());
      }
    }
  }

  private List<SysDept> ensureTenantDepts(String tenantId, List<SysDept> tenantDepts) {
    if (tenantDepts != null || StrUtil.isBlank(tenantId)) {
      return tenantDepts;
    }
    Example example = new Example(SysDept.class);
    example.createCriteria().andEqualTo("tenantId", tenantId);
    example.setOrderByClause("parent_id asc, order_num asc, dept_id asc");
    return ignoreAll(() -> deptMapper.selectByExample(example));
  }

  private List<IoTUser> selectTenantUsers(String tenantId) {
    if (StrUtil.isBlank(tenantId)) {
      return new ArrayList<>();
    }
    Example example = new Example(IoTUser.class);
    example.createCriteria().andEqualTo("tenantId", tenantId).andEqualTo("deleted", 0);
    example.setOrderByClause("id asc");
    return ignoreAll(() -> userMapper.selectByExample(example));
  }

  private IoTUser selectActiveUserByUnionId(String unionId) {
    if (StrUtil.isBlank(unionId)) {
      return null;
    }
    Example example = new Example(IoTUser.class);
    example.createCriteria().andEqualTo("unionId", unionId).andEqualTo("deleted", 0);
    return ignoreAll(() -> userMapper.selectOneByExample(example));
  }

  private void addCustomDeptScope(
      DeptDataPermissionScope scope, SysRole role, List<SysDept> tenantDepts) {
    if (scope == null
        || role == null
        || role.getRoleId() == null
        || CollectionUtils.isEmpty(tenantDepts)) {
      return;
    }
    Set<Long> tenantDeptIds = new LinkedHashSet<>();
    for (SysDept dept : tenantDepts) {
      if (dept != null && dept.getDeptId() != null) {
        tenantDeptIds.add(dept.getDeptId());
      }
    }
    Example example = new Example(SysRoleDept.class);
    example.createCriteria().andEqualTo("roleId", role.getRoleId());
    List<SysRoleDept> roleDepts = ignoreAll(() -> roleDeptMapper.selectByExample(example));
    for (SysRoleDept roleDept : roleDepts) {
      if (roleDept != null
          && roleDept.getDeptId() != null
          && tenantDeptIds.contains(roleDept.getDeptId())) {
        scope.scopeDeptIds.add(roleDept.getDeptId());
      }
    }
  }

  private void addDeptScope(DeptDataPermissionScope scope, Long deptId) {
    if (scope == null || deptId == null) {
      return;
    }
    scope.scopeDeptIds.add(deptId);
  }

  private void addDeptAndChildrenScope(
      DeptDataPermissionScope scope, Long deptId, List<SysDept> tenantDepts) {
    if (scope == null || deptId == null || CollectionUtils.isEmpty(tenantDepts)) {
      return;
    }
    for (SysDept dept : tenantDepts) {
      if (dept == null || dept.getDeptId() == null) {
        continue;
      }
      if (deptId.equals(dept.getDeptId()) || containsAncestor(dept.getAncestors(), deptId)) {
        scope.scopeDeptIds.add(dept.getDeptId());
      }
    }
  }

  private boolean containsAncestor(String ancestors, Long deptId) {
    if (StrUtil.isBlank(ancestors) || deptId == null) {
      return false;
    }
    List<String> ancestorList = StrUtil.splitTrim(ancestors, ',');
    return ancestorList != null && ancestorList.contains(String.valueOf(deptId));
  }

  private String resolveTenantId(IoTUser user) {
    if (user == null) {
      return null;
    }
    return StrUtil.blankToDefault(user.getTenantId(), user.getUnionId());
  }

  private boolean isTenantAdmin(IoTUser user) {
    return user != null
        && !user.isAdmin()
        && StrUtil.equalsIgnoreCase(user.getTenantRole(), TENANT_ROLE_ADMIN);
  }

  private String resolveCreatorTenantId(String creatorUnionId) {
    IoTUser creator = selectActiveUserByUnionId(creatorUnionId);
    return resolveTenantId(creator);
  }

  private <T> T ignoreAll(Supplier<T> supplier) {
    TenantIgnoreContext.enter();
    DataPermissionIgnoreContext.enter();
    try {
      return PageContextRunner.runWithoutPaging(supplier);
    } finally {
      DataPermissionIgnoreContext.exit();
      TenantIgnoreContext.exit();
    }
  }
}
