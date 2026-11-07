-- Dameng 8 standalone incremental SQL; current DatabaseMigrator runs PostgreSQL/MySQL only.
-- Inherit only active device-edit roles/packages; roll back all DML on error.
DECLARE
    v_parent_id BIGINT;
    v_count INTEGER;
BEGIN
    LOCK TABLE sys_menu IN EXCLUSIVE MODE;
    LOCK TABLE sys_role_menu IN EXCLUSIVE MODE;
    LOCK TABLE iot_tenant_package_menu IN EXCLUSIVE MODE;
    SELECT MIN(menu_id) INTO v_parent_id FROM sys_menu
    WHERE menu_id=1098 AND menu_type='C' AND status='0' AND perms='tool:instance:list';
    IF v_parent_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20001, 'Device permission parent menu is missing or incompatible');
    END IF;
    SELECT COUNT(*) INTO v_count FROM sys_menu WHERE menu_id=2071801000000000001
    AND (perms IS NULL OR perms <> 'industrial:endpoint:probe');
    IF v_count > 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Industrial probe permission fixed menu ID collides with another permission');
    END IF;
    SELECT COUNT(*) INTO v_count FROM sys_menu WHERE perms='industrial:endpoint:probe';
    IF v_count > 1 THEN
        RAISE_APPLICATION_ERROR(-20003, 'Duplicate industrial probe permission menus');
    END IF;
    SELECT COUNT(*) INTO v_count FROM sys_menu actual WHERE actual.perms='industrial:endpoint:probe'
    AND (COALESCE(actual.menu_type, '?') <> 'F' OR COALESCE(actual.status, '?') <> '0'
         OR NOT EXISTS (SELECT 1 FROM sys_menu parent WHERE parent.menu_id=actual.parent_id
                        AND parent.menu_id=1098 AND parent.menu_type='C'
                                   AND parent.status='0' AND parent.perms='tool:instance:list'));
    IF v_count > 0 THEN
        RAISE_APPLICATION_ERROR(-20004, 'Incompatible existing industrial probe permission menu');
    END IF;

    INSERT INTO sys_menu
        (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,
         menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
    SELECT 2071801000000000001, '测试连接', v_parent_id, 120, '', '', 1, 0,
           'F', '0', '0', 'industrial:endpoint:probe', '', 'migration', CURRENT_TIMESTAMP,
           'migration', CURRENT_TIMESTAMP, '工业设备连接诊断；兼容既有设备配置修改授权'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='industrial:endpoint:probe');

    INSERT INTO sys_role_menu (role_id,menu_id,uuid)
    SELECT candidate.role_id, candidate.menu_id,
           ids.max_id + ROW_NUMBER() OVER (ORDER BY candidate.role_id,candidate.menu_id)
    FROM (
        SELECT DISTINCT source_grant.role_id, target.menu_id
        FROM sys_role_menu source_grant
        JOIN sys_menu source_menu ON source_menu.menu_id=source_grant.menu_id
        CROSS JOIN sys_menu target
        WHERE source_menu.perms='industrial:device:edit' AND source_menu.parent_id=1098
          AND source_menu.menu_type='F' AND source_menu.status='0'
          AND target.perms='industrial:endpoint:probe'
    ) candidate
    CROSS JOIN (SELECT COALESCE(MAX(uuid),0) AS max_id FROM sys_role_menu) ids
    WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu existing
                      WHERE existing.role_id=candidate.role_id AND existing.menu_id=candidate.menu_id);

    INSERT INTO iot_tenant_package_menu (package_id,menu_id,id,create_time)
    SELECT candidate.package_id, candidate.menu_id,
           ids.max_id + ROW_NUMBER() OVER (ORDER BY candidate.package_id,candidate.menu_id), CURRENT_TIMESTAMP
    FROM (
        SELECT DISTINCT source_grant.package_id, target.menu_id
        FROM iot_tenant_package_menu source_grant
        JOIN sys_menu source_menu ON source_menu.menu_id=source_grant.menu_id
        CROSS JOIN sys_menu target
        WHERE source_menu.perms='industrial:device:edit' AND source_menu.parent_id=1098
          AND source_menu.menu_type='F' AND source_menu.status='0'
          AND target.perms='industrial:endpoint:probe'
    ) candidate
    CROSS JOIN (SELECT COALESCE(MAX(id),0) AS max_id FROM iot_tenant_package_menu) ids
    WHERE NOT EXISTS (SELECT 1 FROM iot_tenant_package_menu existing
                      WHERE existing.package_id=candidate.package_id AND existing.menu_id=candidate.menu_id);
    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/
