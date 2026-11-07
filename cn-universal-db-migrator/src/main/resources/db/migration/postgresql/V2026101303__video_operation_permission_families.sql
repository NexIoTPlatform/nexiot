-- Separate watch/control/configuration/maintenance authorization for all video vendors.
-- Preserve existing permission IDs and copy the legacy edit boundary; package grants
-- never expand to packages without the old edit permission.
SET LOCAL lock_timeout = '10s';
LOCK TABLE sys_menu, sys_role_menu, iot_tenant_package_menu IN SHARE ROW EXCLUSIVE MODE;

DO $video_permissions$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms IN ('video:platform:view', 'video:platform:list')) THEN
        RAISE EXCEPTION 'Video permission parent menu is missing';
    END IF;
    IF EXISTS (
        SELECT 1 FROM sys_menu actual
        JOIN (VALUES (2071303000000000001::BIGINT, 'video:device:play'),
                     (2071303000000000002::BIGINT, 'video:device:control'),
                     (2071303000000000003::BIGINT, 'video:device:config'),
                     (2071303000000000004::BIGINT, 'video:device:maintain')) expected(menu_id, perms)
          ON actual.menu_id = expected.menu_id
        WHERE actual.perms IS DISTINCT FROM expected.perms
    ) THEN
        RAISE EXCEPTION 'Video permission fixed menu ID collides with another permission';
    END IF;
    IF EXISTS (
        SELECT perms FROM sys_menu
        WHERE perms IN ('video:device:play', 'video:device:control', 'video:device:config', 'video:device:maintain')
        GROUP BY perms HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate video operation permission menus';
    END IF;
    IF EXISTS (
        SELECT 1 FROM sys_menu actual
        WHERE actual.perms IN ('video:device:play', 'video:device:control', 'video:device:config', 'video:device:maintain')
          AND (COALESCE(actual.menu_type, '?') <> 'F' OR COALESCE(actual.status, '?') <> '0'
               OR NOT EXISTS (SELECT 1 FROM sys_menu parent WHERE parent.menu_id = actual.parent_id
                              AND parent.perms IN ('video:platform:view', 'video:platform:list')))
    ) THEN
        RAISE EXCEPTION 'Incompatible existing video operation permission menu';
    END IF;
END $video_permissions$;

INSERT INTO sys_menu
    (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,
     menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT expected.menu_id, expected.menu_name, parent.menu_id, expected.order_num, '', '', 1, 0,
       'F', '0', '0', expected.perms, '', 'migration', CURRENT_TIMESTAMP, 'migration', CURRENT_TIMESTAMP,
       '视频操作分类权限；兼容已有平台编辑授权'
FROM (VALUES (2071303000000000001::BIGINT, '视频观看', 30, 'video:device:play'),
             (2071303000000000002::BIGINT, '设备控制', 31, 'video:device:control'),
             (2071303000000000003::BIGINT, '设备配置', 32, 'video:device:config'),
             (2071303000000000004::BIGINT, '设备维护', 33, 'video:device:maintain'))
     expected(menu_id,menu_name,order_num,perms)
CROSS JOIN (SELECT MIN(menu_id) AS menu_id FROM sys_menu
            WHERE perms IN ('video:platform:view', 'video:platform:list')) parent
WHERE NOT EXISTS (SELECT 1 FROM sys_menu existing WHERE existing.perms = expected.perms);

WITH candidates AS (
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
)
INSERT INTO sys_role_menu (role_id,menu_id,uuid)
SELECT candidate.role_id, candidate.menu_id,
       ids.max_id + ROW_NUMBER() OVER (ORDER BY candidate.role_id,candidate.menu_id)
FROM candidates candidate CROSS JOIN (SELECT COALESCE(MAX(uuid),0) AS max_id FROM sys_role_menu) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu existing
                  WHERE existing.role_id = candidate.role_id AND existing.menu_id = candidate.menu_id);

WITH candidates AS (
    SELECT DISTINCT source_grant.package_id, target.menu_id
    FROM iot_tenant_package_menu source_grant
    JOIN sys_menu source_menu ON source_menu.menu_id = source_grant.menu_id
    CROSS JOIN sys_menu target
    WHERE source_menu.perms = 'video:platform:edit'
      AND target.perms IN ('video:device:play', 'video:device:control', 'video:device:config', 'video:device:maintain')
)
INSERT INTO iot_tenant_package_menu (package_id,menu_id,id,create_time)
SELECT candidate.package_id, candidate.menu_id,
       ids.max_id + ROW_NUMBER() OVER (ORDER BY candidate.package_id,candidate.menu_id), CURRENT_TIMESTAMP
FROM candidates candidate CROSS JOIN (SELECT COALESCE(MAX(id),0) AS max_id FROM iot_tenant_package_menu) ids
WHERE NOT EXISTS (SELECT 1 FROM iot_tenant_package_menu existing
                  WHERE existing.package_id = candidate.package_id AND existing.menu_id = candidate.menu_id);
