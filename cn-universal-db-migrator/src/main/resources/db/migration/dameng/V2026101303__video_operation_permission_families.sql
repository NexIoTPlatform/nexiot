-- Dameng 8 standalone incremental script. The current DatabaseMigrator enables
-- PostgreSQL/MySQL only and does not consume this directory. Run this block in
-- the existing application schema after DBA review; never replay Docker bootstrap.
-- A single block rolls back every change on error and commits only on success.
DECLARE
    v_parent_id BIGINT;
    v_count INTEGER;
BEGIN
    LOCK TABLE sys_menu IN EXCLUSIVE MODE;
    LOCK TABLE sys_role_menu IN EXCLUSIVE MODE;
    LOCK TABLE iot_tenant_package_menu IN EXCLUSIVE MODE;

    SELECT MIN(menu_id) INTO v_parent_id FROM sys_menu
    WHERE perms IN ('video:platform:view', 'video:platform:list');
    IF v_parent_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20001, 'Video permission parent menu is missing');
    END IF;
    SELECT COUNT(*) INTO v_count
    FROM sys_menu actual
    JOIN (SELECT 2071303000000000001 AS menu_id, 'video:device:play' AS perms FROM DUAL
          UNION ALL SELECT 2071303000000000002, 'video:device:control' FROM DUAL
          UNION ALL SELECT 2071303000000000003, 'video:device:config' FROM DUAL
          UNION ALL SELECT 2071303000000000004, 'video:device:maintain' FROM DUAL) expected
      ON actual.menu_id = expected.menu_id
    WHERE actual.perms IS NULL OR actual.perms <> expected.perms;
    IF v_count > 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Video permission fixed menu ID collides with another permission');
    END IF;
    SELECT COUNT(*) INTO v_count FROM (
        SELECT perms FROM sys_menu
        WHERE perms IN ('video:device:play', 'video:device:control', 'video:device:config', 'video:device:maintain')
        GROUP BY perms HAVING COUNT(*) > 1
    ) duplicates;
    IF v_count > 0 THEN
        RAISE_APPLICATION_ERROR(-20003, 'Duplicate video operation permission menus');
    END IF;
    SELECT COUNT(*) INTO v_count FROM sys_menu actual
    WHERE actual.perms IN ('video:device:play', 'video:device:control', 'video:device:config', 'video:device:maintain')
      AND (COALESCE(actual.menu_type, '?') <> 'F' OR COALESCE(actual.status, '?') <> '0'
           OR NOT EXISTS (SELECT 1 FROM sys_menu parent WHERE parent.menu_id = actual.parent_id
                          AND parent.perms IN ('video:platform:view', 'video:platform:list')));
    IF v_count > 0 THEN
        RAISE_APPLICATION_ERROR(-20004, 'Incompatible existing video operation permission menu');
    END IF;

    INSERT INTO sys_menu
        (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,
         menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
    SELECT expected.menu_id, expected.menu_name, v_parent_id, expected.order_num, '', '', 1, 0,
           'F', '0', '0', expected.perms, '', 'migration', CURRENT_TIMESTAMP, 'migration', CURRENT_TIMESTAMP,
           '视频操作分类权限；兼容已有平台编辑授权'
    FROM (SELECT 2071303000000000001 AS menu_id, '视频观看' AS menu_name, 30 AS order_num, 'video:device:play' AS perms FROM DUAL
          UNION ALL SELECT 2071303000000000002, '设备控制', 31, 'video:device:control' FROM DUAL
          UNION ALL SELECT 2071303000000000003, '设备配置', 32, 'video:device:config' FROM DUAL
          UNION ALL SELECT 2071303000000000004, '设备维护', 33, 'video:device:maintain' FROM DUAL) expected
    WHERE NOT EXISTS (SELECT 1 FROM sys_menu existing WHERE existing.perms = expected.perms);

    INSERT INTO sys_role_menu (role_id,menu_id,uuid)
    SELECT candidate.role_id, candidate.menu_id,
           ids.max_id + ROW_NUMBER() OVER (ORDER BY candidate.role_id,candidate.menu_id)
    FROM (
        SELECT DISTINCT source_grant.role_id, target.menu_id
        FROM sys_role_menu source_grant
        JOIN sys_menu source_menu ON source_menu.menu_id = source_grant.menu_id
        CROSS JOIN sys_menu target
        WHERE source_menu.perms = 'video:platform:edit'
          AND target.perms IN ('video:device:play', 'video:device:control', 'video:device:config', 'video:device:maintain')
        UNION
        SELECT admin_role.role_id, target.menu_id FROM sys_role admin_role CROSS JOIN sys_menu target
        WHERE admin_role.role_key = 'admin'
          AND target.perms IN ('video:device:play', 'video:device:control', 'video:device:config', 'video:device:maintain')
    ) candidate
    CROSS JOIN (SELECT COALESCE(MAX(uuid),0) AS max_id FROM sys_role_menu) ids
    WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu existing
                      WHERE existing.role_id = candidate.role_id AND existing.menu_id = candidate.menu_id);

    INSERT INTO iot_tenant_package_menu (package_id,menu_id,id,create_time)
    SELECT candidate.package_id, candidate.menu_id,
           ids.max_id + ROW_NUMBER() OVER (ORDER BY candidate.package_id,candidate.menu_id), CURRENT_TIMESTAMP
    FROM (
        SELECT DISTINCT source_grant.package_id, target.menu_id
        FROM iot_tenant_package_menu source_grant
        JOIN sys_menu source_menu ON source_menu.menu_id = source_grant.menu_id
        CROSS JOIN sys_menu target
        WHERE source_menu.perms = 'video:platform:edit'
          AND target.perms IN ('video:device:play', 'video:device:control', 'video:device:config', 'video:device:maintain')
    ) candidate
    CROSS JOIN (SELECT COALESCE(MAX(id),0) AS max_id FROM iot_tenant_package_menu) ids
    WHERE NOT EXISTS (SELECT 1 FROM iot_tenant_package_menu existing
                      WHERE existing.package_id = candidate.package_id AND existing.menu_id = candidate.menu_id);
    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/
