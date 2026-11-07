-- Independently assign READ without granting watch/control/configuration/maintenance.
-- Existing video readers/editors retain their current read boundary, as do their packages.
SET LOCAL lock_timeout = '10s';
LOCK TABLE sys_menu, sys_role_menu, iot_tenant_package_menu IN SHARE ROW EXCLUSIVE MODE;

DO $video_read_permission$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms IN ('video:platform:view', 'video:platform:list')) THEN
        RAISE EXCEPTION 'Video permission parent menu is missing';
    END IF;
    IF EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2071401000000000001
               AND perms IS DISTINCT FROM 'video:platform:query') THEN
        RAISE EXCEPTION 'Video READ permission fixed menu ID collides with another permission';
    END IF;
    IF (SELECT COUNT(*) FROM sys_menu WHERE perms='video:platform:query') > 1 THEN
        RAISE EXCEPTION 'Duplicate video READ permission menus';
    END IF;
    IF EXISTS (SELECT 1 FROM sys_menu actual WHERE actual.perms='video:platform:query'
               AND (COALESCE(actual.menu_type, '?') <> 'F' OR COALESCE(actual.status, '?') <> '0'
                    OR NOT EXISTS (SELECT 1 FROM sys_menu parent WHERE parent.menu_id=actual.parent_id
                                   AND parent.perms IN ('video:platform:view', 'video:platform:list')))) THEN
        RAISE EXCEPTION 'Incompatible existing video READ permission menu';
    END IF;
END $video_read_permission$;

INSERT INTO sys_menu
    (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,
     menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 2071401000000000001, '视频读取', parent.menu_id, 34, '', '', 1, 0,
       'F', '0', '0', 'video:platform:query', '', 'migration', CURRENT_TIMESTAMP,
       'migration', CURRENT_TIMESTAMP, '视频读取权限；兼容既有平台查看和编辑授权'
FROM (SELECT MIN(menu_id) AS menu_id FROM sys_menu
      WHERE perms IN ('video:platform:view', 'video:platform:list')) parent
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='video:platform:query');

WITH candidates AS (
    SELECT DISTINCT source_grant.role_id, target.menu_id
    FROM sys_role_menu source_grant
    JOIN sys_menu source_menu ON source_menu.menu_id=source_grant.menu_id
    CROSS JOIN sys_menu target
    WHERE source_menu.perms IN ('video:platform:view', 'video:platform:edit')
      AND target.perms='video:platform:query'
    UNION
    SELECT admin_role.role_id, target.menu_id FROM sys_role admin_role CROSS JOIN sys_menu target
    WHERE admin_role.role_key='admin' AND target.perms='video:platform:query'
)
INSERT INTO sys_role_menu (role_id,menu_id,uuid)
SELECT candidate.role_id, candidate.menu_id,
       ids.max_id + ROW_NUMBER() OVER (ORDER BY candidate.role_id,candidate.menu_id)
FROM candidates candidate CROSS JOIN (SELECT COALESCE(MAX(uuid),0) AS max_id FROM sys_role_menu) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu existing
                  WHERE existing.role_id=candidate.role_id AND existing.menu_id=candidate.menu_id);

WITH candidates AS (
    SELECT DISTINCT source_grant.package_id, target.menu_id
    FROM iot_tenant_package_menu source_grant
    JOIN sys_menu source_menu ON source_menu.menu_id=source_grant.menu_id
    CROSS JOIN sys_menu target
    WHERE source_menu.perms IN ('video:platform:view', 'video:platform:edit')
      AND target.perms='video:platform:query'
)
INSERT INTO iot_tenant_package_menu (package_id,menu_id,id,create_time)
SELECT candidate.package_id, candidate.menu_id,
       ids.max_id + ROW_NUMBER() OVER (ORDER BY candidate.package_id,candidate.menu_id), CURRENT_TIMESTAMP
FROM candidates candidate CROSS JOIN (SELECT COALESCE(MAX(id),0) AS max_id FROM iot_tenant_package_menu) ids
WHERE NOT EXISTS (SELECT 1 FROM iot_tenant_package_menu existing
                  WHERE existing.package_id=candidate.package_id AND existing.menu_id=candidate.menu_id);
