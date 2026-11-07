-- Retire the standalone console; device configuration still uses shared permissions.
-- Preserve their menu IDs and grants, and keep runtime READ for root-only grants.
SET LOCAL lock_timeout = '10s';
LOCK TABLE sys_menu, sys_role_menu, iot_tenant_package_menu IN SHARE ROW EXCLUSIVE MODE;

DO $industrial_console$
DECLARE retired_ids bigint[];
BEGIN
    IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2062600000000000001) THEN
        RETURN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2062600000000000001
                   AND path='industrial' AND menu_type='C' AND perms='industrial:runtime:query') THEN
        RAISE EXCEPTION 'Industrial console menu ID is incompatible';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=1098 AND menu_type='C'
                   AND perms='tool:instance:list' AND status='0') THEN
        RAISE EXCEPTION 'Device menu is missing or incompatible';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2062600000000000051
                   AND parent_id IN (2062600000000000001,1098)
                   AND menu_type='F' AND perms='industrial:runtime:query' AND status='0') THEN
        RAISE EXCEPTION 'Shared industrial READ permission is missing or incompatible';
    END IF;

    -- Only relocate permissions still used by device details, imports and templates.
    UPDATE sys_menu SET parent_id=1098
    WHERE parent_id=2062600000000000001 AND menu_type='F' AND perms IN (
        'industrial:endpoint:edit', 'industrial:endpoint:start', 'industrial:endpoint:stop',
        'industrial:endpoint:reload', 'industrial:device:edit',
        'industrial:point:add', 'industrial:point:edit', 'industrial:point:remove',
        'industrial:point:enable', 'industrial:point:validate', 'industrial:point:preview',
        'industrial:group:add', 'industrial:group:edit', 'industrial:group:remove',
        'industrial:group:enable', 'industrial:group:start', 'industrial:group:stop',
        'industrial:group:preview', 'industrial:runtime:query', 'industrial:runtime:write',
        'industrial:template:query', 'industrial:template:add', 'industrial:template:remove',
        'industrial:config:import', 'industrial:config:export'
    );

    INSERT INTO sys_role_menu(role_id,menu_id,uuid)
    SELECT DISTINCT source.role_id,2062600000000000051,
           ids.max_id + DENSE_RANK() OVER (ORDER BY source.role_id)
    FROM sys_role_menu source CROSS JOIN (SELECT COALESCE(MAX(uuid),0) AS max_id FROM sys_role_menu) ids
    WHERE source.menu_id=2062600000000000001
      AND EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=source.menu_id AND status='0')
      AND NOT EXISTS (SELECT 1 FROM sys_role_menu existing
                      WHERE existing.role_id=source.role_id AND existing.menu_id=2062600000000000051);

    INSERT INTO iot_tenant_package_menu(package_id,menu_id,id,create_time)
    SELECT DISTINCT source.package_id,2062600000000000051,
           ids.max_id + DENSE_RANK() OVER (ORDER BY source.package_id),CURRENT_TIMESTAMP
    FROM iot_tenant_package_menu source
    CROSS JOIN (SELECT COALESCE(MAX(id),0) AS max_id FROM iot_tenant_package_menu) ids
    WHERE source.menu_id=2062600000000000001
      AND EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=source.menu_id AND status='0')
      AND NOT EXISTS (SELECT 1 FROM iot_tenant_package_menu existing
                      WHERE existing.package_id=source.package_id AND existing.menu_id=2062600000000000051);

    WITH RECURSIVE retired AS (
        SELECT menu_id FROM sys_menu WHERE menu_id=2062600000000000001
        UNION
        SELECT child.menu_id FROM sys_menu child JOIN retired parent ON child.parent_id=parent.menu_id
    ) SELECT ARRAY_AGG(menu_id) INTO retired_ids FROM retired;

    DELETE FROM sys_role_menu WHERE menu_id=ANY(retired_ids);
    DELETE FROM iot_tenant_package_menu WHERE menu_id=ANY(retired_ids);
    DELETE FROM sys_menu WHERE menu_id=ANY(retired_ids);
END $industrial_console$;
