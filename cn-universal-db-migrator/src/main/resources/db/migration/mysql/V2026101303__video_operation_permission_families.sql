-- MySQL 8: separate video operation families while preserving the legacy edit boundary.
-- Derived tables with UNION/DISTINCT/GROUP BY/aggregates are intentionally materialized
-- so INSERT ... SELECT does not read its target through a mergeable subquery.
DROP PROCEDURE IF EXISTS nexiot_video_permissions_guard_2026101303;
DELIMITER //
CREATE PROCEDURE nexiot_video_permissions_guard_2026101303()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms IN ('video:platform:view', 'video:platform:list')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Video permission parent menu is missing';
    END IF;
    IF EXISTS (
        SELECT 1 FROM sys_menu actual
        JOIN (SELECT 2071303000000000001 AS menu_id, 'video:device:play' AS perms
              UNION ALL SELECT 2071303000000000002, 'video:device:control'
              UNION ALL SELECT 2071303000000000003, 'video:device:config'
              UNION ALL SELECT 2071303000000000004, 'video:device:maintain') expected
          ON actual.menu_id = expected.menu_id
        WHERE NOT (actual.perms <=> expected.perms)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Video permission fixed menu ID collides with another permission';
    END IF;
    IF EXISTS (
        SELECT perms FROM sys_menu
        WHERE perms IN ('video:device:play', 'video:device:control', 'video:device:config', 'video:device:maintain')
        GROUP BY perms HAVING COUNT(*) > 1
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate video operation permission menus';
    END IF;
    IF EXISTS (
        SELECT 1 FROM sys_menu actual
        WHERE actual.perms IN ('video:device:play', 'video:device:control', 'video:device:config', 'video:device:maintain')
          AND (COALESCE(actual.menu_type, '?') <> 'F' OR COALESCE(actual.status, '?') <> '0'
               OR NOT EXISTS (SELECT 1 FROM sys_menu parent WHERE parent.menu_id = actual.parent_id
                              AND parent.perms IN ('video:platform:view', 'video:platform:list')))
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Incompatible existing video operation permission menu';
    END IF;
END//
DELIMITER ;

START TRANSACTION;
CALL nexiot_video_permissions_guard_2026101303();
INSERT INTO sys_menu
    (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,
     menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT expected.menu_id, expected.menu_name, parent.menu_id, expected.order_num, '', '', 1, 0,
       'F', '0', '0', expected.perms, '', 'migration', CURRENT_TIMESTAMP, 'migration', CURRENT_TIMESTAMP,
       '视频操作分类权限；兼容已有平台编辑授权'
FROM (SELECT 2071303000000000001 AS menu_id, '视频观看' AS menu_name, 30 AS order_num, 'video:device:play' AS perms
      UNION ALL SELECT 2071303000000000002, '设备控制', 31, 'video:device:control'
      UNION ALL SELECT 2071303000000000003, '设备配置', 32, 'video:device:config'
      UNION ALL SELECT 2071303000000000004, '设备维护', 33, 'video:device:maintain') expected
CROSS JOIN (SELECT MIN(menu_id) AS menu_id FROM sys_menu
            WHERE perms IN ('video:platform:view', 'video:platform:list')) parent
WHERE NOT EXISTS (SELECT 1 FROM (SELECT perms FROM sys_menu GROUP BY perms) existing
                  WHERE existing.perms = expected.perms);

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
WHERE NOT EXISTS (SELECT 1 FROM (SELECT role_id,menu_id FROM sys_role_menu GROUP BY role_id,menu_id) existing
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
WHERE NOT EXISTS (SELECT 1 FROM (SELECT package_id,menu_id FROM iot_tenant_package_menu GROUP BY package_id,menu_id) existing
                  WHERE existing.package_id = candidate.package_id AND existing.menu_id = candidate.menu_id);
COMMIT;
DROP PROCEDURE nexiot_video_permissions_guard_2026101303;
